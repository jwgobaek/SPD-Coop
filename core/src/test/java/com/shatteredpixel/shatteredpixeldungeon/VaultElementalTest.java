package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ImpStatue;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.TreeSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class VaultElementalTest extends HeadlessDungeon {
    static VaultBossElemental boss() {
        for (Mob m : Dungeon.level.mobs) if (m instanceof VaultBossElemental) return (VaultBossElemental)m;
        return null;
    }

    static int enter(CoopSession game) {
        ImpVaultTest.entrance(game);
        Dungeon.gold=137;
        Dungeon.energy=9;
        CoopFloorsTest.cross(game);
        VaultLevel level = (VaultLevel)Dungeon.level;
        VaultFinalRoom room = (VaultFinalRoom)level.room(VaultFinalRoom.class);
        int center = level.pointToCell(room.center());
        int door = level.pointToCell(room.lockedDoor);
        int approach = (door-center)/6;
        assertEquals(6, level.distance(door, center));
        int seat = game.activeSeat();
        for (int i=0; i<2; i++) {
            Hero hero=game.hero(i);
            hero.HTBoost+=6000;
            hero.updateHT(false);
            hero.HP=hero.HT;
            hero.STR=18;
            hero.belongings.weapon=new Longsword();
            hero.belongings.weapon.upgrade(3).identify();
            game.ownership.claim(hero.belongings.weapon,game.player(i));
        }
        game.hero(seat).pos=center+2*approach;
        game.hero(1-seat).pos=center-approach;
        game.observe(0);
        game.observe(1);
        return center+3*approach;
    }

    static int treasure() {
        for (Heap heap:Dungeon.level.heaps.valueList()) if (heap.peek() instanceof ImpStatue) return heap.pos;
        return -1;
    }

    static void move(CoopSession game,int target) {
        assertTrue("MOVE "+target+" "+game.stopReason(),CoopFloorsTest.step(game,CoopSession.Action.MOVE,target,null));
    }

    static void strike(CoopSession game) {
        VaultBossElemental target=boss();
        MiningBranchTest.standBeside(game,target.pos);
        assertTrue("ATTACK "+target.HP+" "+target.curForm()+" "+game.stopReason(),
                CoopFloorsTest.step(game,CoopSession.Action.ATTACK,target.pos,null));
    }

    public static String signature(CoopSession game) {
        StringBuilder result=new StringBuilder();
        result.append(game.turn()).append('/').append(game.activeSeat()).append('/').append(Dungeon.level.locked);
        VaultBossElemental target=boss();
        if(target!=null) {
            Bundle b=new Bundle();
            target.storeInBundle(b);
            result.append('/').append(target.id()).append('/').append(target.pos).append('/').append(target.HP)
                .append('/').append(target.curForm()).append('/').append(Arrays.toString(VaultBossElemental.savedFormWeights(b)));
            for(String key:new String[]{"env_atk_cooldown","sp_atk_cooldown","sp_target_cell","last_enemy_pos","lightning_ofs"})
                result.append('/').append(b.getInt(key));
        }
        TreeSet<String> effects=new TreeSet<>();
        for(Actor a:Actor.all()) if(a instanceof Buff) {
            Buff buff=(Buff)a;
            effects.add(buff.getClass().getName()+"/"+buff.id()+"/"+buff.target.id());
        }
        for(int i=0;i<2;i++) result.append('/').append(game.hero(i).HP).append('/').append(game.hero(i).pos);
        result.append("/").append(Dungeon.gold).append("/").append(Dungeon.energy).append("/").append(Imp.Quest.isCompleted()).append("/").append(ImpVaultTest.identities(game));
        return result+"/"+effects+"/"+Arrays.toString(Random.worldState());
    }

    static void waitTurn(CoopSession game) {
        assertTrue(game.stopReason(),CoopFloorsTest.step(game,CoopSession.Action.WAIT,game.hero(game.activeSeat()).pos,null));
    }

    // Run with "write" in the uninterrupted JVM, then each form in a fresh JVM.
    public static void main(String[] args) throws Exception {
        VaultElementalTest probe=new VaultElementalTest();
        probe.temporary.create();
        probe.startBackend();
        Path directory=Paths.get(args[1]);
        try {
            if (args[0].equals("write")) {
                try (CoopSession game=CavesInteriorTest.open(572)) {
                    move(game,enter(game));
                    EnumSet<VaultBossElemental.ElementalForm> seen=EnumSet.noneOf(VaultBossElemental.ElementalForm.class);
                    for (int n=0;boss()!=null && n<400 && seen.size()<3;n++) {
                        VaultBossElemental.ElementalForm form=boss().curForm();
                        if (!seen.contains(form) && boss().HP>120) {
                            String key=form.name();
                            Bundle saved=game.checkpoint();
                            try (OutputStream out=Files.newOutputStream(directory.resolve(key+".dat"))) {
                                assertTrue(Bundle.write(saved,out));
                            }
                            Files.write(directory.resolve(key+".txt"),trace(game,form));
                            seen.add(form);
                        } else strike(game);
                    }
                    assertEquals(3,seen.size());
                }
            } else {
                String key=args[0];
                try (InputStream in=Files.newInputStream(directory.resolve(key+".dat"));
                     CoopSession game=CoopSession.restore(Bundle.read(in))) {
                    java.util.List<String> expected=Files.readAllLines(directory.resolve(key+".txt"));
                    java.util.List<String> actual=trace(game,VaultBossElemental.ElementalForm.valueOf(key));
                    for (int i=0;i<Math.min(expected.size(),actual.size());i++) assertEquals("native action "+i,expected.get(i),actual.get(i));
                    assertEquals(expected.size(),actual.size());
                    System.out.println("PASS "+key+" next draw, RNG, targets, damage and effects across "+actual.size()+" states");
                }
            }
        } finally { probe.stopBackend(); probe.temporary.delete(); }
    }

    private static java.util.List<String> trace(CoopSession game,VaultBossElemental.ElementalForm form) {
        java.util.List<String> states=new java.util.ArrayList<>();
        states.add(signature(game));
        for(int i=0;i<6;i++) { waitTurn(game); states.add(signature(game)); }
        int budget=200;
        while (boss()!=null && boss().curForm()==form && budget-->0) {
            strike(game);
            states.add(signature(game));
        }
        assertNotNull(boss());
        assertNotEquals(form,boss().curForm());
        for(int i=0;i<5;i++) { waitTurn(game); states.add(signature(game)); }
        return states;
    }

    public static boolean published(String stage) {
        if (stage.equals("escape")) return Dungeon.branch==0 && Dungeon.depth==18 && Imp.Quest.isCompleted()
                && Imp.Quest.earnedShop() && Dungeon.gold==137 && Dungeon.energy==9;
        if (Dungeon.branch!=1 || !(Dungeon.level instanceof VaultLevel)) return false;
        VaultBossElemental elemental=boss();
        if (stage.equals("kill")) return elemental==null && ((VaultLevel)Dungeon.level).finalTreasureOpen() && treasure()==696;
        if (stage.equals("statue")) return treasure()<0 && Dungeon.coop.stairs.vaultScore()==4000;
        if (elemental==null || !Dungeon.level.locked) return false;
        if (stage.equals("spawn")) return elemental.HP==600;
        if (stage.equals("warning")) return !elemental.warnedCells().isEmpty();
        if (stage.equals("environment")) return elemental.buff(VaultBossElemental.FireWall.class)!=null ||
                elemental.buff(VaultBossElemental.FrostVortex.class)!=null || elemental.buff(VaultBossElemental.LightningChase.class)!=null;
        return elemental.HP<600 && elemental.curForm().name().equals(stage);
    }

    public static StagedInput prepareBoundary(CoopSession game,String stage) {
        int entry=enter(game);
        if (stage.equals("spawn")) return new StagedInput(game,"MOVE",entry);
        move(game,entry);
        for(int i=0;i<400 && boss()!=null;i++) {
            boolean wait=stage.equals("warning") || stage.equals("environment");
            boolean previous=published(stage);
            VaultBossElemental.ElementalForm previousForm=boss().curForm();
            int cell=boss().pos;
            if(!wait) MiningBranchTest.standBeside(game,cell);
            else cell=game.hero(game.activeSeat()).pos;
            StagedInput input=new StagedInput(game,wait?"WAIT":"ATTACK",cell);
            if(wait) waitTurn(game);
            else assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,cell,null));
            if(published(stage) && (!previous || (boss()!=null && previousForm!=boss().curForm()))) {
                if (stage.equals(previousForm.name())) continue; // require an actual transition into that form
                return input;
            }
        }
        if (stage.equals("escape")) {
            claimAt(game,treasure());
            assertEquals(4000,game.stairs.vaultScore());
            int cell=ringCell(game);
            claimAt(game,cell);
            MiningBranchTest.standBeside(game,game.hero(1-game.activeSeat()).pos);
            for (int i=0;i<80;i++) {
                game.observe(0);
                game.observe(1);
                cell=game.stairs.stair();
                if (cell>=0 && game.hero(game.activeSeat()).belongings.getItem(Ring.class)!=null)
                    return new StagedInput(game,"VAULT_REWARD",cell);
                waitTurn(game);
            }
            throw new AssertionError("No legal crystal offer after waiting for native paralysis");
        }
        if(stage.equals("statue")) {
            assertEquals(696,treasure());
            game.hero(game.activeSeat()).pos=696;
            game.observe(game.activeSeat());
            return new StagedInput(game,"PICK_UP",696);
        }
        throw new AssertionError("No native boundary for "+stage);
    }

    static int ringCell(CoopSession game) {
        for (Heap heap:Dungeon.level.heaps.valueList())
            if (heap.peek() instanceof Ring && CoopVault.questPickup(heap.peek(),heap.pos)) return heap.pos;
        throw new AssertionError("Missing original final-room ring");
    }

    static void claimAt(CoopSession game,int cell) {
        Hero hero=game.hero(game.activeSeat());
        hero.pos=cell;
        game.observe(game.activeSeat());
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,Dungeon.level.heaps.get(cell).peek()));
    }

    static void earnVaultTokenAndEscape(CoopSession game) {
        com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultShaman shaman=
                SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultShaman.class);
        int id=shaman.id();
        for (int i=0;i<200 && shaman.isAlive();i++) CityKingTest.strike(game,shaman);
        assertFalse(shaman.isAlive());
        int drop=-1;
        for (Heap heap:Dungeon.level.heaps.valueList())
            if (heap.pos==shaman.pos && heap.peek() instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken) drop=heap.pos;
        assertTrue("native shaman token at "+shaman.pos,drop>=0);
        claimAt(game,drop);
        MiningBranchTest.standBeside(game,game.hero(1-game.activeSeat()).pos);
        game.observe(0); game.observe(1);
        assertTrue(game.stairs.vaultScore()>=50 && game.stairs.vaultScore()<500);
        CoopFloorsTest.cross(game);
        assertNull(Actor.findById(id));
    }

    @Test public void seed578NativeVaultVariantsKeepTargetsSpellsAndTokenLoot() {
        for(int seat=0;seat<2;seat++) {
            try(CoopSession game=CavesInteriorTest.open(578)) {
                ImpVaultTest.entrance(game);
                CoopFloorsTest.cross(game);
                assertTrue(Dungeon.level instanceof VaultLevel);
                int[] types=new int[3];
                for(Mob mob:Dungeon.level.mobs) {
                    if(mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultElemental.Fire) types[0]++;
                    if(mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultElemental.Frost) types[1]++;
                    if(mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultShaman) types[2]++;
                }
                for(int n:types) assertTrue(n>0);
                Mob shaman=CavesInteriorTest.stage(game,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultShaman.class,seat);
                int hp=game.hero(seat).HP, other=game.hero(1-seat).HP;
                for(int i=0;i<18 && game.hero(seat).HP==hp;i++) waitTurn(game);
                assertTrue(game.hero(seat).HP<hp);
                assertEquals(other,game.hero(1-seat).HP);
                assertTrue(shaman.createLoot() instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken);
            }
        }
    }

    @Test public void seed578FireAndFrostUseNativeMagicForEitherSeat() {
        for (int seat=0; seat<2; seat++) for (boolean fire : new boolean[]{true,false}) {
            try (CoopSession game=CavesInteriorTest.open(578)) {
                ImpVaultTest.entrance(game);
                CoopFloorsTest.cross(game);
                Class<? extends Mob> type=fire
                        ? com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultElemental.Fire.class
                        : com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultElemental.Frost.class;
                Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff> effect=fire
                        ? com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning.class
                        : com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill.class;
                Mob elemental=CavesInteriorTest.stage(game,type,seat);
                for (int i=0; i<24 && game.hero(seat).buff(effect)==null; i++) waitTurn(game);
                assertNotNull(type.getSimpleName(),game.hero(seat).buff(effect));
                assertNull(game.hero(1-seat).buff(effect));
                assertTrue(elemental.createLoot() instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken);
                game.checkpoint();
            }
        }
    }

    public static void forScreen(CoopSession game) {
        move(game,enter(game));
        for(int i=0;i<35 && boss().warnedCells().isEmpty();i++) waitTurn(game);
        assertFalse(boss().warnedCells().isEmpty());
    }

    @Test public void highestTierReturnsOneNativeRingAndTheSharedPurseThroughTheCrystal() {
        Bundle saved;
        long id;
        int holder;
        int strength;
        try (CoopSession game=CavesInteriorTest.open(572)) {
            StagedInput move=prepareBoundary(game,"escape");
            holder=game.activeSeat();
            Ring ring=game.hero(holder).belongings.getItem(Ring.class);
            strength=ring.level();
            id=game.ownership.identity(ring);
            assertTrue(EscapeCrystal.rewardAllowed(ring,4000));
            assertFalse(EscapeCrystal.rewardAllowed(ring,3999));
            assertTrue(game.stairs.consent(1-holder,game.turn(),move.cell,false));
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.VAULT_REWARD,move.cell,ring));
            assertTrue(published("escape"));
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            Item returned=null;
            int matches=0;
            for(int seat=0;seat<2;seat++) for(Item item:game.hero(seat).belongings) {
                if (game.ownership.identity(item)==id) { returned=item; matches++; assertEquals(holder,seat); }
            }
            assertEquals(1,matches);
            assertEquals(strength,returned.level());
            assertEquals(game.player(holder),game.ownership.owner(returned));
            assertTrue(published("escape"));
        }
    }

    @Test public void seed572NativeMeleeChangesFormsUnlocksExistingStatueAndClaimsItOnce() {
        try(CoopSession game=CavesInteriorTest.open(572)) {
            int target=enter(game);
            assertEquals(696,treasure());
            long original=game.ownership.identity(Dungeon.level.heaps.get(696).peek());
            assertFalse(((VaultLevel)Dungeon.level).finalTreasureOpen());
            move(game,target);
            assertNotNull(boss());
            assertEquals(600,boss().HP);
            EnumSet<VaultBossElemental.ElementalForm> forms=EnumSet.noneOf(VaultBossElemental.ElementalForm.class);
            int count=0;
            while(boss()!=null && count++<400) {
                forms.add(boss().curForm());
                strike(game);
            }
            assertNull("Native fight exceeded 400 actions",boss());
            assertEquals(3,forms.size());
            assertTrue(((VaultLevel)Dungeon.level).finalTreasureOpen());
            assertEquals(696,treasure());
            int seat=game.activeSeat();
            game.hero(seat).pos=696;
            game.observe(seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,696,Dungeon.level.heaps.get(696).peek()));
            ImpStatue statue=game.hero(seat).belongings.getItem(ImpStatue.class);
            assertNotNull(statue);
            assertEquals(original,game.ownership.identity(statue));
            assertEquals(game.player(seat),game.ownership.owner(statue));
            assertEquals(-1,treasure());
            assertEquals(4000,game.stairs.vaultScore());
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,696,null));
        }
    }

    @Test public void corruptOrAbsentCooperativeDrawHistoryIsRejected() {
        Bundle b=new Bundle();
        b.put("elemental_form","FIRE");
        assertThrows(IllegalArgumentException.class,()->VaultBossElemental.savedFormWeights(b));
        for(int[] values:new int[][]{{1,1},{1,1,2,0},{-1,2,1},{0,3,0},{0,0,0},{2,0,0}}) {
            b.put("form_chances",values);
            assertThrows(IllegalArgumentException.class,()->VaultBossElemental.savedFormWeights(b));
        }
        b.put("form_chances",new float[]{1f,1.5f,2f});
        assertThrows(IllegalArgumentException.class,()->VaultBossElemental.savedFormWeights(b));
        b.put("form_chances",new int[]{1,2,2});
        assertArrayEquals(new int[]{1,2,2},VaultBossElemental.savedFormWeights(b));
    }
}
