package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Test-only positions and budgets in copies of the genuine sixth-floor shop. */
public final class PrisonShopChecks {
    private PrisonShopChecks() {}

    public static void copiesOf(Bundle original) {
        for (int seat=0;seat<2;seat++) {
            try (CoopSession game=CoopSession.restore(original)) {
                Hero hero=game.hero(seat);
                Heap stock=Dungeon.level.heaps.valueList().stream().filter(h->h.type==Heap.Type.FOR_SALE
                        && h.peek() instanceof Armor).findFirst().get();
                SewerBeeTest.offer(game,seat);
                hero.pos=stock.pos; game.observe(seat);
                Item armor=stock.peek(); long turn=game.turn(); int price=Shopkeeper.sellPrice(armor);
                Dungeon.gold=price-1;
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.BUY,stock.pos,armor));
                assertEquals(price-1,Dungeon.gold); assertSame(armor,stock.peek()); assertEquals(turn,game.turn());
                while (hero.belongings.backpack.items.size()<hero.belongings.backpack.capacity())
                    assertTrue(new ClothArmor().collect(hero.belongings.backpack));
                Dungeon.gold=price;
                assertFalse(CoopTrade.roomFor(hero,armor));
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.BUY,stock.pos,armor));
                assertSame(armor,stock.peek()); assertEquals(turn,game.turn()); assertEquals(price,Dungeon.gold);
            }
            try (CoopSession game=CoopSession.restore(original)) {
                Hero hero=game.hero(seat);
                Shopkeeper shop=CoopTrade.merchant(hero,false);
                SewerSpecialActorTest.atGate(game,shop.pos);
                assertTrue(shop.buybackItems.isEmpty());
                assertTrue(new SmallRation().quantity(5).collect(hero.belongings.backpack));
                Item carried=hero.belongings.getItem(SmallRation.class);
                int originalCount=carried.quantity(),money=Dungeon.gold;
                int value=carried.value()/originalCount;
                long physical=game.ownership.identity(carried);
                Item first=null;
                for (int i=0;i<4;i++) {
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SELL_ONE,shop.pos,carried));
                    Item split=shop.buybackItems.get(shop.buybackItems.size()-1);
                    if (first==null) first=split;
                    assertNotEquals(physical,game.ownership.identity(split));
                    assertEquals(game.player(seat),game.ownership.owner(split));
                    assertEquals(money+(i+1)*value,Dungeon.gold);
                    assertEquals(originalCount-i-1,carried.quantity());
                }
                assertEquals(3,shop.buybackItems.size()); assertFalse(shop.buybackItems.contains(first));
                Item retained=shop.buybackItems.get(0);
                SewerBeeTest.offer(game,1-seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.BUYBACK,shop.pos,retained));
                SewerBeeTest.offer(game,seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.BUYBACK,shop.pos,first));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.BUYBACK,shop.pos,retained));
                assertEquals(2,shop.buybackItems.size()); assertEquals(originalCount-3,carried.quantity());
                assertEquals(money+3*value,Dungeon.gold);
            }
        }
    }
}
