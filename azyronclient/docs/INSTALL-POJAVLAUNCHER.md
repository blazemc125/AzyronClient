# Installing AZYRON CLIENT on PojavLauncher (Android / iOS)

AZYRON CLIENT is a **Fabric mod** for **Minecraft 1.21.11**. PojavLauncher
runs Fabric profiles on Android and iOS — this guide gets AZYRON in-game.

> Requirements: a device that runs PojavLauncher with Minecraft 1.21.x
> (Java Edition), ~3 GB free RAM, and Android 8+ / iOS 15+.

---

## Option A — install the modpack (`.mrpack`, recommended)

The AZYRON modpack bundles everything: Fabric API, Sodium, Lithium,
FerriteCore, EntityCulling, MoreCulling, Dynamic FPS and AZYRON CLIENT.

1. Download **`azyron-client-1.0.0.mrpack`** from the
   [Releases](https://github.com/blazemc125/AzyronClient/releases) page to
   your device.
2. Open PojavLauncher → **Add New Game** (the **+** tile).
3. Choose **Import → Modrinth Modpack** (on some Pojav versions: the
   **Modrinth** browser) and select the `.mrpack` file.
4. Pojav downloads Minecraft 1.21.11 + Fabric + all bundled mods
   automatically (this needs internet once).
5. Launch the imported AZYRON profile. You should see the
   *"Welcome to AZYRON CLIENT"* splash, then the AZYRON main menu.

## Option B — install just the AZYRON JAR

1. Create/launch a **Fabric 1.21.11** profile in PojavLauncher once
   (this generates the game folders).
2. Download **`azyronclient-1.0.0.jar`** and **Fabric API**
   (`fabric-api-…+1.21.11.jar` from Modrinth) to your device.
3. Copy both JARs into the profile's mods folder:
   * **Android:** `Internal storage/Android/data/net.kdt.pojavlaunch.debug/files/.minecraft/mods/`
     (on Android 11+, use the system file manager or a PC via USB; Pojav also
     exposes "Browse game files" in its settings)
   * **iOS:** via the Files app — `PojavLauncher/games/.minecraft/mods/`
4. If you want the big FPS boost on mobile, also add **Sodium** (if your
   Pojav build supports it — some mobile drivers don't), **Lithium** and
   **FerriteCore** for the same MC version.
5. Launch the profile. Open **Settings → Mods Menu** with `Right Shift`
   (use an external keyboard) — or the AZYRON main menu if you use touch.

## Option C — custom "modpack-like" manual install

Same as Option B but also copy `config/azyronclient.json` from a desktop
setup if you want to migrate your HUD layout.

---

## First launch checklist

* [ ] Splash shows **"Welcome to AZYRON CLIENT"** (if the splash mixin
      applied; otherwise vanilla splash — not an error)
* [ ] AZYRON main menu appears with the aurora background and gradient logo
* [ ] FPS/keystrokes HUD visible in a world (toggle with HUD Editor, key `H`)
* [ ] `Right Shift` (external keyboard) opens the **AZYRON Mods Menu**
* [ ] Settings sliders respond immediately (no restart)

## Troubleshooting

| Symptom | Fix |
|---|---|
| Crash on launch, mentions `azyronclient` | Delete `config/azyronclient.json` (it will regenerate) or check you run MC **1.21.11** with Fabric — not Forge/NeoForge. |
| Vanilla title screen instead of AZYRON menu | Your Pojav build blocked the screen hook. The Mods Menu still works via the AZYRON buttons on the *Mods/Options* screen — or press `Right Shift`. File an issue with your PojavLauncher version. |
| Black/pink textures | Add **Sodium** — the mobile GL driver benefits most. If using Option B without Sodium, that's expected on some PowerVR/Mali GPUs. |
| HUD not visible | Press `H` (HUD Editor) and make sure elements are toggled on; or *Settings → Reset HUD layout*. |
| `Right Shift` does nothing | Some AZERTY/mobile layouts generate a different keycode — rebind in Minecraft's Controls under **AZYRON CLIENT**. |

## Performance tips for mobile

* Start with the **BALANCED** profile; switch to **POTATO** on low-end GPUs.
* Keep render distance ≤ 8 chunks, simulation distance ≤ 8.
* AZYRON's **Dynamic FPS** lowers the cap automatically when the app is
  backgrounded — on Android this massively helps battery.
* Cosmetic auras are cheap but not free — turn them off on weak devices
  (Mods Menu → Appearance → Cosmetic Aura).

---

**Still stuck?** Open an issue at
<https://github.com/blazemc125/AzyronClient/issues> with your
PojavLauncher version, device model, and `latest.log` from
`.minecraft/logs/`.
