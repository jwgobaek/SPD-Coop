package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerHazardsTest extends HeadlessDungeon {
    public static int approach(CoopSession game,int seat,boolean dart) {
        Level l=Dungeon.level;
        for (int p=0;p<l.length();p++) {
            if (dart ? !(l.traps.get(p) instanceof WornDartTrap) : l.map[p]!=Terrain.LOCKED_DOOR) continue;
            if (Actor.findChar(p)!=null || l.heaps.get(p)!=null) continue;
            for (int off:PathFinder.NEIGHBOURS8) {
                int near=p+off;
                if (!l.passable[near] || Actor.findChar(near)!=null || l.heaps.get(near)!=null
                        || !CoopSession.supportedLanding(near)) continue;
                game.hero(seat).pos=near;
                if (game.observe(seat).visible(p)) return p;
            }
        }
        throw new AssertionError("Missing native terrain, dart="+dart);
    }

    @Test public void nativeAreaTrapsPersistTheirEffectsForEitherTriggeringHero() {
        for(int seat=0;seat<2;seat++) for(Trap trap:new Trap[]{
                new BurningTrap(),new ChillingTrap(),new ShockingTrap(),new ToxicTrap(),
                new ConfusionTrap(),new OozeTrap(),new AlarmTrap(),new FlockTrap(),
                new SummoningTrap(),new TeleportationTrap()}) {
            Bundle saved;
            int cell;
            java.util.Set<Integer> spawned=new java.util.HashSet<>();
            int endPos;
            try(CoopSession g=CoopSession.open(402,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.offer(g,seat);
                cell=approach(g,seat,true);
                // An exact native trap in a generated room, with the two seats independently observable.
                // This fixture does not claim that all trap classes occur on floor one.
                Level l=Dungeon.level;
                l.setTrap(trap,cell);
                Level.set(cell,Terrain.TRAP);
                trap.reveal();
                Hero h=g.hero(seat);
                h.HP=h.HT=300;
                java.util.Set<Integer> existing=new java.util.HashSet<>();
                for(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:l.mobs) existing.add(mob.id());
                assertTrue("Walk onto "+trap.getClass(),CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
                assertFalse(trap.active);
                Class<? extends Blob> cloud=trap instanceof BurningTrap ? Fire.class : trap instanceof ChillingTrap ? Freezing.class
                        : trap instanceof ShockingTrap ? Electricity.class : trap instanceof ToxicTrap ? ToxicGas.class
                        : trap instanceof ConfusionTrap ? ConfusionGas.class : null;
                if (cloud!=null) assertTrue("Missing cloud "+cloud,l.blobs.get(cloud).volume>0);
                if (trap instanceof OozeTrap) assertNotNull(h.buff(Ooze.class));
                if (trap instanceof TeleportationTrap) assertNotEquals(cell,h.pos);
                if (trap instanceof AlarmTrap) {
                    boolean called=false;
                    for(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:l.mobs) {
                        Bundle b=new Bundle(); mob.storeInBundle(b);
                        if(b.getInt("target")==cell) called=true;
                    }
                    assertTrue(called);
                }
                if (trap instanceof FlockTrap || trap instanceof SummoningTrap) {
                    for(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:l.mobs)
                        if(!existing.contains(mob.id())) {
                            assertSame(mob,Actor.findChar(mob.pos));
                            assertEquals(trap instanceof FlockTrap,mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep);
                            spawned.add(mob.id());
                        }
                    assertFalse(spawned.isEmpty());
                    if(trap instanceof SummoningTrap) assertTrue(spawned.size()<=3);
                }
                endPos=h.pos;
                saved=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(saved)) {
                assertEquals(endPos,g.hero(seat).pos);
                assertEquals(Terrain.INACTIVE_TRAP,Dungeon.level.map[cell]);
                for(int id:spawned) assertNotNull(Actor.findById(id));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.WAIT,0,null));
                if(trap instanceof FlockTrap) {
                    for(int i=0;i<25;i++) SewerSpecialActorTest.waitOne(g);
                    for(int id:spawned) assertNull(Actor.findById(id));
                }
                g.checkpoint();
                System.out.println("sewer_trap="+trap.getClass().getSimpleName()+" seat="+seat+" spawned="+spawned.size());
            }
        }
    }

    @Test public void hiddenOozeIsFloorUntilSearchRevealsItAndPressAffectsBothOccupants() {
        for(int seat=0;seat<2;seat++) try(CoopSession g=CoopSession.open(402,1,HeroClass.MAGE)) {
            g.join(2,HeroClass.MAGE);
            SewerBeeTest.offer(g,seat);
            int cell=approach(g,seat,true);
            OozeTrap trap=new OozeTrap();
            Dungeon.level.setTrap(trap,cell);
            Level.set(cell,Terrain.SECRET_TRAP);
            trap.hide();
            assertEquals(Terrain.EMPTY,g.observe(seat).terrain(cell));
            assertFalse(trap.visible);
            for(int n=0;n<15 && !trap.visible;n++) {
                SewerBeeTest.offer(g,seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.SEARCH,g.hero(seat).pos,null));
            }
            assertTrue(trap.visible);
            assertEquals(Terrain.TRAP,g.observe(seat).terrain(cell));
            for(int off:PathFinder.NEIGHBOURS8) {
                int p=cell+off;
                if(Dungeon.level.passable[p] && Actor.findChar(p)==null && !Dungeon.level.water[p]) {
                    g.hero(1-seat).pos=p;
                    break;
                }
            }
            SewerBeeTest.offer(g,seat);
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
            assertNotNull(g.hero(seat).buff(Ooze.class));
            assertNotNull(g.hero(1-seat).buff(Ooze.class));
        }
    }

    @Test public void walkingAndForcedLandingUseNativeDartAndPersistTheSpentTrap() {
        for(int seat=0;seat<2;seat++) for(boolean forced:new boolean[]{false,true}) {
            Bundle saved;
            int cell;
            try(CoopSession g=CoopSession.open(402,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.offer(g,seat);
                Hero h=g.hero(seat);
                cell=approach(g,seat,true);
                Trap trap=Dungeon.level.traps.get(cell);
                assertFalse(trap.canBeHidden);
                assertTrue(trap.hide().visible);
                assertTrue(trap.active);
                assertTrue(new Ankh().collect(h.belongings.backpack));
                h.HP=1;
                if (forced) {
                    Ballistica path=new Ballistica(h.pos,cell,Ballistica.STOP_TARGET);
                    WandOfBlastWave.throwChar(h,path,1,false,false,trap);
                    assertTrue(CoopFloorsTest.step(g,CoopSession.Action.WAIT,0,null));
                } else assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
                assertEquals(cell,h.pos);
                assertFalse(trap.active);
                assertEquals(Terrain.INACTIVE_TRAP,Dungeon.level.map[cell]);
                assertEquals(1,g.preDeathHP(seat));
                assertEquals(CoopSession.Life.DOWNED,g.life(seat));
                assertEquals(CoopSession.Life.ALIVE,g.life(1-seat));
                saved=g.checkpoint(); // Includes validation that the native VFX actor has finished.
            }
            try(CoopSession g=CoopSession.restore(saved)) {
                assertEquals(CoopSession.Life.DOWNED,g.life(seat));
                assertFalse(Dungeon.level.traps.get(cell).active);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.WAIT,0,null));
            }
        }
    }

    @Test public void anEmptyDartTargetsAnUnobstructedCharacterOnItsOwnActorTick() {
        for(int seat=0;seat<2;seat++) try(CoopSession g=CoopSession.open(402,1,HeroClass.MAGE)) {
            g.join(2,HeroClass.MAGE);
            int cell=approach(g,seat,true);
            Hero h=g.hero(seat);
            int before=h.HP;
            assertNull(Actor.findChar(cell));
            assertTrue(h.pos==new Ballistica(cell,h.pos,Ballistica.PROJECTILE).collisionPos);
            for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m:Dungeon.level.mobs) {
                // Make the intended visible target uniquely closest in the original trap search.
                if(Dungeon.level.trueDistance(cell,m.pos)<=Dungeon.level.trueDistance(cell,h.pos)) m.invisible++;
            }
            int other=g.hero(1-seat).HP;
            Dungeon.level.pressCell(cell);
            assertEquals(before,h.HP);
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.WAIT,0,null));
            assertTrue(h.HP<before && h.HP>=before-8);
            assertEquals(other,g.hero(1-seat).HP);
            g.checkpoint();
        }
    }

    @Test public void otherSeatCanSpendOneDepthKeyAndBothDoorAndBalanceSurviveParking() {
        for(int seat=0;seat<2;seat++) {
            Bundle saved;
            int cell;
            try(CoopSession g=CoopSession.open(402,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.offer(g,seat);
                cell=approach(g,seat,false);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.UNLOCK,cell,null));
                Hero collector=g.hero(seat);
                IronKey key=new IronKey(Dungeon.depth);
                Dungeon.level.drop(key,collector.pos);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,collector.pos,key));
                assertEquals(1,Notes.keyCount(new IronKey(1)));
                assertEquals(0,Notes.keyCount(new IronKey(2)));
                SewerBeeTest.offer(g,1-seat);
                assertEquals(cell,approach(g,1-seat,false));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.UNLOCK,cell,null));
                assertEquals(0,Notes.keyCount(new IronKey(1)));
                assertEquals(Terrain.DOOR,Dungeon.level.map[cell]);
                SewerBeeTest.offer(g,seat);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.UNLOCK,cell,null));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
                SewerBeeTest.cross(g,Dungeon.level.exit());
                saved=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(saved)) {
                SewerBeeTest.cross(g,Dungeon.level.entrance());
                assertTrue(Dungeon.level.map[cell]==Terrain.DOOR || Dungeon.level.map[cell]==Terrain.OPEN_DOOR);
                assertEquals(0,Notes.keyCount(new IronKey(1)));
            }
        }
    }
}
