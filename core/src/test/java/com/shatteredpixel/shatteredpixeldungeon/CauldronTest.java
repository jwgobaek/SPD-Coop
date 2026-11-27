package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDeepSleep;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import org.junit.Test;

import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class CauldronTest extends HeadlessDungeon {
    static StagedInput prepare(CoopSession game, int seat) {
        CoopMageTest.encounter(game, seat);
        Hero owner = game.hero(seat);
        int pot = owner.pos + 1;
        Level.set(pot, Terrain.ALCHEMY);
        game.observe(seat);
        SewerBeeTest.offer(game, seat);
        ScrollOfLullaby scroll = new ScrollOfLullaby();
        Dungeon.level.drop(scroll, owner.pos);
        assertTrue(CoopFloorsTest.step(game, PICK_UP, owner.pos, scroll));
        SewerBeeTest.offer(game, seat);
        assertEquals(game.player(seat), game.ownership.owner(scroll));
        return new StagedInput(game, "BREW_RUNESTONES", pot);
    }

    static boolean published(CoopSession game, int seat) {
        Hero owner = game.hero(seat);
        StoneOfDeepSleep stones = owner.belongings.getItem(StoneOfDeepSleep.class);
        return stones != null && stones.quantity() == 2 && game.ownership.owner(stones) == game.player(seat)
                && owner.belongings.getItem(ScrollOfLullaby.class) == null && new ScrollOfLullaby().isKnown();
    }

    @Test public void nativeRecipeConsumesOneOwnedScrollAndRestoresTheResultStack() {
        for (int seat = 0; seat < 2; seat++) {
            Bundle save;
            long outputId;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
                game.join(22, HeroClass.WARRIOR);
                int pot = prepare(game, seat).cell;
                Hero owner = game.hero(seat);
                ScrollOfLullaby scroll = owner.belongings.getItem(ScrollOfLullaby.class);
                assertFalse(scroll.isKnown());
                long scrollId = game.ownership.identity(scroll);
                long[] random = game.checkpoint().getLongArray("random");
                assertTrue(CoopAlchemy.preview(scroll).startsWith("unknown,"));
                assertTrue(CoopAlchemy.preview(scroll).endsWith(",2,0"));
                assertArrayEquals(random, game.checkpoint().getLongArray("random"));
                assertFalse(CoopFloorsTest.step(game, BREW_RUNESTONES, owner.pos, scroll));
                assertFalse(CoopAlchemy.brew(game, game.hero(1-seat), pot, scroll));
                long turn = game.turn();
                int energy = Dungeon.energy;
                assertTrue(CoopFloorsTest.step(game, BREW_RUNESTONES, pot, scroll));
                assertEquals(energy, Dungeon.energy);
                assertEquals(turn, game.turn());
                assertTrue(published(game, seat));
                outputId = game.ownership.identity(owner.belongings.getItem(StoneOfDeepSleep.class));
                assertNotEquals(scrollId, outputId);
                assertFalse(CoopFloorsTest.step(game, BREW_RUNESTONES, pot, scroll));
                save = game.checkpoint();
            }
            try (CoopSession restored = CoopSession.restore(save)) {
                assertTrue(published(restored, seat));
                assertEquals(outputId, restored.ownership.identity(restored.hero(seat).belongings.getItem(StoneOfDeepSleep.class)));
                assertTrue(CoopFloorsTest.step(restored, WAIT, 0, null));
            }
        }
    }

    static StagedInput unknownScroll(CoopSession game, int seat) {
        prepare(game, seat);
        return new StagedInput(game, "READ_SCROLL", 0);
    }

    static boolean readUnknown(CoopSession game, int seat) {
        return new ScrollOfLullaby().isKnown() && game.hero(seat).belongings.getItem(ScrollOfLullaby.class) == null
                && game.hero(seat).buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy.class) != null
                && game.hero(1-seat).buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy.class) == null;
    }

    @Test public void unidentifiedLullabyExposesOnlyGenericReadingBeforeTheNativeEffect() {
        for (int seat = 0; seat < 2; seat++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
                game.join(22, HeroClass.WARRIOR);
                unknownScroll(game, seat);
                ScrollOfLullaby scroll = game.hero(seat).belongings.getItem(ScrollOfLullaby.class);
                assertFalse(scroll.isKnown());
                var own = com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks.seatImage(game, seat);
                var item = own.belongings.stream().filter(i -> own.source.rows.getOrDefault("brew" + i.id, "").startsWith("unknown,")).findFirst().get();
                assertTrue(own.actions(item).contains("READ_SCROLL"));
                assertFalse(own.actions(item).contains("READ_LULLABY"));
                assertFalse(CoopFloorsTest.step(game, READ_LULLABY, 0, scroll));
                assertTrue(CoopFloorsTest.step(game, READ_SCROLL, 0, scroll));
                assertTrue(readUnknown(game, seat));
            }
        }
    }

    @Test public void twoSeatsAtOneStationCommitOnlyTheirOwnPhysicalStackOnTheirOffer() {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
            game.join(22, HeroClass.WARRIOR);
            int pot = prepare(game, 0).cell;
            Hero host = game.hero(0);
            Hero guest = game.hero(1);
            ScrollOfLullaby first = host.belongings.getItem(ScrollOfLullaby.class);
            ScrollOfLullaby second = new ScrollOfLullaby();
            Dungeon.level.drop(second, guest.pos);
            SewerBeeTest.offer(game, 1);
            assertTrue(CoopFloorsTest.step(game, PICK_UP, guest.pos, second));
            game.observe(0);
            game.observe(1);
            assertTrue(CoopAlchemy.atStation(game, host, pot));
            assertTrue(CoopAlchemy.atStation(game, guest, pot));
            int energy = Dungeon.energy;
            SewerBeeTest.offer(game, 0);
            assertFalse(game.act(game.player(1), game.turn(), BREW_RUNESTONES, pot, second));
            assertFalse(CoopFloorsTest.step(game, BREW_RUNESTONES, pot, second));
            assertTrue(CoopFloorsTest.step(game, BREW_RUNESTONES, pot, first));
            assertTrue(guest.belongings.contains(second));
            SewerBeeTest.offer(game, 1);
            assertTrue(CoopFloorsTest.step(game, BREW_RUNESTONES, pot, second));
            assertTrue(published(game, 0));
            assertTrue(published(game, 1));
            assertEquals(energy, Dungeon.energy);
            assertNotEquals(game.ownership.identity(host.belongings.getItem(StoneOfDeepSleep.class)),
                    game.ownership.identity(guest.belongings.getItem(StoneOfDeepSleep.class)));
        }
    }

    @Test public void fullPackDropsOneBoundStackAndLaterMergesIntoItsPhysicalIdentity() {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
            game.join(22, HeroClass.WARRIOR);
            int pot = prepare(game, 0).cell;
            Hero owner = game.hero(0);
            ScrollOfLullaby scroll = owner.belongings.getItem(ScrollOfLullaby.class);
            scroll.quantity(3);
            Bag pack = owner.belongings.backpack;
            for (var item : new java.util.ArrayList<>(pack.items)) {
                if (item instanceof Bag) item.detachAll(pack);
            }
            while (new Ankh().collect(pack)) { }
            assertTrue(CoopFloorsTest.step(game, BREW_RUNESTONES, pot, scroll));
            assertEquals(2, scroll.quantity());
            StoneOfDeepSleep stack = (StoneOfDeepSleep) Dungeon.level.heaps.get(owner.pos).peek();
            assertEquals(2, stack.quantity());
            assertEquals(game.player(0), game.ownership.owner(stack));
            assertFalse(stack.collect(game.hero(1).belongings.backpack));
            long id = game.ownership.identity(stack);
            owner.belongings.getItem(Ankh.class).detachAll(pack);
            assertTrue(CoopFloorsTest.step(game, PICK_UP, owner.pos, stack));
            SewerBeeTest.offer(game, 0);
            assertTrue(CoopFloorsTest.step(game, BREW_RUNESTONES, pot, scroll));
            assertEquals(1, scroll.quantity());
            assertSame(stack, owner.belongings.getItem(StoneOfDeepSleep.class));
            assertEquals(4, stack.quantity());
            assertEquals(id, game.ownership.identity(stack));
        }
    }
}
