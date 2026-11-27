package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import java.util.ArrayList;

/** Station crafting on the room executor. The inventory picker never detaches ingredients. */
public final class CoopAlchemy {
    private CoopAlchemy() {}

    public static boolean atStation(CoopSession game, Hero hero, int cell) {
        if (!Dungeon.level.insideMap(cell) || Dungeon.level.map[cell] != Terrain.ALCHEMY
                || Dungeon.level.distance(hero.pos, cell) > 1 || !game.knowledge(hero).visible(cell)) return false;
        AlchemistsToolkit.kitEnergy kit = hero.buff(AlchemistsToolkit.kitEnergy.class);
        return kit == null || !kit.isCursed();
    }

    private static Scroll.ScrollToStone recipe(ArrayList<Item> ingredients) {
        for (Recipe candidate : Recipe.findRecipes(ingredients)) {
            if (candidate instanceof Scroll.ScrollToStone) return (Scroll.ScrollToStone)candidate;
        }
        return null;
    }

    private static ArrayList<Item> ingredients(Item item) {
        ArrayList<Item> ingredients = new ArrayList<>(1);
        ingredients.add(item);
        return ingredients;
    }

    public static String preview(Item item) {
        if (!(item instanceof Scroll) || !Recipe.usableInRecipe(item)) return null;
        ArrayList<Item> input = ingredients(item);
        Recipe recipe = recipe(input);
        if (recipe == null) return null;
        Item output = recipe.sampleOutput(input);
        String type = output instanceof Runestone.PlaceHolder ? "unknown" : output.getClass().getSimpleName();
        return type + "," + output.image() + "," + output.quantity() + "," + recipe.cost(input);
    }

    static boolean brew(CoopSession game, Hero hero, int cell, Item item) {
        if (!atStation(game, hero, cell) || !(item instanceof Scroll) || item.quantity() <= 0
                || !Recipe.usableInRecipe(item) || !hero.belongings.backpack.contains(item)
                || game.ownership.owner(item) != game.playerOf(hero)) return false;
        ArrayList<Item> input = ingredients(item);
        Recipe recipe = recipe(input);
        if (recipe == null) return false;
        int cost = recipe.cost(input);
        if (cost > Dungeon.energy) return false;
        Item output = recipe.brew(input);
        if (output == null) throw new IllegalStateException("Accepted alchemy recipe yielded no output");
        Dungeon.energy -= cost;
        if (item.quantity() == 0) item.detachAll(hero.belongings.backpack);
        game.ownership.claim(output, game.playerOf(hero));
        if (!output.collect(hero.belongings.backpack)) Dungeon.level.drop(output, hero.pos);
        Statistics.itemsCrafted++;
        Badges.validateItemsCrafted();
        return true;
    }
}
