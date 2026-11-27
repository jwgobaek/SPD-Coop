package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.lan.ProjectionChecks;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CavesInteriorTest extends HeadlessDungeon {
    static void arrive(CoopSession game,int depth) {
        TenguArenaTest.prepareSocket(game,"caves");
        while (Dungeon.depth<depth) SewerDepthTest.gate(game,true);
    }

    static CoopSession open(int seed) {
        CoopSession g=CoopSession.open(seed,11,HeroClass.WARRIOR);
        g.join(22,HeroClass.MAGE);
        return g;
    }

    @Test public void originalsSurviveThirteenDormantFloorsAndAnOlderSchemaRejectsTheNewDepth() {
        for (int seed:new int[]{570,571}) {
            Bundle saved;
            int[] ids;
            try (CoopSession g=open(seed)) {
                arrive(g,12);
                assertTrue(Dungeon.level instanceof CavesLevel);
                assertTrue(Dungeon.level.mobs.stream().anyMatch(Spinner.class::isInstance));
                assertEquals(seed==571,Dungeon.level.mobs.stream().anyMatch(Blacksmith.class::isInstance));
                while (Dungeon.depth<14) SewerDepthTest.gate(g,true);
                ids=Dungeon.level.mobs.stream().mapToInt(Mob::id).sorted().toArray();
                saved=g.checkpoint();
                assertEquals(8,saved.getInt("schema"));
            }
            try (CoopSession g=CoopSession.restore(saved)) {
                assertArrayEquals(ids,Dungeon.level.mobs.stream().mapToInt(Mob::id).sorted().toArray());
                for(int i=0;i<3;i++) SewerDepthTest.gate(g,false);
                assertEquals(11,Dungeon.depth);
                for (int id:ids) assertNull(Actor.findById(id));
                while(Dungeon.depth<14) SewerDepthTest.gate(g,true);
                for (int id:ids) assertNotNull(Actor.findById(id));
                TenguArenaTest.waitOne(g);
            }
            saved.put("schema",7);
            assertThrows(IllegalArgumentException.class,()->CoopSession.restore(saved));
        }
    }

    @Test public void armoredBruteKeepsItsOriginalRageArmorLootAndIdentityAcrossRestore() {
        Bundle save;
        int id;
        try (CoopSession game=open(578)) {
            arrive(game,13);
            ArmoredBrute brute=new ArmoredBrute();
            brute.pos=Dungeon.level.entrance();
            com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(brute,2000);
            brute=stage(game,ArmoredBrute.class,0);
            assertTrue(CoopSession.basicMob(brute));
            assertEquals("ArmoredBrute",ProjectionChecks.seatImage(game,0).monsters.get(brute.pos));
            assertTrue(brute.drRoll()>=4);
            com.shatteredpixel.shatteredpixeldungeon.items.Item loot=brute.createLoot();
            assertTrue(loot instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor
                    || loot instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor);
            brute.damage(brute.HP+5,game.hero(0));
            assertTrue(brute.isAlive());
            assertEquals(0,brute.HP);
            assertEquals(brute.HT/2+1,brute.buff(ArmoredBrute.ArmoredRage.class).shielding());
            id=brute.id();
            save=game.checkpoint();
        }
        try (CoopSession game=CoopSession.restore(save)) {
            ArmoredBrute brute=(ArmoredBrute)Actor.findById(id);
            assertTrue(brute.isAlive());
            ArmoredBrute.ArmoredRage rage=brute.buff(ArmoredBrute.ArmoredRage.class);
            int before=rage.shielding();
            rage.act();
            assertEquals(before-1,rage.shielding());
            brute.damage(before+10,game.hero(1));
            assertFalse(brute.isAlive());
            assertNotNull(game.checkpoint());
        }
    }

    static <T extends Mob> T stage(CoopSession g,Class<T> type,int seat) {
        Level l=Dungeon.level;
        T mob=type.cast(l.mobs.stream().filter(type::isInstance).findFirst().orElseThrow(AssertionError::new));
        int center=l.width()*(l.height()/2)+l.width()/2;
        for(Mob m:new ArrayList<>(l.mobs)) if (m!=mob) {Actor.remove(m);l.mobs.remove(m);}
        for (int y=-4;y<=4;y++) for (int x=-6;x<=6;x++) {
            int c=center+y*l.width()+x;
            Level.set(c,Terrain.EMPTY);l.traps.remove(c);l.plants.remove(c);l.discoverable[c]=true;
        }
        mob.pos=center;
        Hero target=g.hero(seat),other=g.hero(1-seat);
        target.pos=center+3;other.pos=center-5-l.width()*3;
        target.HT=target.HP=other.HT=other.HP=500;
        mob.aggro(target);
        g.observe(0);g.observe(1);
        return mob;
    }

    public static int prepareEncounter(CoopSession g,String boundary,int seat) {
        boolean spider=boundary.equals("web");
        arrive(g,spider?12:13);
        Mob mob=spider?stage(g,Spinner.class,seat):stage(g,DM200.class,seat);
        if (!spider) {
            // An impassably narrow passage for the LARGE DM200, with an open gas trajectory.
            for (int offset:PathFinder.NEIGHBOURS8) if (offset!=1) Level.set(mob.pos+offset,Terrain.WALL);
            // Persist the geometry across fresh-process staging.
            for (int y:new int[]{-1,1}) for (int x=1;x<=3;x++)
                Level.set(mob.pos+y*Dungeon.level.width()+x,Terrain.WALL);
        }
        return g.hero(seat).pos;
    }

    public static int prepareSocket(CoopSession g,String name) {
        if (name.startsWith("web") || name.startsWith("gas")) {
            String kind=name.substring(0,3);
            return prepareEncounter(g,kind,name.endsWith("1")?1:0);
        }
        if (name.equals("smith")) {
            arrive(g,13);
            Blacksmith smith=SewerSpecialActorTest.actor(Blacksmith.class);
            Hero hero=g.hero(g.activeSeat());
            for(int d:PathFinder.NEIGHBOURS8) if (Dungeon.level.passable[smith.pos+d] && Actor.findChar(smith.pos+d)==null) {hero.pos=smith.pos+d;break;}
            g.observe(g.activeSeat());
            return smith.pos;
        }
        int start=Integer.parseInt(name)-1;
        arrive(g,start);
        SewerSpecialActorTest.atGate(g,Dungeon.level.exit());
        return Dungeon.level.exit();
    }

    public static boolean committed(String name) {
        if (name.startsWith("web")) return Dungeon.level.blobs.get(Web.class)!=null;
        if (name.startsWith("gas")) return Dungeon.level.blobs.get(ToxicGas.class)!=null;
        if (name.equals("smith")) return Blacksmith.Quest.given();
        return Dungeon.depth==Integer.parseInt(name);
    }

    public static String signature(CoopSession g) {
        StringBuilder text=new StringBuilder(TenguArenaTest.signature(g));
        for(Class<? extends Blob> type:Arrays.asList(Web.class,ToxicGas.class)) {
            Blob b=Dungeon.level.blobs.get(type);
            if(b!=null && b.volume>0) text.append('/').append(type.getSimpleName()).append('/').append(b.id())
                    .append('/').append(b.cooldown()).append('/').append(Arrays.hashCode(b.cur));
        }
        for (int seat=0;seat<2;seat++) {
            Pickaxe pick=g.hero(seat).belongings.getItem(Pickaxe.class);
            if(pick!=null) text.append("/pick/").append(seat).append('/').append(g.ownership.identity(pick)).append('/').append(g.ownership.owner(pick));
        }
        return text.toString();
    }

    @Test public void actualWebAndGasTargetBothSeatsAndPersistAfterDormancy() {
        for(String kind:new String[]{"web","gas"}) for (int seat=0;seat<2;seat++) {
            Bundle dormant;
            int id,blobId,cd;
            Class<? extends Blob> type=kind.equals("web")?Web.class:ToxicGas.class;
            String key=kind.equals("web")?"web_cooldown":"vent_cooldown";
            try (CoopSession g=open(570)) {
                prepareEncounter(g,kind,seat);
                Mob m=Dungeon.level.mobs.iterator().next();
                id=m.id();
                for(int i=0;i<80 && Dungeon.level.blobs.get(type)==null;i++) TenguArenaTest.waitOne(g);
                Blob b=Dungeon.level.blobs.get(type);
                assertNotNull(kind+" seat "+seat+" depth="+Dungeon.depth+" actor="+m.getClass()+" state="+m.state+" enemy="+m.isTargeting(g.hero(seat))+" pos="+m.pos+" target="+g.hero(seat).pos+" canSee="+m.fieldOfView[g.hero(seat).pos],b);
                assertTrue(b.volume>0);
                assertTrue(m.isTargeting(g.hero(seat)));
                Bundle data=new Bundle();m.storeInBundle(data);
                cd=data.getInt(key);
                assertTrue(cd>0);
                blobId=b.id();
                if (kind.equals("web")) {
                    assertTrue(data.getInt("last_enemy_pos")>=0);
                    Hero target=g.hero(seat);
                    int cell=-1;
                    for(int i=0;i<b.cur.length;i++) if (b.cur[i]>0 && Actor.findChar(i)==null) {cell=i;break;}
                    assertTrue(cell>=0);
                    target.pos=cell;Dungeon.level.occupyCell(target);
                    assertNotNull(target.buff(Roots.class));
                    assertNull(g.hero(1-seat).buff(Roots.class));
                    for(int i=0;i<60 && target.buff(Poison.class)==null;i++) m.attackProc(target,5);
                    assertNotNull(target.buff(Poison.class));
                    assertSame(m.FLEEING,m.state);
                    assertNull(g.hero(1-seat).buff(Poison.class));
                    target.buff(Roots.class).detach();
                } else assertTrue(Blob.volumeAt(g.hero(seat).pos,ToxicGas.class)>0);
                SewerDepthTest.gate(g,false);
                assertNull(Actor.findById(id));
                dormant=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(dormant)) {
                SewerDepthTest.gate(g,true);
                assertTrue(Actor.findById(id) instanceof Mob);
                assertEquals(blobId,Dungeon.level.blobs.get(type).id());
                TenguArenaTest.waitOne(g);
            }
        }
    }

    @Test public void smithGivesOneBoundPickAndEnablesTheOriginalBranch() {
        for(int seat=0;seat<2;seat++) {
            Bundle saved;long id;
            try(CoopSession g=open(571)) {
                arrive(g,12);
                Blacksmith smith=SewerSpecialActorTest.actor(Blacksmith.class);
                while(g.activeSeat()!=seat) TenguArenaTest.waitOne(g);
                Hero hero=g.hero(seat);
                for(int d:PathFinder.NEIGHBOURS8) if (Dungeon.level.passable[smith.pos+d] && Actor.findChar(smith.pos+d)==null) {hero.pos=smith.pos+d;break;}
                g.observe(seat);
                assertTrue(CoopFloorsTest.step(g,CoopSession.Action.ACCEPT_BLACKSMITH,smith.pos,null));
                Pickaxe pick=hero.belongings.getItem(Pickaxe.class);
                assertNotNull(pick);id=g.ownership.identity(pick);
                assertEquals(g.playerOf(hero),g.ownership.owner(pick));
                assertFalse(Blacksmith.Quest.acceptFor(g.hero(1-seat)));
                LevelTransition mine=Dungeon.level.getTransition(LevelTransition.Type.BRANCH_EXIT);
                assertNotNull(mine);SewerSpecialActorTest.atGate(g,mine.cell());
                assertEquals(mine.cell(),g.stairs.stair());
                saved=g.checkpoint();
            }
            try(CoopSession g=CoopSession.restore(saved)) {
                assertTrue(Blacksmith.Quest.given());
                assertEquals(id,g.ownership.identity(g.hero(seat).belongings.getItem(Pickaxe.class)));
                assertNull(g.hero(1-seat).belongings.getItem(Pickaxe.class));
            }
        }
    }
}
