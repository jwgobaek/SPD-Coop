package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.watabou.utils.Bundlable;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import java.util.Arrays;
import java.util.HashSet;

/** One reader's unfinished native scroll. Option generation happens only in the target action. */
public final class CoopEnchantScroll {
    private final long scroll;
    private final boolean identifiedByReading;
    private long target;
    private long consumed;
    private boolean armor;
    private Bundlable[] options;

    private CoopEnchantScroll(long scroll, boolean identifiedByReading) {
        this.scroll = scroll;
        this.identifiedByReading = identifiedByReading;
    }

    static CoopEnchantScroll begin(CoopSession game, Hero hero, Item item) {
        UnstableSpellbook.bookRecharge book = hero.buff(UnstableSpellbook.bookRecharge.class);
        if (!(item instanceof ScrollOfEnchantment) || !hero.belongings.backpack.contains(item)
                || game.ownership.owner(item) != game.playerOf(hero) || hero.buff(Blindness.class) != null
                || hero.buff(MagicImmune.class) != null || book != null && book.isCursed()) return null;
        ScrollOfEnchantment source = (ScrollOfEnchantment)item;
        CoopEnchantScroll choice = new CoopEnchantScroll(game.ownership.identity(item), !source.isKnown());
        if (choice.identifiedByReading) {
            source.identify();
            choice.consume(game, hero, source);
        }
        return choice;
    }

    private void consume(CoopSession game, Hero hero, Item item) {
        Item unit = item.detach(hero.belongings.backpack);
        if (unit == null) throw new IllegalStateException("Enchantment scroll unavailable");
        consumed = game.ownership.identity(unit);
    }

    private static Item ownedItem(CoopSession game, Hero hero, long id) {
        for (Item item : hero.belongings) {
            if (game.ownership.owner(item) == game.playerOf(hero) && game.ownership.identity(item) == id) return item;
        }
        return null;
    }

    // SpiritBow already gates unsupported procs on both direct and shared attacks.
    public static boolean eligible(Hero hero, Item item) {
        if (!ScrollOfEnchantment.enchantable(item)) return false;
        return item instanceof SpiritBow || (hero.belongings.backpack.contains(item)
                || hero.belongings.weapon()==item || hero.belongings.armor()==item)
                && (CoopSession.basicArmorType(item) || CoopSession.basicWeaponType(item));
    }

    boolean select(CoopSession game, Hero hero, Item item) {
        if (options != null || !eligible(hero, item) || game.ownership.owner(item) != game.playerOf(hero)
                || ownedItem(game, hero, game.ownership.identity(item)) != item) return false;
        if (!identifiedByReading) {
            Item source = ownedItem(game, hero, scroll);
            if (!(source instanceof ScrollOfEnchantment) || !hero.belongings.backpack.contains(source)) return false;
            consume(game, hero, source);
        }
        target = game.ownership.identity(item);
        armor = item instanceof Armor;
        // Android merges Glyph[] and Enchantment[] to Object[] at this branch.
        // Give the stored array its own concrete verifier type.
        Object[] drawn = armor ? ScrollOfEnchantment.drawGlyphs((Armor)item) : ScrollOfEnchantment.drawEnchantments((Weapon)item);
        options = Arrays.copyOf(drawn, drawn.length, Bundlable[].class);
        return true;
    }

    public boolean available(CoopSession game, Hero hero, int index) {
        if (options==null || index<0 || index>=options.length) return false;
        Item gear=ownedItem(game,hero,target);
        if (!eligible(hero,gear)) return false;
        if (gear==hero.belongings.armor()) return CoopSession.supportedWornGlyph((Armor.Glyph)options[index]);
        if (gear==hero.belongings.weapon()) return CoopSession.supportedWornEnchant((Weapon.Enchantment)options[index]);
        return true;
    }

    boolean apply(CoopSession game, Hero hero, int index) {
        if (!available(game,hero,index)) return false;
        Item gear = ownedItem(game, hero, target);
        if (!eligible(hero, gear)) return false;
        if (armor) ((Armor)gear).inscribe((Armor.Glyph)options[index]);
        else ((Weapon)gear).enchant((Weapon.Enchantment)options[index]);
        game.witnessOwned(gear);
        new ScrollOfEnchantment().finishReading(hero);
        return true;
    }

    public boolean chargesTimeOnCancel() { return options == null && identifiedByReading; }
    public boolean hasOptions() { return options != null; }
    public boolean isArmor() { return armor; }
    public String optionKey(int i) {
        String key = options[i].getClass().getName();
        return key.substring(key.indexOf(".items.")+1).toLowerCase(java.util.Locale.ROOT);
    }

    void save(Bundle b) {
        b.put("scroll", scroll);
        b.put("read_unknown", identifiedByReading);
        b.put("target", target);
        b.put("consumed", consumed);
        if (options == null) return;
        String[] types = new String[3];
        for (int i = 0; i < types.length; i++) {
            types[i] = options[i].getClass().getSimpleName();
            Bundle data = new Bundle();
            options[i].storeInBundle(data);
            b.put("option" + i, data);
        }
        b.put("types", types);
    }

    private static Class<?> type(String name, int index, boolean armor) {
        Class<?>[][] groups = armor ? new Class<?>[][]{Armor.Glyph.common, Armor.Glyph.uncommon, Armor.Glyph.rare}
                : new Class<?>[][]{Weapon.Enchantment.common, Weapon.Enchantment.uncommon, Weapon.Enchantment.rare};
        for (int g = 0; g < groups.length; g++) {
            if (index < 2 && g != index) continue;
            for (Class<?> cls : groups[g]) if (cls.getSimpleName().equals(name)) return cls;
        }
        throw new IllegalArgumentException("Invalid scroll enchantment");
    }

    static CoopEnchantScroll restore(Bundle b, CoopSession game, Hero hero, long ceiling) {
        CoopEnchantScroll choice = new CoopEnchantScroll(b.getLong("scroll"), b.getBoolean("read_unknown"));
        choice.target = b.getLong("target");
        choice.consumed = b.getLong("consumed");
        boolean paid = choice.identifiedByReading || choice.target != 0;
        if (paid != (choice.consumed > 0) || choice.consumed < 0 || choice.consumed > ceiling
                || paid && ownedItem(game, hero, choice.consumed) != null) throw new IllegalArgumentException("Invalid consumed scroll");
        if (choice.scroll <= 0 || choice.scroll > ceiling || !b.contains("read_unknown")) throw new IllegalArgumentException("Invalid scroll identity");
        if (choice.target == 0) {
            if (b.contains("types") || !choice.identifiedByReading && !(ownedItem(game, hero, choice.scroll) instanceof ScrollOfEnchantment))
                throw new IllegalArgumentException("Invalid scroll selection");
            return choice;
        }
        String[] types = b.getStringArray("types");
        Item gear = ownedItem(game, hero, choice.target);
        choice.armor = gear instanceof Armor;
        if (choice.target == choice.scroll || !eligible(hero, gear) || types == null || types.length != 3
                || new HashSet<>(Arrays.asList(types)).size() != 3) throw new IllegalArgumentException("Invalid equipment or options");
        choice.options = new Bundlable[3];
        for (int i = 0; i < 3; i++) {
            if (!b.contains("option" + i)) throw new IllegalArgumentException("Missing enchantment state");
            Bundlable option = (Bundlable)Reflection.newInstance(type(types[i], i, choice.armor));
            option.restoreFromBundle(b.getBundle("option" + i));
            choice.options[i] = option;
        }
        return choice;
    }
}
