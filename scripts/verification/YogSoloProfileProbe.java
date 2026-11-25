package com.shatteredpixel.shatteredpixeldungeon;

import com.watabou.utils.Bundle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import static org.junit.Assert.*;

/** Creates a real solo save in a disposable profile and compares its bytes after cooperative victory. */
public class YogSoloProfileProbe extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        YogSoloProfileProbe probe=new YogSoloProfileProbe();
        probe.temporary.create();
        probe.startBackend();
        try {
            if (args.length==1) {
                com.watabou.utils.FileUtils.setDefaultFileProperties(com.badlogic.gdx.Files.FileType.Absolute,
                        args[0]+java.io.File.separator);
                Dungeon.init();
                Dungeon.switchLevel(Dungeon.newLevel(),-1);
                Dungeon.saveAll();
                Badges.loadGlobal();
                Badges.saveGlobal(true);
                Rankings.INSTANCE.load();
                Rankings.INSTANCE.save();
                return;
            }
            Path profile=probe.temporary.getRoot().toPath();
            Process solo=new ProcessBuilder(java.nio.file.Paths.get(System.getProperty("java.home"),"bin","java").toString(),
                    "-Xmx256m","-cp",System.getProperty("java.class.path"),YogSoloProfileProbe.class.getName(),profile.toString())
                    .inheritIO().start();
            assertEquals(0,solo.waitFor());
            List<Path> files=new ArrayList<>();
            try (Stream<Path> paths=Files.walk(profile)) {
                paths.filter(Files::isRegularFile).forEach(files::add);
            }
            assertTrue(files.stream().anyMatch(p->p.endsWith(Badges.BADGES_FILE)));
            assertTrue(files.stream().anyMatch(p->p.endsWith(Rankings.RANKINGS_FILE)));
            assertTrue(files.stream().anyMatch(p->p.endsWith("game.dat")));
            List<byte[]> previous=new ArrayList<>();
            for (Path file:files) previous.add(Files.readAllBytes(file));
            Bundle victory;
            try (CoopSession game=CavesInteriorTest.open(578)) {
                YogArenaTest.prepareBoundary(game,"win");
                assertTrue(game.chooseEnding(1-game.amuletOwner(),true));
                assertEquals(CoopSession.Ending.WON,game.ending());
                victory=game.checkpoint();
            }
            try (CoopSession game=CoopSession.restore(victory)) {
                assertEquals(CoopSession.Ending.WON,game.ending());
            }
            for (int i=0;i<files.size();i++) assertArrayEquals(previous.get(i),Files.readAllBytes(files.get(i)));
            System.out.println("PASS native solo files byte-identical after cooperative victory and restore files="+files.size());
        } finally {
            probe.stopBackend();
            probe.temporary.delete();
        }
    }
}
