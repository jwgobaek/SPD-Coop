package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import java.util.BitSet;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.watabou.utils.PathFinder;

/** One seat's observations of one branch/depth. This contains no entity history. */
public final class FloorKnowledge {
    private final boolean[] visible;
    private boolean[] hawkSight;
    private final BitSet visited = new BitSet(), mapped = new BitSet();
    private final int[] terrain;
    private final BitSet sensedTerrain = new BitSet();
    private final BitSet sensedMobs = new BitSet();

    FloorKnowledge(int length) {
        visible = new boolean[length];
        terrain = new int[length];
    }

    void observe(Level level, Hero observer) {
        level.updateDirectSight(observer, visible);
        SpiritHawk.HawkAlly hawk = null;
        if (observer.isAlive()) {
            for (Mob mob : level.mobs) {
                if (mob instanceof SpiritHawk.HawkAlly && ((SpiritHawk.HawkAlly)mob).belongsTo(observer)) {
                    hawk = (SpiritHawk.HawkAlly)mob;
                    if (hawkSight == null) hawkSight = new boolean[visible.length];
                    level.updateHawkSight(hawk, hawkSight);
                    for (int cell = 0; cell < visible.length; cell++) visible[cell] |= hawkSight[cell];
                    break;
                }
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
        sensedTerrain.clear();
        sensedMobs.clear();
        if (!observer.isAlive()) return;
        // Hawk mind vision reveals terrain around a resident without granting item or attack visibility.
        if (hawk != null && hawk.eagleEye() >= 3) {
            int radius = hawk.eagleEye() - 1;
            for (Mob mob : level.mobs) {
                if (mob.isAlive() && !hawkSight[mob.pos] && level.distance(hawk.pos, mob.pos) <= radius)
                    senseAround(level, mob.pos);
            }
        }
        int range = observer.hasTalent(Talent.HEIGHTENED_SENSES) ? 1 + observer.pointsInTalent(Talent.HEIGHTENED_SENSES) : 0;
        if (observer.buff(DivineSense.DivineSenseTracker.class) != null)
            range = 4 + 4 * observer.pointsInTalent(Talent.DIVINE_SENSE);
        if (observer.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision.class) != null) range = Integer.MAX_VALUE;
        if (range > 0) for (Mob mob : level.mobs) {
            if (mob.isAlive() && !visible[mob.pos] && level.distance(observer.pos, mob.pos) <= range
                    && !Char.hasProp(mob, Char.Property.OBJECT)
                    && !(mob instanceof Mimic && mob.alignment == Char.Alignment.NEUTRAL && ((Mimic)mob).stealthy()))
                senseAround(level, mob.pos);
        }
        for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea area :
                observer.buffs(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea.class)) {
            if (area.depth != Dungeon.depth || area.branch != Dungeon.branch || !level.insideMap(area.pos)) continue;
            for (int offset : PathFinder.NEIGHBOURS9) {
                int cell = area.pos + offset;
                if (cell < 0 || cell >= length() || !level.discoverable[cell] || visible[cell]) continue;
                sensedTerrain.set(cell);
                reveal(level, cell);
                Mob mob = level.findMob(cell);
                if (mob != null && mob.isAlive() && mob.invisible == 0 && !Char.hasProp(mob, Char.Property.OBJECT)
                        && !(mob instanceof Mimic && ((Mimic)mob).stealthy())) sensedMobs.set(cell);
            }
        }
        for (TalismanOfForesight.CharAwareness mark : observer.buffs(TalismanOfForesight.CharAwareness.class)) {
            Actor marked = Actor.findById(mark.charID);
            if (marked instanceof Mob && level.mobs.contains(marked) && ((Mob)marked).isAlive()
                    && !Char.hasProp((Mob)marked, Char.Property.OBJECT)) senseAround(level, ((Mob)marked).pos);
        }
    }

    private void senseAround(Level level, int center) {
        if (!visible[center]) sensedMobs.set(center);
        for (int offset : PathFinder.NEIGHBOURS9) {
            int cell = center + offset;
            if (cell >= 0 && cell < length() && !visible[cell] && level.discoverable[cell]) {
                sensedTerrain.set(cell);
                reveal(level, cell);
            }
        }
    }

    public boolean sensed(int cell) { return sensedTerrain.get(cell); }
    public boolean sensedMob(int cell) { return sensedMobs.get(cell); }

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
