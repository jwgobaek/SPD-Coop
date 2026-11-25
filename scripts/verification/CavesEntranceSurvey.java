package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.Bundle;
import java.util.*;
import static org.junit.Assert.*;

/** Native generation and staged combat are printed as separate observations. No production gates are bypassed. */
public class CavesEntranceSurvey extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        CavesEntranceSurvey survey=new CavesEntranceSurvey();
        survey.temporary.create();
        survey.startBackend();
        try {
            for (int seed=570;seed<=581;seed++) survey.generate(seed);
            Actor.clear();
            Dungeon.level=null;
            Dungeon.hero=null;
            for (int seed=570;seed<=581;seed++) survey.cross(seed);
        } finally {
            survey.stopBackend();
            survey.temporary.delete();
        }
    }

    void generate(int seed) {
        Dungeon.seed=seed;
        Dungeon.init();
        for (int depth=1;depth<=15;depth++) {
            Dungeon.depth=depth;
            Level floor=Dungeon.level=Dungeon.newLevel();
            if (depth<11) continue;
            Set<String> mobs=new TreeSet<>(),rooms=new TreeSet<>(),traps=new TreeSet<>();
            for (Mob mob:floor.mobs) mobs.add(mob.getClass().getSimpleName());
            if (floor instanceof RegularLevel) for (Room room:((RegularLevel)floor).rooms()) rooms.add(room.getClass().getSimpleName());
            for (Trap trap:floor.traps.valueList()) traps.add(trap.getClass().getSimpleName());
            System.out.println("GENERATED\t"+seed+"\t"+depth+"\t"+floor.getClass().getSimpleName()+"\t"+mobs+"\t"+rooms+"\t"+traps);
        }
    }

    void cross(int seed) {
        Bundle saved;
        int actors,parked=0;
        try (CoopSession g=CoopSession.open(seed,11,HeroClass.WARRIOR)) {
            g.join(22,HeroClass.MAGE);
            TenguArenaTest.arrive(g);
            TenguArenaTest.enter(g,false);
            TenguArenaTest.fight(g,PrisonBossLevel.State.FIGHT_START);
            TenguArenaTest.enter(g,true);
            TenguArenaTest.fight(g,PrisonBossLevel.State.FIGHT_ARENA);
            assertEquals(PrisonBossLevel.State.WON,TenguArenaTest.floor().state());
            SewerSpecialActorTest.atGate(g,Dungeon.level.exit());
            int cell=g.stairs.stair();
            assertTrue(cell>=0);
            assertTrue(g.stairs.consent(1-g.activeSeat(),g.turn(),cell,false));
            long[] rng=com.watabou.utils.Random.worldState();
            if (!CoopFloorsTest.step(g,CoopSession.Action.TRAVEL,cell,null)) {
                assertArrayEquals(rng,com.watabou.utils.Random.worldState());
                assertEquals(10,Dungeon.depth);
                assertEquals(PrisonBossLevel.State.WON,TenguArenaTest.floor().state());
                System.out.println("STAGED\t"+seed+"\tWON\tREJECT_FLOOR\t"+g.destinationBlocker);
                return;
            }
            assertEquals(11,Dungeon.depth);
            saved=g.checkpoint();
            Bundle dormant=saved.getBundle("dormant");
            assertEquals(10,dormant.getKeys().size());
            for (String key:dormant.getKeys()) {
                Bundle floor=dormant.getBundle(key).getBundle("level");
                for (Bundle mob:floor.getBundleArray("mobs")) parked+=1+mob.getBundleArray("buffs").length;
                parked+=floor.getBundleArray("blobs").length;
                if (floor.contains("respawner")) parked++;
            }
            actors=Actor.all().size();
        }
        try (CoopSession g=CoopSession.restore(saved)) {
            TenguArenaTest.waitOne(g);
            SewerDepthTest.gate(g,false);
            assertEquals(PrisonBossLevel.State.WON,TenguArenaTest.floor().state());
            TenguArenaTest.waitOne(g);
            System.out.println("STAGED\t"+seed+"\tWON\t11_RESTORED_AND_RETURNED\t"+actors+"\t"+parked);
        }
    }
}
