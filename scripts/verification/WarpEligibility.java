package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

/** Arranged trial rooms; all travel, voting and refunds go through the normal host actions. */
public class WarpEligibility extends HeadlessDungeon {
    @Test public void lockedFloorWrongBranchAndPostVoteChargeLossPreventTravel() {
        for (int seat=0; seat<2; seat++) {
            try (CoopSession game=CoopSession.open(409,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                ClassArmor armor=WarpConsentTest.prepare(game,seat,3);
                Hero mage=game.hero(seat);
                WarpBeacon beacon=(WarpBeacon)mage.armorAbility;
                WarpBeacon.WarpBeaconTracker mark=mage.buff(WarpBeacon.WarpBeaconTracker.class);
                Dungeon.level.locked=true;
                assertFalse(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                Dungeon.level.locked=false;
                mark.branch=1;mark.depth=Dungeon.depth;
                assertEquals(beacon.chargeUse(mage),beacon.recallCost(mage,mark),0f);
                assertFalse(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                mark.branch=0;mark.depth=1;
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                assertTrue(game.stairs.consent(1-seat,game.turn(),mage.pos,false));
                armor.charge=0;
                assertFalse(CoopFloorsTest.step(game,TRAVEL,mage.pos,null));
                assertEquals(2,Dungeon.depth);
                assertEquals(0,armor.charge,0f);
                assertTrue(game.stairs.consent(1-seat,game.turn(),mage.pos,true));
            }
        }
    }

    @Test public void invalidParkedLandingRestoresCostRosterAndDestinationFloor() {
        for (int seat=0; seat<2; seat++) {
            try (CoopSession game=CoopSession.open(409,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                ClassArmor armor=WarpConsentTest.prepare(game,seat,3);
                Hero mage=game.hero(seat);
                mage.buff(WarpBeacon.WarpBeaconTracker.class).pos=0;
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                assertTrue(game.stairs.consent(1-seat,game.turn(),mage.pos,false));
                float charge=armor.charge;
                Bundle before=game.checkpoint();
                int owner=mage.id(),peer=game.hero(1-seat).id();
                assertFalse(CoopFloorsTest.step(game,TRAVEL,mage.pos,null));
                assertTrue(game.restoredAfterTravel());
                assertEquals(2,Dungeon.depth);
                assertEquals(owner,game.hero(seat).id());
                assertEquals(peer,game.hero(1-seat).id());
                assertEquals(charge,((ClassArmor)game.hero(seat).belongings.armor()).charge,0f);
                assertArrayEquals(before.getLongArray("random"), game.checkpoint().getLongArray("random"));
            }
        }
    }
}
