package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class WardingTest extends HeadlessDungeon {
    static StagedInput prepare(CoopSession game,int seat) {
        Rat rat = CoopMageTest.encounter(game,seat);
        rat.destroy();
        WandArtsTest.give(game,seat,new WandOfWarding());
        SewerBeeTest.offer(game,seat);
        return new StagedInput(game,"ZAP",game.hero(seat).pos+2);
    }

    private static Ward ward(CoopSession game, int seat) {
        for (var mob : Dungeon.level.mobs) if (mob instanceof Ward && ((Ward)mob).belongsTo(game.hero(seat))) return (Ward)mob;
        return null;
    }

    static boolean published(CoopSession game,int seat) {
        WandOfWarding wand=game.hero(seat).belongings.getItem(WandOfWarding.class);
        return ward(game,seat)!=null && wand!=null && wand.curCharges==wand.maxCharges-1;
    }

    @Test public void twoOwnersHaveIndependentLimitsTargetsAndSavedActors() throws Exception {
        int[] ids=new int[2];
        long[] physical=new long[2];
        Bundle saved;
        try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            CoopMageTest.encounter(game,0).destroy();
            for(int i=0;i<2;i++) {
                Hero owner=game.hero(i);
                WandOfWarding wand=WandArtsTest.give(game,i,new WandOfWarding());
                physical[i]=game.ownership.identity(wand);
                SewerBeeTest.offer(game,i);
                assertTrue(CoopFloorsTest.step(game,ZAP,owner.pos+2,wand));
                Ward ward=ward(game,i);
                assertNotNull(ward);
                ids[i]=ward.id();
                assertSame(owner,CoopSession.summonerOf(ward));
                SewerBeeTest.offer(game,i);
                assertTrue(CoopFloorsTest.step(game,ZAP,ward.pos,wand));
                assertEquals(2,ward.tier);
                wand.curCharges=1;
                SewerBeeTest.offer(game,i);
                assertFalse(CoopFloorsTest.step(game,ZAP,owner.pos-2,wand));
                if(i==1) assertFalse(CoopFloorsTest.step(game,ZAP,ward(game,0).pos,wand));
                assertEquals(1,wand.curCharges);
            }
            saved=game.checkpoint();
        }
        try(CoopSession game=CoopSession.restore(saved)) {
            assertNotEquals(ids[0],ids[1]);
            for(int i=0;i<2;i++) {
                assertSame(Actor.findById(ids[i]),ward(game,i));
                assertEquals(2,ward(game,i).tier);
                assertEquals(physical[i],game.ownership.identity(game.hero(i).belongings.getItem(WandOfWarding.class)));
            }
            assertTrue(CoopFloorsTest.step(game,WAIT,0,null));
        }
    }

    @Test public void nativeWardAttackExpiresAndDormantWardReturnsToItsOwner() throws Exception {
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            int id;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat target=CoopMageTest.encounter(game,seat);
                Hero owner=game.hero(seat);
                WandOfWarding wand=WandArtsTest.give(game,seat,new WandOfWarding());
                SewerBeeTest.offer(game,seat);
                int hp=target.HP;
                assertTrue(CoopFloorsTest.step(game,ZAP,owner.pos+1,wand));
                for (int n=0;n<12 && target.HP==hp;n++) assertTrue(CoopFloorsTest.step(game,WAIT,0,null));
                assertTrue(target.HP<hp);
                assertNull(ward(game,seat));
                target.destroy();
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,ZAP,owner.pos+2,wand));
                id=ward(game,seat).id();
                SewerSpecialActorTest.atGate(game,Dungeon.level.exit());
                CoopFloorsTest.cross(game);
                assertNull(Actor.findById(id));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
                CoopFloorsTest.cross(game);
                assertEquals(id,ward(game,seat).id());
                assertNull(ward(game,1-seat));
            }
        }
    }

    @Test public void originalStaffHealBlastAndVolleyUseOwnersWard() {
        for(int seat=0;seat<2;seat++) for(int mode=0;mode<3;mode++) {
            try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat foe=CoopMageTest.encounter(game,seat);
                foe.destroy();
                Hero caster=game.hero(seat);
                WandOfWarding wand=WandArtsTest.give(game,seat,new WandOfWarding());
                wand.upgrade(3);
                wand.curCharges=wand.maxCharges;
                MagesStaff staff=(MagesStaff)caster.belongings.weapon();
                if (mode!=1) {
                    TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                    assertSame(wand,staff.imbuedWand());
                    for(int n=0;n<4;n++) {
                        SewerBeeTest.offer(game,seat);
                        assertTrue(CoopFloorsTest.step(game,ZAP,caster.pos+2,staff));
                    }
                    Ward w=ward(game,seat);
                    assertEquals(4,w.tier);
                    w.HP=1;
                }
                if(mode==2) {
                    Rat target=new Rat();
                    target.HP=target.HT=250;
                    target.pos=caster.pos-1;
                    com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(target,1000);
                    Buff.prolong(target,Paralysis.class,100);
                    game.observe(seat);
                    Dungeon.level.updateFieldOfView(caster,caster.fieldOfView);
                    // The ward cannot see the melee target and will not spend its own HP shooting.
                    Buff.prolong(ward(game,seat),Paralysis.class,100);
                    com.watabou.utils.Random.pushGenerator(572);
                    try {
                        for(int n=0;n<24 && ward(game,seat).HP==1;n++) {
                            SewerBeeTest.offer(game,seat);
                            assertTrue(CoopFloorsTest.step(game,ATTACK,target.pos,null));
                        }
                    } finally {com.watabou.utils.Random.popGenerator();}
                    assertTrue(ward(game,seat).HP>1);
                } else {
                    ClassArmor armor=ArmorTalentsTest.invest(game,seat,mode,
                            mode==0?Talent.ELEMENTAL_POWER:Talent.WILD_POWER,1);
                    float energy=armor.charge, charges=wand.curCharges+wand.partialCharge;
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,mode==0?caster.pos:caster.pos+2,armor));
                    assertTrue(armor.charge<energy);
                    if(mode==0) assertTrue(ward(game,seat).HP>1);
                    else {
                        assertNotNull(ward(game,seat));
                        assertTrue(wand.curCharges+wand.partialCharge<charges);
                    }
                }
                assertNotNull(game.checkpoint());
            }
        }
    }
}
