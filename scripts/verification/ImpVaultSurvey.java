package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import java.util.Set;
import java.util.TreeSet;
import static org.junit.Assert.*;

/** Native generation only. Does not enable the co-op vault or mutate either co-op inventory. */
public class ImpVaultSurvey extends HeadlessDungeon {
    private static void inspect(int seed) {
        Dungeon.seed=seed;
        Dungeon.init();
        for (int depth=1;depth<=19;depth++) {
            Dungeon.depth=depth;
            Level world=Dungeon.level=Dungeon.newLevel();
            if (depth<16 || world.mobs.stream().noneMatch(Imp.class::isInstance)) continue;
            LevelTransition entrance=world.getTransition(LevelTransition.Type.BRANCH_EXIT);
            assertNotNull(entrance);
            assertEquals(depth,entrance.destDepth);
            assertEquals(1,entrance.destBranch);
            assertTrue(Imp.Quest.acceptCurrent());
            Dungeon.branch=1;
            VaultLevel vault=(VaultLevel)(Dungeon.level=Dungeon.newLevel());
            Set<String> rooms=new TreeSet<>(),mobs=new TreeSet<>(),traps=new TreeSet<>();
            for (Room room:vault.rooms()) rooms.add(room.getClass().getSimpleName());
            for (Mob mob:vault.mobs) mobs.add(mob.getClass().getSimpleName());
            for (Trap trap:vault.traps.valueList()) traps.add(trap.getClass().getSimpleName());
            assertTrue(rooms.contains("VaultEntranceRoom"));
            assertTrue(rooms.contains("VaultFinalRoom"));
            System.out.println("VAULT\t"+seed+"\t"+depth+"\t"+entrance.cell()+"\t"+vault.width()+"x"+vault.height()
                    +"\t"+rooms+"\t"+mobs+"\t"+traps+"\t"+vault.heaps.valueList().size());
            return;
        }
        throw new AssertionError("Missing native imp on seed "+seed);
    }
    public static void main(String[] args) throws Exception {
        ImpVaultSurvey test=new ImpVaultSurvey();test.temporary.create();test.startBackend();
        try {
            for(int seed=570;seed<=581;seed++) inspect(seed);
        } finally { test.stopBackend();test.temporary.delete(); }
    }
}
