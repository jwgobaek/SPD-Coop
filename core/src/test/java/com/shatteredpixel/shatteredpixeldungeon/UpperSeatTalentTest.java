package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpperSeatTalentTest extends HeadlessDungeon {
    @Test public void nativeUpperTalentEffectsUseTheInvestingHeroAtEveryRank() {
        for (int seat = 0; seat < 2; seat++) {
            verify(seat, HeroClass.WARRIOR, HeroSubClass.BERSERKER, Talent.STRONGMAN);
            verify(seat, HeroClass.WARRIOR, HeroSubClass.BERSERKER, Talent.ENDLESS_RAGE);
            verify(seat, HeroClass.MAGE, HeroSubClass.BATTLEMAGE, Talent.DESPERATE_POWER);
            verify(seat, HeroClass.HUNTRESS, HeroSubClass.SNIPER, Talent.POINT_BLANK);
        }
    }

    private static void verify(int seat, HeroClass type, HeroSubClass subclass, Talent talent) {
        Bundle saved;
        try (CoopSession game = CoopSession.open(572, 11, type)) {
            game.join(22, type);
            SewerBeeTest.offer(game, seat);
            Hero hero = game.hero(seat);
            hero.lvl = 25;
            assertFalse(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
            hero.subClass = subclass;
            Talent.initSubclassTalents(hero);
            for (int rank = 0; rank <= 3; rank++) {
                if (rank > 0)
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
                assertEquals(Integer.toString(rank), ProjectionChecks.seatImage(game,seat).source.rows.get("learn"+talent.ordinal()));
                checkEffect(hero, talent, rank);
                assertEquals(0, game.hero(1-seat).pointsInTalent(talent));
            }
            assertFalse(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
            saved = game.checkpoint();
        }
        try (CoopSession restored = CoopSession.restore(saved)) {
            checkEffect(restored.hero(seat), talent, 3);
            assertEquals(0, restored.hero(1-seat).pointsInTalent(talent));
        }
    }

    private static void checkEffect(Hero hero, Talent talent, int rank) {
        switch (talent) {
            case STRONGMAN:
                assertEquals(hero.STR + (rank == 0 ? 0 : (int)Math.floor(hero.STR*(.03f+.05f*rank))), hero.STR());
                break;
            case ENDLESS_RAGE:
                Berserk rage = Buff.affect(hero, Berserk.class);
                rage.damage(10*hero.HT);
                Bundle state = new Bundle();
                rage.storeInBundle(state);
                assertEquals(1f+.1667f*rank, state.getFloat("power"), .0001f);
                break;
            case DESPERATE_POWER:
                Wand wand = ((MagesStaff)hero.belongings.weapon()).imbuedWand();
                wand.curCharges = 2;
                int ordinary = wand.buffedLvl();
                wand.curCharges = 1;
                assertEquals(ordinary+rank, wand.buffedLvl());
                break;
            case POINT_BLANK:
                ThrowingStone missile = new ThrowingStone();
                Rat rat = new Rat();
                rat.pos = hero.pos + 2;
                float ranged = missile.accuracyFactor(hero, rat);
                rat.pos = hero.pos + 1;
                assertEquals(ranged / 1.5f * (.5f + .25f*rank), missile.accuracyFactor(hero, rat), .0001f);
                break;
            default: throw new AssertionError(talent);
        }
    }
}
