package com.jiggneshgaming.azyron.profiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.core.profile.PerformanceProfile;
import com.jiggneshgaming.azyron.menu.BaseAzyronScreen;
import com.jiggneshgaming.azyron.performance.ProfileManager;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * AZYRON CLIENT V2 — config profiles.
 *
 * <p>Named snapshots of the entire AZYRON configuration, stored as JSON in
 * the Fabric config directory ({@code azyron-profiles/} for profiles,
 * {@code azyron-exports/} for export files). Save / load / rename / delete
 * are real file operations; import copies a file from the export folder.
 * Built-in baselines (Potato / Balanced / Smooth) apply the matching
 * performance bundle plus sensible UI defaults.</p>
 */
public final class AzyronProfilesScreen extends BaseAzyronScreen {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("azyronclient/profiles-screen");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final com.jiggneshgaming.azyron.menu.SearchBox nameBox =
            new com.jiggneshgaming.azyron.menu.SearchBox(0, 0, 0, 0);
    private List<String> profiles = new ArrayList<String>();
    private List<String> exports = new ArrayList<String>();
    private String selected;
    private String feedback = "";
    private long feedbackAt;
    private long deleteArmedAt;

    public AzyronProfilesScreen(Screen backTarget) {
        super("Config Profiles", backTarget);
        refreshLists();
    }

    /* ================= file operations (all real) ================= */

    private static Path profilesDir() throws java.io.IOException {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("azyron-profiles");
        Files.createDirectories(dir);
        return dir;
    }

    private static Path exportsDir() throws java.io.IOException {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("azyron-exports");
        Files.createDirectories(dir);
        return dir;
    }

    private void refreshLists() {
        profiles = jsonNames(safeProfilesDir());
        exports = jsonNames(safeExportsDir());
        if (selected == null && !profiles.isEmpty()) {
            selected = profiles.get(0);
        }
        if (selected != null && !profiles.contains(selected)) {
            selected = null;
        }
    }

    private static Path safeProfilesDir() {
        try {
            return profilesDir();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Path safeExportsDir() {
        try {
            return exportsDir();
        } catch (Throwable t) {
            return null;
        }
    }

    private static List<String> jsonNames(Path dir) {
        List<String> out = new ArrayList<String>();
        if (dir == null) {
            return out;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json"))
                    .map(p -> p.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .forEach(out::add);
        } catch (Throwable t) {
            LOGGER.warn("Could not list profiles in {}: {}", dir, t.toString());
        }
        return out;
    }

    private void saveCurrentAs(String name) {
        String clean = sanitize(name);
        if (clean == null) {
            note("Profile name must be 1-32 chars: a-z 0-9 - _");
            return;
        }
        try {
            AzyronConfig.save();
            Path file = profilesDir().resolve(clean + ".json");
            Path source = FabricLoader.getInstance().getConfigDir().resolve("azyronclient.json");
            Files.copy(source, file, StandardCopyOption.REPLACE_EXISTING);
            selected = clean + ".json";
            refreshLists();
            note("Saved profile \"" + clean + "\"");
        } catch (Throwable t) {
            note("Save failed: " + t.getClass().getSimpleName());
        }
    }

    private void loadProfile(String fileName) {
        try {
            Path file = profilesDir().resolve(fileName);
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                AzyronConfigModel loaded = GSON.fromJson(reader, AzyronConfigModel.class);
                if (loaded == null) {
                    note("Profile is empty or corrupt.");
                    return;
                }
                AzyronConfig.replaceModel(loaded);
                note("Profile \"" + fileName.replace(".json", "") + "\" loaded");
            }
        } catch (Exception t) {
            note("Could not load (invalid JSON?): " + t.getClass().getSimpleName());
        }
    }

    private void renameProfile(String fileName, String newName) {
        String clean = sanitize(newName);
        if (clean == null) {
            note("New name must be 1-32 chars: a-z 0-9 - _");
            return;
        }
        try {
            Path from = profilesDir().resolve(fileName);
            Path to = profilesDir().resolve(clean + ".json");
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            selected = to.getFileName().toString();
            refreshLists();
            note("Renamed to \"" + clean + "\"");
        } catch (Throwable t) {
            note("Rename failed: " + t.getClass().getSimpleName());
        }
    }

    private void deleteProfile(String fileName) {
        try {
            Path file = profilesDir().resolve(fileName);
            Files.deleteIfExists(file);
            if (fileName.equals(selected)) {
                selected = null;
            }
            refreshLists();
            note("Deleted \"" + fileName.replace(".json", "") + "\"");
        } catch (Throwable t) {
            note("Delete failed: " + t.getClass().getSimpleName());
        }
    }

    private void exportCurrent() {
        try {
            AzyronConfig.save();
            String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.ROOT)
                    .format(new java.util.Date());
            Path out = exportsDir().resolve("azyron-export-" + stamp + ".json");
            Path source = FabricLoader.getInstance().getConfigDir().resolve("azyronclient.json");
            Files.copy(source, out, StandardCopyOption.REPLACE_EXISTING);
            refreshLists();
            note("Exported to " + out);
        } catch (Throwable t) {
            note("Export failed: " + t.getClass().getSimpleName());
        }
    }

    private void importExport(String fileName) {
        try {
            Path from = exportsDir().resolve(fileName);
            Path toDir = profilesDir();
            Path to = toDir.resolve(fileName);
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            refreshLists();
            loadProfile(to.getFileName().toString());
            note("Imported and loaded \"" + fileName.replace(".json", "") + "\"");
        } catch (Throwable t) {
            note("Import failed: " + t.getClass().getSimpleName());
        }
    }

    private void applyBaseline(String id) {
        try {
            AzyronConfigModel model = AzyronConfig.get();
            PerformanceProfile.Id perf = PerformanceProfile.Id.fromString(id, PerformanceProfile.Id.BALANCED);
            if ("POTATO".equals(id)) {
                model.animations = false;
                model.animationQuality = "LOW";
                model.lowParticleMode = true;
                model.aura = "NONE";
            } else if ("SMOOTH".equals(id)) {
                model.animations = true;
                model.animationQuality = "HIGH";
                model.lowParticleMode = false;
            } else {
                model.animations = true;
                model.animationQuality = "MEDIUM";
                model.lowParticleMode = false;
            }
            model.profile = perf.name();
            AzyronConfig.replaceModel(model);
            ProfileManager.apply(perf); // applies the gameplay options bundle + persists
            note(id.charAt(0) + id.substring(1).toLowerCase(java.util.Locale.ROOT) + " baseline applied");
        } catch (Throwable t) {
            note("Baseline failed: " + t.getClass().getSimpleName());
        }
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String clean = raw.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");
        if (clean.isEmpty() || clean.length() > 32) {
            return null;
        }
        return clean;
    }

    private void note(String message) {
        this.feedback = message;
        this.feedbackAt = System.currentTimeMillis();
    }

    /* ================= UI ================= */

    @Override
    protected void buildButtons() {
        int x = 12;
        int y = this.height - 32;
        addButton("Back", x, y, 92, 22, this::goBack);

        int rightX = this.width / 2 + 24;
        int bw = 118;
        int by = 60;
        addButton("Save current", rightX, by, bw, 22, () -> saveCurrentAs(nameBox.text));
        addButton("Load selected", rightX + bw + 8, by, bw, 22,
                () -> {
                    if (selected != null) {
                        loadProfile(selected);
                    } else {
                        note("Select a profile on the left first.");
                    }
                });
        addButton("Rename", rightX, by + 30, bw, 22,
                () -> {
                    if (selected != null) {
                        renameProfile(selected, nameBox.text);
                    } else {
                        note("Select a profile first.");
                    }
                });
        addButton("Delete", rightX + bw + 8, by + 30, bw, 22,
                () -> {
                    if (selected == null) {
                        note("Select a profile first.");
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if (now - deleteArmedAt < 3000L) {
                        deleteArmedAt = 0L;
                        deleteProfile(selected);
                    } else {
                        deleteArmedAt = now;
                        note("Click Delete again to confirm.");
                    }
                });
        addButton("Export current", rightX, by + 60, bw, 22, this::exportCurrent);
        addButton("Refresh", rightX + bw + 8, by + 60, bw, 22,
                () -> {
                    refreshLists();
                    note("List refreshed.");
                });

        // Built-in baselines.
        int baseY = by + 108;
        addButton("POTATO", rightX, baseY, 76, 20, () -> applyBaseline("POTATO"));
        addButton("BALANCED", rightX + 84, baseY, 76, 20, () -> applyBaseline("BALANCED"));
        addButton("SMOOTH", rightX + 168, baseY, 76, 20, () -> applyBaseline("SMOOTH"));
    }

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(640, this.width - 20);
        int panelH = this.height - 88;
        int panelX = Math.max(10, (this.width - panelW) / 2);
        int panelY = 12;
        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 8);

        Texts.draw(ctx, tr, "CONFIG PROFILES", panelX + 14, panelY + 9, AzyronConstants.COLOR_TEXT, true);
        Texts.draw(ctx, tr, "Save / load / rename / delete whole client setups — JSON files, yours to keep",
                panelX + 14, panelY + 21, AzyronConstants.COLOR_TEXT_DIM, false);

        /* ---------- left: profile list ---------- */
        int listX = panelX + 14;
        int listY = panelY + 40;
        int listW = this.width / 2 - panelX - 24;
        int rowH = 20;
        Texts.draw(ctx, tr, "SAVED PROFILES (" + profiles.size() + ")", listX, listY, AzyronConstants.COLOR_PURPLE_BRIGHT, true);
        int rowY = listY + 14;
        int maxRows = Math.max(3, (this.height - 170) / rowH);
        for (int i = 0; i < profiles.size() && i < maxRows; i++) {
            String file = profiles.get(i);
            String label = file.replace(".json", "");
            boolean sel = file.equals(selected);
            boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= rowY && mouseY < rowY + rowH - 2;
            int fill = sel ? 0xE0281E45 : (hover ? 0xD0241838 : 0xB0181028);
            RenderUtil.fillRoundedRect(ctx, listX, rowY, listW, rowH - 2, 4, fill);
            if (sel) {
                ctx.fill(listX, rowY, listX + 2, rowY + rowH - 2, AzyronConstants.COLOR_ELECTRIC);
            }
            Texts.draw(ctx, tr, label, listX + 8, rowY + 5,
                    sel ? AzyronConstants.COLOR_ELECTRIC : AzyronConstants.COLOR_TEXT, true);
            rowY += rowH;
        }
        if (profiles.isEmpty()) {
            Texts.draw(ctx, tr, "No saved profiles yet.", listX + 2, rowY + 4, AzyronConstants.COLOR_TEXT_DIM, false);
            Texts.draw(ctx, tr, "Type a name and press Save current.", listX + 2, rowY + 16, 0xFF6B7280, false);
        }
        // Import list — exports folder contents.
        int expY = panelY + panelH - 84;
        Texts.draw(ctx, tr, "IMPORT FROM EXPORTS (" + exports.size() + ")", listX, expY,
                AzyronConstants.COLOR_PURPLE_BRIGHT, true);
        int ey = expY + 14;
        for (int i = 0; i < exports.size() && i < 3; i++) {
            String file = exports.get(exports.size() - 1 - i); // newest first
            String label = file.length() > 34 ? file.substring(0, 31) + "…" : file;
            boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= ey && mouseY < ey + 16;
            RenderUtil.fillRoundedRect(ctx, listX, ey, listW, 15, 4, hover ? 0xD0241838 : 0x90181028);
            Texts.draw(ctx, tr, "+ " + label, listX + 6, ey + 3,
                    hover ? AzyronConstants.COLOR_GOOD : AzyronConstants.COLOR_TEXT_DIM, false);
            ey += 17;
        }

        /* ---------- right: input + actions ---------- */
        int rightX = this.width / 2 + 24;
        Texts.draw(ctx, tr, "PROFILE NAME / RENAME TO", rightX, 44, AzyronConstants.COLOR_TEXT_DIM, true);
        nameBox.x = rightX;
        nameBox.y = 54;
        nameBox.width = Math.max(120, panelX + panelW - rightX - 14);
        nameBox.height = 19;
        nameBox.placeholder = "e.g. ranked_potato";
        nameBox.render(ctx, tr, mouseX, mouseY);

        Texts.draw(ctx, tr, "BUILT-IN BASELINES", rightX, 60 + 108 - 8, AzyronConstants.COLOR_TEXT_DIM, true);

        // Status area — where imports/loads etc. report honestly.
        int statusY = panelY + panelH - 40;
        if (deleteArmedAt != 0L && System.currentTimeMillis() - deleteArmedAt < 3000L) {
            Texts.draw(ctx, tr, "Delete armed — click Delete again to confirm", rightX, statusY,
                    AzyronConstants.COLOR_WARN, true);
        } else if (!feedback.isEmpty() && System.currentTimeMillis() - feedbackAt < 5000L) {
            Texts.draw(ctx, tr, feedback, rightX, statusY, AzyronConstants.COLOR_GOOD, false);
        } else {
            Texts.draw(ctx, tr, "Settings always save automatically on change.", rightX, statusY, 0xFF6B7280, false);
        }
    }

    /* ================= events ================= */

    @Override
    protected boolean azyronClick(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // name box focus
            if (nameBox.contains(mouseX, mouseY)) {
                nameBox.focused = true;
                return true;
            }
            nameBox.focused = false;

            // profile rows
            int panelW = Math.min(640, this.width - 20);
            int panelX = Math.max(10, (this.width - panelW) / 2);
            int panelH = this.height - 88;
            int listX = panelX + 14;
            int listW = this.width / 2 - panelX - 24;
            int listY = 12 + 40;
            int rowH = 20;
            int rowY = listY + 14;
            for (int i = 0; i < profiles.size(); i++) {
                if (mouseX >= listX && mouseX < listX + listW && mouseY >= rowY && mouseY < rowY + rowH - 2) {
                    selected = profiles.get(i);
                    return true;
                }
                rowY += rowH;
            }
            // import rows
            int expY = 12 + panelH - 84;
            int ey = expY + 14;
            for (int i = 0; i < Math.min(3, exports.size()); i++) {
                String file = exports.get(exports.size() - 1 - i);
                if (mouseX >= listX && mouseX < listX + listW && mouseY >= ey && mouseY < ey + 15) {
                    importExport(file);
                    return true;
                }
                ey += 17;
            }
        }
        return super.azyronClick(mouseX, mouseY, button);
    }

    @Override
    protected boolean azyronChar(char chr, int modifiers) {
        if (nameBox.focused) {
            nameBox.charTyped(chr);
            return true;
        }
        return super.azyronChar(chr, modifiers);
    }

    @Override
    protected boolean azyronKey(int keyCode, int scanCode, int modifiers) {
        if (nameBox.focused && nameBox.keyPressed(keyCode)) {
            return true;
        }
        return super.azyronKey(keyCode, scanCode, modifiers);
    }
}
