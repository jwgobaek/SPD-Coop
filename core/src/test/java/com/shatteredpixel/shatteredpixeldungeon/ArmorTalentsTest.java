package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class ArmorTalentsTest extends HeadlessDungeon {
    static ClassArmor invest(CoopSession game,int seat,int choice,Talent talent,int rank) {
        Hero hero=game.hero(seat);
        hero.lvl=30;
        SewerBeeTest.offer(game,seat);
        assertTrue(CoopFloorsTest.step(game,USE_CROWN,choice,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
        SewerBeeTest.offer(game,seat);
        for(int i=0;i<rank;i++)
            assertTrue(CoopFloorsTest.step(game,INVEST_TALENT,talent.ordinal(),null));
        assertEquals(rank,hero.pointsInTalent(talent));
        assertEquals(0,game.hero(1-seat).pointsInTalent(talent));
        return (ClassArmor)hero.belongings.armor();
    }

    static StagedInput stage(CoopSession game,int seat,String type,Rat rat) {
        Hero hero=game.hero(seat);
        switch(type) {
            case "remoteplace":
                invest(game,seat,2,Talent.REMOTE_BEACON,2);
                return new StagedInput(game,"ARMOR_ABILITY",hero.pos+1);
            case "ninjalog":
                invest(game,seat,0,Talent.BODY_REPLACEMENT,3);
                return new StagedInput(game,"ARMOR_ABILITY",hero.pos+1);
            case "rayblast": {
                invest(game,seat,0,Talent.REACTIVE_BARRIER,2);
                com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration wand=WandArtsTest.give(game,seat,
                        new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration());
                assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),hero.belongings.weapon()));
                SewerBeeTest.offer(game,seat);
                return new StagedInput(game,"ARMOR_ABILITY",hero.pos);
            }
            case "divineshield":
            case "beamingray":
            case "lifelink": {
                boolean ascended=type.equals("divineshield");
                Talent spell=ascended ? Talent.DIVINE_INTERVENTION : type.equals("lifelink") ? Talent.LIFE_LINK : Talent.BEAMING_RAY;
                ClassArmor armor=invest(game,seat,ascended ? 0:2,spell,3);
                TierGrowthTest.powerTome(hero);
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,ascended ? hero.pos:hero.pos+1,armor));
                SewerBeeTest.offer(game,seat);
                return new StagedInput(game,spell.name(),spell==Talent.BEAMING_RAY ? rat.pos:hero.pos);
            }
            default: throw new IllegalArgumentException(type);
        }
    }

    static boolean published(CoopSession game,int seat,String type) {
        Hero hero=game.hero(seat);
        switch(type) {
            case "remoteplace": return hero.buff(WarpBeacon.WarpBeaconTracker.class)!=null;
            case "ninjalog":
                for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:Dungeon.level.mobs)
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.NinjaLog
                            && ((com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.NinjaLog)mob).belongsTo(hero)) return true;
                return false;
            case "rayblast": return hero.buff(Barrier.class)!=null && hero.buff(Barrier.class).shielding()>0;
            case "divineshield": return hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class).divineInverventionCast;
            case "beamingray": return PowerOfMany.getPoweredAlly(hero).buff(BeamingRay.BeamingRayBoost.class)!=null;
            case "lifelink": return hero.buff(LifeLink.class)!=null && hero.buff(LifeLink.class).object==PowerOfMany.getPoweredAlly(hero).id();
            default: throw new IllegalArgumentException(type);
        }
    }

    @Test public void enduranceStoresTheWearersMitigationAndRetaliation() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.SHRUG_IT_OFF,Talent.SUSTAINED_RETRIBUTION,Talent.EVEN_THE_ODDS}) {
                Bundle saved;
                int hits=1+(talent==Talent.SUSTAINED_RETRIBUTION ? rank : 0);
                int bonus;
                try(CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                    game.join(22,HeroClass.WARRIOR);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    rat.pos=hero.pos+2;
                    ClassArmor armor=invest(game,seat,2,talent,rank);
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,hero.pos,armor));
                    Endure.EndureTracker tracker=hero.buff(Endure.EndureTracker.class);
                    assertEquals(50f*(talent==Talent.SHRUG_IT_OFF ? Math.pow(.8,rank) : 1),tracker.adjustDamageTaken(100),.001);
                    tracker.endEnduring();
                    Bundle effect=new Bundle(); tracker.storeInBundle(effect);
                    assertEquals(hits,effect.getInt(Endure.EndureTracker.HITS_LEFT));
                    int expected=(int)(50*(1f+(talent==Talent.SUSTAINED_RETRIBUTION ? .15f*rank : 0)));
                    expected=(int)(expected*(1f+(talent==Talent.EVEN_THE_ODDS ? .05f*rank : 0)));
                    bonus=expected/hits;
                    assertEquals(bonus,effect.getInt(Endure.EndureTracker.DAMAGE_BONUS));
                    saved=game.checkpoint();
                }
                try(CoopSession game=CoopSession.restore(saved)) {
                    Hero hero=game.hero(seat);
                    Endure.EndureTracker tracker=hero.buff(Endure.EndureTracker.class);
                    assertNull(game.hero(1-seat).buff(Endure.EndureTracker.class));
                    for(int i=0;i<hits;i++) assertEquals(10+bonus,tracker.damageFactor(10),0f);
                    assertNull(hero.buff(Endure.EndureTracker.class));
                }
            }
    }

    @Test public void leapDamageAndSecondLeapCostUseTheWearersInvestment() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.BODY_SLAM,Talent.DOUBLE_JUMP}) {
                Bundle saved;
                float nextCost;
                try(CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                    game.join(22,HeroClass.WARRIOR);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    ClassArmor armor=invest(game,seat,0,talent,rank);
                    float first=hero.armorAbility.chargeUse(hero);
                    int hp=rat.HP;
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,rat.pos-1,armor));
                    assertEquals(rat.pos-1,hero.pos);
                    if (talent==Talent.BODY_SLAM && rank>=2) assertTrue(rat.HP<hp);
                    if (rank==0 || talent==Talent.DOUBLE_JUMP) assertEquals(hp,rat.HP);
                    nextCost=hero.armorAbility.chargeUse(hero);
                    assertEquals(first*(talent==Talent.DOUBLE_JUMP ? Math.pow(.84,rank) : 1),nextCost,.001);
                    saved=game.checkpoint();
                }
                try(CoopSession game=CoopSession.restore(saved)) {
                    Hero hero=game.hero(seat);
                    assertEquals(nextCost,hero.armorAbility.chargeUse(hero),0f);
                    assertEquals(talent==Talent.DOUBLE_JUMP && rank>0,hero.buff(HeroicLeap.DoubleJumpTracker.class)!=null);
                }
            }
    }

    @Test public void shockwaveUsesTheWearersConeAndProcPoints() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.EXPANDING_WAVE,Talent.STRIKING_WAVE,Talent.SHOCK_FORCE}) {
                try(CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                    game.join(22,HeroClass.WARRIOR);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    int aim=hero.pos+3;
                    if (talent==Talent.EXPANDING_WAVE) {
                        aim=hero.pos+6;
                        for(int x=1;x<=6;x++) { Level.set(hero.pos+x,Terrain.EMPTY); Dungeon.level.discoverable[hero.pos+x]=true; }
                        rat.pos=aim;
                    }
                    ClassArmor armor=invest(game,seat,1,talent,rank);
                    game.observe(seat);
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,aim,armor));
                    assertEquals(talent!=Talent.EXPANDING_WAVE || rank>0,rat.HP<rat.HT);
                    if (talent==Talent.SHOCK_FORCE && rank==4) assertNotNull(rat.buff(Paralysis.class));
                    if (talent==Talent.SHOCK_FORCE && rank==0) assertNotNull(rat.buff(Cripple.class));
                }
            }
    }

    @Test public void wildMagicSpendsTheReadersPartialChargesAcrossRealShots() {
        int[] baselineDamage = new int[2];
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.FIRE_EVERYTHING,Talent.CONSERVED_MAGIC,Talent.WILD_POWER}) {
                Bundle saved;
                float totalAfter;
                try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                    game.join(22,HeroClass.MAGE);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    ClassArmor armor=invest(game,seat,1,talent,rank);
                    WandOfMagicMissile[] wands=new WandOfMagicMissile[8];
                    for(int i=0;i<wands.length;i++) {
                        wands[i]=WandArtsTest.give(game,seat,new WandOfMagicMissile());
                    }
                    SewerBeeTest.offer(game,seat);
                    for (WandOfMagicMissile wand : wands) { wand.curCharges=1; wand.partialCharge=.25f; }
                    int hp=rat.HP;
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,rat.pos,armor));
                    totalAfter=0;
                    int fired=0;
                    for(WandOfMagicMissile wand:wands) {
                        float charge=wand.curCharges+wand.partialCharge;
                        if (charge<1.24f) fired++;
                        totalAfter+=charge;
                    }
                    assertEquals(4+(talent==Talent.FIRE_EVERYTHING?rank:0),fired);
                    float cost=.5f*(float)Math.pow(.67,talent==Talent.CONSERVED_MAGIC?rank:0);
                    // The offered action also advances the native chargers, including those on unfired wands.
                    float regained = totalAfter - (10f-fired*cost);
                    assertTrue("post-volley recharge " + regained, regained >= -.001f && regained < .04f*wands.length);
                    if (talent == Talent.WILD_POWER) {
                        if (rank == 0) baselineDamage[seat] = hp-rat.HP;
                        if (rank == 4) assertTrue(hp-rat.HP > baselineDamage[seat]);
                    }
                    assertTrue(rat.HP<hp);
                    saved=game.checkpoint();
                }
                try(CoopSession restored=CoopSession.restore(saved)) {
                    float charge=0;
                    for(WandOfMagicMissile wand:restored.hero(seat).belongings.getAllItems(WandOfMagicMissile.class))
                        charge+=wand.curCharges+wand.partialCharge;
                    assertEquals(totalAfter,charge,.001f);
                }
            }
    }

    @Test public void landingWavePushesAwayFromTheWearerWithNativeVulnerability() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++) {
            try(CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                int start=rat.pos;
                for(int i=1;i<=6;i++) Level.set(start+i,Terrain.EMPTY);
                ClassArmor armor=invest(game,seat,0,Talent.IMPACT_WAVE,rank);
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,start-1,armor));
                assertEquals(start+(rank==0 ? 0 : rank+1),rat.pos);
                if(rank==4) assertNotNull(rat.buff(Vulnerable.class));
                assertNull(game.hero(1-seat).buff(Vulnerable.class));
            }
        }
    }

    @Test public void naturePowerAcceleratesOnlyTheArcherAndHerOwnBow() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++) {
            Bundle saved;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                ClassArmor armor=invest(game,seat,1,Talent.GROWING_POWER,rank);
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,hero.pos,armor));
                saved=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat), other=game.hero(1-seat);
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow bow=hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow.class);
                assertEquals(2f+.25f*rank,hero.speed(),.001);
                assertEquals(1f,other.speed(),.001);
                assertEquals(1f/(1f+(8f+rank)/24f),bow.delayFactor(hero),.001);
            }
        }
    }

    @Test public void bothDuelistsCanStrikeFurtherOrRaiseAStrongerBlockingShield() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.ELEMENTAL_REACH,Talent.STRIKING_FORCE}) {
                try(CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                    game.join(22,HeroClass.DUELIST);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    int target=hero.pos+(talent==Talent.ELEMENTAL_REACH ? 5 : 3);
                    for(int i=1;i<=6;i++) Level.set(hero.pos+i,Terrain.EMPTY);
                    rat.pos=target;
                    ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon)hero.belongings.weapon).enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking());
                    ClassArmor armor=invest(game,seat,1,talent,rank);
                    game.observe(seat);
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,target,armor));
                    Barrier shield=hero.buff(Barrier.class);
                    int expected=talent==Talent.ELEMENTAL_REACH ? (rank==0?0:6) : Math.round(6*(1+.30f*rank));
                    assertEquals(expected,shield==null ? 0 : shield.shielding());
                    assertNull(game.hero(1-seat).buff(Barrier.class));
                }
            }
    }

    @Test public void foesStrikingAfterimagesAffectOnlyThatDuelistAndTheAttacker() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.FEIGNED_RETREAT,Talent.EXPOSE_WEAKNESS,Talent.COUNTER_ABILITY}) {
                try(CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                    game.join(22,HeroClass.DUELIST);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    ClassArmor armor=invest(game,seat,2,talent,rank);
                    int start=hero.pos;
                    rat.pos=start-1;
                    rat.state=rat.HUNTING;
                    rat.aggro(hero);
                    rat.fixTime(rat.cooldown()-hero.cooldown()-.25f);
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,start+1,armor));
                    SewerBeeTest.offer(game,seat);
                    assertNotNull(rat.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint.AfterImage.FeintConfusion.class));
                    assertEquals(talent==Talent.FEIGNED_RETREAT && rank>0, hero.buff(Haste.class)!=null);
                    assertNull(game.hero(1-seat).buff(Haste.class));
                    assertEquals(talent==Talent.EXPOSE_WEAKNESS && rank>0,rat.buff(Weakness.class)!=null);
                    assertEquals(talent==Talent.EXPOSE_WEAKNESS && rank>0,rat.buff(Vulnerable.class)!=null);
                    assertEquals(talent==Talent.COUNTER_ABILITY && rank>0,hero.buff(Talent.CounterAbilityTacker.class)!=null);
                    if (talent==Talent.COUNTER_ABILITY) {
                        MeleeWeapon.Charger charge=hero.buff(MeleeWeapon.Charger.class);
                        float before=charge.charges+charge.partialCharge;
                        assertTrue(CoopFloorsTest.step(game,WEAPON_ABILITY,rat.pos,hero.belongings.weapon()));
                        assertEquals(before-1+rank*.375f,charge.charges+charge.partialCharge,.07f);
                        assertNull(hero.buff(Talent.CounterAbilityTacker.class));
                        assertNull(game.hero(1-seat).buff(Talent.CounterAbilityTacker.class));
                    }
                }
            }
    }

    @Test public void smokeBombUsesTheRoguesOwnHasteInvisibilityAndLog() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.HASTY_RETREAT,Talent.BODY_REPLACEMENT,Talent.SHADOW_STEP}) {
                Bundle saved;
                int start;
                try(CoopSession game=CoopSession.open(572,11,HeroClass.ROGUE)) {
                    game.join(22,HeroClass.ROGUE);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero rogue=game.hero(seat);
                    start=rogue.pos;
                    rat.pos=start+1;
                    ClassArmor armor=invest(game,seat,0,talent,rank);
                    float cost=rogue.armorAbility.chargeUse(rogue);
                    if(talent==Talent.SHADOW_STEP) {
                        assertTrue(CoopFloorsTest.step(game,CLOAK,0,rogue.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows.class)));
                        SewerBeeTest.offer(game,seat);
                        assertTrue(rogue.invisible>0);
                        assertEquals(cost*(float)Math.pow(.84,rank),rogue.armorAbility.chargeUse(rogue),.001);
                    }
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,start-1,armor));
                    assertEquals(start-1,rogue.pos);
                    assertEquals(talent!=Talent.SHADOW_STEP || rank==0,rat.buff(Blindness.class)!=null);
                    assertEquals(talent==Talent.HASTY_RETREAT && rank>0,rogue.buff(Haste.class)!=null);
                    assertNull(game.hero(1-seat).buff(Haste.class));
                    saved=game.checkpoint();
                }
                try(CoopSession game=CoopSession.restore(saved)) {
                    com.shatteredpixel.shatteredpixeldungeon.actors.Char log=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(start);
                    if (talent==Talent.BODY_REPLACEMENT && rank>0) {
                        assertTrue(log instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.NinjaLog);
                        assertSame(game.hero(seat),((com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.NinjaLog)log).owner());
                        assertEquals(20*rank,log.HT);
                        assertTrue(log.drRoll()>=rank);
                    } else assertNull(log);
                }
            }
    }

    @Test public void challengeApproachHealingAndNextChargeBelongToItsWearer() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.CLOSE_THE_GAP,Talent.INVIGORATING_VICTORY,Talent.ELIMINATION_MATCH}) {
                try(CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                    game.join(22,HeroClass.DUELIST);
                    Rat enemy=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    int origin=hero.pos;
                    ClassArmor armor=invest(game,seat,0,talent,rank);
                    if (talent==Talent.CLOSE_THE_GAP) {
                        enemy.pos=origin+5;
                        for (int i=1;i<=5;i++) Level.set(origin+i,Terrain.EMPTY);
                        game.observe(seat);
                    } else enemy.pos=origin+1;
                    hero.HP=5; hero.HT=60;
                    float normalCost=hero.armorAbility.chargeUse(hero);
                    int friendHP=game.hero(1-seat).HP;
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,enemy.pos,armor));
                    assertEquals(origin+(talent==Talent.CLOSE_THE_GAP && rank>0 ? Math.min(rank+1,4) : 0),hero.pos);
                    enemy.HP=1;
                    Buff.prolong(enemy,Paralysis.class,4);
                    if (talent!=Talent.CLOSE_THE_GAP) {
                        // The challenge reward requires the challenger to deliver a lethal blow.
                        SewerBeeTest.offer(game, seat);
                        hero.belongings.weapon().upgrade(3);
                        assertTrue(CoopFloorsTest.step(game,ATTACK,enemy.pos,null));
                        assertFalse(enemy.isAlive());
                        assertEquals(5+(talent==Talent.INVIGORATING_VICTORY ? 5*rank:0),hero.HP);
                        assertEquals(normalCost*(talent==Talent.ELIMINATION_MATCH ? Math.pow(.84,rank):1),hero.armorAbility.chargeUse(hero),.01);
                        assertEquals(friendHP,game.hero(1-seat).HP);
                    }
                }
            }
    }

    @Test public void ascendedSpellsUseTheReadersTomeAndPersistTheirCastLimits() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.DIVINE_INTERVENTION,Talent.JUDGEMENT,Talent.FLASH}) {
                Bundle saved;
                CoopSession.Action cast=CoopSession.Action.valueOf(talent.name());
                int cost=talent==Talent.DIVINE_INTERVENTION ? 5 : talent==Talent.JUDGEMENT ? 3 : 2;
                try(CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                    game.join(22,HeroClass.CLERIC);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    ClassArmor armor=invest(game,seat,0,talent,rank);
                    TierGrowthTest.powerTome(hero);
                    com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome.class);
                    int destination=talent==Talent.FLASH ? hero.pos-2 : hero.pos;
                    assertFalse(CoopFloorsTest.step(game,cast,destination,tome));
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,hero.pos,armor));
                    SewerBeeTest.offer(game,seat);
                    int charges=tome.charges(),hp=rat.HP;
                    assertEquals(rank>0,CoopFloorsTest.step(game,cast,destination,tome));
                    if(rank==0) continue;
                    assertEquals(charges-cost,tome.charges());
                    assertEquals(destination,hero.pos);
                    assertNull(game.hero(1-seat).buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineIntervention.DivineShield.class));
                    if (talent==Talent.JUDGEMENT) assertTrue(hp-rat.HP>=5+5*rank);
                    saved=game.checkpoint();
                }
                try(CoopSession game=CoopSession.restore(saved)) {
                    Hero hero=game.hero(seat);
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff buff=hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class);
                    assertNotNull(buff);
                    assertEquals(talent==Talent.DIVINE_INTERVENTION,buff.divineInverventionCast);
                    if (talent==Talent.DIVINE_INTERVENTION) assertEquals(100+50*rank,buff.shielding());
                    if (talent==Talent.FLASH) assertEquals(1,buff.flashCasts);
                    if (talent==Talent.JUDGEMENT) assertEquals(0,buff.spellCasts);
                    assertNull(game.hero(1-seat).buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class));
                }
            }
    }
    @Test public void remoteBeaconReusesOnlyTheCastersKnownReachableFloor() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=4;rank++) {
            Bundle saved;
            int destination;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                ClassArmor armor=invest(game,seat,2,Talent.REMOTE_BEACON,rank);
                armor.charge=100;
                destination=hero.pos+2;
                assertEquals(rank>0,CoopFloorsTest.step(game,ARMOR_ABILITY,destination,armor));
                if (rank==0) continue;
                assertNull(game.hero(1-seat).buff(WarpBeacon.WarpBeaconTracker.class));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat);
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,hero.belongings.armor()));
                assertEquals(destination,hero.pos);
                assertNotNull(hero.buff(WarpBeacon.WarpBeaconTracker.class));
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.CLEAR,hero.belongings.armor()));
                assertNull(hero.buff(WarpBeacon.WarpBeaconTracker.class));
            }
        }
    }

    @Test public void spectralAttacksUseTheOwnersConePenetrationAndBow() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=4;rank++) {
            for (Talent talent : new Talent[]{Talent.FAN_OF_BLADES,Talent.PROJECTING_BLADES,Talent.SPIRIT_BLADES}) {
                try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                    game.join(22,HeroClass.HUNTRESS);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    ClassArmor armor=invest(game,seat,0,talent,rank);
                    int width=Dungeon.level.width();
                    hero.belongings.weapon().upgrade(8);
                    rat.pos=hero.pos+4;
                    Rat second=new Rat();
                    second.HP=second.HT=200;
                    second.defenseSkill=rat.defenseSkill=0;
                    second.pos=rat.pos+width;
                    GameScene.add(second,2000);
                    if (talent==Talent.PROJECTING_BLADES) {
                        // A transparent solid grate blocks an ordinary spectral blade.
                        Level.set(hero.pos+1,Terrain.CRYSTAL_DOOR);
                    }
                    if (talent==Talent.SPIRIT_BLADES) {
                        hero.belongings.getItem(SpiritBow.class).enchant(new Blocking());
                    }
                    game.observe(seat);
                    assertTrue(game.knowledge(hero).visible(rat.pos));
                    boolean usable=talent!=Talent.PROJECTING_BLADES || rank>0;
                    assertEquals(usable,CoopFloorsTest.step(game,ARMOR_ABILITY,rat.pos,armor));
                    assertEquals(usable,rat.HP<rat.HT);
                    if(talent==Talent.FAN_OF_BLADES) assertEquals(rank>0,second.HP<second.HT);
                    if(talent==Talent.SPIRIT_BLADES && rank==4) {
                        for(int retry=0;hero.buff(Blocking.BlockBuff.class)==null && retry<32;retry++) {
                            SewerBeeTest.offer(game,seat);
                            armor.charge=100;
                            rat.HP=rat.HT;
                            assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,rat.pos,armor));
                        }
                        assertNotNull(hero.buff(Blocking.BlockBuff.class));
                        assertNull(game.hero(1-seat).buff(Blocking.BlockBuff.class));
                    }
                }
            }
        }
    }

    @Test public void wildMomentumExtendsOnlyTheShootersNaturesPowerOnAKill() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=4;rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                ClassArmor armor=invest(game,seat,1,Talent.WILD_MOMENTUM,rank);
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,hero.pos,armor));
                SewerBeeTest.offer(game,seat);
                rat.defenseSkill=0;
                rat.HP=1;
                assertTrue(CoopFloorsTest.step(game,SHOOT,rat.pos,hero.belongings.getItem(SpiritBow.class)));
                assertFalse(rat.isAlive());
                SewerBeeTest.offer(game,seat);
                NaturesPower.naturesPowerTracker tracker=hero.buff(NaturesPower.naturesPowerTracker.class);
                assertEquals(rank==0 ? 2:1,tracker.extensionsLeft);
                assertTrue(tracker.cooldown()>=5+rank);
                assertNull(game.hero(1-seat).buff(NaturesPower.naturesPowerTracker.class));
            }
        }
    }
    @Test public void telefragRejectsThePartnerAndDamagesOnlyTheCasterAndOccupiedFoe() {
        for (int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++) {
            try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat enemy=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat), ally=game.hero(1-seat);
                ClassArmor armor=invest(game,seat,2,Talent.TELEFRAG,rank);
                int mark=hero.pos, allyStart=ally.pos;
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,mark,armor));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,MOVE,mark-1,null));
                SewerBeeTest.offer(game,seat);
                ally.pos=mark;
                int allyHP=ally.HP;
                assertFalse(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                assertEquals(allyHP,ally.HP);
                ally.pos=allyStart;
                enemy.pos=mark;
                hero.HP=hero.HT=50;
                game.observe(seat);
                assertEquals(rank>0,CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                if (rank==0) continue;
                assertEquals(50-5*rank,hero.HP);
                assertEquals(mark,hero.pos);
                assertTrue(enemy.HT-enemy.HP>=10*rank && enemy.HT-enemy.HP<=15*rank);
                assertTrue(Dungeon.level.adjacent(mark,enemy.pos));
                assertEquals(allyHP,ally.HP);
            }
        }
    }
    @Test public void directedPowerBoostsThePrimaryWeaponsActualProcForEachEnemyInTheCone() {
        for (int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++) {
            try(CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Rat enemy=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                enemy.pos=hero.pos+1;
                for(int distance=2;distance<=4;distance++) {
                    Rat extra=new Rat();
                    extra.pos=hero.pos+distance;
                    GameScene.add(extra,2000);
                }
                MeleeWeapon rapier=(MeleeWeapon)hero.belongings.weapon();
                rapier.level(20);
                rapier.enchant(new Blocking());
                ClassArmor armor=invest(game,seat,1,Talent.DIRECTED_POWER,rank);
                game.observe(seat);
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,enemy.pos,armor));
                Blocking.BlockBuff block=hero.buff(Blocking.BlockBuff.class);
                // Four hostile characters. At rank two and above the original proc exceeds one.
                if(rank>=2) {
                    assertNotNull(block);
                    float chance=(24f/60f)*(1+.3f*4*rank);
                    assertEquals(Math.round(22*chance),block.shielding(),1);
                }
                assertNull(game.hero(1-seat).buff(Blocking.BlockBuff.class));
            }
        }
    }
    @Test public void naturesWrathRunsTheNativePlantAfterTheActualArrow() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=4;rank++) {
            boolean witnessed=false;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Rat enemy=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                enemy.HP=enemy.HT=4000;
                enemy.defenseSkill=0;
                ClassArmor armor=invest(game,seat,1,Talent.NATURES_WRATH,rank);
                SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
                for(int shot=0;shot<96 && !witnessed;shot++) {
                    SewerBeeTest.offer(game,seat);
                    if (hero.buff(NaturesPower.naturesPowerTracker.class)==null) {
                        armor.charge=100;
                        assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,hero.pos,armor));
                        SewerBeeTest.offer(game,seat);
                    }
                    assertTrue(CoopFloorsTest.step(game,SHOOT,enemy.pos,bow));
                    SewerBeeTest.offer(game,seat);
                    witnessed=enemy.buff(com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap.HazardAssistTracker.class)!=null;
                    if(rank==0 && shot==8) break;
                }
                assertEquals(rank>0,witnessed);
                assertNull(game.hero(1-seat).buff(com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap.HazardAssistTracker.class));
            }
        }
    }
    @Test public void thePoweredAlliesCarryOnlyTheirOwnClericsSpellAndTarget() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++) {
            for(Talent talent:new Talent[]{Talent.BEAMING_RAY,Talent.LIFE_LINK}) {
                Bundle save;
                int allyId, foeId;
                try(CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                    game.join(22,HeroClass.CLERIC);
                    Rat enemy=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    ClassArmor armor=invest(game,seat,2,talent,rank);
                    TierGrowthTest.powerTome(hero);
                    HolyTome tome=hero.belongings.getItem(HolyTome.class);
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,hero.pos+1,armor));
                    SewerBeeTest.offer(game,seat);
                    Char ally=PowerOfMany.getPoweredAlly(hero);
                    assertNotNull(ally);
                    allyId=ally.id(); foeId=enemy.id();
                    int energy=tome.charges();
                    int target=talent==Talent.LIFE_LINK ? hero.pos : enemy.pos;
                    assertEquals(rank>0,CoopFloorsTest.step(game,CoopSession.Action.valueOf(talent.name()),target,tome));
                    if(rank==0) continue;
                    assertTrue(tome.charges()<energy);
                    assertNull(game.hero(1-seat).buff(LifeLink.class));
                    save=game.checkpoint();
                }
                try(CoopSession game=CoopSession.restore(save)) {
                    Hero hero=game.hero(seat);
                    Char ally=PowerOfMany.getPoweredAlly(hero);
                    assertEquals(allyId,ally.id());
                    assertEquals(rank,PowerOfMany.rankForAlly(ally,talent));
                    assertNull(PowerOfMany.getPoweredAlly(game.hero(1-seat)));
                    if(talent==Talent.BEAMING_RAY) {
                        assertEquals(foeId,ally.buff(BeamingRay.BeamingRayBoost.class).object);
                        Char foe=(Char)com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findById(foeId);
                        assertTrue(Dungeon.level.adjacent(ally.pos,foe.pos));
                    } else {
                        assertEquals(allyId,hero.buff(LifeLink.class).object);
                        assertEquals(hero.id(),ally.buff(LifeLink.class).object);
                        assertTrue(ally.buff(LifeLink.class).cooldown()>=Math.round(6.67f+3.33f*rank)-2);
                        int hp=hero.HP;
                        ally.damage(8,this);
                        assertTrue(hero.HP<hp);
                    }
                }
            }
        }
    }
}
