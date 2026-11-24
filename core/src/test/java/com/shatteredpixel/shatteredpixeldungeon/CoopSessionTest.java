package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.Test;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class CoopSessionTest extends HeadlessDungeon {
    private CoopSession open(long seed) {
        CoopSession session = CoopSession.open(seed, 71, HeroClass.WARRIOR);
        try {
            session.join(94, HeroClass.HUNTRESS);
            return session;
        } catch (RuntimeException e) {
            session.close();
            throw e;
        }
    }

    private boolean send(CoopSession s, CoopSession.Action action, int cell, Item item) {
        return s.act(s.player(s.activeSeat()), s.turn(), action, cell, item);
    }

    private int neighbour(Hero hero) {
        for (int offset : PathFinder.NEIGHBOURS8) {
            int cell = hero.pos + offset;
            if (Dungeon.level.map[cell] == Terrain.EMPTY && Dungeon.level.heaps.get(cell) == null
                    && Dungeon.level.plants.get(cell) == null && Actor.findChar(cell) == null) return cell;
        }
        throw new AssertionError("No adjacent plain floor");
    }

    @Test(timeout = 30000)
    public void joinPreservesGeneratedWorldAndRegistersEachOwnerOnce() {
        try (CoopSession session = CoopSession.open(402, 71, HeroClass.HUNTRESS)) {
            Hero host = Dungeon.hero;
            Level level = Dungeon.level;
            int[] terrain = level.map.clone();
            HashSet<Mob> mobs = new HashSet<>(level.mobs);
            HashSet<Actor> before = Actor.all();
            int id = host.id(), hp = --host.HP;
            int limitedDrops = Dungeon.LimitedDrops.VELVET_POUCH.count;
            int floorCount = Dungeon.generatedLevels.size();
            Item weapon = host.belongings.weapon;
            int pos = host.pos;
            Dungeon.gold = 37;
            Statistics.foodEaten = 8;
            Random.pushGenerator(512);
            long expected = Random.Long();
            Random.popGenerator();
            Random.pushGenerator(512);
            try {
                session.join(94, HeroClass.WARRIOR);
                assertEquals(expected, Random.Long());
            } finally {
                Random.popGenerator();
            }
            Hero guest = session.hero(1);
            assertEquals(floorCount, Dungeon.generatedLevels.size());
            assertEquals(limitedDrops, Dungeon.LimitedDrops.VELVET_POUCH.count);
            assertEquals(71, session.player(0));
            assertEquals(94, session.player(1));
            assertSame(host, session.hero(0));
            assertSame(host, Dungeon.hero);
            assertSame(level, Dungeon.level);
            assertArrayEquals(terrain, level.map);
            assertEquals(mobs, level.mobs);
            assertTrue(Actor.all().containsAll(before));
            assertEquals(37, Dungeon.gold);
            assertEquals(8, Statistics.foodEaten);
            assertEquals(id, host.id());
            assertEquals(hp, host.HP);
            assertSame(weapon, host.belongings.weapon);
            assertEquals(pos, host.pos);
            assertEquals(HeroClass.WARRIOR, guest.heroClass);
            assertEquals(HeroClass.HUNTRESS, host.heroClass);
            assertNotEquals(host.id(), guest.id());
            assertNotEquals(host.pos, guest.pos);
            for (Hero h : new Hero[]{host, guest}) {
                assertTrue(level.passable[h.pos]);
                assertSame(h, Actor.findById(h.id()));
                assertSame(h, Actor.findChar(h.pos));
                assertSame(h, h.belongings.backpack.owner);
                assertTrue(Actor.all().containsAll(h.buffs()));
            }
            assertNotSame(host.talents, guest.talents);
            assertNotSame(host.belongings.backpack, guest.belongings.backpack);
            assertEquals(0, session.activeSeat());
            assertThrows(IllegalStateException.class, () -> session.join(95, HeroClass.HUNTRESS));
        }
    }

    @Test(timeout = 30000)
    public void movesCheckSeatTimeWallsAndOccupants() {
        try (CoopSession s = open(402)) {
            Hero first = s.hero(0), second = s.hero(1);
            int p1 = first.pos, p2 = second.pos;
            long offered = s.turn();
            assertFalse(s.act(94, offered, WAIT, 0, null));
            assertFalse(send(s, MOVE, p2, null));
            assertFalse(send(s, MOVE, -1, null));
            int cell = neighbour(first);
            Level.set(cell, Terrain.WALL);
            assertFalse(send(s, MOVE, cell, null));
            assertEquals(offered, s.turn());
            assertEquals(0, Actor.now(), 0);
            Level.set(cell, Terrain.EMPTY);
            Buff.prolong(first, Haste.class, 6f);
            Buff.prolong(second, Slow.class, 6f);
            assertNull(second.buff(Haste.class));
            assertNull(first.buff(Slow.class));
            assertTrue(send(s, MOVE, cell, null));
            assertFalse(s.act(71, offered, MOVE, p1, null));
            assertEquals(cell, first.pos);
            assertEquals(p2, second.pos);
            assertEquals(1, s.activeSeat());
            float t1 = first.cooldown();
            assertEquals(1 / first.speed(), t1, 0.0001f);
            int destination = neighbour(second);
            assertTrue(send(s, MOVE, destination, null));
            assertEquals(cell, first.pos);
            assertEquals(destination, second.pos);
            assertEquals(0, s.activeSeat());
            assertTrue(second.cooldown() > first.cooldown());
        }
    }

    @Test(timeout = 30000)
    public void pickupEquipAndEatRequireTheActingOwnersItem() {
        try (CoopSession s = open(402)) {
            Shortsword[] loot = {new Shortsword(), new Shortsword()};
            loot[1].upgrade();
            for (int seat = 0; seat < 2; seat++) {
                Hero hero = s.hero(seat);
                Dungeon.level.drop(loot[seat], hero.pos);
                assertEquals(seat, s.activeSeat());
                long offer = s.turn();
                assertTrue(send(s, PICK_UP, hero.pos, loot[seat]));
                assertSame(loot[seat], hero.belongings.getItem(Shortsword.class));
                assertFalse(s.act(s.player(seat), offer, PICK_UP, hero.pos, loot[seat]));
                assertFalse(send(s, PICK_UP, hero.pos, loot[seat]));
                assertNull(Dungeon.level.heaps.get(hero.pos));
            }
            for (int seat = 0; seat < 2; seat++) {
                assertEquals(seat, s.activeSeat());
                assertFalse(send(s, EQUIP, 0, loot[1-seat]));
                assertTrue(send(s, EQUIP, 0, loot[seat]));
                assertSame(loot[seat], s.hero(seat).belongings.weapon);
                assertEquals(seat, loot[seat].level());
            }
            for (int seat = 0; seat < 2; seat++) {
                Hero hero = s.hero(seat);
                Food food = hero.belongings.getItem(Food.class);
                food.quantity(2);
                assertFalse(send(s, EAT, 0, s.hero(1-seat).belongings.getItem(Food.class)));
                assertTrue(send(s, EAT, 0, food));
                assertEquals(1, food.quantity());
            }
            assertEquals(2, Statistics.foodEaten);
        }
    }

    @Test(timeout = 30000)
    public void basicMonsterChoosesGuestAndTakesSingleTurns() {
        try (CoopSession s = open(402)) {
            Hero host = s.hero(0), guest = s.hero(1);
            Rat rat = new Rat();
            for (int offset : PathFinder.NEIGHBOURS8) {
                int cell = guest.pos + offset;
                if (Dungeon.level.passable[cell] && !Dungeon.level.adjacent(host.pos, cell)
                        && Actor.findChar(cell) == null) { rat.pos = cell; break; }
            }
            assertTrue(Dungeon.level.adjacent(guest.pos, rat.pos));
            rat.state = rat.HUNTING;
            GameScene.add(rat);
            int hp = host.HP;
            Random.pushGenerator(408);
            try {
                for (int i = 0; i < 8; i++) assertTrue(send(s, WAIT, 0, null));
            } finally { Random.popGenerator(); }
            assertTrue(rat.isTargeting(guest));
            assertEquals(hp, host.HP);
            assertTrue(guest.HP < guest.HT);
            assertEquals(4, Actor.now() + rat.cooldown(), 0.001f);
        }
    }

    @Test(timeout = 30000)
    public void bothHeroesAttackOneActorAndCannotReplay() {
        try (CoopSession s = open(402)) {
            Rat rat = new Rat();
            Hero first = s.hero(0), second = s.hero(1);
            for (int delta : PathFinder.NEIGHBOURS8) {
                int cell = first.pos + delta;
                if (Dungeon.level.adjacent(cell, second.pos) && Dungeon.level.passable[cell]
                        && Actor.findChar(cell) == null) { rat.pos = cell; break; }
            }
            assertTrue(Dungeon.level.adjacent(first.pos, rat.pos));
            int hp = rat.HP = rat.HT = 20;
            GameScene.add(rat);
            Random.pushGenerator(402);
            try {
                for (int seat = 0; seat < 2; seat++) {
                    long offer = s.turn();
                    assertEquals(seat, s.activeSeat());
                    Random.pushGenerator(402);
                    try {
                        float scheduled = Actor.now() + s.hero(seat).cooldown();
                        float cost = s.hero(seat).attackDelay();
                        assertTrue(send(s, ATTACK, rat.pos, null));
                        assertEquals(scheduled + cost, Actor.now() + s.hero(seat).cooldown(), 0.001f);
                    } finally { Random.popGenerator(); }
                    assertTrue("seat=" + seat + " HP " + hp + " -> " + rat.HP, rat.HP < hp);
                    hp = rat.HP;
                    assertFalse(s.act(s.player(seat), offer, ATTACK, rat.pos, null));
                    assertEquals(hp, rat.HP);
                }
            } finally { Random.popGenerator(); }
        }
    }

    @Test(timeout = 30000)
    public void combatGeometryTracksEngagementWithoutAdvancingActors() {
        try (CoopSession game = open(402)) {
            Level level = Dungeon.level;
            int w = level.width(), center = level.length()/2/w*w + w/2;
            Hero a = game.hero(0), b = game.hero(1);
            for (int y=-3; y<=3; y++) for (int x=-5; x<=5; x++) {
                int cell = center+y*w+x;
                Level.set(cell, x==0 ? Terrain.WALL : Terrain.EMPTY);
                level.discoverable[cell]=true;
            }
            for (Mob mob : level.mobs) mob.clearEnemy();
            a.pos=center-3; b.pos=center+3;
            Rat left=new Rat(), right=new Rat();
            left.pos=a.pos+1; right.pos=b.pos-1;
            GameScene.add(left); GameScene.add(right);
            left.aggro(a); right.aggro(b);
            HashSet<Actor> actors = Actor.all();
            float now = Actor.now();
            long turn = game.turn();
            int hp = a.HP + b.HP + left.HP + right.HP;
            for (int repeat=0; repeat<5; repeat++) {
                assertEquals(2, CombatZones.calculate(game,null).size());
                java.util.List<java.util.BitSet> publicView = CombatZones.calculate(game,game.observe(0));
                assertEquals(1, publicView.size());
                assertFalse(publicView.get(0).get(b.pos));
                right.aggro(a);
                assertEquals(1, CombatZones.calculate(game,null).size());
                right.aggro(b);
                Level.set(center,Terrain.EMPTY);
                a.pos=center-1; b.pos=center+1;
                assertEquals(1, CombatZones.calculate(game,null).size());
                a.pos=center-3; b.pos=center+3;
                Level.set(center,Terrain.WALL);
            }
            left.HP=0;
            assertEquals(1,CombatZones.calculate(game,null).size());
            assertTrue(CombatZones.calculate(game,game.observe(0)).isEmpty());
            assertEquals(actors,Actor.all());
            assertEquals(now,Actor.now(),0);
            assertEquals(turn,game.turn());
            assertEquals(hp-left.HT,a.HP+b.HP+right.HP);
        }
    }

    @Test(timeout = 30000)
    public void droppedUnitsKeepTheOwnerThroughSplitMergeAndPickup() {
        try (CoopSession game = open(402)) {
            Hero a = game.hero(0), b = game.hero(1);
            int cell = neighbour(a);
            Food loot = new Food();
            loot.quantity(2);
            Dungeon.level.drop(loot, cell);
            assertEquals(0,game.ownership.owner(loot));
            assertTrue(send(game,MOVE,cell,null));
            assertTrue(send(game,WAIT,0,null));
            assertTrue(send(game,PICK_UP,cell,loot));
            Food stack = a.belongings.getItem(Food.class);
            assertEquals(3,stack.quantity());
            assertEquals(0,loot.quantity());
            assertEquals(game.player(0),game.ownership.owner(stack));
            assertTrue(send(game,WAIT,0,null));
            assertTrue(send(game,DROP,cell,stack));
            Item first = Dungeon.level.heaps.get(cell).peek();
            assertNotEquals(game.ownership.identity(stack),game.ownership.identity(first));
            assertEquals(game.player(0),game.ownership.owner(first));
            assertFalse(first.collect(b.belongings.backpack));
            Food other = b.belongings.getItem(Food.class);
            assertFalse(first.canStackWith(other));
            Dungeon.level.drop(other.detach(b.belongings.backpack),cell);
            assertEquals(2,Dungeon.level.heaps.get(cell).items.size());
            assertEquals(2,stack.quantity());
            assertFalse(send(game,EAT,0,stack));
            assertFalse(send(game,PICK_UP,cell,first));
            assertTrue(send(game,WAIT,0,null));
            Item equipped = a.belongings.weapon;
            assertTrue(send(game,UNEQUIP,0,equipped));
            assertTrue(a.belongings.backpack.contains(equipped));
            assertEquals(game.player(0),game.ownership.owner(equipped));
            assertTrue(send(game,WAIT,0,null));
            assertTrue(send(game,EQUIP,0,equipped));
            assertSame(equipped,a.belongings.weapon);
        }
    }

    @Test(timeout = 30000)
    public void partnerRevivesOnceAtTheReservedCellThenBothCanAct() {
        int[] before = {100,17,10,9,3,2,1,Integer.MAX_VALUE};
        int[] expected = {30,5,3,2,1,1,1,644245094};
        for (int i=0;i<before.length;i++) assertEquals(expected[i],CoopSession.reviveHP(before[i],Integer.MAX_VALUE));
        assertEquals(2, CoopSession.reviveHP(17,2));
        try (CoopSession s=open(402)) {
            Hero casualty=s.hero(0), rescuer=s.hero(1);
            com.shatteredpixel.shatteredpixeldungeon.items.Ankh ankh = new com.shatteredpixel.shatteredpixeldungeon.items.Ankh();
            ankh.bless();
            assertTrue(ankh.collect(casualty.belongings.backpack));
            casualty.HP=17;
            Rat rat=new Rat();
            rat.pos=neighbour(casualty);
            GameScene.add(rat,100);
            for (int i=0;i<8 && casualty.isAlive();i++) rat.attack(casualty,1f,10000f,Float.POSITIVE_INFINITY);
            assertEquals(CoopSession.Life.DOWNED,s.life(0));
            assertEquals(17,s.preDeathHP(0));
            casualty.die(rat);
            assertEquals(17,s.preDeathHP(0));
            s.finishEffects();
            assertEquals(1,s.activeSeat());
            assertSame(casualty, Actor.findChar(casualty.pos));
            assertFalse(s.act(s.player(0),s.turn(),WAIT,0,null));
            assertFalse(send(s,REVIVE,rescuer.pos,null));
            int position=rescuer.pos;
            rescuer.pos=casualty.pos+3;
            assertFalse(send(s,REVIVE,casualty.pos,null));
            rescuer.pos=position;
            float due=casualty.cooldown();
            assertTrue(send(s,WAIT,0,null));
            Actor.fixTime();
            assertEquals(due,casualty.cooldown(),0f);
            long offered=s.turn();
            assertTrue(send(s,REVIVE,casualty.pos,null));
            assertFalse(s.act(s.player(1),offered,REVIVE,casualty.pos,null));
            assertEquals(CoopSession.Life.ALIVE,s.life(0));
            assertEquals(5,casualty.HP);
            assertFalse(casualty.belongings.backpack.contains(ankh));
            assertSame(casualty,Actor.findById(casualty.id()));
            assertEquals(0,s.activeSeat());
            assertTrue(send(s,WAIT,0,null));
            casualty.die(this);
            s.finishEffects();
            assertEquals(CoopSession.Life.DEAD,s.life(0));
            assertFalse(send(s,REVIVE,casualty.pos,null));
            rescuer.HP=1;
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison dot =
                    new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison();
            dot.target=rescuer;
            dot.set(1);
            dot.act();
            assertTrue(s.defeated());
            assertFalse(s.stopped());
            assertEquals(1,s.preDeathHP(1));
            assertEquals(-1,s.activeSeat());
            assertFalse(s.act(s.player(1),s.turn(),WAIT,0,null));
        }
    }

    @Test(timeout = 30000)
    public void seatsContendForOneHeapOnTheGeneratedFloor() {
        try (CoopSession s = open(402)) {
            Hero host = s.hero(0), guest = s.hero(1);
            int cell = neighbour(host);
            Shortsword loot = new Shortsword();
            Dungeon.level.drop(loot, cell);
            assertTrue(send(s, MOVE, cell, null));
            assertFalse(send(s, MOVE, cell, null));
            assertFalse(send(s, PICK_UP, cell, loot));
            assertTrue(send(s, WAIT, 0, null));
            assertTrue(send(s, PICK_UP, cell, loot));
            assertNull(Dungeon.level.heaps.get(cell));
            assertSame(loot, host.belongings.getItem(Shortsword.class));
            assertNull(guest.belongings.getItem(Shortsword.class));
            assertFalse(send(s, PICK_UP, cell, loot));
        }
    }

    @Test(timeout = 30000)
    public void unsupportedActorStopsBeforeItsFirstAction() {
        try (CoopSession s = open(402)) {
            Crab crab = new Crab();
            crab.pos = neighbour(s.hero(1));
            GameScene.add(crab);
            int before = crab.pos;
            assertTrue(send(s, WAIT, 0, null));
            assertThrows(UnsupportedOperationException.class, () -> send(s, WAIT, 0, null));
            assertTrue(s.stopped());
            assertTrue(s.stopReason().contains("Crab"));
            assertEquals(before, crab.pos);
            assertFalse(s.act(71, s.turn(), WAIT, 0, null));
        }
    }

    @Test(timeout = 30000)
    public void guestKillsRatWithoutCreditingTheOtherSeat() {
        try (CoopSession session = open(402)) {
            Hero host = session.hero(0), guest = session.hero(1);
            assertTrue(send(session, WAIT, 0, null));
            Rat rat = new Rat();
            rat.pos = neighbour(guest);
            rat.HP = 1;
            GameScene.add(rat);
            int hostXP = host.exp, guestXP = guest.exp;
            long offered = session.turn();
            Random.pushGenerator(402);
            try {
                assertTrue(send(session, ATTACK, rat.pos, null));
            } finally {
                Random.popGenerator();
            }
            assertFalse(rat.isAlive());
            assertFalse(Dungeon.level.mobs.contains(rat));
            assertFalse(Actor.all().contains(rat));
            assertEquals(hostXP, host.exp);
            assertEquals(guestXP + rat.EXP, guest.exp);
            assertFalse(session.act(94, offered, ATTACK, rat.pos, null));
            assertEquals(guestXP + rat.EXP, guest.exp);
        }
    }

    @Test(timeout = 30000)
    public void sessionRejectsForeignExecutionAndPreservesSinglePlayerStorage() throws Exception {
        String solo = FileUtils.basePath();
        Badges.loadGlobal();
        int badges = Badges.totalUnlocked(true), foodUses = Catalog.useCount(Food.class);
        String slot = GamesInProgress.gameFile(1);
        FileUtils.getFileHandle(slot).writeString("existing single player slot", false);
        try (CoopSession s = open(402)) {
            Level level = Dungeon.level;
            long offered = s.turn();
            AtomicReference<Throwable> problem = new AtomicReference<>();
            Thread foreign = new Thread(() -> {
                try {
                    s.act(71, offered, WAIT, 0, null);
                } catch (Throwable e) { problem.set(e); }
            });
            foreign.start();
            foreign.join(3000);
            assertFalse(foreign.isAlive());
            assertTrue(problem.get() instanceof IllegalStateException);
            assertThrows(IllegalStateException.class, Actor::process);
            assertThrows(UnsupportedOperationException.class, Dungeon::newLevel);
            assertThrows(UnsupportedOperationException.class, Dungeon::saveAll);
            assertThrows(UnsupportedOperationException.class, () -> Dungeon.deleteGame(1, true));
            assertSame(level, Dungeon.level);
            assertEquals(offered, s.turn());
            assertTrue(send(s, EAT, 0, s.hero(0).belongings.getItem(Food.class)));
            assertEquals(foodUses + 1, Catalog.useCount(Food.class));
            s.hero(1).damage(10000, this);
            assertFalse(s.stopped());
            assertEquals(CoopSession.Life.DEAD, s.life(1));
            s.finishEffects();
            assertEquals(0, s.activeSeat());
            assertTrue(send(s, WAIT, 0, null));
            assertFalse(s.act(94, s.turn(), WAIT, 0, null));
            assertFalse(s.act(71, offered, WAIT, 0, null));
        }
        assertFalse(Actor.processing());
        assertEquals(solo, FileUtils.basePath());
        assertEquals(badges, Badges.totalUnlocked(true));
        assertEquals(foodUses, Catalog.useCount(Food.class));
        assertEquals("existing single player slot", FileUtils.getFileHandle(slot).readString());
    }

    @Test(timeout = 30000)
    public void twoSeatsCompleteOneHundredTwentyActionsOnOneEngineThread() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread engine = new Thread(() -> {
            try {
                for (long seed : new long[]{402, 2402, 3402}) {
                    try (CoopSession s = open(seed)) {
                        long previous = 0;
                        float clock = 0;
                        int[] guestRoute = {s.hero(1).pos, neighbour(s.hero(1))};
                        for (int i = 0; i < 120; i++) {
                            assertEquals(i % 2, s.activeSeat());
                            assertEquals(previous + 1, s.turn());
                            previous = s.turn();
                            assertTrue(Actor.now() >= clock);
                            clock = Actor.now();
                            if (s.activeSeat() == 1) {
                                Hero guest = s.hero(1);
                                int dst = guest.pos == guestRoute[0] ? guestRoute[1] : guestRoute[0];
                                assertTrue(send(s, MOVE, dst, null));
                                assertEquals(dst, guest.pos);
                            } else {
                                assertTrue(send(s, WAIT, 0, null));
                            }
                            for (int seat = 0; seat < 2; seat++) {
                                Hero h = s.hero(seat);
                                assertSame(h, Actor.findChar(h.pos));
                                assertEquals(1, h.belongings.getItem(Food.class).quantity());
                            }
                        }
                        assertEquals(60, Actor.now(), 0.001f);
                    }
                }
            } catch (Throwable t) { failure.set(t); }
        }, "coop-engine-integration");
        engine.setDaemon(true);
        engine.start();
        engine.join(20000);
        assertFalse("Authoritative scheduler did not finish", engine.isAlive());
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
}
