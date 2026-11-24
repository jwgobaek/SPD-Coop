package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.headless.*;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.utils.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Fresh host process for every invocation. Control files are confined to a test-private directory. */
public final class RestartHost {
    public static void main(String[] args) throws Exception {
        Path base=Paths.get(args[0]);
        String mode=args[1];
        HeadlessApplicationConfiguration config=new HeadlessApplicationConfiguration();
        config.preferencesDirectory=base.toString()+"/";
        HeadlessApplication backend=new HeadlessApplication(new ApplicationAdapter(){},config);
        FileUtils.setDefaultFileProperties(com.badlogic.gdx.Files.FileType.Absolute,base.toString()+"/");
        Game.version="4.0.2-INDEV"; Game.versionCode=922;
        new Game(null,null) {{ requestedReset=false; }};
        try { host(base,mode); } finally { backend.exit(); }
    }

    private static void host(Path path,String mode) throws Exception {
        CoopSaves saves=new CoopSaves(path.toFile(),stage->{
            if ((mode.equals("kill_"+stage) || (mode.equals("ascendfault") && stage.equals("BEFORE_HEAD"))) && java.nio.file.Files.exists(path.resolve("fault-armed")))
                Runtime.getRuntime().halt(80);
            if (stage.equals("WRITING") && java.nio.file.Files.exists(path.resolve("fail-write")))
                throw new IOException("Injected write failure");
            if (stage.equals("AFTER_HEAD") && java.nio.file.Files.exists(path.resolve("kill-before-reply")))
                Runtime.getRuntime().halt(78);
        });
        InetAddress loop=InetAddress.getLoopbackAddress();
        boolean special = mode.equals("sentry") || mode.equals("piranha") || mode.equals("quest") || mode.equals("hive");
        try(LanRoom room=(special || mode.equals("new") || mode.equals("walk") || mode.startsWith("cast")) ? LanRoom.persistentGame(loop,0,0,mode.equals("hive")?409:mode.equals("quest")?403:mode.equals("piranha")?406:mode.equals("walk")?415:402,saves)
                : LanRoom.resume(loop,0,0,saves,saves.sessions().get(0),mode.equals("backup") || mode.equals("floorbackup"))) {
            Rat rat=null;
            if (mode.equals("revive") || mode.equals("soak")) {
                CoopSession g=Dungeon.coop;
                Hero guest=g.hero(1);
                assertEquals(mode.equals("revive"),guest.frozenInCoop());
                assertEquals(mode.equals("revive"),guest.belongings.getItem(Ankh.class)!=null);
                Item item=Dungeon.level.heaps.get(g.hero(0).pos).peek();
                assertEquals(g.player(0),g.ownership.owner(item));
                assertEquals(Long.parseLong(read(path,"dropid")),g.ownership.identity(item));
            }
            write(path,"endpoint.tmp",room.tcpPort()+"\n"+room.hostJoinCommand());
            java.nio.file.Files.move(path.resolve("endpoint.tmp"),path.resolve("endpoint"));
            boolean started=false, profiled=false;
            String lastGuide="";
            int arrangedDepth=0;
            String arrangedQuest="";
            while(!java.nio.file.Files.exists(path.resolve("stop"))) {
                if (java.nio.file.Files.deleteIfExists(path.resolve("inflict"))) {
                    CoopSession g=Dungeon.coop;
                    assertEquals(0,g.activeSeat());
                    Hero guest=g.hero(1);
                    guest.HP=17;
                    rat.attack(guest,1,10000,Float.POSITIVE_INFINITY);
                    assertEquals(CoopSession.Life.DOWNED,g.life(1));
                    write(path,"injured","ok");
                }
                room.pump();
                if (room.failure()!=null && !(mode.equals("failure") && room.failure().equals("CHECKPOINT_FAILED")))
                    throw new IllegalStateException(room.failure());
                if (!profiled && (mode.equals("walk") || mode.equals("backatstairs")) && room.state()==LanRoom.State.IN_GAME) {
                    profiled=true;
                    measureSaves(path,room,Dungeon.depth);
                }
                if (mode.equals("walk") && room.state()==LanRoom.State.IN_GAME) {
                    CoopSession g=Dungeon.coop;
                    if (!started) { started=true; com.watabou.utils.Random.pushGenerator(415); }
                    String suggestion=g.turn()+" "+g.activeSeat()+" "+CoopFloorsTest.guide(g);
                    if(!suggestion.equals(lastGuide)) {
                        write(path,"guide.tmp",suggestion);
                        java.nio.file.Files.move(path.resolve("guide.tmp"),path.resolve("guide"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                        lastGuide=suggestion;
                    }
                }
                if (special && room.state()==LanRoom.State.IN_GAME && arrangedDepth!=Dungeon.depth) {
                    arrangedDepth=Dungeon.depth;
                    if (arrangedDepth==1) SewerSpecialActorTest.atGate(Dungeon.coop,Dungeon.level.exit());
                    else if (mode.equals("hive")) {
                        CoopSession g=Dungeon.coop;
                        boolean found=false;
                        for (Heap heap:Dungeon.level.heaps.valueList()) {
                            if (heap.type!=Heap.Type.HEAP || !(heap.peek() instanceof Honeypot) || Actor.findChar(heap.pos)!=null) continue;
                            g.hero(1).pos=heap.pos;
                            found=true;
                            break;
                        }
                        assertTrue(found);
                    }
                    else if (!mode.equals("quest")) SewerSpecialActorTest.exposeOneSeat(Dungeon.coop,1,mode.equals("sentry"));
                }
                if (mode.equals("quest") && arrangedDepth==2) {
                    String offer=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.Quest.sewerOffer();
                    if (!offer.equals(arrangedQuest) && !offer.isEmpty()) {
                        arrangedQuest=offer;
                        for (Mob mob:Dungeon.level.mobs) {
                            boolean opponent=offer.equals("HUNT");
                            if (opponent ? mob instanceof FetidRat : mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost) {
                                // Keep dialogue available between the two socket requests in this placement fixture.
                                if (!opponent) mob.state=mob.PASSIVE;
                                SewerGhostTest.meet(Dungeon.coop,0,mob);
                                SewerGhostTest.meet(Dungeon.coop,1,mob);
                                break;
                            }
                        }
                    }
                }
                if (mode.startsWith("cast") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    int seat=Integer.parseInt(mode.substring(4));
                    com.shatteredpixel.shatteredpixeldungeon.CoopMageTest.encounter(Dungeon.coop,seat);
                }
                if (mode.equals("new") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    CoopSession g=Dungeon.coop;
                    Hero h=g.hero(0), guest=g.hero(1);
                    assertTrue(new Ankh().collect(guest.belongings.backpack));
                    Dungeon.level.drop(new Food(),h.pos);
                    for (Mob m:new ArrayList<>(Dungeon.level.mobs)) { Actor.remove(m); Dungeon.level.mobs.remove(m); }
                    rat=new Rat();
                    for(int step:PathFinder.NEIGHBOURS8) {
                        int cell=h.pos+step;
                        if (Dungeon.level.passable[cell] && Actor.findChar(cell)==null) { rat.pos=cell; break; }
                    }
                    assertTrue(rat.pos>0);
                    rat.HT=rat.HP=1000;
                    GameScene.add(rat,10000);
                }
                Thread.sleep(1);
            }
            if (mode.equals("new")) {
                CoopSession g=Dungeon.coop;
                Item item=Dungeon.level.heaps.get(g.hero(0).pos).peek();
                assertEquals(g.player(0),g.ownership.owner(item));
                write(path,"dropid",Long.toString(g.ownership.identity(item)));
            }
            if (mode.equals("soak")) Runtime.getRuntime().halt(79);
            if (mode.equals("failure")) {
                assertEquals(LanRoom.State.PAUSED,room.state());
                assertThrows(IOException.class,room::saveAndClose);
            } else room.saveAndClose();
        }
    }

    private static void measureSaves(Path path,LanRoom room,int depth) throws IOException {
        long[] samples=new long[21];
        for(int i=0;i<samples.length;i++) {
            long start=System.nanoTime();
            room.save();
            samples[i]=System.nanoTime()-start;
        }
        long disk=0,size=0;
        try(java.util.stream.Stream<Path> paths=java.nio.file.Files.walk(path.resolve("coop-saves-v1"))) {
            for(Iterator<Path> it=paths.iterator();it.hasNext();) {
                Path entry=it.next();
                if(!java.nio.file.Files.isRegularFile(entry)) continue;
                long n=java.nio.file.Files.size(entry);
                disk+=n;
                if(entry.getFileName().toString().matches("g[0-9]+")) size=Math.max(size,n);
            }
        }
        Arrays.sort(samples);
        Runtime vm=Runtime.getRuntime();
        write(path,"perf"+depth,"floor="+depth+" explicit_disk_saves=21 median_us="+samples[10]/1000+
                " p95_us="+samples[19]/1000+" generation_bytes="+size+" slot_bytes="+disk+
                " host_used_heap_bytes="+(vm.totalMemory()-vm.freeMemory()));
    }

    static void write(Path dir,String name,String value) throws IOException {
        java.nio.file.Files.write(dir.resolve(name),value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    static String read(Path dir,String name) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(dir.resolve(name)),java.nio.charset.StandardCharsets.UTF_8);
    }
}
