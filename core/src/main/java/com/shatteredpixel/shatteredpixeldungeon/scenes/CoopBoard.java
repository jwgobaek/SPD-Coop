package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.*;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.*;
import com.watabou.utils.PointF;

/** A viewport over the verified seat map. No Char, Level or actor-linked sprite is constructed here. */
final class CoopBoard extends Group {
    private static final int TILE=16;
    private final int columns,rows,ox,oy;
    private final float left,top;
    interface Input { void tap(PublicDungeon view,int cell); }
    CoopBoard(PublicDungeon view,Input input,float x,float y,float w,float h,String effect) {
        columns=Math.max(1,((int)w/TILE-1)/2*2+1);
        rows=Math.max(1,((int)h/TILE-1)/2*2+1);
        left=Math.round(x+(w-columns*TILE)/2); top=Math.round(y+(h-rows*TILE)/2);
        ox=view.pos%view.width-columns/2; oy=view.pos/view.width-rows/2;
        for(int dy=0;dy<rows;dy++) for(int dx=0;dx<columns;dx++) {
            int cx=ox+dx,cy=oy+dy;
            if(cx<0 || cx>=view.width || cy<0) continue;
            int cell=cy*view.width+cx, t=view.terrain(cell);
            if(t<0) continue;
            float px=left+dx*TILE, py=top+dy*TILE;
            int index=tile(t);
            Image ground=new Image(Assets.Environment.TILES_SEWERS,(index%16)*TILE,(index/16)*TILE,TILE,TILE);
            if(!view.visible(cell)) {
                if(view.source.mappedOnly(cell)) ground.hardlight(0x55546A);
                else ground.brightness(0.42f);
            }
            ground.x=px;ground.y=py;add(ground);
            if(!view.visible(cell)) continue;
            if(view.source.rows.containsKey("stench"+cell)) {
                com.watabou.noosa.ColorBlock gas=new com.watabou.noosa.ColorBlock(TILE,TILE,0x447DA63A);
                gas.x=px;gas.y=py;add(gas);
            }
            String z=view.source.rows.get("z"+cell);
            if(z!=null) edges(px,py,Integer.parseInt(z));
            PublicDungeon.Loot loot=view.floor.get(cell);
            if(loot!=null) place(new ItemSprite(loot.sprite),px,py);
            else if(view.source.rows.containsKey("h"+cell)) place(new ItemSprite("CRYSTAL_CHEST".equals(view.source.rows.get("h"+cell)) ? ItemSpriteSheet.CRYSTAL_CHEST : ItemSpriteSheet.CHEST),px,py);
            String monster=view.monsters.get(cell);
            if(monster!=null) {
                Image sprite;
                switch(monster) {
                    case "Mimic": sprite=new Image(Assets.Sprites.MIMIC,48,0,16,16);break;
                    case "Bee": sprite=new Image(Assets.Sprites.BEE,0,0,16,16);break;
                    case "Ghost": sprite=new Image(Assets.Sprites.GHOST,0,0,14,15);break;
                    case "FetidRat": sprite=new Image(Assets.Sprites.RAT,0,30,16,15);break;
                    case "Piranha": sprite=new Image(Assets.Sprites.PIRANHA,0,0,12,16);break;
                    case "Sentry": sprite=new Image(Assets.Sprites.SENTRY,0,0,8,15);
                        if(view.source.rows.containsKey("charge"+cell)) sprite.tint(1f,0.2f,0.2f,0.55f);
                        break;
                    case "Gnoll": sprite=new Image(Assets.Sprites.GNOLL,0,0,12,15);break;
                    case "Snake": sprite=new Image(Assets.Sprites.SNAKE,0,0,12,11);break;
                    case "Albino": sprite=new Image(Assets.Sprites.RAT,0,15,16,15);break;
                    default: sprite=new Image(Assets.Sprites.RAT,0,0,16,15);
                }
                place(sprite,px,py);
            }
            if(view.ally==cell) place(hero(view.allyClass,view.source.rows.get("allytier"),
                    view.source.rows.get("allylife"),""),px,py);
        }
        // A downed observer has no direct sight but always knows their own position.
        place(hero(view.heroClass,view.source.rows.get("selftier"),view.source.rows.get("life"),effect),
                left+columns/2*TILE,top+rows/2*TILE);
        if ("ZAP".equals(effect)) {
            String[] receipt=view.source.rows.get("receipt").split(",");
            if (receipt.length==4) {
                int to=Integer.parseInt(receipt[2]);
                if (view.visible(to)) {
                    com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile bolt=new com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile();
                    add(bolt);
                    bolt.reset(com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.MAGIC_MISSILE,
                        center(view,view.pos),center(view,to),null);
                }
            }
        }
        add(new PointerArea(left,top,columns*TILE,rows*TILE){
            protected void onClick(PointerEvent e) {
                PointF p=camera().screenToCamera((int)e.current.x,(int)e.current.y);
                int cx=ox+(int)Math.floor((p.x-left)/TILE),cy=oy+(int)Math.floor((p.y-top)/TILE);
                if(cx>=0 && cx<view.width && cy>=0) input.tap(view,cx+cy*view.width);
            }
        });
    }
    private PointF center(PublicDungeon view,int cell) {
        return new PointF(left+(cell%view.width-ox+0.5f)*TILE,top+(cell/view.width-oy+0.5f)*TILE);
    }
    private static int tile(int t) {
        Integer idx=DungeonTileSheet.directFlatVisuals.get(t);
        if(idx==null) idx=DungeonTileSheet.directVisuals.get(t);
        if(idx!=null) return idx;
        if(t==Terrain.WATER) return DungeonTileSheet.WATER;
        if(t==Terrain.CHASM) return DungeonTileSheet.CHASM;
        return DungeonTileSheet.FLAT_WALL;
    }
    private void place(Image im,float x,float y) {
        im.x=x+(TILE-im.width())/2; im.y=y+TILE-im.height(); add(im);
    }
    private static MovieClip hero(String type,String tier,String life,String action) {
        MovieClip sprite=new MovieClip(HeroClass.valueOf(type).spritesheet());
        int row=tier==null?0:Math.max(0,Math.min(6,Integer.parseInt(tier)));
        TextureFilm film=new TextureFilm(HeroSprite.tiers(),row,12,15);
        MovieClip.Animation idle=new MovieClip.Animation(1,true).frames(film,0,0,0,1,0,0,1,1);
        if(!"ALIVE".equals(life)) sprite.play(new MovieClip.Animation(1,false).frames(film,12));
        else {
            MovieClip.Animation move=null;
            if(action.equals("MOVE")) move=new MovieClip.Animation(20,false).frames(film,2,3,4,5,6,7);
            if(action.equals("ZAP") || action.equals("ATTACK")) move=new MovieClip.Animation(15,false).frames(film,13,14,15,0);
            if(action.equals("PICK_UP") || action.equals("REVIVE") || action.equals("EAT"))
                move=new MovieClip.Animation(8,false).frames(film,16,17,16,17);
            sprite.play(move==null?idle:move);
            sprite.listener=animation->sprite.play(idle);
        }
        return sprite;
    }
    private void edges(float x,float y,int mask) {
        if((mask&1)!=0) line(x,y,16,1);
        if((mask&2)!=0) line(x+15,y,1,16);
        if((mask&4)!=0) line(x,y+15,16,1);
        if((mask&8)!=0) line(x,y,1,16);
    }
    private void line(float x,float y,int w,int h) {
        ColorBlock edge=new ColorBlock(w,h,0x99CE7843);edge.x=x;edge.y=y;add(edge);
    }
}
