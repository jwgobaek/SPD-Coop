package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.watabou.utils.PathFinder;
import java.util.*;

/** Route diagnostic. The guide may inspect terrain for routing; all progression goes through ordinary engine inputs. */
public class SewerJourney extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        SewerJourney run=new SewerJourney(); run.temporary.create();
        run.startBackend();
        try {for(String seed:args) run.travel(Long.parseLong(seed));}
        finally {run.stopBackend();run.temporary.delete();}
    }
    private final Map<String,Integer> counts=new TreeMap<>();
    private boolean act(CoopSession game,CoopSession.Action verb,int cell,Item item) {
        if (!CoopFloorsTest.step(game,verb,cell,item)) return false;
        counts.merge(verb.name(),1,Integer::sum);
        return true;
    }
    private void travel(long seed) {
        counts.clear();
        String result="action budget"; int highest=1;
        try(CoopSession g=CoopSession.open(seed,1,HeroClass.WARRIOR)) {
            g.join(2,HeroClass.HUNTRESS);
            com.watabou.utils.Random.pushGenerator(seed); // fixed combat stream, never redrawn
            for(int n=0;n<2500;n++) {
                highest=Math.max(highest,Dungeon.depth);
                int s=g.activeSeat();
                if(s<0 || g.stopped() || !g.hero(0).isAlive() || !g.hero(1).isAlive()) {result="casualty/halt "+g.life(0)+"/"+g.life(1)+" stops="+g.stopReason();break;}
                Hero h=g.hero(s); Level l=Dungeon.level;
                if(Dungeon.depth==5 && l.map[l.exit()]==Terrain.UNLOCKED_EXIT) { result="unlocked sixth-floor gate"; break; }
                if(Dungeon.depth<5 && g.stairs.stair()==l.exit()) {
                    if(!g.stairs.consent(1-s,g.turn(),l.exit(),false) || !act(g,CoopSession.Action.TRAVEL,l.exit(),null)) {
                        result="destination "+g.destinationBlocker; break;
                    }
                    continue;
                }
                PotionOfHealing heal=h.belongings.getItem(PotionOfHealing.class);
                if(h.HP<h.HT/2 && heal!=null && act(g,CoopSession.Action.DRINK,0,heal)) continue;
                ArrayList<Mob> enemies=new ArrayList<>();
                for(Mob m:l.mobs) if(m.alignment==Char.Alignment.ENEMY && m.invisible<=0 && g.observe(s).visible(m.pos)) enemies.add(m);
                enemies.sort(Comparator.comparingInt((Mob m)->l.distance(h.pos,m.pos)).thenComparingInt(Actor::id));
                boolean acted=false;
                for(Mob m:enemies) {
                    if(s==0 && l.adjacent(h.pos,m.pos) && act(g,CoopSession.Action.ATTACK,m.pos,null)) {acted=true;break;}
                    Item weapon=s==0 ? h.belongings.weapon() : h.belongings.getItem(SpiritBow.class);
                    if(s==1 && act(g,CoopSession.Action.SHOOT,m.pos,weapon)) {acted=true;break;}
                }
                if(acted) continue;
                Heap heap=l.heaps.get(h.pos);
                if(heap!=null && heap.type==Heap.Type.HEAP && !heap.hidden && act(g,CoopSession.Action.PICK_UP,h.pos,heap.peek())) continue;
                Hunger hunger=h.buff(Hunger.class);
                if(hunger!=null && hunger.isStarving()) {
                    Food meal=h.belongings.getItem(Food.class);
                    if(meal!=null && act(g,CoopSession.Action.EAT,0,meal)) continue;
                } else if(h.HP*4<h.HT*3 && enemies.isEmpty() && act(g,CoopSession.Action.WAIT,0,null)) continue;
                int goal=l.exit();
                int supply=-1;
                for(Heap pile:l.heaps.valueList()) {
                    if(pile.hidden || pile.type!=Heap.Type.HEAP || !g.knowledge(h).visible(pile.pos)
                            || Actor.findChar(pile.pos)!=null || !g.ownership.allowed(pile.peek(),g.player(s))) continue;
                    Item item=pile.peek();
                    if(!(item instanceof Food || item instanceof IronKey || item instanceof PotionOfHealing)) continue;
                    if(!h.belongings.backpack.canHold(item)) continue;
                    if(l.distance(h.pos,pile.pos)<=6 && (supply<0 || l.distance(h.pos,pile.pos)<l.distance(h.pos,supply))) supply=pile.pos;
                }
                if(supply>=0 && (l.adjacent(h.pos,supply) || CoopFloorsTest.nextStep(g,supply,true,true)>=0)) {
                    if(l.adjacent(h.pos,supply) && act(g,CoopSession.Action.MOVE,supply,null)) continue;
                    goal=supply;
                }
                if(Dungeon.depth==5) {
                    for(Mob m:l.mobs) if(m instanceof Goo) goal=m.pos;
                    for(Heap pile:l.heaps.valueList()) if(pile.peek() instanceof WornKey) goal=pile.pos;
                    if(l.adjacent(h.pos,l.exit()) && act(g,CoopSession.Action.UNLOCK,l.exit(),null)) continue;
                    if(l.adjacent(h.pos,goal) && l.heaps.get(goal)!=null && act(g,CoopSession.Action.MOVE,goal,null)) continue;
                }
                if(Dungeon.depth<5 && goal==l.exit() && l.distance(h.pos,goal)<=1 && g.stairs.stair()<0) {
                    // Yield a one-cell doorway to the companion who still needs to approach.
                    int partner=g.hero(1-s).pos, best=-1;
                    for(int off:PathFinder.NEIGHBOURS8) {
                        int c=h.pos+off;
                        if(l.distance(c,goal)>1 || !l.passable[c] || Actor.findChar(c)!=null || l.getTransition(c)!=null) continue;
                        if(l.distance(c,partner)>l.distance(h.pos,partner)) best=c;
                    }
                    if(best>=0 && act(g,CoopSession.Action.MOVE,best,null)) continue;
                }
                int next=CoopFloorsTest.nextStep(g,goal,true,true);
                if(next<0) next=CoopFloorsTest.nextStep(g,goal,false,true);
                if(next>=0 && next!=h.pos) {
                    if(l.map[next]==Terrain.SECRET_DOOR && act(g,CoopSession.Action.SEARCH,h.pos,null)) continue;
                    if(act(g,CoopSession.Action.MOVE,next,null)) continue;
                }
                if(!act(g,CoopSession.Action.WAIT,0,null)) {result="rejected wait";break;}
            }
            System.out.println("JOURNEY seed="+seed+" highest="+highest+" result="+result+" actions="+counts);
        }
    }
}
