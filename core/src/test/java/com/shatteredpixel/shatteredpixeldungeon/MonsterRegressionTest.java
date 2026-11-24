package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.Test;

import static org.junit.Assert.*;

public class MonsterRegressionTest extends HeadlessDungeon {
    // Expose the protected turn entry point and chosen target without changing Rat's AI.
    private static class ObservedRat extends Rat {
        Char takeTurn() {
            super.act();
            return enemy;
        }
    }

    @Test(timeout = 30000)
    public void huntingRatSelectsVisibleHeroAndPaysForEachAttack() {
        Dungeon.seed = 408;
        Dungeon.init();
        Dungeon.switchLevel(Dungeon.newLevel(), -1);
        Hero hero = Dungeon.hero;
        new TestSprite(hero);
        ObservedRat rat = new ObservedRat();
        rat.pos = -1;
        for (int offset : PathFinder.NEIGHBOURS8) {
            int cell = hero.pos + offset;
            if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
                rat.pos = cell;
                break;
            }
        }
        assertTrue(rat.pos >= 0);
        new TestSprite(rat);
        Dungeon.level.mobs.add(rat);
        Actor.add(rat);
        rat.state = rat.HUNTING;
        int before = hero.HP;
        float start = rat.cooldown();
        Random.pushGenerator(408);
        try {
            for (int turn = 1; turn <= 4; turn++) {
                assertSame(hero, rat.takeTurn());
                assertEquals(start + turn * rat.attackDelay(), rat.cooldown(), 0.001f);
            }
        } finally {
            Random.popGenerator();
        }
        assertTrue(hero.HP < before);
        assertTrue(hero.isAlive());
    }
}
