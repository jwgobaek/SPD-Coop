package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Owner-only isolated trial. Runs the actual generated mask and native subclass combat on either seat. */
public class WarriorGrowthCombat extends HeadlessDungeon {
    private static class Callback extends TengusMask {
        static void select(TengusMask original,Hero owner,HeroSubClass choice) {
            Hero prior=curUser;
            CharSprite live=owner.sprite;
            try {
                new NativeGrowthChoiceProbe.QuietSprite(owner);
                curUser=owner;
                original.choose(choice);
            } finally {
                curUser=prior;
                owner.sprite=live;
            }
        }
    }

    private static void run(int seat,HeroSubClass subclass) {
        Bundle checkpoint;
        int actor;
        long maskId;
        float power=0;
        int hits=0;
        try(CoopSession game=CoopSession.open(578,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            int cell=TenguArenaTest.prepareSocket(game,"claim");
            while (game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
            Hero owner=game.hero(seat);
            if (game.hero(1-seat).pos==cell) CavesBossTest.beside(game,1-seat,cell);
            owner.pos=cell;
            game.observe(seat);
            TengusMask mask=(TengusMask)Dungeon.level.heaps.get(cell).peek();
            maskId=game.ownership.identity(mask);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,mask));
            assertEquals(game.player(seat),game.ownership.owner(mask));
            Callback.select(mask,owner,subclass);
            assertSame(subclass,owner.subClass);
            assertNull(owner.belongings.getItem(TengusMask.class));
            assertNull(game.hero(1-seat).belongings.getItem(TengusMask.class));
            assertSame(HeroSubClass.NONE,game.hero(1-seat).subClass);
            assertFalse(owner.talents.get(2).isEmpty());
            SewerDepthTest.gate(game,false);
            Guard enemy=CavesInteriorTest.stage(game,Guard.class,seat);
            CavesBossTest.beside(game,seat,enemy.pos);
            if (subclass==HeroSubClass.BERSERKER) {
                enemy.attack(owner,1f,15,Float.POSITIVE_INFINITY);
                Berserk rage=owner.buff(Berserk.class);
                assertNotNull(rage);
                Bundle state=new Bundle();rage.storeInBundle(state);power=state.getFloat("power");
                assertTrue(power>0 && rage.damageFactor(100)>100f);
                assertNull(game.hero(1-seat).buff(Berserk.class));
                actor=rage.id();
            } else {
                for (int i=0;i<20 && owner.buff(Combo.class)==null;i++) {
                    if (seat!=game.activeSeat()) TenguArenaTest.waitOne(game);
                    else {
                        CavesBossTest.beside(game,seat,enemy.pos);
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,enemy.pos,null));
                    }
                }
                Combo combo=owner.buff(Combo.class);
                assertNotNull(combo);hits=combo.getComboCount();assertTrue(hits>0);
                assertNull(game.hero(1-seat).buff(Combo.class));
                actor=combo.id();
            }
            checkpoint=game.checkpoint();
        }
        try(CoopSession game=CoopSession.restore(checkpoint)) {
            Hero h=game.hero(seat);
            assertSame(subclass,h.subClass);
            assertSame(HeroSubClass.NONE,game.hero(1-seat).subClass);
            assertNull(h.belongings.getItem(TengusMask.class));
            if (subclass==HeroSubClass.BERSERKER) {
                assertSame(h.buff(Berserk.class),Actor.findById(actor));
                Bundle saved=new Bundle();h.buff(Berserk.class).storeInBundle(saved);
                assertEquals(power,saved.getFloat("power"),0f);
            } else {
                assertSame(h.buff(Combo.class),Actor.findById(actor));
                assertEquals(hits,h.buff(Combo.class).getComboCount());
            }
            System.out.printf("OWNER_TRIAL\t%d\t%s\t%d\t%d\t%.5f\t%d%n",seat,subclass,maskId,actor,power,hits);
        }
    }

    public static void main(String[] args) throws Exception {
        WarriorGrowthCombat p=new WarriorGrowthCombat();
        p.temporary.create();p.startBackend();
        try {
            for(int seat=0;seat<2;seat++) {
                run(seat,HeroSubClass.BERSERKER);
                run(seat,HeroSubClass.GLADIATOR);
            }
        } finally {p.stopBackend();p.temporary.delete();}
    }
}
