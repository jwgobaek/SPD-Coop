package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Random;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import com.watabou.utils.Bundle;

/** Test classpath only. The input chooser sees the same seat projection as an ordinary client. */
public final class ExperienceProgress {
    private final static int[] granted = new int[2];
    private static int remainderSeat;
    private static String policy;

    /** Called instead of the single earnExp line in the isolated JVM's compiled Mob copy. */
    public static void award(Mob mob, Hero killer, int nativeAmount) {
        CoopSession game=Dungeon.coop;
        if (game==null || policy.equals("last")) {
            killer.earnExp(nativeAmount,mob.getClass());
            if (game!=null) granted[killer==game.hero(0)?0:1]+=nativeAmount;
            return;
        }
        boolean[] eligible=new boolean[2];
        for (int s=0;s<2;s++) eligible[s]=game.life(s)==CoopSession.Life.ALIVE && game.hero(s).lvl<=mob.maxLvl;
        int count=(eligible[0]?1:0)+(eligible[1]?1:0);
        if (count==0) return;
        int pool=mob.EXP;
        for (int s=0;s<2;s++) {
            if (!eligible[s]) continue;
            int amount=policy.equals("each") ? pool : pool/count;
            if (policy.equals("split") && count==2 && pool%2==1 && s==remainderSeat) amount++;
            game.hero(s).earnExp(amount,mob.getClass());
            granted[s]+=amount;
        }
        if (policy.equals("split") && count==2 && pool%2==1) remainderSeat=1-remainderSeat;
    }

    private static PublicDungeon view(DisplayReplica replica,RoomSync room,int seat) throws Exception {
        replica.refresh(command -> {
            String[] f=command.split(" ");
            return f[0].equals("SYNC") ? room.sync(seat,Long.parseLong(f[1]),f[2])
                    : room.part(seat,Long.parseLong(f[1]),Integer.parseInt(f[2]));
        });
        return new PublicDungeon(replica.image());
    }

    private static String send(RoomSync room,PublicDungeon v,int seat,long player,long seq,VaultPublicRoute.Input input) {
        String[] action={"","","","",Long.toString(player),Integer.toString(seat),
                v.source.rows.get("self").split(",")[0],Long.toString(seq),Long.toString(v.source.version),
                v.source.rows.get("turn"),input.action,Integer.toString(input.cell),Long.toString(input.item)};
        String response=room.action(seat,action);
        return response.substring(response.lastIndexOf(' ')+1);
    }

    private static void play(int seed,int budget) throws Exception {
        HeroClass front=HeroClass.valueOf(System.getProperty("trial.front","WARRIOR"));
        HeroClass partner=HeroClass.valueOf(System.getProperty("trial.partner","HUNTRESS"));
        String prior=System.getProperty("trial.resumeGame");
        CoopSession initial;
        if (prior==null) {
            initial=CoopSession.open(seed,1,front);
        } else {
            try (var stream=Files.newInputStream(Path.of(prior))) {
                initial=CoopSession.restore(Bundle.read(stream));
            }
        }
        try (CoopSession game=initial) {
            if (prior==null) {
                game.join(2,partner);
                Random.pushGenerator(seed);
            } else if (Dungeon.seed!=seed || Dungeon.branch!=0 || game.hero(0).heroClass!=front
                    || game.hero(1).heroClass!=partner || !"last".equals(policy)) {
                throw new IllegalArgumentException("Expected the same normal seed, classes, and last-hit XP policy");
            }
            UUID uuid=UUID.randomUUID();
            RoomSync room=new RoomSync(uuid,game);
            DisplayReplica[] clients={new DisplayReplica(uuid,0),new DisplayReplica(uuid,1)};
            VaultPublicRoute.Guide[] guides={new VaultPublicRoute.Guide(),new VaultPublicRoute.Guide()};
            long[] seq={0,0};
            int acted=0,depth=1,down=0;
            java.util.Set<String> declined=new java.util.HashSet<>();
            java.util.Map<String,Integer> abilityUse=new java.util.TreeMap<>();
            int[] investments={0,0}, afterExitMoves={0,0}, nearExit={0,0};
            int consents=0, declinedStairs=0, traveled=0;
            int[] moveTotals=new int[2], repeats=new int[2], firstExit={-1,-1}, gateSignals=new int[2], firstPoints={-1,-1}, frontiers=new int[2], exitSeen={-1,-1};
            java.util.List<java.util.Set<String>> walked=java.util.Arrays.asList(new java.util.HashSet<>(),new java.util.HashSet<>());
            boolean[] wasDown={false,false};
            PublicDungeon[] lastObservation=new PublicDungeon[2];
            String result="budget";
            for (int n=0;n<budget;++n) {
                if (game.activeSeat()<0) {result=game.defeated()?"defeat":"stopped"; break;}
                int seat=game.activeSeat();
                PublicDungeon v=view(clients[seat],room,seat);
                lastObservation[seat]=v;
                if (depth!=v.depth) {
                    depth=v.depth;
                    guides[0]=new VaultPublicRoute.Guide();
                    guides[1]=new VaultPublicRoute.Guide();
                }
                VaultPublicRoute.Input input=guides[seat].choose(v);
                boolean keepRecovery = input.action.equals("REVIVE") || input.action.equals("DRINK") || input.action.equals("EAT")
                        || input.action.equals("MOVE") && guides[seat].isolatedAndOutnumbered(v);
                if (v.offered && !keepRecovery && (v.heroClass.equals("CLERIC") || v.heroClass.equals("DUELIST"))) {
                    var targets = visibleAttackTargets(v);
                    for (PublicDungeon.Loot tool : v.belongings) {
                        String verb=v.actions(tool).contains("CAST") ? "GUIDING_LIGHT" :
                                tool.weaponSkill && tool.sprite==com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.RAPIER ? "WEAPON_ABILITY" : null;
                        if (verb==null || tool.charges<=0) continue;
                        for (int target : targets) {
                            int distance=Math.max(Math.abs(v.pos%v.width-target%v.width),Math.abs(v.pos/v.width-target/v.width));
                            if (verb.equals("WEAPON_ABILITY") && distance!=2) continue;
                            if (declined.contains(seat+"/"+v.depth+"/"+v.pos+"/"+target+"/"+verb)) continue;
                            input=new VaultPublicRoute.Input(verb,target,tool.id);
                            break;
                        }
                    }
                }
                if (v.offered && Boolean.getBoolean("trial.campaignGuide") && v.heroClass.equals("ROGUE")
                        && (input.action.equals("ATTACK") || input.action.equals("THROW"))) {
                    for (PublicDungeon.Loot cloak : v.belongings) {
                        if (v.actions(cloak).contains("CLOAK") && cloak.charges>0
                                && !v.source.rows.containsKey("cloaked"+cloak.id)) {
                            input=new VaultPublicRoute.Input("CLOAK",0,cloak.id);
                            break;
                        }
                    }
                }
                if (v.offered) {
                    for (com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent t:com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.values()) {
                        String offered=v.source.rows.get("learn"+t.ordinal());
                        String tier=v.source.rows.get("tier"+t.ordinal());
                        if (offered!=null && Integer.parseInt(v.source.rows.getOrDefault("freepoints"+tier,"0"))>0 && Integer.parseInt(offered)<t.maxPoints()) {
                            input=new VaultPublicRoute.Input("INVEST_TALENT",t.ordinal(),0);
                            break;
                        }
                    }
                }
                if (v.stair>=0 && v.terrain(v.stair)==Terrain.EXIT) {
                    PublicDungeon other=view(clients[1-seat],room,1-seat);
                    if (!v.consent && other.stair==v.stair) {
                        String consent=send(room,other,1-seat,game.player(1-seat),++seq[1-seat],new VaultPublicRoute.Input("CONSENT",other.stair,0));
                        if (consent.equals("APPLIED")) consents++; else declinedStairs++;
                        v=view(clients[seat],room,seat);
                    }
                    if (v.consent) input=new VaultPublicRoute.Input("TRAVEL",v.stair,0);
                }
                if (Boolean.getBoolean("trial.traceRecovery") && (v.hp<=v.maxHp/2 || v.depth>=2)) {
                    PublicDungeon observed=v;
                    String supplies=observed.belongings.stream().filter(it -> !observed.actions(it).isEmpty())
                            .map(it -> it.kind+"="+it.quantity+(it.showsCharges()?"/"+it.charges:"")).collect(java.util.stream.Collectors.joining(","));
                    System.out.printf("RECOVERY %d depth=%d seat=%d pos=%d hp=%d ally=%d life=%s mobs=%s supplies=%s action=%s target=%d%n",
                            n,v.depth,seat,v.pos,v.hp,v.ally,v.source.rows.get("life"),v.monsters,supplies,input.action,input.cell);
                    if (Boolean.getBoolean("trial.traceStairs")) {
                        int known=guides[seat].knownExit;
                        System.out.printf("EXIT_VIEW n=%d depth=%d seat=%d pos=%d known=%d visible=%b near=%b ally=%d stair=%d%n",
                                n,v.depth,seat,v.pos,known,known>=0 && v.visible(known),
                                known>=0 && Math.max(Math.abs(v.pos%v.width-known%v.width),
                                        Math.abs(v.pos/v.width-known/v.width))<=1,v.ally,v.stair);
                    }
                }
                String status=send(room,v,seat,game.player(seat),++seq[seat],input);
                if (input.action.equals("WEAPON_ABILITY") || input.action.equals("GUIDING_LIGHT") || input.action.equals("CLOAK")) {
                    if (status.equals("APPLIED")) abilityUse.merge(input.action,1,Integer::sum);
                    else declined.add(seat+"/"+v.depth+"/"+v.pos+"/"+input.cell+"/"+input.action);
                }
                if (input.action.equals("INVEST_TALENT") && status.equals("APPLIED")) investments[seat]++;
                if ("MOVE".equals(input.action) && status.equals("APPLIED")) {
                    moveTotals[seat]++;
                    if (!walked.get(seat).add(v.depth+"/"+v.branch+"/"+input.cell)) repeats[seat]++;
                }
                if (firstExit[seat]<0 && guides[seat].knownExit>=0) { firstExit[seat]=n; exitSeen[seat]=guides[seat].knownExit; }
                if (v.stair>=0) gateSignals[seat]++;
                if (firstExit[seat]>=0 && input.action.equals("MOVE") && status.equals("APPLIED")) afterExitMoves[seat]++;
                if (guides[seat].knownExit>=0 && VaultPublicRoute.adjacent(v.pos,guides[seat].knownExit,v.width)) nearExit[seat]++;
                if (input.action.equals("TRAVEL") && status.equals("APPLIED")) traveled++;
                if (firstPoints[seat]<0 && Integer.parseInt(v.source.rows.getOrDefault("freepoints1","0"))>0) firstPoints[seat]=n;
                frontiers[seat]=Math.max(frontiers[seat],guides[seat].reachableFrontier);
                if (Boolean.getBoolean("trial.trace")) System.out.printf("STEP %d %d %d %s %d %s stair=%d mobs=%s loot=%s%n",n,seat,v.pos,input.action,input.cell,status,v.stair,v.monsters,v.floor.keySet());
                if (status.equals("APPLIED") || status.equals("PARTY_DEFEATED")) acted++;
                else if (input.item!=0) guides[seat].rejectedItems.add(input.item);
                else if (input.action.equals("OPEN")) guides[seat].rejectedChests.add(input.cell);
                else if (input.action.equals("REVIVE")) guides[seat].reviveDenied=true;
                else if (input.action.equals("MOVE")) guides[seat].blocked.add(input.cell);
                else {result=input.action+"/"+status;break;}
                for (int s=0;s<2;s++) {
                    boolean fallen=game.life(s)!=CoopSession.Life.ALIVE;
                    if (fallen && !wasDown[s]) {
                        down++;
                        if (Boolean.getBoolean("trial.traceStairs") && lastObservation[s]!=null) {
                            PublicDungeon observed=lastObservation[s];
                            String supplies=observed.belongings.stream()
                                    .map(i -> i.kind+"="+i.quantity+(i.showsCharges()?"/"+i.charges:""))
                                    .collect(java.util.stream.Collectors.joining(","));
                            System.out.printf("FALL n=%d seat=%d depth=%d lastPos=%d hpBefore=%d allyVisibleAt=%d publicMobs=%s ownItems=%s actingSeat=%d action=%s target=%d result=%s%n",
                                    n,s,observed.depth,observed.pos,observed.hp,observed.ally,observed.monsters,
                                    supplies,seat,input.action,input.cell,status);
                        }
                    }
                    wasDown[s]=fallen;
                }
            }
            String destination=System.getProperty("trial.saveGame");
            if (destination!=null) {
                try (var stream=Files.newOutputStream(Path.of(destination),StandardOpenOption.CREATE_NEW)) {
                    if (!Bundle.write(game.checkpoint(),stream)) throw new java.io.IOException("Could not write campaign checkpoint");
                }
            }
            if (!abilityUse.isEmpty()) System.out.println("NATIVE_ABILITIES "+seed+" "+policy+" "+abilityUse);
            for (int s=0;s<2;s++) {
                System.out.printf("INVESTED\t%d\t%s\t%d\t%d%n",seed,policy,s,investments[s]);
                System.out.printf("STAIRS\t%d\t%d\tmovesAfterDiscovery=%d\tnearTurns=%d\tpartyConsents=%d\tdeclined=%d\ttraveled=%d%n",seed,s,afterExitMoves[s],nearExit[s],consents,declinedStairs,traveled);
                System.out.printf("NAVIGATION\t%d\t%d\tmoves=%d\trevisits=%d\tunique=%d\tfrontierPeak=%d\tfirstExitAction=%d\texitCell=%d\tgateSignals=%d\tfirstPointAction=%d%n",
                        seed,s,moveTotals[s],repeats[s],walked.get(s).size(),frontiers[s],firstExit[s],exitSeen[s],gateSignals[s],firstPoints[s]);
                Hero h=game.hero(s);
                int food=0,heal=0,up=0;
                for (Item i:h.belongings) {
                    if(i instanceof Food) food+=i.quantity();
                    if(i instanceof PotionOfHealing) heal+=i.quantity();
                    if(i instanceof ScrollOfUpgrade) up+=i.quantity();
                }
                String weapon=h.belongings.weapon==null?"none":h.belongings.weapon.getClass().getSimpleName()+"/"+h.belongings.weapon.trueLevel();
                String armor=h.belongings.armor==null?"none":h.belongings.armor.getClass().getSimpleName()+"/"+h.belongings.armor.trueLevel();
                System.out.printf("XP_RUN\t%d\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s\t%s\t%s%n",
                    seed,policy,s,depth,acted,Statistics.enemiesSlain,down,granted[s],h.lvl,h.HT,h.HP,h.STR(),food,heal,up,Dungeon.gold,h.pos,weapon,armor,result);
            }
        }
    }

    private static java.util.List<Integer> visibleAttackTargets(PublicDungeon view) {
        var targets = new java.util.ArrayList<>(view.monsters.keySet());
        targets.removeIf(cell -> !view.visible(cell) || !VaultPublicRoute.Guide.hostile(view.monsters.get(cell)));
        targets.sort(java.util.Comparator.comparingInt((Integer cell) -> Math.max(
                Math.abs(view.pos % view.width - cell % view.width), Math.abs(view.pos / view.width - cell / view.width)))
                .thenComparingInt(Integer::intValue));
        return targets;
    }

    public static void main(String[] args) throws Exception {
        policy=args[0];
        if (!policy.equals("last") && !policy.equals("split") && !policy.equals("each")) throw new IllegalArgumentException(policy);
        ImpVaultTest fixture=new ImpVaultTest();
        fixture.temporary.create();fixture.startBackend();
        try {play(Integer.parseInt(args[1]),Integer.parseInt(args[2]));}
        finally {fixture.stopBackend();fixture.temporary.delete();}
    }
}
