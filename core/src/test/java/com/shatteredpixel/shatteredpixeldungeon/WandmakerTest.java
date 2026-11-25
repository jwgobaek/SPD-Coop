package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Rotberry;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;

/** Isolated quest fixtures. Positions, combat stats and the already-open sewer exit are arranged. */
public class WandmakerTest extends HeadlessDungeon {
    @Test public void nativeMassGrave() { exercise(572, CorpseDust.class); }
    @Test public void nativeRitual() { exercise(575, Embers.class); }
    @Test public void nativeGarden() { exercise(577, Rotberry.Seed.class); }

    static void offered(CoopSession game, int seat) {
        while (game.activeSeat() != seat) SewerSpecialActorTest.waitOne(game);
    }

    static void beside(CoopSession game, int seat, int cell) {
        Level level = Dungeon.level;
        for (int offset : PathFinder.NEIGHBOURS8) {
            int next = cell + offset;
            if (level.passable[next] && !level.pit[next] && Actor.findChar(next) == null && level.traps.get(next) == null) {
                game.hero(seat).pos = next;
                game.observe(seat);
                level.updateFieldOfView(game.hero(seat), game.hero(seat).fieldOfView);
                return;
            }
        }
        throw new AssertionError("No test placement at " + cell);
    }

    private static void act(CoopSession game, int seat, CoopSession.Action action, int cell, Item item) {
        offered(game, seat);
        assertTrue(action+" at "+cell, CoopFloorsTest.step(game, action, cell, item));
        assertFalse(game.stopReason(), game.stopped());
    }

    public static void enterPrison(CoopSession game) {
        for (int seat = 0; seat < 2; seat++) {
            Hero hero = game.hero(seat);
            hero.HT = hero.HP = 500;
            hero.belongings.weapon().upgrade(5);
        }
        while (Dungeon.depth < 7) {
            if (Dungeon.depth == 5) Level.set(Dungeon.level.exit(), Terrain.UNLOCKED_EXIT);
            SewerDepthTest.gate(game, true);
        }
    }

    private static Heap heap(Class<? extends Item> type) {
        for (Heap heap : Dungeon.level.heaps.valueList())
            if (type.isInstance(heap.peek())) return heap;
        throw new AssertionError("No native drop " + type.getSimpleName());
    }

    private static void pickup(CoopSession game, int seat, Heap heap) {
        offered(game, seat);
        if (Actor.findChar(heap.pos) instanceof Mob) {
            // A haunted bone pile spawns a separate guardian on the loot.
            // Keep the guardian in the scheduler and separate the two fixture cells.
            Mob guardian=(Mob)Actor.findChar(heap.pos);
            for (int offset : PathFinder.NEIGHBOURS8) {
                int next=heap.pos+offset;
                if (Dungeon.level.passable[next] && Actor.findChar(next)==null) {
                    guardian.pos=next;
                    break;
                }
            }
            assertNotEquals(heap.pos,guardian.pos);
        }
        game.hero(seat).pos = heap.pos;
        game.observe(seat);
        act(game, seat, CoopSession.Action.PICK_UP, heap.pos, heap.peek());
    }

    private static void combat(CoopSession game, int seat, Mob mob) {
        for (int i = 0; mob.isAlive() && i < 45; i++) {
            offered(game,seat);
            if (!mob.isAlive()) break;
            beside(game, seat, mob.pos);
            act(game,seat,CoopSession.Action.ATTACK,mob.pos,null);
        }
        assertFalse(mob.isAlive());
    }

    private static void objective(CoopSession game, int seat, Class<? extends Item> type) {
        if (type == CorpseDust.class) {
            Heap bones = heap(type);
            assertEquals(Heap.Type.SKELETON, bones.type);
            assertTrue(bones.haunted);
            offered(game,seat);
            beside(game,seat,bones.pos);
            act(game, seat, CoopSession.Action.OPEN, bones.pos, null);
        } else if (type == Embers.class) {
            for (int i = 0; i < 4; i++) {
                int cell = CeremonialCandle.ritualPos + PathFinder.NEIGHBOURS4[i];
                int holder = i%2;
                Heap candle = null;
                for (Heap floor : Dungeon.level.heaps.valueList()) {
                    if (floor.peek() instanceof CeremonialCandle && !((CeremonialCandle)floor.peek()).aflame) {
                        candle = floor;
                        break;
                    }
                }
                assertNotNull(candle);
                pickup(game, holder, candle);
                offered(game, holder);
                game.hero(holder).pos = cell;
                game.observe(holder);
                act(game,holder,CoopSession.Action.DROP,cell,game.hero(holder).belongings.getItem(CeremonialCandle.class));
            }
            combat(game,seat,SewerSpecialActorTest.actor(Elemental.NewbornFireElemental.class));
        } else {
            combat(game,seat,SewerSpecialActorTest.actor(RotHeart.class));
            for (Mob mob : Dungeon.level.mobs) assertFalse(mob instanceof RotLasher);
        }
        pickup(game,seat,heap(type));
        if (type == CorpseDust.class) {
            assertNotNull(game.hero(seat).buff(CorpseDust.DustGhostSpawner.class));
            assertNull(game.hero(1-seat).buff(CorpseDust.DustGhostSpawner.class));
            for (int n = 0; n < 24 && Dungeon.level.mobs.stream().noneMatch(m -> m instanceof CorpseDust.DustWraith); n++)
                SewerSpecialActorTest.waitOne(game);
            SewerSpecialActorTest.actor(CorpseDust.DustWraith.class);
        }
    }

    public static void prepareReturn(CoopSession game, int seed, int seat) {
        enterPrison(game);
        objective(game, seat, seed == 572 ? CorpseDust.class : seed == 575 ? Embers.class : Rotberry.Seed.class);
        Wandmaker npc = SewerSpecialActorTest.actor(Wandmaker.class);
        offered(game,seat);
        beside(game,seat,npc.pos);
        beside(game,1-seat,npc.pos);
    }

    @Test public void fullBagRewardKeepsItsPhysicalIdentityOnTheFloorForTheClaimant() {
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            long id;
            int cell;
            try (CoopSession game=CoopSession.open(577,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.MAGE);
                enterPrison(game);
                Wandmaker npc=SewerSpecialActorTest.actor(Wandmaker.class);
                cell=npc.pos;
                offered(game,seat); beside(game,seat,cell);
                act(game,seat,CoopSession.Action.ACCEPT_WANDMAKER,cell,null);
                objective(game,seat,Rotberry.Seed.class);
                Hero owner=game.hero(seat);
                // Keep the objective stack's slot occupied when the first seed is consumed.
                owner.belongings.getItem(Rotberry.Seed.class).quantity(2);
                while (owner.belongings.backpack.canHold(new com.shatteredpixel.shatteredpixeldungeon.items.Ankh()))
                    assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.Ankh().collect(owner.belongings.backpack));
                Wand reward=Wandmaker.Quest.wand1;
                offered(game,seat); beside(game,seat,cell);
                act(game,seat,CoopSession.Action.WAND_FIRST,cell,null);
                assertEquals(1,owner.belongings.getItem(Rotberry.Seed.class).quantity());
                assertFalse(owner.belongings.contains(reward));
                assertTrue(Dungeon.level.heaps.get(cell).items.contains(reward));
                assertEquals(game.player(seat),game.ownership.owner(reward));
                id=game.ownership.identity(reward);
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Item wand=Dungeon.level.heaps.get(cell).peek();
                assertEquals(id,game.ownership.identity(wand));
                assertFalse(game.ownership.allowed(wand,game.player(1-seat)));
                assertNull(Wandmaker.Quest.wand1);
                assertNull(Wandmaker.Quest.wand2);
            }
        }
    }

    @Test public void returningDustDispelsActiveWraithsAndPreservesTheOriginalParkedFloor() {
        try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.MAGE);
            enterPrison(game);
            Wandmaker npc=SewerSpecialActorTest.actor(Wandmaker.class);
            offered(game,1); beside(game,1,npc.pos);
            act(game,1,CoopSession.Action.ACCEPT_WANDMAKER,npc.pos,null);
            objective(game,1,CorpseDust.class);
            SewerDepthTest.gate(game,false);
            for (int i=0;i<40 && Dungeon.level.mobs.stream().noneMatch(m->m instanceof CorpseDust.DustWraith);i++)
                SewerSpecialActorTest.waitOne(game);
            int parked=SewerSpecialActorTest.actor(CorpseDust.DustWraith.class).id();
            SewerDepthTest.gate(game,true);
            assertNull(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findById(parked));
            offered(game,1); npc=SewerSpecialActorTest.actor(Wandmaker.class); beside(game,1,npc.pos);
            act(game,1,CoopSession.Action.WAND_SECOND,npc.pos,null);
            assertNull(game.hero(1).buff(CorpseDust.DustGhostSpawner.class));
            assertTrue(Dungeon.level.mobs.stream().noneMatch(m->m instanceof CorpseDust.DustWraith));
            SewerDepthTest.gate(game,false);
            assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findById(parked) instanceof CorpseDust.DustWraith);
            assertNull(game.hero(1).buff(CorpseDust.DustGhostSpawner.class));
        }
    }

    private void exercise(int seed, Class<? extends Item> type) {
        Bundle start;
        try (CoopSession game = CoopSession.open(seed,11,HeroClass.WARRIOR)) {
            game.join(12, HeroClass.MAGE);
            enterPrison(game);
            start = game.checkpoint();
        }
        for (int seat = 0; seat < 2; seat++) {
            Bundle progress;
            try (CoopSession game = CoopSession.restore(start)) {
                Wandmaker npc = SewerSpecialActorTest.actor(Wandmaker.class);
                offered(game,seat);
                beside(game,seat,npc.pos);
                act(game,seat,CoopSession.Action.ACCEPT_WANDMAKER,npc.pos,null);
                objective(game,seat,type);
                assertTrue(Wandmaker.Quest.offerFor(game.hero(seat)).contains(",REWARD,"));
                assertTrue(Wandmaker.Quest.offerFor(game.hero(1-seat)).endsWith(",HUNT"));
                int itemId = npc.id();
                offered(game, 1-seat);
                beside(game, 1-seat, npc.pos);
                assertFalse(CoopFloorsTest.step(game, CoopSession.Action.WAND_FIRST, npc.pos, null));
                SewerDepthTest.gate(game, false);
                assertNull(Actor.findById(itemId));
                SewerDepthTest.gate(game, true);
                assertTrue(Actor.findById(itemId) instanceof Wandmaker);
                progress = game.checkpoint();
            }
            Bundle completed;
            long rewardID;
            Class<? extends Wand> selected;
            try (CoopSession game = CoopSession.restore(progress)) {
                Wandmaker npc = SewerSpecialActorTest.actor(Wandmaker.class);
                offered(game,seat);
                beside(game,seat,npc.pos);
                Wand reward = seat == 0 ? Wandmaker.Quest.wand1 : Wandmaker.Quest.wand2;
                selected = reward.getClass();
                act(game,seat,seat == 0 ? CoopSession.Action.WAND_FIRST : CoopSession.Action.WAND_SECOND,npc.pos,null);
                assertNull(Wandmaker.Quest.wand1);
                assertNull(game.hero(seat).belongings.getItem(type));
                assertTrue(game.hero(seat).belongings.contains(reward));
                assertEquals(game.player(seat),game.ownership.owner(reward));
                assertNull(game.hero(seat).buff(CorpseDust.DustGhostSpawner.class));
                rewardID = game.ownership.identity(reward);
                completed = game.checkpoint();
            }
            try (CoopSession game = CoopSession.restore(completed)) {
                assertNull(Wandmaker.Quest.wand2);
                assertEquals(rewardID, game.ownership.identity(game.hero(seat).belongings.getItem(selected)));
                for (Mob mob : Dungeon.level.mobs) assertFalse(mob instanceof Wandmaker || mob instanceof CorpseDust.DustWraith);
                SewerSpecialActorTest.waitOne(game);
            }
        }
    }
}
