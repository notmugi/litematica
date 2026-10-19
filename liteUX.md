# liteUX — Litematica UX/UI Overhaul Plan

> Implementation now exists. Read [liteUX-implementation.md](liteUX-implementation.md) first:
> it records source-verified corrections to this original proposal, delivered behavior,
> build/test instructions and the remaining gameplay release gate.

**Goal:** Same mod, same features, but a normal human can use it without a 45-minute YouTube tutorial. Every change below preserves existing functionality — it reorganizes, renames, guards, and teaches. Nothing is deleted unless noted as merged/aliased.

**Repo analyzed:** sakura-ryoko/litematica `LTS/26.2`. GUI lives in `src/main/java/fi/dy/masa/litematica/gui/`, config in `config/Configs.java` (~200 options), hotkeys in `config/Hotkeys.java` (~60 binds), tool modes in `tool/ToolMode.java`. Rendering and schematic logic stay untouched.

---

## The core problem (diagnosis)

1. **Tool-gated gameplay.** Almost every action requires holding an invisible "stick" tool item, in a specific hidden "tool mode" (9 of them), cycled with Ctrl+scroll. No UI tells you this. If you're not holding the stick, nothing works and nothing says why.
2. **A main menu that's a directory tree, not a workflow.** `GuiMainMenu` is 10 same-looking buttons with no grouping, no status, no "what do I do next". Users must already know the difference between "Loaded Schematics", "Schematic Placements", "Area Selection Browser", and "Schematic Manager" (there are *four* separate GUIs for schematics).
3. **Jargon everywhere.** "Placements", "Sub-regions", "Origin", "VCS delete", "DataFixer schema", "Servux", "EasyPlace protocol v3". No tooltips explaining the workflow.
4. **~200 flat config options + ~60 hotkeys** in tabbed grids (`GuiConfigs`), sorted by internal category, not by user goal. Dangerous options (paste behaviors that can grief a world) sit next to cosmetic ones with equal visual weight.
5. **No safety bumpers.** Destructive ops (Delete mode, Paste, Rebuild "break all") are one unlabeled keybind or scroll-away from normal use, with no confirmation.
6. **No first-run experience.** You install the mod, press M, get a wall of buttons, and alt-tab to a wiki.

---

## Design principles for the implementation

- **Workflow-first, feature-second.** Organize UI around what the user is trying to do ("Build from a schematic", "Copy part of my world", "Get a shopping list"), not around internal subsystems.
- **Plain language labels with the jargon as subtitle.** e.g. "Placed Schematics (ghost previews)" — keep the old term in tooltip/config name so existing guides and muscle memory don't break.
- **Progressive disclosure.** Basic/Advanced split everywhere: the 90% use case is one click; the 200 configs still exist but behind an "Advanced" door.
- **Every destructive action gets a confirmation** with a "don't ask again" that lands in config (not a per-session flag).
- **State is always visible.** Current tool mode, loaded schematic count, active placement, layer — in one persistent HUD strip, not hidden behind holding I while holding the stick.
- **Old keybinds and config names keep working.** All renames are display-layer (`StringUtils.translate` changes + new GUI classes); config JSON keys untouched. Existing users lose nothing.

---

## Change 1 — New main hub (`GuiMainMenu` → `GuiHome`)

Replace the flat 10-button list with a status bar + 4 task cards + footer.

```
┌────────────────────────────────────────────────────────────┐
│ Litematica vX.Y.Z                    [Settings ⚙] [Keys ⌨] │
│ Status: 3 schematics loaded · 1 placed · Tool: OFF         │
├────────────────────────────────────────────────────────────┤
│  📐 Place a schematic      Ghost-preview a build in world  │
│  ✂️  Save a selection      Copy part of your world to file│
│  🧱 Build assist           Material list, easy place, etc │
│  📁 My schematics          Browse, edit, convert files    │
├────────────────────────────────────────────────────────────┤
│  Advanced: Loaded Schematics · Task Manager · Verifier ·   │
│  Projects (legacy)                                         │
└────────────────────────────────────────────────────────────┘
```

Mapping (nothing removed, just re-homed):

| Old button | New home |
|---|---|
| Load Schematics to Memory | "Place a schematic" card → step 1 |
| Schematic Placements | "Place a schematic" card → list tab |
| Loaded Schematics | Advanced footer |
| Area Editor / Area Selection Browser | "Save a selection" card |
| Schematic Manager | "My schematics" card |
| Material List | "Build assist" card (also still M+L) |
| Task Manager, Verifier, Projects | Advanced footer |
| Configuration | ⚙ top-right |
| Tool mode / Selection mode cycle buttons | Replaced by a labeled dropdown + big "Tool: ON/OFF" toggle in the status bar |

The status bar shows: tool mode (plain name), whether the stick is required & held, loaded/placed counts, current layer. It also renders as a slim in-game HUD strip (toggleable, replaces/augments the current info-overlay which requires holding the tool + pressing I).

---

## Change 2 — Guided "Place a schematic" flow (the 90% path)

Current flow requires: open Load GUI → pick file → load → open Placements GUI → create placement → configure placement in a third GUI. Replace with one screen, 3 steps, each with a "Next" button:

1. **Pick a file** — existing `GuiSchematicLoad` browser, but with preview (name, size WxHxL, author from `SchematicMetadata`, thumbnail if present) and a big friendly empty-state: "Drop .litematic files into `.minecraft/schematics/`".
2. **Place it** — on load, auto-create the placement at the player's location (this is what 95% want). Buttons: `[Place at my position] [Place at coordinates…] [Just load it]`. Existing manual placement GUI remains as "Advanced placement options" link.
3. **Adjust it** — compact panel: Move (nudge buttons + drag with tool), Rotate / Mirror (visual buttons, not keybind-only), Layer slider, Material List button. Everything in `GuiPlacementConfiguration` still reachable via "More options".

This is a new GUI (`GuiQuickPlace`) that calls the exact same `DataManager`/`SchematicPlacement` APIs — no logic rewrite.

---

## Change 3 — The stick, demystified

The tool item stays (it's load-bearing for in-world interaction), but:

- **Status HUD always shows** `Tool: AREA SELECT (hold stick)` or `Tool: OFF` — never silent.
- **`EXECUTE_REQUIRE_TOOL` default flips to OFF** for GUI-adjacent actions, and the setting is renamed in the GUI to "Require holding the tool stick for actions" with a real explanation. (Keep default ON only for in-world destructive modes: Delete/Rebuild/Paste.)
- **Give the tool item a real texture and name** ("Litematica Tool — hold to select/place") via a resource override, instead of a vanilla stick. Add a `[Give me the tool]` button in the Home status bar (creative: give item; survival: just show "crafting is just a stick, it's cosmetic").
- **Tool mode picker** replaces Ctrl+scroll roulette: a radial or simple list GUI on the existing mode-switch key showing all 9 modes with icons + one-line descriptions, creative-only modes grayed out in survival (they're already skipped by `ToolMode.cycle` — show them disabled instead of invisible). Scroll-cycling still works for veterans.

---

## Change 4 — Safety bumpers (destructive actions)

Confirmation dialog (new `GuiConfirm`, generic, reused) with "Don't ask again" checkbox writing to a config boolean. Triggers:

| Action | Guard |
|---|---|
| Paste schematic (any mode) | Confirm listing region count + NBT/entity behavior, warn if survival |
| Delete mode execute | Confirm with affected box count; require tool held (above) |
| Rebuild "break all/replace all" keybinds | These binds are **unbound by default already** — keep that, add confirm when bound & fired |
| Unloading a schematic with unsaved edits | Confirm |
| Deleting a schematic file in Schematic Manager | Confirm (currently instant) |
| Layer/verifier "mismatch fix" bulk ops | Confirm |

Also: **paste defaults review** — `PASTE_NBT_BEHAVIOR: NONE` and `PASTE_REPLACE_BEHAVIOR: NONE` are safe; document them in the confirm dialog in plain words ("Will not overwrite chest contents" etc).

---

## Change 5 — Config screen overhaul (`GuiConfigs`)

~200 options stay, but reorganized from internal-category tabs to goal-category tabs with a search box at top (malilib supports filtering — wire it to option display name + description):

- **Getting Started** (new tab, ~10 options): require-tool toggle, info HUD on/off, tool give button, render toggle, easy place on/off, language.
- **Building** (Easy Place*, pick block, printer-ish options) — each with 1-line "what this does" tooltips (many already have comment keys; write the missing ones in en_us lang).
- **Pasting & Commands** (all `PASTE_*`, `COMMAND_*`) — section header warns these change the world.
- **Rendering** (Visuals, Colors, Render Layers tabs — keep as-is, they're fine).
- **Files & Schematics** (custom dir, datafixer, lowercase names).
- **Advanced/Server** (Servux, entity sync, debug HUD, logging) — collapsed by default.
- **Hotkeys** tab: add a search box and group headers ("Menus", "Building", "Selection", "Expert — unbound by default"). Mark every unbound-by-default bind as "Advanced" so new users aren't scared by the wall.

Add per-tab "Reset to defaults" (malilib has per-option reset; add bulk).

Config JSON keys unchanged — pure GUI regrouping via a new tab→option mapping class.

---

## Change 6 — Language pass (en_us.json)

Rewrite display strings to plain English; keep keys. Examples:

| Current | New |
|---|---|
| "Schematic Placements" | "Placed Schematics (ghost previews)" |
| "Loaded Schematics" | "Loaded into memory" |
| "Area Selection" (mode) | "Select Area" |
| "Schematic Placement" (mode) | "Place/Move Schematic" |
| "Rebuild" | "Fix Mismatched Blocks" |
| "Easy Place" | "Easy Place (auto-pick correct block)" |
| "Verifier" | "Check My Build" |
| "Origin" | "Anchor point (origin)" |
| "Sub-region" | "Section of the schematic" |

Every GUI button that currently has no hover text gets a one-line tooltip. Target reading level: 12-year-old.

---

## Change 7 — First-run experience

On first launch (new config flag `firstRunDone`):

1. A one-time welcome GUI: 3 slides — "1. Press M for the menu. 2. Place a schematic with the big blue card. 3. Hold the tool stick to move things in-world." Buttons: `[Take me to Place a schematic] [Just let me in]`.
2. Optional 4-step interactive checklist in the Home status bar ("⬜ Load a schematic ⬜ Place it ⬜ Open material list ✔") that checks itself off via existing DataManager state. Dismissable forever.

---

## Change 8 — HUD / info overlay cleanup

- Default ON: slim top-left strip: tool mode · active placement name · layer X/Y · material-list missing-count (if list active). Clickable? No — hover shows the relevant hotkey. This replaces needing to hold stick + I for the same info.
- The existing full overlay (holding I) stays as "detailed mode".

---

## What NOT to touch

- `schematic/`, `world/`, `render/` (except adding the HUD strip renderer), `network/`, `scheduler/` — zero behavior changes.
- All config JSON key names and hotkey defaults — existing users' configs and guides keep working.
- malilib itself — build new GUIs with existing malilib widgets (`GuiBase`, `ButtonGeneric`, `GuiConfigsBase`). No new deps.

## Suggested implementation order

1. Language pass + tooltips (cheap, huge win, no logic risk).
2. Confirmation dialogs + destructive-action guards.
3. Home hub GUI (re-skin of existing buttons, no logic).
4. Tool status HUD strip + tool item name/texture.
5. Quick-Place 3-step flow (new GUI over existing APIs).
6. Config reorganization + search.
7. First-run wizard + checklist.

Each step is independently shippable.
