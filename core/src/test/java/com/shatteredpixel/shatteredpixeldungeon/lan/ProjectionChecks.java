package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import java.util.*;
import static org.junit.Assert.*;

public final class ProjectionChecks {
    public static void check(CoopSession game) throws Exception {
        Level level=Dungeon.level;
        int w=level.width(), center=level.length()/2/w*w+w/2;
        for (int dy=-4;dy<=4;dy++) for (int dx=-4;dx<=4;dx++) {
            int p=center+dy*w+dx;
            Level.set(p,dx==0 ? Terrain.WALL : Terrain.EMPTY);
            level.discoverable[p]=true;
        }
        Hero host=game.hero(0), guest=game.hero(1);
        host.pos=center-2;
        guest.pos=center+2;
        Level.set(host.pos-w,Terrain.SECRET_TRAP);
        Level.set(host.pos+w,Terrain.SECRET_DOOR);
        Rat near=new Rat(), hidden=new Rat();
        near.pos=host.pos-1;
        hidden.pos=guest.pos+1;
        GameScene.add(near);
        GameScene.add(hidden);
        guest.belongings.getItem(Food.class).quantity(7);
        level.drop(new Food(),hidden.pos);
        UUID session=UUID.randomUUID();
        SeatProjection a=new SeatProjection(session,0), b=new SeatProjection(session,1);
        a.capture(game,1,"IN_GAME",null);
        b.capture(game,1,"IN_GAME",null);
        String mobKey="m"+near.id(), hiddenKey="m"+hidden.id();
        assertTrue(a.current.rows.containsKey(mobKey));
        assertFalse(a.current.rows.containsKey(hiddenKey));
        assertTrue(b.current.rows.containsKey(hiddenKey));
        assertFalse(a.current.rows.containsKey("ally"));
        assertFalse(a.current.rows.containsKey("t"+guest.pos));
        assertEquals(""+Terrain.WALL,a.current.rows.get("t"+(host.pos+w)));
        assertEquals(""+Terrain.EMPTY,a.current.rows.get("t"+(host.pos-w)));
        for (String value : a.current.rows.values()) assertFalse(value.startsWith("F"+hidden.pos+","));
        for (Map.Entry<String,String> e : b.current.rows.entrySet()) {
            if (e.getKey().startsWith("i")) assertFalse(a.current.rows.containsKey(e.getKey()));
        }
        DisplayReplica display=new DisplayReplica(session,0);
        display.apply(a.current.encode(null,null));
        assertEquals(a.current.rows,display.image().rows);
        near.HP--;
        Buff buff=Buff.prolong(host,Haste.class,4);
        a.capture(game,2,"IN_GAME",null);
        display.apply(a.current.encode(a.previous,null));
        assertTrue(display.image().rows.containsKey("b"+buff.id()));
        assertEquals(""+near.HP,display.image().rows.get(mobKey).split(",")[2]);
        near.pos=guest.pos-w;
        a.capture(game,3,"IN_GAME",null);
        String delta=a.current.encode(a.previous,a.witnessedDeath);
        display.apply(delta);
        assertTrue(display.lostMonsters().contains(mobKey));
        assertTrue(display.witnessedDeaths().isEmpty());
        assertFalse(display.image().rows.containsKey(mobKey));
        near.HP=0;
        level.mobs.remove(near);
        hidden.pos++;
        buff.detach();
        a.capture(game,4,"IN_GAME",near);
        assertNull(a.witnessedDeath);
        assertFalse(a.current.encode(a.previous,null).contains(mobKey));
        String snapshot=a.current.encode(null,null);
        assertFalse(snapshot.contains("+"+hiddenKey+"="));
        display.apply(snapshot);
        assertFalse(display.image().rows.containsKey("b"+buff.id()));
        assertEquals(a.current.rows,display.image().rows);
    }
}
