#!/usr/bin/env python3
"""
AZYRON CLIENT — refresh the pinned Fabric toolchain versions in gradle.properties.

The Minecraft target (minecraft_version) is intentional and never changed here.
What IS refreshed:

  yarn_mappings      latest Yarn build for the target MC   (Fabric Meta API)
  loader_version     latest stable Fabric Loader            (Fabric Meta API)
  fabric_api_version latest Fabric API build for target MC  (Fabric Maven metadata)

Network failures are non-fatal: the script keeps the existing pins and exits
with status 0 so offline builds still use the committed values.

Usage:  python3 scripts/update_pins.py [--check-only] [--properties <path>]
  --check-only           print the resolved pins, do not rewrite the properties file
  --properties <path>    rewrite a gradle/versions/<mc>.properties file instead of
                         gradle.properties (used by the multi-version CI matrix)
"""
import json
import re
import sys
import urllib.request

META_YARN = "https://meta.fabricmc.net/v2/versions/yarn/{mc}"
META_LOADER = "https://meta.fabricmc.net/v2/versions/loader"
MAVEN_METADATA = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml"

PROPERTIES = "gradle.properties"
TIMEOUT = 30


def fetch_json(url):
    with urllib.request.urlopen(url, timeout=TIMEOUT) as response:
        return json.load(response)


def fetch_text(url):
    with urllib.request.urlopen(url, timeout=TIMEOUT) as response:
        return response.read().decode("utf-8")


def read_properties(path):
    with open(path, "r", encoding="utf-8") as handle:
        return handle.read()


def get_property(text, name):
    match = re.search(r"^%s=([^\n]*)$" % re.escape(name), text, re.MULTILINE)
    return match.group(1).strip() if match else None


def set_property(text, name, value):
    return re.sub(r"^%s=[^\n]*$" % re.escape(name), "%s=%s" % (name, value), text,
                  flags=re.MULTILINE)


def main():
    argv = sys.argv[1:]
    check_only = "--check-only" in argv
    properties_path = PROPERTIES
    if "--properties" in argv:
        index = argv.index("--properties")
        if index + 1 >= len(argv):
            print("ERROR: --properties requires a path", file=sys.stderr)
            return 1
        properties_path = argv[index + 1]
    original = read_properties(properties_path)
    mc = get_property(original, "minecraft_version")
    if not mc:
        print("ERROR: minecraft_version missing from %s" % properties_path, file=sys.stderr)
        return 1

    resolved = {}
    errors = []

    # --- yarn: latest build for the target MC ---
    try:
        yarn_rows = fetch_json(META_YARN.format(mc=mc))
        builds = [row for row in yarn_rows
                  if isinstance(row, dict) and str(row.get("version", "")).startswith(mc + "+")]
        if not builds:
            builds = yarn_rows
        builds.sort(key=lambda row: row.get("build", 0), reverse=True)
        resolved["yarn_mappings"] = builds[0]["version"]
    except Exception as exc:  # noqa: BLE001 - keep build alive, keep old pin
        errors.append("yarn: %s" % exc)

    # --- loader: latest stable ---
    try:
        loader_rows = fetch_json(META_LOADER)
        stable = [row for row in loader_rows if row.get("stable")]
        pool = stable or loader_rows
        resolved["loader_version"] = pool[0]["version"]
    except Exception as exc:  # noqa: BLE001
        errors.append("loader: %s" % exc)

    # --- fabric api: newest version whose suffix matches +<mc> ---
    try:
        metadata = fetch_text(MAVEN_METADATA)
        versions = re.findall(r"<version>([^<]+)</version>", metadata)
        candidates = [v for v in versions if v.endswith("+" + mc)]
        if not candidates:
            candidates = [v for v in versions if ("+" + mc.split(".")[0] + ".") in v]
        candidates.sort(key=fabric_api_sort_key, reverse=True)
        resolved["fabric_api_version"] = candidates[0]
    except Exception as exc:  # noqa: BLE001
        errors.append("fabric-api: %s" % exc)

    current = {name: get_property(original, name) for name in
               ("yarn_mappings", "loader_version", "fabric_api_version")}
    print("Target Minecraft:      %s" % mc)
    for name in ("yarn_mappings", "loader_version", "fabric_api_version"):
        new = resolved.get(name, current[name])
        marker = "" if new == current[name] else "  (updated from %s)" % current[name]
        print("%-18s %s%s" % (name + ":", new, marker))

    for error in errors:
        print("WARNING: could not resolve a pin (%s) — keeping committed value." % error,
              file=sys.stderr)

    if check_only:
        return 0

    updated = original
    for name, value in resolved.items():
        if value:
            updated = set_property(updated, name, value)
    if updated != original:
        with open(properties_path, "w", encoding="utf-8") as handle:
            handle.write(updated)
        print("%s updated." % properties_path)
    else:
        print("%s already up to date." % properties_path)
    return 0


def fabric_api_sort_key(version):
    base, _, suffix = version.partition("+")
    numbers = []
    for piece in base.split("."):
        digits = re.sub(r"[^0-9]", "", piece)
        numbers.append(int(digits) if digits else 0)
    while len(numbers) < 3:
        numbers.append(0)
    return tuple(numbers)


if __name__ == "__main__":
    sys.exit(main())
