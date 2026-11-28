package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Bundle;

import static org.junit.Assert.*;

public class CorrosionLifecycle extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        CorrosionLifecycle trial = new CorrosionLifecycle();
        trial.temporary.create();
        trial.startBackend();
        try {
            for (int seat = 0; seat < 2; seat++) {
                trial.followCloud(seat, true);
                trial.followCloud(seat, false);
                trial.afterCasterDeath(seat);
                trial.peerEntersExistingGas(seat);
                trial.existingDamageAcrossSourceChanges(seat);
            }
        } finally {
            trial.stopBackend();
            trial.temporary.delete();
        }
    }

    private void followCloud(int seat, boolean sameCell) {
        int gasId;
        int ownerId;
        Bundle saved;
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
            game.join(22, HeroClass.MAGE);
            CoopMageTest.encounter(game, seat);
            Hero owner = game.hero(seat);
            Hero other = game.hero(1-seat);
            other.HP = other.HT = owner.HP = owner.HT = 200;
            int first = owner.pos;
            int second = sameCell ? first : first + 2;
            WandOfCorrosion wand = new WandOfCorrosion();
            wand.onZap(owner, new Ballistica(owner.pos, first, Ballistica.STOP_TARGET));
            wand.onZap(other, new Ballistica(other.pos, second, Ballistica.STOP_TARGET));
            CorrosiveGas gas = gas();
            assertEquals(100, gas.volume);
            assertEquals(sameCell ? 100 : 50, gas.cur[first]);
            assertEquals(sameCell ? 100 : 50, gas.cur[second]);
            ownerId = owner.id();
            gasId = gas.id();
            for (int i = 0; i < 4 && gas.cur[first+1] == 0; i++) TenguArenaTest.waitOne(game);
            assertTrue(gas.cur[first+1] > 0);
            assertEquals(ownerId, state(gas).getInt("coop_caster"));
            SewerBeeTest.cross(game, Dungeon.level.exit());
            assertEquals(2, Dungeon.depth);
            assertNull(Actor.findById(gasId));
            saved = game.checkpoint();
        }
        try (CoopSession game = CoopSession.restore(saved)) {
            assertEquals(2, Dungeon.depth);
            SewerBeeTest.cross(game, Dungeon.level.entrance());
            assertEquals(1, Dungeon.depth);
            CorrosiveGas gas = gas();
            assertSame(gas, Actor.findById(gasId));
            assertEquals(ownerId, state(gas).getInt("coop_caster"));
            for (int i = 0; i < 160 && gas.volume > 0; i++) TenguArenaTest.waitOne(game);
            assertEquals(0, gas.volume);
            assertEquals(0, state(gas).getInt("coop_caster"));
            assertEquals(0, state(gas).getInt("strength"));
            System.out.println("PASS gas seat=" + seat + " sameCell=" + sameCell + " dormant-floor return and expiry");
        }
    }

    private void afterCasterDeath(int seat) {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
            game.join(22, HeroClass.MAGE);
            Rat rat = CoopMageTest.encounter(game, seat);
            Hero caster = game.hero(seat);
            Hero peer = game.hero(1-seat);
            int originalId = caster.id();
            new WandOfCorrosion().onZap(caster, new Ballistica(caster.pos, rat.pos, Ballistica.STOP_TARGET));
            for (int i = 0; i < 8 && rat.buff(Corrosion.class) == null; i++) TenguArenaTest.waitOne(game);
            Corrosion dot = rat.buff(Corrosion.class);
            assertNotNull(dot);
            assertSame(caster, dot.caster());
            caster.damage(caster.HP + 100, rat);
            game.finishEffects();
            assertEquals(CoopSession.Life.DEAD, game.life(seat));
            assertEquals(originalId, state(gas()).getInt("coop_caster"));
            int exp = peer.exp;
            rat.HP = 1;
            dot.act();
            assertFalse(rat.isAlive());
            assertFalse(rat.wasDefeatedBy(peer));
            assertEquals(exp, peer.exp);
            System.out.println("PASS lethal DOT after caster death seat=" + seat + " no transfer of kill or XP");
        }
    }

    private void peerEntersExistingGas(int seat) {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
            game.join(22, HeroClass.MAGE);
            CoopMageTest.encounter(game, seat);
            Hero caster = game.hero(seat);
            int peerSeat = 1 - seat;
            Hero peer = game.hero(peerSeat);
            SewerBeeTest.offer(game, peerSeat);
            int landing = peer.pos + 1;
            new WandOfCorrosion().onZap(caster, new Ballistica(caster.pos, landing, Ballistica.STOP_TARGET));
            assertEquals(0, gas().cur[peer.pos]);
            assertEquals(50, gas().cur[landing]);
            int before = peer.HP;
            assertTrue(game.act(game.player(peerSeat), game.turn(), CoopSession.Action.MOVE, landing, null));
            assertEquals(landing, peer.pos);
            for (int i = 0; i < 8 && peer.HP == before; i++) TenguArenaTest.waitOne(game);
            assertTrue(peer.HP < before);
            Corrosion effect = peer.buff(Corrosion.class);
            assertNotNull(effect);
            assertSame(caster, effect.caster());
            System.out.println("PASS peer entered existing player gas seat=" + peerSeat + " HP " + before + "->" + peer.HP);
        }
    }

    private void existingDamageAcrossSourceChanges(int seat) {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
            game.join(22, HeroClass.MAGE);
            CoopMageTest.encounter(game, seat);
            Hero first = game.hero(seat), peer = game.hero(1-seat);
            WandOfCorrosion wand = new WandOfCorrosion();
            wand.onZap(first, new Ballistica(first.pos, peer.pos, Ballistica.STOP_TARGET));
            CorrosiveGas cloud = gas();
            cloud.act();
            Corrosion dot = peer.buff(Corrosion.class);
            assertNotNull(dot);
            assertSame(first, dot.caster());
            int id = dot.id();
            wand.upgrade(2);
            wand.onZap(peer, new Ballistica(peer.pos, peer.pos, Ballistica.STOP_TARGET));
            assertEquals(peer.id(), state(cloud).getInt("coop_caster"));
            assertSame(first, dot.caster());
            int hp = peer.HP;
            dot.act();
            assertTrue(peer.HP < hp);
            assertSame(first, dot.caster());
            cloud.act();
            assertSame(dot, peer.buff(Corrosion.class));
            assertSame(peer, dot.caster());
            cloud.setStrength(8, com.shatteredpixel.shatteredpixeldungeon.levels.traps.CorrosionTrap.class);
            assertSame(peer, dot.caster());
            cloud.act();
            assertEquals(id, peer.buff(Corrosion.class).id());
            assertNull(dot.caster());
            System.out.printf("PASS seat=%d DOT=%d HP=%d->%d retains old caster until re-exposure; environment clears it%n",
                    seat, id, hp, peer.HP);
        }
    }

    private static CorrosiveGas gas() {
        return (CorrosiveGas) Dungeon.level.blobs.get(CorrosiveGas.class);
    }

    private static Bundle state(CorrosiveGas gas) {
        Bundle state = new Bundle();
        gas.storeInBundle(state);
        return state;
    }
}
