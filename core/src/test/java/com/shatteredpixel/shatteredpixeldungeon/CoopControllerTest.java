package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.lan.*;
import java.io.File;
import java.net.*;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoopControllerTest extends HeadlessDungeon {
    private static CoopController.Frame until(CoopController c,Predicate<CoopController.Frame> match) throws Exception {
        long end=System.nanoTime()+8000000000L;
        while(System.nanoTime()<end) {
            CoopController.Frame f=c.frame();
            if(!c.busy() && match.test(f)) return f;
            if(f.page==CoopController.Page.DISCONNECTED) fail("Disconnected "+f.notice);
            Thread.sleep(5);
        }
        throw new AssertionError("Timeout "+c.frame().page+" "+c.frame().notice+" "+c.frame().roster);
    }
    private static void stop(CoopController c) throws Exception {
        c.shutdown();
        for(int n=0;n<1500 && c.frame().page!=CoopController.Page.CLOSED;n++) Thread.sleep(5);
        assertEquals(CoopController.Page.CLOSED,c.frame().page);
    }
    @Test(timeout=60000) public void realLobbySaveAndColdClientResume() throws Exception {
        File hr=temporary.newFolder(), gr=temporary.newFolder();
        InetAddress loop=InetAddress.getLoopbackAddress();
        CoopController h=new CoopController(hr,null,loop,0,0), g=new CoopController(gr,null,loop,0,0);
        try {
            h.host();
            CoopController.Frame lobby=until(h,f->f.page==CoopController.Page.LOBBY);
            String addr="127.0.0.1:"+lobby.address.split(":")[1];
            UUID id=lobby.session;
            g.join(addr,id); until(g,f->f.page==CoopController.Page.LOBBY);
            h.select("WARRIOR");g.select("HUNTRESS");
            until(h,f->f.roster.contains("HUNTRESS")&&f.roster.contains("WARRIOR"));
            until(g,f->f.roster.contains("HUNTRESS"));
            h.ready(true);g.ready(true); until(h,f->f.roster.startsWith("LOBBY READY"));
            h.start(); CoopController.Frame first=until(h,f->f.image!=null);
            until(g,f->f.image!=null);
            assertTrue(h.act(first.image,"WAIT",0,0)); assertFalse(h.act(first.image,"WAIT",0,0));
            CoopController.Frame after=until(h,f->f.image!=null && f.image.version>first.image.version);
            assertEquals("APPLIED",after.lastResult); String turn=after.image.rows.get("turn");
            h.leave(true); until(h,f->f.page==CoopController.Page.MENU && f.notice.equals("SAVED"));
            stop(g); g=new CoopController(gr,null,loop,0,0);
            h.saved(false); CoopController.Frame saves=until(h,f->f.page==CoopController.Page.SAVES);
            assertEquals(1,saves.list.size()); assertNotNull(saves.list.get(0).backup);
            assertEquals("WARRIOR",saves.list.get(0).current.hostClass);
            h.resume(id,false);
            CoopController.Frame restored=until(h,f->f.image!=null);
            String resumedAddr="127.0.0.1:"+restored.address.split(":")[1];
            g.saved(true); assertEquals(id,until(g,f->f.page==CoopController.Page.TICKETS).list.get(0).id);
            g.reconnect(id,resumedAddr);
            CoopController.Frame guest=until(g,f->f.image!=null && "IN_GAME".equals(f.image.rows.get("status")));
            assertEquals(turn,guest.image.rows.get("turn"));
            assertEquals(1,guest.seat); assertEquals("HUNTRESS",guest.image.rows.get("self").split(",")[4]);
        } finally {stop(g); stop(h);}
    }
}
