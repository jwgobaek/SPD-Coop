package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Matched native melee hits from the same saved state after a player's actual dart throw. */
public class DartFollowupChecks extends HeadlessDungeon {
    private static void verify(int seat) {
        Bundle saved;
        int targetId;
        try (CoopSession game = CoopSession.open(575, 1, HeroClass.HUNTRESS)) {
            game.join(2, HeroClass.HUNTRESS);
            Rat enemy = CoopMageTest.encounter(game, seat);
            targetId = enemy.id();
            Hero owner = game.hero(seat);
            owner.earnExp(owner.maxExp(), Rat.class);
            SewerBeeTest.offer(game, seat);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.FOLLOWUP_STRIKE.ordinal(), null));
            Dart darts = new Dart();
            darts.quantity(12);
            assertTrue(darts.collect(owner.belongings.backpack));
            game.ownership.claim(darts, game.player(seat));
            for (int attempt = 0; attempt < 10 && owner.buff(Talent.FollowupStrikeTracker.class) == null; attempt++) {
                SewerBeeTest.offer(game, seat);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.THROW, enemy.pos, darts));
            }
            assertEquals(targetId, owner.buff(Talent.FollowupStrikeTracker.class).object);
            assertNull(game.hero(1-seat).buff(Talent.FollowupStrikeTracker.class));
            saved = game.checkpoint();
        }
        int[] damage = new int[2];
        for (int marked = 0; marked <= 1; marked++) {
            try (CoopSession game = CoopSession.restore(saved)) {
                Hero owner = game.hero(seat);
                Rat enemy = (Rat)Actor.findById(targetId);
                CavesBossTest.beside(game, seat, enemy.pos);
                Talent.FollowupStrikeTracker tracker = owner.buff(Talent.FollowupStrikeTracker.class);
                assertEquals(targetId, tracker.object);
                if (marked == 0) tracker.detach();
                int hp = enemy.HP;
                assertTrue(owner.attack(enemy, 1f, 0, Float.POSITIVE_INFINITY));
                damage[marked] = hp - enemy.HP;
                assertNull(owner.buff(Talent.FollowupStrikeTracker.class));
            }
        }
        assertEquals(damage[0] + 2, damage[1]);
        System.out.printf("PASS seat=%d unmarked=%d marked=%d%n", seat, damage[0], damage[1]);
    }

    public static void main(String[] args) throws Exception {
        DartFollowupChecks test = new DartFollowupChecks();
        test.temporary.create();
        test.startBackend();
        try { verify(0); verify(1); }
        finally { test.stopBackend(); test.temporary.delete(); }
    }
}
