package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public abstract class HeadlessDungeon {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final CountDownLatch stopped = new CountDownLatch(1);
    private HeadlessApplication app;

    @Before public void startBackend() throws Exception {
        String storage = temporary.getRoot().getAbsolutePath() + File.separator;
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.preferencesDirectory = storage;
        app = new HeadlessApplication(new ApplicationAdapter() {
            @Override public void dispose() { stopped.countDown(); }
        }, config);
        FileUtils.setDefaultFileProperties(Files.FileType.Absolute, storage);
        Game.version = "4.0.2-INDEV";
        Game.versionCode = 922;
        GamesInProgress.curSlot = 1;
        GamesInProgress.selectedClass = HeroClass.WARRIOR;
        // Rendering never starts in this backend. The engine is already in its playable scene.
        new Game(null, null) {
            { requestedReset = false; }
        };
    }

    // The engine owns the action. Only its visual completion is driven by the test.
    static class TestSprite extends CharSprite {
        int attackFrames;
        int operateFrames;
        TestSprite(Char owner) {
            ch = owner;
            owner.sprite = this;
            visible = false;
        }
        @Override public void move(int from, int to) { ch.onMotionComplete(); }
        @Override public void attack(int cell) { attackFrames++; }
        @Override public void operate(int cell) { operateFrames++; }
    }

    @After public void stopBackend() throws Exception {
        app.exit();
        assertTrue("Headless backend failed to stop", stopped.await(5, TimeUnit.SECONDS));
    }
}
