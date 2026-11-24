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
        if(args[0].equals("guest")) { guest(new File(args[1]),args.length>2); return; }
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
    private static void guest(File root,boolean resume) throws Exception {
        CoopController c=new CoopController(root,null,InetAddress.getLoopbackAddress(),0,0);
        try {
            if(resume) {
                c.saved(true); await(()->c.frame().page==CoopController.Page.TICKETS && !c.busy());
                c.reconnect(c.frame().list.get(0).id,"127.0.0.1:"+LanClient.TCP_PORT);
            } else {
            c.join("127.0.0.1:"+LanClient.TCP_PORT,null);
            await(()->!c.busy() && c.frame().page==CoopController.Page.LOBBY);
            c.select("HUNTRESS"); await(()->!c.busy() && c.frame().roster.contains("HUNTRESS"));
            c.ready(true);
            }
            await(()->c.frame().dungeon!=null);
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
        if(node==null || !node.exists || (!node.visible && !(node instanceof PointerArea))) return null;
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
        press(p);
    }
    private static void press(Point p) throws Exception {
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
    private static Point mapPoint(PublicDungeon view,int cell) throws Exception {
        return render(()->{
            Group board=(Group)find(Game.scene(),g->g.getClass().getSimpleName().equals("CoopBoard"));
            PointerArea area=(PointerArea)find(board,g->g instanceof PointerArea);
            return area.camera().cameraToScreen(area.x+area.width()/2+16*(cell%view.width-view.pos%view.width),
                    area.y+area.height()/2+16*(cell/view.width-view.pos/view.width));
        });
    }
    private static CoopController hostController() throws Exception {
        return render(()->{
            Field f=CoopScene.class.getDeclaredField("active"); f.setAccessible(true);
            return (CoopController)f.get(null);
        });
    }
    private static CoopController.Frame frame() throws Exception {
        return render(()->{
            Field f=CoopScene.class.getDeclaredField("active"); f.setAccessible(true);
            CoopController c=(CoopController)f.get(null);
            if(c!=null && c.frame().page==CoopController.Page.DISCONNECTED)
                throw new AssertionError("Connection failed: "+c.frame().issue);
            return c==null?null:c.frame();
        });
    }
    private static void exercise() throws Exception {
        await(()->Gdx.app!=null && render(()->Game.scene()!=null));
        render(()->{ShatteredPixelDungeon.switchNoFade(TitleScene.class);return null;});
        await(()->button("Single player")!=null);
        shot("title"); click("Single player");
        await(()->render(()->Game.scene() instanceof HeroSelectScene || Game.scene() instanceof StartScene));
        shot("solo-entry");
        render(()->{ShatteredPixelDungeon.switchNoFade(TitleScene.class);return null;});
        render(()->{
            Field active=CoopScene.class.getDeclaredField("active"); active.setAccessible(true);
            active.set(null,new CoopController(CoopPlatform.root(),null,null,LanClient.TCP_PORT,LanClient.UDP_PORT,
                    new java.util.Random(){@Override public long nextLong(){return 402;}}));
            return null;
        });
        click("Multiplayer"); await(()->button("Create room")!=null);
        shot("multiplayer"); click("Create room"); await(()->button("Warrior")!=null);
        shot("lobby"); click("Warrior");
        File root=java.nio.file.Files.createTempDirectory("spd-render-seat").toFile();
        Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                CoopRenderSmoke.class.getName(),"guest",root.toString()).inheritIO().start();
        try {
            click("Ready"); await(()->button("Start game")!=null); shot("ready"); click("Start game");
            await(()->frame().dungeon!=null); await(()->button("Inventory")!=null);
            shot("dungeon");
            PublicDungeon before=frame().dungeon;
            int destination=-1;
            for(int dy=-1;dy<=1 && destination<0;dy++) for(int dx=-1;dx<=1;dx++) {
                int cell=before.pos+dx+dy*before.width;
                if(cell!=before.pos && cell!=before.ally && before.visible(cell) && !before.monsters.containsKey(cell)
                        && !before.source.rows.containsKey("h"+cell) && java.util.Arrays.asList(1,2,9,14,20,29).contains(before.terrain(cell))) { destination=cell; break; }
            }
            if(destination<0) throw new AssertionError("No directly visible adjacent floor");
            final int target=destination;
            press(mapPoint(before,target)); await(()->frame().dungeon.pos==target); await(()->frame().dungeon.offered);
            click("Inventory");
            await(()->render(()->find(Game.scene(),g->g instanceof WndCoopBag)!=null)); shot("inventory");
            PublicDungeon bag=frame().dungeon;
            java.util.ArrayList<PublicDungeon.Loot> sorted=new java.util.ArrayList<>(bag.belongings);
            sorted.sort((a,b)->Boolean.compare(a.slot.equals("BAG"),b.slot.equals("BAG")));
            int food=-1;
            for(int i=0;i<sorted.size();i++) if(bag.actions(sorted.get(i)).contains("EAT")) {food=i;break;}
            if(food<0) throw new AssertionError("Missing starter food");
            final int slot=food;
            Point item=render(()->{
                WndCoopBag wnd=(WndCoopBag)find(Game.scene(),g->g instanceof WndCoopBag);
                return wnd.camera().cameraToScreen(13+24*(slot%5),28+22*(slot/5));
            });
            press(item); click("Eat"); await(()->frame().lastAction>=2 && "APPLIED".equals(frame().lastResult));
            await(()->frame().dungeon.offered);
            click("Wait"); await(()->frame().lastAction>=3 && "APPLIED".equals(frame().lastResult));
            await(()->frame().dungeon.offered);
            RenderEncounter.arrange(hostController());
            click("Wait"); await(()->"DOWNED".equals(frame().image.rows.get("allylife")) && frame().dungeon.offered);
            if(frame().image.rows.keySet().stream().noneMatch(k->k.startsWith("z"))) throw new AssertionError("No visible battle edges");
            shot("encounter-revive"); PublicDungeon rescue=frame().dungeon; press(mapPoint(rescue,rescue.ally));
            await(()->"ALIVE".equals(frame().image.rows.get("allylife")) && frame().lastResult.equals("APPLIED"));
            await(()->frame().dungeon.offered); PublicDungeon combat=frame().dungeon;
            press(mapPoint(combat,combat.pos-1)); await(()->frame().lastAction>=6 && frame().lastResult.equals("APPLIED"));
            click("Game menu"); click("Diagnostics"); await(()->button("Share or copy report")!=null); shot("diagnostics"); click("Back");
            click("Game menu"); click("Save and close room"); await(()->button("Saved games")!=null);
            click("Saved games"); await(()->frame().list.size()==1); shot("saves"); click("Back");
            click("Reconnect"); await(()->frame().list.size()==1 && frame().page==CoopController.Page.TICKETS);
            shot("reconnect"); click("Back"); click("Saved games");
            await(()->frame().page==CoopController.Page.SAVES && frame().list.size()==1);
            CoopController.Entry saved=frame().list.get(0);
            String label=saved.id.toString().substring(0,8)+"  Floor "+saved.current.depth+" · generation "+saved.current.generation;
            click(label); click("Continue game"); await(()->frame().image!=null);
            if(!child.waitFor(5,TimeUnit.SECONDS)) throw new AssertionError("Old guest did not exit");
            child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                    CoopRenderSmoke.class.getName(),"guest",root.toString(),"resume").inheritIO().start();
            await(()->frame().dungeon!=null && frame().dungeon.offered);
            shot("resumed"); long resumedVersion=frame().image.version;
            click("Wait"); await(()->frame().image.version>resumedVersion && "APPLIED".equals(frame().lastResult));
            click("Game menu"); click("Save and close room"); await(()->button("Create room")!=null);
        } finally {
            if(child.isAlive()) child.destroy(); if(!child.waitFor(5,TimeUnit.SECONDS)) child.destroyForcibly();
        }
    }
}

