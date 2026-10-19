# liteux for Minecraft 1.21.1

This branch ports the task-oriented liteux changes from
`mugi/liteux-ux-overhaul` onto `LTS/1.21`, targeting **Minecraft 1.21.1**.
It is no longer the classic-UI shortcut from liteux.1/liteux.2.

Build:

    JAVA_HOME=/path/to/jdk21 ./gradlew build

Install the non-sources jar from `build/libs` with Fabric and malilib 0.21.10
(the dependency's artifact name is `malilib-fabric-1.21`). The 26.2 branch uses
Java 25 instead. Builds are separate, not a universal jar.

Ported: task home/help/tool picker; Ghosts tabs; quick-place browser and position
flow; unified placement controls; settings categories/search/reset/dropdown fixes;
status HUD; world-edit/discard confirmations and guarded staged operations;
empty-selection geometry/model fixes; editor navigation and Add Box lifecycle;
Clear selection in Copy a build; saved-area browser entry points removed; and
native bottom-right Back buttons on schematic-save screens.

Version-specific adapters use Yarn, DrawContext, primitive malilib input callbacks,
legacy File-based schematic APIs and JSON placement snapshots. Minecraft's engine,
render pipeline and unrelated upstream 26.2-only features are not backported.

Validation: Gradle build, Java safety/coordinate checks, integration contracts,
locale references, real isolated client UI/dialog/dropdown smoke tests. See
`tests/client-smoke/README.md` for the optional local GLFW test workaround and
limits (no in-world save/paste/network acceptance test).
