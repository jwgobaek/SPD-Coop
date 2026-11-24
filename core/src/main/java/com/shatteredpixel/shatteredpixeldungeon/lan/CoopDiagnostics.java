package com.shatteredpixel.shatteredpixeldungeon.lan;

/** A field allowlist for local display and user-initiated text sharing. */
public final class CoopDiagnostics {
    private static volatile java.io.File activeRoot;
    private static boolean installed;

    /** Persist only the exception type and game call site. Messages and arguments can contain credentials. */
    public static synchronized void watch(java.io.File root) {
        activeRoot=root;
        if(installed) return;
        Thread.UncaughtExceptionHandler next=Thread.getDefaultUncaughtExceptionHandler();
        if(next==null) return;
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try {
                java.io.File active=activeRoot;
                if(active!=null) {
                    try(java.io.DataOutputStream file=new java.io.DataOutputStream(
                            new java.io.FileOutputStream(new java.io.File(active,"last-crash.txt")))) {
                        file.writeUTF(crashLocation(error));
                    }
                }
            } catch(Exception ignored) { /* Preserve platform crash handling. */ }
            finally { next.uncaughtException(thread,error); }
        });
        installed=true;
    }
    public static void stopWatching() { activeRoot=null; }
    static String crashLocation(Throwable error) {
        String where=error.getClass().getSimpleName();
        for(StackTraceElement site:error.getStackTrace()) {
            if(site.getClassName().startsWith("com.shatteredpixel.") || site.getClassName().startsWith("com.watabou.")) {
                where+=" "+site.getClassName()+"."+site.getMethodName()+"("+site.getLineNumber()+")"; break;
            }
        }
        return where.substring(0,Math.min(220,where.length()));
    }
    static String previousCrash(java.io.File root) {
        java.io.File path=new java.io.File(root,"last-crash.txt");
        if(!path.isFile() || path.length()>512) return "-";
        try(java.io.DataInputStream in=new java.io.DataInputStream(new java.io.FileInputStream(path))) {
            String value=in.readUTF();
            return value.matches("[a-zA-Z0-9_.$() -]{1,220}") ? value : "-";
        } catch(java.io.IOException ignored) { return "-"; }
    }
    private CoopDiagnostics() {}
    public static String report(CoopController.Frame f) {
        StateImage image=f.image;
        return "SPD Co-op " + com.watabou.noosa.Game.version + " / " + BuildRevision.ID
                + "\nLAN v1 / SYNC12 / save " + CoopSaves.SCHEMA
                + "\nSession " + (f.session==null ? "—" : f.session) + " / seat " + f.seat
                + "\nConnection " + f.page + " / " + (image==null?"—":image.rows.get("status"))
                + "\nTurn " + (image==null?"—":image.rows.get("turn")) + " / version " + (image==null?"—":image.version)
                + "\nAction " + f.lastAction + " " + f.lastResult + " / pending " + f.pending
                + "\nSave " + ("SAVED".equals(f.notice)?"SAVED":image!=null && image.rows.containsKey("epoch")?"PERSISTENT":"—")
                + "\nTCP bytes " + f.wireBytes + "\nLast error " + (f.issue.isEmpty()?"-":f.issue) + "\nLast crash " + f.crash;
    }
}
