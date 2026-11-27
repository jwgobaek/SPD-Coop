package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/** Native types personally witnessed by one player. Class names on disk are data, never loaded by reflection. */
public final class TrinityDiscoveries {
    public enum Choice {
        CHILLING(-101, Talent.BODY_FORM, Chilling.class),
        SWIFTNESS(-102, Talent.BODY_FORM, Swiftness.class),
        LIGHTNING(-103, Talent.MIND_FORM, WandOfLightning.class),
        HASTE(-104, Talent.SPIRIT_FORM, RingOfHaste.class),
        BLAZING(-105, Talent.BODY_FORM, Blazing.class),
        BLOCKING(-106, Talent.BODY_FORM, Blocking.class),
        MAGIC_MISSILE(-107, Talent.MIND_FORM, com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile.class),
        FROST(-108, Talent.MIND_FORM, com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost.class);

        public final int target;
        public final Talent form;
        public final Class<? extends Bundlable> effect;

        Choice(int target, Talent form, Class<? extends Bundlable> effect) {
            this.target = target;
            this.form = form;
            this.effect = effect;
        }

        public String rowKey() { return "discovery" + (-target); }

        public boolean matches(Item item) {
            return item.isIdentified() && (effect == item.getClass() || effect == inscription(item));
        }
    }

    private static final Set<String> NATIVE_TYPES = new HashSet<>();
    static {
        for (Catalog group : Arrays.asList(Catalog.ENCHANTMENTS, Catalog.GLYPHS, Catalog.WANDS,
                Catalog.THROWN_WEAPONS, Catalog.TIPPED_DARTS, Catalog.RINGS, Catalog.ARTIFACTS)) {
            for (Class<?> type : group.items()) if (type != HolyTome.class) NATIVE_TYPES.add(type.getName());
        }
    }

    private final Set<String> seen = new TreeSet<>();

    private static Class<?> inscription(Item item) {
        if (item instanceof Armor && ((Armor)item).glyph != null) return ((Armor)item).glyph.getClass();
        if (item instanceof Weapon && ((Weapon)item).enchantment != null) return ((Weapon)item).enchantment.getClass();
        return null;
    }

    void witness(Item item) {
        if (!item.isIdentified()) return;
        record(item.getClass());
        record(inscription(item));
    }

    void record(Class<?> type) {
        if (type != null && NATIVE_TYPES.contains(type.getName())) seen.add(type.getName());
    }

    public boolean knows(Choice choice) { return seen.contains(choice.effect.getName()); }
    boolean isEmpty() { return seen.isEmpty(); }

    void save(Bundle bundle, String key) { bundle.put(key, seen.toArray(new String[0])); }

    void restore(Bundle bundle, String key) {
        if (!bundle.contains(key)) throw new IllegalArgumentException("Missing discovery history");
        String[] names = bundle.getStringArray(key);
        if (names == null || names.length > NATIVE_TYPES.size()) throw new IllegalArgumentException("Invalid discovery count");
        for (String name : names) {
            if (!NATIVE_TYPES.contains(name) || !seen.add(name)) throw new IllegalArgumentException("Invalid discovered type");
        }
    }
}
