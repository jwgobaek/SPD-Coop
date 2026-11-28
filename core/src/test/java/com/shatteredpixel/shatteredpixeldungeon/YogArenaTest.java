package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;

/** Staged adventurers and positions. Native boss health, clocks, fist selection, damage and turn order. */
public class YogArenaTest extends HeadlessDungeon {
    static void arrive(CoopSession game) { arrive(game,578); }

    static void arrive(CoopSession game, int arenaSeed) { arrive(game,arenaSeed,4); }

    static void arrive(CoopSession game, int arenaSeed,int surviving) {
        CityKingTest.prepareBoundary(game,"claim");
        while (Dungeon.depth<24) {
            SewerDepthTest.gate(game,true);
            if (Dungeon.depth>=21 && Dungeon.depth<25-surviving) {
                DemonSpawner spawner=resident(DemonSpawner.class);
                assertNotNull(spawner);
                for (int i=0;i<250 && spawner.isAlive();i++) CityKingTest.strike(game,spawner);
                assertFalse(spawner.isAlive());
            }
        }
        // Replay original arena seeds using the staged 578 approach; only the 578 route is continuous.
        Dungeon.seed=arenaSeed;
        for (int seat=0;seat<2;seat++) {
            Hero fighter=game.hero(seat);
            fighter.HTBoost += 4500;
            fighter.updateHT(false);
            fighter.HP=fighter.HT;
            fighter.belongings.weapon.upgrade(6);
        }
        SewerDepthTest.gate(game,true);
        assertEquals(HallsBossLevel.class,Dungeon.level.getClass());
        assertFalse(Dungeon.level.locked);
    }

    static <T extends Mob> T resident(Class<T> type) {
        for (Mob m:Dungeon.level.mobs) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    static void start(CoopSession game) {
        int spawn=Dungeon.level.exit()+3*Dungeon.level.width();
        for (int dir:PathFinder.NEIGHBOURS8) {
            int cell=spawn+dir;
            if (Dungeon.level.passable[cell] && Actor.findChar(cell)==null) {
                MiningBranchTest.standBeside(game,cell);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,cell,null));
                break;
            }
        }
        assertTrue(Dungeon.level.locked);
        YogDzewa boss=resident(YogDzewa.class);
        assertNotNull(boss);
        assertEquals(1000,boss.HT);
        for (int i=0;i<8 && boss.phase()==0;i++) TenguArenaTest.waitOne(game);
        assertEquals(1,boss.phase());
    }

    @Test public void destroyedHallsSpawnersSetTheNativeSavedSummonRotation() {
        for (int living=0;living<=4;living++) {
            try (CoopSession game=CavesInteriorTest.open(578)) {
                arrive(game,578,living);
                start(game);
                assertEquals(living,Statistics.spawnersAlive);
                Bundle boss=new Bundle();
                resident(YogDzewa.class).storeInBundle(boss);
                Class[] rolls=boss.getClassArray("regular_summons");
                assertEquals(4,rolls.length);
                assertEquals(living,java.util.Arrays.stream(rolls).filter(YogDzewa.YogRipper.class::equals).count());
                assertEquals(4-living,java.util.Arrays.stream(rolls).filter(YogDzewa.Larva.class::equals).count());
            }
        }
    }

    /** Emit actual staged saves for a Desktop fixture, keeping both rosters consistent. */
    public static void main(String[] args) throws Exception {
        YogArenaTest fixture=new YogArenaTest();
        fixture.temporary.create();
        fixture.startBackend();
        try {
            for (String name:new String[]{"yog-warning","yog-first","yog-amulet","yog-finish"}) {
                try (CoopSession game=CoopSession.open(578,1,com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR)) {
                    game.join(2,com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.MAGE);
                    forScreen(game,name);
                    try (java.io.OutputStream out=java.nio.file.Files.newOutputStream(java.nio.file.Paths.get(args[0],name))) {
                        assertTrue(Bundle.write(game.checkpoint(),out,false));
                    }
                }
            }
        } finally {
            fixture.stopBackend();
            fixture.temporary.delete();
        }
    }

    public static void forScreen(CoopSession game,String name) {
        if (name.equals("yog-amulet")) {
            prepareBoundary(game,"amulet");
            for (int i=0;i<10 && game.activeSeat()!=1;i++) TenguArenaTest.waitOne(game);
            assertEquals(1,game.activeSeat());
            takeAmulet(game);
            return;
        }
        prepareBoundary(game,name.equals("yog-warning")?"warning":name.equals("yog-first")?"first":"propose");
        if (Dungeon.depth==25) {
            Mob focus=name.equals("yog-first") ? resident(YogFist.class) : resident(YogDzewa.class);
            for (int off:PathFinder.NEIGHBOURS8) {
                int cell=focus.pos+off;
                if (Dungeon.level.passable[cell] && Actor.findChar(cell)==null) {
                    game.hero(0).pos=cell;
                    game.observe(0);
                    assertTrue(game.knowledge(game.hero(0)).visible(focus.pos));
                    break;
                }
            }
        }
        if (name.equals("yog-finish")) {
            int owner=game.amuletOwner();
            assertTrue(game.chooseEnding(owner,true));
            assertTrue(game.chooseEnding(1-owner,true));
        }
    }

    @Test public void genuineRayWindupAndFirstFistRetainTheirIdentity() {
        Bundle windup;
        int id;
        int[] cells;
        try (CoopSession game=CavesInteriorTest.open(578)) {
            arrive(game);
            start(game);
            YogDzewa boss=resident(YogDzewa.class);
            for (int i=0;i<80 && boss.aimedCells().length==0;i++) TenguArenaTest.waitOne(game);
            cells=boss.aimedCells();
            assertTrue(cells.length>0);
            id=boss.id();
            windup=game.checkpoint();
        }
        Bundle first;
        int fistID;
        try (CoopSession game=CoopSession.restore(windup)) {
            YogDzewa boss=(YogDzewa)Actor.findById(id);
            assertArrayEquals(cells,boss.aimedCells());
            for (int i=0;i<100 && boss.phase()==1;i++) CityKingTest.strike(game,boss);
            assertEquals(2,boss.phase());
            assertEquals(700,boss.HP);
            YogFist fist=resident(YogFist.class);
            assertNotNull(fist);
            assertTrue(boss.isInvulnerable(Hero.class));
            assertTrue(fist.isInvulnerable(Hero.class));
            fistID=fist.id();
            first=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(first)) {
            assertEquals(fistID,resident(YogFist.class).id());
            assertEquals(id,resident(YogDzewa.class).id());
            TenguArenaTest.waitOne(game);
        }
    }

    static void fight(CoopSession game, java.util.List<String> defeated) {
        fight(game,defeated,null);
    }

    static StagedInput fight(CoopSession game, java.util.List<String> defeated,String boundary) {
        return fight(game, defeated, boundary, null);
    }

    interface TrialAttack {
        boolean use(CoopSession game, Mob visibleTarget);
    }

    static StagedInput fight(CoopSession game, java.util.List<String> defeated, String boundary, TrialAttack special) {
        YogDzewa boss=resident(YogDzewa.class);
        boolean phaseFive=false;
        YogFist previous=null;
        for (int step=0;step<900 && boss.isAlive();step++) {
            YogFist fist=resident(YogFist.class);
            if (fist!=previous && previous!=null) {
                assertFalse(previous.isAlive());
                defeated.add(previous.getClass().getSimpleName());
            }
            if (fist!=previous && fist!=null) {
                assertTrue(boss.isInvulnerable(Hero.class));
                SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
            }
            previous=fist;
            phaseFive |= boss.phase()==5;
            Hero acting=game.hero(game.activeSeat());
            PotionOfHealing potion=acting.belongings.getItem(PotionOfHealing.class);
            if (acting.HP<acting.HT/2 && acting.buff(Healing.class)==null && potion!=null && potion.isKnown()) {
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.DRINK,0,potion));
                assertFalse("party lost while healing",game.defeated());
                continue;
            }
            if (fist!=null && fist.isInvulnerable(Hero.class)) {
                if (Dungeon.level.distance(game.hero(0).pos,boss.pos)<11 ||
                        Dungeon.level.distance(game.hero(1).pos,boss.pos)<11) {
                    SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
                }
                StagedInput before="phase5".equals(boundary) && boss.phase()==4 && fist.HP<=100 ? new StagedInput(game,"WAIT",0) : null;
                TenguArenaTest.waitOne(game);
                if (before!=null && published(boundary)) return before;
            } else {
                Mob target=fist==null ? boss : fist;
                target=CityKingTest.reachableOpponent(game,target);
                if (target==null) {
                    TenguArenaTest.waitOne(game);
                    continue;
                }
                MiningBranchTest.standBeside(game,target.pos);
                boolean capture="kill".equals(boundary) && boss.phase()==5 && boss.HP<=100
                        || ("first".equals(boundary) || "second".equals(boundary) || "third".equals(boundary))
                             && fist==null && boss.phase()>=1 && boss.phase()<=3 && boss.HP<=1100-300*boss.phase()
                        || ("first-death".equals(boundary) || "second-death".equals(boundary) || "phase5".equals(boundary))
                             && fist!=null && fist.HP<=100;
                boolean visible=game.observe(game.activeSeat()).visible(target.pos) && target.invisible==0;
                StagedInput before=capture ? new StagedInput(game,visible?"ATTACK":"WAIT",visible?target.pos:0) : null;
                if (visible) {
                    if (special==null || !special.use(game,target))
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,target.pos,null));
                }
                else TenguArenaTest.waitOne(game);
                if (before!=null && published(boundary)) return before;
            }
            assertFalse("party lost at "+boss.phase()+" HP "+boss.HP,game.defeated());
        }
        assertTrue("last phase missed, phase="+boss.phase()+" hp="+boss.HP+" fist="+resident(YogFist.class),phaseFive);
        assertNull(resident(YogDzewa.class));
        assertEquals(3,defeated.size());
        assertFalse(Dungeon.level.locked);
        assertEquals(Terrain.EXIT,Dungeon.level.map[Dungeon.level.exit()]);
        return null;
    }

    static void awaitExit(CoopSession game) {
        SewerSpecialActorTest.atGate(game,Dungeon.level.exit());
        for (int i=0; i<100 && game.stairs.stair()!=Dungeon.level.exit(); i++) {
            if (game.activeSeat()<0) break;
            TenguArenaTest.waitOne(game);
        }
        String state = "";
        if (game.stairs.stair()!=Dungeon.level.exit()) {
            state = "active="+game.activeSeat()+" turn="+game.turn()+" actorTime="
                    +com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()+" stop="+game.stopReason();
            for (int seat=0; seat<2; seat++) {
                var hero = game.hero(seat);
                state += " seat"+seat+"="+game.life(seat)+",hp="+hero.HP+",pos="+hero.pos
                        +",seesExit="+game.knowledge(hero).visible(Dungeon.level.exit());
            }
        }
        assertEquals("Both surviving heroes must see and reach the open exit "+state, Dungeon.level.exit(), game.stairs.stair());
    }

    static int amuletCell() {
        for (com.shatteredpixel.shatteredpixeldungeon.items.Heap heap:Dungeon.level.heaps.valueList())
            if (heap.peek() instanceof com.shatteredpixel.shatteredpixeldungeon.items.Amulet) return heap.pos;
        return -1;
    }

    static void positionAtAmulet(CoopSession game) {
        int cell=amuletCell();
        assertTrue(cell>=0);
        SewerSpecialActorTest.atGate(game,cell);
        game.hero(game.activeSeat()).pos=cell;
        game.observe(0);
        game.observe(1);
    }

    static void takeAmulet(CoopSession game) {
        positionAtAmulet(game);
        int seat=game.activeSeat(),cell=amuletCell();
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,Dungeon.level.heaps.get(cell).peek()));
        assertEquals(seat,game.amuletOwner());
        assertTrue(Statistics.amuletObtained);
        assertEquals(CoopSession.Ending.OFFER,game.ending());
    }

    public static String signature(CoopSession game) {
        YogDzewa boss=resident(YogDzewa.class);
        StringBuilder result=new StringBuilder(MiningBranchTest.signature(game));
        result.append('|').append(game.ending()).append('|').append(game.amuletOwner());
        if (game.amuletOwner()>=0) {
            com.shatteredpixel.shatteredpixeldungeon.items.Amulet item=game.hero(game.amuletOwner()).belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.Amulet.class);
            result.append('|').append(game.ownership.identity(item)).append('|').append(game.ownership.owner(item));
        }
        if (boss!=null) {
            Bundle b=new Bundle();
            boss.storeInBundle(b);
            result.append('|').append(boss.id()).append('|').append(boss.phase()).append('|').append(boss.HP)
                .append('|').append(java.util.Arrays.toString(boss.aimedCells())).append('|').append(b.getInt("beam_target"))
                .append('|').append(java.util.Arrays.toString(b.getClassArray("fist_summons")))
                .append('|').append(java.util.Arrays.toString(b.getClassArray("regular_summons")))
                .append('|').append(b.getFloat("ability_cd")).append('|').append(b.getFloat("summon_cd"));
        }
        return result.toString();
    }

    public static boolean published(String boundary) {
        if (boundary.equals("mine-return") || boundary.equals("branch-capacity")) {
            Bundle dormant=Dungeon.coop.checkpoint().getBundle("dormant");
            return published("amulet") && dormant.contains("1013") && dormant.contains("1018") && dormant.getKeys().size()==27;
        }
        CoopSession game=Dungeon.coop;
        YogDzewa boss=resident(YogDzewa.class);
        switch (boundary) {
            case "warning":return boss!=null && boss.aimedCells().length>0;
            case "ray":return boss!=null && boss.aimedCells().length==0;
            case "down":return game.life(0)==CoopSession.Life.DOWNED || game.life(1)==CoopSession.Life.DOWNED;
            case "revive":return game.life(0)==CoopSession.Life.ALIVE && game.life(1)==CoopSession.Life.ALIVE;
            case "regular":return resident(YogDzewa.YogRipper.class)!=null || resident(YogDzewa.Larva.class)!=null;
            case "second":return boss!=null && boss.phase()==3 && resident(YogFist.class)!=null && boss.HP==400;
            case "third":return boss!=null && boss.phase()==4 && resident(YogFist.class)!=null && boss.HP==100;
            case "first-death":return boss!=null && boss.phase()==2 && resident(YogFist.class)==null;
            case "second-death":return boss!=null && boss.phase()==3 && resident(YogFist.class)==null;
            case "phase5":return boss!=null && boss.phase()==5 && resident(YogFist.class)==null;
            case "first":return boss!=null && boss.phase()==2 && resident(YogFist.class)!=null;
            case "kill":return Dungeon.depth==25 && boss==null && !Dungeon.level.locked && Dungeon.level.map[Dungeon.level.exit()]==Terrain.EXIT;
            case "return25":return Dungeon.depth==25 && !Dungeon.level.locked && boss==null && game.ending()==CoopSession.Ending.CONTINUING;
            case "revisit26":return Dungeon.depth==26 && amuletCell()<0 && game.amuletOwner()>=0 && game.ending()==CoopSession.Ending.CONTINUING;
            case "last":return Dungeon.depth==26 && amuletCell()>=0;
            case "amulet":return game.ending()==CoopSession.Ending.OFFER;
            case "propose":return game.ending()==CoopSession.Ending.PROPOSED;
            case "win":return game.ending()==CoopSession.Ending.WON;
            default:throw new IllegalArgumentException(boundary);
        }
    }

    /** Compare the uninterrupted 578 route with a separate, mixed-seed storage fixture. */
    static StagedInput visitSideLevels(CoopSession game,boolean combinedCapacity) {
        while (Dungeon.depth>13) SewerDepthTest.gate(game,false);
        Blacksmith smith=resident(Blacksmith.class);
        assertNotNull(smith);
        MiningBranchTest.standBeside(game,smith.pos);
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ACCEPT_BLACKSMITH,smith.pos,null));
        visitMine(game);
        while (Dungeon.depth<18) SewerDepthTest.gate(game,true);
        Imp imp=resident(Imp.class);
        assertNotNull(imp);
        MiningBranchTest.standBeside(game,imp.pos);
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ACCEPT_IMP,imp.pos,null));
        int gate=Dungeon.level.getTransition(LevelTransition.Type.BRANCH_EXIT).cell();
        MiningBranchTest.atBranchGate(game,gate);
        if (combinedCapacity) {
            // Isolated capacity fixture: only the native vault roll comes from 572.
            long campaignSeed=Dungeon.seed;
            Dungeon.seed=572;
            CoopFloorsTest.cross(game);
            assertTrue(Dungeon.level instanceof VaultLevel);
            Dungeon.seed=campaignSeed;
            assertTrue(new DwarfToken().collect(game.hero(game.activeSeat()).belongings.backpack));
            game.observe(0);
            game.observe(1);
            CoopFloorsTest.cross(game);
            assertEquals(0,Dungeon.branch);
        } else {
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.TRAVEL,gate,null));
            CoopFloorsTest.cross(game);
            assertTrue(Dungeon.level instanceof VaultLevel);
            assertEquals(578,Dungeon.seed);
            VaultElementalTest.earnVaultTokenAndEscape(game);
            assertEquals(0,Dungeon.branch);
        }
        while (Dungeon.depth<26) SewerDepthTest.gate(game,true);
        Bundle dormant=game.checkpoint().getBundle("dormant");
        assertEquals(27,dormant.getKeys().size());
        if (combinedCapacity) assertTrue(dormant.getBundle("1018").getBundle("level").getString("__className").endsWith("VaultLevel"));
        assertTrue(dormant.getBundle("1013").getBundle("level").getString("__className").endsWith("MiningLevel"));
        positionAtAmulet(game);
        return new StagedInput(game,"PICK_UP",amuletCell());
    }

    private static void visitMine(CoopSession game) {
        MiningBranchTest.atBranchGate(game,Dungeon.level.getTransition(LevelTransition.Type.BRANCH_EXIT).cell());
        CoopFloorsTest.cross(game);
        assertEquals(1,Dungeon.branch);
        assertTrue(Dungeon.level instanceof MiningLevel);
        MiningBranchTest.atBranchGate(game,Dungeon.level.getTransition(
                LevelTransition.Type.BRANCH_ENTRANCE).cell());
        CoopFloorsTest.cross(game);
        assertEquals(0,Dungeon.branch);
    }

    private static void giveArenaHealing(CoopSession game) {
        for (int seat=0; seat<2; seat++) {
            PotionOfHealing potions=new PotionOfHealing();
            potions.quantity(3);
            potions.setKnown();
            assertTrue(potions.collect(game.hero(seat).belongings.backpack));
        }
    }

    public static StagedInput prepareBoundary(CoopSession game,String boundary) {
        arrive(game);
        // Both bearers must survive to exercise the full-pack pickup and consent boundary.
        if (boundary.equals("amulet")) giveArenaHealing(game);
        start(game);
        if (boundary.equals("down") || boundary.equals("revive")) {
            Hero ally=game.hero(1-game.activeSeat());
            MiningBranchTest.standBeside(game,ally.pos);
            assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.Ankh().collect(ally.belongings.backpack));
            ally.HP=17;
            ally.damage(999,YogDzewa.class);
            assertTrue(published("down"));
            return new StagedInput(game,boundary.equals("down")?"WAIT":"REVIVE",boundary.equals("down")?0:ally.pos);
        }
        if (boundary.equals("warning") || boundary.equals("ray") || boundary.equals("regular")) {
            if (boundary.equals("ray"))
                for (int i=0;i<100 && !published("warning");i++) TenguArenaTest.waitOne(game);
            for (int i=0;i<100;i++) {
                StagedInput before=new StagedInput(game,"WAIT",0);
                TenguArenaTest.waitOne(game);
                if (published(boundary)) return before;
            }
            throw new AssertionError(boundary);
        }
        StagedInput combat=fight(game,new java.util.ArrayList<>(),boundary);
        if (combat!=null) return combat;
        awaitExit(game);
        if (boundary.equals("last")) {
            return new StagedInput(game,"TRAVEL",Dungeon.level.exit());
        }
        CoopFloorsTest.cross(game);
        positionAtAmulet(game);
        if (boundary.equals("mine-return") || boundary.equals("branch-capacity")) return visitSideLevels(game,boundary.equals("branch-capacity"));
        if (boundary.equals("amulet")) return new StagedInput(game,"PICK_UP",amuletCell());
        takeAmulet(game);
        if (boundary.equals("return25") || boundary.equals("revisit26")) {
            assertTrue(game.chooseEnding(game.amuletOwner(),false));
            SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
            if (boundary.equals("return25")) return new StagedInput(game,"TRAVEL",Dungeon.level.entrance());
            CoopFloorsTest.cross(game);
            assertTrue(published("return25"));
            SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
            assertEquals(-1,game.stairs.stair());
            awaitExit(game);
            return new StagedInput(game,"TRAVEL",Dungeon.level.exit());
        }
        if (boundary.equals("win")) assertTrue(game.chooseEnding(game.amuletOwner(),true));
        return new StagedInput(game,"END_RUN",0);
    }

    @Test public void nativeThreeFistEncountersOpenTheRouteToOneAmulet() {
        for (int seed:new int[]{578,570,572,573}) {
            Bundle won;
            int winner;
            try (CoopSession game=CavesInteriorTest.open(578)) {
                arrive(game,seed);
                giveArenaHealing(game);
                start(game);
                java.util.ArrayList<String> defeated=new java.util.ArrayList<>();
                fight(game,defeated);
                System.out.println("YOG_VICTORY seed="+seed+" defeated="+defeated);
                awaitExit(game);
                CoopFloorsTest.cross(game);
                assertEquals(LastLevel.class,Dungeon.level.getClass());
                assertEquals(26,Dungeon.depth);
                assertEquals(1,Dungeon.level.heaps.valueList().stream().flatMap(h -> h.items.stream())
                        .filter(i -> i instanceof com.shatteredpixel.shatteredpixeldungeon.items.Amulet).count());
                takeAmulet(game);
                int owner=game.amuletOwner();
                assertFalse(game.chooseEnding(1-owner,true));
                assertTrue(game.chooseEnding(owner,true));
                assertFalse(game.chooseEnding(owner,true));
                assertTrue(game.chooseEnding(1-owner,false));
                assertEquals(CoopSession.Ending.CONTINUING,game.ending());
                TenguArenaTest.waitOne(game);
                assertTrue(game.chooseEnding(owner,true));
                assertTrue(game.chooseEnding(1-owner,true));
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.WAIT,0,null));
                assertFalse(game.chooseEnding(1-owner,true));
                won=game.checkpoint();
                winner=owner;
            }
            try (CoopSession again=CoopSession.restore(won)) {
                    assertEquals(CoopSession.Ending.WON,again.ending());
                    assertEquals(winner,again.amuletOwner());
            }
        }
    }
}
