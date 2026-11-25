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
    private final int[] nest,exitWalls;
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
        solo=level; nest=null; exitWalls=null;
        width=level.width(); height=level.height(); depth=Dungeon.depth; branch=Dungeon.branch;
        mining=level instanceof MiningLevel; hallsBoss=level instanceof HallsBossLevel;
        lastShop=level instanceof LastShopLevel;
        tiles=level.map; visible=level.heroFOV; visited=level.visited; mapped=level.mapped;
        discoverable=level.discoverable; variance=DungeonTileSheet.tileVariance;
    }

    static DungeonRenderMap solo() { return new DungeonRenderMap(Dungeon.level); }

    public DungeonRenderMap(PublicDungeon view) {
        solo=null;
        width=view.width; height=view.height; depth=view.depth; branch=view.branch;
        mining=branch==1 && depth>=12 && depth<=14; hallsBoss=depth==25 && branch==0; lastShop=false;
        int length=width*height;
        nest=new int[length]; exitWalls=new int[length];
        tiles=new int[length]; visible=new boolean[length]; visited=new boolean[length];
        mapped=new boolean[length]; discoverable=new boolean[length]; variance=new byte[length];
        // This seed controls artwork only. It cannot reconstruct the private dungeon generator.
        Random cosmetic=new Random(view.source.session.getLeastSignificantBits() ^ depth);
        for (int i=0;i<length;i++) variance[i]=(byte)cosmetic.nextInt(100);
        apply(view);
    }

    public boolean apply(PublicDungeon view) {
        if (view.width!=width || view.height!=height || view.depth!=depth || view.branch!=branch) throw new IllegalArgumentException("Different floor");
        boolean changed=false;
        changedCells.setEmpty();
        for (int i=0;i<tiles.length;i++) {
            int terrain=view.terrain(i);
            boolean seen=view.visible(i), mapOnly=!seen && terrain>=0 && view.source.mappedOnly(i);
            String detail=view.source.rows.get(((depth==10 || depth==15 || depth==20 || depth==25 || depth==26) ? "exitfloor" : "nest")+i);
            int frame=terrain>=0 && !mapOnly && detail!=null ? Integer.parseInt(detail) : -1;
            if (depth==26 && (frame==46 || frame==54) && variance[i]>=50) frame++;
            String wall=view.source.rows.get("exitwall"+i);
            int wallFrame=(depth==10 || depth==15 || depth==20 || depth==25 || depth==26) && terrain>=0 && !mapOnly && wall!=null ? Integer.parseInt(wall) : -1;
            if (wallFrame < -1 || wallFrame>511 || frame < -1 || frame > (depth>=25 ? 63 : depth==20 ? 255 : depth==10 ? 175 : depth==15 ? 63 : 8)) throw new IllegalArgumentException("Nest frame");
            if (exitWalls[i]!=wallFrame || nest[i]!=frame || tiles[i]!=terrain || visible[i]!=seen || mapped[i]!=mapOnly
                    || feature(i)!=feature(view,i)) {
                changed=true;
                changedCells.union(i%width,i/width);
            }
            nest[i]=frame; exitWalls[i]=wallFrame;
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
        return customLayer(depth>=25 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.HALLS_SP : depth==20 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.CITY_BOSS : depth==15 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.CAVES_BOSS : depth==10 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.PRISON_EXIT : com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.SEWER_BOSS,nest);
    }
    public com.watabou.noosa.Tilemap exitWallsLayer() {
        return customLayer(depth>=25 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.HALLS_SP : depth==20 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.CITY_BOSS : depth==15 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.CAVES_BOSS : com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.PRISON_EXIT,exitWalls);
    }
    private com.watabou.noosa.Tilemap customLayer(String texture,int[] frames) {
        com.watabou.noosa.Tilemap layer=new com.watabou.noosa.Tilemap(texture,new com.watabou.noosa.TextureFilm(texture,16,16));
        layer.map(frames,width);
        return layer;
    }
    String tilesTexture() {
        if (!isSeat()) return solo.tilesTex();
        if (mining) return "1".equals(seat.source.rows.get("mine"))
                ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_CAVES_CRYSTAL
                : com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_CAVES_GNOLL;
        if (depth>=21) return com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_HALLS;
        if (depth>=16) return com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_CITY;
        return depth>=11 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_CAVES : depth>=6 ? com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_PRISON
                : com.shatteredpixel.shatteredpixeldungeon.Assets.Environment.TILES_SEWERS;
    }
    boolean isSeat() { return solo==null; }
    boolean pit(int cell) { return solo==null ? tiles[cell]==Terrain.CHASM : solo.pit[cell]; }
    boolean inside(int cell) { return cell>=width && cell<tiles.length-width && cell%width>0 && cell%width<width-1; }
    int alt(int visual,int pos) { return DungeonTileSheet.visualWithVariance(visual,variance[pos]); }
}
