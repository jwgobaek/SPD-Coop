package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

/** Write a native pre-shot fixture, then replay the same seat action in a separate JVM. */
public class LightningChainProbe extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        LightningChainProbe probe = new LightningChainProbe();
        probe.temporary.create();
        probe.startBackend();
        try {
            Path path = Path.of(args[1]);
            if (args[0].equals("write")) stage(path,Integer.parseInt(args[2]),Boolean.parseBoolean(args[3]));
            else replay(path);
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }

    private static void stage(Path file,int seat,boolean water) throws Exception {
        try (CoopSession game = CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            Rat main = CoopMageTest.encounter(game,seat);
            SewerBeeTest.offer(game,seat);
            SealArtsTest.stage(game,"spark",seat,main);
            SewerBeeTest.offer(game,seat);
            Hero hero=game.hero(seat);
            main.pos=hero.pos+1;
            game.hero(1-seat).pos=hero.pos-4;
            Rat distant=new Rat();
            distant.HP=distant.HT=100;
            distant.pos=main.pos+2*Dungeon.level.width();
            GameScene.add(distant,2000);
            Level.set(main.pos,water ? Terrain.WATER : Terrain.EMPTY);
            game.observe(seat);
            Bundle b=new Bundle();
            b.put("state",game.checkpoint());
            b.put("seat",seat); b.put("water",water);
            b.put("first",main.id()); b.put("second",distant.id());
            try (OutputStream out=Files.newOutputStream(file)) { Bundle.write(b,out); }
        }
    }

    private static void replay(Path file) throws Exception {
        Bundle b;
        try (InputStream in=Files.newInputStream(file)) { b=Bundle.read(in); }
        int seat=b.getInt("seat");
        boolean wet=b.getBoolean("water");
        try (CoopSession game=CoopSession.restore(b.getBundle("state"))) {
            Hero hero=game.hero(seat);
            Rat first=(Rat)Actor.findById(b.getInt("first")), second=(Rat)Actor.findById(b.getInt("second"));
            MagesStaff staff=(MagesStaff)hero.belongings.weapon();
            int hp=hero.HP,partner=game.hero(1-seat).HP,charge=staff.imbuedWand().curCharges;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,first.pos,staff));
            assertTrue(hero.HP<hp && first.HP<first.HT);
            assertEquals(wet,second.HP<second.HT);
            assertEquals(partner,game.hero(1-seat).HP);
            assertEquals(charge-1,staff.imbuedWand().curCharges);
            System.out.printf("SHOT seat=%d wet=%b hero=%d first=%d/%d second=%d/%d wand=%d charge=%d partial=%s nextRng=%d%n",
                    seat,wet,hero.HP,first.id(),first.HP,second.id(),second.HP,
                    game.ownership.identity(staff.imbuedWand()),staff.imbuedWand().curCharges,staff.imbuedWand().partialCharge,Random.Long());
        }
    }
}
