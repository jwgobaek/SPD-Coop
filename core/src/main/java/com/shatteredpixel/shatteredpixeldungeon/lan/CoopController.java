package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Worker-owned client state. The renderer consumes only the immutable Frame publication. */
public final class CoopController {
    public enum Page { MENU, SEARCH, ROOMS, SAVES, TICKETS, CONNECTING, LOBBY, AUTH, SNAPSHOT,
        RECEIPT, EPOCH, GAME, DISCONNECTED, CLOSED }
    public static final class Entry {
        public final UUID id;
        public final String address;
        public final CoopSaves.Summary current,backup;
        Entry(UUID id,String address,CoopSaves.Summary current,CoopSaves.Summary backup) {
            this.id=id; this.address=address; this.current=current; this.backup=backup;
        }
    }
    public static final class Frame {
        public final Page page;
        public final String notice,roster,address,lastResult,cue,issue,crash;
        public final List<Entry> list;
        public final StateImage image;
        public final PublicDungeon dungeon;
        public final UUID session;
        public final int seat;
        public final long pending,lastAction,wireBytes;
        Frame(CoopController c) {
            page=c.page; wireBytes=c.client==null ? 0 : c.client.transferredBytes(); issue=c.issue; crash=c.crash; cue=c.cue; notice=c.notice; roster=c.roster; list=c.list; image=c.image; dungeon=c.dungeon;
            seat=c.ticket==null ? -1 : c.ticket.seat; session=c.ticket==null ? null : c.ticket.session;
            address=c.advertised; pending=c.ticket==null ? 0 : c.ticket.pending;
            lastResult=c.lastResult; lastAction=c.lastAction;
        }
    }

    private final File root;
    private final SeatTickets.Keys keys;
    private final InetAddress bind;
    private final int tcp,udp;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->new Thread(r,"Coop client"));
    private final AtomicBoolean busy=new AtomicBoolean();
    private volatile boolean closing;
    private volatile Frame frame;
    private Page page=Page.MENU;
    private String notice="", roster="", advertised="",lastResult="";
    private long lastAction;
    private String cue="",issue="",crash="-";
    private List<Entry> list=Collections.emptyList();
    private StateImage image;
    private PublicDungeon dungeon;
    private HostedRoom host;
    private SeatTickets tickets;
    private CoopSaves saves;
    private SeatTickets.Ticket ticket;
    private LanClient client;
    private SeatActions actions;
    private DisplayReplica replica;
    private UUID proposedEpoch;
    private long confirmedVersion=-1;

    public CoopController(File root,SeatTickets.Keys keys,InetAddress bind,int tcp,int udp) {
        this.root=root; this.keys=keys; this.bind=bind; this.tcp=tcp; this.udp=udp;
        publish();
        worker.scheduleWithFixedDelay(this::tick,120,160,TimeUnit.MILLISECONDS);
    }

    public Frame frame() { return frame; }
    public boolean busy() { return busy.get() || closing; }
    private void publish() { frame=new Frame(this); }
    private void show(Page next) { page=next; publish(); }
    private void storage() throws IOException {
        if (!root.isDirectory() && !root.mkdirs()) throw new IOException("STORAGE");
        if(saves==null) { saves=new CoopSaves(root); crash=CoopDiagnostics.previousCrash(root); }
        if(tickets==null) tickets=new SeatTickets(root,keys);
    }
    private interface Job { void run() throws Exception; }
    private boolean enqueue(Job job) {
        if (closing || !busy.compareAndSet(false,true)) return false;
        try {
            worker.execute(()->{
                try { notice=""; storage(); job.run(); }
                catch(Exception e) { error(e); }
                finally { publish(); busy.set(false); }
            });
        } catch(RejectedExecutionException done) { busy.set(false); return false; }
        return true;
    }
    private void error(Exception e) {
        String msg=e.getMessage();
        if(e instanceof LanClient.NetworkException) notice=((LanClient.NetworkException)e).code.name();
        else notice=msg!=null && msg.matches("[A-Z_]{1,48}") ? msg : "FAILED";
        issue=notice+" ("+e.getClass().getSimpleName()+")";
        if (page==Page.EPOCH || page==Page.CONNECTING || page==Page.AUTH || page==Page.SNAPSHOT || page==Page.RECEIPT ||
                page==Page.GAME || page==Page.LOBBY) {
            dropClient(); show(Page.DISCONNECTED);
        }
        publish();
    }
    private static void expect(String reply,String expected) throws IOException {
        if (!reply.equals(expected)) throw new IOException(reply.startsWith("ERROR ") ? reply.substring(6) : "PROTOCOL");
    }
    private void dropClient() {
        if(client!=null) try {client.close();} catch(IOException ignored) {}
        client=null;
        if(replica!=null) replica.close();
        replica=null; image=null; dungeon=null; confirmedVersion=-1;
    }

    public void discover() { discover(null); }
    public void discover(InetSocketAddress destination) { enqueue(()->{
        show(Page.SEARCH);
        Map<UUID,InetSocketAddress> found=destination==null ? LanClient.discover(900) : LanClient.discover(destination,900);
        List<Entry> entries=new ArrayList<>();
        for(Map.Entry<UUID,InetSocketAddress> e:found.entrySet())
            entries.add(new Entry(e.getKey(),format(e.getValue()),null,null));
        list=Collections.unmodifiableList(entries); show(Page.ROOMS);
    }); }

    public void saved(boolean reconnect) { enqueue(()->{
        if(page==Page.DISCONNECTED) closeRoom(false);
        List<Entry> entries=new ArrayList<>();
        for(UUID id:reconnect ? tickets.sessions() : saves.sessions()) {
            if (reconnect) {
                String address="";
                try { SeatTickets.Ticket t=tickets.read(id); address=format(InetSocketAddress.createUnresolved(t.host,t.port)); }
                catch(Exception unreadable) { /* Keep damaged entries visible. */ }
                entries.add(new Entry(id,address,null,null));
            } else {
                CoopSaves.Summary current=null,backup=null;
                try { current=saves.inspect(id,false); } catch(IOException invalid) { }
                try { backup=saves.inspect(id,true); } catch(IOException unavailable) { }
                entries.add(new Entry(id,"",current,backup));
            }
        }
        list=Collections.unmodifiableList(entries); show(reconnect ? Page.TICKETS : Page.SAVES);
    }); }

    public void host() { enqueue(()->{
        show(Page.CONNECTING); closeRoom(false); ticket=null; lastAction=0; lastResult="";
        host=new HostedRoom(saves,bind,tcp,udp,new java.security.SecureRandom().nextLong(),null,false);
        advertised=localAddress(host.port);
        join(new InetSocketAddress("127.0.0.1",host.port),host.localJoin,host.session);
    }); }

    public void join(String address,UUID expected) { enqueue(()->{
        show(Page.CONNECTING); closeRoom(false); ticket=null;
        InetSocketAddress target=parse(address);
        advertised=format(target); lastAction=0; lastResult="";
        join(target,"JOIN",expected);
    }); }

    private void join(InetSocketAddress address,String command,UUID expected) throws Exception {
        client=LanClient.synchronizedGame(address);
        UUID id=UUID.fromString(client.session);
        if(expected!=null && !id.equals(expected)) throw new IOException("SESSION");
        String reply=client.request(command);
        if (!reply.startsWith("JOINED ")) expect(reply,"JOINED");
        String[] f=reply.split(" ");
        ticket=tickets.enroll(client,Integer.parseInt(f[1]),Long.parseLong(f[2]),address.getHostString(),address.getPort());
        actions=new SeatActions(id,ticket.seat,ticket.player);
        show(Page.LOBBY); poll();
    }
    public void select(String type) { if(type.equals("WARRIOR") || type.equals("HUNTRESS")) lobby("SELECT "+type); }
    public void ready(boolean value) { lobby("READY "+(value?"1":"0")); }
    public void start() { lobby("START"); }
    private void lobby(String command) { enqueue(()->{
        if(page!=Page.LOBBY) return;
        String result=client.request(command);
        if(result.startsWith("ERROR ")) notice=result.substring(6);
        else expect(result,command.equals("START")?"GAME_STARTED":"OK");
        poll();
    }); }

    public void resume(UUID id,boolean backup) { enqueue(()->{
        show(Page.CONNECTING); closeRoom(false); ticket=tickets.read(id);
        if(ticket.seat!=0) throw new IOException("AUTH");
        host=new HostedRoom(saves,bind,tcp,udp,0,id,backup);
        advertised=localAddress(host.port);
        openResume(new InetSocketAddress("127.0.0.1",host.port));
    }); }
    public void reconnect(UUID id,String address) { enqueue(()->{
        dropClient(); show(Page.CONNECTING); ticket=tickets.read(id);
        InetSocketAddress target=host!=null && host.session.equals(id) ? new InetSocketAddress("127.0.0.1",host.port) :
                address.isEmpty() ? new InetSocketAddress(ticket.host,ticket.port) : parse(address);
        openResume(target);
    }); }
    private void openResume(InetSocketAddress target) throws Exception {
        client=LanClient.synchronizedGame(target);
        if(!ticket.session.toString().equals(client.session)) throw new IOException("SESSION");
        ticket.host=target.getHostString(); ticket.port=target.getPort();
        authenticate(ticket.epoch);
    }
    public void acceptEpoch() { enqueue(()->{ if(page==Page.EPOCH) authenticate(proposedEpoch); }); }
    private void authenticate(UUID epoch) throws Exception {
        show(Page.AUTH);
        String reply=ticket.key.reconnect(client,epoch,ticket.seat,ticket.player);
        if(reply.startsWith("RECOVERY_EPOCH ")) {
            proposedEpoch=UUID.fromString(reply.substring(15)); show(Page.EPOCH); return;
        }
        if(!reply.startsWith("RESUMED ")) expect(reply,"RESUMED");
        String[] f=reply.split(" ");
        if(Integer.parseInt(f[1])!=ticket.seat || Long.parseLong(f[2])!=ticket.player) throw new IOException("AUTH");
        actions=new SeatActions(ticket.session,ticket.seat,ticket.player,Long.parseLong(f[4]));
        if(!ticket.epoch.equals(epoch)) { lastResult="ROLLED_BACK"; ticket.pending=0; }
        ticket.epoch=epoch; tickets.write(ticket);
        replica=new DisplayReplica(ticket.session,ticket.seat);
        show(Page.SNAPSHOT); replica.refresh(client);
        if(ticket.pending>0) {
            show(Page.RECEIPT);
            String result=client.request("LOOKUP "+epoch+" "+ticket.pending);
            lastAction=ticket.pending; lastResult=outcome(result);
            ticket.pending=0; tickets.write(ticket);
        }
        replica.confirmReconnect(client); confirmedVersion=replica.image().version;
        show(Page.GAME); poll();
    }
    private void tick() {
        if(closing || client==null) return;
        try { poll(); } catch(Exception e) { error(e); }
    }
    private void poll() throws Exception {
        if(page==Page.EPOCH) { expect(client.request("PING"),"PONG"); return; }
        if(page==Page.LOBBY) {
            String value=client.request("STATE");
            if(value.startsWith("LOBBY ")) {
                if(!value.equals(roster)) { roster=value; publish(); } return;
            }
            expect(value,"USE_SYNC"); replica=new DisplayReplica(ticket.session,ticket.seat); page=Page.GAME;
        }
        if(page!=Page.GAME) return;
        replica.refresh(client);
        StateImage next=replica.image();
        if("RECONNECTING".equals(next.rows.get("reason")) && next.version!=confirmedVersion) {
            replica.confirmReconnect(client); next=replica.image(); confirmedVersion=next.version;
        }
        if(image!=next) {
            cue="";
            if(image!=null && replica.appliedDelta() && next.version==image.version+1 &&
                    "IN_GAME".equals(image.rows.get("status")) && "IN_GAME".equals(next.rows.get("status"))) {
                String receipt=next.rows.get("receipt");
                if(receipt!=null && receipt.endsWith(",APPLIED")) cue=receipt.split(",")[1];
                if(!replica.witnessedDeaths().isEmpty()) cue="KILL";
                if("DOWNED".equals(image.rows.get("life")) && "ALIVE".equals(next.rows.get("life"))) cue="REVIVED";
                if(Integer.parseInt(next.rows.get("self").split(",")[2])<dungeon.hp) cue="HURT";
            }
            dungeon=next.rows.containsKey("self") ? new PublicDungeon(next) : null;
            image=next; publish();
        }
    }
    public boolean act(StateImage offered,String verb,int cell,long item) {
        if(offered==null || frame.image!=offered || !Integer.toString(frame.seat).equals(offered.rows.get("active"))) return false;
        return enqueue(()->{
            if(image!=offered || page!=Page.GAME || ticket.pending!=0) return;
            SeatActions.Pending p=actions.prepare(image,verb,cell,item);
            ticket.pending=p.id; tickets.write(ticket); publish();
            String result=p.submit(client);
            lastAction=p.id; lastResult=outcome(result);
            if(!result.startsWith("RESULT ")) throw new IOException(lastResult);
            ticket.pending=0; tickets.write(ticket); poll();
        });
    }
    private static String outcome(String reply) {
        if(reply.startsWith("RESULT ")) return reply.substring(reply.lastIndexOf(' ')+1);
        return reply.startsWith("ERROR ") ? reply.substring(6) : reply.equals("UNKNOWN") ? reply : "PROTOCOL";
    }

    public void menu() { enqueue(()->{ closeRoom(false); ticket=null; list=Collections.emptyList(); show(Page.MENU); }); }
    public void leave(boolean save) { enqueue(()->{ closeRoom(save); ticket=null; notice=save?"SAVED":"LEFT"; show(Page.MENU); }); }
    private void closeRoom(boolean save) throws Exception {
        if(host!=null) { host.close(save); host=null; }
        dropClient(); roster=""; advertised="";
    }

    /** Lifecycle exit always releases resources and leaves the last durable generation available. */
    public synchronized void shutdown() {
        if(closing) return;
        closing=true;
        worker.execute(()->{
            try { closeRoom(true); }
            catch(Exception e) { notice="CHECKPOINT_FAILED"; try {closeRoom(false);} catch(Exception ignored) {} }
            finally { show(Page.CLOSED); worker.shutdown(); }
        });
    }

    static InetSocketAddress parse(String value) throws IOException {
        String s=value.trim(); String host=s; int port=LanClient.TCP_PORT;
        if(s.isEmpty() || s.length()>255) throw new IOException("ADDRESS");
        try {
            if(s.startsWith("[")) {
                int end=s.indexOf(']'); host=s.substring(1,end);
                if(end+1<s.length()) { if(s.charAt(end+1)!=':') throw new IOException("ADDRESS"); port=Integer.parseInt(s.substring(end+2)); }
            } else if(s.indexOf(':')>=0 && s.indexOf(':')==s.lastIndexOf(':')) {
                int cut=s.lastIndexOf(':'); host=s.substring(0,cut); port=Integer.parseInt(s.substring(cut+1));
            }
            if(host.isEmpty() || port<1 || port>65535) throw new IOException("ADDRESS");
            return new InetSocketAddress(host,port);
        } catch(IllegalArgumentException | IndexOutOfBoundsException bad) { throw new IOException("ADDRESS"); }
    }
    private static String format(InetSocketAddress a) {
        String h=a.getHostString(); return (h.contains(":")?"["+h+"]":h)+":"+a.getPort();
    }
    private static String localAddress(int port) throws SocketException {
        for(NetworkInterface nic:Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if(!nic.isUp() || nic.isLoopback()) continue;
            for(InetAddress ip:Collections.list(nic.getInetAddresses()))
                if(ip instanceof Inet4Address) return ip.getHostAddress()+":"+port;
        }
        return "127.0.0.1:"+port;
    }
}
