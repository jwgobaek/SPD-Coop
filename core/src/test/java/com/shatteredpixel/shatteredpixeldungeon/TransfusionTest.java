package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class TransfusionTest extends HeadlessDungeon {
    static StagedInput prepare(CoopSession game,int seat) {
        Rat target=CoopMageTest.encounter(game,seat);
        WandArtsTest.give(game,seat,new WandOfTransfusion());
        SewerBeeTest.offer(game,seat);
        return new StagedInput(game,"ZAP",target.pos);
    }

    static boolean published(CoopSession game,int seat) {
        Hero caster=game.hero(seat);
        WandOfTransfusion wand=caster.belongings.getItem(WandOfTransfusion.class);
        return wand!=null && wand.curCharges==wand.maxCharges-1 && caster.buff(Barrier.class)!=null
                && Dungeon.level.mobs.stream().anyMatch(m -> m.buff(Charm.class)!=null && m.buff(Charm.class).object==caster.id());
    }

    @Test public void nativeCharmAndHealingUseTheActingMageAndCosts() {
        for(int seat=0;seat<2;seat++) try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            Hero caster=game.hero(seat),peer=game.hero(1-seat);
            Rat target=CoopMageTest.encounter(game,seat);
            WandOfTransfusion wand=WandArtsTest.give(game,seat,new WandOfTransfusion());
            wand.upgrade(2);
            wand.curCharges=wand.maxCharges;
            SewerBeeTest.offer(game,seat);
            int charges=wand.curCharges;
            assertFalse(CoopFloorsTest.step(game,ZAP,peer.pos,wand));
            assertTrue(CoopFloorsTest.step(game,ZAP,target.pos,wand));
            Charm charm=target.buff(Charm.class);
            assertEquals(caster.id(),charm.object);
            assertTrue(charm.ignoreHeroAllies);
            assertEquals(charges-1,wand.curCharges);
            assertNotNull(caster.buff(Barrier.class));
            assertNull(peer.buff(Barrier.class));
            SewerBeeTest.offer(game,seat);
            Buff.detach(caster,Barrier.class);
            target.HP=target.HT-2;
            int hp=caster.HP;
            assertTrue(CoopFloorsTest.step(game,ZAP,target.pos,wand));
            assertEquals(target.HT,target.HP);
            assertNotNull(target.buff(Barrier.class));
            assertEquals(hp-Math.round(caster.HT*.05f),caster.HP);
            assertEquals(charges-2,wand.curCharges);
        }
    }

    @Test public void healingOwnSummonCanDownTheCasterAndUsesNormalCoopRevival() {
        for(int seat=0;seat<2;seat++) {
            Bundle save;
            int allyId;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Hero caster=game.hero(seat);
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                StagedInput summon=SealArtsTest.stage(game,"lightally",seat,rat);
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,summon.cell,caster.belongings.armor()));
                SewerBeeTest.offer(game,seat);
                var ally=com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.getPoweredAlly(caster);
                allyId=ally.id();
                ally.HP=ally.HT-1;
                var ankh=new com.shatteredpixel.shatteredpixeldungeon.items.Ankh();
                assertTrue(ankh.collect(caster.belongings.backpack));
                game.ownership.claim(ankh,game.player(seat));
                WandOfTransfusion wand=WandArtsTest.give(game,seat,new WandOfTransfusion());
                wand.upgrade(2);
                caster.HP=1;
                assertTrue(CoopFloorsTest.step(game,ZAP,ally.pos,wand));
                assertEquals(CoopSession.Life.DOWNED,game.life(seat));
                assertEquals(1,game.preDeathHP(seat));
                assertEquals(ally.HT,ally.HP);
                assertNotNull(ally.buff(Barrier.class));
                save=game.checkpoint();
            }
            try(CoopSession restored=CoopSession.restore(save)) {
                Hero down=restored.hero(seat);
                assertEquals(down,CoopSession.summonerOf((Mob)Actor.findById(allyId)));
                assertEquals(1-seat,restored.activeSeat());
                assertTrue(CoopFloorsTest.step(restored,REVIVE,down.pos,null));
                assertEquals(CoopSession.reviveHP(1,down.HT),down.HP);
            }
        }
    }

    @Test public void undeadTakeRolledDamageWhileEmptyGroundStillConsumesACharge() {
        for(int seat=0;seat<2;seat++) try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            WandmakerTest.enterPrison(game);
            Rat rat=CoopMageTest.encounter(game,seat);
            Hero caster=game.hero(seat);
            Skeleton target=new Skeleton();
            target.pos=caster.pos-2;
            target.HP=target.HT=70;
            GameScene.add(target,2000);
            game.observe(seat);
            WandOfTransfusion wand=WandArtsTest.give(game,seat,new WandOfTransfusion());
            SewerBeeTest.offer(game,seat);
            int charges=wand.curCharges;
            assertTrue(CoopFloorsTest.step(game,ZAP,target.pos,wand));
            assertTrue(70-target.HP>=wand.min() && 70-target.HP<=wand.max());
            assertNull(target.buff(Charm.class));
            SewerBeeTest.offer(game,seat);
            assertTrue(CoopFloorsTest.step(game,ZAP,caster.pos+1,wand));
            assertEquals(charges-2,wand.curCharges);
            assertNull(rat.buff(Charm.class));
        }
    }

    @Test public void imbuedBattlemageEarnsAStoredHealthWaiverOnlyForAnOwnCharm() {
        for(int seat=0;seat<2;seat++) {
            Bundle save;
            int enemy;
            long wandId;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero caster=TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                MagesStaff staff=(MagesStaff)caster.belongings.weapon();
                WandOfTransfusion wand=WandArtsTest.give(game,seat,new WandOfTransfusion());
                wand.partialCharge=.375f;
                wandId=game.ownership.identity(wand);
                assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                assertSame(wand,staff.imbuedWand());
                assertEquals(.375f,wand.partialCharge,0f);
                assertNotNull(caster.belongings.getItem(WandOfMagicMissile.class));
                rat.pos=caster.pos+1;
                game.observe(seat);
                assertTrue(CoopFloorsTest.step(game,ZAP,rat.pos,staff));
                SewerBeeTest.offer(game,seat);
                Charm charm=rat.buff(Charm.class);
                assertEquals(caster.id(),charm.object);
                wand.onHit(staff,game.hero(1-seat),rat,1);
                Bundle stored=new Bundle();
                wand.storeInBundle(stored);
                assertFalse(stored.getBoolean("freecharge"));
                Buff.prolong(rat,Paralysis.class,20);
                for(int turn=0;turn<5 && !stored.getBoolean("freecharge");turn++) {
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,ATTACK,rat.pos,null));
                    wand.storeInBundle(stored);
                }
                assertTrue(stored.getBoolean("freecharge"));
                assertNotNull(caster.buff(Barrier.class));
                enemy=rat.id();
                save=game.checkpoint();
            }
            try(CoopSession restored=CoopSession.restore(save)) {
                Hero caster=restored.hero(seat);
                MagesStaff staff=(MagesStaff)caster.belongings.weapon();
                WandOfTransfusion wand=(WandOfTransfusion)staff.imbuedWand();
                assertEquals(wandId,restored.ownership.identity(wand));
                Rat rat=(Rat)Actor.findById(enemy);
                SewerBeeTest.offer(restored,seat);
                if(rat.buff(Charm.class)==null) {
                    assertTrue(CoopFloorsTest.step(restored,ZAP,rat.pos,staff));
                    SewerBeeTest.offer(restored,seat);
                }
                Buff.detach(caster,Barrier.class);
                int hp=caster.HP, charges=wand.curCharges;
                assertTrue(CoopFloorsTest.step(restored,ZAP,rat.pos,staff));
                assertEquals(charges-1,wand.curCharges);
                assertEquals(hp,caster.HP);
                Bundle state=new Bundle();
                wand.storeInBundle(state);
                assertFalse(state.getBoolean("freecharge"));
            }
        }
    }

    @Test public void blastAndWildMagicPreserveCasterTargetAndArmorCost() {
        for(int seat=0;seat<2;seat++) for(int kind=0;kind<2;kind++) {
            try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero caster=game.hero(seat), peer=game.hero(1-seat);
                boolean wild=kind==1;
                ClassArmor armor=ArmorTalentsTest.invest(game,seat,kind,wild?Talent.WILD_POWER:Talent.ELEMENTAL_POWER,1);
                WandOfTransfusion wand=WandArtsTest.give(game,seat,new WandOfTransfusion());
                if(!wild) {
                    MagesStaff staff=(MagesStaff)caster.belongings.weapon();
                    assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                }
                SewerBeeTest.offer(game,seat);
                float armorBefore=armor.charge, wandBefore=wand.curCharges+wand.partialCharge;
                int peerHp=peer.HP;
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,wild?rat.pos:caster.pos,armor));
                assertTrue(armor.charge<armorBefore);
                assertEquals(caster.id(),rat.buff(Charm.class).object);
                assertEquals(peerHp,peer.HP);
                assertNull(peer.buff(Barrier.class));
                if(wild) assertTrue(wand.curCharges+wand.partialCharge<wandBefore);
            }
        }
    }
}
