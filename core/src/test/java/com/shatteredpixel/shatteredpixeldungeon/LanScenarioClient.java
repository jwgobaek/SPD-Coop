package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.lan.LanClient;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

/** Separate client JVM. No libGDX initialization or host engine object references. */
public class LanScenarioClient {
    private static String token(String[] view,String prefix) {
        for (String field : view) if (field.startsWith(prefix)) return field.substring(prefix.length());
        throw new AssertionError("Missing " + prefix + " in " + Arrays.toString(view));
    }
    private static void frame(DataOutputStream out,String line) throws IOException {
        byte[] bytes = line.getBytes(StandardCharsets.US_ASCII);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
    private static String read(DataInputStream in) throws IOException {
        int n = in.readInt();
        assertTrue(n > 0 && n <= LanClient.MAX_FRAME);
        byte[] bytes = new byte[n];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }
    private static void framing(InetSocketAddress address) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(address,2000);
            socket.setSoTimeout(2000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            byte[] partial = "1 1 - HELLO".getBytes(StandardCharsets.US_ASCII);
            out.writeInt(partial.length);
            out.write(partial,0,2);
            Thread.sleep(80);
            out.write(partial,2,partial.length-2);
            String session = read(in).split(" ")[3];
            frame(out,"2 2 " + session + " PING");
            frame(out,"1 3 previous-session PING");
            frame(out,"1 4 " + session + " PING");
            frame(out,"1 4 " + session + " PING");
            assertEquals("1 2 ERROR VERSION",read(in));
            assertEquals("1 3 ERROR SESSION",read(in));
            assertEquals("1 4 PONG",read(in));
            assertEquals("1 4 ERROR REPLAY",read(in));
            out.writeInt(LanClient.MAX_FRAME + 1);
            assertEquals(-1,in.read());
        }
    }
    public static void main(String[] args) throws Exception {
        InetAddress loop = InetAddress.getByName("127.0.0.1");
        InetSocketAddress address = new InetSocketAddress(loop,Integer.parseInt(args[0]));
        Map<UUID,InetSocketAddress> rooms = LanClient.discover(new InetSocketAddress(loop,Integer.parseInt(args[1])),180);
        assertEquals(1, rooms.size());
        assertEquals(address, rooms.values().iterator().next());
        framing(address);
        String capability = new BufferedReader(new InputStreamReader(System.in)).readLine();
        try (LanClient host = new LanClient(address); Socket stalled = new Socket()) {
            assertEquals("ERROR AUTH",host.request("HOST invalid"));
            String[] h = host.request(capability).split(" ");
            assertEquals("JOINED",h[0]);
            stalled.connect(address,2000);
            stalled.setSoTimeout(15000);
            stalled.getOutputStream().write(0); // unfinished length prefix
            long idleStart = System.nanoTime();
            for (int i=0;i<50;i++) {
                try (LanClient guest = new LanClient(address)) {
                    assertTrue(guest.request("JOIN").startsWith("JOINED 1 "));
                    assertEquals("BYE",guest.request("DISCONNECT"));
                }
                awaitLobby(host);
            }
            while (System.nanoTime()-idleStart < 10500000000L) {
                assertEquals("PONG",host.request("PING"));
                Thread.sleep(100);
            }
            assertEquals(-1,stalled.getInputStream().read());
            try (LanClient guest = new LanClient(address)) {
                String[] g = guest.request("JOIN").split(" ");
                assertEquals("JOINED",g[0]);
                assertEquals("ERROR DUPLICATE",guest.request("JOIN"));
                try (LanClient extra = new LanClient(address)) { assertEquals("ERROR FULL",extra.request("JOIN")); }
                assertEquals("ERROR NOT_READY",host.request("START"));
                assertEquals("ERROR AUTH",guest.request("START"));
                assertEquals("ERROR CLASS",guest.request("SELECT MAGE"));
                assertEquals("OK",host.request("SELECT WARRIOR"));
                assertEquals("OK",guest.request("SELECT HUNTRESS"));
                assertEquals("OK",host.request("READY 1"));
                assertEquals("OK",guest.request("READY 1"));
                assertTrue(host.request("STATE").startsWith("LOBBY READY "));
                assertEquals("OK",guest.request("READY 0"));
                assertEquals("ERROR NOT_READY",host.request("START"));
                assertEquals("OK",guest.request("READY 1"));
                assertEquals("GAME_STARTED",host.request("START"));
                try (LanClient extra = new LanClient(address)) { assertEquals("ERROR LOCKED",extra.request("JOIN")); }
                assertTrue(LanClient.discover(new InetSocketAddress(loop,Integer.parseInt(args[1])),80).isEmpty());
                int[] counts = new int[2];
                int accepted = 0;
                long next = 1;
                String hostHandle = null;
                for (; accepted < 310; accepted++) {
                    Thread.sleep(25); // Leave room for STATE and replay checks inside the normal peer rate budget.
                    String[] view = host.request("STATE").split(" ");
                    int seat = view[0].equals("TURN") ? 0 : 1;
                    LanClient client = seat == 0 ? host : guest;
                    if (seat == 1) view = guest.request("STATE").split(" ");
                    assertEquals("TURN",view[0]);
                    assertEquals(next,Long.parseLong(view[1]));
                    String ids = (seat == 0 ? h[2] : g[2]) + " " + seat + " " + view[2] + " ";
                    String prefix = "ACT " + ids + next + " ";
                    if (counts[seat] == 0) {
                        LanClient other = seat == 0 ? guest : host;
                        assertEquals("ERROR AUTH",other.request(prefix+"WAIT 0 0"));
                        String[] waiting = other.request("STATE").split(" ");
                        assertEquals("WAITING",waiting[0]);
                        assertEquals(5, waiting.length);
                        String otherPlayer = seat == 0 ? g[2] : h[2];
                        assertEquals("ERROR TURN",other.request("ACT "+otherPlayer+" "+(1-seat)+" "+waiting[2]+" "+next+" WAIT 0 0"));
                        assertEquals("ERROR AUTH",client.request("ACT "+(seat == 0 ? h[2] : g[2])+" "+seat+" "+waiting[2]+" "+next+" WAIT 0 0"));
                        assertEquals("ERROR TURN",client.request("ACT "+ids+"0 WAIT 0 0"));
                        assertEquals("ERROR INVALID",client.request(prefix+"MOVE -1 0"));
                        assertEquals("ERROR ACTION",client.request(prefix+"CAST 0 0"));
                    }
                    String command = "WAIT 0 0";
                    if (seat == 0 && counts[0] == 0) {
                        hostHandle = token(view,"I:FOOD:");
                        command = "PICK_UP "+view[3]+" "+token(view,"F:WEAPON:");
                    } else if (seat == 0 && counts[0] == 1) command = "EQUIP 0 "+token(view,"I:WEAPON:");
                    else if (seat == 1 && counts[1] == 0) {
                        assertEquals("ERROR ITEM",client.request(prefix+"EAT 0 "+hostHandle));
                        command = "EAT 0 "+token(view,"I:FOOD:");
                    } else if (seat == 0 && counts[0] == 2) command = "ATTACK "+token(view,"ENEMY:")+" 0";
                    else if (seat == 1 && counts[1] == 1) {
                        boolean moved = false;
                        for (String field : view) if (field.startsWith("FLOOR:")) {
                            command = "MOVE "+field.substring(6)+" 0";
                            String result = client.request(prefix+command);
                            if (result.equals("ERROR INVALID")) continue;
                            assertEquals("APPLIED "+next+" "+(next+1)+" "+field.substring(6),result);
                            moved = true;
                            break;
                        }
                        assertTrue(moved);
                        command = null;
                    }
                    if (command != null) assertEquals("APPLIED "+next+" "+(next+1)+" "+view[3],client.request(prefix+command));
                    assertEquals("ERROR TURN",client.request(prefix+"WAIT 0 0"));
                    counts[seat]++;
                    next++;
                }
                assertTrue(counts[0]>0 && counts[1]>0);
                System.out.println("Applied=" + accepted + " seats=" + Arrays.toString(counts) + " lobby cycles=50");
            }
            long end = System.nanoTime()+2000000000L;
            while (!host.request("STATE").startsWith("LOBBY PAUSED ")) assertTrue(System.nanoTime()<end);
            assertEquals("BYE",host.request("DISCONNECT"));
        }
    }
    private static void awaitLobby(LanClient host) throws IOException {
        long end = System.nanoTime()+2000000000L;
        while (!host.request("STATE").startsWith("LOBBY LOBBY_OPEN ")) assertTrue(System.nanoTime()<end);
    }
}
