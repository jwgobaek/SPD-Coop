package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.lan.CoopController;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CoopScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Image;
import java.util.ArrayList;
import java.util.Map;

/** Original shop icons, translations and window controls backed by one immutable seat view. */
public final class WndSeatTrade {
    private WndSeatTrade() {}

    private static boolean unchanged(PublicDungeon previous,PublicDungeon current,int cell,long id,String verb) {
        if (current==null || !previous.source.session.equals(current.source.session) || previous.source.seat!=current.source.seat
                || !java.util.Objects.equals(previous.source.rows.get("epoch"),current.source.rows.get("epoch"))
                || previous.depth!=current.depth || previous.pos!=current.pos || !current.visible(cell)) return false;
        String key=verb.equals("BUYBACK") ? "refund"+id : verb.startsWith("SELL") ? "sell"+id : "quote"+cell;
        return java.util.Objects.equals(previous.source.rows.get(key),current.source.rows.get(key))
                && java.util.Objects.equals(previous.source.rows.get("gold"),current.source.rows.get("gold"))
                && java.util.Objects.equals(previous.source.rows.get("merchant"),current.source.rows.get("merchant"));
    }

    private static void confirm(CoopScene scene,PublicDungeon view,CoopController client,int cell,long id,
                                int sprite,int price,int quantity,String verb,String label,boolean allowed) {
        int gold=Integer.parseInt(view.source.rows.get("gold"));
        String info=CoopScene.msg("shop_balance",gold,quantity);
        String quotation = verb.equals("BUY") ? view.source.rows.get("quote"+cell) : verb.equals("BUYBACK") ? view.source.rows.get("refund"+id) : null;
        boolean space=quotation==null || quotation.endsWith(",1");
        if (!space) info+="\n"+CoopScene.msg("shop_full");
        if ((verb.equals("BUY") || verb.equals("BUYBACK")) && gold<price) info+="\n"+CoopScene.msg("shop_poor");
        String key=view.source.rows.get("tradelabel"+id);
        if (key==null) key=view.source.rows.get("label"+id);
        String title=key==null ? Messages.NO_TEXT_FOUND : Messages.get(key+".name");
        if (key!=null) {
            String description=Messages.get(key+".desc");
            if (!Messages.NO_TEXT_FOUND.equals(description)) info=description+"\n\n"+info;
        }
        scene.add(new WndOptions(new ItemSprite(sprite),Messages.NO_TEXT_FOUND.equals(title)?CoopScene.msg("shop"):Messages.titleCase(title),
                info,label,CoopScene.msg("cancel")) {
            @Override protected boolean enabled(int index) { return index!=0 || allowed && space; }
            @Override protected void onSelect(int index) {
                if (index!=0) return;
                PublicDungeon live=client.frame().dungeon;
                if (!unchanged(view,live,cell,id,verb)) scene.add(new WndMessage(CoopScene.msg("shop_changed")));
                else if (!live.offered) scene.add(new WndMessage(CoopScene.msg("theirturn")));
                else client.act(live.source,verb,cell,id);
            }
        });
    }

    public static void stock(CoopScene scene,PublicDungeon view,int cell,CoopController client) {
        String row=view.source.rows.get("quote"+cell);
        if (row==null) {
            scene.add(new WndOptions(CoopScene.msg("shop"),CoopScene.msg("shop_unsupported"),CoopScene.msg("move"),CoopScene.msg("cancel")) {
                @Override protected boolean enabled(int i) {
                    return i!=0 || view.offered && cell!=view.pos && Math.abs(cell%view.width-view.pos%view.width)<=1
                            && Math.abs(cell/view.width-view.pos/view.width)<=1;
                }
                @Override protected void onSelect(int i) { if (i==0) client.act(view.source,"MOVE",cell,0); }
            });
            return;
        }
        String[] quote=row.split(",");
        int price=Integer.parseInt(quote[1]), count=Integer.parseInt(quote[2]);
        int sprite=Integer.parseInt(view.source.rows.get("sale"+cell));
        if (cell==view.pos) {
            confirm(scene,view,client,cell,Long.parseLong(quote[0]),sprite,price,count,"BUY",
                    Messages.get(WndTradeItem.class,"buy",price),price<=Integer.parseInt(view.source.rows.get("gold")));
        } else {
            boolean adjacent=Math.abs(cell%view.width-view.pos%view.width)<=1 && Math.abs(cell/view.width-view.pos/view.width)<=1;
            confirm(scene,view,client,cell,0,sprite,price,count,"MOVE",CoopScene.msg("shop_approach",price),adjacent);
        }
    }

    public static void sell(CoopScene scene,PublicDungeon view,PublicDungeon.Loot item,String verb,CoopController client) {
        int price=Integer.parseInt(view.source.rows.get("sell"+item.id).split(",")[0]);
        boolean one=verb.equals("SELL_ONE");
        if (one) price/=item.quantity;
        confirm(scene,view,client,Integer.parseInt(view.source.rows.get("merchant")),item.id,item.sprite,price,
                one?1:item.quantity,verb,Messages.get(WndTradeItem.class,one?"sell_1":"sell",price),true);
    }

    public static void merchant(CoopScene scene,PublicDungeon view,int cell,CoopController client) {
        ArrayList<String> buttons=new ArrayList<>();
        ArrayList<Long> handles=new ArrayList<>();
        ArrayList<Integer> images=new ArrayList<>();
        buttons.add(Messages.get(Shopkeeper.class,"sell"));
        for (Map.Entry<String,String> row:view.source.rows.entrySet()) {
            if (!row.getKey().startsWith("refund")) continue;
            String[] quote=row.getValue().split(",");
            handles.add(Long.parseLong(row.getKey().substring(6)));
            images.add(Integer.parseInt(quote[0]));
            buttons.add(CoopScene.msg("shop_buyback",Integer.parseInt(quote[2]),Integer.parseInt(quote[1])));
        }
        buttons.add(CoopScene.msg("cancel"));
        scene.add(new WndOptions(CoopScene.msg("shop"),CoopScene.msg("shop_gold",Integer.parseInt(view.source.rows.get("gold"))),
                buttons.toArray(new String[0])) {
            @Override protected void onSelect(int index) {
                if (index==0 && client.frame().dungeon!=null) scene.add(new WndSeatInventory(client.frame().dungeon,client));
                else if (index<=handles.size() && index>0) {
                    long id=handles.get(index-1);
                    String[] quote=view.source.rows.get("refund"+id).split(",");
                    int price=Integer.parseInt(quote[2]);
                    confirm(scene,view,client,cell,id,images.get(index-1),price,Integer.parseInt(quote[1]),"BUYBACK",
                            buttons.get(index),price<=Integer.parseInt(view.source.rows.get("gold")));
                }
            }
            @Override protected boolean hasIcon(int index) { return index>0 && index<=images.size(); }
            @Override protected Image getIcon(int index) {
                return index>0 && index<=images.size() ? new ItemSprite(images.get(index-1)) : null;
            }
        });
    }
}
