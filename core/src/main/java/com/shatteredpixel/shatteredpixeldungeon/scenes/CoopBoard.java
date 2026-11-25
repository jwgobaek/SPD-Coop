package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.*;
import com.shatteredpixel.shatteredpixeldungeon.tiles.*;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.utils.GameMath;
import java.util.*;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.*;
import com.watabou.utils.PointF;

/** A viewport over the verified seat map. No Char, Level or actor-linked sprite is constructed here. */
final class CoopBoard extends Group {
    private static final int TILE=16;
    private static final int[] CLOUD_COLORS = {0x77ED6525, 0x667EDFFF, 0x66E5E067, 0x664AA940, 0x669E60CD};
    interface Input { void tap(PublicDungeon view, int cell); }

    private final DungeonRenderMap map;
    private final Tilemap[] layers;
    private final FogOfWar fog;
    private final SkinnedBlock water;
    private final Group markings = new Group();
    private final Group heaps = new Group();
    private final Group actors = new Group();
    private final Group effects = new Group();
    private final PointerArea inputArea;
    private final Map<String, Image> sprites = new HashMap<>();
    private PublicDungeon view;

    CoopBoard(PublicDungeon initial, Input input) {
        view = initial;
        map = new DungeonRenderMap(view);
        float zoom = GameMath.gate(PixelScene.minZoom, PixelScene.defaultZoom + SPDSettings.zoom(), PixelScene.maxZoom);
        camera = Camera.add(Camera.createFullscreen(zoom));
        water = new SkinnedBlock(view.width*TILE, view.height*TILE, view.depth>=6 ? Assets.Environment.WATER_PRISON : Assets.Environment.WATER_SEWERS);
        water.autoAdjust = true;
        add(water);
        layers = new Tilemap[]{
                new DungeonTerrainTilemap(map), map.nestLayer(), new WallOcclusionTilemap(map), new TerrainFeaturesTilemap(map),
                new RaisedTerrainTilemap(map), new DungeonWallsTilemap(map), new WallBlockingTilemap(map)};
        for (int i=0; i<layers.length; i++) {
            if (i==4) { add(markings); add(heaps); add(actors); }
            add(layers[i]);
        }
        add(effects);
        fog = new FogOfWar(map);
        add(fog);
        inputArea = new PointerArea(0, 0, camera.width, camera.height) {
            private final PointF last=new PointF();
            private boolean dragged;
            @Override protected void onPointerDown(PointerEvent e) {
                last.set(e.current); dragged=false;
            }
            @Override protected void onDrag(PointerEvent e) {
                if (!dragged && PointF.distance(e.start,e.current)<4*camera().zoom) return;
                dragged=true;
                Camera c=camera();
                float x=c.scroll.x+c.width/2f+(last.x-e.current.x)/c.zoom;
                float y=c.scroll.y+c.height/2f+(last.y-e.current.y)/c.zoom;
                c.snapTo(GameMath.gate(0,x,view.width*TILE),GameMath.gate(0,y,view.height*TILE));
                last.set(e.current);
                trackViewport();
            }
            @Override protected void onClick(PointerEvent e) {
                if (dragged) return;
                PointF point = camera().screenToCamera((int)e.current.x, (int)e.current.y);
                int x = (int)Math.floor(point.x/TILE), y = (int)Math.floor(point.y/TILE);
                if (x>=0 && x<view.width && y>=0 && y<view.height) input.tap(view, x+y*view.width);
            }
        };
        add(inputArea);
        refresh(initial, "",false);
    }

    private void trackViewport() {
        inputArea.x=camera.scroll.x; inputArea.y=camera.scroll.y;
        inputArea.width=camera.width; inputArea.height=camera.height;
    }
    void centerOnHero() { camera.snapTo(center(view.pos)); trackViewport(); }
    void zoom(int step) {
        camera.zoom(GameMath.gate(PixelScene.minZoom,camera.zoom+step,PixelScene.maxZoom));
        trackViewport();
    }
    boolean sameFloor(PublicDungeon next) {
        return view.source.session.equals(next.source.session) && view.source.seat==next.source.seat
                && view.depth==next.depth && view.width==next.width && view.height==next.height;
    }

    void refresh(PublicDungeon next, String cue, boolean advanced) {
        if (view==next && cue.isEmpty() && !sprites.isEmpty()) return;
        if (view.source != next.source && map.apply(next)) {
            com.watabou.utils.Rect area=map.changedArea();
            for (int y=area.top; y<area.bottom; y++) for (int x=area.left; x<area.right; x++) {
                int cell=y*next.width+x;
                for (Tilemap layer:layers) {
                    if (layer instanceof DungeonTilemap) ((DungeonTilemap)layer).updateVisualCell(cell);
                    else layer.updateMapCell(cell);
                }
            }
            fog.updateFog(area);
        }
        boolean follow=sprites.isEmpty() || view.pos!=next.pos;
        PublicDungeon previous=view;
        view = next;
        PointF center = center(view.pos);
        if (follow) camera.snapTo(center);
        trackViewport();

        Set<String> keep = new HashSet<>();
        for (Map.Entry<String,String> row : view.source.rows.entrySet()) {
            String key=row.getKey(), value=row.getValue();
            if (key.length()<2 || !Character.isDigit(key.charAt(key.length()-1))) continue;
            if (key.charAt(0)=='m' && Character.isDigit(key.charAt(1))) {
                String[] mob = value.split(",");
                int cell = Integer.parseInt(mob[0]);
                Image sprite=object(keep, key+":"+mob[1],cell,()->{
                    Image result=monster(mob[1]);
                    if ("ArmoredStatue".equals(mob[1])) {
                        String tier=view.source.rows.get("mail"+key.substring(1));
                        if (tier!=null) ((StatueSprite)result).setArmor(Integer.parseInt(tier));
                    }
                    return result;
                });
                boolean charging=view.source.rows.containsKey("charge"+cell);
                if (sprite instanceof GooSprite) ((GooSprite)sprite).displayCharge(charging);
                else if (sprite instanceof NecromancerSprite) ((NecromancerSprite)sprite).displayCharge(charging);
                else if (sprite instanceof SpectralNecromancerSprite) ((SpectralNecromancerSprite)sprite).displayCharge(charging);
                else if (sprite instanceof SentrySprite) {
                    sprite.resetColor();
                    if (charging) sprite.tint(1f,0.2f,0.2f,0.55f);
                }
            } else if (key.charAt(0)=='h' && Character.isDigit(key.charAt(1))) {
                int cell = Integer.parseInt(key.substring(1));
                if (view.floor.containsKey(cell)) continue;
                int image="SKELETON".equals(value) ? ItemSpriteSheet.BONES : "CRYSTAL_CHEST".equals(value) ? ItemSpriteSheet.CRYSTAL_CHEST :
                        "LOCKED_CHEST".equals(value) ? ItemSpriteSheet.LOCKED_CHEST : ItemSpriteSheet.CHEST;
                object(keep,key+":"+image,cell,()->new ItemSprite(image));
            } else if (key.startsWith("sale")) {
                int cell=Integer.parseInt(key.substring(4)), image=Integer.parseInt(value);
                object(keep,key+":"+image,cell,()->new ItemSprite(image));
            } else if (key.startsWith("stench") || key.startsWith("cloud")) {
                boolean stench=key.startsWith("stench");
                int cell=Integer.parseInt(key.substring(stench ? 6 : 5));
                if (stench) object(keep,key,cell,()->new ColorBlock(TILE,TILE,0x447DA63A));
                else for (int i=0;i<CLOUD_COLORS.length;i++) {
                    int color=CLOUD_COLORS[i];
                    if ((Integer.parseInt(value)&(1<<i))!=0)
                        object(keep,key+":"+i,cell,()->new ColorBlock(TILE,TILE,color));
                }
            } else if (key.charAt(0)=='z' && Character.isDigit(key.charAt(1))) {
                int cell=Integer.parseInt(key.substring(1)), mask=Integer.parseInt(value);
                for (int side=0;side<4;side++) {
                    if ((mask & (1<<side))==0) continue;
                    final boolean horizontal=(side%2)==0;
                    Image edge=object(keep,key+":"+side,cell,()->new ColorBlock(horizontal?16:1,horizontal?1:16,0x99CE7843));
                    edge.x=cell%view.width*TILE+(side==1?15:0);
                    edge.y=cell/view.width*TILE+(side==2?15:0);
                }
            }
        }
        for (Map.Entry<Integer,PublicDungeon.Loot> heap : view.floor.entrySet()) {
            PublicDungeon.Loot item=heap.getValue();
            object(keep,"i"+item.id+":"+item.sprite,heap.getKey(),()->new ItemSprite(item.sprite));
        }
        if (view.ally>=0) {
            boolean walked=advanced && previous.ally>=0 && previous.ally!=view.ally;
            avatar(keep,"ally",view.ally,view.allyClass,walked ? "MOVE" : "",walked ? previous.ally : view.ally,view.ally);
        }
        int facing=view.pos;
        String receipt=view.source.rows.get("receipt");
        if (advanced && receipt!=null && (cue.equals("ZAP") || cue.equals("SHOOT") || cue.equals("THROW") || cue.equals("KNIFE") || cue.equals("ATTACK"))) {
            String[] fields=receipt.split(",");
            if (fields.length==4) {
                int target=Integer.parseInt(fields[2]);
                if (view.visible(target)) facing=target;
            }
        }
        avatar(keep,"self",view.pos,view.heroClass,cue,advanced ? previous.pos : view.pos,facing);
        Iterator<Map.Entry<String,Image>> it=sprites.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String,Image> entry=it.next();
            if (!keep.contains(entry.getKey())) {
                Image sprite=entry.getValue();
                sprite.parent.remove(sprite); sprite.destroy(); it.remove();
            }
        }
        if (advanced && ("SHOOT".equals(cue) || "THROW".equals(cue) || "KNIFE".equals(cue)) && facing!=view.pos) {
            MissileSprite shot=(MissileSprite)effects.recycle(MissileSprite.class);
            shot.seatShot(center(view.pos),center(facing),"SHOOT".equals(cue),"KNIFE".equals(cue));
        }
        if ("ZAP".equals(cue)) {
            String[] parts=view.source.rows.get("receipt").split(",");
            if (parts.length==4) {
                int to=Integer.parseInt(parts[2]);
                if (view.visible(to)) {
                    com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile bolt=new com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile();
                    effects.add(bolt);
                    bolt.reset(com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.MAGIC_MISSILE,
                            center(view.pos),center(to),null);
                }
            }
        }
    }

    private void avatar(Set<String> keep, String role, int cell, String type, String cue, int from, int to) {
        String tier=view.source.rows.get(role+"tier");
        String life=view.source.rows.get(role.equals("self") ? "life" : "allylife");
        String key=role+":"+type+":"+tier+":"+life;
        MovieClip clip=(MovieClip)object(keep,key,cell,()->hero(type,tier,life,""));
        if (to%view.width!=from%view.width) clip.flipHorizontal=to%view.width<from%view.width;
        if (!cue.isEmpty() && "ALIVE".equals(life)) animate(clip,type,tier,life,cue);
    }

    private interface Factory { Image create(); }
    private Image object(Set<String> keep,String key,int cell,Factory factory) {
        keep.add(key);
        Image sprite=sprites.get(key);
        if(sprite==null) {
            sprite=factory.create();
            Group group;
            if (key.startsWith("self") || key.startsWith("ally") || key.startsWith("m")) group=actors;
            else if (key.startsWith("i") || key.startsWith("h")) group=heaps;
            else group=markings;
            group.add(sprite);
            sprites.put(key,sprite);
        }
        sprite.x=cell%view.width*TILE+(TILE-sprite.width())/2;
        sprite.y=cell/view.width*TILE+TILE-sprite.height();
        if (sprite instanceof CharSprite) sprite.point(((CharSprite)sprite).worldToCamera(cell,view.width,camera));
        else if (sprite instanceof ItemSprite) sprite.point(((ItemSprite)sprite).worldToCamera(cell,view.width,camera));
        else if (key.startsWith("self") || key.startsWith("ally"))
            sprite.point(DungeonTilemap.spritePosition(cell,view.width,camera,sprite.width(),sprite.height(),6/16f));
        return sprite;
    }
    private PointF center(int cell) {
        return new PointF((cell%view.width+0.5f)*TILE,(cell/view.width+0.5f)*TILE);
    }
    @Override public void update() {
        super.update();
        water.offsetTo(0,water.offsetY()-5*Game.elapsed);
    }
    @Override public void destroy() {
        Camera.remove(camera).destroy();
        super.destroy();
    }

    private static Image monster(String monster) {
                Image sprite;
                switch(monster) {
                    case "Crab": sprite=new CrabSprite();break;
                    case "HermitCrab": sprite=new HermitCrabSprite();break;
                    case "Slime": sprite=new SlimeSprite();break;
                    case "Swarm": sprite=new SwarmSprite();break;
                    case "CausticSlime": sprite=new CausticSlimeSprite();break;
                    case "Thief": sprite=new ThiefSprite();break;
                    case "Guard": sprite=new GuardSprite();break;
                    case "DM100": sprite=new DM100Sprite();break;
                    case "Necromancer": sprite=new NecromancerSprite();break;
                    case "SpectralNecromancer": sprite=new SpectralNecromancerSprite();break;
                    case "NecroSkeleton": sprite=new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer.NecroSkeleton.NecroSkeletonSprite();break;
                    case "Skeleton": sprite=new SkeletonSprite();break;
                    case "Wandmaker": sprite=new WandmakerSprite();break;
                    case "RotHeart": sprite=new RotHeartSprite();break;
                    case "RotLasher": sprite=new RotLasherSprite();break;
                    case "DustWraith": case "Wraith": sprite=new WraithSprite();break;
                    case "TormentedSpirit": sprite=new TormentedSpiritSprite();break;
                    case "NewbornFireElemental": sprite=new ElementalSprite.NewbornFire();break;
                    case "Shopkeeper": sprite=new ShopkeeperSprite();break;
                    case "RatKing": sprite=new RatKingSprite();break;
                    case "GnollTrickster": sprite=new GnollTricksterSprite();break;
                    case "GreatCrab": sprite=new GreatCrabSprite();break;
                    case "Goo":
                        sprite=new GooSprite();

                        break;
                    case "ArmoredStatue": case "Statue": sprite=new StatueSprite();break;
                    case "GnollExile": sprite=new GnollExileSprite();break;
                    case "PhantomPiranha": sprite=new PhantomPiranhaSprite();break;
                    case "Sheep": sprite=new SheepSprite();break;
                    case "GoldenMimic": sprite=new MimicSprite.Golden();break;
                    case "CrystalMimic": sprite=new MimicSprite.Crystal();break;
                    case "Mimic": sprite=new MimicSprite();break;
                    case "Bee": sprite=new BeeSprite();break;
                    case "Ghost": sprite=new GhostSprite();break;
                    case "FetidRat": sprite=new FetidRatSprite();break;
                    case "Piranha": sprite=new PiranhaSprite();break;
                    case "Sentry": sprite=new SentrySprite.Red();

                        break;
                    case "Gnoll": sprite=new GnollSprite();break;
                    case "Snake": sprite=new SnakeSprite();break;
                    case "Albino": sprite=new AlbinoSprite();break;
                    case "Rat": sprite=new RatSprite();break;
                    default: throw new IllegalArgumentException("Unknown projected monster");
                }
        return sprite;
    }
    private static MovieClip hero(String type,String tier,String life,String action) {
        MovieClip sprite=new MovieClip(HeroClass.valueOf(type).spritesheet());
        animate(sprite,type,tier,life,action);
        return sprite;
    }
    private static void animate(MovieClip sprite,String type,String tier,String life,String action) {
        int row=tier==null?0:Math.max(0,Math.min(6,Integer.parseInt(tier)));
        TextureFilm film=new TextureFilm(HeroSprite.tiers(),row,12,15);
        MovieClip.Animation idle=new MovieClip.Animation(1,true).frames(film,0,0,0,1,0,0,1,1);
        if(!"ALIVE".equals(life)) sprite.play(new MovieClip.Animation(1,false).frames(film,12));
        else {
            MovieClip.Animation move=null;
            if(action.equals("MOVE")) move=new MovieClip.Animation(20,false).frames(film,2,3,4,5,6,7);
            if(action.equals("ZAP") || action.equals("SHOOT") || action.equals("ATTACK")) move=new MovieClip.Animation(15,false).frames(film,13,14,15,0);
            if(action.equals("PICK_UP") || action.equals("REVIVE") || action.equals("EAT"))
                move=new MovieClip.Animation(8,false).frames(film,16,17,16,17);
            sprite.play(move==null?idle:move);
            sprite.listener=animation->sprite.play(idle);
        }
    }
}
