package com.shatteredpixel.shatteredpixeldungeon;

import com.watabou.utils.Bundle;

/** Test-only engine image immediately before a native action, replayed through authenticated TCP. */
public final class StagedInput {
    public final Bundle before;
    public final String verb;
    public final int cell, itemOrder;
    StagedInput(CoopSession game,String verb,int cell) { this(game,verb,cell,0); }
    StagedInput(CoopSession game,String verb,int cell,int itemOrder) {
        before=game.checkpoint();
        this.verb=verb;this.cell=cell;this.itemOrder=itemOrder;
    }
}
