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
            boolean traveled=checkGate(seed,"gate");
            if (seed!=421) assertTrue("Previously playable native seed "+seed,traveled);
            if (traveled) admitted++; else rejected++;
        }
        assertTrue(admitted>=21);
        assertEquals(22,admitted+rejected);
    }

    @Test public void consecutiveAdditionalDestinationsPreserveTheirPlayableBoundaries() {
        for(int seed=424;seed<=445;seed++) checkGate(seed,"additional");
        assertTrue(checkGate(479,"rare"));
    }

    private boolean checkGate(int seed,String cohort) {
        String reason="none";
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

                assertNotNull(Dungeon.level.getTransition(Dungeon.level.entrance()));
            } else {
                reason=game.destinationBlocker;
                assertNotNull(reason);
                assertTrue(game.restoredAfterTravel());
                assertEquals(before.getLong("turn"),boundary.getLong("turn"));
                assertArrayEquals(before.getLongArray("random"),boundary.getLongArray("random"));
                assertArrayEquals(before.getBundle("floor").getIntArray("map"),Dungeon.level.map);
            }
        }
        catch (CoopFloors.UnsupportedDestination unsupportedStart) {
            assertNull(Dungeon.coop);
            assertNull(Dungeon.level);
            System.out.println(cohort+" seed="+seed+" placed_at_stairs=false outcome=REJECT_START"
                    +" restored=false rng=not_applicable next_action=false blocker="+unsupportedStart.getMessage());
            return false;
        }
        try (CoopSession game=CoopSession.restore(boundary)) {
            assertEquals(expectedDepth,Dungeon.depth);
            Bundle restored=game.checkpoint();
            assertArrayEquals(boundary.getLongArray("random"),restored.getLongArray("random"));
            assertArrayEquals(boundary.getBundle("floor").getIntArray("map"),Dungeon.level.map);
            assertEquals(boundary.getLong("turn"),game.turn());
            SewerSpecialActorTest.waitOne(game);
            if (expectedDepth==2) {
                SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
                CoopFloorsTest.cross(game);
                assertEquals(1,Dungeon.depth);
                SewerSpecialActorTest.waitOne(game);
            }
            System.out.println(cohort+" seed="+seed+" placed_at_stairs=true outcome="+
                    (expectedDepth==2 ? "ROUND_TRIP" : "REJECT_FLOOR")+" restored=true rng=true next_action=true blocker="+reason);
        }
        return expectedDepth==2;
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
