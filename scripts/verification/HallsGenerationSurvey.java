package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.Bundle;
import java.util.*;
import static org.junit.Assert.*;

/** Native generation and original boss bundle survey. Does not enter or win a co-op boss fight. */
public class HallsGenerationSurvey extends HeadlessDungeon {
    static String order(Bundle state,String key) {
        StringJoiner list=new StringJoiner(",");
        for (Class<?> type:state.getClassArray(key)) list.add(type.getSimpleName());
        return list.toString();
    }

    public static void main(String[] args) throws Exception {
        HallsGenerationSurvey probe=new HallsGenerationSurvey();
        probe.temporary.create();
        probe.startBackend();
        try {
            for(int seed=570;seed<=581;seed++) {
                Dungeon.seed=seed;
                Dungeon.init();
                for (int depth=1;depth<=26;depth++) {
                    Dungeon.depth=depth;
                    Level l=Dungeon.level=Dungeon.newLevel();
                    if (depth<21) continue;
                    Set<String> residents=new TreeSet<>(),rooms=new TreeSet<>(),traps=new TreeSet<>(),loot=new TreeSet<>();
                    int amulets=0;
                    for (Mob mob:l.mobs) residents.add(mob.getClass().getSimpleName());
                    for (Trap trap:l.traps.valueList()) traps.add(trap.getClass().getSimpleName());
                    for (Heap heap:l.heaps.valueList()) for (Item item:heap.items) {
                        loot.add(item.getClass().getSimpleName());
                        if (item instanceof Amulet) amulets+=item.quantity();
                    }
                    if (l instanceof RegularLevel) for (Room room:((RegularLevel)l).rooms()) rooms.add(room.getClass().getSimpleName());
                    System.out.println("FLOOR\t"+seed+"\t"+depth+"\t"+l.getClass().getSimpleName()+"\t"+l.width()+"x"+l.height()
                            +"\t"+String.join(",",residents)+"\t"+String.join(",",rooms)+"\t"+String.join(",",traps)+"\t"+String.join(",",loot)+"\t"+l.tilesTex());
                    if (depth==25) {
                        assertTrue(l instanceof HallsBossLevel);
                        YogDzewa boss=new YogDzewa();
                        Bundle b=new Bundle();boss.storeInBundle(b);
                        YogDzewa copied=new YogDzewa();
                        copied.restoreFromBundle(b);
                        Bundle after=new Bundle();copied.storeInBundle(after);
                        for (String key:new String[]{"fist_summons","challenges_summons","regular_summons"})
                            assertEquals(order(b,key),order(after,key));
                        assertEquals(3,b.getClassArray("fist_summons").length);
                        assertEquals(4,b.getClassArray("regular_summons").length);
                        System.out.println("BOSS\t"+seed+"\t"+Statistics.spawnersAlive+"\t"+b.getInt("phase")+"\t"+boss.HP
                                +"\t"+order(b,"fist_summons")+"\t"+order(b,"regular_summons"));
                    }
                    if (depth==26) { assertTrue(l instanceof LastLevel);assertEquals(1,amulets); }
                }
            }
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
