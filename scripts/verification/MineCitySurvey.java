package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.Bundle;
import java.util.Set;
import java.util.TreeSet;
import java.nio.charset.StandardCharsets;

/** Native generation uses all twelve consecutive seeds. The separate stair probe uses placed heroes. */
public class MineCitySurvey extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        MineCitySurvey probe=new MineCitySurvey();probe.temporary.create();probe.startBackend();
        try {
            for(int seed=570;seed<=581;seed++) {
                Dungeon.seed=seed;Dungeon.init();
                for(int depth=1;depth<=21;depth++) {
                    Dungeon.depth=depth;
                    Level level=Dungeon.level=Dungeon.newLevel();
                    if (depth<16) continue;
                    Set<String> actors=new TreeSet<>(),rooms=new TreeSet<>(),traps=new TreeSet<>(),goods=new TreeSet<>();
                    for (Mob m:level.mobs) actors.add(m.getClass().getSimpleName());
                    for (Trap t:level.traps.valueList()) traps.add(t.getClass().getSimpleName());
                    if (level instanceof RegularLevel) for(Room room:((RegularLevel)level).rooms()) rooms.add(room.getClass().getSimpleName());
                    for (Heap heap:level.heaps.valueList()) for (Item item:heap.items) goods.add(item.getClass().getSimpleName());
                    System.out.println("GENERATED\t"+seed+"\t"+depth+"\t"+level.getClass().getSimpleName()+"\t"+actors+"\t"+rooms+"\t"+traps+"\t"+goods);
                }
            }
            Actor.clear();Dungeon.hero=null;Dungeon.level=null;
            for (int seed=570;seed<=581;seed++) {
                try(CoopSession game=CavesInteriorTest.open(seed)) {
                    CityEntranceTest.win(game);
                    for (int destination=16; destination<=19; destination++) {
                        SewerSpecialActorTest.atGate(game,Dungeon.level.exit());
                        int cell=game.stairs.stair();
                        game.stairs.consent(1-game.activeSeat(),game.turn(),cell,false);
                        long start=System.nanoTime();
                        if (!CoopFloorsTest.step(game,CoopSession.Action.TRAVEL,cell,null)) {
                            System.out.println("STAIRS\t"+seed+"\t"+destination+"\tREJECT\t"+game.destinationBlocker);
                            break;
                        }
                        long elapsed=(System.nanoTime()-start)/1000;
                        Bundle saved=game.checkpoint();
                        int size=saved.toString().getBytes(StandardCharsets.UTF_8).length;
                        int actors=Actor.all().size();
                        SewerDepthTest.gate(game,false);
                        SewerDepthTest.gate(game,true);
                        TenguArenaTest.waitOne(game);
                        System.out.println("STAIRS\t"+seed+"\t"+destination+"\tROUNDTRIP\t"+elapsed+"\t"+size+"\t"+actors);
                    }
                } catch (RuntimeException | AssertionError error) {
                    System.out.println("STAIRS\t"+seed+"\tFAIL\t"+error);
                }
            }
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
