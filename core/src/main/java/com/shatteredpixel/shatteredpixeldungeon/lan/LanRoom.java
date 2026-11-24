package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.PathFinder;
import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import com.watabou.utils.Bundle;

/** Construct, pump and close on the engine executor. Clients (including the host seat) use TCP. */
public final class LanRoom implements AutoCloseable {
    public enum State { LOBBY_OPEN, PLAYER_JOINED, READY, STARTING, ROSTER_LOCKED, IN_GAME, PAUSED, PARTY_DEFEATED, CLOSED }
    private static final class Seat {
        LanTransport.Peer peer;
        PublicKey verifier;
        boolean confirmed;
        final long player;
        HeroClass type;
        boolean ready;
        Seat(LanTransport.Peer peer, long player) { this.peer = peer; this.player = player; }
    }
    public final UUID session;
    private UUID epoch;
    private CoopSaves.Slot slot;
    private boolean waiting, persistenceFailed;
    private final SecureRandom challenges = new SecureRandom();
    private final String hostKey = UUID.randomUUID().toString();
    private final Thread owner = Thread.currentThread();
    private final LanTransport io;
    private final Seat[] seats = new Seat[2];
    private final Map<Long,Item> offered = new LinkedHashMap<>();
    private final long seed;
    private long playerSequence, itemSequence, itemTurn;
    private String itemList, problem;
    private State state = State.LOBBY_OPEN;
    private CoopSession game;
    private boolean locked;
    private RoomSync sync;
    private final boolean synchronizedRoom;

    public LanRoom(long seed) throws IOException {
        this(null, LanClient.TCP_PORT, LanClient.UDP_PORT, seed);
    }

    public LanRoom(InetAddress bind, int tcp, int udp, long seed) throws IOException {
        this(bind,tcp,udp,seed,false);
    }

    public static LanRoom synchronizedGame(InetAddress bind,int tcp,int udp,long seed) throws IOException {
        return new LanRoom(bind,tcp,udp,seed,true);
    }

    private LanRoom(InetAddress bind,int tcp,int udp,long seed,boolean synchronizedRoom) throws IOException {
        this(bind,tcp,udp,seed,synchronizedRoom,UUID.randomUUID());
    }

    private LanRoom(InetAddress bind,int tcp,int udp,long seed,boolean synchronizedRoom,UUID session) throws IOException {
        this.session=session;
        this.synchronizedRoom=synchronizedRoom;
        this.seed = seed;
        io = new LanTransport(bind,tcp,udp,session);
    }
    public static LanRoom persistentGame(InetAddress bind,int tcp,int udp,long seed,CoopSaves saves) throws IOException {
        LanRoom room=new LanRoom(bind,tcp,udp,seed,true);
        try {
            room.slot=saves.open(room.session);
            if (room.slot.currentGeneration()!=0) throw new IOException("Session already exists");
            room.epoch=UUID.randomUUID();
            return room;
        } catch (IOException | RuntimeException failure) { room.close(); throw failure; }
    }

    public static LanRoom resume(InetAddress bind,int tcp,int udp,CoopSaves saves,UUID session,boolean recoverBackup) throws IOException {
        CoopSaves.Slot disk=saves.existing(session);
        LanRoom room=null;
        try {
            Bundle b=disk.read(recoverBackup);
            if (!b.getBoolean("locked") || !b.contains("epoch")) throw new IOException("Invalid locked roster");
            UUID savedEpoch=UUID.fromString(b.getString("epoch"));
            room=new LanRoom(bind,tcp,udp,0,true,session);
            room.slot=disk;
            room.io.discoverable=false;
            room.epoch=recoverBackup ? UUID.randomUUID() : savedEpoch;
            room.locked=true;
            room.waiting=true;
            room.game=CoopSession.restore(b.getBundle("engine"));
            for (int i=0;i<room.seats.length;i++) {
                Seat s=room.seats[i]=new Seat(null,room.game.player(i));
                s.type=room.game.hero(i).heroClass;
                s.verifier=SeatCredential.verifier(b.getString("key"+i));
            }
            if (room.seats[0].verifier.equals(room.seats[1].verifier)) throw new IOException("Duplicate roster key");
            room.sync=new RoomSync(session,room.game);
            room.sync.restore(b.getBundle("sync"),room.epoch);
            room.state=room.game.defeated() ? State.PARTY_DEFEATED : State.PAUSED;
            room.persist(recoverBackup);
            return room;
        } catch (IOException | RuntimeException | GeneralSecurityException failure) {
            if (room!=null) room.close(); else disk.close();
            throw new IOException("Cannot restore selected generation",failure);
        }
    }

    public long save() throws IOException { checkOwner(); return persist(false); }

    public void saveAndClose() throws IOException { save(); close(); }

    private long persist(boolean recover) throws IOException {
        if (slot==null || !locked || sync==null || sync.paused || persistenceFailed) throw new IOException("No safe checkpoint");
        try {
            Bundle b=new Bundle();
            b.put("locked",true); b.put("epoch",epoch.toString());
            for(int i=0;i<seats.length;i++) b.put("key"+i,SeatCredential.hex(seats[i].verifier.getEncoded()));
            b.put("engine",game.checkpoint()); b.put("sync",sync.checkpoint());
            return slot.publish(b,recover);
        } catch (IOException | RuntimeException failure) {
            persistenceFailed=true; state=State.PAUSED; problem="CHECKPOINT_FAILED";
            throw new IOException("Checkpoint failed",failure);
        }
    }

    private void disconnected(int i) {
        seats[i].peer=null;
        if (!waiting) {
            waiting=true;
            for (Seat seat:seats) seat.confirmed=false;
            if (!persistenceFailed && sync!=null && !sync.paused) {
                sync.connectionBoundary(epoch,true);
                if (!game.defeated()) state=State.PAUSED;
            }
        }
    }

    public int tcpPort() { return io.listener.socket().getLocalPort(); }
    public int udpPort() { return io.discoveryFailure == null ? io.discovery.socket().getLocalPort() : -1; }
    public String discoveryFailure() { return io.discoveryFailure; }
    /** Local controller only. Never put this capability in discovery, diagnostics or lobby state. */
    public String hostJoinCommand() { return "HOST " + hostKey; }
    public State state() { checkOwner(); return state; }
    public String failure() { checkOwner(); return problem; }
    private void checkOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Room executor required");
    }
    public void pump() {
        checkOwner();
        if (state == State.CLOSED) return;
        if (io.failure != null) { problem = io.failure; close(); return; }
        for (int i=0; i<seats.length; i++) {
            if (seats[i] != null && seats[i].peer!=null && seats[i].peer.dead) {
                if (slot!=null && locked) { disconnected(i); continue; }
                if (i == 0) { close(); return; }
                if (locked) {
                    if (state == State.PARTY_DEFEATED) continue;
                    state = State.PAUSED;
                    if (sync != null) sync.peerDisconnected();
                }
                else { seats[i] = null; lobby(); }
            }
        }
        for (int i=0; i<32; i++) {
            LanTransport.Request request = io.requests.poll();
            if (request == null) break;
            if (!request.peer.dead && !request.peer.draining) handle(request);
        }
    }

    private void lobby() {
        if (seats[1] == null) state = State.LOBBY_OPEN;
        else if (seats[0] != null && seats[0].ready && seats[1].ready) state = State.READY;
        else state = State.PLAYER_JOINED;
    }

    private void handle(LanTransport.Request request) {
        String[] f = request.fields;
        LanTransport.Peer peer = request.peer;
        long id;
        try { id = Long.parseLong(f[1]); } catch (NumberFormatException invalid) { peer.dead = true; return; }
        String result;
        if (!f[0].equals("1")) result = "ERROR VERSION";
        else if (id <= peer.lastRequest) result = "ERROR REPLAY";
        else {
            peer.lastRequest = id;
            try { result = dispatch(peer,f); }
            catch (NumberFormatException invalid) { result = "ERROR NUMBER"; }
            catch (IOException failed) { result = "ERROR CHECKPOINT_FAILED"; }
        }
        peer.reply("1 " + id + " " + result);
        if (result.equals("BYE")) peer.draining = true;
    }

    private String dispatch(LanTransport.Peer peer, String[] f) throws IOException {
        String verb = f[3];
        if (verb.equals("HELLO") && f.length == 4 && f[2].equals("-")) {
            peer.greeted = true;
            return "HELLO " + session;
        }
        if (!peer.greeted || !session.toString().equals(f[2])) return "ERROR SESSION";
        if (verb.equals("DISCONNECT") && f.length == 4) return "BYE";
        if (verb.equals("PING") && f.length == 4) return "PONG";
        int seat = -1;
        for (int i=0; i<seats.length; i++) if (seats[i] != null && seats[i].peer == peer) seat = i;
        if (verb.equals("MODE") && f.length == 5 && f[4].equals("SYNC1")) {
            if (seat >= 0) return "ERROR LOCKED";
            if (!synchronizedRoom) return "ERROR MODE";
            peer.syncMode = true;
            return "MODE SYNC1";
        }
        if (verb.equals("JOIN") || verb.equals("HOST")) {
            if (locked) return "ERROR LOCKED";
            if (seat >= 0) return "ERROR DUPLICATE";
            boolean host = verb.equals("HOST");
            if (host && (!peer.loopback || f.length != 5 || !hostKey.equals(f[4]))) return "ERROR AUTH";
            if (!host && f.length != 4) return "ERROR FORMAT";
            seat = host ? 0 : 1;
            if (seats[seat] != null) return "ERROR FULL";
            if (peer.syncMode != synchronizedRoom) return "ERROR MODE";
            seats[seat] = new Seat(peer, ++playerSequence);
            lobby();
            return "JOINED " + seat + " " + seats[seat].player;
        }
        if (slot!=null && locked && peer.syncMode && (verb.equals("CHALLENGE") || verb.equals("PROVE")))
            return authenticate(peer,seat,f);
        if (seat < 0) return "ERROR JOIN_FIRST";
        Seat self = seats[seat];
        if (slot!=null && verb.equals("ENROLL") && !locked && f.length==5) {
            if (self.verifier!=null) return "ERROR ALREADY_ENROLLED";
            try {
                PublicKey key=SeatCredential.verifier(f[4]);
                Seat other=seats[1-seat];
                if (other!=null && key.equals(other.verifier)) return "ERROR DUPLICATE_KEY";
                self.verifier=key;
            }
            catch (GeneralSecurityException invalid) { return "ERROR KEY"; }
            return "ENROLLED "+epoch;
        }
        if (persistenceFailed) return "ERROR CHECKPOINT_FAILED";
        if (peer.syncMode && sync != null) {
            if (slot!=null) {
                for(int i=0;i<seats.length;i++) if(seats[i].peer!=null && seats[i].peer.dead) disconnected(i);
                if (verb.equals("CONFIRM") && f.length==6) {
                    if (!waiting || sync.paused || !sync.matches(seat,Long.parseLong(f[4]),f[5])) return "ERROR SNAPSHOT";
                    self.confirmed=true;
                    if (seats[0].confirmed && seats[1].confirmed && seats[0].peer!=null && seats[1].peer!=null) {
                        waiting=false;
                        sync.connectionBoundary(epoch,false);
                        state=game.defeated() ? State.PARTY_DEFEATED : State.IN_GAME;
                        persist(false);
                    }
                    return "CONFIRMED";
                }
                if (verb.equals("LOOKUP") && f.length==6)
                    return epoch.toString().equals(f[4]) ? sync.lookup(seat,Long.parseLong(f[5])) : "ERROR EPOCH";
                if (verb.equals("DO") || verb.startsWith("DO_")) {
                    if (!verb.equals("DO_"+epoch)) return "ERROR EPOCH";
                    if (waiting || sync.paused) return "ERROR PAUSED";
                    long before=sync.highest(seat);
                    String result=sync.action(seat,f);
                    if (sync.paused) { state=State.PAUSED; problem=sync.failure; return result; }
                    if (game.defeated()) state=State.PARTY_DEFEATED;
                    if (sync.highest(seat)!=before) persist(false);
                    return result;
                }
            }
            if (verb.equals("SYNC") && f.length == 6) return sync.sync(seat,Long.parseLong(f[4]),f[5]);
            if (verb.equals("PAGE") && f.length == 6) return sync.part(seat,Long.parseLong(f[4]),Integer.parseInt(f[5]));
            if (verb.equals("DO")) {
                for (Seat member : seats) if (member.peer.dead) sync.peerDisconnected();
                String result = sync.action(seat,f);
                if (game.defeated()) state = State.PARTY_DEFEATED;
                else if (sync.paused) { state = State.PAUSED; problem = sync.failure; }
                return result;
            }
            if (verb.equals("STATE")) return "USE_SYNC";
        }
        if (peer.syncMode && verb.equals("ACT")) return "ERROR MODE";
        if (verb.equals("STATE") && f.length == 4) return view(seat);
        if (verb.equals("ACT")) return action(seat,f);
        if (locked) return "ERROR LOCKED";
        if (verb.equals("SELECT") && f.length == 5) {
            if (!f[4].equals("WARRIOR") && !f[4].equals("HUNTRESS")) return "ERROR CLASS";
            self.type = HeroClass.valueOf(f[4]);
            self.ready = false;
        } else if (verb.equals("READY") && f.length == 5) {
            if ((slot!=null && self.verifier==null) || self.type == null || !(f[4].equals("0") || f[4].equals("1"))) return "ERROR READY";
            self.ready = f[4].equals("1");
        } else if (verb.equals("START") && f.length == 4) {
            if (seat != 0) return "ERROR AUTH";
            if (state != State.READY || seats[1].peer.dead) return "ERROR NOT_READY";
            state = State.STARTING;
            locked = true;
            io.discoverable = false;
            state = State.ROSTER_LOCKED;
            try {
                game = CoopSession.open(seed,self.player,self.type);
                game.join(seats[1].player,seats[1].type);
                if (peer.syncMode) sync = new RoomSync(session,game);
            } catch (RuntimeException e) { return pause(e.toString()); }
            state = State.IN_GAME;
            if (slot!=null) { sync.connectionBoundary(epoch,false); persist(false); }
            return "GAME_STARTED";
        } else return "ERROR COMMAND";
        lobby();
        return "OK";
    }

    private String authenticate(LanTransport.Peer peer,int attached,String[] f) {
        if (persistenceFailed || sync==null || sync.paused) return "ERROR PAUSED";
        if (attached>=0) return "ERROR DUPLICATE";
        if (f[3].equals("CHALLENGE")) {
            peer.nonce=null;
            if (f.length!=7) return "ERROR FORMAT";
            if (!epoch.toString().equals(f[4])) return "RECOVERY_EPOCH "+epoch;
            int index=Integer.parseInt(f[5]);
            if (index<0 || index>=seats.length || Long.parseLong(f[6])!=seats[index].player) return "ERROR AUTH";
            if (seats[index].peer!=null && !seats[index].peer.dead) return "ERROR DUPLICATE";
            byte[] bytes=new byte[32];
            challenges.nextBytes(bytes);
            peer.nonce=SeatCredential.hex(bytes); peer.challengeSeat=index;
            peer.challengeDeadline=System.nanoTime()+5000000000L;
            return "NONCE "+peer.nonce;
        }
        String nonce=peer.nonce;
        peer.nonce=null;
        if (nonce==null || f.length!=5 || System.nanoTime()-peer.challengeDeadline>=0) return "ERROR AUTH";
        int index=peer.challengeSeat;
        Seat target=seats[index];
        if (target.peer!=null && !target.peer.dead) return "ERROR DUPLICATE";
        try {
            Signature verifier=Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(target.verifier);
            verifier.update(SeatCredential.message(session.toString(),epoch,index,target.player,nonce));
            if (!verifier.verify(SeatCredential.bytes(f[4],80))) return "ERROR AUTH";
        } catch (GeneralSecurityException invalid) { return "ERROR AUTH"; }
        if (target.peer!=null) disconnected(index);
        target.peer=peer; target.confirmed=false;
        return "RESUMED "+index+" "+target.player+" "+game.hero(index).id()+" "+sync.highest(index);
    }

    private String pause(String reason) {
        problem = reason;
        state = State.PAUSED;
        return "ERROR ENGINE_STOPPED";
    }

    private String view(int seat) {
        if (state != State.IN_GAME) {
            StringBuilder roster = new StringBuilder("LOBBY ").append(state);
            for (Seat s : seats) {
                roster.append(' ');
                if (s == null) roster.append('-');
                else roster.append(s.player).append(':').append(s.type).append(':').append(s.ready ? 1 : 0);
            }
            return roster.toString();
        }
        Hero hero = game.hero(seat);
        String status = " " + game.turn() + " " + hero.id() + " " + hero.pos + " " + hero.HP;
        if (game.activeSeat() != seat) return "WAITING" + status;
        if (itemTurn != game.turn()) {
            itemTurn = game.turn();
            offered.clear();
            StringBuilder list = new StringBuilder();
            Heap heap = Dungeon.level.heaps.get(hero.pos);
            if (heap != null && !heap.hidden && heap.type == Heap.Type.HEAP && heap.peek() != null) appendItem(list,"F",heap.peek());
            for (Item item : hero.belongings.backpack.items) appendItem(list,"I",item);
            for (int offset : PathFinder.NEIGHBOURS8) {
                int cell = hero.pos + offset;
                if (!Dungeon.level.insideMap(cell) || !hero.fieldOfView[cell]) continue;
                Char target = Actor.findChar(cell);
                if (target instanceof Mob && target.invisible <= 0 && target.alignment == Char.Alignment.ENEMY) {
                    list.append(" ENEMY:").append(cell);
                } else if (Dungeon.level.passable[cell]) {
                    list.append(" FLOOR:").append(cell);
                }
            }
            itemList = list.toString();
        }
        return "TURN" + status + itemList;
    }

    private void appendItem(StringBuilder list,String location,Item item) {
        if (offered.size() >= 32) return;
        // Reveal categories needed by supported commands only. No true names of unidentified items.
        String kind;
        if (item.getClass() == com.shatteredpixel.shatteredpixeldungeon.items.food.Food.class) kind = "FOOD";
        else if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon) kind = "WEAPON";
        else return;
        offered.put(++itemSequence,item);
        list.append(' ').append(location).append(':').append(kind).append(':').append(itemSequence);
    }

    private String action(int seat,String[] f) {
        if (state != State.IN_GAME) return "ERROR STATE";
        if (f.length != 11) return "ERROR FORMAT";
        if (Long.parseLong(f[4]) != seats[seat].player || Integer.parseInt(f[5]) != seat
                || Integer.parseInt(f[6]) != game.hero(seat).id()) return "ERROR AUTH";
        long turn = Long.parseLong(f[7]);
        if (turn != game.turn() || seat != game.activeSeat()) return "ERROR TURN";
        CoopSession.Action action;
        try { action = CoopSession.Action.valueOf(f[8]); } catch (IllegalArgumentException e) { return "ERROR ACTION"; }
        if (action == CoopSession.Action.REVIVE || action == CoopSession.Action.DROP
                || action == CoopSession.Action.UNEQUIP) return "ERROR MODE";
        int cell = Integer.parseInt(f[9]);
        long handle = Long.parseLong(f[10]);
        boolean needsItem = action.requiresItem();
        Item item = handle == 0 || turn != itemTurn ? null : offered.get(handle);
        if (needsItem ? item == null : handle != 0) return "ERROR ITEM";
        for (Seat s : seats) if (s.peer.dead) { state = State.PAUSED; return "ERROR STATE"; }
        try {
            if (!game.act(seats[seat].player,turn,action,cell,item)) return "ERROR INVALID";
            if (game.defeated()) { state = State.PARTY_DEFEATED; return "PARTY_DEFEATED"; }
            if (game.stopped()) return pause(game.stopReason());
            return "APPLIED " + turn + " " + game.turn() + " " + game.hero(seat).pos;
        } catch (RuntimeException e) { return pause(e.toString()); }
    }

    public void close() {
        checkOwner();
        if (state == State.CLOSED) return;
        state = State.CLOSED;
        try { io.close(); } finally {
            try { if (game != null) game.close(); } finally {
                if (slot!=null) try { slot.close(); } catch (IOException failed) { problem="CLOSE_FAILED"; }
            }
        }
    }
}
