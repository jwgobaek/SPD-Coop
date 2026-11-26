package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.watabou.utils.Bundle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class FrostVolleyReplay extends HeadlessDungeon {
    private static void prepare(Path file, int seat) throws Exception {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
            game.join(22, HeroClass.MAGE);
            Rat rat = CoopMageTest.encounter(game, seat);
            SewerBeeTest.offer(game, seat);
            FrostSensesTest.stage(game, "icevolley", seat, rat);
            Hero hero = game.hero(seat);
            if (hero.belongings.getAllItems(Wand.class).stream().noneMatch(w -> w instanceof WandOfMagicMissile))
                WandArtsTest.give(game, seat, new WandOfMagicMissile());
            Map<String, Wand> kinds = new HashMap<>();
            for (Wand w : hero.belongings.getAllItems(Wand.class)) {
                assertNull(kinds.put(w.getClass().getSimpleName(), w));
                w.curCharges = 1;
                w.partialCharge = .25f;
            }
            assertEquals(4, kinds.size());
            Bundle fixture = new Bundle();
            fixture.put("game", game.checkpoint());
            fixture.put("target", rat.id());
            try (java.io.OutputStream out = Files.newOutputStream(file)) { assertTrue(Bundle.write(fixture, out)); }
        }
    }
    private static void replay(Path file, int seat) throws Exception {
        Bundle fixture;
        try (java.io.InputStream in = Files.newInputStream(file)) { fixture = Bundle.read(in); }
        try (CoopSession game = CoopSession.restore(fixture.getBundle("game"))) {
            Hero hero = game.hero(seat);
            Rat rat = (Rat) Actor.findById(fixture.getInt("target"));
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, rat.pos, hero.belongings.armor()));
            for (Wand wand : hero.belongings.getAllItems(Wand.class)) {
                assertEquals(game.player(seat), game.ownership.owner(wand));
                assertTrue(wand.curCharges + wand.partialCharge < 1.25f);
                System.out.printf("SHOT seat=%d type=%s id=%d charge=%d partial=%s%n", seat,
                        wand.getClass().getSimpleName(), game.ownership.identity(wand), wand.curCharges, wand.partialCharge);
            }
            assertTrue(rat.HP < rat.HT);
            System.out.println("RESULT hp=" + rat.HP + " signature=" + WandArtsTest.artsSignature(game));
        }
    }
    public static void main(String[] args) throws Exception {
        FrostVolleyReplay p = new FrostVolleyReplay();
        p.temporary.create();
        p.startBackend();
        try {
            if (args[0].equals("prepare")) prepare(Path.of(args[1]), Integer.parseInt(args[2]));
            else replay(Path.of(args[1]), Integer.parseInt(args[2]));
        } finally { p.stopBackend(); p.temporary.delete(); }
    }
}
