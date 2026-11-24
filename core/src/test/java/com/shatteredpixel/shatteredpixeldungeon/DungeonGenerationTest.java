package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import org.junit.Test;

import static org.junit.Assert.*;

public class DungeonGenerationTest extends HeadlessDungeon {
    @Test(timeout = 30000)
    public void warriorBeginsOnGeneratedSewerFloor() throws Exception {
        Dungeon.seed = 402;
        Dungeon.init();
        Level floor = Dungeon.newLevel();
        assertTrue(floor instanceof SewerLevel);
        // Recorded from the v4.0.2 reference checkout for warrior seed 402.
        assertEquals(-585159512, java.util.Arrays.hashCode(floor.map));
        assertNotEquals(floor.entrance(), floor.exit());
        assertEquals(floor.width() * floor.height(), floor.map.length);
        assertEquals(HeroClass.WARRIOR, Dungeon.hero.heroClass);
        assertTrue(Dungeon.hero.isAlive());
        Dungeon.switchLevel(floor, -1);
        assertEquals(floor.entrance(), Dungeon.hero.pos);
        assertArrayEquals(floor.map, Dungeon.loadLevel(1).map);
    }
}
