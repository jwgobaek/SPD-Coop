package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RogueBaselineTest extends SeededFloorBaseline {
    @Test(timeout = 30000)
    public void generatedFloorMatchesUpstream() throws Exception {
        assertEquals("92974d623c0dcdeed74f36a0816f3137213d17602cd25c92cc18841eba1de02b", snapshot(3402, HeroClass.ROGUE));
    }
}
