package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ImpStatue;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import java.util.Arrays;
import java.util.EnumMap;

/** Starts at the vault entrance. Navigation reads recipient terrain memory and visible entities. */
public final class VaultNaturalRoute extends HeadlessDungeon {
    static boolean apply(CoopSession g,CoopSession.Action a,int c,Item item,EnumMap<CoopSession.Action,Integer> counts) {
        if (!CoopFloorsTest.step(g,a,c,item)) return false;
        counts.merge(a,1,Integer::sum);
        return true;
    }
    static int next(CoopSession g,int[] walked,boolean[] blocked) {
        int seat=g.activeSeat();
        Hero h=g.hero(seat);FloorKnowledge sight=g.knowledge(h);
        int n=sight.length(),best=-1,high=Integer.MIN_VALUE;
        int[] first=new int[n],distance=new int[n],queue=new int[n];
        Arrays.fill(first,-1);first[h.pos]=h.pos;queue[0]=h.pos;
        for (int head=0,tail=1;head<tail;head++) {
            int at=queue[head++];
            for(int off:PathFinder.NEIGHBOURS8) {
                int cell=at+off;
                if(cell<0 || cell>=n || blocked[cell] || first[cell]>=0 || !(sight.visited(cell)||sight.mapped(cell))) continue;
                int tile=sight.terrain(cell);
                if(tile<0 || (Terrain.flags[tile]&Terrain.PASSABLE)==0) continue;
                boolean visible=sight.visible(cell);
                Char person=visible?Actor.findChar(cell):null;
                if(person!=null) continue;
                distance[cell]=distance[at]+1;first[cell]=at==h.pos?cell:first[at];queue[tail++]=cell;
                Heap stock=visible?Dungeon.level.heaps.get(cell):null;
                int interest=stock!=null && !stock.hidden && stock.type==Heap.Type.HEAP ? 200 : 0;
                int rank=interest-distance[cell]-Math.min(20,walked[cell])*25;
                if(rank>high) {best=first[cell];high=rank;}
            }
        }
        return best;
    }
    public static void main(String[] args) throws Exception {
        VaultNaturalRoute probe=new VaultNaturalRoute();
        probe.temporary.create();probe.startBackend();
        try(CoopSession game=CavesInteriorTest.open(572)) {
            ImpVaultTest.entrance(game);
            for(int seat=0;seat<2;seat++) {
                Hero h=game.hero(seat);h.HTBoost=0;h.updateHT(false);
            }
            CoopFloorsTest.cross(game);
            int[][] walked=new int[2][Dungeon.level.length()];
            boolean[][] blocked=new boolean[2][Dungeon.level.length()];
            EnumMap<CoopSession.Action,Integer> counts=new EnumMap<>(CoopSession.Action.class);
            int i=0;
            for(;i<420 && !game.stopped() && !game.defeated();i++) {
                int s=game.activeSeat();Hero h=game.hero(s);
                if(h.belongings.getItem(ImpStatue.class)!=null) break;
                walked[s][h.pos]++;
                boolean used=false;
                for(int off:PathFinder.NEIGHBOURS8) {
                    int c=h.pos+off;
                    if(!game.knowledge(h).visible(c)) continue;
                    Char other=Actor.findChar(c);
                    if(other instanceof VaultTokenDoor) used=apply(game,CoopSession.Action.VAULT_INTERACT,c,null,counts);
                    else if(other instanceof Mob && other.alignment==Char.Alignment.ENEMY && other.invisible==0)
                        used=apply(game,CoopSession.Action.ATTACK,c,null,counts);
                    if(used) break;
                }
                if(used) continue;
                Heap heap=Dungeon.level.heaps.get(h.pos);
                if(heap!=null && !heap.hidden && heap.type==Heap.Type.HEAP) {
                    if(apply(game,CoopSession.Action.PICK_UP,h.pos,heap.peek(),counts)) continue;
                    blocked[s][h.pos]=true;
                }
                for(Item item:h.belongings.backpack.items) {
                    if(h.belongings.weapon()==null && item instanceof MeleeWeapon || h.belongings.armor()==null && item instanceof Armor)
                        if(apply(game,CoopSession.Action.EQUIP,h.pos,item,counts)) {used=true;break;}
                }
                if(used) continue;
                int c=next(game,walked[s],blocked[s]);
                if(c>=0 && apply(game,CoopSession.Action.MOVE,c,null,counts)) continue;
                if(c>=0) blocked[s][c]=true;
                if(!apply(game,CoopSession.Action.WAIT,h.pos,null,counts)) break;
            }
            System.out.printf("VAULT_ENTRY\t%d\t%d\t%d\t%d\t%s\t%s\t%s%n",i,game.stairs.vaultScore(),
                    game.hero(0).HP,game.hero(1).HP,game.defeated(),game.stopReason(),counts);
        } finally {probe.stopBackend();probe.temporary.delete();}
    }
}
