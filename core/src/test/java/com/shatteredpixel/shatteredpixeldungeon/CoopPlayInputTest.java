package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise the same public-map and inventory commands used by the widgets against a real saved game. */
public class CoopPlayInputTest extends HeadlessDungeon {
    @Test public void nativeRenderInputsForBothSeatsLeaveWorldUnchanged() {
        try (CoopSession game=CoopSession.open(402,1,com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR)) {
            game.join(2,com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.HUNTRESS);
            com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks.checkNativeRenderInputs(game);
        }
    }

    @Test(timeout=5000) public void renderingDoesNotConsumeWorldRandomState() throws Exception {
        com.watabou.utils.Random.enterWorld();
        try {
            long[] before=com.watabou.utils.Random.worldState();
            Thread visual=new Thread(()->{
                com.watabou.utils.Random.pushGenerator(2026);
                com.watabou.utils.Random.Float(false);
                com.watabou.utils.Random.Int(); com.watabou.utils.Random.popGenerator();
            });
            visual.start(); visual.join(1500); assertFalse(visual.isAlive());
            assertArrayEquals(before,com.watabou.utils.Random.worldState());
            com.watabou.utils.Random.Int();
            assertFalse(java.util.Arrays.equals(before,com.watabou.utils.Random.worldState()));
        } finally {com.watabou.utils.Random.leaveWorld();}
    }

    @Test(timeout=70000) public void seatInputsCombatInventoryAndRevival() throws Exception {
        scenario(false);
    }

    @Test(timeout=70000) public void smallRationAndDoorOverSocketsIncludingSeatRecovery() throws Exception {
        scenario(true);
    }
    private void scenario(boolean small) throws Exception {
        File log=temporary.newFile();
        java.util.List<Long> saveMillis=new java.util.ArrayList<>();
        try(LanRoom room=LanRoom.persistentGame(InetAddress.getLoopbackAddress(),0,0,402,new CoopSaves(temporary.newFolder()))) {
            Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",
                    System.getProperty("coop.client.classpath"),CoopPlayClient.class.getName(),
                    Integer.toString(room.tcpPort()),temporary.newFolder().toString(),Boolean.toString(small))
                    .redirectErrorStream(true).redirectOutput(log).start();
            Rat rat=new Rat(); Ankh ankh=new Ankh();
            int center=-1; boolean wounded=false,revived=false;
            try {
                try(PrintWriter stdin=new PrintWriter(child.getOutputStream())) {stdin.println(room.hostJoinCommand());}
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(55);
                while(child.isAlive() && System.nanoTime()<end) {
                    room.pump();
                    CoopSession game=Dungeon.coop;
                    if(game!=null && room.state()==LanRoom.State.IN_GAME) {
                        Level level=Dungeon.level; Hero host=game.hero(0),guest=game.hero(1);
                        if(center<0) {
                            center=level.length()/2/level.width()*level.width()+level.width()/2;
                            for(Mob mob:new ArrayList<>(level.mobs)) {Actor.remove(mob);level.mobs.remove(mob);}
                            level.transitions.clear();
                            for(int y=-3;y<=3;y++) for(int x=-4;x<=4;x++) {
                                int cell=center+x+y*level.width();
                                Level.set(cell,Math.abs(y)==3 || Math.abs(x)==4?Terrain.WALL:Terrain.EMPTY);
                                level.heaps.remove(cell); level.traps.remove(cell); level.plants.remove(cell);level.discoverable[cell]=true;
                            }
                            guest.pos=center;host.pos=center+1+level.width();
                            host.belongings.backpack.items.add(ankh); game.ownership.claim(ankh,game.player(0));
                            level.drop(small?new com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation():new Food(),center);
                            if(small) Level.set(center+1,Terrain.DOOR);
                            rat.pos=center-1; rat.HT=rat.HP=100; GameScene.add(rat,1000);
                        } else if(!wounded && guest.pos==center+1) {
                            if(small) assertEquals(Terrain.OPEN_DOOR,level.map[guest.pos]);
                            wounded=true;
                            Actor.add(new Actor(){ protected boolean act(){
                                host.HP=17; rat.attack(host,1,10000,Float.POSITIVE_INFINITY);
                                assertEquals(CoopSession.Life.DOWNED,game.life(0)); Actor.remove(this); return true;
                            }});
                        } else if(wounded && game.preDeathHP(0)==17 && game.life(0)==CoopSession.Life.ALIVE && !revived) {
                            assertEquals(5,host.HP); assertFalse(host.belongings.backpack.contains(ankh)); revived=true;
                            if(small) for(int n=0;n<20;n++) {
                                long start=System.nanoTime(); room.save(); saveMillis.add((System.nanoTime()-start)/1000);
                            }
                        }
                    }
                    if(room.failure()!=null) fail(room.failure());
                    Thread.sleep(1);
                }
                assertTrue("Child deadline",child.waitFor(1,TimeUnit.SECONDS));
                String trace=new String(Files.readAllBytes(log.toPath()));
                assertEquals(trace,0,child.exitValue()); assertTrue(revived); assertTrue(trace,trace.contains("confirmed inputs=10"));
                System.out.print(trace);
                if(small) {
                    assertEquals(20,saveMillis.size());java.util.Collections.sort(saveMillis);
                    System.out.println("explicit durable checkpoints=20 micros median="+saveMillis.get(9)+" p95="+saveMillis.get(18));
                }
            } finally {if(child.isAlive()) child.destroyForcibly(); assertTrue(child.waitFor(5,TimeUnit.SECONDS));}
        }
    }
}

