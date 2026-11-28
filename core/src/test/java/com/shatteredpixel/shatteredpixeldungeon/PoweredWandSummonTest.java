package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class PoweredWandSummonTest extends HeadlessDungeon {
    static Mob conjure(CoopSession game, int seat, boolean stationary) {
        Mob rat = CoopMageTest.encounter(game, seat);
        Hero owner = game.hero(seat);
        Wand wand = stationary ? new WandOfWarding() : new WandOfLivingEarth();
        wand.upgrade(5);
        wand.curCharges = wand.maxCharges;
        WandArtsTest.give(game, seat, wand);
        if (stationary) {
            rat.destroy();
            SewerBeeTest.offer(game, seat);
            assertTrue(CoopFloorsTest.step(game, ZAP, owner.pos+2, wand));
            return Dungeon.level.mobs.stream().filter(m -> m instanceof WandOfWarding.Ward && CoopSession.summonerOf(m) == owner).findFirst().orElseThrow();
        }
        while (wand.curCharges > 0 && WandOfLivingEarth.activeGuardian(owner) == null) {
            SewerBeeTest.offer(game, seat);
            assertTrue(CoopFloorsTest.step(game, ZAP, rat.pos, wand));
        }
        assertNotNull(WandOfLivingEarth.activeGuardian(owner));
        return WandOfLivingEarth.activeGuardian(owner);
    }

    static void empower(CoopSession game, int seat, Mob ally) {
        Hero owner = game.hero(seat);
        var armor = ArmorTalentsTest.invest(game, seat, 2, Talent.STASIS, 1);
        TierGrowthTest.powerTome(owner);
        SewerBeeTest.offer(game, seat);
        assertTrue(CoopFloorsTest.step(game, ARMOR_ABILITY, ally.pos, armor));
        assertSame(ally, PowerOfMany.getPoweredAlly(owner));
        assertTrue(ally.buff(PowerOfMany.PowerBuff.class).belongsTo(owner));
        assertNull(PowerOfMany.getPoweredAlly(game.hero(1-seat)));
    }

    static void hold(CoopSession game, int seat, Mob ally) {
        Hero owner = game.hero(seat);
        HolyTome tome = owner.belongings.getItem(HolyTome.class);
        SewerBeeTest.offer(game, seat);
        int before = tome.charges();
        assertTrue(CoopFloorsTest.step(game, STASIS, owner.pos, tome));
        assertEquals(before-2, tome.charges());
        assertSame(ally, Stasis.getStasisAlly(owner));
        assertFalse(Dungeon.level.mobs.contains(ally));
        assertNull(Actor.findById(ally.id()));
    }

    static StagedInput prepare(CoopSession game, int seat, String stage) {
        Mob ally = conjure(game, seat, stage.startsWith("ward"));
        empower(game, seat, ally);
        if (stage.contains("free")) hold(game, seat, ally);
        SewerBeeTest.offer(game, seat);
        return new StagedInput(game, "STASIS", game.hero(seat).pos);
    }

    static boolean published(CoopSession game, int seat, String stage) {
        Hero owner = game.hero(seat);
        Mob held = Stasis.getStasisAlly(owner);
        Mob ally = stage.contains("hold") ? held : (Mob) PowerOfMany.getPoweredAlly(owner);
        if (ally == null || CoopSession.summonerOf(ally) != owner || !ally.buff(PowerOfMany.PowerBuff.class).belongsTo(owner))
            return false;
        if (stage.startsWith("ward") != (ally instanceof WandOfWarding.Ward)) return false;
        return stage.contains("hold") ? Actor.findById(ally.id()) == null : held == null && Actor.findById(ally.id()) == ally;
    }

    @Test public void nativeWandSummonsRestoreAndReleaseForTheirOwnCaster() {
        for (int seat=0; seat<2; seat++) for (boolean ward : new boolean[]{false,true}) {
            int id, hp;
            Bundle saved;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
                game.join(22, HeroClass.CLERIC);
                Mob ally = conjure(game, seat, ward);
                empower(game, seat, ally);
                hold(game, seat, ally);
                id=ally.id(); hp=ally.HP;
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero owner=game.hero(seat);
                Mob ally=Stasis.getStasisAlly(owner);
                assertEquals(id, ally.id()); assertEquals(hp, ally.HP);
                assertSame(owner, CoopSession.summonerOf(ally));
                assertNull(Stasis.getStasisAlly(game.hero(1-seat)));
                HolyTome tome=owner.belongings.getItem(HolyTome.class);
                SewerBeeTest.offer(game,seat);
                int before=tome.charges();
                assertTrue(CoopFloorsTest.step(game,STASIS,owner.pos,tome));
                assertEquals(before,tome.charges());
                assertSame(ally,Actor.findById(id));
                assertSame(ally,PowerOfMany.getPoweredAlly(owner));
                game.validateActorGraph();
            }
        }
    }

    @Test public void heldWardRemainsOnItsOriginalFloorWithItsPowerOwner() {
        for (int seat=0; seat<2; seat++) {
            Bundle saved;
            int id;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Mob ward=conjure(game,seat,true);
                empower(game,seat,ward);
                hold(game,seat,ward);
                id=ward.id();
                SewerSpecialActorTest.atGate(game,Dungeon.level.exit());
                CoopFloorsTest.cross(game);
                assertNull(Actor.findById(id));
                assertNull(Stasis.getStasisAlly(game.hero(seat)));
                assertTrue(game.stairs.hasParkedPower(game.hero(seat)));
                assertFalse(game.stairs.hasParkedPower(game.hero(1-seat)));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
                CoopFloorsTest.cross(game);
                assertEquals(id,PowerOfMany.getPoweredAlly(game.hero(seat)).id());
                game.validateActorGraph();
            }
        }
    }
}
