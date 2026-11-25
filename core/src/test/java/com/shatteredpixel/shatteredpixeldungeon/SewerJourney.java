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
        try {for(String seed:args) {
            try { run.travel(Long.parseLong(seed)); }
            catch(RuntimeException stopped) {
                System.out.println("JOURNEY seed="+seed+" engineFailure="+stopped+" actions="+run.counts);
                if (!(stopped instanceof CoopFloors.UnsupportedDestination)) stopped.printStackTrace(System.err);
            }
        }}
        finally {run.stopBackend();run.temporary.delete();}
        if(System.getProperty("journey.trace")!=null)
            java.nio.file.Files.write(java.nio.file.Paths.get(System.getProperty("journey.trace")),run.route);
    }
    private final boolean coverAlly=Boolean.getBoolean("journey.coverAlly");
    private final List<String> route=new ArrayList<>();
    private final Map<String,Integer> counts=new TreeMap<>();
    private boolean act(CoopSession game,CoopSession.Action verb,int cell,Item item) {
        int seat=game.activeSeat(), depth=Dungeon.depth, from=game.hero(seat).pos;
        if (!CoopFloorsTest.step(game,verb,cell,item)) return false;
        if(System.getProperty("journey.trace")!=null) {
            String state=seat+" "+depth+" "+from+" "+verb+" "+cell+" "+Dungeon.depth+" "+game.hero(seat).pos+" "+game.hero(seat).HP;
            if (Boolean.getBoolean("journey.traceActors")) {
                StringBuilder actors=new StringBuilder().append(com.watabou.utils.Random.worldState()[0]);
                List<Mob> mobs=new ArrayList<>(Dungeon.level.mobs);
                mobs.sort(Comparator.comparingInt(Actor::id));
                for (Mob mob : mobs) {
                    com.watabou.utils.Bundle saved=new com.watabou.utils.Bundle();
                    mob.storeInBundle(saved);
                    actors.append(' ').append(mob.id()).append('/').append(mob.getClass().getSimpleName())
                            .append('/').append(mob.pos).append('/').append(mob.HP).append('/').append(saved.getString("state"))
                            .append('/').append(saved.getInt("target")).append('/').append(saved.getInt("enemy_id"));
                }
                state+=" "+actors;
            }
            route.add(state);
        }
        counts.merge(verb.name(),1,Integer::sum);
        return true;
    }
    private int retreat(Hero actor,Hero ally,Goo goo,boolean rinse) {
        Level level=Dungeon.level;
        int best=-1,score=Integer.MIN_VALUE;
        for(int off:PathFinder.NEIGHBOURS8) {
            int p=actor.pos+off;
            if(!level.insideMap(p) || !level.passable[p] || Actor.findChar(p)!=null || level.getTransition(p)!=null
                    || level.pit[p] || level.plants.get(p)!=null || level.traps.get(p)!=null) continue;
            int distance=level.distance(p,goo.pos);
            if(distance<=level.distance(actor.pos,goo.pos) && !(rinse && level.water[p] && distance>=2)) continue;
            int exits=0;
            for(int n:PathFinder.NEIGHBOURS8) if(level.passable[p+n] && !level.pit[p+n]) exits++;
            int allyDistance=level.distance(p,ally.pos);
            int candidate=distance*10+exits - Math.abs(allyDistance-4)*2;
            if (coverAlly) {
                candidate=distance*8+exits;
                if (ally.isAlive()) candidate-=Math.abs(allyDistance-3)*9;
            }
            if(level.water[p]) candidate+=rinse?35:-12;
            if(candidate>score) {score=candidate;best=p;}
        }
        return best;
    }

    private void travel(long seed) throws java.io.IOException {
        counts.clear();
        int goalDepth=Integer.getInteger("journey.goalDepth",Boolean.getBoolean("journey.prison") ? 6 : 5);
        if (goalDepth<5 || goalDepth>CoopFloors.LAST_PLAYABLE) throw new IllegalArgumentException("Journey depth");
        String result="action budget"; int highest=1;
        HeroClass front=HeroClass.valueOf(System.getProperty("journey.front","WARRIOR"));
        HeroClass back=HeroClass.valueOf(System.getProperty("journey.back","HUNTRESS"));
        String resume=System.getProperty("journey.resume");
        CoopSession session;
        if (resume==null) {
            session=CoopSession.open(seed,1,front);
        } else {
            try (java.io.InputStream in=java.nio.file.Files.newInputStream(java.nio.file.Paths.get(resume))) {
                session=CoopSession.restore(com.watabou.utils.Bundle.read(in));
            }
        }
        try(CoopSession g=session) {
            if (resume==null) {
                g.join(2,back);
                com.watabou.utils.Random.pushGenerator(seed); // fixed combat stream, never redrawn
            }
            Map<Integer,Integer> visits=new HashMap<>();
            int checkpointStep=Integer.getInteger("journey.checkpointStep",0);
            int start=resume==null ? 0 : checkpointStep;
            if (start>0) {
                // The route planner remembers visits. Rebuild only its input history from the
                // control transcript; no positions, actors or items are written to the engine.
                List<String> history=java.nio.file.Files.readAllLines(java.nio.file.Paths.get(System.getProperty("journey.history")));
                for (String line : history.subList(0,start)) {
                    String[] fields=line.split(" ",4);
                    int key=Integer.parseInt(fields[1])*200000+Integer.parseInt(fields[0])*100000+Integer.parseInt(fields[2]);
                    visits.merge(key,1,Integer::sum);
                }
            }
            for(int n=start;n<2500;n++) {
                String save=System.getProperty("journey.checkpoint");
                if (save!=null && n==checkpointStep) {
                    try (java.io.OutputStream out=java.nio.file.Files.newOutputStream(java.nio.file.Paths.get(save))) {
                        if (!com.watabou.utils.Bundle.write(g.checkpoint(),out)) throw new java.io.IOException("Checkpoint write failed");
                    }
                }
                highest=Math.max(highest,Dungeon.depth);
                if (goalDepth>5 && Dungeon.depth==goalDepth) {
                    result="both alive, exit unlocked, entered prison "+goalDepth; break;
                }
                int s=g.activeSeat();
                if(s<0 || g.stopped() || (Dungeon.depth<5 && (g.life(0)==CoopSession.Life.DEAD || g.life(1)==CoopSession.Life.DEAD))) {result="casualty/halt "+g.life(0)+"/"+g.life(1)+" stops="+g.stopReason();break;}
                Hero h=g.hero(s); Level l=Dungeon.level;
                int visitsHere=visits.merge(Dungeon.depth*200000+s*100000+h.pos,1,Integer::sum);
                if(Dungeon.depth==5 && l.map[l.exit()]==Terrain.UNLOCKED_EXIT &&
                        (g.hero(0).belongings.getItem(GooBlob.class)!=null || g.hero(1).belongings.getItem(GooBlob.class)!=null)) {
                    if (goalDepth>5 && g.hero(0).isAlive() && g.hero(1).isAlive()) {
                        if (g.stairs.stair()==l.exit()) {
                            if (!g.stairs.consent(1-s,g.turn(),l.exit(),false) || !act(g,CoopSession.Action.TRAVEL,l.exit(),null)) {
                                result="prison refused "+g.destinationBlocker; break;
                            }
                            continue;
                        }
                        int step=CoopFloorsTest.nextStep(g,l.exit(),true,true);
                        if (step>=0 && step!=h.pos && act(g,CoopSession.Action.MOVE,step,null)) continue;
                        if (act(g,CoopSession.Action.WAIT,0,null)) continue;
                        result="prison approach stopped"; break;
                    }
                    result=g.hero(0).isAlive() && g.hero(1).isAlive() ? "both alive, exit unlocked" : "exit unlocked with a fallen seat"; break;
                }
                if(g.life(1-s)==CoopSession.Life.DOWNED) {
                    int ally=g.hero(1-s).pos;
                    if(l.adjacent(h.pos,ally) && act(g,CoopSession.Action.REVIVE,ally,null)) continue;
                    int toward=CoopFloorsTest.nextStep(g,ally,true,true);
                    if(toward>=0 && act(g,CoopSession.Action.MOVE,toward,null)) continue;
                }
                if(Dungeon.depth!=5 && g.stairs.stair()==l.exit()) {
                    if(Dungeon.depth==4 && (g.hero(0).HP<g.hero(0).HT-1 || g.hero(1).HP<g.hero(1).HT-1)
                            && !g.hero(0).buff(Hunger.class).isStarving() && !g.hero(1).buff(Hunger.class).isStarving() && h.getVisibleEnemies().isEmpty()
                            && act(g,CoopSession.Action.WAIT,0,null)) continue;
                    if(!g.stairs.consent(1-s,g.turn(),l.exit(),false) || !act(g,CoopSession.Action.TRAVEL,l.exit(),null)) {
                        result="destination "+g.destinationBlocker; break;
                    }
                    continue;
                }
                PotionOfHealing heal=h.belongings.getItem(PotionOfHealing.class);
                if(h.HP<h.HT*2/3 && heal!=null && act(g,CoopSession.Action.DRINK,0,heal)) continue;
                ArrayList<Mob> enemies=new ArrayList<>();
                for(Mob m:l.mobs) if(m.alignment==Char.Alignment.ENEMY && m.invisible<=0 && g.observe(s).visible(m.pos)) enemies.add(m);
                enemies.sort(Comparator.comparingInt((Mob m)->l.distance(h.pos,m.pos)).thenComparingInt(Actor::id));
                boolean acted=false;
                for(Mob m:enemies) if(m instanceof Goo) {
                    Goo goo=(Goo)m;
                    int range=l.distance(h.pos,goo.pos);
                    boolean ooze=h.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze.class)!=null;
                    boolean retreatFromMelee=coverAlly ? g.hero(1-s).isAlive() : h.HP<h.HT/2;
                    if((range<=1 && goo.chargeStage()!=1 && retreatFromMelee) || (range<=2 && goo.chargeStage()==2) || (l.water[goo.pos] && range<=2)
                            || (ooze && !l.water[h.pos] && range<=2)) {
                        int away=retreat(h,g.hero(1-s),goo,ooze);
                        if(away>=0 && act(g,CoopSession.Action.MOVE,away,null)) {acted=true;break;}
                    }
                }
                if(acted) continue;
                Hero partnerHero=g.hero(1-s);
                if(Dungeon.depth<5 && !enemies.isEmpty() && h.HP<h.HT/2 && partnerHero.isAlive()
                        && l.adjacent(h.pos,enemies.get(0).pos)) {
                    int best=-1,bestScore=0;
                    for(int shift:PathFinder.NEIGHBOURS8) {
                        int candidate=h.pos+shift;
                        if(!l.passable[candidate] || Actor.findChar(candidate)!=null || l.pit[candidate]
                                || l.getTransition(candidate)!=null || l.traps.get(candidate)!=null || l.plants.get(candidate)!=null) continue;
                        int nearest=Integer.MAX_VALUE;
                        for(Mob enemy:enemies) nearest=Math.min(nearest,l.distance(candidate,enemy.pos));
                        int score=nearest*8-l.distance(candidate,partnerHero.pos);
                        if(nearest>1 && score>bestScore){ best=candidate; bestScore=score; }
                    }
                    if(best>=0 && act(g,CoopSession.Action.MOVE,best,null)) continue;
                }
                for(Mob m:enemies) {
                    if(h.heroClass==HeroClass.MAGE && act(g,CoopSession.Action.ZAP,m.pos,h.belongings.weapon())) {acted=true;break;}
                    if(h.heroClass==HeroClass.HUNTRESS && act(g,CoopSession.Action.SHOOT,m.pos,h.belongings.getItem(SpiritBow.class))) {acted=true;break;}
                    if(l.adjacent(h.pos,m.pos) && act(g,CoopSession.Action.ATTACK,m.pos,null)) {acted=true;break;}
                }
                if(acted) continue;
                if(Dungeon.depth==5 && s==0 && !enemies.isEmpty() && enemies.get(0) instanceof Goo
                        && l.distance(h.pos,enemies.get(0).pos)>=2 && act(g,CoopSession.Action.WAIT,0,null)) continue;
                if(h.heroClass==HeroClass.WARRIOR || h.heroClass==HeroClass.HUNTRESS) {
                    com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor current=h.belongings.armor;
                    boolean changed=false;
                    for(Item bag:h.belongings.backpack.items.toArray(new Item[0])) {
                        if(!CoopSession.basicArmorType(bag)) continue;
                        com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor armor=(com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)bag;
                        if(armor.glyph!=null || armor.STRReq()>h.STR() || (current!=null && armor.tier<=current.tier)) continue;
                        changed=current!=null && current.checkSeal()!=null ? act(g,CoopSession.Action.UNEQUIP,0,current) : act(g,CoopSession.Action.EQUIP,0,armor);
                        if(changed) break;
                    }
                    if(changed) continue;
                }
                Heap heap=l.heaps.get(h.pos);
                if(heap!=null && heap.type==Heap.Type.HEAP && !heap.hidden && act(g,CoopSession.Action.PICK_UP,h.pos,heap.peek())) continue;
                Hunger hunger=h.buff(Hunger.class);
                if(hunger!=null && hunger.isStarving()) {
                    Food meal=h.belongings.getItem(Food.class);
                    if(meal!=null && act(g,CoopSession.Action.EAT,0,meal)) continue;
                } else if(h.HP<h.HT/2 && enemies.isEmpty() && act(g,CoopSession.Action.WAIT,0,null)) continue;
                int goal=l.exit();
                if(partnerHero.isAlive() && Dungeon.depth<5 && enemies.isEmpty() && l.distance(h.pos,partnerHero.pos)>4
                        && !partnerHero.getVisibleEnemies().isEmpty()) {
                    int escort=CoopFloorsTest.nextStep(g,partnerHero.pos,true,true);
                    if(escort>=0 && act(g,CoopSession.Action.MOVE,escort,null)) continue;
                }
                int supply=-1;
                for(Heap pile:l.heaps.valueList()) {
                    if(pile.hidden || pile.type!=Heap.Type.HEAP || !g.knowledge(h).visible(pile.pos)
                            || Actor.findChar(pile.pos)!=null || !g.ownership.allowed(pile.peek(),g.player(s))) continue;
                    Item item=pile.peek();
                    if(!(item instanceof Food || item instanceof IronKey || item instanceof PotionOfHealing)) continue;
                    if(!h.belongings.backpack.canHold(item)) continue;
                    if(l.distance(h.pos,pile.pos)<=6 && (supply<0 || l.distance(h.pos,pile.pos)<l.distance(h.pos,supply))) supply=pile.pos;
                }
                if(visitsHere<3 && supply>=0 && (l.adjacent(h.pos,supply) || CoopFloorsTest.nextStep(g,supply,true,true)>=0)) {
                    if(l.adjacent(h.pos,supply) && act(g,CoopSession.Action.MOVE,supply,null)) continue;
                    goal=supply;
                }
                if(Dungeon.depth==5) {
                    for(Mob m:l.mobs) if(m instanceof Goo) goal=m.pos;
                    Heap loot=null;
                    for(Heap pile:l.heaps.valueList()) {
                        if(pile.peek() instanceof GooBlob && (loot==null || l.distance(h.pos,pile.pos)<l.distance(h.pos,loot.pos))) loot=pile;
                    }
                    if(loot!=null && g.hero(0).belongings.getItem(GooBlob.class)==null && g.hero(1).belongings.getItem(GooBlob.class)==null)
                        goal=loot.pos;
                    for(Heap pile:l.heaps.valueList()) if(pile.peek() instanceof WornKey) goal=pile.pos;
                    if(l.adjacent(h.pos,l.exit()) && act(g,CoopSession.Action.UNLOCK,l.exit(),null)) continue;
                    if(l.adjacent(h.pos,goal) && l.heaps.get(goal)!=null && act(g,CoopSession.Action.MOVE,goal,null)) continue;
                }
                if(Dungeon.depth!=5 && goal==l.exit() && l.distance(h.pos,goal)<=1 && g.stairs.stair()<0) {
                    // Yield a one-cell doorway to the companion who still needs to approach.
                    int partner=g.hero(1-s).pos, best=-1;
                    for(int off:PathFinder.NEIGHBOURS8) {
                        int c=h.pos+off;
                        if(l.distance(c,goal)>1 || !l.passable[c] || Actor.findChar(c)!=null || l.getTransition(c)!=null) continue;
                        if(l.distance(c,partner)>l.distance(h.pos,partner) && visitsHere<4) best=c;
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
            int bossHP=-1;
            for(Mob m:Dungeon.level.mobs) if(m instanceof Goo) bossHP=m.HP;
            int blobs=0;
            for(int seat=0;seat<2;seat++) {
                GooBlob loot=g.hero(seat).belongings.getItem(GooBlob.class);
                if(loot!=null) blobs+=loot.quantity();
            }
            System.out.println("JOURNEY claimedBlobs="+blobs+" bossHP="+bossHP+" hp="+g.hero(0).HP+"/"+g.hero(1).HP+" front="+front+" back="+back+" seed="+seed+" highest="+highest+" result="+result+" actions="+counts);
        }
    }
}
