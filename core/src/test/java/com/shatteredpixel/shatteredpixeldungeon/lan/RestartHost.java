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
            if (mode.equals("kill_"+stage) && java.nio.file.Files.exists(path.resolve("fault-armed")))
                Runtime.getRuntime().halt(80);
            if (stage.equals("WRITING") && java.nio.file.Files.exists(path.resolve("fail-write")))
                throw new IOException("Injected write failure");
            if (stage.equals("AFTER_HEAD") && java.nio.file.Files.exists(path.resolve("kill-before-reply")))
                Runtime.getRuntime().halt(78);
        });
        InetAddress loop=InetAddress.getLoopbackAddress();
        try(LanRoom room=(mode.equals("new") || mode.startsWith("cast")) ? LanRoom.persistentGame(loop,0,0,402,saves)
                : LanRoom.resume(loop,0,0,saves,saves.sessions().get(0),mode.equals("backup"))) {
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
            boolean started=false;
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

    static void write(Path dir,String name,String value) throws IOException {
        java.nio.file.Files.write(dir.resolve(name),value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    static String read(Path dir,String name) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(dir.resolve(name)),java.nio.charset.StandardCharsets.UTF_8);
    }
}
