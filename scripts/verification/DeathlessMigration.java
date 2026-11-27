package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.watabou.utils.Bundle;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/** Run prepare on the historical classpath, then revive in a new JVM on the candidate classpath. */
public final class DeathlessMigration extends HeadlessDungeon {
    private static void prepare(Path file, int seat) throws Exception {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
            game.join(22, HeroClass.WARRIOR);
            SewerBeeTest.offer(game, seat);
            Hero hero = game.hero(seat);
            assertTrue(Dungeon.level.adjacent(hero.pos, game.hero(1-seat).pos));
            Ankh ankh = new Ankh();
            Dungeon.level.drop(ankh, hero.pos);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.PICK_UP, hero.pos, ankh));
            TierGrowthTest.stage(game, seat, "deathlesswait");
            game.finishEffects();
            assertEquals(0, hero.HP);
            assertEquals(CoopSession.Life.ALIVE, game.life(seat));
            assertTrue(hero.buff(Berserk.class).sustainsLife());
            Bundle save = game.checkpoint();
            assertEquals(23, save.getInt("schema"));
            try (OutputStream output = Files.newOutputStream(file)) {
                assertTrue(Bundle.write(save, output));
            }
        }
    }

    private static void revive(Path file, int seat) throws Exception {
        try (InputStream input = Files.newInputStream(file);
             CoopSession game = CoopSession.restore(Bundle.read(input))) {
            Hero hero = game.hero(seat);
            assertEquals(0, hero.HP);
            assertEquals(13, game.preDeathHP(seat));
            assertTrue(hero.buff(Berserk.class).sustainsLife());
            assertEquals(game.player(seat), game.ownership.owner(hero.belongings.getItem(Ankh.class)));
            hero.damage(100, Rat.class);
            game.finishEffects();
            assertEquals(CoopSession.Life.DOWNED, game.life(seat));
            assertEquals(13, game.preDeathHP(seat));
            assertEquals(1-seat, game.activeSeat());
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.REVIVE, hero.pos, null));
            assertEquals(CoopSession.Life.ALIVE, game.life(seat));
            assertEquals(3, hero.HP);
            assertNull(hero.belongings.getItem(Ankh.class));
            System.out.println("PASS schema23 seat=" + seat + " preDeathHP=13 shieldLost=1 revivedHP=3 ownerAnkhConsumed=1");
        }
    }

    public static void main(String[] args) throws Exception {
        DeathlessMigration backend = new DeathlessMigration();
        backend.temporary.create();
        backend.startBackend();
        try {
            Path save = Path.of(args[1]);
            int seat = Integer.parseInt(args[2]);
            if (args[0].equals("prepare")) prepare(save, seat);
            else if (args[0].equals("revive")) revive(save, seat);
            else throw new IllegalArgumentException("Expected prepare or revive");
        } finally {
            backend.stopBackend();
            backend.temporary.delete();
        }
    }
}
