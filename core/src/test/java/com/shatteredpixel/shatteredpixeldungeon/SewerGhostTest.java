package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FetidRat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollTrickster;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GreatCrab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerGhostTest extends HeadlessDungeon {
    public static void meet(CoopSession game,int seat,Mob target) {
        Level floor=Dungeon.level;
        Hero hero=game.hero(seat);
        for (int offset:PathFinder.NEIGHBOURS8) {
            int cell=target.pos+offset;
            if (floor.passable[cell] && !floor.pit[cell] && (Actor.findChar(cell)==null || Actor.findChar(cell)==hero) && floor.traps.get(cell)==null) {
                hero.pos=cell;
                game.observe(seat);
                floor.updateFieldOfView(hero,hero.fieldOfView);
                return;
            }
        }
        throw new AssertionError("No encounter cell");
    }

    static int bosses() {
        int count=0;
        for(Mob m:Dungeon.level.mobs) if(m instanceof FetidRat || m instanceof GnollTrickster || m instanceof GreatCrab) count++;
        return count;
    }

    @Test public void bothSeatsAcceptAndFinishOneNativeDepthTwoQuest() {
        verifyNativeQuest(403,2,FetidRat.class,20);
    }

    @Test public void restoredTricksterAndGreatCrabGiveOneSharedReward() {
        verifyNativeQuest(402,3,GnollTrickster.class,20);
        verifyNativeQuest(421,4,GreatCrab.class,25);
    }

    private <T extends Mob> void verifyNativeQuest(int seed,int depth,Class<T> bossType,int nativeHP) {
        for(int seat=0;seat<2;seat++) {
            Bundle started,won;
            int ghostId, ratId;
            try(CoopSession g=CoopSession.open(seed,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                for(int target=2;target<=depth;target++) SewerDepthTest.gate(g,true);
                Ghost ghost=SewerSpecialActorTest.actor(Ghost.class);
                ghostId=ghost.id();
                while(g.activeSeat()!=seat) SewerSpecialActorTest.waitOne(g);
                meet(g,seat,ghost);
                assertEquals("OFFER",Ghost.Quest.sewerOffer());
                assertEquals(0,bosses());
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ACCEPT_GHOST,ghost.pos,null));
                assertEquals("HUNT",Ghost.Quest.sewerOffer());
                assertEquals(1,bosses());
                ratId=SewerSpecialActorTest.actor(bossType).id();
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.ACCEPT_GHOST,ghost.pos,null));
                started=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(started)) {
                Mob rat=bossType.cast(Actor.findById(ratId));
                assertEquals(nativeHP,rat.HP);
                // Isolated fight with normal starting staff and rat HP; no changes to monster AI or drops.
                int shots=0;
                for(int n=0;n<45 && rat.isAlive();n++) {
                    int acting=g.activeSeat();
                    Hero hero=g.hero(acting);
                    meet(g,acting,rat);
                    Item staff=hero.belongings.weapon();
                    assertTrue(staff instanceof MagesStaff);
                    if(CoopFloorsTest.step(g,CoopSession.Action.ZAP,rat.pos,staff)) shots++;
                    else assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ATTACK,rat.pos,null));
                }
                assertTrue(shots>0);
                assertEquals(0,bosses());
                assertTrue(Ghost.Quest.processed());
                assertTrue("Quest completion credits a positive score",Statistics.questScores[0]>0);
                won=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(won)) {
                while(g.activeSeat()!=seat) SewerSpecialActorTest.waitOne(g);
                Ghost ghost=(Ghost)Actor.findById(ghostId);
                meet(g,seat,ghost);
                int score=Statistics.questScores[0];
                assertEquals("REWARD",Ghost.Quest.sewerOffer());
                CoopSession.Action choice=seat==0?CoopSession.Action.GHOST_WEAPON:CoopSession.Action.GHOST_ARMOR;
                Item expected=seat==0?Ghost.Quest.weapon:Ghost.Quest.armor;
                assertTrue(CoopFloorsTest.step(g,choice,ghost.pos,null));
                assertTrue(g.hero(seat).belongings.contains(expected));
                assertFalse(g.hero(1-seat).belongings.contains(expected));
                assertEquals(g.player(seat),g.ownership.owner(expected));
                assertTrue(expected.isIdentified());
                assertTrue(Ghost.Quest.completed());
                assertFalse(CoopFloorsTest.step(g,choice,ghost.pos,null));
                assertEquals(score,Statistics.questScores[0]);
                won=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(won)) {
                assertNull(Actor.findById(ghostId));
                assertEquals(0,bosses());
                assertTrue(Ghost.Quest.completed());
                SewerSpecialActorTest.waitOne(g);
            }
        }
    }
}
