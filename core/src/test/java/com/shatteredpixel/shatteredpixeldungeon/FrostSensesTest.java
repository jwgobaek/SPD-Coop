package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import org.junit.Test;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio;
import static org.junit.Assert.*;

public class FrostSensesTest extends HeadlessDungeon {
    static StagedInput stage(CoopSession game,String type,int seat,Rat target) {
        Hero hero=game.hero(seat);
        if (type.equals("swap") || type.equals("monkfocus")) {
            boolean champion = type.equals("swap");
            game.beginMaskTrial(CoopSession.MaskTrial.ROSTER);
            com.shatteredpixel.shatteredpixeldungeon.items.TengusMask mask = new com.shatteredpixel.shatteredpixeldungeon.items.TengusMask();
            assertTrue(mask.collect(hero.belongings.backpack));
            game.ownership.claim(mask,game.player(seat));
            HeroSubClass choice = champion ? HeroSubClass.CHAMPION : HeroSubClass.MONK;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_MASK,choice.ordinal(),mask));
            SewerBeeTest.offer(game,seat);
            if (champion) {
                Quarterstaff spare = new Quarterstaff();
                spare.identify();
                assertTrue(spare.collect(hero.belongings.backpack));
                game.ownership.claim(spare,game.player(seat));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP_SECOND,0,spare));
                SewerBeeTest.offer(game,seat);
                return new StagedInput(game,"SWAP_WEAPONS",hero.pos);
            }
            for (int i=0;i<2;i++) {
                Rat foe = new Rat();
                foe.pos = hero.pos+1;
                foe.HP = 1;
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(foe,2000);
                game.observe(seat);
                for (int n=0;n<20 && foe.isAlive();n++) {
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,foe.pos,null));
                    SewerBeeTest.offer(game,seat);
                }
                assertFalse(foe.isAlive());
            }
            assertEquals(2f,hero.buff(MonkEnergy.class).energy,0f);
            return new StagedInput(game,"MONK_FOCUS",hero.pos);
        }
        if (type.startsWith("ice")) {
            hero.lvl=10;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.WAND_PRESERVATION.ordinal(),null));
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.ARCANE_VISION.ordinal(),null));
            Wand frost=WandArtsTest.give(game,seat,new WandOfFrost());
            int at=WandArtsTest.choice(game,seat,frost);
            if (type.equals("iceimbue")) return new StagedInput(game,"IMBUE",at);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.IMBUE,at,hero.belongings.weapon()));
            if (type.equals("ice")) return new StagedInput(game,"ZAP",target.pos);
            boolean volley=type.equals("icevolley");
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,volley?1:0,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
            SewerBeeTest.offer(game,seat);
            if (volley) {
                WandArtsTest.give(game,seat,new WandOfFrost());
                WandArtsTest.give(game,seat,new WandOfFireblast());
                WandArtsTest.give(game,seat,new WandOfBlastWave());
            }
            return new StagedInput(game,"ARMOR_ABILITY",volley?target.pos:hero.pos);
        }
        if (type.equals("markdeath") || type.endsWith("clone")) {
            boolean death=type.equals("markdeath");
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,death?1:2,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
            SewerBeeTest.offer(game,seat);
            if (type.equals("orderclone")) {
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,hero.pos,hero.belongings.armor()));
                SewerBeeTest.offer(game,seat);
            }
            return new StagedInput(game,"ARMOR_ABILITY",death || type.equals("orderclone") ? target.pos : hero.pos);
        }
        if (type.equals("guardshield") || type.equals("stance")) {
            MeleeWeapon weapon=type.equals("stance") ? new Quarterstaff() : new RoundShield();
            weapon.identify();
            assertTrue(weapon.collect(hero.belongings.backpack));
            game.ownership.claim(weapon,game.player(seat));
            hero.STR=20;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,weapon));
            SewerBeeTest.offer(game,seat);
            return new StagedInput(game,"WEAPON_ABILITY",hero.pos);
        }
        if (type.equals("smite") || type.equals("radiance")) {
            HeroSubClass choice=type.equals("smite") ? HeroSubClass.PALADIN : HeroSubClass.PRIEST;
            game.beginMaskTrial(CoopSession.MaskTrial.OWNER);
            com.shatteredpixel.shatteredpixeldungeon.items.TengusMask mask=new com.shatteredpixel.shatteredpixeldungeon.items.TengusMask();
            assertTrue(mask.collect(hero.belongings.backpack));
            game.ownership.claim(mask,game.player(seat));
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_MASK,choice.ordinal(),mask));
            SewerBeeTest.offer(game,seat);
            target.pos=hero.pos+1;
            game.observe(seat);
            return new StagedInput(game,type.toUpperCase(java.util.Locale.ROOT),choice==HeroSubClass.PRIEST ? hero.pos : target.pos);
        }
        hero.lvl=10;
        Talent talent=type.equals("divinesense") ? Talent.DIVINE_SENSE : type.equals("sunray") ? Talent.SUNRAY : Talent.SHIELD_OF_LIGHT;
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,talent.ordinal(),null));
        return new StagedInput(game,talent.name(),talent==Talent.DIVINE_SENSE ? hero.pos : target.pos);
    }

    static boolean published(CoopSession game,String type,int seat) {
        Hero hero=game.hero(seat);
        switch (type) {
        case "iceimbue":
            return ((MagesStaff)hero.belongings.weapon()).imbuedWand() instanceof WandOfFrost
                    && hero.belongings.getItem(WandOfMagicMissile.class)!=null;
        case "ice":
            return !hero.buffs(TalismanOfForesight.CharAwareness.class).isEmpty()
                    && Dungeon.level.mobs.stream().anyMatch(m->m.buff(Chill.class)!=null && m.HP<m.HT);
        case "iceblast":
            return ((ClassArmor)hero.belongings.armor()).charge<26
                    && Dungeon.level.mobs.stream().anyMatch(m->m.buff(Frost.class)!=null && m.HP<m.HT);
        case "icevolley": return ((ClassArmor)hero.belongings.armor()).charge<26 && Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT);
        case "markdeath": return Dungeon.level.mobs.stream().anyMatch(m->m.deathMarked && m.buff(DeathMark.DeathMarkTracker.class).caster()==hero);
        case "clone": case "orderclone": return ShadowClone.getShadowAlly(hero)!=null && ShadowClone.getShadowAlly(game.hero(1-seat))==null;
        case "stance": return hero.buff(Quarterstaff.DefensiveStance.class)!=null;
        case "swap": return hero.subClass==HeroSubClass.CHAMPION && hero.belongings.weapon() instanceof Quarterstaff
                    && hero.belongings.secondWep() instanceof Rapier && game.ownership.owner(hero.belongings.secondWep())==game.player(seat);
        case "monkfocus": return hero.subClass==HeroSubClass.MONK && hero.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class)!=null
                    && hero.buff(MonkEnergy.class).energy==0;
        case "smite": return hero.subClass==HeroSubClass.PALADIN && Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT);
        case "radiance": return hero.subClass==HeroSubClass.PRIEST && Dungeon.level.mobs.stream().anyMatch(m->m.buff(GuidingLight.Illuminated.class)!=null && m.buff(GuidingLight.Illuminated.class).caster()==hero);
        case "guardshield": return hero.buff(RoundShield.GuardTracker.class)!=null;
        case "sunray": return Dungeon.level.mobs.stream().anyMatch(m->m.buff(Sunray.SunRayUsedTracker.class)!=null && m.HP<m.HT);
        case "divinesense": return hero.buff(DivineSense.DivineSenseTracker.class)!=null;
        case "lightshield": return hero.buff(ShieldOfLight.ShieldOfLightTracker.class)!=null;
        default: throw new IllegalArgumentException(type);
        }
    }

    @Test public void onlyTheInvestingSeatSeesTheMovingContactThroughAWall() throws Exception {
        for (HeroClass role : new HeroClass[]{HeroClass.HUNTRESS, HeroClass.MAGE}) {
            for (int seat = 0; seat < 2; seat++) {
                Bundle saved;
                int contact;
                try (CoopSession game = CoopSession.open(572, 11, role)) {
                    game.join(22, role);
                    Rat rat = CoopMageTest.encounter(game, seat);
                    SewerBeeTest.offer(game, seat);
                    Hero hero = game.hero(seat);
                    hero.lvl = 10;
                    Talent talent = role == HeroClass.MAGE ? Talent.ARCANE_VISION : Talent.HEIGHTENED_SENSES;
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
                    if (role == HeroClass.MAGE) {
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ZAP, rat.pos, hero.belongings.weapon()));
                        assertEquals(1, hero.buffs(TalismanOfForesight.CharAwareness.class).size());
                    }
                    Level level = Dungeon.level;
                    for (int y = -3; y <= 3; y++) Level.set(hero.pos + 1 + y * level.width(), Terrain.WALL);
                    game.hero(1-seat).pos = hero.pos - 1;
                    rat.pos = hero.pos + 3;
                    assertFalse(game.observe(seat).visible(rat.pos));
                    if (role == HeroClass.HUNTRESS) {
                        assertFalse(game.observe(seat).sensedMob(rat.pos));
                        SewerBeeTest.offer(game, seat);
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
                    }
                    ProjectionChecks.sensedContact(game, seat, rat);
                    contact = rat.pos;
                    saved = game.checkpoint();
                    assertEquals(20, saved.getInt("schema"));
                }
                try (CoopSession game = CoopSession.restore(saved)) {
                    assertTrue(game.observe(seat).sensedMob(contact));
                    assertFalse(game.observe(1-seat).sensedMob(contact));
                    assertFalse(game.observe(seat).visible(contact));
                    if (role == HeroClass.MAGE) {
                        game.hero(seat).buff(TalismanOfForesight.CharAwareness.class).detach();
                        assertFalse(game.observe(seat).sensedMob(contact));
                    }
                }
            }
        }
    }

    @Test public void frostStaffMeleeSchedulesOneFreezeAfterDamageAndSavesThePendingTarget() {
        for (int seat = 0; seat < 2; seat++) {
            Bundle saved;
            int id;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
                game.join(22, HeroClass.MAGE);
                Rat rat = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                StagedInput imbue = stage(game, "iceimbue", seat, rat);
                Hero hero = game.hero(seat);
                MagesStaff staff = (MagesStaff)hero.belongings.weapon();
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.IMBUE, imbue.cell, staff));
                // The native staff's on-hit proc runs before Char.attack applies the rolled melee damage.
                hero.subClass = HeroSubClass.BATTLEMAGE;
                Buff.prolong(rat, Chill.class, 12f);
                staff.proc(hero, rat, 3);
                assertNotNull(rat.buff(WandOfFrost.DelayedFreeze.class));
                assertNull(rat.buff(Frost.class));
                rat.damage(3, hero);
                id = rat.id();
                saved = game.checkpoint();
            }
            try (CoopSession game = CoopSession.restore(saved)) {
                Rat rat = (Rat)Actor.findById(id);
                WandOfFrost.DelayedFreeze pending = rat.buff(WandOfFrost.DelayedFreeze.class);
                assertNotNull(pending);
                assertNull(rat.buff(Frost.class));
                pending.act();
                assertNull(rat.buff(WandOfFrost.DelayedFreeze.class));
                assertTrue(rat.buff(Frost.class).cooldown() >= Frost.DURATION);
            }
        }
    }

    @Test public void frostImpactFreezesOwnedMeatExtinguishesFireAndRespectsFrozenTargets() {
        for (int seat = 0; seat < 2; seat++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.MAGE)) {
                game.join(22, HeroClass.MAGE);
                Rat rat = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                Hero hero = game.hero(seat);
                WandOfFrost wand = WandArtsTest.give(game, seat, new WandOfFrost());
                Level.set(rat.pos, Terrain.WATER);
                MysteryMeat meat = new MysteryMeat();
                game.ownership.claim(meat, game.player(1-seat));
                long id = game.ownership.identity(meat);
                Dungeon.level.drop(meat, rat.pos);
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(Blob.seed(rat.pos, 4, Fire.class));
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ZAP, rat.pos, wand));
                assertTrue(rat.HP < rat.HT);
                assertTrue(rat.buff(Chill.class).cooldown() >= 3);
                Item frozen = Dungeon.level.heaps.get(rat.pos).peek();
                assertTrue(frozen instanceof FrozenCarpaccio);
                assertEquals(id, game.ownership.identity(frozen));
                assertEquals(game.player(1-seat), game.ownership.owner(frozen));
                assertEquals(0, Blob.volumeAt(rat.pos, Fire.class));
                SewerBeeTest.offer(game, seat);
                Buff.prolong(rat, Frost.class, 5f);
                int before = rat.HP;
                int charges = wand.curCharges;
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ZAP, rat.pos, wand));
                assertEquals(before, rat.HP);
                assertEquals(charges - 1, wand.curCharges);
            }
        }
    }

    @Test public void lethalMarkPaysTheInvestingRogueOnceAfterTheMarkedEnemyDies() {
        for (int seat = 0; seat < 2; seat++) {
            Bundle saved;
            int id;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.ROGUE)) {
                game.join(22, HeroClass.ROGUE);
                Rat rat = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                Hero hero = game.hero(seat);
                hero.lvl = 26;
                StagedInput mark = stage(game, "markdeath", seat, rat);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.DEATHLY_DURABILITY.ordinal(), null));
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, mark.cell, hero.belongings.armor()));
                id = rat.id();
                rat.damage(rat.HP + 5, hero);
                assertTrue(rat.isAlive());
                assertEquals(0, rat.HP);
                saved = game.checkpoint();
            }
            try (CoopSession game = CoopSession.restore(saved)) {
                Rat rat = (Rat)Actor.findById(id);
                rat.buff(DeathMark.DeathMarkTracker.class).detach();
                assertFalse(rat.isAlive());
                assertEquals(25, game.hero(seat).buff(Barrier.class).shielding());
                assertNull(game.hero(1-seat).buff(Barrier.class));
                game.checkpoint();
            }
        }
    }

    @Test public void theOwnersCloneSwapsWithItsSummonerOnlyAndCommandsCostNoCharge() {
        for (int seat = 0; seat < 2; seat++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.ROGUE)) {
                game.join(22, HeroClass.ROGUE);
                Rat rat = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                Hero hero = game.hero(seat);
                hero.lvl = 26;
                stage(game, "clone", seat, rat);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, Talent.PERFECT_COPY.ordinal(), null));
                ClassArmor armor = (ClassArmor)hero.belongings.armor();
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, hero.pos, armor));
                SewerBeeTest.offer(game, seat);
                ShadowClone.ShadowAlly clone = ShadowClone.getShadowAlly(hero);
                assertFalse(clone.interact(game.hero(1-seat)));
                int ownerPos = hero.pos;
                int clonePos = clone.pos;
                int partnerPos = game.hero(1-seat).pos;
                float charge = armor.charge;
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, clonePos, armor));
                assertEquals(clonePos, hero.pos);
                assertEquals(ownerPos, clone.pos);
                assertEquals(partnerPos, game.hero(1-seat).pos);
                assertEquals(charge, armor.charge, 0f);
            }
        }
    }

    @Test public void actualAbilityInputsSurviveRestorationForEitherSeat() {
        for(String type:new String[]{"ice","iceimbue","iceblast","icevolley","sunray","divinesense","lightshield","smite","radiance","swap","monkfocus","stance","guardshield","markdeath","clone","orderclone"}) for(int seat=0;seat<2;seat++) {
            try {
                HeroClass role=type.startsWith("ice") ? HeroClass.MAGE : type.endsWith("clone") || type.equals("markdeath") ? HeroClass.ROGUE :
                        type.equals("swap") || type.equals("monkfocus") || type.equals("stance") || type.equals("guardshield") ? HeroClass.DUELIST : HeroClass.CLERIC;
                Bundle saved;
                try(CoopSession g=CoopSession.open(572,11,role)) {
                    g.join(22,role);
                    Rat rat=CoopMageTest.encounter(g,seat);
                    SewerBeeTest.offer(g,seat);
                    StagedInput input=stage(g,type,seat,rat);
                    Hero hero=g.hero(seat);
                    Item item=input.verb.equals("ARMOR_ABILITY") ? hero.belongings.armor() :
                            role==HeroClass.CLERIC ? hero.belongings.artifact : hero.belongings.weapon();
                    CoopSession.Action action=CoopSession.Action.valueOf(input.verb);
                    assertTrue(CoopFloorsTest.step(g,action,input.cell,action.requiresItem() ? item : null));
                    assertTrue(published(g,type,seat));
                    saved=g.checkpoint();
                }
                try(CoopSession g=CoopSession.restore(saved)) { assertTrue(published(g,type,seat)); }
            } catch(Throwable e) { throw new AssertionError(type+" seat "+seat,e); }
        }
    }
}
