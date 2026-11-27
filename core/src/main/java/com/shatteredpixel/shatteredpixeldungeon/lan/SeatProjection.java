package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.TrinityDiscoveries;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultLaser;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultSentry;
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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
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
            if (game.won() && status.equals("IN_GAME")) status="WON";
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
            if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel
                    && ((com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel)level).hasSecretHint(hero))
                rows.put("secrethint", "1");
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
                    if (!sight.visible(mob.pos)) continue;
                    if (mob instanceof VaultLaser || mob instanceof VaultSentry) {
                        Iterable<Integer> cells = mob instanceof VaultLaser
                                ? ((VaultLaser)mob).warnedCells() : ((VaultSentry)mob).warnedCells();
                        for (int cell : cells) if (sight.visible(cell)) rows.put("aim"+cell, "1");
                        continue;
                    }
                    if (!(mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental)) continue;
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
            if (game.ending()==CoopSession.Ending.CONTINUING && game.amuletOwner()==seat) {
                int ascentGate=game.stairs.ascentGate();
                if (ascentGate>=0) rows.put("ascentgate",Integer.toString(ascentGate));
            }
            int stair=game.stairs.stair();
            if(stair>=0) {
                rows.put("stair",stair+","+(game.stairs.agreed()?1:0));
                if (game.stairs.warpPending()) {
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.WarpBeaconTracker mark=game.stairs.warpDestination();
                    rows.put("warpdestination",mark.depth+","+mark.branch);
                } else if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel) rows.put("vaultscore",Integer.toString(game.stairs.vaultScore()));
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
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff ascended =
                    hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm.AscendBuff.class);
            if (hero.armorAbility instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany
                    && com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany.getPoweredAlly(hero) != null)
                rows.put("poweredspells","1");
            if (ascended != null) rows.put("ascendedspells",ascended.divineInverventionCast ? "used" : "ready");
            int staffReach = CoopSession.disintegrationReach(hero);
            if (staffReach > 1) rows.put("staffreach", Integer.toString(staffReach));
            rows.put("strength",Integer.toString(hero.STR()));
            rows.put("subclass",hero.subClass.name());
            if (hero.subClass == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.CHAMPION)
                rows.put("weaponswap","1");
            if (hero.subClass == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.MONK) {
                com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy energy = hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.class);
                rows.put("monkenergy",Float.toString(energy==null ? 0 : energy.energy));
                if (energy!=null && energy.cooldown==0) {
                    for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.MonkAbility ability : com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.MonkAbility.abilities)
                        if (ability.usable(energy)) rows.put(ability.seatAction().substring(5).toLowerCase(java.util.Locale.ROOT),"1");
                }
                if (energy!=null && energy.cooldown==0 && com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy.MonkAbility.abilities[1].usable(energy))
                    rows.put("monkfocus","1");
            }
            if (game.mayChooseArmor(seat,false)) rows.put("crownright","1");
            if (game.mayClaimSubclass(seat)) rows.put("maskright","1");
            Combo combo = hero.buff(Combo.class);
            if (hero.subClass == HeroSubClass.GLADIATOR && combo != null) {
                rows.put("combocount", Integer.toString(combo.getComboCount()));
                rows.put("combotime", Float.toString(combo.remainingTime()));
                rows.put("comboenhanced", Integer.toString(hero.pointsInTalent(Talent.ENHANCED_COMBO)));
                for (Combo.ComboMove move : Combo.ComboMove.values())
                    if (combo.canUseMove(move)) rows.put("finish" + move.ordinal(), "1");
            }
            Berserk rage = hero.buff(Berserk.class);
            if (hero.subClass==HeroSubClass.ASSASSIN && hero.invisible>0 && hero.buff(Preparation.class)!=null)
                rows.put("ambush",Integer.toString(hero.buff(Preparation.class).attackLevel()));
            if (hero.subClass==HeroSubClass.FREERUNNER && hero.buff(Momentum.class)!=null && hero.buff(Momentum.class).canStart())
                rows.put("freerun","1");
            if (hero.subClass == HeroSubClass.BERSERKER && rage != null && rage.readyForSeat()) rows.put("rage", "1");
            SnipersMark mark = hero.buff(SnipersMark.class);
            if (hero.subClass == HeroSubClass.SNIPER && mark != null) {
                com.shatteredpixel.shatteredpixeldungeon.actors.Actor marked =
                        com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findById(mark.object);
                if (marked instanceof Mob && ((Mob)marked).isAlive() && ((Mob)marked).invisible <= 0 && sight.visible(((Mob)marked).pos))
                    rows.put("snipercell", Integer.toString(((Mob)marked).pos));
            }
            if (com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.remembersRecharging(hero))
                rows.put("recallrecharging","1");
            for (int tier=1; tier<=hero.talents.size(); tier++) {
                rows.put("freepoints"+tier, Integer.toString(hero.talentPointsAvailable(tier)));
                for (Talent talent : hero.talents.get(tier-1).keySet()) {
                    if (game.offersTalent(hero, talent)) {
                        rows.put("learn"+talent.ordinal(), Integer.toString(hero.pointsInTalent(talent)));
                        rows.put("tier"+talent.ordinal(), Integer.toString(tier));
                    }
                }
            }
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
            if (hero.hasTalent(Talent.WALL_OF_LIGHT) && com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.INSTANCE.chargeUse(hero)==0)
                rows.put("endlightwall","1");
            StenchGas stench = (StenchGas)level.blobs.get(StenchGas.class);
            for (int cell=0;cell<sight.length();cell++) {
                if (sight.sensed(cell)) rows.put("sense"+cell, sight.sensedMob(cell) ? "mob" : "terrain");
                if (!sight.visible(cell)) continue;
                rows.put("t"+cell,Integer.toString(sight.terrain(cell)));
                if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.volumeAt(cell,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.LightWall.class)>0
                        || com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.volumeAt(cell,com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.WallOfLight.GuestLightWall.class)>0) rows.put("lightwall"+cell,"1");
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
                    String kind=mob.getClass().getSimpleName();
                    if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultElemental.Fire
                            || mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultElemental.Frost) kind="Vault"+kind;
                    rows.put("m"+mob.id(),mob.pos+","+kind+","+mob.HP+","+mob.HT);
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
                    if (CoopSession.summonerOf(mob) == hero && hero.hasTalent(Talent.ALLY_WARP)
                            && mob.canInteract(hero)) rows.put("warpally"+mob.pos, "1");
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
            if (hero.armorAbility instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity) {
                com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity trinity=(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity)hero.armorAbility;
                for (Talent form : new Talent[]{Talent.BODY_FORM,Talent.MIND_FORM,Talent.SPIRIT_FORM})
                    if (trinity.hasForm(form)) rows.put("trinity"+form.ordinal(),"1");
            }
            java.util.EnumSet<TrinityDiscoveries.Choice> remembered = java.util.EnumSet.noneOf(TrinityDiscoveries.Choice.class);
            if (hero.armorAbility instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity) {
                for (TrinityDiscoveries.Choice choice : TrinityDiscoveries.Choice.values()) {
                    if (hero.hasTalent(choice.form) && game.discoveries(seat).knows(choice)) remembered.add(choice);
                }
            }
            com.shatteredpixel.shatteredpixeldungeon.CoopEnchantScroll enchant = game.enchantScroll(seat);
            if (enchant != null) {
                rows.put("enchantphase", enchant.hasOptions() ? "options" : enchant.chargesTimeOnCancel() ? "consumed" : "inventory");
                if (enchant.isArmor()) rows.put("enchantarmor", "1");
                if (enchant.hasOptions()) for (int i=0; i<3; i++) {
                    rows.put("enchantoption"+i, enchant.optionKey(i));
                    if (!enchant.available(game,hero,i)) rows.put("enchantlimited"+i,"1");
                }
            }
            int selection=0;
            for (Item item : hero.belongings) {
                selection++;
                putItem(rows,visibleItems,item,slot(hero.belongings,item));
                if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows
                        && item.actions(hero).contains(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows.AC_STEALTH))
                    {
                    rows.put("stealth"+ids.get(item), "1");
                    if (((com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows)item).stealthed())
                        rows.put("cloaked"+ids.get(item), "1");
                }
                if (game.ownership.owner(item)==game.player(seat)) {
                    if (enchant != null && !enchant.hasOptions() && com.shatteredpixel.shatteredpixeldungeon.CoopEnchantScroll.eligible(hero, item))
                        rows.put("enchantgear"+ids.get(item), "1");
                    if (hero.hasTalent(Talent.HOLY_INTUITION) && com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition.INSTANCE.usableOnItem(item))
                        rows.put("intuition"+ids.get(item),Integer.toString(selection));
                    for (TrinityDiscoveries.Choice choice : TrinityDiscoveries.Choice.values()) {
                        if (!hero.hasTalent(choice.form) || !choice.matches(item)) continue;
                        String prefix = choice.form == Talent.BODY_FORM ? "bodyitem" : choice.form == Talent.MIND_FORM ? "minditem" : "spirititem";
                        rows.put(prefix + ids.get(item), Integer.toString(selection));
                        remembered.remove(choice);
                    }
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.UsedItemTracker imprint=hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription.UsedItemTracker.class);
                    if (imprint!=null && imprint.item==com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify.class && CoopSession.equipmentForScroll(item) && !item.isIdentified())
                        rows.put("recallitem"+ids.get(item),Integer.toString(selection));
                    if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome && hero.heroClass==HeroClass.CLERIC) {
                        com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome)item;
                        if (tome.actions(hero).contains("CAST")) rows.put("spells"+ids.get(item),"1");
                    }
                    if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor) {
                        com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor armor=(com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)item;
                        if (armor.checkSeal()!=null) rows.put("detachseal"+ids.get(item),"1");
                        else if (armor.cursedKnown) {
                            rows.put("sealarmor"+ids.get(item),Integer.toString(selection));
                            if (armor.cursed) rows.put("sealcursetarget"+ids.get(item),"1");
                            if (armor.glyph!=null) rows.put("armorglyph"+ids.get(item),armor.glyph.getClass().getName().replace("com.shatteredpixel.shatteredpixeldungeon.","").toLowerCase(java.util.Locale.ROOT));
                        }
                    }
                    if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal && hero.belongings.backpack.contains(item)) {
                        com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph glyph=((com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal)item).getGlyph();
                        rows.put("affixseal"+ids.get(item),glyph==null ? "none" : glyph.getClass().getName().replace("com.shatteredpixel.shatteredpixeldungeon.","").toLowerCase(java.util.Locale.ROOT));
                        if (glyph!=null && glyph.curse()) rows.put("sealcursed"+ids.get(item),"1");
                    }
                    if (hero.heroClass==HeroClass.DUELIST && item==hero.belongings.weapon()
                            && CoopSession.supportedWeaponAbility(item)) {
                        com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger pool=hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger.class);
                        if (pool!=null) rows.put("weaponpool"+ids.get(item),pool.charges+","+pool.chargeCap());
                    }
                    if (item instanceof MagesStaff && ((MagesStaff)item).coopWand()!=null)
                        rows.put("imbue"+ids.get(item), hero.hasTalent(Talent.WAND_PRESERVATION)
                                && hero.buff(Talent.WandPreservationCounter.class)==null ? "KEEP" : "REPLACE");
                    if (CoopSession.supportedHandheldWand(item) && hero.belongings.backpack.contains(item)) {
                        rows.put("wandchoice"+ids.get(item),Integer.toString(selection));
                        if (item.isIdentified()) rows.put("imbuelevel"+ids.get(item),Integer.toString(item.trueLevel()));
                    }
                    if (item instanceof MagesStaff && item.levelKnown)
                        rows.put("imbuelevel"+ids.get(item),Integer.toString(item.trueLevel()));
                    if (item instanceof KingsCrown && game.mayChooseArmor(seat,true)) rows.put("crown"+ids.get(item),"1");
                    if (item == hero.belongings.armor() && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor && CoopSession.playableArmorAbility(hero.armorAbility)) {
                        rows.put("ability"+ids.get(item),hero.armorAbility.getClass().getSimpleName());
                        if (hero.armorAbility instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone
                                && com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone.getShadowAlly(hero)!=null)
                            rows.put("clonecommand","1");
                        if (hero.armorAbility instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk
                                && com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.getHawk(hero)!=null)
                            rows.put("hawkcommand","1");
                        com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.WarpBeaconTracker beacon =
                                hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon.WarpBeaconTracker.class);
                        if (beacon != null) rows.put("beacondepth", Integer.toString(beacon.depth));
                        else if (hero.armorAbility instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon
                                && hero.hasTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.REMOTE_BEACON)) rows.put("remotebeacon", "1");
                        rows.put("armorcharge"+ids.get(item),Integer.toString((int)((com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)item).charge));
                    }
                    if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.TengusMask && game.mayUseMask(seat))
                        rows.put("mask"+ids.get(item),"1");
                    if (rows.containsKey("vaultscore") && com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal.rewardAllowed(item,Integer.parseInt(rows.get("vaultscore")))) rows.put("vaultpick"+ids.get(item),"1");
                    if (shop!=null && com.shatteredpixel.shatteredpixeldungeon.CoopTrade.maySell(hero,item))
                        rows.put("sell"+ids.get(item),item.value()+","+(com.shatteredpixel.shatteredpixeldungeon.CoopTrade.sellOne(item) ? 1 : 0));
                    if (CoopSession.basicMissile(item) && item.isIdentified()) {
                        String family=item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart ? "poisondart" :
                                item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.BlindingDart ? "blindingdart" :
                                item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike ? "spike" : item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife ? "knife" :
                        item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart ? "dart" : "stone";
                        rows.put("family"+ids.get(item),family+"."+StateImage.hash(shortcutSalt+"/"+family+"/"+
                                ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)item).setID).substring(0,32));
                    }
                    if (rows.containsKey("favor") && CoopSmith.eligible(item) && CoopSession.equipmentForScroll(item)) {
                        rows.put("forge"+ids.get(item),selection+","+(CoopSmith.canHarden(item)?1:0)+","+(item.level()<2?1:0));
                    }
                    if (item instanceof SpiritBow) {
                        SpiritBow bow = (SpiritBow)item;
                        long id = ids.get(item);
                        rows.put("bowtarget"+id,Integer.toString(selection));
                        if (bow.enchantment != null && (!bow.enchantment.curse() || bow.cursedKnown)) {
                            String type = bow.enchantment.getClass().getName();
                            rows.put("bowenchant"+id,type.substring(type.indexOf(".items.")+1).toLowerCase(Locale.ROOT));
                            if (!SpiritBow.supportedCoopEnchantment(bow)) rows.put("bowlimited"+id,"1");
                        }
                    }
                    if (CoopSession.equipmentForScroll(item))
                        rows.put("select"+ids.get(item),selection+","+(item.isUpgradable()?1:0)+","+(item.isIdentified()?0:1));
                    // Only an owned item in this seat's actual inventory gets a shortcut reference.
                    String token=StateImage.hash(shortcutSalt+"/item/"+game.ownership.identity(item)).substring(0,32);
                    rows.put("ref"+ids.get(item),token);
                }
            }
            for (TrinityDiscoveries.Choice choice : remembered) rows.put(choice.rowKey(), "1");
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
        if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment) kind="ENCHANT_BOW";
        if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor) kind="CLASS_ARMOR";
        if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.VaultBeacon) kind="VAULT_BEACON";
        if (!slot.startsWith("F") && item.isIdentified() && CoopSession.basicArmorType(item)
                && ((com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)item).glyph!=null) kind="INSCRIBED";
        if (!slot.startsWith("F") && CoopSession.supportedScroll(item)
                && ((com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll)item).isKnown())
            kind=item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging ? "RECHARGING_SCROLL"
                    : item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade ? "UPGRADE" : "IDENTIFY";
        if (!slot.startsWith("F") && CoopSession.basicMissile(item) && item.isIdentified())
            kind=item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike ? "SPIKE" : item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife ? "KNIFE" :
                        item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart ? "DART" : "STONE";
        if (!slot.startsWith("F") && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment) kind="READ_SCROLL";
        if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle) kind="CANDLE";
        if (!slot.startsWith("F") && item.isIdentified() && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste) kind="HASTE_RING";
        String charges="";
        if (!slot.startsWith("F") && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty) {
            com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty horn=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty)item;
            kind="HORN";
            charges=","+horn.charges()+","+horn.capacity();
        }
        if (!slot.startsWith("F") && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome) {
            com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome tome=(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome)item;
            kind="TOME";
            charges=","+tome.charges()+","+tome.capacity();
        } else if (!slot.startsWith("F") && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows) {
            com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows cloak = (com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows)item;
            kind = "CLOAK";
            charges = ","+cloak.charges()+","+cloak.capacity();
        } else if (!slot.startsWith("F") && CoopSession.supportedHandheldWand(item)) {
            com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand = (com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand)item;
            if (wand.isIdentified()) {
                kind = "WAND";
                charges = "," + wand.curCharges + "," + wand.maxCharges;
            } else {
                kind = "UNIDENTIFIED_WAND";
            }
        } else if (!slot.startsWith("F") && item instanceof MagesStaff) {
            com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand wand=((MagesStaff)item).coopWand();
            if (wand!=null) {
                kind="STAFF";
                charges=","+wand.curCharges+","+wand.maxCharges;
                if (!(wand instanceof WandOfMagicMissile)) rows.put("areawand"+id,"1");
            }
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
