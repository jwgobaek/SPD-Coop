package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.EarthGuardian;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth.RockArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class LivingEarthTest extends HeadlessDungeon {
    static StagedInput prepare(CoopSession game, int seat) {
        CoopMageTest.encounter(game,seat);
        Hero owner=game.hero(seat);
        WandArtsTest.give(game,seat,new WandOfLivingEarth());
        EarthGuardian guardian=new EarthGuardian();
        guardian.setInfo(owner,1,1);
        guardian.pos=owner.pos+1;
        GameScene.add(guardian,100);
        game.observe(seat);
        SewerBeeTest.offer(game,seat);
        return new StagedInput(game,"ZAP",guardian.pos);
    }

    static boolean published(CoopSession game, int seat) {
        Hero owner=game.hero(seat);
        EarthGuardian guardian=WandOfLivingEarth.activeGuardian(owner);
        WandOfLivingEarth wand=owner.belongings.getItem(WandOfLivingEarth.class);
        return guardian!=null && guardian.HP>1 && guardian.owner()==owner && wand!=null
                && wand.curCharges==wand.maxCharges-1 && game.ownership.owner(wand)==game.player(seat);
    }

    @Test public void nearGuardiansTravelAndReturnWithTheirOriginalOwners() throws Exception {
        int[] ids=new int[2];
        Bundle save;
        try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            int exit=Dungeon.level.exit();
            SewerSpecialActorTest.atGate(game,exit);
            for(int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                EarthGuardian guardian=new EarthGuardian();
                guardian.setInfo(hero,seat+1,10);
                int pos=-1;
                for(int offset:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int c=hero.pos+offset;
                    if (Dungeon.level.passable[c] && Actor.findChar(c)==null && c!=exit) {pos=c;break;}
                }
                assertTrue(pos>=0);
                guardian.pos=pos;
                GameScene.add(guardian,100);
                ids[seat]=guardian.id();
            }
            CoopFloorsTest.cross(game);
            assertEquals(2,Dungeon.depth);
            for(int seat=0;seat<2;seat++) {
                EarthGuardian g=WandOfLivingEarth.activeGuardian(game.hero(seat));
                assertNotNull(g);
                assertEquals(ids[seat],g.id());
                assertFalse(game.stairs.hasParkedGuardian(game.hero(seat)));
            }
            save=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(save)) {
            int entry=Dungeon.level.entrance();
            SewerSpecialActorTest.atGate(game,entry);
            CoopFloorsTest.cross(game);
            assertEquals(1,Dungeon.depth);
            for(int seat=0;seat<2;seat++) assertEquals(ids[seat],WandOfLivingEarth.activeGuardian(game.hero(seat)).id());
        }
    }

    @Test public void paidShotsAndPreservedStaffRestoreTwoIndependentGuardians() throws Exception {
        Bundle save;
        int[] ids = new int[2], hp = new int[2];
        long[] wands = new long[2];
        try (CoopSession game = CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            Rat rat = CoopMageTest.encounter(game,0);
            rat.HT = rat.HP = 2000;
            Buff.prolong(rat,Paralysis.class,100);
            for (int seat=0;seat<2;seat++) {
                Hero hero = game.hero(seat);
                if (seat==1) TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                WandOfLivingEarth wand = WandArtsTest.give(game,seat,new WandOfLivingEarth());
                wand.level(seat+2);
                wand.curCharges = wand.maxCharges;
                wands[seat] = game.ownership.identity(wand);
                MagesStaff staff = (MagesStaff)hero.belongings.weapon();
                if (seat==1) {
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                }
                for (int i=0;i<10 && WandOfLivingEarth.activeGuardian(hero)==null;i++) {
                    SewerBeeTest.offer(game,seat);
                    if (seat==1) {
                        // Give the second owner a separate line of fire past the first guardian.
                        int dest=hero.pos+Dungeon.level.width();
                        if (Actor.findChar(dest)==null && Dungeon.level.passable[dest]) rat.pos=dest;
                        game.observe(seat);
                        Dungeon.level.updateFieldOfView(hero,hero.fieldOfView);
                    }
                    int charges = wand.curCharges;
                    int before = rat.HP;
                    assertTrue("paid shot seat "+seat+" offer="+game.activeSeat()+" ownerPos="+hero.pos+" target="+rat.pos+" visible="+hero.fieldOfView[rat.pos]+" charges="+charges+" staff="+staff.imbuedWand(),CoopFloorsTest.step(game,ZAP,rat.pos,seat==1?staff:wand));
                    assertTrue(rat.HP<before);
                    assertEquals(charges-1,wand.curCharges);
                    assertTrue(hero.buff(RockArmor.class)!=null || WandOfLivingEarth.activeGuardian(hero)!=null);
                }
                EarthGuardian guardian = WandOfLivingEarth.activeGuardian(hero);
                assertNotNull(guardian);
                ids[seat] = guardian.id();
                assertSame(hero,guardian.owner());
                assertEquals(16+8*wand.buffedLvl(),guardian.HT);
            }
            assertNotEquals(ids[0],ids[1]);
            for (int seat=0;seat<2;seat++) {
                EarthGuardian guardian=(EarthGuardian)Actor.findById(ids[seat]);
                hp[seat]=guardian.HP;
                assertSame(game.hero(seat),CoopSession.summonerOf(guardian));
            }
            save=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(save)) {
            for (int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                EarthGuardian g = WandOfLivingEarth.activeGuardian(hero);
                assertEquals(ids[seat],g.id());
                assertEquals(hp[seat],g.HP);
                assertSame(hero,g.owner());
                assertEquals(wands[seat],game.ownership.identity(seat==0?hero.belongings.getItem(WandOfLivingEarth.class):((MagesStaff)hero.belongings.weapon()).imbuedWand()));
            }
        }
    }

    @Test public void blastAndBattlemageHealOnlyTheCastersGuardianAndWildMagicFires() {
        for (int seat=0;seat<2;seat++) for (int mode=0;mode<3;mode++) {
            try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero owner=game.hero(seat);
                WandOfLivingEarth wand=WandArtsTest.give(game,seat,new WandOfLivingEarth());
                MagesStaff staff=(MagesStaff)owner.belongings.weapon();
                rat.HT=rat.HP=700;
                Buff.prolong(rat,Paralysis.class,100);
                if (mode==2) {
                    TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                    rat.pos=owner.pos+1;
                }
                if (mode!=1) {
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                }
                ClassArmor armor=mode==2 ? null : ArmorTalentsTest.invest(game,seat,mode,
                        mode==0 ? Talent.ELEMENTAL_POWER : Talent.WILD_POWER,1);
                EarthGuardian[] allies=new EarthGuardian[2];
                for(int s=0;s<2;s++) {
                    allies[s]=new EarthGuardian();
                    allies[s].setInfo(game.hero(s),2,1);
                    allies[s].pos=game.hero(s).pos-1;
                    GameScene.add(allies[s],100);
                }
                game.observe(seat);
                Dungeon.level.updateFieldOfView(owner, owner.fieldOfView);
                int before=rat.HP;
                float charges=wand.curCharges+wand.partialCharge;
                if (mode==2) {
                    for(int i=0;i<30 && allies[seat].HP==1;i++) {
                        SewerBeeTest.offer(game,seat);
                        assertTrue("melee owner="+owner.pos+" rat="+rat.pos+" hp="+owner.HP+" ratHP="+rat.HP+" offered="+game.activeSeat()+" sees="+owner.fieldOfView[rat.pos],CoopFloorsTest.step(game,ATTACK,rat.pos,null));
                    }
                } else {
                    SewerBeeTest.offer(game,seat);
                    float charge=armor.charge;
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,mode==0?owner.pos:rat.pos,armor));
                    assertTrue(armor.charge<charge);
                    if (mode==1) assertTrue(wand.curCharges+wand.partialCharge<charges);
                }
                assertTrue("paid damage mode="+mode,rat.HP<before);
                assertTrue("guardian healing mode="+mode,allies[seat].HP>1);
                assertEquals(1,allies[1-seat].HP);
            }
        }
    }
}
