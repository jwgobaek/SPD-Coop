package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.Test;
import static org.junit.Assert.*;

/** Staged native boss combat. Initial positions, equipment and hero HP are explicit fixtures. */
public class CavesBossTest extends HeadlessDungeon {
    static CavesBossLevel level() { return (CavesBossLevel)Dungeon.level; }
    static DM300 boss() {
        for (Mob m:level().mobs) if (m instanceof DM300) return (DM300)m;
        return null;
    }
    static Pylon pylon() {
        for (Mob m:level().mobs) if (m instanceof Pylon && m.alignment==Char.Alignment.ENEMY) return (Pylon)m;
        return null;
    }
    static void arrive(CoopSession g) {
        CavesInteriorTest.arrive(g,15);
        assertFalse(level().exitOpen());assertNull(boss());
        for(int seat=0;seat<2;seat++) {
            Hero h=g.hero(seat);
            h.HT=h.HP=1500;
            h.belongings.weapon.upgrade(6);
        }
    }
    static int beside(CoopSession g,int seat,int pos) {
        Hero h=g.hero(seat);
        for(int d:PathFinder.NEIGHBOURS8) {
            int c=pos+d;
            if(!Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c)!=null && Actor.findChar(c)!=h) continue;
            h.pos=c;Dungeon.level.updateFieldOfView(h,h.fieldOfView);g.observe(seat);return c;
        }
        throw new AssertionError("No open adjacent cell "+pos);
    }
    static int entry(CoopSession g) {
        for(int p:CavesBossLevel.pylonPositions) for(int offset:PathFinder.NEIGHBOURS8) {
            int dest=p+offset;
            if(level().passable[dest] && !level().pit[dest] && Actor.findChar(dest)==null) {
                beside(g,g.activeSeat(),dest);return dest;
            }
        }
        throw new AssertionError("No boss entry");
    }
    static void start(CoopSession g) {
        int cell=entry(g);
        assertTrue(CoopFloorsTest.step(g,CoopSession.Action.MOVE,cell,null));
        assertNotNull(boss());assertTrue(level().locked);assertEquals(-1,g.stairs.stair());
        assertNotEquals(g.hero(0).pos,g.hero(1).pos);
    }
    static void strike(CoopSession g,Mob m) {
        beside(g,g.activeSeat(),m.pos);
        assertTrue("Strike "+m.getClass().getSimpleName()+" "+g.stopReason()+" pos="+m.pos+" occ="+Actor.findChar(m.pos)+" adj="+level().adjacent(g.hero(g.activeSeat()).pos,m.pos)+" canSee="+g.hero(g.activeSeat()).fieldOfView[m.pos]+" admitted="+CoopSession.basicMob(m)+" al="+m.alignment+" life="+m.isAlive()+" paral="+g.hero(g.activeSeat()).paralysed,
                CoopFloorsTest.step(g,CoopSession.Action.ATTACK,m.pos,null));
    }
    static void fight(CoopSession g) {
        int strikes=0;
        while (boss()!=null && strikes++<350) {
            Mob target=pylon()==null?boss():pylon();
            strike(g,target);
        }
        assertNull(boss());
        assertFalse(level().locked);assertTrue(level().exitOpen());
    }
    public static int prepareBoundary(CoopSession g,String name) {
        if(name.equals("visit")) {
            CavesInteriorTest.arrive(g,14);SewerSpecialActorTest.atGate(g,Dungeon.level.exit());return Dungeon.level.exit();
        }
        arrive(g);
        if(name.equals("enter")) return entry(g);
        start(g);
        DM300 dm=boss();
        if(name.equals("devices") || name.equals("down") || name.equals("revive")) {
            for(int n=0;n<100 && !dm.supercharged;n++) strike(g,dm);
            assertNotNull(pylon());
            Hero ally=g.hero(g.activeSeat()),victim=g.hero(1-g.activeSeat());
            beside(g,1-g.activeSeat(),ally.pos);
            dm.fieldOfView=new boolean[level().length()];level().updateFieldOfView(dm,dm.fieldOfView);
            if(name.equals("devices")) {
                dm.ventGas(victim);
                // A distant aim point avoids leaving a visual push callback in the setup checkpoint.
                if(level().distance(dm.pos,victim.pos)<3) {
                    for(int i=0;i<level().length();i++) if(level().passable[i] && level().distance(dm.pos,i)>=4 && Actor.findChar(i)==null) {
                        victim.pos=i;g.observe(1-g.activeSeat());break;
                    }
                }
                dm.dropRocks(victim);
            } else {
                assertTrue(new Ankh().collect(victim.belongings.backpack));
                victim.HP=17;victim.damage(999,DM300.class);g.observe(g.activeSeat());
                assertEquals(CoopSession.Life.DOWNED,g.life(1-g.activeSeat()));
            }
            return victim.pos;
        }
        if(name.equals("reward") || name.equals("return") || name.equals("revisit")) {
            fight(g);
            if(!name.equals("reward")) {
                if(name.equals("revisit")) {
                    CityEntranceTest.stageAtStair(g,Dungeon.level.entrance());
                    CoopFloorsTest.cross(g);
                }
                int dest=name.equals("revisit")?levelExit():Dungeon.level.entrance();
                CityEntranceTest.stageAtStair(g,dest);return dest;
            }
            for(Heap heap:level().heaps.valueList()) if(heap.peek() instanceof MetalShard) {
                int seat=g.activeSeat();
                if (g.hero(1-seat).pos==heap.pos) beside(g,1-seat,heap.pos);
                g.hero(seat).pos=heap.pos;
                assertNotEquals(g.hero(1-seat).pos,g.hero(seat).pos);
                g.observe(seat);
                return heap.pos;
            }
            throw new AssertionError("No shards");
        }
        Mob target=dm;
        if(name.equals("charge")) dm.HP=dm.HT/3*2+1;
        else {
            while(!dm.supercharged) strike(g,dm);
            if(name.equals("pylon")) {target=pylon();target.HP=1;}
            else {
                while(dm.pylonsActivated<2 || dm.supercharged) strike(g,pylon()==null?dm:pylon());
                dm.HP=1;
            }
        }
        beside(g,0,target.pos);beside(g,1,target.pos);
        return target.pos;
    }
    private static int levelExit() { return Dungeon.level.exit(); }

    public static boolean published(String name) {
        if(name.equals("return")) return Dungeon.depth==14;
        if(Dungeon.depth!=15) return false;
        DM300 dm=boss();
        switch(name) {
        case "visit": return dm==null && !level().exitOpen();
        case "enter": return dm!=null && level().locked;
        case "charge": return dm!=null && dm.supercharged && pylon()!=null;
        case "pylon": return dm!=null && !dm.supercharged && dm.pylonsActivated==1 && pylon()==null;
        case "devices": return dm!=null && pylon()!=null && level().blobs.get(ToxicGas.class)!=null;
        case "down": return gDown();
        case "revive": return !gDown();
        case "reward": return level().exitOpen() && (Dungeon.coop.hero(0).belongings.getItem(MetalShard.class)!=null || Dungeon.coop.hero(1).belongings.getItem(MetalShard.class)!=null);
        default: return level().exitOpen() && dm==null && shards()>=2;
        }
    }
    private static boolean gDown() {return Dungeon.coop.life(0)==CoopSession.Life.DOWNED || Dungeon.coop.life(1)==CoopSession.Life.DOWNED;}

    public static String signature(CoopSession g) {
        String base=CavesInteriorTest.signature(g);
        if(Dungeon.depth!=15) return base;
        java.util.ArrayList<String> extra=new java.util.ArrayList<>();
        for(Mob m:level().mobs) {
            Bundle b=new Bundle();m.storeInBundle(b);
            if(m instanceof Pylon) extra.add(m.id()+"/"+m.alignment+"/"+b.getInt("target_neighbour"));
            if(m instanceof DM300) {
                extra.add(m.id()+"/"+b.getBoolean("supercharged")+"/"+b.getInt("pylons_activated")+"/"+b.getInt("turns_since_last_ability")+"/"+b.getInt("ability_cooldown"));
                for(DM300.FallingRockBuff rocks:m.buffs(DM300.FallingRockBuff.class)) {
                    Bundle data=new Bundle();rocks.storeInBundle(data);extra.add("rocks/"+rocks.id()+"/"+rocks.cooldown()+"/"+java.util.Arrays.hashCode(data.getIntArray("positions")));
                }
            }
        }
        for(DM300.PendingGas pending:level().pendingGas()) {
            Bundle data=new Bundle();pending.storeInBundle(data);extra.add("vent/"+pending.id()+"/"+pending.cooldown()+"/"+data.getInt("origin")+"/"+data.getInt("impact")+"/"+java.util.Arrays.hashCode(data.getIntArray("cells")));
        }
        Blob energy=level().blobs.get(CavesBossLevel.PylonEnergy.class);
        if(energy!=null && energy.volume>0) extra.add("energy/"+energy.id()+"/"+java.util.Arrays.hashCode(energy.cur));
        for(int seat=0;seat<2;seat++) {
            MetalShard shard=g.hero(seat).belongings.getItem(MetalShard.class);
            if(shard!=null) extra.add("shard/"+seat+"/"+g.ownership.identity(shard)+"/"+g.ownership.owner(shard)+"/"+shard.quantity());
        }
        java.util.Collections.sort(extra);
        return base+"/"+extra+"/"+shards()+"/"+level().exitOpen()+"/"+Statistics.bossScores[2];
    }

    public static void forScreen(CoopSession g,String phase) {
        if(phase.equals("DM_START")) {
            for(int seat=0;seat<2;seat++) {g.hero(seat).HP=g.hero(seat).HT=1500;g.hero(seat).belongings.weapon.upgrade(6);}
            start(g);beside(g,0,boss().pos);
        } else if(phase.equals("DM_CHARGE")) {
            for(int i=0;i<120 && !boss().supercharged;i++) strike(g,boss());
            assertNotNull(pylon());beside(g,0,pylon().pos);
        } else {
            fight(g);
            for(Heap h:level().heaps.valueList()) if(h.peek() instanceof MetalShard) {beside(g,0,h.pos);break;}
        }
        g.observe(0);g.observe(1);
    }

    static int shards() {
        return CoopSession.floorItems(level()).values().stream().filter(MetalShard.class::isInstance).mapToInt(Item::quantity).sum();
    }

    @Test public void eitherSeatStartsAndNativeCombatDestroysTwoPylonsAndUnlocksExitOnce() {
        for (int startSeat=0;startSeat<2;startSeat++) {
            Bundle won;int quantity;
            try(CoopSession g=CavesInteriorTest.open(570)) {
                arrive(g);
                while(g.activeSeat()!=startSeat) TenguArenaTest.waitOne(g);
                start(g);
                for(int i=0;i<80 && !boss().supercharged;i++) strike(g,boss());
                assertTrue(boss().supercharged);
                assertNotNull(pylon());assertEquals(1,boss().pylonsActivated);
                int before=boss().HP;strike(g,boss());assertEquals(before,boss().HP);
                Blob energy=level().blobs.get(CavesBossLevel.PylonEnergy.class);
                assertNotNull(energy);assertTrue(energy.volume>0);
                fight(g);quantity=shards();assertTrue(quantity>=2 && quantity<=4);
                assertEquals(2,level().mobs.stream().filter(Pylon.class::isInstance).count());
                won=g.checkpoint();assertEquals(9,won.getInt("schema"));
            }
            try(CoopSession g=CoopSession.restore(won)) {
                assertTrue(level().exitOpen());assertEquals(quantity,shards());
                int entrance=Dungeon.level.entrance();
                SewerSpecialActorTest.atGate(g,entrance);
                // Charged cells and rockfall can leave a surviving hero immobilized at victory.
                for (int waits=0;g.stairs.stair()!=entrance && waits<20;waits++) {
                    assertTrue("Expected native movement restriction after the restored fight",
                            g.hero(0).paralysed>0 || g.hero(1).paralysed>0 || g.hero(0).rooted || g.hero(1).rooted);
                    assertFalse(g.stairs.consent(1-g.activeSeat(),g.turn(),entrance,false));
                    TenguArenaTest.waitOne(g);
                }
                assertEquals(entrance,g.stairs.stair());
                CoopFloorsTest.cross(g);assertEquals(14,Dungeon.depth);
                SewerDepthTest.gate(g,true);assertNull(boss());assertEquals(quantity,shards());
                int cell=-1;
                for(int pos:Dungeon.level.heaps.keyArray()) if(Dungeon.level.heaps.get(pos).peek() instanceof MetalShard) {cell=pos;break;}
                assertTrue(cell>=0);int seat=g.activeSeat();
                g.hero(seat).pos=cell;g.observe(seat);
                MetalShard item=(MetalShard)Dungeon.level.heaps.get(cell).peek();long id=g.ownership.identity(item);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.PICK_UP,cell,item));
                assertEquals(id,g.ownership.identity(item));assertEquals(g.player(seat),g.ownership.owner(item));
            }
        }
    }

    @Test public void delayedGasAndRockCellsKeepTheirIdsAndRunAtOriginalTimesAfterRestore() {
        Bundle pending;int actor,rock,origin;
        try(CoopSession g=CavesInteriorTest.open(570)) {
            arrive(g);start(g);
            Hero target=g.hero(1);assertTrue(level().distance(target.pos,boss().pos)>2);
            origin=target.pos;
            boss().fieldOfView=new boolean[level().length()];level().updateFieldOfView(boss(),boss().fieldOfView);
            boss().ventGas(target);
            boss().dropRocks(target);
            DM300.PendingGas gas=level().pendingGas().get(0);actor=gas.id();
            rock=boss().buff(DM300.FallingRockBuff.class).id();
            pending=g.checkpoint();
        }
        try(CoopSession g=CoopSession.restore(pending)) {
            assertEquals(actor,level().pendingGas().get(0).id());
            assertEquals(rock,boss().buff(DM300.FallingRockBuff.class).id());
            for(int i=0;i<20 && (Actor.findById(actor)!=null || Actor.findById(rock)!=null);i++) TenguArenaTest.waitOne(g);
            assertNull(Actor.findById(actor));assertNull(Actor.findById(rock));
            assertTrue(Blob.volumeAt(origin,ToxicGas.class)>0);
        }
    }
}
