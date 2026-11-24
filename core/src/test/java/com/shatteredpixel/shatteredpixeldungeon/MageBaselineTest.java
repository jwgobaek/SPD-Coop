package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MageBaselineTest extends SeededFloorBaseline {
    @Test(timeout = 30000)
    public void generatedFloorMatchesUpstream() throws Exception {
        assertEquals("c3a9a3929a3a9755cf9e6dad58c315f5da69869420217a2a23ff60ff763805e9", snapshot(2402, HeroClass.MAGE));
    }
}
