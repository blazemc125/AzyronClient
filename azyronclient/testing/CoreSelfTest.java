import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.core.cps.CpsTracker;
import com.jiggneshgaming.azyron.core.format.FormatUtils;
import com.jiggneshgaming.azyron.core.hud.HudGeometry;
import com.jiggneshgaming.azyron.core.profile.PerformanceProfile;
import com.jiggneshgaming.azyron.core.zoom.ZoomMath;

/**
 * AZYRON CLIENT — offline unit tests for the dependency-free core.
 *
 * <p>Plain Java 8, no external test framework required. Prints one line per
 * check; exits with a non-zero status if any check fails.</p>
 */
public class CoreSelfTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("AZYRON CLIENT — core self-test");
        System.out.println("==============================");

        testZoomMath();
        testCpsTracker();
        testFormatUtils();
        testHudGeometry();
        testConfigModel();
        testPerformanceProfiles();

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
        System.out.println("ALL CORE TESTS PASSED");
    }

    /* ---------------------------------------------------- zoom -- */

    private static void testZoomMath() {
        section("zoom");
        check("clamp identity", almost(2.0F, ZoomMath.clampZoom(2.0F)));
        check("clamp low", almost(ZoomMath.MIN_ZOOM, ZoomMath.clampZoom(0.1F)));
        check("clamp high", almost(ZoomMath.MAX_ZOOM, ZoomMath.clampZoom(99F)));
        check("clamp NaN", almost(ZoomMath.MIN_ZOOM, ZoomMath.clampZoom(Float.NaN)));

        check("target fov 4x", almost(17.5F, ZoomMath.targetFov(70F, 4.0F)));
        check("target fov clamped zoom", almost(70F / 12F, ZoomMath.targetFov(70F, 500F)));
        check("target fov never < 1 degree", almost(1.0F, ZoomMath.targetFov(10F, 12F)));
        check("target fov identity", almost(70F, ZoomMath.targetFov(70F, 1.0F)));

        float v = ZoomMath.lerp(70F, 17.5F, ZoomMath.SMOOTH_STEP, 0.05F);
        check("lerp moves toward target", v < 70F && v > 17.5F);
        check("lerp snaps at epsilon", almost(17.5F, ZoomMath.lerp(17.51F, 17.5F, 0.3F, 0.05F)));
        check("lerp zero fraction stays", almost(50F, ZoomMath.lerp(50F, 10F, 0.0F, 0.01F)));
    }

    /* ---------------------------------------------------- cps --- */

    private static void testCpsTracker() {
        section("cps");
        CpsTracker tracker = new CpsTracker();
        long t = 10_000L;
        for (int i = 0; i < 7; i++) {
            tracker.press(CpsTracker.LEFT, t + i * 100L);
        }
        tracker.press(CpsTracker.RIGHT, t);
        check("left cps counts presses in window", tracker.cps(CpsTracker.LEFT, t + 600) == 7);
        check("right cps counts separately", tracker.cps(CpsTracker.RIGHT, t + 600) == 1);
        // Advance beyond the window; presses should expire.
        check("presses expire after 1s", tracker.cps(CpsTracker.LEFT, t + 2000L) == 0);
        // Re-fill and confirm rolling behavior.
        tracker.press(CpsTracker.LEFT, 20_000L);
        check("rolling window after clear-ish gap", tracker.cps(CpsTracker.LEFT, 20_100L) == 1);
        tracker.clear();
        check("clear empties tracker", tracker.cps(CpsTracker.LEFT, 20_100L) == 0);
        boolean threw = false;
        try {
            tracker.press(99, 0L);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check("invalid button throws", threw);
    }

    /* ------------------------------------------------- format --- */

    private static void testFormatUtils() {
        section("format");
        check("ping quality offline", FormatUtils.pingQuality(-1) == FormatUtils.PingQuality.OFFLINE);
        check("ping quality good", FormatUtils.pingQuality(42) == FormatUtils.PingQuality.GOOD);
        check("ping quality medium", FormatUtils.pingQuality(120) == FormatUtils.PingQuality.MEDIUM);
        check("ping quality bad", FormatUtils.pingQuality(400) == FormatUtils.PingQuality.BAD);
        check("ping format unknown", "— ms".equals(FormatUtils.formatPing(-1)));
        check("ping format value", "63 ms".equals(FormatUtils.formatPing(63)));
        check("coords int", "XYZ: 10 / 63 / -7".equals(FormatUtils.formatCoordsInt(10.7, 63.9, -6.2)));
        check("duration ticks", "1:05".equals(FormatUtils.formatDurationTicks(1300)));
        check("duration zero", "0:00".equals(FormatUtils.formatDurationTicks(0)));
        check("roman level 0", "I".equals(FormatUtils.roman(0)));
        check("roman level 2", "III".equals(FormatUtils.roman(2)));
        check("roman level 9", "X".equals(FormatUtils.roman(9)));
        check("fps color high", FormatUtils.fpsColor(144) == 0xFF4ADE80);
        check("fps color low", FormatUtils.fpsColor(12) == 0xFFF87171);
    }

    /* --------------------------------------------------- hud --- */

    private static void testHudGeometry() {
        section("hud-geometry");
        int[] c = HudGeometry.clampToScreen(-50, 9999, 100, 40, 800, 600);
        check("clamp negative x", c[0] == 0);
        check("clamp overflow y", c[1] == 560);
        int[] c2 = HudGeometry.clampToScreen(700, 550, 200, 100, 800, 600);
        check("clamp right edge", c2[0] == 600);
        check("clamp bottom edge", c2[1] == 500);
        check("overlap true", HudGeometry.overlaps(0, 0, 10, 10, 5, 5, 10, 10));
        check("overlap false", !HudGeometry.overlaps(0, 0, 10, 10, 50, 50, 10, 10));
        check("clamp degenerate", HudGeometry.clamp(5, 10, 1) == 10);
    }

    /* ------------------------------------------------ config --- */

    private static void testConfigModel() {
        section("config");
        AzyronConfigModel model = new AzyronConfigModel();
        model.validate();
        check("default profile balanced", "BALANCED".equals(model.profile));
        check("default hud fps visible", model.isHudVisible("fps"));
        check("default zoom level", model.zoomLevel == 4.0D);
        check("modules map exists", model.modules != null);

        model.profile = "potato";
        model.zoomLevel = 999D;
        model.crosshairStyle = "not-a-style";
        model.hudScale = -5D;
        model.backgroundOpacity = 99D;
        model.unfocusedFps = 10_000;
        model.crosshairColor = 0x00112233;
        model.validate();
        check("profile normalized upper", "POTATO".equals(model.profile));
        check("zoom clamped", model.zoomLevel == 12.0D);
        check("crosshair fallback", "CLASSIC".equals(model.crosshairStyle));
        check("hud scale clamped", model.hudScale == 0.5D);
        check("opacity clamped", model.backgroundOpacity == 0.9D);
        check("unfocused fps clamped", model.unfocusedFps == 120);
        check("color alpha forced", (model.crosshairColor & 0xFF000000) == 0xFF000000);

        model.profile = "garbage";
        model.validate();
        check("profile fallback", "BALANCED".equals(model.profile));

        AzyronConfigModel.HudPosition pos = model.positionOf("fps");
        check("positionOf returns defaults", pos != null && pos.x >= 0);
        model.resetPositions();
        check("reset positions restores size", model.positions.size() >= 9);
    }

    /* ----------------------------------------------- profiles --- */

    private static void testPerformanceProfiles() {
        section("profiles");
        PerformanceProfile.Settings potato = PerformanceProfile.forId(PerformanceProfile.Id.POTATO);
        PerformanceProfile.Settings balanced = PerformanceProfile.forId(PerformanceProfile.Id.BALANCED);
        PerformanceProfile.Settings smooth = PerformanceProfile.forId(PerformanceProfile.Id.SMOOTH);

        check("potato fast graphics", potato.graphicsMode == PerformanceProfile.GRAPHICS_FAST);
        check("potato minimal particles", potato.particles == PerformanceProfile.PARTICLES_MINIMAL);
        check("potato view distance low", potato.viewDistance <= 8);
        check("balanced fancy graphics", balanced.graphicsMode == PerformanceProfile.GRAPHICS_FANCY);
        check("balanced view distance reasonable", balanced.viewDistance >= 10);
        check("smooth caps fps for stability", smooth.maxFps <= 120);
        check("smooth paces with vsync", smooth.vsync);
        check("profiles are distinct", potato != balanced && balanced != smooth);

        PerformanceProfile.Id next = PerformanceProfile.Id.BALANCED.next();
        check("profile cycle wraps", PerformanceProfile.Id.SMOOTH.next() == PerformanceProfile.Id.POTATO);
        check("cycle middle", next == PerformanceProfile.Id.SMOOTH);
        check("fromString junk", PerformanceProfile.Id.fromString("wat", PerformanceProfile.Id.BALANCED)
                == PerformanceProfile.Id.BALANCED);
        check("fromString case-insensitive", PerformanceProfile.Id.fromString("potato", null)
                == PerformanceProfile.Id.POTATO);
    }

    /* ------------------------------------------------- helpers --- */

    private static void section(String name) {
        System.out.println();
        System.out.println("-- " + name + " --");
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  [PASS] " + what);
        } else {
            failed++;
            System.out.println("  [FAIL] " + what);
        }
    }

    private static boolean almost(float expected, float actual) {
        return Math.abs(expected - actual) < 0.0001F;
    }
}
