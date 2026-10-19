# Minecraft 1.21.1 liteux client regression

Run with Java 21:

    JAVA_HOME=/path/to/jdk21 sh tests/client-smoke/run.sh

On this host the bundled GLFW hangs during window creation. The system library
works; opt into it for tests only:

    JAVA_HOME=/path/to/jdk21 sh tests/client-smoke/run.sh -PsmokeGlfw=/usr/lib/libglfw.so

No launcher JVM arguments or release dependencies are changed by that override.
The dev-only test source set is not included in the release jar. It uses an
isolated game directory in `build/ux-smoke/run`, not a player instance/world.

Tests exercise actual Add Box/cancel/Enter, editor model refresh, mode switching,
Clear, zero-box areas, Simple empty geometry and JSON roundtrip, save-screen Back
placement/return, and category/mod dropdown edges/filtering/reopening/selection.
They also render home, help, mode picker, both Ghosts tabs, quick-place browser,
settings groups, layers and confirmation screens at two GUI scale settings and
save screenshots. The runner requires PASS.txt and rejects FAIL.txt; a clean
Minecraft exit by itself is not a pass.

This does not test world-edit/network behavior or interactive tool clicks in a
world. Safety approval logic and source-integration contracts have separate tests
in `tests/check-ux-safety.sh`; coordinates in `tests/quick-place/check.sh`.
