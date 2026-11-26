package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blooming;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.junit.Test;
import static org.junit.Assert.*;

public class SealArtsTest extends HeadlessDungeon {
    /** Disposable profiles stop before the selector, leaving the actual cast to the client. */
    public static void prepareSelectors(CoopSession game, String kind, int seats) {
        Rat enemy = CoopMageTest.encounter(game, 0);
        for (int seat = 0; seat < seats; seat++) {
            SewerBeeTest.offer(game, seat);
            if (kind.equals("intuition") || kind.equals("recall")) {
                stage(game, kind.equals("recall") ? "recallid" : "intuition", seat, enemy);
                continue;
            }
            Hero hero = game.hero(seat);
            hero.lvl = 30;
            if (seat == 0) {
                KingsCrown crown = CrownAbilityTest.crown(game, seat, CoopSession.MaskTrial.ROSTER);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.USE_CROWN, 1, crown));
            } else {
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.CLAIM_ARMOR, 1, null));
            }
            SewerBeeTest.offer(game, seat);
            Talent talent;
            Item reference;
            switch (kind) {
            case "body":
                talent = Talent.BODY_FORM;
                reference = new ClothArmor().inscribe(new Swiftness());
                break;
            case "mind":
                talent = Talent.MIND_FORM;
                reference = new WandOfLightning();
                break;
            case "spirit":
                talent = Talent.SPIRIT_FORM;
                reference = new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste();
                break;
            default: throw new IllegalArgumentException("Unknown selector " + kind);
            }
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
            HolyTome tome = (HolyTome) hero.belongings.artifact;
            tome.upgrade();
            tome.charge(hero, 10);
            reference.identify();
            assertTrue(reference.collect(hero.belongings.backpack));
            game.ownership.claim(reference, game.player(seat));
            if (Boolean.getBoolean("spd.selector.history"))
                Dungeon.level.drop(reference.detachAll(hero.belongings.backpack), hero.pos);
        }
        SewerBeeTest.offer(game, 0);
    }

    static HeroClass role(String type) {
        if (type.startsWith("spark")) return HeroClass.MAGE;
        if (type.startsWith("monk") || type.startsWith("strike") || type.equals("heroicenergy")) return HeroClass.DUELIST;
        if (type.startsWith("seal") || type.equals("rationthrow")) return HeroClass.WARRIOR;
        if (type.equals("challenge") || type.equals("elestrike") || type.equals("feint")) return HeroClass.DUELIST;
        return HeroClass.CLERIC;
    }

    static StagedInput stage(CoopSession game, String type, int seat, Rat rat) {
        Hero hero = game.hero(seat);
        Armor armor = hero.belongings.armor();
        if (type.equals("readrecharge") || type.equals("recallrecharge")) {
            hero.lvl = 15;
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT,Talent.RECALL_INSCRIPTION.ordinal(),null));
            ScrollOfRecharging scroll = new ScrollOfRecharging();
            scroll.setKnown();
            assertTrue(scroll.collect(hero.belongings.backpack));
            game.ownership.claim(scroll,game.player(seat));
            if (type.equals("readrecharge")) return new StagedInput(game,"READ_RECHARGING",0);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.READ_RECHARGING, 0, scroll));
            assertNull(hero.belongings.getItem(ScrollOfRecharging.class));
            assertNotNull(hero.buff(Recharging.class));
            assertTrue(RecallInscription.remembersRecharging(hero));
            SewerBeeTest.offer(game,seat);
            return new StagedInput(game,"RECALL_INSCRIPTION",hero.pos);
        }
        if (type.startsWith("strike") || type.equals("heroicenergy")) {
            stage(game, "elestrike", seat, rat);
            if (type.equals("heroicenergy")) {
                hero.lvl = 30;
                return new StagedInput(game, "INVEST_TALENT", Talent.HEROIC_ENERGY.ordinal());
            }
            MeleeWeapon weapon = (MeleeWeapon)hero.belongings.weapon();
            switch (type) {
                case "strikekinetic":
                    weapon.enchant(new Kinetic());
                    Buff.affect(hero, Kinetic.ConservedDamage.class).setBonus(25);
                    break;
                case "strikebloom": weapon.enchant(new Blooming()); break;
                case "strikeblock": weapon.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking()); break;
                case "strikeshock": weapon.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking()); break;
                case "strikevenom": weapon.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Venomous()); break;
                default: throw new IllegalArgumentException(type);
            }
            Rat second = new Rat();
            second.pos = hero.pos+2;
            second.HP = second.HT = 150;
            com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(second,2000);
            game.observe(seat);
            return new StagedInput(game,"ARMOR_ABILITY",hero.pos+3);
        }
        if (type.startsWith("monk")) {
            FrostSensesTest.stage(game,"monkfocus",seat,rat);
            for (int i=0;i<4;i++) {
                Rat foe=new Rat(); foe.HP=1; foe.pos=hero.pos+1;
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(foe,2000);
                game.observe(seat);
                for (int n=0;n<20 && foe.isAlive();n++) {
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,foe.pos,null));
                    SewerBeeTest.offer(game,seat);
                }
                assertFalse(foe.isAlive());
            }
            boolean attack=type.equals("monkflurry") || type.equals("monkdragonkick");
            if (attack) {
                rat.pos=hero.pos+1;
                for (int i=2;i<=8;i++) {
                    int tile=hero.pos+i;
                    Dungeon.level.traps.remove(tile);
                    Dungeon.level.plants.remove(tile);
                    com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(tile,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);
                }
            }
            game.observe(seat);
            return new StagedInput(game,"MONK_"+type.substring(4).toUpperCase(java.util.Locale.ROOT),
                    attack ? rat.pos : type.equals("monkdash") ? hero.pos-2 : hero.pos);
        }
        if (type.startsWith("spark")) {
            hero.lvl=10;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.WAND_PRESERVATION.ordinal(),null));
            Wand lightning=WandArtsTest.give(game,seat,new WandOfLightning());
            int index=WandArtsTest.choice(game,seat,lightning);
            if (type.equals("sparkimbue")) return new StagedInput(game,"IMBUE",index);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.IMBUE,index,hero.belongings.weapon()));
            if (type.equals("spark")) return new StagedInput(game,"ZAP",rat.pos);
            boolean volley=type.equals("sparkvolley");
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,volley ? 1 : 0,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
            SewerBeeTest.offer(game,seat);
            if (volley) WandArtsTest.give(game,seat,new WandOfLightning());
            return new StagedInput(game,"ARMOR_ABILITY",volley ? rat.pos : hero.pos);
        }
        if (type.equals("intuition") || type.equals("recallid") || type.equals("bless") || type.startsWith("trinity")) {
            hero.lvl=30;
            boolean trinity=type.startsWith("trinity");
            if (trinity) {
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,1,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.OWNER)));
                SewerBeeTest.offer(game,seat);
            }
            Talent talent=type.startsWith("trinitymind") ? Talent.MIND_FORM : type.startsWith("trinityspirit") ? Talent.SPIRIT_FORM :
                    type.equals("recallid") ? Talent.RECALL_INSCRIPTION : trinity ? Talent.BODY_FORM : type.equals("intuition") ? Talent.HOLY_INTUITION : Talent.BLESS;
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,talent.ordinal(),null));
            if (talent==Talent.BLESS) return new StagedInput(game,"BLESS",hero.pos);
            if (talent==Talent.MIND_FORM || talent==Talent.SPIRIT_FORM) {
                if (talent==Talent.SPIRIT_FORM) {
                    HolyTome tome=(HolyTome)hero.belongings.artifact;
                    tome.upgrade(); tome.charge(hero,10);
                }
                Item reference=talent==Talent.MIND_FORM ? new WandOfLightning() : new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste();
                reference.identify();
                assertTrue(reference.collect(hero.belongings.backpack));
                game.ownership.claim(reference,game.player(seat));
                int index=0;
                for (Item entry : hero.belongings) { index++; if (entry==reference) break; }
                if (type.endsWith("history")) {
                    return historyInput(game, seat, reference, talent == Talent.MIND_FORM
                            ? TrinityDiscoveries.Choice.LIGHTNING : TrinityDiscoveries.Choice.HASTE);
                }
                if (type.endsWith("select")) return new StagedInput(game, talent.name(), index);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.valueOf(talent.name()),index,hero.belongings.artifact));
                return new StagedInput(game,talent==Talent.MIND_FORM ? "TRINITY_MIND" : "TRINITY_SPIRIT",talent==Talent.MIND_FORM ? rat.pos : hero.pos);
            }
            if (type.equals("recallid")) {
                com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify scroll=new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify();
                scroll.setKnown(); assertTrue(scroll.collect(hero.belongings.backpack)); game.ownership.claim(scroll,game.player(seat));
                Quarterstaff toIdentify=new Quarterstaff();
                assertTrue(toIdentify.collect(hero.belongings.backpack)); game.ownership.claim(toIdentify,game.player(seat));
                int index=0;
                for (Item entry : hero.belongings) { index++; if (entry==toIdentify) break; }
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.IDENTIFY,index,scroll));
                SewerBeeTest.offer(game,seat);
            }
            ClothArmor reference=new ClothArmor();
            assertTrue(reference.collect(hero.belongings.backpack));
            game.ownership.claim(reference,game.player(seat));
            if (trinity) { reference.inscribe(new Swiftness()); reference.identify(); }
            if (type.equals("trinitybodyhistory")) return historyInput(game, seat, reference, TrinityDiscoveries.Choice.SWIFTNESS);
            if (type.equals("trinityfirehistory") || type.equals("trinityblockhistory")) {
                boolean blocking = type.equals("trinityblockhistory");
                TrinityDiscoveries.Choice choice = blocking ? TrinityDiscoveries.Choice.BLOCKING : TrinityDiscoveries.Choice.BLAZING;
                Quarterstaff staff = new Quarterstaff();
                assertTrue(staff.collect(hero.belongings.backpack));
                staff.enchant(blocking ? new Blocking() : new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing());
                assertFalse(game.discoveries(seat).knows(choice));
                staff.identify();
                return historyInput(game, seat, staff, choice);
            }
            int index=0;
            for (Item entry : hero.belongings) { index++; if (entry==reference) break; }
            if (type.equals("trinity")) {
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.BODY_FORM,index,hero.belongings.artifact));
                return new StagedInput(game,"ARMOR_ABILITY",hero.pos);
            }
            return new StagedInput(game,talent.name(),index);
        }
        if (role(type) == HeroClass.WARRIOR) {
            hero.lvl = 10;
            Talent talent = type.equals("rationthrow") ? Talent.IMPROVISED_PROJECTILES : Talent.RUNIC_TRANSFERENCE;
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.INVEST_TALENT, talent.ordinal(), null));
            if (type.equals("rationthrow")) return new StagedInput(game, "THROW", rat.pos);
            armor.inscribe(new Swiftness());
            if (type.equals("sealdetach")) return new StagedInput(game, "DETACH_SEAL", 0);
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.DETACH_SEAL, 0, armor));
            if (!type.equals("sealattach")) armor.inscribe(new Flow());
            int index = 0;
            for (Item item : hero.belongings) {
                index++;
                if (item == armor) break;
            }
            return new StagedInput(game, type.equals("sealattach") ? "AFFIX_SEAL" :
                    type.equals("sealkeep") ? "KEEP_ARMOR_GLYPH" : "TRANSFER_SEAL_GLYPH", index);
        }
        int index;
        switch (type) {
        case "challenge": case "ascendform": index = 0; break;
        case "elestrike": index = 1; break;
        default: index = 2;
        }
        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.USE_CROWN, index,
                CrownAbilityTest.crown(game, seat, CoopSession.MaskTrial.OWNER)));
        SewerBeeTest.offer(game, seat);
        int cell = hero.pos;
        switch (type) {
        case "challenge": case "elestrike": rat.pos = hero.pos+1; cell = rat.pos; break;
        case "lightorder":
            assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, hero.pos-1, hero.belongings.armor()));
            SewerBeeTest.offer(game, seat);
            cell = rat.pos; break;
        case "feint": case "lightally": cell = hero.pos-1; break;
        }
        game.observe(seat);
        return new StagedInput(game, "ARMOR_ABILITY", cell);
    }

    static StagedInput historyInput(CoopSession game, int seat, Item reference, TrinityDiscoveries.Choice choice) {
        Hero hero = game.hero(seat);
        assertTrue(game.discoveries(seat).knows(choice));
        assertFalse(game.discoveries(1 - seat).knows(choice));
        Item removed = reference.detachAll(hero.belongings.backpack);
        Dungeon.level.drop(removed, hero.pos);
        assertFalse(hero.belongings.contains(reference));
        return new StagedInput(game, choice.form.name(), choice.target);
    }

    static boolean published(CoopSession game, String type, int seat) {
        Hero hero = game.hero(seat);
        Armor armor = hero.belongings.armor();
        BrokenSeal seal = armor.checkSeal();
        switch (type) {
        case "heroicenergy": return hero.pointsInTalent(Talent.HEROIC_ENERGY) == 1 && game.hero(1-seat).pointsInTalent(Talent.HEROIC_ENERGY) == 0;
        case "strikeshock": return ((ClassArmor)armor).charge < 26 && com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.volumeAt(
                hero.pos+2,com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity.class) > 0;
        case "strikeblock": return ((ClassArmor)armor).charge < 26 && hero.buff(Barrier.class) != null && hero.buff(Barrier.class).shielding() >= 11;
        case "strikekinetic":
            return ((ClassArmor)armor).charge < 26 && hero.buff(Kinetic.ConservedDamage.class) == null
                    && Dungeon.level.mobs.stream().anyMatch(m -> m.pos == hero.pos+2 && m.HP == m.HT-10);
        case "strikebloom":
            return ((ClassArmor)armor).charge < 26 && hero.buff(ElementalStrike.ElementalStrikeFurrowCounter.class) != null
                    && Dungeon.level.mobs.stream().anyMatch(m -> m.pos == hero.pos+2 && m.buff(Roots.class) != null)
                    && java.util.Arrays.stream(Dungeon.level.map).anyMatch(t -> t == Terrain.HIGH_GRASS);
        case "strikevenom": return ((ClassArmor)armor).charge < 26 && Dungeon.level.mobs.stream().anyMatch(m -> m.pos == hero.pos+2 && m.buff(Poison.class) != null);
        case "monkflurry": return hero.buff(MonkEnergy.MonkAbility.FlurryCooldownTracker.class)!=null && Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT);
        case "monkdragonkick": return Dungeon.level.mobs.stream().anyMatch(m -> m instanceof Rat && m.HP<m.HT);
        case "monkdash": return Dungeon.level.mobs.stream().anyMatch(m -> m instanceof Rat && m.pos==hero.pos+5);
        case "monkmeditate": return hero.buff(MonkEnergy.MonkAbility.Meditate.MeditationRecharge.class)!=null || hero.buff(Recharging.class)!=null;
        case "trinitymind": return Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT) && ((ClassArmor)armor).charge<26;
        case "trinityspirit": return hero.buff(SpiritForm.SpiritFormBuff.class)!=null && hero.buff(SpiritForm.SpiritFormBuff.class).ring() instanceof com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
        case "readrecharge": return hero.buff(Recharging.class) != null && RecallInscription.remembersRecharging(hero)
                && hero.belongings.getItem(ScrollOfRecharging.class) == null && game.hero(1-seat).buff(Recharging.class) == null;
        case "recallrecharge": return hero.buff(Recharging.class) != null && hero.buff(RecallInscription.UsedItemTracker.class) == null
                && hero.belongings.getItem(ScrollOfRecharging.class) == null && ((HolyTome)hero.belongings.artifact).charges() == 0;
        case "recallid": return hero.belongings.backpack.items.stream().anyMatch(i -> i instanceof ClothArmor && i.isIdentified()) && hero.buff(RecallInscription.UsedItemTracker.class)==null;
        case "intuition": return hero.belongings.backpack.items.stream().anyMatch(i -> i instanceof ClothArmor && i.cursedKnown && !i.levelKnown);
        case "bless": return hero.buff(Bless.class)!=null && hero.buff(Barrier.class)!=null && game.hero(1-seat).buff(Bless.class)==null;
        case "trinitybodyhistory": case "trinityfirehistory": case "trinityblockhistory": case "trinitymindhistory": case "trinityspirithistory": {
            TrinityDiscoveries.Choice choice = type.equals("trinitybodyhistory") ? TrinityDiscoveries.Choice.SWIFTNESS :
                    type.equals("trinityfirehistory") ? TrinityDiscoveries.Choice.BLAZING :
                    type.equals("trinityblockhistory") ? TrinityDiscoveries.Choice.BLOCKING :
                    type.equals("trinitymindhistory") ? TrinityDiscoveries.Choice.LIGHTNING : TrinityDiscoveries.Choice.HASTE;
            return game.discoveries(seat).knows(choice) && !game.discoveries(1 - seat).knows(choice)
                    && ((Trinity)hero.armorAbility).hasForm(choice.form) && ((HolyTome)hero.belongings.artifact).charges() < 3;
        }
        case "trinitymindselect": case "trinityspiritselect":
            Talent form = type.equals("trinitymindselect") ? Talent.MIND_FORM : Talent.SPIRIT_FORM;
            HolyTome tome = (HolyTome) hero.belongings.artifact;
            return ((Trinity) hero.armorAbility).hasForm(form) && tome.charges() < tome.capacity()
                    && game.hero(1-seat).armorAbility == null;
        case "trinityselect": return ((Trinity)hero.armorAbility).hasForm(Talent.BODY_FORM) && ((HolyTome)hero.belongings.artifact).charges()<3 && hero.armorAbility instanceof Trinity;
        case "trinity": return hero.buff(BodyForm.BodyFormBuff.class)!=null && hero.buff(BodyForm.BodyFormBuff.class).glyph() instanceof Swiftness
                && armor.hasGlyph(Swiftness.class,hero) && !game.hero(1-seat).belongings.armor().hasGlyph(Swiftness.class,game.hero(1-seat));
        case "sparkimbue": return ((MagesStaff)hero.belongings.weapon()).imbuedWand() instanceof WandOfLightning && hero.belongings.getItem(WandOfMagicMissile.class)!=null;
        case "spark": case "sparkvolley": case "sparkblast": return Dungeon.level.mobs.stream().anyMatch(m->m.HP<m.HT);
        case "sealdetach": return seal == null && armor.glyph == null
                && hero.belongings.getItem(BrokenSeal.class).getGlyph() instanceof Swiftness;
        case "sealattach": case "sealtransfer": return seal != null && armor.glyph instanceof Swiftness && game.ownership.owner(seal)==game.player(seat);
        case "sealkeep": return seal != null && armor.glyph instanceof Flow && game.ownership.owner(seal)==game.player(seat);
        case "rationthrow": return Dungeon.level.mobs.stream().anyMatch(m -> m.buff(Blindness.class)!=null && m.HP==m.HT
                && Dungeon.level.heaps.get(m.pos)!=null && game.ownership.owner(Dungeon.level.heaps.get(m.pos).peek())==game.player(seat))
                && hero.buff(Talent.ImprovisedProjectileCooldown.class)!=null;
        case "challenge": return hero.buff(Challenge.DuelParticipant.class)!=null && game.hero(1-seat).buff(Challenge.DuelParticipant.class)==null
                && Dungeon.level.mobs.stream().anyMatch(m -> m.buff(Challenge.DuelParticipant.class)!=null) && ((ClassArmor)armor).charge<1;
        case "elestrike": return Dungeon.level.mobs.stream().anyMatch(m -> m.HP<m.HT) && ((ClassArmor)armor).charge<26;
        case "feint": return ((ClassArmor)armor).charge<16 && Dungeon.level.mobs.stream().anyMatch(m -> m instanceof Rat && m.pos==hero.pos+4);
        case "ascendform": return hero.buff(AscendedForm.AscendBuff.class)!=null && hero.buff(AscendedForm.AscendBuff.class).shielding()==30
                && game.hero(1-seat).buff(AscendedForm.AscendBuff.class)==null;
        case "lightally": case "lightorder": return PowerOfMany.getPoweredAlly(hero) instanceof PowerOfMany.LightAlly
                && PowerOfMany.getPoweredAlly(game.hero(1-seat))==null && ((ClassArmor)armor).charge<26;
        default: throw new IllegalArgumentException(type);
        }
    }

    @Test public void nativeSeatActionsKeepEffectsAndOwnershipAcrossRestore() {
        for (String type : new String[]{"sealdetach","sealattach","sealkeep","sealtransfer","rationthrow","challenge","elestrike","feint","ascendform","lightally","lightorder","trinitymind","trinityspirit","trinitymindselect","trinityspiritselect","recallid","intuition","bless","trinityselect","trinity","monkflurry","monkdash","monkdragonkick","monkmeditate","sparkimbue","spark","sparkblast","sparkvolley"}) {
            for (int seat=0;seat<2;seat++) {
                try {
                    Bundle saved;
                    try (CoopSession game=CoopSession.open(572,11,role(type))) {
                        game.join(22,role(type));
                        Rat rat=CoopMageTest.encounter(game,seat);
                        SewerBeeTest.offer(game,seat);
                        StagedInput input=stage(game,type,seat,rat);
                        Hero hero=game.hero(seat);
                        Item item=hero.belongings.armor();
                        if (!CoopSession.Action.valueOf(input.verb).requiresItem()) item=null;
                        else if (input.verb.equals("ZAP") || input.verb.equals("IMBUE")) item=hero.belongings.weapon();
                        else if (input.verb.matches("BODY_FORM|MIND_FORM|SPIRIT_FORM|BLESS|HOLY_INTUITION|RECALL_INSCRIPTION")) item=hero.belongings.artifact;
                        else if (type.equals("rationthrow")) item=hero.belongings.getItem(Food.class);
                        else if (type.startsWith("seal") && !type.equals("sealdetach")) item=hero.belongings.getItem(BrokenSeal.class);
                        assertTrue("input "+input.verb+" energy "+(hero.buff(MonkEnergy.class)==null ? "-" : hero.buff(MonkEnergy.class).energy)+" active "+game.activeSeat()+" pos "+hero.pos+" cell "+input.cell,CoopFloorsTest.step(game,CoopSession.Action.valueOf(input.verb),input.cell,item));
                        assertTrue("effect",published(game,type,seat));
                        saved=game.checkpoint();
                        assertEquals(21,CoopSession.growthSchema(saved));
                    }
                    try (CoopSession game=CoopSession.restore(saved)) { assertTrue("restore",published(game,type,seat)); }
                } catch (Throwable e) { throw new AssertionError(type+" seat "+seat,e); }
            }
        }
    }

    @Test public void aBrokenProjectileRemainsPersonalDiscoveryWithoutUnlockingAnUnsupportedForm() {
        for (int seat = 0; seat < 2; seat++) {
            Bundle save;
            try (CoopSession game = CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Rat rat = CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                var spike = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike();
                spike.quantity(1);
                spike.identify();
                spike.damage(spike.durabilityLeft());
                assertTrue(spike.collect(game.hero(seat).belongings.backpack));
                Buff.prolong(rat,Paralysis.class,20);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,spike));
                assertFalse(game.hero(seat).belongings.contains(spike));
                assertTrue(rat.HP < rat.HT);
                assertTrue(spike.durabilityLeft() <= 0);
                save = game.checkpoint();
            }
            try (CoopSession restored = CoopSession.restore(save)) {
                Bundle next = restored.checkpoint();
                String name = com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike.class.getName();
                assertTrue(java.util.Arrays.asList(next.getStringArray("discovered"+seat)).contains(name));
                assertFalse(java.util.Arrays.asList(next.getStringArray("discovered"+(1-seat))).contains(name));
                for (TrinityDiscoveries.Choice choice : TrinityDiscoveries.Choice.values())
                    assertNotEquals(name, choice.effect.getName());
            }
        }
    }

    @Test public void soldAndAbandonedReferencesSurviveTravelAndReload() {
        for (int seat = 0; seat < 2; seat++) {
            Bundle saved;
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
                game.join(22, HeroClass.CLERIC);
                Rat rat = CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                stage(game,"trinitymindselect",seat,rat);
                Hero hero = game.hero(seat);
                var merchant = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper();
                merchant.pos = hero.pos - 1;
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(merchant,2000);
                game.observe(seat);
                WandOfLightning wand = hero.belongings.getItem(WandOfLightning.class);
                long id = game.ownership.identity(wand);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.SELL,merchant.pos,wand));
                assertFalse(hero.belongings.contains(wand));
                assertTrue(merchant.buybackItems.contains(wand));
                assertEquals(id,game.ownership.identity(wand));
                var ring = new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste();
                ring.identify();
                assertTrue(ring.collect(hero.belongings.backpack));
                Dungeon.level.drop(ring.detachAll(hero.belongings.backpack),hero.pos);
                SewerSpecialActorTest.atGate(game,Dungeon.level.exit());
                CoopFloorsTest.cross(game);
                assertEquals(2,Dungeon.depth);
                assertTrue(game.discoveries(seat).knows(TrinityDiscoveries.Choice.HASTE));
                assertFalse(game.discoveries(1-seat).knows(TrinityDiscoveries.Choice.HASTE));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MIND_FORM,
                        TrinityDiscoveries.Choice.LIGHTNING.target, hero.belongings.artifact));
                saved = game.checkpoint();
            }
            try (CoopSession restored = CoopSession.restore(saved)) {
                assertEquals(2,Dungeon.depth);
                assertTrue(restored.discoveries(seat).knows(TrinityDiscoveries.Choice.LIGHTNING));
                assertFalse(restored.discoveries(1-seat).knows(TrinityDiscoveries.Choice.LIGHTNING));
                assertTrue(((Trinity)restored.hero(seat).armorAbility).hasForm(Talent.MIND_FORM));
            }
        }
    }

    @Test public void rechargingRecallUsesTheOriginalImprintAndNeverCreatesABagScroll() {
        for (int seat=0; seat<2; seat++) {
            Bundle save;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.CLERIC)) {
                game.join(22,HeroClass.CLERIC);
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                StagedInput input=stage(game,"recallrecharge",seat,rat);
                Hero hero=game.hero(seat);
                var own=com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks.seatImage(game,seat);
                var peer=com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks.seatImage(game,1-seat);
                assertTrue(own.source.rows.containsKey("recallrecharging"));
                assertFalse(peer.source.rows.containsKey("recallrecharging"));
                float before=hero.buff(Recharging.class).cooldown();
                assertFalse(CoopFloorsTest.step(game, CoopSession.Action.RECALL_INSCRIPTION,0,hero.belongings.artifact));
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.RECALL_INSCRIPTION,input.cell,hero.belongings.artifact));
                assertTrue(hero.buff(Recharging.class).cooldown()>before);
                assertTrue(published(game,"recallrecharge",seat));
                assertNull(game.hero(1-seat).buff(Recharging.class));
                save=game.checkpoint();
            }
            try (CoopSession restored=CoopSession.restore(save)) {
                assertTrue(published(restored,"recallrecharge",seat));
            }
        }
    }

    @Test public void elementalStrikeResolvesTheCastersKineticAndBloomingCones() {
        for (String kind : new String[]{"strikekinetic", "strikebloom"}) for (int seat = 0; seat < 2; seat++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.DUELIST)) {
                game.join(22, HeroClass.DUELIST);
                Rat rat = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                StagedInput input = stage(game, kind, seat, rat);
                int[] before = Dungeon.level.map.clone();
                Hero hero = game.hero(seat);
                assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, input.cell, hero.belongings.armor()));
                assertTrue(published(game, kind, seat));
                if (kind.equals("strikebloom")) {
                    int grass = 0;
                    for (int i=0; i<before.length; i++)
                        if (before[i] != Terrain.HIGH_GRASS && Dungeon.level.map[i] == Terrain.HIGH_GRASS) grass++;
                    assertTrue(grass > 0 && grass <= 8);
                    assertNull(game.hero(1-seat).buff(ElementalStrike.ElementalStrikeFurrowCounter.class));
                }
            }
        }
    }

    @Test public void absentTrinityReferencesUseOnlyTheSelectingSeatsHistory() {
        for (String kind : new String[]{"trinitybodyhistory", "trinitymindhistory", "trinityspirithistory", "trinityfirehistory", "trinityblockhistory"}) {
            for (int seat = 0; seat < 2; seat++) {
                Bundle saved;
                try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
                    game.join(22, HeroClass.CLERIC);
                    Rat foe = CoopMageTest.encounter(game, seat);
                    SewerBeeTest.offer(game, seat);
                    StagedInput pick = stage(game, kind, seat, foe);
                    assertEquals(22, pick.before.getInt("schema"));
                    HolyTome tome = (HolyTome) game.hero(seat).belongings.artifact;
                    assertFalse(CoopFloorsTest.step(game, CoopSession.Action.valueOf(pick.verb), -999, tome));
                    assertTrue(CoopFloorsTest.step(game, CoopSession.Action.valueOf(pick.verb), pick.cell, tome));
                    assertTrue(published(game, kind, seat));
                    if (kind.equals("trinityfirehistory")) {
                        SewerBeeTest.offer(game, seat);
                        Hero hero = game.hero(seat);
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, hero.pos, hero.belongings.armor()));
                        assertTrue(hero.buff(BodyForm.BodyFormBuff.class).enchant() instanceof
                                com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing);
                        foe.pos = hero.pos+1;
                        com.watabou.utils.Random.pushGenerator(572);
                        try {
                            for (int n = 0; n < 24 && foe.buff(Burning.class) == null; n++)
                                assertTrue(hero.attack(foe, 0, 1, Hero.INFINITE_ACCURACY));
                        } finally {
                            com.watabou.utils.Random.popGenerator();
                        }
                        assertNotNull(foe.buff(Burning.class));
                        assertNull(game.hero(1-seat).buff(BodyForm.BodyFormBuff.class));
                    }
                    if (kind.equals("trinityblockhistory")) {
                        SewerBeeTest.offer(game, seat);
                        Hero hero = game.hero(seat);
                        assertTrue(CoopFloorsTest.step(game, CoopSession.Action.ARMOR_ABILITY, hero.pos, hero.belongings.armor()));
                        assertTrue(hero.buff(BodyForm.BodyFormBuff.class).enchant() instanceof Blocking);
                        foe.pos = hero.pos + 1;
                        com.watabou.utils.Random.pushGenerator(572);
                        try {
                            for (int n = 0; n < 100 && hero.buff(Blocking.BlockBuff.class) == null; n++)
                                assertTrue(hero.attack(foe, 0, 0, Hero.INFINITE_ACCURACY));
                        } finally {
                            com.watabou.utils.Random.popGenerator();
                        }
                        assertTrue(hero.buff(Blocking.BlockBuff.class).shielding() > 0);
                        assertNull(game.hero(1-seat).buff(Blocking.BlockBuff.class));
                    }
                    saved = game.checkpoint();
                }
                try (CoopSession game = CoopSession.restore(saved)) {
                    assertTrue(published(game, kind, seat));
                }
            }
        }
    }

    @Test public void itemSelectorsRejectOtherSeatsTomeAndMissingTargetBeforeAnyEffect() {
        String[] selectors = {"intuition", "recallid", "trinityselect", "trinitymindselect", "trinityspiritselect"};
        for (String selector : selectors) for (int seat = 0; seat < 2; seat++) {
            try (CoopSession game = CoopSession.open(572, 11, HeroClass.CLERIC)) {
                game.join(22, HeroClass.CLERIC);
                Rat foe = CoopMageTest.encounter(game, seat);
                SewerBeeTest.offer(game, seat);
                StagedInput choice = stage(game, selector, seat, foe);
                CoopSession.Action spell = CoopSession.Action.valueOf(choice.verb);
                HolyTome mine = (HolyTome) game.hero(seat).belongings.artifact;
                HolyTome theirs = (HolyTome) game.hero(1-seat).belongings.artifact;
                int before = mine.charges(), other = theirs.charges();
                long turn = game.turn();

                assertFalse(selector, CoopFloorsTest.step(game, spell, choice.cell, theirs));
                assertFalse(selector, CoopFloorsTest.step(game, spell, Integer.MAX_VALUE, mine));
                assertEquals(turn, game.turn());
                assertEquals(before, mine.charges());
                assertEquals(other, theirs.charges());
                assertFalse(selector, published(game, selector, seat));

                assertTrue(selector, CoopFloorsTest.step(game, spell, choice.cell, mine));
                assertTrue(selector, published(game, selector, seat));
                assertTrue(mine.charges() < before);
                assertEquals(other, theirs.charges());
            }
        }
    }

    @Test public void sealTierAndPhysicalIdsFollowTheirActualWarrior() {
        for (int rank=0;rank<=2;rank++) for (boolean rare:new boolean[]{false,true}) for (int seat=0;seat<2;seat++) {
            Bundle saved;
            long sealId,armorId;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                hero.lvl=10;
                for (int i=0;i<rank;i++) assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.RUNIC_TRANSFERENCE.ordinal(),null));
                Armor armor=hero.belongings.armor();
                armor.upgrade();
                armor.inscribe(rare ? new Thorns() : new Swiftness());
                BrokenSeal seal=armor.checkSeal();
                sealId=game.ownership.identity(seal);
                armorId=game.ownership.identity(armor);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.DETACH_SEAL,0,armor));
                assertEquals(1,seal.level());
                assertEquals(0,armor.level());
                boolean transfer=rank==2 || rank==1 && !rare;
                assertEquals(transfer,seal.getGlyph()!=null);
                assertEquals(transfer,armor.glyph==null);
                assertNotNull(game.hero(1-seat).belongings.armor().checkSeal());
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat);
                assertEquals(sealId,game.ownership.identity(hero.belongings.getItem(BrokenSeal.class)));
                assertEquals(armorId,game.ownership.identity(hero.belongings.armor()));
            }
        }
    }
    @Test public void nativeLightningChainAndBattlemageChargeUseTheCastingSeat() {
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            int first, second;
            long internal;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                StagedInput zap=stage(game,"spark",seat,rat);
                MagesStaff staff=(MagesStaff)hero.belongings.weapon();
                internal=game.ownership.identity(staff.imbuedWand());
                Rat linked=new Rat();
                linked.HT=linked.HP=100; linked.pos=rat.pos+Dungeon.level.width();
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(linked,2000);
                first=rat.id();second=linked.id();
                game.hero(1-seat).pos=rat.pos-1;
                int partner=game.hero(1-seat).HP;
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ZAP,zap.cell,staff));
                game.hero(1-seat).pos=linked.pos+1;
                game.observe(seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,zap.cell,staff));
                assertTrue(rat.HP<rat.HT && linked.HP<linked.HT);
                assertEquals(partner,game.hero(1-seat).HP);
                hero.subClass=HeroSubClass.BATTLEMAGE;
                for (int i=0;i<100 && hero.buff(WandOfLightning.LightningCharge.class)==null;i++) staff.proc(hero,rat,3);
                assertNotNull(hero.buff(WandOfLightning.LightningCharge.class));
                assertNull(game.hero(1-seat).buff(WandOfLightning.LightningCharge.class));
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                assertNotNull(Actor.findById(first)); assertNotNull(Actor.findById(second));
                assertEquals(internal,game.ownership.identity(((MagesStaff)game.hero(seat).belongings.weapon()).imbuedWand()));
                assertNotNull(game.hero(seat).buff(WandOfLightning.LightningCharge.class));
            }
        }
    }

    @Test public void thrownFoodStaysBoundAndCooldownSuppressesASecondBlind() {
        for (int rank=0;rank<=2;rank++) for (int seat=0;seat<2;seat++) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                hero.lvl=10;
                for (int i=0;i<rank;i++) assertTrue(CoopFloorsTest.step(game,CoopSession.Action.INVEST_TALENT,Talent.IMPROVISED_PROJECTILES.ordinal(),null));
                Food food=hero.belongings.getItem(Food.class);
                food.quantity(3);
                long stack=game.ownership.identity(food);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,food));
                assertEquals(rank>0,rat.buff(Blindness.class)!=null);
                assertEquals(rank>0,hero.buff(Talent.ImprovisedProjectileCooldown.class)!=null);
                Item unit=Dungeon.level.heaps.get(rat.pos).peek();
                assertEquals(2,food.quantity()); assertNotEquals(stack,game.ownership.identity(unit));
                assertFalse(game.ownership.allowed(unit,game.player(1-seat)));
                assertEquals(rat.HT,rat.HP);
                if (rank>0) rat.buff(Blindness.class).detach();
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.THROW,rat.pos,food));
                assertNull(rat.buff(Blindness.class));
                Dungeon.level.mobs.remove(rat); Actor.remove(rat);
                hero.pos=rat.pos;
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,hero.pos,Dungeon.level.heaps.get(hero.pos).peek()));
                assertEquals(3,food.quantity());
                game.checkpoint();
            }
        }
    }

    @Test public void duelistConeUsesTheWearersActualEnchantment() {
        for (int seat=0;seat<2;seat++) for (boolean fire:new boolean[]{false,true}) {
            try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
                game.join(22,HeroClass.DUELIST);
                Rat rat=CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                StagedInput input=stage(game,"elestrike",seat,rat);
                MeleeWeapon weapon=(MeleeWeapon)hero.belongings.weapon();
                weapon.enchant(fire ? new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing()
                        : new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling());
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,input.cell,hero.belongings.armor()));
                assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.volumeAt(rat.pos,fire ?
                        com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire.class : com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing.class)>0);
                game.checkpoint();
            }
        }
    }

    @Test public void elementalStrikeStoresItsElectricityShieldOrSecondaryPoison() {
        for (int seat = 0; seat < 2; seat++) {
            for (var enchantment : new com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment[]{
                    new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking(),
                    new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking(),
                    new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Venomous()}) {
                Bundle saved;
                int secondaryId, cell;
                try (CoopSession game = CoopSession.open(572, 11, HeroClass.DUELIST)) {
                    game.join(22, HeroClass.DUELIST);
                    Rat foe = CoopMageTest.encounter(game,seat);
                    SewerBeeTest.offer(game,seat);
                    stage(game,"elestrike",seat,foe);
                    Hero hero = game.hero(seat);
                    Rat second = new Rat();
                    second.pos = hero.pos + 2;
                    second.HT = second.HP = 150;
                    com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(second,2000);
                    cell = second.pos;
                    secondaryId = second.id();
                    ((MeleeWeapon)hero.belongings.weapon()).enchant(enchantment);
                    game.observe(seat);
                    ClassArmor armor = (ClassArmor)hero.belongings.armor();
                    float charge = armor.charge;
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,hero.pos+3,armor));
                    assertTrue(armor.charge <= charge-24);
                    assertNull(game.hero(1-seat).buff(Barrier.class));
                    saved = game.checkpoint();
                }
                try (CoopSession resumed = CoopSession.restore(saved)) {
                    if (enchantment instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking) {
                        assertTrue(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.volumeAt(cell,
                                com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity.class) > 0);
                    } else if (enchantment instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking) {
                        Barrier shield = resumed.hero(seat).buff(Barrier.class);
                        assertNotNull(shield);
                        assertTrue(shield.shielding() >= 11 && shield.shielding() <= 12);
                    } else {
                        Rat second = (Rat)com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findById(secondaryId);
                        assertNotNull(second.buff(Poison.class));
                        assertNull(resumed.hero(1-seat).buff(Poison.class));
                    }
                }
            }
        }
    }

    @Test public void aSecondChallengerCannotSpendOnTheOngoingDuel() {
        for (int seat=0;seat<2;seat++) try (CoopSession game=CoopSession.open(572,11,HeroClass.DUELIST)) {
            game.join(22,HeroClass.DUELIST);
            Rat rat=CoopMageTest.encounter(game,seat);
            SewerBeeTest.offer(game,seat);
            Hero hero=game.hero(seat),partner=game.hero(1-seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,0,CrownAbilityTest.crown(game,seat,CoopSession.MaskTrial.ROSTER)));
            SewerBeeTest.offer(game,1-seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLAIM_ARMOR,0,null));
            SewerBeeTest.offer(game,seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,rat.pos,hero.belongings.armor()));
            SewerBeeTest.offer(game,1-seat);
            float charge=((ClassArmor)partner.belongings.armor()).charge;
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,rat.pos,partner.belongings.armor()));
            assertEquals(charge,((ClassArmor)partner.belongings.armor()).charge,0f);
            assertNull(partner.buff(Challenge.SpectatorFreeze.class));
            rat.damage(rat.HP+1,hero);
            for (int i=0;i<12 && hero.buff(Challenge.DuelParticipant.class)!=null;i++)
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.WAIT,0,null));
            assertNull(hero.buff(Challenge.DuelParticipant.class));
        }
    }

    @Test public void fullBackpackDropsTheSameSealAndOnlyItsOwnerRecoversIt() {
        for (int seat=0;seat<2;seat++) {
            Bundle saved;
            long id;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.WARRIOR)) {
                game.join(22,HeroClass.WARRIOR);
                CoopMageTest.encounter(game,seat);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                Armor armor=hero.belongings.armor();
                BrokenSeal seal=armor.checkSeal();
                id=game.ownership.identity(seal);
                while (hero.belongings.backpack.canHold(seal)) {
                    Dagger filler=new Dagger();
                    assertTrue(filler.collect(hero.belongings.backpack));
                    game.ownership.claim(filler,game.player(seat));
                }
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.DETACH_SEAL,0,armor));
                assertSame(seal,Dungeon.level.heaps.get(hero.pos).peek());
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                Hero hero=game.hero(seat), other=game.hero(1-seat);
                int drop=hero.pos;
                Item seal=Dungeon.level.heaps.get(drop).peek();
                assertEquals(id,game.ownership.identity(seal));
                int adjacent=other.pos;
                hero.pos=adjacent; other.pos=drop;
                SewerBeeTest.offer(game,1-seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,drop,seal));
                hero.pos=drop;other.pos=adjacent;
                hero.belongings.getItem(Dagger.class).detachAll(hero.belongings.backpack);
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,drop,seal));
                assertEquals(id,game.ownership.identity(hero.belongings.getItem(BrokenSeal.class)));
            }
        }
    }

}
