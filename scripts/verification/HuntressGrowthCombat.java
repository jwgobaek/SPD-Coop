package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnipersMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Isolated owner-only choice and real host MOVE / THROW. No production subclass selection. */
public class HuntressGrowthCombat extends HeadlessDungeon {
    private static void exercise(int seat, HeroSubClass choice) {
        Bundle checkpoint;
        int effectId;
        int value;
        try (CoopSession game=CoopSession.open(578,11,HeroClass.HUNTRESS)) {
            game.join(22,HeroClass.HUNTRESS);
            int cell=TenguArenaTest.prepareSocket(game,"claim");
            SewerBeeTest.offer(game,seat);
            Hero owner=game.hero(seat);
            if (game.hero(1-seat).pos==cell) CavesBossTest.beside(game,1-seat,cell);
            owner.pos=cell;
            game.observe(seat);
            TengusMask mask=(TengusMask)Dungeon.level.heaps.get(cell).peek();
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,mask));
            assertEquals(game.player(seat),game.ownership.owner(mask));
            WarriorGrowthCombat.Callback.select(mask,owner,choice);
            SewerDepthTest.gate(game,false);
            SewerBeeTest.offer(game,seat);
            if (choice==HeroSubClass.WARDEN) {
                int plant=SewerPlantTest.arrange(game,seat,new Earthroot.Seed(),true);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,plant,null));
                Barkskin bark=owner.buff(Barkskin.class);
                assertNotNull(bark);
                assertNull(game.hero(1-seat).buff(Barkskin.class));
                assertNull(Dungeon.level.plants.get(plant));
                value=bark.level(); effectId=bark.id();
                assertTrue(value>0);
            } else {
                Guard guard=CavesInteriorTest.stage(game,Guard.class,seat);
                ThrowingStone stones=new ThrowingStone();
                stones.quantity(8);
                Dungeon.level.drop(stones,owner.pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,owner.pos,stones));
                for (int tries=0;tries<8 && owner.buff(SnipersMark.class)==null;tries++) {
                    SewerBeeTest.offer(game,seat);
                    stones=owner.belongings.getItem(ThrowingStone.class);
                    assertNotNull(stones);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,guard.pos,stones));
                }
                SnipersMark mark=owner.buff(SnipersMark.class);
                assertNotNull(mark);
                assertNull(game.hero(1-seat).buff(SnipersMark.class));
                value=guard.id(); effectId=mark.id();
                assertEquals(value,mark.object);
            }
            checkpoint=game.checkpoint();
        }
        try (CoopSession restored=CoopSession.restore(checkpoint)) {
            Hero owner=restored.hero(seat);
            assertEquals(choice,owner.subClass);
            assertEquals(HeroSubClass.NONE,restored.hero(1-seat).subClass);
            assertNull(owner.belongings.getItem(TengusMask.class));
            if (choice==HeroSubClass.WARDEN) {
                assertSame(owner.buff(Barkskin.class),Actor.findById(effectId));
                assertEquals(value,owner.buff(Barkskin.class).level());
            } else {
                assertSame(owner.buff(SnipersMark.class),Actor.findById(effectId));
                assertEquals(value,owner.buff(SnipersMark.class).object);
                assertTrue(Actor.findById(value) instanceof Guard);
            }
            System.out.printf("HUNTRESS_TRIAL\t%d\t%s\t%d\t%d%n",seat,choice,effectId,value);
        }
    }
    public static void main(String[] args) throws Exception {
        HuntressGrowthCombat probe=new HuntressGrowthCombat();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (int seat=0;seat<2;seat++) for (String c:args) exercise(seat,HeroSubClass.valueOf(c));
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
