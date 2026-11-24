package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

abstract class SeededFloorBaseline extends HeadlessDungeon {
    String snapshot(long seed, HeroClass type) throws Exception {
        Dungeon.seed = seed;
        GamesInProgress.selectedClass = type;
        Dungeon.init();
        // Fix tutorial progression as well as the seed. EntranceRoom uses an
        // independent, unseeded generator when placing unread guide pages.
        SPDSettings.intro(false);
        Document.ADVENTURERS_GUIDE.findPage(Document.GUIDE_INTRO);
        Document.ADVENTURERS_GUIDE.readPage(Document.GUIDE_INTRO);
        Level floor = Dungeon.newLevel();
        List<String> entries = new ArrayList<>();
        entries.add("map=" + Arrays.toString(floor.map));
        entries.add("entrance=" + floor.entrance() + ",exit=" + floor.exit());
        entries.add("hero=" + Dungeon.hero.heroClass + ",hp=" + Dungeon.hero.HP);
        for (Mob mob : floor.mobs) entries.add("mob=" + mob.pos + ":" + mob.getClass().getName());
        for (Heap heap : floor.heaps.valueList()) {
            for (Item item : heap.items) {
                entries.add("heap=" + heap.pos + ":" + describe(item));
            }
        }
        for (Item item : Dungeon.hero.belongings) {
            entries.add("bag=" + describe(item) + ":" + item.isEquipped(Dungeon.hero));
        }
        Collections.sort(entries);
        byte[] data = String.join("\n", entries).getBytes(StandardCharsets.UTF_8);
        StringBuilder digest = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) {
            digest.append(String.format("%02x", b));
        }
        return digest.toString();
    }

    private String describe(Item item) {
        return item.getClass().getName() + ":" + item.quantity() + ":" + item.level();
    }
}
