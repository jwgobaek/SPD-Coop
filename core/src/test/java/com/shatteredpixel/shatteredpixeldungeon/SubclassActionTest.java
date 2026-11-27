package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class SubclassActionTest extends HeadlessDungeon {
    static Combo prepare(CoopSession game, int seat, Rat foe, int hits) {
        Hero hero = game.hero(seat);
        hero.subClass = HeroSubClass.GLADIATOR;
        Talent.initSubclassTalents(hero);
        CavesBossTest.beside(game,seat,foe.pos);
        SewerBeeTest.offer(game,seat);
        Combo combo = Buff.affect(hero,Combo.class);
        for (int n=0; n<hits; n++) combo.hit(foe);
        return combo;
    }

    @Test public void fiveNativeFinishersResolveForEachSeatAndRestoreAtTheNextOffer() {
        for (int seat=0; seat<2; seat++) for (Combo.ComboMove move : Combo.ComboMove.values()) {
            Bundle saved;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Rat enemy=CoopMageTest.encounter(game,seat);
                Combo combo=prepare(game,seat,enemy,move.comboReq);
                Hero hero=game.hero(seat);
                assertNull(game.hero(1-seat).buff(Combo.class));
                assertTrue(ProjectionChecks.seatImage(game,seat).source.rows.containsKey("finish"+move.ordinal()));
                assertFalse(ProjectionChecks.seatImage(game,1-seat).source.rows.containsKey("combocount"));
                CoopSession.Action action=CoopSession.Action.valueOf(move.name());
                assertFalse(CoopFloorsTest.step(game,action,game.hero(1-seat).pos,null));
                int pos=enemy.pos, hp=enemy.HP;
                // CRUSH halves its splash roll before subtracting the second rat's actual DR.
                if (move == Combo.ComboMove.CRUSH) hero.belongings.weapon().upgrade(3);
                Rat other = new Rat();
                other.pos=hero.pos+Dungeon.level.width();
                other.HT=other.HP=120;
                if (Actor.findChar(other.pos)!=null) other.pos=enemy.pos+1;
                GameScene.add(other,2000);
                assertTrue(move.name(),CoopFloorsTest.step(game,action,move==Combo.ComboMove.PARRY?hero.pos:pos,null));
                switch (move) {
                    case CLOBBER:
                        assertNotEquals(pos,enemy.pos);
                        assertFalse(combo.canUseMove(move));
                        assertEquals(3,combo.getComboCount());
                        break;
                    case PARRY:
                        assertFalse(combo.canUseMove(move));
                        break;
                    case CRUSH:
                        assertTrue(other.HP < other.HT);
                        // fall through
                    default:
                        assertTrue(enemy.HP<hp);
                        assertNull(hero.buff(Combo.class));
                }
                saved=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(saved)) {
                assertNull(game.hero(1-seat).buff(Combo.class));
                assertNotNull(ProjectionChecks.seatImage(game,seat));
                TenguArenaTest.waitOne(game);
            }
        }
    }

    @Test public void parryTrackerAndRiposteReconnectToTheOriginalEnemy() {
        for(int seat=0;seat<2;seat++) {
            Bundle saved;
            int targetId;
            try(CoopSession g=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                g.join(22,HeroClass.WARRIOR);
                Rat rat=CoopMageTest.encounter(g,seat);
                Combo combo=prepare(g,seat,rat,6);
                Hero h=g.hero(seat);
                // A base sword and a rat can both roll 1. Give this damage assertion a positive lower bound.
                h.belongings.weapon().upgrade(2);
                Buff.affect(h,Combo.ParryTracker.class,Actor.TICK);
                int hp=h.HP;
                rat.attack(h,1,0,1f);
                assertEquals(hp,h.HP);
                assertNotNull(h.buff(Combo.RiposteTracker.class));
                assertEquals(6,combo.getComboCount());
                targetId=rat.id();
                saved=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(saved)) {
                Rat rat=(Rat)Actor.findById(targetId);
                int hp=rat.HP;
                for (int i=0;i<3 && g.hero(seat).buff(Combo.RiposteTracker.class)!=null;i++) TenguArenaTest.waitOne(g);
                assertNull(g.hero(seat).buff(Combo.RiposteTracker.class));
                assertTrue(rat.HP<hp);
            }
        }
    }

    @Test public void manualRageUsesOnlyItsOwnersSealAndNoNativeTurn() {
        for(int seat=0;seat<2;seat++) try(CoopSession g=CoopSession.open(571,11,HeroClass.WARRIOR)) {
            g.join(22,HeroClass.WARRIOR);
            Rat rat=CoopMageTest.encounter(g,seat);
            SewerBeeTest.offer(g,seat);
            Hero h=g.hero(seat);
            h.subClass=HeroSubClass.BERSERKER;
            assertFalse(CoopFloorsTest.step(g,CoopSession.Action.BERSERK,h.pos,null));
            Berserk rage=Buff.affect(h,Berserk.class);
            assertNotNull(h.buff(WarriorShield.class));
            rage.damage(h.HT*4);
            int boost=rage.currentShieldBoost();
            long turn=g.turn();
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.BERSERK,h.pos,null));
            assertTrue(rage.berserking());
            assertEquals(boost,rage.shielding());
            assertEquals(turn,g.turn());
            assertFalse(CoopFloorsTest.step(g,CoopSession.Action.BERSERK,h.pos,null));
            assertNull(g.hero(1-seat).buff(Berserk.class));
            g.checkpoint();
        }
    }

    @Test public void markedEnemyAndBowRemainSeatLocalForAllThreeAugments() {
        for(int seat=0;seat<2;seat++) for(Weapon.Augment aug:Weapon.Augment.values()) {
            Bundle save;
            int pos;
            try(CoopSession g=CoopSession.open(578,11,HeroClass.HUNTRESS)) {
                g.join(22,HeroClass.HUNTRESS);
                Rat rat=CoopMageTest.encounter(g,seat);
                SewerBeeTest.offer(g,seat);
                Hero h=g.hero(seat);
                h.subClass=HeroSubClass.SNIPER;
                h.belongings.getItem(SpiritBow.class).augment=aug;
                Buff.prolong(h,SnipersMark.class,4f).set(rat.id(),0.25f);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.SNIPER_SHOT,g.hero(1-seat).pos,null));
                pos=rat.pos;
                save=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(save)) {
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.SNIPER_SHOT,pos,null));
                assertNull(g.hero(seat).buff(SnipersMark.class));
                assertNull(g.hero(1-seat).buff(SnipersMark.class));
                assertFalse(g.hero(seat).belongings.getItem(SpiritBow.class).sniperSpecial);
                g.checkpoint();
            }
        }
    }

    @Test public void nativeSecondTierMealUsesItsOwnPointPoolAndBuff() {
        for(int seat=0;seat<2;seat++) for(HeroClass type:new HeroClass[]{HeroClass.WARRIOR,HeroClass.MAGE,HeroClass.HUNTRESS}) {
            Talent meal = type==HeroClass.WARRIOR ? Talent.IRON_STOMACH : type==HeroClass.MAGE ? Talent.ENERGIZING_MEAL : Talent.INVIGORATING_MEAL;
            try(CoopSession g=CoopSession.open(570,11,type)) {
                g.join(22,type);
                CoopMageTest.encounter(g,seat);
                SewerBeeTest.offer(g,seat);
                Hero h=g.hero(seat);
                while(h.lvl<6) h.earnExp(h.maxExp(),Rat.class);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,meal.ordinal(),null));
                h.earnExp(h.maxExp(),Rat.class);
                int first=h.talentPointsAvailable(1);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,meal.ordinal(),null));
                assertEquals(first,h.talentPointsAvailable(1));
                assertEquals(0,h.talentPointsAvailable(2));
                assertEquals(0,g.hero(1-seat).pointsInTalent(meal));
                if (type==HeroClass.WARRIOR) {
                    // Inspect the one-turn effect during eating, before another actor can expire it.
                    Food food = h.belongings.getItem(Food.class);
                    food.execute(h,Food.AC_EAT);
                    assertNotNull(h.buff(Talent.WarriorFoodImmunity.class));
                    int hp=h.HP;
                    h.damage(8,food);
                    assertTrue(h.HP >= hp-2);
                    continue;
                }
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.EAT,0,h.belongings.getItem(Food.class)));
                if(type==HeroClass.MAGE) assertNotNull(h.buff(Recharging.class));
                else assertNotNull(h.buff(Haste.class));
                g.checkpoint();
            }
        }
    }

    static StagedInput stage(CoopSession g,String stage,int seat) {
        Rat rat=CoopMageTest.encounter(g,seat);
        SewerBeeTest.offer(g,seat);
        Hero h=g.hero(seat);
        String type=stage.substring(0,stage.length()-1);
        switch(type) {
        case "poisontip": case "holylance": case "cleanse": case "scoutshot": case "holdfast": case "deathlesswait": case "deathlesstrap":
            return TierGrowthTest.stage(g, seat, type);
        case "trinitymindmissilehistory": case "readrecharge": case "recallrecharge": case "recallid": case "trinitymind": case "trinityspirit": case "intuition": case "bless": case "trinitybodyhistory": case "trinitymindhistory": case "trinityspirithistory": case "trinityfirehistory": case "trinityblockhistory": case "trinitymindselect": case "trinityspiritselect": case "trinityselect": case "trinity": case "monkflurry": case "monkdash": case "monkdragonkick": case "monkmeditate": case "sparkimbue": case "spark": case "sparkblast": case "sparkvolley":
        case "sealdetach": case "sealattach": case "sealkeep": case "sealtransfer": case "rationthrow": case "strikeshock": case "strikeblock": case "strikevenom": case "strikekinetic": case "strikebloom": case "heroicenergy": case "challenge": case "elestrike": case "feint": case "ascendform": case "lightally": case "lightorder":
            return SealArtsTest.stage(g,type,seat,rat);
        case "swap": case "monkfocus": case "smite": case "radiance": case "ice": case "iceimbue": case "iceblast": case "icevolley": case "markdeath": case "clone": case "orderclone": case "sunray": case "divinesense": case "lightshield": case "guardshield": case "stance":
            return FrostSensesTest.stage(g,type,seat,rat);
        case "smoke": case "ambush": case "freerun": case "spike": case "lunge": case "charged": case "light": case "ward": case "blessweapon":
        case "hawk": case "direct": case "imbue": case "volley": return WandArtsTest.stage(g,type,seat,rat);
        case "battery":
            h.lvl=9;
            assertTrue(CoopFloorsTest.step(g, CoopSession.Action.INVEST_TALENT,Talent.SHIELD_BATTERY.ordinal(),null));
            return new StagedInput(g,"ZAP",h.pos);
        case "lethal":
            prepare(g,seat,rat,8);
            h.lvl=25;
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,Talent.LETHAL_DEFENSE.ordinal(),null));
            h.belongings.weapon().upgrade(12);
            Rat extra=new Rat();
            rat.HP=extra.HP=1;
            extra.pos=rat.pos+Dungeon.level.width();
            com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(extra,2000);
            return new StagedInput(g,"CRUSH",rat.pos);
        case "doubleparry": case "parryready":
            prepare(g,seat,rat,9);
            h.lvl=25;
            for (int i=0;i<2;i++) assertTrue(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,Talent.ENHANCED_COMBO.ordinal(),null));
            if(type.equals("parryready")) return new StagedInput(g,"PARRY",h.pos);
            Buff.affect(h,Combo.ParryTracker.class,Actor.TICK);
            int before=h.HP;
            rat.attack(h,1,0,1f);
            assertNotNull(h.buff(Combo.ParryTracker.class));
            rat.attack(h,1,0,1f);
            assertEquals(before,h.HP);
            assertNotNull(h.buff(Combo.RiposteTracker.class));
            return new StagedInput(g,"WAIT",0);
        case "leaptrap": case "enhanced":
            prepare(g,seat,rat,10);
            h.lvl=25;
            for (int i=0;i<3;i++) assertTrue(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,Talent.ENHANCED_COMBO.ordinal(),null));
            h.pos=rat.pos-3;
            if(type.equals("leaptrap")) {
                int dest=rat.pos-1;
                Dungeon.level.setTrap(new com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap(),dest);
                com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(dest,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_TRAP);
                Dungeon.level.traps.get(dest).hide();
            }
            g.observe(seat);
            return new StagedInput(g,"CLOBBER",rat.pos);
        case "blades": case "beacon": case "recall": case "endure":
            int ability=type.equals("blades")?0:2;
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,ability,CrownAbilityTest.crown(g,seat,CoopSession.MaskTrial.OWNER)));
            SewerBeeTest.offer(g,seat);
            if (type.equals("recall")) {
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ARMOR_ABILITY,h.pos,h.belongings.armor()));
                SewerBeeTest.offer(g,seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,h.pos-1,null));
                SewerBeeTest.offer(g,seat);
                return new StagedInput(g,"ARMOR_ABILITY",-1);
            }
            return new StagedInput(g,"ARMOR_ABILITY",type.equals("blades")?rat.pos:h.pos);
        case "cloak":
            return new StagedInput(g,"CLOAK",0);
        case "cachepickup": return CachedRationsTest.preparePickup(g,seat);
        case "cacheinvest":
            h.lvl=5;
            return new StagedInput(g,"INVEST_TALENT",Talent.CACHED_RATIONS.ordinal());
        case "strongman": case "endless_rage": case "desperate_power": case "point_blank":
            h.subClass = h.heroClass.subClasses()[0];
            Talent.initSubclassTalents(h);
            h.lvl = 25;
            return new StagedInput(g, "INVEST_TALENT", Talent.valueOf(type.toUpperCase(java.util.Locale.ROOT)).ordinal());
        case "third":
            h.subClass=HeroSubClass.GLADIATOR;
            Talent.initSubclassTalents(h);
            h.lvl=18;
            return new StagedInput(g,"INVEST_TALENT",Talent.CLEAVE.ordinal());
        case "shock":
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,1,CrownAbilityTest.crown(g,seat,CoopSession.MaskTrial.OWNER)));
            SewerBeeTest.offer(g,seat);
            return new StagedInput(g,"ARMOR_ABILITY",rat.pos);
        case "clobber": case "slam": case "crush": case "fury": case "parry": case "riposte":
            Combo.ComboMove move=type.equals("riposte")?Combo.ComboMove.PARRY:Combo.ComboMove.valueOf(type.toUpperCase(java.util.Locale.ROOT));
            prepare(g,seat,rat,move.comboReq);
            if(type.equals("riposte")) {
                h.belongings.weapon().upgrade(3);
                Buff.affect(h,Combo.ParryTracker.class,Actor.TICK);
                rat.attack(h,1,0,1f);
                assertNotNull(h.buff(Combo.RiposteTracker.class));
                return new StagedInput(g,"WAIT",0);
            }
            return new StagedInput(g,move.name(),move==Combo.ComboMove.PARRY?h.pos:rat.pos);
        case "rage":
            h.subClass=HeroSubClass.BERSERKER;
            Talent.initSubclassTalents(h);
            Buff.affect(h,Berserk.class).damage(h.HT*4);
            return new StagedInput(g,"BERSERK",h.pos);
        case "sniper":
            h.subClass=HeroSubClass.SNIPER;
            Talent.initSubclassTalents(h);
            Buff.prolong(h,SnipersMark.class,4f).set(rat.id(),0.25f);
            return new StagedInput(g,"SNIPER_SHOT",rat.pos);
        case "warlock": case "battlemage":
            g.beginMaskTrial(CoopSession.MaskTrial.OWNER);
            com.shatteredpixel.shatteredpixeldungeon.items.TengusMask mask=new com.shatteredpixel.shatteredpixeldungeon.items.TengusMask();
            assertTrue(mask.collect(h.belongings.backpack));
            g.ownership.claim(mask,g.player(seat));
            return new StagedInput(g,"USE_MASK",HeroSubClass.valueOf(type.toUpperCase(java.util.Locale.ROOT)).ordinal());
        case "soul":
            h.subClass=HeroSubClass.WARLOCK;
            Talent.initSubclassTalents(h);
            // A staged high-level staff makes the native mark chance certain. Damage still comes from the original wand.
            h.belongings.weapon().upgrade(33);
            rat.HT=rat.HP=1000;
            return new StagedInput(g,"ZAP",rat.pos);
        case "armorright":
            SewerBeeTest.offer(g,1-seat);
            com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown ownerCrown=CrownAbilityTest.crown(g,1-seat,CoopSession.MaskTrial.ROSTER);
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,CrownAbilityTest.choice(g.hero(1-seat)),ownerCrown));
            SewerBeeTest.offer(g,seat);
            return new StagedInput(g,"CLAIM_ARMOR",CrownAbilityTest.choice(h));
        case "meal":
            while(h.lvl<7) h.earnExp(h.maxExp(),Rat.class);
            return new StagedInput(g,"INVEST_TALENT",Talent.ENERGIZING_MEAL.ordinal());
        default:
            com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown crown=CrownAbilityTest.crown(g,seat,CoopSession.MaskTrial.ROSTER);
            int choice=CrownAbilityTest.choice(h);
            if(type.equals("crown")) return new StagedInput(g,"USE_CROWN",choice);
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,choice,crown));
            SewerBeeTest.offer(g,seat);
            return new StagedInput(g,"ARMOR_ABILITY",CrownAbilityTest.destination(h));
        }
    }

    static boolean published(CoopSession g,String stage,int seat) {
        Hero h=g.hero(seat);
        Combo combo=h.buff(Combo.class);
        String type=stage.substring(0,stage.length()-1);
        switch(type) {
        case "poisontip": case "holylance": case "cleanse": case "scoutshot": case "holdfast": case "deathlesswait": case "deathlesstrap":
            return TierGrowthTest.published(g, seat, type);
        case "trinitymindmissilehistory": case "readrecharge": case "recallrecharge": case "recallid": case "trinitymind": case "trinityspirit": case "intuition": case "bless": case "trinitybodyhistory": case "trinitymindhistory": case "trinityspirithistory": case "trinityfirehistory": case "trinityblockhistory": case "trinitymindselect": case "trinityspiritselect": case "trinityselect": case "trinity": case "monkflurry": case "monkdash": case "monkdragonkick": case "monkmeditate": case "sparkimbue": case "spark": case "sparkblast": case "sparkvolley":
        case "sealdetach": case "sealattach": case "sealkeep": case "sealtransfer": case "rationthrow": case "strikeshock": case "strikeblock": case "strikevenom": case "strikekinetic": case "strikebloom": case "heroicenergy": case "challenge": case "elestrike": case "feint": case "ascendform": case "lightally": case "lightorder":
            return SealArtsTest.published(g,type,seat);
        case "swap": case "monkfocus": case "smite": case "radiance": case "ice": case "iceimbue": case "iceblast": case "icevolley": case "markdeath": case "clone": case "orderclone": case "sunray": case "divinesense": case "lightshield": case "guardshield": case "stance":
            return FrostSensesTest.published(g,type,seat);
        case "smoke": case "ambush": case "freerun": case "spike": case "lunge": case "charged": case "light": case "ward": case "blessweapon":
        case "hawk": case "direct": case "imbue": case "volley": return WandArtsTest.published(g,type,seat);
        case "battery": return h.buff(Barrier.class)!=null && ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff)h.belongings.weapon()).starterWand().curCharges==0;
        case "lethal":
            Bundle seal=new Bundle();
            h.buff(com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class).storeInBundle(seal);
            return seal.getInt("cooldown")==-100 && Dungeon.level.mobs.stream().noneMatch(m->m instanceof Rat);
        case "parryready": return h.pointsInTalent(Talent.ENHANCED_COMBO)==2 && (combo==null || !combo.canUseMove(Combo.ComboMove.PARRY));
        case "doubleparry": return h.buff(Combo.RiposteTracker.class)==null && Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT);
        case "leaptrap": return Dungeon.level.map[h.pos]==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.INACTIVE_TRAP
                && combo!=null && !combo.canUseMove(Combo.ComboMove.CLOBBER);
        case "enhanced": return combo!=null && !combo.canUseMove(Combo.ComboMove.CLOBBER)
                && Dungeon.level.mobs.stream().anyMatch(m->m.buff(Vertigo.class)!=null);
        case "blades": return h.armorAbility instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades
                && ((com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)h.belongings.armor()).charge < 26;
        case "endure": return h.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker.class)!=null;
        case "beacon": return h.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.WarpBeaconTracker.class)!=null;
        case "recall": return h.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.WarpBeaconTracker.class)!=null
                && ((com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)h.belongings.armor()).charge<20;
        case "cloak": return h.invisible==1 && h.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows.cloakStealth.class)!=null;
        case "cachepickup": return CachedRationsTest.ownsCollectedCache(g,seat);
        case "cacheinvest": return h.pointsInTalent(Talent.CACHED_RATIONS)==1 && g.hero(1-seat).pointsInTalent(Talent.CACHED_RATIONS)==0;
        case "strongman": case "endless_rage": case "desperate_power": case "point_blank":
            Talent invested = Talent.valueOf(type.toUpperCase(java.util.Locale.ROOT));
            return h.pointsInTalent(invested) == 1 && g.hero(1-seat).pointsInTalent(invested) == 0;
        case "third": return h.pointsInTalent(Talent.CLEAVE)==1 && g.hero(1-seat).pointsInTalent(Talent.CLEAVE)==0;
        case "shock": return h.armorAbility instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave
                && Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT && m.buff(Cripple.class)!=null);
        case "clobber": return combo!=null && !combo.canUseMove(Combo.ComboMove.CLOBBER);
        case "warlock": case "battlemage": return h.subClass==HeroSubClass.valueOf(type.toUpperCase(java.util.Locale.ROOT)) &&
                h.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.TengusMask.class)==null;
        case "soul": return Dungeon.level.mobs.stream().anyMatch(m->SoulMark.source(m)==h);
        case "armorright": return h.armorAbility!=null && !g.mayChooseArmor(seat,false) && g.hero(1-seat).armorAbility!=null;
        case "slam": case "crush": case "fury":
            return combo==null && Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT);
        case "parry": return combo==null || !combo.canUseMove(Combo.ComboMove.PARRY);
        case "riposte": return h.buff(Combo.RiposteTracker.class)==null && Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT);
        case "rage": return h.buff(Berserk.class)!=null && h.buff(Berserk.class).berserking();
        case "sniper": return h.buff(SnipersMark.class)==null;
        case "meal": return h.pointsInTalent(Talent.ENERGIZING_MEAL)==1;
        case "crown": return h.armorAbility!=null && g.mayChooseArmor(1-seat,false);
        default: return h.armorAbility!=null && ((com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)h.belongings.armor()).charge<45;
        }
    }

    static String signature(CoopSession g) {
        StringBuilder result=new StringBuilder();
        for(int seat=0;seat<2;seat++) {
            Hero h=g.hero(seat);
            Combo combo=h.buff(Combo.class);
            result.append(h.pos).append('/').append(h.HP).append('/').append(h.shielding()).append('/')
                  .append(combo==null?"-":combo.getComboCount()+":"+combo.remainingTime()).append('/')
                  .append(h.armorAbility==null?"-":h.armorAbility.getClass().getSimpleName()).append('/')
                  .append(h.belongings.armor() instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor
                          ? ((com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)h.belongings.armor()).charge:0).append('/');
        }
        Dungeon.level.mobs.stream().sorted(java.util.Comparator.comparingInt(Actor::id)).forEach(m->result.append(m.id()).append('/').append(m.HP).append('/').append(m.pos).append('/').append(SoulMark.source(m)==null?0:SoulMark.source(m).id()).append('|'));
        return result.toString();
    }
}
