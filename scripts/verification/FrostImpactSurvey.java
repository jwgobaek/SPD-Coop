package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom.EternalFire;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;

import static org.junit.Assert.*;

public class FrostImpactSurvey extends HeadlessDungeon {
    private static void inspect(int seat) {
        Bundle before;
        int target;
        try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
            game.join(22, HeroClass.MAGE);
            Rat rat = CoopMageTest.encounter(game, seat);
            SewerBeeTest.offer(game, seat);
            target = rat.id();
            WandArtsTest.give(game, seat, new WandOfFrost());
            before = game.checkpoint();
        }
        int[] damage = new int[2];
        for (int i = 0; i < 2; i++) {
            try (CoopSession game = CoopSession.restore(before)) {
                Rat rat = (Rat) Actor.findById(target);
                if (i == 1) Buff.prolong(rat, Chill.class, 10);
                WandOfFrost wand = game.hero(seat).belongings.getItem(WandOfFrost.class);
                int hp = rat.HP;
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ZAP, rat.pos, wand));
                damage[i] = hp - rat.HP;
            }
        }
        assertEquals(Math.round(damage[0] * Math.pow(0.9333f, 10)), damage[1]);
        try (CoopSession game = CoopSession.restore(before)) {
            Hero hero = game.hero(seat);
            WandOfFrost wand = hero.belongings.getItem(WandOfFrost.class);
            int gap = hero.pos - 2;
            Level.set(gap - 1, Terrain.WALL);
            Level.set(gap, Terrain.TRAP);
            WornDartTrap trap = new WornDartTrap();
            Dungeon.level.setTrap(trap.reveal(), gap);
            game.observe(seat);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ZAP, gap, wand));
            assertFalse(trap.active);
        }
        try (CoopSession game = CoopSession.restore(before)) {
            Hero hero = game.hero(seat);
            WandOfFrost wand = hero.belongings.getItem(WandOfFrost.class);
            int gap = hero.pos + 2;
            EternalFire flame = Blob.seed(gap, 1, EternalFire.class);
            GameScene.add(flame);
            game.observe(seat);
            assertTrue(flame.volume > 0);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ZAP, gap, wand));
            assertEquals(0, flame.volume);
            assertTrue(Dungeon.level.passable[gap]);
        }
        System.out.printf("PASS seat=%d firstDamage=%d chilledDamage=%d floorTrap=spent eternalFire=cleared%n",
                seat, damage[0], damage[1]);
    }

    public static void main(String[] args) throws Exception {
        FrostImpactSurvey survey = new FrostImpactSurvey();
        survey.temporary.create();
        survey.startBackend();
        try {
            inspect(0);
            inspect(1);
        } finally {
            survey.stopBackend();
            survey.temporary.delete();
        }
    }
}
