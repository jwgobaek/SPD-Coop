package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** Ephemeral display geometry. No pathfinder globals, actor registration, RNG or time changes. */
public final class CombatZones {
    private CombatZones() {}

    /** A null observer computes the host view. Public views use only observable participants and paths. */
    public static List<BitSet> calculate(CoopSession game, FloorKnowledge observer) {
        Level level = Dungeon.level;
        List<BitSet> zones = new ArrayList<>();
        for (int seat = 0; seat < 2; seat++) {
            Hero hero = game.hero(seat);
            if (!hero.isAlive() || (observer != null && (!observer.visible(hero.pos) || hero.invisible > 0))) continue;
            for (Mob mob : level.mobs) {
                if (!mob.isAlive() || mob.invisible > 0 || mob.alignment != Char.Alignment.ENEMY
                        || !CoopSession.basicMob(mob) || (observer != null && !observer.visible(mob.pos))) continue;
                if (!mob.isTargeting(hero) && !game.recentlyAttacked(seat, mob)) continue;
                boolean[] direct = new boolean[level.length()];
                level.updateDirectSight(hero, direct);
                if (!direct[mob.pos]) continue;
                BitSet area = corridor(level, observer, hero.pos, mob.pos);
                if (area.isEmpty()) continue;
                for (int i = zones.size()-1; i >= 0; i--) {
                    if (area.intersects(zones.get(i))) area.or(zones.remove(i));
                }
                zones.add(area);
            }
        }
        return zones;
    }

    private static final int RANGE = 8;
    private static BitSet corridor(Level level, FloorKnowledge observer, int from, int to) {
        int[] a = distances(level, observer, from);
        BitSet area = new BitSet();
        if (a[to] < 0) return area;
        int[] b = distances(level, observer, to);
        for (int cell = 0; cell < a.length; cell++) {
            if (a[cell] >= 0 && b[cell] >= 0 && a[cell] + b[cell] <= a[to] + 2) area.set(cell);
        }
        return area;
    }

    private static int[] distances(Level level, FloorKnowledge observer, int start) {
        int[] distance = new int[level.length()], queue = new int[level.length()];
        java.util.Arrays.fill(distance, -1);
        distance[start] = 0;
        queue[0] = start;
        int end = 1;
        int[] steps = {-level.width(), 1, level.width(), -1};
        for (int head = 0; head < end; head++) {
            int cell = queue[head];
            if (distance[cell] == RANGE) continue;
            for (int step : steps) {
                int next = cell + step;
                if (!level.insideMap(next) || distance[next] >= 0 || !level.passable[next] || level.pit[next]
                        || (observer != null && !observer.visible(next))) continue;
                distance[next] = distance[cell] + 1;
                queue[end++] = next;
            }
        }
        return distance;
    }

    /** NESW bits identify exposed cardinal edges on a receiver's clipped component. */
    public static int edgeMask(BitSet area, int cell, int width) {
        int mask = 0;
        int[] steps = {-width, 1, width, -1};
        for (int i = 0; i < steps.length; i++) if (!area.get(cell + steps[i])) mask |= 1 << i;
        return mask;
    }
}
