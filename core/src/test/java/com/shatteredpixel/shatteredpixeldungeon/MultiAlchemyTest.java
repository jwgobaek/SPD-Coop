package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.Test;
import java.util.ArrayList;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class MultiAlchemyTest extends HeadlessDungeon {
    static int station(CoopSession game, int seat) {
        CoopMageTest.encounter(game, seat);
        int cell = game.hero(seat).pos + 1;
        Level.set(cell, Terrain.ALCHEMY);
        game.observe(seat);
        SewerBeeTest.offer(game,seat);
        return cell;
    }

    static void pick(CoopSession game, int seat, Item item) {
        Hero hero = game.hero(seat);
        Dungeon.level.drop(item, hero.pos);
        SewerBeeTest.offer(game, seat);
        assertTrue(CoopFloorsTest.step(game, PICK_UP, hero.pos, item));
        SewerBeeTest.offer(game, seat);
    }

    static boolean brew(CoopSession game, int seat, int pot, Item... ingredients) {
        return game.act(game.player(seat), game.turn(), BREW_SEEDS, pot, ingredients[0], ingredients);
    }

    static StagedInput prepare(CoopSession game, int seat) {
        int pot = station(game,seat);
        pick(game,seat,new Firebloom.Seed().quantity(3));
        return new StagedInput(game,"BREW_SEEDS",pot);
    }

    static boolean published(CoopSession game, int seat) {
        Hero owner=game.hero(seat);
        Potion bottle=owner.belongings.getItem(PotionOfLiquidFlame.class);
        return bottle != null && bottle.isKnown() && bottle.quantity()==1
                && game.ownership.owner(bottle)==game.player(seat) && owner.belongings.getItem(Firebloom.Seed.class)==null;
    }

    static StagedInput preparePaid(CoopSession game, int seat) {
        int pot=station(game,seat);
        ScrollOfUpgrade scroll=new ScrollOfUpgrade();
        scroll.identify();
        pick(game,seat,scroll);
        pick(game,seat,new EnergyCrystal().quantity(6));
        return new StagedInput(game,"BREW_ENCHANTMENT",pot);
    }

    static boolean paid(CoopSession game, int seat) {
        Hero owner=game.hero(seat);
        Item exotic=owner.belongings.getItem(ScrollOfEnchantment.class);
        return exotic!=null && game.ownership.owner(exotic)==game.player(seat)
                && owner.belongings.getItem(ScrollOfUpgrade.class)==null && Dungeon.energy==0;
    }

    @Test public void allTwelvePureRecipesProduceOneOwnedPotionAndRestoreBothSeats() {
        for (int seat = 0; seat < 2; seat++) for (var entry : Potion.SeedToPotion.types.entrySet()) {
            Bundle save;
            long id;
            int healingCount;
            try (CoopSession game = CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                int pot = station(game,seat);
                Item seed = Reflection.newInstance(entry.getKey()).quantity(3);
                pick(game,seat,seed);
                long sourceId = game.ownership.identity(seed);
                int energy = Dungeon.energy;
                assertTrue(brew(game,seat,pot,seed,seed,seed));
                Potion output = game.hero(seat).belongings.getItem(entry.getValue());
                assertNotNull(entry.getKey().getName(),output);
                assertTrue(output.isKnown());
                assertEquals(1,output.quantity());
                assertEquals(game.player(seat),game.ownership.owner(output));
                assertEquals(energy,Dungeon.energy);
                assertEquals(0,seed.quantity());
                assertFalse(game.hero(seat).belongings.contains(seed));
                id = game.ownership.identity(output);
                assertNotEquals(sourceId,id);
                healingCount = Dungeon.LimitedDrops.COOKING_HP.count;
                assertEquals(output instanceof PotionOfHealing ? 1 : 0,healingCount);
                save = game.checkpoint();
            }
            try (CoopSession game = CoopSession.restore(save)) {
                assertEquals(id,game.ownership.identity(game.hero(seat).belongings.getItem(entry.getValue())));
                assertEquals(healingCount,Dungeon.LimitedDrops.COOKING_HP.count);
                assertTrue(CoopFloorsTest.step(game,WAIT,0,null));
            }
        }
    }

    @Test public void mixedRecipesAndLimitedHealingMatchTheOriginalDrawAndNextRandom() {
        for (int mode = 0; mode < 3; mode++) for (int trial=0;trial<12;trial++) {
            Bundle start;
            long[] afterDraw;
            boolean known;
            Class<? extends Potion> type;
            int count, pot;
            Item[] selection;
            try (CoopSession game=CoopSession.open(570+trial,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                pot=station(game,0);
                Item a=new Sungrass.Seed().quantity(3), b=new Firebloom.Seed(), c=new Mageroyal.Seed();
                pick(game,0,a);
                pick(game,0,b);
                pick(game,0,c);
                selection = mode==0 ? new Item[]{a,a,a} : mode==1 ? new Item[]{a,a,b} : new Item[]{a,b,c};
                Dungeon.LimitedDrops.COOKING_HP.count=10;
                start=game.checkpoint();
                var nativeUnits=new ArrayList<Item>();
                for(Item s:selection) nativeUnits.add(s.duplicate().quantity(1));
                Potion expected=(Potion)new Potion.SeedToPotion().brew(nativeUnits);
                afterDraw=Random.worldState();
                known=expected.isKnown();
                type=expected.getClass();
                count=Dungeon.LimitedDrops.COOKING_HP.count;
            }
            try(CoopSession restored=CoopSession.restore(start)) {
                    for(int i=0;i<3;i++) selection[i]=restored.hero(0).belongings.getItem(selection[i].getClass());
                    assertTrue(brew(restored,0,pot,selection));
                    var output=restored.hero(0).belongings.getItem(type);
                    assertNotNull(output);
                    assertEquals(known,output.isKnown());
                    assertEquals(count,Dungeon.LimitedDrops.COOKING_HP.count);
                    assertArrayEquals(afterDraw,Random.worldState());
            }
        }
    }

    @Test public void equalTypesInDifferentPhysicalStacksAreDebitedIndependently() {
        try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            int pot=station(game,0);
            Item stack=new Firebloom.Seed().quantity(5);
            pick(game,0,stack);
            Item other=stack.split(2);
            game.hero(0).belongings.backpack.items.add(other);
            assertNotEquals(game.ownership.identity(stack),game.ownership.identity(other));
            assertTrue(brew(game,0,pot,stack,other,other));
            assertEquals(2,stack.quantity());
            assertEquals(0,other.quantity());
            assertFalse(game.hero(0).belongings.contains(other));
            assertEquals(1,game.hero(0).belongings.getItem(PotionOfLiquidFlame.class).quantity());
            game.checkpoint();
        }
    }

    @Test public void insufficientRepeatedHandlesWrongOwnerAndUnregisteredIngredientsAreAtomic() {
        try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            int pot=station(game,0);
            Item local=new Firebloom.Seed().quantity(2), foreign=new Icecap.Seed(), other=new Ankh();
            pick(game,0,local);
            pick(game,1,foreign);
            pick(game,0,other);
            long[] rng=Random.worldState();
            int count=Statistics.itemsCrafted;
            assertFalse(brew(game,0,pot,local,local,local));
            assertFalse(brew(game,0,pot,local,local,foreign));
            assertFalse(brew(game,0,pot,local,local,other));
            assertFalse(brew(game,0,pot,local,local,new BlandfruitBush.Seed()));
            assertFalse(brew(game,0,pot,local,local));
            assertEquals(2,local.quantity());
            assertEquals(1,foreign.quantity());
            assertEquals(count,Statistics.itemsCrafted);
            assertArrayEquals(rng,Random.worldState());
            var own=ProjectionChecks.seatImage(game,0);
            assertEquals(1,own.source.rows.keySet().stream().filter(k->k.startsWith("brewseed")).count());
            assertArrayEquals(rng,Random.worldState());
        }
    }

    @Test public void fullBagReturnsOneBoundPotionThenMergesIntoTheExistingStack() {
        try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            int pot=station(game,0);
            Item seed=new Firebloom.Seed().quantity(7);
            Hero hero=game.hero(0);
            Bag bag=hero.belongings.backpack;
            for(Item item:new ArrayList<>(bag.items)) if(item instanceof Bag) item.detachAll(bag);
            pick(game,0,seed);
            while(new Ankh().collect(bag)) {}
            assertTrue(brew(game,0,pot,seed,seed,seed));
            Item bottle=Dungeon.level.heaps.get(hero.pos).peek();
            assertTrue(bottle instanceof PotionOfLiquidFlame);
            long id=game.ownership.identity(bottle);
            assertFalse(bottle.collect(game.hero(1).belongings.backpack));
            hero.belongings.getItem(Ankh.class).detachAll(bag);
            assertTrue(CoopFloorsTest.step(game,PICK_UP,hero.pos,bottle));
            SewerBeeTest.offer(game,0);
            assertTrue(brew(game,0,pot,seed,seed,seed));
            assertEquals(1,seed.quantity());
            assertEquals(2,bottle.quantity());
            assertEquals(id,game.ownership.identity(hero.belongings.getItem(PotionOfLiquidFlame.class)));
        }
    }

    @Test public void onePlayersPaidExoticRecipeConsumesTheSharedCrystalsExactlyOnce() {
        try(CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            int pot=station(game,0);
            ScrollOfUpgrade first=new ScrollOfUpgrade(),second=new ScrollOfUpgrade();
            first.identify();
            pick(game,0,first);
            pick(game,1,second);
            assertFalse(CoopFloorsTest.step(game,BREW_ENCHANTMENT,pot,second));
            int energy=Dungeon.energy;
            pick(game,0,new EnergyCrystal().quantity(6));
            assertEquals(energy+6,Dungeon.energy);
            assertTrue(CoopFloorsTest.step(game,BREW_ENCHANTMENT,pot,first));
            assertEquals(energy,Dungeon.energy);
            SewerBeeTest.offer(game,1);
            assertFalse(CoopFloorsTest.step(game,BREW_ENCHANTMENT,pot,second));
            assertTrue(game.hero(1).belongings.contains(second));
            Item result=game.hero(0).belongings.getItem(ScrollOfEnchantment.class);
            assertNotNull(result);
            assertEquals(game.player(0),game.ownership.owner(result));
        }
    }
}
