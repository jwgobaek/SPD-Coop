package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import java.util.*;
import com.watabou.utils.Bundle;

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
        commit(null,-1,null,"IN_GAME");
    }

    private void commit(Mob attacked,int actor,String receipt,String status) {
        long next=version+1;
        for (Seat seat : seats) {
            seat.wire=null;
            seat.view.capture(game,next,status,attacked,seat.view.seat==actor ? receipt : null);
        }
        version=next;
    }

    long highest(int seat) { return seats[seat].highWater; }

    String lookup(int index,long id) {
        if (id<=0) return "ERROR ACTION_ID";
        Seat s=seats[index];
        Record record=s.results.get(id);
        return record!=null ? record.result : id<=s.highWater ? "ERROR EXPIRED" : "UNKNOWN";
    }

    boolean matches(int index,long v,String checksum) {
        return v==version && seats[index].view.current.checksum.equals(checksum);
    }

    void connectionBoundary(UUID epoch,boolean waiting) {
        game.stairs.cancel();
        for (Seat s:seats) {
            s.view.epoch=epoch;
            if (waiting) s.view.resetHandles(s.view.cursor());
        }
        commit(null,-1,null,game.defeated() ? "PARTY_DEFEATED" : waiting ? "RECONNECTING" : "IN_GAME");
    }

    Bundle checkpoint() {
        if (paused) throw new IllegalStateException("Uncertain receipt ledger");
        Bundle b=new Bundle();
        b.put("version",version);
        for (int i=0;i<seats.length;i++) {
            Seat seat=seats[i];
            b.put("shortcutSalt"+i,seat.view.shortcutSalt.toString());
            Bundle ledger=new Bundle();
            ledger.put("highest",seat.highWater);
            ledger.put("cursor",seat.view.cursor());
            ledger.put("count",seat.results.size());
            int n=0;
            for (Map.Entry<Long,Record> e:seat.results.entrySet()) {
                Bundle record=new Bundle();
                record.put("id",e.getKey().longValue());
                record.put("body",e.getValue().body); record.put("result",e.getValue().result);
                ledger.put("r"+n++,record);
            }
            b.put("seat"+i,ledger);
        }
        return b;
    }

    void restore(Bundle b,UUID epoch) {
        version=b.getLong("version");
        if (version<=0 || version>=Long.MAX_VALUE-1) throw new IllegalArgumentException("Invalid state version");
        for (int i=0;i<seats.length;i++) {
            Seat s=seats[i];
            if (b.contains("shortcutSalt"+i)) s.view.shortcutSalt=UUID.fromString(b.getString("shortcutSalt"+i));
            Bundle ledger=b.getBundle("seat"+i);
            int count=ledger.getInt("count");
            long high=ledger.getLong("highest"), last=0;
            if (count<0 || count>CACHE || high<0 || ledger.getKeys().size()!=count+3)
                throw new IllegalArgumentException("Invalid ledger");
            s.view.resetHandles(ledger.getLong("cursor"));
            for (int n=0;n<count;n++) {
                Bundle entry=ledger.getBundle("r"+n);
                long id=entry.getLong("id");
                String body=entry.getString("body"), result=entry.getString("result");
                String[] f=result.split(" "), args=body.split(" ");
                if (id<=last || id>high || f.length!=8 || args.length!=5 || !f[0].equals("RESULT") ||
                        Long.parseLong(f[1])!=id || Integer.parseInt(f[2])!=i || Integer.parseInt(f[3])!=game.hero(i).id() ||
                        !f[4].equals(args[1]) || !f[5].equals(args[0]) || Long.parseLong(f[6])>version ||
                        Long.parseLong(f[6])<1 || !f[7].matches("APPLIED|PARTY_DEFEATED|REJECT_(DEFEATED|VERSION|TURN|ACTION|ITEM|INVALID|FLOOR|ENDING)"))
                    throw new IllegalArgumentException("Invalid action receipt");
                long base=Long.parseLong(args[0]), turn=Long.parseLong(args[1]), after=Long.parseLong(f[6]);
                Integer.parseInt(args[3]); Long.parseLong(args[4]);
                if (f[7].equals("APPLIED") || f[7].equals("PARTY_DEFEATED")) {
                    if (!args[2].equals("END_RUN") && !args[2].equals("CONTINUE_RUN")) CoopSession.Action.valueOf(args[2]);
                    else if (Integer.parseInt(args[3])!=0 || Long.parseLong(args[4])!=0 || game.ending()==CoopSession.Ending.NONE)
                        throw new IllegalArgumentException("Invalid ending receipt");
                    if (turn<1 || turn>game.turn() || base<1 || base!=after-1)
                        throw new IllegalArgumentException("Inconsistent committed boundary");
                }
                s.results.put(id,new Record(body,result)); last=id;
            }
            if (last!=high) throw new IllegalArgumentException("Ledger ceiling");
            s.highWater=high;
        }
        connectionBoundary(epoch,true);
    }

    void peerDisconnected() {
        if (game.defeated()) return;
        pause("DISCONNECTED","Peer disconnected",-1,null);
    }

    private void pause(String code,String reason,int actor,String receipt) {
        if (paused) return;
        failure=reason;
        paused=true;
        commit(null,actor,receipt,code);
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
        String outcome=apply(index,id,base,turn,f[10],cell,handle);
        String result="RESULT "+id+" "+index+" "+actor+" "+turn+" "+base+" "+version+" "+outcome;
        if (seat.results.size()==CACHE) seat.results.remove(seat.results.keySet().iterator().next());
        seat.results.put(id,new Record(body,result));
        return result;
    }

    private String apply(int seat,long id,long base,long turn,String verb,int cell,long handle) {
        if (game.defeated()) return "REJECT_DEFEATED";
        if (paused) return "REJECT_PAUSED";
        if (base!=version) return "REJECT_VERSION";
        if (turn!=game.turn()) return "REJECT_TURN";
        if (verb.equals("END_RUN") || verb.equals("CONTINUE_RUN")) {
            if (cell!=0 || handle!=0 || !game.chooseEnding(seat,verb.equals("END_RUN"))) return "REJECT_INVALID";
            commit(null,seat,id+","+verb+",APPLIED","IN_GAME");
            return "APPLIED";
        }
        if (game.awaitingEnding()) return "REJECT_ENDING";
        if (verb.equals("CONSENT") || verb.equals("CANCEL_TRAVEL")) {
            if(handle!=0 || !game.stairs.consent(seat,turn,cell,verb.equals("CANCEL_TRAVEL"))) return "REJECT_INVALID";
            commit(null,seat,id+","+verb+",APPLIED","IN_GAME");
            return "APPLIED";
        }
        if (seat!=game.activeSeat()) return "REJECT_TURN";
        CoopSession.Action action;
        try { action=CoopSession.Action.valueOf(verb); }
        catch (IllegalArgumentException e) { return "REJECT_ACTION"; }
        boolean usesItem=action.requiresItem();
        Item item=seats[seat].view.resolve(handle);
        if (usesItem ? item==null : handle!=0) return "REJECT_ITEM";
        if ((action==CoopSession.Action.MOVE || action==CoopSession.Action.ATTACK)
                && !seats[seat].view.current.rows.containsKey("t"+cell)) return "REJECT_INVALID";
        boolean targeted=action==CoopSession.Action.ATTACK || action==CoopSession.Action.ZAP || action==CoopSession.Action.SHOOT;
        Char target=(targeted || action==CoopSession.Action.THROW) ? Actor.findChar(cell) : null;
        if (action==CoopSession.Action.THROW && (target!=null && !seats[seat].view.current.rows.containsKey("m"+target.id())
                || !seats[seat].view.current.rows.containsKey("t"+cell))) return "REJECT_INVALID";
        String container=seats[seat].view.current.rows.get("h"+cell);
        boolean aimingAtChest=action==CoopSession.Action.ZAP && ("CHEST".equals(container) || "CRYSTAL_CHEST".equals(container));
        if (targeted && !aimingAtChest && (target==null || !seats[seat].view.current.rows.containsKey("m"+target.id())))
            return "REJECT_INVALID";
        try {
            if (!game.act(game.player(seat),turn,action,cell,item)) {
                if ((action==CoopSession.Action.TRAVEL || action==CoopSession.Action.VAULT_REWARD) && game.restoredAfterTravel()) {
                    for(Seat member:seats) member.view.resetHandles(member.view.cursor());
                    commit(null,seat,id+",TRAVEL,REJECT_FLOOR","IN_GAME");
                    return "REJECT_FLOOR";
                }
                return "REJECT_INVALID";
            }
            if (game.defeated()) {
                commit(null,seat,id+","+verb+",PARTY_DEFEATED","PARTY_DEFEATED");
                return "PARTY_DEFEATED";
            }
            if (game.stopped()) {
                pause("ENGINE_STOPPED",game.stopReason(),seat,id+","+verb+",APPLIED_STOPPED");
                return "APPLIED_STOPPED";
            }
            Mob defeated=target instanceof Mob && ((Mob)target).wasDefeatedBy(game.hero(seat)) ? (Mob)target : null;
            String displayVerb=action==CoopSession.Action.THROW && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife ? "KNIFE" : verb;
            String detail=targeted || action==CoopSession.Action.THROW ? displayVerb+","+cell : verb;
            commit(defeated,seat,id+","+detail+",APPLIED","IN_GAME");
            return "APPLIED";
        } catch (RuntimeException failure) {
            pause("ENGINE_ERROR",failure.toString(),seat,id+","+verb+",HALTED_UNCERTAIN");
            return "HALTED_UNCERTAIN";
        }
    }
}
