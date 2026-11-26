package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import static org.junit.Assert.*;

/** Compare one staged mask for the warrior against no subclass from the same 25th-floor state. */
public class WarriorYogGrowth extends HeadlessDungeon {
    private static Bundle arrange() {
        try (CoopSession game=CavesInteriorTest.open(578)) {
            YogArenaTest.arrive(game);
            while (Dungeon.depth>10) SewerDepthTest.gate(game,false);
            SewerBeeTest.offer(game,0);
            com.shatteredpixel.shatteredpixeldungeon.items.Heap treasure=Dungeon.level.heaps.valueList().stream()
                    .filter(heap -> heap.peek() instanceof TengusMask).findFirst().orElseThrow(AssertionError::new);
            if (game.hero(1).pos==treasure.pos) CavesBossTest.beside(game,1,treasure.pos);
            game.hero(0).pos=treasure.pos;
            game.observe(0);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,treasure.pos,treasure.peek()));
            while (Dungeon.depth<25) SewerDepthTest.gate(game,true);
            assertNotNull(game.hero(0).belongings.getItem(TengusMask.class));
            return game.checkpoint();
        }
    }
    private static boolean special(CoopSession game, Mob target, Map<String,Integer> used) {
        Hero h=game.hero(game.activeSeat());
        CoopSession.Action action=null;
        int cell=target.pos;
        ClassArmor armor=null;
        Combo combo=h.buff(Combo.class);
        Berserk rage=h.buff(Berserk.class);
        if (rage!=null && rage.readyForSeat()) {
            action=CoopSession.Action.BERSERK;
            cell=h.pos;
        } else if (combo!=null && combo.canUseMove(Combo.ComboMove.SLAM)) {
            action=CoopSession.Action.SLAM;
        } else if (h.heroClass==com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR
                && h.belongings.armor() instanceof ClassArmor) {
            armor=(ClassArmor)h.belongings.armor();
            if (armor.charge>=h.armorAbility.chargeUse(h)) {
                action=CoopSession.Action.ARMOR_ABILITY;
                cell=CrownAbilityTest.destination(h);
            }
        }
        if (action==null || !CoopFloorsTest.step(game,action,cell,armor)) return false;
        used.merge(action.name(),1,Integer::sum);
        return true;
    }
    private static void fight(Bundle initial, HeroSubClass choice, boolean activeSkills) {
        try (CoopSession game=CoopSession.restore(initial)) {
            for (int i=0;i<2;i++) {
                Hero h=game.hero(i);
                while (h.lvl<25) h.earnExp(h.maxExp(),WarriorYogGrowth.class);
                h.HTBoost=0;
                h.updateHT(false);
                h.HP=h.HT;
                h.STR=20;
                h.belongings.weapon=new Greatsword();
                h.belongings.weapon.level(4);
                Armor old=h.belongings.armor();
                h.belongings.armor=new PlateArmor();
                h.belongings.armor.level(4);
                if (activeSkills && old.checkSeal()!=null) {
                    h.belongings.armor.affixSeal(old.detachSeal(),h);
                    assertNotNull(h.belongings.armor.checkSeal());
                }
                game.ownership.claim(h.belongings.weapon,game.player(i));
                game.ownership.claim(h.belongings.armor,game.player(i));
                assertEquals(140,h.HT);
            }
            Hero owner=game.hero(0);
            if (activeSkills) {
                game.beginMaskTrial(CoopSession.MaskTrial.OWNER);
                SewerBeeTest.offer(game,0);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_MASK,choice.ordinal(),owner.belongings.getItem(TengusMask.class)));
                game.beginCrownTrial(CoopSession.MaskTrial.ROSTER);
                int seat=owner.belongings.getItem(KingsCrown.class)==null?1:0;
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,CrownAbilityTest.choice(game.hero(seat)),game.hero(seat).belongings.getItem(KingsCrown.class)));
                if (seat==1) {
                    SewerBeeTest.offer(game,0);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLAIM_ARMOR,CrownAbilityTest.choice(owner),null));
                }
            } else if (choice!=HeroSubClass.NONE)
                WarriorGrowthCombat.Callback.select(owner.belongings.getItem(TengusMask.class),owner,choice);
            assertEquals(choice,owner.subClass);
            assertEquals(HeroSubClass.NONE,game.hero(1).subClass);
            YogArenaTest.start(game);
            long start=game.turn();
            ArrayList<String> fists=new ArrayList<>();
            Map<String,Integer> used=new TreeMap<>();
            try { YogArenaTest.fight(game,fists,null,activeSkills?(g,m)->special(g,m,used):null); }
            catch (AssertionError e) {
                if (!game.defeated() || !String.valueOf(e.getMessage()).contains("party lost")) throw e;
            }
            YogDzewa boss=YogArenaTest.resident(YogDzewa.class);
            System.out.println("TRIAL_ACTIONS "+choice+" active="+activeSkills+" used="+used);
            System.out.printf("GROWTH_PAIR\t%s\t%d\t%d\t%d\t%d\t%d\t%s\t%s%n",choice,game.turn()-start,
                    game.hero(0).HP,game.hero(1).HP,boss==null?0:boss.phase(),boss==null?0:boss.HP,fists,game.defeated());
        }
    }
    public static void main(String[] args) throws Exception {
        WarriorYogGrowth probe=new WarriorYogGrowth();
        probe.temporary.create();
        probe.startBackend();
        try {
            Bundle initial=arrange();
            for (HeroSubClass choice:new HeroSubClass[]{HeroSubClass.NONE,HeroSubClass.BERSERKER,HeroSubClass.GLADIATOR})
                fight(initial,choice,false);
            for (HeroSubClass choice:new HeroSubClass[]{HeroSubClass.BERSERKER,HeroSubClass.GLADIATOR})
                fight(initial,choice,true);
        } finally {probe.stopBackend();probe.temporary.delete();}
    }
}
