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
    @Test public void originalCaveStatueChainsToBothSeatsAndDropsItsTwoPersistentItems() {
        for (int seat=0;seat<2;seat++) {
            Bundle parked;
            int actor;
            long blade,mail;
            try (CoopSession g=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                g.join(22,HeroClass.MAGE);
                TenguArenaTest.prepareSocket(g,"caves");
                SewerDepthTest.gate(g,true);
                ArmoredStatue statue=SewerSpecialActorTest.actor(ArmoredStatue.class);
                actor=statue.id();
                assertTrue(statue.weapon() instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade);
                assertTrue(statue.weapon().enchantment instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking);
                assertTrue(statue.armor() instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor);
                assertTrue(statue.armor().glyph instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness);
                assertEquals(20+6*statue.weapon().level(),statue.weapon().max(statue.weapon().level()));
                assertEquals(statue.weapon().delayFactor(statue),statue.attackDelay(),0.001f);
                Hero victim=g.hero(seat),other=g.hero(1-seat);
                boolean adjacent=false;
                for(int a:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int first=statue.pos+a;
                    if (!Dungeon.level.passable[first] || Actor.findChar(first)!=null) continue;
                    for(int b:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                        int second=first+b;
                        if (!Dungeon.level.passable[second] || Actor.findChar(second)!=null) continue;
                        victim.pos=first; other.pos=second; adjacent=true; break;
                    }
                    if (adjacent) break;
                }
                assertTrue(adjacent);
                statue.aggro(victim);
                int before=other.HP;
                for (int i=0;i<160 && other.HP==before;i++) statue.attackProc(victim,8);
                assertTrue(other.HP<before);
                blade=g.ownership.identity(statue.weapon());
                mail=g.ownership.identity(statue.armor());
                assertNotEquals(blade,mail);
                SewerDepthTest.gate(g,false);
                assertNull(Actor.findById(actor));
                parked=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(parked)) {
                SewerDepthTest.gate(g,true);
                ArmoredStatue statue=(ArmoredStatue)Actor.findById(actor);
                assertEquals(blade,g.ownership.identity(statue.weapon()));
                assertEquals(mail,g.ownership.identity(statue.armor()));
                statue.die(g.hero(seat));
                assertEquals(1,CoopSession.floorItems(Dungeon.level).values().stream().filter(i->g.ownership.identity(i)==blade).count());
                assertEquals(1,CoopSession.floorItems(Dungeon.level).values().stream().filter(i->g.ownership.identity(i)==mail).count());
            }
        }
    }

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
                // The original boss level-up recalculates HT. Give this isolated six-way bolt fixture enough turns.
                target.HT=target.HP=500; ally.HT=ally.HP=500;
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
