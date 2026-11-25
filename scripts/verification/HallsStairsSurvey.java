package com.shatteredpixel.shatteredpixeldungeon;

/** Staged stair placement and native boss combat. Records refusals at the first blocked destination. */
public class HallsStairsSurvey extends HeadlessDungeon {
    public static void main(String[] args) throws Exception {
        HallsStairsSurvey probe=new HallsStairsSurvey();
        probe.temporary.create();
        probe.startBackend();
        try {
            for (int seed=570;seed<=581;seed++) try (CoopSession game=CavesInteriorTest.open(seed)) {
                try {
                    CityKingTest.prepareBoundary(game,"claim");
                    while (Dungeon.depth<24) SewerDepthTest.gate(game,true);
                    while (Dungeon.depth>20) SewerDepthTest.gate(game,false);
                    System.out.println("STAIRS\t"+seed+"\t24\treturned to 20");
                } catch (AssertionError | RuntimeException problem) {
                    System.out.println("STAIRS\t"+seed+"\t"+Dungeon.depth+"\t"+problem.getMessage());
                }
            }
        } finally { probe.stopBackend();probe.temporary.delete(); }
    }
}
