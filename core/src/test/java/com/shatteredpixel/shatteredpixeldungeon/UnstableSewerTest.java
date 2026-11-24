package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class UnstableSewerTest extends HeadlessDungeon {
    // Choose a reproducible first draw and on-hit roll on a private test RNG stack.
    // Unstable.proc still performs the actual uniform draw from its production list.
    private static long seedFor(int index) {
        for(long seed=0;seed<100000;seed++) {
            Random.pushGenerator(seed);
            boolean selected = Random.Int(14)==index && Random.Float()<0.01f;
            Random.popGenerator();
            if (selected) return seed;
        }
        throw new AssertionError("Missing test seed");
    }

    private static int strip(CoopSession g) {
        Level l=Dungeon.level;
        for(int p=l.width()*2;p<l.length()-l.width()*2-4;p++) {
            if(p%l.width()>l.width()-6) continue;
            boolean clear=true;
            for(int n=0;n<4;n++) if (!l.passable[p+n] || l.pit[p+n] || l.traps.get(p+n)!=null
                    || l.plants.get(p+n)!=null || l.getTransition(p+n)!=null || l.heaps.get(p+n)!=null
                    || Actor.findChar(p+n)!=null) clear=false;
            if(clear) return p;
        }
        throw new AssertionError("No clear encounter strip");
    }

    @Test public void eachNativeRandomDelegateExecutesAgainstEitherSeat() {
        for (int i=0;i<14;i++) for (int seat=0;seat<2;seat++) {
            try(CoopSession g=CoopSession.open(421,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(g,Dungeon.level.exit());
                Statue statue=SewerSpecialActorTest.actor(Statue.class);
                Weapon weapon=statue.weapon();
                assertEquals(Unstable.class,weapon.enchantment.getClass());
                Hero victim=g.hero(seat), observer=g.hero(1-seat);
                int start=strip(g);
                statue.pos=start;
                victim.pos=start+1;
                observer.pos=start+3;
                victim.HP=victim.HT=100;
                statue.HP=1;
                Level.set(victim.pos,Terrain.EMPTY);
                Dungeon.level.updateFieldOfView(observer,observer.fieldOfView);
                long seed=seedFor(i);
                Random.pushGenerator(seed);
                int damage=weapon.enchantment.proc(weapon,statue,victim,8);
                switch(i) {
                    case 0: assertNotNull(victim.buff(Burning.class)); break;
                    case 1: assertTrue(statue.shielding()>0); break;
                    case 2: assertEquals(Terrain.HIGH_GRASS,Dungeon.level.map[victim.pos]); break;
                    case 3: assertNotNull(victim.buff(Chill.class)); break;
                    case 4:
                        assertNotNull(statue.buff(Kinetic.KineticTracker.class));
                        victim.damage(damage,statue);
                        assertNull(statue.buff(Kinetic.KineticTracker.class));
                        break;
                    case 5: assertNull(victim.buff(Corrupting.CorruptingTracker.class)); break;
                    case 6:
                        assertTrue(Pushing.pushingExistsForChar(victim));
                        SewerSpecialActorTest.waitOne(g);
                        assertNotEquals(start+1,victim.pos);
                        assertFalse(Pushing.pushingExistsForChar(victim));
                        break;
                    case 7:
                        assertNotNull(statue.buff(Grim.GrimTracker.class));
                        victim.HP=12;
                        victim.damage(damage,statue);
                        assertFalse(victim.isAlive());
                        assertEquals(4,g.preDeathHP(seat));
                        break;
                    case 8: assertNotNull(victim.buff(Lucky.LuckProc.class)); break;
                    case 9:
                        assertEquals(observer.HT-4,observer.HP);
                        assertEquals(100,victim.HP);
                        break;
                    case 10: assertEquals(5,statue.HP); break;
                    case 11: assertNotNull(victim.buff(Poison.class)); break;
                    case 12:
                        assertNotNull(statue.buff(Vorpal.VorpalTracker.class));
                        victim.damage(damage,statue);
                        assertNotNull(victim.buff(Bleeding.class));
                        break;
                    case 13:
                        assertNotNull(victim.buff(Vertigo.class));
                        assertNull(observer.buff(Vertigo.class));
                        break;
                    default: throw new AssertionError();
                }
                assertEquals(8,damage);
                Random.popGenerator();
                System.out.println("unstable delegate="+i+" seat="+seat+" draw_seed="+seed);
            }
        }
    }

    @Test public void vertigoSpendsOneNativeMoveAndKeepsItsRemainingClockAcrossRestore() {
        for(int seat=0;seat<2;seat++) {
            Bundle checkpoint;
            float remaining;
            int start;
            try(CoopSession g=CoopSession.open(421,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.offer(g,seat);
                start=strip(g)+1;
                Hero h=g.hero(seat);
                h.pos=start;
                g.observe(seat);
                Buff.prolong(h,Vertigo.class,5);
                remaining=h.buff(Vertigo.class).cooldown();
                checkpoint=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(checkpoint)) {
                Hero h=g.hero(seat);
                assertEquals(remaining,h.buff(Vertigo.class).cooldown(),0.0001f);
                long seed=0;
                for(;;seed++) {
                    Random.pushGenerator(seed);
                    int p=start+com.watabou.utils.PathFinder.NEIGHBOURS8[Random.Int(8)];
                    Random.popGenerator();
                    if(p==start-1) break;
                }
                Random.pushGenerator(seed);
                int other=g.hero(1-seat).pos;
                float before=com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()+h.cooldown();
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,start+1,null));
                assertEquals(start-1,h.pos);
                assertEquals(before+1/h.speed(),Actor.now()+h.cooldown(),0.0001f);
                assertEquals(other,g.hero(1-seat).pos);
                g.checkpoint();
            }
        }
    }

    @Test public void naturalUnstableStrikesContinueAfterParkingAndRevival() {
        for(int seat=0;seat<2;seat++) {
            Bundle parked;
            long weaponId;
            int statueId;
            try(CoopSession g=CoopSession.open(421,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(g,Dungeon.level.exit());
                Statue statue=SewerSpecialActorTest.actor(Statue.class);
                statueId=statue.id();
                weaponId=g.ownership.identity(statue.weapon());
                assertEquals(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar.class,statue.weapon().getClass());
                assertEquals(Unstable.class,statue.weapon().enchantment.getClass());
                SewerBeeTest.cross(g,Dungeon.level.entrance());
                assertNull(Actor.findById(statueId));
                parked=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(parked)) {
                SewerBeeTest.cross(g,Dungeon.level.exit());
                Statue statue=(Statue)Actor.findById(statueId);
                assertEquals(weaponId,g.ownership.identity(statue.weapon()));
                Hero h=g.hero(seat);
                assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.Ankh().collect(h.belongings.backpack));
                Random.pushGenerator(421+seat);
                int hits=0;
                // Real accuracy rolls, random enchantment draws and final damage. Stage the combatants only.
                for(int n=0;n<70 && g.life(seat)==CoopSession.Life.ALIVE;n++) {
                    SewerGhostTest.meet(g,seat,statue);
                    if(statue.attack(h)) hits++;
                    if(g.life(seat)==CoopSession.Life.ALIVE) SewerSpecialActorTest.waitOne(g);
                }
                assertTrue(hits>=2);
                assertEquals(CoopSession.Life.DOWNED,g.life(seat));
                g.finishEffects();
                int pre=g.preDeathHP(seat);
                int body=h.pos;
                // Move the rescuer adjacent at the next input boundary in this combat fixture.
                for(int off:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int p=body+off;
                    if(Dungeon.level.passable[p] && CoopSession.supportedLanding(p) && Actor.findChar(p)==null) {
                        g.hero(1-seat).pos=p;
                        break;
                    }
                }
                g.observe(1-seat);
                if(g.activeSeat()==seat) throw new AssertionError("Downed hero offered input");
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.REVIVE,body,null));
                assertEquals(CoopSession.Life.ALIVE,g.life(seat));
                assertTrue(h.HP<=Math.max(1,pre*3/10));
                assertNull(h.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.Ankh.class));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.WAIT,0,null));
                g.checkpoint();
                System.out.println("natural unstable seat="+seat+" native_hits="+hits);
            }
        }
    }

    @Test public void nativePushResolvesOccupancyOnWallsWaterGrassAndLateArrivals() {
        for(int terrain : new int[]{Terrain.WALL,Terrain.WATER,Terrain.HIGH_GRASS,Terrain.EMPTY}) {
            try(CoopSession g=CoopSession.open(421,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                int pos=strip(g);
                com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat rat=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();
                rat.pos=pos; rat.HT=rat.HP=50; GameScene.add(rat,100);
                Hero pusher=g.hero(1);
                Level.set(pos+1,terrain==Terrain.WALL ? Terrain.EMPTY : terrain);
                Level.set(pos+2,terrain==Terrain.WALL ? Terrain.WALL : Terrain.EMPTY);
                if(terrain==Terrain.HIGH_GRASS) g.hero(0).pos=pos+2;
                com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica ray =
                        new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(pos,pos+3,
                                com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);
                int distance=terrain==Terrain.WATER || terrain==Terrain.EMPTY ? 1 : 2;
                com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(rat,ray,distance,true,true,pusher);
                assertTrue(Pushing.pushingExistsForChar(rat));
                if(terrain==Terrain.EMPTY) g.hero(0).pos=pos+1;
                SewerSpecialActorTest.waitOne(g);
                assertEquals(terrain==Terrain.EMPTY ? pos : pos+1,rat.pos);
                assertFalse(Pushing.pushingExistsForChar(rat));
                assertSame(rat,Actor.findChar(rat.pos));
                if(terrain==Terrain.WALL || terrain==Terrain.HIGH_GRASS) assertTrue(rat.HP<50);
                else assertEquals(50,rat.HP);
                g.checkpoint();
            }
        }
    }

    @Test public void oneUnchangedRandomStreamProducesSeveralDifferentOnHitResults() {
        try(CoopSession g=CoopSession.open(421,1,HeroClass.MAGE)) {
            g.join(2,HeroClass.MAGE);
            SewerBeeTest.cross(g,Dungeon.level.exit());
            Statue statue=SewerSpecialActorTest.actor(Statue.class);
            Hero target=g.hero(1);
            int strip=strip(g);
            statue.pos=strip; target.pos=strip+1;
            target.HP=target.HT=2000;
            java.util.Set<String> markers=new java.util.HashSet<>();
            Random.pushGenerator(421);
            try {
                for(int n=0;n<80 && markers.size()<4;n++) {
                    if (!statue.attack(target)) continue;
                    for(Buff buff:target.buffs())
                        if(buff instanceof Burning || buff instanceof Poison || buff instanceof Vertigo || buff instanceof Bleeding
                                || buff instanceof Chill || buff instanceof Lucky.LuckProc) markers.add(buff.getClass().getSimpleName());
                    if (statue.shielding()>0) markers.add("Blocking");
                    if (Pushing.pushingExistsForChar(target)) markers.add("Elastic");
                }
                assertTrue(markers.toString(),markers.size()>=4);
                System.out.println("mixed native strike markers="+new java.util.TreeSet<>(markers));
            } finally { Random.popGenerator(); }
        }
    }

    @Test public void spreadingFireTransformsOwnedStacksAndPausesOnParkedFloor() {
        Bundle saved;
        int cell;
        long original,cooked;
        try(CoopSession g=CoopSession.open(416,1,HeroClass.MAGE)) {
            g.join(2,HeroClass.WARRIOR);
            SewerBeeTest.cross(g,Dungeon.level.exit());
            cell=strip(g)+1;
            Level.set(cell-1,Terrain.GRASS);
            Level.set(cell,Terrain.HIGH_GRASS);
            Item meat=new MysteryMeat().quantity(3);
            g.ownership.claim(meat,g.player(1));
            original=g.ownership.identity(meat);
            Dungeon.level.drop(meat,cell);
            GameScene.add(Blob.seed(cell-1,4,Fire.class));
            for(int n=0;n<8 && Dungeon.level.heaps.get(cell).peek()==meat;n++) SewerSpecialActorTest.waitOne(g);
            Item result=Dungeon.level.heaps.get(cell).peek();
            assertTrue(result instanceof ChargrilledMeat);
            assertEquals(3,result.quantity());
            assertEquals(g.player(1),g.ownership.owner(result));
            cooked=g.ownership.identity(result);
            assertNotEquals(original,cooked);
            assertTrue(Blob.volumeAt(cell,Fire.class)>0);
            SewerBeeTest.cross(g,Dungeon.level.entrance());
            saved=g.checkpoint();
        }
        try(CoopSession g=CoopSession.restore(saved)) {
            for(int i=0;i<8;i++) SewerSpecialActorTest.waitOne(g);
            SewerBeeTest.cross(g,Dungeon.level.exit());
            assertTrue(Blob.volumeAt(cell,Fire.class)>0);
            Item meat=Dungeon.level.heaps.get(cell).peek();
            assertEquals(cooked,g.ownership.identity(meat));
            assertEquals(g.player(1),g.ownership.owner(meat));
            for(int i=0;i<18 && Dungeon.level.map[cell]!=Terrain.EMBERS;i++) SewerSpecialActorTest.waitOne(g);
            assertEquals(Terrain.EMBERS,Dungeon.level.map[cell]);
        }
    }
}
