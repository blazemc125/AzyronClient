package com.jiggneshgaming.azyron.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AZYRON CLIENT — configuration holder.
 *
 * <p>Loads/saves {@link AzyronConfigModel} as JSON in the Fabric config
 * directory. All reads go through {@link #get()}; {@link #save()} validates
 * before writing so a corrupt file can never be produced by the game.</p>
 */
public final class AzyronConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/config");
    private static final String FILE_NAME = "azyronclient.json";

    private static AzyronConfigModel model = new AzyronConfigModel();
    private static Path file;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<Runnable>();

    private AzyronConfig() {
    }

    public static AzyronConfigModel get() {
        return model;
    }

    public static void addChangeListener(Runnable listener) {
        if (listener != null) {
            CHANGE_LISTENERS.add(listener);
        }
    }

    private static void notifyChanged() {
        for (Runnable listener : CHANGE_LISTENERS) {
            try {
                listener.run();
            } catch (Throwable t) {
                LOGGER.warn("Change listener failed: {}", t.toString());
            }
        }
    }

    /** Load from disk; on any error a fresh default model is used (never crashes the game). */
    public static void load() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            file = configDir.resolve(FILE_NAME);
            if (Files.isRegularFile(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    AzyronConfigModel loaded = GSON.fromJson(reader, AzyronConfigModel.class);
                    if (loaded != null) {
                        model = loaded;
                    }
                } catch (Exception parseError) {
                    LOGGER.error("azyronclient.json is corrupt ({}); backing up and starting fresh.",
                            parseError.toString());
                    backup(file);
                    model = new AzyronConfigModel();
                }
            } else {
                model = new AzyronConfigModel();
            }
            model.validate();
            save();
            LOGGER.info("AZYRON CLIENT config loaded from {}", file);
        } catch (Throwable t) {
            LOGGER.error("Could not load azyronclient config; defaults will be used.", t);
            model = new AzyronConfigModel();
            model.validate();
        }
    }

    /** Validate + persist to disk atomically (tmp file + move). */
    public static void save() {
        if (file == null) {
            try {
                file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
            } catch (Throwable t) {
                LOGGER.warn("Config path unavailable; settings stay in memory only.");
                notifyChanged();
                return;
            }
        }
        model.validate();
        Path tmp = null;
        try {
            Files.createDirectories(file.getParent());
            tmp = file.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(model, writer);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException atomicMiss) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            notifyChanged();
        } catch (Throwable t) {
            LOGGER.error("Could not save azyronclient.json", t);
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** Replace the live model (used by config profiles) and persist it. */
    public static synchronized void replaceModel(AzyronConfigModel fresh) {
        if (fresh == null) {
            return;
        }
        fresh.validate();
        model = fresh;
        save();
    }

    /** Replace the live model with a fresh default one (Reset-all). */
    public static void resetAll() {
        model = new AzyronConfigModel();
        model.validate();
        save();
    }

    /** Persist a mutation performed on the live model and notify listeners. */
    public static void updateAndSave(Runnable mutation) {
        if (mutation != null) {
            mutation.run();
        }
        save();
    }

    private static void backup(Path src) {
        try {
            Path dst = src.resolveSibling(src.getFileName().toString() + ".corrupt-"
                    + System.currentTimeMillis() + ".bak");
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.warn("Could not back up corrupt config: {}", e.toString());
        }
    }

    /** For tests/diagnostics: resolve the config file path without loading. */
    public static Path resolveConfigFile() {
        try {
            return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        } catch (Throwable t) {
            return null;
        }
    }

    public static List<String> describe() {
        return Collections.singletonList("AZYRON CLIENT 1.0.0 — config: " + FILE_NAME);
    }
}
