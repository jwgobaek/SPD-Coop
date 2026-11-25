package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Acidic;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.*;

public class AcidicTest extends HeadlessDungeon {
    @Test public void generatedScorpionRestoresAndAppliesOozeToItsActualVictim() {
        Bundle dormant;
        int id;
        try (CoopSession game = CavesInteriorTest.open(570)) {
            CityKingTest.prepareBoundary(game, "claim");
            while (Dungeon.depth < 24) SewerDepthTest.gate(game, true);
            id = SewerSpecialActorTest.actor(Acidic.class).id();
            SewerDepthTest.gate(game, false);
            assertNull(Actor.findById(id));
            dormant = game.checkpoint();
        }

        for (int seat = 0; seat < 2; seat++) {
            Bundle active;
            try (CoopSession game = CoopSession.restore(dormant)) {
                SewerDepthTest.gate(game, true);
                Acidic acidic = CavesInteriorTest.stage(game, Acidic.class, seat);
                assertEquals(id, acidic.id());
                int shootingPos=game.hero(seat).pos;
                game.hero(seat).pos=acidic.pos+1;
                game.observe(seat);
                assertEquals("Acidic", com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks.seatImage(game,seat).monsters.get(acidic.pos));
                game.hero(seat).pos=shootingPos;
                game.observe(seat);
                Hero victim = game.hero(seat);
                int before = victim.HP;
                for (int turn = 0; turn < 32 && victim.buff(Ooze.class) == null; turn++) {
                    TenguArenaTest.waitOne(game);
                }
                assertTrue(victim.HP < before);
                assertNotNull(victim.buff(Ooze.class));
                assertNull(game.hero(1-seat).buff(Ooze.class));
                active = game.checkpoint();
            }

            try (CoopSession game = CoopSession.restore(active)) {
                Acidic acidic = (Acidic)Actor.findById(id);
                int shootingPos=game.hero(seat).pos;
                game.hero(seat).pos=acidic.pos+1;
                game.observe(seat);
                assertEquals("Acidic", com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks.seatImage(game,seat).monsters.get(acidic.pos));
                game.hero(seat).pos=shootingPos;
                game.observe(seat);
                Hero victim = game.hero(seat);
                Ooze ooze = victim.buff(Ooze.class);
                assertNotNull(ooze);
                int hp = victim.HP;
                ooze.act();
                assertEquals(hp - (1 + Dungeon.scalingDepth()/5), victim.HP);
                Hero other = game.hero(1-seat);
                acidic.defenseProc(other, 8);
                assertNull(other.buff(Ooze.class));
                CavesBossTest.beside(game, 1-seat, acidic.pos);
                acidic.defenseProc(other, 8);
                assertNotNull(other.buff(Ooze.class));
                for (int turn = 0; turn < 150 && acidic.isAlive(); turn++) {
                    CityKingTest.strike(game, acidic);
                }
                assertFalse(acidic.isAlive());
                assertTrue(Dungeon.level.heaps.get(acidic.pos).items.stream()
                        .anyMatch(PotionOfExperience.class::isInstance));
            }
        }
    }
}
