package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.*;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class SharedGrowthTest extends HeadlessDungeon {
    static int selection(Hero hero, Item target) {
        int index=0;
        for (Item item : hero.belongings) {
            ++index;
            if (item==target) return index;
        }
        throw new AssertionError("Missing selection");
    }

    static StagedInput stage(CoopSession game, int seat, String kind, Rat target) {
        Talent spell;
        switch (kind) {
            case "paladinheal": spell = Talent.LAY_ON_HANDS; break;
            case "paladinaura": spell = Talent.AURA_OF_PROTECTION; break;
            case "prayer": spell = Talent.MNEMONIC_PRAYER; break;
            case "hallow": spell = Talent.HALLOWED_GROUND; break;
            case "wall": spell = Talent.WALL_OF_LIGHT; break;
            default: spell = null;
        }
        if (spell != null) {
            boolean priest = spell == Talent.MNEMONIC_PRAYER || spell == Talent.HALLOWED_GROUND;
            Hero caster = TierGrowthTest.prepare(game, seat, priest ? HeroSubClass.PRIEST : HeroSubClass.PALADIN, spell, 1);
            TierGrowthTest.powerTome(caster);
            int cell = caster.pos;
            if (spell == Talent.WALL_OF_LIGHT) cell++;
            if (spell == Talent.MNEMONIC_PRAYER) {
                Buff.prolong(target, Blindness.class, 8f);
                cell = target.pos;
            }
            return new StagedInput(game,spell.name(),cell);
        }
        Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.SNIPER,Talent.SHARED_ENCHANTMENT,kind.equals("bowthrow")?3:0);
        SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
        if (kind.equals("bowrank")) return new StagedInput(game,"INVEST_TALENT",Talent.SHARED_ENCHANTMENT.ordinal());
        if (kind.equals("bowstone")) {
            StoneOfEnchantment stone=new StoneOfEnchantment();
            Dungeon.level.drop(stone,hero.pos);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,hero.pos,stone));
            SewerBeeTest.offer(game,seat);
            return new StagedInput(game,"ENCHANT_BOW",selection(hero,bow));
        }
        bow.enchant(new Crystal());
        ThrowingStone shot=new ThrowingStone();
        shot.identify();
        assertTrue(shot.collect(hero.belongings.backpack));
        game.ownership.claim(shot,game.player(seat));
        Buff.prolong(target,Paralysis.class,10f);
        return new StagedInput(game,"THROW",target.pos);
    }

    static boolean published(CoopSession game,int seat,String kind) {
        Hero hero=game.hero(seat);
        switch (kind) {
            case "paladinheal": return hero.buff(Barrier.class) != null && game.hero(1-seat).buff(Barrier.class) == null;
            case "paladinaura": return AuraOfProtection.source(hero) == hero && AuraOfProtection.source(game.hero(1-seat)) == null;
            case "prayer": return Dungeon.level.mobs.stream().anyMatch(m -> m.buff(Blindness.class) != null && m.buff(Blindness.class).mnemonicExtended);
            case "hallow": return Blob.volumeAt(hero.pos,HallowedGround.terrainType(hero)) > 0;
            case "wall": return Dungeon.level.solid[hero.pos+1] && WallOfLight.INSTANCE.chargeUse(hero) == 0f;
        }
        if (kind.equals("bowthrow")) return hero.buff(Crystal.CrystalRepair.class)!=null;
        if (kind.equals("bowrank")) return hero.pointsInTalent(Talent.SHARED_ENCHANTMENT)==1;
        return hero.belongings.getItem(SpiritBow.class).enchantment!=null && hero.belongings.getItem(StoneOfEnchantment.class)==null;
    }

    @Test public void trinityRemembersFrostAndCastsTheNativeColdEffectWithoutTheWand() {
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                hero.lvl=30;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,1,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.MIND_FORM.ordinal(),null));
                WandOfFrost frost=new WandOfFrost();
                frost.identify(false);
                Dungeon.level.drop(frost,hero.pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,hero.pos,frost));
                SewerBeeTest.offer(game,seat);
                StagedInput select=SealArtsTest.historyInput(game,seat,frost,TrinityDiscoveries.Choice.FROST);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MIND_FORM,select.cell,hero.belongings.getItem(HolyTome.class)));
                SewerBeeTest.offer(game,seat);
                ClassArmor armor=(ClassArmor)hero.belongings.armor();
                float before=armor.charge;
                int hp=rat.HP;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.TRINITY_MIND,rat.pos,armor));
                assertTrue(armor.charge<before);
                assertTrue(rat.HP<hp);
                assertNotNull(rat.buff(Chill.class));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                assertTrue(game.discoveries(seat).knows(TrinityDiscoveries.Choice.FROST));
                assertFalse(game.discoveries(1-seat).knows(TrinityDiscoveries.Choice.FROST));
                assertNull(game.hero(seat).belongings.getItem(WandOfFrost.class));
            }
        }
    }

    @Test public void hawkSpeedEvasionsAndDebuffsFollowItsSummonersArmorTalent() {
        for (int seat=0;seat<2;seat++) for (Talent talent : new Talent[]{Talent.SWIFT_SPIRIT,Talent.GO_FOR_THE_EYES}) {
            for (int rank=0;rank<=4;rank++) {
                Bundle saved;
                try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                    game.join(22,HeroClass.HUNTRESS);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    SewerBeeTest.offer(game,seat);
                    Hero hero=game.hero(seat);
                    hero.lvl=30;
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,2,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
                    SewerBeeTest.offer(game,seat);
                    for (int i=0;i<rank;i++)
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,talent.ordinal(),null));
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,hero.pos,hero.belongings.armor()));
                    SpiritHawk.HawkAlly hawk=SpiritHawk.getHawk(hero);
                    assertNull(SpiritHawk.getHawk(game.hero(1-seat)));
                    if (talent==Talent.SWIFT_SPIRIT) {
                        assertEquals(2f+rank/2f,hawk.speed(),.001f);
                        for (int i=0;i<2*rank;i++) assertEquals(Char.INFINITE_EVASION,hawk.defenseSkill(rat));
                        assertTrue(hawk.defenseSkill(rat)<Char.INFINITE_EVASION);
                    } else {
                        hawk.attackProc(rat,3);
                        assertEquals(rank>0,rat.buff(Blindness.class)!=null);
                        assertEquals(rank>=3,rat.buff(Cripple.class)!=null);
                    }
                    saved=game.checkpoint();
                }
                try (CoopSession game=CoopSession.restore(saved)) {
                    assertSame(game.hero(seat),SpiritHawk.getHawk(game.hero(seat)).commander());
                    assertEquals(rank,game.hero(seat).pointsInTalent(talent));
                }
            }
        }
    }

    @Test public void twoCastersKeepIndependentWallsAndHallowedGroundAfterRestore() {
        for (boolean wall : new boolean[]{true,false}) for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            Talent talent=wall ? Talent.WALL_OF_LIGHT : Talent.HALLOWED_GROUND;
            CoopSession.Action cast=CoopSession.Action.valueOf(talent.name());
            Bundle saved;
            int center;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                CoopMageTest.encounter(game,seat);
                Hero hero=TierGrowthTest.prepare(game,seat,wall ? HeroSubClass.PALADIN : HeroSubClass.PRIEST,talent,rank);
                Hero friend=game.hero(1-seat);
                HolyTome tome=hero.belongings.getItem(HolyTome.class);
                TierGrowthTest.powerTome(hero);
                int charges=tome.charges();
                center=wall ? hero.pos+1 : hero.pos;
                assertEquals(rank>0,CoopFloorsTest.step(game,cast,center,tome));
                if (rank==0) continue;
                assertEquals(charges-(wall?3:2),tome.charges());
                if (wall) {
                    assertTrue(Dungeon.level.solid[center]);
                    assertTrue(ProjectionChecks.seatImage(game,seat).source.rows.containsKey("lightwall"+center));
                    assertEquals(0f,WallOfLight.INSTANCE.chargeUse(hero),0f);
                    assertEquals(3f,WallOfLight.INSTANCE.chargeUse(friend),0f);
                } else {
                    assertTrue(Blob.volumeAt(center,HallowedGround.terrainType(hero))>0);
                    assertEquals(0,Blob.volumeAt(center,HallowedGround.terrainType(friend)));
                    assertNotNull(hero.buff(Barrier.class));
                    assertNull(friend.buff(Barrier.class));
                    assertEquals(com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.GRASS,Dungeon.level.map[center]);
                }
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero owner=game.hero(seat);
                if (wall) {
                    assertTrue(Dungeon.level.solid[center]);
                    SewerBeeTest.offer(game,seat);
                    HolyTome tome=owner.belongings.getItem(HolyTome.class);
                    int charges=tome.charges();
                    assertTrue(CoopFloorsTest.step(game,cast,owner.pos,tome));
                    assertEquals(charges,tome.charges());
                    assertFalse(Dungeon.level.solid[center]);
                } else {
                    assertTrue(Blob.volumeAt(center,HallowedGround.terrainType(owner))>0);
                    assertEquals(0,Blob.volumeAt(center,HallowedGround.terrainType(game.hero(1-seat))));
                }
            }
        }
    }

    @Test public void prayerExtendsABuffOnceAndSparesTheOtherSeat() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            Bundle saved;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Rat foe=CoopMageTest.encounter(game,seat);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.PRIEST,Talent.MNEMONIC_PRAYER,rank);
                HolyTome tome=hero.belongings.getItem(HolyTome.class);
                TierGrowthTest.powerTome(hero);
                int charges=tome.charges();
                Buff.prolong(foe,Blindness.class,8);
                Blindness effect=foe.buff(Blindness.class);
                float before=effect.cooldown();
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.MNEMONIC_PRAYER,game.hero(1-seat).pos,tome));
                assertEquals(rank>0,CoopFloorsTest.step(game,CoopSession.Action.MNEMONIC_PRAYER,foe.pos,tome));
                if (rank==0) continue;
                assertEquals(charges-1,tome.charges());
                assertTrue(effect.mnemonicExtended);
                assertEquals(before+rank+2,effect.cooldown(),.001f);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MNEMONIC_PRAYER,foe.pos,tome));
                assertEquals(before+rank+2,effect.cooldown(),.001f);
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                assertTrue(Dungeon.level.mobs.stream().anyMatch(m->m.buff(Blindness.class)!=null && m.buff(Blindness.class).mnemonicExtended));
            }
        }
    }

    @Test public void bountyHunterUsesThePreparedAssassinsNativeLootChanceAndDrop() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.ROGUE)) {
                game.join(22,HeroClass.ROGUE);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.ASSASSIN,Talent.BOUNTY_HUNTER,rank);
                hero.lvl=5;
                int dropped=0;
                for (int n=0;n<80 && dropped==0;n++) {
                    Rat staged=CoopMageTest.encounter(game,seat);
                    Actor.remove(staged);
                    Dungeon.level.mobs.remove(staged);
                    Crab crab=new Crab();
                    crab.pos=hero.pos+1;
                    crab.HP=1;
                    GameScene.add(crab,2000);
                    Buff.prolong(crab,Paralysis.class,20f);
                    Buff.affect(hero,Preparation.class);
                    Dungeon.level.updateFieldOfView(hero,hero.fieldOfView);
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,crab.pos,null));
                    if (crab.isAlive()) continue;
                    assertTrue(crab.wasDefeatedBy(hero));
                    Preparation prep=Buff.affect(hero,Preparation.class);
                    Buff.affect(hero,Talent.BountyHunterTracker.class,0);
                    float nativeBonus=.02f*(1 << (prep.attackLevel()-1))*rank;
                    assertEquals(.167f*(1+nativeBonus),crab.lootChance(),.00001f);
                    assertNull(game.hero(1-seat).buff(Talent.BountyHunterTracker.class));
                    com.shatteredpixel.shatteredpixeldungeon.items.Heap pile=Dungeon.level.heaps.get(crab.pos);
                    if (pile!=null && pile.peek() instanceof com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat) dropped++;
                }
                assertEquals(1,dropped);
            }
        }
    }

    @Test public void championsSecondAbilityUsesTheFirstPhysicalWeaponAfterRestore() {
        float[] naturalRecharge=new float[2];
        for (Talent talent : new Talent[]{Talent.VARIED_CHARGE,Talent.COMBINED_LETHALITY}) {
            for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
                Bundle saved;
                int targetId;
                try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                    game.join(22,HeroClass.DUELIST);
                    Rat enemy=CoopMageTest.encounter(game,seat);
                    Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.CHAMPION,talent,rank);
                    hero.STR=16;
                    targetId=enemy.id();
                    Quarterstaff staff=new Quarterstaff();
                    staff.identify();
                    assertTrue(staff.collect(hero.belongings.backpack));
                    game.ownership.claim(staff,game.player(seat));
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP_SECOND,0,staff));
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SWAP_WEAPONS,hero.pos,null));
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WEAPON_ABILITY,hero.pos,staff));
                    saved=game.checkpoint();
                }
                try (CoopSession game=CoopSession.restore(saved)) {
                    Hero hero=game.hero(seat);
                    Rat enemy=(Rat)Actor.findById(targetId);
                    enemy.pos=hero.pos+2;
                    enemy.HP=20;
                    Dungeon.level.updateFieldOfView(hero,hero.fieldOfView);
                    game.observe(seat);
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SWAP_WEAPONS,hero.pos,null));
                    MeleeWeapon.Charger pool=hero.buff(MeleeWeapon.Charger.class);
                    pool.charges=4;
                    pool.partialCharge=0;
                    if (talent==Talent.COMBINED_LETHALITY && rank>0) {
                        Talent.CombinedLethalityAbilityTracker tracker=hero.buff(Talent.CombinedLethalityAbilityTracker.class);
                        assertNotNull(tracker);
                        assertTrue(tracker.tracks((MeleeWeapon)hero.belongings.secondWep));
                        assertFalse(tracker.tracks((MeleeWeapon)hero.belongings.weapon));
                    }
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WEAPON_ABILITY,enemy.pos,hero.belongings.weapon));
                    if (talent==Talent.VARIED_CHARGE) {
                        if (rank==0) naturalRecharge[seat]=pool.partialCharge;
                        assertEquals(naturalRecharge[seat]+rank/6f,pool.partialCharge,.001f);
                        assertNull(hero.buff(Talent.VariedChargeTracker.class));
                    } else assertEquals(rank==0,enemy.isAlive());
                }
            }
        }
    }

    @Test public void monksFocusRefundDependsOnItsRealEnergyCostAndPriorWeaponAbility() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.MONK,Talent.COMBINED_ENERGY,rank);
                Rat rat=CoopMageTest.encounter(game,seat);
                rat.pos=hero.pos+2;
                game.observe(seat);
                Buff.affect(hero,MonkEnergy.class).energy=8;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WEAPON_ABILITY,rat.pos,hero.belongings.weapon));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MONK_FOCUS,hero.pos,null));
                assertEquals(rank==3 ? 7f : 6f,hero.buff(MonkEnergy.class).energy,0f);
                assertNull(game.hero(1-seat).buff(Talent.CombinedEnergyAbilityTracker.class));
            }
        }
    }

    @Test public void warlocksOwnTheirCorruptedWraithsAfterAMarkedKillAndRestart() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            Bundle saved=null;
            int summoned=0;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Hero caster=TierGrowthTest.prepare(game,seat,HeroSubClass.WARLOCK,Talent.NECROMANCERS_MINIONS,rank);
                for (int tries=0; tries<45; tries++) {
                    Rat rat=CoopMageTest.encounter(game,seat);
                    rat.pos=caster.pos+1;
                    rat.HP=1;
                    Buff.prolong(rat,Paralysis.class,10);
                    SoulMark.applyFrom(caster,rat,10);
                    Dungeon.level.updateFieldOfView(caster,caster.fieldOfView);
                    SewerBeeTest.offer(game,seat);
                    assertTrue("seat="+seat+" rank="+rank+" attempt="+tries+" stop="+game.stopReason()+" actor="+(Actor.findChar(rat.pos)==rat)+" fov="+caster.fieldOfView[rat.pos]+" active="+game.activeSeat()+" hp="+rat.HP+" distance="+Dungeon.level.distance(caster.pos,rat.pos),CoopFloorsTest.step(game,CoopSession.Action.ATTACK,rat.pos,null));
                    for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob : Dungeon.level.mobs) {
                        if (!(mob instanceof Wraith)) continue;
                        assertSame(caster, CoopSession.summonerOf(mob));
                        assertEquals(Char.Alignment.ALLY,mob.alignment);
                        assertNotNull(mob.buff(Corruption.class));
                        summoned=mob.id();
                    }
                    if (summoned!=0) { saved=game.checkpoint(); break; }
                    if (rank==0 && tries>=8) break;
                }
                assertEquals(rank>0,summoned!=0);
            }
            if (saved!=null) try (CoopSession game=CoopSession.restore(saved)) {
                Wraith w=(Wraith)Actor.findById(summoned);
                assertSame(game.hero(seat),w.summoner());
                assertNotNull(w.buff(Corruption.class));
            }
        }
    }

    @Test public void warpSwapsOnlyAnOwnedVisibleAllyWithinTheMagesRank() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.WARLOCK,Talent.ALLY_WARP,rank);
                Rat rat=CoopMageTest.encounter(game,seat);
                int start=hero.pos;
                int destination=start-2;
                Wraith ally=Wraith.spawnAt(destination,Wraith.class);
                Buff.affect(ally,Corruption.class);
                ally.bindSummoner(game.hero(1-seat));
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ALLY_WARP,destination,null));
                ally.bindSummoner(hero);
                game.observe(seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ALLY_WARP,game.hero(1-seat).pos,null));
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ALLY_WARP,rat.pos,null));
                assertEquals(rank>0,ProjectionChecks.seatImage(game,seat).source.rows.containsKey("warpally"+destination));
                assertEquals(rank>0,CoopFloorsTest.step(game,CoopSession.Action.ALLY_WARP,destination,null));
                assertEquals(rank>0 ? destination : start,hero.pos);
                assertEquals(rank>0 ? start : destination,ally.pos);
            }
        }
    }

    @Test public void stealthRaisesOnlyTheWearersIdentifiedHasteRing() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=3; rank++) {
            Bundle saved;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.ROGUE)) {
                game.join(22,HeroClass.ROGUE);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.ASSASSIN,Talent.ENHANCED_RINGS,rank);
                RingOfHaste ring=new RingOfHaste();
                Dungeon.level.drop(ring,hero.pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,hero.pos,ring));
                SewerBeeTest.offer(game,seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,ring));
                ring.identify(false);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,ring));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLOAK,0,hero.belongings.getItem(CloakOfShadows.class)));
                assertEquals(rank>0 ? 1 : 0,ring.buffedLvl());
                assertNull(game.hero(1-seat).buff(EnhancedRings.class));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                assertEquals(rank>0 ? 1 : 0,game.hero(seat).belongings.ring().buffedLvl());
            }
        }
    }

    @Test public void paladinSelfBarrierAndAuraRespectOwnershipAndSpellRanks() {
        for (Talent talent : new Talent[]{Talent.LAY_ON_HANDS, Talent.AURA_OF_PROTECTION}) {
            boolean hands = talent == Talent.LAY_ON_HANDS;
            CoopSession.Action cast = CoopSession.Action.valueOf(talent.name());
            for (int seat = 0; seat < 2; seat++) for (int rank = 0; rank <= 3; rank++) {
                Bundle saved;
                try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
                    game.join(22, HeroClass.CLERIC);
                    Hero hero = TierGrowthTest.prepare(game, seat, HeroSubClass.PALADIN, talent, rank);
                    Hero partner = game.hero(1 - seat);
                    TierGrowthTest.powerTome(hero);
                    HolyTome tome = hero.belongings.getItem(HolyTome.class);
                    int initial = tome.charges();
                    assertFalse(CoopFloorsTest.step(game, cast, partner.pos, tome));
                    assertEquals(initial, tome.charges());
                    assertEquals(rank > 0, CoopFloorsTest.step(game, cast, hero.pos, tome));
                    if (rank == 0) continue;
                    if (hands) {
                        assertEquals(10 + 5 * rank, hero.buff(Barrier.class).shielding());
                        assertNull(partner.buff(Barrier.class));
                    } else {
                        assertSame(hero, AuraOfProtection.source(hero));
                        assertNull(AuraOfProtection.source(partner));
                        assertEquals(1.25f + .25f * rank, Armor.Glyph.genericProcChanceMultiplier(hero), .001f);
                        assertEquals(1f, Armor.Glyph.genericProcChanceMultiplier(partner), .001f);
                    }
                    assertEquals(initial - (hands ? 1 : 2), tome.charges());
                    saved = game.checkpoint();
                }
                try (CoopSession restored = CoopSession.restore(saved)) {
                    assertEquals(rank, restored.hero(seat).pointsInTalent(talent));
                    if (hands) assertEquals(10 + 5 * rank, restored.hero(seat).buff(Barrier.class).shielding());
                    else assertSame(restored.hero(seat), AuraOfProtection.source(restored.hero(seat)));
                }
            }
        }
    }

    @Test public void aBattlemagesStaffChargesTheHornHePickedUpAndEquipped() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            Bundle saved;
            float charge;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.MYSTICAL_CHARGE,rank);
                Rat rat=CoopMageTest.encounter(game,seat);
                rat.pos=hero.pos+1;
                Buff.prolong(rat,Paralysis.class,40f);
                HornOfPlenty horn=new HornOfPlenty();
                Dungeon.level.drop(horn,hero.pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,hero.pos,horn));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,horn));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,rat.pos,null));
                Bundle state=new Bundle();
                horn.storeInBundle(state);
                charge=state.getFloat("partialcharge");
                assertEquals(rank/8f,charge,.0001f);
                assertNull(game.hero(1-seat).belongings.getItem(HornOfPlenty.class));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                HornOfPlenty horn=game.hero(seat).belongings.getItem(HornOfPlenty.class);
                Bundle state=new Bundle();
                horn.storeInBundle(state);
                assertEquals(charge,state.getFloat("partialcharge"),0f);
                assertEquals(game.player(seat),game.ownership.owner(horn));
            }
        }
    }

    @Test public void championUsesOnlyTheLevelOfHerEquippedSecondWeapon() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            Bundle saved;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.CHAMPION,Talent.TWIN_UPGRADES,rank);
                Quarterstaff second=new Quarterstaff();
                second.level(3);
                second.identify();
                assertTrue(second.collect(hero.belongings.backpack));
                game.ownership.claim(second,game.player(seat));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP_SECOND,0,second));
                assertSame(second,hero.belongings.secondWep);
                assertEquals(rank>=2 ? 3 : 0,hero.belongings.weapon.buffedLvl());
                assertEquals(0,game.hero(1-seat).belongings.weapon.buffedLvl());
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat);
                assertEquals(rank>=2 ? 3 : 0,hero.belongings.weapon.buffedLvl());
                assertEquals(3,hero.belongings.secondWep.trueLevel());
                assertNotEquals(game.ownership.identity(hero.belongings.weapon),game.ownership.identity(hero.belongings.secondWep));
            }
        }
    }

    @Test public void nativeLungeOpensPreciseAssaultOnlyForItsUser() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.CHAMPION,Talent.PRECISE_ASSAULT,rank);
                Rat rat=CoopMageTest.encounter(game,seat);
                rat.pos=hero.pos+2;
                game.observe(seat);
                int before=hero.attackSkill(rat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WEAPON_ABILITY,rat.pos,hero.belongings.weapon));
                assertEquals(rank>0,hero.buff(Talent.PreciseAssaultTracker.class)!=null);
                int after=hero.attackSkill(rat);
                if (rank==3) assertTrue(after>=Char.INFINITE_ACCURACY);
                else assertEquals(before*(rank==2?5:rank==1?2:1),after);
                assertNull(game.hero(1-seat).buff(Talent.PreciseAssaultTracker.class));
            }
        }
    }

    @Test public void thrownSpikesMarkTheirVictimForTheDuelistsNextMeleeAttack() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.CHAMPION,Talent.DEADLY_FOLLOWUP,rank);
                Rat rat=CoopMageTest.encounter(game,seat);
                Buff.prolong(rat,Paralysis.class,20f);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,hero.belongings.getItem(ThrowingSpike.class)));
                assertEquals(10+rank,Talent.onAttackProc(hero,rat,10));
                assertEquals(10,Talent.onAttackProc(game.hero(1-seat),rat,10));
                assertEquals(10,Talent.onAttackProc(hero,new Rat(),10));
                if(rank>0) assertEquals(rat.id(),hero.buff(Talent.DeadlyFollowupTracker.class).object);
            }
        }
    }

    @Test public void aMonksOriginalKillRewardDependsOnHisOwnLightEquipment() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            Bundle saved;
            float expected=new float[]{1f,2f,2.5f,3f}[rank];
            try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.MONK,Talent.UNENCUMBERED_SPIRIT,rank);
                Rat rat=CoopMageTest.encounter(game,seat);
                rat.HP=1;
                rat.pos=hero.pos+1;
                Buff.prolong(rat,Paralysis.class,20f);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WAIT,hero.pos,null));
                SewerBeeTest.offer(game,seat);
                for (int attempt=0; attempt<8 && rat.isAlive(); attempt++) {
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,rat.pos,null));
                }
                assertFalse(rat.isAlive());
                assertEquals(expected,hero.buff(MonkEnergy.class).energy,.0001f);
                assertNull(game.hero(1-seat).buff(MonkEnergy.class));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                assertEquals(expected,game.hero(seat).buff(MonkEnergy.class).energy,.0001f);
            }
        }
    }

    @Test public void aCollectedStoneEnchantsOnlyTheSelectedOwnersPhysicalBow() {
        for (int seat=0; seat<2; seat++) {
            Bundle saved;
            String effect;
            long bowID;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                StoneOfEnchantment stone=new StoneOfEnchantment();
                Dungeon.level.drop(stone,hero.pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,hero.pos,stone));
                SewerBeeTest.offer(game,seat);
                SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
                bowID=game.ownership.identity(bow);
                long turn=game.turn();
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ENCHANT_BOW,0,stone));
                assertEquals(turn,game.turn());
                assertNull(bow.enchantment);
                assertTrue(hero.belongings.contains(stone));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ENCHANT_BOW,selection(hero,bow),stone));
                assertFalse(hero.belongings.contains(stone));
                assertNull(game.hero(1-seat).belongings.getItem(SpiritBow.class).enchantment);
                effect=bow.enchantment.getClass().getName();
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                SpiritBow bow=game.hero(seat).belongings.getItem(SpiritBow.class);
                assertEquals(bowID,game.ownership.identity(bow));
                assertEquals(game.player(seat),game.ownership.owner(bow));
                assertEquals(effect,bow.enchantment.getClass().getName());
                assertNull(game.hero(seat).belongings.getItem(StoneOfEnchantment.class));
            }
        }
    }

    @Test public void nativeCrystalChecksEveryRankAgainstTheThrowersBow() {
        for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.SNIPER,Talent.SHARED_ENCHANTMENT,rank);
                SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
                bow.enchant(new Crystal());
                Rat rat=CoopMageTest.encounter(game,seat);
                rat.HP=rat.HT=500;
                ThrowingStone missile=new ThrowingStone();
                assertTrue(missile.collect(hero.belongings.backpack));
                game.ownership.claim(missile,game.player(seat));
                int triggered=0;
                for (int i=0;i<18;i++) {
                    ThrowingStone shot=new ThrowingStone();
                    assertTrue(shot.collect(hero.belongings.backpack));
                    game.ownership.claim(shot,game.player(seat));
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,
                            hero.belongings.getItem(ThrowingStone.class)));
                    Crystal.CrystalRepair repair=hero.buff(Crystal.CrystalRepair.class);
                    if (repair!=null) { triggered++; repair.detach(); }
                    assertNull(game.hero(1-seat).buff(Crystal.CrystalRepair.class));
                }
                if (rank==0) assertEquals(0,triggered);
                else assertTrue("seat "+seat+" rank "+rank,triggered>0);
            }
        }
    }

    @Test public void sharedBowEffectsUseTheShootersBowAndRespectMagicImmunity() {
        for (int seat=0;seat<2;seat++) for (int effect=0;effect<3;effect++) for (boolean immune:new boolean[]{false,true}) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.SNIPER,Talent.SHARED_ENCHANTMENT,3);
                SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
                game.hero(1-seat).belongings.getItem(SpiritBow.class).enchant(new Crystal());
                bow.enchant(effect==0 ? new Chilling() : effect==1 ? new Blazing() : new Blocking());
                Rat rat=CoopMageTest.encounter(game,seat);
                rat.HP=rat.HT=5000;
                Buff.prolong(rat,Paralysis.class,200f);
                if (immune) Buff.prolong(hero,MagicImmune.class,200f);
                boolean observed=false;
                ThrowingStone missiles=new ThrowingStone();
                missiles.quantity(45);
                assertTrue(missiles.collect(hero.belongings.backpack));
                game.ownership.claim(missiles,game.player(seat));
                for (int i=0;i<40 && !observed;i++) {
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,hero.belongings.getItem(ThrowingStone.class)));
                    observed=effect==0 ? rat.buff(Chill.class)!=null : effect==1 ? rat.buff(Burning.class)!=null : hero.buff(Blocking.BlockBuff.class)!=null;
                    assertNull(hero.buff(Crystal.CrystalRepair.class));
                    assertNull(game.hero(1-seat).buff(Blocking.BlockBuff.class));
                }
                assertEquals(!immune,observed);
            }
        }
    }

    @Test public void aPackedClassArtifactUsesOnlyItsOwnersTalentAndSurvivesRestore() {
        for (HeroClass role : new HeroClass[]{HeroClass.ROGUE,HeroClass.CLERIC})
            for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
                boolean rogue=role==HeroClass.ROGUE;
                Bundle save;
                try (CoopSession game=CoopSession.open(572,11,role)) {
                    game.join(22,role);
                    SewerBeeTest.offer(game,seat);
                    Hero hero=game.hero(seat);
                    Artifact art=hero.belongings.artifact;
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.UNEQUIP,0,art));
                    TierGrowthTest.prepare(game,seat,rogue?HeroSubClass.ASSASSIN:HeroSubClass.PRIEST,
                            rogue?Talent.LIGHT_CLOAK:Talent.LIGHT_READING,rank);
                    assertTrue(hero.belongings.backpack.contains(art));
                    assertEquals(rank>0,art.actions(hero).contains(rogue?"STEALTH":"CAST"));
                    save=game.checkpoint();
                }
                try (CoopSession game=CoopSession.restore(save)) {
                    SewerBeeTest.offer(game,seat);
                    Hero hero=game.hero(seat);
                    Artifact art=rogue?hero.belongings.getItem(CloakOfShadows.class):hero.belongings.getItem(HolyTome.class);
                    assertTrue(hero.belongings.backpack.contains(art));
                    assertEquals(rank>0,CoopFloorsTest.step(game,rogue?CoopSession.Action.CLOAK:CoopSession.Action.HOLY_WARD,rogue?0:hero.pos,art));
                    assertEquals(0,game.hero(1-seat).invisible);
                }
            }
    }
}
