package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Immutable private preferences. An empty quantity never confers authority to use an item. */
public final class SeatQuickslots {
    public static final int COUNT=4;
    private final Binding[] slots;
    private static final String[] DEFAULT_KINDS={"STAFF","BOW","FOOD","HEALING"};

    private static final String[] ROGUE_KINDS={"CLOAK","KNIFE","FOOD","HEALING"};

    private static final class Binding {
        final String ref, family;
        final int sprite;
        Binding(String ref,String family,int sprite) {
            this.ref=ref; this.family=family; this.sprite=sprite;
        }
        Binding(PublicDungeon.Loot item) { this(item.reference,item.stackType,item.sprite); }
    }

    private SeatQuickslots(Binding[] slots) { this.slots=slots; }
    public static SeatQuickslots empty() { return new SeatQuickslots(new Binding[COUNT]); }
    public static SeatQuickslots defaults(PublicDungeon view) {
        SeatQuickslots layout=empty();
        String[] defaults = "CLERIC".equals(view.heroClass) ? new String[]{"TOME","","FOOD","HEALING"} : "DUELIST".equals(view.heroClass) ? new String[]{"MELEE","SPIKE","FOOD","HEALING"} : "ROGUE".equals(view.heroClass) ? ROGUE_KINDS : DEFAULT_KINDS;
        for (int i=0;i<COUNT;i++) {
            for (PublicDungeon.Loot item:view.belongings) {
                if (defaults[i].equals(item.kind) && !item.reference.isEmpty()) {
                    layout=layout.bind(i,item);
                    break;
                }
            }
        }
        return layout;
    }
    public boolean assigned(int slot) { return slots[slot]!=null; }
    public int sprite(int slot) { return assigned(slot) ? slots[slot].sprite : -1; }
    public PublicDungeon.Loot item(int index,PublicDungeon view) {
        if (!assigned(index)) return null;
        for (PublicDungeon.Loot item:view.belongings)
            if (slots[index].ref.equals(item.reference)) return item;
        return null;
    }
    SeatQuickslots bind(int index,PublicDungeon.Loot item) {
        Binding[] next=slots.clone();
        if (item!=null) {
            if (item.reference.isEmpty()) throw new IllegalArgumentException("Missing item reference");
            for (int i=0;i<COUNT;i++) {
                if (i!=index && assigned(i) && slots[i].ref.equals(item.reference)) next[i]=slots[index];
            }
        }
        next[index]=item==null ? null : new Binding(item);
        return new SeatQuickslots(next);
    }
    SeatQuickslots reconnectItems(PublicDungeon view) {
        Set<String> reserved=new HashSet<>();
        for (Binding slot:slots) if (slot!=null) reserved.add(slot.ref);
        SeatQuickslots layout=this;
        for (int i=0;i<COUNT;i++) {
            Binding slot=slots[i];
            if (slot==null || slot.family.isEmpty() || item(i,view)!=null) continue;
            for (PublicDungeon.Loot candidate:view.belongings) {
                if (!candidate.reference.isEmpty() && slot.family.equals(candidate.stackType) && reserved.add(candidate.reference)) {
                    layout=layout.bind(i,candidate);
                    break;
                }
            }
        }
        return layout;
    }
    void write(DataOutput out) throws IOException {
        for (Binding slot:slots) {
            out.writeBoolean(slot!=null);
            if (slot!=null) { out.writeUTF(slot.ref); out.writeUTF(slot.family); out.writeShort(slot.sprite); }
        }
    }
    static SeatQuickslots read(DataInput in) throws IOException {
        Binding[] slots=new Binding[COUNT];
        Set<String> refs=new HashSet<>();
        for (int i=0;i<COUNT;i++) if (in.readBoolean()) {
            String ref=in.readUTF(), family=in.readUTF();
            int sprite=in.readUnsignedShort();
            if (!ref.matches("[0-9a-f]{32}") || !refs.add(ref) || !family.matches("[a-z0-9._]{0,160}") || sprite>1023)
                throw new IOException("SHORTCUTS");
            slots[i]=new Binding(ref,family,sprite);
        }
        return new SeatQuickslots(slots);
    }
}
