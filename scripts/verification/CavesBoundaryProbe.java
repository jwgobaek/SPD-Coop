package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel.PylonEnergy;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import static org.junit.Assert.*;

/** Additional isolated native behavior probes, including replay of a single electrical tick. */
public class CavesBoundaryProbe extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        CavesBoundaryProbe p = new CavesBoundaryProbe();
        p.temporary.create();
        p.startBackend();
        try {
            electricity();
            for (int seat = 0; seat < 2; seat++) fullPack(seat);
        } finally {
            p.stopBackend();
            p.temporary.delete();
        }
    }

    private static void electricity() {
        Bundle before;
        int[] damage;
        long[] rng;
        try (CoopSession g = CavesInteriorTest.open(570)) {
            CavesBossTest.arrive(g);
            CavesBossTest.start(g);
            while (!CavesBossTest.boss().supercharged) CavesBossTest.strike(g, CavesBossTest.boss());
            int seat = 0;
            for (int i = 0; i < Dungeon.level.length() && seat < 2; i++) {
                if (Dungeon.level.passable[i] && Actor.findChar(i) == null && Blob.volumeAt(i, PylonEnergy.class) > 0) {
                    g.hero(seat).pos = i;
                    g.observe(seat++);
                }
            }
            assertEquals(2, seat);
            before = g.checkpoint();
            damage = tick(g);
            rng = Random.worldState();
        }
        try (CoopSession restored = CoopSession.restore(before)) {
            assertArrayEquals(damage, tick(restored));
            assertArrayEquals(rng, Random.worldState());
        }
        System.out.printf("PASS native energized tick, seat 0 damage %d, seat 1 damage %d, restored RNG matches%n", damage[0], damage[1]);
    }

    private static int[] tick(CoopSession g) {
        int[] damage = {g.hero(0).HP, g.hero(1).HP};
        Dungeon.level.blobs.get(PylonEnergy.class).act();
        for (int seat = 0; seat < 2; seat++) {
            damage[seat] -= g.hero(seat).HP;
            assertTrue(damage[seat] >= 6 && damage[seat] <= 12);
        }
        return damage;
    }

    private static void fullPack(int seat) {
        int seed = seat == 0 ? 570 : 571;
        Bundle saved;
        long id;
        try (CoopSession g = CavesInteriorTest.open(seed)) {
            CavesInteriorTest.arrive(g, seat == 0 ? 13 : 12);
            Blacksmith smith = SewerSpecialActorTest.actor(Blacksmith.class);
            while (g.activeSeat() != seat) TenguArenaTest.waitOne(g);
            Hero hero = g.hero(seat);
            for (int d : PathFinder.NEIGHBOURS8) {
                int pos = smith.pos + d;
                if (Dungeon.level.passable[pos] && Actor.findChar(pos) == null && Dungeon.level.heaps.get(pos) == null) {
                    hero.pos = pos;
                    break;
                }
            }
            g.observe(seat);
            while (hero.belongings.backpack.items.size() < hero.belongings.backpack.capacity()) {
                Item filler = new Dagger();
                assertTrue(filler.collect(hero.belongings.backpack));
                g.ownership.claim(filler, g.player(seat));
            }
            assertTrue(CoopFloorsTest.step(g, CoopSession.Action.ACCEPT_BLACKSMITH, smith.pos, null));
            Item pick = Dungeon.level.heaps.get(hero.pos).peek();
            assertTrue(pick instanceof Pickaxe);
            assertNull(hero.belongings.getItem(Pickaxe.class));
            assertFalse(g.ownership.allowed(pick, g.player(1-seat)));
            id = g.ownership.identity(pick);
            assertTrue(id > 0);
            saved = g.checkpoint();
        }
        try (CoopSession g = CoopSession.restore(saved)) {
            Item pick = Dungeon.level.heaps.get(g.hero(seat).pos).peek();
            assertEquals(id, g.ownership.identity(pick));
            assertFalse(g.ownership.allowed(pick, g.player(1-seat)));
            assertFalse(Blacksmith.Quest.acceptFor(g.hero(1-seat)));
        }
        System.out.println("PASS full pack smith, generated quest, singular owned floor pick after restore, seat " + seat);
    }
}
