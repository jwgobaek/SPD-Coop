package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original generation, staged stairs and opened sewer exit. Includes rejected seeds. */
public class PrisonStairSurveyTest extends HeadlessDungeon {
    @Test public void revisitSixAfterSeventhFloorCheckpointOrVerifyRejection() {
        for (int seed=570; seed<=581; seed++) {
            Bundle onSeven=null;
            String blocker=null;
            int reached=0;
            try (CoopSession game=CoopSession.open(seed,11,HeroClass.WARRIOR)) {
                game.join(12,HeroClass.MAGE);
                reached=1;
                while (Dungeon.depth<7) {
                    int cell=Dungeon.level.exit();
                    if (Dungeon.depth==5) Level.set(cell,Terrain.UNLOCKED_EXIT);
                    SewerSpecialActorTest.atGate(game,cell);
                    Bundle before=game.checkpoint();
                    assertTrue(game.stairs.consent(1-game.activeSeat(),game.turn(),cell,false));
                    if (!CoopFloorsTest.step(game,CoopSession.Action.TRAVEL,cell,null)) {
                        assertTrue(game.restoredAfterTravel());
                        assertFalse(game.stopped());
                        assertEquals(reached,Dungeon.depth);
                        Bundle after=game.checkpoint();
                        assertEquals(before.getLong("turn"),after.getLong("turn"));
                        assertArrayEquals(before.getLongArray("random"),after.getLongArray("random"));
                        assertArrayEquals(before.getBundle("floor").getIntArray("map"),after.getBundle("floor").getIntArray("map"));
                        blocker=game.destinationBlocker;
                        assertNotNull(blocker);
                        break;
                    }
                    reached=Dungeon.depth;
                }
                if (reached==7) onSeven=game.checkpoint();
            } catch (CoopFloors.UnsupportedDestination blockedAtStart) {
                assertEquals(0,reached);
                blocker=blockedAtStart.getMessage();
            }
            if (onSeven!=null) try (CoopSession game=CoopSession.restore(onSeven)) {
                assertEquals(7,Dungeon.depth);
                SewerDepthTest.gate(game,false);
                assertEquals(6,Dungeon.depth);
                SewerSpecialActorTest.waitOne(game);
            }
            assertEquals("staged seven-floor regression for seed "+seed,7,reached);
            System.out.println("QUEST_STAIRS\t"+seed+"\t"+reached+"\t"+(onSeven!=null)+"\t"+(blocker==null?"-":blocker));
        }
    }
}
