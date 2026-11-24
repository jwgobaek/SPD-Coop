package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.*;

/** The room and every engine operation stay on this single owner thread. */
final class HostedRoom {
    private final ScheduledExecutorService owner=Executors.newSingleThreadScheduledExecutor(r->new Thread(r,"Coop engine"));
    private LanRoom room;
    final int port;
    final String localJoin;
    final UUID session;
    volatile boolean failed;

    HostedRoom(CoopSaves saves,InetAddress bind,int tcp,int udp,long seed,UUID resume,boolean backup) throws Exception {
        try {
            room=call(()->{
                com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.releaseInactiveDungeon();
                return resume==null ? LanRoom.persistentGame(bind,tcp,udp,seed,saves)
                    : LanRoom.resume(bind,tcp,udp,saves,resume,backup);
            });
            port=room.tcpPort(); session=room.session; localJoin=room.hostJoinCommand();
            owner.scheduleWithFixedDelay(()->{
                try { room.pump(); }
                catch(RuntimeException error) { failed=true; room.close(); }
            },0,5,TimeUnit.MILLISECONDS);
        } catch(Exception error) {owner.shutdownNow(); throw error;}
    }

    private <T> T call(Callable<T> operation) throws Exception {
        try { return owner.submit(operation).get(); }
        catch(ExecutionException error) {
            if(error.getCause() instanceof Exception) throw (Exception)error.getCause();
            throw new IOException("ENGINE",error.getCause());
        }
    }

    void close(boolean save) throws Exception {
        call(()->{
            if (save && (room.state()==LanRoom.State.IN_GAME || room.state()==LanRoom.State.PAUSED ||
                    room.state()==LanRoom.State.PARTY_DEFEATED)) room.saveAndClose();
            else room.close();
            return null;
        });
        owner.shutdown();
    }
}
