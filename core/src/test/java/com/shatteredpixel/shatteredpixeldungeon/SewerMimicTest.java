package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerMimicTest extends HeadlessDungeon {
    @Test public void eitherMageCanStrikeTheDisguisedChestWithTheStartingStaff() {
        for (int seat=0;seat<2;seat++) {
            try (CoopSession g=CoopSession.open(408,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(g,Dungeon.level.exit());
                SewerBeeTest.offer(g,seat);
                Mimic m=SewerSpecialActorTest.actor(Mimic.class);
                SewerGhostTest.meet(g,seat,m);
                Hero caster=g.hero(seat);
                int hp=m.HP;
                assertTrue(CoopSession.disguised(m));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ZAP,m.pos,caster.belongings.weapon()));
                assertTrue(m.HP<hp);
                assertEquals(Char.Alignment.ENEMY,m.alignment);
            }
        }
    }

    @Test public void goldenMimicRetainsNativeAmbushStatsAndTreasure() {
        for (int seed : new int[]{427,428}) for (int seat=0;seat<2;seat++) {
            Bundle parked;
            int id;
            long prize;
            try (CoopSession game=CoopSession.open(seed,1,HeroClass.MAGE)) {
                game.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(game,Dungeon.level.exit());
                Mimic gold=SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic.class);
                assertTrue(CoopSession.disguised(gold));
                assertFalse(gold.items.isEmpty());
                for(Item item:gold.items) if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem
                        || item instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand) {
                    assertFalse(item.cursed);
                    assertTrue(item.cursedKnown);
                }
                id=gold.id();
                prize=game.ownership.identity(gold.items.get(0));
                SewerBeeTest.cross(game,Dungeon.level.entrance());
                parked=game.checkpoint();
            }
            try(CoopSession game=CoopSession.restore(parked)) {
                SewerBeeTest.cross(game,Dungeon.level.exit());
                Mimic gold=(Mimic)Actor.findById(id);
                SewerBeeTest.offer(game,seat);
                SewerGhostTest.meet(game,seat,gold);
                Hero h=game.hero(seat);
                int initial=h.HP, other=game.hero(1-seat).HP;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.OPEN,gold.pos,null));
                assertTrue(gold.isTargeting(h));
                assertTrue(h.HP<initial);
                assertEquals(other,game.hero(1-seat).HP);
                Item loot=gold.items.get(0);
                gold.damage(gold.HP+100,h);
                assertFalse(gold.isAlive());
                assertTrue(CoopSession.floorItems(Dungeon.level).containsValue(loot));
                assertEquals(prize,game.ownership.identity(loot));
                game.checkpoint();
            }
        }
    }

    @Test public void generatedMimicAmbushesEitherOpenerAndRetainsItsPrizeOnReturn() {
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            int id;
            long prize;
            try (CoopSession g=CoopSession.open(408,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.MAGE);
                SewerBeeTest.cross(g,Dungeon.level.exit());
                Mimic m=SewerSpecialActorTest.actor(Mimic.class);
                id=m.id();
                assertTrue(CoopSession.disguised(m));
                prize=g.ownership.identity(m.items.get(0));
                SewerBeeTest.cross(g,Dungeon.level.entrance());
                assertNull(Actor.findById(id));
                saved=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                SewerBeeTest.cross(g,Dungeon.level.exit());
                Mimic m=(Mimic)Actor.findById(id);
                assertEquals(prize,g.ownership.identity(m.items.get(0)));
                SewerBeeTest.offer(g,seat);
                SewerGhostTest.meet(g,seat,m);
                Hero h=g.hero(seat);
                int hp=h.HP, other=g.hero(1-seat).HP;
                float due=h.cooldown();
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.OPEN,m.pos,null));
                assertEquals(Char.Alignment.ENEMY,m.alignment);
                assertTrue(m.isTargeting(h));
                assertTrue(h.HP<hp || h.shielding()>0);
                assertEquals(other,g.hero(1-seat).HP);
                assertTrue(h.cooldown()>=due);
                saved=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                Mimic m=(Mimic)Actor.findById(id);
                assertEquals(Char.Alignment.ENEMY,m.alignment);
                Item loot=m.items.get(0);
                int goldBefore=Dungeon.gold;
                int expectedGold=m.items.stream().mapToInt(Item::quantity).sum();
                for (int n=0;n<12 && m.isAlive();n++) {
                    Hero caster=g.hero(g.activeSeat());
                    SewerGhostTest.meet(g,g.activeSeat(),m);
                    assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ZAP,m.pos,caster.belongings.weapon()));
                }
                assertFalse(m.isAlive());
                assertTrue(CoopSession.floorItems(Dungeon.level).containsValue(loot));
                assertEquals(prize,g.ownership.identity(loot));
                Hero collector=g.hero(g.activeSeat());
                collector.pos=m.pos;
                assertNull(Actor.findById(id));
                while (Dungeon.level.heaps.get(m.pos)!=null) {
                    Item top=Dungeon.level.heaps.get(m.pos).peek();
                    SewerBeeTest.offer(g,g.playerOf(collector)==g.player(0)?0:1);
                    assertTrue(top instanceof com.shatteredpixel.shatteredpixeldungeon.items.Gold);
                    assertTrue(top.getClass().getSimpleName(),CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,m.pos,top));
                }
                assertEquals(goldBefore+expectedGold,Dungeon.gold);
                g.checkpoint();
            }
        }
    }
}
