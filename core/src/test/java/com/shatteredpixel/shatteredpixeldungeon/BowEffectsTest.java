package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;

public class BowEffectsTest extends HeadlessDungeon {
    @Test public void nativeBowAndSharedMissilesUseTheShootingSeatsEnchantment() {
        for (Weapon.Enchantment effect:new Weapon.Enchantment[]{new Venomous(),new Vorpal(),new Shocking(),new Eldritch(),new Vampiric()})
            for (int seat=0;seat<2;seat++) for (boolean shared:new boolean[]{false,true}) for (int rank=0;rank<=3;rank++) {
                boolean proc=false;
                try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                    game.join(22,HeroClass.HUNTRESS);
                    Hero owner=TierGrowthTest.prepare(game,seat,HeroSubClass.SNIPER,Talent.SHARED_ENCHANTMENT,rank);
                    Hero peer=game.hero(1-seat);
                    SpiritBow bow=owner.belongings.getItem(SpiritBow.class);
                    bow.enchant(effect);
                    owner.HP=owner.HT=100;
                    Buff.detach(owner,Regeneration.class);
                    Buff.detach(peer,Regeneration.class);
                    ThrowingStone stones=new ThrowingStone();
                    if (shared) {
                        stones.quantity(100);
                        assertTrue(stones.collect(owner.belongings.backpack));
                        game.ownership.claim(stones,game.player(seat));
                    }
                    for (int attempt=0;attempt<96 && !proc;attempt++) {
                        Rat target=CoopMageTest.encounter(game,seat);
                        Rat witness=new Rat();
                        witness.pos=target.pos+Dungeon.level.width();
                        witness.HP=witness.HT=100;
                        GameScene.add(witness,2000);
                        witness.fieldOfView=new boolean[Dungeon.level.length()];
                        Dungeon.level.updateFieldOfView(witness,witness.fieldOfView);
                        Buff.prolong(target,Paralysis.class,10);
                        SewerBeeTest.offer(game,seat);
                        owner.HP=12;
                        int peerHP=peer.HP;
                        if (shared) {
                            assertTrue(CoopFloorsTest.step(game,THROW,target.pos,owner.belongings.getItem(ThrowingStone.class)));
                        } else assertTrue(CoopFloorsTest.step(game,SHOOT,target.pos,bow));
                        if (effect instanceof Venomous) proc=target.buff(Poison.class)!=null;
                        if (effect instanceof Vorpal) proc=target.buff(Bleeding.class)!=null;
                        if (effect instanceof Shocking) proc=witness.HP<100;
                        if (effect instanceof Eldritch) proc=witness.buff(Terror.class)!=null && witness.buff(Terror.class).object==owner.id();
                        if (effect instanceof Vampiric) proc=owner.HP>12;
                        assertEquals(peerHP,peer.HP);
                    }
                    assertEquals(effect.getClass().getSimpleName()+" seat "+seat+" shared "+shared+" rank "+rank,!shared || rank>0,proc);
                    game.checkpoint();
                }
            }
    }
}
