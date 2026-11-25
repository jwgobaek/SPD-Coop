package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import static org.junit.Assert.*;

/** Isolated native AI checks on copies of a real seventh-floor save. No fixture is published. */
public final class PrisonCombatChecks {
    private PrisonCombatChecks() {}

    public static void copiesOf(Bundle engine) {
        for (int seat=0;seat<2;seat++) for (boolean chain:new boolean[]{true,false}) {
            Bundle after;
            int mobId;
            try (CoopSession game=CoopSession.restore(engine)) {
                assertEquals(7,Dungeon.depth);
                Level level=Dungeon.level;
                Hero target=game.hero(seat),observer=game.hero(1-seat);
                for (Mob old:new ArrayList<>(level.mobs)) { Actor.remove(old); level.mobs.remove(old); }
                int center=level.width()*(level.height()/2)+level.width()/2;
                for (int y=-2;y<=2;y++) for (int x=-5;x<=5;x++) {
                    int pos=center+y*level.width()+x;
                    Level.set(pos,Terrain.EMPTY);
                    level.traps.remove(pos); level.plants.remove(pos);
                    level.discoverable[pos]=true;
                }
                target.pos=center+3; observer.pos=center-4-level.width();
                target.HP=target.HT; observer.HP=observer.HT;
                Mob enemy=chain ? new Guard() : new DM100();
                enemy.pos=center;
                GameScene.add(enemy);
                game.attachSprite(enemy); enemy.aggro(target);
                mobId=enemy.id();
                game.observe(0); game.observe(1);
                int oldHP=target.HP,oldObserver=observer.HP;
                for (int n=0;n<16 && (chain ? target.buff(Cripple.class)==null : oldHP==target.HP);n++)
                    SewerSpecialActorTest.waitOne(game);
                assertFalse(game.stopped());
                if (chain) {
                    assertNotNull(target.buff(Cripple.class));
                    assertTrue(level.adjacent(enemy.pos,target.pos));
                    assertNull(observer.buff(Cripple.class));
                    Bundle state=new Bundle(); enemy.storeInBundle(state); assertTrue(state.getBoolean("chainsused"));
                } else {
                    assertEquals(center+3,target.pos); assertTrue(target.HP<oldHP);
                    assertTrue(level.distance(enemy.pos,target.pos)>1);
                }
                assertEquals(oldObserver,observer.HP);
                after=game.checkpoint();
            }
            try (CoopSession restored=CoopSession.restore(after)) {
                Mob enemy=(Mob)Actor.findById(mobId);
                assertTrue(chain ? enemy instanceof Guard : enemy instanceof DM100);
                if (chain) assertNotNull(restored.hero(seat).buff(Cripple.class));
                assertTrue(CoopFloorsTest.step(restored,CoopSession.Action.WAIT,0,null));
                assertFalse(restored.stopped());
            }
        }
    }
}
