package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class CityEntranceTest extends HeadlessDungeon {
    public static void win(CoopSession game) {
        CavesBossTest.arrive(game);
        CavesBossTest.start(game);
        CavesBossTest.fight(game);
        stageAtExit(game);
    }
    static void stageAtExit(CoopSession game) {
        int exit=Dungeon.level.exit();
        assertFalse(Dungeon.level.locked);
        for (int waits=0;;waits++) {
            SewerSpecialActorTest.atGate(game,exit);
            if (game.stairs.stair()==exit) return;
            boolean immobilized=game.hero(0).rooted || game.hero(1).rooted
                    || game.hero(0).paralysed>0 || game.hero(1).paralysed>0;
            assertTrue("Unexpected unavailable exit on floor "+Dungeon.depth,immobilized && waits<64);
            TenguArenaTest.waitOne(game);
        }
    }
    public static int prepareBoundary(CoopSession game,String name) {
        win(game);
        if (name.equals("16")) return game.stairs.stair();
        CoopFloorsTest.cross(game);
        int dest=name.equals("imp")?19:Integer.parseInt(name);
        while(Dungeon.depth<dest-(name.equals("imp")?0:1)) {
            if(name.equals("imp") && Dungeon.level.mobs.stream().anyMatch(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.class::isInstance)) break;
            SewerDepthTest.gate(game,true);
        }
        if(name.equals("imp")) {
            Mob imp=SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.class);
            MiningBranchTest.standBeside(game,imp.pos);
            return imp.pos;
        }
        SewerSpecialActorTest.atGate(game,Dungeon.level.exit());
        return game.stairs.stair();
    }
    public static boolean published(String name) {
        return name.equals("imp") ? com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.given()
                : Dungeon.branch==0 && Dungeon.depth==Integer.parseInt(name);
    }
    @Test public void consecutiveCityStairsAndOriginalImp() {
        Bundle state;
        try(CoopSession game=CavesInteriorTest.open(572)) {
            int npc=prepareBoundary(game,"imp");
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ACCEPT_IMP,npc,null));
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ACCEPT_IMP,npc,null));
            while(Dungeon.depth<19) SewerDepthTest.gate(game,true);
            assertNotNull(SewerSpecialActorTest.actor(Golem.class));
            assertNotNull(SewerSpecialActorTest.actor(Monk.class));
            assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.given());
            state=game.checkpoint();
        }
        try(CoopSession game=CoopSession.restore(state)) {
            assertEquals(19,Dungeon.depth);
            for(int depth=18;depth>=15;depth--) { SewerDepthTest.gate(game,false);assertEquals(depth,Dungeon.depth); }
            for(int depth=16;depth<=19;depth++) { SewerDepthTest.gate(game,true);assertEquals(depth,Dungeon.depth); }
            assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.given());
            TenguArenaTest.waitOne(game);
        }
    }
    @Test public void monkParryCooldownSurvivesAFullRoomRestore() {
        Bundle checkpoint;
        int monkId;
        float cooldown;
        try (CoopSession game=CavesInteriorTest.open(572)) {
            CityKingTest.reach(game,19);
            Monk monk=SewerSpecialActorTest.actor(Monk.class);
            monkId=monk.id();
            // A native block consumes Focus and draws a fractional 6-to-7 turn delay.
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(monk,Monk.Focus.class);
            monk.defenseVerb();
            Bundle before=new Bundle();
            monk.storeInBundle(before);
            cooldown=before.getFloat("focus_cooldown");
            assertTrue(cooldown>=6 && cooldown<=7);
            checkpoint=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(checkpoint)) {
            Monk monk=(Monk)Actor.findById(monkId);
            Bundle after=new Bundle();
            monk.storeInBundle(after);
            assertEquals(cooldown,after.getFloat("focus_cooldown"),0f);
            TenguArenaTest.waitOne(game);
        }
    }

    @Test public void originalCityTradePricesAndWarlockAtBothSeats() {
        Bundle saved;
        try (CoopSession game=CavesInteriorTest.open(572)) {
            win(game);
            CoopFloorsTest.cross(game);
            saved=game.checkpoint();
        }
        PrisonShopChecks.copiesOf(saved);
        for (int seat=0;seat<2;seat++) try (CoopSession game=CoopSession.restore(saved)) {
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero=game.hero(seat);
            com.shatteredpixel.shatteredpixeldungeon.items.Heap stock=Dungeon.level.heaps.valueList().stream()
                    .filter(h->h.type==com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.FOR_SALE
                            && h.peek() instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)
                    .findFirst().get();
            com.shatteredpixel.shatteredpixeldungeon.items.Item armor=stock.peek();
            SewerBeeTest.offer(game,seat);
            hero.pos=stock.pos;
            game.observe(seat);
            Dungeon.gold=Shopkeeper.sellPrice(armor);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.BUY,stock.pos,armor));
            assertEquals(0,Dungeon.gold);
            assertEquals(game.player(seat),game.ownership.owner(armor));
            assertTrue(hero.belongings.contains(armor));
            Warlock caster=SewerSpecialActorTest.actor(Warlock.class);
            caster.aggro(hero);
            hero.HT=hero.HP=1000;
            int otherHp=game.hero(1-seat).HP;
            for (int i=0;i<24 && hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade.class)==null;i++)
                caster.onZapComplete();
            assertNotNull(hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade.class));
            assertTrue(hero.HP<1000);
            assertEquals(otherHp,game.hero(1-seat).HP);
            assertNull(game.hero(1-seat).buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade.class));
            game.checkpoint();
        }
    }
    @Test public void originalMarketAndGhoulResurrectionAfterResume() {
        Bundle state;
        int downed=0,hostId=0;
        try(CoopSession game=CavesInteriorTest.open(571)) {
            win(game);
            CoopFloorsTest.cross(game);
            assertTrue(Dungeon.level instanceof CityLevel);
            assertEquals(16,Dungeon.depth);
            assertNotNull(SewerSpecialActorTest.actor(Shopkeeper.class));
            assertNotNull(SewerSpecialActorTest.actor(Warlock.class));
            Ghoul ghoul=SewerSpecialActorTest.actor(Ghoul.class);
            for(int i=0;i<16 && Dungeon.level.mobs.stream().filter(Ghoul.class::isInstance).count()<2;i++) TenguArenaTest.waitOne(game);
            // Place the pair in reach before dealing a lethal hit in this isolated native life-link test.
            for(Mob m:Dungeon.level.mobs) if(m instanceof Ghoul && m!=ghoul) {
                for(int d:com.watabou.utils.PathFinder.NEIGHBOURS8) if(Dungeon.level.passable[ghoul.pos+d] && Actor.findChar(ghoul.pos+d)==null) {
                    m.pos=ghoul.pos+d;break;
                }
                break;
            }
            ghoul.damage(ghoul.HP+200,game.hero(0));downed=ghoul.id();
            assertFalse(Dungeon.level.mobs.contains(ghoul));
            for(Mob m:Dungeon.level.mobs) if(m.buff(Ghoul.GhoulLifeLink.class)!=null) hostId=m.id();
            assertTrue(hostId>0);
            state=game.checkpoint();
            assertEquals(10,state.getInt("schema"));
        }
        try(CoopSession game=CoopSession.restore(state)) {
            assertNull(Actor.findById(downed));
            Ghoul host=(Ghoul)Actor.findById(hostId);
            assertEquals(downed,host.buff(Ghoul.GhoulLifeLink.class).downedGhoul().id());
            for(int i=0;i<40 && Actor.findById(downed)==null;i++) TenguArenaTest.waitOne(game);
            assertTrue(Actor.findById(downed) instanceof Ghoul);
            SewerDepthTest.gate(game,false);
            assertEquals(15,Dungeon.depth);
            assertTrue(((CavesBossLevel)Dungeon.level).exitOpen());
            SewerDepthTest.gate(game,true);
            assertEquals(16,Dungeon.depth);
            assertNotNull(Actor.findById(downed));
            TenguArenaTest.waitOne(game);
        }
    }
}
