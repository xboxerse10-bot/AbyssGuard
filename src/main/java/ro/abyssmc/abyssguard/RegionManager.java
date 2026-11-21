package ro.abyssmc.abyssguard;

import java.util.*;

/** Engine independent of Fabric; easy to unit test and migrate. */
public final class RegionManager {
    private final Map<String,Region> regions = new TreeMap<>();
    public synchronized void add(Region r) {
        if (!r.id.matches("[a-z0-9_-]{1,48}")) throw new IllegalArgumentException("Invalid region ID");
        if (regions.containsKey(r.id)) throw new IllegalArgumentException("Region already exists");
        regions.put(r.id,r);
    }
    public synchronized Region get(String id) { return regions.get(id.toLowerCase(Locale.ROOT)); }
    public synchronized boolean delete(String id) { return regions.remove(id.toLowerCase(Locale.ROOT)) != null; }
    public synchronized Collection<Region> all() { return List.copyOf(regions.values()); }
    public synchronized List<Region> at(String dim,double x,double y,double z) {
        return regions.values().stream().filter(r->r.contains(dim,x,y,z))
            .sorted(Comparator.comparingInt((Region r)->r.priority).reversed().thenComparing(r->r.id)).toList();
    }
    /** Explicit flag at the highest priority wins. Membership allows ordinary interaction unless denied. */
    public synchronized boolean allowed(String dim,double x,double y,double z,UUID actor,Flag flag,boolean bypass) {
        if (bypass) return true;
        for (Region r:at(dim,x,y,z)) {
            String setting=r.flags.get(flag.name());
            if (setting!=null && !flag.isText()) {
                if (setting.equalsIgnoreCase("deny")) return false;
                if (setting.equalsIgnoreCase("allow")) return true;
            }
            if (isBuildLike(flag) && r.isMember(actor)) return true;
            if (isBuildLike(flag)) return false;
        }
        return true;
    }
    private static boolean isBuildLike(Flag flag) {
        return switch(flag) {
            case BUILD,BLOCK_BREAK,BLOCK_PLACE,INTERACT,USE,CHEST_ACCESS,VEHICLE_PLACE,VEHICLE_DESTROY -> true;
            default -> false;
        };
    }
}
