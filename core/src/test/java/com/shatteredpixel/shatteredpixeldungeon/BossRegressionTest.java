package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import org.junit.Test;
import static org.junit.Assert.*;

public class BossRegressionTest extends HeadlessDungeon {
    @Test(timeout = 30000)
    public void defeatingGooUnlocksFloorAndDropsKey() {
        Dungeon.seed = 405;
        Dungeon.init();
        Dungeon.depth = 5;
        Dungeon.switchLevel(Dungeon.newLevel(), -1);
        assertTrue(Dungeon.level instanceof SewerBossLevel);
        Goo goo = (Goo) Dungeon.level.mobs.stream().filter(Goo.class::isInstance).findFirst().get();
        new TestSprite(Dungeon.hero);
        new TestSprite(goo);
        goo.damage(1, Dungeon.hero);
        assertTrue(Dungeon.level.locked);
        assertNotNull(Dungeon.hero.buff(LockedFloor.class));
        assertEquals(goo.HT - 1, goo.HP);
        goo.damage(goo.HT / 2, Dungeon.hero);
        assertTrue(goo.isAlive() && goo.HP <= goo.HT / 2);
        goo.damage(goo.HT, Dungeon.hero);
        assertFalse(goo.isAlive());
        assertFalse(Dungeon.level.mobs.contains(goo));
        assertFalse(Dungeon.level.locked);
        assertTrue(Dungeon.level.heaps.get(goo.pos).items.stream().anyMatch(WornKey.class::isInstance));
    }
}
