package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class SeatTalentTest extends HeadlessDungeon {
    @Test public void heroicEnergyUsesOnlyTheSelectedArmorAndSurvivesRestore() {
        float[] costMultiplier = {1f, .88f, .77f, .68f, .60f};
        for (HeroClass role : HeroClass.values()) for (int ability = 0; ability < 3; ability++) {
            for (int seat = 0; seat < 2; seat++) {
                Bundle save;
                try (CoopSession game = CoopSession.open(572, 11, role)) {
                    game.join(22,role);
                    CoopMageTest.encounter(game,seat);
                    SewerBeeTest.offer(game,seat);
                    Hero hero = game.hero(seat);
                    hero.lvl = 30;
                    assertFalse(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.HEROIC_ENERGY.ordinal(), null));
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.USE_CROWN, ability,
                            CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
                    SewerBeeTest.offer(game,seat);
                    float base = hero.armorAbility.chargeUse(hero);
                    for (int rank = 1; rank <= 4; rank++) {
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT,Talent.HEROIC_ENERGY.ordinal(),null));
                        assertEquals(base * costMultiplier[rank], hero.armorAbility.chargeUse(hero), .001f);
                    }
                    assertFalse(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT,Talent.HEROIC_ENERGY.ordinal(),null));
                    if (role == HeroClass.CLERIC && ability == 0) {
                        var armor = (com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)hero.belongings.armor();
                        float before = armor.charge;
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,hero.pos,armor));
                        assertEquals(0.60f * base,before - armor.charge,.5f);
                    }
                    save = game.checkpoint();
                }
                try (CoopSession restored = CoopSession.restore(save)) {
                    assertEquals(4,restored.hero(seat).pointsInTalent(Talent.HEROIC_ENERGY));
                    assertEquals(0,restored.hero(1-seat).pointsInTalent(Talent.HEROIC_ENERGY));
                }
            }
        }
    }

    @Test public void swiftEquipHealingPotionAndRechargingFollowTheDuelist() {
        for (int seat = 0; seat < 2; seat++) for (int rank = 0; rank <= 2; rank++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.DUELIST)) {
                game.join(22, HeroClass.DUELIST);
                Rat foe = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                Hero hero = game.hero(seat), other = game.hero(1-seat);
                hero.lvl = 15;
                for (Talent talent : new Talent[]{Talent.SWIFT_EQUIP, Talent.LIQUID_AGILITY, Talent.WEAPON_RECHARGING})
                    for (int point = 0; point < rank; point++)
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
                Quarterstaff staff = new Quarterstaff();
                assertTrue(staff.collect(hero.belongings.backpack));
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.EQUIP, 0, staff));
                Talent.SwiftEquipCooldown swift = hero.buff(Talent.SwiftEquipCooldown.class);
                assertEquals(rank > 0, swift != null);
                if (swift != null) assertEquals(rank == 2, swift.hasSecondUse());
                assertNull(other.buff(Talent.SwiftEquipCooldown.class));
                SewerBeeTest.offer(game, seat);
                var potion = new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();
                potion.identify();
                assertTrue(potion.collect(hero.belongings.backpack));
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.DRINK, 0, potion));
                assertEquals(rank > 0, hero.buff(Talent.LiquidAgilACCTracker.class) != null);
                assertNull(other.buff(Talent.LiquidAgilACCTracker.class));
                if (rank > 0) {
                    assertEquals(1, hero.buff(Talent.LiquidAgilACCTracker.class).uses);
                    int accuracy = hero.attackSkill(foe);
                    assertTrue(rank == 2 ? accuracy >= Hero.INFINITE_ACCURACY : accuracy > 0);
                }
                MeleeWeapon.Charger charger = hero.buff(MeleeWeapon.Charger.class);
                charger.charges = 0;
                charger.partialCharge = 0;
                charger.act();
                float ordinary = charger.partialCharge;
                charger.partialCharge = 0;
                Buff.prolong(hero, Recharging.class, 5);
                charger.act();
                assertEquals(ordinary + (rank == 0 ? 0 : 1f / (20 - 5*rank)), charger.partialCharge, .00001f);
                assertNull(other.buff(Recharging.class));
                assertNotNull(game.checkpoint());
            }
        }
    }

    @Test public void searingLightConsumesTheMarkAndAddsOnlyTheAttackersBonus() {
        for (int seat = 0; seat < 2; seat++) for (int rank = 0; rank <= 2; rank++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
                game.join(22, HeroClass.CLERIC);
                Rat foe = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                Hero hero = game.hero(seat);
                hero.lvl = 8;
                for (int point = 0; point < rank; point++)
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.SEARING_LIGHT.ordinal(), null));
                foe.pos = hero.pos + 1;
                foe.HT = foe.HP = 150;
                game.observe(seat);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.GUIDING_LIGHT, foe.pos, hero.belongings.artifact));
                assertNotNull(foe.buff(GuidingLight.Illuminated.class));
                int before = foe.HP;
                com.watabou.utils.Random.pushGenerator(572);
                try {
                    assertTrue(hero.attack(foe, 0, 20, Hero.INFINITE_ACCURACY));
                } finally {
                    com.watabou.utils.Random.popGenerator();
                }
                int marked = before - foe.HP;
                assertNull(foe.buff(GuidingLight.Illuminated.class));
                before = foe.HP;
                com.watabou.utils.Random.pushGenerator(572);
                try {
                    assertTrue(hero.attack(foe, 0, 20, Hero.INFINITE_ACCURACY));
                } finally {
                    com.watabou.utils.Random.popGenerator();
                }
                assertEquals(rank == 0 ? 0 : 1 + 2*rank, marked - (before - foe.HP));
                assertEquals(0, game.hero(1-seat).pointsInTalent(Talent.SEARING_LIGHT));
            }
        }
    }

    @Test public void duelistsWaitAndLungeAndRoguesSearchAtEachRank() {
        for (int seat = 0; seat < 2; seat++) for (int rank = 0; rank <= 2; rank++) {
            try (CoopSession game = CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Rat foe = CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                Hero hero = game.hero(seat);
                hero.lvl = 15;
                for (Talent talent : new Talent[]{Talent.PATIENT_STRIKE,Talent.AGGRESSIVE_BARRIER,Talent.LETHAL_HASTE}) {
                    for (int n = 0; n < rank; n++)
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,talent.ordinal(),null));
                }
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WAIT,0,null));
                SewerBeeTest.offer(game,seat);
                assertEquals(rank > 0, hero.buff(Talent.PatientStrikeTracker.class) != null);
                if (rank > 0) {
                    int damage = Talent.onAttackProc(hero,foe,10);
                    assertTrue(damage >= 10+rank && damage <= 12);
                    assertNull(hero.buff(Talent.PatientStrikeTracker.class));
                }
                hero.HP = hero.HT / 3;
                foe.HP = 1;
                StagedInput lunge = WandArtsTest.stage(game,"lunge",seat,foe);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WEAPON_ABILITY,lunge.cell,hero.belongings.weapon()));
                assertFalse(foe.isAlive());
                assertEquals(rank > 0,hero.buff(Barrier.class) != null);
                assertEquals(rank > 0,hero.buff(GreaterHaste.class) != null);
                if (rank > 0) {
                    int remaining = Integer.parseInt(hero.buff(GreaterHaste.class).iconTextDisplay());
                    assertTrue(remaining >= 1 + 2*rank && remaining <= 2 + 2*rank);
                    assertTrue(hero.buff(Barrier.class).shielding() > 0 && hero.buff(Barrier.class).shielding() <= 1 + 2*rank);
                }
                assertNull(game.hero(1-seat).buff(GreaterHaste.class));
                assertNull(game.hero(1-seat).buff(Barrier.class));
                assertNotNull(game.checkpoint());
            }
            try (CoopSession game = CoopSession.open(572,11,HeroClass.ROGUE)) {
                game.join(22,HeroClass.ROGUE);
                CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                Hero hero = game.hero(seat);
                hero.lvl = 12;
                for (int n = 0; n < rank; n++)
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.WIDE_SEARCH.ordinal(),null));
                int width = Dungeon.level.width();
                for (int y = 0; y <= 3; y++) for (int x = 0; x <= 3; x++)
                    com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(hero.pos + x + y*width,
                            com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);
                int edge = hero.pos + 3, corner = hero.pos + 3 + 3*width;
                for (int tile : new int[]{edge,corner})
                    com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(tile,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR);
                Dungeon.level.updateFieldOfView(hero, hero.fieldOfView);
                game.observe(seat);
                assertTrue("search boundary visible", hero.fieldOfView[edge] && hero.fieldOfView[corner]);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SEARCH,hero.pos,null));
                assertEquals("edge seat="+seat+" rank="+rank,rank < 1,Dungeon.level.secret[edge]);
                assertEquals("corner seat="+seat+" rank="+rank,rank < 2,Dungeon.level.secret[corner]);
                assertEquals(0,game.hero(1-seat).pointsInTalent(Talent.WIDE_SEARCH));
            }
        }
    }

    @Test public void intuitionAndInscribedStealthRespectTheInvestingSeat() {
        for (HeroClass role : new HeroClass[]{HeroClass.ROGUE, HeroClass.DUELIST}) {
            for (int seat = 0; seat < 2; seat++) for (int rank = 0; rank <= 2; rank++) {
                try (CoopSession game = CoopSession.open(572, 11, role)) {
                    game.join(22, role);
                    CoopMageTest.encounter(game, seat);
                    SewerBeeTest.offer(game, seat);
                    Hero hero = game.hero(seat);
                    while (hero.lvl < 9) hero.earnExp(hero.maxExp(), Rat.class);
                    Talent intuition = role == HeroClass.DUELIST ? Talent.ADVENTURERS_INTUITION : Talent.THIEFS_INTUITION;
                    var ownRing = new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste();
                    var otherRing = new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy();
                    assertTrue(ownRing.collect(hero.belongings.backpack));
                    assertTrue(otherRing.collect(game.hero(1-seat).belongings.backpack));
                    Quarterstaff staff = new Quarterstaff();
                    assertTrue(staff.collect(hero.belongings.backpack));
                    if (role == HeroClass.DUELIST) {
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.EQUIP, 0, staff));
                        SewerBeeTest.offer(game, seat);
                    }
                    for (int n = 0; n < rank; n++)
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, intuition.ordinal(), null));
                    assertEquals(0, game.hero(1-seat).pointsInTalent(intuition));
                    if (role == HeroClass.DUELIST) {
                        assertEquals(rank == 2, staff.isIdentified());
                        assertEquals(1f + 1.5f*rank, Talent.itemIDSpeedFactor(hero, staff), .001f);
                        assertEquals(1f + .75f*rank, Talent.itemIDSpeedFactor(hero, hero.belongings.armor()), .001f);
                    } else {
                        assertEquals(rank == 2, ownRing.isKnown());
                        assertFalse(otherRing.isKnown());
                        assertEquals(1f + rank, Talent.itemIDSpeedFactor(hero, ownRing), .001f);
                        assertTrue(ownRing.doEquip(hero));
                        assertEquals(rank > 0, ownRing.isKnown());
                        assertEquals(rank == 2, ownRing.isIdentified());
                        for (int n = 0; n < rank; n++)
                            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.INSCRIBED_STEALTH.ordinal(), null));
                        var scroll = new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify();
                        scroll.setKnown();
                        assertTrue(scroll.collect(hero.belongings.backpack));
                        int index = 0;
                        for (Item entry : hero.belongings) { index++; if (entry == staff) break; }
                        SewerBeeTest.offer(game, seat);
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.IDENTIFY, index, scroll));
                        assertTrue(staff.isIdentified());
                        assertEquals(rank > 0, hero.buff(Invisibility.class) != null);
                        assertNull(game.hero(1-seat).buff(Invisibility.class));
                    }
                    assertNotNull(game.checkpoint());
                }
            }
        }
    }

    @Test public void newlyPlayableMealTalentsUseOnlyTheEatersResources() {
        for (Talent talent : new Talent[]{Talent.MYSTICAL_MEAL, Talent.STRENGTHENING_MEAL, Talent.FOCUSED_MEAL,
                Talent.SATIATED_SPELLS, Talent.ENLIGHTENING_MEAL}) {
            HeroClass role = talent == Talent.MYSTICAL_MEAL ? HeroClass.ROGUE :
                    talent == Talent.STRENGTHENING_MEAL || talent == Talent.FOCUSED_MEAL ? HeroClass.DUELIST : HeroClass.CLERIC;
            for (int seat = 0; seat < 2; seat++) for (int rank = 0; rank <= 2; rank++) {
                Bundle saved;
                try (CoopSession game = CoopSession.open(572, 11, role)) {
                    game.join(22, role);
                    CoopMageTest.encounter(game, seat);
                    SewerBeeTest.offer(game, seat);
                    Hero h = game.hero(seat);
                    while (h.lvl < 9) h.earnExp(h.maxExp(), Rat.class);
                    for (int point = 0; point < rank; point++) {
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
                    }
                    if (talent == Talent.FOCUSED_MEAL) {
                        MeleeWeapon.Charger charger = h.buff(MeleeWeapon.Charger.class);
                        charger.charges = 0;
                        charger.partialCharge = 0;
                    }
                    if (talent == Talent.ENLIGHTENING_MEAL) {
                        HolyTome tome = (HolyTome) h.belongings.artifact;
                        tome.spendCharge(h, tome.charges());
                    }
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.EAT, 0, h.belongings.getItem(Food.class)));
                    assertEquals(0, game.hero(1-seat).pointsInTalent(talent));
                    if (talent == Talent.MYSTICAL_MEAL) {
                        ArtifactRecharge charge = h.buff(ArtifactRecharge.class);
                        assertEquals(rank > 0, charge != null);
                        if (charge != null) assertTrue(charge.left() >= 2*rank - 1 && charge.left() <= 1+2*rank);
                        assertNull(game.hero(1-seat).buff(ArtifactRecharge.class));
                    } else if (talent == Talent.STRENGTHENING_MEAL) {
                        PhysicalEmpower buff = h.buff(PhysicalEmpower.class);
                        assertEquals(rank > 0, buff != null);
                        if (buff != null) {
                            assertEquals(3,buff.dmgBoost);
                            assertEquals(1+rank,buff.left);
                        }
                        assertNull(game.hero(1-seat).buff(PhysicalEmpower.class));
                    } else if (talent == Talent.FOCUSED_MEAL || talent == Talent.ENLIGHTENING_MEAL) {
                        float charge;
                        if (role == HeroClass.DUELIST) {
                            MeleeWeapon.Charger charger = h.buff(MeleeWeapon.Charger.class);
                            charge = charger.charges + charger.partialCharge;
                        } else {
                            HolyTome tome = (HolyTome) h.belongings.artifact;
                            Bundle state = new Bundle();
                            tome.storeInBundle(state);
                            charge = tome.charges() + state.getFloat("partialcharge");
                        }
                        float expected = rank == 0 ? 0f : (1 + rank) / 3f;
                        // Original chargers may advance once while the meal's turn completes.
                        assertTrue(talent+" rank="+rank+" actual="+charge, charge >= expected-.001f && charge < expected+.2f);
                    } else {
                        assertEquals(rank > 0, h.buff(Talent.SatiatedSpellsTracker.class) != null);
                        if (rank > 0) {
                            SewerBeeTest.offer(game,seat);
                            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.HOLY_WEAPON, h.pos, h.belongings.artifact));
                            assertNull(h.buff(Talent.SatiatedSpellsTracker.class));
                            assertNotNull(h.buff(Barrier.class));
                            assertTrue(h.buff(Barrier.class).shielding() > 0 && h.buff(Barrier.class).shielding() <= 1+2*rank);
                            assertNull(game.hero(1-seat).buff(Barrier.class));
                        }
                    }
                    saved = game.checkpoint();
                }
                try (CoopSession restored = CoopSession.restore(saved)) {
                    assertEquals(rank, restored.hero(seat).pointsInTalent(talent));
                    assertEquals(0, restored.hero(1-seat).pointsInTalent(talent));
                }
            }
        }
    }

}
