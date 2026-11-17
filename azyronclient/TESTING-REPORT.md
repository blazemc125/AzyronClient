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

## 5. Known issues

* **Zoom fallback FOV**: if the FOV mixin fails to apply, zoom temporarily
  sets the vanilla FOV slider to its minimum while held — noticeable if you
  watch the options screen, invisible in gameplay.
* **Dynamic FPS detection**: uses window focus state; AZERTY/mobile keyboards
  are unaffected, but on some Wayland compositors focus events lag by a second.
* **HUD zoom indicator** only shows while zoomed — by design.
* **Keystrokes LMB/RMB highlight** shows LMB only (RMB state tracked internally,
  display pending).
* **Reset EVERYTHING** button label refreshes on render; during the 3.5 s
  armed window the button reads "Click again!".
* **Cosmetic auras are client-side only** — other players can't see them yet.
* **PojavLauncher touch devices**: screens are built for keyboards; external
  keyboard strongly recommended for the Mods Menu.

## 6. Release checklist before calling 1.0 done

- [ ] CI green on `main` (core tests + Loom build + pack)
- [ ] Manual smoke test on desktop Fabric 1.21.11 (splash → menu → world → HUD → settings)
- [ ] Manual smoke test on PojavLauncher Android (at least one device)
- [ ] Confirm `Right Shift` binding survives AZERTY + mobile keyboards
- [ ] Tag `azyronclient-v1.0.0` and cut the GitHub release with JAR + `.mrpack`
