package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Two actual branches in one seed and one save generation, using staged cooperative arrivals. */
public class VaultMineCoexistence extends HeadlessDungeon {
    static void branch(CoopSession g,LevelTransition.Type type) {
        MiningBranchTest.atBranchGate(g,Dungeon.level.getTransition(type).cell());
        CoopFloorsTest.cross(g);
    }
    public static void main(String[] args) throws Exception {
        VaultMineCoexistence probe=new VaultMineCoexistence();
        probe.temporary.create();probe.startBackend();
        try {
            Bundle saved;
            try (CoopSession g=CavesInteriorTest.open(572)) {
                CavesInteriorTest.arrive(g,14);
                Blacksmith smith=SewerSpecialActorTest.actor(Blacksmith.class);
                MiningBranchTest.standBeside(g,smith.pos);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ACCEPT_BLACKSMITH,smith.pos,null));
                branch(g,LevelTransition.Type.BRANCH_EXIT);
                assertEquals(Blacksmith.Quest.GNOLL,Blacksmith.Quest.Type());
                branch(g,LevelTransition.Type.BRANCH_ENTRANCE);
                assertTrue(Blacksmith.Quest.completed());
                SewerDepthTest.gate(g,true);
                for (int seat=0;seat<2;seat++) {
                    g.hero(seat).HT=g.hero(seat).HP=1500;
                    g.hero(seat).belongings.weapon.upgrade(6);
                }
                CavesBossTest.start(g);CavesBossTest.fight(g);
                while (Dungeon.depth<18) { CityEntranceTest.stageAtExit(g);CoopFloorsTest.cross(g); }
                Imp imp=SewerSpecialActorTest.actor(Imp.class);
                MiningBranchTest.standBeside(g,imp.pos);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ACCEPT_IMP,imp.pos,null));
                branch(g,LevelTransition.Type.BRANCH_EXIT);
                assertEquals(1,Dungeon.branch);
                saved=g.checkpoint();
                assertTrue(saved.getBundle("dormant").contains("1014"));
                assertTrue(saved.getBundle("dormant").contains("18"));
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                assertTrue(new DwarfToken().collect(g.hero(g.activeSeat()).belongings.backpack));
                g.observe(0);g.observe(1);
                CoopFloorsTest.cross(g);
                saved=g.checkpoint();
                Bundle parked=saved.getBundle("dormant");
                String mine=parked.getBundle("1014").getBundle("level").getString("__className");
                String vault=parked.getBundle("1018").getBundle("level").getString("__className");
                assertTrue(mine.endsWith("MiningLevel") && vault.endsWith("VaultLevel"));
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                assertTrue(Blacksmith.Quest.completed() && Imp.Quest.isCompleted());
                assertEquals(18,Dungeon.depth);
                assertEquals(0,Dungeon.branch);
                TenguArenaTest.waitOne(g);
                System.out.println("PASS seed=572 mine=1/14 vault=1/18 active=0/18 double_restore=true both_quests_complete=true");
            }
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
