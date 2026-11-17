# AZYRON CLIENT — test scaffolding

Two independent layers live here:

## `CoreSelfTest.java` — real unit tests (run anywhere)

Plain Java, no dependencies, no Minecraft. Exercises the dependency-free
core (`src/main/java/com/jiggneshgaming/azyron/core/**`): zoom math, CPS
tracker, formatting, HUD geometry/clamping, config validation, performance
profiles.

```bash
javac -d build/core $(find src/main/java/com/jiggneshgaming/azyron/core -name '*.java')
javac -cp build/core -d build/core testing/CoreSelfTest.java
java -cp build/core CoreSelfTest
```

Expected: `Passed: 64   Failed: 0`. CI runs this as a gate before the
Minecraft build (`.github/workflows/azyron-client.yml` → `core-tests` job).

## `stubs/` — compile-only Minecraft/Fabric stubs

The sandbox (and most lightweight CI runners without a Minecraft jar) cannot
download Mojang/Fabric artifacts. `testing/stubs/` contains minimal
source-compatible stubs of:

* `java.*` (trimmed Java SE surface the mod uses — String/Integer/Math/…)
* `net.minecraft.*` (Dynamic/DrawContext/Screen/GameOptions/… as consumed by the mod)
* `net.fabricmc.*`, `org.lwjgl.glfw.GLFW`, `org.spongepowered.asm.mixin.*`,
  `com.google.gson.*`, `org.slf4j.*`

**They are for signature smoke-checks only. They are never shipped, never
executed, and they intentionally do not implement behavior.**

### Rebuilding `build/java-stubs.jar`

```bash
# any JDK 8+ javac works; against a 1.8 bootclasspath the result is most useful
javac -bootclasspath build/empty.jar -sourcepath testing/stubs \
      -d build/stub-java-classes $(find testing/stubs -name '*.java')
python3 - <<'EOF'
import zipfile, os
with zipfile.ZipFile("build/java-stubs.jar", "w", zipfile.ZIP_DEFLATED) as z:
    for dirpath, _, files in os.walk("build/stub-java-classes"):
        for f in files:
            p = os.path.join(dirpath, f)
            z.write(p, os.path.relpath(p, "build/stub-java-classes"))
EOF
javac -bootclasspath build/java-stubs.jar -sourcepath src/main/java \
      -d build/mod-classes $(find src/main/java -name '*.java')
```

(`build/empty.jar` is a one-entry zip used as an empty boot classpath.)

### What stubs catch — and what they don't

Catches: arity mismatches, wrong types, missing imports, cross-file API
contradictions, bad overrides.

Does **not** catch: yarn renames in the real mappings, runtime behavior,
mixin application. See `TESTING-REPORT.md` before shipping.

Rule of thumb: any Minecraft integration whose yarn name drifts is written
against reflection with a fallback (`util/ClientRefs`, `util/ReflectionUtil`,
`util/ScreenNav`, `util/Links`, `util/Sessions`, `util/PotionNames`), so a
version bump degrades features instead of crashing.
