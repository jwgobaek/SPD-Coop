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
    private static final int[] CLOUD_COLORS = {0x77ED6525, 0x667EDFFF, 0x66E5E067, 0x664AA940, 0x669E60CD, 0x778D8B36};
    interface Input { void tap(PublicDungeon view, int cell); }

    private final DungeonRenderMap map;
    private final Tilemap[] layers;
    private final FogOfWar fog;
    private final SkinnedBlock water;
    private final Map<String,com.watabou.noosa.particles.Emitter> webs = new HashMap<>();
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
        water = new SkinnedBlock(view.width*TILE, view.height*TILE, view.depth>=21 ? Assets.Environment.WATER_HALLS : view.depth>=16 ? Assets.Environment.WATER_CITY : view.depth>=11 ? Assets.Environment.WATER_CAVES : view.depth>=6 ? Assets.Environment.WATER_PRISON : Assets.Environment.WATER_SEWERS);
        water.autoAdjust = true;
        add(water);
        layers = new Tilemap[]{
                new DungeonTerrainTilemap(map), map.nestLayer(), new WallOcclusionTilemap(map), new TerrainFeaturesTilemap(map),
                new RaisedTerrainTilemap(map), new DungeonWallsTilemap(map), map.exitWallsLayer(), new WallBlockingTilemap(map)};
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
                && java.util.Objects.equals(view.source.rows.get("floor"),next.source.rows.get("floor")) && view.width==next.width && view.height==next.height;
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

        for (Iterator<Map.Entry<String,com.watabou.noosa.particles.Emitter>> it=webs.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,com.watabou.noosa.particles.Emitter> web=it.next();
            if (!view.source.rows.containsKey(web.getKey())) {
                markings.remove(web.getValue());
                web.getValue().destroy();
                it.remove();
            }
        }
        Set<String> keep = new HashSet<>();
        for (Map.Entry<String,String> row : view.source.rows.entrySet()) {
            String key=row.getKey(), value=row.getValue();
            if (key.length()<2 || !Character.isDigit(key.charAt(key.length()-1))) continue;
            if (key.startsWith("sense") && value.equals("mob")) {
                int cell = Integer.parseInt(key.substring(5));
                object(keep, key, cell, () -> new com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon(
                        com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.MIND_VISION, true));
            } else if (key.startsWith("aim")) {
                int cell=Integer.parseInt(key.substring(3));
                if (view.visible(cell)) object(keep,key,cell,()->{
                    Image target=com.shatteredpixel.shatteredpixeldungeon.ui.Icons.get(com.shatteredpixel.shatteredpixeldungeon.ui.Icons.TARGET);
                    target.hardlight(0xFF0000);
                    return target;
                });
            } else if (key.charAt(0)=='m' && Character.isDigit(key.charAt(1))) {
                String[] mob = value.split(",");
                int cell = Integer.parseInt(mob[0]);
                boolean charged=view.source.rows.containsKey("charge"+cell);
                boolean bossForm=charged && ("Pylon".equals(mob[1]) || "DM300".equals(mob[1]));
                String form=view.source.rows.get("element"+cell);
                String wardTier=view.source.rows.get("wardtier"+key.substring(1));
                Image sprite=object(keep, key+":"+mob[1]+(wardTier==null?"":":"+wardTier)+(bossForm?":active":"")+(form==null?"":form),cell,()->{
                    Image result=monster(mob[1], view.source.rows.get("minesprite"+key.substring(1)), wardTier);
                    if (result instanceof VaultBossElementalSprite && form!=null)
                        ((VaultBossElementalSprite)result).setForm(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental.ElementalForm.valueOf(form));
                    if ("ArmoredStatue".equals(mob[1])) {
                        String tier=view.source.rows.get("mail"+key.substring(1));
                        if (tier!=null) ((StatueSprite)result).setArmor(Integer.parseInt(tier));
                    }
                    if (bossForm && result instanceof PylonSprite) ((PylonSprite)result).activate();
                    if (bossForm && result instanceof DM300Sprite) ((DM300Sprite)result).updateChargeState(true);
                    return result;
                });
                boolean charging=view.source.rows.containsKey("charge"+cell);
                if (sprite instanceof EyeSprite) ((EyeSprite)sprite).displayPreparation(charging);
                else if (sprite instanceof RipperSprite) ((RipperSprite)sprite).displayPreparation(charging);
                else if (sprite instanceof GooSprite) ((GooSprite)sprite).displayCharge(charging);
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
            } else if (key.startsWith("lightwall")) {
                int cell = Integer.parseInt(key.substring(9));
                if (!webs.containsKey(key)) {
                    com.watabou.noosa.particles.Emitter emitter = new com.watabou.noosa.particles.Emitter();
                    markings.add(emitter);
                    emitter.pos(center(cell));
                    emitter.pour(com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.WhiteParticle.WALL, 0.02f);
                    webs.put(key,emitter);
                }
                keep.add(key);
            } else if (key.startsWith("web") || key.startsWith("rocks") || key.startsWith("summon")) {
                boolean rocks=key.startsWith("rocks");
                boolean summon=key.startsWith("summon");
                int cell=Integer.parseInt(key.substring(summon?6:rocks?5:3));
                if (!webs.containsKey(key)) {
                    com.watabou.noosa.particles.Emitter emitter=new com.watabou.noosa.particles.Emitter();
                    markings.add(emitter);
                    emitter.pos(center(cell));
                    if (summon) com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing.Summoning.emitWarning(emitter,Integer.parseInt(value));
                    else if (rocks) emitter.pour(com.shatteredpixel.shatteredpixeldungeon.effects.particles.EarthParticle.FALLING,0.1f);
                    else emitter.pour(com.shatteredpixel.shatteredpixeldungeon.effects.particles.WebParticle.FACTORY,0.25f);
                    webs.put(key,emitter);
                }
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
        if (advanced && receipt!=null && (cue.equals("ZAP") || cue.equals("SHOOT") || cue.equals("THROW") || cue.equals("KNIFE") || cue.equals("SPIKE") || cue.equals("DART") || cue.equals("ATTACK"))) {
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
        if (advanced && ("SHOOT".equals(cue) || "THROW".equals(cue) || "KNIFE".equals(cue) || "SPIKE".equals(cue) || "DART".equals(cue)) && facing!=view.pos) {
            MissileSprite shot=(MissileSprite)effects.recycle(MissileSprite.class);
            shot.seatShot(center(view.pos),center(facing),cue);
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

    private static Image monster(String monster, String color, String wardTier) {
                Image sprite;
                switch(monster) {
                    case "VaultDM100": sprite=new DM100Sprite.Vault();break;
                    case "VaultDM200": sprite=new DM200Sprite.Vault();break;
                    case "VaultSkeleton": sprite=new SkeletonSprite.Vault();break;
                    case "VaultGhoul": sprite=new GhoulSprite();break;
                    case "VaultGolem": sprite=new GolemSprite();break;
                    case "VaultSentry": sprite=new SentrySprite.VaultScan();break;
                    case "VaultLaser": sprite=new SentrySprite.VaultLaser();break;
                    case "VaultMirror": sprite=new VaultMirrorSprite();break;
                    case "VaultTokenDoor": sprite=new VaultTokenDoorSprite();break;
                    case "Eye": sprite=new EyeSprite();break;
                    case "DemonSpawner": sprite=new SpawnerSprite();break;
                    case "YogRipper": case "RipperDemon": sprite=new RipperSprite();break;
                    case "Scorpio": sprite=new ScorpioSprite();break;
                    case "Acidic": sprite=new AcidicSprite();break;
                    case "YogDzewa": sprite=new YogSprite();break;
                    case "Larva": sprite=new LarvaSprite();break;
                    case "BrightFist": sprite=new FistSprite.Bright();break;
                    case "DarkFist": sprite=new FistSprite.Dark();break;
                    case "RustedFist": sprite=new FistSprite.Rusted();break;
                    case "RottingFist": sprite=new FistSprite.Rotting();break;
                    case "SoiledFist": sprite=new FistSprite.Soiled();break;
                    case "BurningFist": sprite=new FistSprite.Burning();break;
                    case "Succubus": sprite=new SuccubusSprite();break;
                    case "Crab": sprite=new CrabSprite();break;
                    case "VaultBossElemental": sprite=new VaultBossElementalSprite();break;
                    case "HermitCrab": sprite=new HermitCrabSprite();break;
                    case "Slime": sprite=new SlimeSprite();break;
                    case "Swarm": sprite=new SwarmSprite();break;
                    case "CausticSlime": sprite=new CausticSlimeSprite();break;
                    case "Thief": sprite=new ThiefSprite();break;
                    case "Guard": sprite=new GuardSprite();break;
                    case "Bat": sprite=new BatSprite();break;
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
                    case "Brute": sprite=new BruteSprite();break;
                    case "ArmoredBrute": sprite=new ShieldedSprite();break;
                    case "VaultShaman": sprite=new ShamanSprite.Vault();break;
                    case "RedShaman": sprite=new ShamanSprite.Red();break;
                    case "CrystalGuardian": sprite="Green".equals(color) ? new CrystalGuardianSprite.Green() : "Red".equals(color) ? new CrystalGuardianSprite.Red() : new CrystalGuardianSprite.Blue();break;
                    case "CrystalSpire": sprite="Green".equals(color) ? new CrystalSpireSprite.Green() : "Red".equals(color) ? new CrystalSpireSprite.Red() : new CrystalSpireSprite.Blue();break;
                    case "CrystalWisp": sprite="Green".equals(color) ? new CrystalWispSprite.Green() : "Red".equals(color) ? new CrystalWispSprite.Red() : new CrystalWispSprite.Blue();break;
                    case "GnollGeomancer": sprite=new GnollGeomancerSprite();break;
                    case "DwarfKing": sprite=new KingSprite();break;
                    case "DKMonk": case "Monk": sprite=new MonkSprite();break;
                    case "DKGolem": case "Golem": sprite=new GolemSprite();break;
                    case "Imp": sprite=new ImpSprite();break;
                    case "VaultFire": case "FireElemental": sprite=new ElementalSprite.Fire();break;
                    case "ShockElemental": sprite=new ElementalSprite.Shock();break;
                    case "DKGhoul": case "Ghoul": sprite=new GhoulSprite();break;
                    case "DKWarlock": case "Warlock": sprite=new WarlockSprite();break;
                    case "VaultFrost": case "FrostElemental": sprite=new ElementalSprite.Frost();break;
                    case "GnollGuard": sprite=new GnollGuardSprite();break;
                    case "GnollSapper": sprite=new GnollSapperSprite();break;
                    case "Spinner": sprite=new SpinnerSprite();break;
                    case "Blacksmith": sprite=new BlacksmithSprite();break;
                    case "DM300": sprite=new DM300Sprite();break;
                    case "Pylon": sprite=new PylonSprite();break;
                    case "DM201": sprite=new DM201Sprite();break;
                    case "DM200": sprite=new DM200Sprite();break;
                    case "BlueShaman": sprite=new ShamanSprite.Blue();break;
                    case "PurpleShaman": sprite=new ShamanSprite.Purple();break;
                    case "Tengu": sprite=new TenguSprite();break;
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
                    case "Ward":
                        WardSprite ward=new WardSprite();
                        ward.updateTier(Integer.parseInt(wardTier));
                        sprite=ward;
                        break;
                    case "Lotus": sprite=new LotusSprite();break;
                    case "EarthGuardian": sprite=new com.shatteredpixel.shatteredpixeldungeon.sprites.EarthGuardianSprite();break;
                    case "NinjaLog": sprite=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb.NinjaLogSprite();break;
                    case "AfterImage": sprite=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint.AfterImage.AfterImageSprite();break;
                    case "LightAlly": sprite=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.LightAllySprite();break;
                    case "ShadowAlly": sprite=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone.ShadowSprite();break;
                    case "HawkAlly": sprite=new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.HawkSprite();break;
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
