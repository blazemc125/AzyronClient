#!/usr/bin/env python3
"""
AZYRON CLIENT — build the installable Modrinth modpack (.mrpack).

Bundles:
  * the AZYRON CLIENT JAR (client-overrides/mods — not on Modrinth itself)
  * Fabric API + recommended optimization mods, resolved live from the
    Modrinth API for the target Minecraft version and Fabric loader

Usage:
  python3 scripts/make_mrpack.py \
      --jar build/libs/azyronclient-1.0.0.jar \
      --out dist/azyron-client-1.0.0.mrpack \
      --mc 1.21.11 --version 1.0.0 \
      [--loader 0.17.2] [--fapi "0.129.0+1.21.11"]

Only the Python standard library is used. Network access to api.modrinth.com
and cdn.modrinth.com is required (CI has it; offline runs can pass --offline
to pack only the AZYRON JAR).
"""
import argparse
import hashlib
import io
import json
import os
import sys
import urllib.parse
import urllib.request
import zipfile

API = "https://api.modrinth.com/v2"
TIMEOUT = 60
USER_AGENT = "azyronclient-ci/1.0 (github.com/blazemc125/AzyronClient)"

# slug -> (project name, environment) — environment: required on client
OPTIMIZERS = [
    ("fabric-api", "required"),
    ("sodium", "required"),
    ("lithium", "required"),
    ("ferrite-core", "required"),
    ("entityculling", "optional"),
    ("moreculling", "optional"),
    ("dynamic-fps", "optional"),
]


def http_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        return json.load(response)


def http_bytes(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        return response.read()


def latest_version_file(slug, mc):
    query = urllib.parse.urlencode({
        "loaders": json.dumps(["fabric"]),
        "game_versions": json.dumps([mc]),
    })
    versions = http_json("%s/project/%s/version?%s" % (API, slug, query))
    if not versions:
        return None
    for version in versions:
        for file_info in version.get("files", []):
            if file_info.get("primary") and file_info.get("filename", "").endswith(".jar"):
                return file_info
        for file_info in version.get("files", []):
            if file_info.get("filename", "").endswith(".jar"):
                return file_info
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--mc", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--loader", default=None)
    parser.add_argument("--fapi", default=None)  # advisory only; overrides are authoritative
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--icon", default=None,
                        help="PNG shown by launchers next to the modpack name")
    args = parser.parse_args()

    if not os.path.isfile(args.jar):
        print("ERROR: JAR not found: %s" % args.jar, file=sys.stderr)
        return 1

    overrides = {}  # arcname -> bytes

    with open(args.jar, "rb") as handle:
        azyron_bytes = handle.read()
    overrides["client-overrides/mods/" + os.path.basename(args.jar)] = azyron_bytes

    if not args.offline:
        for slug, environment in OPTIMIZERS:
            try:
                file_info = latest_version_file(slug, args.mc)
                if file_info is None:
                    print("WARNING: no Fabric build of %s for Minecraft %s — skipping." % (slug, args.mc),
                          file=sys.stderr)
                    continue
                data = http_bytes(file_info["url"])
                expected_sha1 = (file_info.get("hashes") or {}).get("sha1")
                if expected_sha1:
                    actual = hashlib.sha1(data).hexdigest()
                    if actual != expected_sha1:
                        print("ERROR: sha1 mismatch for %s (%s != %s)" % (slug, actual, expected_sha1),
                              file=sys.stderr)
                        return 1
                overrides["client-overrides/mods/" + file_info["filename"]] = data
                print("Bundled %-14s %s (%d KB)" % (slug + ":", file_info["filename"], len(data) // 1024))
            except Exception as exc:  # noqa: BLE001
                print("WARNING: could not bundle %s: %s — skipping." % (slug, exc), file=sys.stderr)

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": args.version,
        "name": "AZYRON CLIENT %s" % args.version,
        "summary": "Performance. Smoothness. Style. — AZYRON CLIENT: FPS-optimization profiles, "
                   "custom menus, HUD suite, crosshair, zoom, keystrokes, CPS, ping, "
                   "toggle sprint and cosmetic auras for Minecraft %s (Fabric)." % args.mc,
        "files": [],
        "dependencies": {
            "minecraft": args.mc,
        },
    }
    if args.loader:
        index["dependencies"]["fabric-loader"] = args.loader

    if args.icon:
        if not os.path.isfile(args.icon):
            print("ERROR: icon not found: %s" % args.icon, file=sys.stderr)
            return 1
        with open(args.icon, "rb") as handle:
            overrides = dict(overrides)
            icon_bytes = handle.read()
        index["icon"] = "icon.png"
    else:
        icon_bytes = None

    out_dir = os.path.dirname(args.out) or "."
    os.makedirs(out_dir, exist_ok=True)

    with zipfile.ZipFile(args.out, "w", zipfile.ZIP_DEFLATED) as pack:
        if icon_bytes is not None:
            pack.writestr("icon.png", icon_bytes)
        pack.writestr("modrinth.index.json", json.dumps(index, indent=2, sort_keys=True))
        for arcname, data in sorted(overrides.items()):
            pack.writestr(arcname, data)

    size_kb = os.path.getsize(args.out) // 1024
    print("Wrote %s (%d KB, %d override files)" % (args.out, size_kb, len(overrides)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
