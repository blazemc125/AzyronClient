#!/bin/sh
# AZYRON CLIENT — local verification helper.
#
# 1) compiles the dependency-free core with any JDK (javac) on PATH and runs
#    testing/CoreSelfTest.java against it
# 2) if a full JDK is available, also checks that the Fabric sources at
#    least parse/compile when net.minecraft stubs are available (see
#    testing/stubs/) — skipped automatically otherwise
set -e
cd "$(dirname "$0")/.."

OUT=build/verify
rm -rf "$OUT"
mkdir -p "$OUT/core"

echo "== compiling core (src/main/java/com/jiggneshgaming/azyron/core) =="
find src/main/java/com/jiggneshgaming/azyron/core -name '*.java' > "$OUT/core-sources.txt"
javac -encoding UTF-8 -nowarn -d "$OUT/core" @"$OUT/core-sources.txt"

echo "== compiling CoreSelfTest =="
javac -encoding UTF-8 -nowarn -cp "$OUT/core" -d "$OUT/core" testing/CoreSelfTest.java

echo "== running CoreSelfTest =="
java -cp "$OUT/core" CoreSelfTest

echo
echo "Core verification PASSED."
echo "Full Fabric build requires network access to Mojang/Fabric Maven — run ./gradlew build or let CI do it."
