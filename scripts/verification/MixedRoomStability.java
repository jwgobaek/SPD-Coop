package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.ImpVaultTest;
import com.shatteredpixel.shatteredpixeldungeon.CoopMageTest;
import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MiningBranchTest;
import com.shatteredpixel.shatteredpixeldungeon.PlayerGrowthTest;
import com.shatteredpixel.shatteredpixeldungeon.SewerSpecialActorTest;
import com.shatteredpixel.shatteredpixeldungeon.StagedInput;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.junit.Assert.*;

/** One JVM, one backend, real persistent rooms and two authenticated TCP clients per room. */
public final class MixedRoomStability {
    final Map<String,Integer> verbs=new TreeMap<>();
    final List<Long> latencies=new ArrayList<>();
    final Map<String,List<Long>> latencyByVerb=new TreeMap<>();

    void record(String action,long start) {
        long elapsed=System.nanoTime()-start;
        verbs.merge(action,1,Integer::sum);
        latencies.add(elapsed);
        latencyByVerb.computeIfAbsent(action,key->new ArrayList<>()).add(elapsed);
    }

    final List<WeakReference<LanRoom>> closed=new ArrayList<>();
    int rejected,duplicates,rejoins;
    long saveNanos;
    String[] normalRoster;
    private static VaultPublicRoute.Input input(PublicDungeon view,VaultPublicRoute.Guide guide) {
        if (view.source.rows.containsKey("enchantphase")) {
            if (view.source.rows.get("enchantphase").equals("options"))
                return new VaultPublicRoute.Input("ENCHANT_CHOOSE", 0, 0);
            for (PublicDungeon.Loot gear : view.belongings)
                if (view.source.rows.containsKey("enchantgear"+gear.id))
                    return new VaultPublicRoute.Input("ENCHANT_TARGET", 0, gear.id);
            return new VaultPublicRoute.Input("ENCHANT_CANCEL", 0, 0);
        }
        for(Talent t:Talent.values()) {
            String rank=view.source.rows.get("learn"+t.ordinal());
            String tier=view.source.rows.get("tier"+t.ordinal());
            if(rank!=null && Integer.parseInt(rank)<t.maxPoints() &&
                    Integer.parseInt(view.source.rows.getOrDefault("freepoints"+tier,"0"))>0)
                return new VaultPublicRoute.Input("INVEST_TALENT",t.ordinal(),0);
        }
        if (Boolean.getBoolean("stability.rogue") && "ROGUE".equals(view.heroClass) && !view.monsters.isEmpty()) {
            for (PublicDungeon.Loot item : view.belongings) {
                if ("1".equals(view.source.rows.get("stealth"+item.id)) && item.charges>0
                        && !view.source.rows.containsKey("cloaked"+item.id))
                    return new VaultPublicRoute.Input("CLOAK",0,item.id);
            }
        }
        return guide.choose(view);
    }
    // Deliberately arranged rooms are reported separately from the ordinary start workload.
    static final String[] GROWTH = {"third0","enhanced1","shock0","endure1","blades0","beacon1",
            "recall0","crown0","battery1","cloak0","dart","food","healing","floor","mine"};

    static final String[] WAND_ARTS={"imbue0","volley1","hawk0","direct1","lunge0","spike1","charged0","light1","ward0","blessweapon1","ambush1","freerun0","smoke1","third1","floor","mine"};

    static final String[] TIER_GROWTH = {"holdfast0","holdfast1","scoutshot0","scoutshot1","cleanse0","cleanse1",
            "holylance0","holylance1","deathlesswait0","deathlesswait1","trinitymindmissilehistory0","trinitymindmissilehistory1",
            "spark0","monkfocus1","elestrike0","poisontip0","poisontip1","floor","mine"};
    static final String[] DISCOVERIES = {"trinitybodyhistory0", "trinitybodyhistory1", "trinitymindhistory0",
            "trinitymindhistory1", "trinityspirithistory0", "trinityspirithistory1", "trinityfirehistory0",
            "trinityfirehistory1", "strikeblock0", "strikeblock1", "strikeshock0", "strikeshock1",
            "strikevenom0", "strikevenom1", "heroicenergy0", "heroicenergy1", "food", "floor"};

    static final String[] SHARED_GROWTH = {"bowstone0","bowstone1","bowrank0","bowrank1","bowthrow0","bowthrow1",
            "paladinheal0","paladinheal1","paladinaura0","paladinaura1","prayer0","prayer1","hallow0","hallow1",
            "wall0","wall1","warpwraith0","warpwraith1","hornhit0","hornhit1","cloakring0","cloakring1",
            "packedcloak0","packedcloak1","bookward0","bookward1","spark0","monkfocus1","poisontip0","elestrike1","food","dart","floor","mine"};

    static final String[] WAND_GARDEN = {"regrowth0","wardzap1","kitbrew1","guessstone0","seedbrew0",
            "regrowth1","wardzap0","earthheal0","visiondrink1","kitbrew0","floor","mine",
            "earthtrap0","kitstones1","kitseeds0","kitvision1","earthtrap1"};
    static final String[] EARTH_ALCHEMY = {"earthheal0","kitbrew1","detectstone0","guessstone1","seedbrew0",
            "earthheal1","kitbrew0","visiondrink1","mapread0","wornaugment1","floor","mine"};
    static final String[] RUNE_READING = {"detectstone0","guessstone1","fearstone0","seedbrew1","mapread0","cauldron1",
            "detectstone1","guessstone0","visiondrink1","bloodzap0","wornaugment1","maprecall0","floor","mine"};
    static final String[] RUNE_EFFECTS = {"fearstone0","fearstone1","visiondrink0","visiondrink1",
            "corruptzap0","corruptzap1","maprecall0","mapread1","seedbrew1","exoticbrew0","floor","mine"};
    static final String[] SEED_ALCHEMY = {"seedbrew0","seedbrew1","exoticbrew0","exoticbrew1","bloodzap0","bloodzap1",
            "healing","stasishold0","stasisrelease1","trinitymindraycast0","mine"};
    static final String[] CAULDRON = {"cauldron0", "cauldron1", "gaswait0", "gaswait1",
            "stasishold0", "stasisrelease1", "scrolltarget0", "scrollapply1", "readlullaby1", "floor", "mine"};

    static final String[] ORIGINAL_ITEMS = {"bowthrowBlooming0","bowthrowElastic1","bowthrowLucky0","bowthrowUnstable1",
            "bowthrowCorrupting0","bowthrowGrim1","readlullaby0","recalllullaby1","trinitybloomhistory0",
            "stasishold1","stasisrelease0","scrolltarget1","scrollapply0","floor","mine"};

    static final String[] STASIS_WARP = {"stasisrank0","stasishold1","stasisrelease0","warppropose1","warpreturn0",
            "scrolltarget1","scrollapply0","bowthrow1","rayblast0","lifelink1","food","floor","mine"};
    static final String[] ENCHANTMENT = {"scrollread0","scrollread1","scrolltarget0","scrolltarget1","scrollapply0",
            "scrollabort1","eagleview0","eagleview1","remoteplace0","rayblast1","ninjalog1",
            "divineshield1","lifelink0","beamingray1","bowthrow0","spark1","hallow0","feint1","food","dart","floor","mine"};

    static final String[] CACHE_GROWTH = {"cacheinvest0","cacheinvest1","trinityblockhistory0","trinityblockhistory1",
            "strikekinetic0","strikekinetic1","strikebloom0","strikebloom1","readrecharge0","readrecharge1","recallrecharge0","recallrecharge1","food","floor"};

    static final String[] SELECTORS = {"intuition0","intuition1","recallid0","recallid1",
            "trinityselect0","trinityselect1","trinitymindselect0","trinitymindselect1",
            "trinityspiritselect0","trinityspiritselect1","trinity0","trinity1",
            "trinitymind0","trinitymind1","trinityspirit0","trinityspirit1"};
    static final String[] SEAL_ARTS = {"sealdetach0","sealattach1","sealkeep0","sealtransfer1","rationthrow0","challenge1",
            "elestrike0","feint1","ascendform0","trinity1","trinitymind0","trinityspirit1","lightally0","lightorder1",
            "monkflurry0","monkdash1","monkdragonkick0","monkmeditate1","intuition0","bless1","recallid0","spark1","floor","mine"};
    static final String[] FROST_SENSES = {"iceimbue0", "ice1", "iceblast0", "icevolley1", "markdeath0", "clone1",
            "orderclone0", "stance1", "guardshield0", "lightshield1", "divinesense0", "sunray1",
            "radiance0", "smite1", "swap0", "monkfocus1", "floor", "mine"};

    static String stageClass(String stage,int seat) {
        if (stage==null) return seat==0 && Boolean.getBoolean("stability.rogue")?"ROGUE":seat==0?"MAGE":"HUNTRESS";
        if (stage.matches("(scrollread|scrolltarget|scrollapply|scrollabort|eagleview)[01]")) return "HUNTRESS";
        if (com.shatteredpixel.shatteredpixeldungeon.BowEffectsTest.isThrowFixture(stage)) return "HUNTRESS";
        if (stage.matches("(readlullaby|recalllullaby|trinitybloomhistory)[01]")) return "CLERIC";
        if (stage.startsWith("mapread") || stage.startsWith("maprecall")) return "CLERIC";
        if (stage.startsWith("stasis") || stage.startsWith("trinitymindray")) return "CLERIC";
        if (stage.startsWith("warppropose") || stage.startsWith("warpreturn")) return "MAGE";
        if (stage.startsWith("ninjalog")) return "ROGUE";
        if (stage.startsWith("cacheinvest")) return seat == stage.charAt(stage.length()-1)-'0' ? "ROGUE" : "MAGE";
        if (stage.matches("(remoteplace|rayblast|warpwraith|hornhit|spark|ice|iceimbue|iceblast|icevolley|imbue|volley)[01]")) return "MAGE";
        if (stage.matches("(strikekinetic|strikebloom|strikeblock|strikeshock|strikevenom|heroicenergy|challenge|elestrike|feint|monkflurry|monkdash|monkdragonkick|monkmeditate|swap|monkfocus|stance|guardshield|lunge|spike|charged)[01]")) return "DUELIST";
        if (stage.matches("(beamingray|lifelink|divineshield|bookward|paladinheal|paladinaura|prayer|hallow|wall|holylance|cleanse|trinitymindmissilehistory|trinityblockhistory|readrecharge|recallrecharge|trinitybodyhistory|trinitymindhistory|trinityspirithistory|trinityfirehistory|trinity|trinityselect|trinitymind|trinitymindselect|trinityspirit|trinityspiritselect|lightally|lightorder|ascendform|intuition|bless|recallid|radiance|smite|divinesense|sunray|lightshield|light|ward|blessweapon)[01]")) return "CLERIC";
        if (stage.matches("(bowstone|bowrank|bowthrow|poisontip|scoutshot|hawk|direct)[01]") || stage.startsWith("blades")) return "HUNTRESS";
        if (stage.matches("(cloakring|packedcloak|markdeath|clone|orderclone|cloak|ambush|freerun|smoke)[01]")) return "ROGUE";
        if (stage.startsWith("beacon") || stage.startsWith("recall") || stage.startsWith("battery")) return "MAGE";
        return "WARRIOR";
    }

    static VaultPublicRoute.Input arrange(String stage) {
        CoopSession room=Dungeon.coop;
        Hero h=room.hero(room.activeSeat());
        int cell=h.pos;
        String verb;
        switch(stage) {
        case "gaswait0": case "gaswait1": {
            int seat = stage.charAt(stage.length()-1)-'0';
            Hero caster = room.hero(seat);
            var wand = new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion();
            wand.onZap(caster, new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(
                    caster.pos, caster.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_TARGET));
            return new VaultPublicRoute.Input("WAIT",0,0);
        }
        case "floor":
            cell=Dungeon.level.getTransition(LevelTransition.Type.REGULAR_EXIT).cell();
            SewerSpecialActorTest.atGate(room,cell);
            verb="TRAVEL";
            break;
        case "mine":
            cell=MiningBranchTest.prepareBoundary(room,"gnoll-enter");
            verb="TRAVEL";
            break;
        case "dart": case "healing": case "food":
            cell=CoopMageTest.encounter(room,room.activeSeat()).pos;
            if (!stage.equals("food")) {
                Item item=stage.equals("dart")?new Dart():new PotionOfHealing();
                item.identify();
                if(item instanceof Dart) item.quantity(4);
                assertTrue(item.collect(h.belongings.backpack));
                room.ownership.claim(item,room.player(room.activeSeat()));
            }
            verb=stage.equals("dart")?"THROW":stage.equals("healing")?"DRINK":"EAT";
            if(!stage.equals("dart")) cell=0;
            break;
        default:
            boolean paired = stage.startsWith("paired-exotic");
            String nativeStage = paired ? "exoticbrew" + stage.charAt(stage.length()-1) : stage;
            StagedInput staged=PlayerGrowthTest.stage(room,nativeStage);
            if (paired) {
                int peer = 1-room.activeSeat();
                Item second = new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade();
                assertTrue(second.collect(room.hero(peer).belongings.backpack));
                room.ownership.claim(second,room.player(peer));
                room.observe(peer);
            }
            verb=staged.verb;
            cell=staged.cell;
        }
        return new VaultPublicRoute.Input(verb,cell,0);
    }

    static VaultPublicRoute.Input fromView(PublicDungeon view,VaultPublicRoute.Input staged) {
        String verb=staged.action;
        long item=0;
        if (verb.equals("BREW_SEEDS") || verb.equals("BREW_ENCHANTMENT")) {
            String key=verb.equals("BREW_SEEDS") ? "brewseed" : "brewexotic";
            item=view.belongings.stream().filter(it->view.source.rows.containsKey(key+it.id)).findFirst().get().id;
        }
        if (verb.equals("BREW_EXOTIC")) item=view.belongings.stream()
                .filter(it->view.source.rows.containsKey("exoticrecipe"+it.id)).findFirst().get().id;
        if (verb.equals("BREW_RUNESTONES")) item=view.belongings.stream()
                .filter(it->view.source.rows.containsKey("brew"+it.id)).findFirst().get().id;
        if (verb.equals("READ_SCROLL")) item=view.belongings.stream().filter(it->view.actions(it).contains(verb)).findFirst().get().id;
        if (verb.equals("ENCHANT_TARGET")) item=view.belongings.stream().filter(it->view.source.rows.containsKey("enchantgear"+it.id)).findFirst().get().id;
        if (verb.equals("SHOOT")) item=view.belongings.stream().filter(it->it.kind.equals("BOW")).findFirst().get().id;
        if (verb.matches("STASIS|BEAMING_RAY|LIFE_LINK|DIVINE_INTERVENTION|LAY_ON_HANDS|AURA_OF_PROTECTION|MNEMONIC_PRAYER|HALLOWED_GROUND|WALL_OF_LIGHT|HOLY_LANCE|CLEANSE|BODY_FORM|MIND_FORM|SPIRIT_FORM|BLESS|HOLY_INTUITION|RECALL_INSCRIPTION|SMITE|RADIANCE|DIVINE_SENSE|SUNRAY|SHIELD_OF_LIGHT|GUIDING_LIGHT|HOLY_WEAPON|HOLY_WARD")) item=view.belongings.stream().filter(it->view.actions(it).contains("CAST")).findFirst().get().id;
        if (verb.equals("TRINITY_MIND") || verb.equals("TRINITY_SPIRIT"))
            item=view.belongings.stream().filter(it->view.actions(it).contains("ARMOR_ABILITY")).findFirst().get().id;
        if (verb.equals("DETACH_SEAL") || verb.equals("AFFIX_SEAL") || verb.endsWith("_GLYPH")) {
            String visible=verb.equals("DETACH_SEAL") ? verb : "AFFIX_SEAL";
            item=view.belongings.stream().filter(it->view.actions(it).contains(visible)).findFirst().get().id;
        }
        if(Arrays.asList("DETECT_MAGIC","GUESS_ITEM","AUGMENT_GEAR","ENCHANT_BOW","READ_RECHARGING","READ_LULLABY","WEAPON_ABILITY","IMBUE","THROW","EAT","DRINK","ZAP","USE_CROWN","CLOAK","ARMOR_ABILITY").contains(verb)) {
            item=view.belongings.stream().filter(it->view.actions(it).contains(verb))
                    .filter(it->!verb.equals("THROW") || it.kind.equals("THROW_STONE") || it.kind.equals("DART") || it.kind.equals("SPIKE") || it.kind.equals("STONE") || it.kind.equals("FOOD") && view.heroClass.equals("WARRIOR"))
                    .findFirst().orElseThrow(()->new AssertionError("missing public action "+verb)).id;
        }
        return new VaultPublicRoute.Input(verb,staged.cell,item);
    }

    void play(LanRoom room,InetSocketAddress addr,int budget,String stage,CompletableFuture<VaultPublicRoute.Input> ready) throws Exception {
        LanClient[] clients={LanClient.synchronizedGame(addr),LanClient.synchronizedGame(addr)};
        try {
            SeatCredential[] keys={SeatCredential.generate(),SeatCredential.generate()};
            DisplayReplica[] images=new DisplayReplica[2];
            SeatActions[] actions=new SeatActions[2];
            VaultPublicRoute.Guide[] guides={new VaultPublicRoute.Guide(),new VaultPublicRoute.Guide()};
            UUID epoch=null;
            for(int i=0;i<2;i++) {
                assertTrue(clients[i].request(i==0?room.hostJoinCommand():"JOIN").startsWith("JOINED "));
                UUID seated=UUID.fromString(keys[i].enroll(clients[i]).split(" ")[1]);
                if(i==0) epoch=seated; else assertEquals(epoch,seated);
                assertEquals("OK",clients[i].request("SELECT "+(normalRoster==null ? stageClass(stage,i) : normalRoster[i])));
                images[i]=new DisplayReplica(room.session,i);
                actions[i]=new SeatActions(room.session,i,i+1);
            }
            for(LanClient c:clients)c.request("READY 1");
            assertEquals("GAME_STARTED",clients[0].request("START"));
            if (normalRoster!=null) {
                for (int role=0;role<2;role++) {
                    images[role].refresh(clients[role]);
                    PublicDungeon view=new PublicDungeon(images[role].image());
                    assertEquals(normalRoster[role],view.heroClass);
                    SeatQuickslots shortcuts=SeatQuickslots.defaults(view);
                    if ("DUELIST".equals(view.heroClass)) {
                        assertEquals("WEAPON_ABILITY",PublicDungeon.shortcutAction(shortcuts.item(0,view)));
                        assertEquals(view.offered,view.actions(shortcuts.item(0,view)).contains("WEAPON_ABILITY"));
                        assertEquals("SPIKE",shortcuts.item(1,view).kind);
                        assertEquals(2,shortcuts.item(1,view).quantity);
                    }
                    if ("CLERIC".equals(view.heroClass)) {
                        assertEquals("CAST",PublicDungeon.shortcutAction(shortcuts.item(0,view)));
                        assertEquals(view.offered,view.actions(shortcuts.item(0,view)).contains("CAST"));
                        assertEquals(3,shortcuts.item(0,view).charges);
                    }
                }
            }
            VaultPublicRoute.Input first=stage==null?null:ready.get(90,TimeUnit.SECONDS);
            int seat=0;
            for(int n=0;n<budget;n++) {
                if((n==0 && first!=null) || n==15 || n%100==99) {
                    clients[1].close();
                    clients[1]=LanClient.synchronizedGame(addr);
                    assertTrue(keys[1].reconnect(clients[1],epoch,1,2).startsWith("RESUMED "));
                    images[1].confirmReconnect(clients[1]);
                    images[0].confirmReconnect(clients[0]);
                    rejoins++;
                }
                images[seat].refresh(clients[seat]);
                int active=Integer.parseInt(images[seat].image().rows.get("active"));
                if(active<0) break;
                if(seat!=active) {
                    seat=active;
                    images[seat].refresh(clients[seat]);
                }
                PublicDungeon v=new PublicDungeon(images[seat].image());
                boolean warp=v.source.rows.containsKey("warpdestination");
                if(!v.consent && (warp || n==0 && first!=null && first.action.equals("TRAVEL"))) {
                    int other=1-seat;
                    images[other].refresh(clients[other]);
                    SeatActions.Pending consent=actions[other].prepare(images[other].image(),"CONSENT",warp ? v.stair : first.cell,0);
                    long began=System.nanoTime();
                    assertTrue(consent.submit(clients[other]).endsWith(" APPLIED"));
                    record("CONSENT",began);
                    images[seat].refresh(clients[seat]);
                    v=new PublicDungeon(images[seat].image());
                }
                VaultPublicRoute.Input move=warp && v.consent ? new VaultPublicRoute.Input("TRAVEL",v.stair,0) : n==0 && first!=null?fromView(v,first):input(v,guides[seat]);
                SeatActions.Pending action=move.action.equals("BREW_SEEDS")
                        ? actions[seat].prepare(v.source,move.action,move.cell,move.item+"_"+move.item+"_"+move.item)
                        : actions[seat].prepare(v.source,move.action,move.cell,move.item);
                String competing = null;
                int peer = 1-seat;
                if (n==0 && stage!=null && stage.startsWith("paired-exotic")) {
                    images[peer].refresh(clients[peer]);
                    PublicDungeon other = new PublicDungeon(images[peer].image());
                    assertEquals(v.source.version,other.source.version);
                    assertEquals("6",v.source.rows.get("cauldron"+move.cell));
                    assertEquals("6",other.source.rows.get("cauldron"+move.cell));
                    VaultPublicRoute.Input selection=fromView(other,first);
                    assertThrows(IllegalStateException.class, () -> actions[peer].prepare(other.source,selection.action,selection.cell,selection.item));
                    String actor=other.source.rows.get("self").split(",",2)[0];
                    competing="DO_"+epoch+" "+(peer+1)+" "+peer+" "+actor+" "+actions[peer].allocate()
                            +" "+other.source.version+" "+other.source.rows.get("turn")+" "+selection.action+" "+selection.cell+" "+selection.item;
                }
                long start=System.nanoTime();
                String receipt=action.submit(clients[seat]);
                if(n==0 && first!=null) assertTrue(stage+" "+receipt,receipt.endsWith(" APPLIED"));
                if (competing!=null) {
                    String result=clients[peer].request(competing);
                    assertTrue(result,result.endsWith(" REJECT_VERSION") || result.startsWith("REJECT_VERSION"));
                    assertEquals(receipt,action.submit(clients[seat]));
                    images[peer].refresh(clients[peer]);
                    PublicDungeon after = new PublicDungeon(images[peer].image());
                    assertEquals("0",after.source.rows.get("cauldron"+move.cell));
                    assertTrue(fromView(after,first).item>0);
                    System.out.printf("PAID_RACE winner=%d initialEnergy=6 finalEnergy=0 preservedPeerScroll=true retry=%s%n",seat,result);
                }
                if(receipt.endsWith(" APPLIED") || receipt.endsWith(" PARTY_DEFEATED")) {
                    record(move.action,start);
                } else {
                    rejected++;
                    if(move.item!=0) guides[seat].rejectedItems.add(move.item);
                    if(move.action.equals("MOVE")) guides[seat].blocked.add(move.cell);
                    if(move.action.equals("OPEN")) guides[seat].rejectedChests.add(move.cell);
                    if(move.action.equals("REVIVE")) guides[seat].reviveDenied=true;
                }
                if(n%67==0 && receipt.endsWith(" APPLIED")) {
                    assertEquals(receipt,action.submit(clients[seat]));
                    duplicates++;
                }
            }
        } finally {
            for(LanClient client:clients) client.close();
        }
    }

    void room(File root,int seed,int budget,String stage) throws Exception {
        InetAddress loop=InetAddress.getLoopbackAddress();
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try(LanRoom room=LanRoom.persistentGame(loop,0,0,seed,new CoopSaves(root))) {
            InetSocketAddress address=new InetSocketAddress(loop,room.tcpPort());
            CompletableFuture<VaultPublicRoute.Input> ready=new CompletableFuture<>();
            Future<?> run=worker.submit(()-> {play(room,address,budget,stage,ready);return null;});
            while(!run.isDone()) {
                room.pump();
                if(stage!=null && !ready.isDone() && room.state()==LanRoom.State.IN_GAME) {
                    try {
                        VaultPublicRoute.Input first=arrange(stage);
                        room.save();
                        ready.complete(first);
                    } catch(Throwable error) {
                        ready.completeExceptionally(error);
                        throw error;
                    }
                }
                Thread.sleep(1);
            }
            run.get();
            long start=System.nanoTime();
            room.saveAndClose();
            saveNanos=System.nanoTime()-start;
            closed.add(new WeakReference<>(room));
        } finally {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(10,TimeUnit.SECONDS));
        }
    }

    static long usedHeap() {
        Runtime r=Runtime.getRuntime();
        return r.totalMemory()-r.freeMemory();
    }
    static long quantile(List<Long> values,double q) {
        return values.isEmpty()?0:values.get((int)Math.ceil(values.size()*q)-1)/1000;
    }
    void observe(File root,int index) throws Exception {
        long before=usedHeap();
        System.gc();
        Thread.sleep(100);
        Set<Thread> live=Thread.getAllStackTraces().keySet();
        long workers=live.stream().filter(t->t.getName().equals("spd-lan-io")).count();
        long descriptors,sockets;
        try(Stream<Path> fd=Files.list(Paths.get("/proc/self/fd"))) {
            List<Path> paths=new ArrayList<>();
            fd.forEach(paths::add);descriptors=paths.size();
            sockets=paths.stream().filter(f->{try{return Files.readSymbolicLink(f).toString().startsWith("socket:");}
                catch(java.io.IOException e){return false;}}).count();
        }
        long bytes;
        try(Stream<Path> disk=Files.walk(root.toPath())) {
            bytes=disk.filter(Files::isRegularFile).mapToLong(f->f.toFile().length()).sum();
        }
        List<Long> sorted=new ArrayList<>(latencies);
        Collections.sort(sorted);
        System.out.printf("ROOM %d applied=%d verbs=%s rejected=%d duplicates=%d rejoins=%d heap=%d postGC=%d actors=%d threads=%d lanWorkers=%d fd=%d sockets=%d retainedRooms=%d cumulativeSaveBytes=%d lastSaveUs=%d actionUs=%d/%d/%d%n",
            index,latencies.size(),verbs,rejected,duplicates,rejoins,before,usedHeap(),Actor.all().size(),live.size(),workers,descriptors,sockets,
            closed.stream().filter(ref->ref.get()!=null).count(),bytes,saveNanos/1000,quantile(sorted,.5),quantile(sorted,.95),quantile(sorted,.99));
    }
    public static void main(String[] args) throws Exception {
        ImpVaultTest fixture=new ImpVaultTest();
        fixture.temporary.create();fixture.startBackend();
        try {
            MixedRoomStability trial=new MixedRoomStability();
            if (Boolean.getBoolean("stability.newRosters")) {
                String[] classes={"WARRIOR","MAGE","HUNTRESS","ROGUE","DUELIST","CLERIC"};
                int count=0;
                for (int host=0;host<classes.length;host++) for (int guest=0;guest<classes.length;guest++) {
                    if (host<4 && guest<4) continue;
                    trial.normalRoster=new String[]{classes[host],classes[guest]};
                    trial.room(fixture.temporary.getRoot(),570+count%12,Integer.parseInt(args[1]),null);
                    trial.observe(fixture.temporary.getRoot(),++count);
                    System.out.println("ROSTER "+classes[host]+"/"+classes[guest]+" normal admission, starting shortcuts and guest reauthentication");
                }
                return;
            }
            for(int i=0;i<Integer.parseInt(args[0]);i++) {
                String stage=Boolean.getBoolean("stability.sealArts") ? SEAL_ARTS[i%SEAL_ARTS.length] : Boolean.getBoolean("stability.frostSenses") ? FROST_SENSES[i%FROST_SENSES.length] : Boolean.getBoolean("stability.wandArts") ? WAND_ARTS[i%WAND_ARTS.length] : Boolean.getBoolean("stability.growth")?GROWTH[i%GROWTH.length]:null;
                if (Boolean.getBoolean("stability.selectors")) stage = SELECTORS[i % SELECTORS.length];
                if (Boolean.getBoolean("stability.discoveries")) stage = DISCOVERIES[i % DISCOVERIES.length];
                if (Boolean.getBoolean("stability.tierGrowth")) stage = TIER_GROWTH[i % TIER_GROWTH.length];
                if (Boolean.getBoolean("stability.cacheGrowth")) stage = CACHE_GROWTH[i % CACHE_GROWTH.length];
                if (Boolean.getBoolean("stability.sharedGrowth")) stage = SHARED_GROWTH[i % SHARED_GROWTH.length];
                if (Boolean.getBoolean("stability.enchantment")) stage = ENCHANTMENT[i % ENCHANTMENT.length];
                if (Boolean.getBoolean("stability.originalItems")) stage = ORIGINAL_ITEMS[i % ORIGINAL_ITEMS.length];
                if (Boolean.getBoolean("stability.stasisWarp")) stage = STASIS_WARP[i % STASIS_WARP.length];
                if (Boolean.getBoolean("stability.paidRace")) stage = "paired-exotic"+(i%2);
                if (Boolean.getBoolean("stability.wandGarden")) stage = WAND_GARDEN[i % WAND_GARDEN.length];
                if (Boolean.getBoolean("stability.earthAlchemy")) stage = EARTH_ALCHEMY[i % EARTH_ALCHEMY.length];
                if (Boolean.getBoolean("stability.runeReading")) stage = RUNE_READING[i % RUNE_READING.length];
                if (Boolean.getBoolean("stability.runeEffects")) stage = RUNE_EFFECTS[i % RUNE_EFFECTS.length];
                if (Boolean.getBoolean("stability.seedAlchemy")) stage = SEED_ALCHEMY[i % SEED_ALCHEMY.length];
                if (Boolean.getBoolean("stability.cauldron")) stage = CAULDRON[i % CAULDRON.length];
                trial.room(fixture.temporary.getRoot(),"mine".equals(stage)?571:570+i%12,Integer.parseInt(args[1]),stage);
                trial.observe(fixture.temporary.getRoot(),i+1);
            }
            for (Map.Entry<String,List<Long>> entry:trial.latencyByVerb.entrySet()) {
                List<Long> samples=entry.getValue();
                Collections.sort(samples);
                System.out.printf("ACTION_US %s n=%d median=%d p95=%d p99=%d%n",entry.getKey(),samples.size(),
                        quantile(samples,.5),quantile(samples,.95),quantile(samples,.99));
            }
        } finally {fixture.stopBackend();fixture.temporary.delete();}
    }
}
