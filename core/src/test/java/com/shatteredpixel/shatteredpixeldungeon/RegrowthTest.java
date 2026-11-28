package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class RegrowthTest extends HeadlessDungeon {
    private static Bundle counters(WandOfRegrowth wand) {
        Bundle b = new Bundle();
        wand.storeInBundle(b);
        return b;
    }

    static StagedInput prepare(CoopSession game, int seat) {
        Rat target=CoopMageTest.encounter(game,seat);
        WandArtsTest.give(game,seat,new WandOfRegrowth());
        SewerBeeTest.offer(game,seat);
        return new StagedInput(game,"ZAP",target.pos);
    }

    static boolean published(CoopSession game, int seat) {
        Hero owner=game.hero(seat);
        WandOfRegrowth wand=owner.belongings.getItem(WandOfRegrowth.class);
        return wand!=null && game.ownership.owner(wand)==game.player(seat)
                && counters(wand).getInt("totChrgUsed")==1 && wand.curCharges==wand.maxCharges-1
                && Dungeon.level.mobs.stream().anyMatch(m->m instanceof Rat && m.buff(Roots.class)!=null);
    }

    @Test public void nativeSeedpodsAndDewcatchersSurviveSaveAndDropOnLanding() throws Exception {
        for (int seat=0;seat<2;seat++) for (boolean dew:new boolean[]{false,true}) {
            Bundle saved;
            int cell;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                CoopMageTest.encounter(game,seat).destroy();
                cell=game.hero(seat).pos+1;
                var planted=Dungeon.level.plant(dew ? new WandOfRegrowth.Dewcatcher.Seed() : new WandOfRegrowth.Seedpod.Seed(),cell);
                assertNotNull(planted);
                assertTrue(planted.supportsCoopLanding());
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                SewerBeeTest.offer(game,seat);
                assertNotNull(Dungeon.level.plants.get(cell));
                java.util.function.IntSupplier count=() -> {
                    int units=0;
                    for (int offset:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                        var heap=Dungeon.level.heaps.get(cell+offset);
                        if (heap!=null) for (var item:heap.items) {
                            if (dew ? item instanceof com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop
                                    : item instanceof com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed) units+=item.quantity();
                        }
                    }
                    return units;
                };
                int before=count.getAsInt();
                assertTrue(CoopFloorsTest.step(game,MOVE,cell,null));
                assertNull(Dungeon.level.plants.get(cell));
                int dropped=count.getAsInt()-before;
                assertTrue("original drop count "+dropped, dew ? dropped>=3 && dropped<=6 : dropped>=2 && dropped<=4);
            }
        }
    }

    @Test public void eachOwnersNativeChargeLimitAndLotusSurviveRestore() throws Exception {
        for (int seat = 0; seat < 2; seat++) {
            Bundle saved;
            long physical;
            int lotusId;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
                game.join(22, HeroClass.MAGE);
                Rat rat = CoopMageTest.encounter(game, seat);
                Hero hero = game.hero(seat), peer = game.hero(1-seat);
                hero.lvl = 4 + seat * 9;
                WandOfRegrowth original = new WandOfRegrowth();
                original.level(5);
                // A physical wand with two charges remaining before the owner's native degradation limit.
                Bundle nearLimit = counters(original);
                int limit = 20 + 7 * hero.lvl;
                nearLimit.put("totChrgUsed", limit - 2);
                WandOfRegrowth wand = new WandOfRegrowth();
                wand.restoreFromBundle(nearLimit);
                assertEquals(5,wand.level());
                WandArtsTest.give(game,seat,wand);
                wand.curCharges = 8;
                physical = game.ownership.identity(wand);
                SewerBeeTest.offer(game, seat);
                assertFalse(CoopFloorsTest.step(game, ZAP, peer.pos, wand));
                assertEquals(8, wand.curCharges);
                int target = hero.pos+2;
                assertTrue(CoopFloorsTest.step(game, ZAP, target, wand));
                assertEquals(5, wand.curCharges);
                assertEquals(limit, counters(wand).getInt("totChrgUsed"));
                assertEquals(1, counters(wand).getInt("chargesOverLimit"));
                assertNotNull(rat.buff(Roots.class));
                assertTrue(Actor.findChar(target) instanceof WandOfRegrowth.Lotus);
                WandOfRegrowth.Lotus lotus = (WandOfRegrowth.Lotus)Actor.findChar(target);
                assertTrue(lotus.belongsTo(hero));
                assertFalse(lotus.belongsTo(peer));
                lotusId = lotus.id();
                saved = game.checkpoint();
            }
            try (CoopSession game = CoopSession.restore(saved)) {
                WandOfRegrowth wand = game.hero(seat).belongings.getItem(WandOfRegrowth.class);
                assertEquals(physical, game.ownership.identity(wand));
                assertEquals(1, counters(wand).getInt("chargesOverLimit"));
                assertTrue(((WandOfRegrowth.Lotus)Actor.findById(lotusId)).belongsTo(game.hero(seat)));
                assertTrue(CoopFloorsTest.step(game, WAIT, 0, null));
            }
        }
    }

    @Test public void imbuedStaffMeleeAndBothArmorCastsUseTheActingMage() {
        for (int seat = 0; seat < 2; seat++) for (int mode = 0; mode < 3; mode++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
                game.join(22, HeroClass.MAGE);
                Rat rat = CoopMageTest.encounter(game, seat);
                Hero caster = game.hero(seat);
                WandOfRegrowth wand = WandArtsTest.give(game, seat, new WandOfRegrowth());
                MagesStaff staff = (MagesStaff)caster.belongings.weapon();
                Buff.prolong(rat, Paralysis.class, 100);
                if (mode != 1) {
                    TierGrowthTest.prepare(game,seat,HeroSubClass.BATTLEMAGE,Talent.WAND_PRESERVATION,2);
                    SewerBeeTest.offer(game,seat);
                    assertTrue(CoopFloorsTest.step(game, IMBUE, WandArtsTest.choice(game,seat,wand),staff));
                    assertSame(wand, staff.imbuedWand());
                }
                if (mode == 2) {
                    caster.HP /= 2;
                    rat.pos = caster.pos + 1;
                    Level.set(caster.pos, Terrain.GRASS);
                    game.observe(seat);
                    Dungeon.level.updateFieldOfView(caster,caster.fieldOfView);
                    int before = caster.HP;
                    for (int i=0; i<12 && caster.buff(Sungrass.Health.class)==null && caster.HP<=before; i++) {
                        SewerBeeTest.offer(game,seat);
                        assertTrue(CoopFloorsTest.step(game, ATTACK, rat.pos,null));
                    }
                    assertTrue(caster.buff(Sungrass.Health.class)!=null || caster.HP>before);
                    assertNull(game.hero(1-seat).buff(Sungrass.Health.class));
                } else {
                    ClassArmor armor = ArmorTalentsTest.invest(game,seat,mode,
                            mode==0 ? Talent.ELEMENTAL_POWER : Talent.WILD_POWER,1);
                    SewerBeeTest.offer(game,seat);
                    float energy = armor.charge;
                    float charges = wand.curCharges + wand.partialCharge;
                    assertTrue(CoopFloorsTest.step(game,ARMOR_ABILITY,mode==0?caster.pos:rat.pos,armor));
                    assertTrue(armor.charge < energy);
                    assertNotNull(rat.buff(Roots.class));
                    if (mode==1) {
                        assertTrue(wand.curCharges + wand.partialCharge < charges);
                        assertTrue(counters(wand).getInt("totChrgUsed")>0);
                    }
                }
                assertNotNull(game.checkpoint());
            }
        }
    }
}
