package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.*;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerTravelGearTest extends HeadlessDungeon {
    @Test public void healingCuresTheDrinkerAndRestoresRemainingTicks() {
        for (int seat=0;seat<2;seat++) {
            Bundle boundary;
            try(CoopSession game=CoopSession.open(415,1,HeroClass.WARRIOR)) {
                game.join(2,HeroClass.WARRIOR);
                SewerBeeTest.offer(game,seat);
                Hero h=game.hero(seat);
                h.HP=2;
                int otherHP=game.hero(1-seat).HP;
                PotionOfHealing potion=new PotionOfHealing();
                potion.quantity(2);
                Dungeon.level.drop(potion,h.pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,h.pos,potion));
                SewerBeeTest.offer(game,1-seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.DRINK,0,potion));
                SewerBeeTest.offer(game,seat);
                Buff.prolong(h,Cripple.class,5);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.DRINK,0,potion));
                assertNull(h.buff(Cripple.class));
                assertNotNull(h.buff(Healing.class));
                assertTrue(h.HP<h.HT);
                assertEquals(otherHP,game.hero(1-seat).HP);
                assertEquals(1,potion.quantity());
                boundary=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(boundary)) {
                Hero h=game.hero(seat);
                assertNotNull(h.buff(Healing.class));
                for(int i=0;i<30 && h.HP<h.HT;i++) SewerSpecialActorTest.waitOne(game);
                assertEquals(h.HT,h.HP);
                assertEquals(1,h.belongings.getItem(PotionOfHealing.class).quantity());
            }
        }
    }

    @Test public void bowUsesActualSeatLevelTrajectoryAndClockOnBothSidesOfRestore() {
        for(int seat=0;seat<2;seat++) {
            Bundle b;
            int id;
            try(CoopSession g=CoopSession.open(415,1,HeroClass.HUNTRESS)) {
                g.join(2,HeroClass.HUNTRESS);
                SewerBeeTest.offer(g,seat);
                Rat mark=CoopMageTest.encounter(g,seat);
                id=mark.id();
                Hero h=g.hero(seat);
                SpiritBow bow=h.belongings.getItem(SpiritBow.class);
                h.lvl=10; g.hero(1-seat).lvl=1;
                assertEquals(2,bow.level()); assertEquals(0,g.hero(1-seat).belongings.getItem(SpiritBow.class).level());
                Level.set(h.pos+1,Terrain.WALL);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.SHOOT,mark.pos,bow));
                Level.set(h.pos+1,Terrain.EMPTY);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.SHOOT,mark.pos,g.hero(1-seat).belongings.getItem(SpiritBow.class)));
                float before=Actor.now()+h.cooldown(), cost=bow.knockArrow().castDelay(h,mark.pos);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.SHOOT,mark.pos,bow));
                assertEquals(before+cost,Actor.now()+h.cooldown(),0.001f);
                b=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(b)) {
                Rat mark=(Rat)Actor.findById(id); int hp=mark.HP;
                Hero h=g.hero(seat);
                for(int i=0;i<16 && hp==mark.HP;i++) {
                    SewerBeeTest.offer(g,seat);
                    assertTrue(CoopFloorsTest.step(g,CoopSession.Action.SHOOT,mark.pos,h.belongings.getItem(SpiritBow.class)));
                }
                assertTrue(mark.HP<hp);
            }
        }
    }

    @Test public void equipAndRemoveRealArmorWithAnOwnedWarriorSeal() {
        for(int seat=0;seat<2;seat++) {
            Bundle boundary;
            try(CoopSession g=CoopSession.open(415,1,HeroClass.WARRIOR)) {
                g.join(2,HeroClass.WARRIOR); SewerBeeTest.offer(g,seat);
                Hero user=g.hero(seat);
                Armor old=user.belongings.armor;
                Armor replacement=new LeatherArmor();
                Dungeon.level.drop(replacement,user.pos);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,user.pos,replacement));
                SewerBeeTest.offer(g,seat);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.EQUIP,0,replacement));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.UNEQUIP,0,old));
                assertNotNull(old.checkSeal());
                SewerBeeTest.offer(g,seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.EQUIP,0,replacement));
                assertSame(replacement,user.belongings.armor);
                assertEquals(g.player(seat),g.ownership.owner(replacement));
                assertNotSame(old,g.hero(1-seat).belongings.armor);
                boundary=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(boundary)) {
                assertTrue(g.hero(seat).belongings.armor instanceof LeatherArmor);
                assertNotNull(g.hero(seat).belongings.getItem(ClothArmor.class).checkSeal());
            }
        }
    }

    @Test public void sewerPlantsGrantTheirNativeBuffToTheOccupant() {
        for(int seat=0;seat<2;seat++) for(boolean star:new boolean[]{false,true}) {
            Bundle image;
            Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> effect=star ?
                    com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless.class :
                    com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Armor.class;
            try(CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.MAGE);
                SewerBeeTest.offer(game,seat);
                int cell=SewerHazardsTest.approach(game,seat,true);
                Dungeon.level.plant(star ? new com.shatteredpixel.shatteredpixeldungeon.plants.Starflower.Seed() :
                        new com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot.Seed(),cell);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,cell,null));
                assertNotNull(game.hero(seat).buff(effect));
                assertNull(game.hero(1-seat).buff(effect));
                image=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(image)) {
                assertNotNull(game.hero(seat).buff(effect));
                SewerSpecialActorTest.waitOne(game);
            }
        }
    }

    @Test public void fadeleafTeleportsOnlyTheOccupantAndStaysWitheredAfterRestore() {
        for(int seat=0;seat<2;seat++) {
            Bundle image;
            int cell, destination;
            try(CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.MAGE);
                SewerBeeTest.offer(game,seat);
                cell=SewerHazardsTest.approach(game,seat,true);
                int other=game.hero(1-seat).pos;
                Dungeon.level.plant(new com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf.Seed(),cell);
                assertNotNull(Dungeon.level.plants.get(cell));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,cell,null));
                destination=game.hero(seat).pos;
                assertNotEquals(cell,destination);
                assertEquals(other,game.hero(1-seat).pos);
                assertNull(Dungeon.level.plants.get(cell));
                image=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(image)) {
                assertNull(Dungeon.level.plants.get(cell));
                assertEquals(destination,game.hero(seat).pos);
                SewerSpecialActorTest.waitOne(game);
            }
        }
    }

    @Test public void nativeRockfallGrippingAndPersistentGateway() {
        for(int seat=0;seat<2;seat++) for(Trap trap:new Trap[]{new GatewayTrap(),new GrippingTrap(),new RockfallTrap()}) {
            Bundle save;
            int cell;
            try(CoopSession g=CoopSession.open(402,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.offer(g,seat);
                cell=SewerHazardsTest.approach(g,seat,true);
                Hero hero=g.hero(seat); hero.HP=hero.HT=100;
                Dungeon.level.setTrap(trap,cell); trap.reveal(); Level.set(cell,Terrain.TRAP);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
                if(trap instanceof GatewayTrap) {
                    Bundle raw=new Bundle();trap.storeInBundle(raw);assertTrue(raw.getInt("tele_pos")>=0);
                    assertNotEquals(cell,hero.pos);
                } else if(trap instanceof GrippingTrap) assertNotNull(hero.buff(Cripple.class));
                else assertTrue(hero.HP<100);
                save=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(save)) {
                Trap restored=Dungeon.level.traps.get(cell);
                assertEquals(trap.getClass(),restored.getClass());assertEquals(trap.active,restored.active);
                if(restored instanceof GatewayTrap) {
                    Bundle raw=new Bundle();restored.storeInBundle(raw);assertTrue(raw.getInt("tele_pos")>=0);
                }
                SewerSpecialActorTest.waitOne(g);
            }
        }
    }
}
