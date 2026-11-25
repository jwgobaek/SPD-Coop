package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.security.*;
import java.util.*;

/** App-private resume metadata. A pending action is recorded before its first network write. */
public final class SeatTickets {
    public interface Keys {
        SeatCredential create(UUID session) throws GeneralSecurityException, IOException;
        SeatCredential load(UUID session) throws GeneralSecurityException, IOException;
    }

    public static final class Ticket {
        public final UUID session;
        public final int seat;
        public final long player;
        public String host;
        public int port;
        final SeatCredential key;
        boolean nativeKey;
        UUID epoch;
        long pending;
        SeatQuickslots quickslots, vaultQuickslots;
        SeatQuickslots layout(boolean vault) { return vault ? vaultQuickslots : quickslots; }
        void layout(boolean vault,SeatQuickslots layout) {
            if (vault) vaultQuickslots=layout;
            else quickslots=layout;
        }
        Ticket(UUID session,int seat,long player,String host,int port,UUID epoch,SeatCredential key) {
            this.session=session; this.seat=seat; this.player=player; this.host=host;
            this.port=port; this.epoch=epoch; this.key=key;
        }
    }
    private final File directory;
    private final Keys keys;
    public SeatTickets(File root,Keys keys) throws IOException {
        directory=new File(root,"coop-seats-v1");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("STORAGE");
        this.keys=keys;
    }
    public List<UUID> sessions() throws IOException {
        File[] files=directory.listFiles();
        if (files==null) throw new IOException("STORAGE");
        List<UUID> ids=new ArrayList<>();
        for(File f:files) if(f.getName().endsWith(".seat")) {
            try { ids.add(UUID.fromString(f.getName().substring(0,f.getName().length()-5))); }
            catch(IllegalArgumentException ignored) {}
        }
        Collections.sort(ids);
        return ids;
    }

    Ticket enroll(LanClient client,int seat,long player,String host,int port) throws Exception {
        UUID id=UUID.fromString(client.session);
        SeatCredential key=keys==null ? SeatCredential.generate() : keys.create(id);
        String result=key.enroll(client);
        if (!result.startsWith("ENROLLED ")) throw new IOException("ENROLL");
        Ticket ticket=new Ticket(id,seat,player,host,port,UUID.fromString(result.substring(9)),key);
        ticket.nativeKey=keys!=null;
        write(ticket);
        return ticket;
    }

    void write(Ticket t) throws IOException {
        File temp=new File(directory,t.session+".tmp");
        try(FileOutputStream file=new FileOutputStream(temp); DataOutputStream out=new DataOutputStream(file)) {
            out.writeInt(t.vaultQuickslots==null ? 2 : 3); out.writeUTF(t.session.toString()); out.writeInt(t.seat); out.writeLong(t.player);
            out.writeUTF(t.host); out.writeInt(t.port); out.writeUTF(t.epoch.toString()); out.writeLong(t.pending);
            out.writeBoolean(t.nativeKey);
            if (!t.nativeKey) t.key.writePrivate(out);
            out.writeBoolean(t.quickslots!=null);
            if (t.quickslots!=null) t.quickslots.write(out);
            if (t.vaultQuickslots!=null) t.vaultQuickslots.write(out);
            out.flush(); file.getFD().sync();
        }
        if (!temp.renameTo(new File(directory,t.session+".seat"))) throw new IOException("STORAGE");
        CoopSaves.syncDirectory(directory);
    }
    public Ticket read(UUID id) throws Exception {
        File file=new File(directory,id+".seat");
        if (file.length()>4096) throw new IOException("CREDENTIAL");
        try(DataInputStream in=new DataInputStream(new FileInputStream(file))) {
            int version=in.readInt();
            if ((version<1 || version>3) || !id.toString().equals(in.readUTF())) throw new IOException("CREDENTIAL");
            int seat=in.readInt(); long player=in.readLong();
            String host=in.readUTF(); int port=in.readInt(); UUID epoch=UUID.fromString(in.readUTF());
            long pending=in.readLong();
            boolean nativeKey=in.readBoolean();
            if (seat<0 || seat>1 || player<1 || port<1 || port>65535 || pending<0 || host.length()>255 ||
                    (nativeKey && keys==null)) throw new IOException("CREDENTIAL");
            SeatCredential credential=nativeKey ? keys.load(id) : SeatCredential.readPrivate(in);

            Ticket ticket=new Ticket(id,seat,player,host,port,epoch,credential);
            if (version>=2 && in.readBoolean()) ticket.quickslots=SeatQuickslots.read(in);
            if (version==3) ticket.vaultQuickslots=SeatQuickslots.read(in);
            if (in.read()!=-1) throw new IOException("CREDENTIAL");
            ticket.pending=pending; ticket.nativeKey=nativeKey;
            return ticket;
        }
    }
}
