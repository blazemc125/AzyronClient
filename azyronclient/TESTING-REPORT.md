# AZYRON CLIENT — Testing Report

**Date:** 2026-11-17
**Scope:** `azyronclient/` — Fabric mod for Minecraft 1.21.11
**Environment:** Linux sandbox (no Minecraft runtime, no Fabric Maven access)

This report is deliberately explicit about *what has been verified, how, and
what has not been verified yet*. Nothing here should be read as "device-tested".

---

## 1. What was verified locally

### 1.1 Core library — unit tested ✅

`src/main/java/com/jiggneshgaming/azyron/core/**` is dependency-free Java.
Compiled and executed in-sandbox against `testing/CoreSelfTest.java`:

> **Result: 64/64 checks passed** (ZoomMath, CpsTracker, FormatUtils,
> HudGeometry, AzyronConfigModel validation/clamps, PerformanceProfile).

Reproduce: `sh scripts/verify_local.sh` (needs any JDK 8+) or see README §Build.

### 1.2 Fabric sources — stub-compiled ✅ (signature smoke check)

The full mod source set (`util`, `config`, `modules`, `keybinds`, `cps`,
`keys`, `performance`, `sprint`, `zoom`, `cosmetics`, `hud`, `crosshair`,
`menu`, `mixin`, `AzyronClientMod`) was compiled against hand-written
**stub classes** for Minecraft / Fabric API / LWJGL / Java SE (`testing/stubs/`),
which mirror the yarn API signatures the mod targets:

> **Result: clean compile (javac 8 against stubs; all 100+ source files).**

This catches type errors, wrong method arity, missing imports and cross-file
API drift — but **not** yarn renames in the real 1.21.11 mappings. High-risk
call sites were chosen to be stable yarn names (`GameOptions.getFov()`,
`getGraphicsMode()`, `getParticles()`, `getMaxFps()`, `getCurrentFps()`,
`ClientPlayerEntity`, `PlayerListEntry.getLatency()`,
`livingEntity.getStatusEffects()`, `DrawContext.drawText/fill/drawItem/getMatrices()`).
Anything signature-drift-prone is deliberately behind reflection with a
fallback (`ClientRefs`, `ReflectionUtil`, `ScreenNav`, `Links`,
`DynamicFpsManager` option pokes, `PotionNames`).

### 1.3 Mixins are optional ✅

`azyronclient.mixins.json` sets `"injectors": { "defaultRequire": 0 }` and every
mixin body catches `Throwable`. If any yarn signature moved in 1.21.11, the
feature degrades (vanilla crosshair stays visible / FOV falls back to the
vanilla FOV slider / splash is unbranded) — **never a crash**.

### 1.4 Config persistence ✅ (by construction + core tests)

Atomic tmp-file + rename writes, corrupt-file backup, model-level clamping
(`AzyronConfigModel.validate()`), reset paths. Core validation is covered by
the self-test; the atomic-write path is plain `java.nio` with a
`AtomicMoveNotSupportedException` fallback.

## 2. What is verified only by CI

`.github/workflows/azyron-client.yml`:

1. **CoreSelfTest** on Temurin 21 (gate).
2. **`python3 scripts/update_pins.py`** — rewrites `yarn_mappings` /
   `loader_version` / `fabric_api_version` to the latest published
   Fabric artifacts for MC 1.21.11 via the Fabric Meta API / Maven metadata
   (non-fatal on network failure; committed pins remain).
3. **`./gradlew build`** — full Loom build against the real
   mojang-mapped + yarn-mapped Minecraft. **This is the only authoritative
   compile check of the Fabric sources** and it runs on GitHub's network
   (this sandbox cannot reach Mojang/Fabric Maven — see §4).
4. **`.mrpack` packaging** — `make_mrpack.py` resolves Fabric API + Sodium,
   Lithium, FerriteCore, EntityCulling, MoreCulling, Dynamic FPS from the
   Modrinth API for `1.21.11` and verifies SHA-1 hashes.

## 3. What is NOT verified yet ⚠️

* **Runtime behavior in an actual game session** — clicking through menus,
  HUD rendering, mixin application at splash/FOV time, cosmetic auras.
* **PojavLauncher Android/iOS launch** — no device in CI.
* **Yarn name drift for 1.21.11** — compiles will confirm; reflection-backed
  code paths degrade gracefully by design.
* **Performance claims** — no benchmark suite yet; "2–3× FPS" refers to
  bundled Sodium, not to AZYRON's own code.
* **Shader compatibility, other performance mods, OptiFine** — untested.
  AZYRON is Fabric-only.

## 4. Why the sandbox couldn't build the JAR

Outbound traffic from this environment is limited to `github.com`,
`api.github.com`, `codeload.github.com`, `registry.npmjs.org`, `pypi.org` and
`files.pythonhosted.org`. `maven.fabricmc.net`, `maven.minecraftforge.net`,
Mojang's CDN and Modrinth are unreachable, so Loom cannot download Minecraft.
The CI workflow compensates by building on GitHub-hosted runners.

## 5. Bugfix pass — 2026-11-17 (complete bug-fix request)

Eleven reported issues were investigated against the yarn-1.21.11+build.1 API
docs and the full source. Verification is compile-level (CI) plus code
inspection — **no in-game runtime testing was possible in this environment**.

**Root causes found and fixed:**

1. **Mods Menu disappears / can't be reopened** — the title-screen hook used a
   one-shot `titleReplaced` latch (after any quit-to-title the AZYRON menu was
   never shown again), the Right Shift handler only *opened* the menu and
   never closed it, and the `currentScreen` reflection gate made the open path
   fragile. Fixed: TitleScreen is replaced every time (quit-to-title
   included, no recursion since the AZYRON menu is not a TitleScreen),
   Right Shift now *toggles* the Mods Menu, and every per-feature tick is
   wrapped so one failing feature can't kill the tick loop.
2. **Back navigation dead-ends** — `goBack()` sent `setScreen(null)` for
   top-level screens even outside a world (blank/dead screen), and ESC on the
   AZYRON main menu did the same. Now: parent screen if any, else in-game HUD
   when a world is loaded, else the AZYRON main menu — never a blank screen.
   Vanilla singleplayer/multiplayer screens are constructed directly instead
   of reflectively (reflection failures made the buttons appear dead).
3. **Missing/invisible GUI screens** — screens built their entire content
   behind a silent `textRenderer == null` early-return, and navigation to
   vanilla screens failed silently. Fixed: one-time error log with background
   still drawn, vanilla screens constructed directly, and the screen-replace
   latch removed.
4. **KeyBinding registration errors** — registration went through reflection
   against a `(String, Type, int, String)` constructor that **does not exist**
   in 1.21.11 (removed in 1.21.9), so every AZYRON keybinding silently failed
   and the features were dead. Rewritten to the real yarn API:
   `KeyBinding.Category.create(Identifier)` +
   `new KeyBinding(String, Type, int, Category)` — compile-verified. The
   controls screen category translation key `key.category.azyronclient.main`
   was added to `en_us.json`.
5. **No dedicated touch button for the Mods Menu** — added an `AZYRON` button
   to the default Pojav control layout (`app_pojavlauncher/.../default.json`),
   publishing GLFW Right Shift (344), the same key the Mods Menu keybind uses.
   Fresh installs get it on first launch; existing layouts keep their own
   buttons (Pojav only copies the default once).
6. **Zoom / Sprint Toggle / HUD Editor broken** — all three were dead because
   of (4); additionally the HUD keystrokes panel never showed RMB state
   (`demo ? false : false`). Both fixed. Zoom keeps the FOV-mixin path and the
   FOV-slider fallback; the mixin handler now declares the exact yarn
   signature `getFov(Camera, float, boolean)`.
7. **Graphics settings NullPointerException** — the saved profile (incl. the
   graphics preset) was applied inside `onInitializeClient`, while the client
   is not ready to accept option changes (the graphics preset applies a
   resource reload; vanilla dereferences half-initialized state →
   NullPointerException). Now applied on the first client tick; the preset
   option is null-checked; profiles still apply on demand from the menu.
8. **Invalid fullscreen configuration** — Pojav wrote `fullscreen:off` into
   `options.txt` (Minecraft expects `true`/`false`). Fixed to `false`
   (`GameView.realStart()`); `GameOptionsUtils` already normalized legacy
   values.
9. **GUI rendering & initialization** — see (1)(3); the AZYRON main menu is
   re-installed on every vanilla title screen init, and screens can no longer
   silently swallow renderer lookup failures.
10. **FPS / memory** — menu background band colors are cached per window
    height (no per-frame `lerpColor`), orb glow rings 7→5, circle fills step
    2px rows, vignette 48→12 bands; DynamicFPS uses direct
    `SimpleOption<Integer>` get/set instead of reflective `getValue`;
    DynamicFPS null-guards restored paths.
11. **General** — RMB keystroke highlight fixed; sprint-toggle RMB display
    corrected; tick handlers isolated via `tickSafely`; HUD editor and any
    stray ESC paths route to the same safe-close logic; category language key
    aligned with the 1.21.11 `key.category.<namespace>.<path>` convention.

**Still unverified:** everything that requires an actual game session
(click-through, mixin application at runtime, rendering on device). Verify on
a desktop Fabric 1.21.11 instance and on PojavLauncher before publishing.

## 5b. Invisible main-menu GUI — root-cause pass (2026-11-17, commit after 5fc6e71)

**Reported:** the complete main-menu GUI was missing in-game — Singleplayer /
Multiplayer / Options / Language / Quit buttons absent, the AZYRON Mod Menu
did not open, and only the background, the (launcher) FPS counter and the
(launcher) mouse overlay were visible.

**Root cause fixed this round (three layers deep):**

1. `BaseAzyronScreen.render(...)` resolved the font via
   `ClientRefs.textRenderer()` (reflection) and, on ANY failure, returned
   **silently after drawing only the background** — leaving exactly the
   reported state: background visible, zero buttons/text, zero errors, and
   clicks landing on nothing because the button panels were never drawn.
   The fix: the text renderer is taken DIRECTLY from the `protected final
   TextRenderer textRenderer` field on `Screen` (zero reflection on the
   critical path), and every render phase (background / content / buttons /
   footer) is individually exception-isolated so no single failure can hide
   the GUI again. If the font is ever unavailable, buttons are still drawn as
   bare panels (clickable) and a red on-screen error badge names the failure
   (no silent blank UI). Failing phases log once, never spam.
2. `ScreenEvents.AFTER_INIT` used to call `setScreen()` **synchronously,
   nested inside the title screen's init event** — the new screen could end
   up half-initialized (layout/input not fully wired). The swap is now
   deferred via `client.execute(...)` and double-checks the title screen is
   still the current screen before replacing it.
3. Screen init (`buildButtons`) and button click actions are exception
   isolated; a broken layout or click handler can no longer kill the screen
   or the input pipeline.

**Also in this pass**

* The AZYRON main menu now includes **all vanilla main-menu entries**:
  Singleplayer / Multiplayer / **Options** (vanilla `OptionsScreen`) /
  **Language** (vanilla `LanguageOptionsScreen`, LanguageManager resolved
  from the client's package-private field — reflection, logged) / Quit
  Game — alongside the AZYRON entries (Mods / Cosmetics / Settings).
  Layout tightens itself on small screens.
* `HudEditorScreen` uses the direct `this.textRenderer` and isolates broken
  HUD previews; `PotionNames` uses the direct yarn API
  (`RegistryEntry.value()` + `StatusEffect.getName()`); `Links` uses
  `Util.getOperatingSystem().open(URI)`; `Sessions` resolves the UUID via
  `getUuidOrNull`/`getUuid` (String or UUID) with the authlib profile
  fallback; `ProfileManager` uses typed `SimpleOption<...>` accessors
  (view/sim distance, max FPS, mipmap, biome blend, vsync, AO, cloud mode).
* **Android Back** continues to map to GLFW ESC (`GameActivity` BACK →
  `sendKeyPress(KEYCODE_ESCAPE)` when no keyboard/controller is active);
  AZYRON screens translate ESC into `goBack()` (sub-screens → parent,
  top-level → HUD or the AZYRON main menu). Verified by code inspection of
  `GameActivity.dispatchKeyEvent`, `GameView`, `KeycodeUtils`.

**Still unverified:** on-device rendering and click-through. CI compiles
and the core self-tests pass; a real PojavLauncher/desktop session is
required before publishing. If anything still fails, the new error badge
will name the failing layer instead of showing a blank screen.

## 5c. World-entry crash — root-cause pass (2026-11-17, after e090cac)

**Reported from the on-device log:** "TextRenderer cannot be resolved"
appears repeatedly; Chunk Sections UBO repeatedly resizes; the integrated
server falls 56 ticks behind; the player joins the Singleplayer world; then
the launcher log ends with an unexpected EOF (no Java crash report).

**Root cause (confirmed in code, not a guess):**
All Minecraft-member access in the mod went through *literal-name reflection*
(`getDeclaredField("textRenderer")`, `"session"`, `"languageManager"`, ...).
Fabric ships intermediary names at runtime — `field_1772`, not
`textRenderer` — so in **every production build** those lookups throw
`NoSuchFieldException`/`NoSuchMethodException` and silently return null.
This is why the menus shipped without text/buttons (fixed in 607e935 by
direct field access on Screen) and why the HUD then logged "TextRenderer
cannot be resolved" at world entry. Worse, that lookup happened
**per element per frame** (9 lookups/frame for the HUD, each allocating an
exception and scanning the class), pure GC churn exactly when the device is
already saturated at world load.

**Fix (production-safe by construction):**
* New `azyronclient.accesswidener` opens exactly three private members:
  `MinecraftClient.textRenderer`, `MinecraftClient.session`,
  `MinecraftClient.languageManager` (read-only). Loom remaps it for
  production; no runtime name is ever guessed.
* `ClientRefs` is direct compile-time access only — zero reflection.
* `Sessions` reads `session.getUuidOrNull()` directly and **caches** the
  UUID (the ping element no longer parses anything per frame).
* `Links` uses `Util.getOperatingSystem().open(URI)` directly.
* Language screen gets `client.languageManager` directly (typed).
* ReflectionUtil deleted — the class no longer exists in the mod.
* HudEditorScreen and BaseAzyronScreen already use the direct
  `this.textRenderer` field (607e935).

**What this does NOT fix (device/pack-side, honest):**
"Chunk Sections UBO repeatedly resizes" + "server falls 56 ticks behind" is
the modpack (Sodium is bundled as *required*) streaming chunks into the
GPU-bottlenecked device at join with the BALANCED profile (Fancy graphics,
view distance 12, clouds on). The AZYRON mod contains **zero** chunk-rendering,
UBO, pipeline or world-renderer code, and no native/GL calls at all — every
draw goes through `DrawContext`. The EOF (process death with no Java crash)
is consistent with the OS killing the app under memory pressure or a native
GL failure in the pack's render stack, not a Java exception in this mod.
Device-side mitigations, in order: (1) switch the AZYRON profile chip to
POTATO (Fast graphics, view 6, minimal particles, clouds off) before joining;
(2) close background apps; (3) if it still dies at join, remove Sodium from
the instance (or install the JAR-only build) and compare — if the crash
disappears it was the pack's renderer on that GPU. No launcher crash report
was produced, so no further automatic diagnosis is possible from the log
alone.

**Verification scope (unchanged honesty):** CI compile + core self-tests +
code inspection only. No game session was run in this environment. The
access-widener descriptor strings are validated by Loom **at build time** —
if any of the three field descriptors were wrong, the CI build would fail.
A successful device session is still required before publishing.

## 6. Known issues

* **Zoom fallback FOV**: if the FOV mixin fails to apply, zoom temporarily
  sets the vanilla FOV slider to its minimum while held — noticeable if you
  watch the options screen, invisible in gameplay.
* **Dynamic FPS detection**: uses window focus state; AZERTY/mobile keyboards
  are unaffected, but on some Wayland compositors focus events lag by a second.
* **HUD zoom indicator** only shows while zoomed — by design.
* **Keystrokes LMB/RMB highlight** — both buttons now track live state
  (fixed 2026-11-17).
* **Reset EVERYTHING** button label refreshes on render; during the 3.5 s
  armed window the button reads "Click again!".
* **Cosmetic auras are client-side only** — other players can't see them yet.
* **PojavLauncher touch devices**: screens are built for keyboards; external
  keyboard strongly recommended for the Mods Menu.

## 7. Release checklist before calling 1.0 done

- [ ] CI green on `main` (core tests + Loom build + pack)
- [ ] Manual smoke test on desktop Fabric 1.21.11 (splash → menu → world → HUD → settings)
- [ ] Manual smoke test on PojavLauncher Android (at least one device)
- [ ] Confirm `Right Shift` binding survives AZERTY + mobile keyboards
- [ ] Tag `azyronclient-v1.0.0` and cut the GitHub release with JAR + `.mrpack`
