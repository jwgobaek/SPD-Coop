package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;

import static org.junit.Assert.*;

/** Check the public data consumed by the native inventory detail window before and after reading. */
public final class UnidentifiedScrollLabels extends HeadlessDungeon {
    private static PublicDungeon.Loot scroll(PublicDungeon view) {
        return view.belongings.stream().filter(item -> item.kind.equals("READ_SCROLL")
                || item.kind.equals("RECHARGING_SCROLL") || item.kind.equals("LULLABY_SCROLL")).findFirst().orElseThrow();
    }

    private static void check(int seat, boolean lullaby) throws Exception {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
            game.join(22, HeroClass.WARRIOR);
            MultiAlchemyTest.station(game, seat);
            Scroll original = lullaby ? new ScrollOfLullaby() : new ScrollOfRecharging();
            MultiAlchemyTest.pick(game, seat, original.quantity(2));
            PublicDungeon view = ProjectionChecks.decodedSeat(game, seat);
            PublicDungeon.Loot unknown = scroll(view);
            assertFalse(original.isKnown());
            assertEquals("READ_SCROLL", unknown.kind);
            assertFalse(view.source.rows.containsKey("label" + unknown.id));
            assertTrue(view.source.rows.get("brew" + unknown.id).startsWith("unknown,"));
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.READ_SCROLL, 0, original));
            assertEquals(1, original.quantity());
            assertTrue(original.isKnown());
            view = ProjectionChecks.decodedSeat(game, seat);
            PublicDungeon.Loot known = scroll(view);
            assertEquals(lullaby ? "items.scrolls.scrolloflullaby" : "items.scrolls.scrollofrecharging",
                    view.source.rows.get("label" + known.id));
            System.out.printf("PASS seat=%d type=%s generic before reading, native name after reading%n",
                    seat, original.getClass().getSimpleName());
        }
    }

    public static void main(String[] args) throws Exception {
        UnidentifiedScrollLabels fixture = new UnidentifiedScrollLabels();
        fixture.temporary.create();
        fixture.startBackend();
        try {
            for (int seat = 0; seat < 2; seat++) {
                check(seat, true);
                check(seat, false);
            }
        } finally {
            fixture.stopBackend();
            fixture.temporary.delete();
        }
    }
}
