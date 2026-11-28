package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerGrowthTest extends HeadlessDungeon {
    private static Talent first(Hero hero) {
        switch (hero.heroClass) {
            case WARRIOR: return Talent.HEARTY_MEAL;
            case MAGE: return Talent.EMPOWERING_MEAL;
            case HUNTRESS: return Talent.FOLLOWUP_STRIKE;
            case ROGUE: return Talent.PROTECTIVE_SHADOWS;
            case DUELIST: return Talent.AGGRESSIVE_BARRIER;
            case CLERIC: return Talent.SATIATED_SPELLS;
            default: throw new AssertionError();
        }
    }

    @Test public void actualFoodConsumesOnlyItsOwnersInvestedTierOneEffects() {
        for (int owner = 0; owner < 2; owner++) for (HeroClass type : new HeroClass[]{HeroClass.MAGE,HeroClass.WARRIOR}) {
            Bundle state;
            try (CoopSession g=CoopSession.open(581,1,type)) {
                g.join(2,type);
                CoopMageTest.encounter(g,owner);
                SewerBeeTest.offer(g,owner);
                Hero h=g.hero(owner);
                Talent t=first(h);
                long turn=g.turn();
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,t.ordinal(),null));
                h.earnExp(h.maxExp(), Rat.class);
                assertTrue(ProjectionChecks.seatImage(g,owner).source.rows.containsKey("learn"+t.ordinal()));
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,Talent.SUCKER_PUNCH.ordinal(),null));
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,t.ordinal(),null));
                assertEquals(turn,g.turn());
                assertEquals(owner,g.activeSeat());
                assertEquals(1,h.pointsInTalent(t));
                assertEquals(0,g.hero(1-owner).pointsInTalent(t));
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.INVEST_TALENT,t.ordinal(),null));
                state=g.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(state)) {
                Hero h=g.hero(owner);
                assertEquals(0,h.talentPointsAvailable(1));
                h.HP=2;
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.EAT,0,h.belongings.getItem(Food.class)));
                if (type==HeroClass.MAGE) {
                    assertNotNull(h.buff(WandEmpower.class));
                    assertEquals(2,h.buff(WandEmpower.class).dmgBoost);
                    assertEquals(3,h.buff(WandEmpower.class).left);
                    assertNull(g.hero(1-owner).buff(WandEmpower.class));
                } else assertTrue("native hearty meal",h.HP >= 6);
            }
        }
    }

    @Test public void warlockWandMarkHealsAndFeedsItsCasterAfterRestore() {
        for (int seat=0;seat<2;seat++) {
            Bundle checkpoint;
            int enemyId;
            try (CoopSession game=CoopSession.open(575,11,HeroClass.MAGE)) {
                game.join(22,HeroClass.MAGE);
                Rat enemy=CoopMageTest.encounter(game,seat);
                enemyId=enemy.id();
                Hero caster=game.hero(seat);
                caster.subClass=HeroSubClass.WARLOCK;
                Talent.initSubclassTalents(caster);
                caster.talents.get(2).put(Talent.SOUL_SIPHON,3);
                caster.talents.get(2).put(Talent.SOUL_EATER,3);
                com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand=
                        new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();
                wand.upgrade(5);
                wand.identify(false);
                assertTrue(wand.collect(caster.belongings.backpack));
                game.ownership.claim(wand,game.player(seat));
                // The native proc is probabilistic. Replay a fixed roll stream for this
                // ownership/restoration regression while retaining the real wand and scheduler.
                com.watabou.utils.Random.pushGenerator(575);
                try {
                    for(int i=0;i<8 && SoulMark.source(enemy)==null;i++) {
                        SewerBeeTest.offer(game,seat);
                        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ZAP,enemy.pos,wand));
                    }
                } finally {
                    com.watabou.utils.Random.popGenerator();
                }
                assertSame(caster,SoulMark.source(enemy));
                float expires=enemy.buff(SoulMark.class).cooldown();
                SoulMark.applyFrom(game.hero(1-seat),enemy,100);
                assertSame(caster,SoulMark.source(enemy));
                assertEquals(expires,enemy.buff(SoulMark.class).cooldown(),0f);
                checkpoint=game.checkpoint();
            }
            try (CoopSession g=CoopSession.restore(checkpoint)) {
                Rat enemy=(Rat)com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findById(enemyId);
                Hero caster=g.hero(seat), ally=g.hero(1-seat);
                assertSame(caster,SoulMark.source(enemy));
                caster.HP=ally.HP=1;
                Hunger hunger=Buff.affect(caster,Hunger.class);
                hunger.affectHunger(-100);
                int before=hunger.hunger();
                enemy.defenseProc(ally,10);
                assertEquals(3,caster.HP);
                assertEquals(1,ally.HP);
                assertEquals(before-4,hunger.hunger());
                enemy.defenseProc(caster,10);
                assertEquals(7,caster.HP);
                assertEquals(before-14,hunger.hunger());
                caster.HP=caster.HT;
                enemy.defenseProc(caster,10);
                assertEquals(caster.HT,caster.HP);
            }
        }
    }

    static TengusMask pickMask(CoopSession game,int seat) {
        int cell=TenguArenaTest.prepareSocket(game,"claim");
        SewerBeeTest.offer(game,seat);
        if (game.hero(1-seat).pos==cell) CavesBossTest.beside(game,1-seat,cell);
        game.hero(seat).pos=cell;
        game.observe(seat);
        TengusMask mask=(TengusMask)Dungeon.level.heaps.get(cell).peek();
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,mask));
        SewerBeeTest.offer(game,seat);
        return mask;
    }

    @Test public void aSingleNativeMaskGrantsExactlyTheSelectedTrialRights() {
        for (int owner=0;owner<2;owner++) for (CoopSession.MaskTrial mode:CoopSession.MaskTrial.values()) {
            Bundle state;
            try(CoopSession g=CoopSession.open(578,11,HeroClass.WARRIOR)) {
                g.join(22,HeroClass.HUNTRESS);
                TengusMask mask=pickMask(g,owner);
                if (mode!=CoopSession.MaskTrial.OFF) g.beginMaskTrial(mode);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.USE_MASK,HeroSubClass.WARLOCK.ordinal(),mask));
                SewerBeeTest.offer(g,1-owner);
                assertFalse(CoopFloorsTest.step(g,CoopSession.Action.USE_MASK,g.hero(1-owner).heroClass.subClasses()[0].ordinal(),mask));
                SewerBeeTest.offer(g,owner);
                HeroSubClass choice=g.hero(owner).heroClass.subClasses()[0];
                assertEquals(mode!=CoopSession.MaskTrial.OFF,CoopFloorsTest.step(g,CoopSession.Action.USE_MASK,choice.ordinal(),mask));
                if (mode==CoopSession.MaskTrial.OFF) {
                    assertTrue(g.hero(owner).belongings.contains(mask));
                    continue;
                }
                assertEquals(choice,g.hero(owner).subClass);
                assertNull(g.hero(owner).belongings.getItem(TengusMask.class));
                assertFalse(g.hero(owner).talents.get(2).isEmpty());
                state=g.checkpoint();
                assertEquals(16,state.getInt("schema"));
            }
            try(CoopSession g=CoopSession.restore(state)) {
                SewerBeeTest.offer(g,1-owner);
                int choice=g.hero(1-owner).heroClass.subClasses()[1].ordinal();
                assertEquals(mode==CoopSession.MaskTrial.ROSTER,g.mayClaimSubclass(1-owner));
                assertEquals(mode==CoopSession.MaskTrial.ROSTER,CoopFloorsTest.step(g,CoopSession.Action.CLAIM_SUBCLASS,choice,null));
                assertFalse(g.mayClaimSubclass(owner));
                assertFalse(g.mayClaimSubclass(1-owner));
                assertNull(g.hero(1-owner).belongings.getItem(TengusMask.class));
                g.checkpoint();
            }
        }
    }

    // Replay the same public investment or mask choice through two authenticated TCP clients.
    public static StagedInput stage(CoopSession g,String stage) {
        int seat=stage.endsWith("1")?1:0;
        if (stage.matches("kitbrew[01]")) return PortableAlchemyTest.prepare(g,seat,2);
        if (stage.matches("earthheal[01]")) return LivingEarthTest.prepare(g,seat);
        if (stage.matches("(detectstone|guessstone)[01]")) return RuneReadingTest.prepare(g, seat, stage);
        if (stage.matches("(exoticvision|exoticterror|wornaugment|visiondrink|fearstone|corruptzap|mapread|maprecall)[01]")) return ConsumableEffectsTest.prepare(g, seat, stage);
        if (stage.matches("unknownread[01]")) return CauldronTest.unknownScroll(g, seat);
        if (stage.matches("gaswait[01]")) return CorrosionAttributionTest.prepareGasTick(g, seat);
        if (stage.matches("seedbrew[01]")) return MultiAlchemyTest.prepare(g,seat);
        if (stage.matches("exoticbrew[01]")) return MultiAlchemyTest.preparePaid(g,seat);
        if (stage.matches("bloodzap[01]")) return TransfusionTest.prepare(g,seat);
        if (stage.matches("cauldron[01]")) return CauldronTest.prepare(g, seat);
        if (BowEffectsTest.isThrowFixture(stage)) return BowEffectsTest.prepareThrow(g,seat,stage);
        if (newAbility(stage)) return SubclassActionTest.stage(g,stage,seat);
        if (stage.startsWith("talent")) {
            SewerBeeTest.offer(g,seat);
            g.hero(seat).earnExp(g.hero(seat).maxExp(),Rat.class);
            return new StagedInput(g,"INVEST_TALENT",first(g.hero(seat)).ordinal());
        }
        int owner=stage.startsWith("right")?1-seat:seat;
        TengusMask mask=pickMask(g,owner);
        g.beginMaskTrial(stage.startsWith("owner")?CoopSession.MaskTrial.OWNER:CoopSession.MaskTrial.ROSTER);
        if (stage.startsWith("right")) {
            assertTrue(CoopFloorsTest.step(g,CoopSession.Action.USE_MASK,g.hero(owner).heroClass.subClasses()[0].ordinal(),mask));
            SewerBeeTest.offer(g,seat);
        }
        return new StagedInput(g,stage.startsWith("right")?"CLAIM_SUBCLASS":"USE_MASK",g.hero(seat).heroClass.subClasses()[0].ordinal());
    }

    public static boolean published(String stage) {
        CoopSession g=Dungeon.coop;
        int seat=stage.endsWith("1")?1:0;
        if (stage.matches("kitbrew[01]")) return PortableAlchemyTest.published(g,seat);
        if (stage.matches("earthheal[01]")) return LivingEarthTest.published(g,seat);
        if (stage.matches("(detectstone|guessstone)[01]")) return RuneReadingTest.published(g, seat, stage);
        if (stage.matches("(exoticvision|exoticterror|wornaugment|visiondrink|fearstone|corruptzap|mapread|maprecall)[01]")) return ConsumableEffectsTest.published(g, seat, stage);
        if (stage.matches("unknownread[01]")) return CauldronTest.readUnknown(g, seat);
        if (stage.matches("gaswait[01]")) return CorrosionAttributionTest.publishedGasTick(g, seat);
        if (stage.matches("seedbrew[01]")) return MultiAlchemyTest.published(g,seat);
        if (stage.matches("exoticbrew[01]")) return MultiAlchemyTest.paid(g,seat);
        if (stage.matches("bloodzap[01]")) return TransfusionTest.published(g,seat);
        if (stage.matches("cauldron[01]")) return CauldronTest.published(g, seat);
        if (BowEffectsTest.isThrowFixture(stage)) {
            var stones=g.hero(seat).belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone.class);
            return stones!=null && stones.quantity()==3 && g.ownership.owner(stones)==g.player(seat);
        }
        if (newAbility(stage)) return SubclassActionTest.published(g,stage,seat);
        if (stage.startsWith("talent")) return g.hero(seat).pointsInTalent(first(g.hero(seat)))==1;
        return g.hero(seat).subClass==g.hero(seat).heroClass.subClasses()[0] &&
                g.hero(seat).belongings.getItem(TengusMask.class)==null &&
                g.mayClaimSubclass(1-seat)==stage.startsWith("grant");
    }

    static boolean newAbility(String stage) {
        if (stage.startsWith("upper_")) return true;
        return stage.matches("(remoteplace|ninjalog|rayblast|warppropose|warpreturn|stasisfloor|stasisorder|stasisexpiry|stasisblocked|stasisrank|stasishold|stasisrelease|divineshield|beamingray|lifelink|scrollread|scrolltarget|scrollapply|scrollabort|eagleview|warpwraith|hornhit|cloakring|packedcloak|bookward|paladinheal|paladinaura|prayer|hallow|wall|bowstone|bowrank|bowthrow|poisontip|holylance|cleanse|scoutshot|holdfast|deathlesswait|deathlesstrap|trinitymindrayhistory|trinitymindraycast|trinitymindprismhistory|trinitymindmissilehistory|recallid|readrecharge|recallrecharge|readlullaby|recalllullaby|trinitymind|trinityspirit|trinitybodyhistory|trinitymindhistory|trinityspirithistory|trinitybloomhistory|trinityfirehistory|trinityblockhistory|trinitymindselect|trinityspiritselect|intuition|bless|trinityselect|trinity|monkflurry|monkdash|monkdragonkick|monkmeditate|prismmelee|prismimbue|prismzap|prismblast|prismvolley|sparkimbue|spark|sparkblast|sparkvolley|sealdetach|sealattach|sealkeep|sealtransfer|rationthrow|strikeshock|strikeblock|strikevenom|strikekinetic|strikebloom|heroicenergy|challenge|elestrike|feint|ascendform|lightally|lightorder|ice|iceimbue|iceblast|icevolley|swap|monkfocus|radiance|smite|markdeath|clone|orderclone|sunray|divinesense|lightshield|guardshield|stance|smoke|ambush|freerun|lunge|spike|charged|light|ward|blessweapon|hawk|direct|imbue|volley|leaptrap|doubleparry|parryready|lethal|cloak|cacheinvest|cachepickup|strongman|endless_rage|desperate_power|point_blank|third|shock|battery|enhanced|blades|beacon|recall|endure|clobber|slam|parry|riposte|crush|fury|rage|sniper|crown|armorright|leap|blast|nature|meal|warlock|battlemage|soul)[01]");
    }

    public static String signature(CoopSession g, String stage) {
        if (stage.matches("gaswait[01]")) return CorrosionAttributionTest.gasSignature(g, stage.endsWith("1") ? 1 : 0);
        return (stage.startsWith("scroll") || stage.startsWith("eagleview") ? EnchantScrollTest.signature(g,stage.charAt(stage.length()-1)-'0')+"/" : "") + g.turn()+"/"+g.hero(0).subClass+"/"+g.hero(1).subClass+"/"+
                g.hero(0).pointsInTalent(first(g.hero(0)))+"/"+g.hero(1).pointsInTalent(first(g.hero(1)))+"/"+
                g.mayClaimSubclass(0)+"/"+g.mayClaimSubclass(1)+(newAbility(stage)?"/"+SubclassActionTest.signature(g):"")
                +(stage.matches("(imbue|volley|hawk|direct|lunge|spike|charged|light|ward|blessweapon)[01]")
                        ? "/"+WandArtsTest.artsSignature(g) : "");
    }
}
