package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import org.junit.Test;
import static org.junit.Assert.*;

public class EndgameRegressionTest extends HeadlessDungeon {
    @Test(timeout = 30000)
    public void blessedAnkhProtectsOnceAndFinalDeathClearsSave() {
        Dungeon.seed = 407;
        Dungeon.init();
        Dungeon.switchLevel(Dungeon.newLevel(), -1);
        Hero hero = Dungeon.hero;
        new TestSprite(hero);
        Ankh ankh = new Ankh();
        ankh.bless();
        assertTrue(ankh.collect(hero.belongings.backpack));
        hero.damage(hero.HT * 3, this);
        assertEquals(hero.HT / 4, hero.HP);
        assertFalse(hero.belongings.backpack.contains(ankh));
        assertEquals(1, Statistics.ankhsUsed);
        hero.buff(Invulnerability.class).detach();
        hero.damage(hero.HT * 3, hero.buff(Hunger.class));
        assertFalse(hero.isAlive());
        assertFalse(Rankings.INSTANCE.records.get(Rankings.INSTANCE.lastRecord).win);
        assertNull(GamesInProgress.check(1));
    }
}
