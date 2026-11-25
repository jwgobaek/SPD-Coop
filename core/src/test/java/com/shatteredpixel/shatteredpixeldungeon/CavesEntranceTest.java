package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

/** Local encounter fixtures on the actual generated cave, after victory using native attacks. */
public class CavesEntranceTest extends HeadlessDungeon {
    @Test public void shamanBoltsReachEachSeatAndBruteRagePersistsWithItsActor() {
        Bundle entrance;
        try (CoopSession g=CoopSession.open(570,11,HeroClass.WARRIOR)) {
            g.join(22,HeroClass.MAGE);
            TenguArenaTest.prepareSocket(g,"caves");
            SewerDepthTest.gate(g,true);
            entrance=g.checkpoint();
        }
        for (int seat=0;seat<2;seat++) for (int color=0;color<3;color++) {
            Bundle rageState;
            int id;
            try (CoopSession g=CoopSession.restore(entrance)) {
                Level l=Dungeon.level;
                for (Mob m:new ArrayList<>(l.mobs)) { Actor.remove(m); l.mobs.remove(m); }
                int center=l.width()*(l.height()/2)+l.width()/2;
                for (int y=-3;y<=3;y++) for (int x=-6;x<=6;x++) {
                    int cell=center+y*l.width()+x;
                    Level.set(cell,Terrain.EMPTY); l.traps.remove(cell); l.plants.remove(cell);
                    l.discoverable[cell]=true;
                }
                Hero target=g.hero(seat),ally=g.hero(1-seat);
                target.pos=center+3; ally.pos=center-5-l.width();
                target.HP=target.HT; ally.HP=ally.HT;
                Shaman enemy=color==0 ? new Shaman.RedShaman() : color==1 ? new Shaman.BlueShaman() : new Shaman.PurpleShaman();
                Class<? extends Buff> debuff=color==0 ? Weakness.class : color==1 ? Vulnerable.class : Hex.class;
                enemy.pos=center; GameScene.add(enemy); g.attachSprite(enemy); enemy.aggro(target);
                g.observe(0); g.observe(1);
                for (int n=0;n<60 && target.buff(debuff)==null;n++) TenguArenaTest.waitOne(g);
                assertNotNull(target.buff(debuff));
                assertTrue(target.HP<target.HT);
                assertNull(ally.buff(debuff));
                Brute brute=new Brute(); brute.pos=center-l.width(); GameScene.add(brute); g.attachSprite(brute);
                brute.damage(brute.HT+1,target);
                assertTrue(brute.isAlive());
                assertTrue(brute.buff(Brute.BruteRage.class).shielding()>0);
                id=brute.id();
                rageState=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(rageState)) {
                Brute brute=(Brute)Actor.findById(id);
                assertTrue(brute.isAlive());
                for (int i=0;i<40 && Actor.findById(id)!=null;i++) TenguArenaTest.waitOne(g);
                assertNull(Actor.findById(id));
            }
        }
    }
}
