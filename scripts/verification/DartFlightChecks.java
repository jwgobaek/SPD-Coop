package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.BlindingDart;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart;
import com.watabou.utils.Bundle;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Staged sewer shots with native hit rolls, followed by a real heap pickup after engine restoration. */
public class DartFlightChecks extends HeadlessDungeon {
    private static void check(int seat,boolean occupied) throws Exception {
        Bundle saved;
        long landedId;
        int cell,ratId;
        try(CoopSession g=CoopSession.open(415,11,HeroClass.WARRIOR)) {
            g.join(22,HeroClass.WARRIOR);
            Rat target=CoopMageTest.encounter(g,seat);
            target.HT=target.HP=500;
            ratId=target.id();
            Hero h=g.hero(seat);
            cell=occupied?target.pos:h.pos+2;
            if (occupied) {
                // A delayed, unaware rat otherwise grants a guaranteed surprise hit on every throw.
                target.aggro(h);
                java.lang.reflect.Field aware=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob.class.getDeclaredField("enemySeen");
                aware.setAccessible(true);
                aware.setBoolean(target,true);
                target.fieldOfView=new boolean[Dungeon.level.length()];
                Dungeon.level.updateFieldOfView(target,target.fieldOfView);
                assertTrue(target.defenseSkill(h)>0);
            }
            Dart stack=new Dart();
            stack.quantity(35);
            assertTrue(stack.collect(h.belongings.backpack));
            g.ownership.claim(stack,g.player(seat));
            boolean landed=false;
            for(int i=0;i<34 && !landed;i++) {
                SewerBeeTest.offer(g,seat);
                long before=g.turn();
                int health=target.HP,quantity=stack.quantity();
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.THROW,cell,stack));
                assertTrue(g.turn()>before);
                assertEquals(quantity-1,stack.quantity());
                Heap heap=Dungeon.level.heaps.get(cell);
                landed=heap!=null && heap.peek() instanceof Dart;
                if (landed) assertEquals(health,target.HP);
            }
            assertTrue("native floor landing",landed);
            Dart drop=(Dart)Dungeon.level.heaps.get(cell).peek();
            landedId=g.ownership.identity(drop);
            assertEquals(1,drop.quantity());
            assertEquals(Dart.MAX_DURABILITY,drop.durabilityLeft(),0f);
            assertEquals(g.player(seat),g.ownership.owner(drop));
            saved=g.checkpoint();
        }
        try(CoopSession g=CoopSession.restore(saved)) {
            if (occupied) {
                // Move the living fixture target off the heap, preserving any independently pinned darts.
                Rat rat=(Rat)Actor.findById(ratId);
                rat.pos+=Dungeon.level.width();
            }
            Dart drop=(Dart)Dungeon.level.heaps.get(cell).peek();
            assertEquals(landedId,g.ownership.identity(drop));
            SewerBeeTest.offer(g,1-seat);
            Hero other=g.hero(1-seat);
            int origin=other.pos;
            other.pos=cell; g.observe(1-seat);
            assertFalse(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,cell,drop));
            other.pos=origin;
            SewerBeeTest.offer(g,seat);
            Hero h=g.hero(seat);
            h.pos=cell;g.observe(seat);
            Dart stack=h.belongings.getItem(Dart.class);
            int prior=stack.quantity();
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,cell,drop));
            assertEquals(prior+1,stack.quantity());
            g.checkpoint();
            System.out.printf("PASS seat=%d target=%s originalDurability=100 recovered=1%n",seat,occupied?"native-miss":"empty");
        }
    }

    private static void prepareShared(Path file, int seat) throws Exception {
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.HUNTRESS)) {
            game.join(22, HeroClass.HUNTRESS);
            Hero user = TierGrowthTest.prepare(game, seat, HeroSubClass.SNIPER, Talent.SHARED_ENCHANTMENT, 3);
            Rat rat = CoopMageTest.encounter(game, seat);
            rat.HP = rat.HT = 700;
            Buff.prolong(rat, Paralysis.class, 800f);
            SpiritBow bow = user.belongings.getItem(SpiritBow.class);
            StoneOfEnchantment stones = new StoneOfEnchantment();
            stones.quantity(80);
            Dungeon.level.drop(stones, user.pos);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.PICK_UP, user.pos, stones));
            int used = 0;
            while (!(bow.enchantment instanceof Chilling) && used < 80) {
                SewerBeeTest.offer(game, seat);
                StoneOfEnchantment held = user.belongings.getItem(StoneOfEnchantment.class);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ENCHANT_BOW, SharedGrowthTest.selection(user, bow), held));
                used++;
            }
            assertTrue(bow.enchantment instanceof Chilling);
            assertNull(game.hero(1-seat).belongings.getItem(SpiritBow.class).enchantment);
            Dart dart = seat == 0 ? new BlindingDart() : new PoisonDart();
            dart.quantity(30).identify(false);
            Dungeon.level.drop(dart, user.pos);
            SewerBeeTest.offer(game, seat);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.PICK_UP, user.pos, dart));
            SewerBeeTest.offer(game, seat);
            Bundle fixture = new Bundle();
            fixture.put("game", game.checkpoint());
            fixture.put("target", rat.id());
            fixture.put("bow", game.ownership.identity(bow));
            try (OutputStream output = Files.newOutputStream(file)) { assertTrue(Bundle.write(fixture, output)); }
            System.out.println("STONE seat=" + seat + " consumed=" + used + " result=Chilling");
        }
    }

    private static void replayShared(Path file, int seat) throws Exception {
        Bundle fixture;
        try (InputStream input = Files.newInputStream(file)) { fixture = Bundle.read(input); }
        try (CoopSession game = CoopSession.restore(fixture.getBundle("game"))) {
            Hero user = game.hero(seat);
            SpiritBow bow = user.belongings.getItem(SpiritBow.class);
            assertEquals(fixture.getLong("bow"), game.ownership.identity(bow));
            assertTrue(bow.enchantment instanceof Chilling);
            Rat rat = (Rat)Actor.findById(fixture.getInt("target"));
            boolean chilled = false, tipped = false;
            int used = 0;
            while (!(chilled && tipped) && used < 25) {
                SewerBeeTest.offer(game, seat);
                Dart dart = seat == 0 ? user.belongings.getItem(BlindingDart.class) : user.belongings.getItem(PoisonDart.class);
                int before = dart.quantity();
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.THROW, rat.pos, dart));
                assertEquals(before-1, dart.quantity());
                chilled |= rat.buff(Chill.class) != null;
                tipped |= seat == 0 ? rat.buff(Blindness.class) != null : rat.buff(Poison.class) != null;
                used++;
            }
            assertTrue(chilled && tipped);
            PinCushion cushion = rat.buff(PinCushion.class);
            assertNotNull(cushion);
            MissileWeapon pinned = cushion.getStuckItems().get(0);
            assertEquals(game.player(seat), game.ownership.owner(pinned));
            long id = game.ownership.identity(pinned);
            assertTrue(id > 0);
            assertNull(game.hero(1-seat).belongings.getItem(SpiritBow.class).enchantment);
            Bundle state = game.checkpoint();
            System.out.println("REPLAY seat=" + seat + " shots=" + used + " targetHP=" + rat.HP
                    + " bowID=" + fixture.getLong("bow") + " pinnedID=" + id + " durability=" + pinned.durabilityLeft()
                    + " tippedAndChilled=true rng=" + Arrays.toString(state.getLongArray("random")));
        }
    }

    public static void main(String[] args) throws Exception {
        DartFlightChecks test=new DartFlightChecks();
        test.temporary.create();test.startBackend();
        try {
            if (args.length == 0) {
                for (int seat=0; seat<2; seat++) { check(seat,false); check(seat,true); }
            } else if (args[0].equals("--prepare-shared")) {
                prepareShared(Path.of(args[1]), Integer.parseInt(args[2]));
            } else if (args[0].equals("--replay-shared")) {
                replayShared(Path.of(args[1]), Integer.parseInt(args[2]));
            } else throw new IllegalArgumentException("Expected --prepare-shared or --replay-shared");
        }
        finally {test.stopBackend();test.temporary.delete();}
    }
}
