package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import java.util.ArrayList;
import static org.junit.Assert.*;

/** Continuous seed on branch zero. Staged stair placement and boosted boss combat. */
public class VaultAscentRoutes extends HeadlessDungeon {
    static void seededRoute(int seed) {
        try (CoopSession game=CavesInteriorTest.open(seed)) {
            try {
                YogArenaTest.arrive(game,seed);
                assertEquals(seed,Dungeon.seed);
                YogArenaTest.start(game);
                ArrayList<String> fists=new ArrayList<>();
                YogArenaTest.fight(game,fists);
                YogArenaTest.awaitExit(game);
                CoopFloorsTest.cross(game);
                assertEquals(26,Dungeon.depth);
                YogArenaTest.takeAmulet(game);
                int owner=game.amuletOwner();
                assertTrue(game.chooseEnding(owner,true));
                assertTrue(game.chooseEnding(1-owner,true));
                assertTrue(game.won());
                System.out.printf("ROUTE\t%d\t26\tordinary victory\t%s%n",seed,String.join(",",fists));
            } catch(AssertionError | RuntimeException refusal) {
                int exit=Dungeon.level.exit();
                for (int seat=0;seat<2;seat++) {
                    Hero h=game.hero(seat);
                    System.out.printf("ROUTE_STOP seat=%d life=%s hp=%d cell=%d exit=%d distance=%d sees=%s rooted=%s paralysis=%d locked=%s%n",
                            seat,game.life(seat),h.HP,h.pos,exit,Dungeon.level.distance(h.pos,exit),
                            game.knowledge(h).visible(exit),h.rooted,h.paralysed,Dungeon.level.locked);
                }
                System.out.printf("ROUTE\t%d\t%d\t%s\t%s%n",seed,Dungeon.depth,
                        refusal.getClass().getSimpleName(),String.valueOf(refusal.getMessage()).replace('\n',' '));
            }
        }
    }

    static void vaultTrial() {
        try (CoopSession game=CavesInteriorTest.open(572)) {
            ImpVaultTest.entrance(game);
            // Discard any earlier boss-fixture health boost before the original vault full heal.
            for(int seat=0;seat<2;seat++) {
                Hero hero=game.hero(seat);
                hero.HTBoost=0;hero.updateHT(false);
            }
            CoopFloorsTest.cross(game);
            VaultLevel level=(VaultLevel)Dungeon.level;
            VaultFinalRoom room=(VaultFinalRoom)level.room(VaultFinalRoom.class);
            int center=level.pointToCell(room.center()),door=level.pointToCell(room.lockedDoor);
            int offset=(door-center)/6,active=game.activeSeat();
            game.hero(active).pos=center+2*offset;
            game.hero(1-active).pos=center-offset;
            game.observe(0);game.observe(1);
            for(int seat=0;seat<2;seat++) {
                Hero h=game.hero(seat);
                System.out.printf("VAULT_START\t%d\t%d\t%d\t%d\t%s%n",seat,h.lvl,h.STR,h.HP,
                        h.belongings.weapon()==null?"unarmed":h.belongings.weapon().getClass().getSimpleName());
            }
            VaultElementalTest.move(game,center+3*offset);
            int actions=1;
            while(VaultElementalTest.boss()!=null && !game.defeated() && !game.stopped() && actions<250) {
                VaultElementalTest.strike(game);actions++;
            }
            VaultBossElemental boss=VaultElementalTest.boss();
            System.out.printf("VAULT_RESULT\t%d\t%s\t%d\t%d\t%d%n",actions,game.defeated()?"party defeat":game.stopReason(),boss==null?0:boss.HP,game.hero(0).HP,game.hero(1).HP);
        }
    }

    public static void main(String[] args) throws Exception {
        VaultAscentRoutes p=new VaultAscentRoutes();p.temporary.create();p.startBackend();
        try {
            if (args.length>0) {
                for (String seed:args) seededRoute(Integer.parseInt(seed));
                return;
            }
            for (int seed=570;seed<=581;seed++) seededRoute(seed);
            vaultTrial();
        } finally {p.stopBackend();p.temporary.delete();}
    }
}
