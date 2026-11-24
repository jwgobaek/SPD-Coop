package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Immutable seat-specific display state. No game engine dependency. */
public final class StateImage {
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
