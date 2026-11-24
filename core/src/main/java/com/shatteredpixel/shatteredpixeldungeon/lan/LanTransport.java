package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/** One nonblocking I/O worker owns every channel. No game objects cross this boundary. */
final class LanTransport implements Closeable {
    static final class Peer {
        final SocketChannel channel;
        final boolean loopback;
        final ByteBuffer input = ByteBuffer.allocate(4 + LanClient.MAX_FRAME);
        final ArrayBlockingQueue<ByteBuffer> output = new ArrayBlockingQueue<>(16);
        volatile boolean dead, draining;
        // Accessed only by the room executor.
        long lastRequest;
        boolean greeted, syncMode;
        long lastFrame = System.nanoTime(), rateStart = lastFrame, lastWrite = lastFrame;
        int count;
        Peer(SocketChannel channel) {
            this.channel = channel;
            loopback = channel.socket().getInetAddress().isLoopbackAddress();
        }
        void reply(String response) {
            byte[] bytes = response.getBytes(StandardCharsets.US_ASCII);
            ByteBuffer frame = ByteBuffer.allocate(4 + bytes.length);
            frame.putInt(bytes.length).put(bytes);
            ((Buffer)frame).flip();
            if (bytes.length > LanClient.MAX_FRAME || !output.offer(frame)) dead = true;
        }
    }
    static final class Request {
        final Peer peer;
        final String[] fields;
        Request(Peer peer, String[] fields) { this.peer = peer; this.fields = fields; }
    }

    final ArrayBlockingQueue<Request> requests = new ArrayBlockingQueue<>(64);
    final ServerSocketChannel listener;
    final DatagramChannel discovery;
    final String advert;
    final Thread worker;
    private volatile boolean closing;
    volatile String failure, discoveryFailure;
    volatile boolean discoverable = true;
    private final List<Peer> peers = new ArrayList<>();

    LanTransport(InetAddress bind, int tcp, int udp, UUID room) throws IOException {
        listener = ServerSocketChannel.open();
        try {
            listener.socket().bind(new InetSocketAddress(bind,tcp), 8);
            listener.configureBlocking(false);
            discovery = openDiscovery(bind, udp);
        } catch (IOException | RuntimeException e) { listener.close(); throw e; }
        advert = "SPD1_ROOM " + room + " " + listener.socket().getLocalPort();
        worker = new Thread(this::run, "spd-lan-io");
        worker.start();
    }

    private DatagramChannel openDiscovery(InetAddress bind, int port) {
        DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(bind,port));
            channel.configureBlocking(false);
            return channel;
        } catch (IOException e) {
            closeQuietly(channel);
            discoveryFailure = "UDP_UNAVAILABLE";
            return null;
        } catch (RuntimeException e) {
            closeQuietly(channel);
            throw e;
        }
    }

    private void pollDiscovery(ByteBuffer datagram) {
        if (discovery == null || discoveryFailure != null) return;
        try {
            ((Buffer)datagram).clear();
            SocketAddress source = discovery.receive(datagram);
            if (source != null && discoverable && "SPD1 DISCOVER".equals(
                    new String(datagram.array(),0,datagram.position(),StandardCharsets.US_ASCII))) {
                discovery.send(ByteBuffer.wrap(advert.getBytes(StandardCharsets.US_ASCII)),source);
            }
        } catch (IOException e) {
            discoveryFailure = "UDP_UNAVAILABLE";
            closeQuietly(discovery);
        }
    }

    private void run() {
        ByteBuffer datagram = ByteBuffer.allocate(96);
        try {
            while (!closing) {
                SocketChannel channel = listener.accept();
                if (channel != null) {
                    if (peers.size() >= 8) channel.close();
                    else {
                        Peer peer = new Peer(channel);
                        peers.add(peer);
                        channel.configureBlocking(false);
                        channel.socket().setTcpNoDelay(true);
                    }
                }
                pollDiscovery(datagram);
                long now = System.nanoTime();
                for (Iterator<Peer> it = peers.iterator(); it.hasNext();) {
                    Peer peer = it.next();
                    try {
                        if (!peer.dead) service(peer, now);
                    } catch (IOException e) { peer.dead = true; }
                    if (peer.dead) { peer.channel.close(); it.remove(); }
                }
                Thread.sleep(peers.isEmpty() ? 20 : 2);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) { failure = e.toString(); }
        finally {
            for (Peer peer : peers) { peer.dead = true; closeQuietly(peer.channel); }
            closeQuietly(discovery);
            closeQuietly(listener);
        }
    }

    private void service(Peer peer, long now) throws IOException {
        ByteBuffer outgoing = peer.output.peek();
        if (outgoing == null) peer.lastWrite = now;
        else {
            if (peer.channel.write(outgoing) > 0) peer.lastWrite = now;
            if (!outgoing.hasRemaining()) peer.output.remove();
            if (now - peer.lastWrite > 5000000000L) peer.dead = true;
        }
        if (peer.draining) {
            if (peer.output.isEmpty()) peer.dead = true;
            return;
        }
        if (now - peer.lastFrame > 10000000000L) { peer.dead = true; return; }
        ByteBuffer input = peer.input;
        int read = peer.channel.read(input);
        if (read < 0) { peer.dead = true; return; }
        if (read == 0) return;
        ((Buffer)input).flip();
        while (input.remaining() >= 4) {
            ((Buffer)input).mark();
            int n = input.getInt();
            if (n < 1 || n > LanClient.MAX_FRAME) { peer.dead = true; break; }
            if (input.remaining() < n) { ((Buffer)input).reset(); break; }
            byte[] payload = new byte[n];
            input.get(payload);
            if (now - peer.rateStart >= 1000000000L) { peer.rateStart = now; peer.count = 0; }
            String line = new String(payload,StandardCharsets.US_ASCII);
            if (++peer.count > 128 || !line.matches("[A-Za-z0-9_ -]+")) { peer.dead = true; break; }
            String[] fields = line.split(" ", -1);
            if (fields.length < 4 || fields.length > (fields[3].equals("DO") ? 13 : 12) || !requests.offer(new Request(peer,fields))) {
                peer.dead = true; break;
            }
            peer.lastFrame = now;
        }
        input.compact();
    }

    private static void closeQuietly(Closeable socket) {
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
    }

    public void close() {
        closing = true;
        try { worker.join(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (worker.isAlive()) throw new IllegalStateException("LAN worker did not stop");
        requests.clear();
    }
}
