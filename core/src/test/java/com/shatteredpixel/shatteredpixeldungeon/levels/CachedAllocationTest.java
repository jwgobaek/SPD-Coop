package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation;
import org.junit.Test;
import static org.junit.Assert.*;

public class CachedAllocationTest extends HeadlessDungeon {
    // Compare distribution candidates in a disposable fixture. Normal two-rogue rooms offer neither.
    @Test public void oneRecipientAndSeparateNativeRollsHaveDifferentResourceTotals() {
        for (int recipients = 1; recipients <= 2; recipients++) {
            try (CoopSession game = CoopSession.open(402, 11, HeroClass.ROGUE)) {
                game.join(22, HeroClass.ROGUE);
                SewerDepthTest.gate(game,true);
                RegularLevel floor = (RegularLevel)Dungeon.level;
                for (int seat = 0; seat < 2; seat++)
                    game.hero(seat).talents.get(0).put(Talent.CACHED_RATIONS,2);
                for (int seat = 0; seat < recipients; seat++) floor.dropCachedRation(game.hero(seat));
                int[] quantities = new int[2];
                for (Heap heap : floor.heaps.valueList()) if (heap.peek() instanceof SupplyRation) {
                    assertEquals(Heap.Type.CHEST,heap.type);
                    long owner = game.ownership.owner(heap.peek());
                    assertTrue(owner == game.player(0) || owner == game.player(1));
                    quantities[owner == game.player(0) ? 0 : 1]++;
                }
                assertArrayEquals(new int[]{1,recipients-1}, quantities);
                for (int seat = 0; seat < 2; seat++) {
                    var counter = game.hero(seat).buff(Talent.CachedRationsDropped.class);
                    if (seat < recipients) assertEquals(2, counter.count(), 0);
                    else assertNull(counter);
                }
            }
        }
    }

    @Test public void nativeCounterGrantsAtMostTwoOrThreeEvenOnLaterEligibleDepths() {
        for (int rank = 1; rank <= 2; rank++) {
            try (CoopSession game = CoopSession.open(402, 11, HeroClass.MAGE)) {
                game.join(22,HeroClass.ROGUE);
                SewerDepthTest.gate(game,true);
                Hero owner = game.hero(1);
                owner.talents.get(0).put(Talent.CACHED_RATIONS,rank);
                RegularLevel floor = (RegularLevel)Dungeon.level;
                // Reuse the eligible native special room while controlling only the depth threshold.
                for (int depth = 2; depth <= 9; depth++) {
                    Dungeon.depth = depth;
                    floor.dropCachedRation(owner);
                    int expected = 2*Math.min(rank+1, depth/2);
                    assertEquals(expected,owner.buff(Talent.CachedRationsDropped.class).count(),0);
                }
            }
        }
    }
}
