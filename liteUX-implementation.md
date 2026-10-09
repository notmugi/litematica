# liteUX implementation and release checklist

> **Current: liteux.2.** The screen map below describes the initial implementation.
> Read [liteUX-test-checklist.md](liteUX-test-checklist.md) for the consolidated
> screen map and focused manual tests. No live-client testing was run for liteux.2.

Upstream: `sakura-ryoko/litematica`, branch `LTS/26.2`, commit `5bd793d44ad5f047cad3f4b389e8dd0368b6e5a8`.
Minecraft 26.2, Java 25, malilib 0.29.6. Original design: `liteUX.md`.

## Implemented

- **Home:** four workflow cards (place, save, build, files), loaded/placed counts, selected placement/layers, tool toggle and explicit nine-mode picker. Advanced directory retains original destinations, including opt-in projects. Small layouts become a single column. Native controls have a muted dark theme, visible focus and descriptive tooltips.
- **First visit/help:** three short pages on first Home visit; permanent dismissal, reopen from Help, actual configured shortcut/tool instead of hardcoded instructions. Does not interrupt world loading.
- **Place:** shared existing browser/conversion pipeline → explicit Here / Coordinates / Load only choice → adjust exact created preview. Nothing is registered before committing. Coordinates are validated; back navigation preserves drafts and cannot re-commit the same flow. Nudges/rotation/mirror respect locks; materials, global layers, full placement settings and tool movement remain accessible. Explicit visibility repair when global switches hide previews. Warnings survive closing the screen.
- **Save/build hubs:** direct routes to area selection/editor/save, materials, verifier, global layers, Easy Place and rendering. Build assist uses the named selected preview's material list, falling back to the last list only without a selection. Project browser restrictions remain intact.
- **Settings:** Getting started, Building, Paste & commands, Files, Advanced/server, existing rendering/info/colors/hotkeys and All. Search matches descriptions and internal names as well as readable labels. Hotkeys grouped by menus/building/selection/expert. Group reset is confirmed. Native editors and keybind semantics retained; narrow layouts reserve space for editor/reset controls and expose full labels in tooltips. All 248 registered options/hotkeys remain reachable.
- **HUD:** compact tool readiness/selected preview/layer summary without holding the tool; remains visible when rendering is off so the cause is explained. Uses existing **Tool HUD** alignment/scale/offsets. Optional cached missing-count summary when material HUD is enabled. F1 and open screens hide the HUD; original detailed tool HUD remains.
- **Safety:** cancel-default, single-use contextual confirmations for shared paste, fill/replace, delete, world move, bulk/directional schematic edits, dirty unload/reload. Captures world/player/connection, target identity/transforms, settings, layers and existing schematic revision; stale approvals are rejected. Repeated held triggers must release before rearming. Long confirmations wrap/page within the viewport. Combined project delete-and-paste is approved before deletion, not midway through it. Export-to-command-files cannot first delete project world contents. Interrupted moves retain copied schematic data rather than discarding recovery material.
- **Language:** plain-English English display labels without changing translation/config keys or format arguments. All new workflow strings localized through `en_us.json`; other locales are preserved and can translate the new fallback strings separately.
- **Accessibility:** task-screen Tab/Shift+Tab navigation, Enter/Space activation, visible focus, narrator feedback using Minecraft's existing narrator. Coordinate fields retain normal editing keys and never submit on Enter while editing. No emoji/font dependency. No new runtime dependency.

## Deliberate corrections to the original plan

The original investigation was too shallow. These are not regressions or missing features:

1. Existing Load already supported auto-placement. Kept that legacy checkbox behavior; the new guided flow offers an explicit staged alternative, not another conversion implementation.
2. File deletion and project actions already had confirmations. Preserved file confirmation; project confirmation is now at the shared operation boundary rather than doubled at GUI and engine layers.
3. Rebuild **edits the loaded schematic**, not world mismatches. It remains named Edit schematic. Verifier checks/ignores mismatches; no invented bulk-fix operation was added.
4. Did **not** disable `executeRequireHoldingTool`, overwrite vanilla stick textures, spawn items, hijack Ctrl, change original keybinds or reset user preferences. The tool can be any configured item, including components; GUI controls do not require holding it.
5. A HUD cannot provide mouse-hover tooltips during normal gameplay. It shows the actual menu binding instead. Original Info HUD and Tool HUD settings are distinct; the new summary uses Tool HUD controls.
6. A preview is never labelled as a paste. World changes remain separate Creative operations.
7. No permanent "don't ask again" on the destructive dialog itself. Explicit safety preferences in Settings allow an informed opt-out without an accidental checkbox bypass.
8. No fake undo, transactions, or blanket promise to preserve chest contents. Paste semantics differ across direct, commands and Servux.
9. Kept all expert editors instead of rewriting every malilib widget. The focused task screens are new; advanced editors retain their existing capabilities. At narrow sizes the browser dedicates space to filenames; metadata remains in the next step and the existing side preview returns on larger layouts.
10. Skipped redundant persistent onboarding checkboxes and a "give tool" action. The welcome, live status and workflow steps explain progress without more saved state or changing inventories. Existing files remain file-backed; there is no new project database.

## Build and checks

From the repository root:

```sh
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
mkdir -p .work
export TMPDIR="$PWD/.work"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$PWD/.work"
./gradlew build --offline
python3 ux-check-config.py
python3 ux-check-home.py
python3 tests/check-ux-locale.py
./tests/quick-place/check.sh
sh tests/check-ux-safety.sh
```

`--offline` assumes the initial dependency download has completed. Use `./gradlew build` on a fresh machine. The config check compares declarations against upstream `HEAD`; do not change that baseline without updating the test.

Live graphical smoke check:

```sh
tests/client-smoke/run.sh
```

This opens an isolated development Minecraft client in `build/ux-smoke/run`, not your launcher installation. Screenshots and explicit PASS/FAIL marker: `build/ux-smoke/screenshots/`. Tests both 320×240 and 480×360 GUI-scaled viewports. Checks screen construction/rendering, button bounds, Tab not executing actions, and default Enter cancelling confirmation. It does **not** enter or modify a player world. The smoke mod is a separate source set and is excluded from the production jar.

Normal artifact: `build/libs/litematica-fabric-26.2-0.28.8-liteux.2.jar` (use the non-sources jar). Replace the original Litematica jar; **do not install both**. Requires matching Minecraft and malilib. No installation into your launcher was performed.

## Release gate: gameplay tests still required

Compilation, source/logic checks and rendered empty-state screens are not proof of flawless multiplayer behavior. Test on disposable worlds before distributing as stable:

1. Fresh config and copied existing config: Home/help, dismissal after restart, original hotkeys and disabled safety preferences preserved.
2. Load each supported schematic format; cancel/back/load-only/create; verify exactly one preview, no world changes. Test duplicate names, large files, corrupt files, missing thumbnails, custom schematic directory and unreadable files.
3. Coordinate validation, negative positions, world border/build limits, resize with typed values; placement and individual axis locks. Layer slider/global effects and visibility restore.
4. Multiple loaded schematics/placements: choose different selections between screens; material list and transformations must refer to the named target, not a previous list/selection.
5. Save simple/multi-box selection, entities/NBT, overwrite cancellation, export/import, description/preview editing. Existing persistence formats unchanged.
6. Creative world operations on a backed-up test world: Cancel/Enter/Escape produces **zero** changes; Tab then Enter runs exactly once. Test paste via direct, commands and Servux; fill/replace/delete/grid, move, project delete-and-paste, command export. Permissions remain enforced by existing paths/server.
7. Change selection, transform, settings, gamemode, held tool, project version, dimension or connection while confirming: reject stale action. Hold input through confirm/cancel, then release; ensure no repeat or permanently latched input. Interrupt queued move/project stages; retain recovery schematic on interrupted move.
8. Edit schematic bulk/directional operations, dirty unload/reload; cancellation preserves edits. Loaded schematic edits affect all placements of the same object and require explicit saving.
9. Config search by description/internal key; keyboard capture and group reset including hidden results; pending text persists when switching groups. Test all categories, long translations and GUI scales, narrator, hover text, mouse and keyboard focus.
10. HUD: F1/open screen hiding, renderer-off explanation, custom tool components, layer axes/ranges, material result freshness, coexistence with verifier/material overlays and existing HUD offsets.

## Known boundaries

- World operations remain asynchronous and nontransactional. Failure/context change after a destructive stage cannot restore blocks already changed. Servux work already sent cannot be recalled.
- Context guards do not freeze other players or the world. Existing schematic revision is a millisecond metadata timestamp, not a new monotonic revision system.
- Only guided/task screens gained full new keyboard focus handling. Advanced malilib editors retain their native accessibility behavior.
- Screens target Minecraft's usual minimum 320×240 scaled viewport; mods forcing smaller viewports need additional scrolling/layout work.
- English copy is updated; other locales need translation review. Advanced screens were retained, not subjected to an exhaustive tooltip/narration rewrite.
- No performance guarantees for huge schematics beyond upstream behavior. HUD reads cached counts and does not scan worlds each frame.

## Dropdown follow-up (liteux.1)

Removed the category-only hover patch (incorrect argument order and guessed
height). A scoped mixin now closes the shared malilib dropdown before rendering
when the cursor leaves its actual visible bounds, including filtered rows and
the filter field. Covers the inherited mod selector too. Resets filter/drag state
without selecting anything. Applies only in Litematica screens; the stock malilib
jar and other mods' screens remain unchanged. Live regression exercises both
selectors, four exit directions, filter/reopen and category selection at two scales.
