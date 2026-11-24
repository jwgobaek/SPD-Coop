package com.shatteredpixel.shatteredpixeldungeon.lan;

import com.watabou.utils.FileUtils;
import java.io.File;

/** Platform bindings are installed at launch. Desktop uses its normal per-user game directory. */
public final class CoopPlatform {
    private static File privateRoot;
    private static SeatTickets.Keys keys;
    public static void configure(File root,SeatTickets.Keys provider) { privateRoot=root; keys=provider; }
    public interface DiagnosticExport { void send(String report); }
    private static DiagnosticExport exporter;
    public static void diagnosticExport(DiagnosticExport export) { exporter=export; }
    public static void export(String report) {
        if(exporter!=null) exporter.send(report);
        else com.badlogic.gdx.Gdx.app.getClipboard().setContents(report);
    }
    public static File root() {
        return privateRoot==null ? FileUtils.getFileHandle("multiplayer").file().getAbsoluteFile() : privateRoot;
    }
    public static CoopController controller() {
        return new CoopController(root(),keys,null,LanClient.TCP_PORT,LanClient.UDP_PORT);
    }
}
