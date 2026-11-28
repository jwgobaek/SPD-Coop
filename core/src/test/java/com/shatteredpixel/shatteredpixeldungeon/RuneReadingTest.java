package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDetectMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition.IntuitionUseTracker;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class RuneReadingTest extends HeadlessDungeon {
    static int selection(Hero hero, Item target) {
        int i = 0;
        for (Item item : hero.belongings) {
            i++;
            if (item == target) return i;
        }
        throw new IllegalStateException("Missing test item");
    }

    static StagedInput prepare(CoopSession game, int seat, String stage) {
        Hero hero=game.hero(seat);
        if (stage.startsWith("detectstone")) {
            Armor armor=hero.belongings.armor();
            armor.level(3);
            armor.cursed = true;
            armor.levelKnown = armor.cursedKnown = false;
            MultiAlchemyTest.pick(game,seat,new StoneOfDetectMagic().quantity(2));
            return new StagedInput(game,"DETECT_MAGIC",selection(hero,armor));
        }
        Item ring=new RingOfHaste();
        MultiAlchemyTest.pick(game,seat,ring);
        MultiAlchemyTest.pick(game,seat,new StoneOfIntuition());
        int correct=CoopRuneStones.guesses(ring).indexOf(ring.getClass());
        return new StagedInput(game,"GUESS_ITEM",selection(hero,ring)*CoopRuneStones.OPTIONS_PER_ITEM+correct);
    }

    static boolean published(CoopSession game, int seat, String stage) {
        Hero hero=game.hero(seat);
        String reading=game.runeReading(seat);
        if (reading==null || game.runeReading(1-seat)!=null) return false;
        if (stage.startsWith("detectstone")) {
            Armor armor=hero.belongings.armor();
            StoneOfDetectMagic stone=hero.belongings.getItem(StoneOfDetectMagic.class);
            return reading.endsWith(",BOTH") && armor.cursedKnown && !armor.levelKnown
                    && stone!=null && stone.quantity()==1;
        }
        RingOfHaste ring=hero.belongings.getItem(RingOfHaste.class);
        return reading.endsWith(",CORRECT") && ring!=null && ring.isKnown() && !ring.levelKnown
                && hero.buff(IntuitionUseTracker.class)!=null && hero.belongings.getItem(StoneOfIntuition.class)!=null;
    }

    @Test public void detectionKeepsFourFindingsAndExactEquipmentPrivate() throws Exception {
        String[] results = {"NONE","GOOD","BAD","BOTH"};
        for (int seat = 0; seat < 2; seat++) for (int n = 0; n < 4; n++) {
            Bundle save;
            long id;
            try (CoopSession game = CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Hero hero = game.hero(seat);
                Armor armor = hero.belongings.armor();
                armor.level(n%2 == 0 ? 0 : 3);
                armor.cursed = n>=2;
                armor.levelKnown = armor.cursedKnown = false;
                StoneOfDetectMagic stone = new StoneOfDetectMagic();
                MultiAlchemyTest.pick(game,seat,stone.quantity(2));
                id = game.ownership.identity(armor);
                assertTrue(CoopFloorsTest.step(game,DETECT_MAGIC,selection(hero,armor),stone));
                assertEquals(1,stone.quantity());
                assertTrue(armor.cursedKnown);
                assertFalse(armor.levelKnown);
                String reading = game.runeReading(seat);
                assertTrue(reading,reading.endsWith(","+results[n]));
                assertEquals(reading,ProjectionChecks.decodedSeat(game,seat).source.rows.get("runereading"));
                assertFalse(ProjectionChecks.decodedSeat(game,1-seat).source.rows.containsKey("runereading"));
                save = game.checkpoint();
                assertEquals(28,save.getInt("schema"));
            }
            try (CoopSession game = CoopSession.restore(save)) {
                assertTrue(game.runeReading(seat).endsWith(","+results[n]));
                assertNull(game.runeReading(1-seat));
                assertEquals(id,game.ownership.identity(game.hero(seat).belongings.armor()));
                assertFalse(game.hero(seat).belongings.armor().levelKnown);
            }
        }
    }

    @Test public void firstGuessAndSecondGuessPersistOnTheirOwner() throws Exception {
        for (int seat=0; seat<2; seat++) for (boolean firstCorrect : new boolean[]{false,true}) {
            Bundle save;
            try (CoopSession game = CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Hero hero = game.hero(seat);
                StoneOfIntuition stone = new StoneOfIntuition();
                RingOfHaste ring = new RingOfHaste();
                ring.level(3);
                MultiAlchemyTest.pick(game,seat,ring);
                MultiAlchemyTest.pick(game,seat,stone);
                var choices = CoopRuneStones.guesses(ring);
                int correct = choices.indexOf(ring.getClass());
                assertTrue(correct>=0);
                var image=ProjectionChecks.decodedSeat(game,seat).source;
                assertTrue(image.rows.containsValue("items.rings.ringofhaste"));
                assertFalse(ProjectionChecks.decodedSeat(game,1-seat).source.rows.containsValue("items.rings.ringofhaste"));
                int choice=firstCorrect ? correct : (correct+1)%choices.size();
                assertTrue(CoopFloorsTest.step(game,GUESS_ITEM,selection(hero,ring)*32+choice,stone));
                assertEquals(firstCorrect,ring.isKnown());
                assertFalse(ring.levelKnown);
                assertNotNull(hero.buff(IntuitionUseTracker.class));
                assertNull(game.hero(1-seat).buff(IntuitionUseTracker.class));
                assertTrue(hero.belongings.backpack.contains(stone));
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                assertNotNull(hero.buff(IntuitionUseTracker.class));
                var potion=new PotionOfFrost();
                MultiAlchemyTest.pick(game,seat,potion);
                var choices=CoopRuneStones.guesses(potion);
                int correct=choices.indexOf(potion.getClass());
                assertTrue(correct>=0);
                int choice=firstCorrect?(correct+1)%choices.size():correct;
                var stone=hero.belongings.getItem(StoneOfIntuition.class);
                assertTrue(CoopFloorsTest.step(game,GUESS_ITEM,selection(hero,potion)*32+choice,stone));
                assertEquals(!firstCorrect,potion.isKnown());
                assertNull(hero.buff(IntuitionUseTracker.class));
                assertNull(hero.belongings.getItem(StoneOfIntuition.class));
                assertTrue(game.runeReading(seat).endsWith(firstCorrect?",INCORRECT":",CORRECT"));
            }
        }
    }

    @Test public void exoticFamiliesOfferOnlyTheirOriginalUnknownTypes() {
        try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            for (Item item : new Item[]{new PotionOfMagicalSight(),new ScrollOfDread()}) {
                var choices=CoopRuneStones.guesses(item);
                assertTrue(choices.contains(item.getClass()));
                assertTrue(choices.size()<=CoopRuneStones.OPTIONS_PER_ITEM);
                for (Class<?> type : choices) assertTrue(type.getName(),type.getName().contains(".exotic."));
            }
        }
    }
}
