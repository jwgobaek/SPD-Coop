package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

public class DartGearChange extends HeadlessDungeon {
    private static void shot(int seat, boolean shooterCharged, boolean switchGear) {
        Bundle state;
        int targetId;
        try (CoopSession g=CoopSession.open(415,1,HeroClass.WARRIOR)) {
            g.join(2,HeroClass.WARRIOR);
            Rat target=CoopMageTest.encounter(g,seat);
            targetId=target.id();
            for (int player=0;player<2;player++) {
                Hero h=g.hero(player);
                Crossbow bow=new Crossbow();
                bow.level(player+3); bow.identify(false);
                assertTrue(bow.collect(h.belongings.backpack));
                SewerBeeTest.offer(g,player);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.EQUIP,0,bow));
                Dart darts=new Dart();
                darts.quantity(8);
                assertTrue(darts.collect(h.belongings.backpack));
                if (player==(shooterCharged ? seat : 1-seat)) Buff.affect(h,Crossbow.ChargedShot.class);
            }
            if (switchGear) {
                SewerBeeTest.offer(g,seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.UNEQUIP,0,g.hero(seat).belongings.weapon()));
            }
            state=g.checkpoint();
        }
        try (CoopSession g=CoopSession.restore(state)) {
            Hero h=g.hero(seat),ally=g.hero(1-seat);
            Dart darts=h.belongings.getItem(Dart.class);
            Rat target=(Rat)Actor.findById(targetId);
            assertEquals(shooterCharged && !switchGear,darts.accuracyFactor(h,target)==Char.INFINITE_ACCURACY);
            SewerBeeTest.offer(g,seat);
            int health=target.HP;
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.THROW,target.pos,darts));
            if(shooterCharged && !switchGear) assertTrue(target.HP<health);
            assertNull(h.buff(Crossbow.ChargedShot.class));
            assertEquals(!shooterCharged,ally.buff(Crossbow.ChargedShot.class)!=null);
            g.checkpoint();
            System.out.printf("PASS seat=%d charged_seat=%d bow_unequipped=%s target_hp_loss=%d%n",seat,shooterCharged?seat:1-seat,switchGear,health-target.HP);
        }
    }
    public static void main(String[] args) throws Exception {
        DartGearChange test=new DartGearChange();test.temporary.create();test.startBackend();
        try {
            for(int seat=0;seat<2;seat++) for(boolean charged:new boolean[]{false,true})
                for(boolean changed:new boolean[]{false,true}) shot(seat,charged,changed);
        } finally {test.stopBackend();test.temporary.delete();}
    }
}
