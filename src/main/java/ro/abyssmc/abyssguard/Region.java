package ro.abyssmc.abyssguard;

import java.util.*;

/** Persisted cuboid. Dimension uses Minecraft's full dimension ID. */
public final class Region {
    public String id, dimension;
    public int minX, minY, minZ, maxX, maxY, maxZ, priority;
    public UUID creator;
    public Set<UUID> owners = new HashSet<>(), members = new HashSet<>();
    public Map<String, String> flags = new HashMap<>();
    public String parent;
    public Region(String id, String dimension, int x1, int y1, int z1, int x2, int y2, int z2, UUID creator) {
        this.id = id.toLowerCase(Locale.ROOT); this.dimension = dimension;
        minX=Math.min(x1,x2); minY=Math.min(y1,y2); minZ=Math.min(z1,z2);
        maxX=Math.max(x1,x2); maxY=Math.max(y1,y2); maxZ=Math.max(z1,z2);
        this.creator=creator; owners.add(creator);
    }
    public boolean contains(String dim, double x, double y, double z) {
        return dimension.equals(dim) && x >= minX && x <= maxX+1 && y >= minY && y <= maxY+1 && z >= minZ && z <= maxZ+1;
    }
    public boolean intersects(Region r) {
        return dimension.equals(r.dimension) && minX<=r.maxX && maxX>=r.minX && minY<=r.maxY && maxY>=r.minY && minZ<=r.maxZ && maxZ>=r.minZ;
    }
    public boolean isMember(UUID id) { return owners.contains(id) || members.contains(id); }
}
