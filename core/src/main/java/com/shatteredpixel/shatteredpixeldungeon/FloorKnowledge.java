package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import java.util.BitSet;

/** One seat's observations of one branch/depth. This contains no entity history. */
public final class FloorKnowledge {
    private final boolean[] visible;
    private final BitSet visited = new BitSet(), mapped = new BitSet();
    private final int[] terrain;

    FloorKnowledge(int length) {
        visible = new boolean[length];
        terrain = new int[length];
    }

    void observe(Level level, Hero observer) {
        level.updateDirectSight(observer, visible);
        visible[observer.pos] = observer.isAlive();
        for (int cell = 0; cell < visible.length; cell++) {
            visible[cell] &= level.discoverable[cell];
            if (visible[cell]) {
                visited.set(cell);
                terrain[cell] = appearance(level.map[cell]);
            }
        }
    }

    /** A host-authorized terrain reveal has no effect on direct sight or entities. */
    void reveal(Level level, int cell) {
        if (level.discoverable[cell]) {
            mapped.set(cell);
            terrain[cell] = appearance(level.map[cell]);
        }
    }

    static int appearance(int tile) {
        if (tile == Terrain.SECRET_DOOR) return Terrain.WALL;
        if (tile == Terrain.SECRET_TRAP) return Terrain.EMPTY;
        return tile;
    }

    public int length() { return visible.length; }
    public boolean visible(int cell) { return visible[cell]; }
    public boolean visited(int cell) { return visited.get(cell); }
    public boolean mapped(int cell) { return mapped.get(cell); }
    public int terrain(int cell) { return visited(cell) || mapped(cell) ? terrain[cell] : -1; }
}
