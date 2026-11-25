package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSeatInventory;
import com.shatteredpixel.shatteredpixeldungeon.tiles.*;
import java.util.Arrays;
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
    private static boolean controls,shortcutKit,shortcuts,mage,floors,sentry,ghost,mimic,crystal,golden,statue,hazards,unstable,deep,kit,garden;
    private static final long LIMIT=TimeUnit.SECONDS.toNanos(18);
    public static void main(String[] args) throws Exception {
        if(args[0].equals("guest")) { guest(new File(args[1]),args.length>2); return; }
        controls=args.length>1 && args[1].equals("controls");
        shortcuts=args.length>1 && args[1].equals("shortcuts");
        sentry=args.length>1 && args[1].equals("sentry");
        ghost=args.length>1 && args[1].equals("ghost");
        crystal=args.length>1 && args[1].equals("crystal");
        golden=args.length>1 && args[1].equals("golden");
        hazards=args.length>1 && args[1].equals("hazards");
        mimic=crystal || golden || (args.length>1 && args[1].equals("mimic"));
        unstable=args.length>1 && args[1].equals("unstable");
        statue=unstable || (args.length>1 && args[1].equals("statue"));
        garden=args.length>1 && args[1].equals("garden");
        shortcutKit=args.length>1 && args[1].equals("kit-slots");
        kit=shortcutKit || (args.length>1 && args[1].equals("kit"));
        deep=args.length>1 && args[1].equals("deep");
        floors=deep || statue || mimic || ghost || sentry || (args.length>1 && args[1].equals("floors"));
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
        String endpoint="127.0.0.1:"+java.nio.file.Files.readString(new File(root,"port").toPath()).trim();
        try {
            if(resume) {
                c.saved(true); await(()->c.frame().page==CoopController.Page.TICKETS && !c.busy());
                c.reconnect(c.frame().list.get(0).id,endpoint);
            } else {
            c.join(endpoint,null);
            await(()->!c.busy() && c.frame().page==CoopController.Page.LOBBY);
            String character=(new File(root,"stairs").isFile() || new File(root,"kit").isFile())?"WARRIOR":"HUNTRESS";
            c.select(character); await(()->!c.busy() && c.frame().roster.contains(character));
            c.ready(true);
            }
            await(()->c.frame().dungeon!=null);
            long end=System.nanoTime()+LIMIT*3;
            while(System.nanoTime()<end && c.frame().page==CoopController.Page.GAME) {
                CoopController.Frame f=c.frame();
                if(!c.busy() && f.dungeon!=null) {
                    if(f.dungeon.offered && !new File(root,"hold-guest").exists()) {
                        Integer target=null;
                        if(resume && f.dungeon.depth==5)
                            for(java.util.Map.Entry<Integer,String> enemy:f.dungeon.monsters.entrySet())
                                if(enemy.getValue().equals("Goo") && Math.abs(enemy.getKey()%f.dungeon.width-f.dungeon.pos%f.dungeon.width)<=1
                                        && Math.abs(enemy.getKey()/f.dungeon.width-f.dungeon.pos/f.dungeon.width)<=1) target=enemy.getKey();
                        c.act(f.image,target==null?"WAIT":"ATTACK",target==null?0:target,0);
                    }
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
        CoopController.Frame current=frame();
        throw new AssertionError("Desktop condition timed out: "+(current==null ? "no client" :
                current.page+" "+current.notice+" "+current.issue+" action="+current.lastAction+" result="+current.lastResult+
                (current.dungeon==null ? "" : " offered="+current.dungeon.offered+" floor="+current.dungeon.floor.keySet()+" hero="+current.dungeon.pos)));
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
        if(key.equals("huntressClass")) return HeroClass.HUNTRESS.title();
        if(key.equals("mageClass")) return HeroClass.MAGE.title();
        return Messages.get(CoopScene.class,key);
    }
    private static Point button(String name) throws Exception {
        return render(()->{
            StyledButton b=(StyledButton)find(Game.scene(),g->g instanceof StyledButton && g.active &&
                    ((StyledButton)g).text().equalsIgnoreCase(name.startsWith("#") ? name.substring(1) : label(name)));
            if(b!=null) return b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2);
            if (name.equals("menu")) {
                com.shatteredpixel.shatteredpixeldungeon.ui.Button menu=(com.shatteredpixel.shatteredpixeldungeon.ui.Button)find(Game.scene(),
                        g->g.getClass().getSimpleName().equals("MenuButton") && g instanceof com.shatteredpixel.shatteredpixeldungeon.ui.Button);
                if(menu!=null) return menu.camera().cameraToScreen(menu.left()+menu.width()/2,menu.top()+menu.height()/2);
            }
            com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.SeatTool tool=(com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.SeatTool)find(Game.scene(),
                    g->g instanceof com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.SeatTool && g.active &&
                            ((com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.SeatTool)g).command.equals(name));
            if(tool!=null) return tool.camera().cameraToScreen(tool.left()+tool.width()/2,tool.top()+tool.height()/2);
            return null;
        });
    }
    private static void click(String name) throws Exception {
        System.out.println("Input "+name); await(()->button(name)!=null); Point p=button(name);
        press(p);
    }
    private static void hold(String name) throws Exception {
        Point p=button(name);
        render(()->{PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,1,PointerEvent.Type.DOWN,PointerEvent.LEFT));return null;});
        Thread.sleep(1000);
        render(()->{PointerEvent.addPointerEvent(new PointerEvent(p.x,p.y,1,PointerEvent.Type.UP,PointerEvent.LEFT));return null;});
        Thread.sleep(150);
    }

    private static void controlInputs(File root) throws Exception {
        long version=frame().image.version;
        PublicDungeon view=frame().dungeon;
        Point before=mapPoint(view,view.pos);
        click("menu"); click("zoom_in");
        Point adjacent=mapPoint(view,view.pos+1);
        click("menu"); click("zoom_out");
        if (Math.abs(adjacent.x-before.x)<=Math.abs(mapPoint(view,view.pos+1).x-before.x))
            throw new AssertionError("Zoom did not change tile spacing");
        render(()->{PointerEvent.addPointerEvent(new PointerEvent(before.x,before.y,1,PointerEvent.Type.DOWN,PointerEvent.LEFT));return null;});
        Thread.sleep(100);
        render(()->{PointerEvent.addPointerEvent(new PointerEvent(before.x+75,before.y+45,1,PointerEvent.Type.DOWN,PointerEvent.LEFT));return null;});
        Thread.sleep(160);
        render(()->{PointerEvent.addPointerEvent(new PointerEvent(before.x+75,before.y+45,1,PointerEvent.Type.UP,PointerEvent.LEFT));return null;});
        Thread.sleep(120);
        if (Point.distance(before,mapPoint(view,view.pos))<20) throw new AssertionError("Missing camera drag");
        click("menu"); click("center_view");
        if (Point.distance(before,mapPoint(view,view.pos))>2 || version!=frame().image.version) throw new AssertionError("Camera changed game");
        click("bag"); await(()->render(()->find(Game.scene(),g->g instanceof WndSeatInventory)!=null));
        render(()->{WndSeatInventory bag=(WndSeatInventory)find(Game.scene(),g->g instanceof WndSeatInventory);
            Field f=com.shatteredpixel.shatteredpixeldungeon.windows.WndTabbed.class.getDeclaredField("tabs");
            f.setAccessible(true); if (((java.util.List<?>)f.get(bag)).size()<2) throw new AssertionError("Missing real pouch"); return null;});
        Point tab=render(()->{
            WndSeatInventory bag=(WndSeatInventory)find(Game.scene(),g->g instanceof WndSeatInventory);
            Field f=com.shatteredpixel.shatteredpixeldungeon.windows.WndTabbed.class.getDeclaredField("tabs"); f.setAccessible(true);
            com.shatteredpixel.shatteredpixeldungeon.ui.Button b=(com.shatteredpixel.shatteredpixeldungeon.ui.Button)((java.util.List<?>)f.get(bag)).get(1);
            return b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2);
        });
        press(tab); shot("pouch");
        render(()->{((WndSeatInventory)find(Game.scene(),g->g instanceof WndSeatInventory)).hide();return null;});
        RenderEncounter.healing(hostController());
        click("wait"); await(()->frame().dungeon.offered && frame().dungeon.hp<frame().dungeon.maxHp);
        java.nio.file.Files.createFile(new File(root,"hold-guest").toPath());
        long previousAction=frame().lastAction;
        hold("wait");
        await(()->!frame().dungeon.offered && frame().lastAction==previousAction+1);
        long guestTurn=frame().image.version;
        Thread.sleep(900);
        if (frame().image.version!=guestTurn || frame().pending!=0) throw new AssertionError("Rest crossed guest turn");
        shot("rest-stopped");
        System.out.println("PASS camera, actual bag tab, bounded rest stops at next seat");
    }

    private static void shortcutInputs() throws Exception {
        long version=frame().image.version;
        click("slot1");
        press(inventoryPoint("FOOD"));
        await(()->frame().quickslots.item(0,frame().dungeon)!=null);
        if (frame().quickslots.assigned(2)) throw new AssertionError("Duplicate food shortcut");
        hold("slot1"); click("replace_shortcut"); press(inventoryPoint("ARMOR"));
        await(()->frame().quickslots.item(0,frame().dungeon).kind.equals("ARMOR"));
        hold("slot1"); click("clear_shortcut"); await(()->!frame().quickslots.assigned(0));
        click("slot4"); press(inventoryPoint("FOOD"));
        await(()->frame().quickslots.item(3,frame().dungeon)!=null);
        shot("custom-shortcuts");
        if (frame().image.version!=version) throw new AssertionError("Binding changed game state");
        click("slot4");
        await(()->frame().lastResult.equals("APPLIED") && frame().quickslots.item(3,frame().dungeon)==null);
        shot("empty-food-shortcut");
        System.out.println("PASS slot assignment, replacement, relocation, clear and confirmed consumption");
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
        await(()->render(()->{
            Group board=(Group)find(Game.scene(),g->g.getClass().getSimpleName().equals("CoopBoard"));
            Field drawn=board.getClass().getDeclaredField("view"); drawn.setAccessible(true);
            return drawn.get(board)==view;
        }));
        return render(()->{
            Group board=(Group)find(Game.scene(),g->g.getClass().getSimpleName().equals("CoopBoard"));
            return board.camera().cameraToScreen((cell%view.width+0.5f)*16,(cell/view.width+0.5f)*16);
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
    /** Compare the live incremental result with fresh original tilemaps for the exact retained seat image. */
    private static void verifyLayerUpdate(Group board) throws Exception {
        Field field=board.getClass().getDeclaredField("view"); field.setAccessible(true);
        com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonRenderMap map=
                new com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonRenderMap((PublicDungeon)field.get(board));
        field=board.getClass().getDeclaredField("layers"); field.setAccessible(true);
        Tilemap[] actual=(Tilemap[])field.get(board);
        Tilemap[] expected={new DungeonTerrainTilemap(map),map.nestLayer(),new WallOcclusionTilemap(map),
                new TerrainFeaturesTilemap(map),new RaisedTerrainTilemap(map),new DungeonWallsTilemap(map),
                new WallBlockingTilemap(map)};
        field=Tilemap.class.getDeclaredField("data"); field.setAccessible(true);
        try {
            for (int i=0;i<actual.length;i++) {
                if (!Arrays.equals((int[])field.get(actual[i]),(int[])field.get(expected[i])))
                    throw new AssertionError("Incremental layer differs from full rebuild: "+actual[i].getClass().getSimpleName());
            }
        } finally {for (Tilemap layer:expected) layer.destroy();}
        System.out.println("PASS incremental native layers equal full rebuild after movement");
    }
    private static Point inventoryPoint(String kind) throws Exception {
        long id=frame().dungeon.belongings.stream().filter(item->item.kind.equals(kind)).findFirst().get().id;
        return render(()->{
            WndSeatInventory.Slot cell=(WndSeatInventory.Slot)find(Game.scene(),g->g instanceof WndSeatInventory.Slot
                    && ((WndSeatInventory.Slot)g).handle==id);
            if (cell==null || !cell.active) throw new AssertionError("Missing public inventory slot");
            return cell.camera().cameraToScreen(cell.left()+cell.width()/2,cell.top()+cell.height()/2);
        });
    }
    private static void bagAction(String kind,String verb) throws Exception {
        click("bag");
        await(()->render(()->find(Game.scene(),g->g instanceof WndSeatInventory)!=null));
        press(inventoryPoint(kind));
        click(verb);
    }
    private static void aimStaff() throws Exception {
        bagAction("STAFF","zap");
        await(()->button("cancel")!=null);
    }
    private static void floorScreens(File root,Process oldGuest) throws Exception {
        await(()->frame().dungeon.offered);
        PublicDungeon.Loot first=frame().quickslots.item(0,frame().dungeon);
        RenderEncounter.atExit(hostController());
        click("wait");
        await(()->frame().dungeon.offered && frame().dungeon.stair>=0);
        shot("stairs-await-consent");
        java.nio.file.Files.createFile(new File(root,"allow1").toPath());
        await(()->frame().dungeon.consent); shot("stairs-agreed");
        click("#"+Messages.get(CoopScene.class,"travel",2));
        await(()->frame().dungeon.depth==2 && frame().dungeon.offered);
        PublicDungeon.Loot carried=frame().quickslots.item(0,frame().dungeon);
        if (first==null || carried==null || !first.reference.equals(carried.reference) || first.id==carried.id)
            throw new AssertionError("Shortcut did not rebind across floors");
        shot("sewer-2");
        if (deep) {
            for(int depth=3;depth<=5;depth++) {
                final int next=depth;
                RenderEncounter.atExit(hostController());
                click("wait");
                await(()->frame().dungeon.offered && frame().dungeon.stair>=0);
                java.nio.file.Files.createFile(new File(root,"allow"+(depth-1)).toPath());
                await(()->frame().dungeon.consent);
                click("#"+Messages.get(CoopScene.class,"travel",depth));
                await(()->frame().dungeon.depth==next && frame().dungeon.offered);
                shot("sewer-"+depth);
            }
            int cell=RenderEncounter.beside(hostController(),com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo.class);
            click("wait"); await(()->frame().dungeon.offered && frame().dungeon.monsters.containsValue("Goo"));
            shot("goo-arena");
            for(int i=0;i<16 && !frame().image.rows.containsKey("charge"+cell);i++) {
                long before=frame().image.version;
                click("wait"); await(()->frame().image.version>before && frame().dungeon.offered);
            }
            if(frame().image.rows.containsKey("charge"+cell)) shot("goo-charge");
            click("menu"); click("saveexit"); await(()->button("saves")!=null);
            click("saves"); await(()->frame().list.size()==1);
            if(frame().list.get(0).current.depth!=5) throw new AssertionError("Boss floor missing");
            shot("boss-save");
            CoopController.Entry bossSlot=frame().list.get(0);
            click("#"+bossSlot.id.toString().substring(0,8)+"  "+Messages.get(CoopScene.class,"floor",5,bossSlot.current.generation));
            click("resume"); await(()->frame().image!=null);
            if(!oldGuest.waitFor(6,TimeUnit.SECONDS)) throw new AssertionError("First guest did not close");
            Process returning=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                    CoopRenderSmoke.class.getName(),"guest",root.toString(),"resume").inheritIO().start();
            try {
                await(()->frame().dungeon.offered && frame().dungeon.depth==5 && frame().dungeon.monsters.containsValue("Goo"));
                shot("goo-reconnected");
                for(int hits=0;hits<100 && frame().dungeon.monsters.containsValue("Goo");hits++) {
                    PublicDungeon world=frame().dungeon;
                    int enemy=world.monsters.entrySet().stream().filter(e->e.getValue().equals("Goo")).findFirst().get().getKey();
                    press(mapPoint(world,enemy));
                    await(()->frame().dungeon!=null && frame().image.version>world.source.version && frame().dungeon.offered);
                }
                if(frame().dungeon.monsters.containsValue("Goo") || frame().dungeon.floor.values().stream().noneMatch(
                        i->i.sprite==com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.BLOB))
                    throw new AssertionError("Expected visible original boss reward");
                shot("goo-rewards");
                click("menu");click("saveexit");await(()->button("host")!=null);
            } finally { returning.destroy(); if(!returning.waitFor(5,TimeUnit.SECONDS)) returning.destroyForcibly(); }
            return;
        }
        if (statue) {
            int cell=RenderEncounter.beside(hostController(),com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue.class);
            click("wait"); await(()->frame().dungeon.offered && "Statue".equals(frame().dungeon.monsters.get(cell)));
            shot("statue-passive");
            int before=staff(frame().dungeon).charges;
            aimStaff(); press(mapPoint(frame().dungeon,cell));
            await(()->frame().dungeon.offered && staff(frame().dungeon).charges==before-1);
            shot("statue-combat");
        }
        if (mimic) {
            int cell=RenderEncounter.beside(hostController(),golden ? com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic.class
                    : crystal ? com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic.class
                    : com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic.class);
            click("wait");
            await(()->frame().dungeon.offered && frame().dungeon.chest(cell));
            if (frame().dungeon.monsters.containsKey(cell)) throw new AssertionError("Disguise disclosed");
            int before=frame().dungeon.belongings.stream().mapToInt(i->i.quantity).sum();
            shot("closed-chest");
            press(mapPoint(frame().dungeon,cell));
            await(()->frame().dungeon.offered && frame().dungeon.monsters.containsValue(golden?"GoldenMimic":crystal?"CrystalMimic":"Mimic"));
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
            if (statue) {
                if (!frame().dungeon.monsters.containsValue("Statue")) throw new AssertionError("Missing restored statue");
                shot("statue-reconnected");
                click("menu");click("saveexit");await(()->button("host")!=null);
                return;
            }
            if (mimic) {
                if (!frame().dungeon.monsters.containsValue(golden?"GoldenMimic":crystal?"CrystalMimic":"Mimic")) throw new AssertionError("Missing resumed mimic");
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
        int testPort;
        try (java.net.ServerSocket port=new java.net.ServerSocket(0)) { testPort=port.getLocalPort(); }
        render(()->{
            Field active=CoopScene.class.getDeclaredField("active"); active.setAccessible(true);
            active.set(null,new CoopController(CoopPlatform.root(),null,null,testPort,0,
                    new java.util.Random(){@Override public long nextLong(){return deep ? 402 : unstable ? 421 : statue ? 416 : golden ? 427 : crystal ? 411 : mimic ? 408 : ghost ? 403 : floors && !sentry ? 415 : 402;}}));
            return null;
        });
        click("title"); await(()->button("host")!=null);
        shot("multiplayer"); click("host"); await(()->button("warriorClass")!=null);
        shot("lobby"); click(kit?"huntressClass":mage?"mageClass":"warriorClass");
        if(mage) shot("mage-selected");
        File root=java.nio.file.Files.createTempDirectory("spd-render-seat").toFile();
        java.nio.file.Files.writeString(new File(root,"port").toPath(),Integer.toString(testPort));
        if(kit) java.nio.file.Files.createFile(new File(root,"kit").toPath());
        if(floors) java.nio.file.Files.createFile(new File(root,"stairs").toPath());
        Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                CoopRenderSmoke.class.getName(),"guest",root.toString()).inheritIO().start();
        try {
            click("ready"); await(()->button("start")!=null); shot("ready"); click("start");
            await(()->frame().dungeon!=null); await(()->button("bag")!=null);
            shot("dungeon");
            if (!mage && !hazards && !kit && !garden) {
                Point portrait=render(()->{
                    com.shatteredpixel.shatteredpixeldungeon.ui.Button info=(com.shatteredpixel.shatteredpixeldungeon.ui.Button)find(Game.scene(),
                            g->g instanceof com.shatteredpixel.shatteredpixeldungeon.ui.Button
                                    && g.getClass().getEnclosingClass()!=null && g.getClass().getEnclosingClass().getSimpleName().equals("CoopHud")
                                    && ((com.shatteredpixel.shatteredpixeldungeon.ui.Button)g).width()==82);
                    return info.camera().cameraToScreen(info.left()+14,info.top()+14);
                });
                press(portrait);
                await(()->render(()->find(Game.scene(),g->g instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage)!=null));
                shot("character");
                render(()->{((com.shatteredpixel.shatteredpixeldungeon.ui.Window)find(Game.scene(),
                        g->g instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage)).hide();return null;});
            }
            if (controls) { controlInputs(root); return; }
            if (shortcuts) { shortcutInputs(); return; }
            if (mage) {
                long turnBefore=frame().image.version;
                click("slot1");
                await(()->button("cancel")!=null);
                shot("quickslot-target");
                click("cancel");
                if (frame().image.version!=turnBefore) throw new AssertionError("Aiming consumed a turn");
            }
            if(garden) {
                int cell=RenderEncounter.sungrass(hostController());
                click("wait");
                await(()->frame().dungeon.offered && frame().dungeon.plant(cell)==3);
                shot("sungrass-visible");
                press(mapPoint(frame().dungeon,cell));
                try {
                    await(()->frame().dungeon.offered && frame().dungeon.pos==cell && frame().dungeon.plant(cell)<0);
                } catch (AssertionError stalled) {
                    CoopController.Frame f=frame();
                    throw new AssertionError("Sungrass move to "+cell+" from "+f.dungeon.pos+" result "+f.lastResult
                            +" notice "+f.notice+" offered "+f.dungeon.offered+" hp "+f.dungeon.hp, stalled);
                }
                int hp=frame().dungeon.hp;
                for (int i=0;i<6 && frame().dungeon.hp<=hp;i++) {
                    long version=frame().image.version;
                    click("wait");
                    await(()->frame().dungeon.offered && frame().image.version>version);
                }
                if (frame().dungeon.hp<=hp || !frame().dungeon.buffs.contains(com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.HERB_HEALING))
                    throw new AssertionError("Expected a real sungrass tick");
                shot("sungrass-healing");
                return;
            }
            if(kit) {
                RenderEncounter.healing(hostController());
                click("wait"); await(()->frame().dungeon.offered && frame().dungeon.floor.containsKey(frame().dungeon.pos));
                press(mapPoint(frame().dungeon,frame().dungeon.pos));
                await(()->frame().dungeon.offered && frame().dungeon.belongings.stream().anyMatch(i->i.kind.equals("HEALING")));
                int hp=frame().dungeon.hp;
                if (shortcutKit) {
                    click("slot1"); press(inventoryPoint("HEALING"));
                    await(()->frame().quickslots.item(0,frame().dungeon)!=null);
                    click("slot1");
                } else bagAction("HEALING","drink");
                await(()->frame().dungeon.offered && frame().dungeon.hp>hp);
                shot("healing-draught");
                RenderEncounter.beside(hostController(),com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat.class);
                click("wait"); await(()->frame().dungeon.offered && frame().dungeon.monsters.containsValue("Rat"));
                if (shortcutKit) {
                    long version=frame().image.version;
                    click("slot2"); await(()->button("cancel")!=null); click("cancel");
                    if (frame().image.version!=version) throw new AssertionError("Bow targeting consumed time");
                    click("slot2");
                } else bagAction("BOW","shoot");
                await(()->button("cancel")!=null); shot("bow-target");
                PublicDungeon view=frame().dungeon;
                int cell=view.monsters.entrySet().stream().filter(e->"Rat".equals(e.getValue())).findFirst().get().getKey();
                press(mapPoint(view,cell));
                await(()->button("cancel")==null && frame().image.version>view.source.version && frame().lastResult.equals("APPLIED"));
                shot("bow-shot");
                return;
            }
            if(hazards) {
                int trap=RenderEncounter.approachHazard(hostController(),true);
                click("wait"); await(()->frame().dungeon.offered && frame().image.rows.containsKey("trap"+trap));
                shot("dart-before");
                int hp=frame().dungeon.hp;
                press(mapPoint(frame().dungeon,trap));
                await(()->frame().dungeon.offered && frame().dungeon.pos==trap && frame().dungeon.hp<hp);
                shot("dart-spent");
                int door=RenderEncounter.approachHazard(hostController(),false);
                click("wait"); await(()->frame().dungeon.offered && frame().dungeon.floor.containsKey(frame().dungeon.pos));
                shot("locked-door");
                press(mapPoint(frame().dungeon,frame().dungeon.pos));
                await(()->frame().dungeon.offered && "1".equals(frame().image.rows.get("keys")));
                press(mapPoint(frame().dungeon,door));
                await(()->frame().dungeon.terrain(door)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.DOOR
                        && "0".equals(frame().image.rows.get("keys")));
                shot("door-unlocked");
                click("menu"); click("saveexit"); await(()->button("host")!=null);
                return;
            }
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
            Gizmo boardBefore=render(()->find(Game.scene(),g->g.getClass().getSimpleName().equals("CoopBoard")));
            Gizmo hudBefore=render(()->find(Game.scene(),g->g.getClass().getSimpleName().equals("CoopHud")));
            Gizmo inputBefore=render(()->find(boardBefore,g->g instanceof PointerArea));
            press(mapPoint(before,target)); await(()->frame().dungeon.pos==target); await(()->frame().dungeon.offered);
            render(()->{ verifyLayerUpdate((Group)boardBefore); return null; });
            if (!render(()->find(Game.scene(),g->g==boardBefore)==boardBefore && find(Game.scene(),g->g==hudBefore)==hudBefore
                    && find(boardBefore,g->g==inputBefore)==inputBefore))
                throw new AssertionError("An ordinary move recreated the floor, HUD or touch listener");
            click("bag");
            await(()->render(()->find(Game.scene(),g->g instanceof WndSeatInventory)!=null)); shot("inventory");
            press(inventoryPoint("FOOD"));
            await(()->render(()->find(Game.scene(),g->g instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions)!=null));
            shot("item-info");
            click("eat"); await(()->frame().lastAction>=2 && "APPLIED".equals(frame().lastResult));
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

