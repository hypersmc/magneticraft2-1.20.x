#!/usr/bin/env sh
# Compiles the dependency-free boiler resource ledgers with Java 17 rules.
# Does NOT compile Forge or launch Minecraft.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT HUP INT TERM
javac --release 17 -d "$WORK"   "$ROOT/src/main/java/com/magneticraft2/common/systems/steam/BoilerWarmupState.java"   "$ROOT/src/main/java/com/magneticraft2/common/systems/steam/BoilerSteamState.java"   "$ROOT/tools/steel-age/BoilerWarmupStateTest.java"   "$ROOT/tools/steel-age/BoilerSteamStateTest.java"
java -cp "$WORK" BoilerWarmupStateTest
java -cp "$WORK" BoilerSteamStateTest
