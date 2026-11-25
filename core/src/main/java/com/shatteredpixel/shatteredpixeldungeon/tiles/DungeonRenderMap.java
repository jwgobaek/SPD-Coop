package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import java.util.Random;

/** Inputs to the native render layers. A seat owns its arrays and never opens a Level. */
public class DungeonRenderMap {
    final int width, height, depth, branch;
    final boolean mining, hallsBoss, lastShop;
    final int[] tiles;
    final boolean[] visible, visited, mapped, discoverable;
    final byte[] variance;
    private final int[] nest;
    private final Level solo;
    private PublicDungeon seat;
    private final com.watabou.utils.Rect changedCells = new com.watabou.utils.Rect();

    /** Native tile stitching, wall masking and fog sample neighbouring cells. */
    public com.watabou.utils.Rect changedArea() {
        return new com.watabou.utils.Rect(
                Math.max(0,changedCells.left-2),Math.max(0,changedCells.top-2),
                Math.min(width,changedCells.right+2),Math.min(height,changedCells.bottom+2));
    }

    private DungeonRenderMap(Level level) {
        solo=level; nest=null;
        width=level.width(); height=level.height(); depth=Dungeon.depth; branch=Dungeon.branch;
        mining=level instanceof MiningLevel; hallsBoss=level instanceof HallsBossLevel;
        lastShop=level instanceof LastShopLevel;
        tiles=level.map; visible=level.heroFOV; visited=level.visited; mapped=level.mapped;
        discoverable=level.discoverable; variance=DungeonTileSheet.tileVariance;
    }

    static DungeonRenderMap solo() { return new DungeonRenderMap(Dungeon.level); }

    public DungeonRenderMap(PublicDungeon view) {
        solo=null;
        width=view.width; height=view.height; depth=view.depth; branch=0;
        mining=false; hallsBoss=false; lastShop=false;
        int length=width*height;
        nest=new int[length];
        tiles=new int[length]; visible=new boolean[length]; visited=new boolean[length];
        mapped=new boolean[length]; discoverable=new boolean[length]; variance=new byte[length];
        // This seed controls artwork only. It cannot reconstruct the private dungeon generator.
        Random cosmetic=new Random(view.source.session.getLeastSignificantBits() ^ depth);
        for (int i=0;i<length;i++) variance[i]=(byte)cosmetic.nextInt(100);
        apply(view);
    }

    public boolean apply(PublicDungeon view) {
        if (view.width!=width || view.height!=height || view.depth!=depth) throw new IllegalArgumentException("Different floor");
        boolean changed=false;
        changedCells.setEmpty();
        for (int i=0;i<tiles.length;i++) {
            int terrain=view.terrain(i);
            boolean seen=view.visible(i), mapOnly=!seen && terrain>=0 && view.source.mappedOnly(i);
            String detail=view.source.rows.get("nest"+i);
            int frame=terrain>=0 && !mapOnly && detail!=null ? Integer.parseInt(detail) : -1;
            if (frame < -1 || frame > 8) throw new IllegalArgumentException("Nest frame");
            if (nest[i]!=frame || tiles[i]!=terrain || visible[i]!=seen || mapped[i]!=mapOnly
                    || feature(i)!=feature(view,i)) {
                changed=true;
                changedCells.union(i%width,i/width);
            }
            nest[i]=frame;
            tiles[i]=terrain; visible[i]=seen; mapped[i]=mapOnly;
            visited[i]=terrain>=0 && !mapOnly; discoverable[i]=terrain>=0;
        }
        seat=view;
        return changed;
    }
    private static int feature(PublicDungeon view,int i) {
        if (view==null || !view.visible(i)) return -1;
        String trap=view.source.rows.get("trap"+i);
        if (trap!=null) return Integer.parseInt(trap);
        int plant=view.plant(i);
        return plant<0 ? -1 : 112+plant;
    }
    int feature(int cell) { return feature(seat,cell); }
    public int terrain(int cell) { return tiles[cell]; }
    public com.watabou.noosa.Tilemap nestLayer() {
        String texture=com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.SEWER_BOSS;
        com.watabou.noosa.Tilemap layer=new com.watabou.noosa.Tilemap(texture,new com.watabou.noosa.TextureFilm(texture,16,16));
        layer.map(nest,width);
        return layer;
    }
    String tilesTexture() {
        if (!isSeat()) return solo.tilesTex();
        return depth==6 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_PRISON
                : com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_SEWERS;
    }
    boolean isSeat() { return solo==null; }
    boolean pit(int cell) { return solo==null ? tiles[cell]==Terrain.CHASM : solo.pit[cell]; }
    boolean inside(int cell) { return cell>=width && cell<tiles.length-width && cell%width>0 && cell%width<width-1; }
    int alt(int visual,int pos) { return DungeonTileSheet.visualWithVariance(visual,variance[pos]); }
}
