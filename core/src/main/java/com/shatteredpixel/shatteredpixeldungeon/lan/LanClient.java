package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Blocking request/reply endpoint. Use on a client worker thread. */
public final class LanClient implements Closeable {
    public static final int UDP_PORT = 47820, TCP_PORT = 47821, MAX_FRAME = 2048;
    static final int TIMEOUT = 5000;
    private final SocketChannel channel;
    private final Socket socket;
    private final DataInputStream reader;
    private long sequence,transferredBytes;
    public final String session;

    public enum Failure { CONNECT_TIMEOUT, CONNECT_FAILED, WRITE_TIMEOUT, READ_TIMEOUT, CONNECTION_LOST, PROTOCOL }
    public static final class NetworkException extends IOException {
        public final Failure code;
        NetworkException(Failure code, IOException cause) {
            super(code.name(), cause);
            this.code = code;
        }
    }

    public LanClient(InetSocketAddress address) throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        try {
            try { socket.connect(address, TIMEOUT); }
            catch (SocketTimeoutException timeout) { throw new NetworkException(Failure.CONNECT_TIMEOUT,timeout); }
            catch (IOException failed) { throw new NetworkException(Failure.CONNECT_FAILED,failed); }
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(TIMEOUT);
            reader = new DataInputStream(socket.getInputStream());
            String[] hello = exchange("-", "HELLO").split(" ");
            if (hello.length != 2 || !hello[0].equals("HELLO")) throw new NetworkException(Failure.PROTOCOL,new IOException("BAD_HELLO"));
            try { session = UUID.fromString(hello[1]).toString(); }
            catch (IllegalArgumentException invalid) { throw new NetworkException(Failure.PROTOCOL,new IOException("BAD_SESSION")); }
        } catch (IOException failure) { close(); throw failure; }
    }

    static final String PLAY_MODE="SYNC27";

    /** Negotiate the synchronized protocol before joining a seat. */
    public static LanClient synchronizedGame(InetSocketAddress address) throws IOException {
        LanClient client = new LanClient(address);
        try {
            if (!("MODE "+PLAY_MODE).equals(client.request("MODE "+PLAY_MODE))) throw new IOException("VERSION");
            return client;
        } catch (IOException failure) {
            client.close();
            throw failure;
        }
    }

    /** Local endpoint status; PING checks whether the remote room is still reachable. */
    public boolean isOpen() { return channel.isOpen() && !socket.isClosed(); }

    /** Completed TCP exchanges, including the length headers in both directions. */
    public synchronized long transferredBytes() { return transferredBytes; }
    public synchronized long requestCount() { return sequence; }

    public synchronized String request(String command) throws IOException {
        return exchange(session, command);
    }

    synchronized String exchange(String room, String command) throws IOException {
        String prefix = "1 " + (++sequence) + " ";
        byte[] payload = (prefix + room + " " + command).getBytes(StandardCharsets.US_ASCII);
        if (payload.length > MAX_FRAME) throw new NetworkException(Failure.PROTOCOL,new IOException("FRAME_SIZE"));
        try {
            ByteBuffer frame = ByteBuffer.allocate(4 + payload.length);
            frame.putInt(payload.length).put(payload);
            ((Buffer)frame).flip();
            channel.configureBlocking(false);
            long deadline = System.nanoTime() + TIMEOUT * 1000000L;
            while (frame.hasRemaining()) {
                channel.write(frame);
                if (frame.hasRemaining()) {
                    if (System.nanoTime() >= deadline) throw new NetworkException(Failure.WRITE_TIMEOUT,new SocketTimeoutException());
                    try { Thread.sleep(1); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new InterruptedIOException(); }
                }
            }
            channel.configureBlocking(true);
            int length = reader.readInt();
            if (length < 1 || length > MAX_FRAME) throw new NetworkException(Failure.PROTOCOL,new IOException("FRAME_SIZE"));
            byte[] bytes = new byte[length];
            reader.readFully(bytes);
            transferredBytes+=8L+payload.length+length;
            String reply = new String(bytes, StandardCharsets.US_ASCII);
            if (!reply.startsWith(prefix)) throw new NetworkException(Failure.PROTOCOL,new IOException("RESPONSE_ID"));
            return reply.substring(prefix.length());
        } catch (IOException failure) {
            close();
            if (failure instanceof NetworkException) throw failure;
            throw new NetworkException(failure instanceof SocketTimeoutException ? Failure.READ_TIMEOUT : Failure.CONNECTION_LOST, failure);
        }
    }

    public void close() throws IOException { socket.close(); }

    public static Map<UUID, InetSocketAddress> discover(int timeoutMs) throws IOException {
        return discover(new InetSocketAddress(InetAddress.getByName("255.255.255.255"), UDP_PORT), timeoutMs);
    }

    /** Destination may be an interface broadcast address or a unicast LAN address. */
    public static Map<UUID, InetSocketAddress> discover(InetSocketAddress destination, int timeoutMs) throws IOException {
        if (timeoutMs < 1 || timeoutMs > 30000) throw new IllegalArgumentException("timeoutMs");
        Map<UUID, InetSocketAddress> found = new LinkedHashMap<>();
        byte[] query = "SPD1 DISCOVER".getBytes(StandardCharsets.US_ASCII);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.send(new DatagramPacket(query, query.length, destination));
            long until = System.nanoTime() + timeoutMs * 1000000L;
            while (System.nanoTime() < until && found.size() < 64) {
                socket.setSoTimeout((int)Math.max(1, (until-System.nanoTime())/1000000));
                DatagramPacket packet = new DatagramPacket(new byte[96], 96);
                try { socket.receive(packet); } catch (SocketTimeoutException done) { break; }
                String[] fields = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII).split(" ");
                if (fields.length != 3 || !fields[0].equals("SPD1_ROOM")) continue;
                try {
                    UUID room = UUID.fromString(fields[1]);
                    int port = Integer.parseInt(fields[2]);
                    if (port > 0) found.put(room, new InetSocketAddress(packet.getAddress(), port));
                } catch (IllegalArgumentException ignored) { /* unrelated UDP traffic */ }
            }
        }
        return found;
    }
}
