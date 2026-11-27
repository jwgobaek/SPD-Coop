package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.lan.CoopController;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AlchemyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CoopScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

/** Three local ingredient slots are bound to the offer that opened the cauldron. */
public final class WndSeedAlchemy extends Window {
    private final PublicDungeon.Loot[] selection = new PublicDungeon.Loot[3];
    private final RedButton[] slots = new RedButton[3];
    private final RedButton craft;
    private final CoopScene scene;
    private final PublicDungeon view;
    private final CoopController controller;
    private final int cell;

    public WndSeedAlchemy(CoopScene scene, PublicDungeon view, CoopController controller, int cell, PublicDungeon.Loot first) {
        this.scene = scene;
        this.view = view;
        this.controller = controller;
        this.cell = cell;
        selection[0] = first;
        resize(126, 109);
        RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(AlchemyScene.class, "title"), 9);
        title.hardlight(TITLE_COLOR);
        title.setPos((126 - title.width()) / 2, 2);
        add(title);
        for (int n = 0; n < slots.length; n++) {
            final int slot = n;
            slots[n] = new RedButton("") {
                @Override protected void onClick() { choose(slot); }
            };
            slots[n].setRect(3 + 41*n, 17, 38, 25);
            add(slots[n]);
        }
        RenderedTextBlock preview = PixelScene.renderTextBlock(Messages.get(Potion.SeedToPotion.class, "name")
                + "\n" + Messages.get(AlchemyScene.class, "energy") + " 0", 6);
        preview.setPos(32, 48);
        add(preview);
        ItemSprite output = new ItemSprite(ItemSpriteSheet.POTION_HOLDER);
        output.x = 9;
        output.y = 46;
        add(output);
        craft = new RedButton(Messages.get(AlchemyScene.class, "craft")) {
            @Override protected void onClick() {
                if (controller.frame().image == view.source)
                    controller.brewSeeds(view.source, cell, selection[0].id, selection[1].id, selection[2].id);
                hide();
            }
        };
        craft.setRect(1, 69, 124, 18);
        add(craft);
        RedButton cancel = new RedButton(Messages.get(AlchemyScene.class, "cancel")) {
            @Override protected void onClick() { hide(); }
        };
        cancel.setRect(1, 89, 124, 18);
        add(cancel);
        refresh();
    }

    private void choose(int slot) {
        if (controller.frame().image != view.source) { hide(); return; }
        scene.add(WndSeatInventory.select(view, controller, new WndSeatInventory.Selection() {
            @Override public boolean accepts(PublicDungeon.Loot item) {
                if (!view.source.rows.containsKey("brewseed" + item.id)) return false;
                int used = 0;
                for (int i = 0; i < selection.length; i++)
                    if (i != slot && selection[i] != null && selection[i].id == item.id) used++;
                return used < item.quantity;
            }
            @Override public void select(PublicDungeon.Loot item) {
                if (controller.frame().image != view.source || !accepts(item)) return;
                selection[slot] = item;
                refresh();
            }
        }));
    }

    private void refresh() {
        boolean complete = true;
        for (int i = 0; i < slots.length; i++) {
            PublicDungeon.Loot item = selection[i];
            slots[i].icon(new ItemSprite(item == null ? ItemSpriteSheet.SOMETHING : item.sprite));
            slots[i].text(item == null ? "" : "1");
            complete &= item != null;
        }
        craft.enable(complete);
    }
}
