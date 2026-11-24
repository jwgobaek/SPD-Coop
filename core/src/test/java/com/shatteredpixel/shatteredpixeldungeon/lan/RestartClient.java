package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Display-only JVM. Reloads private keys from the test application's private files. */
public final class RestartClient {
    private final LanClient[] clients=new LanClient[2];
    private final DisplayReplica[] models=new DisplayReplica[2];
    private final SeatActions[] actions=new SeatActions[2];
    private final SeatCredential[] keys=new SeatCredential[2];
    private final Path path;
    private final InetSocketAddress address;
    private final UUID session;
    private UUID epoch;
    private int applied,reconnections;
    private RestartClient(Path path) throws Exception {
        this.path=path;
        String[] endpoint=RestartHost.read(path,"endpoint").split("\n");
        address=new InetSocketAddress("127.0.0.1",Integer.parseInt(endpoint[0]));
        for(int i=0;i<2;i++) clients[i]=LanClient.synchronizedGame(address);
        session=UUID.fromString(clients[0].session);
    }

    public static void main(String[] args) throws Exception {
        RestartClient c=new RestartClient(Paths.get(args[0]));
        try { c.run(args[1]); }
        finally { for(LanClient socket:c.clients) if(socket!=null) socket.close(); }
        System.out.println("mode="+args[1]+" acknowledged="+c.applied+" authenticated="+c.reconnections);
    }

    private void refresh() throws IOException { for(int i=0;i<2;i++) models[i].refresh(clients[i]); }
    private StateImage state(int s) { return models[s].image(); }
    private int active() { return Integer.parseInt(state(0).rows.get("active")); }
    private int pos(int s) { return Integer.parseInt(state(s).rows.get("self").split(",")[1]); }
    private long item(int s,String where,String kind) {
        for(Map.Entry<String,String> row:state(s).rows.entrySet())
            if(row.getKey().startsWith("i") && row.getValue().startsWith(where+","+kind+","))
                return Long.parseLong(row.getKey().substring(1));
        return 0;
    }

    private SeatActions.Pending act(String name,int cell,long item) throws Exception {
        int seat=active();
        SeatActions.Pending p=actions[seat].prepare(state(seat),name,cell,item);
        String reply=p.submit(clients[seat]);
        assertTrue(name+" "+reply,reply.endsWith(" APPLIED"));
        applied++;
        refresh();
        return p;
    }
    private void offer(int seat) throws Exception {
        for(int i=0;active()!=seat && i<12;i++) act("WAIT",0,0);
        assertEquals(seat,active());
    }

    private String disclosedWorld(int s) {
        List<String> publicRows=new ArrayList<>();
        for(Map.Entry<String,String> e:state(s).rows.entrySet()) {
            String key=e.getKey();
            if (key.matches("t[0-9]+|r[0-9]+|m[0-9]+|z[0-9]+|b[0-9]+|self|ally|life|allylife|revivable|clock|turn"))
                publicRows.add(key+"="+e.getValue());
            if (key.matches("i[0-9]+")) publicRows.add("item="+e.getValue());
        }
        Collections.sort(publicRows);
        return StateImage.hash(publicRows.toString());
    }

    private void exploreGuest() throws Exception {
        offer(1);
        int home=pos(1), width=Integer.parseInt(state(1).rows.get("width"));
        for(int step:new int[]{1,-1,width,-width,width+1,width-1,-width+1,-width-1}) {
            int cell=home+step;
            if (cell==pos(0) || !"1".equals(state(1).rows.get("t"+cell))) continue;
            SeatActions.Pending move=actions[1].prepare(state(1),"MOVE",cell,0);
            if (!move.submit(clients[1]).endsWith(" APPLIED")) continue;
            applied++; refresh(); assertEquals(cell,pos(1));
            offer(1); act("MOVE",home,0); assertEquals(home,pos(1)); return;
        }
        fail("No supported departure cell");
    }

    private void bind(int i) throws Exception {
        String joined=null;
        for(int n=0;n<24;n++) {
            joined=keys[i].reconnect(clients[i],epoch,i,i+1);
            if (!joined.equals("ERROR DUPLICATE")) break;
            Thread.sleep(12);
        }
        assertTrue(joined,joined.startsWith("RESUMED "+i+" "+(i+1)+" "));
        actions[i]=new SeatActions(session,i,i+1,Long.parseLong(joined.split(" ")[4]));
        models[i]=new DisplayReplica(session,i);
        reconnections++;
    }

    private void barrier() throws Exception {
        for(int i=0;i<2;i++) models[i].confirmReconnect(clients[i]);
        refresh();
    }
    private void reconnectPair() throws Exception {
        for(LanClient c:clients) c.close();
        for(int i=0;i<2;i++) { clients[i]=LanClient.synchronizedGame(address); bind(i); }
        barrier();
    }

    private void run(String mode) throws Exception {
        if (mode.equals("new")) {
            for(int i=0;i<2;i++) {
                assertTrue(clients[i].request(i==0 ? RestartHost.read(path,"endpoint").split("\n")[1] : "JOIN").startsWith("JOINED"));
                keys[i]=SeatCredential.generate();
                try(OutputStream out=Files.newOutputStream(path.resolve("private"+i))) { keys[i].writePrivate(out); }
                epoch=UUID.fromString(keys[i].enroll(clients[i]).split(" ")[1]);
                actions[i]=new SeatActions(session,i,i+1);
                models[i]=new DisplayReplica(session,i);
                clients[i].request("SELECT "+(i==0?"WARRIOR":"HUNTRESS")); clients[i].request("READY 1");
            }
            assertEquals("GAME_STARTED",clients[0].request("START"));
            RestartHost.write(path,"epoch",epoch.toString());
            refresh(); act("WAIT",0,0);
            exploreGuest();
            offer(0);
            SeatActions.Pending p=act("PICK_UP",pos(0),item(0,"F"+pos(0),"FOOD"));
            verifyAfterReconnect(p,0);
            offer(0); act("DROP",pos(0),item(0,"BAG","FOOD"));
            offer(0);
            int cell=-1;
            for(Map.Entry<String,String> e:state(0).rows.entrySet())
                if (e.getKey().matches("m[0-9]+") && e.getValue().contains(",Rat,")) cell=Integer.parseInt(e.getValue().split(",")[0]);
            assertTrue(cell>=0);
            verifyAfterReconnect(act("ATTACK",cell,0),0);
            offer(0); verifyAfterReconnect(act("EAT",0,item(0,"BAG","FOOD")),0);
            offer(0);
            RestartHost.write(path,"inflict","1");
            long until=System.nanoTime()+3000000000L;
            while(!Files.exists(path.resolve("injured")) && System.nanoTime()<until) Thread.sleep(2);
            assertTrue(Files.exists(path.resolve("injured")));
            act("WAIT",0,0);
            assertEquals("DOWNED",state(1).rows.get("life"));
            for(int i=0;i<2;i++) RestartHost.write(path,"public"+i,disclosedWorld(i));
            RestartHost.write(path,"frozenclock",state(0).rows.get("clock"));
            return;
        }
        epoch=UUID.fromString(RestartHost.read(path,"epoch"));
        for(int i=0;i<2;i++) {
            try(InputStream in=Files.newInputStream(path.resolve("private"+i))) { keys[i]=SeatCredential.readPrivate(in); }
        }
        if (mode.equals("backup")) {
            String warning=keys[0].reconnect(clients[0],epoch,0,1);
            assertTrue(warning,warning.startsWith("RECOVERY_EPOCH "));
            epoch=UUID.fromString(warning.split(" ")[1]);
        }
        for(int i=0;i<2;i++) bind(i);
        barrier();
        if (mode.startsWith("kill_") || mode.equals("recover_last")) {
            if(Files.exists(path.resolve("interrupted"))) {
                String[] last=RestartHost.read(path,"interrupted").split(" ");
                int prior=Integer.parseInt(last[0]);
                String receipt=clients[prior].request("LOOKUP "+epoch+" "+last[1]);
                boolean published=last[3].equals("kill_AFTER_HEAD");
                if(published) assertTrue(receipt,receipt.endsWith(" APPLIED")); else assertEquals("UNKNOWN",receipt);
                assertEquals(Long.parseLong(last[2])+(published?1:0),Long.parseLong(state(0).rows.get("turn")));
            }
            if (mode.equals("recover_last")) { act("WAIT",0,0); return; }
            int seat=active();
            SeatActions.Pending pending=actions[seat].prepare(state(seat),"WAIT",0,0);
            RestartHost.write(path,"interrupted",seat+" "+pending.id+" "+state(seat).rows.get("turn")+" "+mode);
            RestartHost.write(path,"fault-armed","1");
            assertThrows(IOException.class,()->pending.submit(clients[seat]));
            return;
        }
        if (mode.equals("revive")) {
            assertEquals(RestartHost.read(path,"frozenclock"),state(0).rows.get("clock"));
            for(int i=0;i<2;i++) assertEquals(RestartHost.read(path,"public"+i),disclosedWorld(i));
            assertEquals("DOWNED",state(1).rows.get("life")); assertTrue(item(1,"BAG","ANKH")>0);
            assertEquals(0,active());
            SeatActions.Pending p=actions[0].prepare(state(0),"REVIVE",pos(1),0);
            RestartHost.write(path,"revival",Long.toString(p.id));
            RestartHost.write(path,"kill-before-reply","1");
            assertThrows(IOException.class,()->p.submit(clients[0]));
        } else if(mode.equals("soak")) {
            String outcome=clients[0].request("LOOKUP "+epoch+" "+RestartHost.read(path,"revival"));
            assertTrue(outcome,outcome.endsWith(" APPLIED"));
            assertEquals("5",state(1).rows.get("self").split(",")[2]); assertEquals(0,item(1,"BAG","ANKH"));
            assertEquals("ALIVE",state(1).rows.get("life"));
            for(int n=0;n<1000;n++) {
                if(n%80==10) reconnectPair();
                Thread.sleep(18);
                int seat=active();
                SeatActions.Pending p=act("WAIT",0,0);
                if(n==999) RestartHost.write(path,"last",seat+" "+p.id);
            }
            assertEquals("ERROR EXPIRED",clients[0].request("LOOKUP "+epoch+" "+RestartHost.read(path,"revival")));
        } else if(mode.equals("failure")) {
            int seat=active();
            SeatActions.Pending pending=actions[seat].prepare(state(seat),"WAIT",0,0);
            RestartHost.write(path,"unconfirmed",seat+" "+pending.id+" "+state(seat).rows.get("turn"));
            RestartHost.write(path,"fail-write","1");
            assertEquals("ERROR CHECKPOINT_FAILED",pending.submit(clients[seat]));
            assertEquals("ERROR CHECKPOINT_FAILED",clients[seat].request("LOOKUP "+epoch+" "+pending.id));
        } else if(mode.equals("verifyfailure")) {
            String[] info=RestartHost.read(path,"unconfirmed").split(" ");
            int seat=Integer.parseInt(info[0]);
            assertEquals("UNKNOWN",clients[seat].request("LOOKUP "+epoch+" "+info[1]));
            assertEquals(info[2],state(seat).rows.get("turn"));
            act("WAIT",0,0);
        } else {
            String[] last=RestartHost.read(path,"last").split(" ");
            if (!mode.equals("backup")) {
                String reply=clients[Integer.parseInt(last[0])].request("LOOKUP "+epoch+" "+last[1]);
                assertTrue(reply,reply.endsWith(" APPLIED"));
            } else assertEquals("ERROR EPOCH",clients[0].request("LOOKUP "+RestartHost.read(path,"epoch")+" "+last[1]));
            act("WAIT",0,0);
        }
    }

    private void verifyAfterReconnect(SeatActions.Pending pending,int seat) throws Exception {
        String before=clients[seat].request("LOOKUP "+epoch+" "+pending.id);
        String self=state(seat).rows.get("self"), clock=state(seat).rows.get("clock");
        reconnectPair();
        assertTrue(before.endsWith(" APPLIED"));
        assertEquals(before,pending.submit(clients[seat]));
        assertEquals(before,clients[seat].request("LOOKUP "+epoch+" "+pending.id));
        assertEquals(self,state(seat).rows.get("self")); assertEquals(clock,state(seat).rows.get("clock"));
    }
}
