package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerCrystalMimicTest extends HeadlessDungeon {
    // Restrict the random draw to one known ordinary stack in this theft fixture.
    public static Food pack(CoopSession g,int seat,int quantity) {
        Hero victim=g.hero(seat);
        for (Item item:new ArrayList<>(victim.belongings.backpack.items)) item.detachAll(victim.belongings.backpack);
        Food food=new Food();
        food.quantity(quantity);
        food.cursed=true;
        food.cursedKnown=true;
        assertTrue(food.collect(victim.belongings.backpack));
        return food;
    }

    @Test public void emptyHandedAmbushStillCostsTimeAndEscapeRetiresTheCarriedPrize() {
        for (int seat=0; seat<2; seat++) {
            Bundle saved;
            int id;
            long prize;
            try (CoopSession g=CoopSession.open(411,1,HeroClass.WARRIOR)) {
                g.join(2,HeroClass.HUNTRESS);
                SewerBeeTest.cross(g,Dungeon.level.exit());
                CrystalMimic m=SewerSpecialActorTest.actor(CrystalMimic.class);
                SewerBeeTest.offer(g,seat);
                Hero opener=g.hero(seat);
                for(Item item:new ArrayList<>(opener.belongings.backpack.items)) item.detachAll(opener.belongings.backpack);
                SewerGhostTest.meet(g,seat,m);
                id=m.id();
                prize=g.ownership.identity(m.items.get(0));
                int prizeCount=m.items.size();
                long turn=g.turn();
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.OPEN,m.pos,null));
                assertEquals(prizeCount,m.items.size());
                assertTrue(g.turn()>turn);
                assertTrue(m.isTargeting(opener));
                // Both observers leave the room; run the actual Fleeing AI until it escapes.
                int placed=0;
                for(int p=0;p<Dungeon.level.length() && placed<2;p++) {
                    if (Dungeon.level.distance(p,m.pos)<14 || !Dungeon.level.passable[p]
                            || Dungeon.level.pit[p] || Dungeon.level.traps.get(p)!=null || Actor.findChar(p)!=null) continue;
                    g.hero(placed++).pos=p;
                }
                assertEquals(2,placed);
                m.clearEnemy(); // lost target exercises the native Fleeing.escaped branch

                for (int n=0;n<80 && Actor.findById(id)!=null;n++) SewerSpecialActorTest.waitOne(g);
                assertNull(Actor.findById(id));
                for (Item i:CoopSession.floorItems(Dungeon.level).values()) assertNotEquals(prize,g.ownership.identity(i));
                saved=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                assertNull(Actor.findById(id));
                for (Item i:CoopSession.floorItems(Dungeon.level).values()) assertNotEquals(prize,g.ownership.identity(i));
            }
        }
    }

    @Test public void stolenUnitKeepsOwnerAndPhysicalIdentityAcrossFloorAndDeath() {
        for (int seat=0;seat<2;seat++) {
            int id, deathCell;
            long partId, bagId;
            Bundle saved;
            try (CoopSession g=CoopSession.open(411,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(g,Dungeon.level.exit());
                CrystalMimic m=SewerSpecialActorTest.actor(CrystalMimic.class);
                id=m.id();
                SewerBeeTest.offer(g,seat);
                SewerGhostTest.meet(g,seat,m);
                Food stack=pack(g,seat,3);
                bagId=g.ownership.identity(stack);
                long otherId=g.ownership.identity(g.hero(1-seat).belongings.getItem(Food.class));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.OPEN,m.pos,null));
                assertEquals(m.FLEEING,m.state);
                assertEquals(2,stack.quantity());
                assertEquals(otherId,g.ownership.identity(g.hero(1-seat).belongings.getItem(Food.class)));
                Item stolen=m.items.get(m.items.size()-1);
                assertEquals(Food.class,stolen.getClass());
                assertEquals(1,stolen.quantity());
                assertTrue(stolen.cursed && stolen.cursedKnown);
                assertEquals(g.player(seat),g.ownership.owner(stolen));
                partId=g.ownership.identity(stolen);
                assertNotEquals(bagId,partId);
                saved=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                CrystalMimic m=(CrystalMimic)Actor.findById(id);
                assertEquals(m.FLEEING,m.state);
                assertEquals(partId,g.ownership.identity(m.items.get(m.items.size()-1)));
                SewerBeeTest.cross(g,Dungeon.level.entrance());
                assertNull(Actor.findById(id));
                saved=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                SewerBeeTest.cross(g,Dungeon.level.exit());
                CrystalMimic m=(CrystalMimic)Actor.findById(id);
                Item stolen=m.items.get(m.items.size()-1);
                assertEquals(partId,g.ownership.identity(stolen));
                assertEquals(g.player(seat),g.ownership.owner(stolen));
                assertEquals(0,g.ownership.owner(m.items.get(0)));
                for (int n=0;n<16 && m.isAlive();n++) {
                    Hero caster=g.hero(g.activeSeat());
                    SewerGhostTest.meet(g,g.activeSeat(),m);
                    assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ZAP,m.pos,caster.belongings.weapon()));
                }
                assertFalse(m.isAlive());
                deathCell=m.pos;
                assertTrue(Dungeon.level.heaps.get(deathCell).items.contains(stolen));
                saved=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                assertNull(Actor.findById(id));
                Heap heap=Dungeon.level.heaps.get(deathCell);
                Item stolen=heap.peek();
                assertEquals(partId,g.ownership.identity(stolen));
                assertTrue(stolen.cursed && stolen.cursedKnown);
                SewerBeeTest.offer(g,1-seat);
                int original=g.hero(1-seat).pos;
                g.hero(1-seat).pos=deathCell;
                long turn=g.turn();
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,deathCell,stolen));
                assertEquals(turn,g.turn());
                g.hero(1-seat).pos=original;
                SewerBeeTest.offer(g,seat);
                g.hero(seat).pos=deathCell;
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,deathCell,stolen));
                Food rest=g.hero(seat).belongings.getItem(Food.class);
                assertEquals(3,rest.quantity());
                assertEquals(bagId,g.ownership.identity(rest));
                assertFalse(CoopSession.floorItems(Dungeon.level).containsValue(stolen));
                saved=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                Food rest=g.hero(seat).belongings.getItem(Food.class);
                assertEquals(3,rest.quantity());
                assertEquals(g.player(seat),g.ownership.owner(rest));
                assertEquals(bagId,g.ownership.identity(rest));
                assertNull(Actor.findById(id));
            }
        }
    }
}
