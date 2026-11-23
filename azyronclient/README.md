<div align="center">

![AZYRON CLIENT](docs/azyron-client-hero.png)

# AZYRON CLIENT V2

### PERFORMANCE. SMOOTHNESS. STYLE.

The V2 upgrade of **AZYRON CLIENT** for **Minecraft 1.21.11 (Fabric)** —
FPS Booster 2.0, a rebuilt HUD suite, config profiles, a mod manager and a
screenshot manager. Built for desktop **and** for PojavLauncher on Android / iOS.

**Made by JIGNESH GAMING**

[![AZYRON CLIENT build](https://github.com/blazemc125/AzyronClient/actions/workflows/azyron-client.yml/badge.svg)](https://github.com/blazemc125/AzyronClient/actions/workflows/azyron-client.yml)

</div>

---

## ✨ Features

| Category | What you get |
|---|---|
| **Performance** | One-tap profiles — `POTATO`, `BALANCED`, `SMOOTH` — that retune graphics, particles, render distance, entity distance, mipmap, biomes and FPS cap. **Dynamic FPS** drops the cap while the window is unfocused (restored on refocus). |
| **Menus** | Aurora-themed main menu (replaces the vanilla title), AZYRON **Mods Menu** in-game (default: `Right Shift`), data-driven **Settings** screen (every option applies live), searchable module list with categories. |
| **HUD** | FPS counter (traffic-light colors), CPS (rolling 1 s window), ping, coordinates, **Keystrokes** (WASD + space + LMB/RMB with press-flash), armor durability, potion effects (name + level + timer), sprint + zoom indicators. All positions draggable in the **HUD Editor** (`H`), persisted on disk. |
| **Gameplay** | **Toggle Sprint** (forced sprint with hunger rules), **Zoom** (`C`, eased or snappy), **Custom Crosshair** (DOT / PLUS / CLASSIC / CIRCLE / T-SHAPE, color + size, hides vanilla). |
| **Cosmetics** | Client-side particle auras — End Sparkle, Soul Flame, Ember Trail, Snowflake. Equip instantly, visible to you (server sync is future work). |
| **V2 HUD modules** | Everything from V1 plus **Direction**, **Clock**, **Biome** and **Player Status** — all draggable, toggleable and recolorable in the HUD Editor. |
| **FPS Booster V2** | Dedicated page: Potato / Balanced / Smooth bundles, one-click optimize, render & simulation distance presets, particle controls, entity render-distance scaling, background (unfocused) FPS cap. Live FPS readout. |
| **Config Profiles** | Save / load / rename / delete whole client setups as JSON, export/import them anywhere, built-in Potato/Balanced/Smooth baselines. Settings auto-save as before. |
| **Mod Manager** | Every loaded Fabric mod with real metadata — search, client/universal filter, dependency & compatibility analysis (missing / version / conflicts), authors, license, environment, mods-folder shortcut. |
| **Screenshot Manager** | In-game list of everything in `screenshots/` — dimensions (read via NativeImage), size, date — one click opens the real file or the folder. |
| **Quick Settings** | Six tiles: Graphics, Video, Controls, HUD Settings, FPS Settings, Client Settings. |
| **Update Checker** | Checks the official GitHub release off-thread; shows current vs latest; opens the release page — never downloads or installs anything by itself. |
| **Branding** | AZYRON premium startup screen (black & white, real reload progress), per-glyph gradient logo, profile chip, Discord panel, "V2" edition in the footer. |

Default keys: `Right Shift` Mods Menu · `C` Zoom (hold) · `R` Toggle sprint · `H` HUD editor · `Esc` back.

---

## 📦 Install

### Desktop (Windows / Linux / macOS)

1. Install [Fabric Loader](https://fabricmc.net/use/) for **Minecraft 1.21.11** and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download `azyronclient-2.0.0.jar` (from **Releases** or the CI artifact) and drop it into your `mods/` folder.
3. Launch the Fabric profile — the AZYRON main menu replaces the vanilla title screen.

### Android / iOS (PojavLauncher)

See **[docs/INSTALL-POJAVLAUNCHER.md](docs/INSTALL-POJAVLAUNCHER.md)** — full walkthrough for PojavLauncher with the `.mrpack` modpack or the plain `.jar`.

### Modpack (`.mrpack`)

`azyron-client-2.0.0.mrpack` is a ready-made Modrinth pack: **AZYRON CLIENT + Fabric API, Sodium, Lithium, FerriteCore, EntityCulling, MoreCulling, Dynamic FPS**, pinned for Minecraft 1.21.11. Import it into any Modrinth-compatible launcher (Prism, Modrinth App, PojavLauncher's Modrinth browser).

---

## 🛠️ Build from source

Requires **JDK 21** and network access to Mojang / Fabric / Gradle Maven.

```bash
cd azyronclient
python3 scripts/update_pins.py   # refresh Fabric pins (optional; CI always runs it)
./gradlew build
# → build/libs/azyronclient-2.0.0.jar
```

### Verify the dependency-free core (no network needed)

```bash
cd azyronclient
javac -d build/core $(find src/main/java/com/jiggneshgaming/azyron/core -name '*.java')
javac -cp build/core -d build/core testing/CoreSelfTest.java
java -cp build/core CoreSelfTest   # 64/64 checks must pass
```

Or just run `sh scripts/verify_local.sh`. See **[TESTING-REPORT.md](TESTING-REPORT.md)** for exactly what is and isn't verified.

### Release pipeline

`.github/workflows/azyron-client.yml` — core tests → `./gradlew build` → `.mrpack` packaging → artifacts → GitHub release on tags `azyronclient-v*`.

---

## ⚙️ Configuration

Settings live in `config/azyronclient.json` (Fabric config dir). Corrupt files are backed up automatically and replaced with defaults; writes are atomic; everything applies live — no restart needed.

Module toggles, HUD positions, crosshair style/size/color, zoom strength, smoothing, unfocused-FPS cap, profile selection, cosmetics — all persist there. **Reset EVERYTHING** in Settings (two-step confirm) restores factory defaults.

## 🧩 Modules & the Mods Menu

In-game, press `Right Shift`:

* sidebar: All / Performance / HUD / Gameplay / Appearance
* search filter with keyboard focus
* live toggles (written straight to `azyronclient.json`)
* gear on supported modules → Settings

## ❓ FAQ

**Does it work with other mods?** Yes — it's a Fabric mod; it doesn't touch vanilla files. Mixins are optional (`defaultRequire: 0`), so a signature drift degrades a feature instead of crashing.

**Does it improve FPS by itself?** The AZYRON profile system retunes vanilla options and our Dynamic FPS limiter saves power when unfocused. For real GPU gains install Sodium — the `.mrpack` bundles it for you.

**Is it server-safe / allowed on servers?** AZYRON CLIENT is client-side only and makes no gameplay changes (no fly, no reach, no combat) — but server rules differ; ask before using modified clients on strict servers.

**OptiFine?** No — Fabric-only.

## 🐛 Known issues

See [TESTING-REPORT.md § Known issues](TESTING-REPORT.md#5-known-issues).

## 📄 License

MIT. AZYRON CLIENT is part of the [AzyronClient](https://github.com/blazemc125/AzyronClient) repository (a PojavLauncher fork).

*Made with 💜 by JIGNESH GAMING — join the Discord from the main menu.*
