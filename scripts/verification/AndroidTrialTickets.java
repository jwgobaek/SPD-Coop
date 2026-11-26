package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Export the two disposable credentials of a fresh RestartHost fixture to disjoint QA profiles. */
public final class AndroidTrialTickets {
    public static void export(Path trial, Path destination, CoopSession encounter) throws Exception {
        Path output = Files.createDirectory(destination);
        CoopSaves saves = new CoopSaves(trial.toFile());
        if (saves.sessions().size() != 1) throw new IllegalArgumentException("Expected one test room");
        UUID room = saves.sessions().get(0);
        UUID epoch = UUID.fromString(Files.readString(trial.resolve("epoch")).trim());
        try (CoopSaves.Slot slot = saves.existing(room)) {
            com.watabou.utils.Bundle state = slot.read(false);
            if (!epoch.toString().equals(state.getString("epoch"))) throw new IllegalStateException("Fixture epoch mismatch");
            for (int seat = 0; seat < 2; seat++) {
                if (state.getBundle("sync").getBundle("seat" + seat).getLong("highest") != 0)
                    throw new IllegalStateException("Expected an unplayed verification fixture");
                Path credential = trial.resolve("private" + seat);
                try (DataInputStream in = new DataInputStream(Files.newInputStream(credential))) {
                    if (!in.readUTF().equals(state.getString("key" + seat))) throw new IllegalStateException("Fixture seat mismatch");
                }
                try (InputStream in = Files.newInputStream(credential)) {
                    SeatCredential key = SeatCredential.readPrivate(in);
                    Path device = Files.createDirectory(output.resolve("seat" + seat));
                    if (seat == 0) {
                        try (CoopSaves.Slot target = new CoopSaves(device.toFile()).open(room)) {
                            if (target.currentGeneration() != 0) throw new IllegalStateException("QA room collision");
                            state.put("engine", encounter.checkpoint());
                            state.put("sync", new RoomSync(room, encounter).checkpoint());
                            target.publish(state, false);
                        }
                    }
                    SeatTickets tickets = new SeatTickets(device.toFile(), null);
                    long player = state.getBundle("engine").getLongArray("players")[seat];
                    SeatTickets.Ticket ticket = new SeatTickets.Ticket(room, seat, player,
                            "127.0.0.1", LanClient.TCP_PORT, epoch, key);
                    tickets.write(ticket);
                    if (tickets.read(room).seat != seat) throw new IllegalStateException("Fixture ticket round-trip");
                }
            }
        }
        System.out.println("PASS fresh, unplayed test room and two independently enrolled test seats");
    }
}
