package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.VaultFlameTraps;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEvasion;
import static org.junit.Assert.*;

/** Boundary inputs exercise the reward predicate, not earned scores. */
public final class VaultRewardTiers extends HeadlessDungeon {
    static void hit(CoopSession g,int seat) {
        VaultFlameTraps trap=(VaultFlameTraps)Dungeon.level.blobs.get(VaultFlameTraps.class);
        assertNotNull(trap);
        int target=-1;
        for(int c=0;c<Dungeon.level.length();c++)
            if(trap.afterTriggerCooldowns[c]>=0 && Dungeon.level.passable[c] && Actor.findChar(c)==null) {target=c;break;}
        assertTrue(target>=0);
        g.hero(1-seat).pos=Dungeon.level.entrance();
        g.hero(seat).pos=target;g.observe(0);g.observe(1);
        int previous=Imp.Quest.hazardFreebies,score=Statistics.questScores[3];
        for(int i=0;i<60 && previous==Imp.Quest.hazardFreebies && score==Statistics.questScores[3];i++) trap.act();
        assertNotNull(g.hero(seat).buff(Burning.class));
        assertTrue(previous!=Imp.Quest.hazardFreebies || score!=Statistics.questScores[3]);
    }

    public static void main(String[] args) throws Exception {
        VaultRewardTiers p=new VaultRewardTiers();p.temporary.create();p.startBackend();
        try(CoopSession g=CavesInteriorTest.open(572)) {
            Food food=new Food();
            RingOfEvasion[] rings={new RingOfEvasion(),new RingOfEvasion(),new RingOfEvasion()};
            rings[1].upgrade();rings[2].upgrade(2);
            for(int score:new int[]{0,49,50,499,500,999,1000,1001,1999,2000,2001,3999,4000,4050}) {
                assertEquals(score>=500,EscapeCrystal.rewardAllowed(food,score));
                assertFalse(EscapeCrystal.rewardAllowed(new EscapeCrystal(),score));
                for(int i=0;i<3;i++) {
                    int gate=i==0?1001:i==1?2001:4000;
                    assertEquals(score>=gate,EscapeCrystal.rewardAllowed(rings[i],score));
                }
                System.out.printf("TIER\t%d\t%s\t%s\t%s\t%s%n",score,
                        EscapeCrystal.rewardAllowed(food,score),EscapeCrystal.rewardAllowed(rings[0],score),
                        EscapeCrystal.rewardAllowed(rings[1],score),EscapeCrystal.rewardAllowed(rings[2],score));
            }
        }
        try {
            Bundle image;
            try(CoopSession g=CavesInteriorTest.open(572)) {
                ImpVaultTest.entrance(g);CoopFloorsTest.cross(g);
                assertEquals(2,Imp.Quest.hazardFreebies);
                hit(g,1);
                assertEquals(1,Imp.Quest.hazardFreebies);
                image=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(image)) {
                int old=Statistics.questScores[3];
                hit(g,0);
                assertEquals(0,Imp.Quest.hazardFreebies);
                assertEquals(old,Statistics.questScores[3]);
                hit(g,1);
                assertEquals(old-100,Statistics.questScores[3]);
                System.out.println("HAZARD original fire, guest/host/guest, two hits free and then -100, restored between hits");
            }
        } finally {p.stopBackend();p.temporary.delete();}
    }
}
