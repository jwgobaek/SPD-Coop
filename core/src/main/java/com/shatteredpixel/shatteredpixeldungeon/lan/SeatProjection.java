package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom.Sentry;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
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
        String floorId=Dungeon.branch+","+Dungeon.depth;
        boolean changed=current!=null && !floorId.equals(current.rows.get("floor"));
        if(changed) resetHandles(nextItem);
        rows.put("floor",floorId);
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
            for (com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap tile : level.customTiles) {
                if (!(tile instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.GooBossRoom.GooNest)) continue;
                com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.GooBossRoom.GooNest nest =
                        (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.GooBossRoom.GooNest)tile;
                for (int y=0;y<nest.tileH;y++) for (int x=0;x<nest.tileW;x++) {
                    int cell=(nest.tileY+y)*level.width()+nest.tileX+x;
                    int sprite=nest.visualAt(x,y);
                    if (sprite>=0 && (sight.visible(cell) || sight.visited(cell)))
                        rows.put("nest"+cell,Integer.toString(sprite));
                }
            }
            int stair=game.stairs.stair();
            if(stair>=0) rows.put("stair",stair+","+(game.stairs.agreed()?1:0));
            for (BitSet area : CombatZones.calculate(game, sight)) {
                for (int cell = area.nextSetBit(0); cell >= 0; cell = area.nextSetBit(cell+1)) {
                    int mask = CombatZones.edgeMask(area, cell, level.width());
                    if (mask != 0) rows.put("z"+cell, Integer.toString(mask));
                }
            }
            rows.put("keys",Integer.toString(com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(
                    new com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey(Dungeon.depth))));
            rows.put("width",Integer.toString(level.width()));
            rows.put("height",Integer.toString(level.height()));
            rows.put("shield",Integer.toString(hero.shielding()));
            rows.put("self",hero.id()+","+hero.pos+","+hero.HP+","+hero.HT+","+hero.heroClass.name()+","+hero.lvl+","+hero.exp);
            rows.put("selftier",Integer.toString(hero.tier()));
            Hero ally = game.hero(1-seat);
            if (sight.visible(ally.pos) && ally.invisible <= 0) {
                rows.put("ally",ally.id()+","+ally.pos+","+ally.heroClass.name());
                rows.put("allytier",Integer.toString(ally.tier()));
                rows.put("allylife",game.life(1-seat).name());
            }
            for (Buff buff : hero.buffs()) {
                if (buff.icon() != BuffIndicator.NONE) rows.put("b"+buff.id(),buff.icon()+","+
                        (buff instanceof FlavourBuff ? Float.toString(buff.visualcooldown()) : "NA"));
            }
            StenchGas stench = (StenchGas)level.blobs.get(StenchGas.class);
            for (int cell=0;cell<sight.length();cell++) {
                if (!sight.visible(cell)) continue;
                rows.put("t"+cell,Integer.toString(sight.terrain(cell)));
                if (stench != null && stench.volume>0 && stench.cur[cell]>0) rows.put("stench"+cell,"1");
                com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap trap = level.traps.get(cell);
                Plant plant = level.plants.get(cell);
                if (trap == null && plant != null) rows.put("plant"+cell, Integer.toString(plant.image));
                if (trap != null && trap.visible) {
                    int color = trap.active ? trap.color : com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap.BLACK;
                    rows.put("trap"+cell,Integer.toString(color + 16*trap.shape));
                }
                int clouds = (Blob.volumeAt(cell, Fire.class)>0 ? 1 : 0)
                        | (Blob.volumeAt(cell, Freezing.class)>0 ? 2 : 0)
                        | (Blob.volumeAt(cell, Electricity.class)>0 ? 4 : 0)
                        | (Blob.volumeAt(cell, ToxicGas.class)>0 ? 8 : 0)
                        | (Blob.volumeAt(cell, ConfusionGas.class)>0 ? 16 : 0);
                if (clouds != 0) rows.put("cloud"+cell,Integer.toString(clouds));
                Heap heap = level.heaps.get(cell);
                if (heap != null && heap.hidden) continue;
                if (heap != null && heap.type == Heap.Type.HEAP && heap.peek()!=null) {
                    putItem(rows,visibleItems,heap.peek(),"F"+cell);
                } else if (heap != null) rows.put("h"+cell,
                        heap.type==Heap.Type.CHEST || heap.type==Heap.Type.CRYSTAL_CHEST || heap.type==Heap.Type.LOCKED_CHEST ? heap.type.name() : "CONTAINER");
            }
            for (Mob mob : level.mobs) {
                if (sight.visible(mob.pos) && mob.invisible <= 0 && mob.isAlive() && CoopSession.basicMob(mob)) {
                    if (CoopSession.disguised(mob)) {
                        Heap.Type chest = Heap.Type.CHEST;
                        if (mob instanceof CrystalMimic) chest = Heap.Type.CRYSTAL_CHEST;
                        if (mob instanceof GoldenMimic) chest = Heap.Type.LOCKED_CHEST;
                        rows.put("h"+mob.pos, chest.name());
                        continue;
                    }
                    rows.put("m"+mob.id(),mob.pos+","+mob.getClass().getSimpleName()+","+mob.HP+","+mob.HT);
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo && ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo)mob).chargeStage()>0)
                        rows.put("charge"+mob.pos,Integer.toString(((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo)mob).chargeStage()));
                    if (mob instanceof Ghost && level.adjacent(hero.pos,mob.pos))
                        rows.put("quest"+mob.pos,Ghost.Quest.sewerOffer());
                    if (mob instanceof Sentry
                            && ((Sentry)mob).charging())
                        rows.put("charge"+mob.pos,"1");
                }
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
        String kind = item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Honeypot ? "HONEYPOT"
                : item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Honeypot.ShatteredPot ? "SHELL"
                : item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow ? "BOW"
                : item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.class && ((com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion)item).isKnown() ? "HEALING"
                : CoopSession.basicArmorType(item) ? "ARMOR" : CoopSession.basicFood(item) ? "FOOD" : item instanceof Ankh ? "ANKH" : CoopSession.basicWeaponType(item) ? "MELEE" : "OTHER";
        String charges="";
        if (!slot.startsWith("F") && item instanceof MagesStaff) {
            WandOfMagicMissile wand=((MagesStaff)item).starterWand();
            if (wand!=null) { kind="STAFF"; charges=","+wand.curCharges+","+wand.maxCharges; }
        }
        if (!slot.startsWith("F") && !kind.equals("OTHER") && item.getClass().getEnclosingClass()==null) {
            String label=item.getClass().getName();
            String prefix="com.shatteredpixel.shatteredpixeldungeon.";
            if (label.startsWith(prefix+"items."))
                rows.put("label"+id,label.substring(prefix.length()).toLowerCase(Locale.ROOT));
        }
        rows.put("i"+id,slot+","+kind+","+item.image()+","+item.quantity()+","+
                (item.levelKnown ? item.visiblyUpgraded() : "NA")+charges);
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
