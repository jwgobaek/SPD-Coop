package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.ImpVaultTest;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.junit.Assert.*;

/** One JVM, one backend, real persistent rooms and two authenticated TCP clients per room. */
public final class MixedRoomStability {
    final Map<String,Integer> verbs=new TreeMap<>();
    final List<Long> latencies=new ArrayList<>();
    final List<WeakReference<LanRoom>> closed=new ArrayList<>();
    int rejected,duplicates,rejoins;
    long saveNanos;
    private static VaultPublicRoute.Input input(PublicDungeon view,VaultPublicRoute.Guide guide) {
        for(Talent t:Talent.values()) {
            String rank=view.source.rows.get("learn"+t.ordinal());
            String tier=view.source.rows.get("tier"+t.ordinal());
            if(rank!=null && Integer.parseInt(rank)<t.maxPoints() &&
                    Integer.parseInt(view.source.rows.getOrDefault("freepoints"+tier,"0"))>0)
                return new VaultPublicRoute.Input("INVEST_TALENT",t.ordinal(),0);
        }
        if (Boolean.getBoolean("stability.rogue") && "ROGUE".equals(view.heroClass) && !view.monsters.isEmpty()) {
            for (PublicDungeon.Loot item : view.belongings) {
                if ("1".equals(view.source.rows.get("stealth"+item.id)) && item.charges>0
                        && !view.source.rows.containsKey("cloaked"+item.id))
                    return new VaultPublicRoute.Input("CLOAK",0,item.id);
            }
        }
        return guide.choose(view);
    }
    void play(LanRoom room,InetSocketAddress addr,int budget) throws Exception {
        LanClient[] clients={LanClient.synchronizedGame(addr),LanClient.synchronizedGame(addr)};
        try {
            SeatCredential[] keys={SeatCredential.generate(),SeatCredential.generate()};
            DisplayReplica[] images=new DisplayReplica[2];
            SeatActions[] actions=new SeatActions[2];
            VaultPublicRoute.Guide[] guides={new VaultPublicRoute.Guide(),new VaultPublicRoute.Guide()};
            UUID epoch=null;
            for(int i=0;i<2;i++) {
                assertTrue(clients[i].request(i==0?room.hostJoinCommand():"JOIN").startsWith("JOINED "));
                UUID seated=UUID.fromString(keys[i].enroll(clients[i]).split(" ")[1]);
                if(i==0) epoch=seated; else assertEquals(epoch,seated);
                clients[i].request(i==0 && Boolean.getBoolean("stability.rogue") ? "SELECT ROGUE" : i==0?"SELECT MAGE":"SELECT HUNTRESS");
                images[i]=new DisplayReplica(room.session,i);
                actions[i]=new SeatActions(room.session,i,i+1);
            }
            for(LanClient c:clients)c.request("READY 1");
            assertEquals("GAME_STARTED",clients[0].request("START"));
            int seat=0;
            for(int n=0;n<budget;n++) {
                if(n==15 || n%100==99) {
                    clients[1].close();
                    clients[1]=LanClient.synchronizedGame(addr);
                    assertTrue(keys[1].reconnect(clients[1],epoch,1,2).startsWith("RESUMED "));
                    images[1].confirmReconnect(clients[1]);
                    images[0].confirmReconnect(clients[0]);
                    rejoins++;
                }
                images[seat].refresh(clients[seat]);
                int active=Integer.parseInt(images[seat].image().rows.get("active"));
                if(active<0) break;
                if(seat!=active) {
                    seat=active;
                    images[seat].refresh(clients[seat]);
                }
                PublicDungeon v=new PublicDungeon(images[seat].image());
                VaultPublicRoute.Input move=input(v,guides[seat]);
                SeatActions.Pending action=actions[seat].prepare(v.source,move.action,move.cell,move.item);
                long start=System.nanoTime();
                String receipt=action.submit(clients[seat]);
                if(receipt.endsWith(" APPLIED") || receipt.endsWith(" PARTY_DEFEATED")) {
                    verbs.merge(move.action,1,Integer::sum);
                    latencies.add(System.nanoTime()-start);
                } else {
                    rejected++;
                    if(move.item!=0) guides[seat].rejectedItems.add(move.item);
                    if(move.action.equals("MOVE")) guides[seat].blocked.add(move.cell);
                    if(move.action.equals("OPEN")) guides[seat].rejectedChests.add(move.cell);
                    if(move.action.equals("REVIVE")) guides[seat].reviveDenied=true;
                }
                if(n%67==0 && receipt.endsWith(" APPLIED")) {
                    assertEquals(receipt,action.submit(clients[seat]));
                    duplicates++;
                }
            }
        } finally {
            for(LanClient client:clients) client.close();
        }
    }

    void room(File root,int seed,int budget) throws Exception {
        InetAddress loop=InetAddress.getLoopbackAddress();
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try(LanRoom room=LanRoom.persistentGame(loop,0,0,seed,new CoopSaves(root))) {
            InetSocketAddress address=new InetSocketAddress(loop,room.tcpPort());
            Future<?> run=worker.submit(()-> {play(room,address,budget);return null;});
            while(!run.isDone()) {room.pump();Thread.sleep(1);}
            run.get();
            long start=System.nanoTime();
            room.saveAndClose();
            saveNanos=System.nanoTime()-start;
            closed.add(new WeakReference<>(room));
        } finally {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(10,TimeUnit.SECONDS));
        }
    }

    static long usedHeap() {
        Runtime r=Runtime.getRuntime();
        return r.totalMemory()-r.freeMemory();
    }
    static long quantile(List<Long> values,double q) {
        return values.isEmpty()?0:values.get((int)Math.ceil(values.size()*q)-1)/1000;
    }
    void observe(File root,int index) throws Exception {
        long before=usedHeap();
        System.gc();
        Thread.sleep(100);
        Set<Thread> live=Thread.getAllStackTraces().keySet();
        long workers=live.stream().filter(t->t.getName().equals("spd-lan-io")).count();
        long descriptors,sockets;
        try(Stream<Path> fd=Files.list(Paths.get("/proc/self/fd"))) {
            List<Path> paths=new ArrayList<>();
            fd.forEach(paths::add);descriptors=paths.size();
            sockets=paths.stream().filter(f->{try{return Files.readSymbolicLink(f).toString().startsWith("socket:");}
                catch(java.io.IOException e){return false;}}).count();
        }
        long bytes;
        try(Stream<Path> disk=Files.walk(root.toPath())) {
            bytes=disk.filter(Files::isRegularFile).mapToLong(f->f.toFile().length()).sum();
        }
        List<Long> sorted=new ArrayList<>(latencies);
        Collections.sort(sorted);
        System.out.printf("ROOM %d applied=%d verbs=%s rejected=%d duplicates=%d rejoins=%d heap=%d postGC=%d actors=%d threads=%d lanWorkers=%d fd=%d sockets=%d retainedRooms=%d cumulativeSaveBytes=%d lastSaveUs=%d actionUs=%d/%d/%d%n",
            index,latencies.size(),verbs,rejected,duplicates,rejoins,before,usedHeap(),Actor.all().size(),live.size(),workers,descriptors,sockets,
            closed.stream().filter(ref->ref.get()!=null).count(),bytes,saveNanos/1000,quantile(sorted,.5),quantile(sorted,.95),quantile(sorted,.99));
    }
    public static void main(String[] args) throws Exception {
        ImpVaultTest fixture=new ImpVaultTest();
        fixture.temporary.create();fixture.startBackend();
        try {
            MixedRoomStability trial=new MixedRoomStability();
            for(int i=0;i<Integer.parseInt(args[0]);i++) {
                trial.room(fixture.temporary.getRoot(),570+i%12,Integer.parseInt(args[1]));
                trial.observe(fixture.temporary.getRoot(),i+1);
            }
        } finally {fixture.stopBackend();fixture.temporary.delete();}
    }
}
