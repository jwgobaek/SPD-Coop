package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.Bundle;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.*;

/** Generation inventory only. It never authorizes a transition or certifies a playable route. */
public class SewerGenerationTest extends HeadlessDungeon {
    @Test public void sampleNativeSewerRegion() {
        Set<String> all = new TreeSet<>();
        for (int seed = 402; seed <= 423; seed++) {
            try (CoopSession game = CoopSession.open(seed, 1, HeroClass.MAGE)) {
                for (int depth = 1; depth <= 5; depth++) {
                    Level floor = Dungeon.level;
                    if (depth > 1) {
                        Dungeon.level = null;
                        Dungeon.depth = depth;
                        floor = Dungeon.newLevel();
                        Dungeon.level = floor;
                    }
                    assertEquals(depth == 5, floor instanceof SewerBossLevel);
                    Set<String> rooms = new TreeSet<>(), actors = new TreeSet<>(), traps = new TreeSet<>();
                    if (floor instanceof RegularLevel) {
                        for (Room room : ((RegularLevel)floor).rooms())
                            if (room instanceof SpecialRoom) rooms.add(room.getClass().getSimpleName());
                    }
                    for (Mob mob : floor.mobs) actors.add(mob.getClass().getSimpleName());
                    for (Trap trap : floor.traps.valueList()) traps.add(trap.getClass().getSimpleName());
                    all.addAll(actors);
                    Bundle record = new Bundle();
                    record.put("level", floor);
                    int bytes = record.toString().getBytes(StandardCharsets.UTF_8).length;
                    assertTrue(bytes > 0);
                    System.out.println("generation seed=" + seed + " depth=" + depth + " actors=" + actors
                            + " rooms=" + rooms + " traps=" + traps + " mobs=" + floor.mobs.size()
                            + " level_json_bytes=" + bytes);
                }
            }
        }
        assertTrue(all.contains("Piranha"));
        assertTrue(all.contains("Sentry"));
        assertTrue(all.contains("Ghost"));
    }
}
