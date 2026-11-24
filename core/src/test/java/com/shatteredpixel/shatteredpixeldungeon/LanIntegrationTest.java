package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.lan.LanRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.Test;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class LanIntegrationTest extends HeadlessDungeon {
    @Test(timeout=100000)
    public void twoSocketSeatsOnRealEngineWithIndependentClientJvm() throws Exception {
        InetAddress loop = InetAddress.getByName("127.0.0.1");
        File log = temporary.newFile();
        Shortsword loot = new Shortsword();
        Rat rat = new Rat();
        int count = 0;
        int[] actions = new int[2];
        boolean combat = false;
        float previousTime = 0;
        try (LanRoom room = new LanRoom(loop,0,0,402)) {
            Process child = new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",
                    System.getProperty("coop.client.classpath"),LanScenarioClient.class.getName(),
                    String.valueOf(room.tcpPort()),String.valueOf(room.udpPort()))
                    .redirectErrorStream(true).redirectOutput(log).start();
            try {
                try (PrintWriter input = new PrintWriter(child.getOutputStream())) { input.println(room.hostJoinCommand()); }
                long deadline = System.nanoTime()+85000000000L;
                while (child.isAlive() || room.state() != LanRoom.State.CLOSED) {
                    String trace = child.isAlive() ? "" : new String(Files.readAllBytes(log.toPath()));
                    if (!child.isAlive()) assertEquals(trace,0,child.exitValue());
                    assertTrue(trace,System.nanoTime()<deadline);
                    CoopSession engine = Dungeon.coop;
                    int seat = engine == null ? -1 : engine.activeSeat();
                    long before = engine == null ? 0 : engine.turn();
                    room.pump();
                    assertNull(room.failure(),room.failure());
                    if (room.state() == LanRoom.State.IN_GAME) {
                        CoopSession current = Dungeon.coop;
                        if (engine == null) {
                            Dungeon.level.drop(loot,current.hero(0).pos);
                            assertNotEquals(current.hero(0).id(),current.hero(1).id());
                            Random.pushGenerator(402);
                        } else if (current.turn() != before) {
                            assertEquals(before+1,current.turn());
                            actions[seat]++;
                            count++;
                            Hero actor = current.hero(seat);
                            assertSame(actor,Actor.findChar(actor.pos));
                            assertTrue(Actor.now()>=previousTime);
                            previousTime = Actor.now();
                            if (actions[0]>=1 && actions[0]<2) assertSame(loot,current.hero(0).belongings.getItem(Shortsword.class));
                            if (actions[0]>=2) assertSame(loot,current.hero(0).belongings.weapon);
                            if (actions[1]>=1) assertNull(current.hero(1).belongings.getItem(Food.class));
                            if (current.activeSeat()==0 && actions[0]==2 && !combat) {
                                Hero host = current.hero(0);
                                for (int delta : PathFinder.NEIGHBOURS8) {
                                    int cell = host.pos+delta;
                                    if (host.fieldOfView[cell] && Dungeon.level.passable[cell] && Actor.findChar(cell)==null) {
                                        rat.pos=cell;
                                        break;
                                    }
                                }
                                assertTrue(Dungeon.level.adjacent(host.pos,rat.pos));
                                rat.HP=1;
                                GameScene.add(rat);
                                Random.popGenerator();
                                Random.pushGenerator(402);
                                combat=true;
                            }
                            if (actions[0]>=3) assertFalse(rat.isAlive());
                        }
                    }
                    Thread.sleep(1);
                }
                assertTrue(child.waitFor(2,TimeUnit.SECONDS));
                String output = new String(Files.readAllBytes(log.toPath()));
                assertEquals(output,0,child.exitValue());
                assertTrue(combat);
                assertEquals(310,count);
                assertNull(Dungeon.coop);
                System.out.print(output);
                Random.popGenerator();
            } finally {
                if (child.isAlive()) child.destroyForcibly();
                assertTrue(child.waitFor(5,TimeUnit.SECONDS));
            }
        }
        for (Thread thread : Thread.getAllStackTraces().keySet()) assertFalse(thread.getName().equals("spd-lan-io"));
        assertFalse(Actor.processing());
    }
}
