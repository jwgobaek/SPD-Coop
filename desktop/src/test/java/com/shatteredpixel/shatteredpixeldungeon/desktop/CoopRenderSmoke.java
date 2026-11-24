package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
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

/** Optional real LWJGL/X11 test. Use an isolated desktop profile and an isolated profile. */
public final class CoopRenderSmoke {
    private static File shots;
    private static boolean mage,floors,sentry,ghost,mimic,crystal;
    private static final long LIMIT=TimeUnit.SECONDS.toNanos(18);
    public static void main(String[] args) throws Exception {
        if(args[0].equals("guest")) { guest(new File(args[1]),args.length>2); return; }
        sentry=args.length>1 && args[1].equals("sentry");
        ghost=args.length>1 && args[1].equals("ghost");
        crystal=args.length>1 && args[1].equals("crystal");
        mimic=crystal || (args.length>1 && args[1].equals("mimic"));
        floors=mimic || ghost || sentry || (args.length>1 && args[1].equals("floors"));
        mage=floors || (args.length>1 && args[1].equals("mage"));
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
            String character=new File(root,"stairs").isFile()?"WARRIOR":"HUNTRESS";
            c.select(character); await(()->!c.busy() && c.frame().roster.contains(character));
            c.ready(true);
            }
            await(()->c.frame().dungeon!=null);
            long end=System.nanoTime()+LIMIT*3;
            while(System.nanoTime()<end && c.frame().page==CoopController.Page.GAME) {
                CoopController.Frame f=c.frame();
                if(!c.busy() && f.dungeon!=null) {
                    if(f.dungeon.offered) c.act(f.image,"WAIT",0,0);
                    else if(f.dungeon.stair>=0 && !f.dungeon.consent && new File(root,"allow"+f.dungeon.depth).exists())
                        c.act(f.image,"CONSENT",f.dungeon.stair,0);
                }
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
    private static String label(String key) {
        if(key.equals("warriorClass")) return HeroClass.WARRIOR.title();
        if(key.equals("mageClass")) return HeroClass.MAGE.title();
        return Messages.get(CoopScene.class,key);
    }
    private static Point button(String name) throws Exception {
        return render(()->{
            StyledButton b=(StyledButton)find(Game.scene(),g->g instanceof StyledButton && g.active &&
                    ((StyledButton)g).text().equalsIgnoreCase(name.startsWith("#") ? name.substring(1) : label(name)));
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
    private static PublicDungeon.Loot staff(PublicDungeon view) {
        return view.belongings.stream().filter(i->i.kind.equals("STAFF") && i.slot.equals("WEAPON")).findFirst().get();
    }
    private static void aimStaff() throws Exception {
        click("bag");
        await(()->render(()->find(Game.scene(),g->g instanceof WndCoopBag)!=null));
        PublicDungeon view=frame().dungeon;
        java.util.ArrayList<PublicDungeon.Loot> sorted=new java.util.ArrayList<>(view.belongings);
        sorted.sort((a,b)->Boolean.compare(a.slot.equals("BAG"),b.slot.equals("BAG")));
        int slot=sorted.indexOf(staff(view));
        press(render(()->{
            WndCoopBag wnd=(WndCoopBag)find(Game.scene(),g->g instanceof WndCoopBag);
            return wnd.camera().cameraToScreen(13+24*(slot%5),28+22*(slot/5));
        }));
        click("zap");
        await(()->button("cancel")!=null);
    }
    private static void floorScreens(File root,Process oldGuest) throws Exception {
        await(()->frame().dungeon.offered);
        RenderEncounter.atExit(hostController());
        click("wait");
        await(()->frame().dungeon.offered && frame().dungeon.stair>=0);
        shot("stairs-await-consent");
        java.nio.file.Files.createFile(new File(root,"allow1").toPath());
        await(()->frame().dungeon.consent); shot("stairs-agreed");
        click("#"+Messages.get(CoopScene.class,"travel",2));
        await(()->frame().dungeon.depth==2 && frame().dungeon.offered);
        shot("sewer-2");
        if (mimic) {
            int cell=RenderEncounter.beside(hostController(),crystal ? com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic.class
                    : com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic.class);
            click("wait");
            await(()->frame().dungeon.offered && frame().dungeon.chest(cell));
            if (frame().dungeon.monsters.containsKey(cell)) throw new AssertionError("Disguise disclosed");
            int before=frame().dungeon.belongings.stream().mapToInt(i->i.quantity).sum();
            shot("closed-chest");
            press(mapPoint(frame().dungeon,cell));
            await(()->frame().dungeon.offered && frame().dungeon.monsters.containsValue(crystal?"CrystalMimic":"Mimic"));
            if (crystal && frame().dungeon.belongings.stream().mapToInt(i->i.quantity).sum()!=before-1) throw new AssertionError("Expected one stolen unit");
            shot(crystal?"crystal-theft":"mimic-revealed");
        }
        if (ghost) {
            RenderEncounter.beside(hostController(),com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost.class);
            click("wait"); await(()->frame().dungeon.offered && frame().image.rows.containsValue("OFFER"));
            PublicDungeon v=frame().dungeon;
            int cell=v.monsters.entrySet().stream().filter(e->e.getValue().equals("Ghost")).findFirst().get().getKey();
            press(mapPoint(v,cell));
            await(()->button("accept")!=null); shot("ghost-offer");
            click("accept"); await(()->frame().image.rows.containsValue("HUNT"));
            shot("ghost-quest-started");
        }
        if (sentry) {
            RenderEncounter.atSentry(hostController());
            click("wait");
            await(()->frame().dungeon.offered && frame().dungeon.monsters.containsValue("Sentry"));
            if (frame().image.rows.keySet().stream().noneMatch(k->k.startsWith("charge")))
                throw new AssertionError("Expected the visible sentry to charge");
            shot("red-sentry-charging");
        }
        click("menu");click("saveexit"); await(()->button("saves")!=null);
        click("saves"); await(()->frame().list.size()==1);
        CoopController.Entry entry=frame().list.get(0);
        if(entry.current.depth!=2) throw new AssertionError("Wrong saved floor");
        click("#"+entry.id.toString().substring(0,8)+"  "+Messages.get(CoopScene.class,"floor",2,entry.current.generation));
        click("resume"); await(()->frame().image!=null);
        if(!oldGuest.waitFor(6,TimeUnit.SECONDS)) throw new AssertionError("Guest not closed");
        Process guest=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                CoopRenderSmoke.class.getName(),"guest",root.toString(),"resume").inheritIO().start();
        try {
            await(()->frame().dungeon!=null && frame().dungeon.depth==2 && frame().dungeon.offered);
            shot("sewer-2-reconnected");
            if (mimic) {
                if (!frame().dungeon.monsters.containsValue(crystal?"CrystalMimic":"Mimic")) throw new AssertionError("Missing resumed mimic");
                shot("mimic-reconnected");
                click("menu");click("saveexit");await(()->button("host")!=null);
                return;
            }
            if (ghost) {
                if (!frame().image.rows.containsValue("HUNT")) throw new AssertionError("Missing resumed quest");
                shot("ghost-quest-reconnected");
                click("menu");click("saveexit");await(()->button("host")!=null);
                return;
            }
            if (sentry) {
                if (!frame().dungeon.monsters.containsValue("Sentry")) throw new AssertionError("Missing resumed sentry");
                shot("red-sentry-reconnected");
                click("menu");click("saveexit");await(()->button("host")!=null);
                return;
            }
            java.nio.file.Files.createFile(new File(root,"allow2").toPath());
            await(()->frame().dungeon.consent);
            click("#"+Messages.get(CoopScene.class,"travel",1));
            await(()->frame().dungeon.depth==1 && frame().dungeon.offered);
            shot("sewer-1-return");
            click("wait"); await(()->frame().lastResult.equals("APPLIED"));
            click("menu");click("saveexit");await(()->button("host")!=null);
        } finally { guest.destroy(); if(!guest.waitFor(5,TimeUnit.SECONDS)) guest.destroyForcibly(); }
    }
    private static void exercise() throws Exception {
        await(()->Gdx.app!=null && render(()->Game.scene()!=null));
        render(()->{ShatteredPixelDungeon.switchNoFade(TitleScene.class);return null;});
        await(()->button("solo")!=null);
        shot("title"); click("solo");
        await(()->render(()->Game.scene() instanceof HeroSelectScene || Game.scene() instanceof StartScene));
        shot("solo-entry");
        render(()->{ShatteredPixelDungeon.switchNoFade(TitleScene.class);return null;});
        render(()->{
            Field active=CoopScene.class.getDeclaredField("active"); active.setAccessible(true);
            active.set(null,new CoopController(CoopPlatform.root(),null,null,LanClient.TCP_PORT,LanClient.UDP_PORT,
                    new java.util.Random(){@Override public long nextLong(){return crystal ? 411 : mimic ? 408 : ghost ? 403 : floors && !sentry ? 415 : 402;}}));
            return null;
        });
        click("title"); await(()->button("host")!=null);
        shot("multiplayer"); click("host"); await(()->button("warriorClass")!=null);
        shot("lobby"); click(mage?"mageClass":"warriorClass");
        if(mage) shot("mage-selected");
        File root=java.nio.file.Files.createTempDirectory("spd-render-seat").toFile();
        if(floors) java.nio.file.Files.createFile(new File(root,"stairs").toPath());
        Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                CoopRenderSmoke.class.getName(),"guest",root.toString()).inheritIO().start();
        try {
            click("ready"); await(()->button("start")!=null); shot("ready"); click("start");
            await(()->frame().dungeon!=null); await(()->button("bag")!=null);
            shot("dungeon");
            if(floors) {
                floorScreens(root,child);
                return;
            }
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
            click("bag");
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
            press(item); click("eat"); await(()->frame().lastAction>=2 && "APPLIED".equals(frame().lastResult));
            await(()->frame().dungeon.offered);
            click("wait"); await(()->frame().lastAction>=3 && "APPLIED".equals(frame().lastResult));
            await(()->frame().dungeon.offered);
            RenderEncounter.arrange(hostController());
            click("wait"); await(()->"DOWNED".equals(frame().image.rows.get("allylife")) && frame().dungeon.offered);
            if(frame().image.rows.keySet().stream().noneMatch(k->k.startsWith("z"))) throw new AssertionError("No visible battle edges");
            shot("encounter-revive"); PublicDungeon rescue=frame().dungeon; press(mapPoint(rescue,rescue.ally));
            await(()->"ALIVE".equals(frame().image.rows.get("allylife")) && frame().lastResult.equals("APPLIED"));
            await(()->frame().dungeon.offered); PublicDungeon combat=frame().dungeon;
            if(mage) {
                int charges=staff(combat).charges;
                long version=combat.source.version;
                aimStaff(); shot("staff-target"); click("cancel");
                if(frame().image.version!=version || staff(frame().dungeon).charges!=charges)
                    throw new AssertionError("Cancelling the target consumed a turn or charge");
                aimStaff();
                press(mapPoint(combat,combat.pos-1));
                await(()->staff(frame().dungeon).charges==charges-1 && frame().lastResult.equals("APPLIED"));
                shot("magic-attack-result");
            } else {
                press(mapPoint(combat,combat.pos-1)); await(()->frame().lastAction>=6 && frame().lastResult.equals("APPLIED"));
            }
            click("menu"); click("diagnostics"); await(()->button("export")!=null); shot("diagnostics"); click("back");
            click("menu"); click("saveexit"); await(()->button("saves")!=null);
            click("saves"); await(()->frame().list.size()==1); shot("saves"); click("back");
            click("reconnect"); await(()->frame().list.size()==1 && frame().page==CoopController.Page.TICKETS);
            shot("reconnect"); click("back"); click("saves");
            await(()->frame().page==CoopController.Page.SAVES && frame().list.size()==1);
            CoopController.Entry saved=frame().list.get(0);
            String label=saved.id.toString().substring(0,8)+"  "+Messages.get(CoopScene.class,"floor",saved.current.depth,saved.current.generation);
            click("#"+label); click("resume"); await(()->frame().image!=null);
            if(!child.waitFor(5,TimeUnit.SECONDS)) throw new AssertionError("Old guest did not exit");
            child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                    CoopRenderSmoke.class.getName(),"guest",root.toString(),"resume").inheritIO().start();
            await(()->frame().dungeon!=null && frame().dungeon.offered);
            shot("resumed"); long resumedVersion=frame().image.version;
            click("wait"); await(()->frame().image.version>resumedVersion && "APPLIED".equals(frame().lastResult));
            click("menu"); click("saveexit"); await(()->button("host")!=null);
        } finally {
            if(child.isAlive()) child.destroy(); if(!child.waitFor(5,TimeUnit.SECONDS)) child.destroyForcibly();
        }
    }
}

