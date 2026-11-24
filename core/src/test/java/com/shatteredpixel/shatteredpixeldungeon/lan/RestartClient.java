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
    private final List<Long> latency=new ArrayList<>();
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
        long started=System.nanoTime();
        String reply=p.submit(clients[seat]);
        latency.add((System.nanoTime()-started)/1000);
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
        boolean special=mode.equals("sentry") || mode.equals("piranha") || mode.equals("quest") || mode.equals("hive");
        if (special) {
            try (LanClient old = new LanClient(address)) {
                assertEquals("ERROR VERSION", old.request("MODE SYNC3"));
            }
        }
        if (special || mode.equals("new") || mode.equals("walk") || mode.startsWith("cast")) {
            for(int i=0;i<2;i++) {
                assertTrue(clients[i].request(i==0 ? RestartHost.read(path,"endpoint").split("\n")[1] : "JOIN").startsWith("JOINED"));
                keys[i]=SeatCredential.generate();
                try(OutputStream out=Files.newOutputStream(path.resolve("private"+i))) { keys[i].writePrivate(out); }
                epoch=UUID.fromString(keys[i].enroll(clients[i]).split(" ")[1]);
                actions[i]=new SeatActions(session,i,i+1);
                models[i]=new DisplayReplica(session,i);
                String character=mode.equals("quest")?"MAGE":mode.startsWith("cast") && i==Integer.parseInt(mode.substring(4)) ? "MAGE" : mode.equals("walk") ? (i==0?"MAGE":"WARRIOR") : i==0?"WARRIOR":"HUNTRESS";
                assertEquals("OK",clients[i].request("SELECT "+character)); clients[i].request("READY 1");
            }
            assertEquals("GAME_STARTED",clients[0].request("START"));
            RestartHost.write(path,"epoch",epoch.toString());
            refresh();
            if(mode.equals("walk")) { walk(); return; }
            act("WAIT",0,0);
            if (mode.equals("quest")) { ghostQuest(); return; }
            if (mode.equals("hive")) { hive(); return; }
            if (special) {
                checkSpecialRoom(mode.equals("sentry")?"Sentry":"Piranha");
                return;
            }
            if(mode.startsWith("cast")) {
                int seat=Integer.parseInt(mode.substring(4));
                offer(seat);
                PublicDungeon view=new PublicDungeon(state(seat));
                long staff=item(seat,"WEAPON","STAFF");
                assertTrue(staff>0);
                assertEquals(0,item(1-seat,"WEAPON","STAFF"));
                int cell=view.pos+3;
                assertEquals("Rat",view.monsters.get(cell));
                int charge=view.belongings.stream().filter(i->i.id==staff).findFirst().get().charges;
                verifyAfterReconnect(act("ZAP",cell,staff),seat);
                assertEquals(charge-1,charges(seat));
                offer(seat);
                long newStaff=item(seat,"WEAPON","STAFF");
                SeatActions.Pending p=actions[seat].prepare(state(seat),"ZAP",cell,newStaff);
                RestartHost.write(path,"cast-record",p.id+" "+cell+" "+(charge-2)+" "+monsterHP(seat,cell)+" "+p.command);
                RestartHost.write(path,"kill-before-reply","1");
                assertThrows(IOException.class,()->p.submit(clients[seat]));
                return;
            }
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
        if (mode.equals("backup") || mode.equals("floorbackup")) {
            String warning=keys[0].reconnect(clients[0],epoch,0,1);
            assertTrue(warning,warning.startsWith("RECOVERY_EPOCH "));
            epoch=UUID.fromString(warning.split(" ")[1]);
        }
        for(int i=0;i<2;i++) bind(i);
        barrier();
        if (mode.equals("questreceipt")) {
            String[] saved=RestartHost.read(path,"ghost-reward").split(" ",5);
            int seat=Integer.parseInt(saved[0]),other=1-seat;
            int ownSize=Integer.parseInt(saved[1]),otherSize=Integer.parseInt(saved[2]);
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+saved[3]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(saved[4]));
            refresh();
            assertEquals(ownSize+1,new PublicDungeon(state(seat)).belongings.size());
            assertEquals(otherSize,new PublicDungeon(state(other)).belongings.size());
            for(int i=0;i<2;i++) assertFalse(new PublicDungeon(state(i)).monsters.containsValue("Ghost"));
            act("WAIT",0,0);
            return;
        }
        if(mode.equals("floorbackup")) {
            assertEquals(1,new PublicDungeon(state(0)).depth);
            assertEquals("ERROR EPOCH",clients[0].request("LOOKUP "+RestartHost.read(path,"epoch")+" 1"));
            act("WAIT",0,0);
            return;
        }
        if (mode.equals("migrate")) {
            assertEquals(RestartHost.read(path,"frozenclock"),state(0).rows.get("clock"));
            for(int i=0;i<2;i++) assertEquals(RestartHost.read(path,"public"+i),disclosedWorld(i));
            assertEquals("DOWNED",state(1).rows.get("life"));
            assertTrue(item(1,"BAG","ANKH")>0);
            assertTrue(clients[0].request("LOOKUP "+epoch+" "+RestartHost.read(path,"legacy-id")).endsWith(" APPLIED"));
            act("REVIVE",pos(1),0);
            assertEquals("ALIVE",state(1).rows.get("life"));
            return;
        }
        if (mode.equals("ascendfault") || mode.equals("backatstairs") || mode.equals("verifyfloor")) {
            afterStairs(mode);
            return;
        }
        if (mode.startsWith("readcast")) {
            int seat=Integer.parseInt(mode.substring(8));
            String[] saved=RestartHost.read(path,"cast-record").split(" ",5);
            int cell=Integer.parseInt(saved[1]);
            int charges=Integer.parseInt(saved[2]);
            int oldHP=Integer.parseInt(saved[3]),hp=monsterHP(seat,cell);
            assertTrue(oldHP-hp>=2 && oldHP-hp<=8);
            assertEquals(charges,charges(seat));
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+saved[0]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(saved[4]));
            refresh();
            assertEquals(hp,monsterHP(seat,cell));
            assertEquals(charges,charges(seat));
            offer(seat);
            act("ZAP",cell,item(seat,"WEAPON","STAFF"));
            assertEquals(charges-1,charges(seat));
            return;
        }
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

    private SeatActions.Pending stairAction(int seat,String verb,int cell) throws Exception {
        return actions[seat].prepare(state(seat),verb,cell,0);
    }

    private SeatActions.Pending consentAndCross() throws Exception {
        refresh();
        int mover=active(),other=1-mover;
        int cell=new PublicDungeon(state(mover)).stair;
        assertTrue(cell>=0);
        String reply=stairAction(other,"CONSENT",cell).submit(clients[other]);
        assertTrue(reply,reply.endsWith(" APPLIED"));
        refresh();
        assertTrue(new PublicDungeon(state(mover)).consent);
        return stairAction(mover,"TRAVEL",cell);
    }

    private void hive() throws Exception {
        int owner=active();
        assertTrue(consentAndCross().submit(clients[owner]).endsWith(" APPLIED")); applied++;
        refresh();
        act("WAIT",0,0);
        offer(1);
        long pot=item(1,"F"+pos(1),"HONEYPOT");
        assertTrue(pot>0);
        act("PICK_UP",pos(1),pot);
        offer(1);
        SeatActions.Pending shatter=act("SHATTER",pos(1),item(1,"BAG","HONEYPOT"));
        verifyAfterReconnect(shatter,1);
        assertTrue(new PublicDungeon(state(1)).monsters.containsValue("Bee"));
        assertTrue(item(1,"BAG","SHELL")>0);
        assertEquals(0,item(0,"BAG","SHELL"));
        offer(1);
        verifyAfterReconnect(act("DROP",pos(1),item(1,"BAG","SHELL")),1);
        assertTrue(item(1,"F"+pos(1),"SHELL")>0);
    }

    private void ghostQuest() throws Exception {
        int owner=active();
        assertTrue(consentAndCross().submit(clients[owner]).endsWith(" APPLIED"));
        applied++;
        refresh();
        act("WAIT",0,0);
        for (int n=0;n<12;n++) {
            Thread.sleep(45);
            int seat=active();
            Optional<Map.Entry<String,String>> ghost=state(seat).rows.entrySet().stream()
                    .filter(e->e.getKey().matches("quest[0-9]+") && e.getValue().equals("OFFER")).findFirst();
            if (ghost.isPresent()) {
                act("ACCEPT_GHOST",Integer.parseInt(ghost.get().getKey().substring(5)),0);
                break;
            }
            act("WAIT",0,0);
        }
        int casts=0;
        for (int n=0;n<35;n++) {
            Thread.sleep(45);
            int seat=active();
            PublicDungeon view=new PublicDungeon(state(seat));
            Optional<Map.Entry<String,String>> reward=state(seat).rows.entrySet().stream()
                    .filter(e->e.getKey().matches("quest[0-9]+") && e.getValue().equals("REWARD")).findFirst();
            if (reward.isPresent()) {
                assertTrue(casts>0);
                SeatActions.Pending request=actions[seat].prepare(state(seat),"GHOST_ARMOR",
                        Integer.parseInt(reward.get().getKey().substring(5)),0);
                RestartHost.write(path,"ghost-reward",seat+" "+view.belongings.size()+" "+
                        new PublicDungeon(state(1-seat)).belongings.size()+" "+request.id+" "+request.command);
                RestartHost.write(path,"kill-before-reply","1");
                assertThrows(IOException.class,()->request.submit(clients[seat]));
                return;
            }
            Optional<Integer> rat=view.monsters.entrySet().stream().filter(e->e.getValue().equals("FetidRat"))
                    .map(Map.Entry::getKey).findFirst();
            if (rat.isPresent() && charges(seat)>0) {
                act("ZAP",rat.get(),item(seat,"WEAPON","STAFF")); casts++;
            } else act("WAIT",0,0);
        }
        fail("No quest reward offer after combat");
    }

    private void checkSpecialRoom(String type) throws Exception {
        int seat=active();
        assertTrue(consentAndCross().submit(clients[seat]).endsWith(" APPLIED"));
        applied++;
        refresh();
        assertEquals(2,new PublicDungeon(state(1)).depth);
        int initialHp=new PublicDungeon(state(1)).hp;
        int hostHp=new PublicDungeon(state(0)).hp;
        boolean charged=false;
        for (int n=0; n<30 && new PublicDungeon(state(1)).hp==initialHp; n++) {
            int actor=active();
            SeatActions.Pending wait=act("WAIT",0,0);
            PublicDungeon observer=new PublicDungeon(state(1));
            assertTrue(observer.monsters.containsValue(type));
            int cell=observer.monsters.entrySet().stream().filter(e->e.getValue().equals(type)).findFirst().get().getKey();
            assertTrue(observer.visible(cell));
            PublicDungeon far=new PublicDungeon(state(0));
            assertFalse(far.visible(cell));
            assertFalse(far.monsters.containsKey(cell));
            assertFalse(far.source.rows.containsKey("charge"+cell));
            if (!charged && observer.source.rows.containsKey("charge"+cell)) {
                verifyAfterReconnect(wait,actor);
                assertEquals("1",state(1).rows.get("charge"+cell));
                charged=true;
            }
        }
        assertEquals(type.equals("Sentry"),charged);
        assertTrue(new PublicDungeon(state(1)).hp<initialHp);
        assertEquals(hostHp,new PublicDungeon(state(0)).hp);
        reconnectPair();
        assertEquals(2,new PublicDungeon(state(1)).depth);
    }

    private void walk() throws Exception {
        int moves=0,hits=0,zaps=0,searches=0;
        int start=pos(0);
        act("DROP",start,item(0,"BAG","FOOD"));
        offer(0);
        act("PICK_UP",start,item(0,"F"+start,"FOOD"));
        offer(0);
        act("DROP",start,item(0,"BAG","FOOD"));
        for(int n=0;n<220;n++) {
            String prefix=state(0).rows.get("turn")+" "+active()+" ";
            String guide="";
            long end=System.nanoTime()+3000000000L;
            do {
                if(Files.exists(path.resolve("guide"))) guide=RestartHost.read(path,"guide");
                if(guide.startsWith(prefix)) break;
                Thread.sleep(5);
            } while(System.nanoTime()<end);
            assertTrue("Guide turn",guide.startsWith(prefix));
            String[] f=guide.split(" ");
            int seat=active(),cell=Integer.parseInt(f[3]);
            if(f[2].equals("STAIR")) break;
            if (seat==0 && zaps==0 && !new PublicDungeon(state(0)).monsters.isEmpty()) {
                for(int target:new PublicDungeon(state(0)).monsters.keySet()) {
                    SeatActions.Pending cast=actions[0].prepare(state(0),"ZAP",target,item(0,"WEAPON","STAFF"));
                    if(cast.submit(clients[0]).endsWith(" APPLIED")) {applied++; zaps++; refresh();break;}
                }
                if(!prefix.equals(state(0).rows.get("turn")+" "+active()+" ")) continue;
            }
            act(f[2],cell,0);
            if (f[2].equals("MOVE")) moves++;
            if (f[2].equals("ATTACK")) hits++;
            if (f[2].equals("SEARCH")) searches++;
            Thread.sleep(25);
        }
        assertTrue("Walked generated dungeon",moves>1 && zaps==1 && hits>0);
        Collections.sort(latency);
        System.out.println("native socket route action_ack_count="+latency.size()+" median_us="+latency.get(latency.size()/2)
                +" p95_us="+latency.get((95*latency.size()+99)/100-1));
        int seat=active();
        SeatActions.Pending pending=consentAndCross();
        RestartHost.write(path,"descent",seat+" "+pending.id+" "+pending.command);
        RestartHost.write(path,"kill-before-reply","1");
        assertThrows(IOException.class,()->pending.submit(clients[seat]));
        System.out.println("native socket route moves="+moves+" melee="+hits+" searches="+searches+" casts="+zaps);
    }

    private void afterStairs(String mode) throws Exception {
        int depth=mode.equals("verifyfloor")?1:2;
        for(int seat=0;seat<2;seat++) assertEquals(depth,new PublicDungeon(state(seat)).depth);
        String[] old=RestartHost.read(path,"descent").split(" ",3);
        int owner=Integer.parseInt(old[0]);
        String result=clients[owner].request("LOOKUP "+epoch+" "+old[1]);
        assertTrue(result,result.endsWith(" APPLIED"));
        assertEquals(result,clients[owner].request(old[2]));
        if(mode.equals("verifyfloor")) {
            String[] asc=RestartHost.read(path,"ascent").split(" ",2);
            assertTrue(clients[Integer.parseInt(asc[0])].request("LOOKUP "+epoch+" "+asc[1]).endsWith(" APPLIED"));
            act("WAIT",0,0);return;
        }
        if(mode.equals("backatstairs")) {
            String[] unknown=RestartHost.read(path,"incomplete").split(" ",2);
            assertEquals("UNKNOWN",clients[Integer.parseInt(unknown[0])].request("LOOKUP "+epoch+" "+unknown[1]));
        }
        int seat=active();
        SeatActions.Pending gate=consentAndCross();
        if(mode.equals("ascendfault")) {
            RestartHost.write(path,"incomplete",seat+" "+gate.id);
            RestartHost.write(path,"fault-armed","1");
            assertThrows(IOException.class,()->gate.submit(clients[seat]));
        } else {
            long start=System.nanoTime();
            assertTrue(gate.submit(clients[seat]).endsWith(" APPLIED"));applied++;
            RestartHost.write(path,"perf-travel","ascent_ack_us="+(System.nanoTime()-start)/1000);
            refresh();
            for(int s=0;s<2;s++) {
                assertEquals(1,new PublicDungeon(state(s)).depth);
                assertFalse("Full floor replacement",models[s].appliedDelta());
            }
            RestartHost.write(path,"ascent",seat+" "+gate.id);
            act("WAIT",0,0);
        }
    }

    private int charges(int seat) {
        return new PublicDungeon(state(seat)).belongings.stream().filter(i->i.kind.equals("STAFF")).findFirst().get().charges;
    }

    private int monsterHP(int seat,int cell) {
        for(Map.Entry<String,String> e:state(seat).rows.entrySet())
            if(e.getKey().matches("m[0-9]+") && e.getValue().startsWith(cell+",Rat,"))
                return Integer.parseInt(e.getValue().split(",")[2]);
        throw new AssertionError("Missing visible target");
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
