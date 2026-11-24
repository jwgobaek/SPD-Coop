package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom.Sentry;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SewerSpecialActorTest extends HeadlessDungeon {
    // Test placement affects neither the generated room nor its occupants and clocks.
    public static void atGate(CoopSession game, int cell) {
        Level l = Dungeon.level;
        int seat = 0;
        for (int off : PathFinder.NEIGHBOURS8) {
            int p = cell + off;
            if (l.passable[p] && !l.pit[p] && Actor.findChar(p) == null && l.traps.get(p) == null)
                game.hero(seat++).pos = p;
            if (seat == 2) break;
        }
        assertEquals(2,seat);
        game.observe(0);
        game.observe(1);
    }

    static <T extends Mob> T actor(Class<T> type) {
        for (Mob m : Dungeon.level.mobs) if (m.getClass() == type) return type.cast(m);
        throw new AssertionError("Missing " + type.getSimpleName());
    }

    static void waitOne(CoopSession g) {
        assertTrue(CoopFloorsTest.step(g, CoopSession.Action.WAIT, 0, null));
    }

    static Bundle fields(Actor a) {
        Bundle b = new Bundle();
        a.storeInBundle(b);
        return b;
    }

    public static int[] sentryCells() {
        Level l = Dungeon.level;
        Sentry sentry = actor(Sentry.class);
        boolean[] sight = new boolean[l.length()];
        l.updateFieldOfView(sentry,sight);
        List<Integer> exposed = new ArrayList<>();
        for (Room room : ((RegularLevel)l).rooms()) if (room instanceof SentryRoom && room.inside(l.cellToPoint(sentry.pos))) {
            for (int p = 0; p < l.length(); p++) if (sight[p] && room.inside(l.cellToPoint(p))
                    && l.map[p] == Terrain.EMPTY_SP && Actor.findChar(p) == null) exposed.add(p);
        }
        assertTrue(exposed.size() >= 2);
        return new int[]{exposed.get(0),exposed.get(1)};
    }

    @Test public void singleBeamTracksEitherExposedHeroAndRestartsWhenTheyLeave() {
        for (int seat = 0; seat < 2; seat++) {
            Bundle checkpoint;
            int[] cells;
            int safe, id, hp, otherHp;
            try (CoopSession g = CoopSession.open(402,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.WARRIOR);
                atGate(g,Dungeon.level.exit());
                CoopFloorsTest.cross(g);
                Sentry sentry = actor(Sentry.class);
                id = sentry.id();
                cells = sentryCells();
                Hero h = g.hero(seat), other = g.hero(1-seat);
                safe = h.pos;
                otherHp = other.HP;
                h.pos = cells[0];
                for (int n=0; !sentry.charging() && n<5; n++) waitOne(g);
                assertEquals(h.id(),fields(sentry).getInt("coopTarget"));
                hp = h.HP;
                assertTrue(fields(sentry).getFloat("cur_delay") < fields(sentry).getFloat("initial_delay"));
                assertTrue(fields(sentry).getFloat("cur_delay") > 0);
                checkpoint = g.checkpoint();
            }
            try (CoopSession g = CoopSession.restore(checkpoint)) {
                Sentry sentry = actor(Sentry.class);
                Hero h=g.hero(seat), other=g.hero(1-seat);
                assertEquals(id,sentry.id());
                assertEquals(h.id(),fields(sentry).getInt("coopTarget"));
                other.pos = cells[1];
                for (int n=0; h.HP==hp && n<24; n++) waitOne(g);
                assertTrue(h.HP < hp);
                assertEquals(otherHp,other.HP);
                h.pos = safe;
                for (int n=0; fields(sentry).getInt("coopTarget")==h.id() && n<4; n++) waitOne(g);
                assertEquals(other.id(),fields(sentry).getInt("coopTarget"));
                assertEquals(otherHp,other.HP);
                assertTrue(new Ankh().collect(other.belongings.backpack));
                other.HP=1;
                int safeHp=h.HP;
                for (int n=0; other.isAlive() && n<32; n++) waitOne(g);
                assertEquals(CoopSession.Life.DOWNED,g.life(1-seat));
                assertEquals(1,g.preDeathHP(1-seat));
                assertEquals(safeHp,h.HP);
                waitOne(g);
                assertEquals(seat,g.activeSeat());
            }
        }
    }

    public static void exposeOneSeat(CoopSession game, int seat, boolean sentryRoom) {
        if (sentryRoom) {
            game.hero(seat).pos = sentryCells()[0];
        } else {
            Piranha fish = actor(Piranha.class);
            for (int offset : PathFinder.NEIGHBOURS8) {
                int p = fish.pos + offset;
                if (Dungeon.level.passable[p] && Actor.findChar(p) == null && !Dungeon.level.pit[p]) {
                    game.hero(seat).pos = p;
                    fish.aggro(game.hero(seat));
                    return;
                }
            }
            throw new AssertionError("No piranha encounter cell");
        }
    }

    @Test public void piranhaUsesWaterTargetsEitherSeatAndDropsNativeLootOnce() {
        for (int seat=0; seat<2; seat++) {
            try (CoopSession g=CoopSession.open(406,1,HeroClass.MAGE)) {
                g.join(2,HeroClass.WARRIOR);
                atGate(g,Dungeon.level.exit());
                CoopFloorsTest.cross(g);
                Piranha fish=actor(Piranha.class);
                int id=fish.id(), home=fish.pos, hp=fish.HP;
                assertTrue(Dungeon.level.water[home]);
                atGate(g,Dungeon.level.entrance());
                CoopFloorsTest.cross(g);
                assertNull(Actor.findById(id));
                atGate(g,Dungeon.level.exit());
                CoopFloorsTest.cross(g);
                fish=(Piranha)Actor.findById(id);
                assertEquals(home,fish.pos);
                assertEquals(hp,fish.HP);
                int near=-1;
                for (int offset:PathFinder.NEIGHBOURS8) {
                    int p=fish.pos+offset;
                    if (Dungeon.level.passable[p] && Actor.findChar(p)==null && !Dungeon.level.pit[p]) {near=p;break;}
                }
                assertTrue(near>=0);
                Hero h=g.hero(seat), other=g.hero(1-seat);
                h.pos=near;
                int otherHp=other.HP, heroHp=h.HP;
                fish.aggro(h);
                for (int n=0; h.HP==heroHp && n<24; n++) {
                    waitOne(g);
                    assertTrue(Dungeon.level.water[fish.pos]);
                }
                assertTrue(h.HP < heroHp);
                assertEquals(otherHp,other.HP);
                int cell=fish.pos;
                fish.damage(fish.HP+100,h);
                assertFalse(Dungeon.level.mobs.contains(fish));
                assertTrue(Dungeon.level.heaps.get(cell).peek() instanceof MysteryMeat);
                assertEquals(1,Dungeon.level.heaps.get(cell).peek().quantity());
                assertNotNull(g.checkpoint());
            }
        }
    }
}
