package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

/** Lab-only native selection callback and armor conversion; no seat entitlements or wire verbs. */
public final class NativeGrowthChoiceProbe extends HeadlessDungeon {
    static final class Choice extends TengusMask {
        void chooseForFixture(Hero hero,HeroSubClass subclass) {
            // Supply the native dialog's user without binding Dungeon.hero to the guest.
            Hero previous=curUser;
            try {curUser=hero;choose(subclass);}
            finally {curUser=previous;}
        }
    }
    static final class QuietSprite extends TestSprite {
        QuietSprite(Hero hero) {super(hero);}
        @Override public Emitter centerEmitter() {return new Emitter();}
    }
    static void sample(int owner,HeroClass selected,int candidate,int armorChoice) {
        try(CoopSession game=CoopSession.open(581,1,owner==0?selected:HeroClass.WARRIOR)) {
            game.join(2,owner==1?selected:HeroClass.MAGE);
            Hero h=game.hero(owner),ally=game.hero(1-owner);
            Choice mask=new Choice();
            assertTrue(mask.collect(h.belongings.backpack));
            CharSprite live=h.sprite;
            new QuietSprite(h);
            try {mask.chooseForFixture(h,selected.subClasses()[candidate]);}
            finally {h.sprite=live;}
            assertSame(selected.subClasses()[candidate],h.subClass);
            assertFalse(h.belongings.backpack.contains(mask));
            assertEquals(HeroSubClass.NONE,ally.subClass);
            assertFalse(h.talents.get(2).isEmpty());
            Armor original=h.belongings.armor;
            original.upgrade(2);original.inscribe(new Viscosity());
            ClassArmor upgraded=ClassArmor.upgrade(h,original);
            assertEquals(original.level(),upgraded.level());
            assertSame(original.glyph,upgraded.glyph);
            assertSame(original.checkSeal(),upgraded.checkSeal());
            assertEquals(50,upgraded.charge,0);
            ArmorAbility ability=selected.armorAbilities()[armorChoice];
            h.armorAbility=ability;
            Talent.initArmorTalents(h);
            assertTrue(ability.chargeUse(h)>0);
            assertFalse(h.talents.get(3).isEmpty());
            Bundle b=new Bundle();
            h.storeInBundle(b);
            Hero restored=new Hero();restored.restoreFromBundle(b);
            assertSame(h.subClass,restored.subClass);
            assertSame(h.armorAbility.getClass(),restored.armorAbility.getClass());
            assertEquals(h.talents,restored.talents);
            System.out.printf("CHOICE\t%d\t%s\t%s\t%s\t%d\t%.0f%n",owner,selected,h.subClass,
                    upgraded.getClass().getSimpleName()+"/"+ability.getClass().getSimpleName(),upgraded.level(),upgraded.charge);
        }
    }
    public static void main(String[] args) throws Exception {
        NativeGrowthChoiceProbe p=new NativeGrowthChoiceProbe();p.temporary.create();p.startBackend();
        try {
            for (HeroClass kind:new HeroClass[]{HeroClass.WARRIOR,HeroClass.MAGE,HeroClass.HUNTRESS})
                for(int owner=0;owner<2;owner++) for(int candidate=0;candidate<2;candidate++)
                    for (int armor=0;armor<3;armor++) sample(owner,kind,candidate,armor);
        } finally {p.stopBackend();p.temporary.delete();}
    }
}
