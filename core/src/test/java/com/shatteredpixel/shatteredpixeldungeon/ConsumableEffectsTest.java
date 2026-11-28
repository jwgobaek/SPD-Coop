package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class ConsumableEffectsTest extends HeadlessDungeon {
    static StagedInput prepare(CoopSession game, int seat, String stage) {
        Hero hero = game.hero(seat);
        if (stage.startsWith("exoticvision") || stage.startsWith("exoticterror")) {
            int station = MultiAlchemyTest.station(game, seat);
            boolean vision = stage.startsWith("exoticvision");
            com.shatteredpixel.shatteredpixeldungeon.items.Item ingredient =
                    vision ? new PotionOfMindVision() : new ScrollOfTerror();
            ingredient.identify(false);
            MultiAlchemyTest.pick(game, seat, ingredient);
            Dungeon.energy = vision ? 4 : 6;
            SewerBeeTest.offer(game, seat);
            return new StagedInput(game, "BREW_EXOTIC", station);
        }
        if (stage.startsWith("wornaugment")) {
            int station = MultiAlchemyTest.station(game, seat);
            Scroll scroll = new ScrollOfTransmutation();
            MultiAlchemyTest.pick(game, seat, scroll);
            assertTrue(CoopFloorsTest.step(game, BREW_RUNESTONES, station, scroll));
            int selection = 0;
            for (var gear : hero.belongings) {
                selection++;
                if (gear == hero.belongings.armor()) break;
            }
            SewerBeeTest.offer(game, seat);
            return new StagedInput(game, "AUGMENT_GEAR", selection * 3);
        }
        if (stage.startsWith("visiondrink")) {
            Potion potion = new PotionOfMindVision();
            potion.identify(false);
            MultiAlchemyTest.pick(game, seat, potion);
            SewerBeeTest.offer(game, seat);
            return new StagedInput(game, "DRINK", 0);
        }
        if (stage.startsWith("mapread") || stage.startsWith("maprecall")) {
            hero.lvl = 15;
            SewerBeeTest.offer(game, seat);
            assertTrue(CoopFloorsTest.step(game, INVEST_TALENT, Talent.RECALL_INSCRIPTION.ordinal(), null));
            ScrollOfMagicMapping scroll = new ScrollOfMagicMapping();
            MultiAlchemyTest.pick(game, seat, scroll);
            SewerBeeTest.offer(game, seat);
            if (stage.startsWith("mapread")) return new StagedInput(game, "READ_SCROLL", 0);
            assertTrue(CoopFloorsTest.step(game, READ_SCROLL, 0, scroll));
            SewerBeeTest.offer(game, seat);
            return new StagedInput(game, "RECALL_INSCRIPTION", hero.pos);
        }
        Rat rat = CoopMageTest.encounter(game, seat);
        if (stage.startsWith("fearstone")) {
            MultiAlchemyTest.pick(game, seat, new StoneOfFear().quantity(2));
            SewerBeeTest.offer(game, seat);
            return new StagedInput(game, "THROW", rat.pos);
        }
        WandArtsTest.give(game, seat, new WandOfCorruption());
        rat.HP = 1;
        SewerBeeTest.offer(game, seat);
        return new StagedInput(game, "ZAP", rat.pos);
    }

    static boolean published(CoopSession game, int seat, String stage) {
        Hero owner = game.hero(seat);
        if (stage.startsWith("exoticvision") || stage.startsWith("exoticterror")) {
            boolean vision = stage.startsWith("exoticvision");
            var result = vision
                    ? owner.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight.class)
                    : owner.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread.class);
            return Dungeon.energy == 0 && result != null && result.quantity() == 1
                    && game.ownership.owner(result) == game.player(seat)
                    && (vision ? owner.belongings.getItem(PotionOfMindVision.class) == null
                               : owner.belongings.getItem(ScrollOfTerror.class) == null);
        }
        if (stage.startsWith("wornaugment")) {
            StoneOfAugmentation remaining = owner.belongings.getItem(StoneOfAugmentation.class);
            return CoopConsumables.augment(owner.belongings.armor()) == 0
                    && CoopConsumables.augment(game.hero(1-seat).belongings.armor()) != 0
                    && remaining != null && remaining.quantity() == 1
                    && game.ownership.owner(remaining) == game.player(seat);
        }
        if (stage.startsWith("visiondrink"))
            return owner.buff(MindVision.class) != null && game.hero(1-seat).buff(MindVision.class) == null
                    && owner.belongings.getItem(PotionOfMindVision.class) == null;
        if (stage.startsWith("fearstone")) {
            StoneOfFear stack = owner.belongings.getItem(StoneOfFear.class);
            return stack != null && stack.quantity() == 1 && game.ownership.owner(stack) == game.player(seat)
                    && Dungeon.level.mobs.stream().anyMatch(m -> m.buff(Terror.class) != null
                            && m.buff(Terror.class).object == owner.id());
        }
        if (stage.startsWith("map")) {
            boolean exclusive = false;
            for (int cell = 0; cell < Dungeon.level.length(); cell++) {
                exclusive |= game.knowledge(owner).mapped(cell) && !game.knowledge(game.hero(1-seat)).mapped(cell);
            }
            RecallInscription.UsedItemTracker imprint = owner.buff(RecallInscription.UsedItemTracker.class);
            return exclusive && owner.belongings.getItem(ScrollOfMagicMapping.class) == null
                    && (stage.startsWith("mapread") ? imprint != null && imprint.item == ScrollOfMagicMapping.class
                    : imprint == null);
        }
        WandOfCorruption wand = owner.belongings.getItem(WandOfCorruption.class);
        return wand != null && wand.curCharges == wand.maxCharges - 1 && Dungeon.level.mobs.stream()
                .anyMatch(m -> m.alignment == Char.Alignment.ALLY && m.buff(Corruption.class) != null
                        && m.buff(Corruption.class).casterId() == owner.id());
    }

    @Test public void magicMappingAndRecallPreserveOnlyTheReadersExploration() {
        for (int seat : new int[]{0, 1}) {
            Bundle saved;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
                game.join(22, HeroClass.CLERIC);
                prepare(game, seat, "maprecall");
                assertTrue(published(game, seat, "mapread"));
                saved = game.checkpoint();
            }
            try (CoopSession game = CoopSession.restore(saved)) {
                Hero hero = game.hero(seat);
                HolyTome tome = hero.belongings.getItem(HolyTome.class);
                int initial = tome.charges();
                assertTrue(CoopFloorsTest.step(game, RECALL_INSCRIPTION, hero.pos, tome));
                assertEquals(initial - 3, tome.charges());
                assertTrue(published(game, seat, "maprecall"));
                assertFalse(CoopFloorsTest.step(game, RECALL_INSCRIPTION, hero.pos, tome));
            }
        }
    }

    @Test public void blastCreditsOnlyItsThrowerAndFlockRestoresDistinctSheep() {
        for (int seat : new int[]{0,1}) for (boolean blast : new boolean[]{true,false}) {
            Bundle save;
            java.util.Set<Integer> sheepIds = new java.util.TreeSet<>();
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero owner=game.hero(seat), peer=game.hero(1-seat);
                rat.HP=1;
                Runestone stone=blast ? new StoneOfBlast() : new StoneOfFlock();
                MultiAlchemyTest.pick(game,seat,stone);
                SewerBeeTest.offer(game,seat);
                if (blast) assertFalse(CoopFloorsTest.step(game,THROW,peer.pos,stone));
                int xp=owner.exp, other=peer.exp;
                assertTrue(CoopFloorsTest.step(game,THROW,rat.pos,stone));
                if (blast) {
                    assertFalse(rat.isAlive());
                    assertEquals(xp+rat.EXP,owner.exp);
                    assertEquals(other,peer.exp);
                } else {
                    for (var mob : Dungeon.level.mobs) if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep) {
                        assertTrue(sheepIds.add(mob.id()));
                        assertNotEquals(owner.pos,mob.pos);
                        assertNotEquals(peer.pos,mob.pos);
                    }
                    assertFalse(sheepIds.isEmpty());
                }
                save=game.checkpoint();
            }
            try (CoopSession restored=CoopSession.restore(save)) {
                for (int id:sheepIds) assertTrue(Actor.findById(id) instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep);
                if (blast) assertNull(restored.hero(seat).belongings.getItem(StoneOfBlast.class));
                else assertNull(restored.hero(seat).belongings.getItem(StoneOfFlock.class));
            }
        }
    }

    @Test public void brewedAugmentationSelectsOwnWornGearAndRestoresItsIdentity() {
        for (int seat : new int[]{0,1}) {
            Bundle save;
            long id;
            try (CoopSession game = CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22, HeroClass.WARRIOR);
                int cell = MultiAlchemyTest.station(game,seat);
                Hero owner = game.hero(seat);
                Scroll scroll = new ScrollOfTransmutation();
                MultiAlchemyTest.pick(game,seat,scroll);
                assertTrue(CoopFloorsTest.step(game,BREW_RUNESTONES,cell,scroll));
                StoneOfAugmentation stones = owner.belongings.getItem(StoneOfAugmentation.class);
                id = game.ownership.identity(owner.belongings.armor());
                int index = 0;
                for (var gear : owner.belongings) { ++index; if (gear == owner.belongings.armor()) break; }
                int current = CoopConsumables.augment(owner.belongings.armor());
                assertFalse(CoopFloorsTest.step(game,AUGMENT_GEAR,index*3+current,stones));
                assertTrue(CoopFloorsTest.step(game,AUGMENT_GEAR,index*3,stones));
                assertEquals(1,stones.quantity());
                assertEquals(0,CoopConsumables.augment(owner.belongings.armor()));
                assertEquals(current,CoopConsumables.augment(game.hero(1-seat).belongings.armor()));
                save=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(save)) {
                assertEquals(id,game.ownership.identity(game.hero(seat).belongings.armor()));
                assertEquals(0,CoopConsumables.augment(game.hero(seat).belongings.armor()));
            }
        }
    }

    @Test public void enchantmentStoneKeepsTheOriginalPoolAndTheSelectedPackIdentity() {
        for (int seat : new int[]{0,1}) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                var sword = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword();
                MultiAlchemyTest.pick(game,seat,sword);
                StoneOfEnchantment stone=new StoneOfEnchantment();
                MultiAlchemyTest.pick(game,seat,stone);
                var hero=game.hero(seat);
                long id=game.ownership.identity(sword);
                int index=0;
                for (var gear:hero.belongings) { ++index; if (gear==sword) break; }
                assertTrue(CoopFloorsTest.step(game,ENCHANT_BOW,index,stone));
                assertNotNull(sword.enchantment);
                assertEquals(id,game.ownership.identity(sword));
                assertFalse(hero.belongings.contains(stone));
            }
        }
    }

    @Test public void knownExoticRecipesKeepOriginalCostAndOwnedOutput() {
        for (int seat : new int[]{0,1}) for (boolean scroll : new boolean[]{false,true}) {
            try (CoopSession game = CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                int station = MultiAlchemyTest.station(game, seat);
                com.shatteredpixel.shatteredpixeldungeon.items.Item input =
                        scroll ? new ScrollOfTerror() : new PotionOfMindVision();
                assertNull(CoopAlchemy.exoticPreview(input));
                MultiAlchemyTest.pick(game,seat,input);
                input.identify(false);
                long[] before = com.watabou.utils.Random.worldState();
                int cost = scroll ? 6 : 4;
                String preview = CoopAlchemy.exoticPreview(input);
                assertNotNull(preview);
                assertTrue(preview.endsWith("," + cost));
                assertArrayEquals(before, com.watabou.utils.Random.worldState());
                Dungeon.energy = cost - 1;
                assertFalse(CoopFloorsTest.step(game,BREW_EXOTIC,station,input));
                assertEquals(1,input.quantity());
                Dungeon.energy++;
                assertTrue(CoopFloorsTest.step(game,BREW_EXOTIC,station,input));
                assertEquals(0,Dungeon.energy);
                assertFalse(game.hero(seat).belongings.contains(input));
                Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.Item> type = scroll
                        ? com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread.class
                        : com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight.class;
                var result = game.hero(seat).belongings.getItem(type);
                assertNotNull(result);
                assertEquals(game.player(seat), game.ownership.owner(result));
                assertEquals(1,result.quantity());
            }
        }
    }

    @Test public void thrownPurityClearsNativeGasWithoutDrinkingOrAffectingThePeer() {
        for (int seat : new int[]{0,1}) try (CoopSession game = CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            Rat rat = CoopMageTest.encounter(game,seat);
            PotionOfPurity potion = new PotionOfPurity();
            potion.identify(false);
            MultiAlchemyTest.pick(game,seat,potion);
            var gas = com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.seed(rat.pos,100,
                    com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class);
            com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(gas);
            SewerBeeTest.offer(game,seat);
            assertFalse(CoopFloorsTest.step(game,THROW,game.hero(1-seat).pos,potion));
            assertTrue(CoopFloorsTest.step(game,THROW,rat.pos,potion));
            assertEquals(0,gas.cur[rat.pos]);
            assertFalse(game.hero(seat).belongings.contains(potion));
            assertNull(game.hero(seat).buff(BlobImmunity.class));
        }
    }

    @Test public void unknownDrinkingUsesOnlySharedKnowledgeUntilTheCommittedSip() throws Exception {
        for(int seat=0;seat<2;seat++) for(Potion sample : new Potion[]{new PotionOfHealing(),new PotionOfHaste(),
                new PotionOfInvisibility(),new PotionOfLevitation(),new PotionOfMindVision(),new PotionOfPurity(),
                new PotionOfStrength(),new PotionOfExperience()}) {
            try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Hero owner=game.hero(seat);
                CoopMageTest.encounter(game,seat);
                // Instantiate after the session initializes the original appearance and identity tables.
                Potion bottle=com.watabou.utils.Reflection.newInstance(sample.getClass());
                assertFalse(bottle.isKnown());
                MultiAlchemyTest.pick(game,seat,bottle.quantity(2));
                int before=bottle.quantity();
                assertFalse(CoopConsumables.drinkable(bottle));
                assertFalse(CoopFloorsTest.step(game,DRINK,0,bottle));
                assertEquals(before,bottle.quantity());
                for(Potion known : new Potion[]{new PotionOfLiquidFlame(),new PotionOfFrost(),new PotionOfToxicGas()})
                    known.identify(false);
                assertFalse(CoopConsumables.drinkable(bottle));
                new PotionOfParalyticGas().identify(false);
                assertTrue(CoopConsumables.drinkable(bottle));
                assertFalse(CoopConsumables.throwablePotion(bottle));
                assertEquals("",CoopTrade.label(bottle));
                var view=ProjectionChecks.decodedSeat(game,seat);
                var visible=view.belongings.stream().filter(it->"POTION".equals(it.kind)).findFirst().get();
                assertTrue(view.actions(visible).contains("DRINK"));
                assertFalse(view.actions(visible).contains("THROW"));
                assertFalse(view.source.rows.containsKey("label"+visible.id));
                int strength=owner.STR, level=owner.lvl;
                owner.HP=owner.HT/2;
                assertTrue(CoopFloorsTest.step(game,DRINK,0,bottle));
                assertEquals(1,bottle.quantity());
                assertTrue(bottle.isKnown());
                assertEquals(game.player(seat),game.ownership.owner(bottle));
                if(bottle instanceof PotionOfStrength) assertEquals(strength+1,owner.STR);
                if(bottle instanceof PotionOfExperience) assertTrue(owner.lvl>level);
                assertNotNull(game.checkpoint());
            }
        }
    }

    @Test public void brewedStonesUseTheThrowersPhysicalStackAndNativeEffect() {
        for (int seat : new int[]{0, 1}) for (Scroll scroll : new Scroll[]{new ScrollOfTeleportation(),
                new ScrollOfTerror(), new ScrollOfRecharging(), new ScrollOfMagicMapping(), new ScrollOfRage()}) {
            long id;
            Bundle state;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
                game.join(22, HeroClass.WARRIOR);
                int pot = MultiAlchemyTest.station(game, seat);
                Hero h = game.hero(seat);
                MultiAlchemyTest.pick(game, seat, scroll);
                assertTrue(CoopFloorsTest.step(game, BREW_RUNESTONES, pot, scroll));
                Runestone stone = h.belongings.getItem(Runestone.class);
                assertNotNull(stone);
                id = game.ownership.identity(stone);
                Rat rat = (Rat)Dungeon.level.mobs.iterator().next();
                rat.pos = h.pos-3;
                game.observe(seat);
                SewerBeeTest.offer(game, seat);
                int before = h.pos;
                if (stone instanceof StoneOfShock)
                    assertFalse(CoopFloorsTest.step(game, THROW, game.hero(1-seat).pos, stone));
                assertTrue(stone.getClass().getSimpleName(), CoopFloorsTest.step(game, THROW, rat.pos, stone));
                if (stone instanceof StoneOfBlink) assertEquals(before-2, h.pos);
                if (stone instanceof StoneOfFear) assertEquals(h.id(), rat.buff(Terror.class).object);
                if (stone instanceof StoneOfAggression) assertNotNull(rat.buff(StoneOfAggression.Aggression.class));
                if (stone instanceof StoneOfShock) assertNotNull(rat.buff(Paralysis.class));
                if (stone instanceof StoneOfClairvoyance) {
                    int unique = 0;
                    for (int c = 0; c < Dungeon.level.length(); c++)
                        if (game.knowledge(h).terrain(c) >= 0 && game.knowledge(game.hero(1-seat)).terrain(c) < 0) unique++;
                    assertTrue(unique > 0);
                }
                assertEquals(1, stone.quantity());
                assertEquals(game.player(seat), game.ownership.owner(stone));
                state = game.checkpoint();
            }
            try (CoopSession restored = CoopSession.restore(state)) {
                    assertEquals(id, restored.ownership.identity(restored.hero(seat).belongings.getItem(Runestone.class)));
            }
        }
    }

    @Test public void sixIdentifiedPotionsApplyToTheirOwnerAndRestore() {
        for (int seat : new int[]{0,1}) for (Potion potion : new Potion[]{new PotionOfInvisibility(),
                new PotionOfLevitation(), new PotionOfPurity(), new PotionOfMindVision(),
                new PotionOfStrength(), new PotionOfExperience()}) {
            int strength, level;
            Bundle state;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
                game.join(22, HeroClass.WARRIOR);
                Hero h = game.hero(seat);
                strength = h.STR; level = h.lvl;
                potion.identify(false);
                MultiAlchemyTest.pick(game, seat, potion);
                assertTrue(potion.getClass().getSimpleName(), CoopFloorsTest.step(game, DRINK, 0, potion));
                assertFalse(h.belongings.contains(potion));
                state = game.checkpoint();
            }
            try (CoopSession restored = CoopSession.restore(state)) {
                    Hero owner = restored.hero(seat), peer = restored.hero(1-seat);
                    Class<? extends Buff> buff = potion instanceof PotionOfLevitation ? Levitation.class
                            : potion instanceof PotionOfPurity ? BlobImmunity.class
                            : potion instanceof PotionOfMindVision ? MindVision.class
                            : potion instanceof PotionOfInvisibility ? Invisibility.class : null;
                    if (buff != null) { assertNotNull(owner.buff(buff)); assertNull(peer.buff(buff)); }
                    if (potion instanceof PotionOfStrength) assertEquals(strength+1, owner.STR);
                    if (potion instanceof PotionOfExperience) assertEquals(level+1, owner.lvl);
            }
        }
    }

    @Test public void directCorruptionAndStaffPreservationKeepCasterAndActor() {
        for (int seat : new int[]{0,1}) for (boolean imbue : new boolean[]{false, true}) {
            long id; int actor;
            Bundle state;
            try (CoopSession game = CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat rat = CoopMageTest.encounter(game,seat);
                Hero owner = TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                MagesStaff staff = (MagesStaff)owner.belongings.weapon();
                WandOfCorruption wand = WandArtsTest.give(game,seat,new WandOfCorruption());
                id = game.ownership.identity(wand);
                rat.HP = 1;
                rat.maxLvl = owner.lvl;
                if (imbue) assertTrue(CoopFloorsTest.step(game, IMBUE, WandArtsTest.choice(game,seat,wand),staff));
                SewerBeeTest.offer(game,seat);
                int peerXP = game.hero(1-seat).exp, xp = owner.exp, charges = wand.curCharges;
                assertTrue(CoopFloorsTest.step(game,ZAP,rat.pos,imbue ? staff : wand));
                assertEquals(owner.id(),rat.buff(Corruption.class).casterId());
                assertEquals(Char.Alignment.ALLY,rat.alignment);
                // The original degeneration can tick before the scheduler offers the next seat.
                assertTrue(rat.HP == rat.HT || rat.HP == rat.HT - rat.HT/100);
                assertEquals(charges-1,wand.curCharges);
                assertEquals(peerXP,game.hero(1-seat).exp);
                assertEquals(xp+rat.EXP,owner.exp);
                actor = rat.id();
                state = game.checkpoint();
            }
            try (CoopSession restored = CoopSession.restore(state)) {
                    Hero h = restored.hero(seat);
                    assertEquals(h.id(),((Rat)Actor.findById(actor)).buff(Corruption.class).casterId());
                    assertEquals(id, restored.ownership.identity(imbue ? ((MagesStaff)h.belongings.weapon()).imbuedWand()
                            : h.belongings.getItem(WandOfCorruption.class)));
            }
        }
    }
    @Test public void ownCorruptionMemoryWithoutTheReferenceConvertsTheTarget() {
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
                WandOfCorruption wand=WandArtsTest.give(game,seat,new WandOfCorruption());
                StagedInput memory=SealArtsTest.historyInput(game,seat,wand,TrinityDiscoveries.Choice.CORRUPTION);
                assertTrue(CoopFloorsTest.step(game,MIND_FORM,memory.cell,hero.belongings.artifact));
                assertNull(hero.belongings.getItem(WandOfCorruption.class));
                assertFalse(game.discoveries(1-seat).knows(TrinityDiscoveries.Choice.CORRUPTION));
                save=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(save)) {
                Hero hero=game.hero(seat);
                Rat rat=(Rat)Actor.findById(enemy);
                SewerBeeTest.offer(game,seat);
                ClassArmor armor=(ClassArmor)hero.belongings.armor();
                rat.HP=1;
                float before=armor.charge;
                assertTrue(CoopFloorsTest.step(game,TRINITY_MIND,rat.pos,armor));
                assertEquals(hero.id(),rat.buff(Corruption.class).casterId());
                assertTrue(armor.charge<before);
            }
        }
    }
    @Test public void corruptionArmorVolleyAndStaffStrikeUseNativeDebuffs() {
        for (int seat : new int[]{0, 1}) for (int mode : new int[]{0, 1, 2}) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
                game.join(22, HeroClass.MAGE);
                Rat rat = CoopMageTest.encounter(game, seat);
                Hero hero = game.hero(seat);
                WandOfCorruption wand = WandArtsTest.give(game, seat, new WandOfCorruption());
                MagesStaff staff = (MagesStaff)hero.belongings.weapon();
                if (mode == 2) {
                    TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                    rat.pos = hero.pos+1;
                    rat.HP=rat.HT=500;
                    Buff.prolong(rat, Paralysis.class, 50);
                    game.observe(seat);
                    Dungeon.level.updateFieldOfView(hero, hero.fieldOfView);
                    assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                    for (int hit=0; hit < 40 && rat.buff(Amok.class)==null; hit++) {
                        SewerBeeTest.offer(game,seat);
                        assertTrue("ranked strike seat="+seat+" swing="+hit+" life="+game.life(seat)+" hp="+rat.HP+" hero="+hero.pos+" target="+rat.pos+" seen="+hero.fieldOfView[rat.pos]+" active="+game.activeSeat(), CoopFloorsTest.step(game,ATTACK,rat.pos,null));
                    }
                    assertNotNull(rat.buff(Amok.class));
                    continue;
                }
                ClassArmor armor = ArmorTalentsTest.invest(game, seat, mode,
                        mode == 1 ? Talent.WILD_POWER : Talent.ELEMENTAL_POWER,1);
                if (mode == 0)
                    assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                else rat.HP=1;
                SewerBeeTest.offer(game,seat);
                int peerHp = game.hero(1-seat).HP;
                float before = armor.charge, charges = wand.curCharges + wand.partialCharge;
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,mode==0 ? hero.pos : rat.pos,armor));
                assertTrue(armor.charge < before);
                if (mode == 0) assertNotNull(rat.buff(Amok.class));
                else {
                    assertEquals(hero.id(), rat.buff(Corruption.class).casterId());
                    assertTrue(wand.curCharges + wand.partialCharge < charges);
                }
                assertEquals(peerHp, game.hero(1-seat).HP);
            }
        }
    }
}
