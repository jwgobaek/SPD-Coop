package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.noosa.*;
import com.watabou.utils.RectF;
import java.util.*;

/** Persistent compact HUD. Commands use the exact StateImage from which the controls were drawn. */
final class CoopHud extends Group {
    private final CoopScene scene;
    private final CoopController client;
    private final Image portrait = new Image();
    private final Image hp = new Image(Assets.Interfaces.STATUS,0,40,50,4);
    private final Image shield = new Image(Assets.Interfaces.STATUS,0,44,50,4);
    private final Image xp = new Image(Assets.Interfaces.STATUS,0,48,17,4);
    private final BitmapText health = new BitmapText(PixelScene.pixelFont);
    private final BitmapText level = new BitmapText(PixelScene.pixelFont);
    private final BitmapText floor = new BitmapText(PixelScene.pixelFont);
    private final RenderedTextBlock turn = PixelScene.renderTextBlock("",6);
    private final Group effects = new Group();
    private final List<BuffIcon> icons = new ArrayList<>();
    private final StyledButton stairs, cancel;
    private final Toolbar.SeatTool wait,search,pickup,bag;
    private final Toolbar.SeatTool[] quick = new Toolbar.SeatTool[4];
    private final PublicDungeon.Loot[] items = new PublicDungeon.Loot[4];
    private SeatQuickslots layout;
    private List<Integer> lastBuffs = Collections.emptyList();
    private String lastPortrait, lastLine;
    private final float left,top,right,bottom;
    private PublicDungeon view;
    private PublicDungeon restStart;
    private int restRemaining;
    private long sentWait;
    private float nextRest;
    void stopRest() { restRemaining=0; restStart=null; }
    private boolean canRest(PublicDungeon v) {
        if (!v.offered || v.hp>=v.maxHp || !"ALIVE".equals(v.source.rows.get("life"))
                || "1".equals(v.source.rows.get("hazard")) || v.source.rows.containsKey("cloud"+v.pos)
                || v.source.rows.containsKey("stench"+v.pos)) return false;
        for (String mob:v.monsters.values())
            if (!mob.equals("Ghost") && !mob.equals("Sheep") && !mob.equals("RatKing")) return false;
        return true;
    }
    private void rest() {
        if (restRemaining>0) { stopRest(); return; }
        if (view==null || client.busy() || !canRest(view)) return;
        restRemaining=20;
        sendWait(view);
    }
    private void sendWait(PublicDungeon v) {
        if (!client.act(v.source,"WAIT",0,0)) { stopRest(); return; }
        restStart=v; sentWait=v.source.version; restRemaining--; nextRest=0.3f;
    }
    @Override public void update() {
        super.update();
        if (restRemaining<=0 || restStart==null) return;
        CoopController.Frame frame=client.frame();
        if (frame.page!=CoopController.Page.GAME || frame.dungeon==null || !frame.dungeon.offered) { stopRest(); return; }
        if (scene.hasOpenWindow()) { stopRest(); return; }
        nextRest-=Game.elapsed;
        if (client.busy() || frame.pending!=0 || nextRest>0 || view!=frame.dungeon || view.source.version==sentWait) return;
        if (view.source.version!=sentWait+1 || !"APPLIED".equals(frame.lastResult) || !"WAIT".equals(frame.pendingVerb)
                || !canRest(view) || (view.depth!=restStart.depth || view.branch!=restStart.branch) || view.pos!=restStart.pos || view.hp<restStart.hp
                || !restStart.buffs.containsAll(view.buffs)) { stopRest(); return; }
        sendWait(view);
    }

    CoopHud(CoopScene scene,CoopController client,RectF inset) {
        this.scene=scene; this.client=client;
        left=inset.left; top=inset.top; right=Camera.main.width-inset.right; bottom=Camera.main.height-inset.bottom;
        NinePatch bg=StatusPane.background(false);
        bg.x=left; bg.y=top; add(bg);
        add(portrait); add(shield); add(hp); add(xp); add(health); add(level); add(effects);
        hp.x=shield.x=left+30; hp.y=shield.y=top+2; xp.x=left+2; xp.y=top+30;
        health.x=hp.x+1; health.y=hp.y; health.scale.set(0.5f); health.alpha(0.8f);
        level.hardlight(0xFFFFAA);
        Button info=new Button(){ @Override protected void onClick() {
            if(view!=null) scene.add(new WndTitledMessage(HeroSprite.avatar(HeroClass.valueOf(view.heroClass),tier()),
                    HeroClass.valueOf(view.heroClass).title(),"HP "+view.hp+"/"+view.maxHp+"\n"+
                    characterInfo()));
        }};
        info.setRect(left,top,82,35);add(info);
        Image menuBase=new Image(Assets.Interfaces.MENU,1,0,31,21);
        menuBase.x=right-31;menuBase.y=top;add(menuBase);
        Image menuIcon=new Image(Assets.Interfaces.MENU_BTN,17,2,12,11);
        menuIcon.x=right-13;menuIcon.y=top+8;add(menuIcon);
        Button menu=new MenuButton();
        menu.setRect(right-31,top,31,21);add(menu);
        floor.x=right-26;floor.y=top+7;add(floor);
        turn.setPos(left+33,top+21);add(turn);

        float x=right;
        bag=tool("bag",0,24,160,()->scene.openBag(view));
        bag.setPos(x-=24,bottom-26);
        search=tool("search",44,20,192,()->client.act(view.source,"SEARCH",view.pos,0));
        search.setPos(x-=20,bottom-26);
        wait=tool("wait",24,20,176,()->{
            if (restRemaining>0) stopRest();
            else client.act(view.source,"WAIT",0,0);
        });
        wait.onHold(this::rest);
        wait.setPos(x-=20,bottom-26);
        for(int i=quick.length-1;i>=0;i--) {
            final int slot=i;
            quick[i]=tool("slot"+(i+1),64,22,-1,()->use(slot));
            quick[i].onHold(()->edit(slot));
            quick[i].setPos(x-=22,bottom-24);
        }
        pickup=tool("pick_up",64,22,-1,()->view.tap(client,view.pos));
        pickup.setPos(x-22,bottom-24);
        float width=Math.min(180,right-left-8);
        stairs=new RedButton("") { @Override protected void onClick() {
            scene.offerStairs(view,view.stair);
        }};
        stairs.setRect((left+right-width)/2,top+38,width,16);add(stairs);
        cancel=new RedButton(CoopScene.msg("cancel")) { @Override protected void onClick(){scene.cancelAim();} };
        cancel.setRect((left+right-width)/2,bottom-42,width,16);add(cancel);
    }

    private Toolbar.SeatTool tool(String name,int x,int w,int icon,Runnable action) {
        Toolbar.SeatTool button=new Toolbar.SeatTool(name,x,w,icon,action);
        add(button);return button;
    }

    private String characterInfo() {
        StringBuilder text=new StringBuilder(CoopScene.msg("level",view.level,view.experience))
                .append("/").append(Hero.maxExp(view.level)).append("\n").append(CoopScene.msg("depth",view.depth));
        if (view.source.rows.containsKey("strength")) text.append("\n").append(CoopScene.msg("own_strength",view.source.rows.get("strength")));
        String sub=view.source.rows.get("subclass");
        if (sub!=null && !sub.equals("NONE")) text.append("\n").append(com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.valueOf(sub).title());
        for (Map.Entry<String,String> row:view.source.rows.entrySet()) if (row.getKey().startsWith("talent")) {
            String[] parts=row.getValue().split(",");
            text.append("\n").append(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.valueOf(parts[0]).title()).append(" +").append(parts[1]);
        }
        return text.toString();
    }
    private void edit(int slot) {
        if (view==null || layout==null || client.frame().image!=view.source) return;
        final PublicDungeon shown=view;
        if (!layout.assigned(slot)) {
            scene.add(WndSeatInventory.chooseShortcut(shown,client,slot));
            return;
        }
        scene.add(new WndOptions(CoopScene.msg("choose_shortcut",slot+1),"",
                CoopScene.msg("replace_shortcut"),CoopScene.msg("clear_shortcut"),CoopScene.msg("cancel")) {
            protected void onSelect(int choice) {
                if (choice==0) scene.add(WndSeatInventory.chooseShortcut(shown,client,slot));
                if (choice==1) client.bindShortcut(shown.source,slot,null);
            }
        });
    }

    private void use(int slot) {
        if (layout==null || !layout.assigned(slot)) { edit(slot); return; }
        PublicDungeon.Loot item=items[slot];
        if(item==null) return;
        scene.useItem(view,item,PublicDungeon.shortcutAction(item));
    }

    private final class MenuButton extends Button {
        @Override protected void onClick() { scene.gameMenu(); }
    }

    private int tier() { return Integer.parseInt(view.source.rows.containsKey("selftier") ? view.source.rows.get("selftier") : "0"); }

    void apply(PublicDungeon next, boolean confirmed, String line, boolean aiming) {
        view=next;
        String appearance=view.heroClass+tier();
        if (!appearance.equals(lastPortrait)) {
            portrait.copy(HeroSprite.avatar(HeroClass.valueOf(view.heroClass),tier()));
            portrait.x=left+15-portrait.width()/2; portrait.y=top+16-portrait.height()/2;
            PixelScene.align(portrait);lastPortrait=appearance;
        }
        float total=Math.max(view.maxHp,(float)view.hp+view.shield);
        hp.scale.x=view.hp/total;shield.scale.x=(view.hp+view.shield)/total;
        xp.scale.x=(float)view.experience/Hero.maxExp(view.level);
        health.text(view.hp+(view.shield==0?"":"+"+view.shield)+"/"+view.maxHp);
        level.text(String.valueOf(view.level));level.measure();level.x=left+22-level.width()/2;level.y=top+27;
        floor.text(String.valueOf(view.depth));
        if(!line.equals(lastLine)) {
            turn.text(line);turn.hardlight(view.offered ? 0xB4DA80 : 0xCBC6BA);lastLine=line;
        }
        if(!lastBuffs.equals(view.buffs)) {
            for (BuffIcon icon:icons) icon.destroy();
            icons.clear();
            effects.clear();
            float x=left+32;
            for (int id:view.buffs) { BuffIcon icon=new BuffIcon(id,false);icon.x=x;icon.y=top+9;effects.add(icon);icons.add(icon);x+=9; }
            lastBuffs=view.buffs;
        }
        stairs.visible=!aiming && view.stair>=0;
        stairs.enable(stairs.visible && confirmed && (!view.offered || view.consent));
        if (stairs.visible) {
            String key=view.offered ? (view.consent ? "travel" : "awaitvote") : (view.consent ? "canceltravel" : "vote");
            if (view.source.rows.containsKey("branchgate") && (key.equals("travel") || key.equals("vote"))) {
                stairs.text(CoopScene.msg(view.branch==1?"mine_exit":"mine_enter"));
            } else {
                int direction=view.terrain(view.stair)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.ENTRANCE ? -1 : 1;
                stairs.text(CoopScene.msg(key,view.depth+direction));
            }
        }
        cancel.visible=aiming;cancel.enable(aiming);
        boolean canAct=confirmed && view.offered && !aiming;
        wait.enable(canAct); search.enable(canAct);bag.enable(!aiming);
        PublicDungeon.Loot ground=view.floor.get(view.pos);
        pickup.visible=ground!=null;pickup.enable(canAct && pickup.visible);
        pickup.content(ground==null?-1:ground.sprite);
        layout=client.frame().quickslots;
        for(int i=0;i<quick.length;i++) {
            PublicDungeon.Loot item=layout==null ? null : layout.item(i,view);
            items[i]=item;
            quick[i].content(item!=null ? item.sprite : layout==null ? -1 : layout.sprite(i));
            boolean usable=item!=null && canAct && view.actions(item).contains(PublicDungeon.shortcutAction(item));
            String amount=item==null ? (layout!=null && layout.assigned(i) ? "0" : "") :
                    item.showsCharges() ? item.charges+"/"+item.capacity : item.quantity>1 ? Integer.toString(item.quantity) : "";
            quick[i].amount(amount,usable);
            quick[i].enable(layout!=null && !aiming && confirmed);
        }
    }
}
