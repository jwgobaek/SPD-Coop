package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import org.junit.Test;
import static org.junit.Assert.*;

public class VictoryRegressionTest extends HeadlessDungeon {
    @Test(timeout = 30000)
    public void victoryRecordsClassAndOutcome() {
        Dungeon.seed = 408;
        Dungeon.init();
        Dungeon.switchLevel(Dungeon.newLevel(), -1);
        Dungeon.win(Amulet.class);
        Rankings.Record record = Rankings.INSTANCE.records.get(Rankings.INSTANCE.lastRecord);
        assertTrue(Statistics.gameWon);
        assertTrue(record.win);
        assertEquals(Amulet.class, record.cause);
        assertEquals(Dungeon.hero.heroClass, record.heroClass);
    }
}
