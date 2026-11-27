package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.CorrosionTrap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class CorrosionAttributionTest extends HeadlessDungeon {
    static StagedInput prepareGasTick(CoopSession game, int seat) {
        CoopMageTest.encounter(game, seat);
        Hero owner = game.hero(seat);
        owner.HP = owner.HT = 140;
        new WandOfCorrosion().onZap(owner, new Ballistica(owner.pos, owner.pos, Ballistica.STOP_TARGET));
        for (int i = 0; i < 8 && owner.buff(Corrosion.class) == null; i++) TenguArenaTest.waitOne(game);
        SewerBeeTest.offer(game, seat);
        assertTrue(publishedGasTick(game, seat));
        return new StagedInput(game, "WAIT", 0);
    }

    static boolean publishedGasTick(CoopSession game, int seat) {
        Hero owner = game.hero(seat);
        CorrosiveGas gas = (CorrosiveGas) Dungeon.level.blobs.get(CorrosiveGas.class);
        if (gas == null || gas.volume == 0) return false;
        Bundle state = new Bundle();
        gas.storeInBundle(state);
        Corrosion corrosion = owner.buff(Corrosion.class);
        return state.getInt("coop_caster") == owner.id() && state.getClass("source") == WandOfCorrosion.class
                && corrosion != null && corrosion.caster() == owner;
    }

    static String gasSignature(CoopSession game, int seat) {
        CorrosiveGas gas = (CorrosiveGas) Dungeon.level.blobs.get(CorrosiveGas.class);
        Bundle state = new Bundle();
        gas.storeInBundle(state);
        Hero owner = game.hero(seat);
        return gas.id() + "/" + gas.volume + "/" + java.util.Arrays.hashCode(gas.cur) + "/"
                + state.getInt("strength") + "/" + state.getInt("coop_caster") + "/" + owner.HP
                + "/" + owner.buff(Corrosion.class).id();
    }

    @Test public void theNativeGasPoolKeepsTheFirstCasterAtEqualStrengthAndTheHighestCauseOnRestore() {
        for (int seat = 0; seat < 2; seat++) {
            Bundle save;
            int gasId;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
                game.join(22, HeroClass.MAGE);
                CoopMageTest.encounter(game, seat);
                Hero caster = game.hero(seat);
                Hero peer = game.hero(1-seat);
                caster.HP = caster.HT = peer.HP = peer.HT = 140;
                WandOfCorrosion wand = new WandOfCorrosion();
                wand.onZap(caster, new Ballistica(caster.pos, caster.pos, Ballistica.STOP_TARGET));
                CorrosiveGas gas = (CorrosiveGas)Dungeon.level.blobs.get(CorrosiveGas.class);
                gasId = gas.id();
                assertEquals(50, gas.volume);
                wand.onZap(peer, new Ballistica(peer.pos, peer.pos, Ballistica.STOP_TARGET));
                assertEquals(100, gas.volume);
                Bundle gasState = new Bundle();
                gas.storeInBundle(gasState);
                assertEquals(caster.id(), gasState.getInt("coop_caster"));
                gas.setStrength(4, CorrosionTrap.class);
                gasState = new Bundle();
                gas.storeInBundle(gasState);
                assertEquals(0, gasState.getInt("coop_caster"));
                assertEquals(CorrosionTrap.class, gasState.getClass("source"));
                wand.upgrade(3);
                wand.onZap(peer, new Ballistica(peer.pos, peer.pos, Ballistica.STOP_TARGET));
                save = game.checkpoint();
                assertEquals(27, save.getInt("schema"));
                assertTrue(save.getInt("corrosion_schema") > 0);
            }
            try (CoopSession restored = CoopSession.restore(save)) {
                Hero peer = restored.hero(1-seat);
                CorrosiveGas gas = (CorrosiveGas)Dungeon.level.blobs.get(CorrosiveGas.class);
                assertEquals(gasId, gas.id());
                Bundle state = new Bundle();
                gas.storeInBundle(state);
                assertEquals(5, state.getInt("strength"));
                assertEquals(peer.id(), state.getInt("coop_caster"));
                assertEquals(WandOfCorrosion.class, state.getClass("source"));
                for (int i = 0; i < 8; i++) TenguArenaTest.waitOne(restored);
                for (int i = 0; i < 2; i++) {
                    Hero victim = restored.hero(i);
                    assertTrue(victim.HP < victim.HT);
                    Corrosion dot = victim.buff(Corrosion.class);
                    assertNotNull(dot);
                    assertSame(peer, dot.caster());
                }
            }
        }
    }

    @Test public void aLethalCorrosionTickCreditsOnlyItsLivingSource() {
        for (int seat = 0; seat < 2; seat++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
                game.join(22, HeroClass.MAGE);
                Rat rat = CoopMageTest.encounter(game, seat);
                Hero owner = game.hero(seat);
                int exp = owner.exp;
                int other = game.hero(1-seat).exp;
                rat.HP = 1;
                Corrosion dot = Buff.affect(rat, Corrosion.class);
                dot.set(2f, 2, WandOfCorrosion.class, owner.id());
                dot.act();
                assertFalse(rat.isAlive());
                assertTrue(rat.wasDefeatedBy(owner));
                assertEquals(exp + rat.EXP, owner.exp);
                assertEquals(other, game.hero(1-seat).exp);
            }
        }
    }
}
