package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDetectMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition.IntuitionUseTracker;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CoopRuneStones {
    public static final int OPTIONS_PER_ITEM = 32;

    private CoopRuneStones() {}

    private static String family(Item item) {
        if (item instanceof Ring) return "ring";
        if (item instanceof ExoticScroll) return "exoscroll";
        if (item instanceof ExoticPotion) return "exopotion";
        if (item instanceof Potion) return "potion";
        if (item instanceof Scroll) return "scroll";
        return null;
    }

    static List<Class<? extends Item>> guesses(Item item) {
        List<Class<? extends Item>> choices = StoneOfIntuition.unknownCandidates(item);
        choices.sort(Comparator.comparing(Class::getName));
        return choices;
    }

    public static void project(Map<String,String> rows, Hero hero, Item item, long handle,
                               int selection, boolean guessing, boolean detecting) {
        if (item.cursedKnown && (item instanceof EquipableItem || item instanceof Wand))
            rows.put("curseknown"+handle, item.cursed ? "1" : "0");
        if (hero.buff(MagicImmune.class) != null) return;
        if (detecting && StoneOfDetectMagic.canDetect(item)) rows.put("detectgear"+handle, Integer.toString(selection));
        if (!guessing || !StoneOfIntuition.canGuess(item) || item.isIdentified()) return;
        String group = family(item);
        rows.put("guessgear"+handle, selection+","+group);
        if (rows.containsKey("guess"+group+"0")) return;
        int i = 0;
        for (Class<?> type : guesses(item)) {
            String key = type.getName().substring(Item.class.getPackage().getName().length()+1);
            rows.put("guess"+group+(i++), "items."+key.toLowerCase(Locale.ROOT));
        }
    }

    static String use(CoopSession game, Hero hero, Item stone, int encoded, boolean guessing) {
        if (encoded < 1 || (guessing ? !(stone instanceof StoneOfIntuition) : !(stone instanceof StoneOfDetectMagic))
                || hero.buff(MagicImmune.class) != null || !hero.belongings.backpack.contains(stone)
                || game.ownership.owner(stone) != game.playerOf(hero)) return null;
        int index = guessing ? encoded / OPTIONS_PER_ITEM : encoded;
        Item target = null;
        for (Item owned : hero.belongings) {
            if (--index == 0) { target = owned; break; }
        }
        if (target == null || game.ownership.owner(target) != game.playerOf(hero)) return null;
        boolean consume = true;
        String result;
        if (guessing) {
            if (!StoneOfIntuition.canGuess(target) || target.isIdentified()) return null;
            List<Class<? extends Item>> candidates = guesses(target);
            int choice = encoded % OPTIONS_PER_ITEM;
            if (choice >= candidates.size()) return null;
            boolean correct = target.getClass() == candidates.get(choice);
            if (correct) {
                if (target instanceof Ring) { ((Ring)target).setKnown(); Item.updateQuickslot(); }
                else target.identify();
            }
            IntuitionUseTracker track = hero.buff(IntuitionUseTracker.class);
            consume = track != null;
            if (consume) track.detach(); else Buff.affect(hero, IntuitionUseTracker.class);
            result = correct ? "CORRECT" : "INCORRECT";
        } else {
            if (!StoneOfDetectMagic.canDetect(target)) return null;
            result = StoneOfDetectMagic.detect(target).name();
        }
        if (consume && stone.detach(hero.belongings.backpack) == null)
            throw new IllegalStateException("Missing owned runestone");
        Catalog.countUse(stone.getClass());
        Talent.onRunestoneUsed(hero, hero.pos, stone.getClass());
        Invisibility.dispel(hero);
        hero.spendAndNext(1f);
        return result;
    }
}
