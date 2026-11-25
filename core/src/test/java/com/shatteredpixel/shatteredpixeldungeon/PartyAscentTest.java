package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class PartyAscentTest extends HeadlessDungeon {
    public static void approach(CoopSession game) {
        YogArenaTest.prepareBoundary(game,"return25");
        CoopFloorsTest.cross(game);
        assertEquals(25,Dungeon.depth);
        atEntrance(game);
        assertEquals(-1,game.stairs.stair());
        assertEquals(Dungeon.level.entrance(),game.stairs.ascentGate());
    }

    private static void atEntrance(CoopSession game) {
        int cell=Dungeon.depth==1 ? Dungeon.level.getTransition(LevelTransition.Type.SURFACE).cell() : Dungeon.level.entrance();
        SewerSpecialActorTest.atGate(game,cell);
    }

    public static String signature(CoopSession game) {
        int owner=game.amuletOwner();
        String state=Dungeon.branch+"/"+Dungeon.depth+"/"+game.turn()+"/"+game.activeSeat()+"/"+game.ending()
                +"/"+Statistics.highestAscent+"/"+Statistics.ascended+"/"+owner
                +"/"+game.ownership.identity(game.hero(owner).belongings.getItem(Amulet.class))
                +"/"+Arrays.toString(com.watabou.utils.Random.worldState());
        AscensionChallenge curse=AscensionChallenge.current();
        if (curse!=null) {
            Bundle b=new Bundle();
            curse.storeInBundle(b);
            state+="/"+curse.id()+"/"+b.getFloat(AscensionChallenge.STACKS)+"/"+b.getFloat(AscensionChallenge.DAMAGE)+"/"+b.getBoolean(AscensionChallenge.STACKS_LOWERED);
        }
        for (int seat=0;seat<2;seat++) state+="/"+game.hero(seat).id()+"/"+game.hero(seat).HP+"/"+game.hero(seat).pos+"/"+game.life(seat);
        return state;
    }

    public static void main(String[] args) throws Exception {
        PartyAscentTest probe=new PartyAscentTest();
        probe.temporary.create();
        probe.startBackend();
        if (args.length==2 && args[0].equals("screens")) {
            try {
                for (String stage:new String[]{"agree","surface"}) {
                    try (CoopSession game=CoopSession.open(578,1,com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR)) {
                        game.join(2,com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.MAGE);
                        prepareBoundary(game,stage);
                        if (stage.equals("surface")) CoopFloorsTest.cross(game);
                        String name=stage.equals("agree")?"ascent-vote":"ascent-surface";
                        try (java.io.OutputStream out=java.nio.file.Files.newOutputStream(java.nio.file.Paths.get(args[1],name))) {
                            assertTrue(Bundle.write(game.checkpoint(),out,false));
                        }
                    }
                }
            } finally { probe.stopBackend(); probe.temporary.delete(); }
            return;
        }
        try (java.io.InputStream in=java.nio.file.Files.newInputStream(java.nio.file.Paths.get(args[0]))) {
            Bundle source=Bundle.read(in);
            assertEquals(14,source.getInt("schema"));
            try (CoopSession game=CoopSession.restore(source)) {
                assertEquals(CoopSession.Ending.OFFER,game.ending());
                int owner=game.amuletOwner();
                assertTrue(game.chooseEnding(owner,false));
                atEntrance(game);
                CoopFloorsTest.cross(game);
                atEntrance(game);
                assertTrue(game.chooseAscent(owner));
                assertTrue(game.chooseAscent(1-owner));
                CoopFloorsTest.cross(game);
                assertEquals(24,Dungeon.depth);
                assertEquals(15,game.checkpoint().getInt("schema"));
                System.out.println("PASS original schema 14, same amulet, two votes, schema 15 at depth 24");
            }
        } finally { probe.stopBackend(); probe.temporary.delete(); }
    }

    public static boolean published(String stage) {
        CoopSession g=Dungeon.coop;
        if (stage.equals("offer")) return g.ending()==CoopSession.Ending.ASCENT_PROPOSED && Dungeon.depth==25;
        if (stage.equals("agree")) return g.ending()==CoopSession.Ending.ASCENT_READY && Dungeon.depth==25;
        if (stage.equals("surface")) return g.ending()==CoopSession.Ending.ASCENDED && Statistics.ascended;
        if (g.ending()!=CoopSession.Ending.ASCENDING || AscensionChallenge.current()==null) return false;
        if (stage.equals("kill")) return Dungeon.depth==24 && AscensionChallenge.current().stacks()<2;
        if (stage.equals("down")) return g.life(g.amuletOwner())==CoopSession.Life.DOWNED;
        if (stage.equals("revive")) return Dungeon.depth==21 && g.life(g.amuletOwner())==CoopSession.Life.ALIVE && g.hero(g.amuletOwner()).HP>0;
        if (stage.equals("damage")) return Dungeon.depth==21 && AscensionChallenge.current().stacks()>=8;
        return Dungeon.depth==Integer.parseInt(stage) && Statistics.highestAscent==Dungeon.depth;
    }

    public static StagedInput prepareBoundary(CoopSession game,String stage) {
        approach(game);
        int owner=game.amuletOwner();
        if (stage.equals("offer")) return new StagedInput(game,"START_ASCENT",0);
        assertTrue(game.chooseAscent(owner));
        if (stage.equals("agree")) return new StagedInput(game,"START_ASCENT",0);
        assertTrue(game.chooseAscent(1-owner));
        while(Dungeon.depth>1) {
            atEntrance(game);
            if(stage.equals(Integer.toString(Dungeon.depth-1))) return new StagedInput(game,"TRAVEL",game.stairs.stair());
            CoopFloorsTest.cross(game);
            if(stage.equals("kill") && Dungeon.depth==24) {
                Mob mob=Dungeon.level.mobs.stream().filter(m->m.alignment==com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY
                        && AscensionChallenge.statModifier(m)>1).findFirst().orElseThrow(AssertionError::new);
                for (int i=0;i<100;i++) {
                    MiningBranchTest.standBeside(game,mob.pos);
                    StagedInput before=new StagedInput(game,"ATTACK",mob.pos);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,mob.pos,null));
                    if (published(stage)) return before;
                }
                throw new AssertionError("No qualified kill");
            }
            if((stage.equals("damage") || stage.equals("down") || stage.equals("revive")) && Dungeon.depth==21) {
                Hero carrier=game.hero(owner), partner=game.hero(1-owner);
                if (!stage.equals("damage")) {
                    com.shatteredpixel.shatteredpixeldungeon.items.Ankh ankh=new com.shatteredpixel.shatteredpixeldungeon.items.Ankh();
                    if (carrier.belongings.getItem(ankh.getClass())==null) {
                        assertTrue(ankh.collect(carrier.belongings.backpack));
                        game.ownership.claim(ankh,game.player(owner));
                    }
                    carrier.HP=stage.equals("revive") ? 30 : 1;
                    if (stage.equals("revive")) {
                        // The down probe uses natural curse damage. Separately stage a lethal blow
                        // so that the original 30% revival can survive the following curse tick.
                        carrier.damage(999,AscensionChallenge.current());
                        game.finishEffects();
                        while (game.activeSeat()==owner) VaultElementalTest.waitTurn(game);
                        assertEquals(CoopSession.Life.DOWNED,game.life(owner));
                        MiningBranchTest.standBeside(game,carrier.pos);
                        return new StagedInput(game,"REVIVE",carrier.pos);
                    }
                }
                for(int i=0;i<40;i++) {
                    StagedInput before=new StagedInput(game,"WAIT",0);
                    int a=carrier.HP,b=partner.HP;
                    VaultElementalTest.waitTurn(game);
                    if (stage.equals("damage") && carrier.HP<a && partner.HP<b) return before;
                    if (stage.equals("down") && game.life(owner)==CoopSession.Life.DOWNED) return before;
                }
                throw new AssertionError("No native curse tick");
            }
        }
        atEntrance(game);
        assertEquals("surface",stage);
        return new StagedInput(game,"TRAVEL",game.stairs.stair());
    }

    @Test public void carrierAndPartnerApproveNativeCurseAndKeepOneAmuletThroughTheSurface() {
        Bundle save;
        long amuletId;
        int owner;
        try (CoopSession game=CavesInteriorTest.open(578)) {
            approach(game);
            owner=game.amuletOwner();
            amuletId=game.ownership.identity(game.hero(owner).belongings.getItem(Amulet.class));
            assertFalse(game.chooseAscent(1-owner));
            assertTrue(game.chooseAscent(owner));
            assertFalse(game.chooseAscent(owner));
            assertTrue(game.chooseEnding(1-owner,false));
            assertEquals(CoopSession.Ending.CONTINUING,game.ending());
            assertTrue(game.chooseAscent(owner));
            save=game.checkpoint();
            assertEquals(15,save.getInt("schema"));
        }
        try (CoopSession game=CoopSession.restore(save)) {
            assertTrue(game.chooseAscent(1-owner));
            assertFalse(game.chooseAscent(1-owner));
            for (int destination=24;destination>=1;destination--) {
                atEntrance(game);
                CoopFloorsTest.cross(game);
                assertEquals("native ascent floor",destination,Dungeon.depth);
                assertEquals(destination,Statistics.highestAscent);
                assertSame(game.hero(owner).buff(AscensionChallenge.class),AscensionChallenge.current());
                assertNull(game.hero(1-owner).buff(AscensionChallenge.class));
                assertEquals(26,Dungeon.scalingDepth());
                if (destination==24) assertEquals(2,AscensionChallenge.current().stacks(),0f);
                if (destination==23) assertEquals(4,AscensionChallenge.current().stacks(),0f);
                if (destination==22) assertEquals(.5f,AscensionChallenge.modifyHeroSpeed(1f),0f);
                assertTrue("next durable boundary",game.checkpoint().getBundle("floor").getKeys().size()>0);
            }
            atEntrance(game);
            CoopFloorsTest.cross(game);
            assertTrue(game.won());
            assertTrue(Statistics.ascended);
            assertEquals(CoopSession.Ending.ASCENDED,game.ending());
            assertFalse(game.chooseEnding(owner,true));
            save=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(save)) {
            assertTrue(game.won());
            assertEquals(CoopSession.Ending.ASCENDED,game.ending());
            assertEquals(amuletId,game.ownership.identity(game.hero(owner).belongings.getItem(Amulet.class)));
            assertFalse(CoopFloorsTest.step(game,CoopSession.Action.WAIT,game.hero(game.activeSeat()).pos,null));
        }
    }
}
