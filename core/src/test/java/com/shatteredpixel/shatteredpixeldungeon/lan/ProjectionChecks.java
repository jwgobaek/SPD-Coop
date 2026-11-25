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
    public static void checkShortcuts(CoopSession game) {
        UUID id=UUID.randomUUID();
        SeatProjection host=new SeatProjection(id,0), guest=new SeatProjection(id,1);
        Hero hero=game.hero(0);
        Food ration=(Food)hero.belongings.getItem(Food.class);
        host.capture(game,1,"IN_GAME",null,null);
        PublicDungeon view=new PublicDungeon(host.current);
        SeatQuickslots layout=SeatQuickslots.defaults(view);
        PublicDungeon.Loot food=layout.item(2,view);
        assertNotNull(food);
        layout=layout.bind(1,food);
        assertFalse(layout.assigned(2));
        assertEquals(food.reference,layout.item(1,view).reference);
        ration.quantity(2);
        com.shatteredpixel.shatteredpixeldungeon.items.Item part=ration.detach(hero.belongings.backpack);
        game.ownership.split(ration,part);
        assertNotEquals(game.ownership.identity(ration),game.ownership.identity(part));
        assertTrue(part.collect(hero.belongings.backpack));
        assertEquals(2,ration.quantity());
        ration.detachAll(hero.belongings.backpack);
        Dungeon.level.drop(ration,hero.pos);
        host.capture(game,2,"IN_GAME",null,null);
        view=new PublicDungeon(host.current);
        assertNull(layout.item(1,view));
        assertTrue(view.floor.get(hero.pos).reference.isEmpty());
        assertSame(layout,layout.reconnectItems(view));
        assertFalse(game.ownership.allowed(ration,game.player(1)));
        assertSame(ration,Dungeon.level.heaps.get(hero.pos).pickUp());
        assertTrue(ration.collect(hero.belongings.backpack));
        host.capture(game,3,"IN_GAME",null,null);
        view=new PublicDungeon(host.current);
        assertEquals(food.reference,layout.item(1,view).reference);
        ration.detachAll(hero.belongings.backpack);
        Food replacement=new Food();
        game.ownership.claim(replacement,game.player(0)); assertTrue(replacement.collect(hero.belongings.backpack));
        host.capture(game,4,"IN_GAME",null,null);
        view=new PublicDungeon(host.current);
        assertNull(layout.item(1,view));
        layout=layout.reconnectItems(view);
        assertNotEquals(food.reference,layout.item(1,view).reference);
        guest.capture(game,4,"IN_GAME",null,null);
        assertFalse(guest.current.rows.containsValue(layout.item(1,view).reference));
    }
    public static void checkNativeRenderInputs(CoopSession game) {
        UUID session=UUID.randomUUID();
        for (int seat=0; seat<2; seat++) {
            SeatProjection p=new SeatProjection(session,seat);
            p.capture(game,1,"IN_GAME",null,null);
            PublicDungeon image=new PublicDungeon(p.current);
            long[] rng=com.watabou.utils.Random.worldState();
            int[] terrain=Dungeon.level.map.clone();
            java.util.Set<?> actors=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all();
            long turn=game.turn();
            float time=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now();
            com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonRenderMap map=
                    new com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonRenderMap(image);
            for (String key:p.current.rows.keySet()) if(key.startsWith("label")) {
                String item=p.current.rows.get("i"+key.substring(5));
                assertNotNull(item);
                assertFalse("Only a seat's own bag exposes description keys",item.startsWith("F"));
            }
            int hidden=0, shown=0;
            for (int i=0;i<terrain.length;i++) {
                assertEquals(image.terrain(i),map.terrain(i));
                if (map.terrain(i)<0) hidden++; else shown++;
            }
            assertTrue(hidden>0 && shown>0);
            assertFalse(map.apply(image));
            assertArrayEquals(rng,com.watabou.utils.Random.worldState());
            assertArrayEquals(terrain,Dungeon.level.map);
            assertEquals(actors,com.shatteredpixel.shatteredpixeldungeon.actors.Actor.all());
            assertEquals(turn,game.turn());
            assertEquals(time,com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now(),0);
            assertEquals(image.pos,game.hero(seat).pos);
        }
    }
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
        a.capture(game,1,"IN_GAME",null,null);
        b.capture(game,1,"IN_GAME",null,null);
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
        a.capture(game,2,"IN_GAME",null,null);
        display.apply(a.current.encode(a.previous,null));
        assertTrue(display.image().rows.containsKey("b"+buff.id()));
        assertEquals(""+near.HP,display.image().rows.get(mobKey).split(",")[2]);
        near.pos=guest.pos-w;
        a.capture(game,3,"IN_GAME",null,null);
        String delta=a.current.encode(a.previous,a.witnessedDeath);
        display.apply(delta);
        assertTrue(display.lostMonsters().contains(mobKey));
        assertTrue(display.witnessedDeaths().isEmpty());
        assertFalse(display.image().rows.containsKey(mobKey));
        near.HP=0;
        level.mobs.remove(near);
        hidden.pos++;
        buff.detach();
        a.capture(game,4,"IN_GAME",near,null);
        assertNull(a.witnessedDeath);
        assertFalse(a.current.encode(a.previous,null).contains(mobKey));
        String snapshot=a.current.encode(null,null);
        assertFalse(snapshot.contains("+"+hiddenKey+"="));
        display.apply(snapshot);
        assertFalse(display.image().rows.containsKey("b"+buff.id()));
        assertEquals(a.current.rows,display.image().rows);

        // Cross the divider, then edit the vacated chamber while it is out of sight.
        int home = host.pos, observed = home - 1;
        int lastTile = level.map[observed];
        host.pos = guest.pos;
        a.capture(game,5,"IN_GAME",null,null);
        display.apply(a.current.encode(a.previous,null));
        assertFalse(display.image().rows.containsKey("t"+observed));
        assertEquals(lastTile, display.image().rememberedTerrain(observed));
        Level.set(observed, Terrain.GRASS);
        a.capture(game,6,"IN_GAME",null,null);
        assertEquals(lastTile, a.current.rememberedTerrain(observed));
        DisplayReplica recovered = new DisplayReplica(session,0);
        recovered.apply(a.current.encode(null,null));
        assertEquals(lastTile, recovered.image().rememberedTerrain(observed));
        host.pos = home;
        a.capture(game,7,"IN_GAME",null,null);
        recovered.apply(a.current.encode(a.previous,null));
        assertEquals(-1, recovered.image().rememberedTerrain(observed));
        assertEquals(Integer.toString(Terrain.GRASS), recovered.image().rows.get("t"+observed));
        assertNotSame(game.observe(0), game.observe(1));
        assertFalse(game.observe(1).visible(observed));
        boolean[] singleMap = level.mapped.clone();
        assertFalse(game.observe(0).mapped(observed));
        Buff.prolong(guest,Foresight.class,5);
        assertTrue(game.observe(1).mapped(observed));
        assertFalse(game.observe(0).mapped(observed));
        assertArrayEquals(singleMap,level.mapped);
        b.capture(game,8,"IN_GAME",null,null);
        DisplayReplica mappedView = new DisplayReplica(session,1);
        mappedView.apply(b.current.encode(null,null));
        assertFalse(mappedView.image().rows.containsKey("t"+observed));
        assertEquals(Terrain.GRASS,mappedView.image().rememberedTerrain(observed));
        assertEquals(!game.observe(1).visited(observed),mappedView.image().mappedOnly(observed));
        assertFalse(mappedView.image().rows.containsKey("ally"));
        level.drop(new Food(),guest.pos).hidden = true;
        b.capture(game,9,"IN_GAME",null,null);
        for (String row : b.current.rows.values()) assertFalse(row.startsWith("F"+guest.pos+","));
        assertFalse(b.current.rows.containsKey("h"+guest.pos));
    }
}
