package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.watabou.utils.Bundle;
import static org.junit.Assert.*;

public final class PartyBoundaryProbe extends HeadlessDungeon {
    static void fallenPartner(boolean inside) {
        Bundle saved;int partnerId,cell;
        try(CoopSession g=CavesInteriorTest.open(572)) {
            int step=VaultElementalTest.enter(g),partner=1-g.activeSeat();
            Hero ally=g.hero(partner);
            if(!inside) ally.pos=Dungeon.level.entrance();
            Ankh ankh=new Ankh();
            assertTrue(ankh.collect(ally.belongings.backpack));g.ownership.claim(ankh,g.player(partner));
            partnerId=ally.id();cell=ally.pos;
            ally.HP=30;ally.damage(10000,PartyBoundaryProbe.class);
            assertEquals(CoopSession.Life.DOWNED,g.life(partner));
            VaultElementalTest.move(g,step);
            assertEquals(inside,VaultElementalTest.boss()!=null);
            assertEquals(inside,Dungeon.level.locked);
            saved=g.checkpoint();
        }
        try(CoopSession g=CoopSession.restore(saved)) {
            Hero down=g.hero(0).id()==partnerId?g.hero(0):g.hero(1);
            assertEquals(cell,down.pos);assertEquals(0,down.HP);assertTrue(Dungeon.level.passable[cell]);
            assertEquals(inside,VaultElementalTest.boss()!=null);
            assertEquals(inside,Dungeon.level.locked);
            System.out.println("VAULT_DOWN partner_inside="+inside+" same_actor_cell=true locked="+Dungeon.level.locked);
        }
    }
    static void healing() {
        Bundle saved;
        try(CoopSession g=CavesInteriorTest.open(578)) {
            PartyAscentTest.prepareBoundary(g,"20");
            for(int seat=0;seat<2;seat++) g.hero(seat).HP=g.hero(seat).HT/2;
            CoopFloorsTest.cross(g);assertEquals(20,Dungeon.depth);
            for(int seat=0;seat<2;seat++) assertNotNull(g.hero(seat).buff(Healing.class));
            saved=g.checkpoint();
        }
        try(CoopSession g=CoopSession.restore(saved)) {
            int[] hp={g.hero(0).HP,g.hero(1).HP};
            for(int i=0;i<12 && (g.hero(0).HP<=hp[0] || g.hero(1).HP<=hp[1]);i++) VaultElementalTest.waitTurn(g);
            for(int seat=0;seat<2;seat++) assertTrue(g.hero(seat).HP>hp[seat]);
            System.out.printf("ASCENT_HEAL floor20 %d to %d, %d to %d%n",hp[0],g.hero(0).HP,hp[1],g.hero(1).HP);
        }
    }
    public static void main(String[] args)throws Exception {
        PartyBoundaryProbe p=new PartyBoundaryProbe();p.temporary.create();p.startBackend();
        try {fallenPartner(false);fallenPartner(true);healing();} finally {p.stopBackend();p.temporary.delete();}
    }
}
