package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.watabou.utils.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerDepthTest extends HeadlessDungeon {
    public static void gate(CoopSession game,boolean descend) {
        SewerSpecialActorTest.atGate(game,descend ? Dungeon.level.exit() : Dungeon.level.entrance());
        CoopFloorsTest.cross(game);
    }
    @Test public void consecutiveNaturalDestinationsEitherRoundTripOrRestoreTheLastBoundary() {
        int reached=0,blocked=0;
        ArrayList<Integer> seeds=new ArrayList<>();
        for(int i=402;i<=423;i++) seeds.add(i);
        for(int i=446;i<=453;i++) seeds.add(i);
        seeds.add(716);
        for(int seed:seeds) {
            int deepest=0;
            try(CoopSession g=CoopSession.open(seed,1,HeroClass.WARRIOR)) {
                g.join(2,HeroClass.MAGE);
                deepest=1;
                String reason="none";
                for(int depth=2;depth<=5;depth++) {
                    int cell=Dungeon.level.exit();
                    SewerSpecialActorTest.atGate(g,cell);
                    int oldFloor=Dungeon.depth;
                    Bundle before=g.checkpoint();
                    assertTrue(g.stairs.consent(1-g.activeSeat(),g.turn(),cell,false));
                    if(!CoopFloorsTest.step(g,CoopSession.Action.TRAVEL,cell,null)) {
                        assertTrue(g.restoredAfterTravel()); assertFalse(g.stopped());
                        Bundle after=g.checkpoint();
                        assertEquals(oldFloor,Dungeon.depth);
                        assertArrayEquals(before.getLongArray("random"),after.getLongArray("random"));
                        assertArrayEquals(before.getBundle("floor").getIntArray("map"),after.getBundle("floor").getIntArray("map"));
                        assertEquals(before.getLong("turn"),after.getLong("turn"));
                        reason=g.destinationBlocker; blocked++; break;
                    }
                    deepest=depth;
                }
                if(deepest==5) reached++;
                for(int back=deepest-1;back>=1;back--) gate(g,false);
                SewerSpecialActorTest.waitOne(g);
                System.out.println("SURVEY seed="+seed+" deepest="+deepest+" returned=1 blocker="+reason);
            } catch(CoopFloors.UnsupportedDestination e) {
                assertEquals(0,deepest); assertNull(Dungeon.coop); blocked++;
                System.out.println("SURVEY seed="+seed+" deepest=0 returned=NA blocker="+e.getMessage());
            }
        }
        assertTrue(reached>0); assertTrue(blocked>0); assertEquals(seeds.size(),reached+blocked);
    }

    @Test public void visitEverySewerAndRestoreFourIndependentDormantRecords() {
        for (long seed:new long[]{402,415}) {
            int[] heroes=new int[2];
            long owned;
            int drop;
            Bundle save;
            try (CoopSession game=CoopSession.open(seed,1,HeroClass.WARRIOR)) {
                game.join(2,HeroClass.HUNTRESS);
                for (int seat=0;seat<2;seat++) heroes[seat]=game.hero(seat).id();
                drop=game.hero(0).pos;
                Food food=game.hero(0).belongings.getItem(Food.class);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.DROP,drop,food));
                owned=game.ownership.identity(Dungeon.level.heaps.get(drop).peek());
                for(int depth=2;depth<=5;depth++) {
                    long begin=System.nanoTime();
                    gate(game,true);
                    assertEquals(depth,Dungeon.depth);
                    save=game.checkpoint();
                    assertEquals(depth<=2?2:3,save.getInt("schema"));
                    System.out.println("GATE seed="+seed+" depth="+depth+" actors="+Actor.all().size()+" imageBytes="+
                            save.toString().getBytes(StandardCharsets.UTF_8).length+" gateAndCaptureUs="+(System.nanoTime()-begin)/1000);
                }
                save=game.checkpoint();
                assertEquals(4,save.getBundle("dormant").getKeys().size());
            }
            try(CoopSession game=CoopSession.restore(save)) {
                assertEquals(5,Dungeon.depth);
                assertNotNull(SewerSpecialActorTest.actor(Goo.class));
                for(int depth=4;depth>=1;depth--) {
                    gate(game,false);
                    assertEquals(depth,Dungeon.depth);
                    assertNotEquals(game.hero(0).pos,game.hero(1).pos);
                    for(int seat=0;seat<2;seat++) assertEquals(heroes[seat],game.hero(seat).id());
                    SewerSpecialActorTest.waitOne(game);
                }
                Item ration=Dungeon.level.heaps.get(drop).peek();
                assertEquals(owned,game.ownership.identity(ration));
                assertEquals(1,game.ownership.owner(ration));
            }
            Bundle records=save.getBundle("dormant");
            records.put("4",records.getBundle("3"));
            try (CoopSession invalid=CoopSession.restore(save)) {fail("mixed floors");}
            catch(IllegalArgumentException expected) { assertNull(Dungeon.coop); }
        }
    }

    public static void arrangeBoss(CoopSession game,Goo goo) {
        // Isolated battle on a dry patch of the generated arena. Water regeneration has its own test.
        Level l=Dungeon.level;
        com.watabou.utils.Random.pushGenerator(402); // reproducible isolated combat stream, captured in the save
        int dry=-1;
        for(int c=0;c<l.length();c++) {
            if(l.insideMap(c) && l.openSpace[c] && !l.water[c] && !l.pit[c] && l.findMob(c)==null
                    && Actor.findChar(c)==null && l.plants.get(c)==null && l.traps.get(c)==null
                    && (dry<0 || l.distance(goo.pos,c)<l.distance(goo.pos,dry))) dry=c;
        }
        assertTrue("No dry battle position",dry>=0);
        goo.pos=dry;
        SewerSpecialActorTest.atGate(game,goo.pos);
        for(int seat=0;seat<2;seat++) game.hero(seat).HP=game.hero(seat).HT=150;
    }

    @Test public void waterRegenerationAndAFallenCompanionSurviveASealedFloorCheckpoint() {
        for(int victim=0;victim<2;victim++) {
            Bundle fallen,revived;
            int id;
            try(CoopSession g=CoopSession.open(402,1,HeroClass.WARRIOR)) {
                g.join(2,HeroClass.HUNTRESS);
                for(int depth=2;depth<=5;depth++) gate(g,true);
                Goo goo=SewerSpecialActorTest.actor(Goo.class); id=goo.id();
                int water=-1;
                for(int p=0;p<Dungeon.level.length();p++) if(Dungeon.level.water[p] && Dungeon.level.passable[p]
                        && Actor.findChar(p)==null && (water<0 || Dungeon.level.distance(goo.pos,p)<Dungeon.level.distance(goo.pos,water))) water=p;
                assertTrue(water>=0);
                goo.pos=water;
                goo.damage(5,g.hero(1-victim));
                int injured=goo.HP;
                for(int n=0;n<10 && goo.HP==injured;n++) SewerSpecialActorTest.waitOne(g);
                assertTrue(goo.HP>injured && goo.HP<=goo.HT);
                SewerBeeTest.offer(g,1-victim);
                SewerGhostTest.meet(g,victim,goo);
                Hero down=g.hero(victim);
                for(int off:PathFinder.NEIGHBOURS8) {
                    int p=down.pos+off;
                    if(Dungeon.level.passable[p] && Actor.findChar(p)==null && !Dungeon.level.pit[p]
                            && Dungeon.level.traps.get(p)==null) {g.hero(1-victim).pos=p;break;}
                }
                g.observe(0);g.observe(1);
                down.HP=17;
                Ankh token=new Ankh(); assertTrue(token.collect(down.belongings.backpack));
                assertTrue(goo.attack(down,1,1000,Float.POSITIVE_INFINITY));
                assertEquals(CoopSession.Life.DOWNED,g.life(victim));
                assertEquals(17,g.preDeathHP(victim));
                // Separate the restore/revive checkpoint from a second, immediately lethal enemy turn.
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(goo,
                        com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class,4f);
                fallen=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(fallen)) {
                assertTrue(Dungeon.level.locked);
                assertTrue(g.hero(victim).frozenInCoop());
                assertEquals(id,SewerSpecialActorTest.actor(Goo.class).id());
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.REVIVE,g.hero(victim).pos,null));
                assertEquals(CoopSession.Life.ALIVE,g.life(victim));
                assertTrue(g.hero(victim).HP>0 && g.hero(victim).HP<=5);
                assertNull(g.hero(victim).belongings.getItem(Ankh.class));
                revived=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(revived)) {
                assertTrue(Dungeon.level.locked); assertFalse(g.hero(victim).frozenInCoop());
                assertNull(g.hero(victim).belongings.getItem(Ankh.class));
            }
        }
    }

    @Test public void isolatedBossBattleSealsBothHeroesAndDropsOneNativeRewardSet() {
        Bundle save;
        int bossId;
        try (CoopSession game=CoopSession.open(402,1,HeroClass.WARRIOR)) {
            game.join(2,HeroClass.HUNTRESS);
            for(int i=2;i<=5;i++) gate(game,true);
            Goo goo=SewerSpecialActorTest.actor(Goo.class); bossId=goo.id();
            // Isolated combat fixture. Preserve the generated boss and its native HP, AI and rewards.
            arrangeBoss(game,goo);
            for(int n=0;n<120 && goo.chargeStage()==0;n++) SewerSpecialActorTest.waitOne(game);
            assertTrue(goo.chargeStage()>0);
            assertTrue(Dungeon.level.locked);
            for(int s=0;s<2;s++) assertNotNull(game.hero(s).buff(LockedFloor.class));
            assertEquals(-1,game.stairs.stair());
            save=game.checkpoint();
        }
        try(CoopSession game=CoopSession.restore(save)) {
            Goo goo=(Goo)Actor.findById(bossId);
            assertTrue(goo.chargeStage()>0 && Dungeon.level.locked);
            int[] attacks=new int[2];
            for(int n=0;n<250 && goo.isAlive();n++) {
                int s=game.activeSeat(); assertTrue(s>=0);
                Hero h=game.hero(s);
                if(Dungeon.level.adjacent(h.pos,goo.pos)) {
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,goo.pos,null));
                    attacks[s]++;
                } else if (s==1 && CoopFloorsTest.step(game,CoopSession.Action.SHOOT,goo.pos,h.belongings.getItem(
                        com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow.class))) attacks[s]++;
                else SewerSpecialActorTest.waitOne(game);
            }
            assertTrue(attacks[0]>0 && attacks[1]>0);
            assertFalse(goo.isAlive());
            assertFalse(Dungeon.level.locked);
            int keys=0,blobs=0;
            for(Heap heap:Dungeon.level.heaps.valueList()) for(Item item:heap.items) {
                if(item instanceof WornKey) keys+=item.quantity();
                if(item instanceof GooBlob) blobs+=item.quantity();
            }
            assertEquals(1,keys); assertTrue(blobs>=2 && blobs<=4);
            save=game.checkpoint();
            System.out.println("BOSS isolated native attacks="+Arrays.toString(attacks)+" keys="+keys+" blobs="+blobs);
        }
        try(CoopSession game=CoopSession.restore(save)) {
            assertFalse(Dungeon.level.locked);
            assertNull(Actor.findById(bossId));
            Heap keys=null;
            for(Heap pile:Dungeon.level.heaps.valueList()) if(pile.peek() instanceof WornKey) keys=pile;
            assertNotNull(keys);
            SewerBeeTest.offer(game,1);
            Hero guest=game.hero(1);
            if(game.hero(0).pos==keys.pos) SewerSpecialActorTest.atGate(game,keys.pos);
            guest.pos=keys.pos; game.observe(1);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,guest.pos,keys.peek()));
            assertEquals(1,com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(new WornKey(5)));
            SewerBeeTest.offer(game,0);
            int exit=Dungeon.level.exit();
            SewerSpecialActorTest.atGate(game,exit);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.UNLOCK,exit,null));
            assertEquals(Terrain.UNLOCKED_EXIT,Dungeon.level.map[exit]);
            assertEquals(0,com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(new WornKey(5)));
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.UNLOCK,exit,null));
        }
    }
}
