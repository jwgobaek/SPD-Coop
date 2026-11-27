package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class BrewedItemsTest extends HeadlessDungeon {
    @Test public void craftedHasteAndSleepStoneKeepTheNativeRecipientAndConsumeOneUnit() {
        for(int seat=0;seat<2;seat++) try(CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            int pot=MultiAlchemyTest.station(game,seat);
            Hero hero=game.hero(seat);
            Item seeds=new Swiftthistle.Seed().quantity(3);
            MultiAlchemyTest.pick(game,seat,seeds);
            assertTrue(MultiAlchemyTest.brew(game,seat,pot,seeds,seeds,seeds));
            Potion potion=hero.belongings.getItem(PotionOfHaste.class);
            assertTrue(CoopFloorsTest.step(game,DRINK,0,potion));
            assertNotNull(hero.buff(Haste.class));
            assertNull(game.hero(1-seat).buff(Haste.class));
            ScrollOfLullaby scroll=new ScrollOfLullaby();
            MultiAlchemyTest.pick(game,seat,scroll);
            assertTrue(CoopFloorsTest.step(game,BREW_RUNESTONES,pot,scroll));
            StoneOfDeepSleep stones=hero.belongings.getItem(StoneOfDeepSleep.class);
            long id=game.ownership.identity(stones);
            Rat rat=(Rat)Dungeon.level.mobs.stream().filter(m->m instanceof Rat).findFirst().get();
            rat.pos=hero.pos-2;
            game.observe(seat);
            assertTrue(CoopFloorsTest.step(game,THROW,rat.pos,stones));
            assertNotNull(rat.buff(MagicalSleep.class));
            assertEquals(1,stones.quantity());
            assertEquals(id,game.ownership.identity(stones));
            assertEquals(game.player(seat),game.ownership.owner(stones));
        }
    }

    @Test public void unknownTerrorAndRestoredInscriptionUseOnlyTheReadersFieldOfView() {
        for(int seat=0;seat<2;seat++) {
            Bundle save;
            int resident;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Hero hero=game.hero(seat);
                Rat rat=CoopMageTest.encounter(game,seat);
                resident=rat.id();
                SewerBeeTest.offer(game,seat);
                hero.lvl=15;
                assertTrue(CoopFloorsTest.step(game,INVEST_TALENT,Talent.RECALL_INSCRIPTION.ordinal(),null));
                ScrollOfTerror scroll=new ScrollOfTerror();
                MultiAlchemyTest.pick(game,seat,scroll);
                assertFalse(scroll.isKnown());
                var own=ProjectionChecks.seatImage(game,seat);
                assertFalse(own.source.rows.containsValue("items.scrolls.scrollofterror"));
                assertTrue(CoopFloorsTest.step(game,READ_SCROLL,0,scroll));
                assertTrue(scroll.isKnown());
                assertEquals(hero.id(),rat.buff(Terror.class).object);
                assertEquals(ScrollOfTerror.class,hero.buff(RecallInscription.UsedItemTracker.class).item);
                assertNull(game.hero(1-seat).buff(RecallInscription.UsedItemTracker.class));
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                Hero reader=game.hero(seat);
                SewerBeeTest.offer(game,seat);
                HolyTome tome=(HolyTome)reader.belongings.artifact;
                int before=tome.charges();
                Rat rat=(Rat)Actor.findById(resident);
                Buff.detach(rat,Terror.class);
                assertTrue(CoopFloorsTest.step(game,RECALL_INSCRIPTION,reader.pos,tome));
                assertEquals(reader.id(),rat.buff(Terror.class).object);
                assertEquals(before-3,tome.charges());
                assertNull(reader.buff(RecallInscription.UsedItemTracker.class));
                assertNull(reader.belongings.getItem(ScrollOfTerror.class));
            }
        }
    }

    @Test public void ownTransfusionMemoryWithoutTheReferenceCastsTheNativeCharm() {
        for(int seat=0;seat<2;seat++) {
            Bundle save;
            int enemy;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Rat rat=CoopMageTest.encounter(game,seat);
                enemy=rat.id();
                Hero hero=game.hero(seat);
                hero.lvl=30;
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,USE_CROWN,1,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,INVEST_TALENT,Talent.MIND_FORM.ordinal(),null));
                WandOfTransfusion wand=WandArtsTest.give(game,seat,new WandOfTransfusion());
                StagedInput memory=SealArtsTest.historyInput(game,seat,wand,TrinityDiscoveries.Choice.TRANSFUSION);
                assertTrue(CoopFloorsTest.step(game,MIND_FORM,memory.cell,hero.belongings.artifact));
                assertNull(hero.belongings.getItem(WandOfTransfusion.class));
                assertFalse(game.discoveries(1-seat).knows(TrinityDiscoveries.Choice.TRANSFUSION));
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                Rat rat=(Rat)Actor.findById(enemy);
                SewerBeeTest.offer(game,seat);
                ClassArmor armor=(ClassArmor)hero.belongings.armor();
                float before=armor.charge;
                assertTrue(CoopFloorsTest.step(game,TRINITY_MIND,rat.pos,armor));
                assertEquals(hero.id(),rat.buff(Charm.class).object);
                assertTrue(armor.charge<before);
            }
        }
    }
}
