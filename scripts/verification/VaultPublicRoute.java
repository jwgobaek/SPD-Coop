package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
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
        int lastAlly=-1;
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
        private int retreat(PublicDungeon v) {
            if (!team || v.hp*3>v.maxHp*2 || dangerDistance(v,v.pos)>2) return -1;
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
                if (gap<=previous) continue;
                int value=10*gap-4*(lastAlly<0?0:distance(lastAlly,c,v.width))-visits.getOrDefault(c,0);
                if (value>score) {score=value;best=c;}
            }
            return best;
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
            if (v.ally>=0) lastAlly=v.ally;
            if (!reviveDenied && v.ally>=0 && adjacent(v.pos,v.ally,v.width) && "DOWNED".equals(v.source.rows.get("allylife"))) {
                return new Input("REVIVE",v.ally,0);
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
            PublicDungeon.Loot feet = v.floor.get(v.pos);
            if (feet!=null && !(feet.sprite==com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.DEWDROP && v.hp==v.maxHp) && !rejectedItems.contains(feet.id) && !v.chest(v.pos)) return new Input("PICK_UP",v.pos,feet.id);
            for (Map.Entry<Integer,String> monster : new TreeMap<>(v.monsters).entrySet()) {
                int cell=monster.getKey();
                if (!adjacent(v.pos,cell,v.width)) continue;
                String offer=v.source.rows.get("vaultnpc"+cell);
                if ("take".equals(offer) || "open".equals(offer)) return new Input("VAULT_INTERACT",cell,0);
            }
            int retreatCell=retreat(v);
            if (retreatCell>=0) return new Input("MOVE",retreatCell,0);
            for (Map.Entry<Integer,String> monster : new TreeMap<>(v.monsters).entrySet()) {
                String kind=monster.getValue();
                if (!hostile(kind)) continue;
                int cell=monster.getKey();
                if (adjacent(v.pos,cell,v.width) && !v.source.rows.containsKey("aim"+v.pos)) return new Input("ATTACK",cell,0);
                for (PublicDungeon.Loot item : v.belongings) {
                    for (String verb : Arrays.asList("ZAP","SHOOT","THROW")) {
                        if (v.actions(item).contains(verb) && !rejectedItems.contains(item.id) && !blockedShots.contains(shotKey(item.id,v.pos,cell))) return new Input(verb,cell,item.id);
                    }
                }
            }
            if (investigateWalls(v)) return new Input("SEARCH",v.pos,0);
            int n=v.width*v.height;
            int[] first=new int[n], dist=new int[n], queue=new int[n];
            Arrays.fill(first,-1); queue[0]=v.pos; first[v.pos]=v.pos;
            int best=-1, high=Integer.MIN_VALUE;
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
                    for (int neighbor : new int[]{c-1,c+1,c-v.width,c+v.width}) {
                        if (neighbor>=0 && neighbor<n && v.terrain(neighbor)<0) value+=8;
                    }
                    if (v.branch==0 && tile==Terrain.EXIT) value+=220;
                    if (v.ally>=0) value-=2*Math.max(Math.abs(c%v.width-v.ally%v.width), Math.abs(c/v.width-v.ally/v.width));
                    if (team && lastAlly>=0) value-=8*Math.max(0,distance(c,lastAlly,v.width)-3);
                    PublicDungeon.Loot item=v.floor.get(c);
                    if (item!=null && !rejectedItems.contains(item.id)) value+=150;
                    for (Map.Entry<Integer,String> m:v.monsters.entrySet()) {
                        if (adjacent(c,m.getKey(),v.width) && ("VaultMirror".equals(m.getValue()) || "VaultTokenDoor".equals(m.getValue()))
                                && !adjacent(v.pos,m.getKey(),v.width)) value+=35;
                    }
                    if (value>high) {best=first[c]; high=value;}
                }
            }
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
        try (CoopSession game=ImpVaultTest.openAtVaultEntrance(seed)) {
            UUID session=UUID.randomUUID();
            RoomSync room=new RoomSync(session,game);
            DisplayReplica[] replica={new DisplayReplica(session,0),new DisplayReplica(session,1)};
            Guide[] guide={new Guide(),new Guide()};
            long[] ids={0,0};
            int applied=0;
            for (int attempts=0;attempts<budget && !game.defeated() && !game.stopped();attempts++) {
                int seat=game.activeSeat();
                PublicDungeon v=view(room,replica[seat],seat);
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
