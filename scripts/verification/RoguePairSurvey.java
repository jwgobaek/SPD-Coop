package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark.DeathMarkTracker;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.watabou.utils.Bundle;

import static org.junit.Assert.*;

public class RoguePairSurvey extends HeadlessDungeon {
    private static void pair(boolean clones) {
        int choice = clones ? 2 : 1;
        int[] identities = new int[2];
        Bundle saved;
        try (CoopSession game = CoopSession.open(572, 1, HeroClass.ROGUE)) {
            game.join(2, HeroClass.ROGUE);
            Rat foe = CoopMageTest.encounter(game, 0);
            SewerBeeTest.offer(game, 0);
            KingsCrown crown = CrownAbilityTest.crown(game, 0, CoopSession.MaskTrial.ROSTER);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.USE_CROWN, choice, crown));
            SewerBeeTest.offer(game, 1);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.CLAIM_ARMOR, choice, null));
            for (int seat = 0; seat < 2; seat++) {
                SewerBeeTest.offer(game, seat);
                Hero hero = game.hero(seat);
                ClassArmor armor = (ClassArmor) hero.belongings.armor();
                float before = armor.charge;
                boolean used = CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, clones ? hero.pos : foe.pos, armor);
                if (clones) {
                    assertTrue(used);
                    ShadowClone.ShadowAlly own = ShadowClone.getShadowAlly(hero);
                    identities[seat] = own.id();
                    assertFalse(own.interact(game.hero(1-seat)));
                } else {
                    assertEquals(seat == 0, used);
                    assertSame(game.hero(0), foe.buff(DeathMarkTracker.class).caster());
                    if (seat == 1) assertEquals(before, armor.charge, 0f);
                }
            }
            saved = game.checkpoint();
        }
        try (CoopSession game = CoopSession.restore(saved)) {
            if (clones) {
                assertNotEquals(identities[0], identities[1]);
                for (int seat=0; seat<2; seat++) assertEquals(identities[seat], ShadowClone.getShadowAlly(game.hero(seat)).id());
            } else {
                assertEquals(1, Dungeon.level.mobs.stream().filter(m -> m.buff(DeathMarkTracker.class) != null).count());
            }
        }
        System.out.println(clones ? "PASS simultaneous owner-bound clones after restore" : "PASS one live mark, second charge retained");
    }
    public static void main(String[] args) throws Exception {
        RoguePairSurvey p = new RoguePairSurvey();
        p.temporary.create();
        p.startBackend();
        try { pair(false); pair(true); }
        finally { p.stopBackend(); p.temporary.delete(); }
    }
}
