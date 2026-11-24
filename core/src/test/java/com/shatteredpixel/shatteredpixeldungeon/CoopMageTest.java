package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import java.io.*;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoopMageTest extends HeadlessDungeon {
    private static void offer(CoopSession game,int seat) {
        for(int i=0;i<10 && game.activeSeat()!=seat;i++)
            assertTrue(game.act(game.player(game.activeSeat()),game.turn(),CoopSession.Action.WAIT,0,null));
        assertEquals(seat,game.activeSeat());
    }

    // Controlled lines of sight in a generated sewer. The monster, wand, scheduler and saves are real.
    public static Rat encounter(CoopSession game,int seat) {
        Level level=Dungeon.level;
        for(Mob m:new ArrayList<>(level.mobs)) { Actor.remove(m); level.mobs.remove(m); }
        int width=level.width();
        int center=level.length()/2/width*width+width/2;
        for(int dy=-2;dy<=2;dy++) for(int dx=-4;dx<=4;dx++) {
            int cell=center+dx+dy*width;
            Level.set(cell,Terrain.EMPTY);
            level.discoverable[cell]=true;
        }
        game.hero(seat).pos=center;
        game.hero(1-seat).pos=center+width;
        Rat rat=new Rat();
        rat.HT=rat.HP=200;
        rat.pos=center+3;
        GameScene.add(rat,2000);
        game.observe(seat);
        return rat;
    }

    @Test public void bothSeatsUseOwnStaffAndRestoreItsChargerAndTime() throws Exception {
        HeroClass[] partners={HeroClass.WARRIOR,HeroClass.HUNTRESS,HeroClass.MAGE};
        for(int seat=0;seat<2;seat++) for(HeroClass partner:partners) {
            File disk=temporary.newFile();
            int charges,chargerId,enemyId,upgrade;
            long identity;
            float partial;
            try(CoopSession game=CoopSession.open(402+seat,11,seat==0?HeroClass.MAGE:partner)) {
                game.join(22,seat==1?HeroClass.MAGE:partner);
                Rat rat=encounter(game,seat);
                offer(game,seat);
                Hero mage=game.hero(seat),ally=game.hero(1-seat);
                MagesStaff staff=(MagesStaff)mage.belongings.weapon();
                WandOfMagicMissile wand=staff.starterWand();
                assertSame(mage,wand.chargingHero());
                int charge=wand.curCharges;
                WandEmpower extra=Buff.affect(mage,WandEmpower.class);
                extra.set(5,2);
                WandEmpower other=Buff.affect(ally,WandEmpower.class);
                other.set(100,3);
                long turn=game.turn();
                float due=Actor.now()+mage.cooldown();
                for(int cell:new int[]{-1,Integer.MAX_VALUE,mage.pos,ally.pos,rat.pos+1})
                    assertFalse(game.act(game.player(seat),turn,CoopSession.Action.ZAP,cell,staff));
                assertFalse(game.act(game.player(seat),turn-1,CoopSession.Action.ZAP,rat.pos,staff));
                assertFalse(game.act(game.player(seat),turn,CoopSession.Action.ZAP,rat.pos,ally.belongings.weapon()));
                Level.set(mage.pos+1,Terrain.WALL);
                game.observe(seat);
                assertFalse(game.act(game.player(seat),turn,CoopSession.Action.ZAP,rat.pos,staff));
                Level.set(mage.pos+1,Terrain.EMPTY);
                game.observe(seat);
                int range=mage.viewDistance;
                mage.viewDistance=1;
                assertFalse(game.act(game.player(seat),turn,CoopSession.Action.ZAP,rat.pos,staff));
                mage.viewDistance=range;
                wand.curCharges=0;
                assertFalse(game.act(game.player(seat),turn,CoopSession.Action.ZAP,rat.pos,staff));
                wand.curCharges=charge;
                assertEquals(turn,game.turn());
                assertEquals(due,Actor.now()+mage.cooldown(),0f);
                assertTrue(game.act(game.player(seat),turn,CoopSession.Action.ZAP,rat.pos,staff));
                assertTrue(200-rat.HP>=wand.min()+5 && 200-rat.HP<=wand.max()+5);
                assertEquals(1,extra.left);
                assertEquals(3,other.left);
                assertEquals(1f,Actor.now()+mage.cooldown()-due,0.0001f);
                assertEquals(charge-1,wand.curCharges);
                assertFalse(game.act(game.player(seat),turn,CoopSession.Action.ZAP,rat.pos,staff));
                if(partner==HeroClass.MAGE) {
                    WandOfMagicMissile second=((MagesStaff)ally.belongings.weapon()).starterWand();
                    assertEquals(second.maxCharges,second.curCharges);
                    assertSame(ally,second.chargingHero());
                }
                offer(game,seat);
                assertTrue(game.act(game.player(seat),game.turn(),CoopSession.Action.UNEQUIP,0,staff));
                assertSame(mage,wand.chargingHero());
                assertEquals(1,mage.buffs(Wand.Charger.class).size());
                chargerId=mage.buff(Wand.Charger.class).id();
                identity=game.ownership.identity(staff);
                enemyId=rat.id();
                upgrade=staff.level();
                partial=wand.partialCharge;
                charges=wand.curCharges;
                assertTrue(partial>0);
                try(OutputStream out=new FileOutputStream(disk)) { assertTrue(Bundle.write(game.checkpoint(),out,false)); }
            }
            try(InputStream in=new FileInputStream(disk);CoopSession game=CoopSession.restore(Bundle.read(in))) {
                Hero mage=game.hero(seat);
                MagesStaff staff=mage.belongings.getItem(MagesStaff.class);
                assertTrue(mage.belongings.backpack.contains(staff));
                assertEquals(identity,game.ownership.identity(staff));
                assertEquals(upgrade,staff.level());
                WandOfMagicMissile wand=staff.starterWand();
                assertSame(mage,wand.chargingHero());
                assertEquals(1,mage.buffs(Wand.Charger.class).size());
                assertEquals(chargerId,mage.buff(Wand.Charger.class).id());
                assertEquals(charges,wand.curCharges);
                assertEquals(partial,wand.partialCharge,0f);
                offer(game,seat);
                assertTrue(game.act(game.player(seat),game.turn(),CoopSession.Action.EQUIP,0,staff));
                offer(game,seat);
                Rat rat=(Rat)Actor.findById(enemyId);
                rat.HP=1;
                int exp=mage.exp, untouched=game.hero(1-seat).exp;
                assertTrue(game.act(game.player(seat),game.turn(),CoopSession.Action.ZAP,rat.pos,staff));
                assertFalse(rat.isAlive());
                assertTrue(mage.exp>exp);
                assertEquals(untouched,game.hero(1-seat).exp);
                int depleted=wand.curCharges;
                for(int i=0;i<150 && wand.curCharges==depleted;i++)
                    assertTrue(game.act(game.player(game.activeSeat()),game.turn(),CoopSession.Action.WAIT,0,null));
                assertEquals(depleted+1,wand.curCharges);
            }
        }
    }
}
