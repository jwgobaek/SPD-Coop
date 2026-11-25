package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.watabou.noosa.Game;
import java.util.*;

public final class WndCoopBag extends Window {
    private final PublicDungeon view;
    private final CoopController client;
    public WndCoopBag(PublicDungeon view,CoopController client) { this(view,client,0); }
    private static String msg(String k,Object... args) { return Messages.get(CoopScene.class,k,args); }
    private WndCoopBag(PublicDungeon view,CoopController client,int page) {
        super(124,106);
        this.view=view; this.client=client;
        RenderedTextBlock title=PixelScene.renderTextBlock(msg("bag"),9);
        title.hardlight(TITLE_COLOR); title.setPos(4,3); add(title);
        List<PublicDungeon.Loot> items=new ArrayList<>(view.belongings);
        Collections.sort(items,(a,b)->Boolean.compare(a.slot.equals("BAG"),b.slot.equals("BAG")));
        for(int i=0;i<15 && page*15+i<items.size();i++) {
            PublicDungeon.Loot loot=items.get(page*15+i);
            StyledButton cell=new StyledButton(loot.slot.equals("BAG")?Chrome.Type.GREY_BUTTON_TR:Chrome.Type.RED_BUTTON,"",6){
                protected void onClick(){ choose(loot); }
            };
            cell.icon(new ItemSprite(loot.sprite)); cell.setRect(2+24*(i%5),18+22*(i/5),23,21); add(cell);
            if(loot.quantity>1) {
                RenderedTextBlock qty=PixelScene.renderTextBlock(""+loot.quantity,6);
                qty.setPos(cell.right()-qty.width()-1,cell.bottom()-qty.height());add(qty);
            }
        }
        if(page>0) pageButton("‹",2,page-1);
        if((page+1)*15<items.size()) pageButton("›",98,page+1);
    }
    private void pageButton(String text,int x,int page) {
        RedButton btn=new RedButton(text){ protected void onClick(){hide(); Game.scene().add(new WndCoopBag(view,client,page));} };
        btn.setRect(x,87,23,16);add(btn);
    }
    private void choose(PublicDungeon.Loot item) {
        List<String> verbs=view.actions(item);
        String[] options=new String[verbs.size()+1];
        for(int i=0;i<verbs.size();i++) options[i]=msg(verbs.get(i).toLowerCase(Locale.ROOT));
        options[verbs.size()]=msg("back");
        String info=msg("quantity",item.quantity)+(item.upgrade.equals("NA")?"":"  +"+item.upgrade);
        if (item.showsCharges()) info+="  "+msg("charges",item.charges,item.capacity);
        Game.scene().add(new WndOptions(new ItemSprite(item.sprite),msg("kind_"+item.kind.toLowerCase(Locale.ROOT)),info,options) {
            protected void onSelect(int i) {
                if(i>=verbs.size()) return;
                WndCoopBag.this.hide();
                String verb=verbs.get(i);
                if (verb.equals("ZAP") || verb.equals("SHOOT")) ((CoopScene)Game.scene()).aim(view,item);
                else view.use(client,item,verb);
            }
        });
    }
}
