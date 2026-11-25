package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom.Sentry;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import java.util.*;

/** Host-only projection. Handles survive continuous observation, including floor-to-bag movement. */
final class SeatProjection {
    private static void writeExitTiles(Map<String,String> rows,Iterable<com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap> tiles,
                                       FloorKnowledge sight, Level level, String prefix) {
        for (com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap tile:tiles) {
            if (tile instanceof HallsBossLevel.HallsOverlay) {
                int[] frames=((HallsBossLevel.HallsOverlay)tile).frames(level,null);
                for (int i=0;i<frames.length;i++) {
                    int cell=(tile.tileY+i/tile.tileW)*level.width()+tile.tileX+i%tile.tileW;
                    if (cell>=0 && cell<level.length() && frames[i]>=0 && sight.visible(cell))
                        rows.put(prefix+cell,Integer.toString(frames[i]));
                }
                continue;
            }
            if (tile instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel.CityOverlay) {
                int[] frames=((com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel.CityOverlay)tile).frames();
                for (int i=0;i<frames.length;i++)
                    if (frames[i]>=0 && sight.visible(i)) rows.put(prefix+i,Integer.toString(frames[i]));
                continue;
            }
            if (tile instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel.CaveOverlay) {
                int[] frames=((com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel.CaveOverlay)tile).frames();
                boolean dynamic=tile instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel.ArenaVisuals;
                for (int i=0;i<frames.length;i++) {
                    int cell=(tile.tileY+i/tile.tileW)*level.width()+tile.tileX+i%tile.tileW;
                    if (frames[i]>=0 && (sight.visible(cell) || (!dynamic && sight.visited(cell))))
                        rows.put(prefix+cell,Integer.toString(frames[i]));
                }
                continue;
            }
            if (!(tile instanceof PrisonBossLevel.ExitTiles)) continue;
            PrisonBossLevel.ExitTiles exit=(PrisonBossLevel.ExitTiles)tile;
            for (int y=0;y<exit.tileH;y++) for (int x=0;x<exit.tileW;x++) {
                int cell=(exit.tileY+y)*level.width()+exit.tileX+x;
                int frame=exit.visualAt(x,y);
                if (frame>=0 && (sight.visible(cell) || sight.visited(cell))) rows.put(prefix+cell,Integer.toString(frame));
            }
        }
    }
    final int seat;
    final UUID session;
    private long nextItem;
    private IdentityHashMap<Item,Long> ids = new IdentityHashMap<>();
    private Map<Long,Item> items = Collections.emptyMap();
    StateImage previous, current;
    String witnessedDeath;

    SeatProjection(UUID session, int seat) {
        this.session = session;
        this.seat = seat;
        nextItem = seat + 1;
    }

    UUID epoch;
    UUID shortcutSalt = UUID.randomUUID();
    long cursor() { return nextItem; }
    void resetHandles(long start) {
        if (start < seat+1 || start % 2 != (seat+1)%2 || start > Long.MAX_VALUE-32768)
            throw new IllegalArgumentException("Invalid handle cursor");
        nextItem = start;
        ids.clear(); items=Collections.emptyMap();
        previous=null; current=null;
    }

    Item resolve(long handle) { return items.get(handle); }

    void capture(CoopSession game, long version, String status, Mob attacked,String receipt) {
        Map<String,String> rows = new TreeMap<>();
        Map<Long,Item> visibleItems = new HashMap<>();
        if (game.ending()!=CoopSession.Ending.NONE) {
            rows.put("ending",game.ending().name());
            rows.put("amuletowner",Integer.toString(game.amuletOwner()));
            if (game.ending()==CoopSession.Ending.WON && status.equals("IN_GAME")) status="WON";
        }
        boolean playing=status.equals("IN_GAME");
        boolean defeated = status.equals("PARTY_DEFEATED");
        String floorId=Dungeon.branch+","+Dungeon.depth;
        if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel)
            floorId += "," + ((com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel)Dungeon.level).state().name();
        boolean changed=current!=null && !floorId.equals(current.rows.get("floor"));
        if(changed) resetHandles(nextItem);
        rows.put("floor",floorId);
        if(Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel) rows.put("mine",Integer.toString(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.Type()));
        rows.put("status",playing || defeated || status.equals("WON") ? status : "PAUSED");
        if (epoch!=null) rows.put("epoch",epoch.toString());
        rows.put("life",game.life(seat).name());
        rows.put("revivable",game.life(seat) == CoopSession.Life.DOWNED ? "1" : "0");
        if (!playing && !defeated) rows.put("reason",status);
        if (receipt!=null) rows.put("receipt",receipt);
        rows.put("clock",Float.toString(Statistics.duration+com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now()));
        rows.put("turn",Long.toString(game.turn()));
        rows.put("active",Integer.toString(playing ? game.activeSeat() : -1));
        witnessedDeath = null;
        if (playing || status.equals("RECONNECTING")) {
            Hero hero = game.hero(seat);
            Level level = Dungeon.level;
            FloorKnowledge sight = game.observe(seat);
            writeMemory(rows, sight);
            if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel) {
                com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room room =
                        ((com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel)level).room(hero.pos);
                if (room instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom) {
                    String warning=((com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom)room).warningFor(hero);
                    if (warning!=null) rows.put("vaultwarning",warning);
                }
                if (hero.HP < hero.HT*0.334f) rows.put("vaultinjured","1");
                for (Mob mob:level.mobs) {
                    if (!(mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental) || !sight.visible(mob.pos)) continue;
                    com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental boss =
                            (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental)mob;
                    rows.put("element"+mob.pos,boss.curForm().name());
                    for (int cell:boss.warnedCells()) if (sight.visible(cell)) rows.put("aim"+cell,"1");
                }
            }
            for (com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap tile : level.customTiles) {
                if (!(tile instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.GooBossRoom.GooNest)) continue;
                com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.GooBossRoom.GooNest nest =
                        (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.GooBossRoom.GooNest)tile;
                for (int y=0;y<nest.tileH;y++) for (int x=0;x<nest.tileW;x++) {
                    int cell=(nest.tileY+y)*level.width()+nest.tileX+x;
                    int sprite=nest.visualAt(x,y);
                    if (sprite>=0 && (sight.visible(cell) || sight.visited(cell)))
                        rows.put("nest"+cell,Integer.toString(sprite));
                }
            }
            if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel) {
                for (Mob resident:level.mobs) if (resident instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing)
                    for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing.Summoning cast:resident.buffs(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing.Summoning.class))
                        if (sight.visible(cast.getPos())) rows.put("summon"+cast.getPos(),Integer.toString(cast.warningVisual()));
            }
            if (level instanceof HallsBossLevel) {
                for (Mob mob:level.mobs) if (mob instanceof YogDzewa) {
                    for (int target:((YogDzewa)mob).aimedCells()) {
                        com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica ray=new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(mob.pos,target,com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.WONT_STOP);
                        for (int cell:ray.path) if (sight.visible(cell)) rows.put("aim"+cell,"1");
                    }
                }
            }
            if (level instanceof HallsBossLevel || level instanceof LastLevel || level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel || level instanceof PrisonBossLevel || level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel) {
                writeExitTiles(rows,level.customTiles,sight,level,"exitfloor");
                writeExitTiles(rows,level.customWalls,sight,level,"exitwall");
            }
            int stair=game.stairs.stair();
            if(stair>=0) {
                rows.put("stair",stair+","+(game.stairs.agreed()?1:0));
                if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel) rows.put("vaultscore",Integer.toString(game.stairs.vaultScore()));
                else if (level.getTransition(stair).destBranch!=Dungeon.branch) rows.put("branchgate","1");
            }
            for (BitSet area : CombatZones.calculate(game, sight)) {
                for (int cell = area.nextSetBit(0); cell >= 0; cell = area.nextSetBit(cell+1)) {
                    int mask = CombatZones.edgeMask(area, cell, level.width());
                    if (mask != 0) rows.put("z"+cell, Integer.toString(mask));
                }
            }
            rows.put("keys",Integer.toString(com.shatteredpixel.shatteredpixeldungeon.journal.Notes.keyCount(
                    new com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey(Dungeon.depth))));
            rows.put("width",Integer.toString(level.width()));
            rows.put("height",Integer.toString(level.height()));
            rows.put("shield",Integer.toString(hero.shielding()));
            rows.put("self",hero.id()+","+hero.pos+","+hero.HP+","+hero.HT+","+hero.heroClass.name()+","+hero.lvl+","+hero.exp);
            rows.put("selftier",Integer.toString(hero.tier()));
            rows.put("strength",Integer.toString(hero.STR()));
            rows.put("subclass",hero.subClass.name());
            for (Map<Talent,Integer> tier:hero.talents) for (Map.Entry<Talent,Integer> talent:tier.entrySet()) {
                if (talent.getValue()>0) rows.put("talent"+talent.getKey().ordinal(),talent.getKey().name()+","+talent.getValue());
            }
            Hero ally = game.hero(1-seat);
            if (sight.visible(ally.pos) && ally.invisible <= 0) {
                rows.put("ally",ally.id()+","+ally.pos+","+ally.heroClass.name());
                rows.put("allytier",Integer.toString(ally.tier()));
                rows.put("allylife",game.life(1-seat).name());
            }
            for (Buff buff : hero.buffs()) {
                if (buff.type==Buff.buffType.NEGATIVE && buff.icon()!=BuffIndicator.NONE) rows.put("hazard","1");
                if (buff.icon() != BuffIndicator.NONE) rows.put("b"+buff.id(),buff.icon()+","+
                        (buff instanceof FlavourBuff ? Float.toString(buff.visualcooldown()) : "NA"));
            }
            com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper shop =
                    com.shatteredpixel.shatteredpixeldungeon.CoopTrade.merchant(hero,true);
            if (shop!=null && !sight.visible(shop.pos)) shop=null;
            if (Dungeon.depth>=6) rows.put("gold",Integer.toString(Dungeon.gold));
            if (shop!=null) {
                rows.put("merchant",Integer.toString(shop.pos));
                for (Item sold:shop.buybackItems) if (game.ownership.owner(sold)==game.player(seat)) {
                    long handle=tradeHandle(rows,visibleItems,sold);
                    rows.put("refund"+handle,sold.image()+","+sold.quantity()+","+sold.value()+","+(com.shatteredpixel.shatteredpixeldungeon.CoopTrade.roomFor(hero,sold)?1:0));
                }
            }
            StenchGas stench = (StenchGas)level.blobs.get(StenchGas.class);
            for (int cell=0;cell<sight.length();cell++) {
                if (!sight.visible(cell)) continue;
                rows.put("t"+cell,Integer.toString(sight.terrain(cell)));
                if (stench != null && stench.volume>0 && stench.cur[cell]>0) rows.put("stench"+cell,"1");
                com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap trap = level.traps.get(cell);
                Plant plant = level.plants.get(cell);
                if (trap == null && plant != null) rows.put("plant"+cell, Integer.toString(plant.image));
                if (trap != null && (trap.visible || level instanceof PrisonBossLevel && ((PrisonBossLevel)level).showingTrapHints()
                        && trap instanceof com.shatteredpixel.shatteredpixeldungeon.levels.traps.TenguDartTrap)) {
                    int color = trap.active ? trap.color : com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap.BLACK;
                    rows.put("trap"+cell,Integer.toString(color + 16*trap.shape));
                }
                int clouds = (Blob.volumeAt(cell, Fire.class)>0 || Blob.volumeAt(cell, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu.FireAbility.FireBlob.class)>0 ? 1 : 0)
                        | (Blob.volumeAt(cell, Freezing.class)>0 ? 2 : 0)
                        | (Blob.volumeAt(cell, Electricity.class)>0 || Blob.volumeAt(cell,com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel.PylonEnergy.class)>0 || Blob.volumeAt(cell, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu.ShockerAbility.ShockerBlob.class)>0 ? 4 : 0)
                        | (Blob.volumeAt(cell, ToxicGas.class)>0 ? 8 : 0)
                        | (Blob.volumeAt(cell, ConfusionGas.class)>0 ? 16 : 0)
                        | (Blob.volumeAt(cell, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas.class)>0 ? 32 : 0);
                if (Blob.volumeAt(cell, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Web.class)>0)
                    rows.put("web"+cell,"1");
                if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel)
                    for (Mob caster:level.mobs) if (caster instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300)
                        for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300.FallingRockBuff rocks:caster.buffs(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300.FallingRockBuff.class))
                            if (rocks.targetsCell(cell)) rows.put("rocks"+cell,"1");
                if (clouds != 0) rows.put("cloud"+cell,Integer.toString(clouds));
                Heap heap = level.heaps.get(cell);
                if (heap != null && heap.hidden) continue;
                if (heap != null && heap.type == Heap.Type.HEAP && heap.peek()!=null) {
                    putItem(rows,visibleItems,heap.peek(),"F"+cell);
                } else if (heap!=null && heap.type==Heap.Type.FOR_SALE && heap.peek()!=null) {
                    Item stock=heap.peek();
                    rows.put("sale"+cell,Integer.toString(stock.image()));
                    if (com.shatteredpixel.shatteredpixeldungeon.CoopTrade.supported(stock) && heap.size()==1 && game.ownership.allowed(stock,game.playerOf(hero))) {
                        long handle=tradeHandle(rows,visibleItems,stock);
                        rows.put("quote"+cell,handle+","+com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper.sellPrice(stock)+","+stock.quantity()+","+(com.shatteredpixel.shatteredpixeldungeon.CoopTrade.roomFor(hero,stock)?1:0));
                    }
                } else if (heap != null) rows.put("h"+cell,
                        (Dungeon.depth>=7 && heap.type==Heap.Type.SKELETON) || heap.type==Heap.Type.CHEST || heap.type==Heap.Type.CRYSTAL_CHEST || heap.type==Heap.Type.LOCKED_CHEST ? heap.type.name() : "CONTAINER");
            }
            for (Mob mob : level.mobs) {
                if (sight.visible(mob.pos) && mob.invisible <= 0 && mob.isAlive() && CoopSession.basicMob(mob)) {
                    if (CoopSession.disguised(mob)) {
                        Heap.Type chest = Heap.Type.CHEST;
                        if (mob instanceof CrystalMimic) chest = Heap.Type.CRYSTAL_CHEST;
                        if (mob instanceof GoldenMimic) chest = Heap.Type.LOCKED_CHEST;
                        rows.put("h"+mob.pos, chest.name());
                        continue;
                    }
                    if (mob.buff(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer.RockArmor.class) != null)
                        rows.put("rockarmor"+mob.pos, "1");
                    if (Dungeon.branch == 1 && mob.spriteClass != null)
                        rows.put("minesprite"+mob.id(), mob.spriteClass.getSimpleName());
                    rows.put("m"+mob.id(),mob.pos+","+mob.getClass().getSimpleName()+","+mob.HP+","+mob.HT);
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye && ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye)mob).beamCharged)
                        rows.put("charge"+mob.pos,"1");
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon) {
                        int destination=((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon)mob).preparedLanding();
                        if (destination>=0) {
                            rows.put("charge"+mob.pos,"1");
                            if (level.insideMap(destination) && sight.visible(destination)) rows.put("aim"+destination,"1");
                        }
                    }
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo && ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo)mob).chargeStage()>0)
                        rows.put("charge"+mob.pos,Integer.toString(((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo)mob).chargeStage()));
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer
                            && ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer)mob).summoning)
                        rows.put("charge"+mob.pos,"1");
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue)
                        rows.put("mail"+mob.id(),Integer.toString(((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue)mob).armor().tier));
                    if ((mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon && mob.alignment==com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY)
                            || (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300 && ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300)mob).supercharged))
                        rows.put("charge"+mob.pos,"1");
                    if (level.adjacent(hero.pos,mob.pos)) {
                        if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror)
                            rows.put("vaultnpc"+mob.pos,((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror)mob).reward==null ? "scene_nothing" : "take");
                        if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor) {
                            Item tokens=hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken.class);
                            rows.put("vaultnpc"+mob.pos,tokens==null ? "no_tokens" : tokens.quantity()<10 ? "too_few_tokens" : "open");
                        }
                    }
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp && level.adjacent(hero.pos,mob.pos)
                            && !com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.isOld())
                        rows.put("imp"+mob.pos,com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp.Quest.given() ? "quest_in_progress" : "quest_intro_1");
                    if (mob instanceof Blacksmith && level.adjacent(hero.pos,mob.pos)) {
                        String type = Blacksmith.Quest.Type()==Blacksmith.Quest.CRYSTAL ? "crystal" : "gnoll";
                        if (Blacksmith.Quest.completed()) {
                            rows.put("smith"+mob.pos,"services");
                            rows.put("favor",Integer.toString(Blacksmith.Quest.favor));
                            if (CoopSmith.pendingChoice()) {
                                int index=0;
                                for(Item candidate:Blacksmith.Quest.smithRewards)
                                    rows.put("smithoffer"+index++,com.shatteredpixel.shatteredpixeldungeon.CoopTrade.label(candidate));
                            } else {
                                int favor=Blacksmith.Quest.favor;
                                int pickCost=Blacksmith.Quest.freePickaxe?0:250;
                                if (Blacksmith.Quest.pickaxe!=null && game.ownership.owner(Blacksmith.Quest.pickaxe)==game.player(seat)
                                        && favor>=pickCost) rows.put("pricepick",""+pickCost);
                                int harden=500+1000*Blacksmith.Quest.hardens, reforge=500+1000*Blacksmith.Quest.reforges;
                                int upgrade=1000+1000*Blacksmith.Quest.upgrades;
                                if (favor>=harden) rows.put("priceharden",""+harden);
                                if (favor>=reforge) rows.put("pricereforge",""+reforge);
                                if (favor>=upgrade) rows.put("priceupgrade",""+upgrade);
                                if (favor>=2000) rows.put("priceorder","2000");
                                if (favor>0) rows.put("pricecash",""+favor);
                            }
                        } else rows.put("smith"+mob.pos, (Blacksmith.Quest.given() ? "reminder_" : "intro_quest_")+type);
                    }
                    if (mob instanceof Wandmaker && level.adjacent(hero.pos, mob.pos)) {
                        String offer = Wandmaker.Quest.offerFor(hero);
                        if (offer != null) rows.put("wandquest"+mob.pos, offer);
                    }
                    if (mob instanceof Ghost && level.adjacent(hero.pos,mob.pos))
                        rows.put("quest"+mob.pos,Ghost.Quest.sewerOffer());
                    if (mob instanceof Sentry
                            && ((Sentry)mob).charging())
                        rows.put("charge"+mob.pos,"1");
                }
            }
            if (attacked != null && !attacked.isAlive() && sight.visible(attacked.pos) && current != null) {
                String key = "m"+attacked.id();
                String old = current.rows.get(key);
                if (old != null && old.startsWith(attacked.pos+",")) witnessedDeath = key;
            }
            int selection=0;
            for (Item item : hero.belongings) {
                selection++;
                putItem(rows,visibleItems,item,slot(hero.belongings,item));
                if (game.ownership.owner(item)==game.player(seat)) {
                    if (rows.containsKey("vaultscore") && com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.rewardAllowed(item,Integer.parseInt(rows.get("vaultscore")))) rows.put("vaultpick"+ids.get(item),"1");
                    if (shop!=null && com.shatteredpixel.shatteredpixeldungeon.CoopTrade.maySell(hero,item))
                        rows.put("sell"+ids.get(item),item.value()+","+(com.shatteredpixel.shatteredpixeldungeon.CoopTrade.sellOne(item) ? 1 : 0));
                    if (CoopSession.basicMissile(item) && item.isIdentified()) {
                        String family=item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife ? "knife" : "stone";
                        rows.put("family"+ids.get(item),family+"."+StateImage.hash(shortcutSalt+"/"+family+"/"+
                                ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)item).setID).substring(0,32));
                    }
                    if (rows.containsKey("favor") && CoopSmith.eligible(item) && CoopSession.equipmentForScroll(item)) {
                        rows.put("forge"+ids.get(item),selection+","+(CoopSmith.canHarden(item)?1:0)+","+(item.level()<2?1:0));
                    }
                    if (CoopSession.equipmentForScroll(item))
                        rows.put("select"+ids.get(item),selection+","+(item.isUpgradable()?1:0)+","+(item.isIdentified()?0:1));
                    // Only an owned item in this seat's actual inventory gets a shortcut reference.
                    String token=StateImage.hash(shortcutSalt+"/item/"+game.ownership.identity(item)).substring(0,32);
                    rows.put("ref"+ids.get(item),token);
                }
            }
            int tab=0;
            for (com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag bag:hero.belongings.getBags()) {
                rows.put("bagtab"+tab,bag.getClass().getSimpleName());
                int order=0;
                for (Item item:bag.items) {
                    Long handle=ids.get(item);
                    if (handle!=null && !(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag))
                        rows.put("pocket"+handle,tab+","+order++);
                }
                tab++;
            }
        }
        ids.keySet().retainAll(visibleItems.values());
        items = visibleItems;
        previous = current;
        current = new StateImage(session,seat,version,rows);
    }

    private static void writeMemory(Map<String,String> rows, FloorKnowledge memory) {
        // Each group fits a single bounded SYNC1 value even on a fully explored floor.
        for (int start = 0; start < memory.length(); start += StateImage.TILE_GROUP) {
            StringBuilder group = new StringBuilder();
            boolean known = false;
            for (int cell = start; cell < start + StateImage.TILE_GROUP; cell++) {
                int tile = cell < memory.length() && !memory.visible(cell) ? memory.terrain(cell) : -1;
                if (tile < 0) group.append("..");
                else {
                    known = true;
                    group.append(memory.visited(cell) ? 'v' : 'p');
                    group.append(StateImage.TILES.charAt(tile));
                }
            }
            if (known) rows.put("r" + start / StateImage.TILE_GROUP, group.toString());
        }
    }

    private long tradeHandle(Map<String,String> rows,Map<Long,Item> visible,Item item) {
        long id=handle(visible,item);
        String label=com.shatteredpixel.shatteredpixeldungeon.CoopTrade.label(item);
        if (!label.isEmpty()) rows.put("tradelabel"+id,label);
        return id;
    }

    private long handle(Map<Long,Item> visible, Item item) {
        Long id = ids.get(item);
        if (id == null) {
            id = nextItem;
            if (nextItem > Long.MAX_VALUE-2) throw new IllegalStateException("Item handle exhaustion");
            nextItem += 2;
            ids.put(item,id);
        }
        visible.put(id,item);
        return id;
    }

    private void putItem(Map<String,String> rows, Map<Long,Item> visible, Item item,String slot) {
        long id=handle(visible,item);
        String kind = item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Honeypot ? "HONEYPOT"
                : item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Honeypot.ShatteredPot ? "SHELL"
                : item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow ? "BOW"
                : item.getClass()==com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.class && ((com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion)item).isKnown() ? "HEALING"
                : CoopSession.basicArmorType(item) ? "ARMOR" : CoopSession.basicFood(item) ? "FOOD" : item instanceof Ankh ? "ANKH" : CoopSession.basicWeaponType(item) ? "MELEE" : "OTHER";
        if (!slot.startsWith("F") && item.isIdentified() && CoopSession.basicArmorType(item)
                && ((com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)item).glyph!=null) kind="INSCRIBED";
        if (!slot.startsWith("F") && CoopSession.supportedScroll(item)
                && ((com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll)item).isKnown())
            kind=item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade ? "UPGRADE" : "IDENTIFY";
        if (!slot.startsWith("F") && CoopSession.basicMissile(item) && item.isIdentified())
            kind=item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife ? "KNIFE" : "STONE";
        if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle) kind="CANDLE";
        String charges="";
        if (!slot.startsWith("F") && CoopSession.supportedHandheldWand(item)) {
            com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand = (com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)item;
            kind = "WAND";
            charges = "," + wand.curCharges + "," + wand.maxCharges;
        } else if (!slot.startsWith("F") && item instanceof MagesStaff) {
            WandOfMagicMissile wand=((MagesStaff)item).starterWand();
            if (wand!=null) { kind="STAFF"; charges=","+wand.curCharges+","+wand.maxCharges; }
        }
        if (!slot.startsWith("F") && !kind.equals("OTHER") && item.getClass().getEnclosingClass()==null) {
            String label=item.getClass().getName();
            String prefix="com.shatteredpixel.shatteredpixeldungeon.";
            if (label.startsWith(prefix+"items."))
                rows.put("label"+id,label.substring(prefix.length()).toLowerCase(Locale.ROOT));
        }
        rows.put("i"+id,slot+","+kind+","+item.image()+","+item.quantity()+","+
                (item.levelKnown ? item.visiblyUpgraded() : "NA")+charges);
    }

    private static String slot(Belongings b, Item item) {
        if (item == b.weapon) return "WEAPON";
        if (item == b.armor) return "ARMOR";
        if (item == b.ring) return "RING";
        if (item == b.misc) return "MISC";
        if (item == b.artifact) return "ARTIFACT";
        if (item == b.secondWep) return "SECOND";
        return "BAG";
    }
}
