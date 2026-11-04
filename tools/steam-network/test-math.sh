#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
DIR=$(mktemp -d)
trap 'rm -rf "$DIR"' EXIT HUP INT TERM
javac -d "$DIR" \
    "$ROOT/src/main/java/com/magneticraft2/common/systems/PRESSURE/SteamNetworkMath.java" \
    "$ROOT/tools/steam-network/SteamMathContract.java"
java -cp "$DIR" com.magneticraft2.common.systems.PRESSURE.SteamMathContract
