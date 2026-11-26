package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
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
        try (CoopSession game = CoopSession.open(572, 1, HeroClass.MAGE)) {
            game.join(2, HeroClass.CLERIC);
            Rat target = CoopMageTest.encounter(game, 0);
            SewerBeeTest.offer(game, 0);
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
