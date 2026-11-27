package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.TrinityDiscoveries;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
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
    private static String regionShot="";
    private static boolean caves,tengu,latePrison,wandmaker,seventh,knife,market,armored,scribe,prison,controls,shortcutKit,shortcuts,mage,floors,sentry,ghost,mimic,crystal,golden,statue,hazards,unstable,deep,kit,garden;
    private static final long LIMIT=TimeUnit.SECONDS.toNanos(18);
    public static void main(String[] args) throws Exception {
        if (args[0].equals("selector-host")) {
            selectorHost(new File(args[1]), Integer.parseInt(args[2]));
            return;
        }
        if(args[0].equals("guest")) { guest(new File(args[1]),args.length>2); return; }
        if(args.length>1 && args[1].matches("selector-(intuition|recall|body|mind|spirit)|cache-input|bow-enchant-input|scroll-enchant-input|eagle-input|strength-input|recharging-input|intuition-input|seal-input|ration-input|frost-input|sunray-input|lunge-input|tome-input|volley-input|hawk-input|wand-input|upper-input|combo-input|crown-input|vault-dart|growth-input|ascent-vote|ascent-surface|vault-warning|vault-elemental|yog-warning|yog-first|yog-amulet|yog-finish|imp-vault|halls-21|mine-crystal|mine-gnoll|smith-services|city-market|king-shield|king-crown")) regionShot=args[1];
        knife=args.length>1 && args[1].equals("knife");
        scribe=knife || args.length>1 && args[1].equals("scrolls");
        market=args.length>1 && args[1].equals("market");
        wandmaker=args.length>1 && args[1].equals("wandmaker");
        caves=args.length>1 && args[1].equals("caves");
        tengu=caves || args.length>1 && args[1].equals("tengu");
        latePrison=tengu || args.length>1 && args[1].equals("prison-nine");
        seventh=latePrison || wandmaker || args.length>1 && args[1].equals("seven");
        prison=seventh || market || args.length>1 && args[1].equals("prison");
        controls=args.length>1 && args[1].equals("controls");
        shortcuts=args.length>1 && args[1].equals("shortcuts");
        sentry=args.length>1 && args[1].equals("sentry");
        ghost=args.length>1 && args[1].equals("ghost");
        crystal=args.length>1 && args[1].equals("crystal");
        golden=args.length>1 && args[1].equals("golden");
        hazards=args.length>1 && args[1].equals("hazards");
        mimic=crystal || golden || (args.length>1 && args[1].equals("mimic"));
        unstable=args.length>1 && args[1].equals("unstable");
        armored=args.length>1 && args[1].equals("armored");
        statue=armored || unstable || (args.length>1 && args[1].equals("statue"));
        garden=args.length>1 && args[1].equals("garden");
        shortcutKit=args.length>1 && args[1].equals("kit-slots");
        kit=shortcutKit || (args.length>1 && args[1].equals("kit"));
        deep=prison || (args.length>1 && args[1].equals("deep"));
        floors=deep || statue || mimic || ghost || sentry || (args.length>1 && args[1].equals("floors"));
        mage=regionShot.equals("frost-input") || regionShot.equals("volley-input") || regionShot.equals("wand-input") || floors || (args.length>1 && args[1].equals("mage"));
        shots=new File(args[0]); if(!shots.isDirectory() && !shots.mkdirs()) throw new AssertionError();
        // Test classes share the launcher's package but do not carry release-manifest attributes.
        java.util.jar.Manifest manifest=new java.util.jar.Manifest(DesktopLauncher.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
        for(String name:new String[]{"Specification-Title","Specification-Version","Implementation-Title","Implementation-Version"})
            System.setProperty(name,manifest.getMainAttributes().getValue(name));
        Thread driver=new Thread(()->{
            try { exercise(); System.out.println("PASS desktop seat UI"); Gdx.app.postRunnable(()->Gdx.app.exit()); }
            catch(Throwable error) { error.printStackTrace(); try { shot("failed-input"); } catch(Exception ignored) {} System.exit(1); }
        },"Desktop UI test");
        driver.setDaemon(true); driver.start(); DesktopLauncher.main(new String[0]);
    }
    // Resume the original staged server and tickets through the normal LAN protocol.
    // The GUI process only has the guest profile. The host uses its own engine and storage.
    private static void selectorHost(File directory, int port) throws Exception {
        var backend = new com.shatteredpixel.shatteredpixeldungeon.SealArtsTest();
        backend.temporary.create();
        backend.startBackend();
        CoopController host = new CoopController(new File(directory, "seat0"), null, InetAddress.getLoopbackAddress(), port, 0);
        try {
            host.saved(false);
            await(() -> host.frame().page == CoopController.Page.SAVES && !host.busy());
            if (host.frame().list.size() != 1) throw new AssertionError("Expected one isolated test room");
            host.resume(host.frame().list.get(0).id, false);
            await(() -> host.frame().dungeon != null);
            java.nio.file.Files.createFile(new File(directory, "host-ready").toPath());
            long deadline = System.nanoTime() + 4*LIMIT;
            ProcessHandle gui = ProcessHandle.current().parent().orElseThrow();
            while (gui.isAlive() && System.nanoTime() < deadline && !new File(directory, "complete").exists()) {
                CoopController.Frame offered = host.frame();
                if (!host.busy() && offered.dungeon != null && offered.dungeon.offered)
                    host.act(offered.image, "WAIT", 0, 0);
                Thread.sleep(35);
            }
        } finally {
            host.shutdown();
            backend.stopBackend();
            backend.temporary.delete();
        }
    }

    private static void guestSelectors(File directory, int port) throws Exception {
        String address = "127.0.0.1:" + port;
        Process host = new ProcessBuilder(System.getProperty("java.home")+"/bin/java", "-cp",
                System.getProperty("java.class.path"), CoopRenderSmoke.class.getName(), "selector-host",
                directory.toString(), Integer.toString(port)).inheritIO().start();
        try {
            await(() -> new File(directory, "host-ready").isFile());
            CoopController client = hostController();
            client.saved(true);
            await(() -> client.frame().page == CoopController.Page.TICKETS && !client.busy());
            if (client.frame().list.size() != 1) throw new AssertionError("Expected one guest ticket");
            java.util.UUID room = client.frame().list.get(0).id;
            client.reconnect(room, address);
            await(() -> client.frame().dungeon != null && client.frame().seat == 1);
            await(() -> client.frame().dungeon.offered);
            int unopenedCharge = ownTome().charges;
            String memoryLabel = Boolean.getBoolean("spd.selector.history")
                    ? discoveryLabel(discoveryChoice(frame().dungeon)) : null;
            openSpellPicker(ownTome().id, selectorCase().spell.name());
            if (memoryLabel == null) await(CoopRenderSmoke::selectorOpen);
            else waitForButton(memoryLabel);
            client.leave(false);
            await(() -> client.frame().page == CoopController.Page.MENU && !selectorOpen() && !client.busy());
            if (memoryLabel != null) await(() -> button(memoryLabel) == null);
            client.reconnect(room, address);
            await(() -> client.frame().dungeon != null && client.frame().dungeon.offered);
            if (ownTome().charges != unopenedCharge) throw new AssertionError("Leaving an open selector spent charge");
            selectorInputs();
            int charge = ownTome().charges;
            client.leave(false);
            await(() -> client.frame().page == CoopController.Page.MENU && !client.busy());
            client.reconnect(room, address);
            await(() -> client.frame().dungeon != null && client.frame().seat == 1 && client.frame().dungeon.offered);
            if (ownTome().charges != charge || frame().pending != 0) throw new AssertionError("Guest charge or receipt changed on reconnect");
            waitAndRegainTurn();
            shot("guest-restored-next-action");
            System.out.println("PASS guest reauthentication, charge and next action: " + regionShot);
        } finally {
            java.nio.file.Files.createFile(new File(directory, "complete").toPath());
            if (!host.waitFor(8, TimeUnit.SECONDS)) host.destroyForcibly();
        }
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
            String character=new File(root,"yog-room").isFile()?"MAGE":(new File(root,"stairs").isFile() || new File(root,"kit").isFile())?"WARRIOR":"HUNTRESS";
            c.select(character); await(()->!c.busy() && c.frame().roster.contains(character));
            c.ready(true);
            }
            await(()->c.frame().dungeon!=null);
            long end=System.nanoTime()+LIMIT*3;
            while(System.nanoTime()<end && c.frame().page==CoopController.Page.GAME) {
                CoopController.Frame f=c.frame();
                if(!c.busy() && f.dungeon!=null) {
                    if (new File(root,"yog-room").isFile() && "OFFER".equals(f.image.rows.get("ending")) && "1".equals(f.image.rows.get("amuletowner"))) {
                        c.act(f.image,"END_RUN",0,0);
                    } else if(f.dungeon.offered && !new File(root,"hold-guest").exists() && !f.image.rows.containsKey("ending")) {
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
        if(key.equals("rogueClass")) return HeroClass.ROGUE.title();
        if(key.equals("duelistClass")) return HeroClass.DUELIST.title();
        if(key.equals("clericClass")) return HeroClass.CLERIC.title();
        if(key.equals("warriorClass")) return HeroClass.WARRIOR.title();
        if(key.equals("huntressClass")) return HeroClass.HUNTRESS.title();
        if(key.equals("mageClass")) return HeroClass.MAGE.title();
        return Messages.get(CoopScene.class,key);
    }
    private static void enchantScrollInput() throws Exception {
        await(()->frame().dungeon.offered && frame().dungeon.belongings.stream().anyMatch(i->i.kind.equals("READ_SCROLL")));
        var scroll=frame().dungeon.belongings.stream().filter(i->i.kind.equals("READ_SCROLL")).findFirst().get();
        var bow=frame().dungeon.belongings.stream().filter(i->i.kind.equals("BOW")).findFirst().get();
        click("bag"); await(()->selectorOpen()); openInventoryTab(scroll.bagIndex); press(inventoryPoint(scroll.id)); click("read_scroll");
        await(()->selectorOpen() && "inventory".equals(frame().image.rows.get("enchantphase")));
        shot("known-scroll-target");
        java.awt.Robot keyboard=new java.awt.Robot();
        keyboard.keyPress(java.awt.event.KeyEvent.VK_ESCAPE); keyboard.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
        await(()->frame().pending==0 && frame().dungeon.offered && !frame().image.rows.containsKey("enchantphase") && button("wait")!=null);
        click("bag"); await(()->selectorOpen()); openInventoryTab(scroll.bagIndex); press(inventoryPoint(scroll.id)); click("read_scroll");
        await(()->selectorOpen()); openInventoryTab(bow.bagIndex); press(inventoryPoint(bow.id));
        await(()->"options".equals(frame().image.rows.get("enchantphase")));
        String key=frame().image.rows.get("enchantoption1");
        String choice="#"+Messages.titleCase(Messages.get(key+".name",(Object)"").trim());
        Class<?> type=com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment.class;
        await(()->button(choice)!=null);
        shot("three-native-options");
        click("#"+Messages.get(type,"cancel"));
        shot("confirm-cancel");
        click("#"+Messages.get(type,"cancel_warn_no"));
        click(choice);
        await(()->!frame().image.rows.containsKey("enchantphase") && key.equals(frame().image.rows.get("bowenchant"+bow.id)));
        shot("enchanted-bow");
        System.out.println("PASS real scroll input, reversible known target selection, stable options and chosen bow");
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
    private static Point waitForButton(String name) throws Exception {
        long end=System.nanoTime()+LIMIT;
        do {
            Point point=button(name);
            if (point!=null) return point;
            Thread.sleep(35);
        } while (System.nanoTime()<end);
        throw new AssertionError("Desktop input did not become available: "+name);
    }

    private static void click(String name) throws Exception {
        System.out.println("Input "+name);
        press(waitForButton(name));
    }
    private static void sceneMetrics(String boundary) throws Exception {
        render(()->{
            java.util.concurrent.atomic.AtomicInteger nodes=new java.util.concurrent.atomic.AtomicInteger();
            find(Game.scene(),node->{ nodes.incrementAndGet(); return false; });
            int engines=0, transports=0, clients=0;
            for (Thread thread:Thread.getAllStackTraces().keySet()) {
                if (thread.getName().equals("Coop engine")) engines++;
                if (thread.getName().equals("spd-lan-io")) transports++;
                if (thread.getName().equals("Coop client")) clients++;
            }
            System.out.println("scene boundary="+boundary+" drawn_or_input_nodes="+nodes.get()
                    +" host_engines="+engines+" lan_transports="+transports+" client_workers="+clients);
            return null;
        });
    }
    private static void waitAndRegainTurn() throws Exception {
        long action=frame().lastAction;
        long version=frame().image.version;
        click("wait");
        await(()->{
            CoopController.Frame current=frame();
            return current.dungeon!=null && current.image!=null && current.lastAction==action+1
                    && current.image.version>version && current.dungeon.offered
                    && current.lastResult.equals("APPLIED") && button("wait")!=null;
        });
        PublicDungeon offered=frame().dungeon;
        mapPoint(offered,offered.pos);
    }

    private static void hold(String name) throws Exception {
        Point p=waitForButton(name);
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
        waitAndRegainTurn();
        if (frame().dungeon.hp>=frame().dungeon.maxHp) throw new AssertionError("Missing rest fixture");
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
        return inventoryPoint(id);
    }
    private static void openInventoryTab(int bagIndex) throws Exception {
        java.awt.Robot keyboard = new java.awt.Robot();
        for (int i = 0; i < bagIndex; i++) {
            keyboard.keyPress(java.awt.event.KeyEvent.VK_TAB);
            keyboard.keyRelease(java.awt.event.KeyEvent.VK_TAB);
            keyboard.waitForIdle();
            render(() -> true);
        }
    }
    private static Point inventoryPoint(long id) throws Exception {
        return render(()->{
            WndSeatInventory.Slot cell=(WndSeatInventory.Slot)find(Game.scene(),g->g instanceof WndSeatInventory.Slot
                    && ((WndSeatInventory.Slot)g).handle==id);
            if (cell==null || !cell.active) throw new AssertionError("Missing public inventory slot");
            return cell.camera().cameraToScreen(cell.left()+cell.width()/2,cell.top()+cell.height()/2);
        });
    }
    private static void bagAction(String kind,String verb) throws Exception {
        PublicDungeon offered=frame().dungeon;
        mapPoint(offered,offered.pos); // Wait for the same verified version to reach the clickable HUD.
        click("bag");
        await(()->render(()->find(Game.scene(),g->g instanceof WndSeatInventory)!=null));
        press(inventoryPoint(kind));
        click(verb);
    }
    private static void aimStaff() throws Exception {
        bagAction("STAFF","zap");
        await(()->button("cancel")!=null);
    }
    private static void marketScreens() throws Exception {
        await(()->frame().dungeon.offered);
        int stock=RenderEncounter.market(hostController(),true,0);
        waitAndRegainTurn();
        PublicDungeon before=frame().dungeon;
        press(mapPoint(before,stock)); waitForButton("cancel"); shot("shop-insufficient"); click("cancel");
        if (before.source.version!=frame().image.version) throw new AssertionError("Cancel consumed a turn");
        RenderEncounter.market(hostController(),true,10000); waitAndRegainTurn();
        PublicDungeon rich=frame().dungeon;
        String price=rich.source.rows.get("quote"+stock).split(",")[1];
        press(mapPoint(rich,stock)); waitForButton("cancel"); shot("shop-buy");
        click("#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndTradeItem.class,"buy",Integer.parseInt(price)));
        await(()->frame().dungeon.offered && !frame().image.rows.containsKey("sale"+stock));
        int merchant=RenderEncounter.market(hostController(),false,0); waitAndRegainTurn();
        PublicDungeon owned=frame().dungeon;
        PublicDungeon.Loot armor=owned.belongings.stream().filter(i->i.slot.equals("BAG") &&
                !rich.belongings.stream().anyMatch(old->old.reference.equals(i.reference))).findFirst().get();
        int value=Integer.parseInt(owned.source.rows.get("sell"+armor.id).split(",")[0]);
        press(mapPoint(owned,merchant));
        click("#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.class,"sell"));
        press(inventoryPoint(armor.id)); click("sell"); waitForButton("cancel"); shot("shop-sell");
        click("#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndTradeItem.class,"sell",value));
        await(()->frame().dungeon.offered && frame().image.rows.keySet().stream().anyMatch(k->k.startsWith("refund")));
        PublicDungeon sold=frame().dungeon;
        press(mapPoint(sold,merchant)); waitForButton("cancel"); shot("shop-buyback");
        String redeem="#"+Messages.get(CoopScene.class,"shop_buyback",value,1);
        click(redeem); await(()->frame().dungeon.offered); click(redeem);
        await(()->frame().dungeon.offered && frame().image.rows.keySet().stream().noneMatch(k->k.startsWith("refund")));
        shot("shop-restored-stock");
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
                if (prison) {
                    long beforeKey=frame().image.version;
                    RenderEncounter.atBossKey(hostController()); click("wait");
                    await(()->frame().image.version>beforeKey && frame().dungeon.offered && frame().dungeon.floor.containsKey(frame().dungeon.pos)
                            && frame().dungeon.floor.get(frame().dungeon.pos).sprite==com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.WORN_KEY);
                    PublicDungeon v=frame().dungeon;
                    press(mapPoint(v,v.pos));
                    await(()->frame().dungeon.offered && frame().image.version>v.source.version);
                    RenderEncounter.atExit(hostController());
                    waitAndRegainTurn();
                    PublicDungeon near=frame().dungeon;
                    int exit=-1;
                    for (int j=0;j<near.width*near.height;j++) if(near.visible(j) && near.terrain(j)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_EXIT) {exit=j;break;}
                    if(exit<0) throw new AssertionError("Missing locked gate");
                    press(mapPoint(near,exit));
                    await(()->frame().dungeon.stair>=0 && frame().dungeon.offered);
                    java.nio.file.Files.createFile(new File(root,"allow5").toPath());
                    await(()->frame().dungeon.consent);
                    click("#"+Messages.get(CoopScene.class,"travel",6)); await(()->frame().dungeon.depth==6);
                    shot("prison-arrival");
                    if (market) marketScreens();
                    if (seventh) {
                        waitAndRegainTurn();
                        RenderEncounter.atExit(hostController()); waitAndRegainTurn();
                        java.nio.file.Files.createFile(new File(root,"allow6").toPath());
                        await(()->frame().dungeon.consent && frame().dungeon.offered);
                        click("#"+Messages.get(CoopScene.class,"travel",7));
                        await(()->frame().dungeon.depth==7);
                        shot("prison-seven-arrival");
                        if (latePrison) {
                            for (int depth = 8; depth <= (caves ? 15 : tengu ? 11 : 9); depth++) {
                                final int arrival = depth;
                                waitAndRegainTurn();
                                RenderEncounter.atExit(hostController());
                                waitAndRegainTurn();
                                java.nio.file.Files.createFile(new File(root, "allow" + (depth-1)).toPath());
                                await(() -> frame().dungeon.consent && frame().dungeon.offered);
                                click("#" + Messages.get(CoopScene.class, "travel", depth));
                                await(() -> frame().dungeon.depth == arrival);
                                shot("prison-" + depth + "-arrival");
                                if (depth == 10) {
                                    for (String phase:new String[]{"FIGHT_START","FIGHT_ARENA","WON"}) {
                                        RenderEncounter.tengu(hostController(),phase);
                                        await(()->frame().image.rows.get("floor").endsWith(","+phase));
                                        shot("tengu-"+phase.toLowerCase(java.util.Locale.ROOT));
                                    }
                                    await(()->frame().dungeon.offered);
                                }
                                if (caves && (depth==12 || depth==13)) {
                                    String resident=depth==12?"Spinner":"Blacksmith";
                                    int residentCell=RenderEncounter.beside(hostController(),depth==12
                                            ? com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Spinner.class
                                            : com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.class);
                                    waitAndRegainTurn();
                                    await(()->frame().dungeon.monsters.containsValue(resident));
                                    shot("caves-"+resident.toLowerCase(java.util.Locale.ROOT));
                                    if(depth==13) {
                                        press(mapPoint(frame().dungeon,residentCell));
                                        await(()->button("#"+Messages.get(CoopScene.class,"accept"))!=null);
                                        shot("smith-offer");
                                        click("#"+Messages.get(CoopScene.class,"accept"));
                                        await(()->frame().image.rows.values().stream().anyMatch(value->value.startsWith("reminder_")));
                                    }
                                }
                                if (caves && depth==15) {
                                    for(String phase:new String[]{"DM_START","DM_CHARGE","DM_WON"}) {
                                        RenderEncounter.tengu(hostController(),phase);
                                        await(()->phase.equals("DM_START") ? frame().dungeon.monsters.containsValue("DM300")
                                                : phase.equals("DM_CHARGE") ? frame().image.rows.keySet().stream().anyMatch(k->k.startsWith("charge"))
                                                : !frame().dungeon.monsters.containsValue("DM300") && frame().dungeon.floor.values().stream().anyMatch(i->i.sprite==com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SHARD));
                                        shot(phase.toLowerCase(java.util.Locale.ROOT));
                                    }
                                }
                                if (depth == 8) {
                                    RenderEncounter.beside(hostController(), com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer.class);
                                    waitAndRegainTurn();
                                    shot("prison-necromancer");
                                }
                            }
                        }
                        if (wandmaker) {
                            int npc = RenderEncounter.beside(hostController(),com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker.class);
                            waitAndRegainTurn();
                            await(()->frame().image.rows.containsKey("wandquest"+npc));
                            press(mapPoint(frame().dungeon,npc));
                            await(()->button("accept")!=null);
                            shot("wandmaker-garden-offer");
                            click("accept");
                            await(()->"berry,HUNT".equals(frame().image.rows.get("wandquest"+npc)));
                            shot("wandmaker-accepted");
                            RenderEncounter.questItems(hostController());
                            waitAndRegainTurn();
                            String row=frame().image.rows.get("wandquest"+npc);
                            if (row==null || !row.startsWith("berry,REWARD,")) throw new AssertionError("Missing native candidates");
                            String label=Messages.titleCase(Messages.get("items.wands."+row.split(",")[2].toLowerCase(java.util.Locale.ROOT)+".name"));
                            press(mapPoint(frame().dungeon,npc));
                            await(()->button("#"+label)!=null);
                            shot("wandmaker-two-rewards");
                            click("#"+label);
                            await(()->!frame().image.rows.containsKey("wandquest"+npc));
                            shot("wandmaker-after-reward");
                            waitAndRegainTurn();
                            PublicDungeon.Loot wand=frame().dungeon.belongings.stream()
                                    .filter(item->item.kind.equals("WAND")).findFirst().get();
                            hold("slot4"); press(inventoryPoint(wand.id));
                            await(()->frame().quickslots.item(3,frame().dungeon)!=null
                                    && frame().quickslots.item(3,frame().dungeon).kind.equals("WAND"));
                            click("slot4");
                            await(()->button("cancel")!=null);
                            shot("handheld-missile-target");
                            click("cancel");
                        }
                    }
                }
                click("menu");click("saveexit");await(()->button("host")!=null);
            } finally { returning.destroy(); if(!returning.waitFor(5,TimeUnit.SECONDS)) returning.destroyForcibly(); }
            return;
        }
        if (statue) {
            Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob> kind=armored ?
                    com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue.class : com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue.class;
            int cell=RenderEncounter.beside(hostController(),kind);
            click("wait"); await(()->frame().dungeon.offered && kind.getSimpleName().equals(frame().dungeon.monsters.get(cell)));
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
                if (!frame().dungeon.monsters.containsValue(armored?"ArmoredStatue":"Statue")) throw new AssertionError("Missing restored statue");
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
    private static void openCharacter() throws Exception {
        press(render(()->{
            com.shatteredpixel.shatteredpixeldungeon.ui.Button info=(com.shatteredpixel.shatteredpixeldungeon.ui.Button)find(Game.scene(),
                    g->g instanceof com.shatteredpixel.shatteredpixeldungeon.ui.Button && g.getClass().getEnclosingClass()!=null
                            && g.getClass().getEnclosingClass().getSimpleName().equals("CoopHud")
                            && ((com.shatteredpixel.shatteredpixeldungeon.ui.Button)g).width()==82);
            return info.camera().cameraToScreen(info.left()+14,info.top()+14);
        }));
    }

    private static void vaultDart() throws Exception {
        await(()->frame().dungeon.branch==1 && frame().dungeon.pos==1872 && frame().dungeon.offered);
        click("pick_up");
        await(()->frame().dungeon.belongings.stream().anyMatch(i->i.kind.equals("DART")) && frame().dungeon.offered);
        PublicDungeon v=frame().dungeon;
        PublicDungeon.Loot dart=v.belongings.stream().filter(i->i.kind.equals("DART")).findFirst().get();
        if (dart.quantity!=2) throw new AssertionError("Expected the original two darts");
        hold("slot4"); press(inventoryPoint(dart.id));
        await(()->frame().quickslots.item(3,frame().dungeon)!=null && frame().quickslots.item(3,frame().dungeon).id==dart.id);
        long version=frame().image.version;
        click("slot4"); waitForButton("cancel"); shot("vault-dart-aim"); click("cancel");
        if (frame().image.version!=version) throw new AssertionError("Aim cancellation changed the game");
        click("slot4"); waitForButton("cancel");
        int landing=-1;
        for (int dy=-1;dy<=1 && landing<0;dy++) for (int dx=-1;dx<=1;dx++) {
            int cell=v.pos+dx+dy*v.width,tile=v.terrain(cell);
            if (cell!=v.pos && cell!=v.ally && v.visible(cell) && tile>=0 && !v.monsters.containsKey(cell)
                    && (com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.flags[tile]&com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.PASSABLE)!=0) {
                landing=cell;break;
            }
        }
        if (landing<0) throw new AssertionError("Missing visible landing cell");
        press(mapPoint(v,landing));
        await(()->frame().dungeon.belongings.stream().anyMatch(i->i.id==dart.id && i.quantity==1));
        shot("vault-dart-thrown");
        System.out.println("PASS seed 572 cell 1872 native vault HUD pickup, bind, aim, cancel and throw");
    }

    private static void comboInputs() throws Exception {
        await(()->frame().dungeon.offered && frame().image.rows.containsKey("finish1"));
        PublicDungeon v=frame().dungeon;
        String hud="#"+v.source.rows.get("combocount")+"/"+v.source.rows.get("combotime");
        click(hud);
        com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo.ComboMove move=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo.ComboMove.SLAM;
        String label="#_"+Messages.titleCase(move.title())+" "+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndCombo.class,"combo_req",move.comboReq)+":_ "+move.desc(move.comboReq,0);
        waitForButton(label);shot("combo-menu");click(label);
        await(()->button("cancel")!=null);
        shot("combo-aim");click("cancel");
        if(frame().image.version!=v.source.version) throw new AssertionError("Cancelled aim changed the game");
        click(hud);click(label);
        press(mapPoint(v,v.monsters.entrySet().stream().filter(e->e.getValue().equals("Rat")).findFirst().get().getKey()));
        await(()->frame().image.version>v.source.version && !frame().image.rows.containsKey("combocount"));
        shot("combo-spent");
    }

    private static void crownInputs() throws Exception {
        await(()->{ PublicDungeon current=frame().dungeon; return current.offered && current.belongings.stream().anyMatch(i->current.actions(i).contains("USE_CROWN")); });
        PublicDungeon v=frame().dungeon;
        long id=v.belongings.stream().filter(i->v.actions(i).contains("USE_CROWN")).findFirst().get().id;
        click("bag");press(inventoryPoint(id));click("use_crown");
        String ability="#"+new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap().shortDesc();
        waitForButton(ability);shot("crown-choice");click(ability);
        click("#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseAbility.class,"yes"));
        await(()->frame().dungeon.belongings.stream().anyMatch(i->i.kind.equals("CLASS_ARMOR")));
        shot("crown-equipped");
        await(()->frame().dungeon.offered);
        PublicDungeon equipped=frame().dungeon;
        long armor=equipped.belongings.stream().filter(i->equipped.actions(i).contains("ARMOR_ABILITY")).findFirst().get().id;
        int charge=Integer.parseInt(equipped.source.rows.get("armorcharge"+armor));
        int tile=-1;
        for(int y=-1;y<=1 && tile<0;y++) for(int x=-1;x<=1;x++) {
            int cell=equipped.pos+y*equipped.width+x;
            int terrain=equipped.terrain(cell);
            if (cell!=equipped.pos && cell!=equipped.ally && terrain>=0 && equipped.visible(cell)
                    && !equipped.monsters.containsKey(cell) && !equipped.source.rows.containsKey("trap"+cell)
                    && (com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.flags[terrain]
                          & com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.PASSABLE)!=0) {
                tile=cell; break;
            }
        }
        if(tile<0) throw new AssertionError("Missing visible landing");
        final int destination=tile;
        click("bag");press(inventoryPoint(armor));click("armor_ability");
        shot("crown-aim");click("cancel");
        if(frame().image.version!=equipped.source.version) throw new AssertionError("Cancelled leap changed the game");
        click("bag");press(inventoryPoint(armor));click("armor_ability");
        press(mapPoint(equipped,destination));
        await(()->frame().dungeon.pos==destination && Integer.parseInt(frame().image.rows.get("armorcharge"+armor))<charge);
        shot("crown-leap");
    }

    private static void investVisibleTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent talent, int tier) throws Exception {
        String balance = "freepoints" + tier;
        String invested = "learn" + talent.ordinal();
        int before = Integer.parseInt(frame().image.rows.get(balance));
        int current = Integer.parseInt(frame().image.rows.get(invested));
        if (before < 1 || current < 0 || current >= talent.maxPoints()) throw new AssertionError("Unexpected fixture balance");
        openCharacter();
        click("#" + Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndHero.class, "talents"));
        click("#" + Messages.get(CoopScene.class, "tier_points", tier, before));
        String row = "#" + tier + " · " + Messages.titleCase(talent.title()) + "  " + current + "/" + talent.maxPoints();
        waitForButton(row);
        shot("available-" + talent.name().toLowerCase(java.util.Locale.ROOT));
        click(row);
        click("#" + Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoTalent.class, "upgrade"));
        await(() -> Integer.toString(current + 1).equals(frame().image.rows.get(invested)) && frame().pending == 0);
        if (Integer.parseInt(frame().image.rows.get(balance)) != before-1) throw new AssertionError("Tier balance mismatch");
        shot("invested-" + talent.name().toLowerCase(java.util.Locale.ROOT));
        System.out.println("PASS actual talent selection, receipt and tier balance " + talent.name() + " seat=" + frame().seat);
    }

    private static void selectorInputs() throws Exception {
        await(() -> frame().dungeon.offered && frame().dungeon.monsters.containsValue("Rat"));
        if (Boolean.getBoolean("spd.selector.talents")) {
            investVisibleTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SEARING_LIGHT, 1);
            investVisibleTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.ENLIGHTENING_MEAL, 2);
            investVisibleTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEROIC_ENERGY, 4);
        }
        PublicDungeon view = frame().dungeon;
        PublicDungeon.Loot tome = view.belongings.stream().filter(i -> view.actions(i).contains("CAST")).findFirst().get();
        Selector selector = selectorCase();
        String spell = selector.spell.name();
        String key = selector.key;
        if (Boolean.getBoolean("spd.selector.history")) {
            TrinityDiscoveries.Choice memory = discoveryChoice(view);
            if (Boolean.getBoolean("spd.selector.missile") && memory != TrinityDiscoveries.Choice.MAGIC_MISSILE)
                throw new AssertionError("Expected the cleric's magic missile discovery");
            String label = discoveryLabel(memory);
            if (view.belongings.stream().anyMatch(i -> view.source.rows.containsKey(key+i.id)))
                throw new AssertionError("Physical reference still in the bag");
            openSpellPicker(tome.id, spell);
            waitForButton(label);
            shot("discovered-selection");
            Robot keyboard = new Robot();
            keyboard.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
            keyboard.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
            await(() -> button(label) == null);
            if (frame().image.version != view.source.version || ownTome().charges != tome.charges)
                throw new AssertionError("Cancelling discovery selection spent a charge");
            openSpellPicker(tome.id, spell);
            click("#" + CoopScene.msg("bag"));
            await(CoopRenderSmoke::selectorOpen);
            keyboard.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
            keyboard.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
            await(() -> !selectorOpen());
            if (frame().image.version != view.source.version || ownTome().charges != tome.charges)
                throw new AssertionError("Leaving the nested inventory spent a charge");
            System.out.println("PASS nested inventory opened and cancelled from " + memory + " memory");
            openSpellPicker(tome.id, spell); click(label);
            await(() -> frame().image.version > view.source.version && "APPLIED".equals(frame().lastResult)
                    && frame().pending == 0 && ownTome().charges < tome.charges && frame().image.rows.containsKey("trinity"+memory.form.ordinal()));
            shot("discovered-committed");
            System.out.println("PASS discovery picker, cancel and native charge " + memory);
            return;
        }
        PublicDungeon.Loot target = view.belongings.stream().filter(i -> view.source.rows.containsKey(key+i.id)).findFirst().get();
        openSelector(tome.id, spell);
        shot("owned-selection");
        if (Boolean.getBoolean("spd.selector.resize")) {
            int width = render(() -> Gdx.graphics.getWidth()), height = render(() -> Gdx.graphics.getHeight());
            render(() -> Gdx.graphics.setWindowedMode(620, 800));
            await(() -> render(() -> Gdx.graphics.getHeight() > Gdx.graphics.getWidth()));
            render(() -> Gdx.graphics.setWindowedMode(width, height));
            await(() -> render(() -> Gdx.graphics.getWidth() == width && Gdx.graphics.getHeight() == height));
            if (frame().image != view.source || ownTome().charges != tome.charges)
                throw new AssertionError("Resize submitted a spell");
            if (!selectorOpen()) openSelector(tome.id, spell);
            System.out.println("PASS Desktop selector resize and restore");
        }
        Robot keyboard = new Robot();
        keyboard.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
        keyboard.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
        await(() -> !selectorOpen());
        if (frame().image != view.source) throw new AssertionError("Cancel changed authoritative state");

        // Invalidate the open menu through the actual client, then click the old handle.
        openSelector(tome.id, spell);
        long action = frame().lastAction;
        if (!hostController().act(view.source, "WAIT", 0, 0)) throw new AssertionError("Missing test offer");
        await(() -> frame().lastAction == action+1 && frame().dungeon.offered && frame().image != view.source);
        press(inventoryPoint(target.id));
        await(() -> !selectorOpen());
        PublicDungeon fresh = frame().dungeon;
        if (frame().pending != 0 || frame().lastAction != action+1 ||
                fresh.belongings.stream().noneMatch(i -> i.id==tome.id && i.charges==tome.charges))
            throw new AssertionError("Stale selector spent charge or submitted an action");

        PublicDungeon.Loot current = fresh.belongings.stream().filter(i -> fresh.source.rows.containsKey(key+i.id)).findFirst().get();
        openSelector(tome.id, spell);
        Point targetPoint = inventoryPoint(current.id);
        if (Boolean.getBoolean("spd.selector.doubleTap")) {
            Point screen = render(() -> {
                var window = ((com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics)Gdx.graphics).getWindow();
                return new Point(window.getPositionX()+targetPoint.x, window.getPositionY()+targetPoint.y);
            });
            Robot mouse = new Robot();
            mouse.mouseMove(screen.x, screen.y);
            for (int tap = 0; tap < 2; tap++) {
                mouse.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                mouse.delay(24);
                mouse.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                mouse.delay(32);
            }
        } else press(targetPoint);
        await(() -> {
            PublicDungeon next = frame().dungeon;
            return next.source.version > fresh.source.version && frame().lastAction == action+2 &&
                    "APPLIED".equals(frame().lastResult) && frame().pending == 0 &&
                    next.belongings.stream().anyMatch(i -> i.id==tome.id && i.charges<tome.charges);
        });
        shot("spell-committed");
        if (frame().lastAction != action+2 || frame().pending != 0)
            throw new AssertionError("A second action followed the selection");
        System.out.println("PASS selector cancel, stale handle, reselect and spend: "+regionShot);
    }

    private enum Selector {
        INTUITION("intuition", HolyIntuition.INSTANCE), RECALL("recallitem", RecallInscription.INSTANCE),
        BODY("bodyitem", BodyForm.INSTANCE), MIND("minditem", MindForm.INSTANCE), SPIRIT("spirititem", SpiritForm.INSTANCE);

        final String key;
        final ClericSpell spell;
        Selector(String key, ClericSpell spell) { this.key = key; this.spell = spell; }
    }

    private static Selector selectorCase() {
        return regionShot.equals("intuition-input") ? Selector.INTUITION :
                Selector.valueOf(regionShot.substring("selector-".length()).toUpperCase(java.util.Locale.ROOT));
    }

    private static PublicDungeon.Loot ownTome() throws Exception {
        return frame().dungeon.belongings.stream().filter(i -> i.kind.equals("TOME") && i.slot.equals("ARTIFACT"))
                .findFirst().orElseThrow();
    }

    private static void resumeSelectorHost(File root, Process previousGuest) throws Exception {
        int charged = ownTome().charges;
        click("menu"); click("saveexit"); await(() -> button("saves") != null);
        if (!previousGuest.waitFor(5, TimeUnit.SECONDS)) throw new AssertionError("Old guest still running");
        click("saves"); await(() -> frame().page == CoopController.Page.SAVES && frame().list.size() == 1);
        CoopController.Entry saved = frame().list.get(0);
        click("#"+saved.id.toString().substring(0,8)+"  "+Messages.get(CoopScene.class,"floor",saved.current.depth,saved.current.generation));
        click("resume"); await(() -> frame().dungeon != null);
        Process guest = new ProcessBuilder(System.getProperty("java.home")+"/bin/java", "-cp", System.getProperty("java.class.path"),
                CoopRenderSmoke.class.getName(), "guest", root.toString(), "resume").inheritIO().start();
        try {
            await(() -> frame().dungeon.offered);
            if (ownTome().charges != charged || frame().pending != 0) throw new AssertionError("Host charge or receipt changed on resume");
            waitAndRegainTurn();
            shot("host-restored-next-action");
            System.out.println("PASS host save, two seats resumed and next action: "+regionShot);
        } finally {
            guest.destroy();
            if (!guest.waitFor(5, TimeUnit.SECONDS)) guest.destroyForcibly();
        }
    }

    private static boolean selectorOpen() throws Exception {
        return render(() -> find(Game.scene(), node -> node instanceof WndSeatInventory) != null);
    }

    private static TrinityDiscoveries.Choice discoveryChoice(PublicDungeon view) {
        return Arrays.stream(TrinityDiscoveries.Choice.values())
                .filter(choice -> view.source.rows.containsKey(choice.rowKey())).findFirst().orElseThrow();
    }

    private static String discoveryLabel(TrinityDiscoveries.Choice choice) {
        return "#" + Messages.titleCase(Messages.get(choice.effect, "name", "").trim());
    }

    private static void openSpellPicker(long tomeId, String spell) throws Exception {
        click("bag");
        press(inventoryPoint(tomeId));
        click("cast");
        click("#"+spell);
    }

    private static void openSelector(long tomeId, String spell) throws Exception {
        openSpellPicker(tomeId, spell);
        await(CoopRenderSmoke::selectorOpen);
    }

    private static void sealInputs() throws Exception {
        boolean seal = regionShot.equals("seal-input");
        String action = seal ? "DETACH_SEAL" : "THROW";
        await(() -> frame().dungeon.offered && frame().dungeon.monsters.containsValue("Rat"));
        PublicDungeon view = frame().dungeon;
        PublicDungeon.Loot item = view.belongings.stream().filter(i -> view.actions(i).contains(action)
                && (seal || i.kind.equals("FOOD"))).findFirst().get();
        click("bag");
        press(inventoryPoint(item.id));
        String button = seal ? "detach_seal" : "throw";
        waitForButton(button);
        shot("item-menu");
        click(button);
        if (!seal) {
            shot("ration-aim");
            int target = view.monsters.entrySet().stream().filter(e -> e.getValue().equals("Rat")).findFirst().get().getKey();
            press(mapPoint(view,target));
        }
        await(() -> {
            PublicDungeon next = frame().dungeon;
            return next.source.version > view.source.version && (!seal ||
                    next.belongings.stream().anyMatch(i -> next.actions(i).contains("AFFIX_SEAL")));
        });
        shot("after-action");
        if (seal) {
            PublicDungeon latest = frame().dungeon;
            PublicDungeon.Loot loose = latest.belongings.stream().filter(i -> latest.actions(i).contains("AFFIX_SEAL")).findFirst().get();
            click("bag");
            press(inventoryPoint(loose.id));
            String title = com.shatteredpixel.shatteredpixeldungeon.messages.Messages.titleCase(
                    com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get("items.brokenseal.name"));
            await(() -> render(() -> find(Game.scene(), node -> node instanceof com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock
                    && title.equals(((com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock)node).text())) != null));
            shot("loose-seal-name");
        }
    }

    private static void frostInputs() throws Exception {
        boolean spell = regionShot.equals("sunray-input");
        String kind = spell ? System.getProperty("spd.seat.spell", "sunray") : "frost";
        String action = spell ? "CAST" : "ZAP";
        await(() -> {
            PublicDungeon current = frame().dungeon;
            return current.offered && current.monsters.containsValue("Rat")
                    && current.belongings.stream().anyMatch(i -> current.actions(i).contains(action));
        });
        PublicDungeon view = frame().dungeon;
        PublicDungeon.Loot item = view.belongings.stream().filter(i -> view.actions(i).contains(action)
                && (spell || i.kind.equals("STAFF"))).findFirst().get();
        int target = view.monsters.entrySet().stream().filter(e -> e.getValue().equals("Rat")).findFirst().get().getKey();
        click("bag"); press(inventoryPoint(item.id)); click(spell ? "cast" : "zap");
        if (spell) {
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell selected;
            switch (kind) {
            case "paladinheal": selected = LayOnHands.INSTANCE; break;
            case "paladinaura": selected = AuraOfProtection.INSTANCE; break;
            case "prayer": selected = MnemonicPrayer.INSTANCE; break;
            case "hallow": selected = HallowedGround.INSTANCE; break;
            case "wall": selected = WallOfLight.INSTANCE; break;
            case "holylance": selected = com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance.INSTANCE; break;
            case "cleanse": selected = com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse.INSTANCE; break;
            case "sunray": selected = com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Sunray.INSTANCE; break;
            default: throw new IllegalArgumentException(kind);
            }
            String name = "#" + selected.name();
            waitForButton(name); shot("learned-spell"); click(name);
        }
        if (kind.equals("paladinheal") || kind.equals("hallow")) target=view.pos;
        if (kind.equals("wall")) target=view.pos+1;
        if (!kind.equals("cleanse") && !kind.equals("paladinaura")) {
            shot(kind + "-aim");
            press(mapPoint(view,target));
        }
        await(() -> frame().image.version > view.source.version && frame().dungeon.belongings.stream()
                .anyMatch(i -> i.id == item.id && i.charges < item.charges));
        PublicDungeon current = frame().dungeon;
        if (kind.equals("cleanse") && current.shield <= view.shield) throw new AssertionError("Missing cleanse barrier");
        int spent = item.charges - current.belongings.stream().filter(i -> i.id == item.id).findFirst().orElseThrow().charges;
        if ((kind.equals("holylance") && spent != 4) || (kind.equals("cleanse") && spent != 2)) throw new AssertionError("Unexpected tome cost " + spent);
        int expected = kind.equals("wall") ? 3 : kind.equals("hallow") || kind.equals("paladinaura") ? 2 : kind.equals("paladinheal") || kind.equals("prayer") ? 1 : spent;
        if (spent!=expected) throw new AssertionError(kind+" tome cost "+spent);
        shot(kind + "-hit");
        System.out.println("PASS actual " + kind + " charges " + item.charges + " to " + (item.charges-spent));
    }

    private static void starterSpellInputs(boolean rapier) throws Exception {
        String action=rapier?"WEAPON_ABILITY":"CAST";
        await(()->{ PublicDungeon v=frame().dungeon; return v.offered && v.monsters.containsValue("Rat") && v.belongings.stream().anyMatch(i->v.actions(i).contains(action)); });
        PublicDungeon view=frame().dungeon;
        PublicDungeon.Loot source=view.belongings.stream().filter(i->view.actions(i).contains(action)).findFirst().get();
        int target=view.monsters.entrySet().stream().filter(e->"Rat".equals(e.getValue())).findFirst().get().getKey();
        click("bag");press(inventoryPoint(source.id));click(rapier?"weapon_ability":"cast");
        if (!rapier) {
            String light="#"+com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.INSTANCE.name();
            waitForButton(light);
            shot("holy-tome-spells");
            click(light);
        }
        shot(rapier?"rapier-aim":"guiding-light-aim");
        press(mapPoint(view,target));
        await(()->frame().image.version>view.source.version && frame().dungeon.belongings.stream().anyMatch(i->i.id==source.id && i.charges<source.charges));
        if (rapier && frame().dungeon.pos==view.pos) throw new AssertionError("Lunge did not move");
        shot(rapier?"rapier-strike":"guiding-light-hit");
    }

    private static void armorSpellInputs(boolean hawk) throws Exception {
        await(()->{ PublicDungeon v=frame().dungeon; return v.offered && v.belongings.stream().anyMatch(i->v.actions(i).contains("ARMOR_ABILITY")); });
        PublicDungeon before=frame().dungeon;
        long armor=before.belongings.stream().filter(i->before.actions(i).contains("ARMOR_ABILITY")).findFirst().get().id;
        String chargeKey="armorcharge"+armor;
        int initial=Integer.parseInt(before.source.rows.get(chargeKey));
        click("bag");press(inventoryPoint(armor));click("armor_ability");
        if (!hawk) {
            int cell=before.monsters.entrySet().stream().filter(e->e.getValue().equals("Rat")).findFirst().get().getKey();
            shot("wild-magic-aim");
            press(mapPoint(before,cell));
        }
        await(()->Integer.parseInt(frame().image.rows.get(chargeKey))<initial);
        shot(hawk?"summoned-hawk":"wild-magic-applied");
        if (hawk) {
            await(()->frame().dungeon.offered && frame().image.rows.containsKey("hawkcommand"));
            PublicDungeon active=frame().dungeon;
            int charge=Integer.parseInt(active.source.rows.get(chargeKey));
            click("bag");press(inventoryPoint(armor));click("armor_ability");
            shot("hawk-command-aim");
            press(mapPoint(active,active.pos));
            await(()->frame().image.version>active.source.version);
            if (Integer.parseInt(frame().image.rows.get(chargeKey))!=charge) throw new AssertionError("command spent armor charge");
            shot("hawk-command-applied");
        }
    }

    private static void wandInputs() throws Exception {
        await(()->frame().dungeon.offered && frame().image.rows.keySet().stream().anyMatch(k->k.startsWith("wandchoice")));
        PublicDungeon v=frame().dungeon;
        long wand=v.belongings.stream().filter(i->v.source.rows.containsKey("wandchoice"+i.id)).findFirst().get().id;
        long staff=v.belongings.stream().filter(i->v.actions(i).contains("IMBUE")).findFirst().get().id;
        String yes="#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff.class,"yes");
        String no="#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff.class,"no");
        for (boolean confirm : new boolean[]{false,true}) {
            click("bag"); press(inventoryPoint(staff)); click("imbue");
            if (!confirm) shot("own-wand-picker");
            press(inventoryPoint(wand)); waitForButton(yes);
            if (!confirm) shot("confirm-native-imbuement");
            click(confirm?yes:no);
            if (!confirm && frame().image.version!=v.source.version) throw new AssertionError("Cancelled choice spent state");
        }
        await(()->frame().image.version>v.source.version && !frame().image.rows.containsKey("wandchoice"+wand));
        shot("imbuement-applied");
    }

    private static void upperInputs() throws Exception {
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent cleave=com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.CLEAVE;
        await(()->frame().dungeon.offered && frame().image.rows.containsKey("learn"+cleave.ordinal()));
        openCharacter();
        click("#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndHero.class,"talents"));
        String tier="#"+Messages.get(CoopScene.class,"tier_points",3,Integer.parseInt(frame().image.rows.get("freepoints3")));
        waitForButton(tier); shot("upper-tier-balances"); click(tier);
        String ability="#3 · "+Messages.titleCase(cleave.title())+"  0/"+cleave.maxPoints();
        waitForButton(ability); shot("gladiator-talents"); click(ability);
        String invest="#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoTalent.class,"upgrade");
        waitForButton(invest); shot("cleave-invest"); click(invest);
        await(()->"1".equals(frame().image.rows.get("learn"+cleave.ordinal())));
        shot("cleave-confirmed");
    }

    private static void growthInputs() throws Exception {
        await(()->{ PublicDungeon v=frame().dungeon; return v.offered && v.belongings.stream().anyMatch(i->v.actions(i).contains("USE_MASK")); });
        openCharacter();
        click("#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndHero.class,"talents"));
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent food=com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HEARTY_MEAL;
        click("#"+Messages.get(CoopScene.class,"tier_points",1,Integer.parseInt(frame().image.rows.get("freepoints1"))));
        click("#1 · "+Messages.titleCase(food.title())+"  0/"+food.maxPoints());
        String upgrade="#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoTalent.class,"upgrade");
        waitForButton(upgrade);shot("native-talent");click(upgrade);
        await(()->"1".equals(frame().image.rows.get("learn"+food.ordinal())));
        PublicDungeon v=frame().dungeon;
        long mask=v.belongings.stream().filter(i->v.actions(i).contains("USE_MASK")).findFirst().get().id;
        click("bag");press(inventoryPoint(mask));click("use_mask");
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass way=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BERSERKER;
        String choice="#"+way.shortDesc();
        waitForButton(choice);shot("native-mask");click(choice);
        click("#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseSubclass.class,"yes"));
        await(()->way.name().equals(frame().image.rows.get("subclass")));
    }

    private static void exercise() throws Exception {
        await(()->Gdx.app!=null && render(()->Game.scene()!=null));
        render(()->{ShatteredPixelDungeon.switchNoFade(TitleScene.class);return null;});
        await(()->button("solo")!=null);
        shot("title"); click("solo");
        await(()->render(()->Game.scene() instanceof HeroSelectScene || Game.scene() instanceof StartScene));
        shot("solo-entry");
        render(()->{ShatteredPixelDungeon.switchNoFade(TitleScene.class);return null;});
        String guestProfile = System.getProperty("spd.selector.guestProfile");
        int testPort;
        try (java.net.ServerSocket port=new java.net.ServerSocket(0)) { testPort=port.getLocalPort(); }
        render(()->{
            Field active=CoopScene.class.getDeclaredField("active"); active.setAccessible(true);
            active.set(null,new CoopController(guestProfile == null ? CoopPlatform.root() : new File(guestProfile, "seat1"),null,null,testPort,0,
                    new java.util.Random(){@Override public long nextLong(){return !regionShot.isEmpty() ? ((regionShot.endsWith("-input") || regionShot.startsWith("ascent-"))?578:regionShot.startsWith("yog-")?572:regionShot.equals("mine-crystal")?570:(regionShot.startsWith("vault-") || regionShot.equals("imp-vault") || regionShot.equals("halls-21") || regionShot.equals("city-market") || regionShot.startsWith("king-"))?572:571) : latePrison ? 570 : wandmaker ? 577 : seventh ? 581 : deep ? 402 : armored ? 446 : unstable ? 421 : statue ? 416 : golden ? 427 : crystal ? 411 : mimic ? 408 : ghost ? 403 : floors && !sentry ? 415 : 402;}}));
            return null;
        });
        click("title"); await(()->button("host")!=null);
        if (guestProfile != null) {
            if (!regionShot.startsWith("selector-")) throw new IllegalArgumentException("Expected a selector scenario");
            guestSelectors(new File(guestProfile), testPort);
            return;
        }
        sceneMetrics("before-room");
        shot("multiplayer"); click("host"); await(()->button("warriorClass")!=null);
        shot("lobby");
        String upperTalent = System.getProperty("spd.upper.talent", "STRONGMAN");
        if (regionShot.equals("strength-input")) {
            var subclass = com.shatteredpixel.shatteredpixeldungeon.SharedGrowthTest.upperSubclass(upperTalent);
            for (var heroClass : com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.values()) {
                if (java.util.Arrays.asList(heroClass.subClasses()).contains(subclass)) {
                    click(heroClass.name().toLowerCase(java.util.Locale.ROOT) + "Class");
                    break;
                }
            }
        } else click(regionShot.equals("cache-input")?"rogueClass":regionShot.equals("lunge-input")?"duelistClass":(regionShot.equals("tome-input") || regionShot.equals("recharging-input") || regionShot.startsWith("selector-") || regionShot.equals("intuition-input") || regionShot.equals("sunray-input"))?"clericClass":kit || regionShot.equals("scroll-enchant-input") || regionShot.equals("eagle-input") || regionShot.equals("bow-enchant-input") || regionShot.equals("hawk-input")?"huntressClass":mage?"mageClass":"warriorClass");
        if(mage) shot("mage-selected");
        File root=java.nio.file.Files.createTempDirectory("spd-render-seat").toFile();
        java.nio.file.Files.writeString(new File(root,"port").toPath(),Integer.toString(testPort));
        if(regionShot.startsWith("yog-") || regionShot.startsWith("ascent-")) java.nio.file.Files.createFile(new File(root,"yog-room").toPath());
        if(kit) java.nio.file.Files.createFile(new File(root,"kit").toPath());
        if(floors) java.nio.file.Files.createFile(new File(root,"stairs").toPath());
        Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),
                CoopRenderSmoke.class.getName(),"guest",root.toString()).inheritIO().start();
        try {
            click("ready"); await(()->button("start")!=null); shot("ready"); click("start");
            await(()->frame().dungeon!=null); await(()->button("bag")!=null);
            if (!regionShot.isEmpty()) {
                RenderEncounter.tengu(hostController(),regionShot);
                if (regionShot.equals("vault-dart")) { vaultDart(); return; }
                if (regionShot.equals("growth-input")) { growthInputs(); return; }
                if (regionShot.equals("cache-input") || regionShot.equals("strength-input")) {
                    boolean cache = regionShot.equals("cache-input");
                    var talent = cache ? com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.CACHED_RATIONS
                            : com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.valueOf(upperTalent);
                    await(() -> frame().dungeon.offered && frame().image.rows.containsKey("learn"+talent.ordinal())
                            && Integer.parseInt(frame().image.rows.get("freepoints"+(cache ? 1 : 3))) > 0);
                    investVisibleTalent(talent,cache ? 1 : 3);
                    return;
                }
                if (regionShot.equals("scroll-enchant-input")) { enchantScrollInput(); return; }
                if (regionShot.equals("eagle-input")) {
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent talent=com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.EAGLE_EYE;
                    await(()->frame().dungeon.offered && frame().image.rows.containsKey("learn"+talent.ordinal()));
                    investVisibleTalent(talent,4);
                    return;
                }
                if (regionShot.equals("bow-enchant-input")) {
                    await(() -> frame().dungeon.offered && frame().dungeon.belongings.stream().anyMatch(i -> i.kind.equals("ENCHANT_BOW")));
                    var initial = frame().dungeon;
                    var stone = initial.belongings.stream().filter(i -> i.kind.equals("ENCHANT_BOW")).findFirst().get();
                    var bow = initial.belongings.stream().filter(i -> i.kind.equals("BOW")).findFirst().get();
                    click("bag"); openInventoryTab(stone.bagIndex); press(inventoryPoint(stone.id)); click("enchant_bow");
                    await(() -> selectorOpen());
                    java.awt.Robot keyboard = new java.awt.Robot();
                    keyboard.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
                    keyboard.keyRelease(java.awt.event.KeyEvent.VK_ESCAPE);
                    await(() -> !selectorOpen());
                    if (frame().image.version != initial.source.version) throw new AssertionError("Cancel changed state");
                    click("bag"); openInventoryTab(stone.bagIndex); press(inventoryPoint(stone.id)); click("enchant_bow");
                    await(() -> selectorOpen());
                    openInventoryTab(bow.bagIndex);
                    shot("choose-own-bow");
                    press(inventoryPoint(bow.id));
                    await(() -> frame().pending == 0 && "APPLIED".equals(frame().lastResult)
                            && frame().dungeon.belongings.stream().noneMatch(i -> i.id == stone.id));
                    if (frame().dungeon.belongings.stream().noneMatch(i -> i.id == bow.id)) throw new AssertionError("Bow identity");
                    shot("after-enchantment");
                    System.out.println("PASS bow enchantment bag choice, cancel, selection, consumption and physical bow");
                    return;
                }
                if (regionShot.equals("recharging-input")) {
                    await(() -> frame().dungeon.belongings.stream().anyMatch(i -> i.kind.equals("RECHARGING_SCROLL")));
                    var scroll=frame().dungeon.belongings.stream().filter(i -> i.kind.equals("RECHARGING_SCROLL")).findFirst().get();
                    click("bag"); press(inventoryPoint(scroll.id)); click("read_recharging");
                    await(() -> frame().image.rows.containsKey("recallrecharging") && frame().dungeon.offered);
                    shot("read-recharging");
                    int initial=ownTome().charges;
                    openSpellPicker(ownTome().id,RecallInscription.INSTANCE.name());
                    await(() -> !frame().image.rows.containsKey("recallrecharging") && frame().pending == 0);
                    if (ownTome().charges != initial-3) throw new AssertionError("Recall charge");
                    shot("recalled-recharging");
                    System.out.println("PASS actual read, recall of recharging, consumed imprint and three tome charges");
                    return;
                }
                if (regionShot.equals("upper-input")) { upperInputs(); return; }
                if (regionShot.equals("intuition-input") || regionShot.startsWith("selector-")) {
                    selectorInputs();
                    if (regionShot.startsWith("selector-")) resumeSelectorHost(root, child);
                    return;
                }
                if (regionShot.equals("seal-input") || regionShot.equals("ration-input")) { sealInputs(); return; }
                if (regionShot.equals("frost-input") || regionShot.equals("sunray-input")) { frostInputs(); return; }
                if (regionShot.equals("lunge-input") || regionShot.equals("tome-input")) { starterSpellInputs(regionShot.equals("lunge-input")); return; }
                if (regionShot.equals("hawk-input") || regionShot.equals("volley-input")) { armorSpellInputs(regionShot.equals("hawk-input")); return; }
                if (regionShot.equals("wand-input")) { wandInputs(); return; }
                if (regionShot.equals("combo-input")) { comboInputs(); return; }
                if (regionShot.equals("crown-input")) { crownInputs(); return; }
                if (regionShot.equals("smith-services")) {
                    await(()->frame().dungeon.offered && frame().image.rows.containsKey("priceorder"));
                    PublicDungeon smithView=frame().dungeon;
                    int smith=smithView.monsters.entrySet().stream().filter(e->e.getValue().equals("Blacksmith")).findFirst().get().getKey();
                    String order="#"+CoopScene.msg("service_order",smithView.source.rows.get("priceorder"));
                    press(mapPoint(smithView,smith));
                    await(()->button(order)!=null);
                    shot("smith-services");
                    click(order);
                    await(()->frame().image.rows.containsKey("smithoffer0"));
                    PublicDungeon offers=frame().dungeon;
                    press(mapPoint(offers,smith));
                    String first="#"+Messages.get(offers.source.rows.get("smithoffer0")+".name");
                    await(()->button(first)!=null);
                    shot("smith-equipment");
                    if (!offers.offered) { click("cancel"); waitAndRegainTurn();press(mapPoint(frame().dungeon,smith)); }
                    click(first);
                    await(()->!frame().image.rows.containsKey("smithoffer0"));
                    return;
                }
                if (regionShot.equals("ascent-vote")) {
                    await(()->"ASCENT_PROPOSED".equals(frame().image.rows.get("ending")));
                } else if (regionShot.equals("yog-finish") || regionShot.equals("ascent-surface")) {
                    await(()->"WON".equals(frame().image.rows.get("status")));
                } else await(()->frame().dungeon!=null && (regionShot.startsWith("yog-") ? frame().dungeon.depth==(regionShot.equals("yog-amulet")?26:25) : regionShot.equals("halls-21") ? frame().dungeon.depth==21 : regionShot.startsWith("king-") ? frame().dungeon.depth==20 : regionShot.equals("city-market") ? frame().dungeon.depth==16
                        : frame().dungeon.branch==1));
                if (regionShot.equals("yog-amulet")) {
                    waitForButton("end_vote");
                    shot(regionShot);
                    click("end_vote");
                    await(()->"WON".equals(frame().image.rows.get("status")));
                    shot("yog-victory-input");
                } else shot(regionShot);
                return;
            }
            sceneMetrics("first-floor");
            shot("dungeon");
            if (!mage && !hazards && !kit && !garden) {
                openCharacter();
                waitForButton("#"+Messages.get(com.shatteredpixel.shatteredpixeldungeon.windows.WndHero.class,"talents"));
                shot("character");
                render(()->{((com.shatteredpixel.shatteredpixeldungeon.ui.Window)find(Game.scene(),
                        g->g instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions)).hide();return null;});
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
            if (scribe) {
                if (knife) {
                    RenderEncounter.arrange(hostController());
                    RenderEncounter.knives(hostController()); waitAndRegainTurn();
                    PublicDungeon initial=frame().dungeon;
                    PublicDungeon.Loot item=initial.belongings.stream().filter(i->i.kind.equals("KNIFE")).findFirst().get();
                    hold("slot4"); press(inventoryPoint(item.id));
                    await(()->frame().quickslots.item(3,frame().dungeon)!=null && frame().quickslots.item(3,frame().dungeon).kind.equals("KNIFE"));
                    click("slot4"); await(()->button("cancel")!=null); shot("knife-aim");
                    int rat=initial.monsters.entrySet().stream().filter(e->e.getValue().equals("Rat")).findFirst().get().getKey();
                    press(mapPoint(initial,rat));
                    await(()->frame().dungeon.belongings.stream().anyMatch(i->i.kind.equals("KNIFE") && i.quantity==item.quantity-1));
                    shot("knife-after-throw");
                    return;
                }
                RenderEncounter.scrolls(hostController()); click("wait");
                await(()->frame().dungeon.offered && frame().dungeon.belongings.stream().anyMatch(x->x.kind.equals("IDENTIFY")));
                for (String verb:new String[]{"IDENTIFY","UPGRADE"}) {
                    PublicDungeon view=frame().dungeon;
                    PublicDungeon.Loot gear=view.belongings.stream().filter(x->x.slot.equals("BAG") && x.kind.equals("ARMOR")).findFirst().get();
                    bagAction(verb,verb.toLowerCase(java.util.Locale.ROOT));
                    shot(verb.toLowerCase(java.util.Locale.ROOT)+"-target");
                    render(()->{((WndSeatInventory)find(Game.scene(),g->g instanceof WndSeatInventory)).onBackPressed();return null;});
                    if(frame().image!=view.source) throw new AssertionError("Cancelled equipment selection spent a turn");
                    String slot=verb.equals("UPGRADE") ? "slot2" : "slot1";
                    click(slot); press(inventoryPoint(verb));
                    int index=verb.equals("UPGRADE") ? 1 : 0;
                    await(()->frame().quickslots.item(index,frame().dungeon)!=null);
                    click(slot);
                    press(inventoryPoint(gear.id));
                    await(()->frame().dungeon.offered && frame().dungeon.belongings.stream().anyMatch(x->
                            x.slot.equals("BAG") && x.kind.equals("ARMOR") && x.upgrade.equals(verb.equals("UPGRADE")?"1":"0")));
                }
                click("slot4"); press(inventoryPoint("STONE"));
                await(()->frame().quickslots.item(3,frame().dungeon)!=null);
                PublicDungeon view=frame().dungeon;
                click("slot4"); await(()->button("cancel")!=null); shot("stone-target");
                int cell=-1;
                for (int off:new int[]{1,-1,view.width,-view.width}) {
                    int at=view.pos+off;
                    if (view.visible(at) && view.terrain(at)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY
                            && view.ally!=at && !view.monsters.containsKey(at)) { cell=at; break; }
                }
                if (cell<0) throw new AssertionError("No visible landing cell");
                press(mapPoint(view,cell));
                final int landing=cell;
                await(()->frame().dungeon.floor.containsKey(landing)); shot("stone-landing");
                System.out.println("PASS selected scroll targets, cancelled without an action, threw an owned quickslot stone");
                return;
            }
            if(kit) {
                RenderEncounter.healing(hostController());
                waitAndRegainTurn(); await(()->frame().dungeon.offered && frame().dungeon.floor.containsKey(frame().dungeon.pos));
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
                waitAndRegainTurn(); await(()->frame().dungeon.offered && frame().dungeon.monsters.containsValue("Rat"));
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
            sceneMetrics("after-move");
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
            sceneMetrics("after-save-exit");
        } finally {
            if(child.isAlive()) child.destroy(); if(!child.waitFor(5,TimeUnit.SECONDS)) child.destroyForcibly();
        }
    }
}

