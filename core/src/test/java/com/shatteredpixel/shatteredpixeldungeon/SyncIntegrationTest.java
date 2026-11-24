package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.*;
import org.junit.Test;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class SyncIntegrationTest extends HeadlessDungeon {
    private int neighbor(Hero h) {
        for (int d : PathFinder.NEIGHBOURS8) {
            int pos=h.pos+d;
            if (Dungeon.level.passable[pos] && Actor.findChar(pos)==null) return pos;
        }
        throw new AssertionError("No free adjacent tile");
    }

    @Test(timeout=10000)
    public void directSightPrivacyAndLossOnGeneratedFloor() throws Exception {
        try (CoopSession game=CoopSession.open(402,1,com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR)) {
            game.join(2,com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.HUNTRESS);
            ProjectionChecks.check(game);
        }
    }

    @Test(timeout=230000)
    public void committedViewsAcrossThreeIndependentClientRuns() throws Exception {
        for (int run=0;run<3;run++) scenario(340,true);
    }

    @Test(timeout=80000)
    public void normalSynchronizedGameAcrossSocket() throws Exception {
        scenario(320,false);
    }

    private void scenario(int target,boolean faults) throws Exception {
        File log=temporary.newFile();
        try (LanRoom room=LanRoom.synchronizedGame(InetAddress.getByName("127.0.0.1"),0,0,402)) {
            Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",
                    System.getProperty("coop.client.classpath"),SyncScenarioClient.class.getName(),
                    String.valueOf(room.tcpPort()),String.valueOf(target),String.valueOf(faults)).redirectErrorStream(true).redirectOutput(log).start();
            Rat rat=new Rat();
            Shortsword sword=new Shortsword();
            int committed=0;
            boolean forced=false, stopped=false;
            float clock=0;
            try {
                try (PrintWriter input=new PrintWriter(child.getOutputStream())) { input.println(room.hostJoinCommand()); }
                long deadline=System.nanoTime()+65000000000L;
                while (child.isAlive() && System.nanoTime()<deadline) {
                    CoopSession old=Dungeon.coop;
                    long before=old==null ? 0 : old.turn();
                    Hero acting=old==null || old.activeSeat()<0 ? null : old.hero(old.activeSeat());
                    float due=acting==null ? 0 : Actor.now()+acting.cooldown();
                    room.pump();
                    CoopSession game=Dungeon.coop;
                    if (room.state()==LanRoom.State.IN_GAME && old==null) {
                        Random.pushGenerator(402);
                        if (!faults) continue;
                        Hero owner=game.hero(0), guest=game.hero(1);
                        Dungeon.level.drop(sword,owner.pos);
                        Dungeon.level.drop(new Food(),guest.pos);
                        rat.pos=neighbor(owner);
                        rat.HP=1;
                        GameScene.add(rat,230);
                        Buff.prolong(guest,Haste.class,3);
                    } else if (room.state()==LanRoom.State.IN_GAME && game.turn()!=before) {
                        assertEquals(before+1,game.turn());
                        assertTrue(Actor.now()>=clock);
                        clock=Actor.now();
                        committed++;
                        if (!faults && committed==100) {
                            float elapsed=Statistics.duration+Actor.now();
                            Actor.fixTime();
                            assertEquals(elapsed,Statistics.duration+Actor.now(),0.0001f);
                            clock=Actor.now();
                        }
                        if (faults && committed==target) {
                            assertFalse(rat.isAlive());
                            assertSame(sword,game.hero(0).belongings.weapon);
                            assertEquals(1,game.hero(1).belongings.getItem(Food.class).quantity());
                            DM100 unsupported=new DM100();
                            unsupported.pos=neighbor(game.hero(0));
                            GameScene.add(unsupported);
                            forced=true;
                        }
                    } else if (room.state()==LanRoom.State.PAUSED && room.failure()!=null) {
                        assertTrue(room.failure(),room.failure().contains("DM100"));
                        if (!stopped) {
                            assertNotNull(acting);
                            assertTrue("Accepted WAIT already spent time before scheduler failure",Actor.now()+acting.cooldown()>due);
                        }
                        stopped=true;
                    }
                    Thread.sleep(1);
                }
                assertTrue("Client deadline",child.waitFor(1,TimeUnit.SECONDS));
                String trace=new String(Files.readAllBytes(log.toPath()));
                assertEquals(trace,0,child.exitValue());
                assertTrue(trace,trace.contains("confirmed="+committed+" "));
                assertEquals(faults,forced);
                assertEquals(faults,stopped);
                assertTrue(committed>=target);
                System.out.print(trace);
                Random.popGenerator();
            } finally {
                if (child.isAlive()) child.destroyForcibly();
                assertTrue(child.waitFor(5,TimeUnit.SECONDS));
            }
        }
    }
}
