package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.HighGrass;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import org.junit.Test;
import static org.junit.Assert.*;

public class TierGrowthTest extends HeadlessDungeon {
    private static Hero prepare(CoopSession game, int seat, HeroSubClass subclass, Talent talent, int rank) {
        SewerBeeTest.offer(game, seat);
        Hero hero = game.hero(seat);
        hero.lvl = 25;
        if (hero.subClass != subclass) {
            hero.subClass = subclass;
            Talent.initSubclassTalents(hero);
        }
        for (int i = 0; i < rank; i++) {
            assertTrue(talent.name(), CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
        }
        assertEquals(rank, hero.pointsInTalent(talent));
        assertEquals(0, game.hero(1-seat).pointsInTalent(talent));
        return hero;
    }

    @Test public void waitingGrantsOnlyTheWarriorsNativeDefenseAtTheirCurrentCell() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            Bundle save;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Hero hero=prepare(game,seat,HeroSubClass.GLADIATOR,Talent.HOLD_FAST,rank);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WAIT,hero.pos,null));
                assertEquals(rank>0,hero.buff(HoldFast.class)!=null);
                float factor=rank==0 ? 1f : rank==1 ? .5f : rank==2 ? .25f : 0f;
                assertEquals(factor,HoldFast.buffDecayFactor(hero),0f);
                assertEquals(1f,HoldFast.buffDecayFactor(game.hero(1-seat)),0f);
                if (rank>0) {
                    int bonus=hero.buff(HoldFast.class).armorBonus();
                    assertTrue(bonus>=rank && bonus<=2*rank);
                }
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                assertEquals(rank,hero.pointsInTalent(Talent.HOLD_FAST));
                SewerBeeTest.offer(game,seat);
                boolean moved=false;
                for (int delta:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    if (CoopFloorsTest.step(game,CoopSession.Action.MOVE,hero.pos+delta,null)) { moved=true; break; }
                }
                assertTrue(moved);
                assertEquals(1f,HoldFast.buffDecayFactor(hero),0f);
            }
        }
    }

    @Test public void fatalRageSavesTheSurvivingSeatAndTheHealthBeforeItsLethalHit() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            Bundle save;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Hero hero=prepare(game,seat,HeroSubClass.BERSERKER,Talent.DEATHLESS_FURY,rank);
                hero.HP=13;
                Buff.affect(hero,Berserk.class).damage(4*hero.HT);
                hero.damage(100,Rat.class);
                assertEquals(0,hero.HP);
                assertEquals(rank>0,game.life(seat)==CoopSession.Life.ALIVE);
                assertEquals(13,game.preDeathHP(seat));
                if (rank==0) continue;
                Bundle buff=new Bundle();
                hero.buff(Berserk.class).storeInBundle(buff);
                assertEquals(4-rank,buff.getFloat("levelrecovery"),.001f);
                save=game.checkpoint();
                assertEquals(23,save.getInt("schema"));
            }
            try (CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                assertTrue(hero.buff(Berserk.class).sustainsLife());
                assertEquals(13,game.preDeathHP(seat));
                hero.damage(100,Rat.class);
                assertFalse(game.life(seat)==CoopSession.Life.ALIVE);
                assertEquals(13,game.preDeathHP(seat));
                assertTrue(game.hero(1-seat).isAlive());
            }
        }
    }

    @Test public void rageChangesOnlyTheAttackersNativeEnchantmentMultiplier() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Hero hero=prepare(game,seat,HeroSubClass.BERSERKER,Talent.ENRAGED_CATALYST,rank);
                Berserk rage=Buff.affect(hero,Berserk.class);
                assertEquals(1f,Weapon.Enchantment.genericProcChanceMultiplier(hero),.0001f);
                rage.damage(hero.HT*4);
                assertEquals(1f+.15f*rank,Weapon.Enchantment.genericProcChanceMultiplier(hero),.0001f);
                assertEquals(1f,Weapon.Enchantment.genericProcChanceMultiplier(game.hero(1-seat)),.0001f);
            }
        }
    }

    @Test public void wardenTramplingAndSurplusDewUseOnlyTheInvestorsRank() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            Bundle save;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Hero hero=prepare(game,seat,HeroSubClass.WARDEN,Talent.BARKSKIN,rank);
                prepare(game,seat,HeroSubClass.WARDEN,Talent.SHIELDING_DEW,rank);
                Level.set(hero.pos,Terrain.HIGH_GRASS);
                HighGrass.trample(Dungeon.level,hero.pos);
                assertEquals(Terrain.FURROWED_GRASS,Dungeon.level.map[hero.pos]);
                assertEquals(rank*hero.lvl/3,hero.buff(Barkskin.class)==null ? 0 : hero.buff(Barkskin.class).level());
                hero.HP=hero.HT;
                assertEquals(rank>0,Dewdrop.consumeDew(20,hero,false));
                assertEquals(Math.round(.2f*rank*hero.HT),hero.shielding());
                assertEquals(0,game.hero(1-seat).shielding());
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                assertEquals(Math.round(.2f*rank*game.hero(seat).HT),game.hero(seat).shielding());
            }
        }
    }

    @Test public void scoutFiresFromOwnBowAndRestoresThePrivateRevealedArea() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            Bundle save;
            int cell;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Hero hero=prepare(game,seat,HeroSubClass.SNIPER,Talent.SEER_SHOT,rank);
                cell=-1;
                for (int delta:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int pos=hero.pos+delta;
                    if (Dungeon.level.passable[pos] && Actor.findChar(pos)==null) { cell=pos; break; }
                }
                assertTrue(cell>=0);
                SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
                assertEquals(rank>0,CoopFloorsTest.step(game,CoopSession.Action.SHOOT,cell,bow));
                if (rank==0) continue;
                assertEquals(cell,hero.buff(RevealedArea.class).pos);
                assertNull(game.hero(1-seat).buff(RevealedArea.class));
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                assertEquals(cell,hero.buff(RevealedArea.class).pos);
                SewerBeeTest.offer(game,seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.SHOOT,cell,hero.belongings.getItem(SpiritBow.class)));
            }
        }
    }

    @Test public void cleanseChargesOnlyItsCastersTomeAndLeavesTheOtherHeroAlone() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            Bundle save;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Hero hero=prepare(game,seat,HeroSubClass.PRIEST,Talent.CLEANSE,rank);
                HolyTome tome=hero.belongings.getItem(HolyTome.class);
                int charges=tome.charges();
                Hero peer=game.hero(1-seat);
                Buff.prolong(hero,Cripple.class,20);
                Buff.prolong(peer,Cripple.class,20);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.CLEANSE,peer.pos,tome));
                assertEquals(rank>0,CoopFloorsTest.step(game,CoopSession.Action.CLEANSE,hero.pos,tome));
                if (rank==0) continue;
                assertEquals(charges-2,tome.charges());
                assertNull(hero.buff(Cripple.class));
                assertNotNull(peer.buff(Cripple.class));
                assertTrue(hero.shielding()>=10*rank-1);
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                assertTrue(game.hero(seat).shielding()>0);
                TenguArenaTest.waitOne(game);
            }
        }
    }
    static StagedInput stage(CoopSession game, int seat, String kind) {
        Hero hero=game.hero(seat);
        switch (kind) {
            case "poisontip": {
                prepare(game,seat,HeroSubClass.WARDEN,Talent.DURABLE_TIPS,0);
                Rat foe=(Rat)Dungeon.level.mobs.iterator().next();
                Buff.prolong(foe,Paralysis.class,12);
                PoisonDart dart=new PoisonDart();
                dart.quantity(1); dart.identify(false);
                assertTrue(dart.collect(hero.belongings.backpack));
                game.ownership.claim(dart,game.player(seat));
                return new StagedInput(game,"THROW",foe.pos);
            }
            case "holylance":
                prepare(game,seat,HeroSubClass.PRIEST,Talent.HOLY_LANCE,2);
                powerTome(hero);
                return new StagedInput(game,"HOLY_LANCE",Dungeon.level.mobs.iterator().next().pos);
            case "cleanse":
                prepare(game,seat,HeroSubClass.PRIEST,Talent.CLEANSE,2);
                Buff.prolong(hero,Cripple.class,10);
                return new StagedInput(game,"CLEANSE",hero.pos);
            case "scoutshot":
                prepare(game,seat,HeroSubClass.SNIPER,Talent.SEER_SHOT,2);
                for (int delta:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int cell=hero.pos+delta;
                    if (Dungeon.level.passable[cell] && Actor.findChar(cell)==null) return new StagedInput(game,"SHOOT",cell);
                }
                throw new AssertionError("Missing visible aim");
            case "deathlesswait":
                prepare(game,seat,HeroSubClass.BERSERKER,Talent.DEATHLESS_FURY,1);
                hero.HP=13;
                Buff.affect(hero,Berserk.class).damage(4*hero.HT);
                hero.damage(100,Rat.class);
                return new StagedInput(game,"WAIT",hero.pos);
            default:
                prepare(game,seat,HeroSubClass.GLADIATOR,Talent.HOLD_FAST,0);
                return new StagedInput(game,"INVEST_TALENT",Talent.HOLD_FAST.ordinal());
        }
    }

    static boolean published(CoopSession game, int seat, String kind) {
        Hero hero=game.hero(seat);
        switch (kind) {
            case "poisontip":
                for (var mob:Dungeon.level.mobs) {
                    PinCushion pins=mob.buff(PinCushion.class);
                    if (pins!=null && mob.buff(Poison.class)!=null) for (var shaft:pins.getStuckItems()) {
                        if (shaft.getClass()==Dart.class && game.ownership.owner(shaft)==game.player(seat)) return true;
                    }
                }
                return false;
            case "holylance": return hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance.LanceCooldown.class)!=null;
            case "cleanse": return hero.buff(Cripple.class)==null && hero.shielding()>0 && hero.belongings.getItem(HolyTome.class).charges()<3;
            case "scoutshot": return hero.buff(RevealedArea.class)!=null && game.hero(1-seat).buff(RevealedArea.class)==null;
            case "deathlesswait": return hero.HP==0 && hero.isAlive() && game.preDeathHP(seat)==13;
            default: return hero.pointsInTalent(Talent.HOLD_FAST)==1 && game.hero(1-seat).pointsInTalent(Talent.HOLD_FAST)==0;
        }
    }

    @Test public void sniperDirectSightGrowsOnlyInTheInvestorsUnobstructedCorridor() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) try(CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
            game.join(22,HeroClass.HUNTRESS);
            Hero hero=prepare(game,seat,HeroSubClass.SNIPER,Talent.FARSIGHT,rank);
            int w=Dungeon.level.width(), origin=w*3+2;
            for(int i=0;i<=9;i++) Level.set(origin+i,Terrain.EMPTY);
            hero.pos=origin;
            hero.viewDistance=4;
            FloorKnowledge sight=game.observe(seat);
            assertTrue(sight.visible(origin+4+rank));
            assertFalse(sight.visible(origin+5+rank));
            assertEquals(0,game.hero(1-seat).pointsInTalent(Talent.FARSIGHT));
        }
    }

    @Test public void nativeMarkedDamageRestoresOnlyTheMarkCastersHealthAndHunger() {
        for (int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            Rat foe=CoopMageTest.encounter(game,seat);
            Hero caster=prepare(game,seat,HeroSubClass.WARLOCK,Talent.SOUL_SIPHON,rank);
            prepare(game,seat,HeroSubClass.WARLOCK,Talent.SOUL_EATER,rank);
            Hero peer=game.hero(1-seat);
            SoulMark.applyFrom(caster,foe,10);
            caster.HT=100; caster.HP=10;
            foe.HT=foe.HP=100;
            Hunger hunger=Buff.affect(caster,Hunger.class);
            hunger.affectHunger(-100);
            int before=hunger.hunger(), peerHP=peer.HP;
            foe.defenseProc(peer,30);
            int restored=4*rank;
            assertEquals(10+(int)Math.ceil(restored*.4f),caster.HP);
            assertEquals(before-(int)Math.floor(restored*rank/3f),hunger.hunger());
            assertEquals(peerHP,peer.HP);
            assertSame(caster,SoulMark.source(foe));
            assertNotNull(game.checkpoint());
        }
    }

    @Test public void fullImbuedStaffGivesTheActualBattlemageItsShieldAndNextMeleeProc() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            Rat foe=CoopMageTest.encounter(game,seat);
            Hero hero=prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.EMPOWERED_STRIKE,rank);
            prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.EXCESS_CHARGE,rank);
            com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff staff=
                    (com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff)hero.belongings.weapon();
            staff.upgrade(4);
            staff.imbuedWand().curCharges=staff.imbuedWand().maxCharges;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,foe.pos,staff));
            assertEquals(rank>0,hero.buff(Talent.EmpoweredStrikeTracker.class)!=null);
            if(rank>0) assertTrue(hero.shielding()>=Math.round(4*.67f*rank)-1);
            assertNull(game.hero(1-seat).buff(Talent.EmpoweredStrikeTracker.class));
            assertEquals(1f+rank/2f,com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.procChanceMultiplier(hero),.001f);
            staff.proc(hero,foe,12);
            assertNull(hero.buff(Talent.EmpoweredStrikeTracker.class));
            assertNotNull(game.checkpoint());
        }
    }

    @Test public void preparationAppliesTheOriginalExecutionThresholdAndBlinkRange() {
        float[] threshold={.10f,.13f,.17f,.20f};
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) try(CoopSession game=CoopSession.open(572,11,HeroClass.ROGUE)) {
            game.join(22,HeroClass.ROGUE);
            Rat foe=CoopMageTest.encounter(game,seat);
            Hero hero=prepare(game,seat,HeroSubClass.ASSASSIN,Talent.ENHANCED_LETHALITY,rank);
            prepare(game,seat,HeroSubClass.ASSASSIN,Talent.ASSASSINS_REACH,rank);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLOAK,0,hero.belongings.artifact));
            Preparation prep=hero.buff(Preparation.class);
            for(int i=0;prep.attackLevel()<2 && i<4;i++) prep.act();
            assertEquals(2,prep.attackLevel());
            assertEquals(2+rank,Preparation.AttackLevel.LVL_2.blinkDistance(hero));
            foe.HT=100; foe.HP=Math.round(threshold[rank]*100);
            assertFalse(prep.canKO(foe));
            foe.HP--;
            assertTrue(prep.canKO(foe));
            SewerBeeTest.offer(game,seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ASSASSINATE,foe.pos,null));
            assertFalse(foe.isAlive());
            assertNotNull(game.checkpoint());
        }
    }

    @Test public void vigorUsesOwnersMonkEnergyForAnActualFocus() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) try(CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
            game.join(22,HeroClass.DUELIST);
            Hero hero=prepare(game,seat,HeroSubClass.MONK,Talent.MONASTIC_VIGOR,rank);
            MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);
            energy.energy=energy.energyCap();
            assertEquals(rank>0,energy.abilitiesEmpowered(hero));
            energy.energy=.81f*energy.energyCap();
            assertEquals(rank>=2,energy.abilitiesEmpowered(hero));
            energy.energy=.61f*energy.energyCap();
            assertEquals(rank>=3,energy.abilitiesEmpowered(hero));
            float before=energy.energy;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MONK_FOCUS,hero.pos,null));
            assertTrue(energy.energy<before);
            assertNull(game.hero(1-seat).buff(MonkEnergy.class));
            assertNotNull(game.checkpoint());
        }
    }

    static void powerTome(Hero hero) {
        HolyTome tome=hero.belongings.getItem(HolyTome.class);
        while (tome.capacity()<5) tome.upgrade();
        tome.charge(hero,24);
    }

    @Test public void lanceChecksThePriestsActualTomeTargetDamageAndCooldown() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) {
            Bundle save;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Rat foe=CoopMageTest.encounter(game,seat);
                Hero hero=prepare(game,seat,HeroSubClass.PRIEST,Talent.HOLY_LANCE,rank);
                HolyTome tome=hero.belongings.getItem(HolyTome.class);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.HOLY_LANCE,foe.pos,tome));
                powerTome(hero);
                int old=tome.charges(),hp=foe.HP;
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.HOLY_LANCE,game.hero(1-seat).pos,tome));
                assertEquals(rank>0,CoopFloorsTest.step(game,CoopSession.Action.HOLY_LANCE,foe.pos,tome));
                if(rank==0) continue;
                assertTrue(hp-foe.HP >= 15+15*rank && hp-foe.HP <= Math.round(27.5f*(1+rank)));
                assertNotNull(foe.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class));
                assertEquals(old-4,tome.charges());
                assertNull(game.hero(1-seat).buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance.LanceCooldown.class));
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                assertNotNull(game.hero(seat).buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance.LanceCooldown.class));
            }
        }
    }

    @Test public void upgradedSetGivesTheSniperItsOriginalMarkBonusAndOwnTracker() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) {
            Bundle save;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Rat foe=CoopMageTest.encounter(game,seat);
                Hero hero=prepare(game,seat,HeroSubClass.SNIPER,Talent.SHARED_UPGRADES,rank);
                var missile=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone();
                missile.identify(false);
                assertTrue(missile.collect(hero.belongings.backpack));
                game.ownership.claim(missile,game.player(seat));
                var oldUnit=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone();
                oldUnit.setID=missile.setID;
                var scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade();
                scroll.setKnown(); scroll.quantity(6);
                assertTrue(scroll.collect(hero.belongings.backpack));
                game.ownership.claim(scroll,game.player(seat));
                for(int i=0;i<6;i++) {
                    SewerBeeTest.offer(game,seat);
                    int index=0;
                    for(var item:hero.belongings) { index++; if(item==missile) break; }
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.UPGRADE,index,scroll));
                    assertEquals(i+1,missile.trueLevel());
                }
                assertNotNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon.UpgradedSetTracker.class));
                assertFalse(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon.UpgradedSetTracker.pickupValid(hero,oldUnit));
                assertTrue(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon.UpgradedSetTracker.pickupValid(game.hero(1-seat),oldUnit));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,foe.pos,missile));
                SewerBeeTest.offer(game,seat);
                SnipersMark mark=hero.buff(SnipersMark.class);
                assertNotNull(mark);
                assertEquals(rank/3f,mark.percentDmgBonus,.0001f);
                assertEquals(foe.id(),mark.object);
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                SnipersMark mark=hero.buff(SnipersMark.class);
                assertEquals(rank/3f,mark.percentDmgBonus,.0001f);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SNIPER_SHOT,Actor.findById(mark.object) instanceof Rat ? ((Rat)Actor.findById(mark.object)).pos : -1,null));
                assertNull(hero.buff(SnipersMark.class));
            }
        }
    }
    @Test public void tippedHitsKeepTheOwnersShaftThroughPoisonBlindnessAndSave() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) for(boolean blind:new boolean[]{false,true}) {
            Bundle save;
            long id;
            int target;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Rat foe=CoopMageTest.encounter(game,seat);
                Hero hero=prepare(game,seat,HeroSubClass.WARDEN,Talent.DURABLE_TIPS,rank);
                TippedDart tip=blind ? new BlindingDart() : new PoisonDart();
                tip.quantity(1); tip.identify(false);
                assertTrue(tip.collect(hero.belongings.backpack));
                game.ownership.claim(tip,game.player(seat));
                id=game.ownership.identity(tip);
                target=foe.id();
                Buff.prolong(foe,Paralysis.class,12);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,foe.pos,tip));
                assertNotNull(blind ? foe.buff(Blindness.class) : foe.buff(Poison.class));
                Item shaft=foe.buff(PinCushion.class).getStuckItems().get(0);
                assertEquals(rank==0 ? Dart.class : tip.getClass(),shaft.getClass());
                assertEquals(id,game.ownership.identity(shaft));
                assertEquals(game.player(seat),game.ownership.owner(shaft));
                if(rank>0) {
                    Bundle fields=new Bundle();
                    shaft.storeInBundle(fields);
                    assertEquals(100f-(100f/(1+rank)+.001f),fields.getFloat("durability"),.01f);
                }
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                Rat foe=(Rat)Actor.findById(target);
                Item shaft=foe.buff(PinCushion.class).getStuckItems().get(0);
                assertEquals(id,game.ownership.identity(shaft));
                int floor=foe.pos;
                foe.die(game.hero(seat));
                assertTrue(Dungeon.level.heaps.get(floor).items.contains(shaft));
                assertFalse(game.ownership.allowed(shaft,game.player(1-seat)));
                Hero hero=game.hero(seat);
                hero.pos=floor;
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,floor,shaft));
                assertTrue(hero.belongings.contains(shaft));
                assertEquals(id,game.ownership.identity(shaft));
            }
        }
    }

    @Test public void freerunArmorAndThrownDamageUseTheRunnersOwnGearAndRank() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) try(CoopSession game=CoopSession.open(572,11,HeroClass.ROGUE)) {
            game.join(22,HeroClass.ROGUE);
            Rat foe=CoopMageTest.encounter(game,seat);
            Hero hero=prepare(game,seat,HeroSubClass.FREERUNNER,Talent.EVASIVE_ARMOR,rank);
            prepare(game,seat,HeroSubClass.FREERUNNER,Talent.PROJECTILE_MOMENTUM,rank);
            var stone=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone();
            assertTrue(stone.collect(hero.belongings.backpack));
            game.ownership.claim(stone,game.player(seat));
            com.watabou.utils.Random.pushGenerator(572);
            int regular;
            try { regular=stone.damageRoll(hero); } finally { com.watabou.utils.Random.popGenerator(); }
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,hero.pos-1,null));
            SewerBeeTest.offer(game,seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.FREERUN,hero.pos,null));
            Momentum momentum=hero.buff(Momentum.class);
            assertTrue(momentum.freerunning());
            assertEquals(hero.lvl/2+3*rank,momentum.evasionBonus(hero.lvl,3));
            com.watabou.utils.Random.pushGenerator(572);
            try { assertEquals(Math.round(regular*(1f+.15f*rank)),stone.damageRoll(hero)); }
            finally { com.watabou.utils.Random.popGenerator(); }
            Buff.prolong(foe,Paralysis.class,10);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,foe.pos,stone));
            assertTrue(foe.HP<foe.HT);
            assertEquals(0,game.hero(1-seat).pointsInTalent(Talent.PROJECTILE_MOMENTUM));
            assertNotNull(game.checkpoint());
        }
    }

    @Test public void cloakBuildsOrFreezesMomentumAtTheNativeSpeedyStealthThresholds() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) {
            Bundle save;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.ROGUE)) {
                game.join(22,HeroClass.ROGUE);
                Hero hero=prepare(game,seat,HeroSubClass.FREERUNNER,Talent.SPEEDY_STEALTH,rank);
                Momentum momentum=Buff.affect(hero,Momentum.class);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLOAK,0,hero.belongings.artifact));
                assertTrue(hero.invisible>0);
                momentum.act();
                Bundle b=new Bundle();
                momentum.storeInBundle(b);
                assertEquals(rank>0,b.getInt("stacks")>=2);
                assertEquals(rank==3 ? 2f : 1f,momentum.speedMultiplier(),0f);
                if(rank==0) continue;
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.FREERUN,hero.pos,null));
                momentum.storeInBundle(b);
                int turns=b.getInt("freerun_turns");
                momentum.act(); momentum.storeInBundle(b);
                assertEquals(turns-(rank==1?1:0),b.getInt("freerun_turns"));
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                assertTrue(game.hero(seat).buff(Momentum.class).freerunning());
                assertEquals(0,game.hero(1-seat).invisible);
            }
        }
    }
}
