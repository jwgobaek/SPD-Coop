package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;

public class MiningBranchTest extends HeadlessDungeon {
    public static void forScreen(CoopSession game,String shot) {
        if (shot.equals("smith-services")) {
            prepareBoundary(game,"gnoll-order");
            SewerBeeTest.offer(game,0);
            standBeside(game,SewerSpecialActorTest.actor(Blacksmith.class).pos);
            return;
        }
        if (shot.equals("city-market")) {
            CityEntranceTest.win(game);
            CoopFloorsTest.cross(game);
            int cell=SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.class).pos;
            atBranchGate(game,cell);
        } else {
            enter(game,shot.equals("mine-crystal")?570:571);
            int at=shot.equals("mine-crystal")?SewerSpecialActorTest.actor(CrystalGuardian.class).pos
                    :SewerSpecialActorTest.actor(GnollSapper.class).pos;
            try { atBranchGate(game,at); } catch(AssertionError narrow) {
                atBranchGate(game,Dungeon.level.getTransition(LevelTransition.Type.BRANCH_ENTRANCE).cell());
            }
        }
    }
    static void enter(CoopSession game, int seed) {
        prepareEntrance(game,seed);
        CoopFloorsTest.cross(game);
        assertEquals(1, Dungeon.branch);
        assertTrue(Dungeon.level instanceof MiningLevel);
        assertTrue(Blacksmith.Quest.started());
        assertEquals(seed == 571 ? Blacksmith.Quest.GNOLL : Blacksmith.Quest.CRYSTAL, Blacksmith.Quest.Type());
    }
    private static void prepareEntrance(CoopSession game, int seed) {
        CavesInteriorTest.arrive(game, seed == 571 ? 12 : 13);
        Blacksmith npc = SewerSpecialActorTest.actor(Blacksmith.class);
        LevelTransition gate = Dungeon.level.getTransition(LevelTransition.Type.BRANCH_EXIT);
        atBranchGate(game, gate.cell());
        assertNotEquals(gate.cell(), game.stairs.stair());
        standBeside(game, npc.pos);
        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ACCEPT_BLACKSMITH, npc.pos, null));
        atBranchGate(game, gate.cell());
    }
    static void atBranchGate(CoopSession game, int cell) {
        int count=0;
        int[] places=new int[2];
        for (int off : PathFinder.NEIGHBOURS9) {
            int p=cell+off;
            if (!Dungeon.level.passable[p] || Dungeon.level.pit[p] || Dungeon.level.traps.get(p)!=null
                    || (Actor.findChar(p)!=null && !(Actor.findChar(p) instanceof Hero))) continue;
            places[count++]=p;
            if (count==2) break;
        }
        assertEquals(2,count);
        for (int seat=0;seat<2;seat++) game.hero(seat).pos=places[seat];
        for (int seat=0;seat<2;seat++) game.observe(seat);
    }
    static void standBeside(CoopSession game, int cell) {
        Hero h = game.hero(game.activeSeat());
        for (int off : PathFinder.NEIGHBOURS8) {
            int p = cell + off;
            if (Dungeon.level.insideMap(p) && Dungeon.level.passable[p] && (Actor.findChar(p) == null || Actor.findChar(p)==h)) {
                h.pos = p;
                Dungeon.level.updateFieldOfView(h,h.fieldOfView);
                game.observe(game.activeSeat());
                return;
            }
        }
        fail("No standing cell");
    }
    private static int prepareVein(CoopSession game) {
        int vein=-1;
            for (int i = 0; i < Dungeon.level.length(); i++) {
                if (Dungeon.level.map[i] != Terrain.WALL_DECO) continue;
                for (int offset : PathFinder.NEIGHBOURS8) if (Dungeon.level.passable[i+offset] && Actor.findChar(i+offset)==null) vein=i;
                if (vein >= 0) break;
            }
            assertTrue(vein>=0);
            standBeside(game, vein);
        return vein;
    }
    public static int prepareBoundary(CoopSession game,String name) {
        int seed=name.startsWith("gnoll")?571:570;
        if (name.endsWith("enter")) {
            prepareEntrance(game,seed);
            return game.stairs.stair();
        }
        enter(game,seed);
        int owner=game.hero(0).belongings.getItem(Pickaxe.class)!=null?0:1;
        while(game.activeSeat()!=owner) TenguArenaTest.waitOne(game);
        int cell=prepareVein(game);
        if (name.endsWith("ore")) return cell;
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MINE,cell,null));
        atBranchGate(game,Dungeon.level.getTransition(LevelTransition.Type.BRANCH_ENTRANCE).cell());
        if (name.endsWith("return")) return game.stairs.stair();
        CoopFloorsTest.cross(game);
        // Isolate service prices, independent of the number of ore veins mined on the route.
        Blacksmith.Quest.favor=3000;
        standBeside(game,SewerSpecialActorTest.actor(Blacksmith.class).pos);
        if (name.endsWith("race")) atBranchGate(game,SewerSpecialActorTest.actor(Blacksmith.class).pos);
        if (name.endsWith("claim")) {
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SMITH_ORDER,0,null));
            standBeside(game,SewerSpecialActorTest.actor(Blacksmith.class).pos);
        }
        return 0;
    }
    public static boolean published(String name) {
        if(name.endsWith("boss")) return Dungeon.branch==1 && Blacksmith.Quest.bossBeaten();
        if(name.endsWith("upgrade")) return Dungeon.branch==0 && Blacksmith.Quest.upgrades==1 && Blacksmith.Quest.favor==2000;
        if(name.endsWith("reforge")) return Dungeon.branch==0 && Blacksmith.Quest.reforges==1 && Blacksmith.Quest.favor==2500;
        if(name.endsWith("order")) return CoopSmith.pendingChoice() && Blacksmith.Quest.favor==1000;
        if(name.endsWith("claim")) return Blacksmith.Quest.smiths==1 && Blacksmith.Quest.smithRewards==null && Blacksmith.Quest.favor==1000;
        if(name.endsWith("return")) return Dungeon.branch==0 && Blacksmith.Quest.completed() && Blacksmith.Quest.favor==50;
        if(Dungeon.branch!=1 || !Blacksmith.Quest.started()) return false;
        for(int seat=0;seat<2;seat++) if(Dungeon.coop.hero(seat).belongings.getItem(DarkGold.class)!=null) return name.endsWith("ore");
        return name.endsWith("enter");
    }
    public static String signature(CoopSession game) {
        String value=CavesInteriorTest.signature(game)+"/"+Dungeon.branch+"/"+Blacksmith.Quest.completed()+"/"+Blacksmith.Quest.favor+"/"+java.util.Arrays.hashCode(Dungeon.level.map);
        for (int seat=0;seat<2;seat++) {
            DarkGold gold=game.hero(seat).belongings.getItem(DarkGold.class);
            if (gold!=null) value+="/ore/"+gold.quantity()+"/"+game.ownership.identity(gold)+"/"+game.ownership.owner(gold);
        }
        if(Blacksmith.Quest.pickaxe!=null) value+="/escrow/"+game.ownership.identity(Blacksmith.Quest.pickaxe);
        if(CoopSmith.pendingChoice()) for (com.shatteredpixel.shatteredpixeldungeon.items.Item item:Blacksmith.Quest.smithRewards)
            value+="/offer/"+item.getClass().getSimpleName()+"/"+game.ownership.identity(item);
        for(int seat=0;seat<2;seat++) for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:game.hero(seat).belongings)
            value+="/bag/"+seat+"/"+game.ownership.identity(item)+"/"+game.ownership.owner(item)+"/"+item.trueLevel()+"/"+item.quantity();
        return value;
    }
    @Test public void originalBlacksmithServicesChargeOnlyEligibleOwners() {
        try (CoopSession game=CavesInteriorTest.open(571)) {
            prepareBoundary(game,"gnoll-order");
            int seat=game.activeSeat();
            Hero hero=game.hero(seat);
            com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger dagger=
                    new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger();
            com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger spare=
                    new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger();
            dagger.identify(false);spare.identify(false);
            assertTrue(dagger.collect(hero.belongings.backpack));assertTrue(spare.collect(hero.belongings.backpack));
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.SMITH_UPGRADE,0,game.hero(1-seat).belongings.weapon));
            long id=game.ownership.identity(dagger);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SMITH_UPGRADE,0,dagger));
            assertEquals(1,dagger.level());assertEquals(2000,Blacksmith.Quest.favor);
            while(game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SMITH_HARDEN,0,dagger));
            assertTrue(dagger.enchantHardened);assertEquals(1500,Blacksmith.Quest.favor);
            while(game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
            int order=0;for(com.shatteredpixel.shatteredpixeldungeon.items.Item i:hero.belongings) {order++;if(i==spare) break;}
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SMITH_REFORGE,order,dagger));
            assertFalse(hero.belongings.contains(spare));assertEquals(id,game.ownership.identity(dagger));assertEquals(2,dagger.level());
            assertEquals(1000,Blacksmith.Quest.favor);
            while(game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
            int gold=Dungeon.gold;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SMITH_CASH,0,null));
            assertEquals(gold+1000,Dungeon.gold);assertEquals(0,Blacksmith.Quest.favor);
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.SMITH_CASH,0,null));
        }
    }
    @Test public void originalCrystalBossDropsAndFavor() { fight(570); }
    @Test public void originalGnollBossAndSappers() { fight(571); }
    private static int blockedApproach(CoopSession game,int target) {
        java.util.ArrayDeque<Integer> search=new java.util.ArrayDeque<>();
        boolean[] seen=new boolean[Dungeon.level.length()];
        search.add(target);seen[target]=true;
        while(!search.isEmpty()) {
            int at=search.remove();
            for(int off:PathFinder.NEIGHBOURS8) {
                int p=at+off;
                if(!Dungeon.level.insideMap(p)||seen[p]) continue;
                seen[p]=true;
                if(Dungeon.level.passable[p] && (Actor.findChar(p)==null || Actor.findChar(p)==game.hero(game.activeSeat()))) return at==target?-1:at;
                if(Hero.mineableTerrain(Dungeon.level.map[p]) && Actor.findChar(p)==null) search.add(p);
            }
        }
        throw new AssertionError("No native route to "+target);
    }
    private static StagedInput finishBoss(CoopSession game,int seed) {
            StagedInput input=null;
            for (int seat=0;seat<2;seat++) {
                // Durable isolated combat fixture. The game creates all quest residents and terrain.
                Hero fighter=game.hero(seat);
                fighter.HT=fighter.HP=1500;
                fighter.belongings.weapon.upgrade(6);
            }
            Mob boss=seed==570?SewerSpecialActorTest.actor(CrystalSpire.class):SewerSpecialActorTest.actor(GnollGeomancer.class);
            for(int n=0;n<500 && !Blacksmith.Quest.bossBeaten();n++) {
                assertFalse("engine: "+game.stopReason(),game.stopped());
                assertFalse("party defeated",game.defeated());
                Mob target=boss;
                if(boss instanceof GnollGeomancer && ((GnollGeomancer)boss).hasSapper())
                    for(Mob m:Dungeon.level.mobs) if(m instanceof GnollSapper && m.isAlive()){ target=m;break; }
                boolean pick=target instanceof CrystalSpire || target.buff(GnollGeomancer.RockArmor.class)!=null;
                if(pick && game.hero(game.activeSeat()).belongings.getItem(Pickaxe.class)==null) {
                    TenguArenaTest.waitOne(game);continue;
                }
                // The native boss can be encased in rock. Excavate a route with real mine actions.
                int wall=blockedApproach(game,target.pos);
                if(wall>=0) {
                    if(game.hero(game.activeSeat()).belongings.getItem(Pickaxe.class)==null) TenguArenaTest.waitOne(game);
                    else { standBeside(game,wall); assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MINE,wall,null)); }
                    continue;
                }
                standBeside(game,target.pos);
                input=new StagedInput(game,pick?"MINE":"ATTACK",target.pos);
                assertTrue("native strike "+n+" "+target+" "+game.stopReason()+" live="+target.isAlive()+" actual="+Actor.findChar(target.pos)+" fov="+game.hero(game.activeSeat()).fieldOfView[target.pos]+" alignment="+target.alignment,CoopFloorsTest.step(game,pick?CoopSession.Action.MINE:CoopSession.Action.ATTACK,target.pos,null));
                game.checkpoint();
            }
            assertTrue(Blacksmith.Quest.bossBeaten());
            assertFalse(Dungeon.level.mobs.contains(boss));
            return input;
    }

    public static StagedInput equipmentOrBoss(CoopSession game,String stage) {
        int seed=stage.startsWith("gnoll")?571:570;
        if (stage.endsWith("boss")) {
            enter(game,seed);
            return finishBoss(game,seed);
        }
        prepareBoundary(game,stage);
        Hero hero=game.hero(game.activeSeat());
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger first=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger();
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger second=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger();
        first.identify();second.identify();
        assertTrue(first.collect(hero.belongings.backpack));
        assertTrue(second.collect(hero.belongings.backpack));
        int order=0,firstOrder=0,secondOrder=0;
        for (com.shatteredpixel.shatteredpixeldungeon.items.Item item:hero.belongings) {
            order++;
            if (item==first) firstOrder=order;
            if (item==second) secondOrder=order;
        }
        assertTrue(firstOrder>0 && secondOrder>0);
        return new StagedInput(game,stage.endsWith("upgrade")?"SMITH_UPGRADE":"SMITH_REFORGE",stage.endsWith("upgrade")?0:secondOrder,firstOrder);
    }

    private void fight(int seed) {
        try(CoopSession game=CavesInteriorTest.open(seed)) {
            enter(game,seed);
            finishBoss(game,seed);
            int gate=Dungeon.level.getTransition(LevelTransition.Type.BRANCH_ENTRANCE).cell();
            atBranchGate(game,gate);
            for (int n=0;n<64 && game.stairs.stair()!=gate;n++) {
                TenguArenaTest.waitOne(game);
                atBranchGate(game,gate);
            }
            CoopFloorsTest.cross(game);
            assertEquals(1000,Blacksmith.Quest.favor);
        }
    }
    @Test public void crystalMineRoundTrip() { roundTrip(570); }
    @Test public void gnollMineRoundTrip() { roundTrip(571); }
    private void roundTrip(int seed) {
        Bundle saved;
        int vein = -1, miner, heroId;
        long pickId, goldId;
        try (CoopSession game = CavesInteriorTest.open(seed)) {
            enter(game, seed);
            miner = game.hero(0).belongings.getItem(Pickaxe.class) == null ? 1 : 0;
            while (game.activeSeat()!=miner) TenguArenaTest.waitOne(game);
            vein=prepareVein(game);
            Hero hero=game.hero(miner);
            heroId=hero.id();
            pickId=game.ownership.identity(hero.belongings.getItem(Pickaxe.class));
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.MINE, vein, null));
            assertEquals(Terrain.EMPTY_DECO, Dungeon.level.map[vein]);
            DarkGold gold=hero.belongings.getItem(DarkGold.class);
            assertEquals(1,gold.quantity());
            goldId=game.ownership.identity(gold);
            assertEquals(game.player(miner),game.ownership.owner(gold));
            saved=game.checkpoint();
            assertEquals(10,saved.getInt("schema"));
            assertTrue(saved.getBundle("dormant").contains(Integer.toString(Dungeon.depth)));
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            assertEquals(1,Dungeon.branch);
            assertEquals(Terrain.EMPTY_DECO,Dungeon.level.map[vein]);
            Hero hero=game.hero(miner);
            assertEquals(heroId,hero.id());
            assertEquals(goldId,game.ownership.identity(hero.belongings.getItem(DarkGold.class)));
            TenguArenaTest.waitOne(game);
            atBranchGate(game,Dungeon.level.getTransition(LevelTransition.Type.BRANCH_ENTRANCE).cell());
            CoopFloorsTest.cross(game);
            assertEquals(0,Dungeon.branch);
            assertTrue(Blacksmith.Quest.completed());
            assertEquals(50,Blacksmith.Quest.favor);
            assertEquals(pickId,game.ownership.identity(Blacksmith.Quest.pickaxe));
            assertNull(hero.belongings.getItem(DarkGold.class));
            assertEquals(-1,game.stairs.stair());
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            assertEquals(pickId,game.ownership.identity(Blacksmith.Quest.pickaxe));
            assertEquals(50,Blacksmith.Quest.favor);
            TenguArenaTest.waitOne(game);
        }
    }
}
