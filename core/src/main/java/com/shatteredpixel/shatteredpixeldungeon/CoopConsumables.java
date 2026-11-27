package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.*;

public final class CoopConsumables {
    private CoopConsumables() {}

    public static boolean drinkable(Item item) {
        if (!(item instanceof Potion) || !((Potion)item).isKnown()) return false;
        return item instanceof PotionOfHealing || item instanceof PotionOfHaste
                || item instanceof PotionOfInvisibility || item instanceof PotionOfPurity
                || item instanceof PotionOfLevitation || item instanceof PotionOfMindVision
                || item instanceof PotionOfExperience || item instanceof PotionOfStrength;
    }

    public static boolean throwablePotion(Item item) {
        return drinkable(item) && !(item instanceof PotionOfLevitation);
    }

    public static boolean canEnchantFromPack(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero, Item item) {
        return hero.belongings.backpack.contains(item) && CoopEnchantScroll.eligible(hero, item);
    }

    public static int augment(Item gear) {
        return gear instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
                ? ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon)gear).augment.ordinal()
                : ((com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)gear).augment.ordinal();
    }

    static boolean areaAllowed(Item item, int cell, Hero peer) {
        boolean blast = item instanceof StoneOfBlast;
        if (!blast && !(item instanceof StoneOfFlock)) return true;
        Level level = Dungeon.level;
        boolean[] open = BArray.not(level.solid,null);
        if (blast) BArray.or(open,level.flamable,open);
        PathFinder.buildDistanceMap(cell,open,blast ? 1 : 2);
        if (blast && peer.isAlive() && PathFinder.distance[peer.pos]!=Integer.MAX_VALUE) return false;
        for (int pos=0; pos<level.length(); pos++) {
            if (PathFinder.distance[pos]==Integer.MAX_VALUE) continue;
            if (blast) {
                // Heap explosions may trigger another bomb or shatter a hazardous potion.
                if (level.heaps.get(pos)!=null) return false;
            } else if (level.insideMap(pos) && !level.pit[pos] && Actor.findChar(pos)==null
                    && (level.traps.get(pos)!=null || level.plants.get(pos)!=null)) return false;
        }
        return true;
    }

    public static boolean thrownStone(Item item) {
        return item instanceof StoneOfFlock || item instanceof StoneOfBlast || item instanceof StoneOfDeepSleep || item instanceof StoneOfFear
                || item instanceof StoneOfBlink || item instanceof StoneOfClairvoyance
                || item instanceof StoneOfShock || item instanceof StoneOfAggression;
    }
}
