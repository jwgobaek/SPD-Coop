package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import java.util.Arrays;
import java.util.concurrent.*;

/** External observer compiled and executed against the official v4.0.2 desktop JAR only. */
public final class OfficialGrowthControl {
    static <T> T render(Callable<T> call) throws Exception {
        FutureTask<T> work=new FutureTask<>(call);Gdx.app.postRunnable(work);return work.get(15,TimeUnit.SECONDS);
    }
    static int seed,depth,inputs,kills;
    static final boolean useSupplies=Boolean.getBoolean("solo.useSupplies");
    static final boolean useStaff=Boolean.getBoolean("solo.useStaff");
    static final int actionBudget=Integer.getInteger("solo.inputs",180);
    static final int targetDepth=Integer.getInteger("solo.depth",3);
    static int[] footfalls;
    static void record(String why) {
        Hero h=Dungeon.hero;
        int xp=h.exp,food=0,heal=0,up=0,ankh=0;
        for(int i=1;i<h.lvl;i++) xp+=Hero.maxExp(i);
        for(Item it:h.belongings) {
            if(it instanceof Food) food+=it.quantity();
            if(it instanceof PotionOfHealing) heal+=it.quantity();
            if(it instanceof ScrollOfUpgrade) up+=it.quantity();
            if(it instanceof Ankh) ankh+=it.quantity();
        }
        System.out.printf("SOLO\t%d\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%s\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%s\t%d%n",
                seed,why,Dungeon.depth,inputs,h.lvl,xp,h.HT,h.STR,h.belongings.weapon(),h.belongings.armor(),
                food,heal,up,ankh,Dungeon.gold,Statistics.enemiesSlain,h.subClass,h.HP);
    }
    static int choice() {
        Hero h=Dungeon.hero;Level l=Dungeon.level;
        if (depth!=Dungeon.depth) {depth=Dungeon.depth;footfalls=new int[l.length()];record("entry");}
        footfalls[h.pos]++;
        boolean[] riskyWater=new boolean[l.length()];
        if (useSupplies && !l.water[h.pos]) {
            for (Mob fish:l.mobs) if (fish instanceof Piranha && fish.invisible==0 && l.heroFOV[fish.pos]) {
                for (int i=0;i<l.length();i++) if (l.heroFOV[i] && l.water[i] && l.distance(fish.pos,i)<=2)
                    riskyWater[i]=true;
            }
        }
        Mob nearest=null;
        for(Mob m:l.mobs) if(l.heroFOV[m.pos] && m.invisible==0 && m.alignment==Char.Alignment.ENEMY && !riskyWater[m.pos]
                && (nearest==null || l.distance(h.pos,m.pos)<l.distance(h.pos,nearest.pos))) nearest=m;
        if(nearest!=null) return nearest.pos;
        if(l.heaps.get(h.pos)!=null && l.heaps.get(h.pos).type==com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.HEAP) return h.pos;
        if(l.visited[l.exit()]) return l.exit();
        int[] first=new int[l.length()],queue=new int[l.length()];
        Arrays.fill(first,-1);first[h.pos]=h.pos;int head=0,tail=0;
        queue[tail++]=h.pos;
        int best=-1,lowest=Integer.MAX_VALUE;
        while (head<tail) {
            int cell=queue[head++];
            for (int d:PathFinder.NEIGHBOURS8) {
                int n=cell+d;
                if(!l.insideMap(n) || riskyWater[n] || first[n]>=0 || !(l.visited[n] || l.heroFOV[n]) || !l.passable[n] || l.pit[n]) continue;
                first[n]=cell==h.pos?n:first[cell];queue[tail++]=n;
                if(footfalls[n]<lowest) {best=first[n];lowest=footfalls[n];}
                if(lowest==0) return best;
            }
        }
        return best;
    }
    static void run() throws Exception {
        long timeout=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);
        while (Gdx.app==null || !render(()->Game.scene() instanceof PixelScene)) {
            if(System.nanoTime()>timeout) throw new AssertionError("Desktop startup");Thread.sleep(50);
        }
        render(()->{
            GamesInProgress.curSlot=1;GamesInProgress.selectedClass=HeroClass.MAGE;
            SPDSettings.intro(false);SPDSettings.customSeed(DungeonSeed.convertToCode(seed));
            Dungeon.initSeed();Dungeon.init();
            if(Dungeon.seed!=seed) throw new AssertionError("Seed selection");
            Dungeon.switchLevel(Dungeon.newLevel(),-1);
            InterlevelScene.mode=InterlevelScene.Mode.DESCEND;
            ShatteredPixelDungeon.switchNoFade(GameScene.class);
            return null;
        });
        long stop=System.nanoTime()+TimeUnit.SECONDS.toNanos(useSupplies ? 210 : 120);
        while(inputs<actionBudget && System.nanoTime()<stop) {
            boolean done=render(()->{
                if(!Dungeon.hero.isAlive()) { record("defeat");return true;}
                if(!(Game.scene() instanceof GameScene) || !Dungeon.hero.ready) return false;
                int cell=choice();
                if(Dungeon.depth>=targetDepth || cell<0) {record(cell<0?"no route":"depth budget");return true;}
                if (useSupplies) {
                    Hero h=Dungeon.hero;
                    for (Item item:h.belongings.backpack.items) {
                        if (item instanceof PotionOfHealing && ((PotionOfHealing)item).isKnown() && h.HP*2<h.HT) {
                            item.execute(h,com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.AC_DRINK);
                            inputs++; record("known healing"); return false;
                        }
                        if (item instanceof Food && h.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger.class)!=null
                                && h.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger.class).isStarving()
                                && h.visibleEnemies()==0) {
                            item.execute(h,Food.AC_EAT);
                            inputs++; record("food"); return false;
                        }
                    }
                }
                if(useStaff && Dungeon.level.heroFOV[cell] && Dungeon.level.distance(Dungeon.hero.pos,cell)>1
                        && com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell) instanceof Mob
                        && new Ballistica(Dungeon.hero.pos,cell,Ballistica.MAGIC_BOLT).collisionPos==cell) {
                    MagesStaff staff=Dungeon.hero.belongings.getItem(MagesStaff.class);
                    if (staff!=null && staff.actions(Dungeon.hero).contains(MagesStaff.AC_ZAP)) {
                        staff.execute(Dungeon.hero,MagesStaff.AC_ZAP);
                        GameScene.handleCell(cell);
                        inputs++;
                        record("staff zap");
                        return false;
                    }
                }
                if(Dungeon.hero.handle(cell)) {Dungeon.hero.next();inputs++;}
                if(Statistics.enemiesSlain!=kills) {kills=Statistics.enemiesSlain;record("kill observed");}
                return false;
            });
            if(done) return;
            Thread.sleep(70);
        }
        render(()->{record("input or wall-clock budget");return null;});
    }
    public static void main(String[] args) throws Exception {
        try (java.io.InputStream in=DesktopLauncher.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            java.util.jar.Manifest manifest=new java.util.jar.Manifest(in);
            for(String key:new String[]{"Specification-Title","Specification-Version","Implementation-Title","Implementation-Version"})
                System.setProperty(key,manifest.getMainAttributes().getValue(key));
        }
        seed=Integer.parseInt(args[0]);
        Thread worker=new Thread(()->{
            try {run();Gdx.app.postRunnable(()->Gdx.app.exit());}
            catch(Throwable error) {error.printStackTrace();System.exit(1);}
        });
        worker.setDaemon(true);worker.start();DesktopLauncher.main(new String[0]);
    }
}
