package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerPlantTest extends HeadlessDungeon {
    /** Isolated landing in an original level, optionally outside the other seat's direct sight. */
    public static int arrange(CoopSession game, int seat, Plant.Seed seed, boolean privateCell) {
        Level level = Dungeon.level;
        FloorKnowledge other = game.observe(1-seat);
        for (int pos = 0; pos < level.length(); pos++) {
            if (level.map[pos] != Terrain.EMPTY || Actor.findChar(pos) != null || level.heaps.get(pos) != null
                    || level.plants.get(pos) != null || (privateCell && other.visible(pos))) continue;
            for (int offset : PathFinder.NEIGHBOURS8) {
                int near = pos + offset;
                if (!level.insideMap(near) || level.map[near] != Terrain.EMPTY || Actor.findChar(near) != null
                        || level.heaps.get(near) != null || level.plants.get(near) != null) continue;
                game.hero(seat).pos = near;
                level.plant(seed,pos);
                assertTrue(game.observe(seat).visible(pos));
                return pos;
            }
        }
        throw new AssertionError("No free landing");
    }

    private static void move(CoopSession game, int cell) {
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,cell,null));
        assertNull(Dungeon.level.plants.get(cell));
    }

    @Test public void blindweedAndStormvineAffectOnlyTheOccupantAndPersist() {
        for (int seat=0; seat<2; seat++) for (boolean blind : new boolean[]{true,false}) {
            Bundle save;
            Class<? extends FlavourBuff> type = blind ? Blindness.class : Vertigo.class;
            float duration;
            try (CoopSession game = CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.HUNTRESS);
                SewerBeeTest.offer(game,seat);
                int pos = arrange(game,seat,blind ? new Blindweed.Seed() : new Stormvine.Seed(),false);
                move(game,pos);
                Hero hero = game.hero(seat);
                assertEquals(blind,hero.buff(Cripple.class)!=null);
                assertNull(game.hero(1-seat).buff(type));
                duration=hero.buff(type).cooldown();
                assertTrue(duration>0);
                if (blind) {
                    FloorKnowledge sight = game.observe(seat);
                    for (int i=0;i<sight.length();i++)
                        if (Dungeon.level.distance(pos,i)>1) assertFalse(sight.visible(i));
                }
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                assertEquals(duration,game.hero(seat).buff(type).cooldown(),0f);
                assertNull(game.hero(1-seat).buff(type));
                SewerSpecialActorTest.waitOne(game);
            }
        }
    }

    @Test public void mageroyalCuresOnlyTheOccupantWithoutHealingTheParty() {
        for (int seat=0; seat<2; seat++) try (CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
            game.join(2,HeroClass.HUNTRESS);
            SewerBeeTest.offer(game,seat);
            int cell=arrange(game,seat,new Mageroyal.Seed(),false);
            for (int i=0;i<2;i++) {
                Buff.prolong(game.hero(i),Weakness.class,20);
                Buff.prolong(game.hero(i),Vulnerable.class,20);
            }
            move(game,cell);
            assertNull(game.hero(seat).buff(Weakness.class));
            assertNull(game.hero(seat).buff(Vulnerable.class));
            assertNotNull(game.hero(1-seat).buff(Weakness.class));
            assertNotNull(game.hero(1-seat).buff(Vulnerable.class));
        }
    }

    @Test public void sungrassRetainsItsPartialHealingAndStopsAfterLeavingTheCell() {
        for (int seat=0; seat<2; seat++) {
            Bundle save;
            int from, hp;
            try (CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.HUNTRESS);
                SewerBeeTest.offer(game,seat);
                int cell=arrange(game,seat,new Sungrass.Seed(),false);
                Hero hero=game.hero(seat);
                from=hero.pos;
                hero.HP=4;
                move(game,cell);
                for (int i=0;i<20 && hero.HP==4;i++) SewerSpecialActorTest.waitOne(game);
                assertTrue(hero.HP>4 && hero.HP<hero.HT);
                assertNotNull(hero.buff(Sungrass.Health.class));
                assertNull(game.hero(1-seat).buff(Sungrass.Health.class));
                hp=hero.HP;
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                assertEquals(hp,game.hero(seat).HP);
                assertNotNull(game.hero(seat).buff(Sungrass.Health.class));
                SewerBeeTest.offer(game,seat);
                move(game,from);
                for (int i=0;i<4 && game.hero(seat).buff(Sungrass.Health.class)!=null;i++) SewerSpecialActorTest.waitOne(game);
                assertNull(game.hero(seat).buff(Sungrass.Health.class));
            }
        }
    }

    @Test public void icecapConsumesOneCarriedUnitAndRestoringFrostCannotConsumeAnother() {
        for (int seat=0;seat<2;seat++) {
            Bundle save;
            long bottleId;
            try (CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.HUNTRESS);
                SewerBeeTest.offer(game,seat);
                int cell=arrange(game,seat,new Icecap.Seed(),true);
                Hero hero=game.hero(seat);
                Potion potion=new PotionOfHealing();
                potion.quantity(3);
                assertTrue(potion.collect(hero.belongings.backpack));
                game.ownership.claim(potion,game.player(seat));
                bottleId=game.ownership.identity(potion);
                move(game,cell);
                assertEquals(2,potion.quantity());
                assertNotNull(hero.buff(Frost.class));
                assertNull(game.hero(1-seat).buff(Frost.class));
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                Potion potion=hero.belongings.getItem(PotionOfHealing.class);
                assertEquals(2,potion.quantity());
                assertEquals(bottleId,game.ownership.identity(potion));
                assertEquals(game.player(seat),game.ownership.owner(potion));
                assertEquals(1,hero.paralysed);
                for(int i=0;i<32 && hero.buff(Frost.class)!=null;i++) SewerSpecialActorTest.waitOne(game);
                assertNull(hero.buff(Frost.class));
                assertEquals(0,hero.paralysed);
                assertEquals(2,potion.quantity());
            }
        }
    }

    @Test public void eachBasicPotionShattersOnTheFloorAndSplashesItsOccupant() {
        for(Potion potion : new Potion[]{new PotionOfHealing(),new PotionOfStrength(),new PotionOfMindVision(),
                new PotionOfInvisibility(),new PotionOfHaste(),new PotionOfExperience()}) {
            try (CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.HUNTRESS);
                Hero ally=game.hero(1);
                Buff.affect(ally,Ooze.class);
                int hp=ally.HP;
                Heap heap=Dungeon.level.drop(potion,ally.pos);
                game.ownership.claim(potion,game.player(1));
                heap.freeze();
                if (potion instanceof PotionOfStrength) {
                    assertTrue(potion.unique);
                    assertTrue(heap.items.contains(potion));
                    assertNotNull(ally.buff(Ooze.class));
                    continue;
                }
                assertFalse(heap.items.contains(potion));
                assertNull(ally.buff(Ooze.class));
                assertEquals(hp,ally.HP);
                assertNull(ally.buff(Healing.class));
                Bundle saved=game.checkpoint();
                assertNotNull(saved);
            }
        }
    }

    @Test public void firebloomUsesTheNativeFireBlobAndBurnsTheLanding() {
        for(int seat=0;seat<2;seat++) {
            Bundle saved;
            int cell;
            try(CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.HUNTRESS);
                SewerBeeTest.offer(game,seat);
                cell=arrange(game,seat,new Firebloom.Seed(),true);
                move(game,cell);
                assertTrue(Blob.volumeAt(cell,Fire.class)>0);
                saved=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat);
                int hp=hero.HP;
                assertTrue(Blob.volumeAt(cell,Fire.class)>0);
                for(int i=0;i<12 && hero.HP==hp;i++) SewerSpecialActorTest.waitOne(game);
                assertTrue(hero.HP<hp);
                assertNotNull(hero.buff(Burning.class));
                assertNull(game.hero(1-seat).buff(Burning.class));
            }
        }
    }

    @Test public void swiftthistleSavesTheDelayedPressAndSpendsOnlyTheOwnersBubble() {
        for (int seat=0; seat<2; seat++) {
            Bundle save;
            float clock;
            int delayed;
            try (CoopSession game=CoopSession.open(402,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.HUNTRESS);
                SewerBeeTest.offer(game,seat);
                int first=arrange(game,seat,new Swiftthistle.Seed(),false);
                delayed=game.hero(seat).pos;
                clock=Actor.now();
                move(game,first);
                SewerBeeTest.offer(game,seat);
                assertNotNull(game.hero(seat).buff(Swiftthistle.TimeBubble.class));
                assertNull(game.hero(1-seat).buff(Swiftthistle.TimeBubble.class));
                Dungeon.level.plant(new Starflower.Seed(),delayed);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,delayed,null));
                assertNotNull(Dungeon.level.plants.get(delayed));
                assertNull(game.hero(seat).buff(Bless.class));
                assertEquals(clock,Actor.now(),0f);
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                assertNotNull(Dungeon.level.plants.get(delayed));
                for (int i=0;i<12 && game.hero(seat).buff(Swiftthistle.TimeBubble.class)!=null;i++)
                    SewerSpecialActorTest.waitOne(game);
                assertNull(game.hero(seat).buff(Swiftthistle.TimeBubble.class));
                assertNull(Dungeon.level.plants.get(delayed));
                assertNotNull(game.hero(seat).buff(Bless.class));
                assertNull(game.hero(1-seat).buff(Bless.class));
                for (int i=0;i<5 && Actor.now()==clock;i++) SewerSpecialActorTest.waitOne(game);
                assertTrue(Actor.now()>clock);
            }
        }
    }
}
