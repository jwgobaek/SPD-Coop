package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class PrismaticLightTest extends HeadlessDungeon {
    @Test public void zapMapsOnlyForCasterAndTheSameImbuedWandCripplesInMelee() {
        for (int seat=0; seat<2; seat++) {
            Bundle saved;
            int mobId, cell;
            long id;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                WandOfPrismaticLight wand=WandArtsTest.give(game,seat,new WandOfPrismaticLight());
                id=game.ownership.identity(wand);
                cell=hero.pos+1-Dungeon.level.width();
                Level.set(cell,Terrain.SECRET_DOOR);
                int charge=wand.curCharges;
                Dungeon.level.viewDistance=4;
                assertTrue(CoopFloorsTest.step(game,ZAP,rat.pos,wand));
                assertEquals(charge-1,wand.curCharges);
                assertTrue(rat.HP<rat.HT);
                assertNotNull(hero.buff(Light.class));
                assertEquals(Terrain.DOOR,Dungeon.level.map[cell]);
                assertTrue(game.knowledge(game.hero(seat)).mapped(cell));
                assertFalse(game.knowledge(game.hero(1-seat)).mapped(cell));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),hero.belongings.weapon()));
                rat.pos=hero.pos+1;
                mobId=rat.id();
                Buff.prolong(rat,Paralysis.class,10);
                game.observe(seat);
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat);
                assertEquals(id,game.ownership.identity(((MagesStaff)hero.belongings.weapon()).imbuedWand()));
                assertTrue(game.knowledge(game.hero(seat)).mapped(cell));
                SewerBeeTest.offer(game,seat);
                Rat rat=(Rat)Actor.findById(mobId);
                assertTrue(CoopFloorsTest.step(game,ATTACK,rat.pos,null));
                assertNotNull(rat.buff(Cripple.class));
            }
        }
    }

    @Test public void blastAndVolleyUseTheActualWandOnBothSeats() {
        for (int seat=0; seat<2; seat++) for(boolean volley:new boolean[]{false,true}) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat rat=CoopMageTest.encounter(game,seat);
                Hero hero=game.hero(seat);
                ClassArmor armor=ArmorTalentsTest.invest(game,seat,volley?1:0,volley?Talent.WILD_POWER:Talent.ELEMENTAL_POWER,2);
                WandOfPrismaticLight wand=WandArtsTest.give(game,seat,new WandOfPrismaticLight());
                if (!volley) assertTrue(CoopFloorsTest.step(game,IMBUE,WandArtsTest.choice(game,seat,wand),hero.belongings.weapon()));
                SewerBeeTest.offer(game,seat);
                float charge=armor.charge, shots=wand.curCharges+wand.partialCharge;
                assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,volley?rat.pos:hero.pos,armor));
                assertTrue(rat.HP<rat.HT);
                assertTrue(armor.charge<charge);
                assertTrue(game.knowledge(game.hero(seat)).mapped(hero.pos+1));
                assertFalse(game.knowledge(game.hero(1-seat)).mapped(hero.pos+1));
                if (volley) assertTrue(wand.curCharges+wand.partialCharge<shots);
                else {
                    assertNotNull(hero.buff(Light.class));
                    assertNotNull(rat.buff(Blindness.class));
                }
                game.checkpoint();
            }
        }
    }
}
