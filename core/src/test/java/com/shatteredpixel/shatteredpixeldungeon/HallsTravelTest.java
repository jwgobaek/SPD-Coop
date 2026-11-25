package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

/** Floor placements are staged. The king still dies in combat and every crossing needs both seats. */
public class HallsTravelTest extends HeadlessDungeon {
    @Test public void nativeHallsRestoreAndReturnThroughTheWonCity() {
        Bundle saved;
        long crown;
        int owner;
        try (CoopSession game=CavesInteriorTest.open(578)) {
            CityKingTest.prepareBoundary(game,"claim");
            owner=game.hero(0).belongings.getItem(KingsCrown.class)!=null ? 0 : 1;
            crown=game.ownership.identity(game.hero(owner).belongings.getItem(KingsCrown.class));
            for (int d=21;d<=24;d++) {
                SewerDepthTest.gate(game,true);
                assertEquals(d,Dungeon.depth);
                assertEquals(HallsLevel.class,Dungeon.level.getClass());
                assertEquals(13,game.checkpoint().getInt("schema"));
            }
            com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue statue=SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue.class);
            assertNotNull(statue);
            assertTrue(statue.weapon() instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarScythe);
            assertTrue(statue.weapon().enchantment instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting);
            assertEquals(2,statue.weapon().reachFactor(statue));
            assertEquals(40+6*statue.weapon().level(),statue.weapon().max(statue.weapon().level()));
            // The statue delegates damage to the generated scythe, including its original enchantment.
            for (int seat=0;seat<2;seat++) assertTrue(statue.attackProc(game.hero(seat),17)>=0);
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            TenguArenaTest.waitOne(game);
            for (int d=23;d>=20;d--) {
                SewerDepthTest.gate(game,false);
                assertEquals(d,Dungeon.depth);
            }
            assertTrue(((com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel)Dungeon.level).exitOpen());
            assertEquals(crown,game.ownership.identity(game.hero(owner).belongings.getItem(KingsCrown.class)));
            SewerDepthTest.gate(game,true);
            TenguArenaTest.waitOne(game);
        }
    }
}
