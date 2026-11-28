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

    private static void originalDrink(Supplier<Potion> factory, Class<? extends Blob> hazard, int seat) {
        Bundle saved;
        int cell, actor, amount;
        long item;
        try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.HUNTRESS);
            CoopMageTest.encounter(game,seat);
            Potion.initColors();
            var owner=game.hero(seat);
            Potion bottle=factory.get();
            bottle.quantity(2);
            MultiAlchemyTest.pick(game,seat,bottle);
            cell=owner.pos;
            item=game.ownership.identity(bottle);
            assertFalse(bottle.isKnown());
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.DRINK,0,bottle));
            assertEquals(2,bottle.quantity());
            bottle.execute(owner,Potion.AC_DRINK);
            assertTrue(bottle.isKnown());
            assertEquals(1,bottle.quantity());
            // The isolated native drink spends time outside a room request. Take a real
            // cooperative wait so the checkpoint has a newly scheduled input offer.
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WAIT,owner.pos,null));
            amount=Blob.volumeAt(cell,hazard);
            assertTrue(amount>0);
            actor=Dungeon.level.blobs.get(hazard).id();
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            Potion remaining=game.hero(seat).belongings.getItem(factory.get().getClass());
            assertEquals(1,remaining.quantity());
            assertTrue(remaining.isKnown());
            assertEquals(item,game.ownership.identity(remaining));
            assertEquals(game.player(seat),game.ownership.owner(remaining));
            assertEquals(amount,Blob.volumeAt(cell,hazard));
            assertEquals(actor,Dungeon.level.blobs.get(hazard).id());
            System.out.printf("PASS isolated original drink %s seat=%d cell=%d amount=%d actor=%d originalStack=%d%n",
                    hazard.getSimpleName(),seat,cell,amount,actor,item);
        }
    }

    public static void main(String[] args) throws Exception {
        EnvironmentalPotionSurvey runner = new EnvironmentalPotionSurvey();
        runner.temporary.create();
        runner.startBackend();
        try {
            for (int seat = 0; seat < 2; seat++) {
                originalDrink(PotionOfLiquidFlame::new, Fire.class, seat);
                originalDrink(PotionOfFrost::new, Freezing.class, seat);
                originalDrink(PotionOfToxicGas::new, ToxicGas.class, seat);
                originalDrink(PotionOfParalyticGas::new, ParalyticGas.class, seat);
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
