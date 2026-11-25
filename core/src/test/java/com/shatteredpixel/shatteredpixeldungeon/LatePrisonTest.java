package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class LatePrisonTest extends HeadlessDungeon {
    @Test public void generatedCasterAndBothHeroesReturnAfterNinthFloorRestart() {
        int[] heroes=new int[2];
        int caster, minion;
        Bundle saved;
        try(CoopSession g=CoopSession.open(570,11,HeroClass.WARRIOR)) {
            g.join(12,HeroClass.MAGE);
            WandmakerTest.enterPrison(g);
            assertEquals(5,g.checkpoint().getInt("schema"));
            for(int seat=0;seat<2;seat++) heroes[seat]=g.hero(seat).id();
            SewerDepthTest.gate(g,true);
            assertEquals(8,Dungeon.depth);
            Necromancer necro=(Necromancer)Dungeon.level.mobs.stream().filter(m->m.getClass()==Necromancer.class).findFirst().get();
            caster=necro.id();
            WandmakerTest.beside(g,1,necro.pos);
            necro.aggro(g.hero(1));
            for(int i=0;i<24 && !NecromancerTest.data(necro).contains("my_skeleton");i++) SewerSpecialActorTest.waitOne(g);
            minion=NecromancerTest.data(necro).getInt("my_skeleton");
            assertTrue(Actor.findById(minion) instanceof Necromancer.NecroSkeleton);
            SewerDepthTest.gate(g,true);
            assertEquals(9,Dungeon.depth);
            assertNull(Actor.findById(caster));
            assertNull(Actor.findById(minion));
            saved=g.checkpoint();
            assertEquals(8,saved.getBundle("dormant").getKeys().size());
            assertEquals(6,saved.getInt("schema"));
        }
        saved.put("schema",5);
        assertThrows(IllegalArgumentException.class,()->CoopSession.restore(saved));
        saved.put("schema",6);
        Bundle parked = saved.getBundle("dormant").getBundle("8").getBundle("level");
        Bundle casterRecord = java.util.Arrays.stream(parked.getBundleArray("mobs"))
                .filter(m -> m.getInt("id") == caster).findFirst().get();
        casterRecord.put("my_skeleton", heroes[0]);
        assertThrows(IllegalArgumentException.class, () -> CoopSession.restore(saved));
        casterRecord.put("my_skeleton", minion);
        try(CoopSession g=CoopSession.restore(saved)) {
            assertEquals(9,Dungeon.depth);
            for(int seat=0;seat<2;seat++) assertEquals(heroes[seat],g.hero(seat).id());
            assertNotEquals(g.hero(0).pos,g.hero(1).pos);
            for(Mob m:Dungeon.level.mobs) if(m instanceof Bat){
                for(int seat=0;seat<2;seat++){
                    m.HP=1;
                    int damage=m.attackProc(g.hero(seat),10);
                    assertEquals(Math.min(m.HT,1+Math.max(0,damage-4)),m.HP);
                }
            }
            SewerDepthTest.gate(g,false);
            assertEquals(8,Dungeon.depth);
            Necromancer necro=(Necromancer)Actor.findById(caster);
            assertEquals(minion,NecromancerTest.data(necro).getInt("my_skeleton"));
            assertTrue(Actor.findById(minion) instanceof Necromancer.NecroSkeleton);
            SewerDepthTest.gate(g,false);
            assertEquals(7,Dungeon.depth);
            assertEquals(6,g.checkpoint().getInt("schema"));
            SewerSpecialActorTest.waitOne(g);
        }
    }
}
