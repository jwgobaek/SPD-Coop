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
                TenguArenaTest.waitOne(g);
                assertTrue(rat.HP<hp);
                assertNull(g.hero(seat).buff(Combo.RiposteTracker.class));
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
        case "slam": case "fury": case "parry": case "riposte":
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
        case "slam": case "fury":
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
        Dungeon.level.mobs.stream().sorted(java.util.Comparator.comparingInt(Actor::id)).forEach(m->result.append(m.id()).append('/').append(m.HP).append('/').append(m.pos).append('|'));
        return result.toString();
    }
}
