package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImpVaultTest extends HeadlessDungeon {
    /** Staged city approach, followed by native vault entry. Neither seat keeps a fixture HP boost. */
    public static CoopSession openAtVaultEntrance(int seed) {
        CoopSession game = CavesInteriorTest.open(seed);
        try {
            entrance(game);
            for (int seat = 0; seat < 2; seat++) {
                Hero hero = game.hero(seat);
                hero.HTBoost = 0;
                hero.updateHT(false);
            }
            CoopFloorsTest.cross(game);
            return game;
        } catch (RuntimeException | Error failure) {
            game.close();
            throw failure;
        }
    }

    public static int entrance(CoopSession game) {
        int npc=CityEntranceTest.prepareBoundary(game,"imp");
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ACCEPT_IMP,npc,null));
        int gate=Dungeon.level.getTransition(LevelTransition.Type.BRANCH_EXIT).cell();
        MiningBranchTest.atBranchGate(game,gate);
        return gate;
    }

    public static void forScreen(CoopSession game,String destination) {
        if (destination.equals("imp-vault")) entrance(game);
        else stage(game,"hall21");
        CoopFloorsTest.cross(game);
    }

    public static int stage(CoopSession game, String boundary) {
        if (boundary.startsWith("hall")) {
            CityKingTest.prepareBoundary(game,"claim");
            int depth=Integer.parseInt(boundary.substring(4));
            while (Dungeon.depth+1<depth) SewerDepthTest.gate(game,true);
            int exit=Dungeon.level.exit();
            SewerSpecialActorTest.atGate(game,exit);
            return exit;
        }
        int gate=entrance(game);
        if (boundary.equals("full")) {
            for (int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                while (hero.belongings.backpack.items.size()<hero.belongings.backpack.capacity())
                    assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger().collect(hero.belongings.backpack));
            }
        }
        Dungeon.gold=137;
        Dungeon.energy=9;
        if (boundary.equals("enter")) return gate;
        CoopFloorsTest.cross(game);
        boolean reward=boundary.equals("pick") || boundary.equals("full");
        Hero chooser=game.hero(game.activeSeat());
        assertTrue(new DwarfToken().quantity(reward ? 6 : 1).collect(chooser.belongings.backpack));
        if (reward) assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation().quantity(3).collect(chooser.belongings.backpack));
        Dungeon.gold=71;
        Dungeon.energy=3;
        for (int seat=0;seat<2;seat++) game.observe(seat);
        return game.stairs.stair();
    }

    public static boolean published(String stage) {
        if (stage.startsWith("hall")) return Dungeon.branch==0 && Dungeon.depth==Integer.parseInt(stage.substring(4));
        if (stage.equals("enter")) return Dungeon.level instanceof VaultLevel && Dungeon.gold==0 && Dungeon.energy==0;
        if (!(Dungeon.branch==0 && Dungeon.depth==18 && Imp.Quest.isCompleted() && Dungeon.gold==137 && Dungeon.energy==9)) return false;
        if (stage.equals("pick") || stage.equals("full")) {
            int count=0;
            for (Item candidate:Dungeon.coop.hero(0).belongings) count+=rewardUnits(candidate);
            for (Item candidate:Dungeon.coop.hero(1).belongings) count+=rewardUnits(candidate);
            for (com.shatteredpixel.shatteredpixeldungeon.items.Heap heap:Dungeon.level.heaps.valueList())
                for (Item candidate:heap.items) count+=rewardUnits(candidate);
            count+=rewardUnits(Imp.Quest.reward);
            return count==1;
        }
        return true;
    }

    private static int rewardUnits(Item candidate) {
        if (!(candidate instanceof com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation) || Dungeon.coop.ownership.owner(candidate)==0) return 0;
        assertTrue(Dungeon.coop.ownership.identity(candidate)>0);
        return candidate.quantity();
    }

    public static String receiptState(CoopSession game) {
        String result=MiningBranchTest.signature(game)+"/"+Dungeon.gold+"/"+Dungeon.energy+"/"+identities(game)+"/"+Imp.Quest.isCompleted();
        int holder=game.stairs.crystalHolder();
        if (holder>=0) {
            Bundle stash=game.hero(holder).belongings.getItem(EscapeCrystal.class).storedItems;
            result+="/"+stash.getInt("gold")+"/"+stash.getInt("energy");
            for (int seat=0;seat<2;seat++) {
                Bundle table=stash.getBundle("ids"+seat);
                for (String path:new java.util.TreeSet<>(table.getKeys())) {
                    Bundle item=table.getBundle(path);
                    result+="/"+path+"/"+item.getLong("id")+"/"+item.getLong("owner")+"/"+item.getInt("quantity");
                }
            }
        }
        Item held=Imp.Quest.reward;
        if (held!=null) result+="/pending/"+game.ownership.identity(held)+"/"+game.ownership.owner(held);
        return result;
    }

    static Map<String,Long> identities(CoopSession g) {
        Map<String,Long> result=new TreeMap<>();
        for (int seat=0;seat<2;seat++) for(Map.Entry<String,Item> e:CoopVault.belongings(g.hero(seat),seat).entrySet())
            result.put(e.getKey(),g.ownership.identity(e.getValue()));
        return result;
    }

    @Test public void generatedTokenDoorAndMirrorUseOnlyTheActingSeatsItems() {
        try (CoopSession g=CavesInteriorTest.open(572)) {
            entrance(g);
            CoopFloorsTest.cross(g);
            com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor door=SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor.class);
            com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror mirror=SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror.class);
            int cell=door.pos,seat=g.activeSeat();
            CavesBossTest.beside(g,seat,cell);
            Hero acting=g.hero(seat),partner=g.hero(1-seat);
            assertTrue(new DwarfToken().quantity(10).collect(partner.belongings.backpack));
            assertFalse(CoopFloorsTest.step(g,CoopSession.Action.VAULT_INTERACT,cell,null));
            assertTrue(new DwarfToken().quantity(10).collect(acting.belongings.backpack));
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.VAULT_INTERACT,cell,null));
            assertNull(acting.belongings.getItem(DwarfToken.class));
            assertEquals(10,partner.belongings.getItem(DwarfToken.class).quantity());
            assertFalse(Dungeon.level.mobs.contains(door));
            seat=g.activeSeat();
            CavesBossTest.beside(g,seat,mirror.pos);
            Item offer=mirror.reward;
            long id=g.ownership.identity(offer);
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.VAULT_INTERACT,mirror.pos,null));
            assertNull(mirror.reward);
            assertTrue(Imp.Quest.mirrorUsed);
            assertEquals(id,g.ownership.identity(offer));
            assertEquals(g.player(seat),g.ownership.owner(offer));
            assertTrue(g.hero(seat).belongings.contains(offer));
        }
    }

    @Test public void rewardRequiresBothVotesAndConsumesOneEligibleStackUnit() {
        try (CoopSession g=CavesInteriorTest.open(572)) {
            entrance(g);
            CoopFloorsTest.cross(g);
            int seat=g.activeSeat();
            Hero owner=g.hero(seat);
            assertTrue(new DwarfToken().quantity(6).collect(owner.belongings.backpack));
            Item ration=new com.shatteredpixel.shatteredpixeldungeon.items.food.Food().quantity(3);
            assertTrue(ration.collect(owner.belongings.backpack));
            Item foreign=new com.shatteredpixel.shatteredpixeldungeon.items.food.Food().quantity(2);
            assertTrue(foreign.collect(g.hero(1-seat).belongings.backpack));
            int cell=g.stairs.stair();
            assertTrue(g.stairs.vaultScore()>=500 && g.stairs.vaultScore()<=1000);
            assertTrue(g.stairs.consent(1-seat,g.turn(),cell,false));
            assertFalse(CoopFloorsTest.step(g,CoopSession.Action.TRAVEL,cell,null));
            assertFalse(CoopFloorsTest.step(g,CoopSession.Action.VAULT_REWARD,cell,foreign));
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.VAULT_REWARD,cell,ration));
            assertEquals(0,Dungeon.branch);
            int count=0;
            for (Item i:owner.belongings) if (i.getClass()==ration.getClass()) {
                assertEquals(g.player(seat),g.ownership.owner(i));
                count+=i.quantity();
            }
            assertEquals(2,count); // original starting ration and the single chosen reward
            assertTrue(Imp.Quest.isCompleted());
        }
    }

    @Test public void escrowRejectsDuplicatePhysicalIdsAcrossSeats() {
        try (CoopSession g=CavesInteriorTest.open(572)) {
            entrance(g);
            CoopFloorsTest.cross(g);
            int holder=g.stairs.crystalHolder();
            EscapeCrystal crystal=g.hero(holder).belongings.getItem(EscapeCrystal.class);
            Bundle stash=crystal.storedItems;
            Bundle left=stash.getBundle("ids0").getBundle("s0_weapon");
            Bundle right=stash.getBundle("ids1").getBundle("s1_weapon");
            right.put("id",left.getLong("id"));
            assertThrows(IllegalArgumentException.class,()->CoopVault.validate(g,crystal,holder,g.ownership.sequence(),new java.util.HashSet<>()));
        }
    }

    @Test public void nativeVaultHoldsTwoPacksAndOnePurseAcrossRestart() {
        Bundle saved;
        Map<String,Long> originals;
        try (CoopSession g=CavesInteriorTest.open(572)) {
            entrance(g);
            Dungeon.gold=137;
            Dungeon.energy=9;
            originals=identities(g);
            CoopFloorsTest.cross(g);
            assertTrue(Dungeon.level instanceof VaultLevel);
            assertEquals(63,Dungeon.level.width());
            assertEquals(1,Dungeon.branch);
            assertEquals(0,Dungeon.gold);
            assertEquals(0,Dungeon.energy);
            for (int seat=0;seat<2;seat++) assertNull(g.hero(seat).belongings.weapon);
            saved=g.checkpoint();
            assertEquals(12,saved.getInt("schema"));
        }
        try (CoopSession g=CoopSession.restore(saved)) {
            assertEquals(18,Dungeon.depth);
            TenguArenaTest.waitOne(g);
            Hero active=g.hero(g.activeSeat());
            // Isolated score fixture. Token collection and both votes still run on the original level.
            assertTrue(new DwarfToken().quantity(1).collect(active.belongings.backpack));
            Dungeon.gold=73;
            Dungeon.energy=8;
            for (int seat=0;seat<2;seat++) g.observe(seat);
            int holder=g.stairs.crystalHolder();
            assertTrue(holder>=0);
            int cell=g.stairs.stair();
            assertTrue("native score="+g.stairs.vaultScore(),cell>=0);
            assertTrue(g.stairs.consent(1-g.activeSeat(),g.turn(),cell,false));
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.TRAVEL,cell,null));
            assertEquals(0,Dungeon.branch);
            assertEquals(137,Dungeon.gold);
            assertEquals(9,Dungeon.energy);
            assertEquals(originals,identities(g));
            assertTrue(Imp.Quest.isCompleted());
            for(int seat=0;seat<2;seat++) assertNull(g.hero(seat).belongings.getItem(EscapeCrystal.class));
            assertFalse(CoopFloorsTest.step(g,CoopSession.Action.TRAVEL,cell,null));
            saved=g.checkpoint();
        }
        try (CoopSession g=CoopSession.restore(saved)) {
            assertEquals(originals,identities(g));
            TenguArenaTest.waitOne(g);
        }
    }
}
