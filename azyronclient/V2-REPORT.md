# AZYRON CLIENT V2 — Honest Status Report

**Date:** 2026-11-17
**Product:** AZYRON CLIENT — edition **V2**, version **2.0.0**, by **JIGNESH GAMING**
**Target:** Minecraft **1.21.11** (Fabric), Java 21
**Branch:** `arena/c6abecde-azyronclient` · **code tip `5b10283`** (any later commits are docs-only) · Tag `v1-stable-pre-v2` marks the pre-upgrade tip (`955102f`)
**Author's environment:** headless Linux sandbox. **No Minecraft runtime, no GPU, no Android device, no Fabric Maven access from here.**

> **Read this first.** "It compiles in CI" is the ceiling of what has been
> verified. Nothing in this document should be read as *"tested in-game"*.
> Every untested path is listed in §4. The honest summary: **the mod builds
> green on real Fabric/yarn-mapped Minecraft 1.21.11 in GitHub CI — it has
> never been launched on a real machine.**

---

## 1. What CI proves (and how)

Workflow `.github/workflows/azyron-client.yml`, latest green runs on the V2 branch (code tip `5b10283` → run `45129764969`; docs tip → run `45131312124`):

| Job | Result |
|---|---|
| Core self-tests (offline, Temurin 21) — `testing/CoreSelfTest.java` | ✅ PASS (runs `45129764969`, `45131312124`) |
| Build mod + modpack — `python3 scripts/update_pins.py` then `./gradlew build` (full Loom build against real yarn-mapped MC 1.21.11) | ✅ PASS (runs `45129764969`, `45131312124`) |
| Attach to draft release | ⏭ skipped (dispatch-only, never automatic) |
| Android CI (PojavLauncher packaging path) | ✅ PASS (run `45129765000`) |

The full-Loom build is the **only authoritative compile check** of the Fabric
sources. CI posts javac diagnostics as a comment on the failing commit; the
V2 branch needed five such cycles (Loader/yarn API drift: integer-literal
range, `InputUtil.isKeyPressed` signature, `ModDependency.Kind`,
`getVersionIntervals()`, `VideoOptionsScreen` ctor, removal of the
nonexistent `GraphicsOptionsScreen`).

**Artifact from the green run:** `azyron-client` (7,496,184 bytes,
`expired=false`) — a ZIP containing `azyronclient-2.0.0.jar` and
`azyron-client-2.0.0.mrpack`. It is attached to the workflow run, not to a
public release (see §6).

## 2. How to get the build

* **CI artifact (works today, needs a GitHub sign-in):**
  <https://github.com/blazemc125/AzyronClient/actions/runs/45129764969> →
  open run **45129764969** → artifacts → `azyron-client` →
  unzip → `azyronclient-2.0.0.jar` (drop into `mods/`) and/or
  `azyron-client-2.0.0.mrpack` (import into a Modrinth-compatible launcher).
  Artifacts expire (GitHub keeps them ~90 days).
* **V1 release (pre-V2, untouched):**
  <https://github.com/blazemc125/AzyronClient/releases/download/azyronclient-v1.0.0/azyronclient-1.0.0.jar>
  and
  <https://github.com/blazemc125/AzyronClient/releases/download/azyronclient-v1.0.0/azyron-client-1.0.0.mrpack>
* **V2 public download links:** **not available yet.** There is deliberately
  no V2 release — publishing requires the maintainer's explicit approval.
  When approved, dispatching the workflow creates/attaches a **draft** release
  tagged `azyronclient-v2.0.0`; nothing is ever published automatically.
* The sandbox that produced this report cannot reach the artifact storage
  (only github.com / npm / PyPI are reachable), so the ZIP itself could not
  be re-downloaded and inspected here. Its name and size are as reported by
  the GitHub API.

## 3. Feature-by-feature status (18-part V2 spec)

### Implemented and compile-verified in CI (not runtime-tested)

1. **Premium V2 UI** — electric blue (`#33CCFF`) + purple (`#7C3AED`) theme,
   rounded panels, gradient logo, monograms, animated backgrounds
   (`AzyronConstants`, `MenuBackground`, `BaseAzyronScreen`, `MenuBackground`,
   main-menu crest). Loading screen (`AzyronLoadingScreen`) drives the V2
   brand ("WELCOME TO AZYRON CLIENT V2", real reload progress).
2. **Branding** — AZYRON CLIENT **V2**, v**2.0.0**, Minecraft 1.21.11,
   **JIGNESH GAMING**; `fabric.mod.json` description, constants, README,
   loading screen, main menu, footer, config `describe()`.
3. **Advanced HUD editor** — drag any element (clamped fully on-screen on
   release), **per-element corner-drag resize with a visible handle** (new
   `HudPosition.scale`, 0.6–2.0, normalized for legacy configs), module
   visibility panel (right side), accent-color swatches (electric / amethyst
   / emerald / amber / crimson / white), one-click **Reset layout**, live
   previews of every element, automatic persistence on release.
4. **HUD modules (13 ≥ 12)** — FPS, CPS, Ping, Coordinates, Keystrokes,
   Armor, Potions, Sprint indicator, Zoom indicator, Direction, Clock,
   Biome, Player Status — all real values from the live client (or demo data
   in the editor), drawn in `HudRenderer`.
5. **FPS Booster V2 page** (`fps/AzyronFpsScreen.java`) — POTATO / BALANCED /
   SMOOTH bundles applied through yarn-typed `SimpleOption` writes
   (graphics preset, particles, view/simulation distance, max FPS, mipmaps,
   entity render-distance scaling, biome blend, vsync, AO, clouds), one-click
   "optimize", live FPS readout, unfocused-FPS cap (DynamicFpsManager). All
   option writes are guarded; an unavailable option is logged+skipped.
6. **Crosshair V2** (`crosshair/Crosshairs.java`, `MixinCrosshairHud`) —
   multiple styles, size, thickness, gap, outline, hide-vanilla toggle,
   live preview in the editor; applied via mixin, degrades gracefully.
7. **Zoom V2** — hold/toggle modes, custom zoom key (C/V/Z/X/F), smooth
   eased FOV scaling with configurable sensitivity; raw LWJGL key polling
   (yarn signature drift tolerant); fallback clamps the vanilla FOV slider if
   the FOV mixin fails — never crashes.
8. **Toggle sprint** — keybind (R), Settings row, Modules-screen toggle,
   HUD module, honors hunger/spectator/item-use rules
   (`sprint/ToggleSprint.java`).
9. **Zoom indicator, sprint indicator, keystrokes with RMB fix** — included
   in the V1 bug-fix pass and retained.
10. **Cosmetics V2** — Void / Lightning auras (`cosmetics/AuraManager.java`),
    intensity slider and low-particle mode honored; effects spawn around the
    local player, purely client-side.
11. **Screenshot manager** (`screenshots/AzyronScreenshotsScreen.java`) —
    lists the game's screenshot folder, opens containing folder / copies
    path / deletes (with confirm), grid layout.
12. **Quick Settings** (`quicksettings/`) — popover page for profile,
    zoom, sprint, dynamic FPS, crosshair cycle, animation quality.
13. **Config profiles** (`profiles/AzyronProfilesScreen.java`) — save /
    load / rename / delete named profiles; export/import as JSON files in
    `azyron-exports/`; corrupt file is backed up, never destroyed.
14. **Update checker** (`util/UpdateChecker.java`) — asynchronous GitHub
    release check, current vs latest, opens the releases page.
    **Never downloads or installs anything.**
15. **Animation quality** — `LOW / MEDIUM / HIGH` preset + global disable;
    persisted; consulted by UI animation code paths.
16. **Stability** — mixins are `required=false` with Throwable guards;
    per-feature tick isolation; reflection (`ClientRefs`) throws-free probes
    with fallbacks; atomic config writes; corrupt-config backup.
17. **Real Fabric JAR + MRPack** — produced by CI; `make_mrpack.py` pins
    Fabric API + Sodium, Lithium, FerriteCore, EntityCulling, MoreCulling,
    Dynamic FPS from Modrinth with SHA-1 verification (`--offline` packs only
    the AZYRON JAR).
18. **CI** — core tests + full Loom build + mrpack packaging + artifact
    upload; draft-release attach is dispatch-only.

### Implemented but honestly limited

* **Advanced mod manager** (`mods/AzyronModManagerScreen.java`) — lists all
  loaded mods (name, version, authors, environment, license, description),
  dependency declarations (DEPENDS / BREAKS / CONFLICTS with version-interval
  math done manually — Fabric Loader exposes no `VersionInterval.contains`),
  incompatibility warnings, and an **"enable/disable" action that fabricates
  nothing**: Fabric and Mod Menu provide no supported public API to disable a
  mod at runtime, so the button is present but disabled with an explanation,
  and dependency checks are advisory/read-only. It does not fake mod removal.
* **Cosmetics** — visible only to the local player; no server-side sync.
* **Update checker** — checks for a newer stable release only; no changelog,
  no SHA verification of (never downloaded) packages.
* **Zoom fallback** — if the FOV mixin is rejected, zoom drives the vanilla
  FOV option (visible but coarse); the HUD indicator still reflects state.
* **HUD resize** — per-element scale is stored per element; the *global*
  `hudScale` from V1 also still applies on top; effective = global × local.

### Not implemented / not verified

* **No in-game runtime verification at all** — menus, HUD draw paths,
  mixin application (splash paint, crosshair cancel, FOV hook), keystrokes
  flush, auras, item rendering in the Armor module have never run on a real
  client. Yarn-name drift was handled by guarded reflection where feasible;
  the full Loom build passing removes *compile-time* uncertainty only.
* **No PojavLauncher Android/iOS device test.** Android CI packages the app
  but cannot launch Minecraft.
* **Loading-screen / world-entry behavior** — the splash overlay injection
  and quit-to-title flow are code-inspected only.
* **Benchmarks** — no FPS measurements exist; performance claims derive from
  the bundled optimizers (Sodium & co), not from AZYRON's own code.
* **Shader / other performance-mod compatibility** — untested. Fabric-only.

## 4. Known caveats

* All compile verification happened on GitHub runners; the producing sandbox
  has no JDK/Minecraft access. `sh scripts/verify_local.sh` needs any JDK 8+
  and re-runs the core self-test offline.
* GitHub artifact download is restricted to signed-in users; links in §2 for
  artifacts are not anonymous. The V1 release assets are anonymous links.
* The draft-release job is intentionally dispatch-only; it targets a **new**
  `azyronclient-v2.0.0` draft and has never modified the V1 release.
* V1 stability: V1 source is untouched on `main`-side history; the pre-V2
  tip is tagged `v1-stable-pre-v2`.

## 5. Before V2 can be called "done"

1. Manual smoke test on desktop Fabric 1.21.11: splash → main menu → world →
   HUD (drag / resize / accents / reset) → Settings → Mods Menu → quit to
   title.
2. PojavLauncher launch on at least one Android device (Right Shift keybind
   reachability on touch keyboards).
3. Explicit approval to create the V2 draft release and attach the CI
   artifact; then a maintainer publishes manually. **Nothing publishes
   automatically.**
