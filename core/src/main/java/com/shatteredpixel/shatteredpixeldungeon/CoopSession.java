package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Albino;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/** A first-floor engine session. All commands and actor steps run on its creating thread. */
public final class CoopSession implements AutoCloseable {

    public enum Action { WAIT, MOVE, ATTACK, PICK_UP, EAT, EQUIP, DROP, UNEQUIP, REVIVE, ZAP, SEARCH, CONSENT, CANCEL_TRAVEL, TRAVEL;
        public boolean requiresItem() {
            return this == PICK_UP || this == EAT || this == EQUIP || this == DROP || this == UNEQUIP || this == ZAP;
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
            Dungeon.switchLevel(Dungeon.newLevel(), -1);
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
        return type == Rat.class || type == Snake.class || type == Gnoll.class || type == Albino.class
                || type == Piranha.class || type == SentryRoom.Sentry.class;
    }

    private boolean safeFloor(int cell,boolean moving) {
        Level level = Dungeon.level;
        if (!level.insideMap(cell) || !level.passable[cell] || level.pit[cell] || level.getTransition(cell) != null
                || level.traps.get(cell) != null || level.plants.get(cell) != null) return false;
        int terrain = level.map[cell];
        return (moving && (terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR))
                || terrain == Terrain.EMPTY || terrain == Terrain.EMPTY_SP || terrain == Terrain.GRASS
                || terrain == Terrain.WATER || terrain == Terrain.EMBERS || terrain == Terrain.EMPTY_DECO;
    }

    /** These two classes share Food.execute without any special effect callbacks. */
    public static boolean basicFood(Item item) {
        return item!=null && (item.getClass()==Food.class || item.getClass()==SmallRation.class);
    }

    private boolean basicWeapon(Item item) {
        return (item != null && ((item instanceof MagesStaff && ((MagesStaff)item).starterWand()!=null) || item.getClass() == Gloves.class || item.getClass() == WornShortsword.class
                || item.getClass() == Shortsword.class || item.getClass() == Dagger.class))
                && ((MeleeWeapon)item).enchantment == null && !item.cursed;
    }

    /**
     * Consume one offered turn. Cell is a destination for MOVE and ATTACK. PICK_UP
     * requires the exact item at the actor's feet. Item references are engine-local.
     */
    public boolean act(long player, long offeredTurn, Action action, int cell, Item item) {
        checkExecutor();
        restoredAfterTravel=false;
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
                case WAIT:
                    hero.rest(false);
                    break;
                case MOVE:
                    if (cell < 0 || cell >= level.length() || !level.adjacent(hero.pos, cell)
                            || !safeFloor(cell,true) || Actor.findChar(cell) != null || hero.rooted) return false;
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
                case ZAP:
                    if (!(item instanceof MagesStaff) || hero.belongings.weapon()!=item
                            || cell<0 || cell>=level.length() || !level.insideMap(cell) || !knowledge(activeSeat).visible(cell)) return false;
                    WandOfMagicMissile wand=((MagesStaff)item).starterWand();
                    Char victim=Actor.findChar(cell);
                    if (wand==null || !(victim instanceof Mob) || !basicMob(victim) || !victim.isAlive()
                            || victim.alignment!=Char.Alignment.ENEMY || victim.invisible>0 || hero.isCharmedBy(victim)
                            || level.distance(hero.pos,cell)>hero.viewDistance) return false;
                    Ballistica bolt=new Ballistica(hero.pos,cell,wand.collisionProperties(cell));
                    if (bolt.collisionPos!=cell || !wand.cast(hero,bolt)) return false;
                    lastOpponent[activeSeat]=(Mob)victim;
                    attackTurn[activeSeat]=turn;
                    break;
                case PICK_UP:
                    Heap heap = level.heaps.get(hero.pos);
                    if (cell != hero.pos || heap == null || heap.hidden || heap.type != Heap.Type.HEAP || item == null || heap.peek() != item
                            || (!basicFood(item) && item.getClass() != Ankh.class && !basicWeapon(item))
                            || !hero.belongings.backpack.canHold(item)) return false;
                    hero.checkVisibleMobs();
                    if (!hero.handle(hero.pos) || !(hero.curAction instanceof HeroAction.PickUp)) throw new IllegalStateException("Unexpected pickup action");
                    hero.act();
                    break;
                case EAT:
                    if (!basicFood(item) || !hero.belongings.backpack.contains(item)) return false;
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
                            || !(basicFood(item) || basicWeapon(item))) return false;
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
        b.put("schema",2);
        stairs.save(b);
        b.put("world",Dungeon.worldState());
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

    void leaveFloor() {
        java.util.Arrays.fill(lastOpponent,null);
        java.util.Arrays.fill(attackTurn,0);
    }

    Bundle itemState(Map<String,Item> objects) {
        Bundle items=new Bundle();
        for (Map.Entry<String,Item> e : objects.entrySet()) {
            Item item=e.getValue();
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
        if ((schema!=1 && schema!=2) || world.getInt("version")!=com.watabou.noosa.Game.versionCode ||
                (depth!=1 && (schema!=2 || depth!=2)) || world.getInt("branch")!=0 || world.getInt("challenges")!=0)
            throw new IllegalArgumentException("Unsupported world");
        int width=floor.getInt("width"), height=floor.getInt("height");
        if (width<3 || height<3 || width>256 || height>256 || floor.getIntArray("map").length!=width*height)
            throw new IllegalArgumentException("Invalid floor bounds");
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
                    (e.getKey().startsWith("s") && owner!=players[e.getKey().charAt(1)-'0']))
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
