package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Skeleton;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.lan.AndroidTrialTickets;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import java.nio.file.Path;
import java.util.ArrayList;
import static org.junit.Assert.*;

public final class AndroidSeedAlchemy extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        int seat = Integer.parseInt(args[3]);
        AndroidSeedAlchemy fixture = new AndroidSeedAlchemy();
        fixture.temporary.create();
        fixture.startBackend();
        String mode = args[2];
        HeroClass type;
        switch (mode) {
        case "bloodally": case "blooddown": case "overflow": type = HeroClass.CLERIC; break;
        case "blood": case "undead": case "bloodstaff": type = HeroClass.MAGE; break;
        default: type = HeroClass.WARRIOR;
        }
        try (CoopSession game = CoopSession.open(572, 1, type)) {
            game.join(2, type);
            Hero hero = game.hero(seat);
            switch (mode) {
            case "lullaby":
            case "recharging":
                MultiAlchemyTest.station(game, seat);
                Scroll scroll = mode.equals("lullaby") ? new ScrollOfLullaby() : new ScrollOfRecharging();
                assertFalse(scroll.isKnown());
                Dungeon.level.drop(scroll.quantity(2), hero.pos);
                break;
            case "blink": case "clairvoyance": case "shock": case "aggression":
            case "flock": case "blast": case "purity": case "haste": {
                Rat target = CoopMageTest.encounter(game, seat);
                Item item;
                switch (mode) {
                case "blink": item = new StoneOfBlink(); break;
                case "clairvoyance": item = new StoneOfClairvoyance(); break;
                case "shock": item = new StoneOfShock(); break;
                case "aggression": item = new StoneOfAggression(); break;
                case "flock": item = new StoneOfFlock(); break;
                case "blast": target.HP = 1; item = new StoneOfBlast(); break;
                case "purity":
                    GameScene.add(Blob.seed(target.pos, 100, ToxicGas.class));
                    item = new PotionOfPurity();
                    break;
                default: item = new PotionOfHaste();
                }
                item.identify(false);
                MultiAlchemyTest.pick(game, seat, item);
                break;
            }
            case "seeds":
            case "seedpack":
                MultiAlchemyTest.station(game, seat);
                if (mode.equals("seedpack")) {
                    for (Item item : new ArrayList<>(hero.belongings.backpack.items)) {
                        if (item instanceof Bag) item.detachAll(hero.belongings.backpack);
                    }
                }
                MultiAlchemyTest.pick(game, seat, new Firebloom.Seed().quantity(mode.equals("seedpack") ? 5 : 3));
                MultiAlchemyTest.pick(game, 1-seat, new Firebloom.Seed().quantity(3));
                if (mode.equals("seedpack")) {
                    while (new Ankh().collect(hero.belongings.backpack)) { }
                }
                break;
            case "paid":
                MultiAlchemyTest.preparePaid(game, seat);
                MultiAlchemyTest.pick(game, 1-seat, new ScrollOfUpgrade());
                break;
            case "blood":
                TransfusionTest.prepare(game,seat);
                break;
            case "bloodstaff": {
                Rat target = CoopMageTest.encounter(game,seat);
                TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                MagesStaff staff = (MagesStaff)hero.belongings.weapon();
                WandOfTransfusion wand = WandArtsTest.give(game,seat,new WandOfTransfusion());
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                assertSame(wand,staff.imbuedWand());
                target.pos = hero.pos + 1;
                Buff.prolong(target,Paralysis.class,20);
                game.observe(seat);
                break;
            }
            case "dustwraith":
                CoopMageTest.encounter(game, seat);
                var undead = com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith.spawnAt(
                        hero.pos - 2, com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust.DustWraith.class);
                assertNotNull(undead);
                assertEquals(hero.pos - 2, undead.pos);
                game.observe(seat);
                WandArtsTest.give(game, seat, new WandOfTransfusion());
                break;
            case "undead":
                WandmakerTest.enterPrison(game);
                CoopMageTest.encounter(game,seat);
                Skeleton skeleton = new Skeleton();
                skeleton.pos = hero.pos - 2;
                GameScene.add(skeleton,2000);
                game.observe(seat);
                WandArtsTest.give(game,seat,new WandOfTransfusion());
                break;
            case "overflow":
            case "blooddown":
            case "bloodally":
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                StagedInput summon=SealArtsTest.stage(game,"lightally",seat,rat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,summon.cell,hero.belongings.armor()));
                PowerOfMany.getPoweredAlly(hero).HP=PowerOfMany.getPoweredAlly(hero).HT-1;
                if (mode.equals("overflow")) {
                    // The summon begins without a stronger shield so the wand's overflow is observable.
                    Buff.detach(PowerOfMany.getPoweredAlly(hero), Barrier.class);
                }
                WandArtsTest.give(game,seat,new WandOfTransfusion()).upgrade(2);
                if (mode.equals("blooddown")) {
                    Ankh ankh = new Ankh();
                    assertTrue(ankh.collect(hero.belongings.backpack));
                    game.ownership.claim(ankh,game.player(seat));
                    hero.HP = 1;
                }
                break;
            default: throw new IllegalArgumentException(mode);
            }
            SewerBeeTest.offer(game,seat);
            AndroidTrialTickets.export(Path.of(args[0]), Path.of(args[1]), game);
        } finally {
            fixture.stopBackend();
            fixture.temporary.delete();
        }
    }
}
