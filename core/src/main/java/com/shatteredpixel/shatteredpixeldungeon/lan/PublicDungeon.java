package com.shatteredpixel.shatteredpixeldungeon.lan;

import java.util.*;

/** Typed render and input model derived solely from one verified seat image. */
public final class PublicDungeon {
    public final StateImage source;
    public final int width,height,shield,pos,hp,maxHp,level,experience,ally;
    public final String heroClass,allyClass;
    public final int depth,stair;
    public final boolean consent;
    public final boolean offered;
    public final Map<Integer,String> monsters;
    public final List<Loot> belongings;
    public final Map<Integer,Loot> floor;
    public final List<Integer> buffs;
    public static final class Loot {
        public final long id;
        public final String slot,kind,upgrade,reference,stackType;
        public final int sprite,quantity,charges,capacity,bagIndex,order;
        Loot(String key,String value,Map<String,String> rows) {
            String[] f=value.split(",");
            id=Long.parseLong(key.substring(1)); slot=f[0]; kind=f[1];
            String pocket=rows.get("pocket"+id);
            String[] location=pocket==null ? null : pocket.split(",");
            bagIndex=location==null ? (rows.containsKey("bagtab0") ? -1 : 0) : number(location[0],0,31);
            order=location==null ? 0 : number(location[1],0,2047);
            String ref=rows.get("ref"+id);
            if (ref!=null && (!ref.matches("[0-9a-f]{32}") || slot.startsWith("F"))) throw new IllegalArgumentException("Inventory reference");
            reference=ref==null ? "" : ref;
            String label=rows.get("label"+id);
            String family=rows.get("family"+id);
            stackType=(kind.equals("STONE") || kind.equals("KNIFE")) && family!=null && family.matches(kind.toLowerCase(Locale.ROOT)+"\\.[0-9a-f]{32}") ? family : (kind.equals("FOOD") || kind.equals("HEALING") || kind.equals("UPGRADE") || kind.equals("IDENTIFY")) && label!=null ? label : "";
            sprite=number(f[2],0,1023); quantity=number(f[3],1,Integer.MAX_VALUE); upgrade=f[4];
            charges=kind.equals("STAFF") ? number(f[5],0,10) : 0;
            capacity=kind.equals("STAFF") ? number(f[6],charges,10) : 0;
        }
    }
    public PublicDungeon(StateImage source) {
        this.source=source;
        String[] location=(source.rows.containsKey("floor")?source.rows.get("floor"):"0,1").split(",");
        if(location.length!=2 || !location[0].equals("0")) throw new IllegalArgumentException("Floor");
        depth=number(location[1],1,com.shatteredpixel.shatteredpixeldungeon.CoopFloors.LAST_PLAYABLE);
        String[] gate=(source.rows.containsKey("stair")?source.rows.get("stair"):"-1,0").split(",");
        stair=number(gate[0],-1,65535); consent=number(gate[1],0,1)==1;
        width=number(source.rows.get("width"),3,256);
        // Older hosts did not publish floor height. Reserve the original maximum for those peers.
        height=number(source.rows.containsKey("height") ? source.rows.get("height") : "256",3,256);
        shield=number(source.rows.containsKey("shield") ? source.rows.get("shield") : "0",0,Integer.MAX_VALUE);
        String[] hero=source.rows.get("self").split(",");
        pos=number(hero[1],0,width*height-1); hp=number(hero[2],0,Integer.MAX_VALUE);
        maxHp=number(hero[3],Math.max(1,hp),Integer.MAX_VALUE);
        heroClass=profession(hero[4]); level=number(hero[5],1,999); experience=number(hero[6],0,Integer.MAX_VALUE);
        String[] friend=(source.rows.containsKey("ally") ? source.rows.get("ally") : "").split(",");
        ally=friend.length==3 ? number(friend[1],0,65535) : -1;
        allyClass=ally<0 ? "" : profession(friend[2]);
        if(source.rows.containsKey("selftier")) number(source.rows.get("selftier"),0,6);
        if(source.rows.containsKey("allytier")) number(source.rows.get("allytier"),0,6);
        offered="IN_GAME".equals(source.rows.get("status")) && Integer.toString(source.seat).equals(source.rows.get("active"));
        Map<Integer,String> enemies=new HashMap<>(); Map<Integer,Loot> heaps=new HashMap<>();
        List<Loot> bag=new ArrayList<>(); List<Integer> effects=new ArrayList<>();
        for(Map.Entry<String,String> e:source.rows.entrySet()) {
            String key=e.getKey(), value=e.getValue();
            if(key.length()<2 || !Character.isDigit(key.charAt(1))) continue;
            switch(key.charAt(0)) {
            case 'i':
                Loot item=new Loot(key,value,source.rows);
                if(item.slot.startsWith("F")) heaps.put(number(item.slot.substring(1),0,65535),item);
                else bag.add(item);
                break;
            case 'm':
                String[] mob=value.split(",");
                if(!mob[1].matches("Rat|Albino|Snake|Gnoll|Sentry|Piranha|Ghost|FetidRat|Bee|Mimic|CrystalMimic|GoldenMimic|Sheep|Statue|ArmoredStatue|GnollExile|PhantomPiranha|Crab|HermitCrab|Swarm|Slime|CausticSlime|Thief|GnollTrickster|GreatCrab|RatKing|Goo|Skeleton|DM100|Guard|Shopkeeper")) throw new IllegalArgumentException("Monster");
                enemies.put(number(mob[0],0,65535),mob[1]); break;
            case 'b': effects.add(number(value.split(",")[0],0,255)); break;
            case 't': number(value,0,63); break;
            case 'z': number(value,0,15); break;
            default: break;
            }
        }
        monsters=Collections.unmodifiableMap(enemies); floor=Collections.unmodifiableMap(heaps);
        belongings=Collections.unmodifiableList(bag); buffs=Collections.unmodifiableList(effects);
    }
    private static String profession(String s) {
        if(!s.equals("WARRIOR") && !s.equals("HUNTRESS") && !s.equals("MAGE")) throw new IllegalArgumentException("Class");
        return s;
    }
    private static int number(String s,int low,int high) {
        int n=Integer.parseInt(s);
        if(n<low || n>high) throw new IllegalArgumentException("Public state range");
        return n;
    }
    public static String shortcutAction(Loot item) {
        switch (item.kind) {
            case "FOOD": return "EAT";
            case "HEALING": return "DRINK";
            case "KNIFE": case "STONE": return "THROW";
            case "UPGRADE": case "IDENTIFY": return item.kind;
            case "BOW": return "SHOOT";
            case "STAFF": return item.slot.equals("WEAPON") ? "ZAP" : "EQUIP";
            case "MELEE": case "ARMOR": return item.slot.equals("BAG") ? "EQUIP" : "UNEQUIP";
            case "HONEYPOT": return "SHATTER";
            default: return "";
        }
    }
    public int scrollTarget(Loot item,String verb) {
        String data=source.rows.get("select"+item.id);
        if (data==null || !belongings.contains(item)) return 0;
        String[] fields=data.split(",");
        return fields.length==3 && "1".equals(fields[verb.equals("UPGRADE")?1:2]) ? number(fields[0],1,2048) : 0;
    }

    public List<String> actions(Loot item) {
        List<String> verbs=new ArrayList<>();
        if(!offered || !belongings.contains(item)) return verbs;
        String quote=source.rows.get("sell"+item.id);
        if (quote!=null) {
            verbs.add("SELL");
            if (quote.endsWith(",1")) verbs.add("SELL_ONE");
        }
        boolean weapon=item.kind.equals("MELEE") || item.kind.equals("STAFF");
        if(item.slot.equals("WEAPON") && item.charges>0) verbs.add("ZAP");
        if(item.slot.equals("BAG")) {
            if(item.kind.equals("UPGRADE") || item.kind.equals("IDENTIFY")) verbs.add(item.kind);
            if(item.kind.equals("HEALING")) verbs.add("DRINK");
            if(item.kind.equals("STONE") || item.kind.equals("KNIFE")) verbs.add("THROW");
            if(item.kind.equals("BOW")) verbs.add("SHOOT");
            if(item.kind.equals("FOOD")) verbs.add("EAT");
            if(weapon || item.kind.equals("ARMOR")) verbs.add("EQUIP");
            if(item.kind.equals("HONEYPOT")) verbs.add("SHATTER");
            if(item.kind.equals("FOOD") || weapon || item.kind.equals("ARMOR") || item.kind.equals("INSCRIBED") || item.kind.equals("HONEYPOT") || item.kind.equals("SHELL")) verbs.add("DROP");
        } else if((item.slot.equals("WEAPON") && weapon) || (item.slot.equals("ARMOR") && item.kind.equals("ARMOR"))) verbs.add("UNEQUIP");
        return verbs;
    }
    public boolean use(CoopController c,Loot item,String verb) {
        return !verb.equals("THROW") && !verb.equals("ZAP") && !verb.equals("SHOOT") && actions(item).contains(verb) && c.act(source,verb,verb.equals("DROP") || verb.equals("SHATTER")?pos:0,item.id);
    }
    public int terrain(int cell) {
        String t=source.rows.get("t"+cell);
        return t==null ? source.rememberedTerrain(cell) : Integer.parseInt(t);
    }
    public boolean visible(int cell) { return source.rows.containsKey("t"+cell); }
    /** Original terrain-features atlas column, disclosed only during direct observation. */
    public int plant(int cell) {
        String value = source.rows.get("plant"+cell);
        return value == null || !visible(cell) ? -1 : number(value,0,15);
    }
    public boolean chest(int cell) {
        String kind=source.rows.get("h"+cell);
        return "CHEST".equals(kind) || "CRYSTAL_CHEST".equals(kind) || "LOCKED_CHEST".equals(kind);
    }
    public boolean tap(CoopController c,int cell) {
        if(!offered || cell<0 || !visible(cell) || Math.abs(cell%width-pos%width)>1 || Math.abs(cell/width-pos/width)>1) return false;
        if(cell==ally) return "DOWNED".equals(source.rows.get("allylife")) && c.act(source,"REVIVE",cell,0);
        if("Shopkeeper".equals(monsters.get(cell)) || "Ghost".equals(monsters.get(cell)) || "Sentry".equals(monsters.get(cell))) return false;
        if("Sheep".equals(monsters.get(cell))) return c.act(source,"GREET",cell,0);
        if(monsters.containsKey(cell)) return c.act(source,"ATTACK",cell,0);
        if(terrain(cell)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_DOOR || terrain(cell)==com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_EXIT) return c.act(source,"UNLOCK",cell,0);
        if(chest(cell)) return c.act(source,"OPEN",cell,0);
        if(cell==pos) return floor.containsKey(cell) && c.act(source,"PICK_UP",cell,floor.get(cell).id);
        return c.act(source,"MOVE",cell,0);
    }
}
