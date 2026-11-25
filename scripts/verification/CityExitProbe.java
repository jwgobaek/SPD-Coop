package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import static org.junit.Assert.*;

/** Attempt the real exit after a staged native victory and a successful crown pickup. */
public class CityExitProbe extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        CityExitProbe probe=new CityExitProbe();
        probe.temporary.create();
        probe.startBackend();
        try (CoopSession game=CavesInteriorTest.open(572)) {
            CityKingTest.prepareBoundary(game,"claim");
            assertTrue(((CityBossLevel)Dungeon.level).exitOpen());
            int exit=Dungeon.level.exit();
            SewerSpecialActorTest.atGate(game,exit);
            int owner=game.hero(0).belongings.getItem(KingsCrown.class)!=null ? 0 : 1;
            long crown=game.ownership.identity(game.hero(owner).belongings.getItem(KingsCrown.class));
            CoopFloorsTest.cross(game);
            assertEquals(21,Dungeon.depth);
            assertTrue(Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel);
            assertEquals(13,game.checkpoint().getInt("schema"));
            SewerDepthTest.gate(game,false);
            assertTrue(((CityBossLevel)Dungeon.level).exitOpen());
            assertEquals(crown,game.ownership.identity(game.hero(owner).belongings.getItem(KingsCrown.class)));
            System.out.println("PASS seed=572 gate=20_to_21_to_20 crown_preserved=true won=true");

        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
