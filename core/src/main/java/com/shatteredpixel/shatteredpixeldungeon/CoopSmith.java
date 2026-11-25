package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBlacksmith;

public final class CoopSmith {
    private CoopSmith() {}

    public static boolean eligible(Item item) {
        return item != null && item.isIdentified() && !item.cursed && item.isUpgradable();
    }

    public static boolean canHarden(Item item) {
        return eligible(item) && (item instanceof Weapon && !((Weapon)item).enchantHardened
                || item instanceof Armor && !((Armor)item).glyphHardened);
    }

    public static boolean pendingChoice() {
        return Quest.completed() && Quest.smiths > 0 && Quest.smithRewards != null;
    }

    private static boolean atSmith(Hero hero) {
        if (Dungeon.branch != 0 || !Quest.completed()) return false;
        for (Mob m : Dungeon.level.mobs)
            if (m instanceof Blacksmith && Dungeon.level.adjacent(hero.pos,m.pos)
                    && Dungeon.coop.knowledge(hero).visible(m.pos)) return true;
        return false;
    }

    private static void deliver(Hero hero, Item item) {
        Dungeon.coop.ownership.claim(item, Dungeon.coop.playerOf(hero));
        if (!item.doPickUp(hero)) Dungeon.level.drop(item,hero.pos);
    }

    public static boolean apply(CoopSession.Action service,Hero hero,Item item,int choice) {
        if (!atSmith(hero)) return false;
        long player=Dungeon.coop.playerOf(hero);
        if (item!=null && (!hero.belongings.contains(item) || Dungeon.coop.ownership.owner(item)!=player)) return false;
        if (pendingChoice() && service!=CoopSession.Action.SMITH_CLAIM) return false;
        int cost;
        switch (service) {
            case SMITH_PICK:
                cost=Quest.freePickaxe ? 0 : 250;
                if (Quest.pickaxe==null || Quest.favor<cost || Dungeon.coop.ownership.owner(Quest.pickaxe)!=player) return false;
                deliver(hero,Quest.pickaxe);
                Quest.pickaxe=null;
                break;
            case SMITH_CASH:
                if (Quest.favor<=0) return false;
                cost=Quest.favor;
                new Gold(cost).doPickUp(hero,hero.pos);
                break;
            case SMITH_HARDEN:
                cost=500+1000*Quest.hardens;
                if (!canHarden(item) || !CoopSession.equipmentForScroll(item) || Quest.favor<cost) return false;
                if (item instanceof Armor) ((Armor)item).glyphHardened=true;
                else ((Weapon)item).enchantHardened=true;
                Quest.hardens++;
                break;
            case SMITH_UPGRADE:
                cost=1000+1000*Quest.upgrades;
                if (Quest.favor<cost || !eligible(item) || item.level()>=2 || !CoopSession.equipmentForScroll(item)) return false;
                item.upgrade();
                Quest.upgrades++;
                break;
            case SMITH_REFORGE:
                if (Quest.favor<500+1000*Quest.reforges || !eligible(item) || !CoopSession.equipmentForScroll(item)) return false;
                Item second=null;
                int order=0;
                for (Item candidate:hero.belongings) if (++order==choice) { second=candidate;break; }
                if (!eligible(second) || second==item || second.getClass()!=item.getClass()
                        || Dungeon.coop.ownership.owner(second)!=player) return false;
                if (second.trueLevel()>item.trueLevel()) { Item lower=item;item=second;second=lower; }
                WndBlacksmith.reforgeEquipment(hero,item,second);
                cost=0; // The original reforge method charges favor.
                break;
            case SMITH_ORDER:
                cost=2000;
                if (Quest.favor<cost) return false;
                if (Quest.smithRewards==null || Quest.smithRewards.isEmpty()) Quest.generateRewards(false);
                Quest.smiths++;
                break;
            case SMITH_CLAIM:
                cost=0;
                if (!pendingChoice() || choice<0 || choice>=Quest.smithRewards.size()) return false;
                Item reward=Quest.smithRewards.get(choice);
                if (reward instanceof Weapon && Quest.smithEnchant!=null) ((Weapon)reward).enchant(Quest.smithEnchant);
                else if (reward instanceof Armor && Quest.smithGlyph!=null) ((Armor)reward).inscribe(Quest.smithGlyph);
                reward.identify(false);
                deliver(hero,reward);
                Quest.smithRewards=null;
                break;
            default: return false;
        }
        Quest.favor-=cost;
        if (!Quest.rewardsAvailable()) Notes.remove(Notes.Landmark.TROLL);
        return true;
    }
}
