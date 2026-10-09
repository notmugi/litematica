# Isolated live-client screen smoke test

Run `tests/client-smoke/run.sh` from a graphical desktop (Java 25).
It opens Minecraft using a **separate development game directory** at
`build/ux-smoke/run`, visits the new menus without joining a world, captures
screenshots in `build/ux-smoke/screenshots`, checks button bounds, and exits.
It never installs anything into a launcher profile or accesses player saves.

The runner checks **PASS.txt**/**FAIL.txt** after Gradle exits: the client itself
can exit cleanly after reporting a test failure. FAIL.txt contains the failure. Missing narrator
libraries and the dev account's Realms authentication warnings aren't test failures.

The smoke mod lives in a separate source set added only by the supplied init script;
it is **not part of the release jar**. It uses already-installed Fabric/malilib APIs,
not a new test framework.

This tests initialization/rendering, not actual paste/network/build semantics.
Use disposable singleplayer and multiplayer worlds for the acceptance checklist
in `liteUX-implementation.md` before treating the fork as a stable release.

## Dropdown interaction regression

`DropdownCheck` exercises the real mixin-transformed category and mod selectors
at both scales: click open, header/last-row/scrollbar hover, exit on each of four
edges, typed filter and its visible bounds, cleanup/reopen, and category selection.
Look for two `LITEUX DROPDOWN PASS` lines in the client log. These checks execute
inside the render path; they do not merely inspect a screenshot of a closed menu.
