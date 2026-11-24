package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ActionRegressionTest extends HeadlessDungeon {
    private int emptyNeighbour(int origin) {
        Level floor = Dungeon.level;
        for (int offset : PathFinder.NEIGHBOURS8) {
            int cell = origin + offset;
            if (floor.map[cell] == Terrain.EMPTY && Actor.findChar(cell) == null
                    && floor.heaps.get(cell) == null && floor.plants.get(cell) == null) return cell;
        }
        throw new AssertionError("Expected an empty adjacent tile");
    }

    private List<String> mobs(Level level) {
        List<String> result = new ArrayList<>();
        for (Mob mob : level.mobs) {
            result.add(mob.id() + "/" + mob.getClass().getSimpleName() + "/" + mob.pos + "/" + mob.HP);
        }
        Collections.sort(result);
        return result;
    }

    private List<String> inventory(Hero hero) {
        List<String> result = new ArrayList<>();
        for (Item item : hero.belongings) {
            result.add(item.getClass().getSimpleName() + "/" + item.quantity() + "/" + item.level()
                    + "/" + item.isEquipped(hero));
        }
        Collections.sort(result);
        return result;
    }

    @Test(timeout = 30000)
    public void actionsAndTimeSurviveSaveAndFloorRoundTrip() throws Exception {
        Dungeon.seed = 402;
        Dungeon.init();
        Dungeon.switchLevel(Dungeon.newLevel(), -1);
        Hero hero = Dungeon.hero;
        TestSprite sprite = new TestSprite(hero);
        int step = emptyNeighbour(hero.pos);
        float before = hero.cooldown();
        Buff.prolong(hero, Haste.class, 4f);
        float moveTime = 1f / hero.speed();
        assertTrue(moveTime < 1f);
        assertTrue(hero.handle(step));
        assertTrue(hero.act());
        assertEquals(step, hero.pos);
        assertEquals(before + moveTime, hero.cooldown(), 0.001f);

        Rat target = new Rat();
        target.pos = emptyNeighbour(step);
        new TestSprite(target);
        Dungeon.level.mobs.add(target);
        Actor.add(target);
        Dungeon.observe();
        hero.checkVisibleMobs();
        int hp = target.HP;
        before = hero.cooldown();
        assertTrue(hero.handle(target.pos));
        hero.act();
        assertEquals(1, sprite.attackFrames);
        assertEquals(hp, target.HP);
        Random.pushGenerator(402);
        try {
            hero.onAttackComplete();
        } finally {
            Random.popGenerator();
        }
        assertTrue(target.HP < hp);
        assertEquals(before + hero.attackDelay(), hero.cooldown(), 0.001f);
        assertNull(hero.curAction);

        Food food = hero.belongings.getItem(Food.class);
        assertNotNull(food);
        before = hero.cooldown();
        food.execute(hero, Food.AC_EAT);
        assertEquals(1, sprite.operateFrames);
        hero.onOperateComplete();
        assertEquals(1, Statistics.foodEaten);
        assertFalse(hero.belongings.backpack.contains(food));
        assertTrue(hero.cooldown() > before);
        hero.damage(3, target);
        assertTrue(hero.HP < hero.HT);
        hero.belongings.weapon().upgrade();
        int id = hero.id();
        int health = hero.HP;
        int[] map = Dungeon.level.map.clone();
        boolean[] visited = Dungeon.level.visited.clone();
        List<String> inventory = inventory(hero);
        List<String> enemies = mobs(Dungeon.level);
        float time = hero.cooldown();
        Dungeon.saveAll();
        Dungeon.depth++;
        Dungeon.switchLevel(Dungeon.newLevel(), -1);
        assertEquals(2, Dungeon.depth);
        Dungeon.loadGame(1);
        // switchLevel saved depth 2. Return through the original depth-file path.
        assertEquals(2, Dungeon.depth);
        Dungeon.depth--;
        Dungeon.switchLevel(Dungeon.loadLevel(1), step);
        Dungeon.loadGame(1);
        Level restored = Dungeon.loadLevel(1);
        assertEquals(id, Dungeon.hero.id());
        assertNotSame(hero, Dungeon.hero);
        assertEquals(health, Dungeon.hero.HP);
        assertEquals(step, Dungeon.hero.pos);
        assertEquals(inventory, inventory(Dungeon.hero));
        assertEquals(enemies, mobs(restored));
        assertArrayEquals(map, restored.map);
        assertArrayEquals(visited, restored.visited);
        assertEquals(time, Dungeon.hero.cooldown(), 0.001f);
        assertNotNull(Dungeon.hero.buff(Haste.class));
        Dungeon.switchLevel(restored, step);
        Hero resumed = Dungeon.hero;
        new TestSprite(resumed);
        Dungeon.observe();
        resumed.fieldOfView = restored.heroFOV;
        resumed.checkVisibleMobs();
        int next = emptyNeighbour(step);
        float previous = resumed.cooldown();
        assertTrue(resumed.handle(next));
        assertTrue(resumed.act());
        assertEquals(next, resumed.pos);
        assertEquals(previous + 1f / resumed.speed(), resumed.cooldown(), 0.001f);
    }
}
