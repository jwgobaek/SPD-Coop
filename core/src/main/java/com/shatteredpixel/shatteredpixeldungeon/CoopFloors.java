package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import java.util.HashSet;

/** One active sewer and at most one parked sewer; only the active floor has registered actors. */
public final class CoopFloors {
    private final CoopSession game;
    private Bundle parked;
    private long voteTurn;
    private int votedCell=-1;
    private final int[] positions=new int[2];

    CoopFloors(CoopSession game) { this.game=game; }

    public int stair() {
        Level l=Dungeon.level;
        if (Dungeon.branch!=0 || l.locked || (Dungeon.depth!=1 && Dungeon.depth!=2)) return -1;
        LevelTransition t=l.getTransition(Dungeon.depth==1 ? LevelTransition.Type.REGULAR_EXIT : LevelTransition.Type.REGULAR_ENTRANCE);
        if (t==null || t.destBranch!=0 || t.destDepth!=3-Dungeon.depth) return -1;
        for(int i=0;i<2;i++) {
            Hero h=game.hero(i);
            if(h==null || game.life(i)!=CoopSession.Life.ALIVE || h.rooted || h.paralysed>0 ||
                    l.distance(h.pos,t.cell())>1 || !game.knowledge(h).visible(t.cell())) return -1;
        }
        return t.cell();
    }

    static final class UnsupportedDestination extends RuntimeException {
        UnsupportedDestination(String mob) { super("Unsupported destination actor: "+mob); }
    }

    public boolean hasParkedFloor() { return parked!=null; }

    public void cancel() { votedCell=-1; }
    public boolean consent(int seat,long turn,int cell,boolean withdraw) {
        game.checkExecutor();
        if (game.activeSeat()<0 || game.activeSeat()==seat || turn!=game.turn() || cell<0 || cell!=stair()) return false;
        if (withdraw) {
            if (!agreed()) return false;
            cancel();
        } else {
            if (agreed()) return false;
            voteTurn=turn;
            votedCell=cell;
            for(int i=0;i<2;i++) positions[i]=game.hero(i).pos;
        }
        return true;
    }

    public boolean agreed() {
        return votedCell>=0 && voteTurn==game.turn() && votedCell==stair() &&
                game.hero(0).pos==positions[0] && game.hero(1).pos==positions[1];
    }

    boolean travel(int seat,int cell) {
        if (seat!=game.activeSeat() || cell!=votedCell || !agreed()) return false;
        Level old=Dungeon.level;
        game.leaveFloor();
        Level.beforeTransition(game.hero(0));
        Level.beforeTransition(game.hero(1));
        Actor.leaveCoopFloor();
        Bundle leaving=new Bundle();
        leaving.put("branch",Dungeon.branch);
        leaving.put("depth",Dungeon.depth);
        leaving.put("level",old);
        leaving.put("items",game.itemState(CoopSession.floorItems(old)));
        for(int i=0;i<2;i++) leaving.put("sight"+i,game.knowledge(game.hero(i)).save());
        validateFloor(leaving, Dungeon.depth);
        Dungeon.depth=3-Dungeon.depth;
        Dungeon.level=null;
        Level next;
        if (parked!=null) {
            validateFloor(parked,Dungeon.depth);
            next=game.unpackFloor(parked);
        } else {
            if (Dungeon.levelHasBeenGenerated(Dungeon.depth,0)) throw new IllegalStateException("Missing parked floor");
            next=Dungeon.newLevel();
        }
        for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:next.mobs) {
            if (!CoopSession.basicMob(mob)) throw new UnsupportedDestination(mob.getClass().getSimpleName());
        }
        Dungeon.level=next;
        PathFinder.setMapSize(next.width(),next.height());
        if (parked!=null) {
            game.restoreItems(CoopSession.floorItems(next),parked.getBundle("items"),game.ownership.sequence(),new HashSet<>());
            for(int i=0;i<2;i++) game.knowledge(game.hero(i)).load(parked.getBundle("sight"+i));
        }
        int gate=Dungeon.depth==2 ? next.entrance() : next.exit();
        int[] offsets=new int[9];
        System.arraycopy(PathFinder.NEIGHBOURS8,0,offsets,1,8);
        int placed=0;
        for(int offset:offsets) {
            int pos=gate+offset;
            if (!next.insideMap(pos) || !next.passable[pos] || next.pit[pos] || next.traps.get(pos)!=null ||
                    next.plants.get(pos)!=null || next.findMob(pos)!=null) continue;
            Hero h=game.hero(placed++);
            h.pos=pos;
            h.fieldOfView=new boolean[next.length()];
            h.viewDistance=next.viewDistance;
            h.ready=false;
            h.curAction=h.lastAction=null;
            if (placed==2) break;
        }
        if (placed!=2) throw new IllegalStateException("No safe arrival pair");
        Actor.add(game.hero(1));
        Actor.init();
        if (next.respawner()==null) next.addRespawner(); else Actor.add(next.respawner());
        for(Char ch:Actor.chars()) game.attachSprite(ch);
        game.restoreBeeHomes();
        parked=leaving;
        cancel();
        return true;
    }

    private static void validateFloor(Bundle record,int depth) {
        Bundle floor=record.getBundle("level");
        int w=floor.getInt("width"),h=floor.getInt("height");
        if(record.getInt("branch")!=0 || record.getInt("depth")!=depth || w<3 || h<3 || w>256 || h>256 ||
                floor.getIntArray("map").length!=w*h || record.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1500000)
            throw new IllegalArgumentException("Invalid parked floor");
        for(int i=0;i<2;i++) new FloorKnowledge(w*h).load(record.getBundle("sight"+i));
    }

    private static void inspectActor(Bundle raw,HashSet<Integer> ids,int nextId) {
        int id=raw.getInt("id");
        float due=raw.getFloat("time");
        if(id<1 || id>=nextId || !ids.add(id) || !(due>=0 && due<=Float.MAX_VALUE))
            throw new IllegalArgumentException("Parked actor identity or clock");
    }

    void save(Bundle state) { if(parked!=null) state.put("parked",parked); }

    void restore(Bundle state,HashSet<Long> itemIds) {
        parked=null; cancel();
        boolean expected=Dungeon.levelHasBeenGenerated(3-Dungeon.depth,0);
        if (expected!=state.contains("parked")) throw new IllegalArgumentException("Missing or unexpected floor");
        if (!expected) return;
        parked=state.getBundle("parked");
        validateFloor(parked,3-Dungeon.depth);
        Bundle floor=parked.getBundle("level");
        HashSet<Integer> actors=new HashSet<>();
        for(Actor a:Actor.all()) actors.add(a.id());
        int nextId=state.getBundle("world").getInt("nextid");
        for(Bundle mob:floor.getBundleArray("mobs")) {
            inspectActor(mob,actors,nextId);
            for(Bundle buff:mob.getBundleArray("buffs")) inspectActor(buff,actors,nextId);
        }
        for(Bundle blob:floor.getBundleArray("blobs")) inspectActor(blob,actors,nextId);
        if(floor.contains("respawner")) inspectActor(floor.getBundle("respawner"),actors,nextId);
        if(actors.size()>16384) throw new IllegalArgumentException("Two-floor actor count");
        Bundle entries=parked.getBundle("items");
        int count=0;
        for(Bundle heap:floor.getBundleArray("heaps")) {
            int pos=heap.getInt("pos"), index=0;
            if(pos<0 || pos>=floor.getInt("width")*floor.getInt("height")) throw new IllegalArgumentException("Parked heap cell");
            for(Bundle item:heap.getBundleArray("items")) {
                Bundle entry=entries.getBundle("f"+pos+"_"+index++);
                if(entry.isNull() || !item.getString("__className").equals(entry.getString("type")) ||
                        item.getInt("quantity")<=0 || item.getInt("quantity")!=entry.getInt("quantity"))
                    throw new IllegalArgumentException("Parked physical item mismatch");
                count++;
            }
        }
        if(count!=entries.getKeys().size()) throw new IllegalArgumentException("Parked inventory size");
        if(entries.getKeys().size()>8192) throw new IllegalArgumentException("Parked item count");
        for(String key:entries.getKeys()) {
            Bundle e=entries.getBundle(key);
            long id=e.getLong("id"),owner=e.getLong("owner");
            if(id<1 || id>game.ownership.sequence() || !itemIds.add(id) ||
                    (owner!=0 && owner!=game.player(0) && owner!=game.player(1)))
                throw new IllegalArgumentException("Invalid parked item identity");
        }
    }
}
