package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.AlarmTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.PitfallTrap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;

import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class GuardianInteractions extends HeadlessDungeon {
    private void returning(int seat) {
        Bundle save;
        int peerGuardian;
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
            game.join(22, HeroClass.CLERIC);
            CoopMageTest.encounter(game, seat);
            var owner = game.hero(seat);
            var peer = game.hero(1-seat);
            var armor = ArmorTalentsTest.invest(game, seat, 2, Talent.STASIS, 1);
            TierGrowthTest.powerTome(owner);
            var tome = owner.belongings.getItem(HolyTome.class);
            EarthGuardian[] guardians = new EarthGuardian[2];
            for (int s=0; s<2; s++) {
                var guardian = new EarthGuardian();
                guardian.setInfo(game.hero(s), 2, 9);
                guardian.pos = game.hero(s).pos+1;
                GameScene.add(guardian, 100);
                guardians[s] = guardian;
            }
            peerGuardian = guardians[1-seat].id();
            game.observe(seat);
            int tomeCharges = tome.charges();
            float armorCharge = armor.charge;
            assertFalse(CoopFloorsTest.step(game, ARMOR_ABILITY, guardians[1-seat].pos, armor));
            assertNull(PowerOfMany.getPoweredAlly(owner));
            assertFalse(CoopFloorsTest.step(game, STASIS, owner.pos, tome));
            assertNull(Stasis.getStasisAlly(owner));
            assertEquals(tomeCharges, tome.charges());
            assertEquals(armorCharge, armor.charge, 0);
            assertTrue(CoopFloorsTest.step(game, ARMOR_ABILITY, guardians[seat].pos, armor));
            int oldId = guardians[seat].id();
            assertTrue(guardians[seat].WANDERING.act(false, false));
            assertNull(Actor.findById(oldId));
            assertNull(WandOfLivingEarth.activeGuardian(owner));
            assertTrue(owner.buff(RockArmor.class).isEmpowered());
            assertNull(peer.buff(RockArmor.class));
            assertSame(guardians[1-seat], WandOfLivingEarth.activeGuardian(peer));
            save = game.checkpoint();
        }
        try (CoopSession game = CoopSession.restore(save)) {
            assertNotNull(game.hero(seat).buff(RockArmor.class));
            var other = WandOfLivingEarth.activeGuardian(game.hero(1-seat));
            assertEquals(peerGuardian, other.id());
            assertSame(game.hero(1-seat), other.owner());
            assertNull(WandOfLivingEarth.activeGuardian(game.hero(seat)));
            System.out.println("PASS guardian seat=" + seat + " peer refused, owned Power, native wandering return, peer retained");
        }
    }

    private void trap(int seat) {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
            game.join(22, HeroClass.MAGE);
            CoopMageTest.encounter(game, seat);
            var owner = game.hero(seat);
            var wand = WandArtsTest.give(game, seat, new WandOfLivingEarth());
            int cell = owner.pos+1;
            assertNull(Actor.findChar(cell));
            assertNull(Actor.findChar(cell+1));
            Level.set(cell+1, Terrain.WALL);
            var pit = new PitfallTrap();
            Dungeon.level.setTrap(pit, cell);
            pit.reveal();
            Level.set(cell, Terrain.TRAP);
            game.observe(seat);
            SewerBeeTest.offer(game, seat);
            int charge = wand.curCharges;
            assertFalse(CoopFloorsTest.step(game, ZAP, cell, wand));
            assertEquals(charge, wand.curCharges);
            assertTrue(pit.active);
            var alarm = new AlarmTrap();
            Dungeon.level.setTrap(alarm, cell);
            alarm.reveal();
            game.observe(seat);
            assertTrue("seat="+seat+" active="+game.activeSeat()+" visible="+game.knowledge(owner).visible(cell)
                    +" landing="+CoopSession.supportedLanding(cell)+" solid="+Dungeon.level.solid[cell]
                    +" collision="+new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(owner.pos,cell,wand.collisionProperties(cell)).collisionPos+" target="+cell,
                    CoopFloorsTest.step(game, ZAP, cell, wand));
            assertFalse(alarm.active);
            assertEquals(charge-1, wand.curCharges);
            System.out.println("PASS earth landing seat=" + seat + " unsupported pitfall refused, native alarm triggered");
        }
    }

    public static void main(String[] args) throws Exception {
        var probe = new GuardianInteractions();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (int seat=0; seat<2; seat++) {
                probe.returning(seat);
                probe.trap(seat);
            }
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }
}
