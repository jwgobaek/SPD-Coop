package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class DustBearerTest extends HeadlessDungeon {
    @Test public void nativeSpawnerFreezesWithBearerAndRemainsOwnedOnRevival() {
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            int heroId,spawnerId;
            long itemId;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(12,HeroClass.MAGE);
                WandmakerTest.prepareReturn(game,572,seat);
                Hero bearer=game.hero(seat);
                heroId=bearer.id();
                spawnerId=bearer.buff(CorpseDust.DustGhostSpawner.class).id();
                itemId=game.ownership.identity(bearer.belongings.getItem(CorpseDust.class));
                assertTrue(new Ankh().collect(bearer.belongings.backpack));
                bearer.HP=17;
                Rat killer=new Rat();
                for (int offset : com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int cell=bearer.pos+offset;
                    if (Dungeon.level.passable[cell] && Actor.findChar(cell)==null) {
                        killer.pos=cell;
                        break;
                    }
                }
                assertTrue(Dungeon.level.adjacent(killer.pos,bearer.pos));
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(killer,100);
                for(int i=0;i<8 && bearer.isAlive();i++) killer.attack(bearer,1f,10000f,Float.POSITIVE_INFINITY);
                assertEquals(CoopSession.Life.DOWNED,game.life(seat));
                assertEquals(17,game.preDeathHP(seat));
                game.finishEffects();
                assertEquals(1-seat,game.activeSeat());
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero bearer=game.hero(seat);
                assertEquals(heroId,bearer.id());
                assertEquals(CoopSession.Life.DOWNED,game.life(seat));
                assertEquals(spawnerId,bearer.buff(CorpseDust.DustGhostSpawner.class).id());
                WandmakerTest.beside(game,1-seat,bearer.pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.REVIVE,bearer.pos,null));
                assertEquals(CoopSession.Life.ALIVE,game.life(seat));
                assertEquals(5,bearer.HP);
                assertNull(bearer.belongings.getItem(Ankh.class));
                assertEquals(itemId,game.ownership.identity(bearer.belongings.getItem(CorpseDust.class)));
                assertEquals(spawnerId,bearer.buff(CorpseDust.DustGhostSpawner.class).id());
                assertNull(game.hero(1-seat).buff(CorpseDust.DustGhostSpawner.class));
                assertSame(bearer,Actor.findById(heroId));
                game.checkpoint();
            }
        }
    }
}
