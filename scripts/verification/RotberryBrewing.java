package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Rotberry;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import static org.junit.Assert.*;

public class RotberryBrewing extends HeadlessDungeon {
    private static void verify(int seat) {
        Bundle save;
        try (CoopSession game=CoopSession.open(577,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.MAGE);
            WandmakerTest.prepareReturn(game,577,seat);
            Hero owner=game.hero(seat);
            Wandmaker npc=SewerSpecialActorTest.actor(Wandmaker.class);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ACCEPT_WANDMAKER,npc.pos,null));
            assertTrue(Wandmaker.Quest.offerFor(owner).startsWith("berry,REWARD,"));
            Rotberry.Seed questSeed=owner.belongings.getItem(Rotberry.Seed.class);
            assertEquals(1,questSeed.quantity());
            long id=game.ownership.identity(questSeed);
            Firebloom.Seed other=new Firebloom.Seed();
            MultiAlchemyTest.pick(game,seat,other.quantity(2));
            int pot=-1;
            for (int off: PathFinder.NEIGHBOURS8) {
                int cell=owner.pos+off;
                if (Dungeon.level.passable[cell] && Actor.findChar(cell)==null && Dungeon.level.traps.get(cell)==null) {
                    pot=cell;break;
                }
            }
            assertTrue(pot>=0);
            Level.set(pot,Terrain.ALCHEMY);
            game.observe(seat);
            assertTrue(MultiAlchemyTest.brew(game,seat,pot,questSeed,other,other));
            assertNull(owner.belongings.getItem(Rotberry.Seed.class));
            assertEquals("berry,HUNT",Wandmaker.Quest.offerFor(owner));
            save=game.checkpoint();
            System.out.printf("seat=%d questSeed=%d consumed, native reward unavailable%n",seat,id);
        }
        try (CoopSession restored=CoopSession.restore(save)) {
            assertEquals("berry,HUNT",Wandmaker.Quest.offerFor(restored.hero(seat)));
            assertNull(restored.hero(seat).belongings.getItem(Rotberry.Seed.class));
        }
    }
    public static void main(String[] args) throws Exception {
        RotberryBrewing fixture=new RotberryBrewing();
        fixture.temporary.create();fixture.startBackend();
        try { verify(0);verify(1); } finally { fixture.stopBackend();fixture.temporary.delete(); }
    }
}
