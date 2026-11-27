package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.Arrays;
import static org.junit.Assert.*;

public class BerserkerEnchantmentCombat extends HeadlessDungeon {
    private static String fight(Bundle save, int seat) {
        try (CoopSession game=CoopSession.restore(save)) {
            Hero hero=game.hero(seat);
            Rat rat=(Rat)Dungeon.level.mobs.iterator().next();
            StringBuilder trace=new StringBuilder();
            for (int attack=0;attack<20;attack++) {
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,rat.pos,null));
                trace.append(rat.buff(Chill.class)==null ? '.' : 'C');
                assertNull(game.hero(1-seat).buff(Blocking.BlockBuff.class));
                assertNull(hero.buff(Blocking.BlockBuff.class));
            }
            return trace+" HP="+rat.HP+" RNG="+Arrays.toString(Random.worldState());
        }
    }

    private static void compare(int seat,int points) {
        Bundle save;
        try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            Rat rat=CoopMageTest.encounter(game,seat);
            CavesBossTest.beside(game,seat,rat.pos);
            Hero owner=game.hero(seat);
            owner.lvl=25;
            owner.subClass=HeroSubClass.BERSERKER;
            Talent.initSubclassTalents(owner);
            SewerBeeTest.offer(game,seat);
            for (int i=0;i<points;i++)
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.ENRAGED_CATALYST.ordinal(),null));
            ((Weapon)owner.belongings.weapon()).enchant(new Chilling());
            ((Weapon)game.hero(1-seat).belongings.weapon()).enchant(new Blocking());
            Buff.affect(owner,Berserk.class).damage(4*owner.HT);
            save=game.checkpoint();
        }
        String first=fight(save,seat);
        assertEquals(first,fight(save,seat));
        System.out.printf("RAGE_ATTACKS seat=%d rank=%d first=20 replay=20 %s%n",seat,points,first);
    }

    public static void main(String[] args) throws Exception {
        BerserkerEnchantmentCombat test=new BerserkerEnchantmentCombat();
        test.temporary.create();test.startBackend();
        try {
            for(int seat=0;seat<2;seat++) for(int rank=0;rank<=3;rank++) compare(seat,rank);
        } finally {test.stopBackend();test.temporary.delete();}
    }
}
