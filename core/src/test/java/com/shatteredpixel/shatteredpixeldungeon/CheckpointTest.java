package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class CheckpointTest extends HeadlessDungeon {
    @Test public void progressionEquipmentBuffClocksAndNestedRandomStreamsRoundTrip() {
        Bundle saved;
        Talent[] talents=new Talent[2];
        int[] buffs=new int[2];
        float[] delays=new float[2];
        long inner, outer;
        try(CoopSession game=CoopSession.open(402,11,HeroClass.WARRIOR)) {
            game.join(18,HeroClass.HUNTRESS);
            for(int seat=0;seat<2;seat++) {
                Hero h=game.hero(seat);
                h.lvl=3+seat; h.exp=5+seat; h.STR=12+seat; h.HT=29+seat; h.HP=18+seat;
                h.belongings.weapon.level(2+seat);
                talents[seat]=h.talents.get(0).keySet().iterator().next();
                h.talents.get(0).put(talents[seat],seat+1);
                Haste buff=Buff.prolong(h,Haste.class,4+seat);
                buffs[seat]=buff.id(); delays[seat]=buff.cooldown();
            }
            Random.pushGenerator(901);
            Random.Long();
            saved=game.checkpoint();
            inner=Random.Long(); Random.popGenerator(); outer=Random.Long();
        }
        try(CoopSession game=CoopSession.restore(saved)) {
            assertEquals(inner,Random.Long()); Random.popGenerator(); assertEquals(outer,Random.Long());
            for(int seat=0;seat<2;seat++) {
                Hero h=game.hero(seat);
                assertEquals(3+seat,h.lvl); assertEquals(5+seat,h.exp); assertEquals(12+seat,h.STR);
                assertEquals(29+seat,h.HT); assertEquals(18+seat,h.HP);
                assertEquals(2+seat,h.belongings.weapon.level());
                assertEquals(seat+1,h.pointsInTalent(talents[seat]));
                assertSame(h.buff(Haste.class),Actor.findById(buffs[seat]));
                assertEquals(delays[seat],Actor.findById(buffs[seat]).cooldown(),0);
            }
        }
    }

    @Test public void inconsistentReceiptDoesNotRestoreASeatLedger() {
        try(CoopSession game=CoopSession.open(402,1,HeroClass.WARRIOR)) {
            game.join(2,HeroClass.HUNTRESS);
            com.shatteredpixel.shatteredpixeldungeon.lan.LedgerChecks.verify(game);
        }
    }

    @Test public void twentyDiskRestorationsKeepInputAndNextActionAcrossTwoSeeds() throws Exception {
        int restoredCount=0;
        for(long seed:new long[]{402,2402}) {
            java.io.File file=temporary.newFile();
            HeroClass host=seed==402 ? HeroClass.WARRIOR : HeroClass.HUNTRESS;
            HeroClass guest=host==HeroClass.WARRIOR ? HeroClass.HUNTRESS : HeroClass.WARRIOR;
            long turn;
            int next, actor;
            float now;
            try(CoopSession game=CoopSession.open(seed,71,host)) {
                game.join(94,guest);
                turn=game.turn(); next=game.activeSeat(); actor=game.hero(next).id(); now=Actor.now();
                assertTrue(Bundle.write(game.checkpoint(),new java.io.FileOutputStream(file),false));
            }
            for(int n=0;n<10;n++) {
                Bundle saved;
                try(java.io.InputStream in=new java.io.FileInputStream(file)) { saved=Bundle.read(in); }
                try(CoopSession game=CoopSession.restore(saved)) {
                    assertEquals(turn,game.turn()); assertEquals(now,Actor.now(),0);
                    assertEquals(next,game.activeSeat()); assertEquals(actor,game.hero(next).id());
                    assertTrue(game.act(game.player(next),turn,CoopSession.Action.WAIT,0,null));
                    assertEquals(turn+1,game.turn());
                    turn=game.turn(); next=game.activeSeat(); actor=game.hero(next).id(); now=Actor.now();
                    assertTrue(Bundle.write(game.checkpoint(),new java.io.FileOutputStream(file),false));
                    restoredCount++;
                }
            }
        }
        System.out.println("engine disk restorations="+restoredCount+" subsequent actions="+restoredCount);
    }

    @Test public void rejectsInconsistentWorldWithoutInstallingIt() {
        Bundle b;
        try (CoopSession session=CoopSession.open(402,1,HeroClass.WARRIOR)) {
            session.join(2,HeroClass.HUNTRESS);
            b=session.checkpoint();
        }
        Bundle items=b.getBundle("items");
        Bundle first=items.getBundle("s0_0"), second=items.getBundle("s0_1");
        long id=first.getLong("id");
        first.put("id",second.getLong("id"));
        assertThrows(IllegalArgumentException.class,()->CoopSession.restore(b));
        assertNull(Dungeon.coop);
        first.put("id",id);
        first.put("owner",3);
        assertThrows(IllegalArgumentException.class,()->CoopSession.restore(b));
        first.put("owner",1);
        Bundle guest=b.getBundle("guest");
        int actor=guest.getInt("id");
        guest.put("id",b.getBundle("world").getBundle("hero").getInt("id"));
        assertThrows(IllegalArgumentException.class,()->CoopSession.restore(b));
        guest.put("id",actor);
        b.put("schema",99);
        assertThrows(IllegalArgumentException.class,()->CoopSession.restore(b));
        assertNull(Dungeon.coop);
    }

    @Test public void casualtiesAndCompleteSchedulerRoundTrip() throws Exception {
        Bundle checkpoint;
        long turn, ownerId, expectedRoll;
        int actor, gear, rat, pos, tile;
        float clock;
        try(CoopSession game=CoopSession.open(402,71,HeroClass.WARRIOR)) {
            game.join(94,HeroClass.HUNTRESS);
            Hero guest=game.hero(1);
            assertTrue(new Ankh().collect(guest.belongings.backpack));
            guest.HP=17;
            guest.damage(10000,this);
            Mob mob=Dungeon.level.mobs.iterator().next();
            rat=mob.id(); actor=guest.id(); pos=guest.pos;
            Food food=game.hero(0).belongings.getItem(Food.class);
            assertTrue(game.act(71,game.turn(),CoopSession.Action.DROP,game.hero(0).pos,food));
            Item dropped=Dungeon.level.heaps.get(game.hero(0).pos).peek();
            ownerId=game.ownership.identity(dropped);
            gear=guest.belongings.weapon.level();
            tile=game.knowledge(guest).terrain(pos);
            mob.aggro(guest);
            assertTrue(mob.isTargeting(guest));
            clock=Actor.now(); turn=game.turn();
            checkpoint=game.checkpoint();
            expectedRoll=Random.Long();
        }
        try(CoopSession restored=CoopSession.restore(checkpoint)) {
            Hero guest=restored.hero(1);
            assertEquals(expectedRoll,Random.Long());
            assertEquals(clock,Actor.now(),0); assertEquals(turn,restored.turn());
            assertEquals(actor,guest.id()); assertEquals(pos,guest.pos);
            assertTrue(((Mob)Actor.findById(rat)).isTargeting(guest));
            assertEquals(gear,guest.belongings.weapon.level()); assertEquals(tile,restored.knowledge(guest).terrain(pos));
            Item drop=Dungeon.level.heaps.get(restored.hero(0).pos).peek();
            assertEquals(ownerId,restored.ownership.identity(drop)); assertEquals(71,restored.ownership.owner(drop));
            assertEquals(CoopSession.Life.DOWNED,restored.life(1)); assertTrue(guest.frozenInCoop());
            assertTrue(restored.act(71,turn,CoopSession.Action.REVIVE,pos,null));
            assertEquals(5,guest.HP); assertNull(guest.belongings.getItem(Ankh.class));
            checkpoint=restored.checkpoint();
        }
        try(CoopSession game=CoopSession.restore(checkpoint)) {
            assertFalse(game.hero(1).frozenInCoop());
            assertNull(game.hero(1).belongings.getItem(Ankh.class));
            assertTrue(game.act(game.player(game.activeSeat()),game.turn(),CoopSession.Action.WAIT,0,null));
            for(int seat=0;seat<2;seat++) game.hero(seat).damage(10000,this);
            checkpoint=game.checkpoint();
        }
        java.io.File terminal=temporary.newFile();
        assertTrue(Bundle.write(checkpoint,new java.io.FileOutputStream(terminal),false));
        try(java.io.InputStream in=new java.io.FileInputStream(terminal);
            CoopSession game=CoopSession.restore(Bundle.read(in))) {
            assertTrue(game.defeated()); assertEquals(-1,game.activeSeat());
        }
    }
}
