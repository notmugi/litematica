#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}"
mkdir -p .work
export TMPDIR="$PWD/.work"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$PWD/.work"
rm -f build/ux-smoke/screenshots/PASS.txt build/ux-smoke/screenshots/FAIL.txt
./gradlew -I tests/client-smoke/init.gradle runUxSmoke "$@"
test -f build/ux-smoke/screenshots/PASS.txt && test ! -f build/ux-smoke/screenshots/FAIL.txt
