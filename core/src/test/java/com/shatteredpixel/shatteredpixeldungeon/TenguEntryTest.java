package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real PrisonBossLevel; staged actors and a native phase transition, outside operational stair travel. */
public class TenguEntryTest extends HeadlessDungeon {
    @Test public void eitherEntrantSealsTheNativeCellAndTheSameBossIsParkedOnPause() {
        for (int seat=0;seat<2;seat++) try (CoopSession game=CoopSession.open(570,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.MAGE);
            Dungeon.depth=10;
            PrisonBossLevel floor=new PrisonBossLevel();
            floor.create();
            Dungeon.level=floor;
            Actor.clear();
            for (int i=0;i<2;i++) {
                Hero h=game.hero(i);
                h.pos=floor.entrance()+i;
                h.fieldOfView=new boolean[floor.length()];
                Actor.add(h);
            }
            Bundle first=new Bundle();
            floor.storeInBundle(first);
            int id=first.getBundle("tengu").getInt("id");
            assertTrue(id>0);
            assertNull(Actor.findById(id));
            assertEquals(PrisonBossLevel.State.START,floor.state());
            Hero entrant=game.hero(seat);
            entrant.pos=24*floor.width()+10;
            foodOutsideArena(floor,game,entrant);
            floor.occupyCell(entrant);
            assertEquals(PrisonBossLevel.State.FIGHT_START,floor.state());
            assertTrue(floor.locked);
            Tengu boss=(Tengu)Actor.findById(id);
            assertTrue(floor.mobs.contains(boss));
            for(int i=0;i<2;i++){
                assertNotNull(game.hero(i).buff(LockedFloor.class));
                assertTrue(floor.passable[game.hero(i).pos]);
                assertTrue(floor.cellToPoint(game.hero(i).pos).y>=24);
            }
            assertNotEquals(game.hero(0).pos,game.hero(1).pos);
            floor.progress();
            assertEquals(PrisonBossLevel.State.FIGHT_PAUSE,floor.state());
            assertNull(Actor.findById(id));
            assertFalse(floor.mobs.contains(boss));
            assertTrue(floor.locked);
            assertTrue(CoopSession.floorItems(floor).containsKey("encounter0"));
            Bundle pause=new Bundle();
            floor.storeInBundle(pause);
            PrisonBossLevel copy=new PrisonBossLevel();
            copy.restoreFromBundle(pause);
            assertEquals(PrisonBossLevel.State.FIGHT_PAUSE,copy.state());
            assertTrue(copy.locked);
            assertTrue(copy.storedEncounterItems().stream().anyMatch(i->i instanceof Food));
            Bundle again=new Bundle(); copy.storeInBundle(again);
            assertEquals(id,again.getBundle("tengu").getInt("id"));
            assertEquals(pause.getBundle("tengu").getInt("HP"),again.getBundle("tengu").getInt("HP"));
            Dungeon.level=copy;
            Hero next=game.hero(1-seat);
            next.pos=8*copy.width()+10;
            copy.occupyCell(next);
            assertEquals(PrisonBossLevel.State.FIGHT_ARENA,copy.state());
            assertTrue(copy.locked);
            assertTrue(copy.mobs.contains((Tengu)Actor.findById(id)));
            assertTrue(copy.passable[game.hero(seat).pos]);
            assertNotEquals(game.hero(0).pos,game.hero(1).pos);
            assertTrue(copy.distance(game.hero(0).pos,game.hero(1).pos)<=1);
        }
    }

    private static void foodOutsideArena(PrisonBossLevel floor, CoopSession game, Hero owner) {
        Food food=new Food();
        game.ownership.claim(food,game.playerOf(owner));
        floor.drop(food,floor.entrance());
    }
}
