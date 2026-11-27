package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class BowLandingChecks extends HeadlessDungeon {
    @Test public void unsupportedPushRefusesBeforeConsumingAnItemOrRandomRoll() {
        for(String effect:new String[]{"Elastic","Unstable"}) for(int seat=0;seat<2;seat++)
            for(boolean shared:new boolean[]{false,true}) try(CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                StagedInput input=BowEffectsTest.prepareThrow(game,seat,"bowthrow"+effect+seat);
                Hero owner=game.hero(seat);
                Rat rat=(Rat)Actor.findChar(input.cell);
                rat.HP=rat.HT=200;
                int impact=rat.pos;
                Level.set(impact+2,Terrain.CHASM);
                CoopSession.Action verb=shared?CoopSession.Action.THROW:CoopSession.Action.SHOOT;
                Item item=shared?owner.belongings.getItem(ThrowingStone.class):owner.belongings.getItem(SpiritBow.class);
                long turn=game.turn(), id=game.ownership.identity(item);
                long[] rng=Random.worldState();
                int quantity=item.quantity();
                assertFalse(CoopFloorsTest.step(game,verb,impact,item));
                assertEquals(turn,game.turn());
                assertEquals(quantity,item.quantity());
                assertEquals(id,game.ownership.identity(item));
                assertArrayEquals(rng,Random.worldState());
                Buff.prolong(owner,MagicImmune.class,MagicImmune.DURATION);
                assertTrue(CoopFloorsTest.step(game,verb,impact,item));
                assertEquals(impact,rat.pos);
                assertTrue("Physical projectile still hits",rat.HP<200);
            }
    }
}
