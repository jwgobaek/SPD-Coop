package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import java.util.HashSet;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;

/** One active floor and up to five dormant floors; only the active floor has registered actors. */
public final class CoopFloors {
    private final CoopSession game;
    public static final int LAST_SEWER = 5, FIRST_PRISON = 6;
    static int lastDepth(int schema) {
        return schema==4 ? FIRST_PRISON : schema==3 ? LAST_SEWER : schema;
    }
    private final java.util.Map<Integer,Bundle> dormant = new java.util.TreeMap<>();
    private long voteTurn;
    private int votedCell=-1;
    private final int[] positions=new int[2];

    CoopFloors(CoopSession game) { this.game=game; }

    public int stair() {
        Level l=Dungeon.level;
        if (Dungeon.branch!=0 || l.locked || Dungeon.depth<1 || Dungeon.depth>FIRST_PRISON) return -1;
        for (LevelTransition t : l.transitions) {
            if ((t.type!=LevelTransition.Type.REGULAR_EXIT && t.type!=LevelTransition.Type.REGULAR_ENTRANCE)
                    || t.destBranch!=0 || t.destDepth<1 || t.destDepth>FIRST_PRISON || Math.abs(t.destDepth-Dungeon.depth)!=1) continue;
            if (Dungeon.depth==LAST_SEWER && t.destDepth==FIRST_PRISON
                    && l.map[t.cell()]!=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.UNLOCKED_EXIT) continue;
            if (inRange(l,t)) return t.cell();
        }
        return -1;
    }

    private boolean inRange(Level l,LevelTransition t) {
        for(int i=0;i<2;i++) {
            Hero h=game.hero(i);
            if(h==null || game.life(i)!=CoopSession.Life.ALIVE || h.rooted || h.paralysed>0 ||
                    l.distance(h.pos,t.cell())>1 || !game.knowledge(h).visible(t.cell())) return false;
        }
        return true;
    }

    public static final class UnsupportedDestination extends RuntimeException {
        UnsupportedDestination(String mob) { super("Unsupported destination actor: "+mob); }
    }

    static void requireSupportedResidents(Level next) {
        java.util.SortedSet<String> blockers=new java.util.TreeSet<>();
        for (Mob mob:next.mobs) {
            if (!CoopSession.basicMob(mob)) {
                String detail=mob.getClass().getSimpleName();
                if (mob instanceof Statue) {
                    Weapon weapon=((Statue)mob).weapon();
                    detail+="/"+weapon.getClass().getSimpleName()+"/"+
                            (weapon.enchantment==null ? "plain" : weapon.enchantment.getClass().getSimpleName());
                }
                blockers.add(detail);
            }
        }
        if (!blockers.isEmpty()) throw new UnsupportedDestination(blockers.toString());
    }

    public boolean hasParkedFloor() { return !dormant.isEmpty(); }

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
        int from=Dungeon.depth;
        Dungeon.depth=old.getTransition(cell).destDepth;
        Bundle parked=dormant.remove(Dungeon.depth);
        Dungeon.level=null;
        Level next;
        if (parked!=null) {
            validateFloor(parked,Dungeon.depth);
            next=game.unpackFloor(parked);
        } else {
            if (Dungeon.levelHasBeenGenerated(Dungeon.depth,0)) throw new IllegalStateException("Missing parked floor");
            next=Dungeon.newLevel();
        }
        requireSupportedResidents(next);
        Dungeon.level=next;
        PathFinder.setMapSize(next.width(),next.height());
        if (parked!=null) {
            game.restoreItems(CoopSession.floorItems(next),parked.getBundle("items"),game.ownership.sequence(),new HashSet<>());
            for(int i=0;i<2;i++) game.knowledge(game.hero(i)).load(parked.getBundle("sight"+i));
        }
        int gate=Dungeon.depth>from ? next.entrance() : next.exit();
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
        dormant.put(from,leaving);
        cancel();
        return true;
    }

    static void checkLevelType(Bundle floor,int depth) {
        Class<?> expected=depth==FIRST_PRISON ? com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel.class
                : depth==5 ? com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel.class
                : com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel.class;
        if (!expected.getName().equals(floor.getString("__className")))
            throw new IllegalArgumentException("Unexpected level type at depth "+depth);
    }

    private static void validateFloor(Bundle record,int depth) {
        Bundle floor=record.getBundle("level");
        checkLevelType(floor,depth);
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

    private static void inspectItem(Bundle item,Bundle entry) {
        if(entry.isNull() || !item.getString("__className").equals(entry.getString("type")) ||
                item.getInt("quantity")<=0 || item.getInt("quantity")!=entry.getInt("quantity"))
            throw new IllegalArgumentException("Parked physical item mismatch");
    }

    int schema() {
        if (Dungeon.levelHasBeenGenerated(FIRST_PRISON,0)) return 4;
        return Dungeon.depth>2 || Dungeon.levelHasBeenGenerated(3,0) ? 3 : 2;
    }

    void save(Bundle state) {
        if (dormant.isEmpty()) return;
        if (schema()==2) state.put("parked",dormant.values().iterator().next());
        else {
            Bundle records=new Bundle();
            for (java.util.Map.Entry<Integer,Bundle> entry:dormant.entrySet())
                records.put(String.valueOf(entry.getKey()),entry.getValue());
            state.put("dormant",records);
        }
    }

    void restore(Bundle state,HashSet<Long> itemIds) {
        dormant.clear();
        cancel();
        int schema=state.getInt("schema"), bound=lastDepth(schema);
        boolean multi=schema>=3;
        HashSet<Integer> generated=new HashSet<>(Dungeon.generatedLevels);
        if (!generated.contains(Dungeon.depth) || generated.size()!=Dungeon.generatedLevels.size()
                || generated.size()>bound || (multi && !generated.contains(3)) || (schema==4 && !generated.contains(FIRST_PRISON)))
            throw new IllegalArgumentException("Invalid sewer generation history");
        for(int i=1;i<=generated.size();i++) if (!generated.contains(i)) throw new IllegalArgumentException("Disconnected sewer history");
        Bundle records=state.getBundle("dormant");
        if ((multi && state.contains("parked")) || (!multi && state.contains("dormant")))
            throw new IllegalArgumentException("Mixed floor formats");
        HashSet<Integer> actors=new HashSet<>();
        for (Actor actor:Actor.all()) actors.add(actor.id());
        for (int i=0;i<2;i++) {
            Hero hero=game.hero(i);
            actors.add(hero.id());
            for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:hero.buffs()) actors.add(buff.id());
        }
        int nextId=state.getBundle("world").getInt("nextid");
        for (int depth=1;depth<=bound;depth++) {
            boolean expected=depth!=Dungeon.depth && Dungeon.levelHasBeenGenerated(depth,0);
            boolean present=multi ? records.contains(String.valueOf(depth)) : depth==3-Dungeon.depth && state.contains("parked");
            if (expected!=present) throw new IllegalArgumentException("Missing or unexpected parked depth "+depth);
            if (!present) continue;
            Bundle record=multi ? records.getBundle(String.valueOf(depth)) : state.getBundle("parked");
            validateFloor(record,depth);
            inspectRecord(record,actors,nextId,itemIds);
            dormant.put(depth,record);
        }
        if (multi && records.getKeys().size()!=dormant.size()) throw new IllegalArgumentException("Unknown dormant depth");
    }

    private void inspectRecord(Bundle parked,HashSet<Integer> actors,int nextId,HashSet<Long> itemIds) {
        Bundle floor=parked.getBundle("level");
        for(Bundle mob:floor.getBundleArray("mobs")) {
            inspectActor(mob,actors,nextId);
            for(Bundle buff:mob.getBundleArray("buffs")) inspectActor(buff,actors,nextId);
        }
        for(Bundle blob:floor.getBundleArray("blobs")) inspectActor(blob,actors,nextId);
        if(floor.contains("respawner")) inspectActor(floor.getBundle("respawner"),actors,nextId);
        if(actors.size()>16384) throw new IllegalArgumentException("Sewer actor count");
        Bundle entries=parked.getBundle("items");
        int count=0;
        for(Bundle heap:floor.getBundleArray("heaps")) {
            int pos=heap.getInt("pos"), index=0;
            if(pos<0 || pos>=floor.getInt("width")*floor.getInt("height")) throw new IllegalArgumentException("Parked heap cell");
            for(Bundle item:heap.getBundleArray("items")) {
                inspectItem(item,entries.getBundle("f"+pos+"_"+index++));
                count++;
            }
        }
        for(Bundle mob:floor.getBundleArray("mobs")) {
            boolean armored=mob.getString("__className").equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue.class.getName());
            if (armored) {
                inspectItem(mob.getBundle("armor"),entries.getBundle("arm"+mob.getInt("id")));
                count++;
            }
            if (armored || mob.getString("__className").equals(Statue.class.getName())) {
                inspectItem(mob.getBundle("weapon"),entries.getBundle("w"+mob.getInt("id")));
                count++;
            }
            if (mob.getString("__className").equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Thief.class.getName()) && !mob.getBundle("item").isNull()) {
                inspectItem(mob.getBundle("item"),entries.getBundle("th"+mob.getInt("id")));
                count++;
            }
            if (!mob.contains("items")) continue;
            int index=0;
            for(Bundle item:mob.getBundleArray("items")) {
                inspectItem(item,entries.getBundle("m"+mob.getInt("id")+"_"+index++));
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
