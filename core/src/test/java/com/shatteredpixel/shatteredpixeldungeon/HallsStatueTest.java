package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Brimstone;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class HallsStatueTest extends HeadlessDungeon {
    private void encounter(int seed, int depth, Class<? extends Weapon.Enchantment> effect) {
        Bundle parked;
        int id;
        long weaponId;
        long armorId = 0;
        try (CoopSession game = CavesInteriorTest.open(seed)) {
            if (depth <= 19) CityKingTest.reach(game, depth);
            else {
                CityKingTest.prepareBoundary(game, "claim");
                while (Dungeon.depth < depth) SewerDepthTest.gate(game, true);
            }
            Statue statue = Dungeon.level.mobs.stream().filter(Statue.class::isInstance)
                    .map(Statue.class::cast).filter(s -> s.weapon().enchantment.getClass() == effect)
                    .findFirst().orElseThrow(AssertionError::new);
            id = statue.id();
            Weapon weapon = statue.weapon();
            weaponId = game.ownership.identity(weapon);
            if (statue instanceof ArmoredStatue) armorId = game.ownership.identity(((ArmoredStatue)statue).armor());
            assertEquals(effect, weapon.enchantment.getClass());
            assertEquals(weapon.delayFactor(statue), statue.attackDelay(), 0.001f);
            for (int seat = 0; seat < 2; seat++) {
                Hero target = game.hero(seat);
                CavesBossTest.beside(game, seat, statue.pos);
                if (effect == Kinetic.class) {
                    assertTrue(weapon instanceof Greatshield);
                    assertEquals(6 + 2*weapon.buffedLvl(), weapon.defenseFactor(statue));
                    Buff.affect(statue, Kinetic.ConservedDamage.class).setBonus(9);
                    int damage = statue.attackProc(target, 10);
                    assertEquals(19, damage);
                    target.damage(damage, statue);
                    assertNull(statue.buff(Kinetic.KineticTracker.class));
                } else if (effect == Corrupting.class) {
                    assertTrue(weapon.reachFactor(statue) >= 1);
                    for (int i = 0; i < 48; i++) statue.attackProc(target, 10);
                    assertNull(target.buff(Corrupting.CorruptingTracker.class));
                    assertNull(target.buff(Corruption.class));
                    if (statue instanceof ArmoredStatue) {
                        assertTrue(((ArmoredStatue)statue).armor().glyph instanceof Brimstone);
                        assertTrue(statue.isImmune(Burning.class));
                    }
                } else {
                    assertTrue(weapon instanceof Gauntlet);
                    // Both native enchantments can ignite an opponent. Unstable retains all its delegates.
                    if (effect == Unstable.class) {
                        Random.pushGenerator(UnstableSewerTest.seedFor(0));
                        try {
                            weapon.enchantment.proc(weapon, statue, target, 10);
                        } finally {
                            Random.popGenerator();
                        }
                    } else {
                        for (int i = 0; i < 100 && target.buff(Burning.class) == null; i++) statue.attackProc(target, 10);
                    }
                    assertNotNull(target.buff(Burning.class));
                    if (effect == Blazing.class) {
                        assertTrue(((ArmoredStatue)statue).armor().glyph instanceof Viscosity);
                        assertTrue(target.attack(statue, 1f, 45, Float.POSITIVE_INFINITY));
                        assertNotNull(statue.buff(Viscosity.DeferedDamage.class));
                    }
                }
            }
            SewerDepthTest.gate(game, false);
            assertNull(Actor.findById(id));
            parked = game.checkpoint();
        }
        try (CoopSession game = CoopSession.restore(parked)) {
            SewerDepthTest.gate(game, true);
            Statue statue = (Statue)Actor.findById(id);
            assertEquals(weaponId, game.ownership.identity(statue.weapon()));
            if (armorId != 0) assertEquals(armorId, game.ownership.identity(((ArmoredStatue)statue).armor()));
            int cell = statue.pos;
            for (int i = 0; i < 250 && statue.isAlive(); i++) CityKingTest.strike(game, statue);
            assertFalse(statue.isAlive());
            Heap heap = Dungeon.level.heaps.get(cell);
            assertTrue(heap.items.stream().anyMatch(item -> game.ownership.identity(item) == weaponId));
            if (armorId != 0) {
                long expected = armorId;
                assertTrue(heap.items.stream().anyMatch(item -> game.ownership.identity(item) == expected));
            }
        }
    }

    @Test public void blazingGauntletAndViscosity() { encounter(572, 23, Blazing.class); }
    @Test public void kineticGreatshield() { encounter(574, 24, Kinetic.class); }
    @Test public void corruptingWarScythe() { encounter(576, 24, Corrupting.class); }
    @Test public void unstableGauntlet() { encounter(579, 17, Unstable.class); }
    @Test public void corruptingGlaiveAndBrimstone() { encounter(581, 17, Corrupting.class); }
}
