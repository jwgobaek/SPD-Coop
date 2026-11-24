package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import java.util.*;

/** Host-only projection. Handles survive continuous observation, including floor-to-bag movement. */
final class SeatProjection {
    final int seat;
    final UUID session;
    private long nextItem;
    private IdentityHashMap<Item,Long> ids = new IdentityHashMap<>();
    private Map<Long,Item> items = Collections.emptyMap();
    StateImage previous, current;
    String witnessedDeath;

    SeatProjection(UUID session, int seat) {
        this.session = session;
        this.seat = seat;
        nextItem = seat + 1;
    }

    UUID epoch;
    long cursor() { return nextItem; }
    void resetHandles(long start) {
        if (start < seat+1 || start % 2 != (seat+1)%2 || start > Long.MAX_VALUE-32768)
            throw new IllegalArgumentException("Invalid handle cursor");
        nextItem = start;
        ids.clear(); items=Collections.emptyMap();
        previous=null; current=null;
    }

    Item resolve(long handle) { return items.get(handle); }

    void capture(CoopSession game, long version, String status, Mob attacked,String receipt) {
        Map<String,String> rows = new TreeMap<>();
        Map<Long,Item> visibleItems = new HashMap<>();
        boolean playing=status.equals("IN_GAME");
        boolean defeated = status.equals("PARTY_DEFEATED");
        rows.put("status",playing || defeated ? status : "PAUSED");
        if (epoch!=null) rows.put("epoch",epoch.toString());
        rows.put("life",game.life(seat).name());
        rows.put("revivable",game.life(seat) == CoopSession.Life.DOWNED ? "1" : "0");
        if (!playing && !defeated) rows.put("reason",status);
        if (receipt!=null) rows.put("receipt",receipt);
        rows.put("clock",Float.toString(Statistics.duration+com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()));
        rows.put("turn",Long.toString(game.turn()));
        rows.put("active",Integer.toString(playing ? game.activeSeat() : -1));
        witnessedDeath = null;
        if (playing || status.equals("RECONNECTING")) {
            Hero hero = game.hero(seat);
            Level level = Dungeon.level;
            FloorKnowledge sight = game.observe(seat);
            writeMemory(rows, sight);
            for (BitSet area : CombatZones.calculate(game, sight)) {
                for (int cell = area.nextSetBit(0); cell >= 0; cell = area.nextSetBit(cell+1)) {
                    int mask = CombatZones.edgeMask(area, cell, level.width());
                    if (mask != 0) rows.put("z"+cell, Integer.toString(mask));
                }
            }
            rows.put("width",Integer.toString(level.width()));
            rows.put("self",hero.id()+","+hero.pos+","+hero.HP+","+hero.HT+","+hero.heroClass.name()+","+hero.lvl+","+hero.exp);
            Hero ally = game.hero(1-seat);
            if (sight.visible(ally.pos) && ally.invisible <= 0) {
                rows.put("ally",ally.id()+","+ally.pos+","+ally.heroClass.name());
                rows.put("allylife",game.life(1-seat).name());
            }
            for (Buff buff : hero.buffs()) {
                if (buff.icon() != BuffIndicator.NONE) rows.put("b"+buff.id(),buff.icon()+","+
                        (buff instanceof FlavourBuff ? Float.toString(buff.visualcooldown()) : "NA"));
            }
            for (int cell=0;cell<sight.length();cell++) {
                if (!sight.visible(cell)) continue;
                rows.put("t"+cell,Integer.toString(sight.terrain(cell)));
                Heap heap = level.heaps.get(cell);
                if (heap != null && heap.hidden) continue;
                if (heap != null && heap.type == Heap.Type.HEAP && heap.peek()!=null) {
                    putItem(rows,visibleItems,heap.peek(),"F"+cell);
                } else if (heap != null) rows.put("h"+cell,"CONTAINER");
            }
            for (Mob mob : level.mobs) {
                if (sight.visible(mob.pos) && mob.invisible <= 0 && mob.isAlive() && CoopSession.basicMob(mob))
                    rows.put("m"+mob.id(),mob.pos+","+mob.getClass().getSimpleName()+","+mob.HP+","+mob.HT);
            }
            if (attacked != null && !attacked.isAlive() && sight.visible(attacked.pos) && current != null) {
                String key = "m"+attacked.id();
                String old = current.rows.get(key);
                if (old != null && old.startsWith(attacked.pos+",")) witnessedDeath = key;
            }
            for (Item item : hero.belongings) putItem(rows,visibleItems,item,slot(hero.belongings,item));
        }
        ids.keySet().retainAll(visibleItems.values());
        items = visibleItems;
        previous = current;
        current = new StateImage(session,seat,version,rows);
    }

    private static void writeMemory(Map<String,String> rows, FloorKnowledge memory) {
        // Each group fits a single bounded SYNC1 value even on a fully explored floor.
        for (int start = 0; start < memory.length(); start += StateImage.TILE_GROUP) {
            StringBuilder group = new StringBuilder();
            boolean known = false;
            for (int cell = start; cell < start + StateImage.TILE_GROUP; cell++) {
                int tile = cell < memory.length() && !memory.visible(cell) ? memory.terrain(cell) : -1;
                if (tile < 0) group.append("..");
                else {
                    known = true;
                    group.append(memory.visited(cell) ? 'v' : 'p');
                    group.append(StateImage.TILES.charAt(tile));
                }
            }
            if (known) rows.put("r" + start / StateImage.TILE_GROUP, group.toString());
        }
    }

    private void putItem(Map<String,String> rows, Map<Long,Item> visible, Item item,String slot) {
        Long id = ids.get(item);
        if (id == null) {
            id = nextItem;
            if (nextItem > Long.MAX_VALUE-2) throw new IllegalStateException("Item handle exhaustion");
            nextItem += 2;
            ids.put(item,id);
        }
        visible.put(id,item);
        String kind = item.getClass() == Food.class ? "FOOD" : item instanceof Ankh ? "ANKH" : item instanceof MeleeWeapon ? "MELEE" : "OTHER";
        rows.put("i"+id,slot+","+kind+","+item.image()+","+item.quantity()+","+
                (item.levelKnown ? item.visiblyUpgraded() : "NA"));
    }

    private static String slot(Belongings b, Item item) {
        if (item == b.weapon) return "WEAPON";
        if (item == b.armor) return "ARMOR";
        if (item == b.ring) return "RING";
        if (item == b.misc) return "MISC";
        if (item == b.artifact) return "ARTIFACT";
        if (item == b.secondWep) return "SECOND";
        return "BAG";
    }
}
