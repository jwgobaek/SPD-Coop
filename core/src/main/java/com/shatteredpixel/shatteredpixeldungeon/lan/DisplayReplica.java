package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/** Client-thread model. Publish the immutable image to the UI after refresh returns. */
public final class DisplayReplica {
    private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9]{0,23}");
    private static final Pattern VALUE = Pattern.compile("[A-Za-z0-9_,.\\-]{1,128}");
    private final UUID session;
    private final int seat;
    private volatile StateImage image;
    private Set<String> deaths = Collections.emptySet(), lost = Collections.emptySet();
    private int resyncs;
    private boolean closed;
    private String lastDelta;
    interface Exchange { String request(String command) throws IOException; }

    public DisplayReplica(UUID session,int seat) {
        if (seat<0 || seat>1) throw new IllegalArgumentException("Seat");
        this.session=session;
        this.seat=seat;
    }

    public boolean appliedDelta() { return lastDelta!=null; }

    public StateImage image() { return image; }
    public Set<String> witnessedDeaths() { return deaths; }
    public Set<String> lostMonsters() { return lost; }
    public int resyncCount() { return resyncs; }
    public boolean closed() { return closed; }
    public void close() {
        closed=true;
        image=null;
        deaths=lost=Collections.emptySet();
    }

    /** A new connection always checks a fresh seat-specific snapshot before voting to resume. */
    public void confirmReconnect(LanClient client) throws IOException {
        if (closed || !session.toString().equals(client.session)) throw new IOException("Session mismatch");
        receive(client::request,client.request("SYNC 0 NEW"));
        if (!"CONFIRMED".equals(client.request("CONFIRM "+image.version+" "+image.checksum)))
            throw new IOException("Snapshot confirmation failed");
    }

    /** A missing base or an invalid transfer gets one fresh, bounded snapshot attempt. */
    public void refresh(LanClient client) throws IOException {
        if (closed || !session.toString().equals(client.session)) throw new IOException("Session mismatch");
        refresh(client::request);
    }

    void refresh(Exchange client) throws IOException {
        if (closed) throw new IOException("Closed display");
        try {
            String reply=client.request(image==null ? "SYNC 0 NEW" : "SYNC "+image.version+" "+image.checksum);
            if (image!=null && reply.equals("CURRENT "+image.version+" "+image.checksum)) return;
            try {
                receive(client,reply);
            } catch (InvalidState invalid) {
                resyncs++;
                receive(client,client.request("SYNC 0 NEW"));
            }
        } catch (LanClient.NetworkException failure) {
            close();
            throw failure;
        }
    }

    static final class InvalidState extends IOException {
        InvalidState() { super("Invalid or out-of-sequence public state"); }
    }

    static final class Assembly {
        private final StringBuilder bytes=new StringBuilder();
        private final long deadline=System.nanoTime()+5000000000L;
        private int count, next;
        private String hash, token;

        boolean append(String reply) throws InvalidState {
            if (reply.length()>LanClient.MAX_FRAME) throw new InvalidState();
            String[] p=reply.split(" ",6);
            if (p.length!=6 || !p[0].equals("PART") || !p[1].matches("[0-9]{1,19}")) throw new InvalidState();
            int index, total;
            try { index=Integer.parseInt(p[2]); total=Integer.parseInt(p[3]); }
            catch (NumberFormatException e) { throw new InvalidState(); }
            if (next==0) { token=p[1]; count=total; hash=p[4]; }
            if (System.nanoTime()-deadline>=0 || !token.equals(p[1]) || index!=next || index>=count
                    || count!=total || count<1 || count>StateImage.MAX_PARTS || !hash.equals(p[4])
                    || hash.length()!=64 || p[5].isEmpty() || p[5].length()>StateImage.CHUNK
                    || (index+1<count && p[5].length()!=StateImage.CHUNK)) throw new InvalidState();
            bytes.append(p[5]);
            return ++next==count;
        }

        String finish() throws InvalidState {
            if (count==0 || next!=count || System.nanoTime()-deadline>=0
                    || !StateImage.hash(bytes.toString()).equals(hash)) throw new InvalidState();
            return bytes.toString();
        }
    }

    private void receive(Exchange client,String first) throws IOException {
        apply(readTransfer(client,first));
    }

    static String readTransfer(Exchange client,String first) throws IOException {
        Assembly transfer=new Assembly();
        String reply=first;
        while (!transfer.append(reply)) reply=client.request("PAGE "+transfer.token+" "+transfer.next);
        return transfer.finish();
    }

    /** Validate in a private copy, then publish once. Also used by replay/diagnostic readers. */
    void apply(String wire) throws InvalidState {
        if (closed || wire.length()>StateImage.MAX_BYTES) throw new InvalidState();
        String[] f=wire.split("\\|",7);
        if (f.length!=7 || !f[1].equals(session.toString()) || !f[2].equals(Integer.toString(seat))) throw new InvalidState();
        long base, version;
        try { base=Long.parseLong(f[3]); version=Long.parseLong(f[4]); }
        catch (NumberFormatException e) { throw new InvalidState(); }
        boolean full=f[0].equals("S");
        if (version<1 || (image!=null && version<image.version) || (full ? base!=0 :
                !f[0].equals("D") || base<1 || version!=base+1)) throw new InvalidState();
        if (!full && image!=null && image.version==version) {
            if (StateImage.hash(wire).equals(lastDelta)) return;
            throw new InvalidState();
        }
        if (!full && (image==null || image.version!=base)) throw new InvalidState();
        Map<String,String> rows=full ? new TreeMap<>() : new TreeMap<>(image.rows);
        Set<String> removed=new HashSet<>(), dead=new HashSet<>(), touched=new HashSet<>();
        for (String op : f[6].split(";")) {
            if (op.length()<2) throw new InvalidState();
            int split=op.indexOf('=');
            String key=op.substring(1,split<1 ? op.length() : split);
            if (!KEY.matcher(key).matches() || !touched.add(key)) throw new InvalidState();
            if (op.charAt(0)=='+') {
                if (split<=1 || !VALUE.matcher(op.substring(split+1)).matches()) throw new InvalidState();
                rows.put(key,op.substring(split+1));
            } else {
                if (full || split>=0 || (op.charAt(0)!='!' && op.charAt(0)!='-') || rows.remove(key)==null) throw new InvalidState();
                if (key.startsWith("m")) (op.charAt(0)=='!' ? dead : removed).add(key);
            }
            if (rows.size()>StateImage.MAX_ROWS) throw new InvalidState();
        }
        if (!rows.containsKey("turn") || !rows.containsKey("active") || !rows.containsKey("status")) throw new InvalidState();
        if(!full && !Objects.equals(image.rows.get("floor"),rows.get("floor"))) throw new InvalidState();
        StateImage candidate;
        try { candidate=new StateImage(session,seat,version,rows); }
        catch (IllegalArgumentException e) { throw new InvalidState(); }
        if (!candidate.checksum.equals(f[5])) throw new InvalidState();
        lastDelta=full ? null : StateImage.hash(wire);
        deaths=Collections.unmodifiableSet(dead);
        lost=Collections.unmodifiableSet(removed);
        image=candidate;
    }
}
