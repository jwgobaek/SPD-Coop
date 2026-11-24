package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Immutable seat-specific display state. No game engine dependency. */
public final class StateImage {
    static final int TILE_GROUP = 32;
    static final String TILES = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Current terrain is in t rows. Returns -1 for unseen or currently visible cells. */
    public int rememberedTerrain(int cell) {
        if (cell < 0) return -1;
        String group = rows.get("r" + cell / TILE_GROUP);
        int offset = 2 * (cell % TILE_GROUP);
        if (group == null || offset + 1 >= group.length()) return -1;
        return TILES.indexOf(group.charAt(offset + 1));
    }

    public boolean mappedOnly(int cell) {
        if (rememberedTerrain(cell) < 0) return false;
        return rows.get("r" + cell / TILE_GROUP).charAt(2 * (cell % TILE_GROUP)) == 'p';
    }

    static final int CHUNK = 1400, MAX_PARTS = 64, MAX_BYTES = CHUNK * MAX_PARTS, MAX_ROWS = 2048;
    public final UUID session;
    public final int seat;
    public final long version;
    public final Map<String,String> rows;
    public final String checksum;

    StateImage(UUID session, int seat, long version, Map<String,String> rows) {
        if (rows.size() > MAX_ROWS) throw new IllegalArgumentException("State exceeds row limit");
        this.session = session;
        this.seat = seat;
        this.version = version;
        this.rows = Collections.unmodifiableMap(new TreeMap<>(rows));
        StringBuilder data = new StringBuilder(session+"|"+seat+"|"+version+"|");
        for (Map.Entry<String,String> e : this.rows.entrySet()) data.append(e.getKey()).append('=').append(e.getValue()).append(';');
        if (data.length()+rows.size() > MAX_BYTES-200) throw new IllegalArgumentException("State exceeds byte limit");
        checksum = hash(data.toString());
    }

    static String hash(String data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.US_ASCII));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) {
                out.append(Character.forDigit((b & 255) >>> 4,16));
                out.append(Character.forDigit(b & 15,16));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    /** Encoded ASCII bytes before the bounded transport splits a full snapshot into parts. */
    public int snapshotBytes() { return encode(null, null).length(); }

    String encode(StateImage base, String witnessedDeath) {
        StringBuilder wire = new StringBuilder(base == null ? "S|" : "D|");
        wire.append(session).append('|').append(seat).append('|').append(base == null ? 0 : base.version)
                .append('|').append(version).append('|').append(checksum).append('|');
        for (Map.Entry<String,String> e : rows.entrySet()) {
            if (base == null || !e.getValue().equals(base.rows.get(e.getKey())))
                wire.append('+').append(e.getKey()).append('=').append(e.getValue()).append(';');
        }
        if (base != null) for (String key : base.rows.keySet()) {
            if (!rows.containsKey(key)) wire.append(key.equals(witnessedDeath) ? '!' : '-').append(key).append(';');
        }
        return wire.toString();
    }
}
