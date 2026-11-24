package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndCoopBag;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.*;
import com.watabou.utils.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.File;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;
import javax.imageio.ImageIO;

/** Optional real LWJGL/X11 test. Use an isolated desktop profile and English locale. */
public final class CoopRenderSmoke {
    private static File shots;
    private static final long LIMIT=TimeUnit.SECONDS.toNanos(18);
    public static void main(String[] args) throws Exception {
        if(args[0].equals("guest")) { guest(new File(args[1])); return; }
        shots=new File(args[0]); if(!shots.isDirectory() && !shots.mkdirs()) throw new AssertionError();
        // Test classes share the launcher's package but do not carry release-manifest attributes.
        java.util.jar.Manifest manifest=new java.util.jar.Manifest(DesktopLauncher.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
        for(String name:new String[]{"Specification-Title","Specification-Version","Implementation-Title","Implementation-Version"})
            System.setProperty(name,manifest.getMainAttributes().getValue(name));
        Thread driver=new Thread(()->{
            try { exercise(); System.out.println("PASS desktop seat UI"); Gdx.app.postRunnable(()->Gdx.app.exit()); }
            catch(Throwable error) { error.printStackTrace(); System.exit(1); }
        },"Desktop UI test");
        driver.setDaemon(true); driver.start(); DesktopLauncher.main(new String[0]);
    }
    private static void guest(File root) throws Exception {
        CoopController c=new CoopController(root,null,InetAddress.getLoopbackAddress(),0,0);
        try {
            c.join("127.0.0.1:"+LanClient.TCP_PORT,null);
            await(()->!c.busy() && c.frame().page==CoopController.Page.LOBBY);
            c.select("HUNTRESS"); await(()->!c.busy() && c.frame().roster.contains("HUNTRESS"));
            c.ready(true); await(()->c.frame().dungeon!=null);
            long end=System.nanoTime()+LIMIT*3;
            while(System.nanoTime()<end && c.frame().page==CoopController.Page.GAME) {
                CoopController.Frame f=c.frame();
                if(!c.busy() && f.dungeon!=null && f.dungeon.offered) c.act(f.image,"WAIT",0,0);
                Thread.sleep(25);
            }
        } finally {c.shutdown();}
    }
    private static <T> T render(Callable<T> action) throws Exception {
        FutureTask<T> run=new FutureTask<>(action); Gdx.app.postRunnable(run); return run.get(8,TimeUnit.SECONDS);
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long end=System.nanoTime()+LIMIT;
        while(System.nanoTime()<end) { if(condition.call()) return; Thread.sleep(35); }
        throw new AssertionError("Desktop condition timed out");
    }
    private static Gizmo find(Gizmo node,Predicate<Gizmo> match) throws Exception {
        if(node==null || !node.visible || !node.exists) return null;
        if(match.test(node)) return node;
        if(node instanceof Group) {
            Field field=Group.class.getDeclaredField("members"); field.setAccessible(true);
            for(Object child:(List<?>)field.get(node)) {
                Gizmo item=find((Gizmo)child,match); if(item!=null) return item;
            }
        }
        return null;
    }
    private static Point button(String name) throws Exception {
        return render(()->{
            StyledButton b=(StyledButton)find(Game.scene(),g->g instanceof StyledButton && g.active &&
                    ((StyledButton)g).text().equalsIgnoreCase(name));
            return b==null?null:b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2);
        });
    }
    private static void click(String name) throws Exception {
        System.out.println("Input "+name); await(()->button(name)!=null); Point p=button(name);
        for(PointerEvent.Type type:new PointerEvent.Type[]{PointerEvent.Type.DOWN,PointerEvent.Type.UP}) {
            render(()->{PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,1,type,PointerEvent.LEFT)); return null;});
            Thread.sleep(100);
        }
    }
    private static void shot(String name) throws Exception {
        Thread.sleep(500);
        Rectangle area=render(()->{
            com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window win=((com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics)Gdx.graphics).getWindow();
            return new Rectangle(win.getPositionX(),win.getPositionY(),Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        });
        ImageIO.write(new Robot().createScreenCapture(area),"png",new File(shots,name+".png"));
    }
    private static CoopController.Frame frame() throws Exception {
        return render(()->{
            Field f=CoopScene.class.getDeclaredField("active"); f.setAccessible(true);
            CoopController c=(CoopController)f.get(null); return c==null?null:c.frame();
        });
    }
    private static void exercise() throws Exception {
        await(()->Gdx.app!=null && render(()->Game.scene()!=null));
        render(()->{ShatteredPixelDungeon.switchNoFade(TitleScene.class);return null;});
        await(()->button("Single player")!=null);
        shot("title"); click("Multiplayer"); await(()->button("Create room")!=null);
        shot("multiplayer"); click("Create room"); await(()->button("Warrior")!=null);
        shot("lobby"); click("Warrior");
        File root=java.nio.file.Files.createTempDirectory("spd-render-seat").toFile();
        Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                CoopRenderSmoke.class.getName(),"guest",root.toString()).inheritIO().start();
        try {
            click("Ready"); await(()->button("Start game")!=null); shot("ready"); click("Start game");
            await(()->frame().dungeon!=null); await(()->button("Inventory")!=null);
            shot("dungeon"); click("Inventory");
            await(()->render(()->find(Game.scene(),g->g instanceof WndCoopBag)!=null)); shot("inventory");
            render(()->{((WndCoopBag)find(Game.scene(),g->g instanceof WndCoopBag)).hide();return null;});
            click("Wait"); await(()->"APPLIED".equals(frame().lastResult));
            click("Game menu"); click("Diagnostics"); await(()->button("Share or copy report")!=null); shot("diagnostics"); click("Back");
            click("Game menu"); click("Save and close room"); await(()->button("Saved games")!=null);
            click("Saved games"); await(()->frame().list.size()==1); shot("saves"); click("Back");
            click("Reconnect"); await(()->frame().list.size()==1); shot("reconnect");
        } finally {
            if(child.isAlive()) child.destroy(); if(!child.waitFor(5,TimeUnit.SECONDS)) child.destroyForcibly();
        }
    }
}

