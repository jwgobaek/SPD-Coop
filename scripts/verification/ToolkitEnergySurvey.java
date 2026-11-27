package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Native toolkit energy arithmetic before exposing the portable station to coop clients. */
public final class ToolkitEnergySurvey extends HeadlessDungeon {
    private static void spend(int seat, int stored) {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
            game.join(22, HeroClass.WARRIOR);
            AlchemistsToolkit own = new AlchemistsToolkit();
            AlchemistsToolkit peer = new AlchemistsToolkit();
            own.charge(game.hero(seat), stored * 4f);
            peer.charge(game.hero(1-seat), 12f);
            assertEquals(stored, own.availableEnergy());
            int cost = new ExoticScroll.ScrollToExotic().cost(null);
            Dungeon.energy = cost;
            int remainder = own.consumeEnergy(cost);
            assertEquals(Math.max(0, cost - stored), remainder);
            assertEquals(cost, Dungeon.energy);
            Dungeon.energy -= remainder;
            assertEquals(Math.max(0, stored - cost), own.availableEnergy());
            assertEquals(Math.min(stored, cost), Dungeon.energy);
            assertEquals(3, peer.availableEnergy());
            Bundle image = new Bundle();
            own.storeInBundle(image);
            AlchemistsToolkit restored = new AlchemistsToolkit();
            restored.restoreFromBundle(image);
            assertEquals(own.availableEnergy(), restored.availableEnergy());
            System.out.println("PASS seat=" + seat + " stored=" + stored + " cost=" + cost
                    + " sharedSpent=" + remainder + " restored=" + restored.availableEnergy());
        }
    }

    public static void main(String[] args) throws Exception {
        ToolkitEnergySurvey survey = new ToolkitEnergySurvey();
        survey.temporary.create();
        survey.startBackend();
        try {
            for (int seat = 0; seat < 2; seat++) {
                for (int energy : new int[]{0, 2, 6, 9}) spend(seat, energy);
            }
        } finally {
            survey.stopBackend();
            survey.temporary.delete();
        }
    }
}
