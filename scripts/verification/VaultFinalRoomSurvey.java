package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ImpStatue;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.watabou.utils.Bundle;
import java.lang.reflect.Field;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Original room generation and an isolated replay audit, prior to enabling the final chamber. */
public class VaultFinalRoomSurvey extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        VaultFinalRoomSurvey probe=new VaultFinalRoomSurvey();
        probe.temporary.create();
        probe.startBackend();
        try (CoopSession game=CavesInteriorTest.open(572)) {
            ImpVaultTest.entrance(game);
            CoopFloorsTest.cross(game);
            assertTrue(Dungeon.level instanceof VaultLevel);
            int statues=0,statueCell=-1;
            for (Heap heap:Dungeon.level.heaps.valueList()) {
                for (Item item:heap.items) if (item instanceof ImpStatue) {
                    statues+=item.quantity();
                    statueCell=heap.pos;
                }
            }
            assertEquals(1,statues);
            assertFalse(Dungeon.level.locked);
            System.out.println("PASS seed=572 vault="+Dungeon.level.width()+"x"+Dungeon.level.height()
                    +" ImpStatue already on native floor before boss="+statues+" cell="+statueCell);

            Field weights=VaultBossElemental.class.getDeclaredField("formChances");
            weights.setAccessible(true);
            VaultBossElemental original=new VaultBossElemental();
            Bundle record=new Bundle();
            original.storeInBundle(record);
            // Sample different constructor rolls. The original restores the form and clocks;
            // future form weights must also match for a deterministic cooperative restart.
            boolean diverged=false;
            for (int i=0;i<64;i++) {
                VaultBossElemental restored=new VaultBossElemental();
                restored.restoreFromBundle(record);
                assertEquals(original.curForm(),restored.curForm());
                assertEquals(original.HP,restored.HP);
                if (!Arrays.equals((float[])weights.get(original),(float[])weights.get(restored))) {
                    diverged=true;
                    break;
                }
            }
            System.out.println("Native elemental form weight replay diverged="+diverged);
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }
}
