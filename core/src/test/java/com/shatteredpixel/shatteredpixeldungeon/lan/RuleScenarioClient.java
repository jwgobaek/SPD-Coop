package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import static org.junit.Assert.*;

/** Independent display process. All gameplay commands use the negotiated production SYNC1 endpoint. */
public final class RuleScenarioClient {
    private final LanClient[] peers = new LanClient[2];
    private final DisplayReplica[] views = new DisplayReplica[2];
    private final SeatActions[] actions = new SeatActions[2];
    private int confirmed, rejected, cached, resync, revives;
    private long nonQueries;

    public static void main(String[] args) throws Exception {
        new RuleScenarioClient().run(Integer.parseInt(args[0]),Long.parseLong(args[1]));
    }

    private StateImage view(int seat) { return views[seat].image(); }
    private int active() { return Integer.parseInt(view(0).rows.get("active")); }
    private int position(int seat) { return Integer.parseInt(view(seat).rows.get("self").split(",")[1]); }

    private void refresh() throws IOException {
        for (int seat=0;seat<2;seat++) views[seat].refresh(peers[seat]);
        assertEquals(view(0).version,view(1).version);
        assertEquals(1+confirmed,view(0).version);
        for (int seat=0;seat<2;seat++) {
            Map<String,String> rows=view(seat).rows;
            for (Map.Entry<String,String> e : rows.entrySet()) {
                String k=e.getKey(), v=e.getValue();
                if (k.matches("z[0-9]+")) assertTrue(rows.containsKey("t"+k.substring(1)));
                if (k.matches("m[0-9]+")) assertTrue(rows.containsKey("t"+v.split(",")[0]));
                if (k.matches("i[0-9]+")) {
                    assertFalse(view(1-seat).rows.containsKey(k));
                    if (v.startsWith("F")) assertTrue(rows.containsKey("t"+v.substring(1,v.indexOf(','))));
                }
            }
        }
    }

    private String send(String verb,int cell,long item,boolean accept) throws Exception {
        Thread.sleep(40);
        int seat=active();
        SeatActions.Pending pending=actions[seat].prepare(view(seat),verb,cell,item);
        String receipt=pending.submit(peers[seat]);
        nonQueries++;
        if (accept) {
            assertTrue(verb+" "+cell+" at "+position(seat)+" -> "+receipt,
                    receipt.endsWith(" APPLIED") || receipt.endsWith(" PARTY_DEFEATED"));
            confirmed++;
            if ("REVIVE".equals(verb) || receipt.endsWith(" PARTY_DEFEATED")) {
                assertEquals(receipt,pending.submit(peers[seat]));
                cached++; nonQueries++;
                if ("REVIVE".equals(verb)) revives++;
            }
        } else {
            assertTrue(receipt,receipt.endsWith(" REJECT_INVALID"));
            rejected++;
        }
        refresh();
        return receipt;
    }

    private void offer(int seat) throws Exception {
        for (int i=0; i<5 && active()!=seat; i++) send("WAIT",0,0,true);
        assertEquals(seat,active());
    }

    private long item(int seat,String slot,String kind) {
        for (Map.Entry<String,String> e : view(seat).rows.entrySet())
            if (e.getKey().matches("i[0-9]+") && e.getValue().startsWith(slot+","+kind+","))
                return Long.parseLong(e.getKey().substring(1));
        throw new AssertionError("Missing "+slot+" "+kind+" at seat "+seat);
    }

    private void move(int seat,int cell) throws Exception {
        offer(seat);
        send("MOVE",cell,0,true);
        assertEquals(cell,position(seat));
    }

    private void run(int port,long seed) throws Exception {
        String host=new BufferedReader(new InputStreamReader(System.in)).readLine();
        InetSocketAddress address=new InetSocketAddress("127.0.0.1",port);
        try (LanClient a=LanClient.synchronizedGame(address); LanClient b=LanClient.synchronizedGame(address)) {
            peers[0]=a; peers[1]=b;
            for (int s=0;s<2;s++) {
                String[] joined=peers[s].request(s==0 ? host : "JOIN").split(" ");
                UUID id=UUID.fromString(peers[s].session);
                views[s]=new DisplayReplica(id,s);
                actions[s]=new SeatActions(id,s,Long.parseLong(joined[2]));
                assertEquals("OK",peers[s].request((s==0) != (seed==2402) ? "SELECT WARRIOR":"SELECT HUNTRESS"));
                assertEquals("OK",peers[s].request("READY 1"));
            }
            assertEquals("GAME_STARTED",a.request("START"));
            nonQueries=a.requestCount()+b.requestCount();
            refresh();
            send("WAIT",0,0,true);
            int center=position(0)+4, w=Integer.parseInt(view(0).rows.get("width"));
            assertEquals(center+4,position(1));
            for (int s=0;s<2;s++) {
                assertFalse(view(s).rows.containsKey("t"+position(1-s)));
                assertFalse(view(s).rows.containsKey("ally"));
                int hiddenMobCell=center+(s==0 ? 3:-3)+w;
                for (Map.Entry<String,String> e : view(s).rows.entrySet()) {
                    if (e.getKey().matches("m[0-9]+")) assertFalse(e.getValue().startsWith(hiddenMobCell+","));
                }
                assertTrue(view(s).rows.keySet().stream().anyMatch(k->k.startsWith("z")));
                offer(s);
                int pos=position(s);
                send("PICK_UP",pos,item(s,"F"+pos,s==0 ? "FOOD":"ANKH"),true);
                offer(s);
                send("ATTACK",center+(s==0 ? -3:3)+w,0,true);
            }
            int[][] routes={{center-3,center-2+w,center-1+2*w},{center+3,center+2+w,center+1+2*w}};
            for (int step=0;step<3;step++) for (int s=0;s<2;s++) move(s,routes[s][step]);
            StateImage[] atGate={view(0),view(1)};
            for (int step=1;step>=0;step--) for (int s=0;s<2;s++) move(s,routes[s][step]);
            move(0,center-4); move(1,center+4);
            for (int s=0;s<2;s++) {
                boolean retained=false;
                for (Map.Entry<String,String> e : atGate[s].rows.entrySet()) {
                    if (e.getKey().matches("t[0-9]+") && !view(s).rows.containsKey(e.getKey())) {
                        int p=Integer.parseInt(e.getKey().substring(1));
                        assertEquals(Integer.parseInt(e.getValue()),view(s).rememberedTerrain(p));
                        retained=true;
                    }
                }
                assertTrue("Retreat must leave remembered terrain",retained);
            }
            for (int step=0;step<3;step++) for (int s=0;s<2;s++) move(s,routes[s][step]);
            int drop=position(0);
            offer(0);
            send("DROP",drop,item(0,"BAG","FOOD"),true);
            move(0,drop+w);
            move(1,center+2*w); move(1,drop);
            offer(1);
            send("PICK_UP",drop,item(1,"F"+drop,"FOOD"),false);
            send("WAIT",0,0,true);
            for (int i=0;i<5 && "ALIVE".equals(view(1).rows.get("life"));i++) send("WAIT",0,0,true);
            assertEquals("DOWNED",view(1).rows.get("life"));
            assertEquals("DOWNED",view(0).rows.get("allylife"));
            assertEquals("1",view(1).rows.get("revivable"));
            long resource=item(1,"BAG","ANKH");
            assertEquals(0,active());
            send("REVIVE",drop,0,true);
            assertEquals("ALIVE",view(1).rows.get("life"));
            assertEquals("5",view(1).rows.get("self").split(",")[2]);
            assertFalse(view(1).rows.containsKey("i"+resource));
            offer(1);
            send("WAIT",0,0,true);

            DisplayReplica fresh=new DisplayReplica(UUID.fromString(b.session),1);
            boolean[] corrupt={true};
            fresh.refresh(command->{
                String frame=b.request(command);
                if (corrupt[0]) { corrupt[0]=false; return frame.substring(0,frame.length()-1)+"?"; }
                return frame;
            });
            resync+=fresh.resyncCount();
            assertEquals(1,resync);
            assertEquals(view(1).rows,fresh.image().rows);
            assertTrue(fresh.image().rows.keySet().stream().anyMatch(k->k.matches("r[0-9]+")));
            views[1]=fresh;
            while (confirmed<350 && "IN_GAME".equals(view(0).rows.get("status"))) send("WAIT",0,0,true);
            for (int s=0;s<2;s++) {
                assertEquals("PARTY_DEFEATED",view(s).rows.get("status"));
                assertEquals("DEAD",view(s).rows.get("life"));
                assertEquals("-1",view(s).rows.get("active"));
            }
            long requests=a.requestCount()+b.requestCount();
            System.out.println("rules seed="+seed+" confirmed="+confirmed+" requests="+requests+" syncPageQueries="+
                    (requests-nonQueries)+" rejected="+rejected+" cached="+cached+" resync="+resync+" revives="+revives);
        }
    }
}
