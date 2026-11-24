package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class DungeonGenerationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test(timeout = 30000)
    public void warriorBeginsOnGeneratedSewerFloor() throws Exception {
        String storage = temporary.getRoot().getAbsolutePath() + File.separator;
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.preferencesDirectory = storage;
        CountDownLatch stopped = new CountDownLatch(1);
        HeadlessApplication app = new HeadlessApplication(new ApplicationAdapter() {
            @Override public void dispose() { stopped.countDown(); }
        }, config);
        try {
            FileUtils.setDefaultFileProperties(Files.FileType.Absolute, storage);
            Game.version = "4.0.2-INDEV";
            Game.versionCode = 922;
            GamesInProgress.curSlot = 1;
            GamesInProgress.selectedClass = HeroClass.WARRIOR;
            Dungeon.seed = 402;
            Dungeon.init();
            Level floor = Dungeon.newLevel();
            assertTrue(floor instanceof SewerLevel);
            // Recorded from the v4.0.2 reference checkout for warrior seed 402.
            assertEquals(-585159512, java.util.Arrays.hashCode(floor.map));
            assertNotEquals(floor.entrance(), floor.exit());
            assertEquals(floor.width() * floor.height(), floor.map.length);
            assertEquals(HeroClass.WARRIOR, Dungeon.hero.heroClass);
            assertTrue(Dungeon.hero.isAlive());
            Dungeon.switchLevel(floor, -1);
            assertEquals(floor.entrance(), Dungeon.hero.pos);
            assertArrayEquals(floor.map, Dungeon.loadLevel(1).map);
        } finally {
            app.exit();
            assertTrue("Headless backend failed to stop", stopped.await(5, TimeUnit.SECONDS));
        }
    }
}
