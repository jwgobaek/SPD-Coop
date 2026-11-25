package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class CavesRareTest extends HeadlessDungeon {
    @Test public void rangedThreatPersistsAndOriginalDm201RetaliatesAtEitherSeat() {
        for(int seat=0;seat<2;seat++) {
            Bundle state; int id,pos;
            try(CoopSession g=CavesInteriorTest.open(581)) {
                CavesInteriorTest.arrive(g,13);
                DM201 machine=CavesInteriorTest.stage(g,DM201.class,seat);
                id=machine.id();pos=machine.pos;
                machine.damage(1,g.hero(seat));
                state=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(state)) {
                DM201 m=(DM201)Actor.findById(id);
                for(int i=0;i<30 && Dungeon.level.blobs.get(CorrosiveGas.class)==null;i++) TenguArenaTest.waitOne(g);
                CorrosiveGas gas=(CorrosiveGas)Dungeon.level.blobs.get(CorrosiveGas.class);
                assertNotNull(gas);assertTrue(gas.cur[g.hero(seat).pos]>0);assertEquals(0,gas.cur[g.hero(1-seat).pos]);
                assertEquals(pos,m.pos);m.die(g.hero(seat));
                assertTrue(CoopSession.floorItems(Dungeon.level).values().stream().anyMatch(MetalShard.class::isInstance));
            }
        }
    }

    @Test public void generatedStatueWeaponsRetainNativeProcReachTimeAndPhysicalDrop() {
        for(int seed:new int[]{573,576,578}) for(int seat=0;seat<2;seat++) {
            int depth=seed==578?14:12;
            try(CoopSession g=CavesInteriorTest.open(seed)) {
                CavesInteriorTest.arrive(g,depth);
                Statue m=CavesInteriorTest.stage(g,Statue.class,seat);
                Weapon weapon=m.weapon();long id=g.ownership.identity(weapon);
                String expected=seed==573?"AssassinsBlade":seed==576?"Scimitar":"Longsword";
                assertEquals(expected,weapon.getClass().getSimpleName());
                assertEquals(weapon.delayFactor(m),m.attackDelay(),0.001f);
                Hero victim=g.hero(seat);victim.pos=m.pos+1;
                int before=victim.pos;
                if(weapon.enchantment instanceof Kinetic) {
                    m.attackProc(victim,10);
                    assertNotNull(m.buff(Kinetic.KineticTracker.class));
                } else {
                    for(int i=0;i<80 && victim.pos==before;i++) {
                        m.attackProc(victim,10);TenguArenaTest.waitOne(g);
                    }
                    assertNotEquals(before,victim.pos);
                }
                m.die(victim);
                assertEquals(1,CoopSession.floorItems(Dungeon.level).values().stream().filter(i->g.ownership.identity(i)==id).count());
            }
        }
    }
}
