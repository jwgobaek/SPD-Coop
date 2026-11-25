package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Spinner;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.*;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class CavesTrapsTest extends HeadlessDungeon {
    @Test public void explosionConsumesBothBombsAndOwnedPotionOnceAndRetainsEquipment() {
        for (int seat=0;seat<2;seat++) {
            Bundle state;long weaponId;
            try(CoopSession g=CavesInteriorTest.open(570)) {
                CavesInteriorTest.arrive(g,12);CavesInteriorTest.stage(g,Spinner.class,seat);
                while(g.activeSeat()!=seat) TenguArenaTest.waitOne(g);
                Hero h=g.hero(seat),other=g.hero(1-seat);
                int cell=h.pos+1;
                other.pos=cell+Dungeon.level.width();
                com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb first=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb(),second=new com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb();
                Dungeon.level.drop(first,cell+1);
                Dungeon.level.drop(second,cell+2);
                com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation potion=new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation();
                g.ownership.claim(potion,g.player(seat));Dungeon.level.drop(potion,cell+2);
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword weapon=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
                g.ownership.claim(weapon,g.player(1-seat));weaponId=g.ownership.identity(weapon);
                Dungeon.level.drop(weapon,cell+2);
                Dungeon.level.setTrap(new ExplosiveTrap(),cell);Level.set(cell,Terrain.TRAP);g.observe(seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
                assertTrue(h.HP<h.HT);assertTrue(other.HP<other.HT);
                assertTrue(Blob.volumeAt(cell+2,ConfusionGas.class)>0);
                assertFalse(CoopSession.floorItems(Dungeon.level).containsValue(potion));
                assertFalse(CoopSession.floorItems(Dungeon.level).containsValue(first));
                assertFalse(CoopSession.floorItems(Dungeon.level).containsValue(second));
                state=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(state)) {
                long owner=g.player(1-seat);
                assertTrue(CoopSession.floorItems(Dungeon.level).values().stream().anyMatch(i->g.ownership.identity(i)==weaponId && g.ownership.owner(i)==owner));
            }
        }
    }

    @Test public void nativeAreaCloudsFromBothSeatsPersistAndContinueAfterSave() {
        for(int seat=0;seat<2;seat++) for(int n=0;n<3;n++) {
            Class<? extends Blob> type=n==0?Freezing.class:n==1?Electricity.class:CorrosiveGas.class;
            Bundle saved;int cell,id;
            try(CoopSession g=CavesInteriorTest.open(570)) {
                CavesInteriorTest.arrive(g,12);
                CavesInteriorTest.stage(g,Spinner.class,seat);
                while(g.activeSeat()!=seat) TenguArenaTest.waitOne(g);
                Hero h=g.hero(seat);
                cell=h.pos+1;
                Trap trap=n==0?new FrostTrap():n==1?new StormTrap():new CorrosionTrap();
                Dungeon.level.setTrap(trap,cell);Level.set(cell,Terrain.TRAP);trap.reveal();
                g.observe(seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
                assertFalse(trap.active);
                Blob b=Dungeon.level.blobs.get(type);
                assertNotNull(b);assertTrue(b.volume>0);id=b.id();
                assertTrue(Blob.volumeAt(cell,type)>0);
                if(n!=2) assertTrue(Blob.volumeAt(cell+1,type)>0);
                saved=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(saved)) {
                assertEquals(id,Dungeon.level.blobs.get(type).id());
                assertFalse(Dungeon.level.traps.get(cell).active);
                for(int i=0;i<5;i++) TenguArenaTest.waitOne(g);
                Hero h=g.hero(seat);
                assertTrue(h.HP<h.HT || h.buff(Chill.class)!=null || h.buff(Frost.class)!=null || h.buff(Corrosion.class)!=null);
            }
        }
    }
}
