package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.watabou.utils.Bundle;
import java.util.function.Supplier;
import static org.junit.Assert.*;

/** Exercise native floor-potion destruction separately from a player's DRINK or THROW. */
public final class EnvironmentalPotionSurvey extends HeadlessDungeon {
    private static void shatter(Supplier<Potion> factory, Class<? extends Blob> gas, int seat) {
        Bundle save;
        int cell, amount, id;
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
            game.join(22, HeroClass.HUNTRESS);
            CoopMageTest.encounter(game, seat);
            cell = game.hero(seat).pos + 1;
            Potion.initColors();
            Potion potion = factory.get();
            Heap heap = Dungeon.level.drop(potion, cell);
            assertFalse(potion.isKnown());
            assertSame(potion, heap.peek());
            assertFalse(CoopConsumables.throwablePotion(potion));
            assertEquals(0, Blob.volumeAt(cell, gas));
            heap.explode();
            assertNull(Dungeon.level.heaps.get(cell));
            assertTrue(potion.isKnown());
            amount = Blob.volumeAt(cell, gas);
            assertTrue(amount > 0);
            id = Dungeon.level.blobs.get(gas).id();
            save = game.checkpoint();
        }
        try (CoopSession restored = CoopSession.restore(save)) {
            assertNull(Dungeon.level.heaps.get(cell));
            assertEquals(amount, Blob.volumeAt(cell, gas));
            assertEquals(id, Dungeon.level.blobs.get(gas).id());
            assertTrue(factory.get().isKnown());
            System.out.println("PASS floor shatter " + gas.getSimpleName() + " seat=" + seat
                    + " quantity=" + amount + " restoredActor=" + id);
        }
    }

    public static void main(String[] args) throws Exception {
        EnvironmentalPotionSurvey runner = new EnvironmentalPotionSurvey();
        runner.temporary.create();
        runner.startBackend();
        try {
            for (int seat = 0; seat < 2; seat++) {
                shatter(PotionOfLiquidFlame::new, Fire.class, seat);
                shatter(PotionOfFrost::new, Freezing.class, seat);
                shatter(PotionOfToxicGas::new, ToxicGas.class, seat);
                shatter(PotionOfParalyticGas::new, ParalyticGas.class, seat);
            }
        } finally {
            runner.stopBackend();
            runner.temporary.delete();
        }
    }
}
