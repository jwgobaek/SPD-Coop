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

    Item resolve(long handle) { return items.get(handle); }

    void capture(CoopSession game, long version, String status, Mob attacked,String receipt) {
        Map<String,String> rows = new TreeMap<>();
        Map<Long,Item> visibleItems = new HashMap<>();
        boolean playing=status.equals("IN_GAME");
        rows.put("status",playing ? status : "PAUSED");
        if (!playing) rows.put("reason",status);
        if (receipt!=null) rows.put("receipt",receipt);
        rows.put("clock",Float.toString(Statistics.duration+com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()));
        rows.put("turn",Long.toString(game.turn()));
        rows.put("active",Integer.toString(playing ? game.activeSeat() : -1));
        witnessedDeath = null;
        if (playing) {
            Hero hero = game.hero(seat);
            Level level = Dungeon.level;
            boolean[] sight = new boolean[level.length()];
            level.updateDirectSight(hero,sight);
            sight[hero.pos] = true;
            rows.put("width",Integer.toString(level.width()));
            rows.put("self",hero.id()+","+hero.pos+","+hero.HP+","+hero.HT+","+hero.heroClass.name()+","+hero.lvl+","+hero.exp);
            Hero ally = game.hero(1-seat);
            if (sight[ally.pos] && ally.invisible <= 0) rows.put("ally",ally.id()+","+ally.pos+","+ally.heroClass.name());
            for (Buff buff : hero.buffs()) {
                if (buff.icon() != BuffIndicator.NONE) rows.put("b"+buff.id(),buff.icon()+","+
                        (buff instanceof FlavourBuff ? Float.toString(buff.visualcooldown()) : "NA"));
            }
            for (int cell=0;cell<sight.length;cell++) {
                if (!sight[cell] || !level.discoverable[cell]) continue;
                int tile = level.map[cell];
                if (tile == Terrain.SECRET_DOOR) tile = Terrain.WALL;
                if (tile == Terrain.SECRET_TRAP) tile = Terrain.EMPTY;
                rows.put("t"+cell,Integer.toString(tile));
                Heap heap = level.heaps.get(cell);
                if (heap != null && heap.type == Heap.Type.HEAP && heap.peek()!=null) {
                    putItem(rows,visibleItems,heap.peek(),"F"+cell);
                } else if (heap != null) rows.put("h"+cell,"CONTAINER");
            }
            for (Mob mob : level.mobs) {
                if (sight[mob.pos] && mob.invisible <= 0 && mob.isAlive() && CoopSession.basicMob(mob))
                    rows.put("m"+mob.id(),mob.pos+","+mob.getClass().getSimpleName()+","+mob.HP+","+mob.HT);
            }
            if (attacked != null && !attacked.isAlive() && sight[attacked.pos] && current != null) {
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

    private void putItem(Map<String,String> rows, Map<Long,Item> visible, Item item,String slot) {
        Long id = ids.get(item);
        if (id == null) {
            id = nextItem;
            if (nextItem > Long.MAX_VALUE-2) throw new IllegalStateException("Item handle exhaustion");
            nextItem += 2;
            ids.put(item,id);
        }
        visible.put(id,item);
        String kind = item.getClass() == Food.class ? "FOOD" : item instanceof MeleeWeapon ? "MELEE" : "OTHER";
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
