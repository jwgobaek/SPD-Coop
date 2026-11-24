package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.shatteredpixel.shatteredpixeldungeon.CoopSession;
import com.watabou.utils.Bundle;
import java.util.UUID;
import static org.junit.Assert.*;

public final class LedgerChecks {
    public static void verify(CoopSession game) {
        UUID id=UUID.randomUUID(), epoch=UUID.randomUUID();
        RoomSync source=new RoomSync(id,game);
        int actor=game.hero(0).id();
        String cmd="1 1 "+id+" DO 1 0 "+actor+" 7 1 "+game.turn()+" WAIT 0 0";
        String result=source.action(0,cmd.split(" "));
        assertTrue(result,result.endsWith(" APPLIED"));
        Bundle b=source.checkpoint(), seat=b.getBundle("seat0"), receipt=seat.getBundle("r0");
        seat.put("highest",8L);
        assertThrows(IllegalArgumentException.class,()->new RoomSync(id,game).restore(b,epoch));
        seat.put("highest",7L);
        receipt.put("result",result.replace("RESULT 7 0 ","RESULT 7 1 "));
        assertThrows(IllegalArgumentException.class,()->new RoomSync(id,game).restore(b,epoch));
        receipt.put("result",result.replace(" 2 APPLIED"," 1 APPLIED"));
        assertThrows(IllegalArgumentException.class,()->new RoomSync(id,game).restore(b,epoch));
    }
}
