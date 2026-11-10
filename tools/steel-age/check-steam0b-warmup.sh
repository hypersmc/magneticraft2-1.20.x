#!/usr/bin/env sh
# Dependency-free Java 17 boiler accounting/safety tests.
# Does NOT compile Forge or launch Minecraft.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT HUP INT TERM
javac --release 17 -d "$WORK" \
  "$ROOT/src/main/java/com/magneticraft2/common/systems/steam/BoilerWarmupState.java" \
  "$ROOT/src/main/java/com/magneticraft2/common/systems/steam/BoilerSteamState.java" \
  "$ROOT/src/main/java/com/magneticraft2/common/systems/steam/BoilerSafetyLogic.java" \
  "$ROOT/src/main/java/com/magneticraft2/common/systems/steam/BoilerPressureMath.java" \
  "$ROOT/src/main/java/com/magneticraft2/common/systems/steam/SteamEngineMath.java" \
  "$ROOT/tools/steel-age/BoilerWarmupStateTest.java" \
  "$ROOT/tools/steel-age/BoilerSteamStateTest.java" \
  "$ROOT/tools/steel-age/BoilerSafetyLogicTest.java" \
  "$ROOT/tools/steel-age/BoilerPressureMathTest.java" \
  "$ROOT/tools/steel-age/SteamEngineMathTest.java"
java -cp "$WORK" BoilerWarmupStateTest
java -cp "$WORK" BoilerSteamStateTest
java -cp "$WORK" BoilerSafetyLogicTest
java -cp "$WORK" BoilerPressureMathTest
java -cp "$WORK" SteamEngineMathTest
