package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.lan.AndroidTrialTickets;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
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
        HeroClass type = mode.equals("bloodally") ? HeroClass.CLERIC : mode.equals("blood") ? HeroClass.MAGE : HeroClass.WARRIOR;
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
            case "bloodally":
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                StagedInput summon=SealArtsTest.stage(game,"lightally",seat,rat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,summon.cell,hero.belongings.armor()));
                PowerOfMany.getPoweredAlly(hero).HP=PowerOfMany.getPoweredAlly(hero).HT-1;
                WandArtsTest.give(game,seat,new WandOfTransfusion()).upgrade(2);
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
