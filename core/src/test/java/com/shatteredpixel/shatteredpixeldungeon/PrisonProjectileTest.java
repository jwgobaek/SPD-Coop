package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrisonProjectileTest extends HeadlessDungeon {
    @Test public void bothOwnersKeepTheirSeparateNativeMissilesInsideOneDormantMonster() {
        Bundle parked;
        int ratId;
        long[] ids=new long[2];
        float[] durability=new float[2];
        try (CoopSession game=CoopSession.open(415,1,HeroClass.WARRIOR)) {
            game.join(2,HeroClass.WARRIOR);
            Rat rat=CoopMageTest.encounter(game,0);
            ratId=rat.id();
            for (int seat=0;seat<2;seat++) {
                ThrowingKnife knife=new ThrowingKnife();
                knife.setID=881; // Deliberately collide the native stack key across owners.
                int initial=knife.quantity(); knife.identify(false);
                assertTrue(knife.collect(game.hero(seat).belongings.backpack));
                for (int n=0;n<initial;n++) {
                    SewerBeeTest.offer(game,seat);
                    game.observe(seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,knife));
                    PinCushion cushion=rat.buff(PinCushion.class);
                    if (cushion!=null && cushion.getStuckItems().size()==seat+1) break;
                }
                assertNotNull(rat.buff(PinCushion.class));
                assertEquals(seat+1,rat.buff(PinCushion.class).getStuckItems().size());
                MissileWeapon stuck=rat.buff(PinCushion.class).getStuckItems().get(seat);
                ids[seat]=game.ownership.identity(stuck); durability[seat]=stuck.durabilityLeft();
                assertNotEquals(game.ownership.identity(knife),ids[seat]);
                assertEquals(game.player(seat),game.ownership.owner(stuck));
                assertFalse(game.hero(seat).belongings.contains(stuck));
                assertTrue(knife.quantity()<initial);
            }
            assertNotEquals(ids[0],ids[1]);
            SewerDepthTest.gate(game,true);
            assertEquals(2,Dungeon.depth); assertNull(Actor.findById(ratId));
            parked=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(parked)) {
            assertNull(Actor.findById(ratId));
            SewerDepthTest.gate(game,false);
            Rat rat=(Rat)Actor.findById(ratId);
            assertNotNull(rat);
            int cell=rat.pos;
            for (int seat=0;seat<2;seat++) {
                MissileWeapon stuck=rat.buff(PinCushion.class).getStuckItems().get(seat);
                assertEquals(ids[seat],game.ownership.identity(stuck));
                assertEquals(game.player(seat),game.ownership.owner(stuck));
                assertEquals(durability[seat],stuck.durabilityLeft(),0f);
            }
            rat.die(game.hero(0)); // Isolate original death/drop independently of its combat HP.
            Heap heap=Dungeon.level.heaps.get(cell);
            for (int seat=0;seat<2;seat++) {
                long id=ids[seat];
                Item drop=heap.items.stream().filter(i->game.ownership.identity(i)==id).findFirst().get();
                assertEquals(game.player(seat),game.ownership.owner(drop));
                assertFalse(drop.collect(game.hero(1-seat).belongings.backpack));
            }
            // The uppermost recovered knife decides whose native pickup can succeed first.
            for (int n=0;n<2;n++) {
                Item top=heap.peek();
                int seat=game.ownership.owner(top)==game.player(0) ? 0 : 1;
                SewerBeeTest.offer(game,seat);
                game.hero(seat).pos=cell; game.observe(seat);
                ThrowingKnife stack=game.hero(seat).belongings.getItem(ThrowingKnife.class);
                int original=stack.quantity();
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,top));
                assertEquals(original+1,stack.quantity());
                assertFalse(heap.items.contains(top));
                game.hero(seat).pos=cell+Dungeon.level.width(); game.observe(seat);
            }
            game.checkpoint();
        }
    }
}
