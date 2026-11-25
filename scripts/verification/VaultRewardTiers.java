package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEvasion;
import static org.junit.Assert.*;

/** Boundary inputs exercise the reward predicate, not earned scores. */
public final class VaultRewardTiers extends HeadlessDungeon {
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
        } finally {p.stopBackend();p.temporary.delete();}
    }
}
