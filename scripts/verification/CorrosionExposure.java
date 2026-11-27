package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.CorrosionTrap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.*;

/** Native ambient hazard probe, independent of the wand's cooperative action allowlist. */
public class CorrosionExposure extends HeadlessDungeon {
    @Test public void twoGasSeedsShareStrengthAndCanReachBothSeatsAfterRestore() {
        int originalActor;
        Bundle generation;
        try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22, HeroClass.MAGE);
            CoopMageTest.encounter(game,0);
            for (int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                hero.HP=hero.HT=140;
            }
            CorrosiveGas weak=Blob.seed(game.hero(0).pos,50,CorrosiveGas.class);
            weak.setStrength(2,WandOfCorrosion.class);
            GameScene.add(weak);
            CorrosiveGas strong=Blob.seed(game.hero(1).pos+1,70,CorrosiveGas.class);
            assertSame(weak,strong);
            strong.setStrength(4,CorrosionTrap.class);
            strong.setStrength(2,WandOfCorrosion.class);
            Bundle actual=new Bundle();
            strong.storeInBundle(actual);
            assertEquals(4,actual.getInt("strength"));
            assertEquals(CorrosionTrap.class,actual.getClass("source"));
            originalActor=strong.id();
            generation=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(generation)) {
            Blob gas=Dungeon.level.blobs.get(CorrosiveGas.class);
            assertEquals(originalActor,gas.id());
            int[] exposed={0,0};
            for (int i=0;i<12;i++) {
                TenguArenaTest.waitOne(game);
                for(int seat=0;seat<2;seat++) if(game.hero(seat).buff(Corrosion.class)!=null) exposed[seat]++;
            }
            for (int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                assertTrue(exposed[seat]>0);
                assertTrue(hero.HP<hero.HT);
                System.out.println("CORROSION seat="+seat+" hp="+hero.HP+"/"+hero.HT+" sampledExposures="+exposed[seat]);
            }
        }
    }
}
