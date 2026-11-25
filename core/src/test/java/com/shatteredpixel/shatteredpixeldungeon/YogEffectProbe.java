package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Isolated native effect/clock checks and a separate finite, ordinary-stat arena encounter. */
public class YogEffectProbe extends HeadlessDungeon {
    private static Bundle stage(int seed,String threshold) {
        try (CoopSession game=CavesInteriorTest.open(578)) {
            YogArenaTest.arrive(game,seed);
            YogArenaTest.start(game);
            assertNotNull(YogArenaTest.fight(game,new java.util.ArrayList<>(),threshold));
            return game.checkpoint();
        }
    }

    private static void isolated(Bundle before,int seat) throws Exception {
        String kind;
        int fistID;
        Bundle after;
        try (CoopSession game=CoopSession.restore(before)) {
            YogFist fist=YogArenaTest.resident(YogFist.class);
            kind=fist.getClass().getSimpleName();
            fistID=fist.id();
            Hero target=game.hero(seat),other=game.hero(1-seat);
            int otherHP=other.HP;
            com.shatteredpixel.shatteredpixeldungeon.levels.Level level=Dungeon.level;
            for (int cell=0;cell<level.length();cell++) {
                if (level.map[cell]==Terrain.EMPTY && Actor.findChar(cell)==null && level.distance(cell,fist.pos)>3) {
                    target.pos=cell;
                    break;
                }
            }
            fist.aggro(target);
            Light lamp=Buff.affect(target,Light.class,Light.DURATION);
            float light=lamp.cooldown(),clock=fist.cooldown();
            java.lang.reflect.Method zap=fist.getClass().getDeclaredMethod("zap");
            zap.setAccessible(true);
            int hp=target.HP;
            boolean fired=false;
            for (int i=0;i<24 && !fired;i++) {
                zap.invoke(fist);
                switch(kind) {
                    case "BurningFist":fired=target.buff(Burning.class)!=null && Blob.volumeAt(target.pos,Fire.class)>0;break;
                    case "SoiledFist":fired=target.buff(Roots.class)!=null;break;
                    case "RottingFist":fired=Blob.volumeAt(target.pos,ToxicGas.class)>=100;break;
                    case "RustedFist":fired=target.buff(Cripple.class)!=null;break;
                    case "BrightFist":fired=target.HP<hp && target.buff(Blindness.class)!=null;break;
                    case "DarkFist":fired=target.HP<hp && lamp.cooldown()<light;break;
                    default:throw new AssertionError(kind);
                }
            }
            assertTrue(kind+" native effect",fired);
            assertEquals(otherHP,other.HP);
            assertTrue(fist.cooldown()>clock);
            if (fist instanceof YogFist.RustedFist || fist instanceof YogFist.RottingFist) {
                assertTrue(fist.isInvulnerable(Hero.class));
                YogDzewa boss=YogArenaTest.resident(YogDzewa.class);
                for (int cell=0;cell<level.length();cell++) {
                    if (level.openSpace[cell] && Actor.findChar(cell)==null && level.distance(cell,boss.pos)>8) {
                        fist.pos=cell;
                        break;
                    }
                }
                assertFalse(fist.isInvulnerable(Hero.class));
                int original=fist.HP;
                fist.damage(24,target);
                assertEquals(original,fist.HP);
                if (fist instanceof YogFist.RustedFist) assertNotNull(fist.buff(Viscosity.DeferedDamage.class));
                else assertNotNull(fist.buff(Bleeding.class));
            }
            after=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(after)) {
            assertEquals(fistID,YogArenaTest.resident(YogFist.class).id());
            TenguArenaTest.waitOne(game);
            System.out.println("PASS native cast "+kind+" seat="+seat+" restore and next action");
        }
    }

    private static void growth() {
        try(CoopSession game=CavesInteriorTest.open(578)) {
            YogArenaTest.arrive(game);
            for(int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                while (hero.lvl<25) hero.earnExp(hero.maxExp(),YogEffectProbe.class);
                hero.HTBoost=0;
                hero.updateHT(false);
                hero.HP=hero.HT;
                hero.STR=20;
                hero.belongings.weapon=new Greatsword();
                hero.belongings.weapon.level(4);
                hero.belongings.armor=new PlateArmor();
                hero.belongings.armor.level(4);
                game.ownership.claim(hero.belongings.weapon,game.player(seat));
                game.ownership.claim(hero.belongings.armor,game.player(seat));
                System.out.println("GROWTH_ARENA seat="+seat+" lvl="+hero.lvl+" hp="+hero.HP+" strength="+hero.STR+" Greatsword+4 PlateArmor+4");
            }
            YogArenaTest.start(game);
            long start=game.turn();
            java.util.ArrayList<String> dead=new java.util.ArrayList<>();
            try { YogArenaTest.fight(game,dead); }
            catch (AssertionError e) { if (!game.defeated() && game.life(0)==CoopSession.Life.ALIVE && game.life(1)==CoopSession.Life.ALIVE) throw e; }
            YogDzewa boss=YogArenaTest.resident(YogDzewa.class);
            System.out.println("GROWTH_RESULT seed=578 actions="+(game.turn()-start)+" hp="+game.hero(0).HP+"/"+game.hero(1).HP+" phase="+(boss==null?"won":boss.phase())+" boss_hp="+(boss==null?0:boss.HP)+" defeated_fists="+dead);
        }
    }

    public static void main(String[] args) throws Exception {
        YogEffectProbe probe=new YogEffectProbe();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (Object[] roll:new Object[][]{{570,"first"},{570,"second"},{570,"third"},{572,"second"},{573,"first"},{573,"second"}}) {
                Bundle state=stage((int)roll[0],(String)roll[1]);
                for(int seat=0;seat<2;seat++) isolated(state,seat);
            }
            growth();
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
