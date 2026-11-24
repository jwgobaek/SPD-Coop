package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import java.util.WeakHashMap;

/** Engine-thread-only session identities. Item uses identity equality; retired objects need no strong root. */
public final class ItemOwnership {
    private static final class Entry {
        final long id;
        long owner;
        Entry(long id) { this.id = id; }
    }

    private final WeakHashMap<Item, Entry> entries = new WeakHashMap<>();
    private long sequence;
    private Entry entry(Item item) {
        Entry entry = entries.get(item);
        if (entry == null) {
            if (sequence == Long.MAX_VALUE) throw new IllegalStateException("Item identity exhausted");
            entry = new Entry(++sequence);
            entries.put(item, entry);
        }
        return entry;
    }

    void clear() { entries.clear(); sequence=0; }

    long sequence() { return sequence; }

    void restored(Item item,long id,long owner,long limit) {
        if (entries.containsKey(item) || id<=0 || id>limit) throw new IllegalArgumentException("Item identity");
        Entry restored=new Entry(id);
        restored.owner=owner;
        entries.put(item,restored);
        sequence=limit;
    }

    public long identity(Item item) { return entry(item).id; }
    public long owner(Item item) {
        Entry entry = entries.get(item);
        return entry == null ? 0 : entry.owner;
    }

    public boolean allowed(Item item, long player) { return owner(item) == 0 || owner(item) == player; }
    public void claim(Item item, long player) {
        if (!allowed(item, player)) throw new IllegalStateException("Item belongs to another player");
        entry(item).owner = player;
    }
    public void split(Item original, Item part) { claim(part, owner(original)); }
}
