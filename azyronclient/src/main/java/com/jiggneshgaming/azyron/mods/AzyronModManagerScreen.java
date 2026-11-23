package com.jiggneshgaming.azyron.mods;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.menu.BaseAzyronScreen;
import com.jiggneshgaming.azyron.menu.SearchBox;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * AZYRON CLIENT V2 — installed Mod Manager.
 *
 * <p>Real introspection of every loaded mod via the Fabric Loader API:
 * name, id, version, description, authors, license, environment, and a
 * dependency analysis (missing / version-incompatible / conflicting — the
 * last two resolved against the actually loaded versions, including
 * Minecraft itself). Filter chips (All / Client / Universal) and live
 * search.</p>
 *
 * <p>Honest limitation: Fabric does not permit enabling or disabling mods
 * at runtime — no client can do that safely. This screen therefore gives
 * you inspection and dependency health instead of a fake switch; to
 * disable a mod, move its JAR out of the mods folder (the button opens it).</p>
 */
public final class AzyronModManagerScreen extends BaseAzyronScreen {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("azyronclient/modmanager");

    private enum Filter {
        ALL("All"),
        CLIENT("Client"),
        UNIVERSAL("Universal");

        final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    private static final class ModInfo {
        final ModContainer container;
        final String id;
        final String name;
        final String version;
        final String description;
        final String authors;
        final String license;
        final String environment;
        final List<String> depProblems;

        ModInfo(ModContainer container) {
            this.container = container;
            ModMetadata metadata = container.getMetadata();
            this.id = metadata.getId();
            this.name = metadata.getName();
            this.version = String.valueOf(metadata.getVersion());
            this.description = metadata.getDescription() == null ? "" : metadata.getDescription().trim();
            this.authors = joinPeople(metadata.getAuthors());
            java.util.Collection<String> licenses = metadata.getLicense();
            this.license = licenses == null || licenses.isEmpty() ? "—" : String.join(", ", licenses);
            this.environment = metadata.getEnvironment() == null
                    ? "*" : metadata.getEnvironment().name().toLowerCase(Locale.ROOT);
            this.depProblems = analyzeDependencies(container);
        }
    }

    private final SearchBox search = new SearchBox(0, 0, 0, 0);
    private Filter filter = Filter.ALL;
    private List<ModInfo> mods = new ArrayList<ModInfo>();
    private int selected;
    private int scroll;
    private String feedback = "";
    private long feedbackAt;

    public AzyronModManagerScreen(Screen backTarget) {
        super("Mod Manager", backTarget);
        rescan();
    }

    /* ================= data ================= */

    private void rescan() {
        mods = new ArrayList<ModInfo>();
        try {
            for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
                try {
                    mods.add(new ModInfo(container));
                } catch (Throwable t) {
                    LOGGER.warn("Skipping unreadable mod container: {}", t.toString());
                }
            }
        } catch (Throwable t) {
            LOGGER.error("FabricLoader mod list unavailable", t);
        }
        mods.sort(Comparator.comparing(m -> m.name.toLowerCase(Locale.ROOT)));
        selected = 0;
        scroll = 0;
        note(mods.size() + " mods loaded");
    }

    private static String joinPeople(java.util.Collection<net.fabricmc.loader.api.metadata.Person> people) {
        if (people == null || people.isEmpty()) {
            return "—";
        }
        List<String> names = new ArrayList<String>();
        for (net.fabricmc.loader.api.metadata.Person person : people) {
            try {
                names.add(person.getName());
            } catch (Throwable t) {
                names.add(person.toString());
            }
        }
        return String.join(", ", names);
    }

    private static List<String> analyzeDependencies(ModContainer container) {
        List<String> problems = new ArrayList<String>();
        try {
            Collection<ModDependency> dependencies = container.getMetadata().getDependencies();
            if (dependencies == null) {
                return problems;
            }
            for (ModDependency dep : dependencies) {
                String depId = dep.getModId();
                boolean required = dep.getKind() == ModDependency.Kind.DEPENDS;
                boolean conflicts = dep.getKind() == ModDependency.Kind.BREAKS
                        || dep.getKind() == ModDependency.Kind.CONFLICTS;
                if (!required && !conflicts) {
                    continue;
                }
                ModContainer provider = null;
                try {
                    provider = FabricLoader.getInstance().getModContainer(depId).orElse(null);
                } catch (Throwable ignored) {
                    // unknown id — treat as missing when required
                }
                if (required && provider == null) {
                    problems.add("MISSING: requires " + depId + " " + intervalText(dep));
                    continue;
                }
                if (provider == null) {
                    continue; // conflicts only matter when present
                }
                Version installed = provider.getMetadata().getVersion();
                boolean inRange;
                try {
                    inRange = inAnyInterval(dep.getVersionIntervals(), installed);
                } catch (Throwable t) {
                    inRange = true; // could not evaluate the intervals — don't cry wolf
                }
                if (required && !inRange) {
                    problems.add("VERSION: needs " + depId + " " + intervalText(dep) + ", found " + installed);
                } else if (conflicts && inRange) {
                    problems.add("CONFLICT: breaks with " + depId + " " + installed);
                }
            }
        } catch (Throwable t) {
            problems.add("Dependency check failed: " + t.getClass().getSimpleName());
        }
        return problems;
    }

    /** True when {@code installed} matches ANY of the dependency's intervals (or no constraint exists). */
    private static boolean inAnyInterval(List<VersionInterval> intervals, Version installed) {
        if (intervals == null || intervals.isEmpty()) {
            return true; // no constraint
        }
        for (VersionInterval interval : intervals) {
            if (interval == null) {
                return true;
            }
            try {
                Version min = interval.getMin();
                Version max = interval.getMax();
                if (min != null) {
                    int cmp = installed.compareTo(min);
                    if (cmp < 0 || (cmp == 0 && !interval.isMinInclusive())) {
                        continue;
                    }
                }
                if (max != null) {
                    int cmp = installed.compareTo(max);
                    if (cmp > 0 || (cmp == 0 && !interval.isMaxInclusive())) {
                        continue;
                    }
                }
                return true;
            } catch (Throwable t) {
                return true; // evaluation failed — don't report a bogus problem
            }
        }
        return false;
    }

    private static String intervalText(ModDependency dep) {
        try {
            List<VersionInterval> intervals = dep.getVersionIntervals();
            if (intervals != null && !intervals.isEmpty()) {
                return String.join(" or ", intervals.stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.toList()));
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return "*";
    }

    private List<ModInfo> visibleMods() {
        List<ModInfo> out = new ArrayList<ModInfo>();
        String query = search.text == null ? "" : search.text.trim().toLowerCase(Locale.ROOT);
        for (ModInfo mod : mods) {
            if (filter == Filter.CLIENT && !"client".equalsIgnoreCase(mod.environment)) {
                continue;
            }
            if (filter == Filter.UNIVERSAL && !(mod.environment.equals("*") || mod.environment.equalsIgnoreCase("universal"))) {
                continue;
            }
            if (!query.isEmpty()) {
                boolean hit = mod.name.toLowerCase(Locale.ROOT).contains(query)
                        || mod.id.toLowerCase(Locale.ROOT).contains(query)
                        || mod.description.toLowerCase(Locale.ROOT).contains(query);
                if (!hit) {
                    continue;
                }
            }
            out.add(mod);
        }
        return out;
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
        addButton("Refresh", x + 100, y, 92, 22, this::rescan);
        addButton("Open mods folder", x + 200, y, 128, 22,
                () -> {
                    try {
                        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                        java.awt.Desktop.getDesktop().open(modsDir.toFile());
                        note("Opened the mods folder.");
                    } catch (Throwable t) {
                        note("Cannot open a folder on this device (" + t.getClass().getSimpleName() + ").");
                    }
                });
    }

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(680, this.width - 20);
        int panelH = this.height - 88;
        int panelX = Math.max(10, (this.width - panelW) / 2);
        int panelY = 12;
        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 8);

        /* -------- header + filter chips -------- */
        Texts.draw(ctx, tr, "INSTALLED MODS", panelX + 14, panelY + 9, AzyronConstants.COLOR_TEXT, true);
        Texts.draw(ctx, tr, mods.size() + " loaded • compatibility analyzed live",
                panelX + 14, panelY + 21, AzyronConstants.COLOR_TEXT_DIM, false);

        int chipX = panelX + panelW - 14;
        for (int i = Filter.values().length - 1; i >= 0; i--) {
            Filter f = Filter.values()[i];
            int w = tr.getWidth(f.label) + 14;
            chipX -= w + 6;
            boolean active = f == filter;
            RenderUtil.fillRoundedRect(ctx, chipX, panelY + 10, w, 15, 7, active ? 0xFF0E86C9 : 0xFF241A3E);
            Texts.draw(ctx, tr, f.label, chipX + 7, panelY + 13,
                    active ? 0xFFFFFFFF : AzyronConstants.COLOR_TEXT_DIM, false);
        }

        /* -------- search -------- */
        int listX = panelX + 14;
        int listY = panelY + 38;
        int listW = Math.min(310, panelW / 2);
        search.x = listX;
        search.y = listY;
        search.width = listW;
        search.height = 17;
        search.placeholder = "Search name, id, description...";
        search.render(ctx, tr, mouseX, mouseY);

        /* -------- list -------- */
        int rowY = listY + 24;
        int bottom = panelY + panelH - 34;
        int rowH = 30;
        List<ModInfo> visible = visibleMods();
        int maxScroll = Math.max(0, visible.size() - Math.max(1, (bottom - rowY) / rowH));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = scroll; i < visible.size(); i++) {
            int y = rowY + (i - scroll) * rowH;
            if (y + rowH - 3 > bottom) {
                break;
            }
            ModInfo mod = visible.get(i);
            boolean sel = i == selected;
            boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= y && mouseY < y + rowH - 3;
            int fill = sel ? 0xE0281E45 : (hover ? 0xD0241838 : 0x90181028);
            RenderUtil.fillRoundedRect(ctx, listX, y, listW, rowH - 3, 5, fill);
            if (sel) {
                ctx.fill(listX, y, listX + 2, y + rowH - 3, AzyronConstants.COLOR_ELECTRIC);
            }
            String name = mod.name.length() > 34 ? mod.name.substring(0, 31) + "…" : mod.name;
            int nameColor = mod.depProblems.isEmpty() ? AzyronConstants.COLOR_TEXT : AzyronConstants.COLOR_WARN;
            Texts.draw(ctx, tr, name, listX + 8, y + 3, nameColor, true);
            String meta = "v" + mod.version + "  •  " + envLabel(mod.environment)
                    + (mod.depProblems.isEmpty() ? "" : "  •  " + mod.depProblems.size() + " issue(s)");
            Texts.draw(ctx, tr, meta, listX + 8, y + 15, AzyronConstants.COLOR_TEXT_DIM, false);
        }
        if (visible.isEmpty()) {
            Texts.draw(ctx, tr, "No mods match.", listX + 4, rowY + 8, AzyronConstants.COLOR_TEXT_DIM, false);
        }

        /* -------- detail pane -------- */
        int detailX = listX + listW + 16;
        int detailW = panelX + panelW - detailX - 14;
        int detailBottom = bottom;
        RenderUtil.fillRoundedRect(ctx, detailX, listY, detailW, detailBottom - listY, 6, 0x90141022);
        RenderUtil.drawRoundedBorder(ctx, detailX, listY, detailW, detailBottom - listY, 6,
                AzyronConstants.COLOR_PANEL_BORDER);

        if (selected >= 0 && selected < visible.size()) {
            ModInfo mod = visible.get(selected);
            int tx = detailX + 12;
            int ty = listY + 10;
            Texts.draw(ctx, tr, mod.name, tx, ty, AzyronConstants.COLOR_ELECTRIC, true);
            ty += 13;
            Texts.draw(ctx, tr, "id: " + mod.id + "   version: " + mod.version, tx, ty, AzyronConstants.COLOR_TEXT_DIM, false);
            ty += 12;
            Texts.draw(ctx, tr, "license: " + mod.license, tx, ty, AzyronConstants.COLOR_TEXT_DIM, false);
            ty += 12;
            Texts.draw(ctx, tr, "authors: " + (mod.authors.length() > 60 ? mod.authors.substring(0, 57) + "…" : mod.authors),
                    tx, ty, AzyronConstants.COLOR_TEXT_DIM, false);
            ty += 12;
            Texts.draw(ctx, tr, "environment: " + envLabel(mod.environment), tx, ty, AzyronConstants.COLOR_TEXT_DIM, false);
            ty += 16;

            // Compatibility badge
            if (mod.depProblems.isEmpty()) {
                badge(ctx, tr, tx, ty, "COMPATIBILITY: OK", AzyronConstants.COLOR_GOOD, 0xFF14532D);
            } else {
                badge(ctx, tr, tx, ty, "COMPATIBILITY: " + mod.depProblems.size() + " ISSUE(S)",
                        AzyronConstants.COLOR_WARN, 0xFF451A03);
            }
            ty += 18;
            for (String problem : mod.depProblems) {
                if (ty + 11 > detailBottom - 40) {
                    Texts.draw(ctx, tr, "… and more", tx, ty, AzyronConstants.COLOR_WARN, false);
                    break;
                }
                Texts.draw(ctx, tr, "• " + (problem.length() > 72 ? problem.substring(0, 69) + "…" : problem),
                        tx, ty, problem.startsWith("MISSING") ? AzyronConstants.COLOR_BAD : AzyronConstants.COLOR_WARN,
                        false);
                ty += 11;
            }

            // Description (wrapped, clipped)
            int descY = Math.max(ty + 8, detailBottom - 66);
            if (!mod.description.isEmpty()) {
                Texts.draw(ctx, tr, "DESCRIPTION", tx, descY, AzyronConstants.COLOR_PURPLE_BRIGHT, true);
                wrapText(ctx, tr, mod.description, tx, descY + 12, detailW - 24, 10,
                        AzyronConstants.COLOR_TEXT_DIM, 3);
            }
            Texts.draw(ctx, tr,
                    "Fabric cannot toggle mods at runtime — move the JAR out of the mods folder to disable one.",
                    tx, detailBottom - 20, 0xFF6B7280, false);
        } else {
            Texts.draw(ctx, tr, "Select a mod.", detailX + 12, listY + 10, AzyronConstants.COLOR_TEXT_DIM, false);
        }

        /* -------- feedback -------- */
        if (!feedback.isEmpty() && System.currentTimeMillis() - feedbackAt < 4000L) {
            Texts.draw(ctx, tr, feedback, panelX + 14, panelY + panelH - 20, AzyronConstants.COLOR_GOOD, true);
        }
    }

    private static void badge(DrawContext ctx, TextRenderer tr, int x, int y, String text, int color, int fill) {
        int w = tr.getWidth(text) + 12;
        RenderUtil.fillRoundedRect(ctx, x, y, w, 14, 4, fill);
        Texts.draw(ctx, tr, text, x + 6, y + 3, color, true);
    }

    private static String envLabel(String environment) {
        if (environment == null || environment.equals("*") || environment.equalsIgnoreCase("universal")) {
            return "universal";
        }
        return environment.toLowerCase(Locale.ROOT);
    }

    private static void wrapText(DrawContext ctx, TextRenderer tr, String text, int x, int y,
                                 int maxWidth, int lineHeight, int color, int maxLines) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (tr.getWidth(candidate) > maxWidth && line.length() > 0) {
                Texts.draw(ctx, tr, line.toString(), x, y + lineIndex * lineHeight, color, false);
                lineIndex++;
                if (lineIndex >= maxLines) {
                    return;
                }
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0 && lineIndex < maxLines) {
            Texts.draw(ctx, tr, line.toString(), x, y + lineIndex * lineHeight, color, false);
        }
    }

    /* ================= events ================= */

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            int panelW = Math.min(680, this.width - 20);
            int panelX = Math.max(10, (this.width - panelW) / 2);
            int panelY = 12;

            // search focus
            if (search.contains(mouseX, mouseY)) {
                search.focused = true;
                return true;
            }
            search.focused = false;

            // filter chips
            int chipX = panelX + panelW - 14;
            TextRenderer tr = this.textRenderer;
            for (int i = Filter.values().length - 1; i >= 0; i--) {
                Filter f = Filter.values()[i];
                int w = (tr != null ? tr.getWidth(f.label) : 40) + 14;
                chipX -= w + 6;
                if (mouseX >= chipX && mouseX < chipX + w && mouseY >= panelY + 10 && mouseY < panelY + 25) {
                    filter = f;
                    scroll = 0;
                    selected = 0;
                    return true;
                }
            }

            // rows
            int listX = panelX + 14;
            int listY = panelY + 38;
            int rowY = listY + 24;
            int listW = Math.min(310, panelW / 2);
            List<ModInfo> visible = visibleMods();
            for (int i = scroll; i < visible.size(); i++) {
                int y = rowY + (i - scroll) * 30;
                if (mouseX >= listX && mouseX < listX + listW && mouseY >= y && mouseY < y + 27) {
                    selected = i; // absolute index into the filtered list
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) verticalAmount * 2;
        List<ModInfo> visible = visibleMods();
        int maxScroll = Math.max(0, visible.size() - 8);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        return true;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (search.focused) {
            search.charTyped((char) input.codepoint());
            scroll = 0;
            selected = 0;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int keyCode = input.key();
        if (search.focused && search.keyPressed(keyCode)) {
            return true;
        }
        return super.keyPressed(input);
    }
}
