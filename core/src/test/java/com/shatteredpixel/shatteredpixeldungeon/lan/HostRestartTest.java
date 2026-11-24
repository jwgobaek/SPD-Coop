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

    @Test(timeout=120000) public void generatedSewersRoundTripAcrossProcessDeath() throws Exception {
        run(temp.newFolder().toPath(),new String[]{"walk","ascendfault","backatstairs","verifyfloor","floorbackup"});
    }

    @Test(timeout=80000) public void nativeSpecialRoomsApplyDamageOnlyToExposedSeatOverTcp() throws Exception {
        for(String mode:new String[]{"sentry","piranha","hive"})
            run(temp.newFolder().toPath(),new String[]{mode});
    }

    @Test(timeout=70000) public void questRewardAndLostResponseSurviveFreshHost() throws Exception {
        run(temp.getRoot().toPath(),new String[]{"quest","questreceipt"});
    }

    @Test(timeout=70000) public void chestAmbushReceiptSurvivesNewHostAndAuthenticatedReconnect() throws Exception {
        run(temp.getRoot().toPath(),new String[]{"mimic","mimicreceipt"});
    }

    @Test(timeout=70000) public void goldenChestAmbushSurvivesHostTermination() throws Exception {
        run(temp.getRoot().toPath(),new String[]{"golden","goldenreceipt"});
    }

    @Test(timeout=130000) public void crystalTheftAndKillingShotAreDurableAcrossSevenHostProcesses() throws Exception {
        run(temp.getRoot().toPath(),new String[]{"crystal-new","crystal-open","crystal-park","crystal-return",
                "crystal-fight","crystal-loot","crystal-final"});
    }

    @Test(timeout=45000) public void guestFightsGeneratedStatueAndRecoversConfirmedAttack() throws Exception {
        run(temp.getRoot().toPath(),new String[]{"statue"});
    }

    @Test(timeout=90000) public void guestEngagesNativeExileAndPhantomOnAuthenticatedSockets() throws Exception {
        for(String mode:new String[]{"exile","phantom"}) run(temp.newFolder().toPath(),new String[]{mode});
    }

    @Test(timeout=45000) public void unsupportedNativeEntranceReturnsToLobbyWithoutExposingContents() throws Exception {
        run(temp.getRoot().toPath(),new String[]{"blocked-start"});
    }

    private void run(Path dir,String[] modes) throws Exception {
        for(String mode:modes) {
            Files.deleteIfExists(dir.resolve("fault-armed"));
            Files.deleteIfExists(dir.resolve("endpoint")); Files.deleteIfExists(dir.resolve("stop"));
            Files.deleteIfExists(dir.resolve("kill-before-reply")); Files.deleteIfExists(dir.resolve("fail-write"));
            if(mode.equals("backup") || mode.equals("floorbackup")) {
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
                for (String line : trace.split("\\r?\\n")) {
                    if (line.startsWith("native socket route ") || line.startsWith("mode=")
                            || line.startsWith("public_snapshot ")) System.out.println(line);
                }
                RestartHost.write(dir,"stop","1");
                assertTrue(host.waitFor(10,TimeUnit.SECONDS));
                assertEquals(RestartHost.read(dir,"RestartHost.log"),(mode.startsWith("kill_") || mode.equals("ascendfault"))?80:(mode.equals("revive") || mode.equals("walk") || mode.equals("quest") || (mode.equals("mimic") || mode.equals("golden")) || mode.equals("crystal-open") || mode.equals("crystal-fight") || mode.startsWith("cast"))?78:mode.equals("soak")?79:0,host.exitValue());
                if(mode.equals("verifyfloor")) for(String metric:new String[]{"perf1","perf2","perf-travel"})
                    System.out.println(RestartHost.read(dir,metric));
            } finally {
                if(client!=null) { client.destroyForcibly(); client.waitFor(5,TimeUnit.SECONDS); }
                host.destroyForcibly(); host.waitFor(5,TimeUnit.SECONDS);
            }
        }
    }
}
