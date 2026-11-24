package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot.ShatteredPot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerBeeTest extends HeadlessDungeon {
    static void offer(CoopSession g,int seat) {
        for(int n=0;n<12 && g.activeSeat()!=seat;n++) SewerSpecialActorTest.waitOne(g);
        assertEquals(seat,g.activeSeat());
    }
    static void cross(CoopSession g,int stair) {
        SewerSpecialActorTest.atGate(g,stair);
        CoopFloorsTest.cross(g);
    }

    @Test public void generatedHiveAndShatteredPotRetainDistinctBeeAndOwnerAcrossFloors() {
        for(int seat=0;seat<2;seat++) {
            Bundle saved;
            int guardian, summoned, drop;
            long itemId;
            try(CoopSession g=CoopSession.open(409,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.WARRIOR);
                cross(g,Dungeon.level.exit());
                Bee bee=SewerSpecialActorTest.actor(Bee.class);
                guardian=bee.id();
                ShatteredPot nativeShell=(ShatteredPot)Dungeon.level.heaps.get(bee.potPos()).peek();
                assertEquals(guardian,nativeShell.beeId());
                offer(g,seat);
                Hero h=g.hero(seat);
                Heap honey=null;
                for(Heap heap:Dungeon.level.heaps.valueList())
                    if(heap.type==Heap.Type.HEAP && heap.peek() instanceof Honeypot && Actor.findChar(heap.pos)==null) honey=heap;
                assertNotNull(honey);
                h.pos=honey.pos;
                Item pot=honey.peek();
                g.observe(seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,h.pos,pot));
                offer(g,seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.SHATTER,h.pos,pot));
                ShatteredPot shell=h.belongings.getItem(ShatteredPot.class);
                assertNotNull(shell);
                assertFalse(nativeShell.canStackWith(shell));
                assertEquals(g.player(seat),g.ownership.owner(shell));
                summoned=shell.beeId();
                assertNotEquals(guardian,summoned);
                assertEquals(h.id(),((Bee)Actor.findById(summoned)).potHolderID());
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.SHATTER,h.pos,pot));
                itemId=g.ownership.identity(shell);
                // Isolated seat encounter; retain original bee stats and attack time.
                int before=h.HP;
                for(int n=0;n<15 && h.HP==before;n++) SewerSpecialActorTest.waitOne(g);
                assertTrue(h.HP<before);
                cross(g,Dungeon.level.entrance());
                assertNull(Actor.findById(summoned));
                offer(g,seat);
                drop=g.hero(seat).pos;
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.DROP,drop,shell));
                saved=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(saved)) {
                ShatteredPot shell=(ShatteredPot)Dungeon.level.heaps.get(drop).peek();
                assertEquals(summoned,shell.beeId());
                assertEquals(itemId,g.ownership.identity(shell));
                assertEquals(g.player(seat),g.ownership.owner(shell));
                cross(g,Dungeon.level.exit());
                Bee away=(Bee)Actor.findById(summoned),resident=(Bee)Actor.findById(guardian);
                assertNotNull(away); assertNotNull(resident);
                assertEquals(-1,away.potHolderID());
                assertEquals(-1,away.potPos());
                assertTrue(resident.potPos()>=0);
                cross(g,Dungeon.level.entrance());
                assertEquals(itemId,g.ownership.identity(Dungeon.level.heaps.get(drop).peek()));
            }
        }
    }
}
