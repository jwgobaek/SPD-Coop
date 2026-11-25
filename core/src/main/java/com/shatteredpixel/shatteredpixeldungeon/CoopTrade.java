package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTradeItem;

/** The original shop holds its own three buyback items. Only their owner gets a quote. */
public final class CoopTrade {
    private CoopTrade() {}

    public static Shopkeeper merchant(Hero hero, boolean adjacent) {
        for (Mob mob:Dungeon.level.mobs) {
            if (mob instanceof Shopkeeper && mob.isAlive() && (!adjacent || mob.canInteract(hero))) return (Shopkeeper)mob;
        }
        return null;
    }

    public static boolean supported(Item item) {
        return item instanceof Food || item instanceof Potion || item instanceof Scroll || item instanceof Armor
                || item instanceof Weapon || item instanceof Ankh || item instanceof Runestone
                || item instanceof Ring || item instanceof Wand || item.getClass()==Honeypot.class;
    }

    /** Localisation key for the base type only when the recipient is allowed to know it. */
    public static String label(Item item) {
        if (item instanceof Potion && !((Potion)item).isKnown() || item instanceof Scroll && !((Scroll)item).isKnown()
                || item instanceof Ring && !((Ring)item).isKnown() || item.getClass().getEnclosingClass()!=null) return "";
        String name=item.getClass().getName();
        String prefix="com.shatteredpixel.shatteredpixeldungeon.";
        return name.startsWith(prefix+"items.") ? name.substring(prefix.length()).toLowerCase(java.util.Locale.ROOT) : "";
    }

    public static boolean maySell(Hero hero, Item item) {
        return supported(item) && hero.belongings.contains(item) && Shopkeeper.canSell(hero,item)
                && (!item.isEquipped(hero) || CoopSession.equipmentForScroll(item));
    }

    public static boolean sellOne(Item item) {
        return item.quantity()>1 && !(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon && item.isUpgradable());
    }

    public static boolean roomFor(Hero hero, Item item) {
        for (Bag bag:hero.belongings.getBags()) if (bag.canHold(item)) return true;
        return false;
    }

    static boolean execute(CoopSession game, Hero hero, CoopSession.Action action, int cell, Item item) {
        if (!supported(item) || !Dungeon.level.insideMap(cell) || !game.knowledge(hero).visible(cell)) return false;
        boolean buy=action==CoopSession.Action.BUY;
        Shopkeeper shop=merchant(hero,!buy);
        if (shop==null) return false;
        if (buy) {
            Heap heap=Dungeon.level.heaps.get(cell);
            if (hero.pos!=cell || heap==null || heap.hidden || heap.type!=Heap.Type.FOR_SALE || heap.size()!=1
                    || heap.peek()!=item || Shopkeeper.sellPrice(item)>Dungeon.gold || !roomFor(hero,item)) return false;
            WndTradeItem.buy(hero,heap);
        } else {
            if (cell!=shop.pos || game.ownership.owner(item)!=game.playerOf(hero)) return false;
            if (action==CoopSession.Action.BUYBACK) {
                if (!shop.buybackItems.contains(item) || Dungeon.gold<item.value() || !roomFor(hero,item)) return false;
                shop.buyBack(hero,item);
            } else {
                if (!maySell(hero,item)) return false;
                if (action==CoopSession.Action.SELL_ONE) {
                    if (!sellOne(item)) return false;
                    WndTradeItem.sellOne(hero,item,shop);
                } else WndTradeItem.sell(hero,item,shop);
            }
        }
        return true;
    }
}
