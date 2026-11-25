package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;

/** Seeded encounter. Heroes have their staged DM-300 loadout and are placed at each target. */
public class CityKingTest extends HeadlessDungeon {
    static void reach(CoopSession game,int depth) {
        CityEntranceTest.win(game);
        while(Dungeon.depth<depth) {
            CityEntranceTest.stageAtExit(game);
            CoopFloorsTest.cross(game);
        }
    }
    static void arrive(CoopSession game) {
        reach(game,20);
        assertFalse(((CityBossLevel)Dungeon.level).exitOpen());
        for (int seat=0;seat<2;seat++) {
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero fighter=game.hero(seat);
            while (fighter.lvl<20) fighter.earnExp(fighter.maxExp(), CityKingTest.class);
            fighter.STR=18;
            fighter.belongings.armor.upgrade(6);
            game.hero(seat).HTBoost=1500-20-5*(game.hero(seat).lvl-1);
            game.hero(seat).updateHT(false);
            game.hero(seat).HP=game.hero(seat).HT;
        }
    }
    static DwarfKing boss() {
        for(Mob m:Dungeon.level.mobs) if(m instanceof DwarfKing) return (DwarfKing)m;
        return null;
    }
    static int entry(CoopSession game) {
        int door=CityBossLevel.arenaDoor();
        game.hero(game.activeSeat()).pos=door;
        game.observe(game.activeSeat());
        return door-Dungeon.level.width();
    }
    static Mob subject() {
        for(Mob subject:Dungeon.level.mobs)
            if (subject.buff(DwarfKing.KingDamager.class)!=null && subject.isAlive()) return subject;
        return null;
    }
    static void strike(CoopSession game,Mob target) {
        MiningBranchTest.standBeside(game,target.pos);
        assertTrue(game.stopReason()+" "+target,CoopFloorsTest.step(game,CoopSession.Action.ATTACK,target.pos,null));
    }
    static int crown() {
        for(Heap heap:Dungeon.level.heaps.valueList()) if(heap.peek() instanceof KingsCrown) return heap.pos;
        return -1;
    }
    public static boolean published(String stage) {
        if (stage.equals("return")) return Dungeon.branch==0 && Dungeon.depth==19;
        if(Dungeon.depth!=20) return false;
        if (stage.equals("visit")) return !Dungeon.level.locked && boss()==null && !((CityBossLevel)Dungeon.level).exitOpen();
        if (stage.equals("revisit")) return ((CityBossLevel)Dungeon.level).exitOpen() && boss()==null;
        DwarfKing king=boss();
        if (stage.equals("down")) return Dungeon.level.locked && (Dungeon.coop.life(0)==CoopSession.Life.DOWNED || Dungeon.coop.life(1)==CoopSession.Life.DOWNED);
        if (stage.equals("revive")) return Dungeon.level.locked && Dungeon.coop.life(0)==CoopSession.Life.ALIVE && Dungeon.coop.life(1)==CoopSession.Life.ALIVE;
        if (stage.equals("claim")) {
            for (int seat=0;seat<2;seat++) if (Dungeon.coop.hero(seat).belongings.getItem(KingsCrown.class)!=null) return true;
            return false;
        }
        if (stage.equals("win")) return ((CityBossLevel)Dungeon.level).exitOpen() && crown()>=0 && king==null;
        if (king==null) return false;
        switch(stage) {
            case "enter": return Dungeon.level.locked;
            case "summon": return !king.buffs(DwarfKing.Summoning.class).isEmpty();
            case "link": return !king.buffs(LifeLink.class).isEmpty();
            case "shield": return king.phase()==2 && king.shielding()>0;
            case "minion": return king.phase()==2 && subject()!=null;
            case "deferred": return king.phase()==3 && king.buff(com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity.DeferedDamage.class)!=null;
            default: throw new IllegalArgumentException(stage);
        }
    }
    public static StagedInput prepareBoundary(CoopSession game,String stage) {
        if (stage.equals("visit") || stage.equals("return") || stage.equals("revisit")) {
            if (stage.equals("visit")) reach(game,19);
            else {
                prepareBoundary(game,"claim");
                if (stage.equals("revisit")) SewerDepthTest.gate(game,false);
            }
            int gate=stage.equals("return") ? Dungeon.level.entrance() : Dungeon.level.exit();
            SewerSpecialActorTest.atGate(game,gate);
            return new StagedInput(game,"TRAVEL",gate);
        }
        arrive(game);
        int cell=entry(game);
        StagedInput input=new StagedInput(game,"MOVE",cell);
        assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,cell,null));
        if (stage.equals("down") || stage.equals("revive")) {
            int active=game.activeSeat();
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero survivor=game.hero(active),ally=game.hero(1-active);
            MiningBranchTest.standBeside(game,ally.pos);
            assertTrue(new com.shatteredpixel.shatteredpixeldungeon.items.Ankh().collect(ally.belongings.backpack));
            ally.HP=17;ally.damage(999,DwarfKing.class);
            assertEquals(17,game.preDeathHP(1-active));
            assertTrue(game.knowledge(survivor).visible(ally.pos));
            return new StagedInput(game,stage.equals("down")?"WAIT":"REVIVE",stage.equals("down")?0:ally.pos);
        }
        for (int i=0;i<500 && !published(stage);i++) {
            DwarfKing king=boss();
            if(king==null) {
                cell=crown();
                assertTrue(cell>=0);
                game.hero(game.activeSeat()).pos=cell;game.observe(game.activeSeat());
                input=new StagedInput(game,"PICK_UP",cell);
                assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,Dungeon.level.heaps.get(cell).peek()));
            } else {
                Mob target=king.phase()==2 ? subject() : king;
                if(target==null || king.phase()==1 && king.buffs(LifeLink.class).isEmpty() && i<30) {
                    input=new StagedInput(game,"WAIT",0);TenguArenaTest.waitOne(game);
                } else {
                    MiningBranchTest.standBeside(game,target.pos);
                    input=new StagedInput(game,"ATTACK",target.pos);
                    assertTrue(CoopFloorsTest.step(game,CoopSession.Action.ATTACK,target.pos,null));
                }
            }
        }
        assertTrue(stage,published(stage));
        return input;
    }
    public static String signature(CoopSession game) {
        StringBuilder value=new StringBuilder(MiningBranchTest.signature(game));
        value.append('/').append(Dungeon.level.locked).append('/').append(crown());
        java.util.TreeSet<String> actors=new java.util.TreeSet<>();
        for (Mob m:Dungeon.level.mobs) {
            actors.add(m.id()+"/"+m.getClass().getSimpleName()+"/"+m.pos+"/"+m.HP+"/"+m.shielding());
            for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff:m.buffs()) {
                String detail=buff instanceof LifeLink ? "/"+((LifeLink)buff).object :
                        buff instanceof DwarfKing.Summoning ? "/"+((DwarfKing.Summoning)buff).getPos() : "";
                actors.add(m.id()+"/"+buff.id()+"/"+buff.getClass().getSimpleName()+detail);
            }
        }
        return value+"/"+actors+"/"+(boss()==null ? 0 : boss().phase());
    }
    @Test public void sealedArenaRejectsMissingKingOrInvalidPhase() {
        try (CoopSession game=CavesInteriorTest.open(572)) {
            arrive(game);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,entry(game),null));
            Bundle floor=game.checkpoint().getBundle("floor");
            Bundle record=null;
            for (Bundle mob:floor.getBundleArray("mobs"))
                if (DwarfKing.class.getName().equals(mob.getString("__className"))) record=mob;
            assertNotNull(record);
            record.put("phase",4);
            assertThrows(IllegalArgumentException.class,()->CoopFloors.checkLevelType(floor,20,0));
            record.put("phase",1);floor.put("locked",false);
            assertThrows(IllegalArgumentException.class,()->CoopFloors.checkLevelType(floor,20,0));
        }
    }

    @Test public void nativeThreePhasesAndCrownAfterRestore() {
        CoopSession game=CavesInteriorTest.open(572);
        try {
            arrive(game);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.MOVE,entry(game),null));
            assertNotNull(boss());assertTrue(Dungeon.level.locked);
            assertNotEquals(game.hero(0).pos,game.hero(1).pos);
            int identity=boss().id();
            int phases=0; boolean pending=false,linked=false,invulnerable=false;
            for (int i=0;i<500 && boss()!=null;i++) {
                assertFalse(game.stopReason(),game.stopped());
                assertFalse("Party defeated at turn "+i+" phase "+boss().phase(),game.defeated());
                DwarfKing king=boss();
                int phase=king.phase();
                if (i%40==0) System.out.println("city_king turn="+i+" phase="+phase+" shield="+king.shielding()+" residents="+Dungeon.level.mobs.size()+" hp="+game.hero(0).HP+","+game.hero(1).HP);
                boolean summon=!king.buffs(DwarfKing.Summoning.class).isEmpty();
                boolean link=!king.buffs(LifeLink.class).isEmpty();
                if ((phases & (1<<phase))==0 || summon && !pending || link && !linked) {
                    Bundle saved=game.checkpoint();
                    game.close();game=CoopSession.restore(saved);
                    king=boss();assertEquals(identity,king.id());assertEquals(phase,king.phase());
                    assertEquals(summon,!king.buffs(DwarfKing.Summoning.class).isEmpty());
                    assertEquals(link,!king.buffs(LifeLink.class).isEmpty());
                }
                pending|=summon;linked|=link;phases|=1<<phase;
                if (phase==2) {
                    invulnerable|=king.isInvulnerable(game.hero(0).getClass());
                    Mob minion=subject();
                    if (minion==null) TenguArenaTest.waitOne(game);
                    else strike(game,minion);
                } else if (phase==1 && !linked && i<30) TenguArenaTest.waitOne(game);
                else strike(game,king);
            }
            assertNull(boss());assertEquals(14,phases);assertTrue(invulnerable);assertTrue(pending);assertTrue(linked);
            int cell=crown();assertTrue(cell>=0);
            KingsCrown prize=(KingsCrown)Dungeon.level.heaps.get(cell).peek();
            int seat=game.activeSeat();
            game.hero(seat).pos=cell;game.observe(seat);
            assertTrue(CoopFloorsTest.step(game,CoopSession.Action.PICK_UP,cell,prize));
            long id=game.ownership.identity(prize);
            assertEquals(game.player(seat),game.ownership.owner(prize));
            Bundle saved=game.checkpoint();
            assertEquals(11,saved.getInt("schema"));
            game.close();game=CoopSession.restore(saved);
            assertEquals(id,game.ownership.identity(game.hero(seat).belongings.getItem(KingsCrown.class)));
            SewerDepthTest.gate(game,false);SewerDepthTest.gate(game,true);
            assertTrue(((CityBossLevel)Dungeon.level).exitOpen());assertNull(boss());assertEquals(-1,crown());
            TenguArenaTest.waitOne(game);
        } finally { if (Dungeon.coop==game) game.close(); }
    }
}
