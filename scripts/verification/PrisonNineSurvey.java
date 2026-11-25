package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.utils.Bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import static org.junit.Assert.*;

/** Compile against the APK commit's Headless test classpath and run in the core assets directory. */
public class PrisonNineSurvey extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        PrisonNineSurvey survey = new PrisonNineSurvey();
        survey.temporary.create();
        survey.startBackend();
        try {
            for (int seed = 570; seed <= 581; seed++) survey.cross(seed);
        } finally {
            survey.stopBackend();
            survey.temporary.delete();
        }
    }

    private static int dormantActors(Bundle records) {
        int count = 0;
        for (String key : records.getKeys()) {
            Bundle floor = records.getBundle(key).getBundle("level");
            for (Bundle mob : floor.getBundleArray("mobs")) {
                count += 1 + mob.getBundleArray("buffs").length;
            }
            count += floor.getBundleArray("blobs").length;
            if (floor.contains("respawner")) count++;
        }
        return count;
    }

    private void cross(int seed) throws Exception {
        ByteArrayOutputStream snapshot = new ByteArrayOutputStream();
        int[] ids = new int[2];
        int activeOnNine;
        try (CoopSession game = CoopSession.open(seed,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.MAGE);
            WandmakerTest.enterPrison(game);
            ids[0] = game.hero(0).id();
            ids[1] = game.hero(1).id();
            SewerDepthTest.gate(game,true);
            assertEquals(8,Dungeon.depth);
            SewerDepthTest.gate(game,true);
            assertEquals(9,Dungeon.depth);
            activeOnNine = Actor.all().size();
            assertTrue(Bundle.write(game.checkpoint(),snapshot));
        }
        Bundle saved=Bundle.read(new ByteArrayInputStream(snapshot.toByteArray()));
        assertEquals(6,saved.getInt("schema"));
        assertEquals(8,saved.getBundle("dormant").getKeys().size());
        try (CoopSession game = CoopSession.restore(saved)) {
            for(int seat=0;seat<2;seat++) assertEquals(ids[seat],game.hero(seat).id());
            assertNotEquals(game.hero(0).pos,game.hero(1).pos);
            SewerDepthTest.gate(game,false);
            assertEquals(8,Dungeon.depth);
            SewerDepthTest.gate(game,false);
            assertEquals(7,Dungeon.depth);
            SewerSpecialActorTest.waitOne(game);
            System.out.println("STAGED_NINE\t"+seed+"\t9\t8\t6\t7\tPASS\t"
                    +activeOnNine+"\t"+dormantActors(saved.getBundle("dormant")));
        }
    }
}
