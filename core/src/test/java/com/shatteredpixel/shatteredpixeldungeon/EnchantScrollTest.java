package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.lan.PublicDungeon;
import com.watabou.utils.Bundle;
import org.junit.Test;

import static com.shatteredpixel.shatteredpixeldungeon.CoopSession.Action.*;
import static org.junit.Assert.*;

public class EnchantScrollTest extends HeadlessDungeon {
    private static boolean act(CoopSession game, CoopSession.Action action, int index, Item item) {
        return CoopFloorsTest.step(game, action, index, item);
    }

    static StagedInput stage(CoopSession game,int seat,String name) {
        Hero hero=game.hero(seat);
        if (name.equals("eagleview")) {
            ArmorTalentsTest.invest(game,seat,2,com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.EAGLE_EYE,3);
            return new StagedInput(game,"ARMOR_ABILITY",hero.pos);
        }
        ScrollOfEnchantment scroll=new ScrollOfEnchantment();
        Dungeon.level.drop(scroll,hero.pos);
        assertTrue(act(game,PICK_UP,hero.pos,scroll));
        SewerBeeTest.offer(game,seat);
        scroll.setKnown();
        if (name.equals("scrollread")) return new StagedInput(game,"READ_SCROLL",0);
        assertTrue(act(game,READ_SCROLL,0,scroll));
        if (name.equals("scrolltarget")) return new StagedInput(game,"ENCHANT_TARGET",0);
        assertTrue(act(game,ENCHANT_TARGET,0,hero.belongings.getItem(SpiritBow.class)));
        return new StagedInput(game,name.equals("scrollapply") ? "ENCHANT_CHOOSE" : "ENCHANT_CANCEL",0);
    }

    static boolean published(CoopSession game,int seat,String name) {
        Hero hero=game.hero(seat);
        if (name.equals("eagleview"))
            return com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk.getHawk(hero)!=null;
        CoopEnchantScroll choice=game.enchantScroll(seat);
        if (name.equals("scrollread")) return choice!=null && !choice.hasOptions();
        if (name.equals("scrolltarget")) return choice!=null && choice.hasOptions();
        return choice==null && hero.belongings.getItem(ScrollOfEnchantment.class)==null
                && (hero.belongings.getItem(SpiritBow.class).enchantment!=null)==name.equals("scrollapply");
    }

    static String signature(CoopSession game,int seat) {
        Bundle b=game.checkpoint();
        return (b.contains("enchant"+seat) ? b.getBundle("enchant"+seat).toString() : "none")+"/"+java.util.Arrays.toString(b.getLongArray("random"))
                +"/"+(game.hero(seat).belongings.getItem(SpiritBow.class).enchantment==null ? "none" : game.hero(seat).belongings.getItem(SpiritBow.class).enchantment.getClass().getSimpleName());
    }

    @Test public void readingAndCancellingPayTheNativeCostAtEachBoundary() {
        for (int seat=0; seat<2; seat++) for (boolean known : new boolean[]{false,true}) {
            Bundle saved;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                ScrollOfEnchantment scroll=new ScrollOfEnchantment();
                Dungeon.level.drop(scroll,hero.pos);
                assertTrue(act(game,PICK_UP,hero.pos,scroll));
                SewerBeeTest.offer(game,seat);
                if (known) scroll.setKnown();
                assertEquals(known, scroll.isKnown());
                long turn=game.turn();
                assertTrue(act(game, READ_SCROLL, 0, scroll));
                assertEquals(turn,game.turn());
                assertEquals(known,hero.belongings.contains(scroll));
                assertFalse(act(game,WAIT,hero.pos,null));
                assertFalse(act(game,ENCHANT_TARGET,0,game.hero(1-seat).belongings.getItem(SpiritBow.class)));
                saved=game.checkpoint();
                assertEquals(24,saved.getInt("schema"));
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                long turn=game.turn();
                assertTrue(act(game,ENCHANT_CANCEL,0,null));
                assertEquals(known,game.turn()==turn);
                assertNull(game.enchantScroll(seat));
                assertNull(game.enchantScroll(1-seat));
                assertFalse(act(game,ENCHANT_CANCEL,0,null));
            }
        }
    }

    @Test public void privateOptionsAndPhysicalGearSurviveRestoreWithoutAReroll() {
        for (int seat=0; seat<2; seat++) for (boolean armor : new boolean[]{false,true}) {
            Bundle saved;
            String[] options=new String[3];
            long gearId;
            try (CoopSession game=CoopSession.open(572,11,HeroClass.HUNTRESS)) {
                game.join(22,HeroClass.HUNTRESS);
                SewerBeeTest.offer(game,seat);
                Hero hero=game.hero(seat);
                Item gear;
                if (armor) {
                    gear=new LeatherArmor();
                    assertTrue(gear.collect(hero.belongings.backpack));
                    game.ownership.claim(gear,game.player(seat));
                } else {
                    SpiritBow bow=hero.belongings.getItem(SpiritBow.class);
                    bow.enchant(new Chilling());
                    gear=bow;
                }
                gearId=game.ownership.identity(gear);
                ScrollOfEnchantment scroll=new ScrollOfEnchantment();
                scroll.quantity(2);
                Dungeon.level.drop(scroll,hero.pos);
                assertTrue(act(game,PICK_UP,hero.pos,scroll));
                SewerBeeTest.offer(game,seat);
                scroll.setKnown();
                assertTrue(act(game,READ_SCROLL,0,scroll));
                assertTrue(act(game,ENCHANT_TARGET,0,gear));
                assertEquals(1,scroll.quantity());
                assertFalse(act(game,ENCHANT_TARGET,0,gear));
                assertFalse(act(game,ENCHANT_CHOOSE,3,null));
                PublicDungeon me=ProjectionChecks.seatImage(game,seat);
                PublicDungeon other=ProjectionChecks.seatImage(game,1-seat);
                assertEquals("options",me.source.rows.get("enchantphase"));
                assertFalse(other.source.rows.containsKey("enchantphase"));
                for(int i=0;i<3;i++) {
                    options[i]=game.enchantScroll(seat).optionKey(i);
                    assertEquals(options[i],me.source.rows.get("enchantoption"+i));
                    assertFalse(other.source.rows.containsKey("enchantoption"+i));
                    if (!armor) assertFalse(options[i].endsWith(".chilling"));
                }
                assertEquals(3,new java.util.HashSet<>(java.util.Arrays.asList(options)).size());
                saved=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                long turn=game.turn();
                assertTrue(act(game,ENCHANT_CANCEL,0,null));
                assertEquals(turn,game.turn());
                assertNull(game.enchantScroll(seat));
                assertEquals(1,game.hero(seat).belongings.getItem(ScrollOfEnchantment.class).quantity());
            }
            try (CoopSession game=CoopSession.restore(saved)) {
                for(int i=0;i<3;i++) assertEquals(options[i],game.enchantScroll(seat).optionKey(i));
                assertArrayEquals(saved.getLongArray("random"),game.checkpoint().getLongArray("random"));
                assertTrue(act(game,ENCHANT_CHOOSE,1,null));
                Hero hero=game.hero(seat);
                Item gear=armor ? hero.belongings.getItem(LeatherArmor.class) : hero.belongings.getItem(SpiritBow.class);
                assertEquals(gearId,game.ownership.identity(gear));
                Class<?> actual=armor ? ((LeatherArmor)gear).glyph.getClass() : ((SpiritBow)gear).enchantment.getClass();
                assertTrue(options[1].endsWith("."+actual.getSimpleName().toLowerCase(java.util.Locale.ROOT)));
                assertNull(game.enchantScroll(seat));
            }
        }
    }
}
