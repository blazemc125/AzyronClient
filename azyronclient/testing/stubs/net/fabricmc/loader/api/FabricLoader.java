package net.fabricmc.loader.api;
import java.nio.file.Path;
public final class FabricLoader {
    private FabricLoader() {}
    public static FabricLoader getInstance() { return new FabricLoader(); }
    public Path getConfigDir() { return Path.of("."); }
    public boolean isModLoaded(String modId) { return false; }
    public String getModContainer(String modId) { return null; }
}
