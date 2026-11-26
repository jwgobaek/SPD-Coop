package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Staged sewer shots with native hit rolls, followed by a real heap pickup after engine restoration. */
public class DartFlightChecks extends HeadlessDungeon {
    private static void check(int seat,boolean occupied) throws Exception {
        Bundle saved;
        long landedId;
        int cell,ratId;
        try(CoopSession g=CoopSession.open(415,11,HeroClass.WARRIOR)) {
            g.join(22,HeroClass.WARRIOR);
            Rat target=CoopMageTest.encounter(g,seat);
            target.HT=target.HP=500;
            ratId=target.id();
            Hero h=g.hero(seat);
            cell=occupied?target.pos:h.pos+2;
            if (occupied) {
                // A delayed, unaware rat otherwise grants a guaranteed surprise hit on every throw.
                target.aggro(h);
                java.lang.reflect.Field aware=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob.class.getDeclaredField("enemySeen");
                aware.setAccessible(true);
                aware.setBoolean(target,true);
                target.fieldOfView=new boolean[Dungeon.level.length()];
                Dungeon.level.updateFieldOfView(target,target.fieldOfView);
                assertTrue(target.defenseSkill(h)>0);
            }
            Dart stack=new Dart();
            stack.quantity(35);
            assertTrue(stack.collect(h.belongings.backpack));
            g.ownership.claim(stack,g.player(seat));
            boolean landed=false;
            for(int i=0;i<34 && !landed;i++) {
                SewerBeeTest.offer(g,seat);
                long before=g.turn();
                int health=target.HP,quantity=stack.quantity();
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.THROW,cell,stack));
                assertTrue(g.turn()>before);
                assertEquals(quantity-1,stack.quantity());
                Heap heap=Dungeon.level.heaps.get(cell);
                landed=heap!=null && heap.peek() instanceof Dart;
                if (landed) assertEquals(health,target.HP);
            }
            assertTrue("native floor landing",landed);
            Dart drop=(Dart)Dungeon.level.heaps.get(cell).peek();
            landedId=g.ownership.identity(drop);
            assertEquals(1,drop.quantity());
            assertEquals(Dart.MAX_DURABILITY,drop.durabilityLeft(),0f);
            assertEquals(g.player(seat),g.ownership.owner(drop));
            saved=g.checkpoint();
        }
        try(CoopSession g=CoopSession.restore(saved)) {
            if (occupied) {
                // Move the living fixture target off the heap, preserving any independently pinned darts.
                Rat rat=(Rat)Actor.findById(ratId);
                rat.pos+=Dungeon.level.width();
            }
            Dart drop=(Dart)Dungeon.level.heaps.get(cell).peek();
            assertEquals(landedId,g.ownership.identity(drop));
            SewerBeeTest.offer(g,1-seat);
            Hero other=g.hero(1-seat);
            int origin=other.pos;
            other.pos=cell; g.observe(1-seat);
            assertFalse(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,cell,drop));
            other.pos=origin;
            SewerBeeTest.offer(g,seat);
            Hero h=g.hero(seat);
            h.pos=cell;g.observe(seat);
            Dart stack=h.belongings.getItem(Dart.class);
            int prior=stack.quantity();
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,cell,drop));
            assertEquals(prior+1,stack.quantity());
            g.checkpoint();
            System.out.printf("PASS seat=%d target=%s originalDurability=100 recovered=1%n",seat,occupied?"native-miss":"empty");
        }
    }

    public static void main(String[] args) throws Exception {
        DartFlightChecks test=new DartFlightChecks();
        test.temporary.create();test.startBackend();
        try { for(int s=0;s<2;s++) { check(s,false); check(s,true); } }
        finally {test.stopBackend();test.temporary.delete();}
    }
}
