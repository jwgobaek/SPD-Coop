package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class WandArtsTest extends HeadlessDungeon {
    static <T extends Wand> T give(CoopSession game, int seat, T wand) {
        wand.identify();
        assertTrue(wand.collect(game.hero(seat).belongings.backpack));
        game.ownership.claim(wand,game.player(seat));
        return wand;
    }

    static int choice(CoopSession game, int seat, Wand wand) {
        int index = 0;
        for (Item item : game.hero(seat).belongings) {
            index++;
            if (item == wand) return index;
        }
        throw new AssertionError("Missing wand");
    }

    @Test public void imbueUsesOneOwnersWandAndRetainsItsIdentityAtDepthOne() {
        for (int seat=0; seat<2; seat++) for (int rank=0; rank<=2; rank++) {
            Bundle state;
            long staffId, incomingId, oldId;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                hero.lvl=9;
                for (int i=0;i<rank;i++) assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.WAND_PRESERVATION.ordinal(),null));
                MagesStaff staff=(MagesStaff)hero.belongings.weapon();
                staff.upgrade();
                Wand old=staff.imbuedWand();
                staffId=game.ownership.identity(staff);
                oldId=game.ownership.identity(old);
                WandOfBlastWave incoming=give(game,seat,new WandOfBlastWave());
                incoming.upgrade(2);
                incoming.curCharges=1;
                incoming.partialCharge=.375f;
                incomingId=game.ownership.identity(incoming);
                int selection=choice(game,seat,incoming);
                long turn=game.turn();
                long[] rng=Random.worldState();
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.IMBUE,selection,game.hero(1-seat).belongings.weapon()));
                assertArrayEquals(rng,Random.worldState());
                PublicDungeon view=ProjectionChecks.seatImage(game,seat);
                assertTrue(view.belongings.stream().anyMatch(it->view.actions(it).contains("IMBUE")));
                assertTrue(view.source.rows.containsValue(Integer.toString(selection)));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.IMBUE,selection,staff));
                assertEquals(turn,game.turn());
                assertSame(incoming,staff.imbuedWand());
                assertFalse(hero.belongings.backpack.contains(incoming));
                assertEquals(rank>0,hero.belongings.backpack.contains(old));
                assertEquals(3,staff.trueLevel());
                assertSame(hero,incoming.chargingHero());
                assertEquals(.375f,incoming.partialCharge,0f);
                assertNull(game.hero(1-seat).buff(Talent.WandPreservationCounter.class));
                state=game.checkpoint();
                assertEquals(19,state.getInt("schema"));
            }
            try (CoopSession game=CoopSession.restore(state)) {
                Hero hero=game.hero(seat);
                MagesStaff staff=(MagesStaff)hero.belongings.weapon();
                assertEquals(staffId,game.ownership.identity(staff));
                assertEquals(incomingId,game.ownership.identity(staff.imbuedWand()));
                assertEquals(game.player(seat),game.ownership.owner(staff.imbuedWand()));
                assertSame(hero,staff.coopWand().chargingHero());
                assertEquals(.375f,staff.imbuedWand().partialCharge,0f);
                Wand returned=hero.belongings.getItem(WandOfMagicMissile.class);
                assertEquals(rank>0,returned!=null);
                if (returned!=null) {
                    assertEquals(oldId,game.ownership.identity(returned));
                    assertEquals(0,returned.trueLevel());
                    assertEquals(game.player(seat),game.ownership.owner(returned));
                }
                hero.earnExp(hero.maxExp(),Rat.class);
                assertEquals(rank==1,hero.buff(Talent.WandPreservationCounter.class)!=null);
            }
        }
    }

    static StagedInput stage(CoopSession g, String type, int seat, Rat rat) {
        Hero hero=g.hero(seat);
        if (type.equals("smoke")) {
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,0,CrownAbilityTest.crown(g,seat,CoopSession.MaskTrial.OWNER)));
            SewerBeeTest.offer(g,seat);
            rat.pos=hero.pos+1;
            g.observe(seat);
            return new StagedInput(g,"ARMOR_ABILITY",hero.pos-2);
        }
        if (type.equals("ambush") || type.equals("freerun")) {
            com.shatteredpixel.shatteredpixeldungeon.items.TengusMask mask=new com.shatteredpixel.shatteredpixeldungeon.items.TengusMask();
            assertTrue(mask.collect(hero.belongings.backpack));
            g.ownership.claim(mask,g.player(seat));
            g.beginMaskTrial(CoopSession.MaskTrial.OWNER);
            boolean ambush=type.equals("ambush");
            HeroSubClass way=ambush ? HeroSubClass.ASSASSIN : HeroSubClass.FREERUNNER;
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_MASK,way.ordinal(),mask));
            SewerBeeTest.offer(g,seat);
            if (ambush) {
                rat.pos=hero.pos+2;
                g.observe(seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.CLOAK,0,hero.belongings.artifact));
                SewerBeeTest.offer(g,seat);
                return new StagedInput(g,"ASSASSINATE",rat.pos);
            }
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,hero.pos-1,null));
            SewerBeeTest.offer(g,seat);
            return new StagedInput(g,"FREERUN",hero.pos);
        }
        if (type.equals("lunge")) {
            rat.pos=hero.pos+2;
            g.observe(seat);
            return new StagedInput(g,"WEAPON_ABILITY",rat.pos);
        }
        if (type.equals("charged")) {
            Crossbow bow=new Crossbow();
            bow.identify();
            assertTrue(bow.collect(hero.belongings.backpack));
            g.ownership.claim(bow,g.player(seat));
            hero.STR=20;
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.EQUIP,0,bow));
            SewerBeeTest.offer(g,seat);
            return new StagedInput(g,"WEAPON_ABILITY",hero.pos);
        }
        if (type.equals("spike")) return new StagedInput(g,"THROW",rat.pos);
        if (type.equals("light")) return new StagedInput(g,"GUIDING_LIGHT",rat.pos);
        if (type.equals("blessweapon")) return new StagedInput(g,"HOLY_WEAPON",hero.pos);
        if (type.equals("ward")) return new StagedInput(g,"HOLY_WARD",hero.pos);
        if (type.equals("hawk") || type.equals("direct")) {
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,2,CrownAbilityTest.crown(g,seat,CoopSession.MaskTrial.OWNER)));
            SewerBeeTest.offer(g,seat);
            if (type.equals("direct")) {
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ARMOR_ABILITY,hero.pos,hero.belongings.armor()));
                SewerBeeTest.offer(g,seat);
            }
            return new StagedInput(g,"ARMOR_ABILITY",type.equals("hawk") ? hero.pos : rat.pos);
        }
        if (type.equals("imbue")) {
            hero.lvl=9;
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,Talent.WAND_PRESERVATION.ordinal(),null));
            Wand wand=give(g,seat,new WandOfBlastWave());
            return new StagedInput(g,"IMBUE",choice(g,seat,wand));
        }
        assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,1,CrownAbilityTest.crown(g,seat,CoopSession.MaskTrial.OWNER)));
        SewerBeeTest.offer(g,seat);
        give(g,seat,new WandOfMagicMissile()).curCharges=2;
        rat.HT=rat.HP=120;
        return new StagedInput(g,"ARMOR_ABILITY",rat.pos);
    }

    static boolean published(CoopSession game, String type, int seat) {
        Hero hero=game.hero(seat);
        if (type.equals("smoke")) return ((ClassArmor)hero.belongings.armor()).charge<1 && Dungeon.level.mobs.stream()
                .anyMatch(m->m instanceof Rat && m.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness.class)!=null && Dungeon.level.distance(m.pos,hero.pos)>1);
        if (type.equals("freerun")) return hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum.class).freerunning();
        if (type.equals("ambush")) return hero.invisible==0 && Dungeon.level.mobs.stream().anyMatch(m->m instanceof Rat && m.HP<m.HT);
        if (type.equals("lunge") || type.equals("charged")) {
            return hero.buff(MeleeWeapon.Charger.class).charges < 2 && (type.equals("charged") ?
                    hero.buff(Crossbow.ChargedShot.class)!=null :
                    Dungeon.level.mobs.stream().anyMatch(m->m instanceof Rat && m.HP<m.HT && Dungeon.level.adjacent(hero.pos,m.pos)));
        }
        if (type.equals("spike")) return hero.belongings.getItem(ThrowingSpike.class).quantity()==1;
        if (hero.heroClass==HeroClass.CLERIC) {
            HolyTome tome=hero.belongings.getItem(HolyTome.class);
            if (type.equals("light")) return tome.charges()==2 &&
                    Dungeon.level.mobs.stream().anyMatch(m->m instanceof Rat && m.buff(GuidingLight.Illuminated.class)!=null);
            if (type.equals("blessweapon")) return tome.charges()==1 && hero.buff(HolyWeapon.HolyWepBuff.class)!=null;
            if (type.equals("ward")) return tome.charges()==2 && hero.buff(HolyWard.HolyArmBuff.class)!=null;
        }
        if (type.equals("hawk") || type.equals("direct")) {
            SpiritHawk.HawkAlly hawk=SpiritHawk.getHawk(hero);
            return hawk!=null && hawk.belongsTo(hero) && SpiritHawk.getHawk(game.hero(1-seat))==null
                    && ((ClassArmor)hero.belongings.armor()).charge<16;
        }
        if (type.equals("imbue")) {
            return ((MagesStaff)hero.belongings.weapon()).imbuedWand() instanceof WandOfBlastWave
                    && hero.belongings.getItem(WandOfMagicMissile.class)!=null && hero.buff(Talent.WandPreservationCounter.class)!=null;
        }
        Wand wand=hero.belongings.getItem(WandOfMagicMissile.class);
        return wand!=null && wand.curCharges==1 && ((ClassArmor)hero.belongings.armor()).charge<26
                && Dungeon.level.mobs.stream().anyMatch(m->m instanceof Rat && m.HP<m.HT);
    }

    @Test public void twoSummonersResumeTheirOwnHawksAndCommandWithoutSpending() {
        Bundle state;
        int[] ids=new int[2];
        try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
            game.join(22,HeroClass.HUNTRESS);
            CoopMageTest.encounter(game,0);
            SewerBeeTest.offer(game,0);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,2,CrownAbilityTest.crown(game,0,CoopSession.MaskTrial.ROSTER)));
            SewerBeeTest.offer(game,1);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLAIM_ARMOR,2,null));
            for (int seat=0;seat<2;seat++) {
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,hero.pos,hero.belongings.armor()));
                ids[seat]=SpiritHawk.getHawk(hero).id();
            }
            assertNotEquals(ids[0],ids[1]);
            state=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(state)) {
            for (int seat=0;seat<2;seat++) {
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                assertEquals(ids[seat],SpiritHawk.getHawk(hero).id());
                ClassArmor armor=(ClassArmor)hero.belongings.armor();
                float charge=armor.charge;
                long turn=game.turn();
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,hero.pos,armor));
                assertEquals(turn,game.turn());
                assertEquals(charge,armor.charge,0f);
            }
        }
    }

    @Test public void startingAbilitiesUseTheWearersPoolAndSurviveRestoringTheRoom() {
        for (String type:new String[]{"lunge","spike","charged","light","blessweapon","ward"}) for (int seat=0;seat<2;seat++) {
            HeroClass role=type.equals("lunge") || type.equals("spike") || type.equals("charged") ? HeroClass.DUELIST : HeroClass.CLERIC;
            Bundle state;
            try (CoopSession game=CoopSession.open(572,11,role)) {
                game.join(22,role);
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                StagedInput input=stage(game,type,seat,rat);
                Hero hero=game.hero(seat);
                PublicDungeon view=ProjectionChecks.seatImage(game,seat);
                String verb=role==HeroClass.CLERIC?"CAST":type.equals("spike")?"THROW":"WEAPON_ABILITY";
                assertTrue(type,view.belongings.stream().anyMatch(it->view.actions(it).contains(verb)));
                Item source=role==HeroClass.CLERIC ? hero.belongings.artifact : type.equals("spike") ?
                        hero.belongings.getItem(ThrowingSpike.class) : hero.belongings.weapon();
                assertTrue(type,CoopFloorsTest.step(game,CoopSession.Action.valueOf(input.verb),input.cell,source));
                assertTrue(type,published(game,type,seat));
                state=game.checkpoint();
                assertEquals(19,state.getInt("schema"));
            }
            try (CoopSession game=CoopSession.restore(state)) {
                assertTrue(type,published(game,type,seat));
                Hero other=game.hero(1-seat);
                if (role==HeroClass.CLERIC) assertEquals(3,((HolyTome)other.belongings.artifact).charges());
                else assertEquals(2,other.buff(MeleeWeapon.Charger.class).charges);
            }
        }
    }

    @Test public void summonedHawkOnlyRevealsRemoteTerrainToItsSummoner() {
        for (int seat=0; seat<2; seat++) {
            try (CoopSession g=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                g.join(22,HeroClass.HUNTRESS);
                Rat rat=CoopMageTest.encounter(g,seat);
                SewerBeeTest.offer(g,seat);
                stage(g,"hawk",seat,rat);
                Hero summoner=g.hero(seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ARMOR_ABILITY,summoner.pos,summoner.belongings.armor()));
                int remote=-1;
                for (int cell=0;cell<Dungeon.level.length();cell++) {
                    if (Dungeon.level.passable[cell] && Dungeon.level.discoverable[cell]
                            && !g.knowledge(g.hero(seat)).visible(cell) && !g.knowledge(g.hero(1-seat)).visible(cell)) {
                        remote=cell;
                        break;
                    }
                }
                assertTrue(remote>=0);
                SpiritHawk.getHawk(summoner).pos=remote;
                assertTrue(g.observe(seat).visible(remote));
                assertFalse(g.observe(1-seat).visible(remote));
                assertTrue(ProjectionChecks.seatImage(g,seat).visible(remote));
                assertFalse(ProjectionChecks.seatImage(g,1-seat).visible(remote));
            }
        }
    }

    @Test public void twoImbuedStaffsApplyTheirNativeElementalBlasts() {
        for (int seat=0;seat<2;seat++) for (boolean fire : new boolean[]{true,false}) {
            try (CoopSession g=CoopSession.open(572,11,HeroClass.MAGE)) {
                g.join(22,HeroClass.MAGE);
                Rat rat=CoopMageTest.encounter(g,seat);
                rat.HP=rat.HT=160;
                Hero hero=g.hero(seat);
                SewerBeeTest.offer(g,seat);
                MagesStaff staff=(MagesStaff)hero.belongings.weapon();
                Wand incoming=give(g,seat,fire ? new WandOfFireblast() : new WandOfBlastWave());
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.IMBUE,choice(g,seat,incoming),staff));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,0,CrownAbilityTest.crown(g,seat,CoopSession.MaskTrial.OWNER)));
                SewerBeeTest.offer(g,seat);
                int origin=rat.pos;
                if (!fire) {
                    for (int i=1;i<=3;i++) com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(rat.pos+i,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);
                }
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ARMOR_ABILITY,hero.pos,hero.belongings.armor()));
                assertTrue(rat.HP<rat.HT);
                if (fire) assertTrue(Dungeon.level.blobs.get(Fire.class).volume>0);
                else assertNotEquals(origin,rat.pos);
                g.checkpoint();
            }
        }
    }

    @Test public void bothRogueSpecializationsUseTheirOwnNativeActionAndResume() {
        for (int seat=0;seat<2;seat++) for (String type : new String[]{"ambush","freerun","smoke"}) {
            Bundle state;
            try (CoopSession g=CoopSession.open(572,11,HeroClass.ROGUE)) {
                g.join(22,HeroClass.ROGUE);
                StagedInput action=SubclassActionTest.stage(g,type+seat,seat);
                if (!type.equals("smoke")) assertTrue(ProjectionChecks.seatImage(g,seat).source.rows.containsKey(type));
                assertFalse(ProjectionChecks.seatImage(g,1-seat).source.rows.containsKey(type));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.valueOf(action.verb),action.cell,type.equals("smoke")?g.hero(seat).belongings.armor():null));
                assertTrue(published(g,type,seat));
                state=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(state)) {
                assertTrue(published(g,type,seat));
                assertEquals(HeroSubClass.NONE,g.hero(1-seat).subClass);
            }
        }
    }

    static String artsSignature(CoopSession g) {
        StringBuilder out=new StringBuilder(java.util.Arrays.toString(Random.worldState()));
        for (int seat=0;seat<2;seat++) {
            Hero h=g.hero(seat);
            for (Item item : h.belongings) {
                if (item instanceof MagesStaff) item=((MagesStaff)item).imbuedWand();
                if (item instanceof Wand) {
                    Wand w=(Wand)item;
                    out.append('/').append(w.getClass().getSimpleName()).append(':').append(g.ownership.owner(w));
                    if (g.ownership.owner(w)!=0) out.append(':').append(g.ownership.identity(w));
                    out.append(':').append(w.curCharges).append(':').append(w.partialCharge);
                } else if (item instanceof HolyTome) out.append("/tome:").append(((HolyTome)item).charges());
            }
            MeleeWeapon.Charger charger=h.buff(MeleeWeapon.Charger.class);
            if (charger!=null) out.append("/weapon:").append(charger.charges).append(':').append(charger.partialCharge);
            SpiritHawk.HawkAlly hawk=SpiritHawk.getHawk(h);
            if (hawk!=null) {
                Bundle b=new Bundle();
                hawk.storeInBundle(b);
                out.append("/hawk:").append(hawk.id()).append(':').append(b.getInt("summoner"))
                        .append(':').append(b.getInt("defend_pos")).append(':').append(b.getFloat("time_remaining"));
            }
        }
        return out.toString();
    }

    @Test public void twoShotsSpendOneWandChargeAndOneArmorCost() {
        for (int seat=0;seat<2;seat++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                StagedInput input=stage(game,"volley",seat,rat);
                Wand other=give(game,1-seat,new WandOfMagicMissile());
                int otherCharges=other.curCharges;
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,input.cell,game.hero(seat).belongings.armor()));
                assertTrue(published(game,"volley",seat));
                assertEquals(otherCharges,other.curCharges);
                game.checkpoint();
            }
        }
    }
}
