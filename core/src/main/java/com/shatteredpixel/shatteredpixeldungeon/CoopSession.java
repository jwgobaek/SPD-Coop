package com.shatteredpixel.shatteredpixeldungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineIntervention;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Judgement;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MindForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BlessSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DemonSpawner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Brimstone;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Corrupting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Acidic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Scorpio;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Spinner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM200;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredBrute;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TormentedSpirit;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RotLasher;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RotHeart;
import com.shatteredpixel.shatteredpixeldungeon.plants.Rotberry;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Sunray;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnipersMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Albino;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.HermitCrab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Slime;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CausticSlime;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Thief;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollTrickster;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GreatCrab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RatKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FetidRat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalGuardian;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalSpire;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalWisp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollSapper;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SpectralNecromancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Potential;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Repulsion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollExile;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PhantomPiranha;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Venomous;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HandAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vorpal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Unstable;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dirk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.VaultBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.TargetedDamageWand;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/** A first-floor engine session. All commands and actor steps run on its creating thread. */
public final class CoopSession implements AutoCloseable {

    public enum Action { LIFE_LINK, BEAMING_RAY, DIVINE_INTERVENTION, JUDGEMENT, FLASH, READ_SCROLL, ENCHANT_TARGET, ENCHANT_CHOOSE, ENCHANT_CANCEL, TRINITY_MIND, TRINITY_SPIRIT, RECALL_INSCRIPTION, MIND_FORM, SPIRIT_FORM, BODY_FORM, HOLY_INTUITION, BLESS, DETACH_SEAL, AFFIX_SEAL, KEEP_ARMOR_GLYPH, TRANSFER_SEAL_GLYPH, READ_RECHARGING, EQUIP_SECOND, SWAP_WEAPONS, MONK_FLURRY, MONK_DASH, MONK_DRAGONKICK, MONK_MEDITATE, MONK_FOCUS, RADIANCE, SMITE, ASSASSINATE, FREERUN, DIVINE_SENSE, SUNRAY, SHIELD_OF_LIGHT, GUIDING_LIGHT, HOLY_WEAPON, HOLY_WARD, WEAPON_ABILITY, IMBUE, CLOAK, USE_CROWN, CLAIM_ARMOR, ARMOR_ABILITY, CLOBBER, SLAM, PARRY, CRUSH, FURY, BERSERK, SNIPER_SHOT, USE_MASK, CLAIM_SUBCLASS, INVEST_TALENT, VAULT_RECALL, VAULT_INTERACT, VAULT_REWARD, ACCEPT_IMP, SMITH_PICK, SMITH_CASH, SMITH_HARDEN, SMITH_UPGRADE, SMITH_REFORGE, SMITH_ORDER, SMITH_CLAIM, MINE, ACCEPT_BLACKSMITH, ACCEPT_WANDMAKER, WAND_FIRST, WAND_SECOND, BUY, SELL, SELL_ONE, BUYBACK, THROW, UPGRADE, IDENTIFY, WAIT, MOVE, ATTACK, PICK_UP, EAT, EQUIP, DROP, UNEQUIP, REVIVE, ZAP, SEARCH, CONSENT, CANCEL_TRAVEL, TRAVEL, ACCEPT_GHOST, GHOST_WEAPON, GHOST_ARMOR, SHATTER, OPEN, UNLOCK, GREET, DRINK, SHOOT, CLEANSE, HOLY_LANCE, ENCHANT_BOW, LAY_ON_HANDS, AURA_OF_PROTECTION, ALLY_WARP, MNEMONIC_PRAYER, HALLOWED_GROUND, WALL_OF_LIGHT, STASIS, READ_LULLABY, BREW_RUNESTONES;
        public boolean requiresItem() {
            return this==BREW_RUNESTONES || this==STASIS || this==LIFE_LINK || this==BEAMING_RAY || this==DIVINE_INTERVENTION || this==JUDGEMENT || this==FLASH || this==READ_SCROLL || this==ENCHANT_TARGET || this==HALLOWED_GROUND || this==WALL_OF_LIGHT || this==MNEMONIC_PRAYER || this==LAY_ON_HANDS || this==AURA_OF_PROTECTION || this==ENCHANT_BOW || this==HOLY_LANCE || this==CLEANSE || this==READ_RECHARGING || this==READ_LULLABY || this==TRINITY_MIND || this==TRINITY_SPIRIT || this==RECALL_INSCRIPTION || this==MIND_FORM || this==SPIRIT_FORM || this==BODY_FORM || this==HOLY_INTUITION || this==BLESS || this==DETACH_SEAL || this==AFFIX_SEAL || this==KEEP_ARMOR_GLYPH || this==TRANSFER_SEAL_GLYPH || this==EQUIP_SECOND || this==RADIANCE || this==SMITE || this==DIVINE_SENSE || this==SUNRAY || this==SHIELD_OF_LIGHT || this==GUIDING_LIGHT || this==HOLY_WEAPON || this==HOLY_WARD || this==WEAPON_ABILITY || this==IMBUE || this==CLOAK || this==USE_CROWN || this==ARMOR_ABILITY || this==USE_MASK || this==VAULT_RECALL || this==VAULT_REWARD || this==SMITH_HARDEN || this==SMITH_UPGRADE || this==SMITH_REFORGE || this==BUY || this==SELL || this==SELL_ONE || this==BUYBACK || this == THROW || this == UPGRADE || this == IDENTIFY || this == PICK_UP || this == EAT || this == EQUIP || this == DROP || this == UNEQUIP || this == ZAP || this == SHATTER || this == DRINK || this == SHOOT;
        }
    }

    public final CoopFloors stairs = new CoopFloors(this);
    private final CoopEnchantScroll[] enchantScroll = new CoopEnchantScroll[2];
    public CoopEnchantScroll enchantScroll(int seat) { return enchantScroll[seat]; }
    public final ItemOwnership ownership = new ItemOwnership();
    public static boolean canWarpAlly(Hero hero, Char ally) {
        if (ally == null || !ally.isAlive() || ally.invisible > 0 || ally.alignment != Char.Alignment.ALLY
                || summonerOf(ally) != hero || !hero.hasTalent(Talent.ALLY_WARP)
                || !ally.canInteract(hero) || Char.hasProp(ally, Char.Property.IMMOVABLE) || Char.hasProp(hero, Char.Property.IMMOVABLE)) return false;
        com.shatteredpixel.shatteredpixeldungeon.levels.Level level = Dungeon.level;
        if (!level.passable[ally.pos] && !hero.flying || !supportedLanding(ally.pos) || !supportedLanding(hero.pos)
                || Char.hasProp(ally, Char.Property.LARGE) && !level.openSpace[hero.pos]
                || Char.hasProp(hero, Char.Property.LARGE) && !level.openSpace[ally.pos]) return false;
        com.watabou.utils.PathFinder.buildDistanceMap(hero.pos, com.watabou.utils.BArray.or(level.passable, level.avoid, null));
        return com.watabou.utils.PathFinder.distance[ally.pos] != Integer.MAX_VALUE;
    }

    public static Hero summonerOf(Char ally) {
        if (ally instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith)
            return ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith)ally).summoner();
        // Buff attachment can ask for an aura while a summoned actor is still being restored.
        // The full restore validates the owner after both heroes and all actors have been read.
        if (Dungeon.coop == null) return null;
        for (int seat = 0; seat < 2; seat++) {
            Hero owner = Dungeon.coop.hero(seat);
            if (owner == null) continue;
            if (ally instanceof PowerOfMany.LightAlly && ((PowerOfMany.LightAlly)ally).belongsTo(owner)
                    || ally instanceof SpiritHawk.HawkAlly && ((SpiritHawk.HawkAlly)ally).belongsTo(owner)
                    || ally instanceof ShadowClone.ShadowAlly && ((ShadowClone.ShadowAlly)ally).belongsTo(owner)) return owner;
        }
        return null;
    }

    public long playerOf(Char owner) {
        for (int seat = 0; seat < heroes.length; seat++) if (owner != null && owner == heroes[seat]) return players[seat];
        return 0;
    }
    private final TrinityDiscoveries[] discoveries = {new TrinityDiscoveries(), new TrinityDiscoveries()};

    public TrinityDiscoveries discoveries(int seat) { return discoveries[seat]; }

    /** Called only after a native collect, identification, or inscription on this physical item. */
    public void witnessOwned(Item item) {
        if (restoring) return;
        Hero carrier=carrierOf(item);
        if (carrier!=null) discoveries[carrier==heroes[0] ? 0 : 1].witness(item);
    }

    public Hero carrierOf(Item item) {
        long owner = ownership.owner(item);
        if (owner == 0) return null;
        for (int seat = 0; seat < heroes.length; seat++) {
            Hero hero = heroes[seat];
            if (players[seat] == owner && hero != null && hero.belongings.contains(item)) {
                return hero;
            }
        }
        return null;
    }

    /** Knowing the type of a held ring is a native discovery before its level becomes known. */
    public void witnessRingType(Hero finder, com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring ring) {
        if (restoring || finder == null || !ring.isKnown()) return;
        long player = playerOf(finder);
        if (player != 0 && ownership.owner(ring) == player)
            discoveries[finder == heroes[0] ? 0 : 1].record(ring.getClass());
    }

    private void bindGear(int seat) {
        if (heroes[seat].heroClass == HeroClass.DUELIST || heroes[seat].heroClass == HeroClass.CLERIC) wandIdentities=true;
        for (Item item : heroes[seat].belongings) {
            ownership.claim(item, players[seat]);
            discoveries[seat].witness(item);
        }
    }

    private final java.util.Map<Long, FloorKnowledge[]> floors = new java.util.HashMap<>();

    public FloorKnowledge knowledge(Hero hero) {
        if (hero == heroes[0]) return knowledge(0);
        if (hero == heroes[1]) return knowledge(1);
        throw new IllegalArgumentException("Unknown observer");
    }

    private FloorKnowledge knowledge(int seat) {
        checkExecutor();
        long floor = ((long)Dungeon.branch << 32) | (Dungeon.depth & 0xffffffffL);
        FloorKnowledge[] memories = floors.get(floor);
        if (memories == null) {
            memories = new FloorKnowledge[heroes.length];
            floors.put(floor, memories);
        }
        if (memories[seat] == null) memories[seat] = new FloorKnowledge(Dungeon.level.length());
        return memories[seat];
    }

    public void replaceEncounterMap() {
        checkExecutor();
        long floor = ((long) Dungeon.branch << 32) | (Dungeon.depth & 0xffffffffL);
        floors.remove(floor);
        for (int seat = 0; seat < heroes.length; seat++) observe(seat);
    }

    public FloorKnowledge observe(int seat) {
        FloorKnowledge memory = knowledge(seat);
        memory.observe(Dungeon.level, heroes[seat]);
        return memory;
    }

    public enum Life { ALIVE, DOWNED, DEAD, REVIVING }
    private final Life[] lives = {Life.ALIVE, Life.ALIVE};
    private final int[] preDeath = new int[2];
    private boolean partyDefeated;

    public Life life(int seat) { return lives[seat]; }
    public enum MaskTrial { OFF, OWNER, ROSTER }
    private MaskTrial maskTrial = MaskTrial.OFF;
    private int maskUser = -1;
    private int maskRights;

    // Only isolated test hosts opt in. Neither the production lobby nor a client command enables a trial.
    void beginMaskTrial(MaskTrial trial) {
        checkExecutor();
        if (trial == null || trial == MaskTrial.OFF || maskTrial != MaskTrial.OFF)
            throw new IllegalStateException("Mask trial already configured or invalid");
        maskTrial = trial;
    }

    private MaskTrial crownTrial = MaskTrial.OFF;
    private int crownUser = -1;
    private int crownRights;
    boolean warpTrial() { return crownTrial != MaskTrial.OFF; }

    void beginCrownTrial(MaskTrial trial) {
        checkExecutor();
        if (trial == null || trial == MaskTrial.OFF || crownTrial != MaskTrial.OFF)
            throw new IllegalStateException("Crown trial already configured or invalid");
        crownTrial = trial;
    }

    public static boolean playableArmorAbility(ArmorAbility ability) {
        return ability instanceof Trinity || ability instanceof Challenge || ability instanceof Feint || ability instanceof ElementalStrike || ability instanceof AscendedForm || ability instanceof PowerOfMany || ability instanceof ShadowClone || ability instanceof DeathMark || ability instanceof SmokeBomb || ability instanceof SpiritHawk || ability instanceof WildMagic || ability instanceof HeroicLeap || ability instanceof WarpBeacon || ability instanceof Endure || ability instanceof Shockwave || ability instanceof SpectralBlades || ability instanceof NaturesPower || ability instanceof ElementalBlast;
    }

    public boolean mayChooseArmor(int seat, boolean useCrown) {
        Hero hero = heroes[seat];
        Armor armor = hero.belongings.armor();
        return crownTrial != MaskTrial.OFF && hero.armorAbility == null && armor != null
                && !(armor instanceof ClassArmor) && ownership.owner(armor) == players[seat]
                && (useCrown ? crownUser < 0 : (crownRights & (1<<seat)) != 0);
    }

    private boolean chooseArmor(Hero hero, Item item, int index, boolean useCrown) {
        if (!mayChooseArmor(activeSeat,useCrown)) return false;
        ArmorAbility[] choices = hero.heroClass.armorAbilities();
        if (index < 0 || index >= choices.length || !playableArmorAbility(choices[index])) return false;
        Armor armor = hero.belongings.armor();
        if (useCrown) {
            if (!(item instanceof KingsCrown) || item.quantity()!=1 || !hero.belongings.backpack.contains(item)
                    || ownership.owner(item)!=players[activeSeat]) return false;
            crownUser=activeSeat;
            if (crownTrial==MaskTrial.ROSTER && heroes[1-activeSeat].armorAbility==null) crownRights=1<<(1-activeSeat);
        } else {
            if (item!=null) return false;
            crownRights &= ~(1<<activeSeat);
        }
        // The partner right has no physical reward object.
        if (choices[index] instanceof ShadowClone || choices[index] instanceof DeathMark) requireGrowthFormat();
        if (hero.heroClass==HeroClass.CLERIC || hero.heroClass==HeroClass.DUELIST) requireSealFormat();
        KingsCrown.applyForSeat(hero, armor, choices[index], useCrown ? (KingsCrown)item : null);
        ownership.replaceEquipped(armor,hero.belongings.armor(),players[activeSeat]);
        return true;
    }

    public boolean mayUseMask(int seat) {
        return maskTrial != MaskTrial.OFF && maskUser < 0 && supportsMaskChoice(heroes[seat]);
    }

    public boolean mayClaimSubclass(int seat) {
        return (maskRights & (1 << seat)) != 0 && supportsMaskChoice(heroes[seat]);
    }

    private static boolean supportsMaskChoice(Hero hero) {
        return hero.subClass == HeroSubClass.NONE && hero.heroClass != null;
    }

    private boolean chooseSubclass(Hero hero, int seat, Item item, int choice, boolean consumeMask) {
        if (!supportsMaskChoice(hero) || choice < 0 || choice >= HeroSubClass.values().length) return false;
        HeroSubClass way = HeroSubClass.values()[choice];
        if (!java.util.Arrays.asList(hero.heroClass.subClasses()).contains(way)) return false;
        if (consumeMask) {
            if (!mayUseMask(seat) || !(item instanceof TengusMask) || item.quantity() != 1
                    || ownership.owner(item) != player(seat) || !hero.belongings.backpack.contains(item)) return false;
            ((TengusMask)item).consumeFor(hero);
            maskUser = seat;
            if (maskTrial == MaskTrial.ROSTER && heroes[1-seat].subClass == HeroSubClass.NONE) maskRights = 1 << (1-seat);
        } else {
            if (item != null || !mayClaimSubclass(seat)) return false;
            maskRights &= ~(1 << seat);
        }
        TengusMask.applySubclass(hero, way);
        if (hero.heroClass == HeroClass.CLERIC || hero.heroClass == HeroClass.DUELIST) requireGrowthFormat();
        hero.sprite.operate(hero.pos);
        return true;
    }

    boolean sealIdentities;

    private void requireSealFormat() {
        requireGrowthFormat();
        sealIdentities = true;
    }

    boolean wandIdentities;
    private boolean extendedGrowth;

    private void requireGrowthFormat() {
        extendedGrowth = true;
        wandIdentities = true;
    }

    private boolean corrosionFormat;

    public void requireCorrosionFormat() {
        corrosionFormat = true;
    }

    static int warpSchema(Bundle state) {
        int schema = state.getInt("schema");
        if (schema != 27) return schema;
        int inner = state.getInt("corrosion_schema");
        if (inner < 1 || inner > 26) throw new IllegalArgumentException("Invalid corrosion envelope");
        return inner;
    }

    private static int stasisSchema(Bundle state) {
        int schema=warpSchema(state);
        if (schema!=26) return schema;
        int inner=state.getInt("warp_schema");
        if (inner<17 || inner>25) throw new IllegalArgumentException("Invalid warp envelope");
        return inner;
    }

    private static int scrollSchema(Bundle state) {
        int schema = stasisSchema(state);
        if (schema != 25) return schema;
        int inner = state.getInt("stasis_schema");
        if (inner < 19 || inner > 24) throw new IllegalArgumentException("Invalid stasis envelope");
        return inner;
    }

    private static int rageSchema(Bundle state) {
        int schema = scrollSchema(state);
        if (schema != 24) return schema;
        int inner = state.getInt("scroll_schema");
        if (inner < 1 || inner > 23 || (!state.contains("enchant0") && !state.contains("enchant1")))
            throw new IllegalArgumentException("Invalid enchantment envelope");
        return inner;
    }

    private static int baseSchema(Bundle state) {
        int schema = rageSchema(state);
        if (schema != 23) return schema;
        int base = state.getInt("base_schema");
        if (base < 1 || base > 22) throw new IllegalArgumentException("Invalid rage base schema");
        return base;
    }

    public static int growthSchema(Bundle state) {
        int version = baseSchema(state);
        if (version != 22) return version;
        int growth = state.getInt("growth_schema");
        // Discovery envelopes are written for rosters with a cleric (introduced in 19).
        if (growth < 19 || growth > 21) throw new IllegalArgumentException("Invalid discovery growth format");
        return growth;
    }

    static int floorSchema(Bundle state) {
        int growth = growthSchema(state);
        if (growth < 16 || growth > 21) return growth;
        int floor = state.getInt("floor_schema");
        if (floor < 1 || floor > 15) throw new IllegalArgumentException("Invalid growth floor format");
        return floor;
    }

    public enum Ending { NONE, OFFER, CONTINUING, PROPOSED, WON, ASCENT_PROPOSED, ASCENT_READY, ASCENDING, ASCENDED }
    public boolean ascentHistory() { return ending==Ending.ASCENT_PROPOSED || ending==Ending.ASCENT_READY || ending==Ending.ASCENDING || ending==Ending.ASCENDED; }
    public boolean won() { return ending==Ending.WON || ending==Ending.ASCENDED; }
    public boolean chooseAscent(int seat) {
        checkExecutor();
        if (seat<0 || seat>1 || stairs.ascentGate()<0) return false;
        if (ending==Ending.CONTINUING && seat==amuletOwner()) ending=Ending.ASCENT_PROPOSED;
        else if (ending==Ending.ASCENT_PROPOSED && seat!=amuletOwner()) ending=Ending.ASCENT_READY;
        else return false;
        return true;
    }
    void startAscent() {
        if (ending!=Ending.ASCENT_READY) throw new IllegalStateException("Missing ascent assent");
        com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(hero(amuletOwner()),com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge.class);
        Statistics.highestAscent=25;
        ending=Ending.ASCENDING;
    }
    void reachSurface() {
        if (ending!=Ending.ASCENDING || Dungeon.depth!=1 || amuletOwner()<0 || Statistics.highestAscent!=1)
            throw new IllegalStateException("Missing surface conditions");
        Statistics.ascended=true;
        ending=Ending.ASCENDED;
    }
    private Ending ending = Ending.NONE;
    public Ending ending() { return ending; }

    public int amuletOwner() {
        for (int seat=0;seat<2;seat++) if (hero(seat)!=null && hero(seat).belongings.getItem(
                com.shatteredpixel.shatteredpixeldungeon.items.Amulet.class)!=null) return seat;
        return -1;
    }

    public void foundAmulet(Hero hero) {
        checkExecutor();
        if (Dungeon.depth!=26 || ending!=Ending.NONE || playerOf(hero)==0 || amuletOwner()<0)
            throw new IllegalStateException("Unexpected amulet pickup");
        ending=Ending.OFFER;
    }

    /** Both roster members decide. A vote changes neither the actor clock nor world RNG. */
    public boolean chooseEnding(int seat, boolean accept) {
        checkExecutor();
        int owner=amuletOwner();
        if (owner<0 || seat<0 || seat>1 || ending==Ending.NONE || ending==Ending.WON) return false;
        if (!accept) {
            if (ending!=Ending.PROPOSED && ending!=Ending.ASCENT_PROPOSED && ending!=Ending.ASCENT_READY && !(ending==Ending.OFFER && seat==owner)) return false;
            ending=Ending.CONTINUING;
        } else if (ending==Ending.PROPOSED && seat!=owner) {
            ending=Ending.WON;
        } else if ((ending==Ending.OFFER || ending==Ending.CONTINUING) && seat==owner) {
            ending=Ending.PROPOSED;
        } else return false;
        return true;
    }

    public boolean awaitingEnding() {
        return ending==Ending.OFFER || ending==Ending.PROPOSED || ending==Ending.ASCENT_PROPOSED || won();
    }

    public boolean defeated() { return partyDefeated; }
    public int preDeathHP(int seat) { return preDeath[seat]; }

    public static int reviveHP(int before, int maximum) {
        if (before <= 0 || maximum <= 0) throw new IllegalArgumentException("Invalid revival HP");
        return (int)Math.min(maximum, Math.max(1L, 3L * before / 10));
    }

    private Ankh resource(int seat) {
        for (Ankh ankh : heroes[seat].belongings.getAllItems(Ankh.class)) {
            if (ownership.owner(ankh) == players[seat] && ankh.quantity() > 0
                    && heroes[seat].belongings.backpack.contains(ankh)) return ankh;
        }
        return null;
    }

    public void survivedLethalHit(Hero hero, int before) {
        checkExecutor();
        Berserk rage = hero.buff(Berserk.class);
        if (playerOf(hero) != 0 && before > 0 && hero.HP == 0 && rage != null && rage.sustainsLife()) {
            preDeath[hero == heroes[0] ? 0 : 1] = before;
        }
    }

    public void fallen(Hero hero, int before) {
        checkExecutor();
        int seat = hero == heroes[0] ? 0 : 1;
        if (hero != heroes[seat]) throw new IllegalStateException("Unregistered casualty");
        if (lives[seat] != Life.ALIVE) return;
        if (before == 0 && hero.buff(Berserk.class) != null && hero.buff(Berserk.class).hasRageSurvival()) {
            before = preDeath[seat];
        }
        preDeath[seat] = Math.max(0, before);
        lives[seat] = before > 0 && resource(seat) != null ? Life.DOWNED : Life.DEAD;
        hero.HP = 0;
        hero.ready = false;
        hero.curAction = null;
        hero.next();
        Actor.freezeCoop(hero, true);
        if (activeSeat == seat) activeSeat = -1;
        if (lives[1-seat] != Life.ALIVE) {
            lives[0] = lives[1] = Life.DEAD;
            partyDefeated = true;
            activeSeat = -1;
        }
        for (int i=0; i<heroes.length; i++) {
            if (lives[i] == Life.DEAD) Buff.detach(heroes[i], Stasis.StasisBuff.class);
        }
    }

    /** Settle an engine-owned environmental effect batch that invalidated an open offer. */
    public void finishEffects() {
        checkExecutor();
        if (activeSeat < 0 && !stopped() && !partyDefeated) advance();
    }

    private boolean revive(Hero rescuer, int cell) {
        int seat = 1 - activeSeat;
        Hero ally = heroes[seat];
        Level level = Dungeon.level;
        if (lives[seat] != Life.DOWNED || cell != ally.pos || !level.adjacent(rescuer.pos, cell)
                || !level.passable[cell] || level.pit[cell] || Actor.findChar(cell) != ally
                || !knowledge(activeSeat).visible(cell)) return false;
        Ankh ankh = resource(seat);
        if (ankh == null) return false;
        lives[seat] = Life.REVIVING;
        ankh.detach(ally.belongings.backpack);
        ally.HP = reviveHP(preDeath[seat], ally.HT);
        Actor.freezeCoop(ally, false);
        lives[seat] = Life.ALIVE;
        rescuer.spendAndNext(1f);
        return true;
    }

    private final Thread executor = Thread.currentThread();
    private final Hero[] heroes = new Hero[2];
    private final long[] players = new long[2];
    private final String previousBase;
    private final Runnable restoreJournal, restoreBadges;
    private final Mob[] lastOpponent = new Mob[2];
    private final long[] attackTurn = new long[2];

    boolean recentlyAttacked(int seat, Mob mob) {
        return lastOpponent[seat] == mob && turn - attackTurn[seat] <= 2;
    }

    private int activeSeat = -1;
    private long turn;
    private String stopReason;

    private boolean restoring, executing, restoredAfterTravel;
    // Owner-thread diagnostics only. Destination contents must never appear in a public rejection.
    String destinationBlocker;
    public boolean restoredAfterTravel() { return restoredAfterTravel; }
    public boolean restoring() { return restoring; }

    void restoreVaultGear(Hero hero, Bundle gear) {
        restoring=true;
        try {
            hero.belongings.restoreFromBundle(gear);
            restorePackCharge(hero);
        } finally {
            Belongings.bundleRestoring=false;
            restoring=false;
        }
    }

    private static void restorePackCharge(Hero hero) {
        for (Item item : hero.belongings.backpack) {
            if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag)
                ((com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag)item).owner=hero;
            if (item instanceof MagesStaff) ((MagesStaff)item).applyWandChargeBuff(hero);
            if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)
                ((com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)item).restoreCoopCharge(hero);
        }
    }

    Level unpackFloor(Bundle parked) {
        restoring=true;
        try { return (Level)parked.get("level"); }
        finally { restoring=false; }
    }

    private CoopSession() {
        Random.enterWorld();
        restoreJournal = Journal.enterCoop();
        restoreBadges = Badges.enterCoop();
        previousBase = FileUtils.setBasePath(FileUtils.basePath() + "coop/");
    }

    /** Enter from a fresh engine host with initialized libGDX services. */
    public static CoopSession open(long seed, long player, HeroClass type) {
        return open(seed, player, type, MaskTrial.OFF, MaskTrial.OFF);
    }

    public static synchronized CoopSession open(long seed, long player, HeroClass type, MaskTrial mask, MaskTrial crown) {
        if (mask == null || crown == null) throw new IllegalArgumentException("Growth policy");
        validatePlayer(player, type);
        if (Dungeon.coop != null || Dungeon.hero != null || Dungeon.level != null) {
            throw new IllegalStateException("A dungeon is already loaded");
        }
        if (SPDSettings.challenges() != 0) throw new UnsupportedOperationException("Challenges");
        CoopSession session = new CoopSession();
        Dungeon.coop = session;
        try {
            session.maskTrial = mask;
            session.crownTrial = crown;
            Dungeon.seed = seed;
            Dungeon.init(type);
            session.players[0] = player;
            session.heroes[0] = Dungeon.hero;
            session.bindGear(0);
            Level first=Dungeon.newLevel();
            CoopFloors.requireSupportedResidents(first);
            Dungeon.switchLevel(first, -1);
            return session;
        } catch (RuntimeException | Error failure) {
            session.close();
            throw failure;
        }
    }

    private static void validatePlayer(long player, HeroClass type) {
        if (player <= 0) throw new IllegalArgumentException("Player ID must be positive");
        if (type != HeroClass.WARRIOR && type != HeroClass.HUNTRESS && type != HeroClass.MAGE && type != HeroClass.ROGUE && type != HeroClass.DUELIST && type != HeroClass.CLERIC) {
            throw new UnsupportedOperationException("Unsupported co-op hero class");
        }
    }

    public void join(long player, HeroClass type) {
        checkExecutor();
        validatePlayer(player, type);
        if (stopped() || heroes[1] != null || player == players[0]) throw new IllegalStateException("Seat unavailable");
        Level level = Dungeon.level;
        int spawn = -1;
        for (int offset : PathFinder.NEIGHBOURS8) {
            int cell = heroes[0].pos + offset;
            if (safeFloor(cell,false) && Actor.findChar(cell) == null && level.heaps.get(cell) == null) {
                // Preserve the established placement when grass and bare cells are both free.
                if (level.map[cell] == Terrain.HIGH_GRASS || level.map[cell] == Terrain.FURROWED_GRASS) {
                    if (spawn == -1) spawn = cell;
                } else {
                    spawn = cell;
                    break;
                }
            }
        }
        if (spawn == -1) throw new IllegalStateException("No free entrance neighbour");
        try {
            Hero guest = new Hero();
            guest.live();
            // Starter gear must not consume the running world's random stream.
            Random.pushGenerator(Dungeon.seed ^ player);
            try {
                type.initHero(guest, new QuickSlot());
            } finally {
                Random.popGenerator();
            }
            heroes[1] = guest;
            players[1] = player;
            bindGear(1);
            guest.pos = spawn;
            guest.viewDistance = level.viewDistance;
            guest.fieldOfView = new boolean[level.length()];
            Actor.add(guest);
            for (Char ch : Actor.chars()) attachSprite(ch);
            advance();
        } catch (RuntimeException failure) {
            stop(failure.toString());
            throw failure;
        }
    }

    public Hero hero(int seat) { return heroes[seat]; }
    public long player(int seat) { return players[seat]; }
    public int activeSeat() { return activeSeat; }
    public long turn() { return turn; }
    public boolean stopped() { return stopReason != null; }
    public String stopReason() { return stopReason; }

    public void checkExecutor() {
        if (Thread.currentThread() != executor || Dungeon.coop != this) {
            throw new IllegalStateException("Not the current session executor");
        }
    }

    public void checkCanAdvance() {
        checkExecutor();
        if (stopped() || partyDefeated || activeSeat >= 0 || heroes[1] == null) {
            throw new IllegalStateException("Session is not awaiting an actor step");
        }
    }

    public void stop(String reason) {
        checkExecutor();
        if (stopReason == null) stopReason = reason;
        activeSeat = -1;
    }

    /** Original boss abilities need a hero even while their native AI has no visible enemy. */
    public Hero bossTarget(Char preferred, int origin) {
        if (preferred != null && playerOf(preferred) != 0 && preferred.isAlive()) return (Hero)preferred;
        if (!heroes[0].isAlive()) return heroes[1];
        if (heroes[1].isAlive() && Dungeon.level.distance(origin,heroes[1].pos)
                < Dungeon.level.distance(origin,heroes[0].pos)) return heroes[1];
        return heroes[0];
    }

    public boolean unseenByParty(int pos,int minDistance) {
        boolean[] sight=new boolean[Dungeon.level.length()];
        for (int seat=0;seat<2;seat++) {
            Hero observer=hero(seat);
            if (!observer.isAlive()) continue;
            Dungeon.level.updateDirectSight(observer,sight);
            if (sight[pos] || Dungeon.level.distance(observer.pos,pos)<minDistance) return false;
        }
        return true;
    }

    public int nearerToParty(int pos1,int pos2) {
        int d1 = Integer.MAX_VALUE, d2 = Integer.MAX_VALUE;
        for (int seat = 0; seat < 2; seat++) {
            Char hero = hero(seat);
            if (!hero.isAlive()) continue;
            PathFinder.buildDistanceMap(hero.pos, Dungeon.level.passable);
            if (pos1 >= 0) d1 = Math.min(d1, PathFinder.distance[pos1]);
            if (pos2 >= 0) d2 = Math.min(d2, PathFinder.distance[pos2]);
        }
        return d2 < d1 ? pos2 : pos1;
    }

    /** Secondary ordering only for equal actor time and priority. */
    public boolean precedes(Actor candidate, Actor other) {
        if (candidate == heroes[0] && other == heroes[1]) return true;
        if (candidate == heroes[1] && other == heroes[0]) return false;
        return other != null && candidate.id() < other.id();
    }

    /** Called by Actor.process after it has selected the actual next actor. */
    public boolean offerTurn(Actor selected) {
        checkCanAdvance();
        if (selected instanceof Mob && !basicMob(selected)) {
            throw new UnsupportedOperationException("Unsupported monster: " + selected.getClass().getSimpleName());
        }
        if (selected instanceof Char) attachSprite((Char)selected);
        for (int seat = 0; seat < heroes.length; seat++) {
            Hero hero = heroes[seat];
            if (selected == hero && hero.paralysed == 0) {
                // Let the normal input-ready path refresh FOV, visible enemies and action state.
                hero.curAction = null;
                hero.ready = false;
                hero.act();
                activeSeat = seat;
                turn++;
                return true;
            }
        }
        return false;
    }

    public void attachSprite(Char ch) {
        checkExecutor();
        if (ch.sprite == null) new EngineSprite(ch);
    }

    private static boolean caveDepth() {
        return Dungeon.branch==0 && Dungeon.depth>=11 && Dungeon.depth<=14;
    }

    public static boolean basicMob(Actor actor) {
        Class<?> type = actor.getClass();
        if (type==SmokeBomb.NinjaLog.class || type==Feint.AfterImage.class || type==PowerOfMany.LightAlly.class || type==ShadowClone.ShadowAlly.class || type==SpiritHawk.HawkAlly.class) return true;
        if (Dungeon.branch==1 && CoopFloors.vaultDepth(Dungeon.depth)) return CoopVault.resident(actor.getClass());
        if (Dungeon.branch == 1) {
            return type == CrystalGuardian.class || type == CrystalSpire.class || type == CrystalWisp.class
                    || type == GnollGeomancer.class || type == GnollGuard.class || type == GnollSapper.class;
        }
        if ((Dungeon.depth==19 || Dungeon.depth>=21 && Dungeon.depth<=24) && type==Succubus.class) return true;
        if (Dungeon.depth>=21 && Dungeon.depth<=24 && (type==DemonSpawner.class || type==RipperDemon.class || type==Eye.class || type==Scorpio.class || type==Acidic.class)) return true;
        if (Dungeon.depth==25 && (type==YogDzewa.class || type==YogDzewa.Larva.class || type==YogDzewa.YogRipper.class
                || type==YogFist.BurningFist.class || type==YogFist.SoiledFist.class || type==YogFist.RottingFist.class
                || type==YogFist.RustedFist.class || type==YogFist.BrightFist.class || type==YogFist.DarkFist.class)) return true;
        if (Dungeon.depth==20 && (type==DwarfKing.class || type==DwarfKing.DKGhoul.class || type==DwarfKing.DKMonk.class || type==DwarfKing.DKWarlock.class || type==DwarfKing.DKGolem.class)) return true;
        if (Dungeon.depth>=16 && Dungeon.depth<=19 && (type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul.class
                || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock.class
                || type==Elemental.FrostElemental.class || type==Elemental.FireElemental.class || type==Elemental.ShockElemental.class
                || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Monk.class || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Golem.class
                || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.class
                || (Dungeon.depth==16 && type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.class))) return true;
        if (type==ArmoredStatue.class) {
            ArmoredStatue statue=(ArmoredStatue)actor;
            Armor armor=statue.armor();
            if (armor instanceof ScaleArmor && armor.glyph instanceof Viscosity)
                return statue.weapon() instanceof Gauntlet && statue.weapon().enchantment instanceof Blazing;
            if (armor instanceof PlateArmor && armor.glyph instanceof Brimstone)
                return statue.weapon() instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive && statue.weapon().enchantment instanceof Corrupting;
            return statueWeapon(statue.weapon()) && basicArmorType(armor)
                    && (armor.glyph instanceof Potential || armor.glyph instanceof Repulsion
                    || armor.glyph instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic
                    || armor.glyph instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness);
        }
        if (type==Statue.class) return statueWeapon(((Statue)actor).weapon());
        return (Dungeon.depth>=CoopFloors.FIRST_PRISON && Dungeon.depth<=9 && type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Skeleton.class)
                || ((Dungeon.depth==CoopFloors.FIRST_PRISON || Dungeon.depth==11) && type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.class)
                || (Dungeon.depth>=7 && Dungeon.depth<=9 && (type==Necromancer.class || type==SpectralNecromancer.class || type==Necromancer.NecroSkeleton.class
                        || type==Wandmaker.class || type==RotHeart.class || type==RotLasher.class
                        || type==Elemental.NewbornFireElemental.class || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard.class || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100.class))
                || ((Dungeon.depth==9 || caveDepth()) && type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat.class)
                || (caveDepth() && (type==Brute.class || type==ArmoredBrute.class || type==Shaman.RedShaman.class || type==Shaman.BlueShaman.class || type==Shaman.PurpleShaman.class))
                || (caveDepth() && Dungeon.depth>=12 && (type==Spinner.class || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.class))
                || (caveDepth() && Dungeon.depth>=13 && (type==DM200.class || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM201.class))
                || (Dungeon.depth==15 && (type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300.class || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon.class))
                || type==Wraith.class && ((Wraith)actor).summoner()!=null
                || type==CorpseDust.DustWraith.class || (Dungeon.depth>=7 && Dungeon.depth<=9 && (type==Wraith.class || type==TormentedSpirit.class))
                || type == GnollExile.class || type == PhantomPiranha.class || type == Rat.class || type == Snake.class || type == Gnoll.class || type == Albino.class
                || type == Mimic.class || type == CrystalMimic.class || type == GoldenMimic.class || type == Sheep.class || type == Bee.class || type == Piranha.class || type == SentryRoom.Sentry.class
                || type==Crab.class || type==HermitCrab.class || type==Swarm.class || type==Slime.class || type==CausticSlime.class || type==Thief.class
                || (Dungeon.depth>=2 && Dungeon.depth<=4 && type==Ghost.class)
                || (Dungeon.depth==2 && type==FetidRat.class) || (Dungeon.depth==3 && type==GnollTrickster.class)
                || (Dungeon.depth==10 && type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu.class) || (Dungeon.depth==4 && type==GreatCrab.class) || (Dungeon.depth==5 && (type==Goo.class || type==RatKing.class));
    }

    /** Only passive weapon statistics and on-hit effects run for a statue. Hero abilities have their own gate. */
    public static boolean statueWeapon(Item item) {
        if (!(item instanceof MeleeWeapon)) return false;
        Class<?> base = item.getClass();
        Weapon.Enchantment enchantment=((Weapon)item).enchantment;
        if (base == com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarScythe.class)
            return enchantment instanceof Projecting || enchantment instanceof Shocking || enchantment instanceof Corrupting;
        if (base == com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive.class) return enchantment instanceof Venomous || enchantment instanceof Corrupting;
        if (base == Greatshield.class) return enchantment instanceof Kinetic;
        if (base == Gauntlet.class) return enchantment instanceof Unstable || enchantment instanceof Blazing;
        if (base == com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe.class) return enchantment instanceof Projecting;
        if (base == com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff.class) return ((MeleeWeapon)item).enchantment instanceof Venomous;
        // Runic Slash is a hero ability. A statue uses the blade's tier-four passive damage curve.
        if (base == com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade.class)
            return ((Weapon)item).enchantment instanceof Shocking;
        if (base==com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.AssassinsBlade.class)
            return enchantment instanceof Kinetic || enchantment instanceof Shocking;
        if (base==com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword.class)
            return enchantment instanceof Kinetic || enchantment instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
        if (base==Scimitar.class && ((Weapon)item).enchantment instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic) return true;
        boolean extendedReach = base == Spear.class || base == Whip.class;
        // Innate reach and Projecting both use the original Weapon.reachFactor.
        if (base == Whip.class && ((Weapon)item).enchantment instanceof Projecting) return false;
        if (!extendedReach && base != Sword.class && base != HandAxe.class && base != Sickle.class && base != BattleAxe.class && base != Shortsword.class
                && base != Dirk.class && base != RoundShield.class && base != Scimitar.class) return false;
        Weapon.Enchantment effect = ((Weapon)item).enchantment;
        if (effect == null) return false;
        Class<?> type = effect.getClass();
        return type == com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling.class || type == Kinetic.class || type == Venomous.class || type == Vampiric.class || type == Projecting.class || type == Unstable.class
                || type == Shocking.class || type == Lucky.class || type == Vorpal.class
                || type == com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Corrupting.class;
    }

    public static boolean disguised(Char actor) {
        return actor instanceof Mimic && basicMob(actor) && actor.alignment==Char.Alignment.NEUTRAL;
    }

    /** Landing effects shared by voluntary and forced movement on supported sewer floors. */
    public static boolean supportedLanding(int cell) {
        Level level = Dungeon.level;
        if (!level.insideMap(cell) || level.pit[cell] || level.map[cell] == Terrain.WELL ||
                (level.plants.get(cell) != null && !level.plants.get(cell).supportsCoopLanding())) return false;
        Trap trap = level.traps.get(cell);
        return trap == null || !trap.active || trap.supportedInCoop();
    }

    private boolean safeFloor(int cell,boolean moving) {
        Level level = Dungeon.level;
        if (!supportedLanding(cell) || level.getTransition(cell) != null || (!moving && level.plants.get(cell)!=null)) return false;
        if (moving && level.plants.get(cell) instanceof com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf
                && com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf.requiresWardenReturn(heroes[activeSeat])) return false;
        if (level.traps.get(cell) != null)
            return moving && (level.map[cell] == Terrain.TRAP || level.map[cell] == Terrain.SECRET_TRAP || level.map[cell] == Terrain.INACTIVE_TRAP);
        if (!level.passable[cell]) return false;
        int terrain = level.map[cell];
        return (moving && (terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR))
                || terrain == Terrain.CUSTOM_DECO_EMPTY || terrain == Terrain.EMPTY || terrain == Terrain.EMPTY_SP || terrain == Terrain.GRASS
                || terrain == Terrain.HIGH_GRASS || terrain == Terrain.FURROWED_GRASS
                || terrain == Terrain.WATER || terrain == Terrain.EMBERS || terrain == Terrain.EMPTY_DECO;
    }

    /** Foods with an authoritative eating path. */
    public static boolean basicFood(Item item) {
        return item!=null && (item.getClass()==Food.class || item.getClass()==SmallRation.class || item.getClass()==SupplyRation.class || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.food.Berry.class);
    }

    public static boolean basicArmorType(Item item) {
        return item!=null && (item.getClass()==ClothArmor.class
                || item.getClass()==LeatherArmor.class
                || item.getClass()==MailArmor.class);
    }

    public static boolean equipmentForScroll(Item item) {
        return item instanceof Ring || basicArmorType(item) || basicWeaponType(item) || supportedWandType(item)
                || basicMissile(item) && item.isUpgradable();
    }

    public static boolean basicMissile(Item item) {
        return item!=null && (item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone.class
                || item.getClass()==ThrowingSpike.class || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife.class
                || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart.class
                || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.BlindingDart.class
                || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.class)
                && !item.cursed && SpiritBow.supportedCoopEnchantment((Weapon)item)
                && (item.isUpgradable() ? item.trueLevel()>=0 : item.trueLevel()==0);
    }

    public boolean missileLandingAllowed(Hero hero, int cell) {
        Level level=Dungeon.level;
        if (!level.insideMap(cell) || !knowledge(activeSeat).visible(cell) || cell==hero.pos
                || level.solid[cell] || !supportedLanding(cell) || level.getTransition(cell)!=null) return false;
        Char hit=Actor.findChar(cell);
        return hit==null || (hit instanceof Mob && basicMob(hit) && hit.alignment==Char.Alignment.ENEMY
                && hit.isAlive() && hit.invisible==0 && !hero.isCharmedBy(hit));
    }

    public void recordMissileOpponent(Char hit) {
        if (hit instanceof Mob) {
            lastOpponent[activeSeat]=(Mob)hit;
            attackTurn[activeSeat]=turn;
        }
    }

    public Char abilityTarget(Hero hero, int cell) {
        if (!Dungeon.level.insideMap(cell) || !knowledge(hero).visible(cell)) return null;
        Char foe = Actor.findChar(cell);
        if (!(foe instanceof Mob) || !basicMob(foe) || !foe.isAlive() || foe.invisible > 0
                || foe.alignment != Char.Alignment.ENEMY || hero.isCharmedBy(foe)) return null;
        return foe;
    }

    /** A single rogue in a locked roster owns the native cache. Two-rogue allocation is undecided. */
    public Hero cachedRationsOwner() {
        if (heroes[0] == null || heroes[1] == null) return null;
        boolean first = heroes[0].heroClass == HeroClass.ROGUE;
        boolean second = heroes[1].heroClass == HeroClass.ROGUE;
        if (first == second) return null;
        return first ? heroes[0] : heroes[1];
    }

    public boolean offersTalent(Hero hero, Talent talent) {
        if (talent == Talent.LONGRANGE_WARP) return warpTrial();
        return talent == Talent.CACHED_RATIONS ? hero == cachedRationsOwner() : playableTalent(talent);
    }

    public static boolean playableTalent(Talent talent) {
        switch (talent) {
            case CACHED_RATIONS:
            case HOLD_FAST: case DEATHLESS_FURY: case ENRAGED_CATALYST:
            case LAY_ON_HANDS: case AURA_OF_PROTECTION:
            case ENHANCED_RINGS: case BOUNTY_HUNTER: case VARIED_CHARGE: case COMBINED_ENERGY: case COMBINED_LETHALITY:
            case ALLY_WARP: case NECROMANCERS_MINIONS:
            case HALLOWED_GROUND: case WALL_OF_LIGHT: case MNEMONIC_PRAYER:
            case PRECISE_ASSAULT: case DEADLY_FOLLOWUP: case TWIN_UPGRADES: case UNENCUMBERED_SPIRIT:
            case MYSTICAL_CHARGE: case LIGHT_CLOAK: case LIGHT_READING:
            case SOUL_EATER: case SOUL_SIPHON: case EMPOWERED_STRIKE: case EXCESS_CHARGE:
            case ASSASSINS_REACH: case ENHANCED_LETHALITY: case MONASTIC_VIGOR:
            case SPEEDY_STEALTH: case EVASIVE_ARMOR: case PROJECTILE_MOMENTUM:
            case DURABLE_TIPS: case FARSIGHT: case SHIELDING_DEW: case BARKSKIN: case SEER_SHOT:
            case CLEANSE: case HOLY_LANCE: case SHARED_UPGRADES: case SHARED_ENCHANTMENT:
            case STRONGMAN: case ENDLESS_RAGE: case DESPERATE_POWER: case POINT_BLANK:
            case HEARTY_MEAL: case IRON_WILL: case PROVOKED_ANGER: case VETERANS_INTUITION:
            case EMPOWERING_MEAL: case SCHOLARS_INTUITION: case LINGERING_MAGIC: case BACKUP_BARRIER:
            case FOLLOWUP_STRIKE: case SURVIVALISTS_INTUITION: case NATURES_AID: case NATURES_BOUNTY:
            case IRON_STOMACH: case ENERGIZING_MEAL: case INVIGORATING_MEAL:
            case RUNIC_TRANSFERENCE: case IMPROVISED_PROJECTILES: case LIQUID_WILLPOWER: case LETHAL_MOMENTUM: case INSCRIBED_POWER:
            case ARCANE_VISION: case HEIGHTENED_SENSES: case WAND_PRESERVATION: case SHIELD_BATTERY: case LIQUID_NATURE: case REJUVENATING_STEPS: case DURABLE_PROJECTILES:
            case DIVINE_SENSE: case SUNRAY: case SHIELD_OF_LIGHT:
            case PROTECTIVE_SHADOWS: case SUCKER_PUNCH: case MYSTICAL_MEAL: case INSCRIBED_STEALTH: case THIEFS_INTUITION:
            case STASIS: case BEAMING_RAY: case LIFE_LINK: case DIVINE_INTERVENTION: case JUDGEMENT: case FLASH:
            case CLOSE_THE_GAP: case INVIGORATING_VICTORY: case ELIMINATION_MATCH:
            case HASTY_RETREAT: case BODY_REPLACEMENT: case SHADOW_STEP:
            case GROWING_POWER: case FEIGNED_RETREAT: case EXPOSE_WEAKNESS:
            case STRIKING_FORCE: case ELEMENTAL_REACH:
            case BODY_SLAM: case DOUBLE_JUMP: case IMPACT_WAVE:
            case EXPANDING_WAVE: case STRIKING_WAVE: case SHOCK_FORCE:
            case SUSTAINED_RETRIBUTION: case SHRUG_IT_OFF: case EVEN_THE_ODDS:
            case BLAST_RADIUS: case ELEMENTAL_POWER: case REACTIVE_BARRIER:
            case WILD_POWER: case FIRE_EVERYTHING: case CONSERVED_MAGIC:
            case FAN_OF_BLADES: case PROJECTING_BLADES: case SPIRIT_BLADES: case NATURES_WRATH: case WILD_MOMENTUM:
            case LONGRANGE_WARP: case TELEFRAG: case REMOTE_BEACON: case COUNTER_ABILITY: case DIRECTED_POWER: case EAGLE_EYE: case SWIFT_SPIRIT: case GO_FOR_THE_EYES:
            case ROGUES_FORESIGHT: case SILENT_STEPS: case HEROIC_ENERGY: case SEARING_LIGHT: case LIQUID_AGILITY: case SWIFT_EQUIP: case WEAPON_RECHARGING:
            case PATIENT_STRIKE: case AGGRESSIVE_BARRIER: case LETHAL_HASTE: case WIDE_SEARCH:
            case ADVENTURERS_INTUITION: case STRENGTHENING_MEAL: case FOCUSED_MEAL: case SATIATED_SPELLS: case ENLIGHTENING_MEAL:
            case RECALL_INSCRIPTION: case MIND_FORM: case SPIRIT_FORM: case BODY_FORM: case HOLY_INTUITION: case BLESS: case SHADOW_BLADE: case CLONED_ARMOR: case PERFECT_COPY:
            case FEAR_THE_REAPER: case DOUBLE_MARK: case DEATHLY_DURABILITY:
            case CLEAVE: case ENHANCED_COMBO: case LETHAL_DEFENSE:
                return true;
            default: return false;
        }
    }

    public static int disintegrationReach(Hero hero) {
        if (hero.subClass != HeroSubClass.BATTLEMAGE || !(hero.belongings.weapon() instanceof MagesStaff)) return 1;
        MagesStaff staff = (MagesStaff) hero.belongings.weapon();
        // Public range queries must never invoke enchantment procs or consume their trackers.
        if (staff.wandClass() != WandOfDisintegration.class || staff.enchantment != null) return 1;
        return 1 + Math.round(Wand.procChanceMultiplier(hero));
    }

    public static boolean supportedScroll(Item item) {
        return item!=null && (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment || item.getClass()==ScrollOfUpgrade.class || item.getClass()==ScrollOfIdentify.class || item.getClass()==ScrollOfRecharging.class || item.getClass()==ScrollOfLullaby.class);
    }

    private boolean readEquipmentScroll(Hero reader, Action action, Item source, int selection) {
        if (!(source instanceof ScrollOfIdentify || source instanceof ScrollOfUpgrade) || !(reader.belongings.backpack.contains(source))
                || !((Scroll)source).isKnown() || ownership.owner(source)!=playerOf(reader)
                || (action==Action.UPGRADE)!=(source instanceof ScrollOfUpgrade)
                || reader.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class)!=null
                || reader.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class)!=null) return false;
        int index=0;
        for (Item target:reader.belongings) {
            if (++index!=selection) continue;
            if (!equipmentForScroll(target) || ownership.owner(target)!=playerOf(reader)) return false;
            if (action==Action.UPGRADE ? !target.isUpgradable() : target.isIdentified()) return false;
            Scroll scroll=(Scroll)source.detach(reader.belongings.backpack);
            if (scroll==null) throw new IllegalStateException("Scroll missing at commit");
            if (action==Action.UPGRADE) {
                scroll.finishReading(reader);
                ((ScrollOfUpgrade)scroll).upgradeFor(reader,target);
            } else {
                ScrollOfIdentify.IDItem(target);
                scroll.finishReading(reader);
            }
            return true;
        }
        return false;
    }

    private boolean basicArmor(Item item) {
        return basicArmorType(item) && supportedWornGlyph(((Armor)item).glyph);
    }

    static boolean supportedWornGlyph(Armor.Glyph glyph) {
        return glyph==null || glyph instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness
                || glyph instanceof Brimstone || glyph instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow;
    }

    static boolean supportedWornEnchant(Weapon.Enchantment effect) {
        return effect==null || effect instanceof Venomous || effect instanceof Blocking || effect instanceof Kinetic
                || effect instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
    }

    private static boolean supportedWandType(Item item) {
        return item instanceof WandOfPrismaticLight || item instanceof WandOfDisintegration || item instanceof WandOfMagicMissile || item instanceof WandOfBlastWave || item instanceof WandOfFireblast || item instanceof WandOfFrost || item instanceof WandOfLightning;
    }

    public static boolean supportedHandheldWand(Item item) {
        return supportedWandType(item) && item.cursedKnown && !item.cursed;
    }

    private static TargetedDamageWand wandFor(Hero hero, Item item) {
        if (item instanceof MagesStaff && hero.belongings.weapon() == item)
            return ((MagesStaff)item).coopWand();
        if (supportedHandheldWand(item) && hero.belongings.backpack.contains(item))
            return (TargetedDamageWand)item;
        return null;
    }

    public static boolean supportedWeaponAbility(Item item) {
        return item instanceof Rapier || item instanceof Crossbow || item instanceof Quarterstaff || item instanceof RoundShield;
    }

    public static boolean basicWeaponType(Item item) {
        return item!=null && ((item instanceof MagesStaff && ((MagesStaff)item).coopWand()!=null)
                || item.getClass()==Quarterstaff.class || item.getClass()==RoundShield.class || item.getClass()==Gloves.class || item.getClass()==WornShortsword.class
                || item.getClass()==Rapier.class || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel.class || item.getClass()==Shortsword.class || item.getClass()==Dagger.class || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dirk.class || item.getClass()==Sickle.class
                || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow.class);
    }

    private boolean basicWeapon(Item item) {
        return basicWeaponType(item) && supportedWornEnchant(((MeleeWeapon)item).enchantment) && !item.cursed;
    }

    /**
     * Consume one offered turn. Cell is a destination for MOVE and ATTACK. PICK_UP
     * requires the exact item at the actor's feet. Item references are engine-local.
     */
    public boolean act(long player, long offeredTurn, Action action, int cell, Item item) {
        checkExecutor();
        restoredAfterTravel=false;
        if (awaitingEnding() || ending==Ending.ASCENT_READY && action!=Action.TRAVEL) return false;
        destinationBlocker=null;
        if (activeSeat < 0 || lives[activeSeat] != Life.ALIVE || players[activeSeat] != player || offeredTurn != turn || action == null) return false;
        if (action.requiresItem() && (item == null || !ownership.allowed(item, player))) return false;
        Hero hero = heroes[activeSeat];
        Level level = Dungeon.level;
        CoopEnchantScroll pending = enchantScroll[activeSeat];
        if (pending != null && action != Action.ENCHANT_TARGET && action != Action.ENCHANT_CHOOSE && action != Action.ENCHANT_CANCEL) return false;
        if (stairs.warpPending() && action!=Action.TRAVEL) return false;
        Bundle before=null;
        if(action==Action.TRAVEL || action==Action.VAULT_REWARD) {
            if(cell<0 || cell!=stairs.stair() || !stairs.agreed()) return false;
            before=checkpoint();
        }
        try {
            executing = true;
            switch (action) {
                case SMITH_PICK: case SMITH_CASH: case SMITH_HARDEN: case SMITH_UPGRADE:
                case SMITH_REFORGE: case SMITH_ORDER: case SMITH_CLAIM:
                    if (!CoopSmith.apply(action,hero,item,cell)) return false;
                    break;
                case USE_MASK: case CLAIM_SUBCLASS:
                    if (!chooseSubclass(hero, activeSeat, item, cell, action==Action.USE_MASK)) return false;
                    break;
                case USE_CROWN: case CLAIM_ARMOR:
                    if (!chooseArmor(hero,item,cell,action==Action.USE_CROWN)) return false;
                    break;
                case TRINITY_MIND: case TRINITY_SPIRIT: {
                    if (!(item instanceof ClassArmor) || item!=hero.belongings.armor() || ownership.owner(item)!=player || !(hero.armorAbility instanceof Trinity)
                            || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)) return false;
                    Trinity trinity=(Trinity)hero.armorAbility;
                    if (action==Action.TRINITY_MIND ? !trinity.useMind(hero,(ClassArmor)item,cell) : cell!=hero.pos || !trinity.useSpirit(hero,(ClassArmor)item)) return false;
                    break;
                }
                case ARMOR_ABILITY: {
                    if (!(item instanceof ClassArmor) || hero.belongings.armor()!=item || ownership.owner(item)!=player
                            || !playableArmorAbility(hero.armorAbility)) return false;
                    ClassArmor armor=(ClassArmor)item;
                    WarpBeacon.WarpBeaconTracker beacon=hero.buff(WarpBeacon.WarpBeaconTracker.class);
                    if (hero.armorAbility instanceof WarpBeacon && beacon!=null && cell==WarpBeacon.RECALL
                            && (beacon.depth!=Dungeon.depth || beacon.branch!=Dungeon.branch))
                        return stairs.proposeWarp(activeSeat);
                    if (armor.charge < hero.armorAbility.chargeUse(hero)) return false;
                    if (hero.armorAbility instanceof Trinity) {
                        if (cell!=hero.pos || !((Trinity)hero.armorAbility).useBody(hero,armor)) return false;
                    } else if (hero.armorAbility instanceof Challenge) {
                        if (abilityTarget(hero,cell)==null || !((Challenge)hero.armorAbility).challenge(armor,hero,cell)) return false;
                        return true;
                    } else if (hero.armorAbility instanceof Feint) {
                        if (!level.insideMap(cell) || !knowledge(activeSeat).visible(cell) || !level.adjacent(hero.pos,cell)
                                || hero.rooted || level.solid[cell] || Actor.findChar(cell)!=null || !supportedLanding(cell)) return false;
                        ((Feint)hero.armorAbility).activate(armor,hero,cell);
                    } else if (hero.armorAbility instanceof ElementalStrike) {
                        if (!level.insideMap(cell) || !knowledge(activeSeat).visible(cell) || cell==hero.pos
                                || !(hero.belongings.weapon() instanceof MeleeWeapon)) return false;
                        Weapon.Enchantment ench=((MeleeWeapon)hero.belongings.weapon()).enchantment;
                        if (!ElementalStrike.supportsCoopEnchantment(ench)) return false;
                        ((ElementalStrike)hero.armorAbility).activate(armor,hero,cell);
                    } else if (hero.armorAbility instanceof PowerOfMany) {
                        if (!level.insideMap(cell) || !knowledge(activeSeat).visible(cell) || !supportedLanding(cell)) return false;
                        boolean command=PowerOfMany.getPoweredAlly(hero) instanceof PowerOfMany.LightAlly;
                        if (!((PowerOfMany)hero.armorAbility).empower(armor,hero,cell)) return false;
                        if (command) return true;
                    } else if (hero.armorAbility instanceof ShadowClone) {
                        boolean command = ShadowClone.getShadowAlly(hero) != null;
                        if (!((ShadowClone)hero.armorAbility).useForSeat(armor,hero,cell)) return false;
                        if (command) return true;
                    } else if (hero.armorAbility instanceof DeathMark) {
                        Char foe = abilityTarget(hero,cell);
                        if (foe == null) return false;
                        DeathMark.DeathMarkTracker mark = foe.buff(DeathMark.DeathMarkTracker.class);
                        if (mark != null && mark.caster() != hero) return false;
                        ((DeathMark)hero.armorAbility).activate(armor,hero,cell);
                        return true;
                    } else if (hero.armorAbility instanceof SmokeBomb) {
                        if (!((SmokeBomb)hero.armorAbility).useAt(armor,hero,cell)) return false;
                    } else if (hero.armorAbility instanceof SpiritHawk) {
                        boolean command=SpiritHawk.getHawk(hero)!=null;
                        if (!((SpiritHawk)hero.armorAbility).useForSeat(armor,hero,cell)) return false;
                        if (command) return true;
                    } else if (hero.armorAbility instanceof WildMagic) {
                        if (!level.insideMap(cell) || !knowledge(activeSeat).visible(cell)
                                || !((WildMagic)hero.armorAbility).useForSeat(armor,hero,cell)) return false;
                    } else if (hero.armorAbility instanceof WarpBeacon) {
                        boolean selecting = hero.buff(WarpBeacon.WarpBeaconTracker.class) != null;
                        if (cell==WarpBeacon.CLEAR && stairs.warpPending()) stairs.cancel();
                        if (!((WarpBeacon)hero.armorAbility).useForSeat(armor, hero, cell)) return false;
                        // Native local recall and removal cost no game time.
                        if (selecting) return true;
                    } else if (hero.armorAbility instanceof HeroicLeap) {
                        if (hero.rooted || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)
                                || !level.passable[cell] || !supportedLanding(cell)) return false;
                        int landing=HeroicLeap.landing(hero,cell);
                        if (!supportedLanding(landing) || !level.passable[landing] || !HeroicLeap.safeImpact(hero,landing)) return false;
                        ((HeroicLeap)hero.armorAbility).activate(armor,hero,cell);
                    } else if (hero.armorAbility instanceof SpectralBlades) {
                        if (abilityTarget(hero,cell) == null || !((SpectralBlades)hero.armorAbility).resolve(armor, hero, cell)) return false;
                    } else if (hero.armorAbility instanceof Shockwave) {
                        if (cell == hero.pos || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)) return false;
                        ((Shockwave)hero.armorAbility).activate(armor, hero, cell);
                    } else {
                        if (cell!=hero.pos) return false;
                        if (hero.armorAbility instanceof ElementalBlast &&
                                (hero.belongings.getItem(MagesStaff.class)==null || hero.belongings.getItem(MagesStaff.class).coopWand()==null
                                || hero.belongings.getItem(MagesStaff.class).wandClass()==WandOfBlastWave.class && !ElementalBlast.safeForceBlast(hero))) return false;
                        hero.armorAbility.use(armor,hero);
                    }
                    break;
                }
                case BERSERK: {
                    Berserk rage = hero.buff(Berserk.class);
                    if (hero.subClass != HeroSubClass.BERSERKER || rage == null || !rage.readyForSeat()
                            || cell != hero.pos || item != null) return false;
                    rage.doAction();
                    return true; // Native manual Berserk costs no time.
                }
                case CLOBBER: case SLAM: case PARRY: case CRUSH: case FURY: {
                    Combo combo = hero.buff(Combo.class);
                    if (hero.subClass != HeroSubClass.GLADIATOR || combo == null || item != null) return false;
                    Char foe = null;
                    if (action == Action.PARRY) {
                        if (cell != hero.pos) return false;
                    } else {
                        foe = abilityTarget(hero, cell);
                        if (foe == null) return false;
                    }
                    if (!combo.performForSeat(Combo.ComboMove.valueOf(action.name()), foe)) return false;
                    if (foe != null) {
                        lastOpponent[activeSeat] = (Mob)foe;
                        attackTurn[activeSeat] = turn;
                    }
                    break;
                }
                case SNIPER_SHOT: {
                    SnipersMark mark = hero.buff(SnipersMark.class);
                    SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
                    Char foe = abilityTarget(hero, cell);
                    if (hero.subClass != HeroSubClass.SNIPER || mark == null || bow == null || item != null
                            || ownership.owner(bow) != player || foe == null || !bow.shootInCoop(hero, foe, mark)) return false;
                    lastOpponent[activeSeat] = (Mob)foe;
                    attackTurn[activeSeat] = turn;
                    break;
                }
                case FREERUN: {
                    Momentum momentum=hero.buff(Momentum.class);
                    if (hero.subClass!=HeroSubClass.FREERUNNER || cell!=hero.pos || item!=null
                            || momentum==null || !momentum.canStart()) return false;
                    momentum.doAction();
                    return true;
                }
                case ASSASSINATE: {
                    Preparation prep=hero.buff(Preparation.class);
                    Char target=abilityTarget(hero,cell);
                    if (hero.subClass!=HeroSubClass.ASSASSIN || item!=null || prep==null || target==null
                            || !prep.strikeForSeat(hero,target)) return false;
                    lastOpponent[activeSeat]=(Mob)target;
                    attackTurn[activeSeat]=turn;
                    break;
                }
                case RECALL_INSCRIPTION: case MIND_FORM: case SPIRIT_FORM: case BODY_FORM: case HOLY_INTUITION: case CLEANSE: case BLESS: {
                    if (!(item instanceof HolyTome) || hero.heroClass!=HeroClass.CLERIC || item.cursed
                            || ownership.owner(item)!=player || !hero.belongings.contains(item)) return false;
                    HolyTome tome=(HolyTome)item;
                    ClericSpell spell=action==Action.CLEANSE ? Cleanse.INSTANCE : action==Action.RECALL_INSCRIPTION ? RecallInscription.INSTANCE : action==Action.MIND_FORM ? MindForm.INSTANCE : action==Action.SPIRIT_FORM ? SpiritForm.INSTANCE : action==Action.BODY_FORM ? BodyForm.INSTANCE : action==Action.BLESS ? BlessSpell.INSTANCE : HolyIntuition.INSTANCE;
                    int tier=spell==Cleanse.INSTANCE ? 3 : spell==HolyIntuition.INSTANCE ? 1 : spell==BlessSpell.INSTANCE || spell==RecallInscription.INSTANCE ? 2 : 4;
                    if (!ClericSpell.getSpellList(hero,tier).contains(spell) || !tome.canCast(hero,spell)) return false;
                    if (action == Action.CLEANSE) {
                        if (cell != hero.pos) return false;
                        spell.onCast(tome, hero);
                    } else if (action == Action.RECALL_INSCRIPTION && RecallInscription.hasUntargetedScroll(hero)) {
                        if (cell != hero.pos) return false;
                        // The native spell reads its anonymous scroll, charges the tome, and clears the imprint.
                        spell.onCast(tome, hero);
                    } else if (action==Action.BLESS) {
                        if (!level.insideMap(cell) || !knowledge(activeSeat).visible(cell)) return false;
                        Char target=Actor.findChar(cell);
                        if (target==null || target instanceof Hero && target!=hero || target!=hero && !basicMob(target)) return false;
                        BlessSpell.INSTANCE.onTargetSelected(tome,hero,cell);
                    } else if (spell == BodyForm.INSTANCE || spell == MindForm.INSTANCE || spell == SpiritForm.INSTANCE) {
                        if (!(hero.armorAbility instanceof Trinity)) return false;
                        Talent form = Talent.valueOf(action.name());
                        Item held = null;
                        if (cell > 0) {
                            int index = 0;
                            for (Item entry : hero.belongings) if (++index == cell) { held = entry; break; }
                            if (held == null || ownership.owner(held) != player) return false;
                        }
                        for (TrinityDiscoveries.Choice choice : TrinityDiscoveries.Choice.values()) {
                            if (choice.form != form) continue;
                            boolean valid = held != null ? choice.matches(held)
                                    : choice.target == cell && discoveries[activeSeat].knows(choice);
                            if (valid) {
                                ((Trinity)hero.armorAbility).selectEffect(hero, tome, choice, spell);
                                if (held != null) discoveries[activeSeat].witness(held);
                                return true;
                            }
                        }
                        return false;
                    } else {
                        Item reference=null;
                        int index=0;
                        for (Item entry : hero.belongings) if (++index==cell) { reference=entry; break; }
                        if (reference==null || ownership.owner(reference)!=player) return false;
                        if (action==Action.RECALL_INSCRIPTION) {
                            RecallInscription.UsedItemTracker imprint=hero.buff(RecallInscription.UsedItemTracker.class);
                            if (imprint==null || imprint.item!=ScrollOfIdentify.class || !equipmentForScroll(reference) || reference.isIdentified()) return false;
                            ScrollOfIdentify copy=new ScrollOfIdentify();
                            copy.anonymize();
                            copy.talentChance=0;
                            ScrollOfIdentify.IDItem(reference);
                            copy.finishReading(hero);
                            spell.onSpellCast(tome,hero);
                            imprint.detach();
                            requireSealFormat();
                            break;
                        }
                        if (!HolyIntuition.INSTANCE.usableOnItem(reference)) return false;
                        HolyIntuition.INSTANCE.onItemSelected(tome,hero,reference);
                    }
                    requireSealFormat();
                    break;
                }
                case HALLOWED_GROUND: case WALL_OF_LIGHT: {
                    boolean wall = action==Action.WALL_OF_LIGHT;
                    ClericSpell spell = wall ? WallOfLight.INSTANCE : HallowedGround.INSTANCE;
                    if (!(item instanceof HolyTome) || ownership.owner(item)!=player || !hero.belongings.contains(item)
                            || hero.subClass!=(wall ? HeroSubClass.PALADIN : HeroSubClass.PRIEST)
                            || !((HolyTome)item).canCast(hero,spell) || !level.insideMap(cell)
                            || !knowledge(activeSeat).visible(cell)) return false;
                    HolyTome tome = (HolyTome)item;
                    if (wall && spell.chargeUse(hero)==0) {
                        if (cell!=hero.pos) return false;
                        spell.onCast(tome,hero);
                        return true;
                    }
                    if (wall) {
                        if (!WallOfLight.INSTANCE.castWall(tome,hero,cell)) return false;
                    } else {
                        if (level.solid[cell]) return false;
                        HallowedGround.INSTANCE.onTargetSelected(tome,hero,cell);
                    }
                    break;
                }
                case MNEMONIC_PRAYER: {
                    if (!(item instanceof HolyTome) || ownership.owner(item)!=player || hero.subClass!=HeroSubClass.PRIEST
                            || !hero.belongings.contains(item) || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)
                            || !((HolyTome)item).canCast(hero,MnemonicPrayer.INSTANCE)) return false;
                    Char recipient=Actor.findChar(cell);
                    if (recipient!=hero && (recipient==null || !recipient.isAlive() || recipient.invisible>0
                            || !(summonerOf(recipient)==hero || abilityTarget(hero,cell)==recipient))) return false;
                    MnemonicPrayer.INSTANCE.onTargetSelected((HolyTome)item,hero,cell);
                    return true;
                }
                case LAY_ON_HANDS: case AURA_OF_PROTECTION: {
                    if (!(item instanceof HolyTome) || hero.subClass!=HeroSubClass.PALADIN || ownership.owner(item)!=player
                            || item.cursed || !hero.belongings.contains(item)) return false;
                    HolyTome tome=(HolyTome)item;
                    boolean targeted=action==Action.LAY_ON_HANDS;
                    ClericSpell spell=targeted ? LayOnHands.INSTANCE : AuraOfProtection.INSTANCE;
                    if (!ClericSpell.getSpellList(hero,3).contains(spell) || !tome.canCast(hero,spell)) return false;
                    if (targeted) {
                        if (!level.insideMap(cell) || !knowledge(activeSeat).visible(cell) || level.distance(hero.pos,cell)>1) return false;
                        Char recipient=Actor.findChar(cell);
                        if (recipient!=hero && (recipient==null || !recipient.isAlive() || recipient.invisible>0
                                || !(summonerOf(recipient)==hero || abilityTarget(hero,cell)==recipient))) return false;
                        LayOnHands.INSTANCE.onTargetSelected(tome,hero,cell);
                    } else {
                        if (cell!=hero.pos) return false;
                        spell.onCast(tome,hero);
                    }
                    break;
                }
                case STASIS: case BEAMING_RAY: case LIFE_LINK: case DIVINE_INTERVENTION: case JUDGEMENT: case FLASH: {
                    if (!(item instanceof HolyTome) || !hero.belongings.contains(item) || ownership.owner(item)!=player) return false;
                    HolyTome tome = (HolyTome)item;
                    ClericSpell spell;
                    switch (action) {
                        case BEAMING_RAY: spell = BeamingRay.INSTANCE; break;
                        case LIFE_LINK: spell = LifeLinkSpell.INSTANCE; break;
                        case STASIS: spell = Stasis.INSTANCE; break;
                        case FLASH: spell = Flash.INSTANCE; break;
                        case JUDGEMENT: spell = Judgement.INSTANCE; break;
                        default: spell = DivineIntervention.INSTANCE;
                    }
                    if (!tome.canCast(hero,spell) || !ClericSpell.getSpellList(hero,4).contains(spell)) return false;
                    if (action == Action.STASIS && hero.buff(Stasis.StasisBuff.class) != null) {
                        if (cell != hero.pos || !hero.buff(Stasis.StasisBuff.class).canRelease()) return false;
                        spell.onCast(tome, hero);
                        return true; // Native manual release spends neither charge nor time.
                    }
                    if (action == Action.BEAMING_RAY) {
                        if (!BeamingRay.INSTANCE.cast(tome,hero,cell)) return false;
                    } else if (action == Action.FLASH) {
                        if (!Flash.INSTANCE.cast(tome,hero,cell)) return false;
                    } else {
                        if (cell != hero.pos) return false;
                        spell.onCast(tome,hero);
                    }
                    break;
                }
                case HOLY_LANCE: case RADIANCE: case SMITE: case DIVINE_SENSE: case SUNRAY: case SHIELD_OF_LIGHT: case GUIDING_LIGHT: case HOLY_WEAPON: case HOLY_WARD: {
                    if (!(item instanceof HolyTome) || hero.heroClass!=HeroClass.CLERIC
                            || item.cursed || ownership.owner(item)!=player || !hero.belongings.contains(item)) return false;
                    HolyTome tome=(HolyTome)item;
                    int tier = (action==Action.HOLY_LANCE || action==Action.RADIANCE || action==Action.SMITE) ? 3 : action==Action.DIVINE_SENSE || action==Action.SUNRAY ? 2 : 1;
                    ClericSpell spell=action==Action.HOLY_LANCE ? HolyLance.INSTANCE : action==Action.RADIANCE ? Radiance.INSTANCE : action==Action.SMITE ? Smite.INSTANCE : action==Action.DIVINE_SENSE ? DivineSense.INSTANCE :
                            action==Action.SUNRAY ? Sunray.INSTANCE : action==Action.SHIELD_OF_LIGHT ? ShieldOfLight.INSTANCE :
                            action==Action.GUIDING_LIGHT ? GuidingLight.INSTANCE :
                            action==Action.HOLY_WEAPON ? HolyWeapon.INSTANCE : HolyWard.INSTANCE;
                    if (!tome.canCast(hero,spell) || !ClericSpell.getSpellList(hero,tier).contains(spell)) return false;
                    if (spell==Smite.INSTANCE) {
                        Char enemy=abilityTarget(hero,cell);
                        if (enemy==null || !hero.canAttack(enemy)) return false;
                        Smite.INSTANCE.onTargetSelected(tome,hero,cell);
                        break;
                    }
                    if (spell==Radiance.INSTANCE) {
                        if (cell!=hero.pos) return false;
                        spell.onCast(tome,hero);
                        break;
                    }
                    if (spell==ShieldOfLight.INSTANCE) {
                        if (abilityTarget(hero,cell)==null) return false;
                        ShieldOfLight.INSTANCE.onTargetSelected(tome,hero,cell);
                        return true;
                    }
                    if (spell==HolyLance.INSTANCE || spell==GuidingLight.INSTANCE || spell==Sunray.INSTANCE) {
                        if (cell==hero.pos || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)) return false;
                        Ballistica ray=new Ballistica(hero.pos,cell,spell.targetingFlags());
                        Char target=Actor.findChar(ray.collisionPos);
                        if (target!=null && (target.alignment!=Char.Alignment.ENEMY || target.invisible>0 || !basicMob(target))
                                || target==null && !supportedLanding(ray.collisionPos)) return false;
                        if (spell==HolyLance.INSTANCE) HolyLance.INSTANCE.onTargetSelected(tome,hero,cell);
                        else if (spell==Sunray.INSTANCE) Sunray.INSTANCE.onTargetSelected(tome,hero,cell);
                        else GuidingLight.INSTANCE.onTargetSelected(tome,hero,cell);
                        break;
                    }
                    if (cell!=hero.pos) return false;
                    spell.onCast(tome,hero);
                    return true; // The native holy weapon and ward spells are instantaneous.
                }
                case MONK_FLURRY: case MONK_FOCUS: case MONK_DASH: case MONK_DRAGONKICK: case MONK_MEDITATE: {
                    MonkEnergy energy=hero.buff(MonkEnergy.class);
                    if (hero.subClass!=HeroSubClass.MONK || item!=null || energy==null || energy.cooldown>0) return false;
                    MonkEnergy.MonkAbility ability=null;
                    for (MonkEnergy.MonkAbility candidate : MonkEnergy.MonkAbility.abilities)
                        if (candidate.seatAction().equals(action.name())) ability=candidate;
                    if (ability==null || !ability.usable(energy)) return false;
                    boolean empowered=energy.abilitiesEmpowered(hero);
                    if (action==Action.MONK_DASH) {
                        if (!level.insideMap(cell) || !knowledge(activeSeat).visible(cell) || hero.rooted || Actor.findChar(cell)!=null
                                || level.distance(hero.pos,cell)>(empowered ? 8 : 4) || level.solid[cell] || !supportedLanding(cell)
                                || new Ballistica(hero.pos,cell,Ballistica.PROJECTILE).collisionPos!=cell) return false;
                    } else if (action==Action.MONK_FLURRY || action==Action.MONK_DRAGONKICK) {
                        Char target=abilityTarget(hero,cell);
                        if (target==null || hero.isCharmedBy(target) || !level.adjacent(hero.pos,cell)) return false;
                        if (action==Action.MONK_DRAGONKICK && !MonkEnergy.MonkAbility.DragonKick.safeLandings(hero,target,empowered)) return false;
                    } else if (cell!=hero.pos) return false;
                    ability.doAbility(hero,cell);
                    if (action!=Action.MONK_FOCUS) requireSealFormat();
                    if (action==Action.MONK_DASH || action==Action.MONK_FLURRY || action==Action.MONK_FOCUS && empowered) return true;
                    break;
                }
                case EQUIP_SECOND: {
                    if (hero.subClass!=HeroSubClass.CHAMPION || item==null || !basicWeapon(item)
                            || !hero.belongings.backpack.contains(item) || ownership.owner(item)!=player
                            || hero.belongings.secondWep!=null && hero.belongings.secondWep.cursed) return false;
                    if (!((MeleeWeapon)item).equipSecondary(hero)) return false;
                    break;
                }
                case SWAP_WEAPONS: {
                    MeleeWeapon.Charger charger=hero.buff(MeleeWeapon.Charger.class);
                    return item==null && cell==hero.pos && charger!=null && charger.swapFor(hero);
                }
                case WEAPON_ABILITY: {
                    if (hero.heroClass!=HeroClass.DUELIST || item!=hero.belongings.weapon() || ownership.owner(item)!=player
                            || !supportedWeaponAbility(item)) return false;
                    MeleeWeapon weapon=(MeleeWeapon)item;
                    MeleeWeapon.Charger pool=hero.buff(MeleeWeapon.Charger.class);
                    if (pool==null || weapon.STRReq()>hero.STR() || pool.charges+pool.partialCharge<weapon.abilityChargeUse(hero,null)) return false;
                    if (weapon instanceof Rapier) {
                        if (!level.insideMap(cell) || !((Rapier)weapon).lungeForSeat(hero,cell)) return false;
                    } else {
                        if (cell!=hero.pos) return false;
                        if (weapon instanceof Crossbow) {
                            if (hero.buff(Crossbow.ChargedShot.class)!=null) return false;
                            ((Crossbow)weapon).duelistAbility(hero,cell);
                        } else if (weapon instanceof Quarterstaff) {
                            ((Quarterstaff)weapon).duelistAbility(hero,cell);
                            return true;
                        } else ((RoundShield)weapon).duelistAbility(hero,cell);
                    }
                    break;
                }
                case DETACH_SEAL: {
                    if (!(item instanceof Armor) || !hero.belongings.contains(item) || ownership.owner(item)!=player || cell!=0) return false;
                    Armor armor=(Armor)item;
                    BrokenSeal seal=armor.checkSeal();
                    if (seal==null || !ownership.allowed(seal,player) || !hero.belongings.backpack.canHold(seal) && !supportedLanding(hero.pos)) return false;
                    ownership.claim(seal,player);
                    armor.detachSeal(hero);
                    if (!seal.collect(hero.belongings.backpack)) level.drop(seal,hero.pos);
                    requireSealFormat();
                    return true;
                }
                case AFFIX_SEAL: case KEEP_ARMOR_GLYPH: case TRANSFER_SEAL_GLYPH: {
                    if (!(item instanceof BrokenSeal) || !hero.belongings.backpack.contains(item) || ownership.owner(item)!=player) return false;
                    BrokenSeal seal=(BrokenSeal)item;
                    int selection=0;
                    for (Item candidate : hero.belongings) {
                        if (++selection!=cell) continue;
                        if (!(candidate instanceof Armor) || ownership.owner(candidate)!=player) return false;
                        Armor armor=(Armor)candidate;
                        if (armor.checkSeal()!=null || !armor.cursedKnown || armor.cursed && (seal.getGlyph()==null || !seal.getGlyph().curse())) return false;
                        boolean conflict=armor.glyph!=null && seal.getGlyph()!=null && armor.glyph.getClass()!=seal.getGlyph().getClass();
                        if (conflict==(action==Action.AFFIX_SEAL)) return false;
                        if (seal.detachAll(hero.belongings.backpack)!=seal) throw new IllegalStateException("Missing seal");
                        if (action==Action.KEEP_ARMOR_GLYPH) seal.setGlyph(null);
                        armor.affixSeal(seal,hero);
                        requireSealFormat();
                        return true;
                    }
                    return false;
                }
                case IMBUE: {
                    if (!(item instanceof MagesStaff) || ownership.owner(item) != player
                            || !hero.belongings.contains(item)) return false;
                    int index = 0;
                    for (Item candidate : hero.belongings) {
                        if (++index != cell) continue;
                        if (!supportedHandheldWand(candidate) || ownership.owner(candidate) != player
                                || !hero.belongings.backpack.contains(candidate)) return false;
                        MagesStaff staff = (MagesStaff)item;
                        if (staff.coopWand() == null) return false;
                        ownership.claim(staff.imbuedWand(), player);
                        if (candidate.detachAll(hero.belongings.backpack) != candidate)
                            throw new IllegalStateException("Imbuement wand missing");
                        staff.imbueWand((Wand)candidate, hero);
                        wandIdentities = true;
                        if (staff.imbuedWand() instanceof WandOfFrost) requireGrowthFormat();
                        if (staff.imbuedWand() instanceof WandOfLightning) requireSealFormat();
                        return true; // The original confirmation spends no turn.
                    }
                    return false;
                }
                case INVEST_TALENT: {
                    if (cell < 0 || cell >= Talent.values().length || item != null) return false;
                    Talent talent = Talent.values()[cell];
                    int tier = 1;
                    while (tier <= hero.talents.size() && !hero.talents.get(tier-1).containsKey(talent)) tier++;
                    if (!offersTalent(hero, talent) || tier > hero.talents.size()
                            || hero.talentPointsAvailable(tier) <= 0 || hero.pointsInTalent(talent) >= talent.maxPoints()) return false;
                    hero.upgradeTalent(talent);
                    if (talent==Talent.RUNIC_TRANSFERENCE || talent==Talent.IMPROVISED_PROJECTILES || talent==Talent.HOLY_INTUITION
                            || talent==Talent.BLESS || talent==Talent.RECALL_INSCRIPTION || talent==Talent.MIND_FORM || talent==Talent.BODY_FORM || talent==Talent.SPIRIT_FORM) requireSealFormat();
                    if (talent == Talent.ARCANE_VISION || talent == Talent.HEIGHTENED_SENSES
                            || talent == Talent.DIVINE_SENSE || talent == Talent.SUNRAY || talent == Talent.SHIELD_OF_LIGHT)
                        requireGrowthFormat();
                    // The native talent button spends no game time. Retain the current offer.
                    return true;
                }
                case MINE: {
                    if (!(level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel)
                            || !level.insideMap(cell) || !level.adjacent(hero.pos, cell)
                            || !knowledge(activeSeat).visible(cell)) return false;
                    Item pick = hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe.class);
                    if (pick == null || ownership.owner(pick) != player) return false;
                    Char occupant = Actor.findChar(cell);
                    if (occupant instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalSpire
                            || (occupant instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer
                            && occupant.buff(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer.RockArmor.class) != null)) {
                        occupant.interact(hero);
                    } else {
                        if (occupant != null || !Hero.mineableTerrain(level.map[cell])) return false;
                        hero.checkVisibleMobs();
                        hero.curAction = new HeroAction.Mine(cell);
                        hero.act();
                    }
                    break;
                }
                case SEARCH:
                    if(cell!=hero.pos) return false;
                    hero.search(true);
                    break;
                case VAULT_REWARD: case TRAVEL:
                    if (!stairs.travel(activeSeat,cell,item)) return false;
                    break;
                case VAULT_RECALL:
                    if (!(level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel)
                            || !(item instanceof VaultBeacon) || !hero.belongings.backpack.contains(item)
                            || ownership.owner(item)!=player || !supportedLanding(level.entrance())
                            || !((VaultBeacon)item).returnToEntrance(hero)) return false;
                    break;
                case VAULT_INTERACT: {
                    if (!(level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel)
                            || !level.insideMap(cell) || !level.adjacent(hero.pos,cell) || !knowledge(activeSeat).visible(cell)) return false;
                    Char target=Actor.findChar(cell);
                    if (target instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor) {
                        if (!((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor)target).unlock(hero)) return false;
                    } else if (target instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror) {
                        if (!((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror)target).takeReward(hero)) return false;
                    } else return false;
                    break;
                }
                case ACCEPT_IMP:
                    if (Dungeon.branch!=0 || !level.insideMap(cell) || !level.adjacent(hero.pos,cell) || !knowledge(activeSeat).visible(cell)
                            || !(Actor.findChar(cell) instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp)
                            || !com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.acceptCurrent()) return false;
                    break;
                case ACCEPT_BLACKSMITH:
                    if (!level.insideMap(cell) || !level.adjacent(hero.pos,cell) || !knowledge(activeSeat).visible(cell)
                            || !(Actor.findChar(cell) instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith)
                            || !com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.acceptFor(hero)) return false;
                    break;
                case ACCEPT_WANDMAKER:
                case WAND_FIRST:
                case WAND_SECOND:
                    if (!level.insideMap(cell) || !level.adjacent(hero.pos, cell) || !knowledge(activeSeat).visible(cell)
                            || !(Actor.findChar(cell) instanceof Wandmaker)) return false;
                    if (action == Action.ACCEPT_WANDMAKER) {
                        if (!Wandmaker.Quest.accept()) return false;
                    } else if (!Wandmaker.Quest.claim((Wandmaker)Actor.findChar(cell), hero, action == Action.WAND_SECOND)) return false;
                    break;
                case ACCEPT_GHOST:
                case GHOST_WEAPON:
                case GHOST_ARMOR:
                    if (Dungeon.depth < 2 || Dungeon.depth > 4 || !level.insideMap(cell) || !level.adjacent(hero.pos,cell)
                            || !knowledge(activeSeat).visible(cell) || !(Actor.findChar(cell) instanceof Ghost)) return false;
                    Ghost ghost = (Ghost)Actor.findChar(cell);
                    if (action == Action.ACCEPT_GHOST) {
                        if (!Ghost.Quest.accept(ghost)) return false;
                    } else if (!Ghost.Quest.claim(ghost,hero,action == Action.GHOST_ARMOR)) return false;
                    break;
                case WAIT:
                    hero.rest(false);
                    break;
                case MOVE:
                    if (cell < 0 || cell >= level.length() || !level.adjacent(hero.pos, cell)
                            || !safeFloor(cell,true) || Actor.findChar(cell) != null || hero.rooted) return false;
                    Heap destination = level.heaps.get(cell);
                    if (destination != null && destination.type != Heap.Type.HEAP && destination.type != Heap.Type.FOR_SALE) return false;
                    hero.checkVisibleMobs();
                    boolean unsteady = hero.buff(Vertigo.class) != null;
                    boolean teleport = (level.traps.get(cell) instanceof com.shatteredpixel.shatteredpixeldungeon.levels.traps.TeleportationTrap
                            || level.traps.get(cell) instanceof com.shatteredpixel.shatteredpixeldungeon.levels.traps.GatewayTrap)
                            && level.traps.get(cell).active;
                    teleport |= level.plants.get(cell) instanceof com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf;
                    hero.handle(cell);
                    if (hero.curAction instanceof HeroAction.PickUp || hero.curAction instanceof HeroAction.Buy) hero.curAction = new HeroAction.Move(cell);
                    if (!(hero.curAction instanceof HeroAction.Move)) throw new IllegalStateException("Unexpected move action");
                    hero.act();
                    if (!unsteady && !teleport && hero.pos != cell) throw new IllegalStateException("Move was interrupted");
                    break;
                case ALLY_WARP: {
                    if (item != null || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)) return false;
                    Char ally = Actor.findChar(cell);
                    if (!canWarpAlly(hero, ally)) return false;
                    ally.interact(hero);
                    return true;
                }
                case GREET:
                    if (!level.insideMap(cell) || !level.adjacent(hero.pos,cell) || !knowledge(activeSeat).visible(cell)
                            || !(Actor.findChar(cell) instanceof Sheep)) return false;
                    Actor.findChar(cell).interact(hero);
                    break;
                case UNLOCK:
                    if (!level.insideMap(cell) || !level.adjacent(hero.pos,cell) || !knowledge(activeSeat).visible(cell)
                            || Dungeon.branch!=0) return false;
                    boolean bossExit=level.map[cell]==Terrain.LOCKED_EXIT && Dungeon.depth==5 && !level.locked;
                    if ((!bossExit && level.map[cell]!=Terrain.LOCKED_DOOR)
                            || Notes.keyCount(bossExit ? new WornKey(Dungeon.depth) : new IronKey(Dungeon.depth))<1) return false;
                    hero.curAction = new HeroAction.Unlock(cell);
                    hero.act();
                    if (level.map[cell]!=(bossExit ? Terrain.UNLOCKED_EXIT : Terrain.DOOR)) throw new IllegalStateException("Door remained locked");
                    break;
                case OPEN:
                    if (!level.insideMap(cell) || !level.adjacent(hero.pos,cell) || !knowledge(activeSeat).visible(cell)) return false;
                    Char occupant=Actor.findChar(cell);
                    if (disguised(occupant)) {
                        occupant.interact(hero);
                    } else {
                        Heap chest=level.heaps.get(cell);
                        if (occupant!=null || chest==null || chest.hidden || ((Dungeon.depth<7 || chest.type!=Heap.Type.SKELETON) && (chest.haunted || chest.type!=Heap.Type.CHEST))) return false;
                        hero.checkVisibleMobs();
                        hero.curAction=new HeroAction.OpenChest(cell);
                        hero.act();
                        if (chest.type!=Heap.Type.HEAP) throw new IllegalStateException("Chest did not open");
                    }
                    break;
                case ATTACK:
                    if (cell < 0 || cell >= level.length()) return false;
                    Char enemy = Actor.findChar(cell);
                    if (!(enemy instanceof Mob) || !basicMob(enemy) || enemy.alignment != Char.Alignment.ENEMY
                            || !enemy.isAlive() || enemy.invisible > 0 || !hero.fieldOfView[cell]
                            || hero.isCharmedBy(enemy)) return false;
                    if (!level.adjacent(hero.pos, cell) && (disintegrationReach(hero) <= 1
                            || !knowledge(activeSeat).visible(cell) || !hero.canAttack(enemy))) return false;
                    hero.checkVisibleMobs();
                    if (!hero.handle(cell) || !(hero.curAction instanceof HeroAction.Attack)) throw new IllegalStateException("Unexpected attack action");
                    lastOpponent[activeSeat] = (Mob)enemy;
                    attackTurn[activeSeat] = turn;
                    hero.act();
                    break;
                case ZAP:
                    if (cell<0 || cell>=level.length() || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)) return false;
                    TargetedDamageWand wand=wandFor(hero,item);
                    Char victim=Actor.findChar(cell);
                    Heap targetHeap=level.heaps.get(cell);
                    boolean chestTarget=victim==null && targetHeap!=null && !targetHeap.hidden
                            && (targetHeap.type==Heap.Type.CHEST || targetHeap.type==Heap.Type.CRYSTAL_CHEST || targetHeap.type==Heap.Type.LOCKED_CHEST)
                            && level.traps.get(cell)==null && level.plants.get(cell)==null && !level.pit[cell];
                    if (wand==null || level.distance(hero.pos,cell)>hero.viewDistance) return false;
                    if (cell == hero.pos) {
                        if (ownership.owner(item) != player || !wand.useShieldBattery(hero)) return false;
                        break;
                    }
                    Ballistica bolt=new Ballistica(hero.pos,cell,wand.collisionProperties(cell));
                    boolean eternalFire = wand instanceof WandOfFrost && victim == null
                            && bolt.path.size() > bolt.dist+1 && bolt.path.get(bolt.dist+1) == cell
                            && com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.volumeAt(cell,
                                    com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom.EternalFire.class) > 0;
                    boolean rayWand = wand instanceof WandOfDisintegration;
                    if (rayWand && !((WandOfDisintegration)wand).safeBeam(hero,bolt,false)) return false;
                    boolean groundTarget = rayWand && victim==null || (wand instanceof WandOfPrismaticLight || wand instanceof WandOfBlastWave || wand instanceof WandOfFireblast || wand instanceof WandOfFrost || wand instanceof WandOfLightning) && victim == null && !level.solid[cell];
                    if (!eternalFire && !groundTarget && !chestTarget && (!(victim instanceof Mob) || !basicMob(victim) || !victim.isAlive()
                            || (!disguised(victim) && victim.alignment!=Char.Alignment.ENEMY)
                            || victim.invisible>0 || hero.isCharmedBy(victim))) return false;
                    // Fireblast aims its cone along a WONT_STOP ray. Its own spread checks each obstruction.
                    if (!rayWand && !(wand instanceof WandOfFireblast) && bolt.collisionPos != cell && !eternalFire) return false;
                    if (wand instanceof WandOfFrost && !WandOfFrost.safeImpact(bolt.collisionPos)) return false;
                    if (!wand.cast(hero,bolt)) return false;
                    lastOpponent[activeSeat]=(Mob)victim;
                    attackTurn[activeSeat]=turn;
                    break;
                case PICK_UP:
                    Heap heap = level.heaps.get(hero.pos);
                    if (cell != hero.pos || heap == null || heap.hidden || heap.type != Heap.Type.HEAP || item == null || heap.peek() != item
                            || (item instanceof Dewdrop && !Dewdrop.canCollect(hero,cell))
                            || (!(item instanceof Dewdrop) && !(item instanceof BrokenSeal && ownership.owner(item)==player) && !CoopVault.questPickup(item,cell) && item.getClass()!=Gold.class && item.getClass()!=IronKey.class && item.getClass()!=WornKey.class && !(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe && ownership.owner(item)==player) && item.getClass()!=GooBlob.class && item.getClass()!=com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard.class && item.getClass()!=com.shatteredpixel.shatteredpixeldungeon.items.TengusMask.class && !(Dungeon.depth==26 && item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.Amulet.class && !Statistics.amuletObtained) && item.getClass()!=com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown.class && !supportedWandType(item) && !(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand && ownership.owner(item)==player) && !(item instanceof CorpseDust || item instanceof Embers || item instanceof CeremonialCandle || item instanceof Rotberry.Seed) && !(item instanceof Potion) && !(item instanceof Ring || item instanceof StoneOfEnchantment || item instanceof HornOfPlenty) && !supportedScroll(item) && !Scroll.ScrollToStone.isIngredient(item) && !Scroll.ScrollToStone.isResult(item) && !basicMissile(item) && !basicArmorType(item) && !statueWeapon(item) && !basicFood(item) && !(item instanceof Honeypot || item instanceof Honeypot.ShatteredPot) && item.getClass() != Ankh.class && !basicWeapon(item))
                            || (!(item instanceof Dewdrop) && item.getClass()!=Gold.class && item.getClass()!=IronKey.class && item.getClass()!=WornKey.class && !(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe && ownership.owner(item)==player) && !hero.belongings.backpack.canHold(item))) return false;
                    hero.checkVisibleMobs();
                    if (!hero.handle(hero.pos) || !(hero.curAction instanceof HeroAction.PickUp)) throw new IllegalStateException("Unexpected pickup action");
                    hero.act();
                    break;
                case BUY: case SELL: case SELL_ONE: case BUYBACK:
                    if (!CoopTrade.execute(this,hero,action,cell,item)) return false;
                    break;
                case BREW_RUNESTONES:
                    return CoopAlchemy.brew(this, hero, cell, item);
                case READ_SCROLL:
                    if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment) {
                        enchantScroll[activeSeat] = CoopEnchantScroll.begin(this, hero, item);
                        if (enchantScroll[activeSeat] == null) return false;
                        if (hero.belongings.armor() != null && hero.belongings.armor().checkSeal() != null)
                            requireSealFormat();
                        return true;
                    }
                    if (!(item instanceof Scroll) || ((Scroll)item).isKnown()) return false;
                    // An unknown scroll exposes only READ_SCROLL. Its host-side class selects the effect.
                case READ_LULLABY:
                case READ_RECHARGING:
                    if (cell != 0 || !(item instanceof ScrollOfLullaby || item instanceof ScrollOfRecharging)
                            || action != Action.READ_SCROLL && (!((Scroll)item).isKnown()
                            || (action == Action.READ_LULLABY) != (item instanceof ScrollOfLullaby))
                            || ownership.owner(item) != player || !hero.belongings.backpack.contains(item)
                            || hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class) != null
                            || hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class) != null) return false;
                    com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook.bookRecharge spellbook = hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook.bookRecharge.class);
                    if (spellbook != null && spellbook.isCursed()) return false;
                    if (item instanceof ScrollOfLullaby) ((ScrollOfLullaby)item).readFor(hero);
                    else ((ScrollOfRecharging)item).readFor(hero);
                    break;
                case ENCHANT_TARGET:
                    return pending != null && pending.select(this, hero, item);
                case ENCHANT_CHOOSE:
                    if (pending == null || !pending.apply(this, hero, cell)) return false;
                    enchantScroll[activeSeat] = null;
                    break;
                case ENCHANT_CANCEL:
                    if (pending == null) return false;
                    enchantScroll[activeSeat] = null;
                    if (!pending.chargesTimeOnCancel()) return true;
                    hero.spendAndNext(Actor.TICK);
                    break;
                case ENCHANT_BOW: {
                    if (!(item instanceof StoneOfEnchantment) || !hero.belongings.backpack.contains(item)
                            || ownership.owner(item)!=player || hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class)!=null) return false;
                    SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
                    if (bow==null || ownership.owner(bow)!=player || !hero.belongings.backpack.contains(bow)) return false;
                    int index=0;
                    for (Item owned : hero.belongings) {
                        ++index;
                        if (owned==bow) break;
                    }
                    if (cell!=index) return false;
                    ((StoneOfEnchantment)item).useOnBow(hero,bow);
                    break;
                }
                case IDENTIFY:
                case UPGRADE:
                    if (!readEquipmentScroll(hero,action,item,cell)) return false;
                    break;
                case DRINK:
                    if (!(item instanceof PotionOfHealing) || !((Potion)item).isKnown()
                            || !hero.belongings.backpack.contains(item)) return false;
                    item.execute(hero, Potion.AC_DRINK);
                    break;
                case THROW:
                    if (basicFood(item)) {
                        if (ownership.owner(item)!=player || !hero.belongings.backpack.contains(item) || !level.insideMap(cell)
                                || !knowledge(activeSeat).visible(cell) || cell==hero.pos) return false;
                        int landing=item.throwPos(hero,cell);
                        if (!level.insideMap(landing) || !knowledge(activeSeat).visible(landing) || !supportedLanding(landing)) return false;
                        item.finishThrow(hero,landing,Actor.findChar(landing),item.castDelay(hero,landing));
                        requireSealFormat();
                        break;
                    }
                    if (!basicMissile(item) || !missileLandingAllowed(hero,cell)) return false;
                    int impact=((com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)item).throwInCoop(hero,cell);
                    if (impact<0) return false;
                    break;
                case SHOOT:
                    if (!(item instanceof SpiritBow) || hero.heroClass!=HeroClass.HUNTRESS
                            || !hero.belongings.backpack.contains(item) || !level.insideMap(cell)
                            || !knowledge(activeSeat).visible(cell)) return false;
                    Char mark=Actor.findChar(cell);
                    if (mark == null) {
                        if (!((SpiritBow)item).scoutInCoop(hero,cell)) return false;
                        break;
                    }
                    if (!(mark instanceof Mob) || !basicMob(mark) || !mark.isAlive() || mark.invisible>0
                            || mark.alignment!=Char.Alignment.ENEMY || hero.isCharmedBy(mark)
                            || !((SpiritBow)item).shootInCoop(hero,mark)) return false;
                    lastOpponent[activeSeat]=(Mob)mark;
                    attackTurn[activeSeat]=turn;
                    break;
                case SHATTER:
                    if (!(item instanceof Honeypot) || cell!=hero.pos || !hero.belongings.backpack.contains(item)) return false;
                    // Original shatter needs one empty cardinal neighbour when the hero occupies the cell.
                    boolean space=false;
                    for (int off:PathFinder.NEIGHBOURS4)
                        if (!level.solid[cell+off] && Actor.findChar(cell+off)==null) space=true;
                    if (!space) return false;
                    Item shell=((Honeypot)item).shatter(hero,cell);
                    if (!(shell instanceof Honeypot.ShatteredPot)) throw new IllegalStateException("Bee spawn failed");
                    item.detach(hero.belongings.backpack);
                    ownership.claim(shell,player);
                    if (!shell.collect(hero.belongings.backpack)) {
                        level.drop(shell,cell);
                        ((Honeypot.ShatteredPot)shell).dropPot(hero,cell);
                    }
                    hero.next();
                    break;
                case EAT:
                    if (item instanceof HornOfPlenty) {
                        HornOfPlenty horn=(HornOfPlenty)item;
                        if (cell!=0 || ownership.owner(horn)!=player || !horn.isEquipped(hero) || horn.charges()==0
                                || hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class)!=null) return false;
                        horn.execute(hero,HornOfPlenty.AC_EAT);
                        break;
                    }
                    if (!basicFood(item) || !hero.belongings.backpack.contains(item)) return false;
                    item.execute(hero, Food.AC_EAT);
                    break;
                case CLOAK: {
                    if (!(item instanceof CloakOfShadows) || ownership.owner(item) != player || cell != 0) return false;
                    CloakOfShadows cloak = (CloakOfShadows)item;
                    if (!cloak.actions(hero).contains(CloakOfShadows.AC_STEALTH)) return false;
                    boolean cancelling = cloak.stealthed();
                    cloak.execute(hero, CloakOfShadows.AC_STEALTH);
                    if (cancelling) return true;
                    break;
                }
                case EQUIP:
                    if (item instanceof RingOfHaste) {
                        if (!item.isIdentified() || ownership.owner(item) != player || !hero.belongings.backpack.contains(item)
                                || hero.belongings.ring != null && hero.belongings.misc != null
                                || !((Ring)item).doEquip(hero)) return false;
                        break;
                    }
                    if (item instanceof CloakOfShadows || item instanceof HolyTome || item instanceof HornOfPlenty) {
                        if (ownership.owner(item) != player || !hero.belongings.backpack.contains(item)
                                || hero.belongings.artifact != null && hero.belongings.misc != null
                                || !((com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact)item).doEquip(hero)) return false;
                        break;
                    }
                    if (basicArmor(item) || item instanceof ClassArmor && hero.armorAbility!=null && ownership.owner(item)==player) {
                        Armor old=hero.belongings.armor;
                        // A seal transfer normally opens a selection dialog. Explicit unequip keeps it with its armor.
                        if (!hero.belongings.backpack.contains(item) || (old!=null && (old.cursed || old.checkSeal()!=null))) return false;
                        if (!((Armor)item).doEquip(hero)) return false;
                        break;
                    }
                    if (!basicWeapon(item) || !hero.belongings.backpack.contains(item)
                            || (hero.belongings.weapon != null && hero.belongings.weapon.cursed)) return false;
                    if (!((MeleeWeapon)item).doEquip(hero)) return false;
                    break;
                case REVIVE:
                    if (!revive(hero, cell)) return false;
                    break;
                case UNEQUIP:
                    if (item instanceof RingOfHaste) {
                        if (ownership.owner(item) != player || !item.isEquipped(hero) || item.cursed
                                || !hero.belongings.backpack.canHold(item) || !((Ring)item).doUnequip(hero,true)) return false;
                        break;
                    }
                    if (item instanceof CloakOfShadows || item instanceof HolyTome || item instanceof HornOfPlenty) {
                        if (ownership.owner(item) != player || !item.isEquipped(hero) || item.cursed
                                || !hero.belongings.backpack.canHold(item) || !((com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact)item).doUnequip(hero,true)) return false;
                        break;
                    }
                    if (item!=null && item==hero.belongings.armor && (basicArmor(item) || item instanceof ClassArmor && ownership.owner(item)==player)) {
                        if (item.cursed || !hero.belongings.backpack.canHold(item)
                                || !hero.belongings.armor.doUnequip(hero,true)) return false;
                        break;
                    }
                    if (item != hero.belongings.weapon || !basicWeapon(item) || !hero.belongings.backpack.canHold(item)) return false;
                    if (!((MeleeWeapon)item).doUnequip(hero, true)) return false;
                    break;
                case DROP:
                    Heap floor = level.heaps.get(hero.pos);
                    if (cell != hero.pos || (floor != null && floor.type != Heap.Type.HEAP)
                            || level.pit[cell] || !level.passable[cell] || !hero.belongings.backpack.contains(item)
                            || !(item instanceof CeremonialCandle || basicFood(item) || basicArmorType(item) || basicWeapon(item) || item instanceof Honeypot || item instanceof Honeypot.ShatteredPot)) return false;
                    item.dropCoopUnit(hero);
                    break;
                default:
                    return false;
            }
            stairs.cancel();
            hero.curAction = null;
            if (!stopped() && !partyDefeated && !won()) advance();
            return true;
        } catch (CoopFloors.UnsupportedDestination unsupported) {
            // Only a known destination admission failure can use this pre-action checkpoint.
            // Any failure rebuilding the original graph remains a terminal engine fault.
            restoring=true;
            try {
                Actor.clear();
                Dungeon.level=null;
                Dungeon.hero=null;
                ownership.clear(); floors.clear(); leaveFloor();
                load(before,before.getBundle("world"));
                validateActorGraph();
                restoredAfterTravel=true;
                destinationBlocker=unsupported.getMessage();
                return false;
            } catch (RuntimeException invalid) {
                stop(invalid.toString());
                throw invalid;
            } finally { restoring=false; }
        } catch (RuntimeException failure) {
            stop(failure.toString());
            throw failure;
        } finally {
            executing = false;
        }
    }

    private void advance() {
        activeSeat = -1;
        Actor.process();
        if (!stopped()) for (int seat = 0; seat < heroes.length; seat++) observe(seat);
        if (!partyDefeated && !stopped() && activeSeat < 0) throw new IllegalStateException("Scheduler ended without an input turn");
    }

    /** Only native actor graphs reachable from the two heroes and the current floor are checkpointable. */
    public Bundle checkpoint() {
        checkExecutor();
        if (executing || restoring || stopped() || heroes[1]==null || (!partyDefeated && activeSeat<0))
            throw new IllegalStateException("No committed engine boundary");
        validateActorGraph();
        Bundle b = new Bundle();
        int format = stairs.schema();
        boolean rogue = heroes[0].heroClass == HeroClass.ROGUE || heroes[1].heroClass == HeroClass.ROGUE;
        b.put("schema", sealIdentities ? 21 : extendedGrowth ? 20 : wandIdentities ? 19 : rogue ? 18 : crownTrial != MaskTrial.OFF ? 17 : maskTrial == MaskTrial.OFF ? format : 16);
        if ((heroes[0].heroClass == HeroClass.CLERIC || heroes[1].heroClass == HeroClass.CLERIC)
                && (!discoveries[0].isEmpty() || !discoveries[1].isEmpty())) {
            b.put("growth_schema", b.getInt("schema"));
            b.put("schema", 22);
            for (int seat = 0; seat < heroes.length; seat++) discoveries[seat].save(b, "discovered" + seat);
        }
        for (int seat = 0; seat < heroes.length; seat++) {
            if (lives[seat] == Life.ALIVE && heroes[seat].HP == 0) {
                b.put("base_schema", b.getInt("schema"));
                b.put("schema", 23);
                break;
            }
        }
        if (enchantScroll[0] != null || enchantScroll[1] != null) {
            b.put("scroll_schema", b.getInt("schema"));
            b.put("schema", 24);
            for (int seat = 0; seat < heroes.length; seat++) if (enchantScroll[seat] != null) {
                Bundle choice = new Bundle();
                enchantScroll[seat].save(choice);
                b.put("enchant" + seat, choice);
            }
        }
        if (Stasis.getStasisAlly(heroes[0]) != null || Stasis.getStasisAlly(heroes[1]) != null) {
            b.put("stasis_schema", b.getInt("schema"));
            b.put("schema", 25);
        }
        if (rogue || wandIdentities) b.put("floor_schema", format);
        if (crownTrial != MaskTrial.OFF) {
            b.put("floor_schema",format);
            b.put("crown_trial",crownTrial.name());
            b.put("crown_user",crownUser);
            b.put("crown_rights",crownRights);
        }
        if (maskTrial != MaskTrial.OFF) {
            b.put("floor_schema",format);
            b.put("mask_trial",maskTrial.name());
            b.put("mask_user",maskUser);
            b.put("mask_rights",maskRights);
        }
        stairs.save(b);
        stairs.saveWarp(b);
        if (corrosionFormat) {
            b.put("corrosion_schema", b.getInt("schema"));
            b.put("schema", 27);
        }
        b.put("world",Dungeon.worldState());
        b.put("documents",Document.coopState());
        b.put("guest",heroes[1]);
        b.put("floor",Dungeon.level);
        b.put("players",players);
        b.put("turn",turn);
        b.put("active",activeSeat);
        b.put("defeat",partyDefeated);
        if (format>=14) b.put("ending",ending.name());
        b.put("schedule",Actor.schedulerState());
        b.put("random",Random.worldState());
        for (int i=0;i<2;i++) {
            b.put("life"+i,lives[i].name());
            b.put("before"+i,preDeath[i]);
            b.put("sight"+i,knowledge(i).save());
            b.put("range"+i,heroes[i].viewDistance);
            if (lastOpponent[i]!=null && Dungeon.level.mobs.contains(lastOpponent[i])) {
                b.put("enemy"+i,lastOpponent[i].id());
                b.put("hit"+i,attackTurn[i]);
            }
        }
        b.put("items",itemState(physicalItems(wandIdentities || stairs.schema()>=12)));
        b.put("nextItem",ownership.sequence());
        return b;
    }

    /** Original revive-persistent buffs continue ticking while their ghoul is outside Level.mobs. */
    public static java.util.List<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> linkedBodyBuffs(Level floor) {
        java.util.List<com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> result=new java.util.ArrayList<>();
        for (Mob resident:floor.mobs)
            for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul.GhoulLifeLink link:resident.buffs(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul.GhoulLifeLink.class))
                result.addAll(link.downedGhoul().buffs());
        return result;
    }

    void validateActorGraph() {
        HashSet<Actor> persistent = new HashSet<>();
        persistent.add(heroes[0]); persistent.add(heroes[1]);
        HashSet<Integer> summoners=new HashSet<>();
        for (Mob mob:Dungeon.level.mobs) {
            if (mob instanceof SmokeBomb.NinjaLog && !(((SmokeBomb.NinjaLog)mob).owner().armorAbility instanceof SmokeBomb))
                throw new IllegalArgumentException("Invalid smoke bomb summoner");
            if (mob instanceof Feint.AfterImage && !(((Feint.AfterImage)mob).owner().armorAbility instanceof Feint))
                throw new IllegalStateException("Invalid afterimage owner");
            if (mob instanceof PowerOfMany.LightAlly) {
                Hero owner=((PowerOfMany.LightAlly)mob).commander();
                if (!(owner.armorAbility instanceof PowerOfMany) || mob.buff(PowerOfMany.PowerBuff.class)!=null && PowerOfMany.getPoweredAlly(owner)!=mob
                        || !summoners.add(owner==heroes[0] ? 0 : 1)) throw new IllegalStateException("Invalid light ally owner");
            }
        }
        for (Mob mob:Dungeon.level.mobs) if (mob instanceof SpiritHawk.HawkAlly) {
            SpiritHawk.HawkAlly hawk=(SpiritHawk.HawkAlly)mob;
            int seat=hawk.belongsTo(heroes[0]) ? 0 : 1;
            if (!hawk.belongsTo(heroes[seat]) || !(heroes[seat].armorAbility instanceof SpiritHawk) || !summoners.add(seat))
                throw new IllegalStateException("Invalid hawk owner");
        }
        for (Mob mob:Dungeon.level.mobs) if (mob instanceof ShadowClone.ShadowAlly) {
            ShadowClone.ShadowAlly clone=(ShadowClone.ShadowAlly)mob;
            int seat=clone.belongsTo(heroes[0]) ? 0 : 1;
            if (!clone.belongsTo(heroes[seat]) || !(heroes[seat].armorAbility instanceof ShadowClone) || !summoners.add(seat))
                throw new IllegalStateException("Invalid shadow clone owner");
        }
        HashSet<Integer> heldIds = new HashSet<>();
        for (int seat = 0; seat < heroes.length; seat++) {
            Hero owner = heroes[seat];
            if (owner.buff(Stasis.StasisBuff.class) == null) continue;
            Mob held = Stasis.getStasisAlly(owner);
            if (!(held instanceof PowerOfMany.LightAlly) || !((PowerOfMany.LightAlly)held).belongsTo(owner)
                    || !(owner.armorAbility instanceof PowerOfMany) || !owner.hasTalent(Talent.STASIS)
                    || held.HP <= 0 || held.buff(PowerOfMany.PowerBuff.class) == null
                    || !held.buff(PowerOfMany.PowerBuff.class).belongsTo(owner) || !summoners.add(seat))
                throw new IllegalStateException("Invalid held ally");
            checkHeldId(held, heldIds);
            for (Buff buff : held.buffs()) checkHeldId(buff, heldIds);
        }
        persistent.addAll(Dungeon.level.mobs);
        for (Actor ch : new HashSet<>(persistent)) persistent.addAll(((Char)ch).buffs());
        persistent.addAll(Dungeon.level.blobs.values());
        persistent.addAll(linkedBodyBuffs(Dungeon.level));
        if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel)
            persistent.addAll(((com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel)Dungeon.level).pendingGas());
        Actor spawner = Dungeon.level.respawner();
        if (spawner!=null) persistent.add(spawner);
        if (!persistent.equals(Actor.all())) {
            java.util.Set<Actor> extra=new HashSet<>(Actor.all()); extra.removeAll(persistent);
            java.util.Set<Actor> missing=new HashSet<>(persistent); missing.removeAll(Actor.all());
            throw new IllegalStateException("Transient actor at checkpoint: extra="+extra+", missing="+missing);
        }
    }

    private static void checkHeldId(Actor actor, HashSet<Integer> heldIds) {
        if (Actor.findById(actor.id()) != null || !heldIds.add(actor.id()))
            throw new IllegalStateException("Held actor identity collision");
    }

    /** Reconcile a bee with its individual shell when its native floor becomes active again. */
    void restoreBeeHomes() {
        Map<Integer,Bee> residents=new java.util.HashMap<>();
        for (Mob m:Dungeon.level.mobs) if (m instanceof Bee && m.alignment==Char.Alignment.ENEMY)
            residents.put(m.id(),(Bee)m);
        if (residents.isEmpty()) return;
        for (Hero hero:heroes) for (Item item:hero.belongings) linkShell(residents,item,-1,hero);
        for (Heap heap:Dungeon.level.heaps.valueList()) for (Item item:heap.items) linkShell(residents,item,heap.pos,null);
        for (Mob mob:Dungeon.level.mobs) {
            if (mob instanceof Thief) linkShell(residents,((Thief)mob).item,-1,mob);
            if (mob instanceof Mimic && ((Mimic)mob).items!=null)
                for (Item item:((Mimic)mob).items) linkShell(residents,item,-1,mob);
        }
        for (Bee bee:residents.values()) bee.setPotInfo(-1,null);
    }

    private static void linkShell(Map<Integer,Bee> residents,Item item,int pos,Char holder) {
        if (!(item instanceof Honeypot.ShatteredPot)) return;
        Bee bee=residents.remove(((Honeypot.ShatteredPot)item).beeId());
        if (bee!=null) bee.setPotInfo(pos,holder);
    }

    void leaveFloor() {
        java.util.Arrays.fill(lastOpponent,null);
        java.util.Arrays.fill(attackTurn,0);
    }

    Bundle itemState(Map<String,Item> objects) {
        Bundle items=new Bundle();
        java.util.Set<Item> physical=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Map.Entry<String,Item> e : objects.entrySet()) {
            Item item=e.getValue();
            if (!physical.add(item)) throw new IllegalStateException("Item in multiple physical locations");
            Bundle entry=new Bundle();
            entry.put("id",ownership.identity(item));
            entry.put("owner",ownership.owner(item));
            entry.put("type",item.getClass().getName());
            entry.put("quantity",item.quantity());
            items.put(e.getKey(),entry);
        }
        return items;
    }

    private Map<String,Item> physicalItems(boolean withParts) {
        Map<String,Item> result=new TreeMap<>();
        for (int seat=0;seat<2;seat++) {
            int index=0;
            for (Item item : heroes[seat].belongings) {
                String path="s"+seat+"_"+index++;
                result.put(path,item);
                Item part=CoopVault.attached(item);
                if (withParts && part!=null) {
                    if (!restoring) ownership.claim(part,players[seat]);
                    result.put(path+"_part",part);
                }
            }
        }
        if (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.completed()
                && com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.pickaxe!=null)
            result.put("smithPick",com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.pickaxe);
        if (CoopSmith.pendingChoice()) {
            int choice=0;
            for (Item candidate:com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.smithRewards)
                result.put("smithOffer"+choice++,candidate);
        }
        if (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.isCompleted() && com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.reward!=null)
            result.put("impReward",com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.reward);
        result.putAll(floorItems(Dungeon.level, wandIdentities, sealIdentities));
        return result;
    }

    static Map<String,Item> floorItems(Level level, boolean includeWands, boolean includeSeals) {
        Map<String,Item> items = floorItems(level);
        if (includeWands) {
            for (Map.Entry<String,Item> entry : new TreeMap<>(items).entrySet()) {
                if (includeSeals && entry.getValue() instanceof Armor) {
                    BrokenSeal seal=((Armor)entry.getValue()).checkSeal();
                    if (seal!=null) items.put(entry.getKey()+"_seal",seal);
                }
                if (entry.getValue() instanceof MagesStaff) {
                    Item wand = ((MagesStaff)entry.getValue()).imbuedWand();
                    if (wand != null) items.put(entry.getKey()+"_wand",wand);
                }
            }
        }
        return items;
    }

    static Map<String,Item> floorItems(Level level) {
        Map<String,Item> result=new TreeMap<>();
        if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel) {
            int index = 0;
            for (Item item : ((com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel)level).storedEncounterItems())
                result.put("encounter" + index++, item);
        }
        for (Mob mob : level.mobs) {
            if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror) {
                Item offer=((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror)mob).reward;
                if (offer!=null) result.put("mirror"+mob.id(),offer);
            }
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion pins=mob.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion.class);
            if (pins!=null) {
                int index=0;
                for (Item stuck:pins.getStuckItems()) result.put("pin"+mob.id()+"_"+index++,stuck);
            }
            if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper) {
                int index=0;
                com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper keeper=(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper)mob;
                if (keeper.buybackItems.size()>com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.MAX_BUYBACK_HISTORY)
                    throw new IllegalArgumentException("Buyback history limit");
                for (Item sold:keeper.buybackItems)
                    result.put("shop"+mob.id()+"_"+index++,sold);
            }
            if (mob instanceof Thief && ((Thief)mob).item!=null) result.put("th"+mob.id(),((Thief)mob).item);
            if (mob instanceof ArmoredStatue) result.put("arm"+mob.id(),((ArmoredStatue)mob).armor());
            if (mob instanceof Statue) result.put("w"+mob.id(),((Statue)mob).weapon());
            if (mob instanceof Mimic && ((Mimic)mob).items!=null) {
                int index=0;
                for (Item item:((Mimic)mob).items) result.put("m"+mob.id()+"_"+index++,item);
            }
        }
        for (Heap heap : level.heaps.valueList()) {
            int index=0;
            for (Item item : heap.items) result.put("f"+heap.pos+"_"+index++,item);
        }
        return result;
    }

    public static synchronized CoopSession restore(Bundle b) {
        if (Dungeon.coop!=null || Dungeon.hero!=null || Dungeon.level!=null) throw new IllegalStateException("Dungeon in use");
        Bundle world=b.getBundle("world"), floor=b.getBundle("floor");
        int schema=floorSchema(b),depth=world.getInt("depth");
        if ((schema<1 || schema>15) || world.getInt("version")!=com.watabou.noosa.Game.versionCode ||
                (depth<1 || depth>CoopFloors.lastDepth(schema)) || (world.getInt("branch")!=0 && !(schema>=10 && world.getInt("branch")==1 && (CoopFloors.mineDepth(depth) || schema>=12 && CoopFloors.vaultDepth(depth)))) || world.getInt("challenges")!=0)
            throw new IllegalArgumentException("Unsupported world");
        int width=floor.getInt("width"), height=floor.getInt("height");
        if (width<3 || height<3 || width>256 || height>256 || floor.getIntArray("map").length!=width*height)
            throw new IllegalArgumentException("Invalid floor bounds");
        CoopFloors.checkLevelType(floor,depth,world.getInt("branch"));
        CoopSession s=new CoopSession();
        Dungeon.coop=s;
        s.restoring=true;
        s.corrosionFormat = b.getInt("schema") == 27;
        try {
            s.load(b,world);
            boolean holding = s.heroes[0].buff(Stasis.StasisBuff.class) != null || s.heroes[1].buff(Stasis.StasisBuff.class) != null;
            if (holding != (stasisSchema(b) == 25)) throw new IllegalArgumentException("Missing stasis envelope");
            if (holding) s.validateActorGraph();
            s.restoring=false;
            return s;
        } catch (RuntimeException | Error invalid) {
            Belongings.bundleRestoring=false;
            s.close();
            throw invalid;
        }
    }

    private void load(Bundle b,Bundle world) {
        sealIdentities = growthSchema(b) == 21;
        extendedGrowth = sealIdentities || growthSchema(b) == 20;
        wandIdentities = extendedGrowth || growthSchema(b) == 19;
        long[] roster=b.getLongArray("players");
        if (roster.length!=2 || roster[0]<=0 || roster[1]<=0 || roster[0]==roster[1])
            throw new IllegalArgumentException("Invalid roster");
        System.arraycopy(roster,0,players,0,2);
        Dungeon.restoreWorld(world,true);
        if (b.contains("documents")) Document.restore(b.getBundle("documents"));
        heroes[0]=Dungeon.hero;
        heroes[1]=(Hero)b.get("guest");
        if (baseSchema(b) == 22) {
            if (heroes[0].heroClass != HeroClass.CLERIC && heroes[1].heroClass != HeroClass.CLERIC)
                throw new IllegalArgumentException("Discovery roster has no cleric");
            for (int seat = 0; seat < heroes.length; seat++) discoveries[seat].restore(b, "discovered" + seat);
            if (discoveries[0].isEmpty() && discoveries[1].isEmpty()) throw new IllegalArgumentException("Empty discovery envelope");
        }
        Level level=Dungeon.level=(Level)b.get("floor");
        PathFinder.setMapSize(level.width(),level.height());
        activeSeat=b.getInt("active");
        turn=b.getLong("turn");
        partyDefeated=b.getBoolean("defeat");
        if (turn<1 || (partyDefeated ? activeSeat!=-1 : activeSeat<0 || activeSeat>=2))
            throw new IllegalArgumentException("Invalid turn");
        for (int i=0;i<2;i++) {
            Hero h=heroes[i];
            validatePlayer(players[i],h.heroClass);
            if ((h.heroClass==HeroClass.DUELIST || h.heroClass==HeroClass.CLERIC) && !wandIdentities) throw new IllegalArgumentException("Unsupported roster format");
            if (h.heroClass == HeroClass.ROGUE && growthSchema(b) < 18) throw new IllegalArgumentException("Rogue floor format");
            lives[i]=Life.valueOf(b.getString("life"+i));
            preDeath[i]=b.getInt("before"+i);
            if (!level.insideMap(h.pos) || h.HT<=0 || h.HP<0 || h.HP>h.HT || preDeath[i]<0 ||
                    (lives[i]==Life.ALIVE)!=(h.HP>0 || (rageSchema(b)==23 && preDeath[i]>0
                            && h.buff(Berserk.class)!=null && h.buff(Berserk.class).sustainsLife())) || lives[i]==Life.REVIVING ||
                    (i==activeSeat && lives[i]!=Life.ALIVE) || (partyDefeated && lives[i]!=Life.DEAD))
                throw new IllegalArgumentException("Invalid hero state");
            knowledge(i).load(b.getBundle("sight"+i));
            h.viewDistance=b.getInt("range"+i);
            h.fieldOfView=i==0 ? level.heroFOV : new boolean[level.length()];
        }
        if (rageSchema(b) == 23 && !(lives[0] == Life.ALIVE && heroes[0].HP == 0)
                && !(lives[1] == Life.ALIVE && heroes[1].HP == 0))
            throw new IllegalArgumentException("Rage envelope has no surviving zero-health hero");
        // Original bag restoration in co-op preserves object identity and skips collect().
        for (Hero hero : heroes) restorePackCharge(hero);
        if (growthSchema(b)==18 && heroes[0].heroClass!=HeroClass.ROGUE && heroes[1].heroClass!=HeroClass.ROGUE)
            throw new IllegalArgumentException("Missing Rogue in roster");
        if (heroes[0].pos==heroes[1].pos) throw new IllegalArgumentException("Overlapping heroes");
        Actor.add(heroes[1]);
        Actor.init();
        level.addRespawner();
        Actor.restoreSchedule(b.getBundle("schedule"),partyDefeated ? null : heroes[activeSeat]);
        for (Char ch : Actor.chars()) attachSprite(ch);
        HashSet<Long> ids=new HashSet<>();
        restoreItems(physicalItems(wandIdentities || floorSchema(b)>=12),b.getBundle("items"),b.getLong("nextItem"),ids);
        for (int seat = 0; seat < heroes.length; seat++) {
            if (b.contains("enchant" + seat) && scrollSchema(b) != 24)
                throw new IllegalArgumentException("Unexpected scroll choice");
            enchantScroll[seat] = b.contains("enchant"+seat) ? CoopEnchantScroll.restore(
                    b.getBundle("enchant"+seat), this, heroes[seat], b.getLong("nextItem")) : null;
        }
        if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel) {
            int holder=stairs.crystalHolder();
            if (holder<0 || heroes[1-holder].belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.class)!=null)
                throw new IllegalArgumentException("Missing or duplicate vault crystal");
            CoopVault.validate(this,heroes[holder].belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.class),holder,b.getLong("nextItem"),ids);
        }
        stairs.restore(b,ids);
        int schema=floorSchema(b);
        maskTrial = MaskTrial.OFF;
        maskRights = 0;
        maskUser = -1;
        crownUser=-1;
        crownRights=0;
        crownTrial=MaskTrial.OFF;
        if (growthSchema(b)==17 || growthSchema(b)>=18 && b.contains("crown_trial")) {
            if (!b.contains("crown_user") || !b.contains("crown_rights") || !b.contains("crown_trial"))
                throw new IllegalArgumentException("Missing crown trial ledger");
            crownTrial=MaskTrial.valueOf(b.getString("crown_trial"));
            crownUser=b.getInt("crown_user");
            crownRights=b.getInt("crown_rights");
            if (crownTrial==MaskTrial.OFF || crownUser < -1 || crownUser > 1
                    || crownUser >= 0 && heroes[crownUser].armorAbility==null
                    || crownRights!=0 && (crownUser<0 || crownTrial!=MaskTrial.ROSTER || crownRights!=(1<<(1-crownUser))
                    || heroes[1-crownUser].armorAbility!=null)) throw new IllegalArgumentException("Invalid crown trial ledger");
        }
        if (growthSchema(b) == 16 || growthSchema(b)>=17 && b.contains("mask_trial")) {
            if (!b.contains("mask_user") || !b.contains("mask_rights")) throw new IllegalArgumentException("Missing mask ledger");
            maskTrial = MaskTrial.valueOf(b.getString("mask_trial"));
            maskUser = b.getInt("mask_user");
            maskRights = b.getInt("mask_rights");
            if (maskTrial == MaskTrial.OFF || maskUser < -1 || maskUser > 1 ||
                    maskUser >= 0 && heroes[maskUser].subClass == HeroSubClass.NONE ||
                    maskRights != 0 && (maskUser < 0 || maskTrial != MaskTrial.ROSTER ||
                    maskRights != (1 << (1-maskUser)) || heroes[1-maskUser].subClass != HeroSubClass.NONE))
                throw new IllegalArgumentException("Invalid mask ledger");
        }
        ending=schema>=14 ? Ending.valueOf(b.getString("ending")) : Ending.NONE;
        if ((ascentHistory() || stairs.hasVaultEncounter())!=(schema==15)) throw new IllegalArgumentException("Invalid ascent version");
        int curses=0;
        for (int seat=0;seat<2;seat++) {
            if (heroes[seat].buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge.class)!=null) {
                if (amuletOwner()!=seat) throw new IllegalArgumentException("Invalid curse carrier");
                curses++;
            }
        }
        boolean ascending=ending==Ending.ASCENDING || ending==Ending.ASCENDED;
        if (curses!=(ascending?1:0) || Statistics.ascended!=(ending==Ending.ASCENDED)
                || !ascending && Statistics.highestAscent!=0
                || ascending && (Statistics.highestAscent<1 || Statistics.highestAscent>Dungeon.depth || Statistics.highestAscent>24)
                || ending==Ending.ASCENDED && Dungeon.depth!=1
                || (ending==Ending.ASCENT_PROPOSED || ending==Ending.ASCENT_READY) && (Dungeon.depth!=25 || Dungeon.branch!=0))
            throw new IllegalArgumentException("Invalid party ascent");
        if (schema>=14) {
            int count=0;
            for (int seat=0;seat<2;seat++) for (Item item:hero(seat).belongings)
                if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Amulet) {
                    if (ownership.owner(item)!=player(seat) || item.quantity()!=1) throw new IllegalArgumentException("Invalid amulet owner");
                    count++;
                }
            if (count+stairs.groundAmulets()!=(Dungeon.levelHasBeenGenerated(26,0)?1:0)
                    || count!=(Statistics.amuletObtained?1:0) || Statistics.amuletObtained!=(ending!=Ending.NONE)
                    || ending!=Ending.NONE && !Dungeon.levelHasBeenGenerated(26,0))
                throw new IllegalArgumentException("Invalid party ending");
        }
        for (int i=0;i<2;i++) {
            Hero h=heroes[i];
            if (h.frozenInCoop()!=(lives[i]!=Life.ALIVE) ||
                    (lives[i]==Life.DOWNED && (resource(i)==null || preDeath[i]<=0)))
                throw new IllegalArgumentException("Invalid casualty");
            for (Actor buff : h.buffs()) if (buff.frozenInCoop()!=(h.frozenInCoop() && !(buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge))) throw new IllegalArgumentException("Buff clock");
            if (b.contains("enemy"+i)) {
                lastOpponent[i]=(Mob)Actor.findById(b.getInt("enemy"+i));
                attackTurn[i]=b.getLong("hit"+i);
            }
            h.ready=i==activeSeat;
            level.updateFieldOfView(h,h.fieldOfView);
            observe(i);
        }
        Random.restoreWorld(b.getLongArray("random"));
        stairs.restoreWarp(b);
    }

    void restoreItems(Map<String,Item> objects, Bundle items,long ceiling,HashSet<Long> ids) {
        if (objects.size()>8192 || objects.size()!=items.getKeys().size()) throw new IllegalArgumentException("Item count");
        for (Map.Entry<String,Item> e : objects.entrySet()) {
            Bundle entry=items.getBundle(e.getKey());
            Item item=e.getValue();
            long id=entry.getLong("id"), owner=entry.getLong("owner");
            if (!ids.add(id) || !entry.getString("type").equals(item.getClass().getName()) ||
                    entry.getInt("quantity")!=item.quantity() || item.quantity()<=0 ||
                    (owner!=0 && owner!=players[0] && owner!=players[1]) ||
                    (e.getKey().startsWith("shop") && owner==0) ||
                    ((e.getKey().startsWith("s0_") || e.getKey().startsWith("s1_")) && owner!=players[e.getKey().charAt(1)-'0']))
                throw new IllegalArgumentException("Item owner or object mismatch");
            ownership.restored(item,id,owner,ceiling);
        }
    }

    @Override public void close() {
        checkExecutor();
        stop("Session closed");
        Actor.clear();
        Dungeon.level = null;
        Dungeon.hero = null;
        restoreBadges.run();
        restoreJournal.run();
        Random.leaveWorld();
        FileUtils.setBasePath(previousBase);
        Dungeon.coop = null;
    }

    /** Resolve animation completion on the same engine thread, with no GL scene. */
    private static final class EngineSprite extends CharSprite {
        EngineSprite(Char owner) {
            ch = owner;
            owner.sprite = this;
            visible = false;
        }
        @Override public void attack(int cell) { ch.onAttackComplete(); }
        @Override public void operate(int cell) { ch.onOperateComplete(); }
        @Override public void move(int from, int to) { ch.onMotionComplete(); }
        @Override public void place(int cell) { }
        @Override public void turnTo(int from, int to) { }
        @Override public void showAlert() { }
        @Override public void showInvestigate() { }
        @Override public void showLost() { }
    }
}
