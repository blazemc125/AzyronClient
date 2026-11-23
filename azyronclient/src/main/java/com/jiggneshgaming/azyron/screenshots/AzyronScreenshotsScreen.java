package com.jiggneshgaming.azyron.screenshots;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.menu.BaseAzyronScreen;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * AZYRON CLIENT V2 — Screenshot Manager.
 *
 * <p>Lists the screenshots the game writes to {@code <gameDir>/screenshots},
 * shows real metadata (pixel dimensions read with NativeImage, file size,
 * modification date), and opens the selected shot or the folder with the
 * system viewer/file manager. Preview = instant full-size system preview
 * (one click), which always shows the real file — no stale copies, no
 * GPU texture upload risk, works identically on desktop and Pojav.</p>
 *
 * <p>Honest limitation: in-GUI pixel thumbnails are NOT drawn (the
 * DrawContext texture API is mapping-fragile on 1.21.11 and a wrong call
 * is a compile failure); the one-click system preview covers real preview
 * needs. See TESTING-REPORT.md §6.</p>
 */
public final class AzyronScreenshotsScreen extends BaseAzyronScreen {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("azyronclient/screenshots");

    private static final class Entry {
        final Path path;
        final String name;
        final long sizeBytes;
        final long modifiedAt;

        Entry(Path path) throws java.io.IOException {
            this.path = path;
            this.name = path.getFileName().toString();
            this.sizeBytes = Files.size(path);
            this.modifiedAt = Files.getLastModifiedTime(path).toMillis();
        }
    }

    private List<Entry> entries = new ArrayList<Entry>();
    private final Map<String, int[]> dimensionCache = new HashMap<String, int[]>();
    private int selected;
    private int scroll;
    private String feedback = "";
    private long feedbackAt;

    public AzyronScreenshotsScreen(Screen backTarget) {
        super("Screenshots", backTarget);
        refresh();
    }

    /* ---------------------------------------------------------- */

    private Path screenshotsDir() {
        try {
            return FabricLoader.getInstance().getGameDir().resolve("screenshots");
        } catch (Throwable t) {
            return null;
        }
    }

    private void refresh() {
        entries = new ArrayList<Entry>();
        Path dir = screenshotsDir();
        if (dir != null && Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(AzyronScreenshotsScreen::isImage)
                        .map(AzyronScreenshotsScreen::toEntry)
                        .filter(e -> e != null)
                        .sorted(Comparator.comparingLong(e -> -e.modifiedAt))
                        .limit(64)
                        .forEach(entries::add);
            } catch (Throwable t) {
                LOGGER.warn("Could not list screenshots in {}", dir, t);
            }
        }
        selected = 0;
        scroll = 0;
        note(entries.size() + " screenshot(s) found");
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private static Entry toEntry(Path path) {
        try {
            return new Entry(path);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Pixel dimensions via NativeImage (no GPU involvement). Cached per file. */
    private int[] dimensions(Path path) {
        int[] cached = dimensionCache.get(path.toString());
        if (cached != null) {
            return cached;
        }
        int[] dims = {-1, -1};
        try (InputStream in = Files.newInputStream(path)) {
            net.minecraft.client.texture.NativeImage image = net.minecraft.client.texture.NativeImage.read(in);
            try {
                dims = new int[]{image.getWidth(), image.getHeight()};
            } finally {
                image.close();
            }
        } catch (Throwable t) {
            // Leave unknown — the file may not be a decodable image.
        }
        dimensionCache.put(path.toString(), dims);
        return dims;
    }

    private void openFile(Path file) {
        try {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            desktop.open(file.toFile());
            note("Opened " + file.getFileName());
        } catch (Throwable t) {
            note("Cannot open a system viewer on this device (" + t.getClass().getSimpleName() + ").");
        }
    }

    private void openFolder() {
        Path dir = screenshotsDir();
        try {
            if (dir == null || !Files.isDirectory(dir)) {
                note("Screenshot folder does not exist yet — take one with F2 first.");
                return;
            }
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            desktop.open(dir.toFile());
            note("Opened the screenshots folder.");
        } catch (Throwable t) {
            note("Cannot open the folder on this device (" + t.getClass().getSimpleName() + ").");
        }
    }

    private void note(String message) {
        this.feedback = message;
        this.feedbackAt = System.currentTimeMillis();
    }

    /* ---------------------------------------------------------- */

    @Override
    protected void buildButtons() {
        int x = 12;
        int y = this.height - 32;
        addButton("Back", x, y, 92, 22, this::goBack);
        addButton("Refresh", x + 100, y, 92, 22, this::refresh);
        addButton("Open folder", x + 200, y, 110, 22, this::openFolder);
        addButton("Open image", x + 318, y, 110, 22,
                () -> {
                    if (selected >= 0 && selected < entries.size()) {
                        openFile(entries.get(selected).path);
                    } else {
                        note("Select a screenshot first.");
                    }
                });
    }

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(640, this.width - 20);
        int panelH = this.height - 88;
        int panelX = Math.max(10, (this.width - panelW) / 2);
        int panelY = 12;
        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 8);

        Texts.draw(ctx, tr, "SCREENSHOTS", panelX + 14, panelY + 9, AzyronConstants.COLOR_TEXT, true);
        Texts.draw(ctx, tr, "Everything in <game>/screenshots — F2 saves a shot",
                panelX + 14, panelY + 21, AzyronConstants.COLOR_TEXT_DIM, false);

        /* -------- left: list -------- */
        int listX = panelX + 14;
        int listY = panelY + 38;
        int listW = Math.min(300, panelW - 30);
        int rowH = 34;
        int bottom = panelY + panelH - 34;
        int maxRows = Math.max(1, (bottom - listY) / rowH);
        int maxScroll = Math.max(0, entries.size() - maxRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (entries.isEmpty()) {
            Texts.draw(ctx, tr, "No screenshots yet.", listX + 4, listY + 10, AzyronConstants.COLOR_TEXT_DIM, false);
            Texts.draw(ctx, tr, "Press F2 in-game; shots will appear here.", listX + 4, listY + 23, 0xFF6B7280, false);
        }
        for (int i = 0; i < entries.size() && i < maxRows + scroll; i++) {
            if (i < scroll) {
                continue;
            }
            Entry entry = entries.get(i);
            int y = listY + (i - scroll) * rowH;
            boolean sel = i == selected;
            boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= y && mouseY < y + rowH - 3;
            int fill = sel ? 0xE0281E45 : (hover ? 0xD0241838 : 0x90181028);
            RenderUtil.fillRoundedRect(ctx, listX, y, listW, rowH - 3, 5, fill);
            if (sel) {
                ctx.fill(listX, y, listX + 2, y + rowH - 3, AzyronConstants.COLOR_ELECTRIC);
            }
            String name = entry.name.length() > 40 ? entry.name.substring(0, 37) + "…" : entry.name;
            Texts.draw(ctx, tr, name, listX + 8, y + 4,
                    sel ? AzyronConstants.COLOR_ELECTRIC : AzyronConstants.COLOR_TEXT, true);
            int[] dims = dimensions(entry.path);
            String meta = (dims[0] > 0 ? dims[0] + "x" + dims[1] : "unknown")
                    + "  •  " + formatSize(entry.sizeBytes) + "  •  " + formatDate(entry.modifiedAt);
            Texts.draw(ctx, tr, meta, listX + 8, y + 16, AzyronConstants.COLOR_TEXT_DIM, false);
        }

        /* -------- right: preview card -------- */
        int detailX = listX + listW + 16;
        int detailW = panelX + panelW - detailX - 14;
        if (selected >= 0 && selected < entries.size()) {
            Entry entry = entries.get(selected);
            RenderUtil.fillRoundedRect(ctx, detailX, listY, detailW, bottom - listY, 6, 0x9014100F22);
            RenderUtil.drawRoundedBorder(ctx, detailX, listY, detailW, bottom - listY, 6, AzyronConstants.COLOR_PANEL_BORDER);

            // Procedural "photo" glyph — honest artwork, not a fake pixel preview.
            drawPhotoGlyph(ctx, detailX + detailW / 2, listY + 74);

            String name = entry.name;
            int nameY = listY + 130;
            wrapText(ctx, tr, name, detailX + 12, nameY, detailW - 24, 11, AzyronConstants.COLOR_TEXT, 2);

            int[] dims = dimensions(entry.path);
            String[] lines = {
                    "Dimensions:  " + (dims[0] > 0 ? dims[0] + " × " + dims[1] + " px" : "unknown"),
                    "File size:   " + formatSize(entry.sizeBytes),
                    "Modified:    " + formatDateTime(entry.modifiedAt),
                    "Folder:      …/" + entry.path.getParent().getFileName()
            };
            int lineY = nameY + 48;
            for (String line : lines) {
                Texts.draw(ctx, tr, line, detailX + 12, lineY, AzyronConstants.COLOR_TEXT_DIM, false);
                lineY += 13;
            }
            Texts.draw(ctx, tr, "\"Open image\" shows the real, full-size file.", detailX + 12, lineY + 4,
                    0xFF6B7280, false);
        } else {
            Texts.draw(ctx, tr, "Select a screenshot.", detailX + 12, listY + 10, AzyronConstants.COLOR_TEXT_DIM, false);
        }

        /* -------- feedback -------- */
        if (!feedback.isEmpty() && System.currentTimeMillis() - feedbackAt < 4000L) {
            Texts.draw(ctx, tr, feedback, panelX + 14, panelY + panelH - 20, AzyronConstants.COLOR_GOOD, true);
        }
    }

    private void drawPhotoGlyph(DrawContext ctx, int cx, int cy) {
        // Rounded frame + mountain + sun — a clean, recognizable photo glyph.
        int w = 96;
        int h = 72;
        RenderUtil.fillRoundedRect(ctx, cx - w / 2, cy - h / 2, w, h, 6, 0xFF241A3E);
        RenderUtil.drawRoundedBorder(ctx, cx - w / 2, cy - h / 2, w, h, 6, AzyronConstants.COLOR_ELECTRIC);
        // sun
        for (int dy = -6; dy <= 6; dy++) {
            double dyC = dy - 0.5D;
            int half = (int) Math.floor(Math.sqrt(Math.max(0.0D, 36 - dyC * dyC)));
            ctx.fill(cx - 20 - half, cy - 12 + dy, cx - 20 + half + 1, cy - 12 + dy + 1, 0xFFFDE047);
        }
        // mountains
        for (int i = 0; i < 26; i++) {
            ctx.fill(cx - 38 + i, cy + 22 - Math.min(i, 26 - i) - 8, cx - 37 + i, cy + 22, 0xFF7C3AED);
        }
        for (int i = 0; i < 34; i++) {
            ctx.fill(cx - 2 + i, cy + 22 - Math.min(i, 34 - i) - 14, cx - 1 + i, cy + 22, 0xFF33CCFF);
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        } else if (bytes < 1024L * 1024L) {
            return (bytes / 1024L) + " KB";
        } else {
            return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0D * 1024.0D));
        }
    }

    private static String formatDate(long millis) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        java.util.Calendar now = java.util.Calendar.getInstance();
        boolean today = calendar.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
                && calendar.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR);
        if (today) {
            return String.format(java.util.Locale.ROOT, "%02d:%02d",
                    calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE));
        }
        return String.format(java.util.Locale.ROOT, "%02d/%02d",
                calendar.get(java.util.Calendar.DAY_OF_MONTH), calendar.get(java.util.Calendar.MONTH) + 1);
    }

    private static String formatDateTime(long millis) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT)
                .format(new java.util.Date(millis));
    }

    private static void wrapText(DrawContext ctx, TextRenderer tr, String text, int x, int y,
                                 int maxWidth, int lineHeight, int color, int maxLines) {
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        int cursor = 0;
        while (cursor < text.length() && lineIndex < maxLines) {
            char ch = text.charAt(cursor);
            String candidate = line.toString() + ch;
            if (tr.getWidth(candidate) > maxWidth && line.length() > 0) {
                Texts.draw(ctx, tr, line.toString(), x, y + lineIndex * lineHeight, color, false);
                line = new StringBuilder();
                lineIndex++;
            } else {
                line.append(ch);
                cursor++;
            }
        }
        if (line.length() > 0 && lineIndex < maxLines) {
            Texts.draw(ctx, tr, line.toString(), x, y + lineIndex * lineHeight, color, false);
        }
    }

    /* ---------------------------------------------------------- */

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            int panelW = Math.min(640, this.width - 20);
            int panelX = Math.max(10, (this.width - panelW) / 2);
            int panelH = this.height - 88;
            int listX = panelX + 14;
            int listY = 12 + 38;
            int listW = Math.min(300, panelW - 30);
            int rowH = 34;
            for (int i = scroll; i < entries.size(); i++) {
                int y = listY + (i - scroll) * rowH;
                if (mouseY >= y && mouseY < y + rowH - 3 && mouseX >= listX && mouseX < listX + listW) {
                    selected = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int page = Math.max(3, (this.height - 170) / 34);
        scroll -= (int) verticalAmount * 2;
        int maxScroll = Math.max(0, entries.size() - page);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        return true;
    }
}
