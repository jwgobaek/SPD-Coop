package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.lan.CoopController;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.*;

/** Test arrangement only. Run on the existing room's owner; gestures and snapshots still use TCP. */
final class RenderEncounter {
    static void arrange(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        ((ExecutorService)executor.get(room)).submit(RenderEncounter::setup).get(5,TimeUnit.SECONDS);
    }
    // Rendering-only fixture. The independent-JVM route test walks to these same native stairs.
    static void atExit(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field f=room.getClass().getDeclaredField("owner"); f.setAccessible(true);
        ((ExecutorService)f.get(room)).submit(()->{
            CoopSession game=Dungeon.coop;
            Level l=Dungeon.level;
            int gate=l.exit(),i=0;
            for(int off:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                int pos=gate+off;
                if(!l.passable[pos] || l.pit[pos] || l.findMob(pos)!=null || l.traps.get(pos)!=null || l.plants.get(pos)!=null) continue;
                game.hero(i++).pos=pos;
                if(i==2) break;
            }
            if(i!=2) throw new AssertionError("Missing stair approach");
        }).get(5,TimeUnit.SECONDS);
    }

    /** Prepare actual encounters for a screenshot without putting a geometry or HP shortcut in the game. */
    static void tengu(CoopController controller,String destination) throws Exception {
        Field host=CoopController.class.getDeclaredField("host");
        host.setAccessible(true);
        Object room=host.get(controller);
        Field owner=room.getClass().getDeclaredField("owner");
        owner.setAccessible(true);
        ((ExecutorService)owner.get(room)).submit(()->{
            CoopSession game=Dungeon.coop;
            if(destination.startsWith("mine-") || destination.equals("city-market")) {
                com.shatteredpixel.shatteredpixeldungeon.MiningBranchTest.forScreen(game,destination);
            } else if (destination.startsWith("DM_")) {
                com.shatteredpixel.shatteredpixeldungeon.CavesBossTest.forScreen(game,destination);
            } else if (destination.equals("FIGHT_START")) {
                for (int seat=0;seat<2;seat++) {
                    com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero=game.hero(seat);
                    hero.HT=hero.HP=500;
                    hero.belongings.weapon().upgrade(5);
                }
                com.shatteredpixel.shatteredpixeldungeon.TenguArenaTest.enter(game,false);
            } else if (destination.equals("FIGHT_ARENA")) {
                com.shatteredpixel.shatteredpixeldungeon.TenguArenaTest.fight(game,PrisonBossLevel.State.FIGHT_START);
                com.shatteredpixel.shatteredpixeldungeon.TenguArenaTest.enter(game,true);
                com.shatteredpixel.shatteredpixeldungeon.TenguArenaTest.devices(game);
            } else {
                com.shatteredpixel.shatteredpixeldungeon.TenguArenaTest.fight(game,PrisonBossLevel.State.FIGHT_ARENA);
            }
            // The native controller reads both fresh seat projections after the fixture moves actors.
            Field field=room.getClass().getDeclaredField("room");
            field.setAccessible(true);
            Object lan=field.get(room);
            Field sync=lan.getClass().getDeclaredField("sync");
            sync.setAccessible(true);
            Object value=sync.get(lan);
            java.lang.reflect.Method commit=value.getClass().getDeclaredMethod("commit",Mob.class,int.class,String.class,String.class);
            commit.setAccessible(true);
            commit.invoke(value,null,-1,null,"IN_GAME");
            return null;
        }).get(15,TimeUnit.SECONDS);
    }

    static int sungrass(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        return ((ExecutorService)executor.get(room)).submit(()->{
            Hero hero=Dungeon.coop.hero(0);
            for (int offset:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                int cell=hero.pos+offset;
                if (Dungeon.level.map[cell]!=Terrain.EMPTY || Dungeon.level.getTransition(cell)!=null || Actor.findChar(cell)!=null
                        || Dungeon.level.heaps.get(cell)!=null || Dungeon.level.plants.get(cell)!=null) continue;
                hero.HP=4;
                Dungeon.level.plant(new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed(),cell);
                return cell;
            }
            throw new AssertionError("Missing adjacent plant cell");
        }).get(5,TimeUnit.SECONDS);
    }
    static int market(CoopController client, boolean stock, int gold) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        return ((ExecutorService)executor.get(room)).submit(()->{
            CoopSession game=Dungeon.coop;
            Hero buyer=game.hero(0);
            if (stock) {
                com.shatteredpixel.shatteredpixeldungeon.items.Heap heap=Dungeon.level.heaps.valueList().stream()
                        .filter(h->h.type==com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.FOR_SALE
                                && h.peek() instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor).findFirst().get();
                buyer.pos=heap.pos;
                Dungeon.gold=gold;
                return heap.pos;
            }
            com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper keeper=CoopTrade.merchant(buyer,false);
            for (int off:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                int pos=keeper.pos+off;
                if (Dungeon.level.passable[pos] && Actor.findChar(pos)==null) { buyer.pos=pos; return keeper.pos; }
            }
            throw new AssertionError("Shopkeeper has no adjacent floor");
        }).get(5,TimeUnit.SECONDS);
    }

    static void atBossKey(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        ((ExecutorService)executor.get(room)).submit(()->{
            for(com.shatteredpixel.shatteredpixeldungeon.items.Heap pile:Dungeon.level.heaps.valueList())
                if (pile.peek() instanceof com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey) {
                    Dungeon.coop.hero(0).pos=pile.pos; return;
                }
            throw new AssertionError("Missing original boss key");
        }).get(5,TimeUnit.SECONDS);
    }

    // The native objective and an identified test wand are staged only for UI capture.
    static void questItems(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        ((ExecutorService)executor.get(room)).submit(()->{
            Hero owner=Dungeon.coop.hero(0);
            for (com.shatteredpixel.shatteredpixeldungeon.items.Item item : new com.shatteredpixel.shatteredpixeldungeon.items.Item[]{
                    new com.shatteredpixel.shatteredpixeldungeon.plants.Rotberry.Seed(),
                    new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile().identify(false)}) {
                if (!item.collect(owner.belongings.backpack)) throw new AssertionError("Test bag full");
                Dungeon.coop.ownership.claim(item,Dungeon.coop.player(0));
            }
        }).get(5,TimeUnit.SECONDS);
    }

    static void knives(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        ((ExecutorService)executor.get(room)).submit(()->{
            if (!new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife().identify(false)
                    .collect(Dungeon.coop.hero(0).belongings.backpack)) throw new AssertionError("Test bag full");
        }).get(5,TimeUnit.SECONDS);
    }

    static void scrolls(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        ((ExecutorService)executor.get(room)).submit(()->{
            Hero owner=Dungeon.coop.hero(0);
            for(com.shatteredpixel.shatteredpixeldungeon.items.Item item:new com.shatteredpixel.shatteredpixeldungeon.items.Item[]{
                    new com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor(),
                    new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade().identify(),
                    new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify().identify()}) {
                if(!item.collect(owner.belongings.backpack)) throw new AssertionError("Test bag full");
                Dungeon.coop.ownership.claim(item,Dungeon.coop.player(0));
            }
        }).get(5,TimeUnit.SECONDS);
    }

    static void healing(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field executor=room.getClass().getDeclaredField("owner"); executor.setAccessible(true);
        ((ExecutorService)executor.get(room)).submit(()->{
            Hero hero=Dungeon.coop.hero(0);
            hero.HP=5;
            com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing draught =
                    new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing();
            draught.identify();
            Dungeon.level.drop(draught,hero.pos);
        }).get(5,TimeUnit.SECONDS);
    }

    static int approachHazard(CoopController client, boolean dart) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field field=room.getClass().getDeclaredField("owner"); field.setAccessible(true);
        return ((ExecutorService)field.get(room)).submit(()->{
            Level level=Dungeon.level;
            for (int cell=0; cell<level.length(); cell++) {
                if (dart ? !(level.traps.get(cell) instanceof com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap)
                        : level.map[cell]!=Terrain.LOCKED_DOOR) continue;
                if (Actor.findChar(cell)!=null || level.heaps.get(cell)!=null) continue;
                for (int offset:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int near=cell+offset;
                    if (!level.passable[near] || Actor.findChar(near)!=null || level.heaps.get(near)!=null
                            || !CoopSession.supportedLanding(near)) continue;
                    Dungeon.coop.hero(0).pos=near;
                    if (!Dungeon.coop.observe(0).visible(cell)) continue;
                    if (!dart) level.drop(new com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey(Dungeon.depth),near);
                    return cell;
                }
            }
            throw new AssertionError("Missing generated trap or locked door");
        }).get(5,TimeUnit.SECONDS);
    }

    static int beside(CoopController client, Class<? extends Mob> type) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field field=room.getClass().getDeclaredField("owner"); field.setAccessible(true);
        return ((ExecutorService)field.get(room)).submit(()->{
            Level level=Dungeon.level;
            for (Mob mob:level.mobs) if (mob.getClass()==type) {
                // A screenshot fixture needs a living observer after the real counterattack.
                if (mob instanceof Goo) {
                    int best=-1;
                    for(int cell=0;cell<level.length();cell++) {
                        if(!level.insideMap(cell) || !level.passable[cell] || level.water[cell] || level.pit[cell]
                                || Actor.findChar(cell)!=null || level.getTransition(cell)!=null) continue;
                        int neighbours=0;
                        for(int offset:com.watabou.utils.PathFinder.NEIGHBOURS8)
                            if(level.passable[cell+offset] && !level.pit[cell+offset] && Actor.findChar(cell+offset)==null) neighbours++;
                        if(neighbours>=3 && (best<0 || level.distance(mob.pos,cell)<level.distance(mob.pos,best))) best=cell;
                    }
                    if(best<0) throw new AssertionError("Missing dry encounter cell");
                    mob.pos=best;
                    for(int seat=0;seat<2;seat++) {
                        Hero hero=Dungeon.coop.hero(seat); hero.HP=hero.HT=350;
                        for(int offset:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                            int p=best+offset;
                            if(level.passable[p] && !level.pit[p] && Actor.findChar(p)==null) { hero.pos=p; break; }
                        }
                        Dungeon.coop.observe(seat);
                    }
                    return best;
                }
                if (mob instanceof Statue && ((Statue)mob).weapon().enchantment instanceof
                        com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Unstable)
                    Dungeon.coop.hero(0).HP=Dungeon.coop.hero(0).HT=120;
                if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost) mob.state=mob.PASSIVE;
                for (int offset:com.watabou.utils.PathFinder.NEIGHBOURS8) {
                    int p=mob.pos+offset;
                    if (level.passable[p] && Actor.findChar(p)==null && !level.pit[p] && level.traps.get(p)==null) {
                        Dungeon.coop.hero(0).pos=p;
                        return mob.pos;
                    }
                }
            }
            throw new AssertionError("No reachable "+type.getSimpleName());
        }).get(5,TimeUnit.SECONDS);
    }

    static void atSentry(CoopController client) throws Exception {
        Field host=CoopController.class.getDeclaredField("host"); host.setAccessible(true);
        Object room=host.get(client);
        Field field=room.getClass().getDeclaredField("owner"); field.setAccessible(true);
        ((ExecutorService)field.get(room)).submit(()->{
            CoopSession game=Dungeon.coop;
            Level level=Dungeon.level;
            for (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room r: ((RegularLevel)level).rooms()) {
                if (!(r instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom)) continue;
                for (int p=0;p<level.length();p++) if (r.inside(level.cellToPoint(p)) &&
                        level.map[p]==Terrain.EMPTY_SP && Actor.findChar(p)==null) {
                    game.hero(0).pos=p;
                    return;
                }
            }
            throw new AssertionError("Expected the native sentry room");
        }).get(5,TimeUnit.SECONDS);
    }

    private static void setup() {
        CoopSession game=Dungeon.coop; game.checkExecutor(); Level level=Dungeon.level;
        if(game.activeSeat()!=0) throw new AssertionError("Host turn required");
        for(Mob mob:new ArrayList<>(level.mobs)) {Actor.remove(mob);level.mobs.remove(mob);}
        int center=level.length()/2/level.width()*level.width()+level.width()/2;
        for(int y=-3;y<=3;y++) for(int x=-4;x<=4;x++) {
            int cell=center+x+y*level.width();
            Level.set(cell,Math.abs(y)==3 || Math.abs(x)==4?Terrain.WALL:Terrain.EMPTY);
            level.heaps.remove(cell); level.traps.remove(cell); level.plants.remove(cell);level.discoverable[cell]=true;
        }
        Hero host=game.hero(0), guest=game.hero(1);
        host.pos=center;guest.pos=center+1;
        Ankh ankh=new Ankh(); guest.belongings.backpack.items.add(ankh);game.ownership.claim(ankh,game.player(1));
        Rat rat=new Rat(); rat.pos=center-1; rat.HP=rat.HT=100;GameScene.add(rat,990);rat.aggro(host);
        guest.HP=17;
        rat.attack(guest,1,10000,Float.POSITIVE_INFINITY);
        if(game.life(1)!=CoopSession.Life.DOWNED) throw new AssertionError("Fixture damage did not down guest");
    }
}

