package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;

import static org.junit.Assert.*;

/** Staged original seed and enhanced heroes. All boss damage uses an authorized native attack. */
public class TenguArenaTest extends HeadlessDungeon {
    public static void arrive(CoopSession game) {
        WandmakerTest.enterPrison(game);
        while (Dungeon.depth<10) SewerDepthTest.gate(game,true);
        assertEquals(PrisonBossLevel.State.START,floor().state());
        assertEquals(7,game.checkpoint().getInt("schema"));
    }

    public static PrisonBossLevel floor() { return (PrisonBossLevel)Dungeon.level; }

    public static Tengu boss() {
        return (Tengu)floor().mobs.stream().filter(Tengu.class::isInstance).findFirst().orElseThrow(AssertionError::new);
    }

    public static int prepareEntry(CoopSession game, boolean arena) {
        Hero hero=game.hero(game.activeSeat());
        int destination=(arena?8:24)*floor().width()+10;
        hero.pos=destination+(arena?floor().width():-floor().width());
        if (game.hero(1-game.activeSeat()).pos==destination) game.hero(1-game.activeSeat()).pos=hero.pos+1;
        game.observe(game.activeSeat());
        return destination;
    }

    public static void enter(CoopSession game, boolean arena) {
        int destination=prepareEntry(game,arena);
        assertTrue("Entry failed "+game.stopReason(),CoopFloorsTest.step(game,CoopSession.Action.MOVE,destination,null));
    }

    public static void lineUp(CoopSession game) {
        lineUp(game,game.activeSeat(),true);
    }

    private static void lineUp(CoopSession game,int seat,boolean press) {
        Tengu boss=boss();
        Hero hero=game.hero(seat);
        for (int offset:PathFinder.NEIGHBOURS8) {
            int p=boss.pos+offset;
            if (floor().passable[p] && !floor().pit[p] &&
                    (Actor.findChar(p)==null || Actor.findChar(p)==hero)) {
                hero.pos=p;
                if (press) floor().occupyCell(hero);
                game.observe(seat);
                return;
            }
        }
        throw new AssertionError("No safe adjacent fixture cell");
    }

    public static void fight(CoopSession game, PrisonBossLevel.State phase) {
        for (int i=0; i<200 && floor().state()==phase; i++) {
            lineUp(game);
            assertTrue("Attack failed "+game.stopReason(),CoopFloorsTest.step(game,CoopSession.Action.ATTACK,boss().pos,null));
        }
        assertNotEquals(phase,floor().state());
    }

    public static void devices(CoopSession game) {
        Tengu boss=boss();
        Hero victim=game.hero(1);
        assertTrue(Tengu.throwBomb(boss,victim));
        assertTrue(Tengu.throwFire(boss,victim));
        assertTrue(Tengu.throwShocker(boss,victim));
        assertNotNull(boss.buff(Tengu.BombAbility.class));
        assertNotNull(boss.buff(Tengu.FireAbility.class));
        assertNotNull(boss.buff(Tengu.ShockerAbility.class));
    }

    public static void waitOne(CoopSession game) {
        boolean moved=CoopFloorsTest.step(game,CoopSession.Action.WAIT,0,null);
        assertTrue("Wait failed "+game.stopReason(),moved);
    }

    /** Stage only the starting boundary. The subsequent command is issued by an authenticated client. */
    public static int prepareSocket(CoopSession game,String boundary) {
        arrive(game);
        if (boundary.equals("enter")) return prepareEntry(game,false);
        enter(game,false);
        if (!boundary.equals("pause")) {
            fight(game,PrisonBossLevel.State.FIGHT_START);
            if (boundary.equals("arena")) return prepareEntry(game,true);
            enter(game,true);
        }
        if (boundary.equals("devices")) {
            devices(game);
            return 0;
        }
        if (boundary.equals("claim")) {
            fight(game,PrisonBossLevel.State.FIGHT_ARENA);
            for (Heap heap:floor().heaps.valueList()) if (heap.peek() instanceof TengusMask) {
                game.hero(game.activeSeat()).pos=heap.pos;
                game.observe(game.activeSeat());
                return heap.pos;
            }
            throw new AssertionError("Missing mask");
        }
        assertTrue(boundary.equals("kill") || boundary.equals("pause"));
        Tengu enemy=boss();
        int cutoff=(boundary.equals("kill") ? 0 : enemy.HT/2)+enemy.HT/8-1;
        while (enemy.HP>cutoff) {
            lineUp(game);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,enemy.pos,null));
        }
        lineUp(game,0,false);
        lineUp(game,1,false);
        return enemy.pos;
    }

    public static String signature(CoopSession game) {
        Bundle level=new Bundle();
        floor().storeInBundle(level);
        Bundle enemy=level.getBundle("tengu");
        java.util.List<String> fields=new java.util.ArrayList<>();
        fields.add(floor().state().name()+"/"+floor().locked+"/"+game.turn());
        fields.add(java.util.Arrays.toString(com.watabou.utils.Random.worldState()));
        if (!enemy.isNull()) {
            fields.add(enemy.getInt("id")+"/"+enemy.getInt("HP")+"/"+enemy.getInt("pos"));
            java.util.List<String> effects=new java.util.ArrayList<>();
            for (Bundle effect:enemy.getBundleArray("buffs")) effects.add(effect.toString());
            java.util.Collections.sort(effects);
            fields.addAll(effects);
        }
        for (int seat=0;seat<2;seat++) {
            Hero h=game.hero(seat);
            fields.add(h.id()+"/"+h.pos+"/"+h.HP+"/"+h.exp+"/"+h.lvl);
            for (Item i:h.belongings) if (i instanceof TengusMask)
                fields.add("owned-mask/"+game.ownership.identity(i)+"/"+game.ownership.owner(i));
        }
        for (Heap heap:floor().heaps.valueList()) for (Item i:heap.items) if (i instanceof TengusMask)
            fields.add("floor-mask/"+heap.pos+"/"+game.ownership.identity(i));
        return String.join("\n",fields);
    }

    @Test public void naturalHitsAdvanceTheFightAndPendingHazardsRestoreThroughVictory() {
        Bundle saved;
        int bossId;
        long foodId;
        try (CoopSession game=CoopSession.open(570,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.MAGE);
            arrive(game);
            Food food=new Food();
            game.ownership.claim(food,game.player(1));
            foodId=game.ownership.identity(food);
            floor().drop(food,floor().entrance());
            enter(game,false);
            bossId=boss().id();
            assertTrue(floor().locked);
            fight(game,PrisonBossLevel.State.FIGHT_START);
            assertEquals(PrisonBossLevel.State.FIGHT_PAUSE,floor().state());
            enter(game,true);
            assertEquals(bossId,boss().id());
            devices(game);
            waitOne(game);
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            assertEquals(PrisonBossLevel.State.FIGHT_ARENA,floor().state());
            assertEquals(bossId,boss().id());
            assertTrue(floor().locked);
            Bundle old=saved.getBundle("floor").getBundle("tengu");
            Bundle current=new Bundle();
            boss().storeInBundle(current);
            for (Bundle buff:old.getBundleArray("buffs")) {
                Actor restored=Actor.findById(buff.getInt("id"));
                assertTrue(restored instanceof Buff);
                Bundle read=new Bundle(); restored.storeInBundle(read);
                for(String k:new String[]{"time","timer","bomb_pos","shocker_pos","shocking_ordinals","direction","cur_cells"})
                    if (buff.contains(k)) assertEquals(k,buff.getString(k),read.getString(k));
            }
            for(int i=0;i<10;i++) waitOne(game);
            fight(game,PrisonBossLevel.State.FIGHT_ARENA);
            assertEquals(PrisonBossLevel.State.WON,floor().state());
            assertFalse(floor().locked);
            assertNull(Actor.findById(bossId));
            assertNotEquals(game.hero(0).pos,game.hero(1).pos);
            assertTrue(floor().storedEncounterItems().isEmpty());
            assertEquals(1,CoopSession.floorItems(floor()).values().stream().filter(TengusMask.class::isInstance).count());
            assertEquals(1,CoopSession.floorItems(floor()).values().stream().filter(i->game.ownership.identity(i)==foodId && game.ownership.owner(i)==game.player(1)).count());
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            assertEquals(PrisonBossLevel.State.WON,floor().state());
            SewerDepthTest.gate(game,false); assertEquals(9,Dungeon.depth);
            SewerDepthTest.gate(game,true); assertEquals(PrisonBossLevel.State.WON,floor().state());
            assertEquals(1,CoopSession.floorItems(floor()).values().stream().filter(TengusMask.class::isInstance).count());
            waitOne(game);
        }
    }
}
