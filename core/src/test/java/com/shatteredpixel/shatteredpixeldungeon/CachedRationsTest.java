package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class CachedRationsTest extends HeadlessDungeon {
    private static Heap cache() {
        Heap found = null;
        for (Heap heap : Dungeon.level.heaps.valueList()) {
            if (heap.peek() instanceof SupplyRation) {
                assertNull("duplicate ration on one floor", found);
                found = heap;
            }
        }
        return found;
    }

    static StagedInput preparePickup(CoopSession game, int seat) {
        Hero owner = game.hero(seat);
        owner.lvl = 5;
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.CACHED_RATIONS.ordinal(),null));
        while (Dungeon.depth < 4 && cache() == null) SewerDepthTest.gate(game,true);
        Heap heap = cache();
        assertNotNull(heap);
        SewerSpecialActorTest.atGate(game,heap.pos);
        SewerBeeTest.offer(game,seat);
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.OPEN,heap.pos,null));
        SewerBeeTest.offer(game,seat);
        owner.pos=heap.pos;
        game.observe(seat);
        return new StagedInput(game,"PICK_UP",heap.pos);
    }

    static boolean ownsCollectedCache(CoopSession game,int seat) {
        Hero owner = game.hero(seat);
        SupplyRation ration = owner.belongings.getItem(SupplyRation.class);
        return ration != null && ration.quantity() == 1 && cache() == null && game.ownership.owner(ration) == game.player(seat)
                && owner.buff(Talent.CachedRationsDropped.class).count() == 2
                && game.hero(1-seat).belongings.getItem(SupplyRation.class) == null;
    }

    @Test public void onlyTheRogueOwnsTheNativeChestAcrossFloorRevisits() {
        for (int seat = 0; seat < 2; seat++) for (int rank = 0; rank <= 2; rank++) {
            Bundle save;
            long identity;
            int cell;
            try (CoopSession game = CoopSession.open(402, 11, seat == 0 ? HeroClass.ROGUE : HeroClass.MAGE)) {
                game.join(22, seat == 1 ? HeroClass.ROGUE : HeroClass.MAGE);
                Hero owner = game.hero(seat);
                owner.lvl = 5;
                SewerBeeTest.offer(game, seat);
                assertTrue(ProjectionChecks.seatImage(game, seat).source.rows.containsKey("learn"+Talent.CACHED_RATIONS.ordinal()));
                for (int n = 0; n < rank; n++)
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.CACHED_RATIONS.ordinal(), null));
                SewerDepthTest.gate(game, true);
                if (rank == 0) {
                    assertNull(cache());
                    assertNull(owner.buff(Talent.CachedRationsDropped.class));
                    continue;
                }
                Heap heap = cache();
                assertNotNull("expected eligible special room on tested seed", heap);
                assertEquals(Heap.Type.CHEST, heap.type);
                cell = heap.pos;
                var room = ((RegularLevel)Dungeon.level).room(cell);
                assertTrue(room instanceof SpecialRoom);
                assertFalse(room instanceof ShopRoom || room instanceof SecretRoom);
                assertEquals(game.player(seat), game.ownership.owner(heap.peek()));
                identity = game.ownership.identity(heap.peek());
                assertEquals(2, owner.buff(Talent.CachedRationsDropped.class).count(), 0);
                assertNull(game.hero(1-seat).buff(Talent.CachedRationsDropped.class));
                SewerDepthTest.gate(game, false);
                save = game.checkpoint();
            }
            try (CoopSession game = CoopSession.restore(save)) {
                SewerDepthTest.gate(game, true);
                Heap heap = cache();
                assertNotNull(heap);
                assertEquals(cell, heap.pos);
                assertEquals(identity, game.ownership.identity(heap.peek()));
                assertEquals(game.player(seat), game.ownership.owner(heap.peek()));
                assertEquals(2, game.hero(seat).buff(Talent.CachedRationsDropped.class).count(), 0);
                SewerSpecialActorTest.atGate(game, cell);
                SewerBeeTest.offer(game, seat);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.OPEN, cell, null));
                SewerBeeTest.offer(game, 1-seat);
                Hero peer = game.hero(1-seat);
                int previous = peer.pos;
                peer.pos = cell;
                game.observe(1-seat);
                assertFalse(CoopFloorsTest.step(game, CoopSession.Action.PICK_UP, cell, heap.peek()));
                peer.pos = previous;
                game.observe(1-seat);
                SewerBeeTest.offer(game, seat);
                Hero owner = game.hero(seat);
                owner.pos = cell;
                game.observe(seat);
                SupplyRation ration = (SupplyRation)heap.peek();
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.PICK_UP, cell, ration));
                assertSame(ration, owner.belongings.getItem(SupplyRation.class));
                SewerBeeTest.offer(game, seat);
                owner.HP = owner.HT - 9;
                int before = owner.HP;
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.EAT, 0, ration));
                assertTrue("native healing", owner.HP >= before + 5);
                assertNull(owner.belongings.getItem(SupplyRation.class));
                SewerDepthTest.gate(game, true);
                assertNull(cache());
                SewerDepthTest.gate(game, true);
                assertNotNull(cache());
                assertEquals(4, owner.buff(Talent.CachedRationsDropped.class).count(), 0);
            }
        }
    }

    @Test public void twoRoguesCannotInvestBeforeAResourcePolicyIsSelected() {
        try (CoopSession game = CoopSession.open(402, 11, HeroClass.ROGUE)) {
            game.join(22, HeroClass.ROGUE);
            for (int seat = 0; seat < 2; seat++) {
                SewerBeeTest.offer(game, seat);
                game.hero(seat).lvl = 5;
                assertFalse(ProjectionChecks.seatImage(game, seat).source.rows.containsKey("learn"+Talent.CACHED_RATIONS.ordinal()));
                assertFalse(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.CACHED_RATIONS.ordinal(), null));
            }
            SewerDepthTest.gate(game, true);
            assertNull(cache());
        }
    }
}
