package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;

/** Station crafting on the room executor. The inventory picker never detaches ingredients. */
public final class CoopAlchemy {
    private CoopAlchemy() {}

    /** No map cell has this index. The accepted state version binds the equipped kit to the offer. */
    public static final int PORTABLE = -1;

    public static AlchemistsToolkit portable(CoopSession game, Hero hero) {
        for (Item equipped : new Item[]{hero.belongings.artifact, hero.belongings.misc}) {
            if (equipped instanceof AlchemistsToolkit && game.ownership.owner(equipped) == game.playerOf(hero)) {
                AlchemistsToolkit kit = (AlchemistsToolkit)equipped;
                if (kit.readyToBrew(hero)) return kit;
            }
        }
        return null;
    }

    private static boolean accessible(CoopSession game, Hero hero, int cell, AlchemistsToolkit kit) {
        return cell == PORTABLE ? kit != null : atStation(game, hero, cell);
    }

    private static boolean funded(AlchemistsToolkit kit, int cost) {
        return cost >= 0 && Math.max(0, cost - (kit == null ? 0 : kit.availableEnergy())) <= Dungeon.energy;
    }

    private static void pay(Hero hero, AlchemistsToolkit kit, int cost) {
        int shared = kit == null ? cost : kit.consumeEnergy(hero, cost);
        com.shatteredpixel.shatteredpixeldungeon.journal.Catalog.countUses(
                com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal.class, shared);
        Dungeon.energy -= shared;
    }

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

    public static boolean isSeed(Item item) {
        return item != null && Potion.SeedToPotion.types.containsKey(item.getClass());
    }

    static boolean brewSeeds(CoopSession game, Hero hero, int cell, Item[] selected) {
        AlchemistsToolkit kit = cell == PORTABLE ? portable(game,hero) : null;
        if (selected == null || selected.length != 3 || !accessible(game, hero, cell, kit)) return false;
        Map<Item, Integer> counts = new IdentityHashMap<>();
        ArrayList<Item> units = new ArrayList<>(3);
        for (Item seed : selected) {
            if (!isSeed(seed) || game.ownership.owner(seed) != game.playerOf(hero)
                    || !hero.belongings.backpack.contains(seed)) return false;
            if (counts.merge(seed, 1, Integer::sum) > seed.quantity()) return false;
        }
        for (Item seed : selected) units.add(seed.duplicate().quantity(1));
        Potion.SeedToPotion recipe = new Potion.SeedToPotion();
        if (!recipe.testIngredients(units) || !funded(kit, recipe.cost(units))) return false;
        int cost = recipe.cost(units);
        Item result = recipe.brew(units);
        if (result == null) throw new IllegalStateException("Seed recipe yielded no output");
        for (Item seed : selected) {
            Integer count = counts.remove(seed);
            if (count == null) continue;
            seed.quantity(seed.quantity() - count);
            if (seed.quantity() == 0) seed.detachAll(hero.belongings.backpack);
        }
        pay(hero, kit, cost);
        collectResult(game, hero, result);
        return true;
    }

    public static boolean canBrewEnchantment(Item item) {
        return item instanceof ScrollOfUpgrade && ((ScrollOfUpgrade)item).isKnown();
    }

    private static Recipe exoticRecipe(Item item) {
        if (item == null || !item.isIdentified() || !Recipe.usableInRecipe(item)
                || !(item instanceof Potion || item instanceof Scroll)) return null;
        ArrayList<Item> input = ingredients(item);
        for (Recipe candidate : Recipe.findRecipes(input)) {
            if (candidate instanceof ExoticPotion.PotionToExotic || candidate instanceof ExoticScroll.ScrollToExotic)
                return candidate;
        }
        return null;
    }

    public static String exoticPreview(Item item) {
        Recipe recipe = exoticRecipe(item);
        if (recipe == null) return null;
        ArrayList<Item> input = ingredients(item);
        Item output = recipe.sampleOutput(input);
        String key = CoopTrade.label(output);
        return key.isEmpty() ? null : key + "," + output.image() + "," + recipe.cost(input);
    }

    static boolean brewExotic(CoopSession game, Hero hero, int cell, Item item) {
        return craft(game, hero, cell, item, exoticRecipe(item));
    }

    static boolean brew(CoopSession game, Hero hero, int cell, Item item) {
        return brew(game, hero, cell, item, false);
    }

    static boolean brew(CoopSession game, Hero hero, int cell, Item item, boolean exotic) {
        if (!(item instanceof Scroll) || exotic && !canBrewEnchantment(item)) return false;
        return craft(game, hero, cell, item, exotic ? new ExoticScroll.ScrollToExotic() : recipe(ingredients(item)));
    }

    private static boolean craft(CoopSession game, Hero hero, int cell, Item item, Recipe recipe) {
        AlchemistsToolkit kit = cell == PORTABLE ? portable(game,hero) : null;
        if (recipe == null || item == null || item.quantity() <= 0 || !accessible(game, hero, cell, kit)
                || !Recipe.usableInRecipe(item) || !hero.belongings.backpack.contains(item)
                || game.ownership.owner(item) != game.playerOf(hero)) return false;
        ArrayList<Item> input = ingredients(item);
        if (!recipe.testIngredients(input)) return false;
        int cost = recipe.cost(input);
        if (!funded(kit, cost)) return false;
        Item output = recipe.brew(input);
        if (output == null) throw new IllegalStateException("Accepted alchemy recipe yielded no output");
        pay(hero, kit, cost);
        if (item.quantity() == 0) item.detachAll(hero.belongings.backpack);
        collectResult(game, hero, output);
        return true;
    }

    private static void collectResult(CoopSession game, Hero hero, Item output) {
        game.ownership.claim(output, game.playerOf(hero));
        if (!output.collect(hero.belongings.backpack)) Dungeon.level.drop(output, hero.pos);
        Statistics.itemsCrafted++;
        Badges.validateItemsCrafted();
    }
}
