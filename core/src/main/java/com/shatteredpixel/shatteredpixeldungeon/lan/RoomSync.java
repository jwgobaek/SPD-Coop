package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import java.util.*;

/** All calls are serialized by LanRoom.pump. */
final class RoomSync {
    static final int CACHE = 64;
    private static final class Record {
        final String body, result;
        Record(String body,String result) { this.body=body; this.result=result; }
    }
    private static final class Seat {
        final SeatProjection view;
        long highWater, transfer;
        final Map<Long,Record> results = new LinkedHashMap<>();
        String wire, wireHash;
        long expiry;
        Seat(UUID session,int index) { view = new SeatProjection(session,index); }
        String part(long token,int index) {
            if (token != transfer || wire == null || System.nanoTime()-expiry >= 0) return "ERROR TRANSFER";
            int count = (wire.length()+StateImage.CHUNK-1)/StateImage.CHUNK;
            if (index < 0 || index >= count) return "ERROR PART";
            int start = index*StateImage.CHUNK;
            return "PART "+token+" "+index+" "+count+" "+wireHash+" "+
                    wire.substring(start,Math.min(start+StateImage.CHUNK,wire.length()));
        }
    }

    final CoopSession game;
    private final Seat[] seats;
    long version;
    String failure;
    boolean paused;

    RoomSync(UUID session,CoopSession game) {
        this.game=game;
        seats=new Seat[]{new Seat(session,0),new Seat(session,1)};
        commit(null);
    }

    private void commit(Mob attacked) {
        long next=version+1;
        for (Seat seat : seats) {
            seat.wire=null;
            seat.view.capture(game,next,paused ? "PAUSED" : "IN_GAME",attacked);
        }
        version=next;
    }

    void pause(String reason) {
        if (paused) return;
        failure=reason;
        paused=true;
        commit(null);
    }

    String sync(int index,long base,String hash) {
        Seat seat=seats[index];
        StateImage current=seat.view.current, previous=seat.view.previous;
        if (base==version && hash.equals(current.checksum)) return "CURRENT "+version+" "+hash;
        if (base!=0 && (previous==null || previous.version!=base || !previous.checksum.equals(hash)))
            return "RESYNC";
        seat.wire=current.encode(base==0 ? null : previous,seat.view.witnessedDeath);
        if (seat.wire.length()>StateImage.MAX_BYTES) seat.wire=current.encode(null,null);
        seat.wireHash=StateImage.hash(seat.wire);
        seat.transfer++;
        seat.expiry=System.nanoTime()+5000000000L;
        return seat.part(seat.transfer,0);
    }

    String part(int seat,long token,int index) { return seats[seat].part(token,index); }

    String action(int index,String[] f) {
        if (f.length!=13) return "ERROR FORMAT";
        long player=Long.parseLong(f[4]);
        int actor=Integer.parseInt(f[6]);
        if (player!=game.player(index) || Integer.parseInt(f[5])!=index || actor!=game.hero(index).id()) return "ERROR AUTH";
        long id=Long.parseLong(f[7]), base=Long.parseLong(f[8]), turn=Long.parseLong(f[9]);
        int cell=Integer.parseInt(f[11]);
        long handle=Long.parseLong(f[12]);
        if (id<=0) return "ERROR ACTION_ID";
        Seat seat=seats[index];
        String body=base+" "+turn+" "+f[10]+" "+cell+" "+handle;
        Record cached=seat.results.get(id);
        if (cached!=null) return body.equals(cached.body) ? cached.result : "ERROR CONFLICT";
        if (id<=seat.highWater) return "ERROR EXPIRED";
        seat.highWater=id;
        String outcome=apply(index,base,turn,f[10],cell,handle);
        String result="RESULT "+id+" "+index+" "+actor+" "+turn+" "+base+" "+version+" "+outcome;
        if (seat.results.size()==CACHE) seat.results.remove(seat.results.keySet().iterator().next());
        seat.results.put(id,new Record(body,result));
        return result;
    }

    private String apply(int seat,long base,long turn,String verb,int cell,long handle) {
        if (paused) return "REJECT_PAUSED";
        if (base!=version) return "REJECT_VERSION";
        if (turn!=game.turn() || seat!=game.activeSeat()) return "REJECT_TURN";
        CoopSession.Action action;
        try { action=CoopSession.Action.valueOf(verb); }
        catch (IllegalArgumentException e) { return "REJECT_ACTION"; }
        boolean usesItem=action==CoopSession.Action.PICK_UP || action==CoopSession.Action.EQUIP || action==CoopSession.Action.EAT;
        Item item=seats[seat].view.resolve(handle);
        if (usesItem ? item==null : handle!=0) return "REJECT_ITEM";
        if ((action==CoopSession.Action.MOVE || action==CoopSession.Action.ATTACK)
                && !seats[seat].view.current.rows.containsKey("t"+cell)) return "REJECT_INVALID";
        Char target=action==CoopSession.Action.ATTACK ? Actor.findChar(cell) : null;
        if (action==CoopSession.Action.ATTACK && (target==null || !seats[seat].view.current.rows.containsKey("m"+target.id())))
            return "REJECT_INVALID";
        try {
            if (!game.act(game.player(seat),turn,action,cell,item)) return "REJECT_INVALID";
            if (game.stopped()) {
                pause(game.stopReason());
                return "APPLIED_STOPPED";
            }
            commit(target instanceof Mob && ((Mob)target).wasDefeatedBy(game.hero(seat)) ? (Mob)target : null);
            return "APPLIED";
        } catch (RuntimeException failure) {
            pause(failure.toString());
            return "HALTED_UNCERTAIN";
        }
    }
}
