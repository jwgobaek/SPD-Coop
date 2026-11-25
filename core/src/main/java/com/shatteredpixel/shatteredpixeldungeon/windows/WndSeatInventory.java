package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.lan.CoopController;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.watabou.noosa.Game;
import java.util.*;

/** The original bag grid and tab chrome populated exclusively from the authenticated seat. */
public final class WndSeatInventory extends WndTabbed {
    private static final String[] GEAR={"WEAPON","ARMOR","ARTIFACT","MISC","RING"};
    private static final int[] EMPTY={ItemSpriteSheet.WEAPON_HOLDER,ItemSpriteSheet.ARMOR_HOLDER,
            ItemSpriteSheet.ARTIFACT_HOLDER,ItemSpriteSheet.SOMETHING,ItemSpriteSheet.RING_HOLDER};
    private final PublicDungeon view;
    private final CoopController client;
    private final int shortcut;

    public WndSeatInventory(PublicDungeon view,CoopController client) { this(view,client,0,-1); }

    @Override protected int tabHeight() { return 20; }

    public static WndSeatInventory chooseShortcut(PublicDungeon view,CoopController client,int slot) {
        return new WndSeatInventory(view,client,0,slot);
    }
    private WndSeatInventory(PublicDungeon view,CoopController client,int page,int shortcut) {
        this.shortcut=shortcut;
        this.view=view;
        this.client=client;
        WndBag.Grid grid=WndBag.grid(chrome.marginTop(),chrome.marginHor());
        resize(grid.width,grid.height);
        RenderedTextBlock title=PixelScene.renderTextBlock(shortcut<0 ? msg("bag") : msg("choose_shortcut",shortcut+1),8);
        title.hardlight(TITLE_COLOR);title.setPos(0,(WndBag.TITLE_HEIGHT-title.height())/2);add(title);
        List<PublicDungeon.Loot> pack=new ArrayList<>();
        PublicDungeon.Loot[] gear=new PublicDungeon.Loot[GEAR.length];
        for (PublicDungeon.Loot item:view.belongings) {
            int index=Arrays.asList(GEAR).indexOf(item.slot);
            if (index<0) pack.add(item); else gear[index]=item;
        }
        int capacity=grid.rows*grid.cols-gear.length;
        for (int i=0;i<grid.rows*grid.cols;i++) {
            int index=page*capacity+i-gear.length;
            PublicDungeon.Loot item=i<gear.length ? gear[i] : index<pack.size() ? pack.get(index) : null;
            int placeholder=i<gear.length ? EMPTY[i] : -1;
            Slot cell=new Slot(item,placeholder,i<gear.length);
            cell.setRect((i%grid.cols)*(grid.slotWidth+WndBag.SLOT_MARGIN),
                    WndBag.TITLE_HEIGHT+(i/grid.cols)*(grid.slotHeight+WndBag.SLOT_MARGIN),grid.slotWidth,grid.slotHeight);
            add(cell);
        }
        int pages=Math.max(1,(pack.size()+capacity-1)/capacity);
        for (int i=0;i<pages;i++) {
            final int dest=i;
            Tab tab=i==0 ? new IconTab(Icons.get(Icons.BACKPACK)) {
                @Override protected void onClick(){changePage(page,dest);}
            } : new LabeledTab(String.valueOf(i+1)) {
                @Override protected void onClick(){changePage(page,dest);}
            };
            add(tab);
        }
        select(page);
        layoutTabs();
    }

    private void changePage(int from,int to) {
        if (from==to) return;
        hide(); Game.scene().add(new WndSeatInventory(view,client,to,shortcut));
    }

    public final class Slot extends InventorySlot {
        public final long handle;
        private final PublicDungeon.Loot loot;
        Slot(PublicDungeon.Loot loot,int placeholder,boolean equipped) {
            super(null);
            this.loot=loot;
            handle=loot==null ? 0 : loot.id;
            String count=loot==null ? "" : loot.kind.equals("STAFF") ? loot.charges+"/"+loot.capacity :
                    loot.quantity>1 ? String.valueOf(loot.quantity) : "";
            display(loot==null ? placeholder : loot.sprite,count,loot==null ? "NA" : loot.upgrade,
                    equipped,loot!=null && (shortcut<0 || selectable()));
        }
        private boolean selectable() {
            return loot!=null && !loot.reference.isEmpty() && !PublicDungeon.shortcutAction(loot).isEmpty();
        }
        @Override protected void onClick() {
            if (loot==null) return;
            if (shortcut<0) inspect(loot);
            else if (selectable()) {
                client.bindShortcut(view.source,shortcut,loot);
                WndSeatInventory.this.hide();
            }
        }
    }

    private static String msg(String key,Object...args) { return Messages.get(CoopScene.class,key,args); }

    private void inspect(PublicDungeon.Loot item) {
        String key=view.source.rows.get("label"+item.id);
        String name=key==null ? Messages.NO_TEXT_FOUND : Messages.get(key+".name");
        if (Messages.NO_TEXT_FOUND.equals(name)) name=msg("kind_"+item.kind.toLowerCase(Locale.ROOT));
        String description=key==null ? Messages.NO_TEXT_FOUND : Messages.get(key+".desc");
        String info=msg("quantity",item.quantity);
        if (!item.upgrade.equals("NA") && !item.upgrade.equals("0")) info+="  "+(item.upgrade.startsWith("-")?"":"+")+item.upgrade;
        if (item.kind.equals("STAFF")) info+="  "+msg("charges",item.charges,item.capacity);
        if (!Messages.NO_TEXT_FOUND.equals(description)) info=description+"\n\n"+info;
        List<String> verbs=view.actions(item);
        String[] options=new String[verbs.size()+1];
        for (int i=0;i<verbs.size();i++) options[i]=msg(verbs.get(i).toLowerCase(Locale.ROOT));
        options[verbs.size()]=msg("back");
        Game.scene().add(new WndOptions(new ItemSprite(item.sprite),Messages.titleCase(name),info,options) {
            @Override protected void onSelect(int i) {
                if(i<0 || i>=verbs.size()) return;
                WndSeatInventory.this.hide();
                String verb=verbs.get(i);
                if(verb.equals("ZAP") || verb.equals("SHOOT")) ((CoopScene)Game.scene()).aim(view,item);
                else view.use(client,item,verb);
            }
        });
    }
}
