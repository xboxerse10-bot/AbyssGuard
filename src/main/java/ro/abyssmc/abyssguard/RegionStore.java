package ro.abyssmc.abyssguard;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;

/** Atomic JSON persistence; never overwrites data on a failed parse. */
public final class RegionStore {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    public RegionStore(Path file) { this.file=file; }
    public void load(RegionManager manager) throws IOException {
        if (!Files.exists(file)) return;
        try {
            Region[] regions=GSON.fromJson(Files.readString(file),Region[].class);
            if (regions!=null) for (Region r:regions) manager.add(r);
        } catch (RuntimeException e) { throw new IOException("Region data invalid: "+file,e); }
    }
    public void save(RegionManager manager) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        Files.writeString(tmp,GSON.toJson(manager.all()),StandardCharsets.UTF_8);
        try { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING); }
    }
}
