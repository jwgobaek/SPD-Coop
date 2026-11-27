package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.io.IOException;
import java.util.UUID;

/** One sequencer per joined seat. Keep it when replacing the display during resynchronization. */
public final class SeatActions {
    private final UUID session;
    private final int seat;
    private final long player;
    private long sequence;

    public SeatActions(UUID session,int seat,long player) {
        if (seat<0 || seat>1 || player<=0) throw new IllegalArgumentException("Seat identity");
        this.session=session;
        this.seat=seat;
        this.player=player;
    }

    public SeatActions(UUID session,int seat,long player,long highWater) {
        this(session,seat,player);
        if (highWater<0) throw new IllegalArgumentException("Action ID ceiling");
        sequence=highWater;
    }

    long allocate() {
        if (sequence==Long.MAX_VALUE) throw new IllegalStateException("Action sequence exhausted");
        return ++sequence;
    }

    static boolean offTurn(String action) {
        return "CONSENT".equals(action) || "CANCEL_TRAVEL".equals(action)
                || "START_ASCENT".equals(action) || "END_RUN".equals(action) || "CONTINUE_RUN".equals(action);
    }

    public Pending prepare(StateImage image,String action,int cell,long item) {
        if (!session.equals(image.session) || seat!=image.seat || !"IN_GAME".equals(image.rows.get("status"))
                || (!offTurn(action) && !Integer.toString(seat).equals(image.rows.get("active")))) throw new IllegalStateException("No input offer");
        if (!action.matches("STASIS|BEAMING_RAY|LIFE_LINK|DIVINE_INTERVENTION|JUDGEMENT|FLASH|READ_SCROLL|ENCHANT_TARGET|ENCHANT_CHOOSE|ENCHANT_CANCEL|HALLOWED_GROUND|WALL_OF_LIGHT|MNEMONIC_PRAYER|ALLY_WARP|LAY_ON_HANDS|AURA_OF_PROTECTION|ENCHANT_BOW|HOLY_LANCE|CLEANSE|READ_RECHARGING|TRINITY_MIND|TRINITY_SPIRIT|RECALL_INSCRIPTION|MIND_FORM|SPIRIT_FORM|BODY_FORM|HOLY_INTUITION|BLESS|DETACH_SEAL|AFFIX_SEAL|KEEP_ARMOR_GLYPH|TRANSFER_SEAL_GLYPH|FREERUN|EQUIP_SECOND|SWAP_WEAPONS|MONK_FOCUS|MONK_FLURRY|MONK_DASH|MONK_DRAGONKICK|MONK_MEDITATE|ASSASSINATE|RADIANCE|SMITE|DIVINE_SENSE|SUNRAY|SHIELD_OF_LIGHT|GUIDING_LIGHT|HOLY_WARD|HOLY_WEAPON|WEAPON_ABILITY|IMBUE|CLOAK|USE_CROWN|CLAIM_ARMOR|ARMOR_ABILITY|CLOBBER|SLAM|PARRY|CRUSH|FURY|BERSERK|SNIPER_SHOT|USE_MASK|CLAIM_SUBCLASS|INVEST_TALENT|START_ASCENT|END_RUN|CONTINUE_RUN|VAULT_RECALL|VAULT_INTERACT|VAULT_REWARD|ACCEPT_IMP|SMITH_PICK|SMITH_CASH|SMITH_HARDEN|SMITH_UPGRADE|SMITH_REFORGE|SMITH_ORDER|SMITH_CLAIM|MINE|WAIT|MOVE|ATTACK|PICK_UP|EQUIP|EAT|DROP|UNEQUIP|REVIVE|ZAP|SEARCH|CONSENT|CANCEL_TRAVEL|TRAVEL|ACCEPT_BLACKSMITH|ACCEPT_WANDMAKER|WAND_FIRST|WAND_SECOND|ACCEPT_GHOST|GHOST_WEAPON|GHOST_ARMOR|SHATTER|OPEN|UNLOCK|GREET|DRINK|SHOOT|UPGRADE|IDENTIFY|THROW|BUY|SELL|SELL_ONE|BUYBACK")) throw new IllegalArgumentException("Action");
        long id=allocate();
        String actor=image.rows.get("self").split(",",2)[0];
        String epoch=image.rows.get("epoch");
        String command=(epoch==null ? "DO" : "DO_"+epoch)+" "+player+" "+seat+" "+actor+" "+id+" "+image.version+" "+image.rows.get("turn")+" "+action+" "+cell+" "+item;
        return new Pending(session,id,command);
    }

    /** Submit this same immutable command again if its application-level receipt was lost. */
    public static final class Pending {
        public final long id;
        private final UUID session;
        final String command;
        private Pending(UUID session,long id,String command) { this.session=session; this.id=id; this.command=command; }

        public String submit(LanClient client) throws IOException {
            if (!session.toString().equals(client.session)) throw new IOException("Session mismatch");
            return client.request(command);
        }
    }
}
