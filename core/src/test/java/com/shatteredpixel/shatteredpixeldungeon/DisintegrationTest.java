package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class DisintegrationTest extends HeadlessDungeon {
    @Test public void penetratingBeamAndStaffReachFollowTheActualWandAndMage() {
        for (int seat=0;seat<2;seat++) {
            long id;
            Bundle saved;
            int enemyId;
            try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat a=CoopMageTest.encounter(game,seat);
                Hero caster=TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                MagesStaff staff=(MagesStaff)caster.belongings.weapon();
                a.pos=caster.pos+2;
                Rat b=new Rat();
                b.HP=b.HT=200;
                b.pos=caster.pos+4;
                GameScene.add(b,2000);
                Level.set(caster.pos+3,Terrain.BARRICADE);
                game.observe(seat);
                WandOfDisintegration wand=WandArtsTest.give(game,seat,new WandOfDisintegration());
                id=game.ownership.identity(wand);
                int charges=wand.curCharges;
                assertFalse(CoopFloorsTest.step(game,ZAP,game.hero(1-seat).pos,wand));
                assertEquals(charges,wand.curCharges);
                assertTrue(CoopFloorsTest.step(game,ZAP,a.pos,wand));
                assertTrue(a.HP<a.HT && b.HP<b.HT);
                assertNotEquals(Terrain.BARRICADE,Dungeon.level.map[caster.pos+3]);
                assertEquals(charges-1,wand.curCharges);
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                assertSame(wand,staff.imbuedWand());
                assertTrue(caster.canAttack(a));
                assertEquals("2",ProjectionChecks.seatImage(game,seat).source.rows.get("staffreach"));
                assertNull(ProjectionChecks.seatImage(game,1-seat).source.rows.get("staffreach"));
                saved=game.checkpoint();
                enemyId=a.id();
            }
            try(CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat);
                MagesStaff staff=(MagesStaff)hero.belongings.weapon();
                assertEquals(id,game.ownership.identity(staff.imbuedWand()));
                assertSame(hero,staff.coopWand().chargingHero());
                int pos=hero.pos;
                Rat rat=(Rat)Actor.findById(enemyId);
                Buff.prolong(rat,Paralysis.class,4);
                int hp=rat.HP;
                assertTrue(CoopFloorsTest.step(game,ATTACK,rat.pos,null));
                assertEquals(pos,hero.pos);
                assertTrue(rat.HP<hp);
            }
        }
    }

    @Test public void originalPenetratingBlastAndWildMagicWorkFromBothSeats() {
        for(int seat=0;seat<2;seat++) for(int rank=0;rank<=4;rank++)
            for(Talent talent:new Talent[]{Talent.BLAST_RADIUS,Talent.ELEMENTAL_POWER,Talent.REACTIVE_BARRIER,Talent.WILD_POWER}) {
                try(CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                    game.join(22,HeroClass.MAGE);
                    Rat rat=CoopMageTest.encounter(game,seat);
                    Hero hero=game.hero(seat);
                    boolean wild=talent==Talent.WILD_POWER;
                    ClassArmor armor=ArmorTalentsTest.invest(game,seat,wild ? 1 : 0,talent,rank);
                    WandOfDisintegration wand=WandArtsTest.give(game,seat,new WandOfDisintegration());
                    int hp=rat.HP;
                    if (!wild) {
                        MagesStaff staff=(MagesStaff)hero.belongings.weapon();
                        assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                        if (talent==Talent.BLAST_RADIUS) {
                            rat.pos=hero.pos+5;
                            Level.set(rat.pos,Terrain.EMPTY);
                        }
                        Level.set(hero.pos+2,Terrain.WALL);
                    }
                    SewerBeeTest.offer(game,seat);
                    float before=armor.charge,wandBefore=wand.curCharges+wand.partialCharge;
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,wild ? rat.pos : hero.pos,armor));
                    assertTrue(armor.charge<before);
                    if (talent==Talent.BLAST_RADIUS && rank==0) assertEquals(hp,rat.HP);
                    else assertTrue(hp>rat.HP);
                    if (talent==Talent.ELEMENTAL_POWER) assertTrue(hp-rat.HP>=Math.round(15*(1+.25f*rank)));
                    if (talent==Talent.REACTIVE_BARRIER && rank>0) assertTrue(hero.buff(Barrier.class).shielding()>=Math.round(2.5f*rank)-1);
                    if (wild) assertTrue(wand.curCharges+wand.partialCharge<wandBefore);
                }
            }
    }
}
