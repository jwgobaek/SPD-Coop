package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerResidentsTest extends HeadlessDungeon {
    @Test public void aNativeThiefTakesTheVictimsUnitAndRetainsItOffFloor() {
        for(int seat=0;seat<2;seat++) {
            Bundle dormant;
            int thiefId;
            long heldId;
            try(CoopSession g=CoopSession.open(412,1,HeroClass.WARRIOR)) {
                g.join(2,HeroClass.WARRIOR);
                for(int depth=2;depth<=4;depth++) SewerDepthTest.gate(g,true);
                Thief thief=SewerSpecialActorTest.actor(Thief.class); thiefId=thief.id();
                Food original=SewerCrystalMimicTest.pack(g,seat,3);
                Food partner=g.hero(1-seat).belongings.getItem(Food.class);
                thief.attackProc(g.hero(seat),1);
                assertEquals(2,original.quantity()); assertEquals(1,thief.item.quantity());
                assertEquals(thief.FLEEING,thief.state);
                assertEquals(1,partner.quantity());
                heldId=g.ownership.identity(thief.item);
                assertNotEquals(g.ownership.identity(original),heldId);
                assertEquals(g.player(seat),g.ownership.owner(thief.item));
                SewerDepthTest.gate(g,false);
                assertNull(Actor.findById(thiefId));
                dormant=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(dormant)) {
                SewerDepthTest.gate(g,true);
                Thief thief=(Thief)Actor.findById(thiefId);
                assertEquals(thief.FLEEING,thief.state);
                assertEquals(heldId,g.ownership.identity(thief.item));
                assertEquals(g.player(seat),g.ownership.owner(thief.item));
                assertFalse(g.ownership.allowed(thief.item,g.player(1-seat)));
                assertEquals(2,g.hero(seat).belongings.getItem(Food.class).quantity());
            }
        }
    }
    @Test public void hermitCrabArmorUsesTheCreditedSeatLevelAndSurvivesRestore() {
        for(int seat=0;seat<2;seat++) {
            Bundle after;
            int cell;
            try(CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.MAGE);
                SewerBeeTest.offer(game,seat);
                cell=SewerHazardsTest.approach(game,seat,true);
                HermitCrab crab=new HermitCrab(); crab.pos=cell;
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(crab);
                game.hero(1-seat).lvl=20; // the other seat must not suppress the killer's armor drop
                assertEquals(25,crab.HP);
                assertEquals(1f,crab.speed(),0f);
                crab.damage(100,game.hero(seat));
                assertFalse(crab.isAlive());
                assertTrue(Dungeon.level.heaps.get(cell).items.stream().anyMatch(
                        i->i instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor));
                after=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(after)) {
                assertTrue(Dungeon.level.heaps.get(cell).items.stream().anyMatch(
                        i->i instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor));
                SewerSpecialActorTest.waitOne(game);
            }
        }
    }

    @Test public void aSwarmSplitsItsRemainingHealthAndCausticOozeUsesTheActualVictim() {
        for(int seat=0;seat<2;seat++) try(CoopSession g=CoopSession.open(404,1,HeroClass.WARRIOR)) {
            g.join(2,HeroClass.WARRIOR);
            for(int depth=2;depth<=4;depth++) SewerDepthTest.gate(g,true);
            Swarm swarm=SewerSpecialActorTest.actor(Swarm.class);
            java.util.HashSet<Mob> existing=new java.util.HashSet<>(Dungeon.level.mobs);
            int hp=swarm.HP;
            swarm.defenseProc(g.hero(seat),2);
            java.util.HashSet<Mob> added=new java.util.HashSet<>(Dungeon.level.mobs); added.removeAll(existing);
            assertEquals(1,added.size());
            Mob clone=added.iterator().next();
            assertTrue(clone instanceof Swarm); assertEquals(hp,clone.HP+swarm.HP);
            assertEquals(0,clone.EXP); assertSame(clone,Actor.findById(clone.id()));
            assertNotEquals(swarm.id(),clone.id());
            CausticSlime slime=SewerSpecialActorTest.actor(CausticSlime.class);
            for(int i=0;i<30 && g.hero(seat).buff(Ooze.class)==null;i++) slime.attackProc(g.hero(seat),1);
            assertNotNull(g.hero(seat).buff(Ooze.class)); assertNull(g.hero(1-seat).buff(Ooze.class));
            g.validateActorGraph();
            assertEquals(3,g.checkpoint().getInt("schema"));
        }
    }
}
