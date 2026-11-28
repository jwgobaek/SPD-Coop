package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.watabou.utils.Bundle;

import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

/** Exercise native ring effects through co-op gear actions in a disposable two-seat room. */
public class EquippedRingUse extends HeadlessDungeon {
    private void haste(int seat) throws Exception {
        Bundle save;
        long id;
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.WARRIOR)) {
            game.join(22, HeroClass.WARRIOR);
            CoopMageTest.encounter(game, seat);
            var hero = game.hero(seat);
            var ring = new RingOfHaste();
            ring.upgrade();
            ring.identify(false);
            MultiAlchemyTest.pick(game, seat, ring);
            id = game.ownership.identity(ring);
            var before = ProjectionChecks.decodedSeat(game, seat);
            assertTrue(before.belongings.stream().anyMatch(i -> i.kind.equals("HASTE_RING") && before.actions(i).contains("EQUIP")));
            assertTrue(CoopFloorsTest.step(game, EQUIP, 0, ring));
            assertEquals(1.15f*1.15f, RingOfHaste.speedMultiplier(hero), 0.0001f);
            assertEquals(1f, RingOfHaste.speedMultiplier(game.hero(1-seat)), 0);
            assertEquals(game.player(seat), game.ownership.owner(ring));
            save = game.checkpoint();
        }
        try (CoopSession game = CoopSession.restore(save)) {
            var hero = game.hero(seat);
            var ring = hero.belongings.getItem(RingOfHaste.class);
            assertTrue(ring.isEquipped(hero));
            assertEquals(id, game.ownership.identity(ring));
            assertEquals(1.15f*1.15f, hero.speed(), 0.0001f);
            assertEquals(1f, game.hero(1-seat).speed(), 0);
            SewerBeeTest.offer(game, seat);
            assertTrue(CoopFloorsTest.step(game, UNEQUIP, 0, ring));
            assertEquals(1f, hero.speed(), 0);
            assertTrue(hero.belongings.backpack.contains(ring));
            assertEquals(id, game.ownership.identity(ring));
            System.out.printf("PASS haste seat=%d id=%d speed=1.3225 restored, unequipped=1%n", seat, id);
        }
    }

    private void combined(int seat,boolean curse) {
        Bundle saved;
        long firstId,secondId,otherId;
        try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            CoopMageTest.encounter(game,seat);
            var first=new RingOfAccuracy();
            var second=new RingOfAccuracy();
            var other=new RingOfEnergy();
            first.upgrade(); first.identify(false);
            second.cursed=curse; second.identify(false);
            other.identify(false);
            MultiAlchemyTest.pick(game,seat,first);
            assertTrue(CoopFloorsTest.step(game,EQUIP,0,first));
            MultiAlchemyTest.pick(game,seat,second);
            assertTrue(CoopFloorsTest.step(game,EQUIP,0,second));
            MultiAlchemyTest.pick(game,1-seat,other);
            assertTrue(CoopFloorsTest.step(game,EQUIP,0,other));
            firstId=game.ownership.identity(first);
            secondId=game.ownership.identity(second);
            otherId=game.ownership.identity(other);
            assertNotEquals(firstId,secondId);
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            var hero=game.hero(seat);
            assertEquals(firstId,game.ownership.identity(hero.belongings.ring));
            assertEquals(secondId,game.ownership.identity(hero.belongings.misc));
            assertEquals(otherId,game.ownership.identity(game.hero(1-seat).belongings.ring));
            assertEquals(Math.pow(1.3,curse ? 0 : 3),RingOfAccuracy.accuracyMultiplier(hero),.0001);
            assertEquals(1,RingOfAccuracy.accuracyMultiplier(game.hero(1-seat)),0);
            assertEquals(1,RingOfEnergy.wandChargeMultiplier(hero),0);
            assertEquals(1.175,RingOfEnergy.wandChargeMultiplier(game.hero(1-seat)),.0001);
            SewerBeeTest.offer(game,seat);
            assertEquals(!curse,CoopFloorsTest.step(game,UNEQUIP,0,hero.belongings.misc));
            assertEquals(Math.pow(1.3,curse ? 0 : 2),RingOfAccuracy.accuracyMultiplier(hero),.0001);
            System.out.printf("PASS physical ring pair seat=%d cursed=%b ids=%d/%d peer=%d%n",seat,curse,firstId,secondId,otherId);
        }
    }

    public static void main(String[] args) throws Exception {
        var probe = new EquippedRingUse();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (int seat=0; seat<2; seat++) {
                probe.haste(seat);
                probe.combined(seat,false);
                probe.combined(seat,true);
            }
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }
}
