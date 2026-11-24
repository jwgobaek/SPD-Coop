package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerGuardiansTest extends HeadlessDungeon {
    @Test public void statuesKeepTheirNativeWeaponsAcrossBothFloorsAndDropOneOnDefeat() {
        for(int seed:new int[]{416,420}) for(int seat=0;seat<2;seat++) {
            Bundle saved;
            int id;
            long weaponId;
            try (CoopSession game=CoopSession.open(seed,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(game,Dungeon.level.exit());
                Statue statue=SewerSpecialActorTest.actor(Statue.class);
                id=statue.id();
                Weapon weapon=statue.weapon();
                assertEquals(seed==416 ? Sickle.class : BattleAxe.class,weapon.getClass());
                assertEquals(seed==416 ? Kinetic.class : Vampiric.class,weapon.enchantment.getClass());
                weaponId=game.ownership.identity(weapon);
                assertSame(statue.PASSIVE,statue.state);
                SewerBeeTest.cross(game,Dungeon.level.entrance());
                assertNull(Actor.findById(id));
                saved=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(saved)) {
                com.watabou.utils.Random.pushGenerator(37L*seed+seat);
                SewerBeeTest.cross(game,Dungeon.level.exit());
                Statue statue=(Statue)Actor.findById(id);
                assertEquals(weaponId,game.ownership.identity(statue.weapon()));
                // Durability fixture for repeated native weapon procs, including a two-handed tier-four axe.
                for(int i=0;i<2;i++) game.hero(i).HP=game.hero(i).HT=180;
                SewerBeeTest.offer(game,seat);
                Hero caster=game.hero(seat);
                // Shoot from outside melee range: a full native Vampiric heal can erase a
                // point-blank missile's HP change before the next input boundary.
                Level floor=Dungeon.level;
                boolean firingCell=false;
                for(int cell=0;cell<floor.length();cell++) {
                    if(!floor.passable[cell] || floor.pit[cell] || floor.traps.get(cell)!=null
                            || Actor.findChar(cell)!=null || floor.distance(cell,statue.pos)<3
                            || floor.distance(cell,statue.pos)>caster.viewDistance) continue;
                    if(new Ballistica(cell,statue.pos,Ballistica.MAGIC_BOLT).collisionPos!=statue.pos) continue;
                    caster.pos=cell;
                    if(game.observe(seat).visible(statue.pos)) { firingCell=true; break; }
                }
                assertTrue(firingCell);
                int otherHp=game.hero(1-seat).HP;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,statue.pos,caster.belongings.weapon()));
                assertSame(statue.HUNTING,statue.state);
                assertTrue(statue.HP<statue.HT);
                assertEquals(statue.weapon().delayFactor(statue),statue.attackDelay(),0.0001f);
                // Inspect native on-hit effects independently from random accuracy.
                if (seed==416) {
                    statue.attackProc(caster,5);
                    assertNotNull(statue.buff(Kinetic.KineticTracker.class));
                    caster.damage(5,statue);
                    assertNull(statue.buff(Kinetic.KineticTracker.class));
                } else {
                    int was=statue.HP;
                    for(int n=0;n<150 && statue.HP==was;n++) statue.attackProc(caster,6);
                    assertTrue(statue.HP>was);
                }
                assertEquals(otherHp,game.hero(1-seat).HP);
                for(int n=0;n<80 && statue.isAlive();n++) {
                    int acting=game.activeSeat();
                    SewerGhostTest.meet(game,acting,statue);
                    Hero h=game.hero(acting);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,statue.pos,h.belongings.weapon())
                            || CoopFloorsTest.step(game,CoopSession.Action.ATTACK,statue.pos,null));
                }
                assertFalse(statue.isAlive());
                assertEquals(weaponId,game.ownership.identity(Dungeon.level.heaps.get(statue.pos).peek()));
                Hero owner=game.hero(game.activeSeat());
                owner.pos=statue.pos;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,statue.pos,statue.weapon()));
                assertEquals(game.playerOf(owner),game.ownership.owner(statue.weapon()));
                saved=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(saved)) {
                assertNull(Actor.findById(id));
                int found=0;
                for(int i=0;i<2;i++) for(Item item:game.hero(i).belongings)
                    if(game.ownership.identity(item)==weaponId) {
                        assertEquals(game.player(i),game.ownership.owner(item));
                        found++;
                    }
                assertEquals(1,found);
            }
        }
    }

    @Test public void naturalVenomousStatuePreservesPoisonAndWeaponOnBothSeats() {
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            int statueId,otherHP;
            long itemId;
            float due;
            try (CoopSession game=CoopSession.open(444,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(game,Dungeon.level.exit());
                Statue statue=SewerSpecialActorTest.actor(Statue.class);
                statueId=statue.id();
                itemId=game.ownership.identity(statue.weapon());
                assertEquals(Shortsword.class,statue.weapon().getClass());
                assertEquals(Venomous.class,statue.weapon().enchantment.getClass());
                Hero victim=game.hero(seat);
                otherHP=game.hero(1-seat).HP;
                // Invoke the real on-hit proc independently of the accuracy roll.
                for (int n=0;n<100 && victim.buff(Poison.class)==null;n++) statue.attackProc(victim,4);
                assertNotNull(victim.buff(Poison.class));
                assertNull(game.hero(1-seat).buff(Poison.class));
                due=victim.buff(Poison.class).cooldown();
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero victim=game.hero(seat);
                assertEquals(due,victim.buff(Poison.class).cooldown(),0.0001f);
                assertEquals(itemId,game.ownership.identity(((Statue)Actor.findById(statueId)).weapon()));
                int health=victim.HP;
                for(int i=0;i<20 && victim.HP==health;i++) SewerSpecialActorTest.waitOne(game);
                assertTrue(victim.HP<health);
                assertEquals(otherHP,game.hero(1-seat).HP);
            }
        }
    }

    @Test public void nativeChillingAndShockingProcsResolveAgainstTheCorrectVictims() {
        for(int seat=0;seat<2;seat++) try(CoopSession game=CoopSession.open(416,1,HeroClass.MAGE)) {
            game.join(2,HeroClass.MAGE);
            SewerBeeTest.cross(game,Dungeon.level.exit());
            Statue statue=SewerSpecialActorTest.actor(Statue.class);
            Hero victim=game.hero(seat), other=game.hero(1-seat);
            SewerGhostTest.meet(game,seat,statue);
            SewerGhostTest.meet(game,1-seat,statue);
            // Isolate two native Unstable delegates without admitting the other effects.
            Chilling chill=new Chilling();
            for(int n=0;n<100 && victim.buff(Chill.class)==null;n++) chill.proc(statue.weapon(),statue,victim,8);
            assertNotNull(victim.buff(Chill.class));
            assertNull(other.buff(Chill.class));
            Shocking lightning=new Shocking();
            int initial=victim.HP,otherHP=other.HP;
            for(int n=0;n<100 && other.HP==otherHP;n++) lightning.proc(statue.weapon(),statue,victim,8);
            assertEquals(otherHP-4,other.HP);
            assertEquals(initial,victim.HP);
            game.checkpoint();
        }
    }

    @Test public void exileOnTheGhostFloorUsesItsOwnAggressionAndDropsLoot() {
        for(int seat=0;seat<2;seat++) try(CoopSession game=CoopSession.open(420,1,HeroClass.MAGE)) {
            game.join(2,HeroClass.MAGE);
            SewerBeeTest.cross(game,Dungeon.level.exit());
            GnollExile exile=SewerSpecialActorTest.actor(GnollExile.class);
            assertSame(exile.PASSIVE,exile.state);
            assertEquals("OFFER",com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.Quest.sewerOffer());
            SewerBeeTest.offer(game,seat);
            com.watabou.utils.Random.pushGenerator(420+seat);
            long prior=looseUnits();
            for (int n=0;n<18 && exile.isAlive();n++) {
                int acting=game.activeSeat();
                Hero h=game.hero(acting);
                boolean positioned=false;
                for (int p=0;p<Dungeon.level.length();p++) {
                    if (!Dungeon.level.passable[p] || Dungeon.level.pit[p] || Dungeon.level.traps.get(p)!=null
                            || Dungeon.level.distance(p,exile.pos)>4 || (Actor.findChar(p)!=null && Actor.findChar(p)!=h)) continue;
                    if (new Ballistica(p,exile.pos,Ballistica.MAGIC_BOLT).collisionPos!=exile.pos) continue;
                    h.pos=p;
                    if (game.observe(acting).visible(exile.pos)) { positioned=true; break; }
                }
                assertTrue(positioned);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,exile.pos,h.belongings.weapon())
                        || CoopFloorsTest.step(game,CoopSession.Action.ATTACK,exile.pos,null));
            }
            assertFalse(exile.isAlive());
            assertTrue("Native exile loot is missing",looseUnits()>=prior+2);
            assertEquals("OFFER",com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.Quest.sewerOffer());
            game.checkpoint();
        }
    }

    // Native loot can land in a stack or fall through a nearby chasm.
    private static long looseUnits() {
        long units=0;
        for(com.shatteredpixel.shatteredpixeldungeon.items.Heap heap:Dungeon.level.heaps.valueList())
            for(Item item:heap.items) units+=item.quantity();
        for(java.util.ArrayList<Item> fallen:Dungeon.droppedItems.valueList())
            for(Item item:fallen) units+=item.quantity();
        return units;
    }

    @Test public void distantMagicMakesPhantomPiranhaJumpTowardsTheActualCaster() {
        for (int seat=0;seat<2;seat++) {
            Bundle parked;
            int id,pos,left;
            try(CoopSession game=CoopSession.open(479,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(game,Dungeon.level.exit());
                PhantomPiranha fish=SewerSpecialActorTest.actor(PhantomPiranha.class);
                SewerBeeTest.offer(game,seat);
                Level level=Dungeon.level;
                Hero caster=game.hero(seat);
                int cell=-1;
                for(int p=0;p<level.length();p++) {
                    if (!level.passable[p] || level.pit[p] || level.traps.get(p)!=null || Actor.findChar(p)!=null
                            || level.distance(p,fish.pos)<2 || level.distance(p,fish.pos)>5) continue;
                    if (new Ballistica(p,fish.pos,Ballistica.MAGIC_BOLT).collisionPos!=fish.pos) continue;
                    boolean landing=false;
                    for(int off:com.watabou.utils.PathFinder.NEIGHBOURS8) if(level.water[p+off] && Actor.findChar(p+off)==null) landing=true;
                    if (landing) {cell=p;break;}
                }
                assertTrue(cell>=0);
                caster.pos=cell;
                game.observe(seat);
                int hp=fish.HP;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,fish.pos,caster.belongings.weapon()));
                assertTrue(fish.HP<hp);
                assertTrue(level.water[fish.pos] && level.adjacent(caster.pos,fish.pos));
                assertTrue(fish.isTargeting(caster));
                id=fish.id(); pos=fish.pos; left=fish.HP;
                SewerBeeTest.cross(game,level.entrance());
                assertNull(Actor.findById(id));
                parked=game.checkpoint();
            }
            try (CoopSession restored=CoopSession.restore(parked)) {
                SewerBeeTest.cross(restored,Dungeon.level.exit());
                PhantomPiranha fish=(PhantomPiranha)Actor.findById(id);
                assertEquals(pos,fish.pos);
                assertEquals(left,fish.HP);
                for (int i=0;i<2;i++) restored.hero(i).HP=restored.hero(i).HT=180;
                com.watabou.utils.Random.pushGenerator(479+seat);
                for (int n=0;n<60 && fish.isAlive();n++) {
                    int active=restored.activeSeat();
                    SewerGhostTest.meet(restored,active,fish);
                    Hero striker=restored.hero(active);
                    if (n==0 || !CoopFloorsTest.step(restored,CoopSession.Action.ZAP,fish.pos,striker.belongings.weapon()))
                        assertTrue(CoopFloorsTest.step(restored,CoopSession.Action.ATTACK,fish.pos,null));
                }
                assertFalse(fish.isAlive());
                boolean meat=false;
                for (Item drop : Dungeon.level.heaps.get(fish.pos).items)
                    if (drop instanceof com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat) meat=true;
                assertTrue(meat);
                restored.checkpoint();
            }
        }
    }
}
