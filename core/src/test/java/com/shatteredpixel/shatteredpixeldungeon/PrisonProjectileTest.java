package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrisonProjectileTest extends HeadlessDungeon {
    @Test public void bothOwnersKeepTheirSeparateNativeMissilesInsideOneDormantMonster() {
        checkDormantProjectiles(false);
    }

    @Test public void separateDartsKeepOwnersAndUnlimitedDurabilityThroughDormancyAndPickup() {
        checkDormantProjectiles(true);
    }

    @Test public void chargedThrowsUseTheActualShootersBowLevelEnchantmentAndBuff() {
        for (int seat=0;seat<2;seat++) for (int equipped=0;equipped<4;equipped++) {
            Bundle checkpoint;
            int enemyId;
            try (CoopSession g=CoopSession.open(415,11,HeroClass.WARRIOR)) {
                g.join(22,HeroClass.WARRIOR);
                Rat enemy=CoopMageTest.encounter(g,seat);
                enemyId=enemy.id();
                for (int i=0;i<2;i++) {
                    Hero h=g.hero(i);
                    if ((equipped & (1 << i))!=0) {
                        Crossbow bow=new Crossbow();
                        bow.level(i==0 ? 1 : 9);
                        bow.identify(false);
                        assertTrue(bow.collect(h.belongings.backpack));
                        SewerBeeTest.offer(g,i);
                        assertTrue(CoopFloorsTest.step(g,CoopSession.Action.EQUIP,0,bow));
                        // Exercise two native enchantments in an isolated encounter.
                        bow.enchantment=i==0 ? new Projecting() : new Kinetic();
                    }
                    Dart darts=new Dart();
                    darts.quantity(8);
                    assertTrue(darts.collect(h.belongings.backpack));
                    Buff.affect(h,Crossbow.ChargedShot.class);
                }
                checkpoint=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(checkpoint)) {
                Rat enemy=(Rat)Actor.findById(enemyId);
                Hero h=g.hero(seat), ally=g.hero(1-seat);
                Dart darts=h.belongings.getItem(Dart.class);
                boolean hasBow=(equipped & (1 << seat))!=0;
                assertEquals(hasBow && seat==0,darts.hasEnchant(Projecting.class,h));
                assertEquals(hasBow && seat==1,darts.hasEnchant(Kinetic.class,h));
                assertEquals(hasBow,darts.crossbowHasEnchant(h));
                int hp=enemy.HP;
                for(int i=0;i<8 && enemy.HP==hp;i++) {
                    SewerBeeTest.offer(g,seat);
                    assertTrue(CoopFloorsTest.step(g,CoopSession.Action.THROW,enemy.pos,darts));
                }
                assertTrue(enemy.HP<hp);
                if (hasBow) {
                    int level=seat==0 ? 1 : 9;
                    assertTrue("charged bow lower bound "+(hp-enemy.HP),hp-enemy.HP>=4+level+4+level-2);
                }
                assertNull(h.buff(Crossbow.ChargedShot.class));
                assertNotNull(ally.buff(Crossbow.ChargedShot.class));
                g.checkpoint();
            }
        }
    }

    private void checkDormantProjectiles(boolean darts) {
        Bundle parked;
        int ratId;
        long[] ids=new long[2];
        float[] durability=new float[2];
        try (CoopSession game=CoopSession.open(415,1,HeroClass.WARRIOR)) {
            game.join(2,HeroClass.WARRIOR);
            Rat rat=CoopMageTest.encounter(game,0);
            ratId=rat.id();
            for (int seat=0;seat<2;seat++) {
                MissileWeapon knife=darts ? new Dart() : new ThrowingKnife();
                if (darts) knife.quantity(8);
                knife.setID=darts ? 0 : 881; // Deliberately collide the native stack key across owners.
                int initial=knife.quantity(); knife.identify(false);
                assertTrue(knife.collect(game.hero(seat).belongings.backpack));
                for (int n=0;n<initial;n++) {
                    SewerBeeTest.offer(game,seat);
                    game.observe(seat);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,knife));
                    PinCushion cushion=rat.buff(PinCushion.class);
                    if (cushion!=null && cushion.getStuckItems().size()==seat+1) break;
                }
                assertNotNull(rat.buff(PinCushion.class));
                assertEquals(seat+1,rat.buff(PinCushion.class).getStuckItems().size());
                MissileWeapon stuck=rat.buff(PinCushion.class).getStuckItems().get(seat);
                if (darts) assertEquals(MissileWeapon.MAX_DURABILITY,stuck.durabilityLeft(),0f);
                ids[seat]=game.ownership.identity(stuck); durability[seat]=stuck.durabilityLeft();
                assertNotEquals(game.ownership.identity(knife),ids[seat]);
                assertEquals(game.player(seat),game.ownership.owner(stuck));
                assertFalse(game.hero(seat).belongings.contains(stuck));
                assertTrue(knife.quantity()<initial);
            }
            assertNotEquals(ids[0],ids[1]);
            SewerDepthTest.gate(game,true);
            assertEquals(2,Dungeon.depth); assertNull(Actor.findById(ratId));
            parked=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(parked)) {
            assertNull(Actor.findById(ratId));
            SewerDepthTest.gate(game,false);
            Rat rat=(Rat)Actor.findById(ratId);
            assertNotNull(rat);
            int cell=rat.pos;
            for (int seat=0;seat<2;seat++) {
                MissileWeapon stuck=rat.buff(PinCushion.class).getStuckItems().get(seat);
                assertEquals(ids[seat],game.ownership.identity(stuck));
                assertEquals(game.player(seat),game.ownership.owner(stuck));
                assertEquals(durability[seat],stuck.durabilityLeft(),0f);
            }
            rat.die(game.hero(0)); // Isolate original death/drop independently of its combat HP.
            Heap heap=Dungeon.level.heaps.get(cell);
            for (int seat=0;seat<2;seat++) {
                long id=ids[seat];
                Item drop=heap.items.stream().filter(i->game.ownership.identity(i)==id).findFirst().get();
                assertEquals(game.player(seat),game.ownership.owner(drop));
                assertFalse(drop.collect(game.hero(1-seat).belongings.backpack));
            }
            // The uppermost recovered knife decides whose native pickup can succeed first.
            for (int n=0;n<2;n++) {
                Item top=heap.peek();
                int seat=game.ownership.owner(top)==game.player(0) ? 0 : 1;
                SewerBeeTest.offer(game,seat);
                game.hero(seat).pos=cell; game.observe(seat);
                MissileWeapon stack=darts ? game.hero(seat).belongings.getItem(Dart.class) : game.hero(seat).belongings.getItem(ThrowingKnife.class);
                int original=stack.quantity();
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,top));
                assertEquals(original+1,stack.quantity());
                assertFalse(heap.items.contains(top));
                game.hero(seat).pos=cell+Dungeon.level.width(); game.observe(seat);
            }
            game.checkpoint();
        }
    }
}
