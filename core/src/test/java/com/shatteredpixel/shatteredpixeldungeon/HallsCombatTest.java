package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class HallsCombatTest extends HeadlessDungeon {
    private Bundle floor() {
        try (CoopSession g=CavesInteriorTest.open(572)) {
            CityKingTest.prepareBoundary(g,"claim");
            SewerDepthTest.gate(g,true);
            return g.checkpoint();
        }
    }

    private <T extends Mob> T staged(CoopSession game,T mob,int seat) {
        // Isolated actor probes use the native floor, an open room and an explicit victim.
        for (Mob old:new java.util.ArrayList<>(Dungeon.level.mobs)) {
            Actor.remove(old);
            Dungeon.level.mobs.remove(old);
        }
        mob.pos=Dungeon.level.entrance();
        GameScene.add(mob);
        game.attachSprite(mob);
        CavesInteriorTest.stage(game,mob.getClass(),seat);
        return mob;
    }

    @Test public void originalRipperLeapAndBleedingSurviveTheWindupCheckpoint() {
        Bundle original=floor();
        for (int seat=0;seat<2;seat++) {
            Bundle charged;
            int demon;
            try (CoopSession g=CoopSession.restore(original)) {
                RipperDemon mob=staged(g,new RipperDemon(),seat);
                demon=mob.id();
                Bundle ai=new Bundle();
                for (int step=0;step<16;step++) {
                    TenguArenaTest.waitOne(g);
                    mob.storeInBundle(ai);
                    if (ai.getInt("leap_pos")>=0) break;
                }
                assertEquals(g.hero(seat).pos,ai.getInt("leap_pos"));
                charged=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(charged)) {
                for (int step=0;step<10 && g.hero(seat).buff(Bleeding.class)==null;step++) TenguArenaTest.waitOne(g);
                assertNotNull(g.hero(seat).buff(Bleeding.class));
                assertNull(g.hero(1-seat).buff(Bleeding.class));
                assertTrue(Dungeon.level.adjacent(((RipperDemon)Actor.findById(demon)).pos,g.hero(seat).pos));
            }
        }
    }

    @Test public void chargedEyeResumesItsRealRayThroughBothHeroes() {
        Bundle original=floor();
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            int id;
            try (CoopSession g=CoopSession.restore(original)) {
                Eye eye=staged(g,new Eye(),seat);
                g.hero(1-seat).pos=g.hero(seat).pos+1;
                g.observe(1-seat);
                for (int i=0;i<12 && !eye.beamCharged;i++) TenguArenaTest.waitOne(g);
                assertTrue(eye.beamCharged);
                id=eye.id();
                saved=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                Eye eye=(Eye)Actor.findById(id);
                assertTrue(eye.beamCharged);
                int initial0=g.hero(0).HP,initial1=g.hero(1).HP;
                for (int i=0;i<60 && (g.hero(0).HP==initial0 || g.hero(1).HP==initial1);i++) TenguArenaTest.waitOne(g);
                assertTrue(g.hero(0).HP<initial0);
                assertTrue(g.hero(1).HP<initial1);
            }
        }
    }

    @Test public void spawnerCountsAndSeatSpecificCharmAndCripple() {
        Bundle original=floor(),after;
        int count,id;
        try (CoopSession g=CoopSession.restore(original)) {
            DemonSpawner spawner=SewerSpecialActorTest.actor(DemonSpawner.class);
            count=Statistics.spawnersAlive;
            assertTrue(count>0 && spawner.spawnRecorded);
            for (int i=0;i<6 && Dungeon.level.mobs.stream().noneMatch(RipperDemon.class::isInstance);i++) TenguArenaTest.waitOne(g);
            assertTrue(Dungeon.level.mobs.stream().anyMatch(RipperDemon.class::isInstance));
            id=spawner.id();
            after=g.checkpoint();
        }
        try (CoopSession g=CoopSession.restore(after)) {
            DemonSpawner spawner=(DemonSpawner)Actor.findById(id);
            spawner.die(g.hero(1));
            assertEquals(count-1,Statistics.spawnersAlive);
            after=g.checkpoint();
        }
        try (CoopSession g=CoopSession.restore(after)) {
            assertNull(Actor.findById(id));
            assertEquals(count-1,Statistics.spawnersAlive);
        }
        for (int seat=0;seat<2;seat++) {
            Bundle effects;
            int charmer;
            try (CoopSession g=CoopSession.restore(original)) {
                Succubus succubus=staged(g,new Succubus(),seat);
                charmer=succubus.id();
                Scorpio scorpio=new Scorpio();
                scorpio.pos=succubus.pos+Dungeon.level.width();
                GameScene.add(scorpio);
                g.attachSprite(scorpio);
                for (int i=0;i<64 && !g.hero(seat).isCharmedBy(succubus);i++) succubus.attackProc(g.hero(seat),8);
                for (int i=0;i<64 && g.hero(seat).buff(Cripple.class)==null;i++) scorpio.attackProc(g.hero(seat),8);
                assertNotNull(g.hero(seat).buff(Cripple.class));
                assertTrue(g.hero(seat).isCharmedBy(succubus));
                assertNull(g.hero(1-seat).buff(Charm.class));
                effects=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(effects)) {
                assertTrue(g.hero(seat).isCharmedBy((Mob)Actor.findById(charmer)));
                assertNull(g.hero(1-seat).buff(Cripple.class));
            }
        }
    }
}
