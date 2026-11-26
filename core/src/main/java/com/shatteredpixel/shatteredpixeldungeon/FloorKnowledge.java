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
        if (observer.isAlive()) {
            for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob : level.mobs) {
                if (!(mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.HawkAlly)
                        || !((com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.HawkAlly)mob).belongsTo(observer)) continue;
                if (mob.fieldOfView==null || mob.fieldOfView.length!=level.length()) mob.fieldOfView=new boolean[level.length()];
                level.updateFieldOfView(mob,mob.fieldOfView);
                for (int i=0;i<visible.length;i++) visible[i] |= mob.fieldOfView[i];
            }
        }
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
    public void reveal(Level level, int cell) {
        if (level.discoverable[cell]) {
            mapped.set(cell);
            terrain[cell] = appearance(level.map[cell]);
        }
    }

    com.watabou.utils.Bundle save() {
        com.watabou.utils.Bundle b = new com.watabou.utils.Bundle();
        b.put("visited", visited.toLongArray());
        b.put("mapped", mapped.toLongArray());
        b.put("terrain", terrain);
        return b;
    }

    void load(com.watabou.utils.Bundle b) {
        int[] tiles = b.getIntArray("terrain");
        if (tiles == null || tiles.length != length()) throw new IllegalArgumentException("Deformed memory");
        visited.or(BitSet.valueOf(b.getLongArray("visited")));
        mapped.or(BitSet.valueOf(b.getLongArray("mapped")));
        if (mapped.length() > length() || visited.length() > length()) throw new IllegalArgumentException("Memory bounds");
        for (int cell=0;cell<tiles.length;cell++) {
            if (tiles[cell]<0 || tiles[cell]>Terrain.CUSTOM_DECO_WTR) throw new IllegalArgumentException("Memory terrain");
        }
        System.arraycopy(tiles,0,terrain,0,length());
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
