package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GrippingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.OozeTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.shatteredpixel.shatteredpixeldungeon.lan.SeatQuickslots;
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

    private static void fullInventory(int seat, boolean overCapacity) {
        Bundle checkpoint;
        long retained, staffId;
        try (CoopSession g=CoopSession.open(572,11,HeroClass.MAGE)) {
            g.join(22,HeroClass.MAGE);
            CoopMageTest.encounter(g,seat);
            SewerBeeTest.offer(g,seat);
            Hero owner=g.hero(seat);
            owner.lvl=9;
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,Talent.WAND_PRESERVATION.ordinal(),null));
            MagesStaff staff=(MagesStaff)owner.belongings.weapon();
            staffId=g.ownership.identity(staff);
            Wand old=staff.imbuedWand();
            retained=g.ownership.identity(old);
            Wand next=WandArtsTest.give(g,seat,new WandOfFireblast());
            com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag bag=owner.belongings.backpack;
            while (bag.items.size()<bag.capacity()) {
                com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor armor=new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor();
                assertTrue(armor.collect(bag));
                g.ownership.claim(armor,g.player(seat));
            }
            if (overCapacity) {
                // Only a staged, previously over-capacity bag can still be full after the incoming wand leaves.
                com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor extra=new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor();
                bag.items.add(extra);
                g.ownership.claim(extra,g.player(seat));
            }
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.IMBUE,WandArtsTest.choice(g,seat,next),staff));
            assertEquals(!overCapacity,bag.contains(old));
            if (overCapacity) assertTrue(Dungeon.level.heaps.get(owner.pos).items.contains(old));
            checkpoint=g.checkpoint();
        }
        try (CoopSession g=CoopSession.restore(checkpoint)) {
            Hero owner=g.hero(seat);
            assertEquals(staffId,g.ownership.identity(owner.belongings.weapon()));
            Wand old=overCapacity ? (Wand)Dungeon.level.heaps.get(owner.pos).peek() : owner.belongings.getItem(WandOfMagicMissile.class);
            assertEquals(retained,g.ownership.identity(old));
            assertEquals(g.player(seat),g.ownership.owner(old));
            if (overCapacity) {
                assertFalse(g.ownership.allowed(old,g.player(1-seat)));
                com.shatteredpixel.shatteredpixeldungeon.items.Item armor=owner.belongings.backpack.items.stream()
                        .filter(i->i instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor).findFirst().get();
                // The fixture clears one slot; recovery itself uses the native pickup action.
                armor.detachAll(owner.belongings.backpack);
                SewerBeeTest.offer(g,seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,owner.pos,old));
                assertTrue(owner.belongings.backpack.contains(old));
                assertEquals(retained,g.ownership.identity(old));
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

    private static void blindLunge(int seat) {
        try (CoopSession game=CoopSession.open(571,11,HeroClass.DUELIST)) {
            game.join(22,HeroClass.DUELIST);
            Rat rat=CoopMageTest.encounter(game,seat);
            SewerBeeTest.offer(game,seat);
            Hero hero=game.hero(seat);
            int target=hero.pos+2;
            int start=hero.pos;
            assertNull(Actor.findChar(target));
            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(hero,com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class,5f);
            game.observe(seat);
            PublicDungeon image=ProjectionChecks.seatImage(game,seat);
            assertFalse(image.visible(target));
            assertFalse(image.monsters.containsKey(rat.pos));
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WEAPON_ABILITY,target,hero.belongings.weapon()));
            assertTrue(start!=hero.pos);
            assertEquals(1,hero.buff(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger.class).charges);
            assertNull(hero.belongings.abilityWeapon);
            assertEquals(rat.HT,rat.HP);
            assertFalse(ProjectionChecks.seatImage(game,seat).monsters.containsKey(rat.pos));
            game.checkpoint();
        }
    }

    private static void fallenHawkOwner(int seat, boolean hasAnkh) {
        Bundle saved;
        int actor;
        try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
            game.join(22,HeroClass.HUNTRESS);
            Rat target=CoopMageTest.encounter(game,seat);
            SewerBeeTest.offer(game,seat);
            WandArtsTest.stage(game,"hawk",seat,target);
            Hero hero=game.hero(seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,hero.pos,hero.belongings.armor()));
            actor=SpiritHawk.getHawk(hero).id();
            SewerBeeTest.offer(game,1-seat);
            if (hasAnkh) {
                com.shatteredpixel.shatteredpixeldungeon.items.Ankh ankh=new com.shatteredpixel.shatteredpixeldungeon.items.Ankh();
                assertTrue(ankh.collect(hero.belongings.backpack));
                game.ownership.claim(ankh,game.player(seat));
            }
            hero.damage(999,Rat.class);
            assertEquals(hasAnkh?CoopSession.Life.DOWNED:CoopSession.Life.DEAD,game.life(seat));
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            SpiritHawk.HawkAlly hawk=SpiritHawk.getHawk(game.hero(seat));
            assertEquals(actor,hawk.id());
            assertNull(SpiritHawk.getHawk(game.hero(1-seat)));
            TenguArenaTest.waitOne(game);
            if (hasAnkh) hawk.damage(999,Rat.class);
            else for(int i=0;i<160 && SpiritHawk.getHawk(game.hero(seat))!=null;i++) TenguArenaTest.waitOne(game);
            assertNull(SpiritHawk.getHawk(game.hero(seat)));
            saved=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(saved)) {
            assertNull(Actor.findById(actor));
            assertNull(SpiritHawk.getHawk(game.hero(seat)));
            assertNull(SpiritHawk.getHawk(game.hero(1-seat)));
        }
    }

    private static void nativeVolleyLimit(int seat) {
        try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            Rat target=CoopMageTest.encounter(game,seat);
            SewerBeeTest.offer(game,seat);
            StagedInput selection=WandArtsTest.stage(game,"volley",seat,target);
            Hero hero=game.hero(seat);
            Wand first=hero.belongings.getItem(WandOfMagicMissile.class);
            first.curCharges=0;
            first.partialCharge=0.25f;
            float armor=((com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)hero.belongings.armor()).charge;
            long[] rng=Random.worldState();
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,selection.cell,hero.belongings.armor()));
            assertArrayEquals(rng,Random.worldState());
            assertEquals(armor,((com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)hero.belongings.armor()).charge,0f);
            for(int rank=0;rank<4;rank++) {
                hero.upgradeTalent(Talent.FIRE_EVERYTHING);
                hero.upgradeTalent(Talent.CONSERVED_MAGIC);
            }
            assertEquals(4,hero.pointsInTalent(Talent.FIRE_EVERYTHING));
            assertEquals(4,hero.pointsInTalent(Talent.CONSERVED_MAGIC));
            Wand second=WandArtsTest.give(game,seat,new WandOfMagicMissile());
            Wand third=WandArtsTest.give(game,seat,new WandOfMagicMissile());
            Wand[] own={first,second,third};
            for(Wand wand:own) {
                wand.curCharges=1;
                wand.partialCharge=0.25f;
            }
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,selection.cell,hero.belongings.armor()));
            float remaining=0;
            for(Wand wand:own) remaining+=wand.curCharges+wand.partialCharge;
            float cost=(float)(0.5*Math.pow(0.67,4));
            assertEquals(8*cost,3.75f-remaining,0.0001f);
            assertTrue(target.HP<target.HT);
            game.checkpoint();
        }
    }

    private static void twoPreservationCounters() {
        Bundle checkpoint;
        SeatQuickslots[] layouts=new SeatQuickslots[2];
        String[] references=new String[2];
        java.util.UUID sessionId=java.util.UUID.randomUUID();
        java.util.UUID[] salts={java.util.UUID.randomUUID(),java.util.UUID.randomUUID()};
        try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
            game.join(22,HeroClass.MAGE);
            CoopMageTest.encounter(game,0);
            for (int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                hero.lvl=9;
                SewerBeeTest.offer(game,seat);
                for (int i=0;i<=seat;i++) assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.WAND_PRESERVATION.ordinal(),null));
                MagesStaff staff=(MagesStaff)hero.belongings.weapon();
                Wand wand=WandArtsTest.give(game,seat,seat==0 ? new WandOfFireblast() : new WandOfBlastWave());
                PublicDungeon image=ProjectionChecks.seatImage(game,seat,sessionId,salts[seat]);
                layouts[seat]=SeatQuickslots.defaults(image);
                references[seat]=layouts[seat].item(0,image).reference;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.IMBUE,WandArtsTest.choice(game,seat,wand),staff));
                assertNotNull(hero.buff(Talent.WandPreservationCounter.class));
            }
            checkpoint=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(checkpoint)) {
            for (int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                assertNotNull(hero.buff(Talent.WandPreservationCounter.class));
                assertEquals(references[seat],layouts[seat].item(0,ProjectionChecks.seatImage(game,seat,sessionId,salts[seat])).reference);
            }
            Hero guest=game.hero(1);
            guest.earnExp(guest.maxExp(),Rat.class);
            assertNull(guest.buff(Talent.WandPreservationCounter.class));
            assertNotNull(game.hero(0).buff(Talent.WandPreservationCounter.class));
        }
    }

    private static void enhancedLanding(int seat, Trap trap) {
        try (CoopSession game=CoopSession.open(571,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            SubclassActionTest.stage(game,"enhanced"+seat,seat);
            Rat rat=(Rat)Dungeon.level.mobs.iterator().next();
            Hero hero=game.hero(seat);
            int landing=rat.pos-1;
            Dungeon.level.setTrap(trap,landing);
            Level.set(landing,Terrain.SECRET_TRAP);
            trap.hide();
            game.observe(seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLOBBER,rat.pos,null));
            assertEquals(landing,hero.pos);
            assertEquals(trap.disarmedByActivation ? Terrain.INACTIVE_TRAP : Terrain.TRAP,Dungeon.level.map[landing]);
            if (trap instanceof GrippingTrap) assertNotNull(hero.buff(Cripple.class));
            else assertNotNull(hero.buff(Ooze.class));
            game.checkpoint();
        }
    }

    private static void parrySeparateAttackers(int seat, boolean lethal) {
        try (CoopSession game=CoopSession.open(571,11,HeroClass.WARRIOR)) {
            game.join(22,HeroClass.WARRIOR);
            SubclassActionTest.stage(game,"parryready"+seat,seat);
            Hero hero=game.hero(seat);
            Rat first=(Rat)Dungeon.level.mobs.iterator().next();
            game.hero(1-seat).pos=first.pos+Dungeon.level.width()+1;
            if (lethal) first.HP=1;
            first.timeToNow();
            first.aggro(hero);
            first.state=first.HUNTING;
            Rat second=new Rat();
            second.pos=hero.pos-1;
            second.HT=second.HP=200;
            second.aggro(hero);
            second.state=second.HUNTING;
            GameScene.add(second,0);
            int hp=hero.HP;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PARRY,hero.pos,null));
            for (int n=0;n<3 && (first.HP==(lethal?1:first.HT) || second.HP==second.HT);n++)
                TenguArenaTest.waitOne(game);
            if (lethal) assertFalse(first.isAlive());
            else assertTrue("first attacker "+first.HP,first.HP<first.HT);
            assertTrue("second attacker "+second.HP,second.HP<second.HT);
            assertEquals(hp,hero.HP);
            assertNull(game.hero(1-seat).buff(Combo.class));
            game.checkpoint();
        }
    }

    public static void main(String[] args) throws Exception {
        WandArtsBoundaries probe=new WandArtsBoundaries();
        probe.temporary.create();
        probe.startBackend();
        try {
            twoPreservationCounters();
            System.out.println("PASS two independent preservation counters and restored staff shortcuts");
            for (int seat=0;seat<2;seat++) {
                blindLunge(seat);
                System.out.println("PASS unseen empty Lunge spends only the caster charge without revealing a distant enemy seat "+seat);
                parrySeparateAttackers(seat,false);
                parrySeparateAttackers(seat,true);
                System.out.println("PASS scheduled distinct attackers and two ripostes seat "+seat);
                enhancedLanding(seat,new GrippingTrap());
                enhancedLanding(seat,new OozeTrap());
                System.out.println("PASS enhanced leap native gripping and ooze traps seat "+seat);
                replaceTwice(seat);
                System.out.println("PASS repeated imbue and unknown level seat "+seat);
                fullInventory(seat,false);
                fullInventory(seat,true);
                System.out.println("PASS full inventory and staged overflow, restore and owner pickup seat "+seat);
                nativeVolleyLimit(seat);
                System.out.println("PASS staged rank-four native eight-shot charge cost and insufficient-charge rejection seat "+seat);
                replayVolley(seat);
                System.out.println("PASS restored heterogeneous four-shot volley seat "+seat);
                fallenHawkOwner(seat,true);
                fallenHawkOwner(seat,false);
                System.out.println("PASS hawk death and expiry after downed and dead owner seat "+seat);
                travelWithHawk(seat);
                System.out.println("PASS hawk stairs, restart and return seat "+seat);
            }
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }
}
