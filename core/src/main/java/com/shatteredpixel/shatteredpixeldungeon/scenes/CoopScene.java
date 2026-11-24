package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.*;
import java.util.UUID;

/** All widgets are render-thread only. Scene reconstruction keeps the live controller. */
public class CoopScene extends PixelScene {
    private static CoopController active;
    private CoopController controller;
    private CoopController.Frame shown;
    private Group panel;
    private int first;
    private float left,top,width,bottom,center;
    private boolean exiting;
    private long effectsVersion;
    static String msg(String key,Object... args) { return Messages.get(CoopScene.class,key,args); }

    @Override public void create() {
        super.create();
        if(active==null) active=CoopPlatform.controller();
        CoopDiagnostics.watch(CoopPlatform.root());
        controller=active;
        renderFrame();
        fadeIn();
    }
    @Override public void update() {
        super.update();
        if(controller==null) return;
        if(controller.frame().page==CoopController.Page.CLOSED) {
            if(exiting) { CoopDiagnostics.stopWatching(); active=null; controller=null; ShatteredPixelDungeon.switchNoFade(TitleScene.class); return; }
            String notice=controller.frame().notice;
            active=controller=CoopPlatform.controller();
            CoopDiagnostics.watch(CoopPlatform.root());
            if("CHECKPOINT_FAILED".equals(notice)) alert(msg("checkpoint_failed"));
        }
        CoopController.Frame next=controller.frame();
        if(shown!=next) {
            // A newly registered map PointerArea would otherwise take touches intended for an open Window.
            for(Gizmo child:new java.util.ArrayList<>(members)) {
                if(child instanceof Window) {
                    if(shown!=null && shown.page==next.page) return;
                    ((Window)child).hide();
                }
            }
            renderFrame();
        }
    }
    public static void stopSession() { if(active!=null) active.shutdown(); }
    @Override public void onPause() { if(controller!=null) controller.shutdown(); }
    @Override protected void onBackPressed() {
        if(shown.page==CoopController.Page.MENU) { exiting=true; controller.shutdown(); }
        else if(shown.page==CoopController.Page.GAME || shown.page==CoopController.Page.LOBBY) gameMenu();
        else controller.menu();
    }
    private void renderFrame() {
        if(panel!=null) {remove(panel); panel.destroy();}
        panel=new Group(); addToBack(panel);
        CoopController.Frame f=controller.frame();
        if(shown==null || shown.page!=f.page) first=0;
        shown=f;
        com.watabou.utils.RectF inset=getCommonInsets();
        width=Math.min(212,Camera.main.width-inset.left-inset.right-8);
        center=(Camera.main.width+inset.left-inset.right)/2f;
        left=center-width/2; top=inset.top+3; bottom=Camera.main.height-inset.bottom-2;
        panel.add(new ColorBlock(Camera.main.width,Camera.main.height,0xFF18151B));
        if(f.page==CoopController.Page.GAME && f.image!=null) { game(f); return; }
        Image backdrop=new Image(com.shatteredpixel.shatteredpixeldungeon.Assets.Splashes.SEWERS);
        backdrop.scale.set(Math.max(Camera.main.width/backdrop.width(),Camera.main.height/backdrop.height()));
        backdrop.x=(Camera.main.width-backdrop.width())/2; backdrop.y=(Camera.main.height-backdrop.height())/2;
        backdrop.brightness(0.38f); panel.add(backdrop);
        top+=Math.max(0,(bottom-top-142)/2);
        NinePatch frame=Chrome.get(Chrome.Type.WINDOW);
        frame.x=left-5; frame.y=top-3; panel.add(frame); top+=3;
        label(msg("title"),10,Window.TITLE_COLOR);
        if(!f.notice.isEmpty()) label(status(f.notice),6,0xE5B77D);
        switch(f.page) {
            case MENU:
                button(msg("host"),()->controller.host());
                button(msg("find"),()->controller.discover());
                button(msg("direct"),()->address("",null,false));
                pair(msg("saves"),()->controller.saved(false),msg("reconnect"),()->controller.saved(true));
                button(msg("diagnostics"),this::diagnostics);
                break;
            case ROOMS: case SAVES: case TICKETS:
                label(msg(f.page==CoopController.Page.ROOMS?"rooms":f.page==CoopController.Page.SAVES?"saves":"reconnect"),8,0xEEEEEE);
                if(f.list.isEmpty()) label(msg("empty"),7,0xAAAAAA);
                for(int i=first;i<Math.min(first+3,f.list.size());i++) {
                    CoopController.Entry e=f.list.get(i);
                    String name=e.id.toString().substring(0,8);
                    button(name+"  "+(f.page==CoopController.Page.SAVES ?
                            (e.current==null?msg("damaged"):msg("floor",e.current.depth,e.current.generation)) : e.address),()->entry(e));
                }
                if(f.list.size()>3) pair("‹",()->{first=Math.max(0,first-3);renderFrame();},"›",()->{
                    if(first+3<f.list.size()) first+=3;renderFrame();});
                if(f.page==CoopController.Page.ROOMS) button(msg("direct"),()->address("",null,false));
                break;
            case LOBBY:
                label(f.session==null?"":f.session.toString().substring(0,8)+"   "+f.address,7,0xF3DCB1);
                String[] roster=f.roster.split(" ");
                for(int i=0;i<2;i++) {
                    String[] member=roster.length>i+2?roster[i+2].split(":"):new String[0];
                    String name=member.length==3 ? (member[1].equals("null")?msg("select"):HeroClass.valueOf(member[1]).title()) : msg("waiting");
                    label(msg(i==0?"hostseat":"guestseat")+"  "+name+(member.length==3 && member[2].equals("1")?" / "+msg("ready"):""),7,0xCCCCCC);
                }
                float half=(width-2)/2;
                hero(HeroClass.WARRIOR,left,half); hero(HeroClass.HUNTRESS,left+half+2,half); top+=20;
                boolean ready=roster.length>2+f.seat && f.seat>=0 && roster[2+f.seat].endsWith(":1");
                button(msg(ready?"unready":"ready"),()->controller.ready(!ready));
                if(f.seat==0) button(msg("start"),()->controller.start()).enable(roster.length>1 && roster[1].equals("READY"));
                break;
            case EPOCH:
                label(msg("rollback"),7,0xF1B66A);
                button(msg("accept"),()->controller.acceptEpoch()); break;
            case DISCONNECTED:
                if(f.session!=null) button(msg("reconnect"),()->controller.reconnect(f.session,""));
                button(msg("saves"),()->controller.saved(false)); break;
            default: label(status(f.page.name()),8,0xDDDDDD); break;
        }
        StyledButton back=make(msg(f.page==CoopController.Page.MENU?"titleback":"back"),this::onBackPressed);
        back.setRect(left,Math.min(top+2,bottom-16),width,16);
        frame.size(width+10,back.bottom()-frame.y+5);
    }
    private void hero(HeroClass cls,float x,float w) {
        StyledButton btn=make(cls.title(),()->controller.select(cls.name()));
        btn.icon(new Image(cls.spritesheet(),0,15,12,15));
        btn.setRect(x,top,w,18);
    }
    private void entry(CoopController.Entry e) {
        if(shown.page==CoopController.Page.ROOMS) controller.join(e.address,e.id);
        else if(shown.page==CoopController.Page.TICKETS) address(e.address,e.id,true);
        else {
            String body=e.current==null ? msg("damaged") : msg("savedinfo",e.current.depth,e.current.generation,
                    e.current.hostClass,e.current.guestClass,java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(e.current.savedAt)));
            String[] options=e.current!=null && e.backup!=null ? new String[]{msg("resume"),msg("backup")} :
                    e.current!=null ? new String[]{msg("resume")} : e.backup!=null ? new String[]{msg("backup")} : new String[]{};
            add(new WndOptions(msg("saves"),body,options) {
                protected void onSelect(int n) {
                    if(e.current!=null && n==0) controller.resume(e.id,false);
                    else confirm(msg("rollback")+" "+msg("floor",e.backup.depth,e.backup.generation),()->controller.resume(e.id,true));
                }
            });
        }
    }
    private void address(String initial,UUID id,boolean reconnect) {
        add(new WndTextInput(msg("direct"),msg("address"),initial,255,false,msg("connect"),msg("cancel")){
            @Override public void onSelect(boolean ok,String value) {
                if(ok) { if(reconnect) controller.reconnect(id,value); else controller.join(value,id); }
            }
        });
    }
    private StyledButton make(String text,Runnable click) {
        StyledButton b=new StyledButton(Chrome.Type.GREY_BUTTON_TR,text,8) {
            protected void onClick() { click.run(); }
        };
        panel.add(b); return b;
    }
    private StyledButton button(String text,Runnable click) {
        StyledButton b=make(text,click); b.setRect(left,top,width,16); top+=18; return b;
    }
    private void pair(String l,Runnable a,String r,Runnable b) {
        float half=(width-2)/2;
        make(l,a).setRect(left,top,half,16); make(r,b).setRect(left+half+2,top,half,16); top+=18;
    }
    private void label(String text,int size,int color) {
        RenderedTextBlock block=renderTextBlock(text,size);
        block.maxWidth((int)width); block.hardlight(color);
        block.setPos(center-block.width()/2,top); align(block); panel.add(block); top+=block.height()+3;
    }
    static String status(String code) {
        String text=msg("state_"+code.toLowerCase(java.util.Locale.ROOT));
        return text.equals(Messages.NO_TEXT_FOUND) ? code.replace('_',' ') : text;
    }
    private void confirm(String body,Runnable yes) {
        add(new WndOptions(msg("confirm"),body,msg("accept"),msg("cancel")){
            protected void onSelect(int n){if(n==0)yes.run();}
        });
    }
    private void alert(String body) {add(new WndOptions(msg("title"),body,msg("back")));}
    private void gameMenu() {
        boolean host=shown.seat==0;
        add(new WndOptions(msg("menu"),shown.address+"\n"+status(shown.image==null?"LOBBY":shown.image.rows.get("status")),
                msg("continue"),msg("diagnostics"),msg(host?"saveexit":"leave")){
            protected void onSelect(int n) { if(n==1) diagnostics(); if(n==2) controller.leave(host); }
        });
    }
    private void diagnostics() {
        String report=CoopDiagnostics.report(shown);
        add(new WndOptions(msg("diagnostics"),report,msg("export"),msg("back")){
            protected void onSelect(int n){ if(n==0) CoopPlatform.export(report); }
        });
    }
    private void game(CoopController.Frame f) {
        PublicDungeon v=f.dungeon;
        if(v==null) {
            label(status(f.image.rows.get("status")),10,Window.TITLE_COLOR);
            if(f.image.rows.containsKey("reason")) label(status(f.image.rows.get("reason")),8,0xF6BD89);
            button(msg("menu"),this::gameMenu); return;
        }
        String effect="";
        if(effectsVersion!=0 && f.image.version==effectsVersion+1) effect=f.cue;
        effectsVersion=f.image.version;
        String sound=null;
        switch(effect) {
            case "PICK_UP": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.ITEM;break;
            case "EAT": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.EAT;break;
            case "HURT": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.HIT;break;
            case "KILL": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.DEATH;break;
            case "REVIVE": case "REVIVED": sound=com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.TELEPORT;break;
        }
        if(sound!=null) com.watabou.noosa.audio.Sample.INSTANCE.play(sound);
        label(HeroClass.valueOf(v.heroClass).title()+"    HP "+v.hp+"/"+v.maxHp+"    "+msg("level",v.level,v.experience),8,Window.TITLE_COLOR);
        String line=f.pending!=0 ? msg("pending",f.pending) : f.image.rows.containsKey("reason") ?
                status(f.image.rows.get("reason")) : !"ALIVE".equals(f.image.rows.get("life")) ?
                status(f.image.rows.get("life")) : msg(v.offered?"yourturn":"theirturn");
        if(v.ally>=0 && "DOWNED".equals(f.image.rows.get("allylife"))) line+=" / "+msg("reviveally");
        if(f.lastResult.startsWith("REJECT") || f.lastResult.equals("UNKNOWN")) line+=" · "+status(f.lastResult);
        label(line,7,v.offered?0xB4DA80:0xCBC6BA);
        float bx=left;
        for(int buff:v.buffs) {
            BuffIcon icon=new BuffIcon(buff,false);icon.x=bx;icon.y=top;panel.add(icon);
            bx+=9;if(bx>left+width-7) break;
        }
        if(!v.buffs.isEmpty()) top+=9;
        panel.add(new CoopBoard(v,controller,0,top,Camera.main.width,bottom-top-19,effect));
        float part=(width-4)/3;
        StyledButton wait=make(msg("wait"),()->controller.act(f.image,"WAIT",0,0));
        wait.setRect(left,bottom-17,part,17); wait.enable(v.offered && f.pending==0);
        make(msg("bag"),()->add(new com.shatteredpixel.shatteredpixeldungeon.windows.WndCoopBag(v,controller)))
                .setRect(left+part+2,bottom-17,part,17);
        make(msg("menu"),this::gameMenu).setRect(left+2*part+4,bottom-17,part,17);
    }
}
