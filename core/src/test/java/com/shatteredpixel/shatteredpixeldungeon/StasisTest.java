package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class StasisTest extends HeadlessDungeon {
    @Test public void aHeldAllyKeepsItsIdentityAndReturnsForFreeAtEveryRank() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=4; rank++) {
            Bundle save;
            int actorId, hp;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                CoopMageTest.encounter(game,seat);
                Hero owner=game.hero(seat);
                ClassArmor armor=ArmorTalentsTest.invest(game,seat,2,Talent.STASIS,rank);
                TierGrowthTest.powerTome(owner);
                HolyTome tome=owner.belongings.getItem(HolyTome.class);
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,owner.pos+1,armor));
                SewerBeeTest.offer(game,seat);
                Mob ally=(Mob)PowerOfMany.getPoweredAlly(owner);
                actorId=ally.id();
                hp=ally.HP;
                int charges=tome.charges();
                assertEquals(rank>0, CoopFloorsTest.step(game,STASIS,owner.pos,tome));
                if (rank==0) continue;
                assertEquals(charges-2,tome.charges());
                assertSame(ally,Stasis.getStasisAlly(owner));
                assertNull(PowerOfMany.getPoweredAlly(owner));
                assertFalse(Dungeon.level.mobs.contains(ally));
                assertNull(Actor.findById(actorId));
                assertNull(Stasis.getStasisAlly(game.hero(1-seat)));
                assertEquals(30+30*rank,owner.buff(Stasis.StasisBuff.class).cooldown(),1f);
                save=game.checkpoint();
                assertEquals(25,save.getInt("schema"));
            }
            try (CoopSession game=CoopSession.restore(save)) {
                Hero owner=game.hero(seat);
                Mob ally=Stasis.getStasisAlly(owner);
                assertEquals(actorId,ally.id());
                assertEquals(hp,ally.HP);
                assertNull(Actor.findById(actorId));
                SewerBeeTest.offer(game,seat);
                HolyTome tome=owner.belongings.getItem(HolyTome.class);
                assertFalse(CoopFloorsTest.step(game,ARMOR_ABILITY,owner.pos+1,owner.belongings.armor()));
                int charges=tome.charges();
                assertTrue(CoopFloorsTest.step(game,STASIS,owner.pos,tome));
                assertNull(Stasis.getStasisAlly(owner));
                assertSame(ally,PowerOfMany.getPoweredAlly(owner));
                assertSame(ally,Actor.findById(actorId));
                assertEquals(charges,tome.charges());
                assertTrue(Dungeon.level.adjacent(owner.pos,ally.pos));
                assertNotEquals(game.hero(1-seat).pos,ally.pos);
                assertNotEquals(25,game.checkpoint().getInt("schema"));
            }
        }
    }

    @Test public void expiryWaitsForSafeLandingWithoutDisplacingTheOtherSeat() {
        Bundle save;
        int free, allyId;
        try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
            game.join(22,HeroClass.CLERIC);
            CoopMageTest.encounter(game,0);
            Hero owner=game.hero(0);
            ClassArmor armor=ArmorTalentsTest.invest(game,0,2,Talent.STASIS,1);
            TierGrowthTest.powerTome(owner);
            HolyTome tome=owner.belongings.getItem(HolyTome.class);
            assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,owner.pos+1,armor));
            SewerBeeTest.offer(game,0);
            allyId=PowerOfMany.getPoweredAlly(owner).id();
            assertTrue(CoopFloorsTest.step(game,STASIS,owner.pos,tome));
            SewerBeeTest.offer(game,0);
            free=owner.pos+1;
            for (int offset:PathFinder.NEIGHBOURS8)
                if (Actor.findChar(owner.pos+offset)==null) Level.set(owner.pos+offset,Terrain.WALL);
            Stasis.StasisBuff held=owner.buff(Stasis.StasisBuff.class);
            assertFalse(held.canRelease());
            assertFalse(CoopFloorsTest.step(game,STASIS,owner.pos,tome));
            float before=held.cooldown();
            assertTrue(held.act());
            assertEquals(before+Actor.TICK,held.cooldown(),0.001f);
            assertNull(Actor.findById(allyId));
            save=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(save)) {
            Hero owner=game.hero(0);
            Stasis.StasisBuff held=owner.buff(Stasis.StasisBuff.class);
            assertFalse(held.canRelease());
            Level.set(free,Terrain.EMPTY);
            assertTrue(held.act());
            assertNull(owner.buff(Stasis.StasisBuff.class));
            assertEquals(allyId,PowerOfMany.getPoweredAlly(owner).id());
            assertEquals(free,PowerOfMany.getPoweredAlly(owner).pos);
            game.validateActorGraph();
        }
    }

    @Test public void twoHeldAlliesRestoreIndependentOwnersAndLinks() {
        int[] ids=new int[2];
        Bundle image;
        try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
            game.join(22,HeroClass.CLERIC);
            CoopMageTest.encounter(game,0);
            for (int seat=0;seat<2;seat++) {
                Hero owner=game.hero(seat);
                owner.lvl=30;
                SewerBeeTest.offer(game,seat);
                if (seat == 0)
                    assertTrue(CoopFloorsTest.step(game,USE_CROWN,2,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.ROSTER)));
                else
                    assertTrue(CoopFloorsTest.step(game,CLAIM_ARMOR,2,null));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,INVEST_TALENT,Talent.STASIS.ordinal(),null));
                assertTrue(CoopFloorsTest.step(game,INVEST_TALENT,Talent.LIFE_LINK.ordinal(),null));
                TierGrowthTest.powerTome(owner);
                HolyTome tome=owner.belongings.getItem(HolyTome.class);
                int landing=-1;
                for (int off:PathFinder.NEIGHBOURS8) {
                    int cell=owner.pos+off;
                    if (Dungeon.level.passable[cell] && CoopSession.supportedLanding(cell) && Actor.findChar(cell)==null
                            && game.knowledge(owner).visible(cell)) {landing=cell;break;}
                }
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,landing,owner.belongings.armor()));
                SewerBeeTest.offer(game,seat);
                ids[seat]=PowerOfMany.getPoweredAlly(owner).id();
                assertTrue(CoopFloorsTest.step(game,LIFE_LINK,owner.pos,tome));
                assertEquals(ids[seat],owner.buff(LifeLink.class).object);
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,STASIS,owner.pos,tome));
                assertNull(owner.buff(LifeLink.class));
            }
            image=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(image)) {
            assertNotEquals(ids[0],ids[1]);
            for (int seat:new int[]{1,0}) {
                Hero owner=game.hero(seat);
                Mob ally=Stasis.getStasisAlly(owner);
                assertEquals(ids[seat],ally.id());
                assertEquals(owner.id(),ally.buff(LifeLink.class).object);
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,STASIS,owner.pos,owner.belongings.getItem(HolyTome.class)));
                assertEquals(ids[seat],owner.buff(LifeLink.class).object);
                assertSame(ally,Actor.findById(ids[seat]));
            }
            game.validateActorGraph();
        }
    }
}
