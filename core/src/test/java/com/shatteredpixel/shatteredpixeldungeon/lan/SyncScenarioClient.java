package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.net.*;
import java.util.*;
import static org.junit.Assert.*;

/** Independent display-only process. Faults here occur after TCP frame delivery. */
public final class SyncScenarioClient {
    private final LanClient[] clients=new LanClient[2];
    private final DisplayReplica[] models=new DisplayReplica[2];
    private final long[] players=new long[2];
    private final SeatActions[] streams=new SeatActions[2];
    private int confirmed, duplicates, rejected, resync, lost, terminal;
    private final Set<String> actions=new HashSet<>();

    public static void main(String[] args) throws Exception {
        new SyncScenarioClient().run(Integer.parseInt(args[0]),Integer.parseInt(args[1]),Boolean.parseBoolean(args[2]));
    }

    private String command(int seat,long id,long base,long turn,String args) {
        String actor=models[seat].image().rows.get("self").split(",")[0];
        return "DO "+players[seat]+" "+seat+" "+actor+" "+id+" "+base+" "+turn+" "+args;
    }
    private void reject(int seat,String cmd,String suffix) throws IOException {
        String reply=clients[seat].request(cmd);
        assertTrue(reply,reply.endsWith(suffix));
        rejected++;
    }

    private static String item(StateImage image,String location,String kind) {
        for (Map.Entry<String,String> e : image.rows.entrySet())
            if (e.getKey().startsWith("i") && e.getValue().startsWith(location+","+kind+",")) return e.getKey().substring(1);
        return null;
    }

    private void run(int port,int target,boolean faults) throws Exception {
        InetSocketAddress address=new InetSocketAddress("127.0.0.1",port);
        String hostCommand=new BufferedReader(new InputStreamReader(System.in)).readLine();
        try (LanClient h=LanClient.synchronizedGame(address); LanClient g=LanClient.synchronizedGame(address)) {
            clients[0]=h; clients[1]=g;
            for (int s=0;s<2;s++) {
                LanClient c=clients[s];
                String[] joined=c.request(s==0 ? hostCommand : "JOIN").split(" ");
                assertEquals("JOINED",joined[0]);
                players[s]=Long.parseLong(joined[2]);
                streams[s]=new SeatActions(UUID.fromString(c.session),s,players[s]);
                models[s]=new DisplayReplica(UUID.fromString(c.session),s);
                assertEquals("OK",c.request(s==0 ? "SELECT WARRIOR" : "SELECT HUNTRESS"));
                assertEquals("OK",c.request("READY 1"));
            }
            assertEquals("GAME_STARTED",h.request("START"));
            models[0].refresh(h);
            models[1].refresh(g);
            assertEquals(1,models[0].image().version);
            assertNotEquals(models[0].image().checksum,models[1].image().checksum);
            if (!faults) {
                for (;confirmed<target;confirmed++) {
                    Thread.sleep(22);
                    for (int s=0;s<2;s++) models[s].refresh(clients[s]);
                    int active=Integer.parseInt(models[0].image().rows.get("active"));
                    StateImage base=models[active].image();
                    assertEquals(confirmed+1,base.version);
                    String receipt=streams[active].prepare(base,"WAIT",0,0).submit(clients[active]);
                    assertTrue(receipt,receipt.endsWith(" APPLIED"));
                }
                models[0].refresh(h);
                models[1].refresh(g);
                assertEquals(target+1,models[1].image().version);
                System.out.println("normal confirmed="+confirmed+" requests="+(h.requestCount()+g.requestCount())+
                        " rejected=0 cached=0 resync=0 lost=0 terminal=0");
                return;
            }
            DisplayReplica lagging=new DisplayReplica(UUID.fromString(g.session),1);
            lagging.refresh(g);
            String first=null, retired=null, firstDelta=null;
            StateImage secondBase=null;
            boolean merged=false, moved=false, hit=false;
            while (confirmed<target+8) {
                Thread.sleep(28); // Keep polling, fault checks and actions inside the production peer limit.
                for (int s=0;s<2;s++) models[s].refresh(clients[s]);
                StateImage host=models[0].image();
                int seat=Integer.parseInt(host.rows.get("active"));
                StateImage image=models[seat].image();
                assertEquals(confirmed+1,image.version);
                String[] self=image.rows.get("self").split(",");
                int pos=Integer.parseInt(self[1]);
                long turn=Long.parseLong(image.rows.get("turn"));
                LanClient client=clients[seat];
                String args="WAIT 0 0";
                String groundFood=item(image,"F"+pos,"FOOD"), groundWeapon=item(image,"F"+pos,"MELEE");
                String bagWeapon=item(image,"BAG","MELEE"), bagFood=item(image,"BAG","FOOD");
                if (seat==0 && groundWeapon!=null) args="PICK_UP "+pos+" "+groundWeapon;
                else if (seat==1 && groundFood!=null) {
                    args="PICK_UP "+pos+" "+groundFood;
                    retired=groundFood;
                } else if (seat==1 && !merged && retired!=null) {
                    assertNotNull(bagFood);
                    assertEquals("2",image.rows.get("i"+bagFood).split(",")[3]);
                    assertFalse(image.rows.containsKey("i"+retired));
                    reject(seat,command(seat,streams[seat].allocate(),image.version,turn,"EAT 0 "+retired),"REJECT_ITEM");
                    args="EAT 0 "+bagFood;
                    merged=true;
                } else if (seat==0 && bagWeapon!=null && !actions.contains("EQUIP")) args="EQUIP 0 "+bagWeapon;
                else if (seat==0 && !hit && actions.contains("EQUIP")) {
                    for (Map.Entry<String,String> e : image.rows.entrySet()) {
                        if (!e.getKey().startsWith("m")) continue;
                        String[] mob=e.getValue().split(",");
                        if (mob[2].equals("1") && mob[1].equals("Rat")) { args="ATTACK "+mob[0]+" 0"; break; }
                    }
                } else if (seat==1 && merged && !moved) {
                    int w=Integer.parseInt(image.rows.get("width"));
                    for (int delta : new int[]{1,-1,w,-w,w+1,w-1,-w+1,-w-1}) {
                        if ("1".equals(image.rows.get("t"+(pos+delta)))) {
                            // Choose the actual request below. No engine-side fixture supplies the destination.
                            args="MOVE "+(pos+delta)+" 0";
                            if (image.rows.getOrDefault("ally","").contains(","+(pos+delta)+",")) continue;
                            break;
                        }
                    }
                }
                if (confirmed==4) {
                    reject(seat,"ACT 0 0 0 0 WAIT 0 0","ERROR MODE");
                    reject(seat,command(seat,streams[seat].allocate(),image.version-1,turn,"WAIT 0 0"),"REJECT_VERSION");
                    reject(seat,command(seat,streams[seat].allocate(),image.version,turn-1,"WAIT 0 0"),"REJECT_TURN");
                    String privateFood=item(models[1-seat].image(),"BAG","FOOD");
                    assertNotNull(privateFood);
                    reject(seat,command(seat,streams[seat].allocate(),image.version,turn,"EAT 0 "+privateFood),"REJECT_ITEM");
                }
                if (confirmed==1) {
                    StateImage base=lagging.image();
                    firstDelta=DisplayReplica.readTransfer(g::request,g.request("SYNC "+base.version+" "+base.checksum));
                    secondBase=models[1].image();
                    assertTrue(firstDelta.startsWith("D|"));
                }
                if (confirmed==2) {
                    String later=DisplayReplica.readTransfer(g::request,g.request("SYNC "+secondBase.version+" "+secondBase.checksum));
                    StateImage untouched=lagging.image();
                    assertThrows(DisplayReplica.InvalidState.class,() -> lagging.apply(later));
                    assertSame(untouched,lagging.image());
                    lagging.apply(firstDelta);
                    StateImage once=lagging.image();
                    lagging.apply(firstDelta);
                    assertSame(once,lagging.image());
                    lagging.apply(later);
                    final String delayed=firstDelta;
                    assertThrows(DisplayReplica.InvalidState.class,() -> lagging.apply(delayed));
                    assertEquals(models[1].image().rows,lagging.image().rows);
                }
                if (confirmed==3) {
                    // Corrupt one real snapshot part at the application boundary. The next transfer is unmodified.
                    DisplayReplica recovering=new DisplayReplica(UUID.fromString(g.session),1);
                    boolean[] damage={true};
                    recovering.refresh(command -> {
                        String frame=g.request(command);
                        if (damage[0]) { damage[0]=false; return frame.substring(0,frame.length()-1)+"?"; }
                        return frame;
                    });
                    assertEquals(1,recovering.resyncCount());
                    assertEquals(models[1].image().rows,recovering.image().rows);
                    resync++;
                }
                if (confirmed==6) {
                    lagging.refresh(g);
                    assertEquals(1,lagging.resyncCount());
                    assertEquals(models[1].image().rows,lagging.image().rows);
                    resync++;
                }
                String[] parts=args.split(" ");
                SeatActions.Pending pending=streams[seat].prepare(image,parts[0],Integer.parseInt(parts[1]),Long.parseLong(parts[2]));
                String cmd=pending.command;
                String reply;
                if (confirmed==0) {
                    pending.submit(client); // Fault injector consumes the complete reply before handing control to the model.
                    lost++;
                    Thread.sleep(60);
                    reply=pending.submit(client);
                    duplicates++;
                } else reply=pending.submit(client);
                if (confirmed==0) first=cmd;
                if (reply.endsWith("HALTED_UNCERTAIN")) {
                    assertTrue(confirmed>=target);
                    assertEquals(reply,client.request(cmd));
                    duplicates++;
                    terminal++;
                    models[seat].refresh(client);
                    assertEquals("PAUSED",models[seat].image().rows.get("status"));
                    break;
                }
                assertTrue(reply,reply.endsWith(" APPLIED"));
                if (confirmed<18 || confirmed%50==0) {
                    String cached=client.request(cmd);
                    assertEquals("RESULT "+pending.id+" "+seat+" "+self[0]+" "+turn+" "+image.version+" "+(image.version+1)+" APPLIED",cached);
                    assertEquals(reply,cached);
                    duplicates++;
                    reject(1-seat,cmd,"ERROR AUTH");
                    reject(seat,cmd.substring(0,cmd.lastIndexOf(' ')+1)+"9999999","ERROR CONFLICT");
                }
                String verb=args.split(" ")[0];
                actions.add(verb);
                if (verb.equals("MOVE")) {
                    moved=true;
                    models[seat].refresh(client);
                    assertEquals(args.split(" ")[1],models[seat].image().rows.get("self").split(",")[1]);
                }
                if (verb.equals("ATTACK")) {
                    models[seat].refresh(client);
                    hit=!models[seat].witnessedDeaths().isEmpty();
                }
                confirmed++;
                if (confirmed==2) {
                    assertEquals("ERROR SESSION",h.exchange(UUID.randomUUID().toString(),first));
                    rejected++;
                }
                if (confirmed==200) reject(0,first,"ERROR EXPIRED");
            }
            assertTrue(hit && merged && moved);
            assertEquals(1,terminal);
            assertEquals(6,actions.size());
            System.out.println("confirmed="+confirmed+" requests="+(h.requestCount()+g.requestCount())+
                    " rejected="+rejected+" cached="+duplicates+" resync="+resync+" lost="+lost+" terminal="+terminal);
        }
    }
}
