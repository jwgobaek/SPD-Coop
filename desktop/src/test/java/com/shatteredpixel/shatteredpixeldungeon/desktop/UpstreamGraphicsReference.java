package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.File;
import java.util.concurrent.*;
import java.util.jar.*;
import javax.imageio.ImageIO;

/** External desktop driver compiled against the unmodified v4.0.2 release JAR. */
public class UpstreamGraphicsReference {
    private static File out;
    private static <T> T render(Callable<T> work) throws Exception {
        FutureTask<T> task=new FutureTask<>(work);
        Gdx.app.postRunnable(task);
        return task.get(10, TimeUnit.SECONDS);
    }
    private static void shot(String name) throws Exception {
        Thread.sleep(650);
        Rectangle bounds=render(()->{
            Lwjgl3Graphics graphics=(Lwjgl3Graphics)Gdx.graphics;
            return new Rectangle(graphics.getWindow().getPositionX(),graphics.getWindow().getPositionY(),
                    graphics.getWidth(),graphics.getHeight());
        });
        ImageIO.write(new Robot().createScreenCapture(bounds),"png",new File(out,name+".png"));
    }
    private static void capture(String mode) throws Exception {
        boolean boss=mode.equals("goo"), prison=mode.equals("prison");
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
        while (Gdx.app==null || !render(()->Game.scene() instanceof PixelScene)) {
            if (System.nanoTime()>until) throw new AssertionError("Startup timeout");
            Thread.sleep(50);
        }
        render(()->{
            GamesInProgress.curSlot=1;
            GamesInProgress.selectedClass=boss?HeroClass.MAGE:HeroClass.WARRIOR;
            SPDSettings.intro(false);
            // Use the compact native HUD for the same viewport as the LAN scene.
            SPDSettings.interfaceSize(0);
            SPDSettings.scale(3);
            SPDSettings.zoom(0);
            Dungeon.seed=402;
            Dungeon.init();
            if (boss) Dungeon.depth=5;
            if (prison) Dungeon.depth=6;
            Level level=Dungeon.newLevel();
            int start=level.entrance();
            if (prison) for (com.shatteredpixel.shatteredpixeldungeon.items.Heap heap:level.heaps.valueList()) {
                if (heap.type!=com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.FOR_SALE) continue;
                start=heap.pos;
                break;
            }
            if (boss) for (Mob mob:level.mobs) if (mob instanceof Goo)
                for (int delta:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int cell=mob.pos+delta;
                    if (level.passable[cell] && level.findMob(cell)==null) {start=cell;break;}
                }
            Dungeon.switchLevel(level,start);
            InterlevelScene.mode=InterlevelScene.Mode.NONE;
            ShatteredPixelDungeon.switchNoFade(GameScene.class);
            return null;
        });
        while (!render(()->Game.scene() instanceof GameScene)) {
            if (System.nanoTime()>until) throw new AssertionError("Scene timeout");
            Thread.sleep(50);
        }
        if (prison) {
            shot("prison-shop");
            render(()->{
                com.shatteredpixel.shatteredpixeldungeon.items.Heap offer=Dungeon.level.heaps.get(Dungeon.hero.pos);
                if (offer==null || offer.type!=com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.FOR_SALE)
                    throw new AssertionError("Expected original prison shop stock");
                GameScene.show(new WndTradeItem(offer));
                return null;
            });
            shot("prison-buy");
            return;
        }
        shot(boss?"goo":"sewer");
        if (!boss) {
            Window bag=render(()->{Window w=new WndBag(Dungeon.hero.belongings.backpack);GameScene.show(w);return w;});
            shot("inventory");
            Window hero=render(()->{bag.hide();Window w=new WndHero();GameScene.show(w);return w;});
            shot("character");
            render(()->{
                hero.hide();
                Level level=Dungeon.level;
                for (int door=0;door<level.length();door++) {
                    if (level.map[door]!=com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_DOOR) continue;
                    for (int delta:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                        int cell=door+delta;
                        if (!level.passable[cell] || level.findMob(cell)!=null || level.heaps.get(cell)!=null) continue;
                        Dungeon.hero.pos=cell;
                        Dungeon.hero.sprite.place(cell);
                        Dungeon.observe();
                        GameScene.updateMap();
                        com.watabou.noosa.Camera.main.snapTo(Dungeon.hero.sprite.center());
                        return null;
                    }
                }
                throw new AssertionError("Seed 402 locked door missing");
            });
            shot("locked-door");
        }
    }
    public static void main(String[] args) throws Exception {
        out=new File(args[0]); out.mkdirs();
        Manifest manifest=new Manifest(DesktopLauncher.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
        for (String k:new String[]{"Specification-Title","Specification-Version","Implementation-Title","Implementation-Version"})
            System.setProperty(k,manifest.getMainAttributes().getValue(k));
        Thread worker=new Thread(()->{
            try {capture(args[1]);System.out.println("PASS original "+args[1]);Gdx.app.postRunnable(()->Gdx.app.exit());}
            catch(Throwable t){t.printStackTrace();System.exit(1);}
        });
        worker.setDaemon(true);worker.start();DesktopLauncher.main(new String[0]);
    }
}
