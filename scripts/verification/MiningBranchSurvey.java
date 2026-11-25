package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import static org.junit.Assert.*;

/** Inspect the actual upstream branch without admitting it to the cooperative scheduler. */
public class MiningBranchSurvey extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        MiningBranchSurvey probe = new MiningBranchSurvey();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (int seed = 570; seed <= 581; seed++) inspect(seed);
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }

    static void inspect(int seed) {
        Dungeon.seed = seed;
        Dungeon.init();
        for (int depth = 1; depth <= 14; depth++) {
            Dungeon.depth = depth;
            Level main = Dungeon.level = Dungeon.newLevel();
            if (main.mobs.stream().noneMatch(Blacksmith.class::isInstance)) continue;
            int type = Blacksmith.Quest.Type();
            assertTrue(type == Blacksmith.Quest.CRYSTAL || type == Blacksmith.Quest.GNOLL);
            LevelTransition gate = main.getTransition(LevelTransition.Type.BRANCH_EXIT);
            assertEquals(depth, gate.destDepth);
            assertEquals(1, gate.destBranch);
            Dungeon.branch = gate.destBranch;
            Level mine = Dungeon.level = Dungeon.newLevel();
            assertTrue(mine instanceof MiningLevel);
            LevelTransition back = mine.getTransition(gate.destType);
            assertEquals(0, back.destBranch);
            assertEquals(depth, back.destDepth);
            Set<String> residents = new TreeSet<>();
            for (Mob mob : mine.mobs) residents.add(mob.getClass().getSimpleName());
            long veins = Arrays.stream(mine.map).filter(t -> t == Terrain.WALL_DECO).count();
            System.out.println("MINE\t" + seed + "\t" + depth + "\t" + (type == Blacksmith.Quest.CRYSTAL ? "CRYSTAL" : "GNOLL")
                    + "\t" + gate.cell() + "\t" + back.cell() + "\t" + veins + "\t" + residents);
            return;
        }
        fail("Missing native BlacksmithRoom for seed " + seed);
    }
}
