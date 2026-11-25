package com.shatteredpixel.shatteredpixeldungeon;

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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
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
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
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
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.TargetedDamageWand;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/** A first-floor engine session. All commands and actor steps run on its creating thread. */
public final class CoopSession implements AutoCloseable {

    public enum Action { ACCEPT_WANDMAKER, WAND_FIRST, WAND_SECOND, BUY, SELL, SELL_ONE, BUYBACK, THROW, UPGRADE, IDENTIFY, WAIT, MOVE, ATTACK, PICK_UP, EAT, EQUIP, DROP, UNEQUIP, REVIVE, ZAP, SEARCH, CONSENT, CANCEL_TRAVEL, TRAVEL, ACCEPT_GHOST, GHOST_WEAPON, GHOST_ARMOR, SHATTER, OPEN, UNLOCK, GREET, DRINK, SHOOT;
        public boolean requiresItem() {
            return this==BUY || this==SELL || this==SELL_ONE || this==BUYBACK || this == THROW || this == UPGRADE || this == IDENTIFY || this == PICK_UP || this == EAT || this == EQUIP || this == DROP || this == UNEQUIP || this == ZAP || this == SHATTER || this == DRINK || this == SHOOT;
        }
    }

    public final CoopFloors stairs = new CoopFloors(this);
    public final ItemOwnership ownership = new ItemOwnership();
    public long playerOf(Char owner) {
        for (int seat = 0; seat < heroes.length; seat++) if (owner != null && owner == heroes[seat]) return players[seat];
        return 0;
    }
    private void bindGear(int seat) {
        for (Item item : heroes[seat].belongings) ownership.claim(item, players[seat]);
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

    public void fallen(Hero hero, int before) {
        checkExecutor();
        int seat = hero == heroes[0] ? 0 : 1;
        if (hero != heroes[seat]) throw new IllegalStateException("Unregistered casualty");
        if (lives[seat] != Life.ALIVE) return;
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
    public static synchronized CoopSession open(long seed, long player, HeroClass type) {
        validatePlayer(player, type);
        if (Dungeon.coop != null || Dungeon.hero != null || Dungeon.level != null) {
            throw new IllegalStateException("A dungeon is already loaded");
        }
        if (SPDSettings.challenges() != 0) throw new UnsupportedOperationException("Challenges");
        CoopSession session = new CoopSession();
        Dungeon.coop = session;
        try {
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
        if (type != HeroClass.WARRIOR && type != HeroClass.HUNTRESS && type != HeroClass.MAGE) {
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

    public static boolean basicMob(Actor actor) {
        Class<?> type = actor.getClass();
        if (type==ArmoredStatue.class) {
            ArmoredStatue statue=(ArmoredStatue)actor;
            Armor armor=statue.armor();
            return statueWeapon(statue.weapon()) && basicArmorType(armor)
                    && (armor.glyph instanceof Potential || armor.glyph instanceof Repulsion
                    || armor.glyph instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic
                    || armor.glyph instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness);
        }
        if (type==Statue.class) return statueWeapon(((Statue)actor).weapon());
        return (Dungeon.depth>=CoopFloors.FIRST_PRISON && Dungeon.depth<=CoopFloors.LAST_PLAYABLE && type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Skeleton.class)
                || (Dungeon.depth==CoopFloors.FIRST_PRISON && type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.class)
                || (Dungeon.depth>=7 && Dungeon.depth<=CoopFloors.LAST_PLAYABLE && (type==Necromancer.class || type==SpectralNecromancer.class || type==Necromancer.NecroSkeleton.class
                        || type==Wandmaker.class || type==RotHeart.class || type==RotLasher.class
                        || type==Elemental.NewbornFireElemental.class || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard.class || type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100.class))
                || (Dungeon.depth==9 && type==com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat.class)
                || type==CorpseDust.DustWraith.class || (Dungeon.depth>=7 && Dungeon.depth<=CoopFloors.LAST_PLAYABLE && (type==Wraith.class || type==TormentedSpirit.class))
                || type == GnollExile.class || type == PhantomPiranha.class || type == Rat.class || type == Snake.class || type == Gnoll.class || type == Albino.class
                || type == Mimic.class || type == CrystalMimic.class || type == GoldenMimic.class || type == Sheep.class || type == Bee.class || type == Piranha.class || type == SentryRoom.Sentry.class
                || type==Crab.class || type==HermitCrab.class || type==Swarm.class || type==Slime.class || type==CausticSlime.class || type==Thief.class
                || (Dungeon.depth>=2 && Dungeon.depth<=4 && type==Ghost.class)
                || (Dungeon.depth==2 && type==FetidRat.class) || (Dungeon.depth==3 && type==GnollTrickster.class)
                || (Dungeon.depth==4 && type==GreatCrab.class) || (Dungeon.depth==5 && (type==Goo.class || type==RatKing.class));
    }

    /** Only passive weapon statistics and on-hit effects run for a statue. Hero abilities have their own gate. */
    public static boolean statueWeapon(Item item) {
        if (!(item instanceof MeleeWeapon)) return false;
        Class<?> base = item.getClass();
        if (base == com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff.class) return ((MeleeWeapon)item).enchantment instanceof Venomous;
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
        if (level.traps.get(cell) != null)
            return moving && (level.map[cell] == Terrain.TRAP || level.map[cell] == Terrain.SECRET_TRAP || level.map[cell] == Terrain.INACTIVE_TRAP);
        if (!level.passable[cell]) return false;
        int terrain = level.map[cell];
        return (moving && (terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR))
                || terrain == Terrain.CUSTOM_DECO_EMPTY || terrain == Terrain.EMPTY || terrain == Terrain.EMPTY_SP || terrain == Terrain.GRASS
                || terrain == Terrain.HIGH_GRASS || terrain == Terrain.FURROWED_GRASS
                || terrain == Terrain.WATER || terrain == Terrain.EMBERS || terrain == Terrain.EMPTY_DECO;
    }

    /** These two classes share Food.execute without any special effect callbacks. */
    public static boolean basicFood(Item item) {
        return item!=null && (item.getClass()==Food.class || item.getClass()==SmallRation.class);
    }

    public static boolean basicArmorType(Item item) {
        return item!=null && (item.getClass()==ClothArmor.class
                || item.getClass()==LeatherArmor.class
                || item.getClass()==MailArmor.class);
    }

    public static boolean equipmentForScroll(Item item) {
        return basicArmorType(item) || basicWeaponType(item);
    }

    public static boolean basicMissile(Item item) {
        return item!=null && (item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone.class
                || item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife.class)
                && !item.cursed && ((Weapon)item).enchantment==null && item.trueLevel()==0;
    }

    public static boolean supportedScroll(Item item) {
        return item!=null && (item.getClass()==ScrollOfUpgrade.class || item.getClass()==ScrollOfIdentify.class);
    }

    private boolean readEquipmentScroll(Hero reader, Action action, Item source, int selection) {
        if (!supportedScroll(source) || !(reader.belongings.backpack.contains(source))
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
        return basicArmorType(item) && ((Armor)item).glyph==null;
    }

    public static boolean supportedHandheldWand(Item item) {
        return item != null && (item.getClass() == WandOfMagicMissile.class || item.getClass() == WandOfBlastWave.class || item.getClass() == WandOfFireblast.class)
                && item.isIdentified() && !item.cursed;
    }

    private static TargetedDamageWand wandFor(Hero hero, Item item) {
        if (item instanceof MagesStaff && hero.belongings.weapon() == item)
            return ((MagesStaff)item).starterWand();
        if (supportedHandheldWand(item) && hero.belongings.backpack.contains(item))
            return (TargetedDamageWand)item;
        return null;
    }

    public static boolean basicWeaponType(Item item) {
        return item!=null && ((item instanceof MagesStaff && ((MagesStaff)item).starterWand()!=null)
                || item.getClass()==Gloves.class || item.getClass()==WornShortsword.class
                || item.getClass()==Shortsword.class || item.getClass()==Dagger.class);
    }

    private boolean basicWeapon(Item item) {
        return basicWeaponType(item) && ((MeleeWeapon)item).enchantment==null && !item.cursed;
    }

    /**
     * Consume one offered turn. Cell is a destination for MOVE and ATTACK. PICK_UP
     * requires the exact item at the actor's feet. Item references are engine-local.
     */
    public boolean act(long player, long offeredTurn, Action action, int cell, Item item) {
        checkExecutor();
        restoredAfterTravel=false;
        destinationBlocker=null;
        if (activeSeat < 0 || lives[activeSeat] != Life.ALIVE || players[activeSeat] != player || offeredTurn != turn || action == null) return false;
        if (action.requiresItem() && (item == null || !ownership.allowed(item, player))) return false;
        Hero hero = heroes[activeSeat];
        Level level = Dungeon.level;
        Bundle before=null;
        if(action==Action.TRAVEL) {
            if(cell<0 || cell!=stairs.stair() || !stairs.agreed()) return false;
            before=checkpoint();
        }
        try {
            executing = true;
            switch (action) {
                case SEARCH:
                    if(cell!=hero.pos) return false;
                    hero.search(true);
                    break;
                case TRAVEL:
                    if (!stairs.travel(activeSeat,cell)) return false;
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
                    if (cell < 0 || cell >= level.length() || !level.adjacent(hero.pos, cell)) return false;
                    Char enemy = Actor.findChar(cell);
                    if (!(enemy instanceof Mob) || !basicMob(enemy) || enemy.alignment != Char.Alignment.ENEMY
                            || !enemy.isAlive() || enemy.invisible > 0 || !hero.fieldOfView[cell]
                            || hero.isCharmedBy(enemy)) return false;
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
                    boolean groundTarget = (wand instanceof WandOfBlastWave || wand instanceof WandOfFireblast) && victim == null && !level.solid[cell];
                    if (!groundTarget && !chestTarget && (!(victim instanceof Mob) || !basicMob(victim) || !victim.isAlive()
                            || (!disguised(victim) && victim.alignment!=Char.Alignment.ENEMY)
                            || victim.invisible>0 || hero.isCharmedBy(victim))) return false;
                    Ballistica bolt=new Ballistica(hero.pos,cell,wand.collisionProperties(cell));
                    // Fireblast aims its cone along a WONT_STOP ray. Its own spread checks each obstruction.
                    if (!(wand instanceof WandOfFireblast) && bolt.collisionPos != cell) return false;
                    if (!wand.cast(hero,bolt)) return false;
                    lastOpponent[activeSeat]=(Mob)victim;
                    attackTurn[activeSeat]=turn;
                    break;
                case PICK_UP:
                    Heap heap = level.heaps.get(hero.pos);
                    if (cell != hero.pos || heap == null || heap.hidden || heap.type != Heap.Type.HEAP || item == null || heap.peek() != item
                            || (item.getClass()!=Gold.class && item.getClass()!=IronKey.class && item.getClass()!=WornKey.class && item.getClass()!=GooBlob.class && !(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand && ownership.owner(item)==player) && !(item instanceof CorpseDust || item instanceof Embers || item instanceof CeremonialCandle || item instanceof Rotberry.Seed) && !(item instanceof Potion) && !supportedScroll(item) && !basicMissile(item) && !basicArmorType(item) && !statueWeapon(item) && !basicFood(item) && !(item instanceof Honeypot || item instanceof Honeypot.ShatteredPot) && item.getClass() != Ankh.class && !basicWeapon(item))
                            || (item.getClass()!=Gold.class && item.getClass()!=IronKey.class && item.getClass()!=WornKey.class && !hero.belongings.backpack.canHold(item))) return false;
                    hero.checkVisibleMobs();
                    if (!hero.handle(hero.pos) || !(hero.curAction instanceof HeroAction.PickUp)) throw new IllegalStateException("Unexpected pickup action");
                    hero.act();
                    break;
                case BUY: case SELL: case SELL_ONE: case BUYBACK:
                    if (!CoopTrade.execute(this,hero,action,cell,item)) return false;
                    break;
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
                    if (!basicMissile(item) || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)
                            || cell==hero.pos || level.solid[cell] || !supportedLanding(cell) || level.getTransition(cell)!=null) return false;
                    Char hit=Actor.findChar(cell);
                    if (hit!=null && (!(hit instanceof Mob) || hit.alignment!=Char.Alignment.ENEMY || !basicMob(hit)
                            || !hit.isAlive() || hit.invisible>0 || hero.isCharmedBy(hit))) return false;
                    if (!((com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)item).throwInCoop(hero,cell)) return false;
                    if (hit!=null) { lastOpponent[activeSeat]=(Mob)hit; attackTurn[activeSeat]=turn; }
                    break;
                case SHOOT:
                    if (!(item instanceof SpiritBow) || hero.heroClass!=HeroClass.HUNTRESS
                            || !hero.belongings.backpack.contains(item) || !level.insideMap(cell)
                            || !knowledge(activeSeat).visible(cell)) return false;
                    Char mark=Actor.findChar(cell);
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
                    if (!basicFood(item) || !hero.belongings.backpack.contains(item)) return false;
                    item.execute(hero, Food.AC_EAT);
                    break;
                case EQUIP:
                    if (basicArmor(item)) {
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
                    if (item!=null && item==hero.belongings.armor && basicArmor(item)) {
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
            if (!stopped() && !partyDefeated) advance();
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
        b.put("schema",stairs.schema());
        stairs.save(b);
        b.put("world",Dungeon.worldState());
        b.put("documents",Document.coopState());
        b.put("guest",heroes[1]);
        b.put("floor",Dungeon.level);
        b.put("players",players);
        b.put("turn",turn);
        b.put("active",activeSeat);
        b.put("defeat",partyDefeated);
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
        b.put("items",itemState(physicalItems()));
        b.put("nextItem",ownership.sequence());
        return b;
    }

    void validateActorGraph() {
        HashSet<Actor> persistent = new HashSet<>();
        persistent.add(heroes[0]); persistent.add(heroes[1]);
        persistent.addAll(Dungeon.level.mobs);
        for (Actor ch : new HashSet<>(persistent)) persistent.addAll(((Char)ch).buffs());
        persistent.addAll(Dungeon.level.blobs.values());
        Actor spawner = Dungeon.level.respawner();
        if (spawner!=null) persistent.add(spawner);
        if (!persistent.equals(Actor.all())) throw new IllegalStateException("Transient actor at checkpoint");
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

    private Map<String,Item> physicalItems() {
        Map<String,Item> result=new TreeMap<>();
        for (int seat=0;seat<2;seat++) {
            int index=0;
            for (Item item : heroes[seat].belongings) result.put("s"+seat+"_"+index++,item);
        }
        result.putAll(floorItems(Dungeon.level));
        return result;
    }

    static Map<String,Item> floorItems(Level level) {
        Map<String,Item> result=new TreeMap<>();
        if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel) {
            int index = 0;
            for (Item item : ((com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel)level).storedEncounterItems())
                result.put("encounter" + index++, item);
        }
        for (Mob mob : level.mobs) {
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
        int schema=b.getInt("schema"),depth=world.getInt("depth");
        if ((schema<1 || schema>6) || world.getInt("version")!=com.watabou.noosa.Game.versionCode ||
                (depth<1 || depth>CoopFloors.lastDepth(schema)) || world.getInt("branch")!=0 || world.getInt("challenges")!=0)
            throw new IllegalArgumentException("Unsupported world");
        int width=floor.getInt("width"), height=floor.getInt("height");
        if (width<3 || height<3 || width>256 || height>256 || floor.getIntArray("map").length!=width*height)
            throw new IllegalArgumentException("Invalid floor bounds");
        CoopFloors.checkLevelType(floor,depth);
        CoopSession s=new CoopSession();
        Dungeon.coop=s;
        s.restoring=true;
        try {
            s.load(b,world);
            s.restoring=false;
            return s;
        } catch (RuntimeException | Error invalid) {
            Belongings.bundleRestoring=false;
            s.close();
            throw invalid;
        }
    }

    private void load(Bundle b,Bundle world) {
        long[] roster=b.getLongArray("players");
        if (roster.length!=2 || roster[0]<=0 || roster[1]<=0 || roster[0]==roster[1])
            throw new IllegalArgumentException("Invalid roster");
        System.arraycopy(roster,0,players,0,2);
        Dungeon.restoreWorld(world,true);
        if (b.contains("documents")) Document.restore(b.getBundle("documents"));
        heroes[0]=Dungeon.hero;
        heroes[1]=(Hero)b.get("guest");
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
            lives[i]=Life.valueOf(b.getString("life"+i));
            preDeath[i]=b.getInt("before"+i);
            if (!level.insideMap(h.pos) || h.HT<=0 || h.HP<0 || h.HP>h.HT || preDeath[i]<0 ||
                    (lives[i]==Life.ALIVE)!=(h.HP>0) || lives[i]==Life.REVIVING ||
                    (i==activeSeat && lives[i]!=Life.ALIVE) || (partyDefeated && lives[i]!=Life.DEAD))
                throw new IllegalArgumentException("Invalid hero state");
            knowledge(i).load(b.getBundle("sight"+i));
            h.viewDistance=b.getInt("range"+i);
            h.fieldOfView=i==0 ? level.heroFOV : new boolean[level.length()];
        }
        // Original bag restoration in co-op preserves object identity and skips collect().
        for (Hero hero : heroes) for (Item item : hero.belongings.backpack) {
            if (item instanceof MagesStaff) ((MagesStaff)item).applyWandChargeBuff(hero);
            if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)
                ((com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)item).restoreCoopCharge(hero);
        }
        if (heroes[0].pos==heroes[1].pos) throw new IllegalArgumentException("Overlapping heroes");
        Actor.add(heroes[1]);
        Actor.init();
        level.addRespawner();
        Actor.restoreSchedule(b.getBundle("schedule"),partyDefeated ? null : heroes[activeSeat]);
        for (Char ch : Actor.chars()) attachSprite(ch);
        HashSet<Long> ids=new HashSet<>();
        restoreItems(physicalItems(),b.getBundle("items"),b.getLong("nextItem"),ids);
        stairs.restore(b,ids);
        for (int i=0;i<2;i++) {
            Hero h=heroes[i];
            if (h.frozenInCoop()!=(lives[i]!=Life.ALIVE) ||
                    (lives[i]==Life.DOWNED && (resource(i)==null || preDeath[i]<=0)))
                throw new IllegalArgumentException("Invalid casualty");
            for (Actor buff : h.buffs()) if (buff.frozenInCoop()!=h.frozenInCoop()) throw new IllegalArgumentException("Buff clock");
            if (b.contains("enemy"+i)) {
                lastOpponent[i]=(Mob)Actor.findById(b.getInt("enemy"+i));
                attackTurn[i]=b.getLong("hit"+i);
            }
            h.ready=i==activeSeat;
            level.updateFieldOfView(h,h.fieldOfView);
            observe(i);
        }
        Random.restoreWorld(b.getLongArray("random"));
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
