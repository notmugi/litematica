#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?Set JAVA_HOME to a JDK 21 installation}"
mkdir -p .work build/ux-smoke/screenshots
rm -f build/ux-smoke/screenshots/PASS.txt build/ux-smoke/screenshots/FAIL.txt
./gradlew -I tests/client-smoke/init.gradle runUxSmoke "$@"
test -f build/ux-smoke/screenshots/PASS.txt && test ! -f build/ux-smoke/screenshots/FAIL.txt
