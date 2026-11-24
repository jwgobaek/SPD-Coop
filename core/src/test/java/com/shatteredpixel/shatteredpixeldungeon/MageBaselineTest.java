package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MageBaselineTest extends SeededFloorBaseline {
    @Test public void soloStarterStaffUsesOriginalDamageAndChargeCompletion() {
        Dungeon.seed=2402;
        GamesInProgress.selectedClass=HeroClass.MAGE;
        Dungeon.init();
        Dungeon.switchLevel(Dungeon.newLevel(),-1);
        Hero hero=Dungeon.hero;
        new TestSprite(hero);
        Rat rat=new Rat();
        int cell=hero.pos+1;
        Level.set(cell,Terrain.EMPTY);
        rat.pos=cell;rat.HT=rat.HP=60;
        new TestSprite(rat);
        Actor.add(rat);
        MagesStaff staff=
                (MagesStaff)hero.belongings.weapon();
        WandOfMagicMissile wand=staff.starterWand();
        staff.execute(hero,"ZAP");
        Ballistica ray=
                new Ballistica(hero.pos,cell,wand.collisionProperties(cell));
        assertEquals(cell,ray.collisionPos.intValue());
        int charge=wand.curCharges;
        float due=hero.cooldown();
        wand.onZap(ray); wand.wandUsed();
        org.junit.Assert.assertTrue(60-rat.HP>=wand.min() && 60-rat.HP<=wand.max());
        assertEquals(charge-1,wand.curCharges);
        assertEquals(1f,hero.cooldown()-due,0.001f);
    }

    @Test(timeout = 30000)
    public void generatedFloorMatchesUpstream() throws Exception {
        assertEquals("c3a9a3929a3a9755cf9e6dad58c315f5da69869420217a2a23ff60ff763805e9", snapshot(2402, HeroClass.MAGE));
    }
}
