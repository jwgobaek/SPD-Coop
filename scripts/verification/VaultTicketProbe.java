package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.UUID;

/** Restart the client JVM with the same private ticket directory for each of the three phases. */
public final class VaultTicketProbe {
    private static byte[] layout(int marker) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            for (int slot = 0; slot < SeatQuickslots.COUNT; slot++) {
                out.writeBoolean(slot == marker % SeatQuickslots.COUNT);
                if (slot == marker % SeatQuickslots.COUNT) {
                    out.writeUTF(String.format("%032x", marker));
                    out.writeUTF("potion.healing");
                    out.writeShort(marker);
                }
            }
        }
        return bytes.toByteArray();
    }

    private static SeatQuickslots binding(int marker) throws Exception {
        return SeatQuickslots.read(new DataInputStream(new ByteArrayInputStream(layout(marker))));
    }

    private static void check(SeatQuickslots slots, int marker) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        slots.write(new DataOutputStream(bytes));
        if (!Arrays.equals(layout(marker), bytes.toByteArray())) throw new AssertionError("Changed binding");
    }

    public static void main(String[] args) throws Exception {
        UUID session = UUID.fromString(args[0]);
        String phase = args[2];
        for (int seat = 0; seat < 2; seat++) {
            File directory = new File(args[1], "client" + seat);
            SeatTickets storage = new SeatTickets(directory, null);
            if (phase.equals("town")) {
                SeatTickets.Ticket ticket = new SeatTickets.Ticket(session, seat, 10 + seat, "localhost",
                        13333, UUID.randomUUID(), SeatCredential.generate());
                ticket.layout(false, binding(10 + seat));
                storage.write(ticket);
            } else {
                SeatTickets.Ticket ticket = storage.read(session);
                if (ticket.seat != seat || ticket.player != 10 + seat) throw new AssertionError("Changed identity");
                check(ticket.layout(false), 10 + seat);
                if (phase.equals("enter")) {
                    if (ticket.layout(true) != null) throw new AssertionError("Unexpected vault layout");
                    ticket.layout(true, binding(20 + seat));
                    storage.write(ticket);
                } else if (phase.equals("return")) {
                    check(ticket.layout(true), 20 + seat);
                    ticket.layout(true, null);
                    storage.write(ticket);
                } else if (!phase.equals("read") || ticket.layout(true) != null) {
                    throw new AssertionError("Unexpected ticket state");
                }
            }
        }
        System.out.println("PASS private client ticket phase " + phase + " for both seats");
    }
}
