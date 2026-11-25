package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental.FrostElemental;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Golem;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Monk;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

public class CityResidentProbe extends HeadlessDungeon {
    private static Bundle floor(int seed,int depth) {
        try (CoopSession game=CavesInteriorTest.open(seed)) {
            CityKingTest.reach(game,depth);
            return game.checkpoint();
        }
    }

    private static void bolt(Bundle initial,int seat,Class<? extends Elemental> kind,Class<? extends Buff> effect) {
        Bundle saved;
        int caster,slow;
        try (CoopSession game=CoopSession.restore(initial)) {
            Elemental casterMob=SewerSpecialActorTest.actor(kind);
            Hero target=game.hero(seat);
            casterMob.aggro(target);
            for (int i=0;i<30 && target.buff(effect)==null;i++) casterMob.onZapComplete();
            assertNotNull(target.buff(effect));
            assertNull(game.hero(1-seat).buff(effect));
            caster=casterMob.id();slow=target.buff(effect).id();
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            assertTrue(kind.isInstance(Actor.findById(caster)));
            assertEquals(slow,game.hero(seat).buff(effect).id());
            TenguArenaTest.waitOne(game);
        }
    }

    private static void pull(Bundle initial,int seat) {
        Bundle saved;
        int id,landing;
        try (CoopSession game=CoopSession.restore(initial)) {
            Golem golem=SewerSpecialActorTest.actor(Golem.class);
            Hero target=game.hero(seat);
            int other=game.hero(1-seat).pos;
            int before=target.pos;
            golem.aggro(target);
            golem.teleportEnemy();
            landing=target.pos;id=golem.id();
            assertNotEquals(before,landing);
            assertTrue(Dungeon.level.adjacent(golem.pos,landing));
            assertEquals(other,game.hero(1-seat).pos);
            Bundle state=new Bundle();golem.storeInBundle(state);
            assertEquals(20,state.getInt("enemy_cooldown"));
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            Golem golem=(Golem)Actor.findById(id);
            Bundle state=new Bundle();golem.storeInBundle(state);
            assertEquals(20,state.getInt("enemy_cooldown"));
            assertEquals(landing,game.hero(seat).pos);
            TenguArenaTest.waitOne(game);
        }
    }

    private static void parry(Bundle initial,int seat) {
        try (CoopSession game=CoopSession.restore(initial)) {
            Monk monk=SewerSpecialActorTest.actor(Monk.class);
            SewerBeeTest.offer(game,seat);
            MiningBranchTest.standBeside(game,monk.pos);
            monk.aggro(game.hero(seat));
            for (int i=0;i<16 && monk.buff(Monk.Focus.class)==null;i++) TenguArenaTest.waitOne(game);
            assertNotNull(monk.buff(Monk.Focus.class));
            assertEquals(com.shatteredpixel.shatteredpixeldungeon.actors.Char.INFINITE_EVASION,monk.defenseSkill(game.hero(seat)));
            monk.defenseVerb();
            assertNull(monk.buff(Monk.Focus.class));
            Bundle state=new Bundle();monk.storeInBundle(state);
            assertTrue(state.getFloat("focus_cooldown")>=6 && state.getFloat("focus_cooldown")<=7);
        }
    }

    public static void main(String[] args) throws Exception {
        CityResidentProbe p=new CityResidentProbe();p.temporary.create();p.startBackend();
        try {
            Bundle city=floor(572,16),fire=floor(572,17),shock=floor(575,16),deep=floor(572,19);
            for(int seat=0;seat<2;seat++) {
                bolt(city,seat,FrostElemental.class,Chill.class);
                bolt(fire,seat,Elemental.FireElemental.class,Burning.class);
                bolt(shock,seat,Elemental.ShockElemental.class,Blindness.class);
                pull(deep,seat);parry(deep,seat);
                System.out.println("PASS seeds=572,575 seat="+seat+" frost/fire/shock ranged status and Golem pull across restore, Monk focus and native parry");
            }
        } finally {p.stopBackend();p.temporary.delete();}
    }
}
