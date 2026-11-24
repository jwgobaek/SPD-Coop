package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class DisplayReplicaTest {
    @Test public void boundedChunksAndAtomicInstallation() throws Exception {
        UUID session=UUID.randomUUID();
        Map<String,String> rows=new TreeMap<>();
        rows.put("active","0"); rows.put("status","IN_GAME"); rows.put("turn","1");
        for (int i=0;i<600;i++) rows.put("t"+i,"1");
        StateImage snapshot=new StateImage(session,0,1,rows);
        String wire=snapshot.encode(null,null), hash=StateImage.hash(wire);
        int count=(wire.length()+StateImage.CHUNK-1)/StateImage.CHUNK;
        assertTrue(count>2);
        String[] parts=new String[count];
        for (int i=0;i<count;i++) {
            parts[i]="PART 1 "+i+" "+count+" "+hash+" "+wire.substring(i*StateImage.CHUNK,Math.min(wire.length(),(i+1)*StateImage.CHUNK));
        }
        DisplayReplica.Assembly assembly=new DisplayReplica.Assembly();
        assertFalse(assembly.append(parts[0]));
        assertThrows(DisplayReplica.InvalidState.class,assembly::finish);
        assertThrows(DisplayReplica.InvalidState.class,() -> assembly.append(parts[2]));
        assertThrows(DisplayReplica.InvalidState.class,() -> assembly.append(parts[1].replace("PART 1 ","PART 2 ")));
        assertThrows(DisplayReplica.InvalidState.class,() -> new DisplayReplica.Assembly().append("PART 3 0 65 "+hash+" x"));
        for (int i=1;i<count;i++) assertEquals(i==count-1,assembly.append(parts[i]));
        DisplayReplica model=new DisplayReplica(session,0);
        model.apply(assembly.finish());
        StateImage previous=model.image();
        String corrupt=wire.replace("+t200=1;","+t200=9;");
        assertThrows(DisplayReplica.InvalidState.class,() -> model.apply(corrupt));
        assertSame(previous,model.image());
        assertThrows(DisplayReplica.InvalidState.class,() -> model.apply(wire.replace("|0|0|1|","|1|0|1|")));
        char[] tooLarge=new char[StateImage.MAX_BYTES+1];
        assertThrows(DisplayReplica.InvalidState.class,() -> model.apply(new String(tooLarge)));
        assertEquals(snapshot.rows,previous.rows);
        model.close();
        assertNull(model.image());
        assertThrows(DisplayReplica.InvalidState.class,() -> model.apply(wire));
    }
}
