package com.shatteredpixel.shatteredpixeldungeon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Compare accepted native inputs, floor, seat position and HP at every step across a cold boot. */
public class SewerJourneyRestoreTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();

    private List<String> journey(String mode) throws Exception {
        Path root = directory.getRoot().toPath();
        Path trace = root.resolve(mode + ".tsv");
        Path log = root.resolve(mode + ".log");
        Process host = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-Xmx256m",
                "-Djourney.front=MAGE", "-Djourney." + mode + "=" + root.resolve("start.dat"),
                "-Djourney.trace=" + trace, "-cp", System.getProperty("coop.client.classpath"),
                SewerJourney.class.getName(), "581").redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue("Journey timed out", host.waitFor(60, TimeUnit.SECONDS));
            String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
            assertEquals(output, 0, host.exitValue());
            assertTrue(output, output.contains("result=both alive, exit unlocked"));
            return Files.readAllLines(trace, StandardCharsets.UTF_8);
        } finally {
            host.destroyForcibly();
        }
    }

    @Test public void initialDiskCheckpointRepeatsOriginalGooVictoryInNewJvm() throws Exception {
        List<String> original = journey("checkpoint");
        List<String> restored = journey("resume");
        assertEquals(1121, original.size());
        assertEquals(original, restored);
    }
}
