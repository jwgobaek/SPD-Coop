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
    public static boolean isThrowFixture(String name) {
        return name.matches("bowthrow(Blooming|Elastic|Lucky|Unstable|Corrupting|Grim)[01]");
    }

    static StagedInput prepareThrow(CoopSession game, int seat, String name) {
        Hero owner = TierGrowthTest.prepare(game,seat,HeroSubClass.SNIPER,Talent.SHARED_ENCHANTMENT,3);
        String enchantment = name.substring("bowthrow".length(),name.length()-1);
        Weapon.Enchantment effect;
        switch (enchantment) {
            case "Blooming": effect=new Blooming(); break;
            case "Elastic": effect=new Elastic(); break;
            case "Lucky": effect=new Lucky(); break;
            case "Unstable": effect=new Unstable(); break;
            case "Corrupting": effect=new Corrupting(); break;
            case "Grim": effect=new Grim(); break;
            default: throw new IllegalArgumentException(name);
        }
        owner.belongings.getItem(SpiritBow.class).enchant(effect);
        ThrowingStone stones=owner.belongings.getItem(ThrowingStone.class);
        if (stones==null) {
            stones=new ThrowingStone();
            assertTrue(stones.collect(owner.belongings.backpack));
            game.ownership.claim(stones,game.player(seat));
        }
        stones.identify();
        stones.quantity(4);
        Rat rat=CoopMageTest.encounter(game,seat);
        rat.maxLvl=owner.lvl;
        rat.HP=1;
        Buff.prolong(rat,Paralysis.class,10);
        if (effect instanceof Elastic || effect instanceof Unstable) {
            for (int cell=rat.pos+1;cell<=rat.pos+2;cell++) {
                Dungeon.level.traps.remove(cell);
                Dungeon.level.plants.remove(cell);
                com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);
            }
        }
        SewerBeeTest.offer(game,seat);
        return new StagedInput(game,"THROW",rat.pos);
    }

    @Test public void nativeBowAndSharedMissilesUseTheShootingSeatsEnchantment() {
        for (Weapon.Enchantment effect:new Weapon.Enchantment[]{new Venomous(),new Vorpal(),new Shocking(),new Eldritch(),new Vampiric(),new Blooming(),new Grim(),new Lucky(),new Corrupting(),new Elastic(),new Unstable()})
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
                        stones.quantity(200);
                        assertTrue(stones.collect(owner.belongings.backpack));
                        game.ownership.claim(stones,game.player(seat));
                    }
                    for (int attempt=0;attempt<160 && !proc;attempt++) {
                        Rat target=CoopMageTest.encounter(game,seat);
                        Rat witness=new Rat();
                        witness.pos=target.pos+Dungeon.level.width();
                        witness.HP=witness.HT=100;
                        GameScene.add(witness,2000);
                        witness.fieldOfView=new boolean[Dungeon.level.length()];
                        Dungeon.level.updateFieldOfView(witness,witness.fieldOfView);
                        if (effect instanceof Corrupting) target.HP = 1;
                        if (effect instanceof Grim) {
                            target.HT = 1000;
                            target.HP = 60;
                        }
                        if (effect instanceof Lucky) {
                            // The native loot roll has an experience-level ceiling.
                            target.maxLvl = owner.lvl;
                            target.HP = 1;
                            Dungeon.level.heaps.remove(target.pos);
                        }
                        int impact = target.pos;
                        if (effect instanceof Elastic || effect instanceof Unstable) {
                            for (int cell = impact + 1; cell <= impact + 2; cell++) {
                                Dungeon.level.traps.remove(cell);
                                Dungeon.level.plants.remove(cell);
                                com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell, com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);
                            }
                        }
                        Buff.prolong(target,Paralysis.class,10);
                        SewerBeeTest.offer(game,seat);
                        owner.HP=12;
                        int peerHP=peer.HP;
                        // Replay a fixed sequence of attack rolls. The assertion checks the
                        // native effect at each rank, not a statistical lower bound on procs.
                        com.watabou.utils.Random.pushGenerator(0x9E3779B97F4A7C15L*(attempt+1));
                        try {
                            if (shared) {
                                assertTrue(CoopFloorsTest.step(game,THROW,target.pos,owner.belongings.getItem(ThrowingStone.class)));
                            } else {
                                assertTrue(effect.getClass().getSimpleName()+" seat "+seat+" rank "+rank+" attempt "+attempt,
                                        CoopFloorsTest.step(game,SHOOT,target.pos,bow));
                            }
                        } finally {
                            com.watabou.utils.Random.popGenerator();
                        }
                        if (effect instanceof Blooming)
                            proc = Dungeon.level.map[impact] == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.HIGH_GRASS;
                        if (effect instanceof Corrupting) {
                            proc = Dungeon.level.mobs.stream().anyMatch(m -> m.alignment == com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY
                                    && m.buff(Corruption.class) != null && m.buff(Corruption.class).casterId() == owner.id());
                            if (proc) game.validateActorGraph();
                        }
                        if (effect instanceof Elastic) proc = target.pos != impact;
                        if (effect instanceof Unstable)
                            proc = target.pos != impact || target.buff(Poison.class) != null || target.buff(Bleeding.class) != null
                                    || witness.HP < 100 || owner.HP > 12 || target.buff(Burning.class) != null
                                    || owner.buff(Blocking.BlockBuff.class) != null || target.buff(Chill.class) != null
                                    || Dungeon.level.map[impact] == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.HIGH_GRASS;
                        if (effect instanceof Grim) proc = !target.isAlive();
                        if (effect instanceof Lucky) {
                            com.shatteredpixel.shatteredpixeldungeon.items.Heap heap = Dungeon.level.heaps.get(impact);
                            proc = heap != null && heap.items.stream().anyMatch(item -> !(item instanceof ThrowingStone));
                        }
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
