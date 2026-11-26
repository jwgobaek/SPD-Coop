package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.watabou.utils.*;
import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoopFloorsTest extends HeadlessDungeon {
    public static boolean step(CoopSession game,CoopSession.Action verb,int cell,Item item) {
        return game.act(game.player(game.activeSeat()),game.turn(),verb,cell,item);
    }

    // The guide can inspect the seed's map. Every movement and hit is a normal host action.
    public static int nextStep(CoopSession game,int goal) {
        return nextStep(game,goal,false,false);
    }

    static int nextStep(CoopSession game,int goal,boolean avoidOccupiedCorridors,boolean allowGrass) {
        Level l=Dungeon.level;
        int start=game.hero(game.activeSeat()).pos;
        if(l.distance(start,goal)<=1) return start;
        int[] first=new int[l.length()];
        Arrays.fill(first,-1);
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        queue.add(start); first[start]=start;
        while(!queue.isEmpty()) {
            int p=queue.remove();
            for(int off:PathFinder.NEIGHBOURS8) {
                int c=p+off;
                if(!l.insideMap(c) || first[c]>=0 || (!l.passable[c] && l.map[c]!=Terrain.SECRET_DOOR) || l.pit[c] || l.getTransition(c)!=null ||
                        l.plants.get(c)!=null || l.traps.get(c)!=null || (p==start && Actor.findChar(c)!=null)
                        || (avoidOccupiedCorridors && Actor.findChar(c) instanceof Hero && Actor.findChar(c)!=game.hero(game.activeSeat()))) continue;
                Heap heap=l.heaps.get(c);
                if(heap!=null && heap.type!=Heap.Type.HEAP) continue;
                switch(l.map[c]) {
                    case Terrain.SECRET_DOOR: case Terrain.EMPTY: case Terrain.EMPTY_SP: case Terrain.GRASS: case Terrain.WATER:
                    case Terrain.EMBERS: case Terrain.EMPTY_DECO: case Terrain.DOOR: case Terrain.OPEN_DOOR: break;
                    case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
                        if(allowGrass) break;
                        continue;
                    default: continue;
                }
                first[c]=p==start?c:first[p];
                if(l.distance(c,goal)<=1) return first[c];
                queue.add(c);
            }
        }
        return -1;
    }
    public static String guide(CoopSession game) {
        int seat=game.activeSeat();
        Hero h=game.hero(seat);
        Level l=Dungeon.level;
        if(game.stairs.stair()>=0) return "STAIR "+game.stairs.stair();
        for(Mob m:l.mobs) if(l.adjacent(h.pos,m.pos) && game.observe(seat).visible(m.pos))
            return "ATTACK "+m.pos;
        int next=nextStep(game,l.exit());
        if(next<0 || next==h.pos) return "WAIT 0";
        if(l.map[next]==Terrain.SECRET_DOOR) return "SEARCH "+h.pos;
        return "MOVE "+next;
    }

    static void cross(CoopSession game) {
        int cell=game.stairs.stair();
        assertTrue(cell>=0);
        int other=1-game.activeSeat();
        assertFalse(step(game,CoopSession.Action.TRAVEL,cell,null));
        assertTrue(game.stairs.consent(other,game.turn(),cell,false));
        assertTrue(game.stairs.consent(other,game.turn(),cell,true));
        assertFalse(step(game,CoopSession.Action.TRAVEL,cell,null));
        assertTrue(game.stairs.consent(other,game.turn(),cell,false));
        boolean traveled=step(game,CoopSession.Action.TRAVEL,cell,null);
        assertTrue("Travel stopped="+game.stopReason()+" blocker="+game.destinationBlocker,traveled);
    }

    @Test public void inaccessibleSpecialRoomRetainsLastPlayableFloor() {
        // Seed 530 still generates an unverified Whip/Vorpal armored statue on floor two.
        try(CoopSession game=CoopSession.open(530,1,HeroClass.MAGE)) {
            game.join(2,HeroClass.WARRIOR);
            Level l=Dungeon.level;
            int gate=l.exit(),seat=0;
            for(int off:PathFinder.NEIGHBOURS8) {
                int p=gate+off;
                if(l.passable[p] && Actor.findChar(p)==null && !l.pit[p]) game.hero(seat++).pos=p;
                if(seat==2) break;
            }
            for(int i=0;i<2;i++) game.observe(i);
            assertTrue(game.stairs.consent(1-game.activeSeat(),game.turn(),gate,false));
            Bundle saved=game.checkpoint();
            assertFalse(step(game,CoopSession.Action.TRAVEL,gate,null));
            assertTrue(game.restoredAfterTravel());
            assertEquals(1,Dungeon.depth);
            assertFalse(game.stopped());
            Bundle rebuilt=game.checkpoint();
            assertEquals(saved.getLong("turn"),rebuilt.getLong("turn"));
            assertArrayEquals(saved.getLongArray("random"),rebuilt.getLongArray("random"));
            assertArrayEquals(saved.getBundle("floor").getIntArray("map"),rebuilt.getBundle("floor").getIntArray("map"));
            assertEquals(saved.getBundle("world").getInt("nextid"),rebuilt.getBundle("world").getInt("nextid"));
            assertFalse(game.stairs.agreed());
            assertTrue(step(game,CoopSession.Action.WAIT,0,null));
        }
    }

    @Test public void previousAlphaEngineImageRetainsRosterRandomAndDownedSeat() throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/coop/schema1-engine.gz")) {
            assertNotNull(in);
            Bundle old=Bundle.read(in);
            assertEquals(1,old.getInt("schema"));
            try(CoopSession restored=CoopSession.restore(old)) {
                Bundle updated=restored.checkpoint();
                assertEquals(2,updated.getInt("schema"));
                assertArrayEquals(old.getLongArray("random"),updated.getLongArray("random"));
                assertArrayEquals(old.getLongArray("players"),updated.getLongArray("players"));
                assertEquals(old.getLong("nextItem"),updated.getLong("nextItem"));
                assertEquals(old.getLong("turn"),updated.getLong("turn"));
                assertEquals(old.getBundle("guest").getInt("id"),restored.hero(1).id());
                assertEquals(CoopSession.Life.DOWNED,restored.life(1));
                assertEquals(old.getInt("before1"),restored.preDeathHP(1));
                for(String k:old.getBundle("items").getKeys()) {
                    Bundle a=old.getBundle("items").getBundle(k),b=updated.getBundle("items").getBundle(k);
                    assertEquals(a.getLong("id"),b.getLong("id"));assertEquals(a.getLong("owner"),b.getLong("owner"));
                }
                assertTrue(step(restored,CoopSession.Action.REVIVE,restored.hero(1).pos,null));
                assertEquals(5,restored.hero(1).HP);
            }
        }
    }

    @Test public void walkBetweenGeneratedSewersPreserveDormantWorldAndResume() throws Exception {
        walkAndRestore(Long.getLong("coop.stair.seed",415L));
    }

    @Test public void walkFromSpawnOnTheNaturalMimicAndStatueSeeds() throws Exception {
        for(long seed:new long[]{408,411,416,420}) walkAndRestore(seed);
    }

    @Test public void exploreFromNativeEntranceBeforeTheUnstableAndGoldenFloors() throws Exception {
        for(long seed:new long[]{421,427,428,434,435,437}) walkAndRestore(seed);
    }

    private void walkAndRestore(long seed) throws Exception {
        File disk=temporary.newFile();
        int[] playerIds=new int[2],map;
        Map<Integer,String> monsters=new TreeMap<>();
        long owned; int drop,steps=0,attacks=0,searches=0,casts=0;
        boolean newRoute=seed==421 || seed==427 || seed==428 || seed==434 || seed==435 || seed==437;
        try(CoopSession game=CoopSession.open(seed,1,HeroClass.MAGE)) {
            game.join(2,newRoute ? HeroClass.MAGE : HeroClass.WARRIOR);
            com.watabou.utils.Random.pushGenerator(seed);
            Level l=Dungeon.level;
            drop=game.hero(0).pos;
            Food ration=game.hero(0).belongings.getItem(Food.class);
            assertTrue(step(game,CoopSession.Action.DROP,drop,ration));
            owned=game.ownership.identity(l.heaps.get(drop).peek());
            for(int i=0;i<2;i++) playerIds[i]=game.hero(i).id();
            for(int i=0;i<(newRoute?1000:250) && game.stairs.stair()<0;i++) {
                int seat=game.activeSeat();
                assertTrue("Party defeated on seeded route "+seed,seat>=0);
                Hero h=game.hero(seat);
                // On the additional routes take a visible clear missile shot before advancing
                // into melee. The baseline 415 input policy stays pinned.
                if(seed!=415 && h.heroClass==HeroClass.MAGE) {
                    Mob nearest=null;
                    for(Mob enemy:l.mobs) {
                        if(enemy.alignment!=Char.Alignment.ENEMY || !game.observe(seat).visible(enemy.pos)) continue;
                        if(nearest==null || l.distance(h.pos,enemy.pos)<l.distance(h.pos,nearest.pos)
                                || (l.distance(h.pos,enemy.pos)==l.distance(h.pos,nearest.pos) && enemy.id()<nearest.id()))
                            nearest=enemy;
                    }
                    if(nearest!=null && step(game,CoopSession.Action.ZAP,nearest.pos,h.belongings.weapon())) {
                        casts++;
                        continue;
                    }
                }
                if (newRoute && h.HP<h.HT*3/4 && h.getVisibleEnemies().isEmpty()) {
                    com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger hunger=h.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger.class);
                    if (hunger!=null && hunger.isStarving()) {
                        Food meal=h.belongings.getItem(Food.class);
                        if(meal!=null && step(game,CoopSession.Action.EAT,0,meal)) continue;
                    } else {
                        assertTrue(step(game,CoopSession.Action.WAIT,0,null));
                        continue;
                    }
                }
                int target=-1;
                for(Mob m:l.mobs) if(m.alignment==Char.Alignment.ENEMY && l.adjacent(h.pos,m.pos)
                        && game.observe(seat).visible(m.pos) && (target<0 || m.pos<target)) target=m.pos;
                if(target!=-1) {
                    assertTrue(step(game,CoopSession.Action.ATTACK,target,null));
                    attacks++;
                } else {
                    int next=nextStep(game,l.exit(),seed!=415,newRoute);
                    if(next<0 || next==h.pos) assertTrue(step(game,CoopSession.Action.WAIT,0,null));
                    else if(l.map[next]==Terrain.SECRET_DOOR) {
                        assertTrue(step(game,CoopSession.Action.SEARCH,h.pos,null));
                        searches++;
                    }
                    else { assertTrue(step(game,CoopSession.Action.MOVE,next,null)); steps++; }
                }
            }
            assertTrue("No supported approach on seed "+seed+" walked "+steps+" attacked "+attacks+" positions "+game.hero(0).pos+"/"+game.hero(1).pos+" exit "+l.exit()+" life "+game.life(0)+"/"+game.life(1)+" hp "+game.hero(0).HP+"/"+game.hero(1).HP,game.stairs.stair()>=0);
            assertTrue(steps>1);
            for(Mob m:l.mobs) monsters.put(m.id(),m.pos+","+m.HP);
            map=l.map.clone();
            float time=Statistics.duration+Actor.now();
            cross(game);
            assertEquals(2,Dungeon.depth); assertNotSame(l,Dungeon.level);
            assertEquals(time,Statistics.duration+Actor.now(),0.001);
            assertNotEquals(game.hero(0).pos,game.hero(1).pos);
            for(int id:monsters.keySet()) assertNull(Actor.findById(id));
            assertTrue(step(game,CoopSession.Action.WAIT,0,null));
            try(OutputStream out=new FileOutputStream(disk)) {assertTrue(Bundle.write(game.checkpoint(),out,false));}
        }
        try(InputStream in=new FileInputStream(disk);CoopSession game=CoopSession.restore(Bundle.read(in))) {
            assertEquals(2,Dungeon.depth);
            for(int i=0;i<2;i++) assertEquals(playerIds[i],game.hero(i).id());
            int inactive=1-game.activeSeat();
            Hero immobile=game.hero(inactive);
            int valid=game.stairs.stair();
            assertTrue(game.stairs.consent(inactive,game.turn(),valid,false));
            immobile.rooted=true;
            assertFalse(game.stairs.agreed());
            assertFalse(step(game,CoopSession.Action.TRAVEL,valid,null));
            immobile.rooted=false;
            assertTrue(step(game,CoopSession.Action.WAIT,0,null));
            assertFalse(game.stairs.agreed());
            cross(game);
            assertArrayEquals(map,Dungeon.level.map);
            for(Mob m:Dungeon.level.mobs) assertEquals(monsters.get(m.id()),m.pos+","+m.HP);
            assertEquals(monsters.size(),Dungeon.level.mobs.size());
            Item floor=Dungeon.level.heaps.get(drop).peek();
            assertEquals(owned,game.ownership.identity(floor));assertEquals(1,game.ownership.owner(floor));
            assertTrue(game.knowledge(game.hero(0)).visited(drop));
            assertEquals(1,Dungeon.depth);
            assertTrue(step(game,CoopSession.Action.WAIT,0,null));
            System.out.println("native sewer seed="+seed+" walk="+steps+" attacks="+attacks+" searches="+searches+" casts="+casts);
            Hero fallen=game.hero(1);
            fallen.damage(10000,new Object());
            assertTrue(game.life(1)!=CoopSession.Life.ALIVE);
            assertEquals(-1,game.stairs.stair());
        }
    }
}
