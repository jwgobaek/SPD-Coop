package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
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
        if (boundary.equals("ten")) {
            WandmakerTest.enterPrison(game);
            while (Dungeon.depth<9) SewerDepthTest.gate(game,true);
            SewerSpecialActorTest.atGate(game,Dungeon.level.exit());
            return Dungeon.level.exit();
        }
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
        if (boundary.equals("claim") || boundary.equals("caves") || boundary.equals("return")) {
            fight(game,PrisonBossLevel.State.FIGHT_ARENA);
            if (!boundary.equals("claim")) {
                if (boundary.equals("return")) SewerDepthTest.gate(game,true);
                int gate=boundary.equals("return") ? Dungeon.level.entrance() : Dungeon.level.exit();
                SewerSpecialActorTest.atGate(game,gate);
                return gate;
            }
            for (Heap heap:floor().heaps.valueList()) if (heap.items.stream().anyMatch(TengusMask.class::isInstance)) {
                prepareMaskClaim(game, heap);
                return heap.pos;
            }
            throw new AssertionError("Missing mask");
        }
        if (boundary.equals("down") || boundary.equals("revive")) {
            int seat=1-game.activeSeat();
            Hero hero=game.hero(seat);
            assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.Ankh().collect(hero.belongings.backpack));
            Hero ally=game.hero(1-seat);
            for (int offset:PathFinder.NEIGHBOURS8) {
                int target=ally.pos+offset;
                if (!floor().passable[target] || floor().pit[target] || Actor.findChar(target)!=null) continue;
                hero.pos=target;
                // This fixture measures revival followed by arena damage. Start a fresh native
                // regeneration cycle so a nearly complete healing tick cannot raise HP above five.
                if (boundary.equals("revive") && hero.buff(Regeneration.class)!=null) {
                    hero.buff(Regeneration.class).detach();
                    Buff.affect(hero,Regeneration.class);
                }
                hero.HP=17;
                hero.damage(999,Tengu.class);
                assertEquals(CoopSession.Life.DOWNED,game.life(seat));
                game.observe(1-seat);
                return hero.pos;
            }
            throw new AssertionError("Missing safe downed position");
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
        Dungeon.level.storeInBundle(level);
        Bundle enemy=level.getBundle("tengu");
        java.util.List<String> fields=new java.util.ArrayList<>();
        fields.add(Dungeon.depth+"/"+level.getString("state")+"/"+Dungeon.level.locked+"/"+game.turn());
        java.util.List<String> actors=new java.util.ArrayList<>();
        for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob:Dungeon.level.mobs)
            actors.add(mob.id()+"/"+mob.getClass().getSimpleName()+"/"+mob.pos+"/"+mob.HP);
        java.util.Collections.sort(actors);
        fields.addAll(actors);
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
            fields.add(h.id()+"/"+h.pos+"/"+h.HP+"/"+h.exp+"/"+h.lvl+"/"+game.life(seat)+"/"+game.preDeathHP(seat));
            for (Item i:h.belongings) if (i instanceof TengusMask)
                fields.add("owned-mask/"+game.ownership.identity(i)+"/"+game.ownership.owner(i));
        }
        for (Heap heap:Dungeon.level.heaps.valueList()) for (Item i:heap.items) if (i instanceof TengusMask)
            fields.add("floor-mask/"+heap.pos+"/"+game.ownership.identity(i));
        return String.join("\n",fields);
    }

    private static void prepareMaskClaim(CoopSession game, Heap heap) {
        int collector = game.activeSeat();
        if (game.hero(1-collector).pos == heap.pos) SewerSpecialActorTest.atGate(game,heap.pos);
        game.hero(collector).pos = heap.pos;
        game.observe(collector);
        // Picking up a key above the mask can give the next turn to the other player.
        while (!(heap.peek() instanceof TengusMask)) {
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,heap.pos,heap.peek()));
            SewerBeeTest.offer(game,collector);
        }
        assertEquals(heap.pos,game.hero(game.activeSeat()).pos);
    }

    @Test public void collectorKeepsTheMaskPickupTurnWhenAnIronKeyCoversIt() {
        for (int collector = 0; collector < 2; collector++) {
            try (CoopSession game=CoopSession.open(570,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.MAGE);
                SewerBeeTest.offer(game,collector);
                Hero hero=game.hero(collector);
                TengusMask mask=new TengusMask();
                Heap heap=Dungeon.level.drop(mask,hero.pos);
                Item key=new com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey(Dungeon.depth);
                heap.drop(key);
                assertSame(key,heap.peek());
                prepareMaskClaim(game,heap);
                assertEquals(collector,game.activeSeat());
                assertNotEquals(hero.pos,game.hero(1-collector).pos);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,heap.pos,mask));
                assertSame(mask,hero.belongings.getItem(TengusMask.class));
                assertEquals(game.player(collector),game.ownership.owner(mask));
            }
        }
    }

    @Test public void bothSeatsObserveTheSameNativeHazardTrajectoryAfterRestore() {
        Bundle checkpoint;
        java.util.List<String> trajectory=new java.util.ArrayList<>();
        try (CoopSession g=CoopSession.open(570,11,HeroClass.WARRIOR)) {
            g.join(22,HeroClass.MAGE);
            prepareSocket(g,"devices");
            checkpoint=g.checkpoint();
            for (int i=0;i<14;i++) {
                waitOne(g);
                trajectory.add(signature(g));
            }
        }
        try (CoopSession g=CoopSession.restore(checkpoint)) {
            for (String expected:trajectory) {
                waitOne(g);
                assertEquals(expected,signature(g));
            }
        }
    }

    @Test public void downedPartnerFollowsTheNativeRoomChangeAndKeepsTheirAnkh() {
        try (CoopSession g=CoopSession.open(570,11,HeroClass.WARRIOR)) {
            g.join(22,HeroClass.MAGE);
            arrive(g);
            int cell=prepareEntry(g,false);
            int fallen=1-g.activeSeat();
            Hero hero=g.hero(fallen);
            assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.Ankh().collect(hero.belongings.backpack));
            int id=hero.id();
            hero.HP=17; hero.damage(999,Tengu.class);
            assertEquals(CoopSession.Life.DOWNED,g.life(fallen));
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
            assertTrue(floor().locked);
            assertEquals(-1,g.stairs.stair());
            assertEquals(id,hero.id());
            assertTrue(floor().passable[hero.pos]);
            assertNotEquals(g.hero(1-fallen).pos,hero.pos);
            assertTrue(floor().adjacent(g.hero(1-fallen).pos,hero.pos));
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.REVIVE,hero.pos,null));
            assertEquals(5,hero.HP);
            assertNull(hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.Ankh.class));
        }
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
            SewerDepthTest.gate(game,true);
            assertTrue(Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel);
            assertEquals(11,Dungeon.depth);
            saved=game.checkpoint();
            assertEquals(7,saved.getInt("schema"));
            waitOne(game);
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            assertEquals(11,Dungeon.depth);
            waitOne(game);
            SewerDepthTest.gate(game,false);
            assertEquals(PrisonBossLevel.State.WON,floor().state());
            assertEquals(1,CoopSession.floorItems(floor()).values().stream().filter(TengusMask.class::isInstance).count());
        }
    }
}
