package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static org.junit.Assert.*;

/** Isolated candidate replay. Only the last-hit policy runs in the shipping host. */
public class PartyExperienceSurvey extends HeadlessDungeon {
    static class Kill {
        final int finisher, experience, cap;
        final int[] damage;
        final Class<? extends Mob> source;
        Kill(Mob mob,int seat,int[] damage) {
            this.damage=damage;
            finisher=seat;
            experience=mob.EXP;
            cap=mob.maxLvl;
            source=mob.getClass();
        }
    }

    static int lifetime(Hero hero) {
        int total=hero.exp;
        for (int level=1;level<hero.lvl;level++) total+=Hero.maxExp(level);
        return total;
    }

    static List<Kill> encounters() {
        List<Kill> events=new ArrayList<>();
        for(int seed=570;seed<=581;seed++) try(CoopSession game=CavesInteriorTest.open(seed)) {
            List<Mob> population=new ArrayList<>(Dungeon.level.mobs);
            population.sort(Comparator.comparingInt(Mob::id));
            for (Mob target:population) {
                if (target.alignment!=Char.Alignment.ENEMY || target.EXP<=0 || !target.isAlive()) continue;
                int[] contributions=new int[2];
                for (int attempt=0;attempt<24 && target.isAlive();attempt++) {
                    if (game.life(0)!=CoopSession.Life.ALIVE || game.life(1)!=CoopSession.Life.ALIVE) break;
                    int seat=game.activeSeat();
                    int before=target.HP, previous=lifetime(game.hero(seat));
                    MiningBranchTest.standBeside(game,target.pos);
                    if (!CoopFloorsTest.step(game,CoopSession.Action.ATTACK,target.pos,null)) break;
                    contributions[seat]+=Math.max(0,before-Math.max(0,target.HP));
                    if (!target.isAlive()) {
                        int gained=lifetime(game.hero(seat))-previous;
                        if (gained==0) break;
                        assertEquals(target.EXP,gained);
                        events.add(new Kill(target,seat,contributions));
                        System.out.printf("KILL\t%d\t%s\t%d\t%d\t%d\t%d%n",seed,
                                target.getClass().getSimpleName(),seat,gained,contributions[0],contributions[1]);
                    }
                }
            }
        }
        return events;
    }

    public static void main(String[] args) throws Exception {
        PartyExperienceSurvey survey=new PartyExperienceSurvey();
        survey.temporary.create();survey.startBackend();
        try {
            List<Kill> events=encounters();
            assertFalse(events.isEmpty());
            for(String policy:new String[]{"last_hit","equal_split","full_each","damage_share"}) {
                try (CoopSession replay=CavesInteriorTest.open(570)) {
                    int[] total=new int[2];
                    int tie=0;
                    for(Kill e:events) {
                        int[] payout=new int[2];
                        if(policy.equals("full_each")) payout[0]=payout[1]=e.experience;
                        else if(policy.equals("last_hit")) payout[e.finisher]=e.experience;
                        else {
                            int sum=policy.equals("damage_share")?e.damage[0]+e.damage[1]:2;
                            assertTrue(sum>0);
                            int numerator=e.experience*(policy.equals("damage_share")?e.damage[0]:1);
                            int first=numerator/sum, rem=numerator%sum;
                            if (2*rem>sum || (rem>0 && 2*rem==sum && (tie++%2)==0)) first++;
                            payout[0]=first;payout[1]=e.experience-first;
                        }
                        for(int seat=0;seat<2;seat++) {
                            Hero h=replay.hero(seat);
                            if (payout[seat]>0 && h.lvl<=e.cap) {
                                h.earnExp(payout[seat],e.source);total[seat]+=payout[seat];
                            }
                        }
                    }
                    assertEquals(total[0],lifetime(replay.hero(0)));
                    assertEquals(total[1],lifetime(replay.hero(1)));
                    System.out.printf("POLICY\t%s\t%d\t%d\t%d\t%d\t%d\t%d%n",policy,events.size(),
                            total[0],total[1],total[0]+total[1],replay.hero(0).lvl,replay.hero(1).lvl);
                }
            }
            for (HeroClass cls:HeroClass.values()) {
                Hero hero=new Hero();
                hero.heroClass=cls;
                com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.initClassTalents(hero);
                StringBuilder skills=new StringBuilder();
                for (com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability:cls.armorAbilities()) {
                    if (skills.length()>0) skills.append(',');
                    skills.append(ability.getClass().getSimpleName());
                }
                System.out.printf("CLASS\t%s\t%s\t%s\t%s%n",cls,java.util.Arrays.toString(cls.subClasses()),skills,hero.talents);
            }
        } finally {survey.stopBackend();survey.temporary.delete();}
    }
}
