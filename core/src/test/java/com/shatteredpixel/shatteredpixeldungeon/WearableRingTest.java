package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.watabou.utils.Bundle;
import java.util.List;
import org.junit.Test;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class WearableRingTest extends HeadlessDungeon {
    private static void nativeEffect(Ring ring, Hero owner, int bonus) {
        double actual, expected;
        if (ring instanceof RingOfEnergy) {
            expected = Math.pow(1.175, bonus);
            actual = RingOfEnergy.wandChargeMultiplier(owner);
            assertEquals(expected, RingOfEnergy.artifactChargeMultiplier(owner), 0.0001);
            assertEquals(expected, RingOfEnergy.armorChargeMultiplier(owner), 0.0001);
        } else if (ring instanceof RingOfTenacity) {
            expected = Math.pow(.85, bonus*(1d-owner.HP/(double)owner.HT));
            actual = RingOfTenacity.damageMultiplier(owner);
        } else if (ring instanceof RingOfMight) {
            assertEquals(bonus, RingOfMight.strengthBonus(owner));
            expected = Math.pow(1.035, bonus);
            actual = RingOfMight.HTMultiplier(owner);
        } else if (ring instanceof RingOfFuror) {
            expected = Math.pow(1.09051, bonus);
            actual = RingOfFuror.attackSpeedMultiplier(owner);
        } else if (ring instanceof RingOfEvasion) {
            expected = Math.pow(1.125, bonus);
            actual = RingOfEvasion.evasionMultiplier(owner);
        } else {
            expected = Math.pow(1.3, bonus);
            actual = RingOfAccuracy.accuracyMultiplier(owner);
        }
        assertEquals(expected, actual, 0.0001);
    }

    @Test public void sixIdentifiedRingsKeepTheirNativeEffectsAndOwnersAcrossRestore() throws Exception {
        for (int seat=0; seat<2; seat++) {
            for (Class<? extends Ring> type : List.of(RingOfEnergy.class, RingOfTenacity.class, RingOfMight.class,
                    RingOfFuror.class, RingOfAccuracy.class, RingOfEvasion.class)) {
                Bundle state;
                long id;
                try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                    game.join(22,HeroClass.WARRIOR);
                    CoopMageTest.encounter(game,seat);
                    Hero owner=game.hero(seat);
                    Ring ring=type.getDeclaredConstructor().newInstance();
                    ring.upgrade(); ring.identify(false);
                    MultiAlchemyTest.pick(game,seat,ring);
                    owner.HP=owner.HT/2;
                    var view=ProjectionChecks.decodedSeat(game,seat);
                    assertTrue(view.belongings.stream().anyMatch(i -> i.kind.equals("RING") && view.actions(i).contains("EQUIP")));
                    id=game.ownership.identity(ring);
                    assertTrue(CoopFloorsTest.step(game,EQUIP,0,ring));
                    nativeEffect(ring,owner,2);
                    nativeEffect(ring,game.hero(1-seat),0);
                    state=game.checkpoint();
                }
                try (CoopSession game=CoopSession.restore(state)) {
                    Hero owner=game.hero(seat);
                    Ring ring=owner.belongings.getItem(type);
                    assertEquals(id,game.ownership.identity(ring));
                    assertTrue(ring.isEquipped(owner));
                    nativeEffect(ring,owner,2);
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,UNEQUIP,0,ring));
                    nativeEffect(ring,owner,0);
                    assertTrue(owner.belongings.backpack.contains(ring));
                }
            }
        }
    }
}
