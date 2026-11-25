package com.shatteredpixel.shatteredpixeldungeon;

import com.watabou.utils.Bundle;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import static org.junit.Assert.*;

/** Corrupt copies of a real active mine with its main cave parked at the same depth. */
public class MineIntegrityProbe extends HeadlessDungeon {
    private static void refuses(byte[] checkpoint,String label,Consumer<Bundle> corrupt) throws Exception {
        Bundle copy=Bundle.read(new ByteArrayInputStream(checkpoint));
        corrupt.accept(copy);
        assertThrows(label,IllegalArgumentException.class,()->CoopSession.restore(copy));
        assertNull(Dungeon.coop);
        System.out.println("PASS rejected "+label);
    }
    private static Bundle mainCave(Bundle save,int depth) {
        return save.getBundle("dormant").getBundle(Integer.toString(depth));
    }
    private static void inspect(int seed) throws Exception {
        int depth,hero;
        long item;
        byte[] checkpoint;
        try (CoopSession game=CavesInteriorTest.open(seed)) {
            MiningBranchTest.enter(game,seed);
            depth=Dungeon.depth;hero=game.hero(0).id();
            item=game.ownership.identity(game.hero(0).belongings.weapon);
            checkpoint=game.checkpoint().toString().getBytes(StandardCharsets.UTF_8);
        }
        refuses(checkpoint,"mine records under schema 9",b->b.put("schema",9));
        refuses(checkpoint,"same-depth main cave relabelled as a mine",b->mainCave(b,depth).put("branch",1));
        refuses(checkpoint,"missing main cave",b->b.getBundle("dormant").remove(Integer.toString(depth)));
        refuses(checkpoint,"duplicate live and parked actor",b->mainCave(b,depth).getBundle("level").getBundleArray("mobs")[0].put("id",hero));
        refuses(checkpoint,"duplicate equipped and parked physical item",b->{
            Bundle items=mainCave(b,depth).getBundle("items");
            items.getBundle(items.getKeys().get(0)).put("id",item);
        });
        try (CoopSession game=CoopSession.restore(Bundle.read(new ByteArrayInputStream(checkpoint)))) {
            assertEquals(1,Dungeon.branch);assertEquals(depth,Dungeon.depth);
            assertEquals(hero,game.hero(0).id());assertEquals(item,game.ownership.identity(game.hero(0).belongings.weapon));
            TenguArenaTest.waitOne(game);
            System.out.println("PASS original survives all five rejected copies seed="+seed);
        }
    }
    public static void main(String[] args) throws Exception {
        MineIntegrityProbe probe=new MineIntegrityProbe();
        probe.temporary.create();probe.startBackend();
        try {inspect(571);inspect(570);}
        finally {probe.stopBackend();probe.temporary.delete();}
    }
}
