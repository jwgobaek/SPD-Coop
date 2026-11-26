package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import static org.junit.Assert.*;

/** Compare one staged mask for the warrior against no subclass from the same 25th-floor state. */
public class WarriorYogGrowth extends HeadlessDungeon {
    private static Bundle arrange() {
        try (CoopSession game=CavesInteriorTest.open(578)) {
            YogArenaTest.arrive(game);
            while (Dungeon.depth>10) SewerDepthTest.gate(game,false);
            SewerBeeTest.offer(game,0);
            com.shatteredpixel.shatteredpixeldungeon.items.Heap treasure=Dungeon.level.heaps.valueList().stream()
                    .filter(heap -> heap.peek() instanceof TengusMask).findFirst().orElseThrow(AssertionError::new);
            if (game.hero(1).pos==treasure.pos) CavesBossTest.beside(game,1,treasure.pos);
            game.hero(0).pos=treasure.pos;
            game.observe(0);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,treasure.pos,treasure.peek()));
            while (Dungeon.depth<25) SewerDepthTest.gate(game,true);
            assertNotNull(game.hero(0).belongings.getItem(TengusMask.class));
            return game.checkpoint();
        }
    }
    private static void fight(Bundle initial, HeroSubClass choice) {
        try (CoopSession game=CoopSession.restore(initial)) {
            for (int i=0;i<2;i++) {
                Hero h=game.hero(i);
                while (h.lvl<25) h.earnExp(h.maxExp(),WarriorYogGrowth.class);
                h.HTBoost=0;
                h.updateHT(false);
                h.HP=h.HT;
                h.STR=20;
                h.belongings.weapon=new Greatsword();
                h.belongings.weapon.level(4);
                h.belongings.armor=new PlateArmor();
                h.belongings.armor.level(4);
                game.ownership.claim(h.belongings.weapon,game.player(i));
                game.ownership.claim(h.belongings.armor,game.player(i));
                assertEquals(140,h.HT);
            }
            Hero owner=game.hero(0);
            if (choice!=HeroSubClass.NONE)
                WarriorGrowthCombat.Callback.select(owner.belongings.getItem(TengusMask.class),owner,choice);
            assertEquals(choice,owner.subClass);
            assertEquals(HeroSubClass.NONE,game.hero(1).subClass);
            YogArenaTest.start(game);
            long start=game.turn();
            ArrayList<String> fists=new ArrayList<>();
            try { YogArenaTest.fight(game,fists); }
            catch (AssertionError e) {
                if (!game.defeated() || !String.valueOf(e.getMessage()).contains("party lost")) throw e;
            }
            YogDzewa boss=YogArenaTest.resident(YogDzewa.class);
            System.out.printf("GROWTH_PAIR\t%s\t%d\t%d\t%d\t%d\t%d\t%s\t%s%n",choice,game.turn()-start,
                    game.hero(0).HP,game.hero(1).HP,boss==null?0:boss.phase(),boss==null?0:boss.HP,fists,game.defeated());
        }
    }
    public static void main(String[] args) throws Exception {
        WarriorYogGrowth probe=new WarriorYogGrowth();
        probe.temporary.create();
        probe.startBackend();
        try {
            Bundle initial=arrange();
            for (HeroSubClass choice:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.BERSERKER,HeroSubClass.GLADIATOR})
                fight(initial,choice);
        } finally {probe.stopBackend();probe.temporary.delete();}
    }
}
