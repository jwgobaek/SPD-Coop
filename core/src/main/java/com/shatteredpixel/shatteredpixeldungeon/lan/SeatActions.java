package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.IOException;
import java.util.UUID;

/** One sequencer per joined seat. Keep it when replacing the display during resynchronization. */
public final class SeatActions {
    private final UUID session;
    private final int seat;
    private final long player;
    private long sequence;

    public SeatActions(UUID session,int seat,long player) {
        if (seat<0 || seat>1 || player<=0) throw new IllegalArgumentException("Seat identity");
        this.session=session;
        this.seat=seat;
        this.player=player;
    }

    public SeatActions(UUID session,int seat,long player,long highWater) {
        this(session,seat,player);
        if (highWater<0) throw new IllegalArgumentException("Action ID ceiling");
        sequence=highWater;
    }

    long allocate() {
        if (sequence==Long.MAX_VALUE) throw new IllegalStateException("Action sequence exhausted");
        return ++sequence;
    }

    public Pending prepare(StateImage image,String action,int cell,long item) {
        if (!session.equals(image.session) || seat!=image.seat || !"IN_GAME".equals(image.rows.get("status"))
                || (!action.equals("CONSENT") && !action.equals("CANCEL_TRAVEL") && !Integer.toString(seat).equals(image.rows.get("active")))) throw new IllegalStateException("No input offer");
        if (!action.matches("WAIT|MOVE|ATTACK|PICK_UP|EQUIP|EAT|DROP|UNEQUIP|REVIVE|ZAP|SEARCH|CONSENT|CANCEL_TRAVEL|TRAVEL|ACCEPT_GHOST|GHOST_WEAPON|GHOST_ARMOR|SHATTER")) throw new IllegalArgumentException("Action");
        long id=allocate();
        String actor=image.rows.get("self").split(",",2)[0];
        String epoch=image.rows.get("epoch");
        String command=(epoch==null ? "DO" : "DO_"+epoch)+" "+player+" "+seat+" "+actor+" "+id+" "+image.version+" "+image.rows.get("turn")+" "+action+" "+cell+" "+item;
        return new Pending(session,id,command);
    }

    /** Submit this same immutable command again if its application-level receipt was lost. */
    public static final class Pending {
        public final long id;
        private final UUID session;
        final String command;
        private Pending(UUID session,long id,String command) { this.session=session; this.id=id; this.command=command; }

        public String submit(LanClient client) throws IOException {
            if (!session.toString().equals(client.session)) throw new IOException("Session mismatch");
            return client.request(command);
        }
    }
}
