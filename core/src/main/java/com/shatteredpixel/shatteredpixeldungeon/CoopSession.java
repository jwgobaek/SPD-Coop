package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Albino;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/** A first-floor engine session. All commands and actor steps run on its creating thread. */
public final class CoopSession implements AutoCloseable {

    public enum Action { WAIT, MOVE, ATTACK, PICK_UP, EAT, EQUIP, DROP, UNEQUIP, REVIVE;
        public boolean requiresItem() {
            return this == PICK_UP || this == EAT || this == EQUIP || this == DROP || this == UNEQUIP;
        }
    }

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

    private CoopSession() {
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
            Dungeon.switchLevel(Dungeon.newLevel(), -1);
            return session;
        } catch (RuntimeException | Error failure) {
            session.close();
            throw failure;
        }
    }

    private static void validatePlayer(long player, HeroClass type) {
        if (player <= 0) throw new IllegalArgumentException("Player ID must be positive");
        if (type != HeroClass.WARRIOR && type != HeroClass.HUNTRESS) {
            throw new UnsupportedOperationException("Supported classes are warrior and huntress");
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
            if (safeFloor(cell) && Actor.findChar(cell) == null && level.heaps.get(cell) == null) {
                spawn = cell;
                break;
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

    /** Secondary ordering only for equal actor time and priority. */
    public boolean precedes(Actor candidate, Actor other) {
        return candidate == heroes[0] && other == heroes[1];
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
        return type == Rat.class || type == Snake.class || type == Gnoll.class || type == Albino.class;
    }

    private boolean safeFloor(int cell) {
        Level level = Dungeon.level;
        if (!level.insideMap(cell) || !level.passable[cell] || level.pit[cell] || level.getTransition(cell) != null
                || level.traps.get(cell) != null || level.plants.get(cell) != null) return false;
        int terrain = level.map[cell];
        return terrain == Terrain.EMPTY || terrain == Terrain.EMPTY_SP || terrain == Terrain.GRASS
                || terrain == Terrain.WATER || terrain == Terrain.EMBERS || terrain == Terrain.EMPTY_DECO;
    }

    private boolean basicWeapon(Item item) {
        return (item != null && (item.getClass() == Gloves.class || item.getClass() == WornShortsword.class
                || item.getClass() == Shortsword.class || item.getClass() == Dagger.class))
                && ((MeleeWeapon)item).enchantment == null && !item.cursed;
    }

    /**
     * Consume one offered turn. Cell is a destination for MOVE and ATTACK. PICK_UP
     * requires the exact item at the actor's feet. Item references are engine-local.
     */
    public boolean act(long player, long offeredTurn, Action action, int cell, Item item) {
        checkExecutor();
        if (activeSeat < 0 || lives[activeSeat] != Life.ALIVE || players[activeSeat] != player || offeredTurn != turn || action == null) return false;
        if (action.requiresItem() && (item == null || !ownership.allowed(item, player))) return false;
        Hero hero = heroes[activeSeat];
        Level level = Dungeon.level;
        try {
            switch (action) {
                case WAIT:
                    hero.rest(false);
                    break;
                case MOVE:
                    if (cell < 0 || cell >= level.length() || !level.adjacent(hero.pos, cell)
                            || !safeFloor(cell) || Actor.findChar(cell) != null || hero.rooted) return false;
                    Heap destination = level.heaps.get(cell);
                    if (destination != null && destination.type != Heap.Type.HEAP) return false;
                    hero.checkVisibleMobs();
                    hero.handle(cell);
                    if (hero.curAction instanceof HeroAction.PickUp) hero.curAction = new HeroAction.Move(cell);
                    if (!(hero.curAction instanceof HeroAction.Move)) throw new IllegalStateException("Unexpected move action");
                    hero.act();
                    if (hero.pos != cell) throw new IllegalStateException("Move was interrupted");
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
                case PICK_UP:
                    Heap heap = level.heaps.get(hero.pos);
                    if (cell != hero.pos || heap == null || heap.hidden || heap.type != Heap.Type.HEAP || item == null || heap.peek() != item
                            || (item.getClass() != Food.class && item.getClass() != Ankh.class && !basicWeapon(item))
                            || !hero.belongings.backpack.canHold(item)) return false;
                    hero.checkVisibleMobs();
                    if (!hero.handle(hero.pos) || !(hero.curAction instanceof HeroAction.PickUp)) throw new IllegalStateException("Unexpected pickup action");
                    hero.act();
                    break;
                case EAT:
                    if (item == null || item.getClass() != Food.class || !hero.belongings.backpack.contains(item)) return false;
                    item.execute(hero, Food.AC_EAT);
                    break;
                case EQUIP:
                    if (!basicWeapon(item) || !hero.belongings.backpack.contains(item)
                            || (hero.belongings.weapon != null && hero.belongings.weapon.cursed)) return false;
                    if (!((MeleeWeapon)item).doEquip(hero)) return false;
                    break;
                case REVIVE:
                    if (!revive(hero, cell)) return false;
                    break;
                case UNEQUIP:
                    if (item != hero.belongings.weapon || !basicWeapon(item) || !hero.belongings.backpack.canHold(item)) return false;
                    if (!((MeleeWeapon)item).doUnequip(hero, true)) return false;
                    break;
                case DROP:
                    Heap floor = level.heaps.get(hero.pos);
                    if (cell != hero.pos || (floor != null && floor.type != Heap.Type.HEAP)
                            || level.pit[cell] || !level.passable[cell] || !hero.belongings.backpack.contains(item)
                            || !(item.getClass() == Food.class || basicWeapon(item))) return false;
                    item.dropCoopUnit(hero);
                    break;
                default:
                    return false;
            }
            hero.curAction = null;
            if (!stopped() && !partyDefeated) advance();
            return true;
        } catch (RuntimeException failure) {
            stop(failure.toString());
            throw failure;
        }
    }

    private void advance() {
        activeSeat = -1;
        Actor.process();
        if (!stopped()) for (int seat = 0; seat < heroes.length; seat++) observe(seat);
        if (!partyDefeated && !stopped() && activeSeat < 0) throw new IllegalStateException("Scheduler ended without an input turn");
    }

    @Override public void close() {
        checkExecutor();
        stop("Session closed");
        Actor.clear();
        Dungeon.level = null;
        Dungeon.hero = null;
        restoreBadges.run();
        restoreJournal.run();
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
