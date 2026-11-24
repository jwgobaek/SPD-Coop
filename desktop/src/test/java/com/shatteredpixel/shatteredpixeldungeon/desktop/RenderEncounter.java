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

