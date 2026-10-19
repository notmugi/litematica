#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}
mkdir -p build/quick-place-check
"$JAVA_HOME/bin/javac" -d build/quick-place-check \
  src/main/java/fi/dy/masa/litematica/gui/QuickPlaceCoordinates.java \
  tests/quick-place/QuickPlaceCoordinatesCheck.java
"$JAVA_HOME/bin/java" -ea -cp build/quick-place-check fi.dy.masa.litematica.gui.QuickPlaceCoordinatesCheck
python3 tests/quick-place/integration-check.py
