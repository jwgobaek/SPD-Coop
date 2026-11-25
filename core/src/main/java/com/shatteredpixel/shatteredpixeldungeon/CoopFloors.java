package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import java.util.HashSet;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;

/** One active floor; native generated-level keys distinguish equal depths across branches; only the active floor has registered actors. */
public final class CoopFloors {
    private final CoopSession game;
    public static final int LAST_SEWER = 5, FIRST_PRISON = 6, LAST_PLAYABLE = 26;
    static int lastDepth(int schema) {
        return schema>=14 ? LAST_PLAYABLE : schema==13 ? 24 : schema>=11 ? 20 : schema==10 ? 19 : schema==9 ? 15 : schema==8 ? 14 : schema==7 ? 11 : schema==6 ? 9 : schema==5 ? 7 : schema==4 ? FIRST_PRISON : schema==3 ? LAST_SEWER : schema;
    }
    private final java.util.Map<Integer,Bundle> dormant = new java.util.TreeMap<>();
    private long voteTurn;
    private int votedCell=-1;
    private final int[] positions=new int[2];

    CoopFloors(CoopSession game) { this.game=game; }

    public static boolean vaultDepth(int depth) { return depth>=17 && depth<=19; }
    private boolean vaultEntrance(LevelTransition gate) {
        if (Dungeon.branch!=0 || !vaultDepth(Dungeon.depth) || gate.type!=LevelTransition.Type.BRANCH_EXIT
                || gate.destDepth!=Dungeon.depth || gate.destBranch!=1 || Dungeon.levelHasBeenGenerated(Dungeon.depth,1)
                || !Imp.Quest.given() || Imp.Quest.isCompleted() || Imp.Quest.isOld()) return false;
        for (int seat=0;seat<2;seat++) {
            Hero hero=game.hero(seat);
            if (hero.buff(AscensionChallenge.class)!=null || hero.buff(LostInventory.class)!=null) return false;
        }
        return true;
    }

    int crystalHolder() {
        for (int i=0;i<2;i++) if (game.hero(i).belongings.getItem(EscapeCrystal.class)!=null) return i;
        return -1;
    }

    public int vaultScore() { return EscapeCrystal.vaultScore(game.hero(0),game.hero(1)); }
    static boolean mineDepth(int depth) { return depth >= 12 && depth <= 14; }
    static int floorKey(int depth, int branch) { return depth + 1000 * branch; }

    private Hero miner() {
        for (int seat = 0; seat < 2; seat++) {
            Hero hero = game.hero(seat);
            if (hero == null) continue;
            Pickaxe pick = hero.belongings.getItem(Pickaxe.class);
            if (pick != null && game.ownership.owner(pick) == game.player(seat)) return hero;
        }
        return null;
    }

    private boolean mineGate(LevelTransition gate) {
        if (!mineDepth(Dungeon.depth) || gate.destDepth != Dungeon.depth || !Blacksmith.Quest.given()
                || Blacksmith.Quest.completed() || miner() == null) return false;
        if (Dungeon.branch == 0) return gate.type == LevelTransition.Type.BRANCH_EXIT && gate.destBranch == 1
                && !Blacksmith.Quest.started() && !Dungeon.levelHasBeenGenerated(Dungeon.depth, 1);
        return Dungeon.branch == 1 && gate.type == LevelTransition.Type.BRANCH_ENTRANCE
                && gate.destBranch == 0 && Blacksmith.Quest.started();
    }

    public int stair() {
        Level l=Dungeon.level;
        if (l instanceof VaultLevel) {
            int holder=crystalHolder();
            if (holder<0 || Imp.Quest.isCompleted()) return -1;
            int cell=game.hero(holder).pos;
            for (int i=0;i<2;i++) if (game.life(i)!=CoopSession.Life.ALIVE || game.hero(i).paralysed>0
                    || !game.knowledge(game.hero(i)).visible(cell) || l.distance(game.hero(i).pos,cell)>1) return -1;
            return vaultScore()>=50 ? cell : -1;
        }
        if (l.locked || Dungeon.depth<1 || Dungeon.depth>LAST_PLAYABLE) return -1;
        for (LevelTransition t : l.transitions) {
            if ((mineGate(t) || vaultEntrance(t)) && inRange(l,t)) return t.cell();
            if (Dungeon.branch!=0 || (t.type!=LevelTransition.Type.REGULAR_EXIT && t.type!=LevelTransition.Type.REGULAR_ENTRANCE)
                    || t.destBranch!=0 || t.destDepth<1 || t.destDepth>LAST_PLAYABLE || Math.abs(t.destDepth-Dungeon.depth)!=1) continue;
            if (Dungeon.depth==LAST_SEWER && t.destDepth==FIRST_PRISON
                    && l.map[t.cell()]!=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.UNLOCKED_EXIT) continue;
            if (Dungeon.depth==10 && t.destDepth==11 && ((com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel)l).state()!=com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel.State.WON) continue;
            if (Dungeon.depth==15 && t.destDepth>15 && !((com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel)l).exitOpen()) continue;
            if (Dungeon.depth==20 && t.destDepth==21 && !((com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel)l).exitOpen()) continue;
            if (Dungeon.depth==25 && t.destDepth==26 && l.map[l.exit()]!=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EXIT) continue;
            // The native ascent curse begins at this stair. Keep it closed until the party ascent is implemented.
            if (Dungeon.depth==25 && t.destDepth==24 && com.shatteredpixel.shatteredpixeldungeon.Statistics.amuletObtained) continue;
            if (inRange(l,t)) return t.cell();
        }
        return -1;
    }

    private boolean inRange(Level l,LevelTransition t) {
        for(int i=0;i<2;i++) {
            Hero h=game.hero(i);
            if(h==null || game.life(i)!=CoopSession.Life.ALIVE || h.rooted || h.paralysed>0 ||
                    l.distance(h.pos,t.cell())>1 || !game.knowledge(h).visible(t.cell())) return false;
        }
        return true;
    }

    public static final class UnsupportedDestination extends RuntimeException {
        UnsupportedDestination(String mob) { super("Unsupported destination actor: "+mob); }
    }

    static void requireSupportedResidents(Level next) {
        java.util.SortedSet<String> blockers=new java.util.TreeSet<>();
        for (Mob mob:next.mobs) {
            if (!CoopSession.basicMob(mob)) {
                String detail=mob.getClass().getSimpleName();
                if (mob instanceof Statue) {
                    Weapon weapon=((Statue)mob).weapon();
                    detail+="/"+weapon.getClass().getSimpleName()+"/"+
                            (weapon.enchantment==null ? "plain" : weapon.enchantment.getClass().getSimpleName());
                }
                if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue) {
                    com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor armor =
                            ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue) mob).armor();
                    detail += "/" + armor.getClass().getSimpleName() + "/" +
                            (armor.glyph == null ? "plain" : armor.glyph.getClass().getSimpleName());
                }
                blockers.add(detail);
            }
        }
        if (!blockers.isEmpty()) throw new UnsupportedDestination(blockers.toString());
    }

    public boolean hasParkedFloor() { return !dormant.isEmpty(); }

    public void cancel() { votedCell=-1; }
    public boolean consent(int seat,long turn,int cell,boolean withdraw) {
        game.checkExecutor();
        if (game.activeSeat()<0 || game.activeSeat()==seat || turn!=game.turn() || cell<0 || cell!=stair()) return false;
        if (withdraw) {
            if (!agreed()) return false;
            cancel();
        } else {
            if (agreed()) return false;
            voteTurn=turn;
            votedCell=cell;
            for(int i=0;i<2;i++) positions[i]=game.hero(i).pos;
        }
        return true;
    }

    public boolean agreed() {
        return votedCell>=0 && voteTurn==game.turn() && votedCell==stair() &&
                game.hero(0).pos==positions[0] && game.hero(1).pos==positions[1];
    }

    boolean travel(int seat,int cell) { return travel(seat,cell,null); }

    boolean travel(int seat,int cell,Item chosen) {
        if (seat!=game.activeSeat() || cell!=votedCell || !agreed()) return false;
        Level old=Dungeon.level;
        LevelTransition transition;
        Item reward=null;
        boolean escaping=old instanceof VaultLevel;
        if (escaping) {
            int score=vaultScore();
            if (score<50 || (score>=500 && (chosen==null || !game.hero(seat).belongings.contains(chosen)
                    || game.ownership.owner(chosen)!=game.player(seat) || !EscapeCrystal.rewardAllowed(chosen,score)))
                    || (score<500 && chosen!=null)) return false;
            int holder=crystalHolder();
            EscapeCrystal crystal=game.hero(holder).belongings.getItem(EscapeCrystal.class);
            if (chosen!=null) {
                reward=chosen;
                if (!(chosen instanceof com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem))
                    reward=chosen.duplicate().quantity(1);
                reward=EscapeCrystal.prepareReward(reward);
                reward.detachAll(game.hero(seat).belongings.backpack);
                game.ownership.claim(reward,game.player(seat));
            }
            CoopVault.restore(game,crystal,holder);
            Imp.Quest.complete(score);
            transition=new LevelTransition(old,cell,LevelTransition.Type.BRANCH_ENTRANCE,Dungeon.depth,0,LevelTransition.Type.BRANCH_EXIT);
        } else {
            if (chosen!=null) return false;
            transition=old.getTransition(cell);
        }
        boolean branchChange=transition.destBranch!=Dungeon.branch;
        if (!escaping && vaultEntrance(transition)) {
            CoopVault.enter(game,seat);
        } else if (branchChange && !escaping) {
            if (!mineGate(transition)) return false;
            if (Dungeon.branch==0) Blacksmith.Quest.start();
            else Blacksmith.Quest.completeFor(miner(),game.hero(0),game.hero(1));
        }
        game.leaveFloor();
        Level.beforeTransition(game.hero(0));
        Level.beforeTransition(game.hero(1));
        Actor.leaveCoopFloor();
        Bundle leaving=new Bundle();
        leaving.put("branch",Dungeon.branch);
        leaving.put("depth",Dungeon.depth);
        leaving.put("level",old);
        leaving.put("items",game.itemState(CoopSession.floorItems(old)));
        for(int i=0;i<2;i++) leaving.put("sight"+i,game.knowledge(game.hero(i)).save());
        validateFloor(leaving, Dungeon.depth, Dungeon.branch);
        int from=floorKey(Dungeon.depth,Dungeon.branch);
        Dungeon.depth=transition.destDepth;
        Dungeon.branch=transition.destBranch;
        Bundle parked=dormant.remove(floorKey(Dungeon.depth,Dungeon.branch));
        Dungeon.level=null;
        Level next;
        if (parked!=null) {
            validateFloor(parked,Dungeon.depth,Dungeon.branch);
            next=game.unpackFloor(parked);
        } else {
            if (Dungeon.levelHasBeenGenerated(Dungeon.depth,Dungeon.branch)) throw new IllegalStateException("Missing parked floor");
            next=Dungeon.newLevel();
        }
        requireSupportedResidents(next);
        Dungeon.level=next;
        PathFinder.setMapSize(next.width(),next.height());
        if (parked!=null) {
            game.restoreItems(CoopSession.floorItems(next),parked.getBundle("items"),game.ownership.sequence(),new HashSet<>());
            for(int i=0;i<2;i++) game.knowledge(game.hero(i)).load(parked.getBundle("sight"+i));
        }
        LevelTransition arrival=next.getTransition(transition.destType);
        if (arrival==null || (old instanceof MiningLevel || next instanceof MiningLevel) && arrival.type!=transition.destType) throw new IllegalStateException("Missing original arrival gate");
        int gate=arrival.cell();
        int[] offsets=new int[9];
        System.arraycopy(PathFinder.NEIGHBOURS8,0,offsets,1,8);
        int placed=0;
        for(int offset:offsets) {
            int pos=gate+offset;
            if (!next.insideMap(pos) || !next.passable[pos] || next.pit[pos] || next.traps.get(pos)!=null ||
                    next.plants.get(pos)!=null || next.findMob(pos)!=null) continue;
            Hero h=game.hero(placed++);
            h.pos=pos;
            h.fieldOfView=new boolean[next.length()];
            h.viewDistance=next.viewDistance;
            h.ready=false;
            h.curAction=h.lastAction=null;
            if (placed==2) break;
        }
        if (placed!=2) throw new IllegalStateException("No safe arrival pair");
        if (reward!=null && !reward.collect(game.hero(seat).belongings.backpack)) {
            Imp.Quest.reward=reward;
        }
        Actor.add(game.hero(1));
        Actor.init();
        if (next.respawner()==null) next.addRespawner(); else Actor.add(next.respawner());
        for(Char ch:Actor.chars()) game.attachSprite(ch);
        game.restoreBeeHomes();
        dormant.put(from,leaving);
        cancel();
        return true;
    }

    static void checkLevelType(Bundle floor,int depth,int branch) {
        Class<?> expected=branch==1 ? (vaultDepth(depth) ? VaultLevel.class : MiningLevel.class) : depth==26 ? com.shatteredpixel.shatteredpixeldungeon.levels.LastLevel.class : depth==25 ? com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel.class : depth>=21 ? com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel.class : depth==20 ? com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel.class : depth>=16 ? com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel.class : depth==15 ? com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel.class : depth>=11 ? com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel.class : depth==10 ? com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel.class : depth>=FIRST_PRISON ? com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel.class
                : depth==5 ? com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel.class
                : com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel.class;
        if (!expected.getName().equals(floor.getString("__className")))
            throw new IllegalArgumentException("Unexpected level type at depth "+depth);
        validateSummons(floor);
        if (depth == 10) validateBoss(floor);
        if (depth == 15) validateCavesBoss(floor);
        if (depth == 20) validateCityBoss(floor);
        if (depth == 25) validateYog(floor);
    }

    private static void validateYog(Bundle floor) {
        String yog=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa.class.getName();
        String fist=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist.class.getName()+"$";
        int bosses=0, fists=0;
        int phase=-1;
        int cells=floor.getInt("width")*floor.getInt("height");
        for (Bundle mob:floor.getBundleArray("mobs")) {
            String type=mob.getString("__className");
            if (type.startsWith(fist)) fists++;
            if (!yog.equals(type)) continue;
            bosses++;
            phase=mob.getInt("phase");
            if (phase<0 || phase>5 || mob.getInt("HP")<=0 || mob.getInt("HP")>1000
                    || !Float.isFinite(mob.getFloat("ability_cd")) || !Float.isFinite(mob.getFloat("summon_cd")))
                throw new IllegalArgumentException("Invalid Yog state");
            int[] aims=mob.getIntArray("targeted_cells");
            if (aims.length>0 && mob.getInt("beam_target")<=0) throw new IllegalArgumentException("Missing ray target");
            for (int target:aims) if (target<0 || target>=cells) throw new IllegalArgumentException("Invalid ray cell");
            String[] regular=mob.getStringArray("regular_summons");
            if (regular.length!=4) throw new IllegalArgumentException("Invalid Yog summon rotation");
            for (String kind:regular) {
                String name=kind.replace("class ","");
                if (!name.equals(yog+"$Larva") && !name.equals(yog+"$YogRipper")) throw new IllegalArgumentException("Invalid Yog summon");
            }
            String[] remaining=mob.getStringArray("fist_summons");
            if (remaining.length!=Math.max(0,Math.min(3,4-phase))) throw new IllegalArgumentException("Invalid fist rotation");
            java.util.HashSet<String> pairs=new java.util.HashSet<>();
            for (String kind:remaining) {
                String name=kind.replace("class ","");
                if (!name.startsWith(fist)) throw new IllegalArgumentException("Invalid fist class");
                switch (name.substring(fist.length())) {
                    case "BurningFist":case "SoiledFist":name="earth";break;
                    case "RottingFist":case "RustedFist":name="decay";break;
                    case "BrightFist":case "DarkFist":name="light";break;
                    default:throw new IllegalArgumentException("Invalid Yog fist");
                }
                if (!pairs.add(name)) throw new IllegalArgumentException("Duplicate Yog fist pair");
            }
        }
        if (bosses!=(floor.getBoolean("locked")?1:0) || fists>1 || fists>0 && (phase<2 || phase>4))
            throw new IllegalArgumentException("Inconsistent Yog arena");
    }

    private static void validateCityBoss(Bundle floor) {
        int[] terrain=floor.getIntArray("map");
        int lower=com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel.arenaDoor();
        int upper=com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel.exitDoor();
        int lockedDoor=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_DOOR;
        boolean sealed=floor.getBoolean("locked");
        if (terrain.length!=15*48 || sealed!=(terrain[lower]==lockedDoor) || sealed && terrain[upper]!=lockedDoor)
            throw new IllegalArgumentException("Invalid city arena boundary");
        String kingType=com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing.class.getName();
        int kings=0;
        for (Bundle mob:floor.getBundleArray("mobs")) {
            if (!kingType.equals(mob.getString("__className"))) continue;
            kings++;
            int phase=mob.getInt("phase");
            if (phase<1 || phase>3 || mob.getInt("HP")<=0 || mob.getInt("summons_made")<0)
                throw new IllegalArgumentException("Invalid Dwarf King phase");
            for (Bundle buff:mob.getBundleArray("buffs")) {
                if (!(kingType+"$Summoning").equals(buff.getString("__className"))) continue;
                int pos=buff.getInt("pos");
                if (buff.getInt("delay")<1 || pos<0 || pos>=terrain.length)
                    throw new IllegalArgumentException("Invalid city summon");
            }
        }
        if (kings!=(sealed ? 1 : 0)) throw new IllegalArgumentException("City seal and king disagree");
    }

    private static void validateCavesBoss(Bundle floor) {
        int bosses=0, pylons=0, active=0, charges=0;
        boolean charged=false;
        java.util.Set<Integer> positions=new HashSet<>();
        for (Bundle mob:floor.getBundleArray("mobs")) {
            String type=mob.getString("__className");
            if (type.equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300.class.getName())) {
                bosses++;
                charges=mob.getInt("pylons_activated");
                charged=mob.getBoolean("supercharged");
                if (charges<0 || charges>2 || mob.getInt("HP")<=0) throw new IllegalArgumentException("Invalid DM-300");
            } else if (type.equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon.class.getName())) {
                pylons++;
                int pos=mob.getInt("pos");
                if (!positions.add(pos) || java.util.Arrays.stream(com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel.pylonPositions).noneMatch(p->p==pos)
                        || mob.getInt("target_neighbour")<0 || mob.getInt("target_neighbour")>=8)
                    throw new IllegalArgumentException("Invalid pylon");
                String alignment=mob.getString("alignment");
                if ("ENEMY".equals(alignment)) active++;
                else if (!"NEUTRAL".equals(alignment)) throw new IllegalArgumentException("Invalid pylon allegiance");
            }
        }
        int[] map=floor.getIntArray("map");
        com.watabou.utils.Rect gate=com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel.gate;
        int entry=gate.top*floor.getInt("width")+gate.left;
        if (entry<0 || entry>=map.length) throw new IllegalArgumentException("Invalid cave gate");
        boolean open=map[entry]==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY;
        boolean locked=floor.getBoolean("locked");
        if (bosses!=(locked?1:0) || (open && locked) || active!=(charged?1:0)
                || pylons!=(locked ? 4-charges+active : open ? 2 : 4))
            throw new IllegalArgumentException("Invalid DM-300 phase or pylons");
        if (floor.contains("pending_gas")) for(Bundle gas:floor.getBundleArray("pending_gas")) {
            if (!com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300.PendingGas.class.getName().equals(gas.getString("__className"))
                    || gas.getInt("caster")<=0 || gas.getInt("multiplier")!=1 || gas.getInt("origin")<=0 || gas.getInt("origin")>=map.length
                    || gas.getInt("impact")<=0 || gas.getInt("impact")>=map.length || gas.getIntArray("cells").length==0)
                throw new IllegalArgumentException("Invalid pending gas");
            for (int c:gas.getIntArray("cells")) if (c<0 || c>=map.length) throw new IllegalArgumentException("Invalid gas path");
        }
    }

    private static void validateBoss(Bundle floor) {
        String state = floor.getString("state");
        boolean fighting = "FIGHT_START".equals(state) || "FIGHT_ARENA".equals(state);
        boolean won = "WON".equals(state);
        if (!won && !fighting && !"START".equals(state) && !"FIGHT_PAUSE".equals(state))
            throw new IllegalArgumentException("Invalid Tengu stage");
        Bundle boss = floor.getBundle("tengu");
        String type = com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu.class.getName();
        if (floor.getBoolean("locked") != (!won && !"START".equals(state))
                || won != boss.isNull() || (!won && (!type.equals(boss.getString("__className")) || boss.getInt("HP") <= 0)))
            throw new IllegalArgumentException("Invalid Tengu record or seal");
        int count = 0;
        for (Bundle mob : floor.getBundleArray("mobs")) if (type.equals(mob.getString("__className"))) {
            if (!fighting || mob.getInt("id") != boss.getInt("id") || mob.getInt("HP") != boss.getInt("HP"))
                throw new IllegalArgumentException("Inconsistent Tengu entry");
            count++;
        }
        if (count != (fighting ? 1 : 0)) throw new IllegalArgumentException("Tengu resident count");
    }

    private static void inspectOffFloorBoss(Bundle floor, HashSet<Integer> actors, int nextId) {
        String state = floor.getString("state");
        if ("START".equals(state) || "FIGHT_PAUSE".equals(state)) {
            Bundle boss = floor.getBundle("tengu");
            inspectActor(boss, actors, nextId);
            for (Bundle buff : boss.getBundleArray("buffs")) inspectActor(buff, actors, nextId);
        }
    }

    private static void validateSummons(Bundle floor) {
        Bundle[] residents = floor.getBundleArray("mobs");
        java.util.Map<Integer,String> kinds = new java.util.HashMap<>();
        for (Bundle resident : residents) kinds.put(resident.getInt("id"), resident.getString("__className"));
        HashSet<Integer> claimed = new HashSet<>();
        String skeleton = com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer.NecroSkeleton.class.getName();
        String wraith = com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith.class.getName();
        for (Bundle resident : residents) {
            if (resident.contains("my_skeleton")) {
                int id = resident.getInt("my_skeleton");
                if (!skeleton.equals(kinds.get(id)) || !claimed.add(id))
                    throw new IllegalArgumentException("Invalid summoned skeleton on this floor");
            }
            if (resident.contains("wraith_ids")) for (int id : resident.getIntArray("wraith_ids")) {
                if (!wraith.equals(kinds.get(id)) || !claimed.add(id))
                    throw new IllegalArgumentException("Invalid summoned wraith on this floor");
            }
            if (resident.getBoolean("summoning")) {
                String type=resident.getString("__className");
                if (!type.equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer.class.getName())
                        && !type.equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SpectralNecromancer.class.getName())) continue;
                int pos=resident.getInt("summoning_pos"), width=floor.getInt("width"), height=floor.getInt("height");
                if (!resident.contains("summoning_pos") || width<3 || height<3 || pos<width || pos>=width*(height-1)
                        || pos%width==0 || pos%width==width-1)
                    throw new IllegalArgumentException("Invalid summon cell");
            }
        }
    }

    private static void validateFloor(Bundle record,int depth,int branch) {
        Bundle floor=record.getBundle("level");
        checkLevelType(floor,depth,branch);
        int w=floor.getInt("width"),h=floor.getInt("height");
        if(record.getInt("branch")!=branch || record.getInt("depth")!=depth || w<3 || h<3 || w>256 || h>256 ||
                floor.getIntArray("map").length!=w*h || record.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1500000)
            throw new IllegalArgumentException("Invalid parked floor");
        for(int i=0;i<2;i++) new FloorKnowledge(w*h).load(record.getBundle("sight"+i));
    }

    private static void inspectActor(Bundle raw,HashSet<Integer> ids,int nextId) {
        int id=raw.getInt("id");
        float due=raw.getFloat("time");
        if(id<1 || id>=nextId || !ids.add(id) || !(due>=0 && due<=Float.MAX_VALUE))
            throw new IllegalArgumentException("Parked actor identity or clock");
    }

    private static void inspectItem(Bundle item,Bundle entry) {
        if(entry.isNull() || !item.getString("__className").equals(entry.getString("type")) ||
                item.getInt("quantity")<=0 || item.getInt("quantity")!=entry.getInt("quantity"))
            throw new IllegalArgumentException("Parked physical item mismatch");
    }

    int groundAmulets() {
        int count=0;
        for (com.shatteredpixel.shatteredpixeldungeon.items.Heap heap:Dungeon.level.heaps.valueList()) {
            for (Item item:heap.items) if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Amulet) {
                if (Dungeon.depth!=26 || Dungeon.branch!=0 || item.quantity()!=1)
                    throw new IllegalArgumentException("Unexpected amulet heap");
                count++;
            }
        }
        String type=com.shatteredpixel.shatteredpixeldungeon.items.Amulet.class.getName();
        for (Bundle floor:dormant.values()) {
            for (Bundle entry:floor.getBundle("level").getBundleArray("heaps"))
                for (Bundle item:entry.getBundleArray("items")) if (type.equals(item.getString("__className"))) {
                    if (floor.getInt("depth")!=26 || floor.getInt("branch")!=0 || item.getInt("quantity")!=1)
                        throw new IllegalArgumentException("Unexpected stored amulet");
                    count++;
                }
        }
        return count;
    }

    int schema() {
        if (Dungeon.levelHasBeenGenerated(25,0)) return 14;
        if (Dungeon.levelHasBeenGenerated(21,0)) return 13;
        for (int d=17;d<=19;d++) if (Dungeon.levelHasBeenGenerated(d,1)) return 12;
        if (Dungeon.levelHasBeenGenerated(20,0)) return 11;
        if (Dungeon.levelHasBeenGenerated(16,0)) return 10;
        for (int d=12;d<=14;d++) if (Dungeon.levelHasBeenGenerated(d,1)) return 10;
        if (Dungeon.levelHasBeenGenerated(15,0)) return 9;
        if (Dungeon.levelHasBeenGenerated(12,0)) return 8;
        if (Dungeon.levelHasBeenGenerated(10,0)) return 7;
        if (Dungeon.levelHasBeenGenerated(8,0)) return 6;
        if (Dungeon.levelHasBeenGenerated(7,0)) return 5;
        if (Dungeon.levelHasBeenGenerated(FIRST_PRISON,0)) return 4;
        return Dungeon.depth>2 || Dungeon.levelHasBeenGenerated(3,0) ? 3 : 2;
    }

    void save(Bundle state) {
        if (dormant.isEmpty()) return;
        if (schema()==2) state.put("parked",dormant.values().iterator().next());
        else {
            Bundle records=new Bundle();
            for (java.util.Map.Entry<Integer,Bundle> entry:dormant.entrySet())
                records.put(String.valueOf(entry.getKey()),entry.getValue());
            state.put("dormant",records);
        }
    }

    void restore(Bundle state,HashSet<Long> itemIds) {
        dormant.clear();
        cancel();
        int schema=state.getInt("schema"), bound=lastDepth(schema);
        boolean multi=schema>=3;
        HashSet<Integer> generated=new HashSet<>(Dungeon.generatedLevels);
        int active=floorKey(Dungeon.depth,Dungeon.branch);
        int mineCount=0, vaultCount=0;
        for (int key:generated) {
            if (key>=1 && key<=bound) continue;
            if (schema>=10 && key/1000==1 && mineDepth(key%1000) && generated.contains(key%1000)) mineCount++;
            else if (schema>=12 && key/1000==1 && vaultDepth(key%1000) && generated.contains(key%1000)) vaultCount++;
            else throw new IllegalArgumentException("Unknown generated floor "+key);
        }
        if (mineCount!=0 && (!Blacksmith.Quest.given() || !Blacksmith.Quest.started()
                || (Dungeon.branch==1 && mineDepth(Dungeon.depth))==Blacksmith.Quest.completed()))
            throw new IllegalArgumentException("Mine and quest history disagree");
        boolean insideVault=Dungeon.branch==1 && vaultDepth(Dungeon.depth);
        if (vaultCount>1 || (schema==12 && vaultCount==0) || vaultCount>0 && (!Imp.Quest.given() || Imp.Quest.isOld()
                || insideVault==Imp.Quest.isCompleted())) throw new IllegalArgumentException("Vault and quest history disagree");
        int mainCount=generated.size()-mineCount-vaultCount;
        int required = schema==8 ? 12 : schema==7 ? 10 : schema==6 ? 8 : bound;
        if (!generated.contains(active) || generated.size()!=Dungeon.generatedLevels.size() || mineCount>1
                || (schema==14 && !generated.contains(25))
                || (schema==13 && !generated.contains(21))
                || (schema==11 && !generated.contains(20))
                || (schema==10 && mineCount==0 && !generated.contains(16)) || (multi && !generated.contains(3))
                || (schema>=4 && schema<10 && !generated.contains(required)))
            throw new IllegalArgumentException("Invalid generation history");
        for(int i=1;i<=mainCount;i++) if (!generated.contains(i)) throw new IllegalArgumentException("Disconnected dungeon history");
        Bundle records=state.getBundle("dormant");
        if ((multi && state.contains("parked")) || (!multi && state.contains("dormant")))
            throw new IllegalArgumentException("Mixed floor formats");
        HashSet<Integer> actors=new HashSet<>();
        for (Actor actor:Actor.all()) if (!actors.add(actor.id())) throw new IllegalArgumentException("Duplicate live Actor ID");
        for (int i=0;i<2;i++) {
            Hero hero=game.hero(i);
            actors.add(hero.id());
            for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:hero.buffs()) actors.add(buff.id());
        }
        int nextId=state.getBundle("world").getInt("nextid");
        inspectDownedGhouls(state.getBundle("floor"),actors,nextId,true);
        if (Dungeon.depth==10) inspectOffFloorBoss(state.getBundle("floor"),actors,nextId);
        for (int key:generated) {
            boolean present=multi ? records.contains(String.valueOf(key)) : key==3-Dungeon.depth && state.contains("parked");
            if ((key!=active)!=present) throw new IllegalArgumentException("Missing or unexpected parked floor "+key);
            if (!present) continue;
            Bundle record=multi ? records.getBundle(String.valueOf(key)) : state.getBundle("parked");
            validateFloor(record,key%1000,key/1000);
            inspectRecord(record,actors,nextId,itemIds);
            dormant.put(key,record);
        }
        if (multi && records.getKeys().size()!=dormant.size()) throw new IllegalArgumentException("Unknown dormant depth");
    }

    private static void inspectDownedGhouls(Bundle floor,HashSet<Integer> actors,int nextId,boolean active) {
        for (Bundle resident:floor.getBundleArray("mobs")) {
            for (Bundle buff:resident.getBundleArray("buffs")) {
                if (!buff.getString("__className").equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul.GhoulLifeLink.class.getName())) continue;
                Bundle body=buff.getBundle("ghoul");
                if ((!com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul.class.getName().equals(body.getString("__className")) && !com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultGhoul.class.getName().equals(body.getString("__className")) && !com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing.DKGhoul.class.getName().equals(body.getString("__className")))
                        || body.getInt("pos")<0 || body.getInt("pos")>=floor.getInt("width")*floor.getInt("height"))
                    throw new IllegalArgumentException("Invalid linked ghoul");
                inspectActor(body,actors,nextId);
                for(Bundle held:body.getBundleArray("buffs")) {
                    if (!active) inspectActor(held,actors,nextId);
                    else if (!actors.contains(held.getInt("id"))) throw new IllegalArgumentException("Missing linked body buff");
                }
            }
        }
    }

    private void inspectRecord(Bundle parked,HashSet<Integer> actors,int nextId,HashSet<Long> itemIds) {
        Bundle floor=parked.getBundle("level");
        for(Bundle mob:floor.getBundleArray("mobs")) {
            inspectActor(mob,actors,nextId);
            for(Bundle buff:mob.getBundleArray("buffs")) inspectActor(buff,actors,nextId);
        }
        inspectDownedGhouls(floor,actors,nextId,false);
        if (floor.contains("pending_gas")) for (Bundle gas:floor.getBundleArray("pending_gas")) inspectActor(gas,actors,nextId);
        for(Bundle blob:floor.getBundleArray("blobs")) inspectActor(blob,actors,nextId);
        if(floor.contains("respawner")) inspectActor(floor.getBundle("respawner"),actors,nextId);
        if(actors.size()>16384) throw new IllegalArgumentException("Sewer actor count");
        Bundle entries=parked.getBundle("items");
        int count=0;
        if (parked.getInt("depth")==10) {
            inspectOffFloorBoss(floor,actors,nextId);
            for (Bundle item:floor.getBundleArray("storeditems")) inspectItem(item,entries.getBundle("encounter"+count++));
        }
        for(Bundle heap:floor.getBundleArray("heaps")) {
            int pos=heap.getInt("pos"), index=0;
            if(pos<0 || pos>=floor.getInt("width")*floor.getInt("height")) throw new IllegalArgumentException("Parked heap cell");
            for(Bundle item:heap.getBundleArray("items")) {
                inspectItem(item,entries.getBundle("f"+pos+"_"+index++));
                count++;
            }
        }
        for(Bundle mob:floor.getBundleArray("mobs")) {
            if (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror.class.getName().equals(mob.getString("__className")) && mob.contains("reward")) {
                inspectItem(mob.getBundle("reward"),entries.getBundle("mirror"+mob.getInt("id")));
                count++;
            }
            for (Bundle buff:mob.getBundleArray("buffs")) {
                if (!buff.getString("__className").equals(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion.class.getName())) continue;
                int index=0;
                for (Bundle item:buff.getBundleArray("items")) {
                    Bundle entry=entries.getBundle("pin"+mob.getInt("id")+"_"+index++);
                    inspectItem(item,entry);
                    count++;
                }
            }
            if (mob.getString("__className").equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.class.getName())) {
                int index=0;
                for (Bundle item:mob.getBundleArray("buyback_items")) {
                    Bundle entry=entries.getBundle("shop"+mob.getInt("id")+"_"+index++);
                    if (index>com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.MAX_BUYBACK_HISTORY || entry.getLong("owner")==0)
                        throw new IllegalArgumentException("Buyback history");
                    inspectItem(item,entry);
                    count++;
                }
            }
            boolean armored=mob.getString("__className").equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue.class.getName());
            if (armored) {
                inspectItem(mob.getBundle("armor"),entries.getBundle("arm"+mob.getInt("id")));
                count++;
            }
            if (armored || mob.getString("__className").equals(Statue.class.getName())) {
                inspectItem(mob.getBundle("weapon"),entries.getBundle("w"+mob.getInt("id")));
                count++;
            }
            if (mob.getString("__className").equals(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Thief.class.getName()) && !mob.getBundle("item").isNull()) {
                inspectItem(mob.getBundle("item"),entries.getBundle("th"+mob.getInt("id")));
                count++;
            }
            if (!mob.contains("items")) continue;
            int index=0;
            for(Bundle item:mob.getBundleArray("items")) {
                inspectItem(item,entries.getBundle("m"+mob.getInt("id")+"_"+index++));
                count++;
            }
        }
        if(count!=entries.getKeys().size()) throw new IllegalArgumentException("Parked inventory size");
        if(entries.getKeys().size()>8192) throw new IllegalArgumentException("Parked item count");
        for(String key:entries.getKeys()) {
            Bundle e=entries.getBundle(key);
            long id=e.getLong("id"),owner=e.getLong("owner");
            if(id<1 || id>game.ownership.sequence() || !itemIds.add(id) ||
                    (owner!=0 && owner!=game.player(0) && owner!=game.player(1)))
                throw new IllegalArgumentException("Invalid parked item identity");
        }
    }
}
