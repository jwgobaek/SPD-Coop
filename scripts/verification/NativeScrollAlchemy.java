package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDeepSleep;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

/** Measures the native recipe and its unidentified preview before adding a cooperative station. */
public class NativeScrollAlchemy extends HeadlessDungeon {
    @Test public void lullabyRecipeHidesItsOutputUntilIdentificationAndConsumesOneScroll() {
        try (CoopSession game = CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            Scroll scroll = new ScrollOfLullaby();
            scroll.quantity(2);
            assertFalse(scroll.isKnown());

            ArrayList<Item> input = new ArrayList<>();
            input.add(scroll);
            Recipe recipe = Recipe.findRecipes(input).stream()
                    .filter(Scroll.ScrollToStone.class::isInstance)
                    .findFirst().orElseThrow(AssertionError::new);
            Item preview = recipe.sampleOutput(input);
            assertTrue(preview instanceof Runestone.PlaceHolder);
            assertEquals(2, preview.quantity());
            assertFalse(scroll.isKnown());
            assertEquals(2, scroll.quantity());
            assertEquals(0, recipe.cost(input));

            Item result = recipe.brew(input);
            assertEquals(StoneOfDeepSleep.class, result.getClass());
            assertEquals(2, result.quantity());
            assertEquals(1, scroll.quantity());
            assertTrue(scroll.isKnown());
            assertEquals(StoneOfDeepSleep.class, recipe.sampleOutput(input).getClass());

            input.clear();
            Item exotic = new ScrollOfEnchantment();
            input.add(exotic);
            assertFalse(recipe.testIngredients(input));
            assertNull(recipe.brew(input));
            assertEquals(1, exotic.quantity());
        }
    }
}
