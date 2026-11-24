package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReconnectTest extends HeadlessDungeon {
    @Test(timeout=90000) public void lockedSeatsMustProvePossessionAndVerifyTheirOwnSnapshot() throws Exception {
        InetAddress loop=InetAddress.getLoopbackAddress();
        try(LanRoom room=LanRoom.persistentGame(loop,0,0,402,new CoopSaves(temporary.getRoot()))) {
            InetSocketAddress address=new InetSocketAddress(loop,room.tcpPort());
            String hostJoin=room.hostJoinCommand();
            ExecutorService worker=Executors.newSingleThreadExecutor();
            try {
                Future<Void> result=worker.submit(()->{
                    SeatCredential a=SeatCredential.generate(), b=SeatCredential.generate();
                    try(LanClient host=LanClient.synchronizedGame(address);
                        LanClient guest=LanClient.synchronizedGame(address)) {
                        assertEquals("JOINED 0 1",host.request(hostJoin));
                        assertEquals("JOINED 1 2",guest.request("JOIN"));
                        UUID epoch=UUID.fromString(a.enroll(host).split(" ")[1]);
                        assertEquals("ENROLLED "+epoch,b.enroll(guest));
                        host.request("SELECT WARRIOR"); guest.request("SELECT HUNTRESS");
                        host.request("READY 1"); guest.request("READY 1");
                        assertEquals("GAME_STARTED",host.request("START"));
                        DisplayReplica r=new DisplayReplica(room.session,0); r.refresh(host);
                        SeatActions.Pending move=new SeatActions(room.session,0,1).prepare(r.image(),"WAIT",0,0);
                        String committed=move.submit(host);
                        assertTrue(committed,committed.endsWith(" APPLIED"));
                        guest.close();
                        try(LanClient next=LanClient.synchronizedGame(address)) {
                            assertEquals("ERROR LOCKED",next.request("JOIN"));
                            assertEquals("ERROR AUTH",a.reconnect(next,epoch,1,2));
                            assertEquals("ERROR AUTH",next.request("PROVE 00"));
                            assertEquals("ERROR DUPLICATE",a.reconnect(next,epoch,0,1));
                            assertTrue(b.reconnect(next,epoch,1,2).startsWith("RESUMED 1 2 "));
                            DisplayReplica g=new DisplayReplica(room.session,1); g.refresh(next); r.refresh(host);
                            assertEquals("PAUSED",g.image().rows.get("status"));
                            assertNotNull(g.image().rows.get("self"));
                            assertEquals("ERROR SNAPSHOT",next.request("CONFIRM "+g.image().version+" "+r.image().checksum));
                            assertEquals("CONFIRMED",next.request("CONFIRM "+g.image().version+" "+g.image().checksum));
                            assertEquals("ERROR PAUSED",move.submit(host));
                            assertEquals("UNKNOWN",next.request("LOOKUP "+epoch+" 1"));
                            assertEquals(committed,host.request("LOOKUP "+epoch+" 1"));
                            assertEquals("CONFIRMED",host.request("CONFIRM "+r.image().version+" "+r.image().checksum));
                            r.refresh(host); g.refresh(next);
                            assertEquals("IN_GAME",g.image().rows.get("status"));
                            assertEquals(committed,move.submit(host));
                            assertEquals("ERROR EPOCH",host.request("LOOKUP "+UUID.randomUUID()+" 1"));
                        }
                    }
                    return null;
                });
                while(!result.isDone()) { room.pump(); Thread.sleep(1); }
                result.get();
                room.saveAndClose();
            } finally { worker.shutdownNow(); }
        }
    }
}
