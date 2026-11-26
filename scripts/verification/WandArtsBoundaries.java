package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import static org.junit.Assert.*;

/** Additional native state comparisons in the existing engine fixture. */
public final class WandArtsBoundaries extends HeadlessDungeon {
    private static void travelWithHawk(int seat) {
        Bundle away;
        int id;
        try (CoopSession g=CoopSession.open(402,11,HeroClass.HUNTRESS)) {
            g.join(22,HeroClass.HUNTRESS);
            Rat enemy=CoopMageTest.encounter(g,seat);
            SewerBeeTest.offer(g,seat);
            WandArtsTest.stage(g,"hawk",seat,enemy);
            Hero owner=g.hero(seat);
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ARMOR_ABILITY,owner.pos,owner.belongings.armor()));
            id=SpiritHawk.getHawk(owner).id();
            SewerSpecialActorTest.atGate(g,Dungeon.level.exit());
            CoopFloorsTest.cross(g);
            assertEquals(2,Dungeon.depth);
            assertEquals(id,SpiritHawk.getHawk(owner).id());
            assertNull(SpiritHawk.getHawk(g.hero(1-seat)));
            away=g.checkpoint();
        }
        try (CoopSession g=CoopSession.restore(away)) {
            assertSame(Actor.findById(id),SpiritHawk.getHawk(g.hero(seat)));
            SewerSpecialActorTest.atGate(g,Dungeon.level.entrance());
            CoopFloorsTest.cross(g);
            assertEquals(1,Dungeon.depth);
            assertSame(Actor.findById(id),SpiritHawk.getHawk(g.hero(seat)));
            assertNull(SpiritHawk.getHawk(g.hero(1-seat)));
        }
    }

    private static void replaceTwice(int seat) {
        try (CoopSession g=CoopSession.open(572,11,HeroClass.MAGE)) {
            g.join(22,HeroClass.MAGE);
            SewerBeeTest.offer(g,seat);
            Hero owner=g.hero(seat);
            owner.lvl=9;
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,Talent.WAND_PRESERVATION.ordinal(),null));
            MagesStaff staff=(MagesStaff)owner.belongings.weapon();
            staff.upgrade(3);
            Wand preserved=staff.imbuedWand();
            for (int n=0;n<2;n++) {
                Wand incoming=WandArtsTest.give(g,seat,n==0 ? new WandOfFireblast() : new WandOfBlastWave());
                incoming.upgrade(n);
                incoming.levelKnown=false;
                PublicDungeon view=ProjectionChecks.seatImage(g,seat);
                int index=WandArtsTest.choice(g,seat,incoming);
                long handle=view.belongings.stream().filter(i->Integer.toString(index).equals(view.source.rows.get("wandchoice"+i.id))).findFirst().get().id;
                assertFalse(view.source.rows.containsKey("imbuelevel"+handle));
                incoming.cursedKnown=false;
                long[] rng=Random.worldState();
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.IMBUE,index,staff));
                assertArrayEquals(rng,Random.worldState());
                incoming.cursedKnown=true;
                Wand old=staff.imbuedWand();
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.IMBUE,index,staff));
                assertSame(incoming,staff.imbuedWand());
                assertEquals(3,staff.trueLevel());
                assertEquals(n==0,owner.belongings.backpack.contains(old));
                assertTrue(owner.belongings.backpack.contains(preserved));
            }
        }
    }

    private static void replayVolley(int seat) {
        Bundle staged;
        int cell;
        try (CoopSession g=CoopSession.open(572,11,HeroClass.MAGE)) {
            g.join(22,HeroClass.MAGE);
            Rat target=CoopMageTest.encounter(g,seat);
            SewerBeeTest.offer(g,seat);
            cell=WandArtsTest.stage(g,"volley",seat,target).cell;
            Wand fire=WandArtsTest.give(g,seat,new WandOfFireblast());
            fire.upgrade(2);
            fire.curCharges=2;
            fire.partialCharge=.375f;
            WandArtsTest.give(g,1-seat,new WandOfMagicMissile());
            staged=g.checkpoint();
        }
        String reference=null;
        for (int i=0;i<2;i++) {
            try (CoopSession g=CoopSession.restore(staged)) {
                Hero h=g.hero(seat);
                Wand fire=h.belongings.getItem(WandOfFireblast.class);
                Wand missile=h.belongings.getItem(WandOfMagicMissile.class);
                int partner=g.hero(1-seat).belongings.getItem(WandOfMagicMissile.class).curCharges;
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ARMOR_ABILITY,cell,h.belongings.armor()));
                assertEquals(1,fire.curCharges);
                assertEquals(1,missile.curCharges);
                assertEquals(partner,g.hero(1-seat).belongings.getItem(WandOfMagicMissile.class).curCharges);
                String actual=SubclassActionTest.signature(g)+WandArtsTest.artsSignature(g);
                if (reference==null) reference=actual;
                else assertEquals(reference,actual);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        WandArtsBoundaries probe=new WandArtsBoundaries();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (int seat=0;seat<2;seat++) {
                replaceTwice(seat);
                System.out.println("PASS repeated imbue and unknown level seat "+seat);
                replayVolley(seat);
                System.out.println("PASS restored heterogeneous four-shot volley seat "+seat);
                travelWithHawk(seat);
                System.out.println("PASS hawk stairs, restart and return seat "+seat);
            }
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }
}
