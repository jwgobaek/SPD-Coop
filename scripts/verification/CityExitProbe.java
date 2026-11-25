package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.watabou.utils.Bundle;
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
            long turn=game.turn();
            Bundle before=game.checkpoint();
            assertEquals(-1,game.stairs.stair());
            assertFalse(game.stairs.consent(1-game.activeSeat(),turn,exit,false));
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.TRAVEL,exit,null));
            assertEquals(20,Dungeon.depth);
            assertEquals(turn,game.turn());
            assertArrayEquals(before.getLongArray("random"),game.checkpoint().getLongArray("random"));
            assertEquals(crown,game.ownership.identity(game.hero(owner).belongings.getItem(KingsCrown.class)));
            System.out.println("PASS seed=572 branch=0 won=true crown_preserved=true gate=20_to_21 refused=true turn_and_rng_preserved=true");
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
