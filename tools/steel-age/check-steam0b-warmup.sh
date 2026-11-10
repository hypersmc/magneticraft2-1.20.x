#!/usr/bin/env sh
# Compiles the actual dependency-free Java model with Java 17 language/API rules.
# Does NOT compile Forge or launch Minecraft.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT HUP INT TERM
javac --release 17 -d "$WORK" \
  "$ROOT/src/main/java/com/magneticraft2/common/systems/steam/BoilerWarmupState.java" \
  "$ROOT/tools/steel-age/BoilerWarmupStateTest.java"
java -cp "$WORK" BoilerWarmupStateTest
