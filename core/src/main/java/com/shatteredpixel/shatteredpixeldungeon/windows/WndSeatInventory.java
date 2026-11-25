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
    private final PublicDungeon.Loot scroll;

    public WndSeatInventory(PublicDungeon view,CoopController client) { this(view,client,0,-1); }

    @Override protected int tabHeight() { return 20; }

    public static WndSeatInventory chooseEquipment(PublicDungeon view,CoopController client,PublicDungeon.Loot scroll) {
        return new WndSeatInventory(view,client,0,-1,scroll);
    }

    public static WndSeatInventory chooseShortcut(PublicDungeon view,CoopController client,int slot) {
        return new WndSeatInventory(view,client,0,slot);
    }
    private WndSeatInventory(PublicDungeon view,CoopController client,int page,int shortcut) {
        this(view,client,page,shortcut,null);
    }

    private WndSeatInventory(PublicDungeon view,CoopController client,int page,int shortcut,PublicDungeon.Loot scroll) {
        this.scroll=scroll;
        this.shortcut=shortcut;
        this.view=view;
        this.client=client;
        WndBag.Grid grid=WndBag.grid(chrome.marginTop(),chrome.marginHor());
        resize(grid.width,grid.height);
        RenderedTextBlock title=PixelScene.renderTextBlock(scroll!=null ? msg("choose_equipment") : shortcut<0 ? msg("bag") : msg("choose_shortcut",shortcut+1),8);
        title.hardlight(TITLE_COLOR);title.setPos(0,(WndBag.TITLE_HEIGHT-title.height())/2);add(title);
        List<PublicDungeon.Loot> pack=new ArrayList<>();
        PublicDungeon.Loot[] gear=new PublicDungeon.Loot[GEAR.length];
        for (PublicDungeon.Loot item:view.belongings) {
            int index=Arrays.asList(GEAR).indexOf(item.slot);
            if (index>=0) gear[index]=item;
            else if (item.bagIndex>=0 || item.slot.equals("SECOND")) pack.add(item);
        }
        Collections.sort(pack,(a,b)->Integer.compare(a.order,b.order));
        int capacity=grid.rows*grid.cols-gear.length;
        List<Integer> tabs=new ArrayList<>();
        List<Integer> offsets=new ArrayList<>();
        for (int bag=0;bag==0 || view.source.rows.containsKey("bagtab"+bag);bag++) {
            int count=0;
            for (PublicDungeon.Loot item:pack) if (item.bagIndex==bag) count++;
            for (int offset=0;offset<Math.max(1,count);offset+=capacity) { tabs.add(bag); offsets.add(offset); }
        }
        int selected=tabs.get(page);
        for (Iterator<PublicDungeon.Loot> it=pack.iterator();it.hasNext();) if (it.next().bagIndex!=selected) it.remove();
        if (shortcut<0 && selected>0) {
            String bag=view.source.rows.get("bagtab"+selected).toLowerCase(Locale.ROOT);
            String translated=Messages.get("items.bags."+bag+".name");
            if (!Messages.NO_TEXT_FOUND.equals(translated)) title.text(Messages.titleCase(translated));
        }
        for (int i=0;i<grid.rows*grid.cols;i++) {
            int index=offsets.get(page)+i-gear.length;
            PublicDungeon.Loot item=i<gear.length ? gear[i] : index<pack.size() ? pack.get(index) : null;
            int placeholder=i<gear.length ? EMPTY[i] : -1;
            Slot cell=new Slot(item,placeholder,i<gear.length);
            cell.setRect((i%grid.cols)*(grid.slotWidth+WndBag.SLOT_MARGIN),
                    WndBag.TITLE_HEIGHT+(i/grid.cols)*(grid.slotHeight+WndBag.SLOT_MARGIN),grid.slotWidth,grid.slotHeight);
            add(cell);
        }
        for (int i=0;i<tabs.size();i++) {
            final int dest=i;
            String type=view.source.rows.get("bagtab"+tabs.get(i));
            Icons icon=Icons.BACKPACK;
            if ("VelvetPouch".equals(type)) icon=Icons.SEED_POUCH;
            if ("ScrollHolder".equals(type)) icon=Icons.SCROLL_HOLDER;
            if ("PotionBandolier".equals(type)) icon=Icons.POTION_BANDOLIER;
            if ("MagicalHolster".equals(type)) icon=Icons.WAND_HOLSTER;
            Tab tab=offsets.get(i)==0 ? new IconTab(Icons.get(icon)) {
                @Override protected void onClick(){changePage(page,dest);}
            } : new LabeledTab(String.valueOf(offsets.get(i)/capacity+1)) {
                @Override protected void onClick(){changePage(page,dest);}
            };
            add(tab);
        }
        select(page);
        layoutTabs();
    }

    private void changePage(int from,int to) {
        if (from==to) return;
        hide(); Game.scene().add(new WndSeatInventory(view,client,to,shortcut,scroll));
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
                    equipped,loot!=null && (scroll!=null ? view.scrollTarget(loot,scroll.kind)>0 : shortcut<0 || selectable()));
        }
        private boolean selectable() {
            return loot!=null && !loot.reference.isEmpty() && !PublicDungeon.shortcutAction(loot).isEmpty();
        }
        @Override protected void onClick() {
            if (loot==null) return;
            if (scroll!=null) {
                int selection=view.scrollTarget(loot,scroll.kind);
                if (selection>0 && client.act(view.source,scroll.kind,selection,scroll.id)) WndSeatInventory.this.hide();
            } else if (shortcut<0) inspect(loot);
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
                ((CoopScene)Game.scene()).useItem(view,item,verb);
            }
        });
    }
}
