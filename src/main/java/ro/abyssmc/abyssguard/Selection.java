package ro.abyssmc.abyssguard;

import java.util.*;

/** Two positions per player, independent of WorldEdit; can be bridged later. */
public final class Selection {
    public record Point(String dimension,int x,int y,int z) {}
    private final Map<UUID,Point[]> selections=new HashMap<>();
    public synchronized void set(UUID player,int slot,Point point) {
        if (slot!=1 && slot!=2) throw new IllegalArgumentException("slot");
        selections.computeIfAbsent(player,k->new Point[2])[slot-1]=point;
    }
    public synchronized Point[] get(UUID player) {
        Point[] p=selections.get(player); return p==null ? new Point[2] : p.clone();
    }
}
