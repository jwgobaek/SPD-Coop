package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Reproducible main-floor survey. Stair and combat fixtures are separate from normal starts. */
public class CavesInteriorSurvey extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        CavesInteriorSurvey probe=new CavesInteriorSurvey();probe.temporary.create();probe.startBackend();
        try {
            for(int seed=570;seed<=581;seed++) {
                Dungeon.seed=seed;Dungeon.init();
                for(int depth=1;depth<=16;depth++) {
                    Dungeon.depth=depth;Level level=Dungeon.level=Dungeon.newLevel();
                    if (depth!=16) continue;
                    Set<String> mobs=new TreeSet<>(),rooms=new TreeSet<>(),traps=new TreeSet<>(),blockers=new TreeSet<>();
                    for(Mob m:level.mobs) {
                        mobs.add(m.getClass().getSimpleName());
                        if(!CoopSession.basicMob(m)) blockers.add(m.getClass().getSimpleName());
                    }
                    for(Room room:((RegularLevel)level).rooms()) rooms.add(room.getClass().getSimpleName());
                    for(Trap trap:level.traps.valueList()) traps.add(trap.getClass().getSimpleName());
                    System.out.println("CITY\t"+seed+"\t"+level.getClass().getSimpleName()+"\t"+mobs+"\t"+rooms+"\t"+traps+"\t"+blockers);
                }
            }
            Actor.clear();Dungeon.level=null;Dungeon.hero=null;
            for(int seed=570;seed<=581;seed++) stage(seed);
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }

    static void stage(int seed) {
        Bundle won;
        int count;
        try(CoopSession g=CavesInteriorTest.open(seed)) {
            CavesInteriorTest.arrive(g,11);
            for(int depth=12;depth<=15;depth++) {
                int exit=Dungeon.level.exit();
                SewerSpecialActorTest.atGate(g,exit);
                assertTrue(g.stairs.consent(1-g.activeSeat(),g.turn(),exit,false));
                long[] rng=com.watabou.utils.Random.worldState();
                long turn=g.turn(),start=System.nanoTime();
                if (!CoopFloorsTest.step(g,CoopSession.Action.TRAVEL,exit,null)) {
                    assertEquals(depth-1,Dungeon.depth);assertEquals(turn,g.turn());assertArrayEquals(rng,com.watabou.utils.Random.worldState());
                    System.out.println("STAIR\t"+seed+"\t"+depth+"\tREJECT\t"+g.destinationBlocker);return;
                }
                long travel=(System.nanoTime()-start)/1000;
                start=System.nanoTime();Bundle state=g.checkpoint();long micros=(System.nanoTime()-start)/1000;
                int bytes=state.toString().getBytes(StandardCharsets.UTF_8).length;
                int parked=0,parkedActors=0;
                Bundle dormant=state.getBundle("dormant");
                for(String key:dormant.getKeys()) {
                    Bundle record=dormant.getBundle(key),old=record.getBundle("level");
                    parked+=record.toString().getBytes(StandardCharsets.UTF_8).length;
                    for(Bundle mob:old.getBundleArray("mobs")) parkedActors+=1+mob.getBundleArray("buffs").length;
                    parkedActors+=old.getBundleArray("blobs").length+(old.contains("respawner")?1:0);
                }
                System.out.println("STAIR\t"+seed+"\t"+depth+"\tPASS\t"+travel+"\t"+micros+"\t"+bytes+"\t"+parked+"\t"+Actor.all().size()+"\t"+parkedActors);
            }
            for(int seat=0;seat<2;seat++) {
                Hero hero=g.hero(seat);hero.HP=hero.HT=1500;hero.belongings.weapon.upgrade(6);
            }
            CavesBossTest.start(g);CavesBossTest.fight(g);count=CavesBossTest.shards();won=g.checkpoint();
        }
        try(CoopSession g=CoopSession.restore(won)) {
            assertTrue(CavesBossTest.level().exitOpen());assertEquals(count,CavesBossTest.shards());
            int waits=0;
            while((g.hero(0).rooted || g.hero(1).rooted || g.hero(0).paralysed>0 || g.hero(1).paralysed>0) && waits++<60) TenguArenaTest.waitOne(g);
            SewerDepthTest.gate(g,false);assertEquals(14,Dungeon.depth);
            SewerDepthTest.gate(g,true);assertNull(CavesBossTest.boss());assertEquals(count,CavesBossTest.shards());
            TenguArenaTest.waitOne(g);
            System.out.println("WON\t"+seed+"\t"+count+"\tRESTORED_RETURNED");
        }
    }
}
