package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LanBoundaryTest {
    private final InetAddress loop = InetAddress.getLoopbackAddress();

    @Test(timeout=5000)
    public void duplicateUdpAnnouncementsAreOneRoom() throws Exception {
        try (DatagramSocket responder = new DatagramSocket(0,loop)) {
            UUID id = UUID.randomUUID();
            FutureTask<Void> replies = new FutureTask<>(() -> {
                DatagramPacket query = new DatagramPacket(new byte[64],64);
                responder.receive(query);
                byte[] bytes = ("SPD1_ROOM "+id+" 47821").getBytes(StandardCharsets.US_ASCII);
                DatagramPacket reply = new DatagramPacket(bytes,bytes.length,query.getSocketAddress());
                responder.send(reply);
                responder.send(reply);
                return null;
            });
            new Thread(replies).start();
            Map<UUID,InetSocketAddress> found = LanClient.discover(new InetSocketAddress(loop,responder.getLocalPort()),300);
            replies.get(1,TimeUnit.SECONDS);
            assertEquals(Collections.singletonMap(id,new InetSocketAddress(loop,47821)),found);
        }
    }

    @Test(timeout=10000)
    public void silentEndpointTimesOutAndRefusedConnectHasCode() throws Exception {
        InetSocketAddress address;
        try (ServerSocket silent = new ServerSocket(0,1,loop)) {
            address = new InetSocketAddress(loop,silent.getLocalPort());
            assertEquals(LanClient.Failure.READ_TIMEOUT,
                    assertThrows(LanClient.NetworkException.class,() -> new LanClient(address)).code);
        }
        assertEquals(LanClient.Failure.CONNECT_FAILED,
                assertThrows(LanClient.NetworkException.class,() -> new LanClient(address)).code);
    }

    @Test(timeout=8000)
    public void queuePressureAndTruncatedFrameDoNotReachEngine() throws Exception {
        try (LanTransport transport = new LanTransport(loop,0,0,UUID.randomUUID())) {
            InetSocketAddress address = new InetSocketAddress(loop,transport.listener.socket().getLocalPort());
            try (Socket partial = new Socket()) {
                partial.connect(address);
                DataOutputStream out = new DataOutputStream(partial.getOutputStream());
                out.writeInt(24);
                out.writeByte('1');
            }
            try (Socket sender = new Socket()) {
                sender.connect(address);
                sender.setSoTimeout(3000);
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(sender.getOutputStream()));
                for (int id=1; id<=65; id++) {
                    byte[] bytes = ("1 "+id+" - HELLO").getBytes(StandardCharsets.US_ASCII);
                    out.writeInt(bytes.length);
                    out.write(bytes);
                }
                out.flush();
                assertEquals(-1,sender.getInputStream().read());
                assertEquals(64,transport.requests.size());
                for (int id=1; id<=64; id++) {
                    LanTransport.Request request = transport.requests.remove();
                    assertTrue(request.peer.dead);
                    assertEquals(String.valueOf(id),request.fields[1]);
                }
            }
        }
    }
}
