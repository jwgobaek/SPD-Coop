package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.watabou.utils.Reflection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import static org.junit.Assert.*;

/** A fresh unknown appearance table isolates the public representation of each generated type. */
public final class UnidentifiedProjectionSurvey extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        UnidentifiedProjectionSurvey survey = new UnidentifiedProjectionSurvey();
        survey.temporary.create();
        survey.startBackend();
        try {
            Map<String, String> publicShapes = new HashMap<>();
            for (Generator.Category category : new Generator.Category[]{Generator.Category.POTION, Generator.Category.SCROLL}) {
                for (Class<?> type : category.classes) for (int seat = 0; seat < 2; seat++) {
                    try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
                        game.join(22, HeroClass.WARRIOR);
                        MultiAlchemyTest.station(game, seat);
                        Potion.initColors();
                        Scroll.initLabels();
                        Item item = Reflection.newInstance(type.asSubclass(Item.class));
                        MultiAlchemyTest.pick(game, seat, item);
                        boolean scroll = item instanceof Scroll;
                        assertFalse(scroll ? ((Scroll)item).isKnown() : ((Potion)item).isKnown());
                        PublicDungeon own = ProjectionChecks.seatImage(game, seat);
                        PublicDungeon.Loot bag = own.belongings.stream().filter(i -> i.sprite == item.image()
                                && i.slot.equals("BAG")).findFirst().orElseThrow();
                        assertEquals(scroll ? "READ_SCROLL" : "OTHER", bag.kind);
                        assertFalse(own.source.rows.containsKey("label" + bag.id));
                        assertFalse(own.source.rows.containsKey("brewexotic" + bag.id));
                        if (scroll) assertTrue(own.source.rows.get("brew" + bag.id).startsWith("unknown,"));
                        // Compare what the player can infer from menus and action flags, including
                        // numeric fields. The randomized appearance itself is intentional.
                        String[] fields = own.source.rows.get("i" + bag.id).split(",", -1);
                        fields[2] = "appearance";
                        String signature = Arrays.toString(fields) + "|" + own.actions(bag) + "|"
                                + PublicDungeon.shortcutAction(bag) + "|" + bag.stackType + "|"
                                + own.source.rows.get("brew" + bag.id) + "|"
                                + own.source.rows.get("exoticrecipe" + bag.id) + "|"
                                + own.source.rows.get("potionthrow" + bag.id);
                        String previous = publicShapes.putIfAbsent(category + "/" + seat, signature);
                        if (previous != null) assertEquals(type.getSimpleName(), previous, signature);
                        for (int recipient = 0; recipient < 2; recipient++) {
                            String snapshot = ProjectionChecks.seatImage(game, recipient).source.rows.toString().toLowerCase(Locale.ROOT);
                            assertFalse(type.getSimpleName(), snapshot.contains(type.getSimpleName().toLowerCase(Locale.ROOT)));
                        }
                        System.out.println("PASS " + type.getSimpleName() + " owner=" + seat);
                    }
                }
            }
        } finally {
            survey.stopBackend();
            survey.temporary.delete();
        }
    }
}
