package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ImpVaultTest;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import java.util.*;

/** Bounded vault entrance trial. The decision function receives only one player's public image and memory. */
public final class VaultPublicRoute {
    static final class Input {
        final String action;
        final int cell;
        final long item;
        Input(String action, int cell, long item) { this.action=action; this.cell=cell; this.item=item; }
    }

    private static boolean adjacent(int from, int to, int width) {
        return Math.abs(from%width-to%width)<=1 && Math.abs(from/width-to/width)<=1;
    }

    static final class Guide {
        final Map<Integer,Integer> visits = new HashMap<>();
        final Set<Long> rejectedItems = new HashSet<>();
        final Set<Integer> blocked = new HashSet<>();
        final Set<Integer> rejectedChests = new HashSet<>();
        boolean reviveDenied;
        final boolean team=Boolean.getBoolean("vault.guide.team");
        final String regroup = System.getProperty("vault.guide.regroup","off");
        int rendezvous = -1;
        int allyDoor = -1;
        int previousPos = -1, recentlyExited = -1, regroupSteps;
        int walkedDoor=-1, crossingDoor=-1, crossingTurns;
        int lastDoorSide = -1, doorEntry = -1, holdDirection;
        int recoveryWaits, coveredWaits;
        int reachableFrontier, knownExit=-1;
        final boolean campaignGuide=Boolean.getBoolean("trial.campaignGuide");
        int exitWaits;
        private boolean escortCover() { return regroup.equals("escortfire"); }
        private boolean doorCover() { return escortCover() || regroup.equals("doorcover"); }
        private boolean coveredByAlly(PublicDungeon v,int to) {
            if (v.ally < 0 || behindCover(v,v.ally,to)) return false;
            for (int shooter:recentShooters.keySet()) {
                if (v.monsters.containsKey(shooter) && !behindCover(v,v.ally,shooter) && behindCover(v,shooter,to)) return true;
            }
            return false;
        }
        private boolean useSideCover() { return regroup.equals("sidecover") || regroup.equals("holdcover") || regroup.equals("nextcover") || regroup.equals("shelterexit") || doorCover(); }
        boolean doorwayStrategy() { return useSideCover() || regroup.equals("recoverexit") || regroup.equals("coverexit") || regroup.equals("crossonce") || regroup.equals("doorway") || regroup.equals("followdoor") || regroup.equals("exitcover") || regroup.equals("trail") || regroup.equals("safeexit") || regroup.equals("handoff"); }
        private boolean visibleDoor(PublicDungeon v, int cell) {
            if (cell<0 || !v.visible(cell)) return false;
            int tile=v.terrain(cell);
            return tile==Terrain.DOOR || tile==Terrain.OPEN_DOOR;
        }
        // Only this seat's earlier public sightings are retained, for at most six of its decisions.
        final Map<Integer,Integer> recentShooters = new HashMap<>();
        private void rememberShooters(PublicDungeon view) {
            recentShooters.replaceAll((cell, left) -> left-1);
            recentShooters.entrySet().removeIf(entry -> entry.getValue() <= 0 || view.visible(entry.getKey()));
            for (Map.Entry<Integer,String> seen : view.monsters.entrySet()) {
                if (seen.getValue().equals("VaultDM100") || seen.getValue().equals("VaultShaman"))
                    recentShooters.put(seen.getKey(),6);
            }
        }

        private int rememberedExposure(PublicDungeon view, int cell) {
            int count = 0;
            for (int shooter : recentShooters.keySet()) if (!behindCover(view,shooter,cell)) count++;
            return count;
        }

        int missingTurns;
        boolean escortFollower;
        int reunions;
        int lastAlly=-1;
        int destination=-1;
        final Set<String> blockedShots=new HashSet<>();

        private int distance(int a,int b,int width) {
            return Math.max(Math.abs(a%width-b%width),Math.abs(a/width-b/width));
        }
        private String shotKey(long item,int from,int to) {
            return item+"/"+from+"/"+to;
        }
        private boolean hostile(String kind) {
            return !(kind.equals("VaultLaser") || kind.equals("VaultSentry") || kind.equals("VaultMirror") ||
                    kind.equals("VaultTokenDoor") || kind.equals("Ghost") || kind.equals("Shopkeeper") || kind.equals("Sheep"));
        }
        private int dangerDistance(PublicDungeon v,int cell) {
            int closest=v.width+v.height;
            for (Map.Entry<Integer,String> m:v.monsters.entrySet()) {
                if (hostile(m.getValue())) closest=Math.min(closest,distance(m.getKey(),cell,v.width));
            }
            return closest;
        }
        // Only retained public tiles can establish cover. An unknown tile never counts as a wall.
        private boolean behindCover(PublicDungeon v,int shooter,int cell) {
            int x=shooter%v.width,y=shooter/v.width,tx=cell%v.width,ty=cell/v.width;
            int dx=Math.abs(tx-x),dy=Math.abs(ty-y),error=dx-dy;
            while (x!=tx || y!=ty) {
                int twice=2*error;
                if (twice > -dy) { error-=dy; x+=Integer.signum(tx-x); }
                if (twice < dx) { error+=dx; y+=Integer.signum(ty-y); }
                if (x==tx && y==ty) return false;
                int tile=v.terrain(y*v.width+x);
                if (tile>=0 && (Terrain.flags[tile]&Terrain.SOLID)!=0) return true;
            }
            return false;
        }
        private int exposedTo(PublicDungeon v,int cell) {
            int count=0;
            for (Map.Entry<Integer,String> enemy:v.monsters.entrySet())
                if ((enemy.getValue().equals("VaultDM100") || enemy.getValue().equals("VaultShaman"))
                        && !behindCover(v,enemy.getKey(),cell)) count++;
            return count;
        }
        private boolean coverOnSecondMove(PublicDungeon view, int first) {
            for (int dy=-1;dy<=1;dy++) for (int dx=-1;dx<=1;dx++) {
                int x=first%view.width+dx, y=first/view.width+dy;
                if (x<0 || y<0 || x>=view.width || y>=view.height) continue;
                int cell=y*view.width+x;
                if (cell==first || cell==view.pos || cell==view.ally || !view.visible(cell)) continue;
                int tile=view.terrain(cell);
                if (tile<0 || (Terrain.flags[tile]&Terrain.PASSABLE)==0 || blocked.contains(cell)
                        || view.monsters.containsKey(cell) || view.source.rows.containsKey("aim"+cell)
                        || view.source.rows.containsKey("trap"+cell)) continue;
                if (rememberedExposure(view,cell)==0 && dangerDistance(view,cell)>1) return true;
            }
            return false;
        }
        private int retreat(PublicDungeon v) {
            if (!team) return -1;
            int exposed=exposedTo(v,v.pos);
            if (exposed==0 && (v.hp*3>v.maxHp*2 || dangerDistance(v,v.pos)>2)) return -1;
            int best=-1,score=Integer.MIN_VALUE;
            int previous=dangerDistance(v,v.pos);
            for (int dy=-1;dy<=1;dy++) for (int dx=-1;dx<=1;dx++) {
                int x=v.pos%v.width+dx,y=v.pos/v.width+dy;
                if (x<0 || x>=v.width || y<0 || y>=v.height) continue;
                int c=y*v.width+x, tile=v.terrain(c);
                if (!v.visible(c) || c==v.ally || blocked.contains(c) || v.monsters.containsKey(c)
                        || v.source.rows.containsKey("aim"+c) || v.source.rows.containsKey("trap"+c)
                        || tile<0 || (Terrain.flags[tile]&Terrain.PASSABLE)==0) continue;
                int gap=dangerDistance(v,c);
                int cover=exposed-exposedTo(v,c);
                boolean withdrawFromShooter = regroup.equals("holdcover") && exposed>0 && !escortFollower
                        && v.hp*2<v.maxHp && v.ally>=0 && distance(c,v.ally,v.width)<distance(v.pos,v.ally,v.width);
                boolean coveredNext = (regroup.equals("nextcover") || regroup.equals("shelterexit") || doorCover()) && exposed>0 && gap>1 && coverOnSecondMove(v,c);
                if (cover<0 || (cover==0 && !coveredNext && (gap<=previous || previous>2 && !withdrawFromShooter))) continue;
                int value=80*cover+(coveredNext ? 30 : 0)+10*gap-4*(lastAlly<0?0:distance(lastAlly,c,v.width))-visits.getOrDefault(c,0);
                // When equally safe, guard the injured follower's route past a known threshold.
                // Greater safety still wins immediately when the entry is the only cover.
                if (useSideCover() && escortFollower && holdDirection > 0 && v.hp*3 < v.maxHp
                        && (c == doorEntry || c == lastDoorSide)) value -= 7;
                if (doorCover() && recentShooters.size()>0 && v.ally>=0) {
                    // Step towards the ally when this can be done behind known cover.
                    value -= 18 * Math.max(0, distance(c,v.ally,v.width)-1);
                }
                if (escortCover() && coveredByAlly(v,c)) value+=90;
                if (value>score) {score=value;best=c;}
            }
            return best;
        }
        // Post-decision diagnostic uses exactly the same public snapshot as the acting seat.
        private void auditNeighbours(PublicDungeon view, int step, int seat) {
            int col = view.pos % view.width, row = view.pos / view.width;
            for (int y = Math.max(0, row-1); y <= Math.min(view.height-1, row+1); y++) {
                for (int x = Math.max(0, col-1); x <= Math.min(view.width-1, col+1); x++) {
                    int cell = x + y * view.width;
                    if (cell == view.pos || !view.visible(cell)) continue;
                    int terrain = view.terrain(cell);
                    boolean passable = terrain >= 0 && (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
                    System.out.printf("PUBLIC_NEIGHBOUR\t%d\t%d\t%d\t%d\t%d\t%d\t%b\t%b\t%d\t%d\t%b\t%b%n",
                            step, seat, view.pos, cell, view.ally, terrain, passable,
                            blocked.contains(cell), exposedTo(view, cell), dangerDistance(view, cell),
                            view.source.rows.containsKey("aim"+cell), view.source.rows.containsKey("trap"+cell));
                    if (view.ally>=0) System.out.printf("COVER_AUDIT\t%d\t%d\t%d\t%d\tally=%d\tsupport=%s\tvisibleRanged=%s%n",
                            step,seat,view.pos,cell,view.ally,coveredByAlly(view,cell),new TreeSet<>(recentShooters.keySet()));
                }
            }
        }
        final Set<Integer> investigated = new HashSet<>();
        private boolean investigateWalls(PublicDungeon v) {
            if (v.branch!=0 || !v.monsters.isEmpty()) return false;
            List<Integer> walls=new ArrayList<>();
            for (int dy=-1;dy<=1;dy++) for (int dx=-1;dx<=1;dx++) {
                int x=v.pos%v.width+dx,y=v.pos/v.width+dy;
                if (x<0 || x>=v.width || y<0 || y>=v.height) continue;
                int c=y*v.width+x,t=v.terrain(c);
                if (v.visible(c) && (t==Terrain.WALL || t==Terrain.WALL_DECO) && !investigated.contains(c)) walls.add(c);
            }
            investigated.addAll(walls);
            return !walls.isEmpty();
        }
        Input choose(PublicDungeon v) {
            visits.merge(v.pos, 1, Integer::sum);
            if (v.ally>=0) {
                if (missingTurns>0) reunions++;
                missingTurns=0;
                lastAlly=v.ally;
                if (adjacent(v.pos,v.ally,v.width) && exposedTo(v,v.pos)==0 && exposedTo(v,v.ally)==0)
                    rendezvous=Math.min(v.pos,v.ally);
            } else missingTurns++;
            boolean returning = rendezvous>=0 && !regroup.equals("off") && (missingTurns>0
                    || regroup.equals("cover") && v.ally>=0 && distance(v.pos,v.ally,v.width)>3);
            int meetingCell = v.ally>=0 ? v.ally : rendezvous;
            boolean escort=regroup.equals("escort") || doorwayStrategy();
            if (escort && v.ally>=0)
                returning=escortFollower && distance(v.pos,v.ally,v.width)>2;
            // A player remembers their own last step onto a threshold, even if the ally disappears.
            boolean restUnderCover = team && regroup.equals("recoverexit");
            boolean avoidFire = team && (regroup.equals("coverexit") || useSideCover() || restUnderCover);
            if (avoidFire) rememberShooters(v);
            boolean crossing = team && (regroup.equals("crossonce") || avoidFire);
            if (crossing) {
                if (holdDirection > 0) holdDirection--;
                if (previousPos >= 0 && v.pos != previousPos && visibleDoor(v, v.pos)) {
                    lastDoorSide = previousPos;
                    doorEntry = v.pos;
                    holdDirection = 5;
                }
                previousPos = v.pos;
                if (escortFollower && lastAlly >= 0 && (v.ally >= 0 || missingTurns <= 6)) {
                    meetingCell = lastAlly;
                    returning = v.ally < 0 || distance(v.pos, lastAlly, v.width) > 1;
                }
            }
            if (!reviveDenied && v.ally>=0 && adjacent(v.pos,v.ally,v.width) && "DOWNED".equals(v.source.rows.get("allylife"))) {
                return new Input("REVIVE",v.ally,0);
            }
            // Remember a threatened exit only after this seat has already crossed another observed door.
            if (team && escortFollower && (regroup.equals("handoff") || regroup.equals("safeexit"))) {
                if (visibleDoor(v,v.pos)) walkedDoor=v.pos;
                if (v.hp*3<v.maxHp && !v.monsters.isEmpty() && walkedDoor>=0 && v.ally!=walkedDoor
                        && visibleDoor(v,v.ally) && adjacent(v.pos,v.ally,v.width)) {
                    crossingDoor=v.ally;
                    crossingTurns=4;
                }
                if (crossingTurns>0) {
                    crossingTurns--;
                    if (v.pos!=crossingDoor && v.ally!=crossingDoor && visibleDoor(v,crossingDoor)
                            && adjacent(v.pos,crossingDoor,v.width) && !blocked.contains(crossingDoor)
                            && !v.monsters.containsKey(crossingDoor) && !v.source.rows.containsKey("aim"+crossingDoor)
                            && !v.source.rows.containsKey("trap"+crossingDoor))
                        return new Input("MOVE",crossingDoor,0);
                    if (v.pos==crossingDoor && v.ally>=0 && !visibleDoor(v,v.ally)) {
                        returning=true;
                        meetingCell=v.ally;
                    }
                }
            }
            boolean trail=team && regroup.equals("trail");
            if (trail && previousPos!=v.pos) {
                if (visibleDoor(v,previousPos) && !visibleDoor(v,v.pos)) {
                    recentlyExited=previousPos;
                    regroupSteps=2;
                }
                previousPos=v.pos;
            }
            if (trail && !escortFollower && regroupSteps>0 && recentlyExited>=0 && v.ally>=0
                    && distance(v.pos,v.ally,v.width)>1 && v.monsters.isEmpty() && exposedTo(v,v.pos)==0) {
                regroupSteps--;
                return new Input("WAIT",0,0);
            }
            if (trail && escortFollower && v.ally<0 && lastAlly>=0 && missingTurns<=6) {
                returning=true;
                meetingCell=lastAlly;
            }
            boolean protectExit = (regroup.equals("exitcover") || regroup.equals("safeexit") || crossing || trail) && escortFollower && v.hp*3<v.maxHp;
            if ((regroup.equals("followdoor") || protectExit) && escortFollower && team) {
                // Follow a door actually seen under the ally. Give the ally a chance to clear it.
                if (visibleDoor(v,v.ally) && v.hp*3<v.maxHp) allyDoor=v.ally;
                if (allyDoor==v.pos || allyDoor>=0 && (visits.getOrDefault(allyDoor,0)>2
                        || v.monsters.containsKey(allyDoor) || v.source.rows.containsKey("aim"+allyDoor))) allyDoor=-1;
                if (visibleDoor(v,allyDoor) && adjacent(v.pos,allyDoor,v.width)
                        && !blocked.contains(allyDoor) && !v.source.rows.containsKey("trap"+allyDoor)
                        && exposedTo(v,allyDoor)<=exposedTo(v,v.pos)) {
                    if (allyDoor!=v.ally) return new Input("MOVE",allyDoor,0);
                    if (dangerDistance(v,v.pos)>1) return new Input("WAIT",0,0);
                }
            }
            for (int c : new TreeSet<>(v.floor.keySet())) {
                if (v.chest(c) && c!=v.pos && adjacent(v.pos,c,v.width) && !rejectedChests.contains(c)) return new Input("OPEN",c,0);
            }
            boolean hungry = v.source.rows.entrySet().stream().anyMatch(row -> row.getKey().matches("b[0-9]+")
                    && (row.getValue().startsWith(BuffIndicator.HUNGER + ",") || row.getValue().startsWith(BuffIndicator.STARVATION + ",")));
            for (PublicDungeon.Loot i : v.belongings) {
                if (hungry && v.monsters.isEmpty() && v.actions(i).contains("EAT")) return new Input("EAT",0,i.id);
                if (v.hp * 3 < v.maxHp && !v.monsters.isEmpty() && !rejectedItems.contains(i.id) && v.actions(i).contains("VAULT_RECALL")) return new Input("VAULT_RECALL",0,i.id);
                if (v.hp * 2 < v.maxHp && v.actions(i).contains("DRINK")) return new Input("DRINK", 0, i.id);
                if (!rejectedItems.contains(i.id) && v.actions(i).contains("EQUIP")
                        && (i.kind.equals("MELEE") || i.kind.equals("ARMOR"))
                        && v.belongings.stream().noneMatch(e -> e.slot.equals(i.kind.equals("MELEE")?"WEAPON":"ARMOR"))) return new Input("EQUIP",0,i.id);
            }
            boolean exposedParty = team && (regroup.equals("shelterexit") || doorCover()) && !recentShooters.isEmpty()
                    && (v.hp * 3 < 2 * v.maxHp || v.ally >= 0 && v.hp < v.maxHp);
            if (escortCover() && escortFollower && v.ally>=0 && v.hp*2>=v.maxHp) {
                for (int shooter : new TreeSet<>(recentShooters.keySet())) {
                    if (!v.visible(shooter) || !v.monsters.containsKey(shooter) || behindCover(v,v.pos,shooter)
                            || behindCover(v,v.ally,shooter)) continue;
                    for (PublicDungeon.Loot item:v.belongings) {
                        if (v.actions(item).contains("THROW") && item.kind.equals("DART")
                                && !rejectedItems.contains(item.id) && !blockedShots.contains(shotKey(item.id,v.pos,shooter)))
                            return new Input("THROW",shooter,item.id);
                    }
                }
            }
            if (exposedParty && exposedTo(v,v.pos)>0) {
                int cover = retreat(v);
                if (cover >= 0) return new Input("MOVE",cover,0);
            }
            if (restUnderCover) {
                if (!v.monsters.isEmpty()) recoveryWaits = 0;
                if (!hungry && v.monsters.isEmpty() && recentShooters.isEmpty() && v.ally >= 0
                        && distance(v.pos,v.ally,v.width) <= 2 && v.hp * 4 < v.maxHp * 3
                        && !visibleDoor(v,v.pos) && !v.source.rows.containsKey("aim"+v.pos) && recoveryWaits < 32) {
                    recoveryWaits++;
                    return new Input("WAIT",0,0);
                }
            }
            PublicDungeon.Loot feet = v.floor.get(v.pos);
            if (crossing && !v.monsters.isEmpty() && (v.hp*2 < v.maxHp || returning)) feet = null;
            if (feet!=null && !(feet.sprite==com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.DEWDROP && v.hp==v.maxHp) && !rejectedItems.contains(feet.id) && !v.chest(v.pos)) return new Input("PICK_UP",v.pos,feet.id);
            for (Map.Entry<Integer,String> monster : new TreeMap<>(v.monsters).entrySet()) {
                int cell=monster.getKey();
                if (!adjacent(v.pos,cell,v.width)) continue;
                String offer=v.source.rows.get("vaultnpc"+cell);
                if ("take".equals(offer) || "open".equals(offer)) return new Input("VAULT_INTERACT",cell,0);
            }
            // An injured leader can hold a covered cell while the visible follower catches up.
            // Walking from cover into a remembered shooter's line merely to gain a step is unsafe.
            if (regroup.equals("holdcover") && !escortFollower && v.hp*2 < v.maxHp
                    && !recentShooters.isEmpty() && rememberedExposure(v,v.pos)==0
                    && !v.source.rows.containsKey("aim"+v.pos) && dangerDistance(v,v.pos)>1
                    && v.ally>=0 && distance(v.pos,v.ally,v.width)>1 && coveredWaits<3) {
                coveredWaits++;
                return new Input("WAIT",v.pos,0);
            }
            coveredWaits=0;
            int retreatCell=retreat(v);
            if (retreatCell>=0) return new Input("MOVE",retreatCell,0);
            for (Map.Entry<Integer,String> monster : new TreeMap<>(v.monsters).entrySet()) {
                String kind=monster.getValue();
                if (!hostile(kind)) continue;
                int cell=monster.getKey();
                if (adjacent(v.pos,cell,v.width) && !v.source.rows.containsKey("aim"+v.pos)) return new Input("ATTACK",cell,0);
                for (PublicDungeon.Loot item : v.belongings) {
                    for (String verb : Arrays.asList("ZAP","SHOOT","THROW")) {
                        if (campaignGuide && verb.equals("THROW") && v.actions(item).contains("EAT")) continue;
                        if (v.actions(item).contains(verb) && !rejectedItems.contains(item.id) && !blockedShots.contains(shotKey(item.id,v.pos,cell))) return new Input(verb,cell,item.id);
                    }
                }
            }
            if (escort && escortFollower && v.ally>=0 && !returning && v.monsters.isEmpty() && v.floor.isEmpty())
                return new Input("WAIT",0,0);
            if (returning && v.pos==meetingCell && v.monsters.isEmpty()) return new Input("WAIT",0,0);
            if (investigateWalls(v)) return new Input("SEARCH",v.pos,0);
            boolean avoidBacktrack = crossing && holdDirection > 0 && !returning && exposedTo(v, v.pos) == 0;
            if (campaignGuide && v.branch==0 && v.terrain(v.pos)==Terrain.EXIT && exitWaits<3) {
                exitWaits++;
                return new Input("WAIT",v.pos,0);
            }
            if (v.terrain(v.pos)!=Terrain.EXIT) exitWaits=0;
            int n=v.width*v.height;
            int[] first=new int[n], dist=new int[n], queue=new int[n];
            Arrays.fill(first,-1); queue[0]=v.pos; first[v.pos]=v.pos;
            int best=-1, bestCell=-1, high=Integer.MIN_VALUE;
            reachableFrontier=0;
            for (int head=0,tail=1;head<tail;head++) {
                int at=queue[head++];
                for (int dy=-1;dy<=1;dy++) for (int dx=-1;dx<=1;dx++) {
                    int x=at%v.width+dx, y=at/v.width+dy;
                    if (x<0 || x>=v.width || y<0 || y>=v.height) continue;
                    int c=y*v.width+x;
                    if (first[c]>=0 || blocked.contains(c) || v.ally==c || v.monsters.containsKey(c)) continue;
                    int tile=v.terrain(c);
                    if (tile<0 || (Terrain.flags[tile]&Terrain.PASSABLE)==0 || v.source.rows.containsKey("aim"+c)
                            || v.source.rows.containsKey("trap"+c)) continue;
                    if (at==v.pos && !v.visible(c)) continue;
                    dist[c]=dist[at]+1; first[c]=at==v.pos?c:first[at]; queue[tail++]=c;
                    int value=-3*dist[c] - 20*visits.getOrDefault(c,0) - 10*visits.getOrDefault(first[c],0);
                    boolean frontier=false;
                    if (tile==Terrain.EXIT) knownExit=c;
                    for (int neighbor : new int[]{c-1,c+1,c-v.width,c+v.width}) {
                        if (neighbor>=0 && neighbor<n && v.terrain(neighbor)<0) { value+=8; frontier=true; }
                    }
                    if (frontier) {
                        reachableFrontier++;
                        if (Boolean.getBoolean("trial.frontierGuide") && v.branch==0)
                            value += 80 / (1+visits.getOrDefault(c,0));
                    }
                    if (exposedParty && rememberedExposure(v,first[c])>0) value-=170;
                    if (exposedParty && v.ally>=0 && first[c]==v.ally) continue;
                    if (v.branch==0 && tile==Terrain.EXIT) value+=campaignGuide ? 600 : 220;
                    if (campaignGuide && v.branch==0 && v.ally>=0 && v.terrain(v.ally)==Terrain.EXIT
                            && adjacent(c,v.ally,v.width)) value+=600;
                    if (v.ally>=0) value-=2*Math.max(Math.abs(c%v.width-v.ally%v.width), Math.abs(c/v.width-v.ally/v.width));
                    if (team && lastAlly>=0) value-=8*Math.max(0,distance(c,lastAlly,v.width)-3);
                    PublicDungeon.Loot item=v.floor.get(c);
                    if (!protectExit && !exposedParty && item!=null && !rejectedItems.contains(item.id) && !(item.sprite==
                            com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.DEWDROP && v.hp==v.maxHp)) {
                        value+=150;
                        if (team && v.visible(c) && (item.kind.equals("MELEE") || item.sprite==
                                com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.DART)) value+=90;
                    }
                    for (Map.Entry<Integer,String> m:v.monsters.entrySet()) {
                        if (adjacent(c,m.getKey(),v.width) && ("VaultMirror".equals(m.getValue()) || "VaultTokenDoor".equals(m.getValue()))
                                && !adjacent(v.pos,m.getKey(),v.width)) value+=35;
                    }
                    // Retain an observed objective between adjacent moves even as the ally takes a turn.
                    if (doorwayStrategy() && v.ally>=0 && escortFollower && v.visible(c) &&
                            (tile==Terrain.DOOR || tile==Terrain.OPEN_DOOR) && adjacent(c,v.ally,v.width))
                        value+=30/(1+visits.getOrDefault(c,0));
                    if (escortCover() && coveredByAlly(v,first[c])) value+=50;
                    if (team && c==destination && !v.monsters.containsKey(c)) value+=20;
                    if (doorCover() && !returning && v.ally>=0
                            && (tile==Terrain.DOOR || tile==Terrain.OPEN_DOOR) && c!=doorEntry
                            && !visits.containsKey(c) && rememberedExposure(v,first[c])==0) {
                        // An already observed, untried exit takes precedence over loot.
                        // The companion still needs a free, known and safe approach.
                        if (distance(first[c],v.ally,v.width)<=3) value+=360;
                    }
                    if (returning) value=-100*distance(c,meetingCell,v.width)-dist[c]-20*exposedTo(v,c);
                    if (protectExit && lastAlly>=0 && (v.ally>=0 || missingTurns<=6))
                        value-=35*Math.max(0,distance(c,lastAlly,v.width)-1);
                    if (trail && !escortFollower && c==recentlyExited && v.pos!=recentlyExited && v.monsters.isEmpty())
                        value-=75;
                    if (avoidBacktrack) {
                        int backward = v.pos == doorEntry ? lastDoorSide : doorEntry;
                        if (first[c] == backward) value -= 160;
                    }
                    if (avoidFire && !recentShooters.isEmpty() && v.hp*3 < v.maxHp*2) {
                        value -= 140*rememberedExposure(v,first[c]) + 100*rememberedExposure(v,c);
                    }
                    // Two equally good regroup paths sometimes differ only in the first step.
                    // Use the recently crossed doorway last if the other first step is just as safe.
                    boolean forwardTie = value == high && useSideCover() && escortFollower
                            && holdDirection > 0 && best == doorEntry && first[c] != doorEntry
                            && rememberedExposure(v,first[c]) <= rememberedExposure(v,best);
                    if (value>high || forwardTie) {best=first[c]; bestCell=c; high=value;}
                }
            }
            destination=bestCell;
            return best<0 ? new Input("WAIT",v.pos,0) : new Input("MOVE",best,0);
        }
    }

    private static PublicDungeon view(RoomSync room, DisplayReplica replica, int seat) throws java.io.IOException {
        replica.refresh(cmd -> {
            String[] f=cmd.split(" ");
            return f[0].equals("SYNC") ? room.sync(seat,Long.parseLong(f[1]),f[2])
                    : room.part(seat,Long.parseLong(f[1]),Integer.parseInt(f[2]));
        });
        return new PublicDungeon(replica.image());
    }

    private static String send(CoopSession game, RoomSync room, PublicDungeon v, int seat, long id, Input input) {
        String[] action={"","","","",Long.toString(game.player(seat)),Integer.toString(seat),
                v.source.rows.get("self").split(",")[0],Long.toString(id),Long.toString(v.source.version),
                v.source.rows.get("turn"),input.action,Integer.toString(input.cell),Long.toString(input.item)};
        String result=room.action(seat,action);
        return result.substring(result.lastIndexOf(' ')+1);
    }

    private static void run(int seed, int budget) throws Exception {
        String resume = System.getProperty("vault.guide.resume");
        CoopSession initial;
        if (resume == null) {
            initial = ImpVaultTest.openAtVaultEntrance(seed);
        } else {
            try (java.io.InputStream input = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(resume))) {
                initial = CoopSession.restore(com.watabou.utils.Bundle.read(input));
            }
        }
        try (CoopSession game = initial) {
            if (resume != null && (Dungeon.seed != seed || Dungeon.branch != 1
                    || !(Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel))) {
                throw new IllegalArgumentException("Checkpoint does not match the requested vault seed");
            }
            UUID session=UUID.randomUUID();
            RoomSync room=new RoomSync(session,game);
            DisplayReplica[] replica={new DisplayReplica(session,0),new DisplayReplica(session,1)};
            Guide[] guide={new Guide(),new Guide()};
            guide[1].escortFollower=true;
            long[] ids={0,0};
            int applied=0;
            for (int attempts=0;attempts<budget && !game.defeated() && !game.stopped();attempts++) {
                int seat=game.activeSeat();
                PublicDungeon v=view(room,replica[seat],seat);
                String savePath=System.getProperty("vault.guide.checkpoint");
                if (savePath!=null && v.hp>0 && v.ally>=0 && v.monsters.isEmpty()) {
                    PublicDungeon partner=view(room,replica[1-seat],1-seat);
                    if (partner.hp>0 && partner.monsters.isEmpty()) {
                        // Record an observer-only save; neither guide receives the partner image.
                        java.nio.file.Path file=java.nio.file.Paths.get(savePath);
                        try (java.io.OutputStream out=java.nio.file.Files.newOutputStream(file)) {
                            com.watabou.utils.Bundle.write(game.checkpoint(),out);
                        }
                        System.out.printf("SAFE_STATE\t%d\t%d\t%s\t%s%n",applied,seat,v.source.checksum,partner.source.checksum);
                    }
                }
                int escapeThreshold=Integer.getInteger("vault.guide.escapeAt",0);
                String score=v.source.rows.get("vaultscore");
                if (escapeThreshold>0 && score!=null && Integer.parseInt(score)>=escapeThreshold && Integer.parseInt(score)<500) {
                    int cell=Integer.parseInt(v.source.rows.get("stair").split(",")[0]);
                    int ally=1-seat;
                    PublicDungeon other=view(room,replica[ally],ally);
                    if (!score.equals(other.source.rows.get("vaultscore"))) throw new AssertionError("disagreeing offers");
                    String consent=send(game,room,other,ally,++ids[ally],new Input("CONSENT",cell,0));
                    String travel=send(game,room,view(room,replica[seat],seat),seat,++ids[seat],new Input("TRAVEL",cell,0));
                    if (!consent.equals("APPLIED") || !travel.equals("APPLIED")) throw new AssertionError(consent+"/"+travel);
                    PublicDungeon city=view(room,replica[seat],seat);
                    if (city.branch!=0 || city.depth!=v.depth || !com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.isCompleted())
                        throw new AssertionError("escape did not complete");
                    System.out.printf("PUBLIC_ESCAPE\t%d\t%d\t%s\t%d\t%d\t%d\t%d%n",seed,applied,score,city.branch,city.depth,game.hero(0).HP,game.hero(1).HP);
                    return;
                }
                Input i=guide[seat].choose(v);
                if (v.pos == Integer.getInteger("vault.guide.auditCell", -1)) {
                    guide[seat].auditNeighbours(v, applied+1, seat);
                }
                if (applied==123 || applied==124) System.out.printf("SIGHT\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s%n",
                        applied,seat,v.pos,v.ally,guide[seat].lastAlly,guide[seat].rendezvous,v.hp,v.source.checksum);

                String outcome=send(game,room,v,seat,++ids[seat],i);
                if (outcome.equals("REJECT_INVALID") && i.action.equals("PICK_UP")) {
                    // Diagnostics only, after the public decision. Never identify a potion, scroll or ring here.
                    com.shatteredpixel.shatteredpixeldungeon.items.Item item =
                            com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.heaps.get(v.pos).peek();
                    String publicKind = item instanceof com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion
                            || item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll
                            || item instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring ? "unidentified" : item.getClass().getSimpleName();
                    System.out.printf("AUDIT_PICKUP\t%d\t%s%n",v.pos,publicKind);
                }
                if (i.action.equals("MOVE") && outcome.equals("APPLIED")) {
                    // Observer-only traversal evidence. The guide never receives room identities or layouts.
                    com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel level =
                            (com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel)com.shatteredpixel.shatteredpixeldungeon.Dungeon.level;
                    for (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room ring:level.rooms()) {
                        if (!(ring instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultRingRoom)) continue;
                        boolean wasInside=ring.inside(level.cellToPoint(v.pos));
                        boolean nowInside=ring.inside(level.cellToPoint(game.hero(seat).pos));
                        if (wasInside!=nowInside) System.out.printf("RING_CROSS\t%d\t%d\t%d,%d\t%s\t%d\t%d\t%d/%d%n",
                                applied+1,seat,ring.left,ring.top,nowInside?"ENTER":"EXIT",v.pos,game.hero(seat).pos,game.hero(0).HP,game.hero(1).HP);
                        for (com.watabou.utils.Point door:ring.connected.values()) {
                            int cell=level.pointToCell(door), pos=game.hero(seat).pos;
                            if (pos==cell || v.pos==cell) {
                                System.out.printf("RING_DOOR_MOVE\t%d\t%d\t%d,%d\t%d\t%d\t%d\t%d/%d%n",
                                        applied+1,seat,ring.left,ring.top,cell,v.pos,pos,game.hero(0).HP,game.hero(1).HP);
                            }
                        }
                    }
                }
                if (outcome.equals("APPLIED") || outcome.equals("PARTY_DEFEATED")) applied++;
                else if (i.action.equals("OPEN")) guide[seat].rejectedChests.add(i.cell);
                else if (i.action.equals("REVIVE")) guide[seat].reviveDenied=true;
                else if (guide[seat].team && (i.action.equals("THROW") || i.action.equals("ZAP") || i.action.equals("SHOOT")))
                    guide[seat].blockedShots.add(guide[seat].shotKey(i.item,v.pos,i.cell));
                else if (i.item!=0) guide[seat].rejectedItems.add(i.item);
                else if (i.action.equals("MOVE")) guide[seat].blocked.add(i.cell);
                else throw new AssertionError(outcome);
                ArrayList<String> bag=new ArrayList<>();
                for (PublicDungeon.Loot it:v.belongings) bag.add(it.slot+"/"+it.kind+"/"+it.sprite+"x"+it.quantity);
                Map<Integer,String> floor=new TreeMap<>();
                v.floor.forEach((c,it)->floor.put(c,it.kind+"/"+it.sprite));
                System.out.printf("INPUT\t%d\t%d\t%d\t%d\t%d\t%s\t%s\t%s\t%s\t%d\t%s%n",applied,seat,v.pos,v.hp,v.maxHp,bag,floor,new TreeMap<>(v.monsters),i.action,i.cell,outcome);
            }
            String finalCheckpoint = System.getProperty("vault.guide.endCheckpoint");
            if (finalCheckpoint != null) {
                // Engine-only research checkpoint. Never overwrite the input or another branch.
                try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(
                        java.nio.file.Paths.get(finalCheckpoint), java.nio.file.StandardOpenOption.CREATE_NEW)) {
                    com.watabou.utils.Bundle.write(game.checkpoint(), out);
                }
            }
            System.out.printf("OBSERVED_REUNIONS\t%d\t%d%n",guide[0].reunions,guide[1].reunions);
            System.out.printf("PUBLIC_ROUTE\t%d\t%d\t%d\t%d\t%d\t%s\t%s%n",seed,applied,game.stairs.vaultScore(),
                    game.hero(0).HP,game.hero(1).HP,game.defeated(),game.stopReason());
        }
    }
    public static void main(String[] args) throws Exception {
        ImpVaultTest test=new ImpVaultTest();
        test.temporary.create();test.startBackend();
        try { run(Integer.parseInt(args[0]),Integer.parseInt(args[1])); }
        finally { test.stopBackend();test.temporary.delete(); }
    }
}
