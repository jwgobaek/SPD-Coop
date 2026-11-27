package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.*;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CauldronRecipeMatrix extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        Map<Class<? extends Scroll>, Class<? extends Runestone>> expected = new LinkedHashMap<>();
        expected.put(ScrollOfIdentify.class, StoneOfIntuition.class);
        expected.put(ScrollOfLullaby.class, StoneOfDeepSleep.class);
        expected.put(ScrollOfMagicMapping.class, StoneOfClairvoyance.class);
        expected.put(ScrollOfMirrorImage.class, StoneOfFlock.class);
        expected.put(ScrollOfRetribution.class, StoneOfBlast.class);
        expected.put(ScrollOfRage.class, StoneOfAggression.class);
        expected.put(ScrollOfRecharging.class, StoneOfShock.class);
        expected.put(ScrollOfRemoveCurse.class, StoneOfDetectMagic.class);
        expected.put(ScrollOfTeleportation.class, StoneOfBlink.class);
        expected.put(ScrollOfTerror.class, StoneOfFear.class);
        expected.put(ScrollOfTransmutation.class, StoneOfAugmentation.class);
        expected.put(ScrollOfUpgrade.class, StoneOfEnchantment.class);
        CauldronRecipeMatrix trial = new CauldronRecipeMatrix();
        trial.temporary.create();
        trial.startBackend();
        try {
            for (int seat = 0; seat < 2; seat++) {
                for (var entry : expected.entrySet()) trial.brew(seat, entry.getKey(), entry.getValue());
            }
        } finally {
            trial.stopBackend();
            trial.temporary.delete();
        }
    }

    private void brew(int seat, Class<? extends Scroll> scrollType, Class<? extends Runestone> outputType) {
        Bundle saved;
        long stackId;
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
            game.join(22, HeroClass.WARRIOR);
            CoopMageTest.encounter(game, seat);
            Hero owner = game.hero(seat);
            int pot = owner.pos + 1;
            Level.set(pot, Terrain.ALCHEMY);
            game.observe(seat);
            SewerBeeTest.offer(game, seat);
            Scroll material = Reflection.newInstance(scrollType);
            Scroll previous = owner.belongings.getItem(scrollType);
            int initial = previous == null ? 0 : previous.quantity();
            Dungeon.level.drop(material, owner.pos);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.PICK_UP, owner.pos, material));
            SewerBeeTest.offer(game, seat);
            Scroll stack = owner.belongings.getItem(scrollType);
            assertEquals(initial + 1, stack.quantity());
            assertEquals(game.player(seat), game.ownership.owner(stack));
            int energy = Dungeon.energy;
            String preview = CoopAlchemy.preview(stack);
            assertEquals(stack.isKnown() ? outputType.getSimpleName() : "unknown", preview.split(",")[0]);
            assertTrue(preview.endsWith(",2,0"));
            assertTrue(ProjectionChecks.seatImage(game, seat).source.rows.containsValue(preview));
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.BREW_RUNESTONES, pot, stack));
            assertEquals(energy, Dungeon.energy);
            assertTrue(stack.isKnown());
            Scroll left = owner.belongings.getItem(scrollType);
            assertEquals(initial, left == null ? 0 : left.quantity());
            Runestone result = owner.belongings.getItem(outputType);
            assertEquals(2, result.quantity());
            stackId = game.ownership.identity(result);
            saved = game.checkpoint();
        }
        try (CoopSession game = CoopSession.restore(saved)) {
            Runestone result = game.hero(seat).belongings.getItem(outputType);
            assertEquals(stackId, game.ownership.identity(result));
            assertEquals(2, result.quantity());
            assertEquals(game.player(seat), game.ownership.owner(result));
            assertNull(game.hero(1-seat).belongings.getItem(outputType));
            System.out.println("PASS seat=" + seat + " " + scrollType.getSimpleName() + " -> 2 " + outputType.getSimpleName());
        }
    }
}
