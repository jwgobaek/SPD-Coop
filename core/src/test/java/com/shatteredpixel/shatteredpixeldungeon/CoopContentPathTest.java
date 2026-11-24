package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.watabou.utils.Bundle;
import java.io.*;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoopContentPathTest extends HeadlessDungeon {
    private static void offer(CoopSession game,int seat) {
        for(int i=0;i<12 && game.activeSeat()!=seat;i++)
            assertTrue(game.act(game.player(game.activeSeat()),game.turn(),CoopSession.Action.WAIT,0,null));
        assertEquals(seat,game.activeSeat());
    }
    @Test public void affordableRationAndOrdinaryDoorOnEitherSeatAcrossDiskRestore() throws Exception {
        for(int seat=0;seat<2;seat++) {
            int door,exit;long ownerId;
            File disk=temporary.newFile();
            try(CoopSession game=CoopSession.open(402+seat,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.HUNTRESS);
                Level level=Dungeon.level;
                for(Mob mob:new ArrayList<>(level.mobs)) {Actor.remove(mob);level.mobs.remove(mob);}
                int center=level.length()/2/level.width()*level.width()+level.width()/2;
                level.transitions.clear(); door=center+1;exit=center+2;
                for(int y=-2;y<=2;y++) for(int x=-3;x<=3;x++) {
                    int cell=center+x+y*level.width(); Level.set(cell,cell==door?Terrain.DOOR:Terrain.EMPTY);
                    level.plants.remove(cell);level.heaps.remove(cell);level.traps.remove(cell);level.discoverable[cell]=true;
                }
                Hero user=game.hero(seat),other=game.hero(1-seat);user.pos=center;other.pos=center+level.width();
                SmallRation ration=new SmallRation(); ration.quantity(2);level.drop(ration,center);
                offer(game,seat);
                assertTrue(game.act(game.player(seat),game.turn(),CoopSession.Action.PICK_UP,center,ration));
                assertEquals(game.player(seat),game.ownership.owner(ration)); ownerId=game.ownership.identity(ration);
                offer(game,seat);user.buff(Hunger.class).affectHunger(-250);
                int before=user.buff(Hunger.class).hunger(), allyHunger=other.buff(Hunger.class).hunger();
                float due=Actor.now()+user.cooldown();
                assertTrue(game.act(game.player(seat),game.turn(),CoopSession.Action.EAT,0,ration));
                assertEquals(1,ration.quantity());
                assertEquals(Hunger.HUNGRY/2f,before-user.buff(Hunger.class).hunger(),3f);
                assertTrue(other.buff(Hunger.class).hunger()>=allyHunger);
                assertEquals(Food.TIME_TO_EAT,Actor.now()+user.cooldown()-due,0.001f);
                offer(game,seat);
                assertTrue(game.act(game.player(seat),game.turn(),CoopSession.Action.MOVE,door,null));
                assertEquals(Terrain.OPEN_DOOR,level.map[door]);assertEquals(door,user.pos);
                assertTrue(Bundle.write(game.checkpoint(),new FileOutputStream(disk),false));
            }
            try(InputStream in=new FileInputStream(disk);CoopSession game=CoopSession.restore(Bundle.read(in))) {
                Hero user=game.hero(seat);
                SmallRation ration=user.belongings.getItem(SmallRation.class);
                assertEquals(ownerId,game.ownership.identity(ration));assertEquals(1,ration.quantity());
                assertNull(game.hero(1-seat).belongings.getItem(SmallRation.class));
                assertEquals(door,user.pos);assertEquals(Terrain.OPEN_DOOR,Dungeon.level.map[door]);
                offer(game,seat);
                assertTrue(game.act(game.player(seat),game.turn(),CoopSession.Action.MOVE,exit,null));
                assertEquals(Terrain.DOOR,Dungeon.level.map[door]);assertEquals(exit,user.pos);
                assertEquals(Terrain.DOOR,game.observe(seat).terrain(door));
            }
        }
    }
}

