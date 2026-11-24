package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class CheckpointTest extends HeadlessDungeon {
    @Test public void casualtiesAndCompleteSchedulerRoundTrip() {
        Bundle checkpoint;
        long turn, ownerId, expectedRoll;
        int actor, gear, rat, pos, tile;
        float clock;
        try(CoopSession game=CoopSession.open(402,71,HeroClass.WARRIOR)) {
            game.join(94,HeroClass.HUNTRESS);
            Hero guest=game.hero(1);
            assertTrue(new Ankh().collect(guest.belongings.backpack));
            guest.HP=17;
            guest.damage(10000,this);
            Mob mob=Dungeon.level.mobs.iterator().next();
            rat=mob.id(); actor=guest.id(); pos=guest.pos;
            Food food=game.hero(0).belongings.getItem(Food.class);
            assertTrue(game.act(71,game.turn(),CoopSession.Action.DROP,game.hero(0).pos,food));
            Item dropped=Dungeon.level.heaps.get(game.hero(0).pos).peek();
            ownerId=game.ownership.identity(dropped);
            gear=guest.belongings.weapon.level();
            tile=game.knowledge(guest).terrain(pos);
            mob.aggro(guest);
            assertTrue(mob.isTargeting(guest));
            clock=Actor.now(); turn=game.turn();
            checkpoint=game.checkpoint();
            expectedRoll=Random.Long();
        }
        try(CoopSession restored=CoopSession.restore(checkpoint)) {
            Hero guest=restored.hero(1);
            assertEquals(expectedRoll,Random.Long());
            assertEquals(clock,Actor.now(),0); assertEquals(turn,restored.turn());
            assertEquals(actor,guest.id()); assertEquals(pos,guest.pos);
            assertTrue(((Mob)Actor.findById(rat)).isTargeting(guest));
            assertEquals(gear,guest.belongings.weapon.level()); assertEquals(tile,restored.knowledge(guest).terrain(pos));
            Item drop=Dungeon.level.heaps.get(restored.hero(0).pos).peek();
            assertEquals(ownerId,restored.ownership.identity(drop)); assertEquals(71,restored.ownership.owner(drop));
            assertEquals(CoopSession.Life.DOWNED,restored.life(1)); assertTrue(guest.frozenInCoop());
            assertTrue(restored.act(71,turn,CoopSession.Action.REVIVE,pos,null));
            assertEquals(5,guest.HP); assertNull(guest.belongings.getItem(Ankh.class));
            checkpoint=restored.checkpoint();
        }
        try(CoopSession game=CoopSession.restore(checkpoint)) {
            assertFalse(game.hero(1).frozenInCoop());
            assertNull(game.hero(1).belongings.getItem(Ankh.class));
            assertTrue(game.act(game.player(game.activeSeat()),game.turn(),CoopSession.Action.WAIT,0,null));
            for(int seat=0;seat<2;seat++) game.hero(seat).damage(10000,this);
            checkpoint=game.checkpoint();
        }
        try(CoopSession game=CoopSession.restore(checkpoint)) {
            assertTrue(game.defeated()); assertEquals(-1,game.activeSeat());
        }
    }
}
