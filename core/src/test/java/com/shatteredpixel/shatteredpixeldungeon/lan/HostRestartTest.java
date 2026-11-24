package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class HostRestartTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    private Process start(Class<?> main,Path dir,String mode) throws IOException {
        return new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-Xmx256m","-cp",
                System.getProperty("coop.client.classpath"),main.getName(),dir.toString(),mode)
                .redirectErrorStream(true).redirectOutput(dir.resolve(main.getSimpleName()+".log").toFile()).start();
    }

    @Test(timeout=220000) public void resumeDownedReviveLoseReplyRecoverAndContinueOnDiskOnly() throws Exception {
        run(temp.getRoot().toPath(), new String[]{"new","revive","soak","after","failure","verifyfailure","kill_WRITING","kill_VERIFYING",
                "kill_BEFORE_HEAD","kill_AFTER_HEAD","recover_last","backup"});
    }

    @Test(timeout=100000) public void confirmedMissileSurvivesLostReplyAndHostDeathOnBothSeats() throws Exception {
        for(int seat=0;seat<2;seat++) run(temp.newFolder().toPath(),new String[]{"cast"+seat,"readcast"+seat});
    }

    private void run(Path dir,String[] modes) throws Exception {
        for(String mode:modes) {
            Files.deleteIfExists(dir.resolve("fault-armed"));
            Files.deleteIfExists(dir.resolve("endpoint")); Files.deleteIfExists(dir.resolve("stop"));
            Files.deleteIfExists(dir.resolve("kill-before-reply")); Files.deleteIfExists(dir.resolve("fail-write"));
            if(mode.equals("backup")) {
                CoopSaves saves=new CoopSaves(dir.toFile());
                UUID session=saves.sessions().get(0);
                long current=saves.inspect(session,false).generation;
                Files.write(dir.resolve("coop-saves-v1").resolve(session.toString()).resolve("g"+current),new byte[]{0});
                assertThrows(IOException.class,()->saves.inspect(session,false));
                assertTrue(saves.inspect(session,true).generation<current);
            }
            Process host=start(RestartHost.class,dir,mode), client=null;
            try {
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
                while(!Files.exists(dir.resolve("endpoint")) && host.isAlive() && System.nanoTime()<end) Thread.sleep(5);
                assertTrue(RestartHost.read(dir,"RestartHost.log"),Files.exists(dir.resolve("endpoint")));
                client=start(RestartClient.class,dir,mode);
                assertTrue("client deadline "+mode,client.waitFor(130,TimeUnit.SECONDS));
                String trace=RestartHost.read(dir,"RestartClient.log");
                assertEquals(mode+" "+trace+" "+RestartHost.read(dir,"RestartHost.log"),0,client.exitValue());
                System.out.println(trace.substring(trace.lastIndexOf("mode=")).trim());
                RestartHost.write(dir,"stop","1");
                assertTrue(host.waitFor(10,TimeUnit.SECONDS));
                assertEquals(RestartHost.read(dir,"RestartHost.log"),mode.startsWith("kill_")?80:(mode.equals("revive") || mode.startsWith("cast"))?78:mode.equals("soak")?79:0,host.exitValue());
            } finally {
                if(client!=null) { client.destroyForcibly(); client.waitFor(5,TimeUnit.SECONDS); }
                host.destroyForcibly(); host.waitFor(5,TimeUnit.SECONDS);
            }
        }
    }
}
