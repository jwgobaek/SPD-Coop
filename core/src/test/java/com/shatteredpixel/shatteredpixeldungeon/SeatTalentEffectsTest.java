package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Berry;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.HighGrass;
import com.shatteredpixel.shatteredpixeldungeon.plants.Mageroyal;
import org.junit.Test;
import static org.junit.Assert.*;

public class SeatTalentEffectsTest extends HeadlessDungeon {
    private static void invest(CoopSession g,int seat,Talent talent) {
        Hero hero=g.hero(seat);
        SewerBeeTest.offer(g,seat);
        if(hero.talentPointsAvailable(1)==0) hero.earnExp(hero.maxExp(),Rat.class);
        long turn=g.turn();
        assertTrue(talent.name(),CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,talent.ordinal(),null));
        assertEquals(turn,g.turn());
        assertFalse(g.hero(1-seat).hasTalent(talent));
    }
    @Test public void warriorSealBreakAndArmorIdentificationUseTheWearersRank() {
        for(int seat=0;seat<2;seat++) for(Talent t:new Talent[]{Talent.VETERANS_INTUITION,Talent.IRON_WILL,Talent.PROVOKED_ANGER})
            try(CoopSession g=CoopSession.open(571,11,HeroClass.WARRIOR)) {
                g.join(22,HeroClass.WARRIOR);
                Rat foe=CoopMageTest.encounter(g,seat);
                Hero h=g.hero(seat);
                int base=h.buff(WarriorShield.class).maxShield();
                for(int rank=1;rank<=2;rank++) {
                    invest(g,seat,t);
                    if(t==Talent.IRON_WILL) assertEquals(base+rank,h.buff(WarriorShield.class).maxShield());
                    if(t==Talent.PROVOKED_ANGER) {
                        Buff.affect(h,Barrier.class).setShield(1);
                        ShieldBuff.processDamage(h,20,foe);
                        assertNotNull(h.buff(Talent.ProvokedAngerTracker.class));
                        assertEquals(6+2*rank,Talent.onAttackProc(h,foe,5));
                        assertNull(h.buff(Talent.ProvokedAngerTracker.class));
                    }
                    if(t==Talent.VETERANS_INTUITION) {
                        LeatherArmor armor=new LeatherArmor();
                        assertEquals(1f+rank,Talent.itemIDSpeedFactor(h,armor),0);
                        Talent.onItemEquipped(h,armor);
                        assertEquals(rank==2,armor.isIdentified());
                    }
                }
            }
    }
    @Test public void mageStaffAndWandEffectsFollowTheCastingOrStrikingHero() {
        for(int seat=0;seat<2;seat++) for(Talent talent:new Talent[]{Talent.BACKUP_BARRIER,Talent.LINGERING_MAGIC,Talent.SCHOLARS_INTUITION})
            try(CoopSession g=CoopSession.open(571,11,HeroClass.MAGE)) {
                g.join(22,HeroClass.MAGE);
                Rat foe=CoopMageTest.encounter(g,seat);
                Hero h=g.hero(seat);
                MagesStaff staff=h.belongings.getItem(MagesStaff.class);
                for(int rank=1;rank<=2;rank++) {
                    invest(g,seat,talent);
                    WandOfMagicMissile wand=new WandOfMagicMissile();
                    assertTrue(wand.collect(h.belongings.backpack));
                    g.ownership.claim(wand,g.player(seat));
                    if(talent==Talent.SCHOLARS_INTUITION) {
                        wand.cursedKnown=true;
                        assertFalse(wand.isIdentified());
                        assertEquals(1+rank*2,Talent.itemIDSpeedFactor(h,wand),0);
                        assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ZAP,foe.pos,wand));
                        assertEquals(rank==2,wand.isIdentified());
                    } else {
                        staff.starterWand().curCharges=1;
                        assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ZAP,foe.pos,staff));
                        if(talent==Talent.BACKUP_BARRIER) {
                            assertTrue(h.shielding()>=rank*2 && h.shielding()<=rank*2+1);
                            assertEquals(0,g.hero(1-seat).shielding());
                        } else {
                            assertNotNull(h.buff(Talent.LingeringMagicTracker.class));
                            int damage=Talent.onAttackProc(h,foe,5);
                            assertTrue(damage>=5+rank && damage<=7);
                            assertNull(h.buff(Talent.LingeringMagicTracker.class));
                        }
                    }
                }
            }
    }
    @Test public void bothMagesCanChooseBattlemageAndChargeOnlyTheirOwnWandsOnHit() {
        for(int seat=0;seat<2;seat++) try(CoopSession g=CoopSession.open(578,11,HeroClass.MAGE)) {
            g.join(22,HeroClass.MAGE);
            com.shatteredpixel.shatteredpixeldungeon.items.TengusMask mask=PlayerGrowthTest.pickMask(g,seat);
            g.beginMaskTrial(CoopSession.MaskTrial.ROSTER);
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_MASK,HeroSubClass.BATTLEMAGE.ordinal(),mask));
            SewerBeeTest.offer(g,1-seat);
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.CLAIM_SUBCLASS,HeroSubClass.WARLOCK.ordinal(),null));
            Rat foe=CoopMageTest.encounter(g,seat);
            CavesBossTest.beside(g,seat,foe.pos);
            Hero h=g.hero(seat);
            Wand a=h.belongings.getItem(MagesStaff.class).starterWand();
            Wand b=g.hero(1-seat).belongings.getItem(MagesStaff.class).starterWand();
            Wand extra=new WandOfMagicMissile();
            assertTrue(extra.collect(h.belongings.backpack));
            g.ownership.claim(extra,g.player(seat));
            a.curCharges=b.curCharges=extra.curCharges=1;
            int hp=foe.HP;
            for(int i=0;i<12 && foe.HP==hp;i++) {
                SewerBeeTest.offer(g,seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ATTACK,foe.pos,null));
            }
            assertTrue(foe.HP<hp);
            assertTrue(a.partialCharge>=0.5 || a.curCharges>1);
            assertTrue(extra.partialCharge>=0.5 || extra.curCharges>1);
            assertTrue(b.partialCharge<0.5 && b.curCharges==1);
            g.checkpoint();
        }
    }
    @Test public void natureTalentsUseObservedPlantsLocalGrassAndNativeFood() {
        for(int seat=0;seat<2;seat++) for(Talent t:new Talent[]{Talent.NATURES_AID,Talent.NATURES_BOUNTY,Talent.SURVIVALISTS_INTUITION})
            try(CoopSession g=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                g.join(22,HeroClass.HUNTRESS);
                Hero h=g.hero(seat);
                for(int rank=1;rank<=2;rank++) {
                    invest(g,seat,t);
                    if(t==Talent.NATURES_AID) {
                        int cell=SewerPlantTest.arrange(g,seat,new Mageroyal.Seed(),true);
                        assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
                        assertNotNull(h.buff(Barkskin.class));
                        assertNull(g.hero(1-seat).buff(Barkskin.class));
                    } else if(t==Talent.SURVIVALISTS_INTUITION) {
                        assertEquals(1+2*rank,Talent.itemIDSpeedFactor(h,new ThrowingStone()),0);
                    } else {
                        for(int n=0;n<2000 && (h.buff(Talent.NatureBerriesDropped.class)==null ||
                                h.buff(Talent.NatureBerriesDropped.class).count()<rank);n++) {
                            Level.set(h.pos,Terrain.HIGH_GRASS);
                            HighGrass.trample(Dungeon.level,h.pos);
                        }
                        assertTrue(h.buff(Talent.NatureBerriesDropped.class).count()>=rank);
                        assertNull(g.hero(1-seat).buff(Talent.NatureBerriesDropped.class));
                        assertTrue(Dungeon.level.heaps.get(h.pos).items.stream().anyMatch(i->i instanceof Berry));
                    }
                }
                g.checkpoint();
            }
    }
}
