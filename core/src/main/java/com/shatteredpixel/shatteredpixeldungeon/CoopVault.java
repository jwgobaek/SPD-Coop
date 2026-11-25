package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultLaser;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultMirror;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultSentry;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/** One original crystal holds two private belongings bundles and a single party purse. */
final class CoopVault {
    private static final String[] EQUIPPED = {"weapon","armor","artifact","misc","ring","second_wep"};
    private CoopVault() {}

    static boolean resident(Class<?> type) {
        return type==VaultElemental.Fire.class || type==VaultElemental.Frost.class || type==VaultShaman.class
                || type==VaultBossElemental.class || type==VaultDM100.class || type==VaultDM200.class || type==VaultGhoul.class || type==VaultGolem.class
                || type==VaultSkeleton.class || type==VaultLaser.class || type==VaultMirror.class
                || type==VaultSentry.class || type==VaultTokenDoor.class;
    }

    static boolean questPickup(Item item, int cell) {
        if (!(Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel)) return false;
        if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken
                || item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.VaultBeacon) return true;
        com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel vault=(com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel)Dungeon.level;
        return vault.finalTreasureOpen()
                && vault.room(cell) instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom
                && (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.ImpStatue
                || item instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring);
    }

    static Item attached(Item parent) {
        if (parent instanceof MagesStaff) return ((MagesStaff) parent).imbuedWand();
        if (parent instanceof Armor) return ((Armor) parent).checkSeal();
        return null;
    }

    private static void locate(Map<String,Item> items, String path, Item item) {
        if (item == null) return;
        items.put(path,item);
        Item part = attached(item);
        if (part != null) items.put(path + (item instanceof Armor ? "/seal" : "/wand"),part);
        if (item instanceof Bag) {
            int i=0;
            for (Item child : ((Bag)item).items) locate(items,path+"/"+i++,child);
        }
    }

    static Map<String,Item> belongings(Hero hero, int seat) {
        String root = "s"+seat+"_";
        Belongings b=hero.belongings;
        Map<String,Item> result=new TreeMap<>();
        Item[] equipped={b.weapon,b.armor,b.artifact,b.misc,b.ring,b.secondWep};
        for (int i=0;i<EQUIPPED.length;i++) locate(result,root+EQUIPPED[i],equipped[i]);
        int i=0;
        for (Item item:b.backpack.items) locate(result,root+i++,item);
        return result;
    }

    static EscapeCrystal enter(CoopSession game, int holder) {
        Bundle stash=new Bundle();
        stash.put("holder",holder);
        stash.put("gold",Dungeon.gold);
        stash.put("energy",Dungeon.energy);
        for (int seat=0;seat<2;seat++) {
            Hero hero=game.hero(seat);
            if (game.life(seat)!=CoopSession.Life.ALIVE || hero.belongings.getItem(EscapeCrystal.class)!=null)
                throw new IllegalStateException("Invalid crystal entry");
            Map<String,Item> items=belongings(hero,seat);
            for (Item item:items.values()) game.ownership.claim(item,game.player(seat));
            stash.put("ids"+seat,game.itemState(items));
        }
        for (int seat=0;seat<2;seat++) {
            Hero hero=game.hero(seat);
            hero.live();
            hero.HP=hero.HT;
            Bundle gear=new Bundle();
            hero.belongings.storeInBundle(gear);
            stash.put("gear"+seat,gear);
            hero.belongings.clear();
            hero.belongings.armor=new ClothArmor();
            hero.belongings.armor.identify();
            game.ownership.claim(hero.belongings.armor,game.player(seat));
            hero.updateHT(false);
        }
        Dungeon.gold=Dungeon.energy=0;
        EscapeCrystal crystal=new EscapeCrystal();
        crystal.storedItems=stash;
        if (!crystal.collect(game.hero(holder).belongings.backpack)) throw new IllegalStateException("Crystal placement");
        return crystal;
    }

    private static void inspect(Bundle item, String path, Bundle table, long player, long ceiling,
                                HashSet<Long> seen, HashSet<String> paths, int depth) {
        if (item.isNull()) return;
        Bundle entry=table.getBundle(path);
        if (entry.isNull()) throw new IllegalArgumentException("Missing escrow identity");
        long id=entry.getLong("id");
        if (depth>24 || paths.size()>=8192 || !paths.add(path) || !seen.add(id) || id<=0 || id>ceiling
                || entry.getLong("owner")!=player || !item.getString("__className").equals(entry.getString("type"))
                || item.getInt("quantity")<=0 || item.getInt("quantity")!=entry.getInt("quantity"))
            throw new IllegalArgumentException("Invalid escrow item");
        for (String key:new String[]{"seal","wand"}) if (item.contains(key))
            inspect(item.getBundle(key),path+"/"+key,table,player,ceiling,seen,paths,depth+1);
        if (item.contains("inventory")) {
            int i=0;
            for (Bundle child:item.getBundleArray("inventory")) inspect(child,path+"/"+i++,table,player,ceiling,seen,paths,depth+1);
        }
    }

    static void validate(CoopSession game, EscapeCrystal crystal, int holder, long ceiling, HashSet<Long> seen) {
        Bundle stash=crystal.storedItems;
        if (stash==null || !stash.contains("holder") || stash.getInt("holder")!=holder
                || !stash.contains("gold") || !stash.contains("energy") || stash.getInt("gold")<0 || stash.getInt("energy")<0)
            throw new IllegalArgumentException("Invalid party vault purse");
        for (int seat=0;seat<2;seat++) {
            String root="s"+seat+"_";
            Bundle gear=stash.getBundle("gear"+seat), table=stash.getBundle("ids"+seat);
            if (gear.isNull() || table.isNull() || !gear.contains("inventory")) throw new IllegalArgumentException("Missing vault seat");
            HashSet<String> paths=new HashSet<>();
            for (String key:EQUIPPED) inspect(gear.getBundle(key),root+key,table,game.player(seat),ceiling,seen,paths,0);
            int i=0;
            for (Bundle item:gear.getBundleArray("inventory"))
                inspect(item,root+i++,table,game.player(seat),ceiling,seen,paths,0);
            if (paths.size()!=table.getKeys().size()) throw new IllegalArgumentException("Extra escrow item");
        }
    }

    static void restore(CoopSession game, EscapeCrystal crystal, int holder) {
        if (!game.hero(holder).belongings.contains(crystal) || game.ownership.owner(crystal)!=game.player(holder))
            throw new IllegalStateException("Missing owned escape crystal");
        validate(game,crystal,holder,game.ownership.sequence(),new HashSet<>());
        Bundle stash=crystal.storedItems;
        HashSet<Long> restored=new HashSet<>();
        for (int seat=0;seat<2;seat++) {
            Hero hero=game.hero(seat);
            hero.live();
            hero.HP=hero.HT;
            hero.belongings.clear();
            game.restoreVaultGear(hero,stash.getBundle("gear"+seat));
            game.restoreItems(belongings(hero,seat),stash.getBundle("ids"+seat),game.ownership.sequence(),restored);
            hero.updateHT(false);
        }
        Dungeon.gold=stash.getInt("gold");
        Dungeon.energy=stash.getInt("energy");
        crystal.storedItems=null;
    }
}
