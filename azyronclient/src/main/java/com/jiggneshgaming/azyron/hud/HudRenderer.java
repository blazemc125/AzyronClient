package com.jiggneshgaming.azyron.hud;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.core.format.FormatUtils;
import com.jiggneshgaming.azyron.core.hud.HudGeometry;
import com.jiggneshgaming.azyron.cps.CpsManager;
import com.jiggneshgaming.azyron.keys.KeystrokesTracker;
import com.jiggneshgaming.azyron.sprint.ToggleSprint;
import com.jiggneshgaming.azyron.util.ClientRefs;
import com.jiggneshgaming.azyron.util.PotionNames;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Sessions;
import com.jiggneshgaming.azyron.util.Texts;
import com.jiggneshgaming.azyron.zoom.ZoomManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.UUID;

import com.jiggneshgaming.azyron.compat.MatrixHelper;
/**
 * AZYRON CLIENT — HUD renderer.
 *
 * <p>Draws every AZYRON HUD element. Used both from the Fabric HUD hook
 * (live game) and from the HUD editor (demo data).</p>
 */
public final class HudRenderer {

    public static final String FPS = "fps";
    public static final String CPS = "cps";
    public static final String PING = "ping";
    public static final String COORDS = "coordinates";
    public static final String KEYSTROKES = "keystrokes";
    public static final String ARMOR = "armor";
    public static final String POTIONS = "potions";
    public static final String SPRINT = "sprint_indicator";
    public static final String ZOOM = "zoom_indicator";
    public static final String DIRECTION = "direction";
    public static final String CLOCK = "clock";
    public static final String BIOME = "biome";
    public static final String STATUS = "player_status";

    public static final String[] ALL_IDS = {
            FPS, CPS, PING, COORDS, KEYSTROKES, ARMOR, POTIONS, SPRINT, ZOOM,
            DIRECTION, CLOCK, BIOME, STATUS
    };

    private HudRenderer() {
    }

    /* ================================================================ */
    /* Entry points                                                       */
    /* ================================================================ */

    /** Called from the Fabric HUD hook every frame. */
    public static void renderHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        // F1 hides the vanilla HUD — respect it.
        if (client.options != null && client.options.hudHidden) {
            return;
        }
        for (String id : ALL_IDS) {
            renderElement(ctx, id, false);
        }
    }

    /** Draw one element; when {@code demo} is true, placeholder data is used. */
    public static void renderElement(DrawContext ctx, String id, boolean demo) {
        AzyronConfigModel config = AzyronConfig.get();
        if (!demo && !config.isHudVisible(id)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = ClientRefs.textRenderer(client);
        if (textRenderer == null) {
            return;
        }
        AzyronConfigModel.HudPosition pos = config.positionOf(id);
        int[] size = measure(id, demo);
        float effective = (float) effectiveScale(id);
        int[] scaledSize = new int[]{Math.max(1, Math.round(size[0] * effective)),
                Math.max(1, Math.round(size[1] * effective))};
        int[] clamped = HudGeometry.clampToScreen(pos.x, pos.y, scaledSize[0], scaledSize[1],
                ClientRefs.scaledWidth(client), ClientRefs.scaledHeight(client));
        int x = clamped[0];
        int y = clamped[1];

        float scale = (float) effectiveScale(id);
        if (Math.abs(scale - 1.0F) > 0.01F) {
            MatrixHelper.pushMatrix(ctx.getMatrices());
            MatrixHelper.translate(ctx.getMatrices(), (float) x, (float) y);
            MatrixHelper.scale(ctx.getMatrices(), scale, scale);
            MatrixHelper.translate(ctx.getMatrices(), (float) -x, (float) -y);
            renderElementInner(ctx, textRenderer, client, id, demo, x, y, size[0], size[1]);
            MatrixHelper.popMatrix(ctx.getMatrices());
        } else {
            renderElementInner(ctx, textRenderer, client, id, demo, x, y, size[0], size[1]);
        }
    }

    /* ================================================================ */
    /* Per-element                                                      */
    /* ================================================================ */

    private static void renderElementInner(DrawContext ctx, TextRenderer tr, MinecraftClient client,
                                           String id, boolean demo, int x, int y, int width, int height) {
        AzyronConfigModel config = AzyronConfig.get();
        switch (id) {
            case FPS: {
                int fps = demo ? 144 : safeCurrentFps(client);
                String text = FormatUtils.formatFps(fps);
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 5, y + 3, FormatUtils.fpsColor(fps), true);
                break;
            }
            case CPS: {
                int left = demo ? 9 : CpsManager.leftCps();
                int right = demo ? 3 : CpsManager.rightCps();
                String text = "CPS " + left + " | " + right;
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 5, y + 3, 0xFFE2E8F0, true);
                break;
            }
            case PING: {
                int ping = demo ? 42 : currentPing(client);
                FormatUtils.PingQuality quality = FormatUtils.pingQuality(ping);
                String text = "Ping: " + FormatUtils.formatPing(ping);
                fillPanel(ctx, x, y, width, height, config);
                // Signal-bars glyph drawn manually.
                drawPingBars(ctx, x + 5, y + 3, quality.color());
                Texts.draw(ctx, tr, text, x + 19, y + 3, quality.color(), true);
                break;
            }
            case COORDS: {
                String text;
                if (demo) {
                    text = "XYZ: 128 / 64 / -230";
                } else {
                    PlayerEntity player = client.player;
                    text = player == null ? "XYZ: —"
                            : FormatUtils.formatCoordsInt(player.getX(), player.getY(), player.getZ());
                }
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 5, y + 3, 0xFFE2E8F0, true);
                break;
            }
            case KEYSTROKES:
                renderKeystrokes(ctx, tr, client, demo, x, y);
                break;
            case ARMOR:
                renderArmor(ctx, tr, client, demo, x, y);
                break;
            case POTIONS:
                renderPotions(ctx, tr, client, demo, x, y);
                break;
            case SPRINT: {
                boolean on = demo ? ToggleSprint.isActive() : AzyronConfig.get().toggleSprint;
                String text = on ? "Sprint: ON" : "Sprint: OFF";
                int color = on ? AzyronConstants.COLOR_GOOD : AzyronConstants.COLOR_TEXT_DIM;
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 5, y + 3, color, true);
                break;
            }
            case ZOOM: {
                boolean zooming = ZoomManager.isZooming() || demo;
                if (!zooming && !demo) {
                    return; // indicator only while zooming
                }
                String text = String.format(java.util.Locale.ROOT, "ZOOM x%.1f", ZoomManager.effectiveZoom());
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 5, y + 3, AzyronConstants.COLOR_ELECTRIC, true);
                break;
            }
            case DIRECTION: {
                String text;
                if (demo) {
                    text = "Facing: NW";
                } else {
                    PlayerEntity player = client.player;
                    text = player == null ? "Facing: —" : "Facing: " + cardinalFromYaw(player.getYaw());
                }
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 6, y + 3, config.hudAccent, true);
                break;
            }
            case CLOCK: {
                String text = demo ? "Day 1  •  12:00" : worldClockText(client);
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 6, y + 3, 0xFFE2E8F0, true);
                break;
            }
            case BIOME: {
                String text = demo ? "Biome: Plains" : "Biome: " + currentBiome(client);
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 6, y + 3, 0xFF9BD18B, true);
                break;
            }
            case STATUS: {
                int hp, food, maxHp;
                if (demo) {
                    hp = 20;
                    food = 18;
                    maxHp = 20;
                } else {
                    PlayerEntity player = client.player;
                    if (player == null) {
                        return;
                    }
                    hp = (int) Math.ceil(player.getHealth());
                    maxHp = Math.max(1, (int) player.getMaxHealth());
                    food = player.getHungerManager() == null ? 0 : player.getHungerManager().getFoodLevel();
                }
                float fraction = maxHp <= 0 ? 0F : hp / (float) maxHp;
                int hpColor = fraction > 0.6F ? AzyronConstants.COLOR_GOOD
                        : fraction > 0.3F ? AzyronConstants.COLOR_WARN : AzyronConstants.COLOR_BAD;
                String text = "HP " + hp + "/" + maxHp + "   Food " + food;
                fillPanel(ctx, x, y, width, height, config);
                Texts.draw(ctx, tr, text, x + 6, y + 3, hpColor, true);
                break;
            }
            default:
                break;
        }
    }

    /* ------------------------------ V2 helpers ------------------------- */

    private static String cardinalFromYaw(float yaw) {
        float normalized = (yaw % 360F + 360F) % 360F;
        int index = Math.round(normalized / 45F) % 8;
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        return dirs[index];
    }

    private static String worldClockText(MinecraftClient client) {
        try {
            if (client.world == null) {
                return "Clock: —";
            }
            long time = client.world.getTimeOfDay();
            long dayTime = ((time % 24000L) + 24000L) % 24000L;
            int hour = (int) ((dayTime / 1000L + 6L) % 24L);
            int minute = (int) ((dayTime % 1000L) * 60L / 1000L);
            long day = Math.floorDiv(time, 24000L) + 1L;
            return String.format(java.util.Locale.ROOT, "Day %d  •  %02d:%02d", day, hour, minute);
        } catch (Throwable t) {
            return "Clock: —";
        }
    }

    private static String currentBiome(MinecraftClient client) {
        try {
            PlayerEntity player = client.player;
            if (client.world == null || player == null) {
                return "—";
            }
            net.minecraft.util.math.BlockPos pos = player.getBlockPos();
            java.util.Optional<net.minecraft.registry.RegistryKey<net.minecraft.world.biome.Biome>> key =
                    client.world.getBiome(pos).getKey();
            if (!key.isPresent()) {
                return "—";
            }
            String path = key.get().getValue().getPath().replace('_', ' ');
            StringBuilder out = new StringBuilder();
            for (String word : path.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return out.toString();
        } catch (Throwable t) {
            return "—";
        }
    }

    private static void fillPanel(DrawContext ctx, int x, int y, int width, int height, AzyronConfigModel config) {
        RenderUtil.fillHudPanel(ctx, x, y, width, height, config.backgroundOpacity);
        // V2 accent strip — the user's HUD accent color anchors every panel.
        int accent = config.hudAccent;
        ctx.fill(x, y + 2, x + 2, y + height - 2, accent);
    }

    /* ------------------------------ fps / ping helpers -------------- */

    private static int safeCurrentFps(MinecraftClient client) {
        try {
            return client.getCurrentFps();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static int currentPing(MinecraftClient client) {
        try {
            UUID uuid = Sessions.selfUuid();
            ClientPlayNetworkHandler handler = client.getNetworkHandler();
            if (uuid == null || handler == null) {
                return -1;
            }
            PlayerListEntry entry = handler.getPlayerListEntry(uuid);
            return entry == null ? -1 : entry.getLatency();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void drawPingBars(DrawContext ctx, int x, int y, int color) {
        int[] heights = {3, 5, 7, 9};
        for (int i = 0; i < heights.length; i++) {
            int h = heights[i];
            ctx.fill(x + i * 4, y + 9 - h, x + i * 4 + 3, y + 9, color);
        }
    }

    /* ------------------------------ keystrokes ---------------------- */

    private static final int KEY_SIZE = 16;
    private static final int KEY_GAP = 2;

    private static void renderKeystrokes(DrawContext ctx, TextRenderer tr, MinecraftClient client,
                                         boolean demo, int x, int y) {
        AzyronConfigModel config = AzyronConfig.get();
        int width = KEY_SIZE * 3 + KEY_GAP * 2 + 8;
        int height = KEY_SIZE * 2 + KEY_GAP + 14 + KEY_GAP + 12 + 8;
        RenderUtil.fillHudPanel(ctx, x, y, width, height, config.backgroundOpacity);

        int cy = y + 4;
        int cx = x + 4;
        // Row 0 — W
        drawKey(ctx, tr, cx + KEY_SIZE + KEY_GAP, cy, KEY_SIZE, KEY_SIZE, "W",
                demo ? 0.35F : KeystrokesTracker.flash(KeystrokesTracker.KEY_W),
                demo || KeystrokesTracker.isDown(KeystrokesTracker.KEY_W));
        // Row 1 — A S D
        cy += KEY_SIZE + KEY_GAP;
        drawKey(ctx, tr, cx, cy, KEY_SIZE, KEY_SIZE, "A",
                demo ? 0F : KeystrokesTracker.flash(KeystrokesTracker.KEY_A),
                demo || KeystrokesTracker.isDown(KeystrokesTracker.KEY_A));
        drawKey(ctx, tr, cx + KEY_SIZE + KEY_GAP, cy, KEY_SIZE, KEY_SIZE, "S",
                demo ? 0.6F : KeystrokesTracker.flash(KeystrokesTracker.KEY_S),
                demo || KeystrokesTracker.isDown(KeystrokesTracker.KEY_S));
        drawKey(ctx, tr, cx + (KEY_SIZE + KEY_GAP) * 2, cy, KEY_SIZE, KEY_SIZE, "D",
                demo ? 0F : KeystrokesTracker.flash(KeystrokesTracker.KEY_D),
                demo || KeystrokesTracker.isDown(KeystrokesTracker.KEY_D));
        // Row 2 — LMB / RMB
        cy += KEY_SIZE + KEY_GAP;
        int barWidth = (width - 8 - KEY_GAP) / 2;
                boolean leftDown = demo || CpsManager.isLeftDown();
                boolean rightDown = !demo && CpsManager.isRightDown();
        drawMouseKey(ctx, tr, cx, cy, barWidth, 14, "LMB", leftDown);
        drawMouseKey(ctx, tr, cx + barWidth + KEY_GAP, cy, barWidth, 14, "RMB", rightDown);
        // Row 3 — SPACE
        cy += 14 + KEY_GAP;
        drawKey(ctx, tr, cx, cy, width - 8, 12, "SPACE",
                demo ? 0F : KeystrokesTracker.flash(KeystrokesTracker.KEY_SPACE),
                demo || KeystrokesTracker.isDown(KeystrokesTracker.KEY_SPACE));
    }

    private static void drawKey(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                                String label, float flash, boolean held) {
        int base = 0xFF191230;
        int active = 0xFF2E4B8F;
        int border = 0xFF3D2B66;
        float t = held ? Math.max(0.35F, flash) : flash * 0.85F;
        int fill = AzyronConstants.lerpColor(base, active, Math.max(0F, Math.min(1F, t)));
        RenderUtil.fillRoundedRect(ctx, x, y, w, h, 3, fill);
        RenderUtil.drawRoundedBorder(ctx, x, y, w, h, 3, border);
        int tw = tr.getWidth(label);
        Texts.draw(ctx, tr, label, x + (w - tw) / 2, y + (h - 9) / 2 + 1, 0xFFF1F5F9, false);
    }

    private static void drawMouseKey(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                                     String label, boolean held) {
        drawKey(ctx, tr, x, y, w, h, label, held ? 1F : 0F, held);
    }

    /* --------------------------------- armor ------------------------ */

    private static void renderArmor(DrawContext ctx, TextRenderer tr, MinecraftClient client,
                                    boolean demo, int x, int y) {
        AzyronConfigModel config = AzyronConfig.get();
        int width = 4 * 22 + 6;
        int height = 34;
        RenderUtil.fillHudPanel(ctx, x, y, width, height, config.backgroundOpacity);

        int slot = 0;
        if (demo) {
            int[] demoDurability = {100, 78, 55, 24};
            for (int i = 0; i < 4; i++) {
                drawArmorSlot(ctx, x + 4 + i * 22, y + 4, i, demoDurability[i], true);
                slot++;
            }
            return;
        }
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        try {
            net.minecraft.entity.EquipmentSlot[] armorSlots = {
                    net.minecraft.entity.EquipmentSlot.FEET,
                    net.minecraft.entity.EquipmentSlot.LEGS,
                    net.minecraft.entity.EquipmentSlot.CHEST,
                    net.minecraft.entity.EquipmentSlot.HEAD};
            for (net.minecraft.entity.EquipmentSlot armorSlot : armorSlots) {
                ItemStack stack = player.getEquippedStack(armorSlot);
                if (stack == null || stack.isEmpty()) {
                    slot++;
                    continue;
                }
                int maxDamage = stack.getMaxDamage();
                int durability;
                if (maxDamage <= 0) {
                    durability = 100;
                } else {
                    durability = Math.round(100.0F * (maxDamage - stack.getDamage()) / (float) maxDamage);
                }
                drawArmorSlot(ctx, x + 4 + slot * 22, y + 4, slot, durability, false);
                try {
                    ctx.drawItem(stack, x + 4 + slot * 22 + 1, y + 4);
                } catch (Throwable ignored) {
                    // Item rendering unavailable — the durability bar still shows.
                }
                slot++;
            }
        } catch (Throwable t) {
            // Armor API unavailable — draw nothing rather than crash.
        }
    }

    private static void drawArmorSlot(DrawContext ctx, int x, int y, int index, int durability, boolean demo) {
        if (demo) {
            // Placeholder armor-colored ingot icon.
            int[] colors = {0xFFE5E4E2, 0xFFD4AF37, 0xFFA8A9AD, 0xFFCD7F32};
            ctx.fill(x + 3, y + 2, x + 15, y + 9, colors[index % colors.length]);
            ctx.fill(x + 5, y + 9, x + 13, y + 14, colors[(index + 1) % colors.length]);
        }
        // Durability bar.
        int clamped = Math.max(0, Math.min(100, durability));
        int barWidth = 18;
        ctx.fill(x + 1, y + 17, x + 1 + barWidth, y + 19, 0xFF1F2937);
        int fill = clamped > 60 ? AzyronConstants.COLOR_GOOD : clamped > 25 ? AzyronConstants.COLOR_WARN : AzyronConstants.COLOR_BAD;
        ctx.fill(x + 1, y + 17, x + 1 + Math.round(barWidth * (clamped / 100.0F)), y + 19, fill);
        // Draw the actual item stack if we had one — demo path uses placeholders.
    }

    /* -------------------------------- potions ----------------------- */

    private static final int[] EFFECT_COLORS = {
            0xFF38BDF8, 0xFF4ADE80, 0xFFFACC15, 0xFFF472B6, 0xFFA78BFA, 0xFFFB923C
    };

    private static void renderPotions(DrawContext ctx, TextRenderer tr, MinecraftClient client,
                                      boolean demo, int x, int y) {
        AzyronConfigModel config = AzyronConfig.get();

        String[] names;
        int[] levels;
        String[] times;
        if (demo) {
            names = new String[]{"Speed", "Strength", "Haste"};
            levels = new int[]{2, 1, 1};
            times = new String[]{"2:14", "0:42", "8:30"};
        } else {
            PlayerEntity player = client.player;
            if (player == null) {
                return;
            }
            try {
                Collection<StatusEffectInstance> effects = player.getStatusEffects();
                if (effects == null || effects.isEmpty()) {
                    return;
                }
                int n = Math.min(6, effects.size());
                names = new String[n];
                levels = new int[n];
                times = new String[n];
                int i = 0;
                for (StatusEffectInstance effect : effects) {
                    if (i >= n) {
                        break;
                    }
                    names[i] = PotionNames.displayName(effect);
                    levels[i] = effect.getAmplifier() + 1;
                    times[i] = FormatUtils.formatDurationTicks(effect.getDuration());
                    i++;
                }
            } catch (Throwable t) {
                return;
            }
        }

        int rowHeight = 13;
        int width = 148;
        int height = names.length * rowHeight + 8;
        RenderUtil.fillHudPanel(ctx, x, y, width, height, config.backgroundOpacity);

        for (int i = 0; i < names.length; i++) {
            int rowY = y + 4 + i * rowHeight;
            int color = EFFECT_COLORS[i % EFFECT_COLORS.length];
            RenderUtil.fillRoundedRect(ctx, x + 4, rowY, 9, 9, 2, color);
            String label = names[i] + " " + FormatUtils.roman(Math.max(0, Math.min(9, levels[i] - 1)))
                    + "  " + times[i];
            Texts.draw(ctx, tr, label, x + 17, rowY, 0xFFE2E8F0, true);
        }
    }

    /** Combined scale for an element: global hudScale times the per-element scale. */
    public static double effectiveScale(String id) {
        AzyronConfigModel config = AzyronConfig.get();
        AzyronConfigModel.HudPosition pos = config.positionOf(id);
        return config.hudScale * pos.effectiveScale();
    }

    /** Measure an element with the combined scale applied (used for hit-testing and clamping). */
    public static int[] measureScaled(String id, boolean demo) {
        int[] base = measure(id, demo);
        double scale = effectiveScale(id);
        return new int[]{Math.max(1, (int) Math.round(base[0] * scale)),
                Math.max(1, (int) Math.round(base[1] * scale))};
    }

    /* ================================================================ */
    /* Measuring (used by the HUD editor for hit-testing)               */
    /* ================================================================ */

    public static int[] measure(String id, boolean demo) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client == null ? null : ClientRefs.textRenderer(client);
        int lineHeight = 15;
        switch (id) {
            case FPS:
                return new int[]{tr == null ? 70 : tr.getWidth("FPS: 144") + 10, lineHeight};
            case CPS:
                return new int[]{tr == null ? 90 : tr.getWidth("CPS 9 | 3") + 10, lineHeight};
            case PING:
                return new int[]{tr == null ? 88 : tr.getWidth("Ping: 63 ms") + 24, lineHeight};
            case COORDS:
                return new int[]{tr == null ? 130 : tr.getWidth("XYZ: 128 / 64 / -230") + 10, lineHeight};
            case KEYSTROKES:
                return new int[]{KEY_SIZE * 3 + KEY_GAP * 2 + 8, KEY_SIZE * 2 + KEY_GAP + 14 + KEY_GAP + 12 + 8};
            case ARMOR:
                return new int[]{4 * 22 + 8, 34};
            case POTIONS:
                return new int[]{148, (demo ? 3 : currentPotionCount()) * 13 + 8};
            case SPRINT:
                return new int[]{tr == null ? 70 : tr.getWidth("Sprint: ON") + 10, lineHeight};
            case ZOOM:
                return new int[]{tr == null ? 76 : tr.getWidth("ZOOM x12.0") + 10, lineHeight};
            case DIRECTION:
                return new int[]{tr == null ? 96 : tr.getWidth("Facing: NW") + 16, lineHeight};
            case CLOCK:
                return new int[]{tr == null ? 104 : tr.getWidth("Day 400  •  23:59") + 16, lineHeight};
            case BIOME:
                return new int[]{tr == null ? 140 : tr.getWidth("Biome: Frozen Ocean") + 16, lineHeight};
            case STATUS:
                return new int[]{tr == null ? 150 : tr.getWidth("HP 20/20   Food 20") + 16, lineHeight};
            default:
                return new int[]{60, lineHeight};
        }
    }

    private static int currentPotionCount() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;
            if (player == null) {
                return 0;
            }
            Collection<StatusEffectInstance> effects = player.getStatusEffects();
            return effects == null ? 0 : Math.min(6, effects.size());
        } catch (Throwable t) {
            return 0;
        }
    }
}
