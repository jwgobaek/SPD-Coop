package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultLaser;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultSentry;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.VaultBeacon;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class VaultPlayTest extends HeadlessDungeon {
    @Test public void bothSeatsPickUpOriginalBeaconsAndRecallWithTheirOwnOne() {
        for (int seat = 0; seat < 2; seat++) {
            Bundle saved;
            long id;
            try (CoopSession game = ImpVaultTest.openAtVaultEntrance(572)) {
                Heap pile = Dungeon.level.heaps.valueList().stream().filter(h -> h.peek() instanceof VaultBeacon)
                        .findFirst().orElseThrow(AssertionError::new);
                while (game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
                game.hero(seat).pos=pile.pos;
                int entrance = Dungeon.level.entrance();
                game.hero(1-seat).pos=entrance;
                game.observe(0);
                game.observe(1);
                VaultBeacon beacon=(VaultBeacon)pile.peek();
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,pile.pos,beacon));
                assertEquals(game.player(seat),game.ownership.owner(beacon));
                id=game.ownership.identity(beacon);
                while (game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
                PublicDungeon view=ProjectionChecks.seatImage(game,seat);
                assertTrue(view.belongings.stream().anyMatch(i->view.actions(i).contains("VAULT_RECALL")));
                long turn=game.turn();
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.VAULT_RECALL,0,beacon));
                assertEquals(turn,game.turn());
                assertTrue(game.hero(seat).belongings.backpack.contains(beacon));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero owner=game.hero(seat);
                VaultBeacon beacon=owner.belongings.getItem(VaultBeacon.class);
                assertEquals(id,game.ownership.identity(beacon));
                CavesBossTest.beside(game,1-seat,Dungeon.level.entrance());
                int otherPos=game.hero(1-seat).pos;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.VAULT_RECALL,0,beacon));
                assertEquals(Dungeon.level.entrance(),owner.pos);
                assertEquals(otherPos,game.hero(1-seat).pos);
                assertNull(owner.belongings.getItem(VaultBeacon.class));
                assertNull(game.hero(1-seat).belongings.getItem(VaultBeacon.class));
                game.checkpoint();
            }
        }
    }

    @Test public void originalVaultSickleCanBePickedUpEquippedAndRestoredByEitherSeat() {
        checkVaultMelee(572,Sickle.class);
    }

    @Test public void nativeVaultDirkIsUsableByEitherSeatAt578() {
        checkVaultMelee(578,com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dirk.class);
    }

    private void checkVaultMelee(int seed,Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon> type) {
        for (int seat=0; seat<2; seat++) {
            long id;
            Bundle saved;
            try (CoopSession game=ImpVaultTest.openAtVaultEntrance(seed)) {
                Heap pile=Dungeon.level.heaps.valueList().stream()
                        .filter(h -> type.isInstance(h.peek())).findFirst().orElseThrow(AssertionError::new);
                while (game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
                Hero hero=game.hero(seat);
                hero.pos=pile.pos;
                game.observe(seat);
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon=type.cast(pile.peek());
                assertNull(weapon.enchantment);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,pile.pos,weapon));
                while (game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
                id=game.ownership.identity(weapon);
                assertTrue(ProjectionChecks.seatImage(game,seat).belongings.stream()
                        .anyMatch(item -> item.sprite==weapon.image && item.kind.equals("MELEE")));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,weapon));
                assertSame(weapon,hero.belongings.weapon);
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon weapon=type.cast(game.hero(seat).belongings.weapon);
                assertEquals(id,game.ownership.identity(weapon));
                assertEquals(game.player(seat),game.ownership.owner(weapon));
                assertNull(game.hero(1-seat).belongings.getItem(type));
            }
        }
    }

    @Test public void actualVaultDartAt1872CanBePickedUpAndAimedByEitherSeat() {
        for (int seat = 0; seat < 2; seat++) {
            try (CoopSession game = ImpVaultTest.openAtVaultEntrance(572)) {
                int cell = 1872;
                Heap pile = Dungeon.level.heaps.get(cell);
                assertNotNull(pile);
                Item dart = pile.peek();
                assertEquals(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart.class, dart.getClass());
                int quantity = dart.quantity();
                while (game.activeSeat() != seat) TenguArenaTest.waitOne(game);
                game.hero(seat).pos = cell;
                game.observe(seat);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.PICK_UP, cell, dart));
                assertEquals(game.player(seat), game.ownership.owner(dart));
                assertEquals(quantity, dart.quantity());
                com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon view = ProjectionChecks.seatImage(game,seat);
                assertTrue(view.belongings.stream().anyMatch(i -> i.kind.equals("DART") && i.quantity == quantity
                        && i.stackType.startsWith("dart.") && "THROW".equals(view.shortcutAction(i))));
                assertFalse(dart.collect(game.hero(1-seat).belongings.backpack));
                game.checkpoint();
            }
        }
    }

    @Test public void looseDewHealsItsPickerAndRejectsAnUnneededDropBeforeSpending() {
        for (int seat=0;seat<2;seat++) {
            try (CoopSession game=ImpVaultTest.openAtVaultEntrance(572)) {
                while(game.activeSeat()!=seat) TenguArenaTest.waitOne(game);
                Hero hero=game.hero(seat);
                int pos=CavesBossTest.beside(game,seat,Dungeon.level.entrance());
                Dewdrop dew=new Dewdrop();
                Dungeon.level.drop(dew,pos);
                int other=game.hero(1-seat).HP;
                long turn=game.turn();
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,pos,dew));
                assertEquals(turn,game.turn());
                assertSame(dew,Dungeon.level.heaps.get(pos).peek());
                hero.HP-=5;
                int before=hero.HP;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,pos,dew));
                assertEquals(before+Math.round(hero.HT*0.05f),hero.HP);
                assertEquals(other,game.hero(1-seat).HP);
                assertNull(hero.belongings.getItem(dew.getClass()));
                game.checkpoint();
            }
        }
    }

    public static Mob positionWarning(CoopSession game, boolean laser, int viewer) {
        Mob mob = null;
        int cell = -1;
        // Generator order varies. Select an actual emitter with a passable first shot.
        for (Mob candidate : Dungeon.level.mobs) {
            Iterable<Integer> warning;
            if (laser && candidate instanceof VaultLaser) {
                VaultLaser trap=(VaultLaser)candidate;
                trap.curCooldown=2; trap.giveWarning=true;
                assertTrue(trap.warnedCells().isEmpty());
                trap.curCooldown=1; warning=trap.warnedCells();
            } else if (!laser && candidate instanceof VaultSentry) {
                VaultSentry trap=(VaultSentry)candidate;
                trap.curCooldown=1; trap.giveWarning=false;
                assertTrue(trap.warnedCells().isEmpty());
                trap.giveWarning=true; warning=trap.warnedCells();
            } else continue;
            for (int c:warning) {
                if (Dungeon.level.passable[c] && Actor.findChar(c)==null && Dungeon.level.adjacent(c,candidate.pos)) {
                    cell=c; mob=candidate; break;
                }
            }
            if (mob!=null) break;
        }
        assertNotNull("Expected a generated emitter with a clear adjacent shot",mob);
        for (Mob other:new java.util.ArrayList<>(Dungeon.level.mobs)) if (other!=mob) {
            Dungeon.level.mobs.remove(other);
            Actor.remove(other);
        }
        game.hero(viewer).pos=cell;
        game.observe(0);
        game.observe(1);
        return mob;
    }

    @Test public void laserAndScanTelegraphsFollowNativeGeometryAndSeatVisibilityAfterRestore() {
        for (boolean laser : new boolean[]{true,false}) {
            Bundle saved;
            int id, cell;
            try (CoopSession game=ImpVaultTest.openAtVaultEntrance(572)) {
                Mob mob=positionWarning(game,laser,1);
                id=mob.id();
                cell=game.hero(1).pos;
                PublicDungeon near=ProjectionChecks.seatImage(game,1);
                assertTrue(near.visible(mob.pos));
                assertTrue(near.source.rows.containsKey("aim"+cell));
                PublicDungeon far=ProjectionChecks.seatImage(game,0);
                for (String key:far.source.rows.keySet()) if (key.startsWith("aim"))
                    assertTrue(far.visible(Integer.parseInt(key.substring(3))));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                assertNotNull(Actor.findById(id));
                assertTrue(ProjectionChecks.seatImage(game,1).source.rows.containsKey("aim"+cell));
                int hp=game.hero(1).HP;
                for (int t=0;t<5 && game.hero(1).HP==hp;t++) TenguArenaTest.waitOne(game);
                assertTrue(game.hero(1).HP<hp);
            }
        }
    }
}
