package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.HeadlessDungeon;
import org.junit.Test;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/** Injects the native linkage failure at the save boundary on the actual hosted owner thread. */
public class HostLinkageRecovery extends HeadlessDungeon {
    @Test public void verifyErrorBeforeHeadRetainsThePreviousGeneration() throws Exception {
        exercise("BEFORE_HEAD");
    }

    @Test public void linkageErrorAfterHeadRetainsThePublishedReceipt() throws Exception {
        exercise("AFTER_HEAD");
    }

    private void exercise(String barrier) throws Exception {
        AtomicBoolean armed = new AtomicBoolean();
        CoopSaves saves = new CoopSaves(temporary.getRoot(), stage -> {
            if (stage.equals(barrier) && armed.compareAndSet(true, false)) {
                if (stage.equals("BEFORE_HEAD")) throw new VerifyError("fixture");
                throw new LinkageError("fixture");
            }
        });
        InetAddress loop = InetAddress.getLoopbackAddress();
        HostedRoom host = new HostedRoom(saves,loop,0,0,581,null,false);
        UUID id=host.session;
        InetSocketAddress address = new InetSocketAddress(loop,host.port);
        SeatCredential[] keys={SeatCredential.generate(),SeatCredential.generate()};
        UUID epoch=null;
        long before;
        Path head=temporary.getRoot().toPath().resolve("coop-saves-v1").resolve(id.toString()).resolve("HEAD");
        SeatActions.Pending pending;
        try (LanClient first=LanClient.synchronizedGame(address); LanClient second=LanClient.synchronizedGame(address)) {
            for (int i=0;i<2;i++) {
                LanClient c=i==0?first:second;
                assertTrue(c.request(i==0?host.localJoin:"JOIN").startsWith("JOINED "));
                UUID enrolled=UUID.fromString(keys[i].enroll(c).split(" ")[1]);
                if (i==0) epoch=enrolled; else assertEquals(epoch,enrolled);
                assertEquals("OK",c.request("SELECT WARRIOR"));
                assertEquals("OK",c.request("READY 1"));
            }
            assertEquals("GAME_STARTED",first.request("START"));
            before=java.nio.ByteBuffer.wrap(Files.readAllBytes(head)).getLong();
            DisplayReplica image=new DisplayReplica(id,0);
            image.refresh(first);
            pending=new SeatActions(id,0,1).prepare(image.image(),"WAIT",0,0);
            armed.set(true);
            try { pending.submit(first); fail("A stopped room returned an action result"); }
            catch (IOException expected) { assertTrue(host.failed); }
        } finally {
            assertFalse("Failed room must not publish on close",host.close(true));
        }
        boolean committed="AFTER_HEAD".equals(barrier);
        assertEquals(before+(committed?1:0),saves.inspect(id,false).generation);
        HostedRoom restored=new HostedRoom(saves,loop,0,0,581,id,false);
        InetSocketAddress resumed=new InetSocketAddress(loop,restored.port);
        try (LanClient first=LanClient.synchronizedGame(resumed); LanClient second=LanClient.synchronizedGame(resumed)) {
            assertTrue(keys[0].reconnect(first,epoch,0,1).startsWith("RESUMED "));
            assertTrue(keys[1].reconnect(second,epoch,1,2).startsWith("RESUMED "));
            DisplayReplica a=new DisplayReplica(id,0), b=new DisplayReplica(id,1);
            a.confirmReconnect(first); b.confirmReconnect(second);
            String receipt=first.request("LOOKUP "+epoch+" "+pending.id);
            if (committed) {
                assertTrue(receipt,receipt.endsWith(" APPLIED"));
                assertEquals(receipt,pending.submit(first));
            } else assertEquals("UNKNOWN",receipt);
        } finally { restored.close(false); }
    }
}
