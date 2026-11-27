package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.TrinityDiscoveries;
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
        if (c.state(0)!=null && c.state(1)!=null)
            System.out.println("public_snapshot mode="+args[1]+" seated_tcp_bytes="+Arrays.stream(c.clients).filter(Objects::nonNull).mapToLong(LanClient::transferredBytes).sum()
                    +" seat0_bytes="+c.state(0).snapshotBytes()
                    +" seat1_bytes="+c.state(1).snapshotBytes());
    }

    private void measurePlayerAction(SeatActions.Pending action,int seat,String verb) throws IOException {
        long bytes=clients[seat].transferredBytes();
        long start=System.nanoTime();
        String receipt=action.submit(clients[seat]);
        long micros=(System.nanoTime()-start)/1000;
        assertTrue(receipt,receipt.endsWith(" APPLIED"));
        System.out.printf("player_action verb=%s seat=%d us=%d command_and_receipt_bytes=%d%n",
                verb,seat,micros,clients[seat].transferredBytes()-bytes);
        refresh();
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
        return act(name,cell,item,"APPLIED");
    }

    private SeatActions.Pending act(String name,int cell,long item,String expected) throws Exception {
        int seat=active();
        SeatActions.Pending p=actions[seat].prepare(state(seat),name,cell,item);
        long started=System.nanoTime();
        String reply=p.submit(clients[seat]);
        latency.add((System.nanoTime()-started)/1000);
        assertTrue(name+" "+reply,reply.endsWith(" "+expected));
        applied++;
        refresh();
        return p;
    }
    private void offer(int seat) throws Exception {
        for(int i=0;active()!=seat && i<12;i++) act("WAIT",0,0);
        assertEquals(seat,active());
    }

    private String disclosedWorld(int seat) {
        return disclosedWorld(seat, false);
    }

    private String disclosedWorld(int s, boolean schemaOneClient) {
        List<String> publicRows=new ArrayList<>();
        for(Map.Entry<String,String> e:state(s).rows.entrySet()) {
            String key=e.getKey();
            if (key.matches("t[0-9]+|r[0-9]+|m[0-9]+|z[0-9]+|b[0-9]+|self|ally|life|allylife|revivable|clock|turn"))
                publicRows.add(key+"="+e.getValue());
            if (key.matches("i[0-9]+")) {
                String row=e.getValue();
                if (schemaOneClient) {
                    // The old client displayed these same physical items with fewer action buttons.
                    String[] parts=row.split(",");
                    switch (parts[1]) {
                        case "BOW": case "ARMOR": case "HEALING": case "HONEYPOT": case "SHELL":
                        case "STONE": case "INSCRIBED": case "UPGRADE": case "IDENTIFY":
                            parts[1]="OTHER";
                            break;
                        case "STAFF":
                            parts[1]="MELEE";
                            break;
                        default: break;
                    }
                    row=String.join(",",Arrays.copyOf(parts,5));
                }
                publicRows.add("item="+row);
            }
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
        boolean special=RestartHost.plant(mode) || mode.equals("goo") || RestartHost.hazard(mode) || mode.equals("blocked-start") || RestartHost.guardian(mode) || mode.equals("crystal-new") || mode.equals("sentry") || mode.equals("piranha") || mode.equals("quest") || mode.equals("hive") || (mode.equals("mimic") || mode.equals("golden"));
        if (special) {
            try (LanClient old = new LanClient(address)) {
                assertEquals("ERROR VERSION",old.request("MODE SYNC13"));
                assertEquals("ERROR VERSION",old.request("MODE SYNC14"));
                assertEquals("ERROR VERSION",old.request("MODE SYNC15"));
                assertEquals("ERROR VERSION",old.request("MODE SYNC16"));
                assertEquals("ERROR VERSION", old.request("MODE SYNC3"));
                assertEquals("ERROR VERSION", old.request("MODE SYNC12"));
                assertEquals("ERROR VERSION", old.request("MODE SYNC33"));
            }
        }
        if (special || mode.startsWith("tengu-setup-") || mode.equals("late-setup") || mode.startsWith("necro-setup-") || mode.startsWith("wand-setup-") || mode.startsWith("scribe") || RestartHost.kit(mode) || RestartHost.pinProjectile(mode) || RestartHost.newCampaign(mode) || mode.equals("new") || mode.equals("walk") || mode.startsWith("cast")) {
            for(int i=0;i<2;i++) {
                assertTrue(clients[i].request(i==0 ? RestartHost.read(path,"endpoint").split("\n")[1] : "JOIN").startsWith("JOINED"));
                keys[i]=SeatCredential.generate();
                try(OutputStream out=Files.newOutputStream(path.resolve("private"+i))) { keys[i].writePrivate(out); }
                epoch=UUID.fromString(keys[i].enroll(clients[i]).split(" ")[1]);
                actions[i]=new SeatActions(session,i,i+1);
                models[i]=new DisplayReplica(session,i);
                String character=(mode.equals("quest") || RestartHost.guardian(mode) || mode.equals("crystal-new"))?"MAGE":mode.startsWith("cast") && !mode.startsWith("cast-hand") && i==mode.charAt(mode.length()-1)-'0' ? "MAGE" : mode.equals("walk") ? (i==0?"MAGE":"WARRIOR") : i==0?"WARRIOR":"HUNTRESS";
                if (mode.startsWith("tengu-setup-") || mode.equals("late-setup") || mode.startsWith("wand-setup-") || mode.startsWith("necro-setup-")) character=i==0?"WARRIOR":"MAGE";
                if (mode.startsWith("tengu-setup-growth-")) {
                    character=i==0?"WARRIOR":"HUNTRESS";
                    if (mode.matches(".*-(holylance|cleanse|trinitymindmissilehistory|recallid|readrecharge|recallrecharge|trinitymind|trinityspirit|trinitybodyhistory|trinitymindhistory|trinityspirithistory|trinityfirehistory|trinityblockhistory|trinitymindselect|trinityspiritselect|intuition|bless|trinityselect|trinity|ascendform|lightally|lightorder|radiance|smite|sunray|divinesense|lightshield|light|blessweapon|ward)[01]")) character="CLERIC";
                    if (mode.matches(".*-(monkflurry|monkdash|monkmeditate|monkdragonkick|strikeshock|strikeblock|strikevenom|strikekinetic|strikebloom|heroicenergy|challenge|elestrike|feint|swap|monkfocus|guardshield|stance|lunge|spike|charged)[01]")) character="DUELIST";
                    if (mode.matches(".*-(cacheinvest|cachepickup)[01]")) character=i==mode.charAt(mode.length()-1)-'0'?"ROGUE":"MAGE";
                    if (mode.matches(".*-(markdeath|clone|orderclone|cloak|ambush|freerun|smoke)[01]")) character="ROGUE";
                    if (mode.matches(".*-(holdfast|deathlesswait|strongman|endless_rage|sealdetach|sealattach|sealkeep|sealtransfer|rationthrow|leaptrap|doubleparry|parryready|lethal|enhanced|endure|third|shock|clobber|slam|crush|fury|parry|riposte|rage|leap|crown|armorright)[01]")) character="WARRIOR";
                    if (mode.matches(".*-(poisontip|scoutshot|point_blank|hawk|direct|blades|sniper|nature)[01]")) character="HUNTRESS";
                    if (mode.matches(".*-(desperate_power|spark|sparkimbue|sparkblast|sparkvolley|ice|iceimbue|iceblast|icevolley|imbue|volley|battery|recall|beacon|meal|blast|warlock|battlemage|soul)[01]")) character="MAGE";
                }
                if (RestartHost.newCampaign(mode)) character=i==0?"MAGE":"HUNTRESS";
                if (mode.startsWith("scribe") && i==mode.charAt(6)-'0') character="WARRIOR";
                if (RestartHost.kit(mode)) character=i==mode.charAt(3)-'0'?"HUNTRESS":"WARRIOR";
                assertEquals("OK",clients[i].request("SELECT "+character)); clients[i].request("READY 1");
            }
            if (mode.equals("blocked-start")) {
                assertEquals("ERROR UNSUPPORTED_FLOOR",clients[0].request("START"));
                assertTrue(clients[1].request("STATE").startsWith("LOBBY READY"));
                assertEquals("OK",clients[1].request("READY 0"));
                return;
            }
            assertEquals("GAME_STARTED",clients[0].request("START"));
            RestartHost.write(path,"epoch",epoch.toString());
            if (mode.startsWith("tengu-setup-") || mode.equals("late-setup") || mode.startsWith("wand-setup-") || mode.startsWith("necro-setup-")) {
                long until = System.nanoTime()+30000000000L;
                while (!Files.exists(path.resolve("return-ready")) && System.nanoTime()<until) Thread.sleep(10);
                assertTrue(Files.exists(path.resolve("return-ready")));
                return;
            }
            refresh();
            if(RestartHost.newCampaign(mode)) { campaign(mode); return; }
            if(mode.equals("walk")) { walk(); return; }
            act("WAIT",0,0);
            if (mode.startsWith("scribe")) {
                int seat=mode.charAt(6)-'0'; offer(seat);
                PublicDungeon view=new PublicDungeon(state(seat));
                PublicDungeon.Loot stones=view.belongings.stream().filter(i->i.kind.equals("STONE")).findFirst().get();
                assertEquals("THROW",PublicDungeon.shortcutAction(stones));
                int number=stones.quantity;
                verifyAfterReconnect(act("THROW",view.pos+3,stones.id),seat);
                offer(seat); view=new PublicDungeon(state(seat));
                assertTrue(view.belongings.stream().anyMatch(i->i.kind.equals("STONE") && i.quantity==number-1));
                PublicDungeon.Loot target=view.belongings.stream().filter(i->i.kind.equals("ARMOR") && i.slot.equals("BAG")).findFirst().get();
                assertEquals("NA",target.upgrade);
                int select=view.scrollTarget(target,"IDENTIFY");
                assertTrue(select>0);
                verifyAfterReconnect(act("IDENTIFY",select,item(seat,"BAG","IDENTIFY")),seat);
                offer(seat); view=new PublicDungeon(state(seat));
                target=view.belongings.stream().filter(i->i.kind.equals("ARMOR") && i.slot.equals("BAG")).findFirst().get();
                assertEquals("0",target.upgrade); assertEquals(0,view.scrollTarget(target,"IDENTIFY"));
                long handle=item(seat,"BAG","UPGRADE");
                SeatActions.Pending bad=actions[seat].prepare(state(seat),"UPGRADE",0,handle);
                assertTrue(bad.submit(clients[seat]).endsWith(" REJECT_INVALID"));
                SeatActions.Pending read=actions[seat].prepare(state(seat),"UPGRADE",view.scrollTarget(target,"UPGRADE"),handle);
                RestartHost.write(path,"scroll-action",seat+" "+read.id+" "+read.command);
                RestartHost.write(path,"kill-before-reply","1");
                assertThrows(IOException.class,()->read.submit(clients[seat]));
                return;
            }
            if (RestartHost.pinProjectile(mode)) {
                int seat=mode.charAt(mode.length()-1)-'0';
                int cell=Integer.parseInt(RestartHost.read(path,"pin-target").split(" ")[1]);
                for (int n=0;n<7;n++) {
                    offer(seat);
                    String kind=mode.startsWith("dart") ? "DART" : "KNIFE";
                    long handle=item(seat,"BAG",kind); assertTrue(handle>0);
                    int count=new PublicDungeon(state(seat)).belongings.stream()
                            .filter(i -> i.id==handle).findFirst().get().quantity;
                    RestartHost.write(path,"pin-stack",kind+" "+(count-1));
                    SeatActions.Pending shot=actions[seat].prepare(state(seat),"THROW",cell,handle);
                    if (Boolean.getBoolean("spd.timePlayerAction")) {
                        measurePlayerAction(shot,seat,"THROW");
                        return;
                    }
                    RestartHost.write(path,"pin-pending",seat+" "+shot.id+" "+shot.command);
                    try { String result=shot.submit(clients[seat]); assertTrue(result,result.endsWith(" APPLIED")); }
                    catch (IOException lost) { return; }
                    refresh();
                }
                fail("No native projectile lodged in the target");
            }
            if (RestartHost.kit(mode)) {
                int seat=mode.charAt(3)-'0'; offer(seat);
                act("PICK_UP",pos(seat),item(seat,"F"+pos(seat),"HEALING"));
                offer(seat); int before=new PublicDungeon(state(seat)).hp;
                verifyAfterReconnect(act("DRINK",0,item(seat,"BAG","HEALING")),seat);
                assertTrue(new PublicDungeon(state(seat)).hp>=before);
                offer(seat);
                int target=pos(seat)+3;
                SeatActions.Pending shot=actions[seat].prepare(state(seat),"SHOOT",target,item(seat,"BAG","BOW"));
                RestartHost.write(path,"bow-action",seat+" "+shot.id+" "+shot.command);
                RestartHost.write(path,"kill-before-reply","1");
                assertThrows(IOException.class,()->shot.submit(clients[seat]));
                return;
            }
            if (mode.equals("goo")) { boss(); return; }
            if (RestartHost.plant(mode)) {
                String[] fixture=RestartHost.read(path,"landing").split(" ");
                int seat=Integer.parseInt(fixture[1]), cell=Integer.parseInt(fixture[2]);
                offer(seat);
                assertEquals(Integer.parseInt(fixture[3]),new PublicDungeon(state(seat)).plant(cell));
                assertFalse(state(1-seat).rows.containsKey("plant"+cell));
                SeatActions.Pending move=actions[seat].prepare(state(seat),"MOVE",cell,0);
                RestartHost.write(path,"landing-action",move.id+" "+move.command);
                RestartHost.write(path,"kill-before-reply","1");
                assertThrows(IOException.class,()->move.submit(clients[seat]));
                return;
            }
            if (RestartHost.hazard(mode)) {
                offer(1);
                int cell=Integer.parseInt(RestartHost.read(path,"hazard-cell"));
                boolean dart=!mode.equals("iron-door");
                if (dart) assertTrue(state(1).rows.containsKey("trap"+cell));
                else {
                    assertEquals("0",state(1).rows.get("keys"));
                    verifyAfterReconnect(act("PICK_UP",pos(1),item(1,"F"+pos(1),"OTHER")),1);
                    assertEquals("1",state(0).rows.get("keys"));
                    offer(1);
                }
                SeatActions.Pending pending=actions[1].prepare(state(1),dart?"MOVE":"UNLOCK",cell,0);
                RestartHost.write(path,"hazard-receipt",pending.id+" "+new PublicDungeon(state(1)).hp+" "+pending.command);
                RestartHost.write(path,"kill-before-reply","1");
                assertThrows(IOException.class,()->pending.submit(clients[1]));
                return;
            }
            if (RestartHost.guardian(mode)) {
                int seat=active();
                assertTrue(consentAndCross().submit(clients[seat]).endsWith(" APPLIED")); applied++;
                refresh(); act("WAIT",0,0); offer(1);
                String key="m"+RestartHost.read(path,"resident-id");
                String[] mob=state(1).rows.get(key).split(",");
                assertEquals(mode.equals("armored") ? "ArmoredStatue" : (mode.equals("statue") || mode.equals("unstable")) ? "Statue" : mode.equals("exile") ? "GnollExile" : "PhantomPiranha",mob[1]);
                verifyAfterReconnect(act("ZAP",Integer.parseInt(mob[0]),item(1,"WEAPON","STAFF")),1);
                assertTrue(Integer.parseInt(state(1).rows.get(key).split(",")[2])<Integer.parseInt(mob[2]));
                if (mode.equals("unstable") || mode.equals("armored")) {
                    for (int i=0;i<40;i++) {
                        int owner=active();
                        SeatActions.Pending wait=actions[owner].prepare(state(owner),"WAIT",0,0);
                        RestartHost.write(path,"last-strike",owner+" "+wait.id+" "+wait.command);
                        RestartHost.write(path,"watch-hit",Integer.toString(new PublicDungeon(state(1)).hp));
                        try { assertTrue(wait.submit(clients[owner]).endsWith(" APPLIED")); applied++; }
                        catch (IOException lost) { return; }
                        refresh();
                    }
                    throw new AssertionError("No resolved hostile strike");
                }
                assertFalse(state(1).rows.toString().contains("Kinetic"));
                return;
            }
            if (mode.equals("crystal-new")) {
                int seat=active();
                assertTrue(consentAndCross().submit(clients[seat]).endsWith(" APPLIED")); applied++;
                refresh();
                act("WAIT",0,0);
                offer(1);
                assertTrue(new PublicDungeon(state(1)).chest(Integer.parseInt(RestartHost.read(path,"encounter-cell"))));
                return;
            }
            if ((mode.equals("mimic") || mode.equals("golden"))) { openChest(mode.equals("golden")); return; }
            if (mode.equals("quest")) { ghostQuest(); return; }
            if (mode.equals("hive")) { hive(); return; }
            if (special) {
                checkSpecialRoom(mode.equals("sentry")?"Sentry":"Piranha");
                return;
            }
            if(mode.startsWith("cast")) {
                int seat=mode.charAt(mode.length()-1)-'0';
                String slot=mode.startsWith("cast-hand") ? "BAG" : "WEAPON";
                String kind=mode.startsWith("cast-hand") ? "WAND" : "STAFF";
                offer(seat);
                PublicDungeon view=new PublicDungeon(state(seat));
                long staff=item(seat,slot,kind);
                assertTrue(staff>0);
                assertEquals(0,item(1-seat,slot,kind));
                int cell=view.pos+3;
                assertEquals("Rat",view.monsters.get(cell));
                int charge=view.belongings.stream().filter(i->i.id==staff).findFirst().get().charges;
                PublicDungeon.Loot wand=view.belongings.stream().filter(i->i.id==staff).findFirst().get();
                SeatQuickslots layout=SeatQuickslots.empty().bind(3,wand);
                try (DataOutputStream out=new DataOutputStream(Files.newOutputStream(path.resolve("user-layout")))) { layout.write(out); }
                if (mode.startsWith("cast-hand-blast") || mode.startsWith("cast-hand-fire")) {
                    SeatActions.Pending pending = actions[seat].prepare(state(seat),"ZAP",cell,staff);
                    RestartHost.write(path,"blast-record",pending.id+" "+charge+" "+pending.command);
                    RestartHost.write(path,"kill-before-reply","1");
                    assertThrows(IOException.class, () -> pending.submit(clients[seat]));
                    return;
                }
                verifyAfterReconnect(act("ZAP",cell,staff),seat);
                assertEquals(charge-1,charges(seat));
                offer(seat);
                long newStaff=item(seat,slot,kind);
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
        if (mode.equals("backup") || mode.equals("floorbackup") || mode.equals("schema3-backup") || mode.equals("prison-backup")) {
            String warning=keys[0].reconnect(clients[0],epoch,0,1);
            assertTrue(warning,warning.startsWith("RECOVERY_EPOCH "));
            epoch=UUID.fromString(warning.split(" ")[1]);
        }
        for(int i=0;i<2;i++) bind(i);
        barrier();
        if (mode.equals("smith-order-race")) {
            int first=active(),second=1-first;
            assertEquals("3000",state(first).rows.get("favor"));
            SeatActions.Pending delayed=actions[first].prepare(state(first),"SMITH_ORDER",0,0);
            offer(second);
            SeatActions.Pending current=actions[second].prepare(state(second),"SMITH_ORDER",0,0);
            java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Future<String> old=workers.submit(()->{start.await();return delayed.submit(clients[first]);});
                java.util.concurrent.Future<String> fresh=workers.submit(()->{start.await();return current.submit(clients[second]);});
                start.countDown();
                assertFalse(old.get(5,java.util.concurrent.TimeUnit.SECONDS).endsWith(" APPLIED"));
                String receipt=fresh.get(5,java.util.concurrent.TimeUnit.SECONDS);
                assertTrue(receipt,receipt.endsWith(" APPLIED"));
                assertEquals(receipt,current.submit(clients[second]));
                refresh();
                for (int seat=0;seat<2;seat++) {
                    assertEquals("1000",state(seat).rows.get("favor"));
                    assertNotNull(state(seat).rows.get("smithoffer0"));
                }
                System.out.println("PASS concurrent authenticated orders charged exactly 2000 favor and returned one durable offer");
            } finally {workers.shutdownNow();}
            return;
        }
        if (mode.equals("measure-mine-crossing")) {
            int mover=active();
            PublicDungeon origin=new PublicDungeon(state(mover));
            long begin=System.nanoTime();
            SeatActions.Pending crossing=consentAndCross();
            assertTrue(crossing.submit(clients[mover]).endsWith(" APPLIED"));
            refresh();
            long micros=(System.nanoTime()-begin)/1000;
            for (int seat=0;seat<2;seat++) {
                PublicDungeon arrived=new PublicDungeon(state(seat));
                assertEquals(origin.depth,arrived.depth);
                assertEquals(1-origin.branch,arrived.branch);
            }
            assertNotEquals(pos(0),pos(1));
            RestartHost.write(path,"crossing-micros",Long.toString(micros));
            return;
        }
        if (mode.equals("legacy-prison")) {
            for (int i=0;i<2;i++) {
                PublicDungeon view=new PublicDungeon(state(i)); assertEquals(6,view.depth);
                assertEquals(RestartHost.read(path,"arrived"+i).split(" ")[0],state(i).rows.get("self").split(",")[0]);
            }
            return;
        }
        if (mode.startsWith("late-down") || mode.startsWith("late-up")) {
            int from=Integer.parseInt(mode.replaceFirst("^late-(down|up)", ""));
            assertEquals(from,new PublicDungeon(state(0)).depth);
            if (mode.equals("late-up9")) {
                String[] prior=RestartHost.read(path,"ninth-crossing").split(" ",3);
                int seat=Integer.parseInt(prior[0]);
                long before=state(seat).version;
                String receipt=clients[seat].request("LOOKUP "+epoch+" "+prior[1]);
                assertTrue(receipt.endsWith(" APPLIED"));
                assertEquals(receipt,clients[seat].request(prior[2]));
                refresh();
                assertEquals(before,state(seat).version);
            }
            int mover=active();
            SeatActions.Pending crossing=consentAndCross();
            if (mode.equals("late-down8")) {
                RestartHost.write(path,"ninth-crossing",mover+" "+crossing.id+" "+crossing.command);
                RestartHost.write(path,"kill-before-reply","1");
                assertThrows(IOException.class,()->crossing.submit(clients[mover]));
                return;
            }
            long start=System.nanoTime();
            String result=crossing.submit(clients[mover]);
            assertTrue(result,result.endsWith(" APPLIED"));
            refresh();
            int destination=from+(mode.startsWith("late-up") ? -1 : 1);
            for(int seat=0;seat<2;seat++) assertEquals(destination,new PublicDungeon(state(seat)).depth);
            assertNotEquals(pos(0),pos(1));
            System.out.println("staged_floor_tcp "+from+"->"+destination+" travel_and_snapshots_us="+(System.nanoTime()-start)/1000);
            act("WAIT",0,0);
            return;
        }
        if (mode.startsWith("tengu-commit-")) {
            String boundary=mode.substring("tengu-commit-".length());
            boolean travel=boundary.equals("ten") || boundary.equals("caves") || boundary.equals("return");
            String verb=boundary.equals("revive") ? "REVIVE" : boundary.equals("down") || boundary.equals("devices") ? "WAIT" : boundary.equals("claim") ? "PICK_UP"
                    : boundary.equals("enter") || boundary.equals("arena") ? "MOVE" : "ATTACK";
            boolean growth=boundary.startsWith("growth-");
            if (growth) verb=RestartHost.read(path,"growth-action");
            boolean interior=boundary.startsWith("interior-");
            boolean dm=boundary.startsWith("dm-");
            boolean mine=boundary.startsWith("mine-");
            boolean vault=boundary.startsWith("vault-");
            if (vault) travel=true;
            boolean ascent=boundary.startsWith("ascent-");
            if (ascent) { verb=RestartHost.read(path,"ascent-action"); travel=verb.equals("TRAVEL"); }
            boolean elemental=boundary.startsWith("elemental-");
            if (elemental) { verb=RestartHost.read(path,"elemental-action"); travel=verb.equals("VAULT_REWARD"); }
            boolean yog=boundary.startsWith("yog-");
            if (yog) { verb=RestartHost.read(path,"yog-action");travel=verb.equals("TRAVEL"); }
            boolean king=boundary.startsWith("king-");
            if (king) { verb=RestartHost.read(path,"king-action");travel=verb.equals("TRAVEL"); }
            boolean city=boundary.startsWith("city-");
            if (city) {
                travel=!boundary.endsWith("imp");
                verb="ACCEPT_IMP";
            }
            boolean equipmentOrBoss=mine && (boundary.endsWith("boss") || boundary.endsWith("upgrade") || boundary.endsWith("reforge"));
            if (mine) {
                travel=boundary.endsWith("enter") || boundary.endsWith("return");
                verb=boundary.endsWith("order")?"SMITH_ORDER":boundary.endsWith("claim")?"SMITH_CLAIM":"MINE";
            }
            if (equipmentOrBoss) verb=RestartHost.read(path,"mine-action");
            if (dm) {
                String name=boundary.substring(3);
                travel=name.equals("visit") || name.equals("return") || name.equals("revisit");
                verb=name.equals("enter")?"MOVE":name.equals("reward")?"PICK_UP":name.equals("revive")?"REVIVE"
                        :name.equals("down") || name.equals("devices")?"WAIT":"ATTACK";
            }
            if (interior) {
                String name=boundary.substring(9);
                travel=Character.isDigit(name.charAt(0));
                verb=name.equals("smith")?"ACCEPT_BLACKSMITH":"WAIT";
            }
            int cell=Integer.parseInt(RestartHost.read(path,"tengu-target"));
            if (growth && boundary.matches("growth-trinity(body|fire|mind|spirit)history[01]")) {
                int owner = boundary.charAt(boundary.length()-1) - '0';
                TrinityDiscoveries.Choice choice = Arrays.stream(TrinityDiscoveries.Choice.values())
                        .filter(candidate -> candidate.target == cell).findFirst().orElseThrow(AssertionError::new);
                assertTrue(state(owner).rows.containsKey(choice.rowKey()));
                assertFalse(state(1-owner).rows.containsKey(choice.rowKey()));
            }
            for (int i=0;i<40;i++) {
                int seat=active();
                if (ascent && verb.equals("START_ASCENT")) {
                    int owner=Integer.parseInt(state(0).rows.get("amuletowner"));
                    seat=boundary.equals("ascent-offer") ? owner : 1-owner;
                }
                if (yog && verb.equals("END_RUN")) {
                    int owner=Integer.parseInt(state(0).rows.get("amuletowner"));
                    seat=boundary.equals("yog-win")?1-owner:owner;
                }
                PublicDungeon view=new PublicDungeon(state(seat));
                if (growth && boundary.startsWith("growth-cachepickup")) assertEquals(2,view.depth);
                else if (growth) assertEquals(boundary.matches("growth-(owner|grant|right)[01]") ? 10 : 1,view.depth);
                else if (ascent) assertEquals(0,view.branch);
                else if (elemental) { assertEquals(18,view.depth); assertEquals(1,view.branch); }
                else if (yog) assertTrue(view.depth==25 || view.depth==26);
                else if (vault) assertEquals(boundary.matches("vault-(return|pick|full)") ? 1 : 0,view.branch);
                else if (king) assertEquals(boundary.endsWith("visit") ? 19 : 20,view.depth);
                else if (city) assertEquals(travel ? Integer.parseInt(boundary.substring(5))-1 : 18,view.depth);
                else if (mine) assertEquals(boundary.endsWith("enter") || boundary.endsWith("claim") || boundary.endsWith("order") || boundary.endsWith("upgrade") || boundary.endsWith("reforge") ? 0 : 1,view.branch);
                else if (dm) assertEquals(boundary.equals("dm-visit") || boundary.equals("dm-revisit") ? 14 : 15,view.depth);
                else if (interior) assertEquals(travel ? Integer.parseInt(boundary.substring(9))-1 : boundary.contains("web")?12:13,view.depth);
                else assertEquals(boundary.equals("ten") ? 9 : boundary.equals("return") ? 11 : 10,view.depth);
                if (boundary.equals("claim")) {
                    assertEquals("collector stands on the mask", cell, view.pos);
                    assertEquals(com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.MASK, view.floor.get(cell).sprite);
                }
                long handle=verb.equals("PICK_UP") ? view.floor.get(cell).id : 0;
                if (growth && verb.equals("ZAP")) handle=item(seat,"WEAPON","STAFF");
                if (growth && verb.equals("THROW")) handle=boundary.contains("rationthrow")
                        ? view.belongings.stream().filter(it -> it.kind.equals("FOOD") && view.actions(it).contains("THROW")).findFirst().get().id
                        : item(seat,"BAG",boundary.contains("poisontip") ? "DART" : "SPIKE");
                if (verb.equals("DETACH_SEAL") || verb.equals("AFFIX_SEAL") || verb.equals("KEEP_ARMOR_GLYPH") || verb.equals("TRANSFER_SEAL_GLYPH")) {
                    String selected=verb.equals("DETACH_SEAL") ? verb : "AFFIX_SEAL";
                    handle=view.belongings.stream().filter(it -> view.actions(it).contains(selected)).findFirst().get().id;
                }
                if (verb.equals("SHOOT")) handle=item(seat,"BAG","BOW");
                if (verb.matches("HOLY_LANCE|CLEANSE|RECALL_INSCRIPTION|HOLY_INTUITION|BODY_FORM|MIND_FORM|SPIRIT_FORM|BLESS|RADIANCE|SMITE|SUNRAY|DIVINE_SENSE|SHIELD_OF_LIGHT|GUIDING_LIGHT|HOLY_WARD|HOLY_WEAPON")) handle=item(seat,"ARTIFACT","TOME");
                if (verb.equals("READ_RECHARGING")) handle=view.belongings.stream().filter(it -> view.actions(it).contains("READ_RECHARGING")).findFirst().get().id;
                if (verb.equals("TRINITY_MIND") || verb.equals("TRINITY_SPIRIT")) handle=view.belongings.stream().filter(it -> view.actions(it).contains("ARMOR_ABILITY")).findFirst().get().id;
                if (verb.equals("WEAPON_ABILITY") || verb.equals("IMBUE") || verb.equals("CLOAK") || verb.equals("USE_CROWN") || verb.equals("ARMOR_ABILITY") || verb.equals("USE_MASK")) { String selected=verb; handle=view.belongings.stream().filter(it -> view.actions(it).contains(selected)).findFirst().get().id; }
                if (verb.equals("VAULT_RECALL")) {
                    handle=view.belongings.stream().filter(item -> view.actions(item).contains("VAULT_RECALL"))
                            .findFirst().orElseThrow(AssertionError::new).id;
                }
                if(equipmentOrBoss && !boundary.endsWith("boss")) {
                    String index=RestartHost.read(path,"mine-index")+",";
                    handle=view.belongings.stream().filter(item->view.source.rows.getOrDefault("forge"+item.id,"").startsWith(index)).findFirst().get().id;
                }
                SeatActions.Pending p=travel ? (boundary.equals("elemental-escape") ? consentAndCross(true,true) : consentAndCross(boundary.equals("vault-pick") || boundary.equals("vault-full"))) : actions[seat].prepare(state(seat),verb,verb.equals("WAIT") ? 0 : cell,handle);
                if (yog) System.out.println("native_fixture "+boundary+" verb="+verb+" input="+cell+" seat="+seat+" depth="+view.depth+" visible="+view.visible(cell)+" actor="+state(seat).rows.get("self"));
                if (growth && Boolean.getBoolean("spd.timePlayerAction")) {
                    measurePlayerAction(p,seat,verb);
                    return;
                }
                RestartHost.write(path,"boss-command",seat+" "+p.id+" "+p.command);
                try { String result=p.submit(clients[seat]); assertTrue(result,result.endsWith(" APPLIED")); }
                catch (IOException lost) { return; }
                refresh();
            }
            fail("Boss boundary did not publish");
        }
        if (mode.equals("necro-summon")) {
            for(int i=0;i<8;i++) {
                int seat=active();
                SeatActions.Pending action=actions[seat].prepare(state(seat),"WAIT",0,0);
                RestartHost.write(path,"summon-request",seat+" "+action.id+" "+action.command);
                try { assertTrue(action.submit(clients[seat]).endsWith(" APPLIED")); }
                catch(IOException dropped) { return; }
                refresh();
            }
            fail("Expected lost reply at native summon publication");
        }
        if (mode.equals("necro-receipt") || mode.startsWith("tengu-read-")) {
            String[] prior=RestartHost.read(path,mode.startsWith("tengu-read-") ? "boss-command" : "summon-request").split(" ",3);
            int seat=Integer.parseInt(prior[0]);
            long version=state(seat).version;
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+prior[1]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(prior[2]));
            refresh();
            assertEquals(version,state(seat).version);
            if (mode.equals("tengu-read-yog-win") || mode.equals("tengu-read-ascent-surface")) {
                for (int i=0;i<2;i++) assertEquals("WON",state(i).rows.get("status"));
                String[] closed=prior[2].split(" ");
                closed[4]=Long.toString(Long.parseLong(prior[1])+1);
                closed[5]=Long.toString(version);
                closed[6]=state(seat).rows.get("turn");
                closed[7]="WAIT";
                String rejected=clients[seat].request(String.join(" ",closed));
                assertTrue(rejected,rejected.endsWith(" REJECT_ENDING"));
                return;
            }
            if (mode.equals("tengu-read-ascent-offer") || mode.equals("tengu-read-ascent-agree")) {
                int owner=Integer.parseInt(state(0).rows.get("amuletowner"));
                assertTrue(actions[owner].prepare(state(owner),"CONTINUE_RUN",0,0).submit(clients[owner]).endsWith(" APPLIED"));
                return;
            }
            if (mode.equals("tengu-read-yog-amulet") || mode.equals("tengu-read-yog-mine-return") || mode.equals("tengu-read-yog-branch-capacity") || mode.equals("tengu-read-yog-propose")) {
                int owner=Integer.parseInt(state(0).rows.get("amuletowner"));
                int voter=mode.endsWith("propose") ? 1-owner : owner;
                assertTrue(actions[voter].prepare(state(voter),"CONTINUE_RUN",0,0).submit(clients[voter]).endsWith(" APPLIED"));
                refresh();
            }
            if (RestartHost.measureBoundary(mode) || mode.equals("tengu-read-dm-devices")) {
                if (Boolean.getBoolean("spd.measureElemental") && RestartHost.measureBoundary(mode)) {
                    // Do not ask the owner for a new snapshot while it times 21 synchronous saves.
                    RestartHost.write(path,"measure-ready","1");
                    String marker=mode.equals("tengu-read-ascent-24")?"perf24":"perf18";
                    long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
                    while (!Files.exists(path.resolve(marker)) && System.nanoTime()<until) Thread.sleep(10);
                    assertTrue("durable save measurements published",Files.exists(path.resolve(marker)));
                }
                for (int i=0;i<21;i++) act("WAIT",0,0);
                Collections.sort(latency);
                System.out.println("tengu_tcp mode="+mode+" restored_arena_waits="+latency.size()+" median_us="+latency.get(10)+" p95_us="+latency.get(19));
            } else act("WAIT",0,0);
            return;
        }
        if (mode.equals("seven-travel")) {
            int seat=active();
            SeatActions.Pending move=consentAndCross();
            RestartHost.write(path,"travel-seven",seat+" "+move.id+" "+move.command);
            RestartHost.write(path,"kill-before-reply","1");
            assertThrows(IOException.class,()->move.submit(clients[seat]));
            return;
        }
        if (mode.equals("seven-resume")) {
            String[] old=RestartHost.read(path,"travel-seven").split(" ",3);
            int seat=Integer.parseInt(old[0]);
            for(int i=0;i<2;i++) {
                PublicDungeon view=new PublicDungeon(state(i)); assertEquals(7,view.depth);
                assertEquals(RestartHost.read(path,"arrived"+i).split(" ")[0],view.source.rows.get("self").split(",")[0]);
                assertEquals(RestartHost.read(path,"arrived"+i).split(" ")[3],
                        view.belongings.stream().filter(x->x.slot.equals("WEAPON")).findFirst().get().reference);
            }
            assertNotEquals(pos(0),pos(1));
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+old[1]);
            assertTrue(receipt,receipt.endsWith(" APPLIED")); assertEquals(receipt,clients[seat].request(old[2]));
            act("WAIT",0,0);
            int active=active();
            String back=consentAndCross().submit(clients[active]); assertTrue(back,back.endsWith(" APPLIED"));
            refresh(); assertEquals(6,new PublicDungeon(state(0)).depth); return;
        }
        if (mode.equals("seven-return")) {
            assertEquals(6,new PublicDungeon(state(1)).depth); act("WAIT",0,0); return;
        }
        if (mode.startsWith("market-")) { trade(mode); return; }
        if (mode.equals("legacy-sewer")) {
            for (int seat=0; seat<2; seat++) {
                PublicDungeon view=new PublicDungeon(state(seat));
                assertEquals(3,view.depth);
                String id=RestartHost.read(path,"last-seat-"+seat);
                assertTrue(clients[seat].request("LOOKUP "+epoch+" "+id).endsWith(" APPLIED"));
            }
            act("WAIT",0,0);
            return;
        }
        if (mode.equals("campaign-resume")) { campaign(mode); return; }
        if (mode.equals("schema3-backup") || mode.equals("prison-backup")) {
            int expectedDepth=mode.equals("prison-backup") ? 5 : 3;
            assertEquals(expectedDepth,new PublicDungeon(state(0)).depth);
            assertEquals(expectedDepth,new PublicDungeon(state(1)).depth);
            assertNotEquals(RestartHost.read(path,"epoch"),epoch.toString());
            assertEquals("ERROR EPOCH",clients[0].request("LOOKUP "+RestartHost.read(path,"epoch")+" 1"));
            act("WAIT",0,0);
            return;
        }
        if (mode.equals("plant-receipt")) {
            String[] fixture=RestartHost.read(path,"landing").split(" ");
            RestartHost.Landing landing=RestartHost.Landing.valueOf(fixture[0]);
            int seat=Integer.parseInt(fixture[1]), cell=Integer.parseInt(fixture[2]);
            String[] pending=RestartHost.read(path,"landing-action").split(" ",2);
            PublicDungeon owner=new PublicDungeon(state(seat));
            assertEquals(cell,owner.pos);
            assertEquals(-1,owner.plant(cell));
            if (landing==RestartHost.Landing.FIREBLOOM) {
                assertNotNull(state(seat).rows.get("cloud"+cell));
                assertFalse(state(1-seat).rows.containsKey("cloud"+cell));
            } else {
                assertEquals(landing!=RestartHost.Landing.MAGEROYAL,owner.buffs.contains(landing.icon));
            }
            assertFalse(new PublicDungeon(state(1-seat)).buffs.contains(landing.icon));
            long before=state(seat).version;
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+pending[0]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(pending[1]));
            refresh();
            assertEquals(before,state(seat).version);
            act("WAIT",0,0);
            return;
        }
        if (mode.startsWith("wand-claim-")) {
            int seat=Integer.parseInt(mode.substring("wand-claim-".length()));
            offer(seat);
            PublicDungeon view=new PublicDungeon(state(seat));
            int cell=view.monsters.entrySet().stream().filter(e -> e.getValue().equals("Wandmaker")).findFirst().get().getKey();
            act("ACCEPT_WANDMAKER",cell,0);
            offer(seat);
            String publicOffer=state(seat).rows.get("wandquest"+cell);
            assertTrue(publicOffer,publicOffer.contains(",REWARD,"));
            assertTrue(state(1-seat).rows.get("wandquest"+cell).endsWith(",HUNT"));
            SeatActions.Pending p=actions[seat].prepare(state(seat),seat==0?"WAND_FIRST":"WAND_SECOND",cell,0);
            RestartHost.write(path,"wand-pending",seat+" "+p.id+" "+p.command);
            RestartHost.write(path,"kill-before-reply","1");
            assertThrows(IOException.class,()->p.submit(clients[seat]));
            return;
        }
        if (mode.equals("wand-receipt")) {
            String[] old=RestartHost.read(path,"wand-pending").split(" ",3);
            int seat=Integer.parseInt(old[0]); long version=state(seat).version;
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+old[1]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(old[2]));
            refresh();
            assertEquals(version,state(seat).version);
            assertFalse(new PublicDungeon(state(seat)).monsters.containsValue("Wandmaker"));
            act("WAIT",0,0);
            return;
        }
        if (mode.equals("pin-receipt")) {
            String[] old=RestartHost.read(path,"pin-pending").split(" ",3);
            int seat=Integer.parseInt(old[0]); long version=state(seat).version;
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+old[1]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(old[2])); refresh();
            assertEquals(version,state(seat).version);
            String[] expected=RestartHost.read(path,"pin-stack").split(" ");
            int remaining=new PublicDungeon(state(seat)).belongings.stream()
                    .filter(i -> i.kind.equals(expected[0])).mapToInt(i -> i.quantity).sum();
            assertEquals(Integer.parseInt(expected[1]),remaining);
            act("WAIT",0,0); return;
        }
        if (mode.equals("kit-receipt")) {
            String[] old=RestartHost.read(path,"bow-action").split(" ",3);
            int seat=Integer.parseInt(old[0]); long version=state(seat).version;
            long id=item(seat,"BAG","HEALING"); assertTrue(id>0);
            assertEquals(1,new PublicDungeon(state(seat)).belongings.stream().filter(i->i.id==id).findFirst().get().quantity);
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+old[1]);
            assertTrue(receipt,receipt.endsWith(" APPLIED")); assertEquals(receipt,clients[seat].request(old[2]));
            refresh(); assertEquals(version,state(seat).version); act("WAIT",0,0);
            return;
        }
        if (mode.equals("prison-resume")) {
            for (int i=0;i<2;i++) {
                PublicDungeon view=new PublicDungeon(state(i));
                assertEquals(6,view.depth);
                String old=RestartHost.read(path,"arrived"+i);
                PublicDungeon.Loot equipped=view.belongings.stream().filter(x->x.slot.equals("WEAPON")).findFirst().get();
                assertEquals(old,state(i).rows.get("self").split(",")[0]+" "+view.pos+" "+view.hp+" "+equipped.reference);
            }
            int seat=active();
            assertTrue(consentAndCross().submit(clients[seat]).endsWith(" APPLIED")); refresh();
            assertEquals(5,new PublicDungeon(state(0)).depth); act("WAIT",0,0);
            return;
        }
        if (mode.equals("readscroll")) {
            String[] old=RestartHost.read(path,"scroll-action").split(" ",3);
            int seat=Integer.parseInt(old[0]);
            long version=state(seat).version;
            String result=clients[seat].request("LOOKUP "+epoch+" "+old[1]);
            assertTrue(result,result.endsWith(" APPLIED")); assertEquals(result,clients[seat].request(old[2]));
            refresh(); assertEquals(version,state(seat).version);
            PublicDungeon view=new PublicDungeon(state(seat));
            assertTrue(view.belongings.stream().anyMatch(i->i.kind.equals("ARMOR") && i.slot.equals("BAG") && i.upgrade.equals("1")));
            assertFalse(view.belongings.stream().anyMatch(i->i.kind.equals("UPGRADE") || i.kind.equals("IDENTIFY")));
            assertFalse(new PublicDungeon(state(1-seat)).belongings.stream().anyMatch(i->i.kind.equals("ARMOR") && i.upgrade.equals("1")));
            act("WAIT",0,0);
            return;
        }
        if (mode.equals("schema2-to-three")) {
            assertEquals(2,new PublicDungeon(state(0)).depth);
            String[] descent=RestartHost.read(path,"descent").split(" ",3);
            int mover=Integer.parseInt(descent[0]);
            String prior=clients[mover].request("LOOKUP "+epoch+" "+descent[1]);
            assertTrue(prior,prior.endsWith(" APPLIED"));
            assertEquals(prior,clients[mover].request(descent[2]));
            act("WAIT",0,0);
            int seat=active();
            assertTrue(consentAndCross().submit(clients[seat]).endsWith(" APPLIED"));
            refresh(); assertEquals(3,new PublicDungeon(state(0)).depth);
            act("WAIT",0,0);
            return;
        }
        if (mode.equals("goo-receipt")) {
            String[] saved=RestartHost.read(path,"boss-action").split(" ",3);
            int seat=Integer.parseInt(saved[0]);
            assertEquals(5,new PublicDungeon(state(seat)).depth);
            long version=state(seat).version;
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+saved[1]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(saved[2]));
            refresh(); assertEquals(version,state(seat).version);
            act("WAIT",0,0);
            return;
        }
        if (mode.startsWith("crystal-")) {
            crystalResume(mode);
            return;
        }
        if (mode.equals("unstable-receipt") || mode.equals("armored-receipt")) {
            String[] saved=RestartHost.read(path,"last-strike").split(" ",3);
            String[] expected=RestartHost.read(path,"committed-hit").split(" ");
            int owner=Integer.parseInt(saved[0]);
            assertEquals(Integer.parseInt(expected[0]),new PublicDungeon(state(1)).hp);
            assertEquals(expected[1],state(1).rows.get("turn"));
            String reply=clients[owner].request("LOOKUP "+epoch+" "+saved[1]);
            assertTrue(reply,reply.endsWith(" APPLIED"));
            assertEquals(reply,clients[owner].request(saved[2]));
            refresh();
            assertEquals(Integer.parseInt(expected[0]),new PublicDungeon(state(1)).hp);
            act("WAIT",0,0);
            return;
        }
        if (mode.equals("dart-receipt") || mode.equals("poison-dart-receipt") || mode.equals("geyser-receipt") || mode.equals("door-receipt")) {
            String[] saved=RestartHost.read(path,"hazard-receipt").split(" ",3);
            int cell=Integer.parseInt(RestartHost.read(path,"hazard-cell"));
            String reply=clients[1].request("LOOKUP "+epoch+" "+saved[0]);
            assertTrue(reply,reply.endsWith(" APPLIED"));
            assertEquals(reply,clients[1].request(saved[2]));
            PublicDungeon guest=new PublicDungeon(state(1));
            if (mode.equals("geyser-receipt")) {
                assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WATER, guest.terrain(cell));
                assertFalse(state(1).rows.containsKey("trap"+cell));
            } else if (!mode.equals("door-receipt")) {
                assertEquals(cell,guest.pos);
                assertTrue(guest.hp<Integer.parseInt(saved[1]));
                assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.INACTIVE_TRAP,guest.terrain(cell));
            } else {
                assertEquals("0",state(1).rows.get("keys"));
                assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.DOOR,guest.terrain(cell));
            }
            act("WAIT",0,0);
            return;
        }
        if (mode.equals("mimicreceipt") || mode.equals("goldenreceipt")) {
            String[] saved=RestartHost.read(path,"chest-request").split(" ",3);
            String receipt=clients[1].request("LOOKUP "+epoch+" "+saved[0]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[1].request(saved[2]));
            assertTrue(new PublicDungeon(state(1)).monsters.containsValue(mode.equals("goldenreceipt") ? "GoldenMimic" : "Mimic"));
            assertTrue(new PublicDungeon(state(1)).hp<Integer.parseInt(saved[1]));
            act("WAIT",0,0);
            return;
        }
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
            for(int i=0;i<2;i++) assertEquals(RestartHost.read(path,"public"+i),disclosedWorld(i,true));
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
        if (mode.startsWith("readcast-hand-blast") || mode.startsWith("readcast-hand-fire")) {
            int seat=mode.charAt(mode.length()-1)-'0';
            String[] saved=RestartHost.read(path,"blast-record").split(" ",3);
            PublicDungeon view=new PublicDungeon(state(seat));
            int cell=view.monsters.entrySet().stream().filter(e -> e.getValue().equals("Rat")).findFirst().get().getKey();
            assertTrue(monsterHP(seat,cell)<200);
            if (mode.startsWith("readcast-hand-blast")) assertNotEquals(view.pos+3,cell);
            else assertEquals(view.pos+3,cell);
            long version=state(seat).version;
            assertEquals(Integer.parseInt(saved[1])-1,charges(seat));
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+saved[0]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(saved[2]));
            refresh(); assertEquals(version,state(seat).version);
            act("WAIT",0,0);
            return;
        }
        if (mode.startsWith("readcast")) {
            int seat=mode.charAt(mode.length()-1)-'0';
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
            PublicDungeon afterRestart=new PublicDungeon(state(seat));
            SeatQuickslots layout;
            try (DataInputStream in=new DataInputStream(Files.newInputStream(path.resolve("user-layout")))) { layout=SeatQuickslots.read(in); }
            PublicDungeon.Loot bound=layout.item(3,afterRestart);
            assertNotNull(bound);
            boolean handheld=mode.startsWith("readcast-hand");
            assertEquals(item(seat,handheld ? "BAG" : "WEAPON",handheld ? "WAND" : "STAFF"),bound.id);
            assertEquals("ZAP",PublicDungeon.shortcutAction(bound));
            assertTrue(afterRestart.actions(bound).contains("ZAP"));
            assertFalse(state(1-seat).rows.containsValue(bound.reference));
            act("ZAP",cell,bound.id);
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

    private SeatActions.Pending consentAndCross() throws Exception { return consentAndCross(false); }
    private SeatActions.Pending consentAndCross(boolean vaultReward) throws Exception { return consentAndCross(vaultReward,false); }
    private SeatActions.Pending consentAndCross(boolean vaultReward,boolean finalTreasure) throws Exception {
        refresh();
        int mover=active(),other=1-mover;
        int cell=new PublicDungeon(state(mover)).stair;
        assertTrue(cell>=0);
        String reply=stairAction(other,"CONSENT",cell).submit(clients[other]);
        assertTrue(reply,reply.endsWith(" APPLIED"));
        refresh();
        assertTrue(new PublicDungeon(state(mover)).consent);
        if (vaultReward) {
            PublicDungeon view=new PublicDungeon(state(mover));
            PublicDungeon.Loot reward=view.belongings.stream().filter(i->(finalTreasure ? i.sprite>=com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.RING_GARNET && i.sprite<=com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.RING_GARNET+15 : "FOOD".equals(i.kind))
                    && view.source.rows.containsKey("vaultpick"+i.id)).findFirst().orElseThrow(AssertionError::new);
            return actions[mover].prepare(state(mover),"VAULT_REWARD",cell,reward.id);
        }
        return stairAction(mover,"TRAVEL",cell);
    }

    private void trade(String mode) throws Exception {
        int seat=Integer.parseInt(mode.substring(mode.length()-1));
        String[] stock=RestartHost.read(path,"stock"+seat).split(" ");
        int cell=Integer.parseInt(stock[0]),sprite=Integer.parseInt(stock[2]),value=Integer.parseInt(stock[3]);
        String verb=mode.startsWith("market-buy") ? "BUY" : mode.startsWith("market-sell") ? "SELL" : "BUYBACK";
        if (!verb.equals("BUY")) {
            String[] old=RestartHost.read(path,"trade-action").split(" ",3);
            String receipt=clients[seat].request("LOOKUP "+epoch+" "+old[1]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[seat].request(old[2]));
        }
        if (mode.startsWith("market-finish")) {
            offer(seat);
            assertFalse(state(seat).rows.keySet().stream().anyMatch(k->k.startsWith("refund")));
            PublicDungeon view=new PublicDungeon(state(seat));
            PublicDungeon.Loot goods=view.belongings.stream().filter(i->i.sprite==sprite && i.slot.equals("BAG")).findFirst().get();
            assertFalse(goods.reference.isEmpty());
            int partner=1-seat;
            offer(partner);
            int shop=Integer.parseInt(state(partner).rows.get("merchant"));
            assertTrue(actions[partner].prepare(state(partner),"SELL",shop,goods.id).submit(clients[partner]).endsWith(" REJECT_ITEM"));
            assertTrue(actions[partner].prepare(state(partner),"BUYBACK",shop,goods.id).submit(clients[partner]).endsWith(" REJECT_ITEM"));
            act("WAIT",0,0);
            return;
        }
        offer(seat);
        PublicDungeon view=new PublicDungeon(state(seat));
        int gold=Integer.parseInt(view.source.rows.get("gold"));
        assertEquals(view.source.rows.get("gold"),state(1-seat).rows.get("gold"));
        long handle;
        int change;
        if (verb.equals("BUY")) {
            String[] quote=view.source.rows.get("quote"+cell).split(",");
            assertTrue(state(1-seat).rows.containsKey("quote"+cell));
            int other=1-seat;
            SeatActions.Pending race=actions[other].prepare(state(other),"CONSENT",cell,Long.parseLong(state(other).rows.get("quote"+cell).split(",")[0]));
            assertTrue(clients[other].request(race.command.replace(" CONSENT "," BUY ")).endsWith(" REJECT_TURN"));
            handle=Long.parseLong(quote[0]); change=-Integer.parseInt(quote[1]);
        } else {
            assertFalse(state(1-seat).rows.containsKey("quote"+cell));
            cell=Integer.parseInt(view.source.rows.get("merchant"));
            if (verb.equals("SELL")) {
                PublicDungeon.Loot bought=view.belongings.stream().filter(i->i.sprite==sprite && i.slot.equals("BAG")
                        && view.source.rows.containsKey("sell"+i.id) && view.source.rows.get("sell"+i.id).startsWith(value+",")).findFirst().get();
                handle=bought.id; change=value;
            } else {
                String key=view.source.rows.keySet().stream().filter(k->k.startsWith("refund")).findFirst().get();
                assertFalse(state(1-seat).rows.keySet().stream().anyMatch(k->k.startsWith("refund")));
                handle=Long.parseLong(key.substring(6)); change=-value;
            }
        }
        SeatActions.Pending pending=actions[seat].prepare(view.source,verb,cell,handle);
        RestartHost.write(path,"trade-action",seat+" "+pending.id+" "+pending.command);
        RestartHost.write(path,"expected-gold",Integer.toString(gold+change));
        RestartHost.write(path,"trade-start",Long.toString(System.currentTimeMillis()));
        RestartHost.write(path,"kill-before-reply","1");
        assertThrows(IOException.class,()->pending.submit(clients[seat]));
    }

    private static final int CAMPAIGN_RESTART_STEP = 400;

    private void campaign(String mode) throws Exception {
        // A separate native-engine process prepares a map-aware list of ordinary player inputs.
        // Each authenticated socket step below checks the independent host's resulting position and HP.
        Path dir=path;
        boolean extended=mode.startsWith("journey-seven-");
        boolean coverAlly=mode.equals("campaign-team") || mode.equals("journey-seven-1105");
        String seed=coverAlly ? "1105" : "581";
        Process guide=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx256m",
                "-Djourney.front=MAGE","-Djourney.goalDepth="+(extended ? 7 : mode.equals("prison") ? 6 : 5),"-Djourney.coverAlly="+coverAlly,"-Djourney.trace="+dir.resolve("route"),"-cp",System.getProperty("java.class.path"),
                "com.shatteredpixel.shatteredpixeldungeon.SewerJourney",seed)
                .redirectErrorStream(true).redirectOutput(dir.resolve("guide.log").toFile()).start();
        boolean defeated;
        try {
            assertTrue(guide.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(0,guide.exitValue());
            String outcome=RestartHost.read(dir,"guide.log");
            defeated=extended && outcome.contains("highest=6 result=casualty/halt DEAD/DEAD stops=null");
            assertTrue(outcome.substring(Math.max(0,outcome.length()-500)),defeated || outcome.contains("both alive, exit unlocked"));
        } finally { guide.destroyForcibly(); }

        boolean resumed=mode.equals("campaign-resume");
        int steps=0, crossings=0, exitsOpened=0;
        int priorCrossings=0;
        List<Long> crossingMicros=new ArrayList<>(),bossMicros=new ArrayList<>();
        List<String> route=Files.readAllLines(path.resolve("route"));
        for(String line:route) {
            String[] row=line.split(" ");
            int seat=Integer.parseInt(row[0]), cell=Integer.parseInt(row[4]);
            String verb=row[3], where="step "+steps+" "+line;
            if (resumed && steps<CAMPAIGN_RESTART_STEP) {
                if (verb.equals("TRAVEL")) priorCrossings++;
                steps++;
                continue;
            }
            PublicDungeon before=new PublicDungeon(state(seat));
            assertEquals(where,seat,active());
            assertEquals(where,Integer.parseInt(row[1]),before.depth);
            assertEquals(where,Integer.parseInt(row[2]),before.pos);
            if(verb.equals("TRAVEL")) {
                long crossingStart=System.nanoTime();
                assertTrue(consentAndCross().submit(clients[seat]).endsWith(" APPLIED"));
                refresh(); crossings++; crossingMicros.add((System.nanoTime()-crossingStart)/1000);
            } else {
                long handle=0;
                switch(verb) {
                    case "PICK_UP": handle=before.floor.get(cell).id; break;
                    case "ZAP": handle=item(seat,"WEAPON","STAFF"); break;
                    case "SHOOT": handle=item(seat,"BAG","BOW"); break;
                    case "DRINK": handle=item(seat,"BAG","HEALING"); break;
                    case "EAT": handle=item(seat,"BAG","FOOD"); break;
                    default: break;
                }
                act(verb,cell,handle,defeated && steps==route.size()-1 ? "PARTY_DEFEATED" : "APPLIED");
                if(before.depth==5 && before.monsters.containsValue("Goo")) bossMicros.add(latency.get(latency.size()-1));
            }
            if (defeated && steps==route.size()-1) {
                // The terminal image deliberately withdraws both map and inventory.
                assertEquals(where,"0,"+row[5],state(seat).rows.get("floor"));
                assertEquals(where,0,Integer.parseInt(row[7]));
                steps++;
                break;
            }
            PublicDungeon after=new PublicDungeon(state(seat));
            assertEquals(where,Integer.parseInt(row[5]),after.depth);
            assertEquals(where,Integer.parseInt(row[6]),after.pos);
            assertEquals(where,Integer.parseInt(row[7]),after.hp);
            if(verb.equals("UNLOCK") && after.depth==5) exitsOpened++;
            steps++;
            if (mode.equals("campaign-start") && steps==CAMPAIGN_RESTART_STEP) {
                assertEquals(3,after.depth);
                assertEquals(2,crossings);
                System.out.println("native socket route checkpoint seed=581 prefix_actions="+steps+" extra_consents="+crossings);
                return;
            }
            Thread.sleep(30);
        }
        assertEquals(extended ? (defeated ? 5 : 6) : mode.equals("prison") ? 5 : 4,priorCrossings+crossings);
        assertEquals(1,exitsOpened);
        if (defeated) {
            for (int i=0;i<2;i++) {
                assertEquals("0,6",state(i).rows.get("floor"));
                assertEquals("DEAD",state(i).rows.get("life"));
                assertEquals("PARTY_DEFEATED",state(i).rows.get("status"));
                assertFalse(state(i).rows.containsKey("self"));
            }
            assertEquals(-1,active());
            System.out.println("native socket route normal-start seed="+seed+" inputs="+steps+" consents="+crossings
                    +" highest=6 outcome=PARTY_DEFEATED no_position_fixture=true");
            return;
        }
        if (mode.equals("prison")) {
            for (int i=0;i<2;i++) {
                PublicDungeon view=new PublicDungeon(state(i)); assertEquals(6,view.depth);
                PublicDungeon.Loot weapon=view.belongings.stream().filter(x->x.slot.equals("WEAPON")).findFirst().get();
                assertFalse(weapon.reference.isEmpty());
                RestartHost.write(path,"arrived"+i,state(i).rows.get("self").split(",")[0]+" "+view.pos+" "+view.hp+" "+weapon.reference);
            }
        }
        int blobs=0;
        for (int seat=0;seat<2;seat++) {
            PublicDungeon survivor=new PublicDungeon(state(seat));
            assertTrue(survivor.hp>0);
            for (PublicDungeon.Loot loot:survivor.belongings) {
                if (loot.sprite==com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.BLOB) blobs+=loot.quantity;
            }
        }
        assertEquals(coverAlly ? 2 : 1,blobs);
        for (List<Long> sample:Arrays.asList(latency,bossMicros,crossingMicros)) {
            assertFalse(sample.isEmpty()); Collections.sort(sample);
            System.out.println("native socket route campaign "+(sample==latency?"action_ack":sample==bossMicros?"visible_goo_ack":"consent_travel_sync")+
                    " count="+sample.size()+" median_us="+sample.get(sample.size()/2)+
                    " p95_us="+sample.get((int)Math.ceil(.95*sample.size())-1));
        }
        System.out.println("native socket route natural campaign seed="+seed+" mode="+mode+" seat_actions="+(resumed ? steps-CAMPAIGN_RESTART_STEP : steps)+
                " extra_consents="+crossings+" carried_GooBlob="+blobs+" hp="+new PublicDungeon(state(0)).hp+"/"+new PublicDungeon(state(1)).hp);
    }

    private void boss() throws Exception {
        for(int depth=2;depth<=5;depth++) {
            int seat=active();
            assertTrue(consentAndCross().submit(clients[seat]).endsWith(" APPLIED"));
            applied++; refresh(); assertEquals(depth,new PublicDungeon(state(seat)).depth);
            act("WAIT",0,0); // publish the test placement through the next real actor boundary
        }
        for(int n=0;n<250;n++) {
            int seat=active();
            PublicDungeon view=new PublicDungeon(state(seat));
            int cell=view.monsters.entrySet().stream().filter(e->e.getValue().equals("Goo"))
                    .map(Map.Entry::getKey).findFirst().orElseThrow(AssertionError::new);
            boolean bow=seat==1;
            SeatActions.Pending request=actions[seat].prepare(state(seat),bow?"SHOOT":"ATTACK",cell,
                    bow?item(seat,"BAG","BOW"):0);
            RestartHost.write(path,"boss-action",seat+" "+request.id+" "+request.command);
            long begin=System.nanoTime();
            try {
                String reply=request.submit(clients[seat]);
                assertTrue(reply,reply.endsWith(" APPLIED")); applied++;
                latency.add((System.nanoTime()-begin)/1000);
                refresh();
            } catch(IOException lost) {
                Collections.sort(latency);
                System.out.println("native socket route isolated boss attacks-before-lost-response="+n+
                        " ack_median_us="+latency.get(latency.size()/2)+" ack_p95_us="+latency.get((latency.size()-1)*95/100));
                return;
            }
        }
        fail("Boss encounter did not end");
    }

    private void crystalResume(String mode) throws Exception {
        PublicDungeon guest=new PublicDungeon(state(1));
        Path layoutFile=path.resolve("mimic-shortcut");
        SeatQuickslots shortcuts;
        if (mode.equals("crystal-open")) {
            PublicDungeon.Loot food=guest.belongings.stream().filter(i->i.kind.equals("FOOD")).findFirst().get();
            shortcuts=SeatQuickslots.empty().bind(1,food);
            try (DataOutputStream out=new DataOutputStream(Files.newOutputStream(layoutFile))) { shortcuts.write(out); }
        } else {
            try (DataInputStream in=new DataInputStream(Files.newInputStream(layoutFile))) { shortcuts=SeatQuickslots.read(in); }
        }
        PublicDungeon.Loot carried=shortcuts.item(1,guest);
        assertNotNull("The victim retains the physical remainder of the split stack",carried);
        assertNull(shortcuts.item(1,new PublicDungeon(state(0))));
        assertFalse(state(0).rows.containsValue(carried.reference));
        assertEquals(mode.equals("crystal-open") || mode.equals("crystal-final") ? 3 : 2,carried.quantity);
        if (mode.equals("crystal-open")) {
            int cell=Integer.parseInt(RestartHost.read(path,"encounter-cell"));
            assertEquals("CRYSTAL_CHEST",state(1).rows.get("h"+cell));
            for(int s=0;s<2;s++) assertFalse(state(s).rows.toString().contains("Mimic"));
            SeatActions.Pending p=actions[1].prepare(state(1),"OPEN",cell,0);
            RestartHost.write(path,"steal-action",p.id+" "+p.command);
            RestartHost.write(path,"kill-before-reply","1");
            assertThrows(IOException.class,()->p.submit(clients[1]));
        } else if (mode.equals("crystal-park") || mode.equals("crystal-return")) {
            if (mode.equals("crystal-park")) {
                String[] steal=RestartHost.read(path,"steal-action").split(" ",2);
                String receipt=clients[1].request("LOOKUP "+epoch+" "+steal[0]);
                assertTrue(receipt,receipt.endsWith(" APPLIED"));
                assertEquals(receipt,clients[1].request(steal[1]));
                assertEquals(2,new PublicDungeon(state(1)).belongings.stream().filter(i->i.kind.equals("FOOD")).findFirst().get().quantity);
            }
            int seat=active();
            assertTrue(consentAndCross().submit(clients[seat]).endsWith(" APPLIED")); applied++;
            refresh();
            assertEquals(mode.equals("crystal-park")?1:2,new PublicDungeon(state(1)).depth);
        } else if (mode.equals("crystal-fight")) {
            for(int n=0;n<16;n++) {
                Files.deleteIfExists(path.resolve("in-range"));
                RestartHost.write(path,"approach","1");
                long deadline=System.nanoTime()+3000000000L;
                while (!Files.exists(path.resolve("in-range")) && System.nanoTime()<deadline) Thread.sleep(3);
                assertTrue(Files.exists(path.resolve("in-range")));
                reconnectPair();
                int seat=active();
                PublicDungeon view=new PublicDungeon(state(seat));
                Optional<Integer> target=view.monsters.entrySet().stream().filter(e->e.getValue().equals("CrystalMimic"))
                        .map(Map.Entry::getKey).findFirst();
                assertTrue("Missing fleeing target",target.isPresent());
                SeatActions.Pending shot=actions[seat].prepare(state(seat),"ZAP",target.get(),item(seat,"WEAPON","STAFF"));
                RestartHost.write(path,"strike",seat+" "+target.get()+" "+shot.id+" "+shot.command);
                String result;
                try { result=shot.submit(clients[seat]); } catch(IOException lost) { return; }
                assertTrue(result,result.endsWith(" APPLIED")); applied++; refresh();
            }
            fail("No committed killing shot");
        } else if (mode.equals("crystal-loot")) {
            String[] shot=RestartHost.read(path,"strike").split(" ",4);
            int shooter=Integer.parseInt(shot[0]),cell=Integer.parseInt(shot[1]);
            String receipt=clients[shooter].request("LOOKUP "+epoch+" "+shot[2]);
            assertTrue(receipt,receipt.endsWith(" APPLIED"));
            assertEquals(receipt,clients[shooter].request(shot[3]));
            offer(1);
            assertEquals(cell,pos(1));
            assertFalse(new PublicDungeon(state(1)).monsters.containsValue("CrystalMimic"));
            act("PICK_UP",cell,item(1,"F"+cell,"FOOD"));
            PublicDungeon.Loot merged=shortcuts.item(1,new PublicDungeon(state(1)));
            assertEquals(carried.reference,merged.reference);
            assertEquals(3,merged.quantity);
        } else {
            assertEquals(3,new PublicDungeon(state(1)).belongings.stream().filter(i->i.kind.equals("FOOD")).findFirst().get().quantity);
            act("WAIT",0,0);
        }
    }

    private void openChest(boolean golden) throws Exception {
        int mover=active();
        assertTrue(consentAndCross().submit(clients[mover]).endsWith(" APPLIED")); applied++;
        refresh();
        act("WAIT",0,0);
        offer(1);
        int cell=Integer.parseInt(RestartHost.read(path,"encounter-cell"));
        PublicDungeon guest=new PublicDungeon(state(1));
        assertTrue(guest.chest(cell));
        assertEquals(golden ? "LOCKED_CHEST" : "CHEST",guest.source.rows.get("h"+cell));
        for(int seat=0;seat<2;seat++) {
            assertFalse(state(seat).rows.toString().contains("Mimic"));
            assertFalse(new PublicDungeon(state(seat)).monsters.containsKey(cell));
        }
        SeatActions.Pending request=actions[1].prepare(state(1),"OPEN",cell,0);
        RestartHost.write(path,"chest-request",request.id+" "+guest.hp+" "+request.command);
        RestartHost.write(path,"kill-before-reply","1");
        assertThrows(IOException.class,()->request.submit(clients[1]));
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
        return new PublicDungeon(state(seat)).belongings.stream().filter(PublicDungeon.Loot::showsCharges).findFirst().get().charges;
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
