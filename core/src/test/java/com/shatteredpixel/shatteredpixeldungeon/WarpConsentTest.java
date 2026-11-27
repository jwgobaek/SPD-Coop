package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class WarpConsentTest extends HeadlessDungeon {
    static ClassArmor prepare(CoopSession game,int seat,int rank) {
        Hero mage=game.hero(seat);
        ClassArmor armor=ArmorTalentsTest.invest(game,seat,2,Talent.LONGRANGE_WARP,rank);
        assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,mage.pos,armor));
        SewerBeeTest.cross(game,Dungeon.level.exit());
        SewerBeeTest.offer(game,seat);
        return armor;
    }

    @Test public void parkedBeaconRequiresBothVotesAndOnlyChargesItsOwnerAfterRestore() {
        for (int seat=0;seat<2;seat++) for (int rank=1;rank<=4;rank++) {
            Bundle accepted;
            int landing;
            float charge,cost;
            try (CoopSession game=CoopSession.open(409,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                assertFalse(game.offersTalent(game.hero(seat),Talent.LONGRANGE_WARP));
                ClassArmor armor=prepare(game,seat,rank);
                Hero mage=game.hero(seat);
                WarpBeacon.WarpBeaconTracker mark=mage.buff(WarpBeacon.WarpBeaconTracker.class);
                assertEquals(2,Dungeon.depth);
                landing=mark.pos;
                if (rank==1) {
                    assertFalse(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                    assertFalse(game.stairs.warpPending());
                }
                armor.charge=80; // Isolated cost fixture after the insufficient-charge check.
                charge=armor.charge;
                cost=mage.armorAbility.chargeUse(mage)*(1.833f-0.333f*rank);
                assertTrue("rank="+rank+" charge="+charge+" cost="+cost+" dst="+mark.depth+" dist="+Dungeon.level.distance(mage.pos,game.hero(1-seat).pos)+" ends="+game.ending()+" can="+Dungeon.interfloorTeleportAllowed()+" amulet="+game.amuletOwner()+" vis="+game.knowledge(game.hero(1-seat)).visible(mage.pos), CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                assertFalse(CoopFloorsTest.step(game,TRAVEL,mage.pos,null));
                assertFalse(game.stairs.consent(seat,game.turn(),mage.pos,false));
                assertFalse(game.stairs.consent(1-seat,game.turn()-1,mage.pos,false));
                assertTrue(game.stairs.consent(1-seat,game.turn(),mage.pos,true));
                assertFalse(game.stairs.warpPending());
                assertEquals(charge,armor.charge,0f);
                assertTrue("rank="+rank+" charge="+charge+" cost="+cost+" dst="+mark.depth+" dist="+Dungeon.level.distance(mage.pos,game.hero(1-seat).pos)+" ends="+game.ending()+" can="+Dungeon.interfloorTeleportAllowed()+" amulet="+game.amuletOwner()+" vis="+game.knowledge(game.hero(1-seat)).visible(mage.pos), CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,armor));
                assertTrue(game.stairs.consent(1-seat,game.turn(),mage.pos,false));
                accepted=game.checkpoint();
                assertEquals(26,accepted.getInt("schema"));
            }
            try (CoopSession game=CoopSession.restore(accepted)) {
                Hero mage=game.hero(seat);
                assertTrue(game.stairs.agreed());
                assertEquals(1,game.stairs.warpDestination().depth);
                assertTrue(CoopFloorsTest.step(game,TRAVEL,mage.pos,null));
                assertEquals(1,Dungeon.depth);
                assertEquals(landing,mage.pos);
                assertTrue(Dungeon.level.adjacent(landing,game.hero(1-seat).pos));
                assertNotEquals(landing,game.hero(1-seat).pos);
                assertEquals(charge-cost,((ClassArmor)mage.belongings.armor()).charge,0.1f);
                assertFalse(game.stairs.warpPending());
                assertNotEquals(26,game.checkpoint().getInt("schema"));
            }
        }
    }
}
