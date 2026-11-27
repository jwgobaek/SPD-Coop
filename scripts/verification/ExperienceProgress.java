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
        try (CoopSession game=CoopSession.open(seed,1,front)) {
            game.join(2,HeroClass.valueOf(System.getProperty("trial.partner","HUNTRESS")));
            Random.pushGenerator(seed);
            UUID uuid=UUID.randomUUID();
            RoomSync room=new RoomSync(uuid,game);
            DisplayReplica[] clients={new DisplayReplica(uuid,0),new DisplayReplica(uuid,1)};
            VaultPublicRoute.Guide[] guides={new VaultPublicRoute.Guide(),new VaultPublicRoute.Guide()};
            long[] seq={0,0};
            int acted=0,depth=1,down=0;
            java.util.Set<String> declined=new java.util.HashSet<>();
            java.util.Map<String,Integer> abilityUse=new java.util.TreeMap<>();
            int[] investments={0,0};
            boolean[] wasDown={false,false};
            String result="budget";
            for (int n=0;n<budget;++n) {
                if (game.activeSeat()<0) {result=game.defeated()?"defeat":"stopped"; break;}
                int seat=game.activeSeat();
                PublicDungeon v=view(clients[seat],room,seat);
                if (depth!=v.depth) {
                    depth=v.depth;
                    guides[0]=new VaultPublicRoute.Guide();
                    guides[1]=new VaultPublicRoute.Guide();
                }
                VaultPublicRoute.Input input=guides[seat].choose(v);
                if (v.offered && (v.heroClass.equals("CLERIC") || v.heroClass.equals("DUELIST"))) {
                    for (PublicDungeon.Loot tool : v.belongings) {
                        String verb=v.actions(tool).contains("CAST") ? "GUIDING_LIGHT" :
                                tool.weaponSkill && tool.sprite==com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.RAPIER ? "WEAPON_ABILITY" : null;
                        if (verb==null || tool.charges<=0) continue;
                        for (int target : new java.util.TreeSet<>(v.monsters.keySet())) {
                            int distance=Math.max(Math.abs(v.pos%v.width-target%v.width),Math.abs(v.pos/v.width-target/v.width));
                            if (!v.visible(target) || verb.equals("WEAPON_ABILITY") && distance!=2) continue;
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
                        send(room,other,1-seat,game.player(1-seat),++seq[1-seat],new VaultPublicRoute.Input("CONSENT",other.stair,0));
                        v=view(clients[seat],room,seat);
                    }
                    if (v.consent) input=new VaultPublicRoute.Input("TRAVEL",v.stair,0);
                }
                String status=send(room,v,seat,game.player(seat),++seq[seat],input);
                if (input.action.equals("WEAPON_ABILITY") || input.action.equals("GUIDING_LIGHT") || input.action.equals("CLOAK")) {
                    if (status.equals("APPLIED")) abilityUse.merge(input.action,1,Integer::sum);
                    else declined.add(seat+"/"+v.depth+"/"+v.pos+"/"+input.cell+"/"+input.action);
                }
                if (input.action.equals("INVEST_TALENT") && status.equals("APPLIED")) investments[seat]++;
                if (Boolean.getBoolean("trial.trace")) System.out.printf("STEP %d %d %d %s %d %s stair=%d mobs=%s loot=%s%n",n,seat,v.pos,input.action,input.cell,status,v.stair,v.monsters,v.floor.keySet());
                if (status.equals("APPLIED") || status.equals("PARTY_DEFEATED")) acted++;
                else if (input.item!=0) guides[seat].rejectedItems.add(input.item);
                else if (input.action.equals("OPEN")) guides[seat].rejectedChests.add(input.cell);
                else if (input.action.equals("REVIVE")) guides[seat].reviveDenied=true;
                else if (input.action.equals("MOVE")) guides[seat].blocked.add(input.cell);
                else {result=input.action+"/"+status;break;}
                for (int s=0;s<2;s++) {
                    boolean fallen=game.life(s)!=CoopSession.Life.ALIVE;
                    if (fallen && !wasDown[s]) down++;
                    wasDown[s]=fallen;
                }
            }
            if (!abilityUse.isEmpty()) System.out.println("NATIVE_ABILITIES "+seed+" "+policy+" "+abilityUse);
            for (int s=0;s<2;s++) {
                System.out.printf("INVESTED\t%d\t%s\t%d\t%d%n",seed,policy,s,investments[s]);
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

    public static void main(String[] args) throws Exception {
        policy=args[0];
        if (!policy.equals("last") && !policy.equals("split") && !policy.equals("each")) throw new IllegalArgumentException(policy);
        ImpVaultTest fixture=new ImpVaultTest();
        fixture.temporary.create();fixture.startBackend();
        try {play(Integer.parseInt(args[1]),Integer.parseInt(args[2]));}
        finally {fixture.stopBackend();fixture.temporary.delete();}
    }
}
