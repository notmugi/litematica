# liteUX babyify — eliminate duplicate paths (liteux.2 plan)

> Implemented in liteux.2 with reuse-first corrections: the existing
> `GuiPlacementConfiguration` is the single editor, `GuiSchematicLoad` is abstract
> conversion support, Build Assist is deleted, and Ghosts is a two-tab workspace.
> Paste is only in the shared editor, layers only in Settings, and Keys only in
> Settings. See `liteUX-test-checklist.md` for the actual map and test steps.

**Rule:** one job = one screen. Never two editors for the same object. The new guided screens win; the old screens either get replaced or demoted to a single "Advanced" button that opens them *as a read-focused fallback*, never as a parallel happy path.

## The current sin list (duplicates to kill)

| # | Duplicate paths | One-way resolution |
|---|---|---|
| 1 | **QuickPlaceAdjust vs GuiPlacementConfiguration** — two editors for the same placement | Merge into ONE placement editor. QuickPlaceAdjust becomes the editor; its "More options" button is removed, and its remaining unique controls (lock toggles, coordinate lock, sub-region list, origin inputs) are folded into a second page/section of the same screen. `GuiPlacementConfiguration` is deleted from every entry point; every "Configure" button opens the merged editor. |
| 2 | **GuiQuickPlace vs GuiSchematicLoad** — two file browsers that load schematics | QuickPlace IS the browser. Legacy browser's unique bits (rename, material-list-from-file, non-litematic conversion warnings, "create placement on load" checkbox) move into QuickPlace as buttons next to a selected file (rename) or the position step. `GuiSchematicLoad` removed; Advanced menu no longer offers "Load Schematics" separately. |
| 3 | **GuiSchematicLoadedList vs GuiSchematicPlacementsList vs Build assist "Choose a ghost placement"** — three ways to pick a placement | ONE "Ghosts" screen: list of placements with select, show/hide, remove, configure (opens merged editor #1), material list. Loaded-schematics list is reachable from this screen via one tab, not a separate menu entry. |
| 4 | **Home "Save a selection" vs Area Editor vs Area Selection Browser vs Save dialog** — the save path touches four screens | ONE "Copy a build" flow: pick/create selection (browser embedded as step 1, not a separate menu), adjust boxes (the existing area editor, only reachable here), Save. The standalone Area Editor and Area Selection Browser menu entries are removed; the same screens are used inside the flow with proper Back chaining. |
| 5 | **Settings groups vs ALL tab vs Visuals/Info overlays/Colors tabs** — overlapping categories | Keep goal groups + Hotkeys. DELETE the All tab (search covers it) and fold Visuals/Info/Colors into "Building & rendering". Render layers stays a button inside that group, not a tab. One dropdown, ~5 entries, no overlap. |
| 6 | **Easy Place toggle in Home build assist AND config AND hotkey** | Keep config + hotkey (existing users). Remove the Home toggle; Build assist shows *status* ("Easy Place: ON — change in Settings") with a Settings button. Status read-only = no second control to disagree. |
| 7 | **Tool mode: picker vs Ctrl+scroll vs Home mode button** | Picker is the GUI way; scroll stays (hotkey, veterans). Home mode button opens picker. No third surface. (Already true; keep it that way.) |
| 8 | **Paste button in Build assist AND QuickPlace adjust AND execute hotkey** | Paste = one creative op with one confirm dialog (already shared). Buttons allowed in both places *only because they call the same path*; text identical. Acceptable — same way, two doorways. Optional: remove from Build assist, keep in Ghosts screen + hotkey. Decide during implementation; default keep both but identical label. |
| 9 | **GuiSchematicManager edit/export/import vs My schematics card** | Manager is already the one screen. Fine as-is; remove "Schematic Manager" wording from Advanced (single card remains). |
| 10 | **Verifier via Build assist vs M,V hotkey vs placements menu** | Verifier opens from the Ghosts screen row (per placement) only; hotkey stays. Build assist's verifier button is removed (it dupes the Ghosts row action). |

## New map after babyify (all of it)

```
Home
 ├─ Place a schematic   → pick file → here/coords/load-only → PLACEMENT EDITOR (shared, #1)
 ├─ Copy a build        → selection flow (#4)
 ├─ Ghosts              → one list (#3): select/configure/materials/verify/paste/remove
 ├─ My schematics       → existing manager (browse/edit/convert)
 └─ Settings / Keys / Help
Advanced (toggle): Task Manager, VCS projects, Task-adjacent legacy menus ONLY
```

Every removed menu entry's hotkey keeps working and opens the surviving screen.

## Implementation order (each independently shippable)

1. **Merged placement editor** (kills #1): absorb GuiPlacementConfiguration's unique widgets into QuickPlaceAdjust as an "Advanced settings" expandable section; retitle "Preview settings"; repoint every Configure entry (placements list widget, hotkey OPEN_GUI_PLACEMENT_SETTINGS, quick place) to it; delete GuiPlacementConfiguration entry points. This is the big one — most of the work.
2. **Browser unification** (#2): port rename/material-list-from-file into QuickPlace; delete GuiSchematicLoad + its Advanced entry.
3. **Ghosts screen** (#3): new combined placements+loaded list replacing three menus; verifier + paste + materials per row.
4. **Copy-a-build flow** (#4): chain browser → editor → save with parents; remove standalone menu entries.
5. **Settings consolidation** (#5): remove All tab + merge visual groups; update UxConfigCategories + ux-check-config.
6. **Single-source toggles** (#6, #10): Build assist becomes status hub only; status labels for Easy Place/rendering with one Settings button each.

## Tests to update

- ux-check-home: new routing (no Load Schematics entry, editor is QuickPlaceAdjust).
- ux-check-config: removed ALL tab + merged groups, still 248 reachable.
- Smoke screens: add merged editor + Ghosts screen at both scales.
- Quick-place integration check: "More options" gone, advanced section present.
- Manual: every old hotkey opens the surviving screen; no screen offers two paths to the same editor.

## Explicitly NOT doing

- No new settings for which path is default. One path, no preference soup.
- No merging of VCS project manager (opt-in advanced, leave alone).
- No combining My schematics with Ghosts: files vs placements are genuinely different objects.
