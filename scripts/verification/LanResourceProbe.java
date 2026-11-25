package com.shatteredpixel.shatteredpixeldungeon;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import static org.junit.Assert.*;

/** Cloud Linux only. Count sockets owned by this JVM after closing real rooms and reconnecting clients. */
public final class LanResourceProbe {
    private static long sockets() throws IOException {
        long count = 0;
        try (Stream<Path> files = Files.list(Path.of("/proc/self/fd"))) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (Files.readSymbolicLink(file).toString().startsWith("socket:[")) count++;
            }
        }
        return count;
    }

    private static void session() {
        Result result = new JUnitCore().run(Request.method(ReconnectTest.class,
                "lockedSeatsMustProvePossessionAndVerifyTheirOwnSnapshot"));
        assertTrue(result.getFailures().toString(), result.wasSuccessful());
    }

    public static void main(String[] args) throws IOException {
        session();
        long before = sockets();
        for (int cycle = 1; cycle <= 6; cycle++) {
            session();
            long after = sockets();
            long workers = Thread.getAllStackTraces().keySet().stream()
                    .filter(t -> t.getName().equals("spd-lan-io")).count();
            System.out.println("cycle=" + cycle + " warmed_socket_fds=" + before
                    + " closed_room_socket_fds=" + after + " live_lan_workers=" + workers);
            assertEquals(before, after);
            assertEquals(0L, workers);
        }
    }
}
