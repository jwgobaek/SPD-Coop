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
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.junit.Test;
import static org.junit.Assert.*;

public class SharedGrowthTest extends HeadlessDungeon {
    public static HeroSubClass upperSubclass(String talent) {
        switch (talent) {
            case "HOLD_FAST": case "DEATHLESS_FURY": case "ENRAGED_CATALYST": case "STRONGMAN": case "ENDLESS_RAGE":
                return HeroSubClass.BERSERKER;
            case "MYSTICAL_CHARGE": case "DESPERATE_POWER": case "EMPOWERED_STRIKE": case "EXCESS_CHARGE": return HeroSubClass.BATTLEMAGE;
            case "NECROMANCERS_MINIONS": case "ALLY_WARP": case "SOUL_SIPHON": case "SOUL_EATER": return HeroSubClass.WARLOCK;
            case "SHARED_ENCHANTMENT": case "POINT_BLANK": case "FARSIGHT": case "SEER_SHOT": case "SHARED_UPGRADES": return HeroSubClass.SNIPER;
            case "DURABLE_TIPS": case "BARKSKIN": case "SHIELDING_DEW": return HeroSubClass.WARDEN;
            case "ENHANCED_RINGS": case "LIGHT_CLOAK": case "BOUNTY_HUNTER": case "ENHANCED_LETHALITY": case "ASSASSINS_REACH": return HeroSubClass.ASSASSIN;
            case "EVASIVE_ARMOR": case "PROJECTILE_MOMENTUM": case "SPEEDY_STEALTH": return HeroSubClass.FREERUNNER;
            case "UNENCUMBERED_SPIRIT": case "COMBINED_ENERGY": case "MONASTIC_VIGOR": return HeroSubClass.MONK;
            case "HALLOWED_GROUND": case "MNEMONIC_PRAYER": case "LIGHT_READING": case "CLEANSE": case "HOLY_LANCE": return HeroSubClass.PRIEST;
            case "WALL_OF_LIGHT": case "LAY_ON_HANDS": case "AURA_OF_PROTECTION": return HeroSubClass.PALADIN;
            case "COMBINED_LETHALITY": case "VARIED_CHARGE": case "TWIN_UPGRADES": case "PRECISE_ASSAULT": case "DEADLY_FOLLOWUP": return HeroSubClass.CHAMPION;
            default: throw new IllegalArgumentException("Unknown upper talent fixture " + talent);
        }
    }
    public static HeroClass upperClass(String kind) {
        HeroSubClass subclass = upperSubclass(kind);
        for (HeroClass role : HeroClass.values()) {
            if (java.util.Arrays.asList(role.subClasses()).contains(subclass)) return role;
        }
        throw new IllegalArgumentException(kind);
    }

    static int selection(Hero hero, Item target) {
        int index=0;
        for (Item item : hero.belongings) {
            ++index;
            if (item==target) return index;
        }
        throw new AssertionError("Missing selection");
    }

    static StagedInput stage(CoopSession game, int seat, String kind, Rat target) {
        if (kind.equals("warpwraith")) {
            Hero mage=TierGrowthTest.prepare(game,seat,HeroSubClass.WARLOCK,Talent.ALLY_WARP,2);
            Wraith ally=Wraith.spawnAt(mage.pos-2,Wraith.class);
            Buff.affect(ally,Corruption.class);
            ally.bindSummoner(mage);
            game.observe(seat);
            return new StagedInput(game,"ALLY_WARP",ally.pos);
        }
        if (kind.equals("hornhit")) {
            Hero mage=TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.MYSTICAL_CHARGE,2);
            target.pos=mage.pos+1;
            Buff.prolong(target,Paralysis.class,40f);
            HornOfPlenty horn=new HornOfPlenty();
            Dungeon.level.drop(horn,mage.pos);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,mage.pos,horn));
            SewerBeeTest.offer(game,seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,horn));
            SewerBeeTest.offer(game,seat);
            return new StagedInput(game,"ATTACK",target.pos);
        }
        if (kind.equals("cloakring")) {
            Hero rogue=TierGrowthTest.prepare(game,seat,HeroSubClass.ASSASSIN,Talent.ENHANCED_RINGS,2);
            RingOfHaste ring=new RingOfHaste();
            Dungeon.level.drop(ring,rogue.pos);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,rogue.pos,ring));
            SewerBeeTest.offer(game,seat);
            ring.identify(false);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,ring));
            SewerBeeTest.offer(game,seat);
            return new StagedInput(game,"CLOAK",0);
        }
        if (kind.equals("packedcloak") || kind.equals("bookward")) {
            Hero hero=game.hero(seat);
            boolean rogue=kind.equals("packedcloak");
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.UNEQUIP,0,hero.belongings.artifact));
            TierGrowthTest.prepare(game,seat,rogue?HeroSubClass.ASSASSIN:HeroSubClass.PRIEST,rogue?Talent.LIGHT_CLOAK:Talent.LIGHT_READING,2);
            return new StagedInput(game,rogue?"CLOAK":"HOLY_WARD",rogue?0:hero.pos);
        }
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
            case "warpwraith":
                int oldPosition=game.hero(1-seat).pos-Dungeon.level.width();
                return hero.pos==oldPosition-2 && Dungeon.level.mobs.stream().anyMatch(m ->
                        m instanceof Wraith && m.pos==oldPosition && CoopSession.summonerOf(m)==hero);
            case "hornhit":
                Bundle state=new Bundle();
                hero.belongings.getItem(HornOfPlenty.class).storeInBundle(state);
                return state.getFloat("partialcharge")>=.25f && game.hero(1-seat).belongings.getItem(HornOfPlenty.class)==null;
            case "cloakring": return hero.invisible>0 && hero.belongings.getItem(RingOfHaste.class).buffedLvl()==1;
            case "packedcloak": return hero.invisible>0 && hero.belongings.artifact==null;
            case "bookward": return hero.belongings.artifact==null && hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class)!=null;
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

    @Test public void trinityRemembersColdAndFireAndCastsEachOriginalEffectWithoutTheWand() {
        for (int seat=0;seat<2;seat++) for (boolean fire : new boolean[]{false,true}) {
            TrinityDiscoveries.Choice choice = fire ? TrinityDiscoveries.Choice.FIREBLAST : TrinityDiscoveries.Choice.FROST;
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
                com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand = fire ? new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast() : new WandOfFrost();
                wand.identify(false);
                Dungeon.level.drop(wand,hero.pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,hero.pos,wand));
                SewerBeeTest.offer(game,seat);
                StagedInput select=SealArtsTest.historyInput(game,seat,wand,choice);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MIND_FORM,select.cell,hero.belongings.getItem(HolyTome.class)));
                SewerBeeTest.offer(game,seat);
                ClassArmor armor=(ClassArmor)hero.belongings.armor();
                float before=armor.charge;
                int hp=rat.HP;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.TRINITY_MIND,rat.pos,armor));
                assertTrue(armor.charge<before);
                assertTrue(rat.HP<hp);
                if (fire) assertNotNull(rat.buff(Burning.class));
                else assertNotNull(rat.buff(Chill.class));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                assertTrue(game.discoveries(seat).knows(choice));
                assertFalse(game.discoveries(1-seat).knows(choice));
                assertNull(game.hero(seat).belongings.getItem(WandOfFrost.class));
                assertNull(game.hero(seat).belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast.class));
            }
        }
    }

    @Test public void eagleEyeShowsOnlyTheSummonersContactBehindTheWall() throws Exception {
        for (int seat = 0; seat < 2; seat++) for (int rank = 0; rank <= 4; rank++) {
            Bundle saved;
            int contact, sight;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.HUNTRESS)) {
                game.join(22, HeroClass.HUNTRESS);
                Rat rat = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                Hero hero = game.hero(seat);
                hero.lvl = 30;
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.USE_CROWN, 2,
                        CrownAbilityTest.crown(game, seat, CoopSession.MaskTrial.OWNER)));
                SewerBeeTest.offer(game, seat);
                for (int i = 0; i < rank; i++)
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.EAGLE_EYE.ordinal(), null));
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, hero.pos, hero.belongings.armor()));
                SpiritHawk.HawkAlly hawk = SpiritHawk.getHawk(hero);
                assertSame(hero, hawk.commander());
                assertEquals(rank, hawk.eagleEye());
                sight = hawk.viewDistance;
                assertTrue(sight == Math.min(8, 6 + rank) || sight == 6 + rank);
                com.shatteredpixel.shatteredpixeldungeon.levels.Level level = Dungeon.level;
                hawk.pos = hero.pos + 1;
                game.hero(1-seat).pos = hero.pos - 1;
                for (int y = -4; y <= 4; y++)
                    com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(hawk.pos + 1 + y * level.width(),
                            com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL);
                contact = hawk.pos + 2;
                rat.pos = contact;
                assertFalse(game.observe(seat).visible(contact));
                assertEquals(rank >= 3, game.observe(seat).sensedMob(contact));
                assertFalse(game.observe(1-seat).sensedMob(contact));
                if (rank >= 3) ProjectionChecks.sensedContact(game, seat, rat, rat.pos + level.width());
                rat.pos = hawk.pos + 3;
                assertEquals(rank == 4, game.observe(seat).sensedMob(rat.pos));
                rat.pos = contact;
                saved = game.checkpoint();
            }
            try (CoopSession restored = CoopSession.restore(saved)) {
                SpiritHawk.HawkAlly hawk = SpiritHawk.getHawk(restored.hero(seat));
                assertEquals(sight, hawk.viewDistance);
                assertEquals(rank >= 3, restored.observe(seat).sensedMob(contact));
                assertFalse(restored.observe(1-seat).sensedMob(contact));
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

    @Test public void overlappingWallsKeepTheOtherCastersTrapCellBlockedUntilBothDismiss() {
        for (int first = 0; first < 2; first++) {
            Bundle saved;
            int trapCell;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
                game.join(22, HeroClass.CLERIC);
                CoopMageTest.encounter(game, first);
                trapCell = game.hero(first).pos + 1;
                com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(trapCell,
                        com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_TRAP);
                Dungeon.level.setTrap(new com.shatteredpixel.shatteredpixeldungeon.levels.traps.PoisonDartTrap(), trapCell);
                for (int seat : new int[]{first, 1-first}) {
                    Hero hero = game.hero(seat);
                    hero.lvl = 25;
                    hero.subClass = HeroSubClass.PALADIN;
                    Talent.initSubclassTalents(hero);
                    TierGrowthTest.powerTome(hero);
                    SewerBeeTest.offer(game, seat);
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.WALL_OF_LIGHT.ordinal(), null));
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.WALL_OF_LIGHT, hero.pos + 1,
                            hero.belongings.getItem(HolyTome.class)));
                    assertTrue(Blob.volumeAt(trapCell, WallOfLight.wallType(hero)) > 0);
                }
                saved = game.checkpoint();
            }
            try (CoopSession game = CoopSession.restore(saved)) {
                for (int seat : new int[]{first, 1-first}) {
                    Hero hero = game.hero(seat);
                    SewerBeeTest.offer(game, seat);
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.WALL_OF_LIGHT, hero.pos,
                            hero.belongings.getItem(HolyTome.class)));
                    assertEquals(0, Blob.volumeAt(trapCell, WallOfLight.wallType(hero)));
                    assertEquals(seat == first, Dungeon.level.solid[trapCell]);
                    assertTrue(Dungeon.level.traps.get(trapCell).active);
                    assertFalse(Dungeon.level.traps.get(trapCell).visible);
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
                    // The terrain Actor may have taken its first growth turn by this boundary.
                    int tile=Dungeon.level.map[center];
                    assertTrue(tile==Terrain.GRASS || tile==Terrain.HIGH_GRASS || tile==Terrain.FURROWED_GRASS);
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

    @Test public void kineticOverkillBelongsToTheBowOwnerAfterADirectShotOrSharedThrow() {
        for (int seat=0;seat<2;seat++) for (boolean thrown : new boolean[]{false,true}) for (int rank=0;rank<=3;rank++) {
            int killed=0,stored=0;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Hero owner=TierGrowthTest.prepare(game,seat,HeroSubClass.SNIPER,Talent.SHARED_ENCHANTMENT,rank);
                SpiritBow bow=owner.belongings.getItem(SpiritBow.class);
                bow.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic());
                for (int attempt=0;attempt<16;attempt++) {
                    Rat rat=CoopMageTest.encounter(game,seat);
                    rat.HP=1;
                    Buff.prolong(rat,Paralysis.class,10);
                    Buff.detach(owner,com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic.ConservedDamage.class);
                    SewerBeeTest.offer(game,seat);
                    if (thrown) {
                        ThrowingStone stone=new ThrowingStone();
                        assertTrue(stone.collect(owner.belongings.backpack));
                        game.ownership.claim(stone,game.player(seat));
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,owner.belongings.getItem(ThrowingStone.class)));
                    } else assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SHOOT,rat.pos,bow));
                    if (!rat.isAlive()) killed++;
                    if (owner.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic.ConservedDamage.class)!=null) stored++;
                    assertNull(game.hero(1-seat).buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic.ConservedDamage.class));
                }
                assertTrue(killed>0);
                if (thrown && rank==0) assertEquals(0,stored);
                else assertTrue(stored>0);
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

    @Test public void projectingUsesOnlyTheThrowersBowOrTheMissilesOwnEnchantment() {
        for (int seat=0;seat<2;seat++) for (boolean ownMissile:new boolean[]{false,true}) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.SNIPER,Talent.SHARED_ENCHANTMENT,ownMissile?0:3);
                Rat target=CoopMageTest.encounter(game,seat);
                Rat intervening=new Rat();
                intervening.pos=hero.pos+1;
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(intervening,2000);
                target.pos=hero.pos+3;
                Buff.prolong(target,Paralysis.class,20f);
                SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
                ThrowingStone missile=new ThrowingStone();
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting projecting =
                        new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting();
                if (ownMissile) missile.enchant(projecting);
                else bow.enchant(projecting);
                assertTrue(missile.collect(hero.belongings.backpack));
                game.ownership.claim(missile,game.player(seat));
                game.observe(seat);
                SewerBeeTest.offer(game,seat);
                int hp=target.HP, guardHP=intervening.HP;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,target.pos,missile));
                assertTrue(target.HP<hp);
                assertEquals(guardHP,intervening.HP);
                assertNull(game.hero(1-seat).belongings.getItem(SpiritBow.class).enchantment);
            }
        }
    }

    @Test public void monasticVigorResolvesTheFourOtherTechniquesWithTheOwnersEnergy() {
        CoopSession.Action[] techniques={CoopSession.Action.MONK_FLURRY,CoopSession.Action.MONK_DASH,
                CoopSession.Action.MONK_DRAGONKICK,CoopSession.Action.MONK_MEDITATE};
        for (CoopSession.Action technique: techniques) for (int seat=0;seat<2;seat++) for (int rank=0;rank<=3;rank++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.MONK,Talent.MONASTIC_VIGOR,rank);
                Rat target=CoopMageTest.encounter(game,seat);
                int start=hero.pos, width=Dungeon.level.width();
                for (int dy=-1;dy<=1;dy++) for (int dx=-1;dx<=8;dx++) {
                    int cell=start+dy*width+dx;
                    Dungeon.level.traps.remove(cell);
                    com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell,
                            com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);
                }
                target.pos=start+1;
                Rat side=new Rat();
                side.pos=start-width;
                int sideStart=side.pos;
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(side,2000);
                MonkEnergy energy=Buff.affect(hero,MonkEnergy.class);
                energy.energy=energy.energyCap();
                boolean empowered=energy.abilitiesEmpowered(hero);
                assertEquals(rank>0,empowered);
                int destination=target.pos;
                if (technique==CoopSession.Action.MONK_DASH) {
                    destination=start+5;
                    target.pos=start+width+1;
                } else if (technique==CoopSession.Action.MONK_MEDITATE) {
                    destination=start;
                    hero.HP=hero.HT/2;
                }
                int beforeHP=hero.HP, targetHP=target.HP;
                float beforeEnergy=energy.energy;
                game.observe(seat);
                boolean valid=technique!=CoopSession.Action.MONK_DASH || empowered;
                assertEquals(technique.name(),valid,CoopFloorsTest.step(game,technique,destination,null));
                if (!valid) { assertEquals(beforeEnergy,energy.energy,0f); continue; }
                assertTrue(energy.energy<beforeEnergy);
                assertNull(game.hero(1-seat).buff(MonkEnergy.class));
                switch (technique) {
                    case MONK_DASH: assertEquals(destination,hero.pos); break;
                    case MONK_MEDITATE:
                        assertEquals(empowered,hero.HP>beforeHP || hero.buff(Healing.class)!=null);
                        break;
                    case MONK_DRAGONKICK: assertEquals(empowered,side.pos!=sideStart); break;
                    case MONK_FLURRY:
                        assertTrue(target.HP<targetHP);
                        assertNotNull(hero.buff(MonkEnergy.MonkAbility.FlurryCooldownTracker.class));
                        break;
                    default: throw new AssertionError(technique);
                }
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
    @Test public void fireClearsBothConsecrationsWhileOtherTerrainAndTrapsRemainOwnedByTheFloor() {
        for (int first=0;first<2;first++) for(boolean sameSpell:new boolean[]{false,true}) {
            int center;
            Bundle save;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                CoopMageTest.encounter(game,first);
                center=game.hero(first).pos+1;
                com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(center,
                        com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_TRAP);
                Dungeon.level.setTrap(new com.shatteredpixel.shatteredpixeldungeon.levels.traps.PoisonDartTrap(),center);
                for(int seat:new int[]{first,1-first}) {
                    boolean wall=seat!=first && !sameSpell;
                    Talent talent=wall ? Talent.WALL_OF_LIGHT : Talent.HALLOWED_GROUND;
                    Hero hero=game.hero(seat);
                    hero.lvl=25;
                    hero.subClass=wall ? HeroSubClass.PALADIN : HeroSubClass.PRIEST;
                    Talent.initSubclassTalents(hero);
                    TierGrowthTest.powerTome(hero);
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,talent.ordinal(),null));
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.valueOf(talent.name()),wall ? hero.pos+1:center,
                            hero.belongings.getItem(HolyTome.class)));
                }
                assertTrue(Blob.volumeAt(center,HallowedGround.terrainType(game.hero(first)))>0);
                assertEquals(!sameSpell,Dungeon.level.solid[center]);
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                if (sameSpell) {
                    assertTrue(Blob.volumeAt(center,HallowedGround.terrainType(game.hero(1-first)))>0);
                    GameScene.add(Blob.seed(center,5,com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire.class));
                    for (int i=0;i<8 && (Blob.volumeAt(center,HallowedGround.terrainType(game.hero(first)))>0
                            || Blob.volumeAt(center,HallowedGround.terrainType(game.hero(1-first)))>0);i++)
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WAIT,0,null));
                    for(int seat=0;seat<2;seat++) assertEquals(0,Blob.volumeAt(center,HallowedGround.terrainType(game.hero(seat))));
                } else {
                    Hero paladin=game.hero(1-first);
                    SewerBeeTest.offer(game,1-first);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WALL_OF_LIGHT,paladin.pos,paladin.belongings.getItem(HolyTome.class)));
                    assertTrue(Blob.volumeAt(center,HallowedGround.terrainType(game.hero(first)))>0);
                    assertFalse(Dungeon.level.solid[center]);
                }
                assertTrue(Dungeon.level.traps.get(center).active);
                assertFalse(Dungeon.level.traps.get(center).visible);
            }
        }
    }
}
