package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class YogBoundaryTest extends HeadlessDungeon {
    @Test public void rootedWindupThenOneRayThroughBothHeroesSurvivesRestore() {
        Bundle saved;
        int[] aim;
        int targetedSeat;
        try (CoopSession game=CavesInteriorTest.open(578)) {
            YogArenaTest.prepareBoundary(game,"warning");
            YogDzewa boss=YogArenaTest.resident(YogDzewa.class);
            Bundle data=new Bundle();
            boss.storeInBundle(data);
            targetedSeat=game.hero(0).id()==data.getInt("beam_target")?0:1;
            Hero quarry=game.hero(targetedSeat);
            assertEquals(quarry.id(),data.getInt("beam_target"));
            aim=boss.aimedCells();
            assertEquals(1,aim.length);
            Ballistica ray=new Ballistica(boss.pos,aim[0],Ballistica.WONT_STOP);
            boolean placed=false;
            for (int cell:ray.path) {
                if (Actor.findChar(cell)==null && Dungeon.level.passable[cell] && !Dungeon.level.pit[cell]) {
                    game.hero(1-targetedSeat).pos=cell;
                    placed=true;break;
                }
            }
            assertTrue(placed);
            assertTrue(ray.path.contains(quarry.pos));
            Buff.affect(quarry,Roots.class,8);
            for (int seat=0;seat<2;seat++) game.observe(seat);
            for (int i=0;i<3;i++) TenguArenaTest.waitOne(game);
            assertTrue(quarry.rooted);
            assertArrayEquals(aim,boss.aimedCells());
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            YogDzewa boss=YogArenaTest.resident(YogDzewa.class);
            assertArrayEquals(aim,boss.aimedCells());
            assertTrue(game.hero(targetedSeat).rooted);
            int points=Statistics.bossScores[4];
            int a=game.hero(0).HP,b=game.hero(1).HP;
            for (int i=0;i<50 && boss.aimedCells().length!=0;i++) TenguArenaTest.waitOne(game);
            assertEquals(0,boss.aimedCells().length);
            assertEquals(points-1000,Statistics.bossScores[4]);
            assertTrue(game.hero(0).HP<a && game.hero(1).HP<b);
        }
    }

    @Test public void fullPackLeavesOneAmuletAndEitherSeatCanClaimIt() {
        for (int chosen=0;chosen<=1;chosen++) {
            try (CoopSession game=CavesInteriorTest.open(578)) {
                YogArenaTest.prepareBoundary(game,"amulet");
                for(int i=0;i<10 && game.activeSeat()!=chosen;i++) TenguArenaTest.waitOne(game);
                assertEquals(chosen,game.activeSeat());
                YogArenaTest.positionAtAmulet(game);
                Hero owner=game.hero(chosen);
                int cell=YogArenaTest.amuletCell();
                Amulet relic=(Amulet)Dungeon.level.heaps.get(cell).peek();
                game.checkpoint();
                long physical=game.ownership.identity(relic);
                assertTrue(physical>0);
                Dagger last=null;
                while(owner.belongings.backpack.items.size()<owner.belongings.backpack.capacity()) {
                    last=new Dagger();
                    assertTrue(last.collect(owner.belongings.backpack));
                }
                long turn=game.turn();
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,relic));
                assertEquals(turn,game.turn());
                assertSame(relic,Dungeon.level.heaps.get(cell).peek());
                assertFalse(Statistics.amuletObtained);
                last.detachAll(owner.belongings.backpack);
                YogArenaTest.takeAmulet(game);
                assertEquals(physical,game.ownership.identity(owner.belongings.getItem(Amulet.class)));
                assertEquals(game.player(chosen),game.ownership.owner(relic));
                long[] rng=Random.worldState();
                turn=game.turn();
                assertTrue(game.chooseEnding(chosen,true));
                assertTrue(game.chooseEnding(1-chosen,false));
                assertEquals(turn,game.turn());
                assertArrayEquals(rng,Random.worldState());
            }
        }
    }
}
