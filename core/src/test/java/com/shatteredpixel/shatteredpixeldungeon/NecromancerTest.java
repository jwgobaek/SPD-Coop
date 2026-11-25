package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class NecromancerTest extends HeadlessDungeon {
    /** Isolated combat placement. Floor travel and generation use separate regression cases. */
    public static Necromancer arrange(CoopSession game, int seat, boolean spectral) {
        Level l = Dungeon.level;
        for (Mob old : new ArrayList<>(l.mobs)) {
            Actor.remove(old);
            l.mobs.remove(old);
        }
        int center = l.width() * (l.height()/2) + l.width()/2;
        for (int y=-3; y<=3; y++) for (int x=-4; x<=5; x++) {
            int cell = center + y*l.width()+x;
            Level.set(cell, Terrain.EMPTY);
            l.traps.remove(cell);
            l.plants.remove(cell);
            l.discoverable[cell] = true;
        }
        Hero target = game.hero(seat);
        target.pos = center+3;
        game.hero(1-seat).pos = center-4;
        game.observe(0);
        game.observe(1);
        Necromancer necro = spectral ? new SpectralNecromancer() : new Necromancer();
        necro.pos = center;
        GameScene.add(necro);
        necro.aggro(target);
        return necro;
    }

    public static Bundle data(Actor actor) {
        Bundle b = new Bundle();
        actor.storeInBundle(b);
        return b;
    }

    public static void prepare(CoopSession game, Necromancer necro) {
        for (int i=0; i<12 && !necro.summoning; i++) SewerSpecialActorTest.waitOne(game);
        assertTrue(necro.summoning);
        assertTrue(data(necro).getBoolean("first_summon"));
    }

    @Test public void occupiedSummonCellRestoresNativeMinionAndHealsForEachSeat() {
        for (int seat=0; seat<2; seat++) {
            int caster, cell, skeleton;
            Bundle preparing, withMinion;
            try (CoopSession game = CoopSession.open(570,11,HeroClass.WARRIOR)) {
                game.join(12,HeroClass.MAGE);
                WandmakerTest.enterPrison(game);
                Necromancer necro=arrange(game,seat,false);
                prepare(game,necro);
                caster=necro.id();
                cell=necro.summoningPos;
                game.hero(seat).pos=cell;
                game.observe(seat);
                preparing=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(preparing)) {
                Necromancer necro=(Necromancer)Actor.findById(caster);
                assertEquals(cell,necro.summoningPos);
                for(int i=0; i<12 && necro.summoning; i++) SewerSpecialActorTest.waitOne(game);
                assertFalse(necro.summoning);
                assertNotEquals(cell,game.hero(seat).pos);
                assertFalse(data(necro).getBoolean("first_summon"));
                skeleton=data(necro).getInt("my_skeleton");
                assertTrue(Actor.findById(skeleton) instanceof Necromancer.NecroSkeleton);
                ((Mob)Actor.findById(skeleton)).HP=10;
                withMinion=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(withMinion)) {
                Necromancer necro=(Necromancer)Actor.findById(caster);
                Mob minion=(Mob)Actor.findById(skeleton);
                for(int i=0; i<32 && minion.buff(Adrenaline.class)==null; i++) SewerSpecialActorTest.waitOne(game);
                assertEquals(minion.HT,minion.HP);
                assertNotNull(minion.buff(Adrenaline.class));
                assertEquals(skeleton,data(necro).getInt("my_skeleton"));
                minion.die(null);
                for(int i=0; i<18 && !necro.summoning; i++) SewerSpecialActorTest.waitOne(game);
                assertTrue(necro.summoning);
                for(int i=0; i<12 && necro.summoning; i++) SewerSpecialActorTest.waitOne(game);
                int replacement=data(necro).getInt("my_skeleton");
                assertNotEquals(skeleton,replacement);
                assertTrue(Actor.findById(replacement) instanceof Necromancer.NecroSkeleton);
                necro.die(game.hero(seat));
                assertNull(Actor.findById(replacement));
                assertFalse(game.stopped());
            }
        }
    }

    @Test public void spectralCasterRestoresAndDispelsItsOwnWraiths() {
        for(int seat=0; seat<2; seat++) {
            int caster;
            int[] spirits;
            Bundle after;
            try(CoopSession game=CoopSession.open(570,11,HeroClass.WARRIOR)) {
                game.join(12,HeroClass.MAGE);
                WandmakerTest.enterPrison(game);
                Necromancer necro=arrange(game,seat,true);
                caster=necro.id();
                for(int i=0; i<25 && data(necro).getIntArray("wraith_ids").length<2; i++)
                    SewerSpecialActorTest.waitOne(game);
                spirits=data(necro).getIntArray("wraith_ids");
                assertTrue(spirits.length>=2);
                after=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(after)) {
                Necromancer necro=(Necromancer)Actor.findById(caster);
                for(int id:spirits) assertTrue(Actor.findById(id) instanceof Wraith);
                necro.die(game.hero(seat));
                for(int id:spirits) assertNull(Actor.findById(id));
                SewerSpecialActorTest.waitOne(game);
            }
        }
    }
}
