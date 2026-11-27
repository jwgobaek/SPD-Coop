package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.HeadlessDungeon;
import com.shatteredpixel.shatteredpixeldungeon.SewerDepthTest;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/** Compare native special-room rolls on a disposable floor. Never loaded by the game. */
public class DualRogueCacheComparison extends HeadlessDungeon {
    private static Map<Long, String> placements(CoopSession game) {
        Map<Long, String> items = new TreeMap<>();
        for (Heap heap : Dungeon.level.heaps.valueList()) {
            for (Item item : heap.items) if (item instanceof SupplyRation) {
                assertEquals(Heap.Type.CHEST, heap.type);
                long owner = game.ownership.owner(item);
                assertTrue(owner == game.player(0) || owner == game.player(1));
                assertNull(items.put(game.ownership.identity(item), owner + "@" + heap.pos));
            }
        }
        return items;
    }

    private static void compare(int hostRank, int guestRank, boolean separate, int tieSeat, Path out) throws Exception {
        Bundle save;
        Map<Long, String> observed;
        long nextInScope;
        long nextOutside;
        try (CoopSession game = CoopSession.open(402, 11, HeroClass.ROGUE)) {
            game.join(22, HeroClass.ROGUE);
            SewerDepthTest.gate(game, true);
            RegularLevel floor = (RegularLevel) Dungeon.level;
            for (int seat = 0; seat < 2; seat++) {
                assertFalse(game.offersTalent(game.hero(seat), Talent.CACHED_RATIONS));
                game.hero(seat).talents.get(0).put(Talent.CACHED_RATIONS, seat == 0 ? hostRank : guestRank);
            }
            Random.pushGenerator(402);
            try {
                Random.pushGenerator(Random.Long());
                try {
                    int recipient = hostRank == 0 ? 1 : guestRank == 0 ? 0 : tieSeat;
                    for (int seat = 0; seat < 2; seat++) {
                        if (separate || seat == recipient) floor.dropCachedRation(game.hero(seat));
                        var count = game.hero(seat).buff(Talent.CachedRationsDropped.class);
                        float actual = count == null ? 0 : count.count();
                        boolean eligible = (seat == 0 ? hostRank : guestRank) > 0;
                        assertEquals(eligible && (separate || seat == recipient) ? 2f : 0f, actual, 0f);
                    }
                    nextInScope = Random.Long();
                } finally {
                    Random.popGenerator();
                }
                nextOutside = Random.Long();
            } finally {
                Random.popGenerator();
            }
            observed = placements(game);
            int eligible = (hostRank > 0 ? 1 : 0) + (guestRank > 0 ? 1 : 0);
            assertEquals(separate ? eligible : Math.min(1, eligible), observed.size());
            SewerDepthTest.gate(game, false);
            SewerDepthTest.gate(game, true);
            assertEquals(observed, placements(game));
            save = game.checkpoint();
            if (out != null) {
                try (var stream = Files.newOutputStream(out)) { assertTrue(Bundle.write(save, stream)); }
                Files.writeString(out.resolveSibling(out.getFileName()+".expected"), observed.toString());
            }
        }
        try (CoopSession restored = CoopSession.restore(save)) {
            assertEquals(observed, placements(restored));
            System.out.printf("CACHE host=%d guest=%d policy=%s tieSeat=%d items=%s inner=%d outer=%d%n",
                    hostRank, guestRank, separate ? "B" : "A", tieSeat, observed, nextInScope, nextOutside);
        }
    }

    private static void repeatedDraw() {
        try (CoopSession game=CoopSession.open(402,11,HeroClass.ROGUE)) {
            game.join(22,HeroClass.ROGUE);
            SewerDepthTest.gate(game,true);
            RegularLevel floor=(RegularLevel)Dungeon.level;
            for (int seat=0;seat<2;seat++) {
                game.hero(seat).talents.get(0).put(Talent.CACHED_RATIONS,2);
                // The two native streams initially propose the same room and cell.
                Random.pushGenerator(402);
                try { floor.dropCachedRation(game.hero(seat)); } finally { Random.popGenerator(); }
            }
            var chests=placements(game);
            assertEquals(2,chests.size());
            assertEquals(2,chests.values().stream().map(i->i.substring(i.indexOf('@')+1)).distinct().count());
            System.out.println("COLLISION occupied first draw retried, "+chests);
        }
    }

    private static void read(Path path) throws Exception {
        try (var stream=Files.newInputStream(path); CoopSession game=CoopSession.restore(Bundle.read(stream))) {
            assertEquals(Files.readString(path.resolveSibling(path.getFileName()+".expected")),placements(game).toString());
            for (int seat=0;seat<2;seat++) assertFalse(game.offersTalent(game.hero(seat),Talent.CACHED_RATIONS));
            int seat=game.activeSeat();
            assertTrue(game.act(game.player(seat),game.turn(),CoopSession.Action.WAIT,0,null));
            System.out.println("FRESH_JVM restored IDs and owners, next WAIT applied");
        }
    }

    public static void main(String[] args) throws Exception {
        DualRogueCacheComparison suite = new DualRogueCacheComparison();
        suite.temporary.create();
        suite.startBackend();
        try {
            if (args.length==2 && args[0].equals("read")) { read(Path.of(args[1])); return; }
            if (args.length==2 && (args[0].equals("A") || args[0].equals("B"))) {
                compare(2,2,args[0].equals("B"),1,Path.of(args[1]));
                return;
            }
            repeatedDraw();
            for (int host = 0; host <= 2; host++) for (int guest = 0; guest <= 2; guest++) {
                compare(host, guest, false, 0,null);
                if (host > 0 && guest > 0) compare(host, guest, false, 1,null);
                compare(host, guest, true, 0,null);
            }
        } finally {
            suite.stopBackend();
            suite.temporary.delete();
        }
    }
}
