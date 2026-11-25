package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CavesInteriorTest;
import com.shatteredpixel.shatteredpixeldungeon.CavesBossTest;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.headless.*;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.utils.*;
import com.shatteredpixel.shatteredpixeldungeon.MiningBranchTest;
import com.shatteredpixel.shatteredpixeldungeon.CityEntranceTest;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Fresh host process for every invocation. Control files are confined to a test-private directory. */
public final class RestartHost {
    static boolean measureBoundary(String mode) {
        return mode.equals("tengu-read-devices") || mode.equals("tengu-read-city-19")
                || mode.equals("tengu-read-mine-crystal-ore")
                || Boolean.getBoolean("spd.measureKing") && mode.equals("tengu-read-king-shield");
    }
    static boolean hazard(String mode) {
        return mode.equals("dart") || mode.equals("geyser") || mode.equals("poison-dart") || mode.equals("iron-door");
    }
    static boolean plant(String mode) { return mode.startsWith("plant-") && !mode.equals("plant-receipt"); }

    enum Landing {
        FIREBLOOM(new Firebloom.Seed(), BuffIndicator.FIRE),
        ICECAP(new Icecap.Seed(), BuffIndicator.FROST),
        SUNGRASS(new Sungrass.Seed(), BuffIndicator.HERB_HEALING),
        BLINDWEED(new Blindweed.Seed(), BuffIndicator.BLINDNESS),
        STORMVINE(new Stormvine.Seed(), BuffIndicator.VERTIGO),
        SWIFTTHISTLE(new Swiftthistle.Seed(), BuffIndicator.TIME),
        MAGEROYAL(new Mageroyal.Seed(), BuffIndicator.WEAKNESS);

        final Plant.Seed seed;
        final int icon;
        Landing(Plant.Seed seed, int icon) {
            this.seed=seed;
            this.icon=icon;
        }
    }
    static boolean guardian(String mode) {
        return mode.equals("armored") || mode.equals("statue") || mode.equals("unstable") || mode.equals("phantom") || mode.equals("exile");
    }
    static boolean knife(String mode) { return mode.equals("knife0") || mode.equals("knife1"); }
    static boolean kit(String mode) { return mode.equals("kit0") || mode.equals("kit1"); }
    static boolean newCampaign(String mode) {
        return mode.startsWith("journey-seven-") || mode.equals("prison") || mode.equals("campaign") || mode.equals("campaign-start") || mode.equals("campaign-team");
    }
    private static long seedFor(String mode) {
        if (mode.startsWith("tengu-setup-city-") || mode.startsWith("tengu-setup-king-")) return 572;
        if(mode.startsWith("tengu-setup-mine-gnoll")) return 571;
        if (mode.startsWith("tengu-setup-") || mode.startsWith("necro-setup-") || mode.equals("late-setup")) return 570;
        if (mode.startsWith("wand-setup-")) return Integer.parseInt(mode.split("-")[2]);
        switch (mode) {
            case "blocked-start": return 457; // Native HandAxe/Elastic remains outside the audited standalone effect set.
            case "armored": return 446;
            case "phantom": return 479;
            case "exile": return 420;
            case "statue": return 416;
            case "unstable": return 421;
            case "crystal-new": return 411;
            case "golden": return 427;
            case "mimic": return 408;
            case "hive": return 409;
            case "quest": return 403;
            case "piranha": return 406;
            case "walk": return 415;
            case "journey-seven-581": case "prison": case "campaign": case "campaign-start": return 581;
            case "journey-seven-1105": case "campaign-team": return 1105;
            default: return 402;
        }
    }

    public static void main(String[] args) throws Exception {
        Path base=Paths.get(args[0]);
        String mode=args[1];
        HeadlessApplicationConfiguration config=new HeadlessApplicationConfiguration();
        config.preferencesDirectory=base.toString()+"/";
        HeadlessApplication backend=new HeadlessApplication(new ApplicationAdapter(){},config);
        FileUtils.setDefaultFileProperties(com.badlogic.gdx.Files.FileType.Absolute,base.toString()+"/");
        Game.version="4.0.2-INDEV"; Game.versionCode=922;
        new Game(null,null) {{ requestedReset=false; }};
        try { host(base,mode); } finally { backend.exit(); }
    }

    private static void host(Path path,String mode) throws Exception {
        CoopSaves saves=new CoopSaves(path.toFile(),stage->{
            if ((mode.equals("kill_"+stage) || (mode.equals("ascendfault") && stage.equals("BEFORE_HEAD"))) && java.nio.file.Files.exists(path.resolve("fault-armed")))
                Runtime.getRuntime().halt(80);
            if (stage.equals("WRITING") && java.nio.file.Files.exists(path.resolve("fail-write")))
                throw new IOException("Injected write failure");
            if (stage.equals("AFTER_HEAD") && mode.startsWith("tengu-commit-") && java.nio.file.Files.exists(path.resolve("boss-command"))) {
                String boundary=mode.substring("tengu-commit-".length());
                if (boundary.startsWith("king-")) {
                    if (CityKingTest.published(boundary.substring(5))) {
                        write(path,"tengu-signature",CityKingTest.signature(Dungeon.coop));
                        Runtime.getRuntime().halt(78);
                    }
                    return;
                }
                if (boundary.startsWith("mine-") || boundary.startsWith("city-")) {
                    if (boundary.startsWith("city-") ? CityEntranceTest.published(boundary.substring(5)) : MiningBranchTest.published(boundary.substring(5))) {
                        write(path,"tengu-signature",MiningBranchTest.signature(Dungeon.coop));
                        Runtime.getRuntime().halt(78);
                    }
                    return;
                }
                if (boundary.startsWith("dm-")) {
                    if (CavesBossTest.published(boundary.substring(3))) {
                        write(path,"tengu-signature",CavesBossTest.signature(Dungeon.coop));
                        Runtime.getRuntime().halt(78);
                    }
                    return;
                }
                if (boundary.startsWith("interior-")) {
                    if (CavesInteriorTest.committed(boundary.substring(9))) {
                        write(path,"tengu-signature",CavesInteriorTest.signature(Dungeon.coop));
                        Runtime.getRuntime().halt(78);
                    }
                    return;
                }
                String phase=Dungeon.depth==10 ? TenguArenaTest.floor().state().name() : "";
                if ((!boundary.equals("kill") || phase.equals("WON")) && (!boundary.equals("pause") || phase.equals("FIGHT_PAUSE"))) {
                    write(path,"tengu-signature",TenguArenaTest.signature(Dungeon.coop));
                    Runtime.getRuntime().halt(78);
                }
            }
            if (stage.equals("AFTER_HEAD") && mode.equals("goo") && java.nio.file.Files.exists(path.resolve("boss-action"))
                    && Actor.findById(Integer.parseInt(read(path,"boss-id")))==null) {
                verifyBossLoot(path);
                Runtime.getRuntime().halt(78);
            }
            if (stage.equals("AFTER_HEAD") && (mode.equals("armored") || mode.equals("unstable")) && java.nio.file.Files.exists(path.resolve("watch-hit"))) {
                int before=Integer.parseInt(read(path,"watch-hit"));
                if (Dungeon.coop.hero(1).HP < before) {
                    write(path,"committed-hit",Dungeon.coop.hero(1).HP+" "+Dungeon.coop.turn());
                    Runtime.getRuntime().halt(78);
                }
            }
            if (stage.equals("AFTER_HEAD") && mode.equals("crystal-fight") && java.nio.file.Files.exists(path.resolve("strike"))
                    && Actor.findById(Integer.parseInt(read(path,"crystal-id")))==null) Runtime.getRuntime().halt(78);
            if (stage.equals("AFTER_HEAD") && mode.equals("necro-summon") && java.nio.file.Files.exists(path.resolve("summon-request"))) {
                Necromancer necro=(Necromancer)Actor.findById(Integer.parseInt(read(path,"caster")));
                int minion=NecromancerTest.data(necro).getInt("my_skeleton");
                if (Actor.findById(minion) instanceof Necromancer.NecroSkeleton) {
                    write(path,"summoned",Integer.toString(minion));
                    Runtime.getRuntime().halt(78);
                }
            }
            if (stage.equals("AFTER_HEAD") && knife(mode) && java.nio.file.Files.exists(path.resolve("pin-pending"))) {
                Mob target=(Mob)Actor.findById(Integer.parseInt(read(path,"pin-target").split(" ")[0]));
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion cushion=target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion.class);
                if (cushion!=null) {
                    com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon stuck=cushion.getStuckItems().get(0);
                    write(path,"pin-identity",target.id()+" "+Dungeon.coop.ownership.identity(stuck)+" "+Dungeon.coop.ownership.owner(stuck)+" "+Float.floatToIntBits(stuck.durabilityLeft()));
                    Runtime.getRuntime().halt(78);
                }
            }
            if (stage.equals("AFTER_HEAD") && java.nio.file.Files.exists(path.resolve("kill-before-reply"))) {
                if (mode.startsWith("market-")) {
                    long start=Long.parseLong(read(path,"trade-start"));
                    System.out.println("trade publication "+mode+" elapsed_us="+(System.currentTimeMillis()-start)*1000
                            +" active_actors="+Actor.all().size()+" used_heap_bytes="+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
                }
                Runtime.getRuntime().halt(78);
            }
        });
        if (mode.equals("schema3-backup") || mode.equals("prison-backup")) {
            UUID id=saves.sessions().get(0);
            assertThrows(IOException.class,()->saves.inspect(id,false));
            assertEquals(mode.equals("prison-backup") ? 5 : 3,saves.inspect(id,true).depth);
        }
        InetAddress loop=InetAddress.getLoopbackAddress();
        boolean special = plant(mode) || mode.equals("goo") || hazard(mode) || mode.equals("blocked-start") || guardian(mode) || mode.equals("crystal-new") || mode.equals("sentry") || mode.equals("piranha") || mode.equals("quest") || mode.equals("hive") || (mode.equals("mimic") || mode.equals("golden"));
        if (mode.equals("seven-resume") || mode.equals("market-buy0")) {
            try (CoopSaves.Slot original=saves.existing(saves.sessions().get(0))) {
                Bundle engine=original.read(false).getBundle("engine");
                if (mode.equals("seven-resume")) PrisonCombatChecks.copiesOf(engine);
                else PrisonShopChecks.copiesOf(engine);
            }
        }
        StagedInput stagedInput=null;
        try(LanRoom room=(special || mode.startsWith("tengu-setup-") || mode.equals("late-setup") || mode.startsWith("necro-setup-") || mode.startsWith("wand-setup-") || mode.startsWith("scribe") || kit(mode) || knife(mode) || newCampaign(mode) || mode.equals("new") || mode.equals("walk") || mode.startsWith("cast")) ? LanRoom.persistentGame(loop,0,0,seedFor(mode),saves)
                : LanRoom.resume(loop,0,0,saves,saves.sessions().get(0),mode.equals("backup") || mode.equals("floorbackup") || mode.equals("schema3-backup") || mode.equals("prison-backup"))) {
            if (mode.startsWith("tengu-read-")) {
                boolean dm=mode.startsWith("tengu-read-dm-");
                boolean interior=mode.startsWith("tengu-read-interior-");
                assertEquals(read(path,"tengu-signature"),mode.startsWith("tengu-read-king-") ? CityKingTest.signature(Dungeon.coop) : (mode.startsWith("tengu-read-mine-") || mode.startsWith("tengu-read-city-")) ? MiningBranchTest.signature(Dungeon.coop) : dm?CavesBossTest.signature(Dungeon.coop):interior?CavesInteriorTest.signature(Dungeon.coop):TenguArenaTest.signature(Dungeon.coop));
                String boundary=mode.substring("tengu-read-".length());
                String expected=boundary.equals("enter") ? "FIGHT_START" : boundary.equals("pause") ? "FIGHT_PAUSE"
                        : boundary.equals("kill") || boundary.equals("claim") ? "WON" : "FIGHT_ARENA";
                if (boundary.startsWith("king-")) { assertTrue(CityKingTest.published(boundary.substring(5))); } else if (boundary.startsWith("city-")) { assertTrue(CityEntranceTest.published(boundary.substring(5))); } else if (boundary.startsWith("mine-")) { assertTrue(MiningBranchTest.published(boundary.substring(5))); } else if (dm) { assertTrue(CavesBossTest.published(boundary.substring(3))); } else if (interior) {
                    assertTrue(CavesInteriorTest.committed(boundary.substring(9)));
                } else if (boundary.equals("caves")) {
                    assertEquals(11,Dungeon.depth);
                    assertNotNull(CoopTrade.merchant(Dungeon.coop.hero(0),false));
                } else {
                    if (boundary.equals("ten")) expected="START";
                    if (boundary.equals("return")) expected="WON";
                    assertEquals(expected,TenguArenaTest.floor().state().name());
                }
                if (boundary.equals("revive") || boundary.equals("down")) {
                    int seat=1-Integer.parseInt(read(path,"boss-command").split(" ")[0]);
                    assertEquals(boundary.equals("revive") ? CoopSession.Life.ALIVE : CoopSession.Life.DOWNED,Dungeon.coop.life(seat));
                    assertEquals(17,Dungeon.coop.preDeathHP(seat));
                    Hero victim = Dungeon.coop.hero(seat);
                    if (boundary.equals("revive")) {
                        // The published signature above fixes the exact HP after the arena's next actors.
                        // The isolated room-entry test also checks 5 HP at an undamaged revival.
                        int revivedHP = CoopSession.reviveHP(17,victim.HT);
                        assertEquals(5,revivedHP);
                        assertTrue(victim.HP > 0 && victim.HP <= revivedHP);
                        assertNull(victim.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.Ankh.class));
                    } else {
                        assertEquals(0,victim.HP);
                        assertNotNull(victim.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.Ankh.class));
                    }
                }
                if (boundary.equals("devices")) {
                    Tengu boss=TenguArenaTest.boss();
                    assertNotNull(boss.buff(Tengu.BombAbility.class));
                    assertNotNull(boss.buff(Tengu.ShockerAbility.class));
                    assertNotNull(boss.buff(Tengu.FireAbility.class));
                }
                if (boundary.equals("claim")) {
                    int seat=Integer.parseInt(read(path,"boss-command").split(" ")[0]);
                    Item mask=Dungeon.coop.hero(seat).belongings.getItem(TengusMask.class);
                    assertNotNull(mask);
                    assertEquals(Dungeon.coop.player(seat),Dungeon.coop.ownership.owner(mask));
                }
            }
            if (mode.startsWith("late-down") || mode.startsWith("late-up")) {
                int depth=Integer.parseInt(mode.replaceFirst("^late-(down|up)", ""));
                assertEquals(depth,Dungeon.depth);
                assertEquals(depth==7 ? 5 : depth==11 ? 7 : 6,Dungeon.coop.checkpoint().getInt("schema"));
                SewerSpecialActorTest.atGate(Dungeon.coop,mode.startsWith("late-down") ? Dungeon.level.exit() : Dungeon.level.entrance());
            }
            if (mode.equals("schema2-to-three")) SewerSpecialActorTest.atGate(Dungeon.coop,Dungeon.level.exit());
            if (mode.equals("prison-backup")) {
                assertEquals(5,Dungeon.depth);
                assertEquals(5,Dungeon.coop.checkpoint().getBundle("dormant").getKeys().size());
            }
            if (mode.equals("seven-travel")) {
                assertEquals(6,Dungeon.depth);
                SewerSpecialActorTest.atGate(Dungeon.coop,Dungeon.level.exit());
            } else if (mode.equals("seven-resume")) {
                assertEquals(7,Dungeon.depth);
                Bundle state=Dungeon.coop.checkpoint();
                assertEquals(5,state.getInt("schema"));
                assertEquals(6,state.getBundle("dormant").getKeys().size());
                System.out.println("prison-seven saved_bytes="+state.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length+
                        " active_actors="+Actor.all().size());
            } else if (mode.equals("seven-return")) {
                assertEquals(6,Dungeon.depth);
                assertEquals(5,Dungeon.coop.checkpoint().getInt("schema"));
                assertNotNull(CoopTrade.merchant(Dungeon.coop.hero(0),false));
            }
            if (mode.startsWith("market-")) arrangeMarket(path,mode);
            if (mode.equals("necro-receipt")) {
                int minion=Integer.parseInt(read(path,"summoned"));
                Necromancer necro=(Necromancer)Actor.findById(Integer.parseInt(read(path,"caster")));
                assertEquals(minion,NecromancerTest.data(necro).getInt("my_skeleton"));
                assertTrue(Actor.findById(minion) instanceof Necromancer.NecroSkeleton);
                assertEquals(1,Dungeon.level.mobs.stream().filter(m->m instanceof Necromancer.NecroSkeleton).count());
            }
            if (mode.equals("wand-receipt")) {
                assertNull(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker.Quest.wand1);
                assertFalse(Dungeon.level.mobs.stream().anyMatch(m -> m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker));
            }
            if (mode.equals("pin-receipt")) {
                String[] saved=read(path,"pin-identity").split(" ");
                Mob target=(Mob)Actor.findById(Integer.parseInt(saved[0]));
                Item stuck=target.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion.class).getStuckItems().get(0);
                assertEquals(Long.parseLong(saved[1]),Dungeon.coop.ownership.identity(stuck));
                assertEquals(Long.parseLong(saved[2]),Dungeon.coop.ownership.owner(stuck));
                assertEquals(Integer.parseInt(saved[3]),Float.floatToIntBits(((com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)stuck).durabilityLeft()));
            }
            if (mode.equals("prison-resume")) {
                assertEquals(6,Dungeon.depth);
                assertTrue(Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel);
                assertEquals(5,Dungeon.coop.checkpoint().getBundle("dormant").getKeys().size());
                assertNotEquals(Dungeon.coop.hero(0).pos,Dungeon.coop.hero(1).pos);
                assertTrue(Dungeon.level.mobs.stream().anyMatch(m->m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper));
            }
            if (mode.equals("goo-receipt")) {
                assertEquals(5,Dungeon.depth);
                assertFalse(Dungeon.level.locked);
                assertNull(Actor.findById(Integer.parseInt(read(path,"boss-id"))));
                assertEquals(4,Dungeon.coop.checkpoint().getBundle("dormant").getKeys().size());
                verifyBossLoot(path);
            }
            if (mode.startsWith("crystal-") && !mode.equals("crystal-new")) crystalResumeFixture(path,mode);
            Rat rat=null;
            if (mode.equals("revive") || mode.equals("soak")) {
                CoopSession g=Dungeon.coop;
                Hero guest=g.hero(1);
                assertEquals(mode.equals("revive"),guest.frozenInCoop());
                assertEquals(mode.equals("revive"),guest.belongings.getItem(Ankh.class)!=null);
                Item item=Dungeon.level.heaps.get(g.hero(0).pos).peek();
                assertEquals(g.player(0),g.ownership.owner(item));
                assertEquals(Long.parseLong(read(path,"dropid")),g.ownership.identity(item));
            }
            write(path,"endpoint.tmp",room.tcpPort()+"\n"+room.hostJoinCommand());
            java.nio.file.Files.move(path.resolve("endpoint.tmp"),path.resolve("endpoint"));
            boolean started=false, profiled=false;
            String lastGuide="";
            int arrangedDepth=0, campaignDepth=0, profiledCampaignDepth=0;
            long arrivalTurn=0;
            String arrangedQuest="";
            while(!java.nio.file.Files.exists(path.resolve("stop"))) {
                if (java.nio.file.Files.deleteIfExists(path.resolve("inflict"))) {
                    CoopSession g=Dungeon.coop;
                    assertEquals(0,g.activeSeat());
                    Hero guest=g.hero(1);
                    guest.HP=17;
                    rat.attack(guest,1,10000,Float.POSITIVE_INFINITY);
                    assertEquals(CoopSession.Life.DOWNED,g.life(1));
                    write(path,"injured","ok");
                }
                if (mode.equals("crystal-fight") && java.nio.file.Files.deleteIfExists(path.resolve("approach"))) {
                    CrystalMimic thief=SewerSpecialActorTest.actor(CrystalMimic.class);
                    SewerGhostTest.meet(Dungeon.coop,Dungeon.coop.activeSeat(),thief);
                    write(path,"in-range","1");
                }
                room.pump();
                if (room.failure()!=null && !(mode.equals("failure") && room.failure().equals("CHECKPOINT_FAILED")))
                    throw new IllegalStateException(room.failure());
                if (!started && newCampaign(mode) && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    com.watabou.utils.Random.pushGenerator(seedFor(mode));
                }
                if (mode.equals("seven-resume") && room.state()==LanRoom.State.IN_GAME
                        && java.nio.file.Files.deleteIfExists(path.resolve("measure-seven"))) {
                    measureSaves(path,room,Dungeon.depth);
                }
                if (mode.equals("campaign") && room.state()==LanRoom.State.IN_GAME) {
                    if (campaignDepth!=Dungeon.depth) { campaignDepth=Dungeon.depth; arrivalTurn=Dungeon.coop.turn(); }
                    if (profiledCampaignDepth!=campaignDepth && Dungeon.coop.turn()-arrivalTurn>=25) {
                        measureSaves(path,room,campaignDepth);
                        profiledCampaignDepth=campaignDepth;
                    }
                }
                if (!profiled && (mode.equals("walk") || mode.equals("backatstairs")) && room.state()==LanRoom.State.IN_GAME) {
                    profiled=true;
                    measureSaves(path,room,Dungeon.depth);
                }
                if (mode.equals("walk") && room.state()==LanRoom.State.IN_GAME) {
                    CoopSession g=Dungeon.coop;
                    if (!started) { started=true; com.watabou.utils.Random.pushGenerator(415); }
                    String suggestion=g.turn()+" "+g.activeSeat()+" "+CoopFloorsTest.guide(g);
                    if(!suggestion.equals(lastGuide)) {
                        write(path,"guide.tmp",suggestion);
                        java.nio.file.Files.move(path.resolve("guide.tmp"),path.resolve("guide"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                        lastGuide=suggestion;
                    }
                }
                if (special && room.state()==LanRoom.State.IN_GAME && arrangedDepth!=Dungeon.depth) {
                    arrangedDepth=Dungeon.depth;
                    if (mode.equals("goo")) {
                        if (Dungeon.depth<5) SewerSpecialActorTest.atGate(Dungeon.coop,Dungeon.level.exit());
                        else {
                            Goo boss=SewerSpecialActorTest.actor(Goo.class);
                            SewerDepthTest.arrangeBoss(Dungeon.coop,boss);
                            write(path,"boss-id",Integer.toString(boss.id()));
                        }
                        measureSaves(path,room,Dungeon.depth);
                    } else if (plant(mode)) {
                        String[] parts=mode.split("-");
                        int seat=Integer.parseInt(parts[2]);
                        Landing landing=Landing.valueOf(parts[1]);
                        int cell=SewerPlantTest.arrange(Dungeon.coop,seat,landing.seed,true);
                        if (landing==Landing.MAGEROYAL)
                            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(Dungeon.coop.hero(seat),
                                    com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness.class,30);
                        write(path,"landing",parts[1]+" "+seat+" "+cell+" "+Dungeon.level.plants.get(cell).image);
                    } else if (hazard(mode)) {
                        CoopSession game = Dungeon.coop;
                        int cell = com.shatteredpixel.shatteredpixeldungeon.SewerHazardsTest.approach(game,1,!mode.equals("iron-door"));
                        if (mode.equals("poison-dart") || mode.equals("geyser")) {
                            com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap trap = mode.equals("geyser")
                                    ? new com.shatteredpixel.shatteredpixeldungeon.levels.traps.GeyserTrap() :
                                    new com.shatteredpixel.shatteredpixeldungeon.levels.traps.PoisonDartTrap();
                            Dungeon.level.setTrap(trap,cell);
                            trap.reveal();
                        }
                        if (mode.equals("iron-door")) Dungeon.level.drop(new com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey(1),game.hero(1).pos);
                        write(path,"hazard-cell",Integer.toString(cell));
                    } else if (arrangedDepth==1) SewerSpecialActorTest.atGate(Dungeon.coop,Dungeon.level.exit());
                    else if (guardian(mode)) {
                        Mob resident;
                        if (mode.equals("phantom")) {
                            PhantomPiranha fish=SewerSpecialActorTest.actor(PhantomPiranha.class);
                            SewerGuardiansTest.approachPool(Dungeon.coop,1,fish);
                            resident=fish;
                        } else {
                            resident=mode.equals("armored") ? SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue.class) : (mode.equals("statue") || mode.equals("unstable")) ? SewerSpecialActorTest.actor(Statue.class)
                                    : SewerSpecialActorTest.actor(GnollExile.class);
                            SewerGhostTest.meet(Dungeon.coop,1,resident);
                            if (mode.equals("armored")) Dungeon.coop.hero(1).HP=Dungeon.coop.hero(1).HT=120;
                            if (mode.equals("unstable")) {
                                assertTrue(((Statue)resident).weapon().enchantment instanceof
                                        com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Unstable);
                                Dungeon.coop.hero(1).HP=Dungeon.coop.hero(1).HT=120;
                            }
                        }
                        write(path,"resident-id",Integer.toString(resident.id()));
                    } else if (mode.equals("crystal-new")) {
                        CrystalMimic thief=SewerSpecialActorTest.actor(CrystalMimic.class);
                        Food food=SewerCrystalMimicTest.pack(Dungeon.coop,1,3);
                        SewerGhostTest.meet(Dungeon.coop,1,thief);
                        write(path,"encounter-cell",Integer.toString(thief.pos));
                        write(path,"crystal-id",Integer.toString(thief.id()));
                        write(path,"bag-id",Long.toString(Dungeon.coop.ownership.identity(food)));
                    } else if ((mode.equals("mimic") || mode.equals("golden"))) {
                        com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic mimic=
                                mode.equals("golden") ? SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic.class) : SewerSpecialActorTest.actor(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic.class);
                        SewerGhostTest.meet(Dungeon.coop,1,mimic);
                        write(path,"encounter-cell",Integer.toString(mimic.pos));
                    } else if (mode.equals("hive")) {
                        CoopSession g=Dungeon.coop;
                        boolean found=false;
                        for (Heap heap:Dungeon.level.heaps.valueList()) {
                            if (heap.type!=Heap.Type.HEAP || !(heap.peek() instanceof Honeypot) || Actor.findChar(heap.pos)!=null) continue;
                            g.hero(1).pos=heap.pos;
                            found=true;
                            break;
                        }
                        assertTrue(found);
                    }
                    else if (!mode.equals("quest")) SewerSpecialActorTest.exposeOneSeat(Dungeon.coop,1,mode.equals("sentry"));
                }
                if (mode.equals("quest") && arrangedDepth==2) {
                    String offer=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.Quest.sewerOffer();
                    if (!offer.equals(arrangedQuest) && !offer.isEmpty()) {
                        arrangedQuest=offer;
                        for (Mob mob:Dungeon.level.mobs) {
                            boolean opponent=offer.equals("HUNT");
                            if (opponent ? mob instanceof FetidRat : mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost) {
                                // Keep dialogue available between the two socket requests in this placement fixture.
                                if (!opponent) mob.state=mob.PASSIVE;
                                SewerGhostTest.meet(Dungeon.coop,0,mob);
                                SewerGhostTest.meet(Dungeon.coop,1,mob);
                                break;
                            }
                        }
                    }
                }
                if (mode.startsWith("scribe") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    int seat=mode.charAt(6)-'0';
                    CoopSession game=Dungeon.coop;
                    CoopMageTest.encounter(game,seat);
                    Hero owner=game.hero(seat);
                    Item armor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor();
                    Item upgrade=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade().identify();
                    Item identify=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify().identify();
                    for (Item piece:new Item[]{armor,upgrade,identify}) {
                        assertTrue(piece.collect(owner.belongings.backpack));
                        game.ownership.claim(piece,game.player(seat));
                    }
                }
                if (mode.startsWith("tengu-setup-") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    String name=mode.substring("tengu-setup-".length());
                    if (name.startsWith("king-")) {
                        stagedInput=CityKingTest.prepareBoundary(Dungeon.coop,name.substring(5));
                        write(path,"king-action",stagedInput.verb);
                    } else if (name.startsWith("mine-") && (name.endsWith("boss") || name.endsWith("upgrade") || name.endsWith("reforge"))) {
                        stagedInput=MiningBranchTest.equipmentOrBoss(Dungeon.coop,name.substring(5));
                        write(path,"mine-action",stagedInput.verb);
                        write(path,"mine-index",Integer.toString(stagedInput.itemOrder));
                    }
                    int target=stagedInput!=null ? stagedInput.cell : name.startsWith("city-") ? CityEntranceTest.prepareBoundary(Dungeon.coop,name.substring(5)) : name.startsWith("mine-") ? MiningBranchTest.prepareBoundary(Dungeon.coop,name.substring(5)) : name.startsWith("dm-") ? CavesBossTest.prepareBoundary(Dungeon.coop,name.substring(3)) : name.startsWith("interior-") ? CavesInteriorTest.prepareSocket(Dungeon.coop,name.substring(9))
                            : TenguArenaTest.prepareSocket(Dungeon.coop,name);
                    write(path,"tengu-target",Integer.toString(target));
                    room.save();
                    write(path,"return-ready","1");
                }
                if (mode.equals("late-setup") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    WandmakerTest.enterPrison(Dungeon.coop);
                    room.save();
                    write(path,"return-ready","1");
                }
                if ((RestartHost.measureBoundary(mode) || mode.equals("tengu-read-caves") || mode.equals("tengu-read-dm-devices"))
                        && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    measureSaves(path,room,Dungeon.depth);
                }
                if (mode.equals("late-up9") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    measureSaves(path,room,9);
                }
                if (mode.startsWith("necro-setup-") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    WandmakerTest.enterPrison(Dungeon.coop);
                    int seat=Integer.parseInt(mode.substring("necro-setup-".length()));
                    Necromancer necro=NecromancerTest.arrange(Dungeon.coop,seat,false);
                    NecromancerTest.prepare(Dungeon.coop,necro);
                    write(path,"caster",Integer.toString(necro.id()));
                    room.save();
                    write(path,"return-ready","1");
                }
                if (mode.startsWith("wand-setup-") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    String[] spec=mode.split("-");
                    com.shatteredpixel.shatteredpixeldungeon.WandmakerTest.prepareReturn(Dungeon.coop,Integer.parseInt(spec[2]),Integer.parseInt(spec[3]));
                    measureSaves(path,room,7);
                    write(path,"return-ready","1");
                }
                if (knife(mode) && !started && room.state()==LanRoom.State.IN_GAME) {
                    int seat=mode.charAt(5)-'0'; started=true;
                    Rat r=CoopMageTest.encounter(Dungeon.coop,seat);
                    Item knives=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife().identify(false);
                    assertTrue(knives.collect(Dungeon.coop.hero(seat).belongings.backpack));
                    write(path,"pin-target",r.id()+" "+r.pos);
                }
                if (kit(mode) && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    CoopSession g=Dungeon.coop;
                    int seat=mode.charAt(3)-'0';
                    CoopMageTest.encounter(g,seat);
                    Hero user=g.hero(seat);
                    user.HP=3;
                    Dungeon.level.drop(new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing().quantity(2),user.pos);
                }
                if (mode.startsWith("cast") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    int seat=mode.charAt(mode.length()-1)-'0';
                    com.shatteredpixel.shatteredpixeldungeon.CoopMageTest.encounter(Dungeon.coop,seat);
                    if (mode.startsWith("cast-hand")) {
                        com.shatteredpixel.shatteredpixeldungeon.items.wands.TargetedDamageWand wand = mode.startsWith("cast-hand-blast")
                                ? new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave() : mode.startsWith("cast-hand-fire")
                                ? new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast() :
                                new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();
                        wand.identify(false);
                        assertTrue(wand.collect(Dungeon.coop.hero(seat).belongings.backpack));
                        Dungeon.coop.ownership.claim(wand, Dungeon.coop.player(seat));
                    }
                }
                if (mode.equals("new") && !started && room.state()==LanRoom.State.IN_GAME) {
                    started=true;
                    CoopSession g=Dungeon.coop;
                    Hero h=g.hero(0), guest=g.hero(1);
                    assertTrue(new Ankh().collect(guest.belongings.backpack));
                    Dungeon.level.drop(new Food(),h.pos);
                    for (Mob m:new ArrayList<>(Dungeon.level.mobs)) { Actor.remove(m); Dungeon.level.mobs.remove(m); }
                    rat=new Rat();
                    for(int step:PathFinder.NEIGHBOURS8) {
                        int cell=h.pos+step;
                        if (Dungeon.level.passable[cell] && Actor.findChar(cell)==null) { rat.pos=cell; break; }
                    }
                    assertTrue(rat.pos>0);
                    rat.HT=rat.HP=1000;
                    GameScene.add(rat,10000);
                }
                Thread.sleep(1);
            }
            if (mode.equals("new")) {
                CoopSession g=Dungeon.coop;
                Item item=Dungeon.level.heaps.get(g.hero(0).pos).peek();
                assertEquals(g.player(0),g.ownership.owner(item));
                write(path,"dropid",Long.toString(g.ownership.identity(item)));
            }
            if (mode.equals("soak")) Runtime.getRuntime().halt(79);
            if (mode.equals("failure")) {
                assertEquals(LanRoom.State.PAUSED,room.state());
                assertThrows(IOException.class,room::saveAndClose);
            } else if (mode.equals("blocked-start")) {
                assertNull(Dungeon.coop);
                assertThrows(IOException.class,room::saveAndClose);
            } else { if (mode.equals("goo-receipt")) verifyBossLoot(path); room.saveAndClose(); }
        }
        // Publish the fixture immediately before the observed native transition. The next host
        // performs the input through TCP. Seat credentials and the room's sync state stay intact.
        if (stagedInput!=null) try(CoopSaves.Slot disk=saves.existing(saves.sessions().get(0))) {
            com.watabou.utils.Bundle prepared=disk.read(false);
            prepared.put("engine",stagedInput.before);
            disk.publish(prepared,false);
        }
    }

    // Only position and spending money are arranged. Stock, shopkeeper, price and receipts are native.
    private static void arrangeMarket(Path path,String mode) throws IOException {
        CoopSession game=Dungeon.coop;
        assertEquals(6,Dungeon.depth);
        com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper shop=CoopTrade.merchant(game.hero(0),false);
        assertNotNull(shop);
        int seat=Integer.parseInt(mode.substring(mode.length()-1));
        Hero buyer=game.hero(seat);
        String suffix=Integer.toString(seat);
        if (mode.startsWith("market-buy")) {
            Heap stock=Dungeon.level.heaps.valueList().stream()
                    .filter(h->h.type==Heap.Type.FOR_SALE && !h.peek().stackable && CoopTrade.supported(h.peek())
                              && com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.canSell(buyer,h.peek()))
                    .findFirst().get();
            SewerSpecialActorTest.atGate(game,stock.pos);
            buyer.pos=stock.pos;
            Dungeon.gold=10000;
            write(path,"stock"+suffix,stock.pos+" "+game.ownership.identity(stock.peek())+" "+stock.peek().image()+" "+stock.peek().value());
        } else {
            String[] stock=read(path,"stock"+suffix).split(" ");
            long id=Long.parseLong(stock[1]);
            assertNull(Dungeon.level.heaps.get(Integer.parseInt(stock[0])));
            int held=0,returned=0;
            for (Item item:buyer.belongings) if (game.ownership.identity(item)==id) {
                held++; assertEquals(game.player(seat),game.ownership.owner(item));
            }
            for (Item item:shop.buybackItems) if (game.ownership.identity(item)==id) {
                returned++; assertEquals(game.player(seat),game.ownership.owner(item));
            }
            assertEquals(1,held+returned);
            assertEquals(mode.startsWith("market-reclaim") ? 1 : 0,returned);
            assertEquals(Integer.parseInt(read(path,"expected-gold")),Dungeon.gold);
            SewerSpecialActorTest.atGate(game,shop.pos);
        }
        for (int i=0;i<2;i++) game.observe(i);
    }

    private static void crystalResumeFixture(Path path,String mode) throws IOException {
        CoopSession game=Dungeon.coop;
        int id=Integer.parseInt(read(path,"crystal-id"));
        CrystalMimic thief=(CrystalMimic)Actor.findById(id);
        Food food=game.hero(1).belongings.getItem(Food.class);
        assertEquals(Long.parseLong(read(path,"bag-id")),game.ownership.identity(food));
        assertEquals(game.player(1),game.ownership.owner(food));
        if (mode.equals("crystal-open")) {
            assertEquals(3,food.quantity());
            assertTrue(CoopSession.disguised(thief));
        } else if (mode.equals("crystal-park") || mode.equals("crystal-fight")) {
            assertEquals(2,food.quantity());
            assertEquals(thief.FLEEING,thief.state);
            Item held=thief.items.get(thief.items.size()-1);
            assertEquals(Food.class,held.getClass());
            assertEquals(1,held.quantity());
            assertTrue(held.cursed && held.cursedKnown);
            assertEquals(game.player(1),game.ownership.owner(held));
            if (mode.equals("crystal-park")) {
                write(path,"stolen-id",Long.toString(game.ownership.identity(held)));
                SewerSpecialActorTest.atGate(game,Dungeon.level.entrance());
            } else {
                assertEquals(Long.parseLong(read(path,"stolen-id")),game.ownership.identity(held));
                for (int seat=0;seat<2;seat++) SewerGhostTest.meet(game,seat,thief);
            }
        } else if (mode.equals("crystal-return")) {
            assertEquals(1,Dungeon.depth);
            assertNull(thief);
            assertEquals(2,food.quantity());
            SewerSpecialActorTest.atGate(game,Dungeon.level.exit());
        } else if (mode.equals("crystal-loot")) {
            assertNull(thief);
            assertEquals(2,food.quantity());
            int pos=Integer.parseInt(read(path,"strike").split(" ")[1]);
            Item recovered=Dungeon.level.heaps.get(pos).peek();
            assertEquals(Long.parseLong(read(path,"stolen-id")),game.ownership.identity(recovered));
            assertEquals(game.player(1),game.ownership.owner(recovered));
            assertTrue(recovered.cursed && recovered.cursedKnown);
            game.hero(1).pos=pos;
        } else {
            assertNull(thief);
            assertEquals(3,food.quantity());
            long retired=Long.parseLong(read(path,"stolen-id"));
            for(Heap h:Dungeon.level.heaps.valueList()) for(Item i:h.items) assertNotEquals(retired,game.ownership.identity(i));
        }
    }

    private static void verifyBossLoot(Path path) throws IOException {
        int keys=0,blobs=0;
        for(Heap heap:Dungeon.level.heaps.valueList()) for(Item item:heap.items) {
            if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey) keys+=item.quantity();
            if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob) blobs+=item.quantity();
        }
        assertEquals(1,keys); assertTrue(blobs>=2 && blobs<=4);
        String before=java.nio.file.Files.exists(path.resolve("boss-loot")) ? read(path,"boss-loot") : "";
        String rewards=keys+" "+blobs+" "+Dungeon.coop.hero(0).exp+" "+Dungeon.coop.hero(1).exp+" "+Statistics.bossScores[0];
        if(before.isEmpty()) write(path,"boss-loot",rewards);
        else assertEquals(before,rewards);
    }

    private static void measureSaves(Path path,LanRoom room,int depth) throws IOException {
        long[] samples=new long[21];
        for(int i=0;i<samples.length;i++) {
            long start=System.nanoTime();
            room.save();
            samples[i]=System.nanoTime()-start;
        }
        long disk=0,size=0;
        try(java.util.stream.Stream<Path> paths=java.nio.file.Files.walk(path.resolve("coop-saves-v1"))) {
            for(Iterator<Path> it=paths.iterator();it.hasNext();) {
                Path entry=it.next();
                if(!java.nio.file.Files.isRegularFile(entry)) continue;
                long n=java.nio.file.Files.size(entry);
                disk+=n;
                if(entry.getFileName().toString().matches("g[0-9]+")) size=Math.max(size,n);
            }
        }
        Bundle checkpoint = Dungeon.coop.checkpoint();
        int parkedBytes = checkpoint.contains("dormant") ? checkpoint.getBundle("dormant").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length : checkpoint.contains("parked") ? checkpoint.getBundle("parked").toString()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8).length : 0;
        Arrays.sort(samples);
        Runtime vm=Runtime.getRuntime();
        write(path,"perf"+depth,"floor="+depth+" explicit_disk_saves=21 median_us="+samples[10]/1000+
                " p95_us="+samples[19]/1000+" generation_bytes="+size+" slot_bytes="+disk+
                " active_actors="+Actor.all().size()+" parked_utf8_bytes="+parkedBytes+
                " host_used_heap_bytes="+(vm.totalMemory()-vm.freeMemory()));
    }

    static void write(Path dir,String name,String value) throws IOException {
        java.nio.file.Files.write(dir.resolve(name),value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    static String read(Path dir,String name) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(dir.resolve(name)),java.nio.charset.StandardCharsets.UTF_8);
    }
}
