package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.lan.AndroidTrialTickets;
import java.nio.file.Path;

import static org.junit.Assert.*;

/** Build a new offline QA room before Android import. No production app or existing player save is an input. */
public class AndroidStagedGrowth extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        AndroidStagedGrowth fixture = new AndroidStagedGrowth();
        fixture.temporary.create();
        fixture.startBackend();
        if (args.length == 3 && args[0].equals("--export-prepared")) {
            try {
                AndroidTrialTickets.exportPrepared(Path.of(args[1]), Path.of(args[2]));
            } finally {
                fixture.stopBackend();
                fixture.temporary.delete();
            }
            return;
        }
        if (args.length == 3 && (args[0].equals("--check-archive") || args[0].equals("--check-discovery-upgrade"))) {
            try {
                AndroidTrialTickets.checkArchivedRoom(Path.of(args[1]), Integer.parseInt(args[2]), args[0].equals("--check-discovery-upgrade"));
            } finally {
                fixture.stopBackend();
                fixture.temporary.delete();
            }
            return;
        }
        if (args.length == 3 && (args[2].equals("scroll-loot") || args[2].equals("scroll-seal"))) {
            HeroClass type = args[2].equals("scroll-seal") ? HeroClass.WARRIOR : HeroClass.HUNTRESS;
            try (CoopSession game = CoopSession.open(572, 1, type)) {
                game.join(2, type);
                CoopMageTest.encounter(game, 0);
                for (int seat = 0; seat < 2; seat++) {
                    var hero = game.hero(seat);
                    var spare = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger();
                    assertTrue(spare.collect(hero.belongings.backpack));
                    game.ownership.claim(spare, game.player(seat));
                    var scrolls = new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment();
                    Dungeon.level.drop(scrolls.quantity(12), hero.pos);
                }
                SewerBeeTest.offer(game, 0);
                AndroidTrialTickets.export(Path.of(args[0]), Path.of(args[1]), game);
            } finally {
                fixture.stopBackend();
                fixture.temporary.delete();
            }
            return;
        }
        boolean pairedClerics = args.length == 3 && args[2].equals("stasis-pair");
        boolean selectors = args.length == 3 && args[2].startsWith("selector-");
        boolean seals=args.length==3 && args[2].equals("seal");
        boolean armor=args.length==3 && args[2].equals("armor");
        boolean monkTrinity=args.length==3 && args[2].equals("monk-trinity");
        boolean sparkRogue=args.length==3 && args[2].equals("spark-rogue");
        HeroClass host=selectors || pairedClerics ? HeroClass.CLERIC : seals ? HeroClass.WARRIOR : (armor || monkTrinity) ? HeroClass.DUELIST : HeroClass.MAGE;
        try (CoopSession game = CoopSession.open(572, 1, host)) {
            game.join(2, sparkRogue ? HeroClass.ROGUE : HeroClass.CLERIC);
            if (pairedClerics) {
                CoopMageTest.encounter(game, 0);
                for (int seat = 0; seat < 2; seat++) {
                    game.hero(seat).lvl = 30;
                    SewerBeeTest.offer(game, seat);
                    if (seat == 0) {
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.USE_CROWN, 2,
                                CrownAbilityTest.crown(game, seat, CoopSession.MaskTrial.ROSTER)));
                    } else {
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.CLAIM_ARMOR, 2, null));
                    }
                    SewerBeeTest.offer(game, seat);
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.STASIS.ordinal(), null));
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.LIFE_LINK.ordinal(), null));
                    TierGrowthTest.powerTome(game.hero(seat));
                }
                SewerBeeTest.offer(game, 0);
                AndroidTrialTickets.export(Path.of(args[0]), Path.of(args[1]), game);
                return;
            }
            if (selectors) {
                SealArtsTest.prepareSelectors(game, args[2].substring("selector-".length()), 2);
                AndroidTrialTickets.export(Path.of(args[0]), Path.of(args[1]), game);
                return;
            }
            Rat target = CoopMageTest.encounter(game, 0);
            SewerBeeTest.offer(game, 0);
            if (monkTrinity || sparkRogue) {
                SealArtsTest.stage(game,monkTrinity ? "monkflurry" : "spark",0,target);
                SewerBeeTest.offer(game,1);
                if (monkTrinity) SealArtsTest.stage(game,"trinitymind",1,target);
                else FrostSensesTest.stage(game,"markdeath",1,target);
                SewerBeeTest.offer(game,0);
                AndroidTrialTickets.export(Path.of(args[0]),Path.of(args[1]),game);
                return;
            }
            if (seals || armor) {
                if (seals) {
                    SealArtsTest.stage(game,"sealdetach",0,target);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,
                            com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.IMPROVISED_PROJECTILES.ordinal(),null));
                    com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor spare=new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
                    spare.inscribe(new com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow());
                    spare.identify();
                    assertTrue(spare.collect(game.hero(0).belongings.backpack));
                    game.ownership.claim(spare,game.player(0));
                } else CrownAbilityTest.crown(game,0,CoopSession.MaskTrial.ROSTER);
                SewerBeeTest.offer(game,1);
                SealArtsTest.stage(game,"intuition",1,target);
                SewerBeeTest.offer(game,0);
                AndroidTrialTickets.export(Path.of(args[0]),Path.of(args[1]),game);
                return;
            }
            FrostSensesTest.stage(game, "iceimbue", 0, target);
            CrownAbilityTest.crown(game, 0, CoopSession.MaskTrial.OWNER);
            SewerBeeTest.offer(game, 1);
            FrostSensesTest.stage(game, "sunray", 1, target);
            game.beginMaskTrial(CoopSession.MaskTrial.OWNER);
            TengusMask mask = new TengusMask();
            assertTrue(mask.collect(game.hero(1).belongings.backpack));
            game.ownership.claim(mask, game.player(1));
            AndroidTrialTickets.export(Path.of(args[0]), Path.of(args[1]), game);
        } finally {
            fixture.stopBackend();
            fixture.temporary.delete();
        }
    }
}
