package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Callable;
import static org.junit.Assert.*;

/** Independent process, with no Dungeon or Actor. Only the test host arranges the encounter. */
public class CoopPlayClient {
    private static void until(Callable<Boolean> check) throws Exception {
        long end=System.nanoTime()+8000000000L;
        while(System.nanoTime()<end) {if(check.call()) return;Thread.sleep(5);}
        throw new AssertionError("Client condition timed out");
    }
    public static void main(String[] args) throws Exception {
        InetSocketAddress addr=new InetSocketAddress("127.0.0.1",Integer.parseInt(args[0]));
        CoopController guest=new CoopController(new File(args[1]),null,addr.getAddress(),0,0);
        try(LanClient h=LanClient.synchronizedGame(addr)) {
            String joined=h.request(new BufferedReader(new InputStreamReader(System.in)).readLine());
            assertTrue(joined.startsWith("JOINED 0 "));
            long player=Long.parseLong(joined.split(" ")[2]); UUID id=UUID.fromString(h.session);
            assertTrue(SeatCredential.generate().enroll(h).startsWith("ENROLLED "));
            guest.join("127.0.0.1:"+addr.getPort(),id);
            until(()->!guest.busy() && guest.frame().page==CoopController.Page.LOBBY);
            guest.select("HUNTRESS"); assertEquals("OK",h.request("SELECT WARRIOR"));
            until(()->!guest.busy() && guest.frame().roster.contains("HUNTRESS"));
            guest.ready(true); assertEquals("OK",h.request("READY 1"));
            until(()->h.request("STATE").startsWith("LOBBY READY"));
            assertEquals("GAME_STARTED",h.request("START"));
            SeatActions host=new SeatActions(id,0,player);
            DisplayReplica view=new DisplayReplica(id,0);
            String[] script={"PICK_UP","DROP","PICK_UP","EAT","UNEQUIP","EQUIP","ATTACK","MOVE","REVIVE","WAIT"};
            boolean small=Boolean.parseBoolean(args[2]);
            int foodSprite=-1;
            long previousGuestBytes=0;
            int index=0, waits=0; List<Long> delays=new ArrayList<>();
            while(index<script.length && waits<55) {
                view.refresh(h);
                if("0".equals(view.image().rows.get("active"))) {
                    // Pace the scripted peer like a player, within the actual transport's burst limit.
                    Thread.sleep(80);
                    assertTrue(host.prepare(view.image(),"WAIT",0,0).submit(h).endsWith(" APPLIED")); waits++; continue;
                }
                try {
                    until(()->!guest.busy() && guest.frame().dungeon!=null && guest.frame().dungeon.offered
                            && guest.frame().image.version>=view.image().version);
                } catch(AssertionError timeout) {
                    CoopController.Frame frame=guest.frame();
                    throw new AssertionError("Waiting for "+script[index]+" host version="+view.image().version
                            +" active="+view.image().rows.get("active")+" guest="+frame.page+" issue="+frame.issue
                            +" guest version="+(frame.image==null?"none":frame.image.version)
                            +" guest active="+(frame.image==null?"none":frame.image.rows.get("active")), timeout);
                }
                Thread.sleep(80);
                PublicDungeon game=guest.frame().dungeon;
                assertFalse(game.tap(guest,-1)); assertFalse(game.tap(guest,game.pos+3));
                String verb=script[index]; boolean sent;
                long then=System.nanoTime();
                if(verb.equals("REVIVE") && !"DOWNED".equals(game.source.rows.get("allylife"))) {
                    assertTrue(guest.act(game.source,"WAIT",0,0)); waits++;
                    until(()->!guest.busy() && guest.frame().image.version>game.source.version); continue;
                }
                if(index==0) foodSprite=game.floor.get(game.pos).sprite;
                final int chosenSprite=foodSprite;
                switch(verb) {
                    case "PICK_UP": sent=game.tap(guest,game.pos);break;
                    case "ATTACK": sent=game.tap(guest,game.pos-1);break;
                    case "MOVE": sent=game.tap(guest,game.pos+1);break;
                    case "REVIVE": sent=game.tap(guest,game.ally);break;
                    case "WAIT": sent=guest.act(game.source,"WAIT",0,0);break;
                    default:
                        PublicDungeon.Loot item=game.belongings.stream().filter(i->game.actions(i).contains(verb)
                                && (!(verb.equals("DROP") || verb.equals("EAT")) || (i.kind.equals("FOOD") && i.sprite==chosenSprite))).findFirst().orElseThrow(AssertionError::new);
                        sent=game.use(guest,item,verb);
                }
                assertTrue(verb,sent); assertFalse(guest.act(game.source,"WAIT",0,0));
                until(()->!guest.busy() && guest.frame().lastAction!=0);
                assertEquals(verb,"APPLIED",guest.frame().lastResult); assertTrue(guest.frame().image.version>game.source.version);
                if(verb.equals("MOVE")) assertEquals(game.pos+1,guest.frame().dungeon.pos);
                if(verb.equals("REVIVE")) assertEquals("ALIVE",guest.frame().image.rows.get("allylife"));
                delays.add((System.nanoTime()-then)/1000000); index++;
                if(small && index==3) {
                    int current=guest.frame().dungeon.pos;
                    previousGuestBytes+=guest.frame().wireBytes;
                    guest.leave(false); until(()->guest.frame().page==CoopController.Page.MENU && !guest.busy());
                    until(()->{view.refresh(h);return "RECONNECTING".equals(view.image().rows.get("reason"));});
                    guest.reconnect(id,"127.0.0.1:"+addr.getPort()); view.confirmReconnect(h);
                    until(()->!guest.busy() && guest.frame().dungeon!=null && "IN_GAME".equals(guest.frame().image.rows.get("status")));
                    assertEquals(current,guest.frame().dungeon.pos);
                    assertTrue(guest.frame().dungeon.belongings.stream().anyMatch(i->i.sprite==chosenSprite && i.quantity==1));
                }
            }
            assertEquals(script.length,index); Collections.sort(delays);
            System.out.println("client TCP bytes="+(previousGuestBytes+h.transferredBytes()+guest.frame().wireBytes)
                    +" last full seat snapshot bytes="+guest.frame().image.snapshotBytes()
                    +" client used heap bytes="+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())
                    +" live JVM threads="+java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount());
            System.out.println("confirmed inputs="+index+" supporting waits="+waits+" controller roundtrip ms median="+delays.get(4)+" p95="+delays.get(9));
        } finally {guest.shutdown();}
    }
}

