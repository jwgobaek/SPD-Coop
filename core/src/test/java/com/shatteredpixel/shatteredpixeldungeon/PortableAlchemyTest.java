package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDeepSleep;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class PortableAlchemyTest extends HeadlessDungeon {

    static StagedInput prepare(CoopSession game, int seat, int stored) {
        return prepareRecipe(game, seat, stored, "kitbrew");
    }

    static StagedInput prepareRecipe(CoopSession game, int seat, int stored, String recipe) {
        CoopMageTest.encounter(game,seat);
        var hero = game.hero(seat);
        AlchemistsToolkit kit = new AlchemistsToolkit();
        MultiAlchemyTest.pick(game,seat,kit);
        kit.charge(hero,stored*4f);
        assertNull(CoopAlchemy.portable(game,hero));
        assertTrue(CoopFloorsTest.step(game,EQUIP,0,kit));
        assertFalse(kit.readyToBrew(hero));
        kit.upgrade(10);
        String verb;
        switch (recipe) {
            case "kitstones":
                MultiAlchemyTest.pick(game, seat, new ScrollOfLullaby());
                verb = "BREW_RUNESTONES";
                break;
            case "kitseeds":
                MultiAlchemyTest.pick(game, seat, new Firebloom.Seed().quantity(3));
                verb = "BREW_SEEDS";
                break;
            case "kitvision":
                MultiAlchemyTest.pick(game, seat, new PotionOfMindVision().identify(false));
                verb = "BREW_EXOTIC";
                break;
            case "kitbrew":
                MultiAlchemyTest.pick(game, seat, new ScrollOfUpgrade().identify());
                verb = "BREW_ENCHANTMENT";
                break;
            default:
                throw new IllegalArgumentException(recipe);
        }
        MultiAlchemyTest.pick(game,seat,new EnergyCrystal().quantity(6));
        assertTrue(kit.readyToBrew(hero));
        return new StagedInput(game,verb,CoopAlchemy.PORTABLE);
    }

    static boolean published(CoopSession game, int seat) {
        var hero = game.hero(seat);
        var result = hero.belongings.getItem(ScrollOfEnchantment.class);
        return result != null && game.ownership.owner(result)==game.player(seat)
                && hero.belongings.getItem(ScrollOfUpgrade.class)==null
                && CoopAlchemy.portable(game,hero).availableEnergy()==0 && Dungeon.energy==2;
    }

    static boolean recipePublished(CoopSession game, int seat, String recipe) {
        var bag = game.hero(seat).belongings;
        var kit = CoopAlchemy.portable(game, game.hero(seat));
        if (kit == null) return false;
        if (recipe.equals("kitvision")) {
            var result = bag.getItem(PotionOfMagicalSight.class);
            return result != null && game.ownership.owner(result) == game.player(seat)
                    && bag.getItem(PotionOfMindVision.class) == null && kit.availableEnergy() == 0 && Dungeon.energy == 4;
        }
        if (kit.availableEnergy() != 2 || Dungeon.energy != 6) return false;
        if (recipe.equals("kitstones")) {
            var result = bag.getItem(StoneOfDeepSleep.class);
            return result != null && result.quantity() == 2 && bag.getItem(ScrollOfLullaby.class) == null
                    && game.ownership.owner(result) == game.player(seat);
        }
        var result = bag.getItem(PotionOfLiquidFlame.class);
        return result != null && result.isKnown() && result.quantity() == 1 && bag.getItem(Firebloom.Seed.class) == null
                && game.ownership.owner(result) == game.player(seat);
    }

    @Test public void portableRecipesShareIngredientsAndUseOnlyTheOwnersTool() throws Exception {
        for (int seat=0; seat<2; seat++) {
            Bundle saved;
            long resultId;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                prepare(game,seat,2);
                var hero=game.hero(seat);
                var kit=CoopAlchemy.portable(game,hero);
                int station=CoopAlchemy.PORTABLE;
                var scroll=new ScrollOfLullaby();
                MultiAlchemyTest.pick(game,seat,scroll);
                assertTrue(CoopFloorsTest.step(game,BREW_RUNESTONES,station,scroll));
                var stones=hero.belongings.getItem(StoneOfDeepSleep.class);
                assertEquals(2,stones.quantity());
                assertEquals(game.player(seat),game.ownership.owner(stones));
                var seed=new Firebloom.Seed();
                seed.quantity(3);
                MultiAlchemyTest.pick(game,seat,seed);
                assertTrue(MultiAlchemyTest.brew(game,seat,station,seed,seed,seed));
                var potion=hero.belongings.getItem(PotionOfLiquidFlame.class);
                assertTrue(potion.isKnown());
                assertEquals(1,potion.quantity());
                assertEquals(6,Dungeon.energy);
                assertEquals(2,kit.availableEnergy());
                var ingredient=new PotionOfMindVision();
                ingredient.identify(false);
                MultiAlchemyTest.pick(game,seat,ingredient);
                assertTrue(CoopFloorsTest.step(game,BREW_EXOTIC,station,ingredient));
                assertEquals(0,kit.availableEnergy());
                assertEquals(4,Dungeon.energy);
                var output=hero.belongings.getItem(PotionOfMagicalSight.class);
                assertEquals(game.player(seat),game.ownership.owner(output));
                assertNull(game.hero(1-seat).belongings.getItem(PotionOfMagicalSight.class));
                resultId=game.ownership.identity(output);
                saved=game.checkpoint();
            }
            try (CoopSession restored=CoopSession.restore(saved)) {
                assertEquals(resultId,restored.ownership.identity(restored.hero(seat).belongings.getItem(PotionOfMagicalSight.class)));
                assertEquals(4,Dungeon.energy);
                assertTrue(CoopFloorsTest.step(restored,WAIT,0,null));
            }
        }
    }

    @Test public void twoWornKitsRecheckTheSharedBalanceAndKeepBothPhysicalCharges() {
        for (int seat=0; seat<2; seat++) {
            Bundle saved;
            long ownerId,peerId;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                prepare(game,seat,2);
                var owner=game.hero(seat);
                var peer=game.hero(1-seat);
                var first=CoopAlchemy.portable(game,owner);
                var second=new AlchemistsToolkit();
                MultiAlchemyTest.pick(game,1-seat,second);
                second.charge(peer,12f);
                assertTrue(CoopFloorsTest.step(game,EQUIP,0,second));
                second.upgrade(10);
                var ingredient=new ScrollOfUpgrade();
                ingredient.identify(false);
                MultiAlchemyTest.pick(game,1-seat,ingredient);
                ownerId=game.ownership.identity(first);
                peerId=game.ownership.identity(second);
                assertNotEquals(ownerId,peerId);
                assertEquals(3,second.availableEnergy());
                assertEquals(6,Dungeon.energy);
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,BREW_ENCHANTMENT,CoopAlchemy.PORTABLE,
                        owner.belongings.getItem(ScrollOfUpgrade.class)));
                assertEquals(0,first.availableEnergy());
                assertEquals(2,Dungeon.energy);
                SewerBeeTest.offer(game,1-seat);
                assertFalse(CoopFloorsTest.step(game,BREW_ENCHANTMENT,CoopAlchemy.PORTABLE,ingredient));
                assertSame(ingredient,peer.belongings.getItem(ScrollOfUpgrade.class));
                assertEquals(3,second.availableEnergy());
                assertEquals(2,Dungeon.energy);
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                var first=CoopAlchemy.portable(game,game.hero(seat));
                var second=CoopAlchemy.portable(game,game.hero(1-seat));
                assertEquals(ownerId,game.ownership.identity(first));
                assertEquals(peerId,game.ownership.identity(second));
                assertEquals(0,first.availableEnergy());
                assertEquals(3,second.availableEnergy());
                assertEquals(2,Dungeon.energy);
                assertNotNull(game.hero(seat).belongings.getItem(ScrollOfEnchantment.class));
                assertNull(game.hero(1-seat).belongings.getItem(ScrollOfEnchantment.class));
            }
        }
    }

    @Test public void originalCostsConsumeOnlyOwnersEquippedToolkitAndRestore() throws Exception {
        for (int seat=0;seat<2;seat++) for (int stored:new int[]{0,2,6,9}) {
            Bundle saved;
            long output;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                prepare(game,seat,stored);
                var hero=game.hero(seat);
                var kit=CoopAlchemy.portable(game,hero);
                var scroll=hero.belongings.getItem(ScrollOfUpgrade.class);
                assertNull(CoopAlchemy.portable(game,game.hero(1-seat)));
                var view=ProjectionChecks.decodedSeat(game,seat);
                assertTrue(view.belongings.stream().anyMatch(i->view.source.rows.containsKey("kitenergy"+i.id)));
                assertFalse(ProjectionChecks.decodedSeat(game,1-seat).source.rows.keySet().stream().anyMatch(k->k.startsWith("kitenergy")));
                kit.cursed=true;
                assertFalse(CoopFloorsTest.step(game,BREW_ENCHANTMENT,CoopAlchemy.PORTABLE,scroll));
                kit.cursed=false;
                Buff.affect(hero,MagicImmune.class);
                assertFalse(CoopFloorsTest.step(game,BREW_ENCHANTMENT,CoopAlchemy.PORTABLE,scroll));
                Buff.detach(hero,MagicImmune.class);
                if (stored < 6) {
                    Dungeon.energy=5-stored;
                    assertFalse(CoopFloorsTest.step(game,BREW_ENCHANTMENT,CoopAlchemy.PORTABLE,scroll));
                    assertEquals(stored,kit.availableEnergy());
                    assertTrue(hero.belongings.contains(scroll));
                    Dungeon.energy=6;
                }
                assertTrue(CoopFloorsTest.step(game,BREW_ENCHANTMENT,CoopAlchemy.PORTABLE,scroll));
                assertEquals(Math.max(0,stored-6),kit.availableEnergy());
                assertEquals(Math.min(6,stored),Dungeon.energy);
                assertFalse(hero.belongings.contains(scroll));
                var exotic=hero.belongings.getItem(ScrollOfEnchantment.class);
                assertEquals(game.player(seat),game.ownership.owner(exotic));
                output=game.ownership.identity(exotic);
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                assertEquals(output,game.ownership.identity(game.hero(seat).belongings.getItem(ScrollOfEnchantment.class)));
                assertEquals(Math.max(0,stored-6),CoopAlchemy.portable(game,game.hero(seat)).availableEnergy());
                assertTrue(CoopFloorsTest.step(game,WAIT,0,null));
            }
        }
    }
}
