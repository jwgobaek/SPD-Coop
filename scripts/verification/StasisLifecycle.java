package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.watabou.utils.Bundle;
import org.junit.Test;

import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

/** Follow an original summoned actor through scheduled expiry, suspension and floor changes. */
public class StasisLifecycle extends HeadlessDungeon {
    private static int hold(CoopSession game,int seat) {
        PlayerGrowthTest.stage(game,"stasishold"+seat);
        Hero hero=game.hero(seat);
        int id=PowerOfMany.getPoweredAlly(hero).id();
        assertTrue(CoopFloorsTest.step(game,STASIS,hero.pos,hero.belongings.getItem(HolyTome.class)));
        return id;
    }

    @Test public void twoFloorTransitionsPreserveExactlyOneHeldAlly() {
        for (int seat=0; seat<2; seat++) {
            Bundle parked;
            int id;
            try (CoopSession game=CoopSession.open(409,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                id=hold(game,seat);
                SewerBeeTest.cross(game,Dungeon.level.exit());
                assertEquals(2,Dungeon.depth);
                assertNull(Actor.findById(id));
                assertEquals(id,Stasis.getStasisAlly(game.hero(seat)).id());
                parked=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(parked)) {
                SewerBeeTest.cross(game,Dungeon.level.entrance());
                assertEquals(1,Dungeon.depth);
                Hero hero=game.hero(seat);
                assertNull(Actor.findById(id));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,STASIS,hero.pos,hero.belongings.getItem(HolyTome.class)));
                assertEquals(id,PowerOfMany.getPoweredAlly(hero).id());
                assertEquals(1,Dungeon.level.mobs.stream().filter(m->m.id()==id).count());
                game.validateActorGraph();
            }
        }
    }

    @Test public void lethalHitsFreezeDownedPocketsAndDiscardFinalCasualties() {
        for (int seat=0; seat<2; seat++) for (boolean revive:new boolean[]{false,true}) {
            Bundle saved;
            int id;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                id=hold(game,seat);
                Hero hero=game.hero(seat);
                if (revive) assertTrue(new Ankh().collect(hero.belongings.backpack));
                hero.HP=17;
                hero.damage(10000,new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat());
                game.finishEffects();
                assertEquals(revive?CoopSession.Life.DOWNED:CoopSession.Life.DEAD,game.life(seat));
                assertEquals(revive,Stasis.getStasisAlly(hero)!=null);
                float time=revive?hero.buff(Stasis.StasisBuff.class).cooldown():0;
                assertTrue(CoopFloorsTest.step(game,WAIT,0,null));
                if (revive) assertEquals(time,hero.buff(Stasis.StasisBuff.class).cooldown(),0f);
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat);
                assertNull(Actor.findById(id));
                if (revive) {
                    assertTrue(CoopFloorsTest.step(game,REVIVE,hero.pos,null));
                    assertEquals(5,hero.HP);
                    assertEquals(id,Stasis.getStasisAlly(hero).id());
                } else assertNull(hero.buff(Stasis.StasisBuff.class));
                game.validateActorGraph();
            }
        }
    }

    @Test public void nativeScheduledExpiryReturnsOneAllyWithoutAManualCast() {
        for (int seat=0; seat<2; seat++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                int id=hold(game,seat);
                Hero hero=game.hero(seat);
                for (Hero h:new Hero[]{hero,game.hero(1-seat)}) h.HP=h.HT=400;
                int requests=0;
                for (;requests<600 && Stasis.getStasisAlly(hero)!=null;requests++) {
                    assertTrue(CoopFloorsTest.step(game,WAIT,0,null));
                }
                assertTrue(requests>30);
                assertNull(Stasis.getStasisAlly(hero));
                assertEquals(id,PowerOfMany.getPoweredAlly(hero).id());
                game.validateActorGraph();
            }
        }
    }

    @Test public void oneRosterRestoresBothNestedScrollChoicesAndConsentedWarpWithAPocket() {
        Bundle choices, proposal;
        int allyId;
        try (CoopSession game=CoopSession.open(409,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.CLERIC);
            CoopMageTest.encounter(game,1);
            Hero mage=game.hero(0),cleric=game.hero(1);
            mage.lvl=cleric.lvl=30;
            SewerBeeTest.offer(game,0);
            assertTrue(CoopFloorsTest.step(game,USE_CROWN,2,CrownAbilityTest.crown(game,0,CoopSession.MaskTrial.ROSTER)));
            SewerBeeTest.offer(game,0);
            for (int i=0;i<3;i++) assertTrue(CoopFloorsTest.step(game,INVEST_TALENT,Talent.LONGRANGE_WARP.ordinal(),null));
            assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,mage.pos,mage.belongings.armor()));
            SewerBeeTest.offer(game,1);
            assertTrue(CoopFloorsTest.step(game,CLAIM_ARMOR,2,null));
            SewerBeeTest.offer(game,1);
            assertTrue(CoopFloorsTest.step(game,INVEST_TALENT,Talent.STASIS.ordinal(),null));
            TierGrowthTest.powerTome(cleric);
            assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,cleric.pos+1,cleric.belongings.armor()));
            SewerBeeTest.offer(game,1);
            allyId=PowerOfMany.getPoweredAlly(cleric).id();
            assertTrue(CoopFloorsTest.step(game,STASIS,cleric.pos,cleric.belongings.getItem(HolyTome.class)));
            SewerBeeTest.offer(game,0);
            WandOfPrismaticLight wand=new WandOfPrismaticLight();
            wand.identify(false);
            Dungeon.level.drop(wand,mage.pos);
            assertTrue(CoopFloorsTest.step(game,PICK_UP,mage.pos,wand));
            SewerBeeTest.offer(game,0);
            LeatherArmor armor=new LeatherArmor();
            Dungeon.level.drop(armor,mage.pos);
            assertTrue(CoopFloorsTest.step(game,PICK_UP,mage.pos,armor));
            SewerBeeTest.offer(game,0);
            ScrollOfEnchantment scroll=new ScrollOfEnchantment();
            Dungeon.level.drop(scroll,mage.pos);
            assertTrue(CoopFloorsTest.step(game,PICK_UP,mage.pos,scroll));
            SewerBeeTest.offer(game,0);
            assertTrue(CoopFloorsTest.step(game,READ_SCROLL,0,scroll));
            assertTrue(CoopFloorsTest.step(game,ENCHANT_TARGET,0,armor));
            choices=game.checkpoint();
            assertEquals(25,choices.getInt("schema"));
            assertEquals(24,choices.getInt("stasis_schema"));
        }
        try (CoopSession game=CoopSession.restore(choices)) {
            assertTrue(game.enchantScroll(0).hasOptions());
            assertTrue(game.discoveries(0).knows(TrinityDiscoveries.Choice.PRISMATIC_LIGHT));
            assertFalse(game.discoveries(1).knows(TrinityDiscoveries.Choice.PRISMATIC_LIGHT));
            assertEquals(allyId,Stasis.getStasisAlly(game.hero(1)).id());
            assertTrue(CoopFloorsTest.step(game,ENCHANT_CANCEL,0,null));
            SewerBeeTest.cross(game,Dungeon.level.exit());
            SewerBeeTest.offer(game,0);
            Hero mage=game.hero(0);
            assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,WarpBeacon.RECALL,mage.belongings.armor()));
            assertTrue(game.stairs.consent(1,game.turn(),mage.pos,false));
            proposal=game.checkpoint();
            assertEquals(26,proposal.getInt("schema"));
            assertEquals(25,proposal.getInt("warp_schema"));
        }
        try (CoopSession game=CoopSession.restore(proposal)) {
            assertTrue(CoopFloorsTest.step(game,TRAVEL,game.hero(0).pos,null));
            assertEquals(1,Dungeon.depth);
            Hero cleric=game.hero(1);
            assertEquals(allyId,Stasis.getStasisAlly(cleric).id());
            SewerBeeTest.offer(game,1);
            assertTrue(CoopFloorsTest.step(game,STASIS,cleric.pos,cleric.belongings.getItem(HolyTome.class)));
            assertEquals(1,Dungeon.level.mobs.stream().filter(m->m.id()==allyId).count());
            game.validateActorGraph();
        }
    }
}
