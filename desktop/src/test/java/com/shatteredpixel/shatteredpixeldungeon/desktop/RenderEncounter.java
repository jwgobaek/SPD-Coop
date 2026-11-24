package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.lan.CoopController;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.*;

/** Test arrangement only. Run on the existing room's owner; gestures and snapshots still use TCP. */
final class RenderEncounter {
    static void arrange(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        ((ExecutorService)executor.get(room)).submit(RenderEncounter::setup).get(5,TimeUnit.SECONDS);
    }
    // Rendering-only fixture. The independent-JVM route test walks to these same native stairs.
    static void atExit(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field f=room.getClass().getDeclaredField("owner"); f.setAccessible(true);
        ((ExecutorService)f.get(room)).submit(()->{
            CoopSession game=Dungeon.coop;
            Level l=Dungeon.level;
            int gate=l.exit(),i=0;
            for(int off:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                int pos=gate+off;
                if(!l.passable[pos] || l.pit[pos] || l.findMob(pos)!=null || l.traps.get(pos)!=null || l.plants.get(pos)!=null) continue;
                game.hero(i++).pos=pos;
                if(i==2) break;
            }
            if(i!=2) throw new AssertionError("Missing stair approach");
        }).get(5,TimeUnit.SECONDS);
    }

    static void atGhost(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field field=room.getClass().getDeclaredField("owner"); field.setAccessible(true);
        ((ExecutorService)field.get(room)).submit(()->{
            Level level=Dungeon.level;
            for (Mob mob:level.mobs) if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost) {
                mob.state=mob.PASSIVE;
                for (int offset:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int p=mob.pos+offset;
                    if (level.passable[p] && Actor.findChar(p)==null && !level.pit[p] && level.traps.get(p)==null) {
                        Dungeon.coop.hero(0).pos=p;
                        return;
                    }
                }
            }
            throw new AssertionError("No reachable ghost");
        }).get(5,TimeUnit.SECONDS);
    }

    static void atSentry(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field field=room.getClass().getDeclaredField("owner"); field.setAccessible(true);
        ((ExecutorService)field.get(room)).submit(()->{
            CoopSession game=Dungeon.coop;
            Level level=Dungeon.level;
            for (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room r: ((RegularLevel)level).rooms()) {
                if (!(r instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom)) continue;
                for (int p=0;p<level.length();p++) if (r.inside(level.cellToPoint(p)) &&
                        level.map[p]==Terrain.EMPTY_SP && Actor.findChar(p)==null) {
                    game.hero(0).pos=p;
                    return;
                }
            }
            throw new AssertionError("Expected the native sentry room");
        }).get(5,TimeUnit.SECONDS);
    }

    private static void setup() {
        CoopSession game=Dungeon.coop; game.checkExecutor(); Level level=Dungeon.level;
        if(game.activeSeat()!=0) throw new AssertionError("Host turn required");
        for(Mob mob:new ArrayList<>(level.mobs)) {Actor.remove(mob);level.mobs.remove(mob);}
        int center=level.length()/2/level.width()*level.width()+level.width()/2;
        for(int y=-3;y<=3;y++) for(int x=-4;x<=4;x++) {
            int cell=center+x+y*level.width();
            Level.set(cell,Math.abs(y)==3 || Math.abs(x)==4?Terrain.WALL:Terrain.EMPTY);
            level.heaps.remove(cell); level.traps.remove(cell); level.plants.remove(cell);level.discoverable[cell]=true;
        }
        Hero host=game.hero(0), guest=game.hero(1);
        host.pos=center;guest.pos=center+1;
        Ankh ankh=new Ankh(); guest.belongings.backpack.items.add(ankh);game.ownership.claim(ankh,game.player(1));
        Rat rat=new Rat(); rat.pos=center-1; rat.HP=rat.HT=100;GameScene.add(rat,990);rat.aggro(host);
        guest.HP=17;
        rat.attack(guest,1,10000,Float.POSITIVE_INFINITY);
        if(game.life(1)!=CoopSession.Life.DOWNED) throw new AssertionError("Fixture damage did not down guest");
    }
}

