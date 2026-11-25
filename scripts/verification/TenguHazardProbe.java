package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

/** Isolated original area effects. Both heroes stand in two legal cells of the same native footprint. */
public final class TenguHazardProbe extends HeadlessDungeon {
    private static void place(CoopSession game, boolean[] area) {
        int seat = 0;
        for (int cell = 0; cell < area.length && seat < 2; cell++) {
            if (!area[cell] || !Dungeon.level.passable[cell] || Dungeon.level.water[cell]
                    || Dungeon.level.pit[cell] || Actor.findChar(cell) != null) continue;
            game.hero(seat++).pos = cell;
        }
        assertEquals("Two live, dry cells in the native hazard", 2, seat);
    }

    private static <T extends Buff> T created(Tengu boss, Class<T> kind, Set<T> prior) {
        List<T> effects = new ArrayList<>(boss.buffs(kind));
        effects.removeAll(prior);
        assertEquals(1, effects.size());
        return effects.get(0);
    }

    private static void measure(CoopSession game, int seat, String effect) {
        Tengu boss = TenguArenaTest.boss();
        boolean[] cells = new boolean[Dungeon.level.length()];
        Blob blob = null;
        Tengu.BombAbility bomb = null;
        if (effect.equals("bomb")) {
            Set<Tengu.BombAbility> prior = boss.buffs(Tengu.BombAbility.class);
            assertTrue(Tengu.throwBomb(boss, game.hero(seat)));
            bomb = created(boss, Tengu.BombAbility.class, prior);
            for (int i = 0; i < cells.length; i++) cells[i] = Dungeon.level.distance(bomb.bombPos, i) <= 1;
        } else {
            if (effect.equals("fire")) {
                Set<Tengu.FireAbility> prior = boss.buffs(Tengu.FireAbility.class);
                assertTrue(Tengu.throwFire(boss, game.hero(seat)));
                created(boss, Tengu.FireAbility.class, prior).act();
                blob = Dungeon.level.blobs.get(Tengu.FireAbility.FireBlob.class);
            } else {
                Set<Tengu.ShockerAbility> prior = boss.buffs(Tengu.ShockerAbility.class);
                assertTrue(Tengu.throwShocker(boss, game.hero(seat)));
                created(boss, Tengu.ShockerAbility.class, prior).act();
                blob = Dungeon.level.blobs.get(Tengu.ShockerAbility.ShockerBlob.class);
            }
            assertNotNull(blob);
            for (int i = 0; i < cells.length; i++) cells[i] = blob.cur[i] > 0;
        }
        place(game, cells);
        int[] before = {game.hero(0).HP, game.hero(1).HP};
        if (bomb != null) {
            for (int tick = 0; tick < 4; tick++) bomb.act();
            assertFalse(boss.buffs(Tengu.BombAbility.class).contains(bomb));
        } else {
            blob.act();
            blob.act();
            if (effect.equals("fire")) {
                for (int victim = 0; victim < 2; victim++) {
                    Burning burning = game.hero(victim).buff(Burning.class);
                    assertNotNull(burning);
                    burning.act();
                }
            }
        }
        int d0 = before[0] - game.hero(0).HP, d1 = before[1] - game.hero(1).HP;
        System.out.println("isolated effect=" + effect + " aim_seat=" + seat + " seat0_damage=" + d0 + " seat1_damage=" + d1);
        assertTrue(d0 > 0 && d1 > 0);
    }

    public static void main(String[] args) throws Exception {
        TenguHazardProbe probe = new TenguHazardProbe();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (String effect : new String[]{"bomb", "fire", "shocker"}) for (int seat = 0; seat < 2; seat++) {
                try (CoopSession game = CoopSession.open(570, 11, HeroClass.WARRIOR)) {
                    game.join(22, HeroClass.MAGE);
                    TenguArenaTest.prepareSocket(game, "arena");
                    TenguArenaTest.enter(game, true);
                    measure(game, seat, effect);
                }
            }
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }
}
