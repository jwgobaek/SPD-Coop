package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.lan.LanRoom;
import com.shatteredpixel.shatteredpixeldungeon.lan.RuleScenarioClient;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class RuleIntegrationTest extends HeadlessDungeon {
    private static final int TARGET = 340;

    /** Controlled terrain, loot and two real hunting actors inside the generated floor. */
    private static class Encounter {
        final Rat left = new Rat(), right = new Rat();
        final Ankh ankh = new Ankh();
        final int center, width, drop;
        boolean merged, split, woundScheduled, revived, defeatScheduled;
        int commits;

        Encounter(CoopSession game) {
            Level level = Dungeon.level;
            width = level.width();
            center = level.length()/2/width*width + width/2;
            drop = center-1+2*width;
            for (Mob mob : new ArrayList<>(level.mobs)) { Actor.remove(mob); level.mobs.remove(mob); }
            level.transitions.clear();
            for (int y=-5; y<=5; y++) for (int x=-6; x<=6; x++) {
                int p=center+y*width+x;
                boolean wall=Math.abs(y)==5 || Math.abs(x)==6 || (x==0 && y!=2);
                Level.set(p,wall ? Terrain.WALL : Terrain.EMPTY);
                level.discoverable[p]=true;
                level.plants.remove(p);
                level.traps.remove(p);
                level.heaps.remove(p);
            }
            Hero a=game.hero(0), b=game.hero(1);
            a.pos=center-4; b.pos=center+4;
            level.drop(new Food(),a.pos);
            level.drop(ankh,b.pos);
            left.pos=center-3+width; right.pos=center+3+width;
            left.HT=left.HP=right.HT=right.HP=100;
            GameScene.add(left,1000); GameScene.add(right,1000);
            left.aggro(a); right.aggro(b);
            Random.pushGenerator(Dungeon.seed);
        }

        void after(CoopSession game,int acting,int priorPos) {
            commits++;
            int count=CombatZones.calculate(game,null).size();
            if (count==1 && game.hero(0).pos==drop && game.hero(1).pos==drop+2) merged=true;
            if (merged && count==2 && game.hero(0).pos==center-4 && game.hero(1).pos==center+4) split=true;
            if (!woundScheduled && acting==1 && priorPos==drop && game.hero(1).pos==drop) {
                woundScheduled=true;
                // Deliver the hit during the next original Actor.process cycle and state commit.
                Actor.add(new Actor() {
                    protected boolean act() {
                        Hero target=game.hero(1);
                        target.HP=17;
                        right.attack(target,1,10000,Float.POSITIVE_INFINITY);
                        assertEquals(CoopSession.Life.DOWNED,game.life(1));
                        assertEquals(17,game.preDeathHP(1));
                        Actor.remove(this);
                        return true;
                    }
                });
            }
            if (woundScheduled && game.preDeathHP(1)==17 && game.life(1)==CoopSession.Life.ALIVE) {
                if (!revived) assertEquals(5,game.hero(1).HP);
                revived=true;
                assertFalse(game.hero(1).belongings.backpack.contains(ankh));
            }
            if (commits==TARGET && !defeatScheduled) {
                defeatScheduled=true;
                Actor.add(new Actor() {
                    protected boolean act() {
                        for (int seat=0;seat<2;seat++) game.hero(seat).damage(10000,left);
                        Actor.remove(this);
                        return true;
                    }
                });
            }
        }
    }

    @Test(timeout=240000)
    public void rulesAcrossSocketsWithThreeDungeonsAndBothClassOrders() throws Exception {
        for (long seed : new long[]{402,2402,3402}) scenario(seed);
    }

    private void scenario(long seed) throws Exception {
        File log=temporary.newFile();
        try (LanRoom room=LanRoom.synchronizedGame(InetAddress.getLoopbackAddress(),0,0,seed)) {
            Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",
                    System.getProperty("coop.client.classpath"),RuleScenarioClient.class.getName(),
                    Integer.toString(room.tcpPort()),Long.toString(seed)).redirectErrorStream(true).redirectOutput(log).start();
            Encounter encounter=null;
            try {
                try (PrintWriter input=new PrintWriter(child.getOutputStream())) { input.println(room.hostJoinCommand()); }
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(72);
                while (child.isAlive() && System.nanoTime()<deadline) {
                    CoopSession game=Dungeon.coop;
                    long before=game==null ? 0 : game.turn();
                    int seat=game==null ? -1 : game.activeSeat();
                    int pos=seat<0 ? -1 : game.hero(seat).pos;
                    LanRoom.State state=room.state();
                    room.pump();
                    if (game==null && room.state()==LanRoom.State.IN_GAME) encounter=new Encounter(Dungeon.coop);
                    else if (encounter!=null && state==LanRoom.State.IN_GAME
                            && (game.turn()!=before || room.state()==LanRoom.State.PARTY_DEFEATED)) encounter.after(game,seat,pos);
                    if (room.failure()!=null) fail(room.failure());
                    Thread.sleep(1);
                }
                assertTrue(child.waitFor(1,TimeUnit.SECONDS));
                String trace=new String(Files.readAllBytes(log.toPath()));
                assertEquals(trace,0,child.exitValue());
                assertNotNull(encounter);
                assertTrue(trace,trace.contains("confirmed="+encounter.commits+" "));
                assertTrue(encounter.merged && encounter.split && encounter.revived && encounter.defeatScheduled);
                assertTrue(encounter.commits>TARGET);
                System.out.print(trace);
            } finally {
                if (encounter!=null) Random.popGenerator();
                if (child.isAlive()) child.destroyForcibly();
                assertTrue(child.waitFor(5,TimeUnit.SECONDS));
            }
        }
    }
}
