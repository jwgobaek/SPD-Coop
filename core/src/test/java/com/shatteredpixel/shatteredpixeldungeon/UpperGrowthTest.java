package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.shatteredpixel.shatteredpixeldungeon.lan.SeatQuickslots;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpperGrowthTest extends HeadlessDungeon {
    private static boolean invest(CoopSession game, Talent talent) {
        return CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null);
    }

    @Test public void bothRoguesEquipVanishSurpriseAndResumeTheirOwnCloak() {
        for (int seat=0; seat<2; seat++) {
            Bundle save;
            int recharge, stealth, target;
            long itemId;
            try (CoopSession game=CoopSession.open(571,11,HeroClass.ROGUE)) {
                game.join(22,HeroClass.ROGUE);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                CavesBossTest.beside(game,seat,rat.pos);
                SewerBeeTest.offer(game,seat);
                PublicDungeon view=ProjectionChecks.seatImage(game,seat);
                assertEquals("CLOAK",SeatQuickslots.defaults(view).item(0,view).kind);
                assertEquals("KNIFE",SeatQuickslots.defaults(view).item(1,view).kind);
                CloakOfShadows cloak=hero.belongings.getItem(CloakOfShadows.class);
                itemId=game.ownership.identity(cloak);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.CLOAK,0,game.hero(1-seat).belongings.artifact));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.UNEQUIP,0,cloak));
                SewerBeeTest.offer(game,seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.CLOAK,0,cloak));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,cloak));
                SewerBeeTest.offer(game,seat);
                hero.lvl=6;
                assertTrue(invest(game,Talent.PROTECTIVE_SHADOWS));
                assertTrue(invest(game,Talent.SUCKER_PUNCH));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLOAK,0,cloak));
                assertEquals(1,hero.invisible);
                assertEquals(0,game.hero(1-seat).invisible);
                SewerBeeTest.offer(game,seat);
                assertEquals(2,cloak.charges());
                assertTrue(hero.buff(Barrier.class).shielding()>0);
                assertTrue(rat.surprisedBy(hero));
                recharge=hero.buff(CloakOfShadows.cloakRecharge.class).id();
                stealth=hero.buff(CloakOfShadows.cloakStealth.class).id();
                target=rat.id();
                save=game.checkpoint();
                assertEquals(18,save.getInt("schema"));
            }
            try (CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                assertEquals(1,hero.invisible);
                assertSame(hero.buff(CloakOfShadows.cloakRecharge.class),Actor.findById(recharge));
                assertSame(hero.buff(CloakOfShadows.cloakStealth.class),Actor.findById(stealth));
                assertEquals(itemId,game.ownership.identity(hero.belongings.artifact));
                Rat rat=(Rat)Actor.findById(target);
                int hp=rat.HP;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,rat.pos,null));
                assertTrue(rat.HP<hp);
                assertNotNull(rat.buff(Talent.SuckerPunchTracker.class));
                assertEquals(0,hero.invisible);
                assertEquals(3,((CloakOfShadows)game.hero(1-seat).belongings.artifact).charges());
                game.checkpoint();
            }
        }
    }

    @Test public void thirdTierChecksClassPointsAndExtendsNativeKillWindow() {
        for(int seat=0;seat<2;seat++) for(int points=0;points<=3;points++) {
            Bundle save;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                SewerBeeTest.offer(game,seat);
                hero.lvl=30;
                assertFalse(invest(game,Talent.CLEAVE));
                Combo combo=SubclassActionTest.prepare(game,seat,rat,2);
                hero.lvl=6;
                assertFalse(invest(game,Talent.CLEAVE));
                hero.lvl=30;
                long turn=game.turn();
                int initial=hero.talentPointsAvailable(3);
                for(int i=0;i<points;i++) assertTrue(invest(game,Talent.CLEAVE));
                if(points==3) assertFalse(invest(game,Talent.CLEAVE));
                assertEquals(initial-points,hero.talentPointsAvailable(3));
                assertEquals(turn,game.turn());
                rat.HP=1;
                hero.belongings.weapon().upgrade(12);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,rat.pos,null));
                assertFalse(rat.isAlive());
                assertTrue(combo.remainingTime()>=14+15*points && combo.remainingTime()<=15+15*points);
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                assertEquals(points,game.hero(seat).pointsInTalent(Talent.CLEAVE));
                assertEquals(0,game.hero(1-seat).pointsInTalent(Talent.CLEAVE));
            }
        }
    }

    @Test public void chargedWaveAndEnduranceFollowWearerThroughCheckpoint() {
        for(int seat=0;seat<2;seat++) for(int choice=1;choice<=2;choice++) {
            Bundle save;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                SewerBeeTest.offer(game,seat);
                KingsCrown crown=CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.ROSTER);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,choice,crown));
                SewerBeeTest.offer(game,seat);
                ClassArmor armor=(ClassArmor)hero.belongings.armor();
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,choice==1?rat.pos:hero.pos,armor));
                assertTrue(armor.charge<16);
                if(choice==1) {
                    assertTrue(rat.HP<rat.HT);
                    assertNotNull(rat.buff(Cripple.class));
                } else {
                    Endure.EndureTracker endurance=hero.buff(Endure.EndureTracker.class);
                    assertNotNull(endurance);
                    assertEquals(5f,endurance.adjustDamageTaken(10f),0.001f);
                }
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                assertNull(game.hero(1-seat).armorAbility);
                assertTrue(((ClassArmor)game.hero(seat).belongings.armor()).charge<16);
                if(choice==2) {
                    Endure.EndureTracker endurance=game.hero(seat).buff(Endure.EndureTracker.class);
                    assertEquals(5,endurance.damageBonus);
                    SewerBeeTest.offer(game,seat);
                    assertFalse(endurance.enduring);
                    assertEquals(9f,endurance.damageFactor(4f),0.001f);
                }
            }
        }
    }

    @Test public void enhancedFinishersUseOnlyTheOwnersRankAndAFreeLandingCell() {
        for(int seat=0;seat<2;seat++) {
            try(CoopSession game=CoopSession.open(571,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat), ally=game.hero(1-seat);
                Combo combo=SubclassActionTest.prepare(game,seat,rat,10);
                hero.lvl=30;
                for(int i=0;i<3;i++) assertTrue(invest(game,Talent.ENHANCED_COMBO));
                hero.pos=rat.pos-3;
                ally.pos=rat.pos-1;
                game.observe(seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.CLOBBER,rat.pos,null));
                assertEquals(10,combo.getComboCount());
                ally.pos+=Dungeon.level.width();
                int landing=rat.pos-1;
                com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(landing,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.CHASM);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.CLOBBER,rat.pos,null));
                com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(landing,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);
                hero.rooted=true;
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.CLOBBER,rat.pos,null));
                hero.rooted=false;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLOBBER,rat.pos,null));
                assertEquals(landing,hero.pos);
                assertNotNull(rat.buff(Vertigo.class));
                assertFalse(combo.canUseMove(Combo.ComboMove.CLOBBER));
                assertEquals(0,ally.pointsInTalent(Talent.ENHANCED_COMBO));
                game.checkpoint();
            }
        }
    }

    @Test public void crushKillsRefundTheWearersSealForEveryVictim() {
        for(int seat=0;seat<2;seat++) for(int rank=1;rank<=3;rank++) {
            try(CoopSession game=CoopSession.open(571,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat), ally=game.hero(1-seat);
                SubclassActionTest.prepare(game,seat,rat,8);
                hero.lvl=30;
                for(int i=0;i<rank;i++) assertTrue(invest(game,Talent.LETHAL_DEFENSE));
                hero.belongings.weapon().upgrade(15);
                Rat extra=new Rat();
                extra.HP=rat.HP=1;
                extra.pos=rat.pos+Dungeon.level.width();
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(extra,2000);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CRUSH,rat.pos,null));
                assertFalse(extra.isAlive());
                assertFalse(rat.isAlive());
                for(Hero h:new Hero[]{hero,ally}) {
                    Bundle b=new Bundle();
                    h.buff(com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class).storeInBundle(b);
                    assertEquals(h==ally?0:Math.max(-150,-100*rank),b.getInt("cooldown"));
                }
            }
        }
    }

    @Test public void shieldBatteryAndLiquidTalentsUseTheActingSeat() {
        for(int seat=0;seat<2;seat++) for(int rank=1;rank<=2;rank++)
            for(HeroClass type:new HeroClass[]{HeroClass.MAGE,HeroClass.WARRIOR,HeroClass.HUNTRESS}) {
                Bundle save;
                try(CoopSession game=CoopSession.open(570,11,type)) {
                    game.join(22,type);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero h=game.hero(seat);
                    CavesBossTest.beside(game,seat,rat.pos);
                    SewerBeeTest.offer(game,seat);
                    h.lvl=12; h.HP=h.HT=100;
                    Talent t=type==HeroClass.MAGE?Talent.SHIELD_BATTERY:type==HeroClass.WARRIOR?Talent.LIQUID_WILLPOWER:Talent.LIQUID_NATURE;
                    for(int i=0;i<rank;i++) assertTrue(invest(game,t));
                    if(type==HeroClass.MAGE) {
                        com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff)h.belongings.weapon();
                        int charges=staff.starterWand().curCharges;
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,h.pos,staff));
                        assertEquals(0,staff.starterWand().curCharges);
                        int granted=charges*(rank==1?4:6);
                        int remaining=h.buff(Barrier.class).shielding();
                        assertTrue(remaining==granted || granted>=20 && remaining==granted-1);
                    } else {
                        com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();
                        potion.identify();
                        assertTrue(potion.collect(h.belongings.backpack));
                        game.ownership.claim(potion,game.player(seat));
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.DRINK,0,potion));
                        if(type==HeroClass.WARRIOR) assertEquals(rank==1?7:10,h.buff(Barrier.class).shielding());
                        else assertNotNull(rat.buff(Roots.class));
                    }
                    assertEquals(0,game.hero(1-seat).pointsInTalent(t));
                    save=game.checkpoint();
                }
                try(CoopSession game=CoopSession.restore(save)) {
                    assertEquals(0,game.hero(1-seat).talentPointsSpent(2));
                    assertEquals(rank,game.hero(seat).talentPointsSpent(2));
                }
            }
    }

    @Test public void beaconPersistsAtItsOwnBranchAndDoesNotDisplaceTheOtherSeat() {
        for(int seat=0;seat<2;seat++) {
            Bundle save;
            int mark;
            try(CoopSession game=CoopSession.open(571,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                CoopMageTest.encounter(game,seat);
                Hero h=game.hero(seat);
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,2,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
                SewerBeeTest.offer(game,seat);
                mark=h.pos;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,mark,h.belongings.armor()));
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                Hero h=game.hero(seat);
                Hero ally=game.hero(1-seat);
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,mark-1,null));
                SewerBeeTest.offer(game,seat);
                int backup=ally.pos;
                ally.pos=mark;
                ClassArmor armor=(ClassArmor)h.belongings.armor();
                float charge=armor.charge;
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,-1,armor));
                assertEquals(charge,armor.charge,0f);
                ally.pos=backup;
                long turn=game.turn();
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,-1,armor));
                assertEquals(mark,h.pos);
                assertEquals(turn,game.turn());
                assertEquals(charge-35,armor.charge,0.001f);
            }
        }
    }
}
