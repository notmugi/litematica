#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
: "${JAVA_HOME:=/usr/lib/jvm/java-25-openjdk}"
export JAVA_HOME
mkdir -p .work
export TMPDIR="$PWD/.work"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$PWD/.work"
./gradlew compileJava --offline
mkdir -p build/ux-safety-check
"$JAVA_HOME/bin/javac" -cp build/classes/java/main -d build/ux-safety-check tests/UxSafetyCheck.java
"$JAVA_HOME/bin/java" -ea -cp build/classes/java/main:build/ux-safety-check UxSafetyCheck
"$JAVA_HOME/bin/javac" -cp build/classes/java/main -d build/ux-safety-check tests/safety/CombinedApprovalCheck.java
"$JAVA_HOME/bin/java" -ea -cp build/classes/java/main:build/ux-safety-check CombinedApprovalCheck
python3 tests/safety/check-integration.py
