package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.watabou.utils.Bundle;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import org.junit.Test;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import static org.junit.Assert.*;

/** Vanilla generation only. No hero is moved or credited with completing these floors. */
public class PrisonGenerationTest extends HeadlessDungeon {
    @Test public void consecutivePrisonWorldsRetainNativeRoomsStockAndBossPhase() {
        TreeSet<String> questRooms=new TreeSet<>();
        for (int seed=570;seed<=581;seed++) {
            Dungeon.seed=seed;
            Dungeon.init();
            for (int depth=1;depth<=10;depth++) {
                Dungeon.depth=depth;
                Level level=Dungeon.newLevel();
                Dungeon.level=level;
                if (depth<6) continue;
                TreeSet<String> mobs=new TreeSet<>(),rooms=new TreeSet<>(),traps=new TreeSet<>(),
                        plants=new TreeSet<>(),stock=new TreeSet<>(),loot=new TreeSet<>();
                for (Mob m:level.mobs) mobs.add(m.getClass().getSimpleName());
                for (Trap trap:level.traps.valueList()) traps.add(trap.getClass().getSimpleName());
                for (Plant plant:level.plants.valueList()) plants.add(plant.getClass().getSimpleName());
                if (depth<10) {
                    assertTrue(level instanceof PrisonLevel);
                    for (Room room:((PrisonLevel)level).rooms()) rooms.add(room.getClass().getSimpleName());
                    if (depth==6) assertTrue(mobs.contains("Shopkeeper"));
                    if (mobs.contains("Wandmaker")) for (String kind:new String[]{"MassGraveRoom","RitualSiteRoom","RotGardenRoom"})
                        if (rooms.contains(kind)) questRooms.add(kind);
                } else {
                    assertEquals(PrisonBossLevel.State.START,((PrisonBossLevel)level).state());
                }
                for (Heap h:level.heaps.valueList()) for (Item item:h.items)
                    (h.type==Heap.Type.FOR_SALE ? stock : loot).add(item.getClass().getSimpleName());
                String gate="DEPTH_LIMIT", rewards="-";
                if (depth<=CoopFloors.LAST_PLAYABLE) {
                    try {
                        CoopFloors.requireSupportedResidents(level);
                        gate="SUPPORTED_RESIDENTS";
                    } catch (CoopFloors.UnsupportedDestination blocked) {
                        gate=blocked.getMessage();
                    }
                }
                if (mobs.contains("Wandmaker")) {
                    rewards=Wandmaker.Quest.wand1.getClass().getSimpleName()+","
                            +Wandmaker.Quest.wand2.getClass().getSimpleName();
                }
                Bundle save=new Bundle(); save.put("level",level);
                System.out.println(String.join("\t","PRISON_GENERATION",Integer.toString(seed),Integer.toString(depth),
                        level.getClass().getSimpleName(),Integer.toString(level.mobs.size()),String.join(",",mobs),
                        String.join(",",rooms),String.join(",",traps),String.join(",",plants),String.join(",",stock),
                        String.join(",",loot),Integer.toString(save.toString().getBytes(StandardCharsets.UTF_8).length),gate,rewards));
            }
        }
        assertEquals(3,questRooms.size());
    }
}
