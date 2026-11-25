package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import static org.junit.Assert.*;

/** Isolated level-20 encounter, with level-derived HP and a bounded melee-only guide. */
public class CityKingGrowthProbe extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        CityKingGrowthProbe probe=new CityKingGrowthProbe();
        probe.temporary.create();
        probe.startBackend();
        try (CoopSession game=CavesInteriorTest.open(572)) {
            CityKingTest.reach(game,20);
            for (int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                while (hero.lvl<20) hero.earnExp(hero.maxExp(),CityKingGrowthProbe.class);
                hero.STR=18;
                hero.HTBoost=0;
                hero.updateHT(false);
                hero.HP=hero.HT;
                hero.belongings.weapon=new Longsword();
                hero.belongings.armor=new ScaleArmor();
                hero.belongings.weapon.level(3).identify();
                hero.belongings.armor.level(3).identify();
                game.ownership.claim(hero.belongings.weapon,game.player(seat));
                game.ownership.claim(hero.belongings.armor,game.player(seat));
            }
            System.out.println("fixture seed=572 native_hp="+game.hero(0).HP+"/"+game.hero(1).HP
                    +" str=18 sword=Longsword+3 armor=ScaleArmor+3");
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,CityKingTest.entry(game),null));
            int maxPhase=0;
            int turns=0;
            while (!game.defeated() && !game.stopped() && CityKingTest.boss()!=null && turns<500) {
                Hero hero=game.hero(game.activeSeat());
                DwarfKing king=CityKingTest.boss();
                maxPhase=Math.max(maxPhase,king.phase());
                Mob target=king.phase()==2 ? CityKingTest.subject() : king;
                if (target==null || !game.knowledge(hero).visible(target.pos)) {
                    target=null;
                    for (Mob mob:Dungeon.level.mobs) {
                        if (mob!=king && mob.isAlive() && game.knowledge(hero).visible(mob.pos)
                                && (target==null || Dungeon.level.distance(hero.pos,mob.pos)<Dungeon.level.distance(hero.pos,target.pos)))
                            target=mob;
                    }
                }
                boolean acted=false;
                if (target!=null) {
                    if (Dungeon.level.adjacent(hero.pos,target.pos))
                        acted=CoopFloorsTest.step(game,CoopSession.Action.ATTACK,target.pos,null);
                    else {
                        boolean[] pass=Dungeon.level.passable.clone();
                        for(int i=0;i<pass.length;i++) pass[i]&=game.knowledge(hero).visible(i);
                        int step=Dungeon.findStep(hero,target.pos,pass,hero.fieldOfView,true);
                        if (step>=0) acted=CoopFloorsTest.step(game,CoopSession.Action.MOVE,step,null);
                    }
                }
                if (!acted) TenguArenaTest.waitOne(game);
                turns++;
            }
            assertFalse(game.stopReason(),game.stopped());
            System.out.println("boss_trial seed=572 turns="+turns+" max_phase="+maxPhase+" hp="+game.hero(0).HP+"/"+game.hero(1).HP
                    +" king_hp="+(CityKingTest.boss()==null ? 0 : CityKingTest.boss().HP)
                    +" crown="+(CityKingTest.crown()>=0)+" party_defeated="+game.defeated());
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
