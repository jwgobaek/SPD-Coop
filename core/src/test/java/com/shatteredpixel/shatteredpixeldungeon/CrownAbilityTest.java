package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.plants.*;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;

public class CrownAbilityTest extends HeadlessDungeon {
    static int choice(Hero hero) {
        ArmorAbility[] choices=hero.heroClass.armorAbilities();
        for(int i=0;i<choices.length;i++) if(CoopSession.playableArmorAbility(choices[i]) && !(choices[i] instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades)) return i;
        throw new AssertionError(hero.heroClass);
    }

    static KingsCrown crown(CoopSession game,int seat,CoopSession.MaskTrial trial) {
        if(trial!=CoopSession.MaskTrial.OFF) game.beginCrownTrial(trial);
        KingsCrown crown=new KingsCrown();
        assertTrue(crown.collect(game.hero(seat).belongings.backpack));
        game.ownership.claim(crown,game.player(seat));
        return crown;
    }

    static int destination(Hero hero) {
        if(hero.heroClass!=HeroClass.WARRIOR) return hero.pos;
        for(int offset:PathFinder.NEIGHBOURS8) {
            int dest=hero.pos+offset;
            if(Dungeon.level.passable[dest] && !Dungeon.level.pit[dest] && Actor.findChar(dest)==null
                    && CoopSession.supportedLanding(dest)) return dest;
        }
        throw new AssertionError("no landing");
    }

    @Test public void classArmorPreservesIdentityAndOnlyChargesItsWearer() {
        for(int seat=0;seat<2;seat++) for(HeroClass type:new HeroClass[]{HeroClass.WARRIOR,HeroClass.MAGE,HeroClass.HUNTRESS}) {
            Bundle save;
            int chargerId, enemyId;
            long id;
            try(CoopSession game=CoopSession.open(572,11,type)) {
                game.join(22,type);
                Rat foe=CoopMageTest.encounter(game,seat);
                enemyId=foe.id();
                Hero hero=game.hero(seat);
                KingsCrown reward=crown(game,seat,CoopSession.MaskTrial.ROSTER);
                SewerBeeTest.offer(game,1-seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,choice(hero),reward));
                SewerBeeTest.offer(game,seat);
                Armor original=hero.belongings.armor();
                original.upgrade(2);
                id=game.ownership.identity(original);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.USE_CROWN,choice(hero),reward));
                ClassArmor armor=(ClassArmor)hero.belongings.armor();
                assertNull(hero.belongings.getItem(KingsCrown.class));
                assertEquals(id,game.ownership.identity(armor));
                assertEquals(original.trueLevel(),armor.trueLevel());
                assertSame(original.checkSeal(),armor.checkSeal());
                assertTrue(armor.charge>=50 && armor.charge<51);
                chargerId=hero.buff(ClassArmor.Charger.class).id();
                save=game.checkpoint();
                assertEquals(17,save.getInt("schema"));
            }
            try(CoopSession game=CoopSession.restore(save)) {
                Hero h=game.hero(seat);
                assertEquals(chargerId,h.buff(ClassArmor.Charger.class).id());
                SewerBeeTest.offer(game,1-seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.CLAIM_ARMOR,choice(game.hero(1-seat)),null));
                assertFalse(game.mayChooseArmor(1-seat,false));
                SewerBeeTest.offer(game,seat);
                ClassArmor armor=(ClassArmor)h.belongings.armor();
                assertEquals(id,game.ownership.identity(armor));
                ClassArmor ally=(ClassArmor)game.hero(1-seat).belongings.armor();
                float partyCharge=ally.charge, initial=armor.charge, cost=h.armorAbility.chargeUse(h);
                int target=destination(h);
                Rat foe=(Rat)Actor.findById(enemyId);
                int hp=foe.HP;
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,target,ally));
                assertTrue(type.name(),CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,target,armor));
                assertTrue(armor.charge>=initial-cost-0.01f && armor.charge<initial-cost+1);
                assertTrue(ally.charge>=partyCharge);
                if(type==HeroClass.WARRIOR) assertEquals(target,h.pos);
                if(type==HeroClass.HUNTRESS) assertNotNull(h.buff(NaturesPower.naturesPowerTracker.class));
                if(type==HeroClass.MAGE) assertTrue("blast damage",foe.HP<hp);
                SewerBeeTest.offer(game,seat);
                assertFalse(CoopFloorsTest.step(game,CoopSession.Action.ARMOR_ABILITY,destination(h),armor));
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.UNEQUIP,0,armor));
                assertNull(h.buff(ClassArmor.Charger.class));
                SewerBeeTest.offer(game,seat);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.EQUIP,0,armor));
                assertNotNull(h.buff(ClassArmor.Charger.class));
                save=game.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(save)) {
                assertEquals(id,g.ownership.identity(g.hero(seat).belongings.armor()));
                assertFalse(g.mayChooseArmor(1-seat,false));
            }
        }
    }

    @Test public void ordinaryAndOwnerTrialCannotGrantAPartnerRight() {
        for(CoopSession.MaskTrial trial:new CoopSession.MaskTrial[]{CoopSession.MaskTrial.OFF,CoopSession.MaskTrial.OWNER})
            try(CoopSession g=CoopSession.open(571,11,HeroClass.WARRIOR)) {
                g.join(22,HeroClass.WARRIOR);
                KingsCrown reward=crown(g,0,trial);
                assertEquals(trial==CoopSession.MaskTrial.OWNER,CoopFloorsTest.step(g,CoopSession.Action.USE_CROWN,choice(g.hero(0)),reward));
                assertFalse(g.mayChooseArmor(1,false));
            }
    }

    @Test public void wardenPlantEffectsFollowTheActualOccupantAcrossRestore() {
        for(int seat=0;seat<2;seat++) {
            Plant.Seed[] seeds={new Blindweed.Seed(),new Stormvine.Seed(),new Mageroyal.Seed(),new Firebloom.Seed(),new Icecap.Seed(),new Starflower.Seed(),new Sorrowmoss.Seed(),new Earthroot.Seed(),new Sungrass.Seed(),new Swiftthistle.Seed()};
            Class<?>[] effects={Invisibility.class,Levitation.class,BlobImmunity.class,FireImbue.class,FrostImbue.class,Recharging.class,ToxicImbue.class,Barkskin.class,Healing.class,Swiftthistle.TimeBubble.class};
            for(int i=0;i<seeds.length;i++) {
                Class<? extends Buff> type=effects[i].asSubclass(Buff.class);
                Bundle save;
                try(CoopSession g=CoopSession.open(402,11,HeroClass.HUNTRESS)) {
                    g.join(22,HeroClass.HUNTRESS);
                    SewerBeeTest.offer(g,seat);
                    g.hero(seat).subClass=HeroSubClass.WARDEN;
                    Talent.initSubclassTalents(g.hero(seat));
                    int cell=SewerPlantTest.arrange(g,seat,seeds[i],true);
                    assertTrue(type.getSimpleName(),CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
                    assertNotNull(type.getName(),g.hero(seat).buff(type));
                    assertNull(g.hero(1-seat).buff(type));
                    save=g.checkpoint();
                }
                try(CoopSession g=CoopSession.restore(save)) {
                    assertNotNull(g.hero(seat).buff(type));
                    assertNull(g.hero(1-seat).buff(type));
                }
            }
        }
    }
}
