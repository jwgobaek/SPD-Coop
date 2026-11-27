package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.CorrosionTrap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Compare admission of a fresh exposure. Existing DOTs retain their own native source and duration. */
public class CorrosionPartyComparison extends HeadlessDungeon {
    private static void observe(CoopSession game, String stage, int expectedCaster) {
        CorrosiveGas pool = (CorrosiveGas) Dungeon.level.blobs.get(CorrosiveGas.class);
        Bundle nativeState = new Bundle();
        pool.storeInBundle(nativeState);
        int caster = nativeState.getInt("coop_caster");
        assertEquals(expectedCaster, caster);
        for (int seat=0; seat<2; seat++) {
            Hero target = game.hero(seat);
            boolean eligible = !target.isImmune(CorrosiveGas.class);
            boolean protectedPeer = caster != 0 && target.id() != caster;
            System.out.printf("%s seat=%d strength=%d source=%s caster=%d cellVolume=%d native=%s proposed=%s%n",
                    stage,seat,nativeState.getInt("strength"),nativeState.getClass("source").getSimpleName(),
                    caster,pool.cur[target.pos],eligible,eligible && !protectedPeer);
        }
    }

    public static void main(String[] args) throws Exception {
        CorrosionPartyComparison fixture=new CorrosionPartyComparison();
        fixture.temporary.create();
        fixture.startBackend();
        try {
            for (int first=0; first<2; first++) {
                try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                    game.join(22,HeroClass.MAGE);
                    CoopMageTest.encounter(game,first);
                    Hero a=game.hero(first), b=game.hero(1-first);
                    WandOfCorrosion wand=new WandOfCorrosion();
                    wand.onZap(a,new Ballistica(a.pos,a.pos,Ballistica.STOP_TARGET));
                    observe(game,"single",a.id());
                    wand.onZap(b,new Ballistica(b.pos,b.pos,Ballistica.STOP_TARGET));
                    observe(game,"equal",a.id());
                    wand.upgrade(2);
                    wand.onZap(b,new Ballistica(b.pos,b.pos,Ballistica.STOP_TARGET));
                    observe(game,"stronger",b.id());
                    ((CorrosiveGas)Dungeon.level.blobs.get(CorrosiveGas.class)).setStrength(8,CorrosionTrap.class);
                    observe(game,"environment",0);
                }
            }
        } finally { fixture.stopBackend(); fixture.temporary.delete(); }
    }
}
