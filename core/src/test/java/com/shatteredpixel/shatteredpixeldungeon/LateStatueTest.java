package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;
import org.junit.Test;

public class LateStatueTest extends HeadlessDungeon {
    @Test public void generatedWeaponsReachProcDropAndRetainPhysicalIdentity() {
        int[][] cases={{570,16},{571,23},{573,23},{575,23},{577,19},{580,16}};
        String[] names={"AssassinsBlade","Greataxe","WarScythe","WarScythe","Glaive","Longsword"};
        for(int index=0;index<cases.length;index++) {
            int seed=cases[index][0], depth=cases[index][1];
            Bundle parked;
            long weapon;
            int id;
            try(CoopSession game=CavesInteriorTest.open(seed)) {
                if(depth<=20) CityKingTest.reach(game,depth);
                else {
                    CityKingTest.prepareBoundary(game,"claim");
                    while(Dungeon.depth<depth) SewerDepthTest.gate(game,true);
                }
                Statue statue=SewerSpecialActorTest.actor(Statue.class);
                Weapon blade=statue.weapon();
                assertEquals(names[index],blade.getClass().getSimpleName());
                id=statue.id();
                weapon=game.ownership.identity(blade);
                assertTrue(statue.attackDelay()>0);
                Hero target=game.hero(index%2),ally=game.hero(1-index%2);
                target.HT=target.HP=ally.HT=ally.HP=2200;
                CavesBossTest.beside(game,index%2,statue.pos);
                CavesBossTest.beside(game,1-index%2,target.pos);
                int hp=ally.HP;
                if (blade.enchantment instanceof Projecting) {
                    assertEquals(2,blade.reachFactor(statue));
                    statue.attackProc(target,12);
                } else if(blade.enchantment instanceof Shocking) {
                    for(int i=0;i<160 && hp==ally.HP;i++) statue.attackProc(target,12);
                    assertTrue(ally.HP<hp);
                } else if(blade.enchantment instanceof Venomous) {
                    for(int i=0;i<160 && target.buff(Poison.class)==null;i++) statue.attackProc(target,12);
                    assertNotNull(target.buff(Poison.class));
                } else {
                    assertTrue(blade.enchantment instanceof Chilling);
                    for(int i=0;i<160 && target.buff(Chill.class)==null;i++) statue.attackProc(target,12);
                    assertNotNull(target.buff(Chill.class));
                }
                SewerDepthTest.gate(game,false);
                assertNull(Actor.findById(id));
                parked=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(parked)) {
                SewerDepthTest.gate(game,true);
                Statue statue=(Statue)Actor.findById(id);
                assertEquals(weapon,game.ownership.identity(statue.weapon()));
                int pos=statue.pos;
                for(int n=0;n<220 && statue.isAlive();n++) CityKingTest.strike(game,statue);
                assertFalse(statue.isAlive());
                assertTrue(Dungeon.level.heaps.get(pos).items.stream().anyMatch(it->game.ownership.identity(it)==weapon));
            }
        }
    }
}
