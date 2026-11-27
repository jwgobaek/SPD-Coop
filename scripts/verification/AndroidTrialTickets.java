package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.TrinityDiscoveries;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.watabou.utils.Bundle;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Export the two disposable credentials of a fresh RestartHost fixture to disjoint QA profiles. */
public final class AndroidTrialTickets {
    /** Smoke a copy of a prior QA generation and both original ticket bindings in a new JVM. */
    public static void checkArchivedRoom(Path root, int expectedSchema, boolean checkDiscoveryUpgrade) throws Exception {
        CoopSaves saves = new CoopSaves(root.resolve("seat0").toFile());
        if (saves.sessions().size() != 1) throw new IllegalArgumentException("Expected one archived QA room");
        UUID room = saves.sessions().get(0);
        try (CoopSaves.Slot slot = saves.existing(room)) {
            com.watabou.utils.Bundle state = slot.read(false);
            com.watabou.utils.Bundle engine = state.getBundle("engine");
            if (engine.getInt("schema") != expectedSchema) throw new IllegalArgumentException("Unexpected schema");
            Bundle upgraded = null;
            try (CoopSession game = CoopSession.restore(engine)) {
                for (int seat = 0; seat < 2; seat++) {
                    SeatTickets.Ticket ticket = new SeatTickets(root.resolve("seat"+seat).toFile(), null).read(room);
                    if (ticket.seat != seat || ticket.player != game.player(seat) ||
                            !ticket.epoch.toString().equals(state.getString("epoch")))
                        throw new IllegalStateException("Invalid archived ticket binding");
                }
                int active = game.activeSeat();
                if (active < 0 || !game.act(game.player(active), game.turn(), CoopSession.Action.WAIT, 0, null))
                    throw new IllegalStateException("No valid next action in restored QA room");
                if (game.checkpoint().getInt("schema") != expectedSchema)
                    throw new IllegalStateException("Unexpected change of schema");
                System.out.println("PASS archived schema="+expectedSchema+" floor_schema="+engine.getInt("floor_schema")
                        +" roster="+game.hero(0).heroClass+"/"+game.hero(1).heroClass+" two ticket bindings and next WAIT");
                if (checkDiscoveryUpgrade) {
                    if (expectedSchema != 21) throw new IllegalArgumentException("Expected a previous schema 21 QA generation");
                    for (int seat = 0; seat < 2; seat++)
                        for (TrinityDiscoveries.Choice choice : TrinityDiscoveries.Choice.values())
                            if (game.discoveries(seat).knows(choice)) throw new IllegalStateException("Inferred a legacy discoverer");
                    WandOfLightning wand = new WandOfLightning();
                    if (!wand.collect(game.hero(1).belongings.backpack)) throw new IllegalStateException("QA bag full");
                    wand.identify();
                    upgraded = game.checkpoint();
                    if (upgraded.getInt("schema") != 22 || CoopSession.growthSchema(upgraded) != expectedSchema
                            || engine.getInt("floor_schema") != upgraded.getInt("floor_schema"))
                        throw new IllegalStateException("Invalid migration");
                }
            }
            if (upgraded != null) {
                try (CoopSession next = CoopSession.restore(upgraded)) {
                    if (!next.discoveries(1).knows(TrinityDiscoveries.Choice.LIGHTNING)
                            || next.discoveries(0).knows(TrinityDiscoveries.Choice.LIGHTNING))
                        throw new IllegalStateException("Discovery did not restore to its owner");
                    int active = next.activeSeat();
                    if (!next.act(next.player(active), next.turn(), CoopSession.Action.WAIT, 0, null))
                        throw new IllegalStateException("No valid next action after migration");
                    System.out.println("PASS schema 21 to 22 after a new guest identification, original floor format, owner-only history, restored next WAIT");
                }
            }
        }
    }

    /** Export an independently enrolled RestartHost setup before its requested gameplay action. */
    public static void exportPrepared(Path trial, Path destination) throws Exception {
        CoopSaves saves = new CoopSaves(trial.toFile());
        if (saves.sessions().size() != 1) throw new IllegalArgumentException("Expected one test room");
        com.watabou.utils.Bundle engine;
        try (CoopSaves.Slot slot = saves.existing(saves.sessions().get(0))) {
            engine = slot.read(false).getBundle("engine");
        }
        try (CoopSession encounter = CoopSession.restore(engine)) {
            export(trial, destination, encounter);
        }
    }

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
