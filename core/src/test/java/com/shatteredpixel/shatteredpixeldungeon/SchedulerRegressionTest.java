package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import org.junit.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class SchedulerRegressionTest extends HeadlessDungeon {
    @Test(timeout = 30000)
    public void schedulerExpiresBuffAndPreservesElapsedTimeAcrossSave() throws Exception {
        Dungeon.seed = 410;
        Dungeon.init();
        Dungeon.switchLevel(Dungeon.newLevel(), -1);
        Hero hero = Dungeon.hero;
        new TestSprite(hero);
        for (Mob mob : Dungeon.level.mobs) new TestSprite(mob);
        Buff.prolong(hero, Haste.class, 1.5f);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(Actor::process, "headless-actor");
        worker.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
        try {
            synchronized (worker) {
                worker.start();
                awaitInput(hero, worker, failure);
                float start = Actor.now();
                for (int turn = 1; turn <= 2; turn++) {
                    hero.rest(false);
                    worker.notifyAll();
                    awaitInput(hero, worker, failure);
                    assertEquals(start + turn, Actor.now(), 0.001f);
                    assertEquals(turn == 1, hero.buff(Haste.class) != null);
                }
                float elapsed = Actor.now() + Statistics.duration;
                Dungeon.saveAll();
                assertEquals(elapsed, Actor.now() + Statistics.duration, 0.001f);
            }
        } finally {
            synchronized (worker) {
                Actor.keepActorThreadAlive = false;
                worker.notifyAll();
            }
            worker.join(3000);
            assertFalse("Actor thread did not stop", worker.isAlive());
            Actor.keepActorThreadAlive = true;
        }
    }

    private void awaitInput(Hero hero, Thread worker, AtomicReference<Throwable> failure)
            throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        // The render loop wakes an idle Actor after a completed visual yields. Wait for
        // the worker's input park as well as Hero.ready so that a reply cannot be lost.
        while (!(hero.ready && worker.getState()==Thread.State.WAITING)
                && failure.get() == null && worker.isAlive()) {
            long remaining = end - System.nanoTime();
            if (remaining <= 0) break;
            if (!hero.ready && !Actor.processing() && worker.getState()==Thread.State.WAITING) worker.notifyAll();
            TimeUnit.NANOSECONDS.timedWait(worker, Math.min(remaining,TimeUnit.MILLISECONDS.toNanos(16)));
        }
        if (failure.get() != null) throw new AssertionError("Actor processing failed", failure.get());
        assertTrue("Actor did not reach the next hero input", hero.ready);
        assertEquals("Actor did not park for input",Thread.State.WAITING,worker.getState());
    }
}
