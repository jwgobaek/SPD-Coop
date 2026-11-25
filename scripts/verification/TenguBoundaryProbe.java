package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.watabou.utils.Bundle;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;

/** Separate writer and reader processes. These are native level bundles, not playable co-op saves. */
public class TenguBoundaryProbe extends HeadlessDungeon {
    private static final PrisonBossLevel.State[] STAGES = {
            PrisonBossLevel.State.START, PrisonBossLevel.State.FIGHT_START,
            PrisonBossLevel.State.FIGHT_PAUSE, PrisonBossLevel.State.FIGHT_ARENA};

    public static void main(String[] args) throws Exception {
        TenguBoundaryProbe probe=new TenguBoundaryProbe();
        probe.temporary.create();
        probe.startBackend();
        try (CoopSession game=CoopSession.open(570,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.MAGE);
            Dungeon.depth=10;
            Path dir=Paths.get(args[1]);
            if (args[0].equals("write")) probe.write(game,dir);
            else if (args[0].equals("read")) probe.read(dir);
            else throw new IllegalArgumentException("Expected write or read");
        } finally { probe.stopBackend(); probe.temporary.delete(); }
    }

    private void write(CoopSession game, Path dir) throws Exception {
        PrisonBossLevel floor=new PrisonBossLevel();
        floor.create();
        Dungeon.level=floor;
        Actor.clear();
        for(int i=0;i<2;i++) {
            Hero hero=game.hero(i);
            hero.pos=floor.entrance()+i;
            hero.fieldOfView=new boolean[floor.length()];
            Actor.add(hero);
        }
        floor.drop(new Food(),floor.entrance());
        for(PrisonBossLevel.State expected:STAGES) {
            if(expected==PrisonBossLevel.State.FIGHT_START) {
                game.hero(1).pos=24*floor.width()+10;
                floor.occupyCell(game.hero(1));
            } else if(expected==PrisonBossLevel.State.FIGHT_PAUSE) floor.progress();
            else if(expected==PrisonBossLevel.State.FIGHT_ARENA) {
                game.hero(0).pos=8*floor.width()+10;
                floor.occupyCell(game.hero(0));
            }
            assertEquals(expected,floor.state());
            Bundle data=new Bundle();
            floor.storeInBundle(data);
            try(OutputStream out=Files.newOutputStream(dir.resolve(expected.name()))) {
                assertTrue(Bundle.write(data,out));
            }
        }
    }

    private void read(Path dir) throws Exception {
        int bossId=0;
        for(PrisonBossLevel.State expected:STAGES) {
            Actor.clear();
            Bundle data;
            try(InputStream in=Files.newInputStream(dir.resolve(expected.name()))) { data=Bundle.read(in); }
            PrisonBossLevel copy=new PrisonBossLevel();
            Dungeon.level=copy;
            copy.restoreFromBundle(data);
            assertEquals(expected,copy.state());
            assertEquals(expected!=PrisonBossLevel.State.START,copy.locked);
            int stored=data.getBundle("tengu").getInt("id");
            if(bossId==0) bossId=stored;
            assertTrue(bossId>0);
            assertEquals(bossId,stored);
            boolean fighting=expected==PrisonBossLevel.State.FIGHT_START || expected==PrisonBossLevel.State.FIGHT_ARENA;
            assertEquals(fighting?1:0,copy.mobs.stream().filter(m->m instanceof Tengu && m.id()==stored).count());
            if(expected==PrisonBossLevel.State.FIGHT_PAUSE || expected==PrisonBossLevel.State.FIGHT_ARENA)
                assertTrue(copy.storedEncounterItems().stream().anyMatch(item->item instanceof Food));
            Bundle after=new Bundle();
            copy.storeInBundle(after);
            assertEquals(stored,after.getBundle("tengu").getInt("id"));
            assertEquals(data.getBundle("tengu").getInt("HP"),after.getBundle("tengu").getInt("HP"));
            System.out.println("NATIVE_BOSS_BUNDLE "+expected+" id="+stored+" scheduled_resident="+fighting+
                    " stored_items="+copy.storedEncounterItems().size()+" locked="+copy.locked);
        }
    }
}
