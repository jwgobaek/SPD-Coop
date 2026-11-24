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
    @Test public void roundTripOrRestoreTheEntryBoundaryOnEachSampledSeed() {
        int admitted=0, rejected=0;
        for (int seed=402; seed<=423; seed++) {
            Bundle boundary;
            int expectedDepth;
            try (CoopSession game=CoopSession.open(seed,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.WARRIOR);
                int exit=Dungeon.level.exit();
                SewerSpecialActorTest.atGate(game,exit);
                assertEquals(exit,game.stairs.stair());
                assertTrue(game.stairs.consent(1-game.activeSeat(),game.turn(),exit,false));
                Bundle before=game.checkpoint();
                boolean entered=CoopFloorsTest.step(game,CoopSession.Action.TRAVEL,exit,null);
                expectedDepth=entered ? 2 : 1;
                assertEquals(expectedDepth,Dungeon.depth);
                assertFalse(game.stopped());
                boundary=game.checkpoint();
                if (entered) {
                    admitted++;
                    assertNotNull(Dungeon.level.getTransition(Dungeon.level.entrance()));
                } else {
                    rejected++;
                    assertTrue(game.restoredAfterTravel());
                    assertEquals(before.getLong("turn"),boundary.getLong("turn"));
                    assertArrayEquals(before.getLongArray("random"),boundary.getLongArray("random"));
                    assertArrayEquals(before.getBundle("floor").getIntArray("map"),Dungeon.level.map);
                }
            }
            try (CoopSession game=CoopSession.restore(boundary)) {
                assertEquals(expectedDepth,Dungeon.depth);
                SewerSpecialActorTest.waitOne(game);
                if (expectedDepth==2) {
                    SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
                    CoopFloorsTest.cross(game);
                    assertEquals(1,Dungeon.depth);
                    SewerSpecialActorTest.waitOne(game);
                }
                System.out.println("gate seed="+seed+" placed_at_stairs=true outcome="+
                        (expectedDepth==2 ? "ROUND_TRIP" : "REJECT_FLOOR")+" restored=true");
            }
        }
        // Keep the admitted native combinations as a floor when more actors gain support.
        assertTrue("Previously playable return routes regressed",admitted>=15);
        assertEquals(22,admitted+rejected);
    }

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
