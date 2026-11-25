package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import static org.junit.Assert.assertTrue;

/** Measures a durable action acknowledgement; the external observer identifies the summon generation. */
public final class SummonResponseProbe {
    public static void main(String[] args) throws Exception {
        Path profile = Paths.get(args[0]);
        int port = Integer.parseInt(RestartHost.read(profile, "endpoint").split("\n")[0]);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
        UUID epoch = UUID.fromString(RestartHost.read(profile, "epoch"));
        try (LanClient first = LanClient.synchronizedGame(address);
             LanClient second = LanClient.synchronizedGame(address)) {
            LanClient[] clients = {first, second};
            UUID session = UUID.fromString(first.session);
            DisplayReplica[] models = new DisplayReplica[2];
            SeatActions[] actions = new SeatActions[2];
            for (int seat = 0; seat < 2; seat++) {
                try (InputStream in = Files.newInputStream(profile.resolve("private" + seat))) {
                    String response = SeatCredential.readPrivate(in).reconnect(clients[seat], epoch, seat, seat + 1);
                    assertTrue(response.startsWith("RESUMED "));
                    actions[seat] = new SeatActions(session, seat, seat + 1, Long.parseLong(response.split(" ")[4]));
                    models[seat] = new DisplayReplica(session, seat);
                }
            }
            for (int seat = 0; seat < 2; seat++) models[seat].confirmReconnect(clients[seat]);
            BufferedReader commands = new BufferedReader(new InputStreamReader(System.in));
            while ("WAIT".equals(commands.readLine())) {
                for (int seat = 0; seat < 2; seat++) models[seat].refresh(clients[seat]);
                int seat = Integer.parseInt(models[0].image().rows.get("active"));
                SeatActions.Pending pending = actions[seat].prepare(models[seat].image(), "WAIT", 0, 0);
                long start = System.nanoTime();
                String receipt = pending.submit(clients[seat]);
                long elapsed = (System.nanoTime() - start) / 1000;
                assertTrue(receipt.endsWith(" APPLIED"));
                System.out.println(elapsed);
            }
        }
    }
}
