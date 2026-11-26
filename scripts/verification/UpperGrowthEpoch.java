package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ScrollEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

/** Run write and read in separate JVMs built from the source revisions under comparison. */
public class UpperGrowthEpoch extends HeadlessDungeon {
    private static void write(Path file) throws Exception {
        try (CoopSession room = CoopSession.open(571,11,HeroClass.WARRIOR)) {
            room.join(22,HeroClass.HUNTRESS);
            CoopMageTest.encounter(room,1);
            SewerBeeTest.offer(room,1);
            Hero wearer = room.hero(1);
            long item = room.ownership.identity(wearer.belongings.armor());
            assertTrue(CoopFloorsTest.step(room,CoopSession.Action.USE_CROWN,CrownAbilityTest.choice(wearer),
                    CrownAbilityTest.crown(room,1,CoopSession.MaskTrial.OWNER)));
            Bundle data = new Bundle();
            data.put("test_item",item);
            data.put("room",room.checkpoint());
            assertEquals(17,data.getBundle("room").getInt("schema"));
            try (OutputStream stream = Files.newOutputStream(file)) {
                assertTrue(Bundle.write(data,stream));
            }
        }
    }

    private static void read(Path file) throws Exception {
        Bundle data;
        try (InputStream stream = Files.newInputStream(file)) {
            data = Bundle.read(stream);
        }
        try (CoopSession room = CoopSession.restore(data.getBundle("room"))) {
            Hero wearer = room.hero(1);
            assertTrue(wearer.belongings.armor() instanceof ClassArmor);
            assertEquals(data.getLong("test_item"),room.ownership.identity(wearer.belongings.armor()));
            assertEquals("OWNER",room.checkpoint().getString("crown_trial"));
            assertFalse(room.mayChooseArmor(0,false));
            SewerBeeTest.offer(room,1);
            assertTrue(CoopFloorsTest.step(room,CoopSession.Action.ARMOR_ABILITY,wearer.pos,wearer.belongings.armor()));
            assertTrue(((ClassArmor)wearer.belongings.armor()).charge < 30);
        }
    }

    private static void tierTwo(int seat, int points, HeroClass type) {
        Bundle saved;
        try (CoopSession room = CoopSession.open(571,11,type)) {
            room.join(22,type);
            CoopMageTest.encounter(room,seat);
            SewerBeeTest.offer(room,seat);
            Hero h = room.hero(seat);
            h.lvl = 12;
            Talent talent = type == HeroClass.MAGE ? Talent.INSCRIBED_POWER : Talent.REJUVENATING_STEPS;
            for (int i=0;i<points;i++) assertTrue(CoopFloorsTest.step(room,CoopSession.Action.INVEST_TALENT,talent.ordinal(),null));
            if (type == HeroClass.MAGE) {
                ScrollOfUpgrade scroll = new ScrollOfUpgrade();
                scroll.identify();
                assertTrue(scroll.collect(h.belongings.backpack));
                room.ownership.claim(scroll,room.player(seat));
                int index = 0;
                for (Item owned : h.belongings) {
                    index++;
                    if (owned == h.belongings.weapon()) break;
                }
                int initial = h.belongings.weapon().trueLevel();
                assertTrue(CoopFloorsTest.step(room,CoopSession.Action.UPGRADE,index,scroll));
                assertEquals(initial+1,h.belongings.weapon().trueLevel());
                assertEquals(Integer.toString(2*(1+points)),h.buff(ScrollEmpower.class).iconTextDisplay());
                assertNull(room.hero(1-seat).buff(ScrollEmpower.class));
            } else {
                int grass = h.pos-1;
                Level.set(grass,Terrain.GRASS);
                assertTrue(CoopFloorsTest.step(room,CoopSession.Action.MOVE,grass,null));
                assertNotNull(h.buff(Talent.RejuvenatingStepsCooldown.class));
                assertNull(room.hero(1-seat).buff(Talent.RejuvenatingStepsCooldown.class));
                // The acting huntress immediately tramples the new grass.
                assertEquals(Terrain.FURROWED_GRASS,Dungeon.level.map[grass]);
            }
            saved = room.checkpoint();
        }
        try (CoopSession room = CoopSession.restore(saved)) {
            assertEquals(points,room.hero(seat).talentPointsSpent(2));
            assertEquals(0,room.hero(1-seat).talentPointsSpent(2));
        }
    }

    private static void thrownDurability(int seat, int rank) {
        try (CoopSession room = CoopSession.open(571,11,HeroClass.HUNTRESS)) {
            room.join(22,HeroClass.HUNTRESS);
            Rat rat = CoopMageTest.encounter(room,seat);
            Hero h = room.hero(seat);
            SewerBeeTest.offer(room,seat);
            h.lvl = 12;
            for (int i=0;i<rank;i++) assertTrue(CoopFloorsTest.step(room,CoopSession.Action.INVEST_TALENT,Talent.DURABLE_PROJECTILES.ordinal(),null));
            ThrowingKnife knives = new ThrowingKnife();
            knives.quantity(8);
            assertTrue(knives.collect(h.belongings.backpack));
            room.ownership.claim(knives,room.player(seat));
            int initialHealth = rat.HP;
            for (int n=0;n<6 && rat.HP==initialHealth;n++) {
                SewerBeeTest.offer(room,seat);
                assertTrue(CoopFloorsTest.step(room,CoopSession.Action.THROW,rat.pos,knives));
            }
            assertTrue(rat.HP<initialHealth);
            float uses = Math.round(5f*(rank==0 ? 1f : 1.25f+.25f*rank));
            float expected = 100f - (100f / uses + .001f);
            assertEquals(expected,knives.durabilityLeft(),.01f);
            assertEquals(0,room.hero(1-seat).pointsInTalent(Talent.DURABLE_PROJECTILES));
            room.checkpoint();
        }
    }

    private static void lethalMomentum(int seat, int rank) {
        try (CoopSession room=CoopSession.open(571,11,HeroClass.WARRIOR)) {
            room.join(22,HeroClass.WARRIOR);
            Rat rat=CoopMageTest.encounter(room,seat);
            CavesBossTest.beside(room,seat,rat.pos);
            Hero h=room.hero(seat);
            h.lvl=12;
            SewerBeeTest.offer(room,seat);
            for(int i=0;i<rank;i++) assertTrue(CoopFloorsTest.step(room,CoopSession.Action.INVEST_TALENT,Talent.LETHAL_MOMENTUM.ordinal(),null));
            rat.HP=1;
            h.belongings.weapon().upgrade(12);
            for(int attempt=0;attempt<8;attempt++) {
                SewerBeeTest.offer(room,seat);
                float before=h.cooldown()+Actor.now();
                assertTrue(CoopFloorsTest.step(room,CoopSession.Action.ATTACK,rat.pos,null));
                if(!rat.isAlive()) {
                    float spent=h.cooldown()+Actor.now()-before;
                    if(rank==2) assertEquals(0f,spent,.001f);
                    if(rank==0) assertTrue(spent>0f);
                    assertEquals(0,room.hero(1-seat).pointsInTalent(Talent.LETHAL_MOMENTUM));
                    System.out.println("KILL seat="+seat+" rank="+rank+" nativeTime="+spent);
                    return;
                }
            }
            throw new AssertionError("No native kill");
        }
    }

    private static void cleaveExpiry(int seat) {
        Bundle save;
        float remaining;
        try(CoopSession room=CoopSession.open(571,11,HeroClass.WARRIOR)) {
            room.join(22,HeroClass.WARRIOR);
            Rat rat=CoopMageTest.encounter(room,seat);
            Combo combo=SubclassActionTest.prepare(room,seat,rat,9);
            Hero h=room.hero(seat);
            h.lvl=25;
            assertTrue(CoopFloorsTest.step(room,CoopSession.Action.INVEST_TALENT,Talent.CLEAVE.ordinal(),null));
            rat.HP=1;
            h.belongings.weapon().upgrade(12);
            for(int n=0;n<8 && rat.isAlive();n++) {
                SewerBeeTest.offer(room,seat);
                assertTrue(CoopFloorsTest.step(room,CoopSession.Action.ATTACK,rat.pos,null));
            }
            assertFalse(rat.isAlive());
            remaining=combo.remainingTime();
            assertTrue(remaining>=29 && remaining<=30);
            save=room.checkpoint();
        }
        try(CoopSession room=CoopSession.restore(save)) {
            Hero h=room.hero(seat);
            assertEquals(remaining,h.buff(Combo.class).remainingTime(),0f);
            for(int n=0;n<90 && h.buff(Combo.class)!=null;n++)
                assertTrue(CoopFloorsTest.step(room,CoopSession.Action.WAIT,0,null));
            assertNull(h.buff(Combo.class));
        }
    }

    public static void main(String[] args) throws Exception {
        UpperGrowthEpoch test = new UpperGrowthEpoch();
        test.temporary.create();
        try {
            test.startBackend();
            if (args[0].equals("write")) write(Path.of(args[1]));
            else if (args[0].equals("read")) read(Path.of(args[1]));
            else if (args[0].equals("effects")) {
                for (int seat=0;seat<2;seat++) for (int rank=1;rank<=2;rank++) {
                    for (HeroClass type:new HeroClass[]{HeroClass.MAGE,HeroClass.HUNTRESS}) tierTwo(seat,rank,type);
                    thrownDurability(seat,rank);
                }
            } else if (args[0].equals("combat")) {
                for (int seat=0;seat<2;seat++) {
                    for(int rank=0;rank<=2;rank++) lethalMomentum(seat,rank);
                    cleaveExpiry(seat);
                }
            } else throw new IllegalArgumentException("write | read | effects | combat");
            System.out.println("PASS "+args[0]);
        } finally {
            test.stopBackend();
            test.temporary.delete();
        }
    }
}
