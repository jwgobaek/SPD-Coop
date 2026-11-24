package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.watabou.utils.Bundle;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class GenerationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static Bundle value(int n) {
        Bundle b = new Bundle(); b.put("action",n); return b;
    }

    /** Own JVM and file lock. Terminate without executing Java finally/close. */
    public static void main(String[] args) throws Exception {
        CoopSaves store = new CoopSaves(new File(args[0]), stage -> {
            if (args[2].equals(stage)) Runtime.getRuntime().halt(77);
        });
        try (CoopSaves.Slot slot = store.open(UUID.fromString(args[1]))) { slot.publish(value(2),false); }
    }

    @Test public void interruptedWritersHaveExactlyOnePublishedHead() throws Exception {
        File base = temporary.getRoot();
        File single = new File(base,"game1.dat");
        Files.write(single.toPath(),new byte[]{7,12});
        CoopSaves store = new CoopSaves(base);
        for (String stage : new String[]{"WRITING","VERIFYING","BEFORE_HEAD","AFTER_HEAD"}) {
            UUID id = UUID.randomUUID();
            try (CoopSaves.Slot slot = store.open(id)) { slot.publish(value(1),false); }
            Process writer = new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",
                    System.getProperty("coop.client.classpath"),getClass().getName(),base.toString(),id.toString(),stage).start();
            try { assertTrue(writer.waitFor(12,TimeUnit.SECONDS)); assertEquals(77,writer.exitValue()); }
            finally { writer.destroyForcibly(); }
            try (CoopSaves.Slot slot = store.open(id)) {
                assertEquals(stage.equals("AFTER_HEAD") ? 2 : 1,slot.read(false).getInt("action"));
                slot.publish(value(3),false);
                assertEquals(3,slot.read(false).getInt("action"));
            }
        }
        assertArrayEquals(new byte[]{7,12},Files.readAllBytes(single.toPath()));
    }

    @Test public void interruptedOrMixedGenerationRequiresExplicitRecovery() throws Exception {
        File base = temporary.getRoot();
        UUID id = UUID.randomUUID();
        CoopSaves store = new CoopSaves(base);
        try (CoopSaves.Slot slot = store.open(id)) {
            for (int i=1;i<=20;i++) {
                slot.publish(value(i),false);
                assertEquals(i,slot.read(false).getInt("action"));
            }
            assertTrue(store.sessions().contains(id));
            assertThrows(IOException.class,()->store.open(id));
            File dir = new File(new File(base,"coop-saves-v1"),id.toString());
            File head = new File(dir,"g20"), backup = new File(dir,"g19");
            byte[] good = Files.readAllBytes(head.toPath());
            Files.write(head.toPath(),Files.readAllBytes(backup.toPath()));
            assertThrows(IOException.class,()->slot.read(false));
            Files.write(head.toPath(),new byte[]{1});
            assertThrows(IOException.class,()->slot.read(false));
            Files.delete(head.toPath());
            assertThrows(IOException.class,()->slot.read(false));
            good[good.length-2] ^= 1;
            Files.write(head.toPath(),good);
            assertThrows(IOException.class,()->slot.read(false));
            slot.publish(slot.read(true),true);
            assertEquals(21,slot.currentGeneration()); assertEquals(19,slot.backupGeneration());
            assertEquals(19,slot.read(false).getInt("action"));
        }
    }
}
