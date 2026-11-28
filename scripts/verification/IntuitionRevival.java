package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition.IntuitionUseTracker;
import com.watabou.utils.Bundle;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class IntuitionRevival extends HeadlessDungeon {
    private void trial(int seat) {
        Bundle down;
        int trackerId;
        long stoneId;
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
            game.join(22, HeroClass.WARRIOR);
            var rat = CoopMageTest.encounter(game, seat);
            var owner = game.hero(seat);
            MultiAlchemyTest.pick(game, seat, new Ankh());
            var guess = RuneReadingTest.prepare(game, seat, "guessstone" + seat);
            var stone = owner.belongings.getItem(StoneOfIntuition.class);
            stoneId = game.ownership.identity(stone);
            assertTrue(CoopFloorsTest.step(game, GUESS_ITEM, guess.cell, stone));
            var tracker = owner.buff(IntuitionUseTracker.class);
            assertTrue(tracker.revivePersists);
            trackerId = tracker.id();
            assertEquals(1, stone.quantity());
            assertTrue(owner.belongings.getItem(RingOfHaste.class).isKnown());
            assertNull(game.hero(1-seat).buff(IntuitionUseTracker.class));
            owner.damage(owner.HP + owner.shielding() + 20, rat);
            game.finishEffects();
            assertEquals(CoopSession.Life.DOWNED, game.life(seat));
            down = game.checkpoint();
        }
        try (CoopSession game = CoopSession.restore(down)) {
            var owner = game.hero(seat);
            SewerBeeTest.offer(game, 1-seat);
            int before = game.preDeathHP(seat);
            assertTrue(CoopFloorsTest.step(game, REVIVE, owner.pos, null));
            assertEquals(CoopSession.reviveHP(before, owner.HT), owner.HP);
            assertEquals(trackerId, owner.buff(IntuitionUseTracker.class).id());
            var stone = owner.belongings.getItem(StoneOfIntuition.class);
            assertEquals(stoneId, game.ownership.identity(stone));
            var potion = new PotionOfHaste();
            MultiAlchemyTest.pick(game, seat, potion);
            var candidates = CoopRuneStones.guesses(potion);
            int choice = candidates.indexOf(PotionOfHaste.class);
            assertTrue(choice >= 0);
            int target = RuneReadingTest.selection(owner, potion)*CoopRuneStones.OPTIONS_PER_ITEM + choice;
            assertTrue(CoopFloorsTest.step(game, GUESS_ITEM, target, stone));
            assertTrue(potion.isKnown());
            assertNull(owner.buff(IntuitionUseTracker.class));
            assertNull(owner.belongings.getItem(StoneOfIntuition.class));
            assertNull(game.hero(1-seat).buff(IntuitionUseTracker.class));
            System.out.printf("PASS intuition seat=%d tracker=%d stone=%d persisted through Ankh, second guess consumed stone%n",
                    seat, trackerId, stoneId);
        }
    }

    public static void main(String[] args) throws Exception {
        var probe = new IntuitionRevival();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (int seat=0; seat<2; seat++) probe.trial(seat);
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }
}
