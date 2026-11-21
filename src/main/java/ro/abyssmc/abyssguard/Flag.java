package ro.abyssmc.abyssguard;

import java.util.*;

public enum Flag {
    BUILD, BLOCK_BREAK, BLOCK_PLACE, INTERACT, USE, CHEST_ACCESS, PVP,
    MOB_DAMAGE, MOB_SPAWNING, CREEPER_EXPLOSION, TNT, OTHER_EXPLOSION,
    FIRE_SPREAD, LAVA_FIRE, LAVA_FLOW, WATER_FLOW, LEAF_DECAY,
    SNOW_FALL, SNOW_MELT, ICE_FORM, ICE_MELT, GRASS_GROWTH,
    ENTRY, EXIT, GREETING, FAREWELL, INVINCIBILITY, FALL_DAMAGE,
    ENDERPEARL, TELEPORT, ITEM_DROP, ITEM_PICKUP, VEHICLE_PLACE,
    VEHICLE_DESTROY, LIGHTER, SLEEP, FEED, DAMAGE_ANIMALS,
    DENY_MESSAGE;
    public static Flag parse(String s) {
        return valueOf(s.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
    public boolean isText() { return this==GREETING || this==FAREWELL || this==DENY_MESSAGE; }
    public static Set<String> names() {
        Set<String> out=new TreeSet<>(); for (Flag f:values()) out.add(f.name().toLowerCase(Locale.ROOT).replace('_','-')); return out;
    }
}
