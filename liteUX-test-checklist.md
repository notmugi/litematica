# liteux.2 — consolidation and what to test

This build removes duplicate UI, rather than adding more shortcuts. No client was launched for this revision; Java compilation and focused regression checks are run instead. Please do these tests in your normal instance, with a disposable/backup world for Paste.

## New map

- **Place a schematic** → file picker → Here / Coordinates / Load only → **Preview settings**.
- **Ghosts** → Placed previews → Open → **the same Preview settings**.
- **Ghosts → Loaded schematics** is the memory/source-management tab (save/reload/unload/create another preview), not another standalone menu.
- **Copy a build** opens the existing selection editor directly; if no named selection exists, choose/create it first. Save is in that editor, not a duplicate hub.
- **My schematics** owns rename, metadata, import/export and custom material-list files.
- **Settings** has six groups. Building & rendering includes visuals, HUD and colors. Global layers has one button there. Typing a search searches all settings; clearing it returns to the selected group.
- Home's old **Build Assist**, legacy load browser, standalone loaded/placement menus, and duplicate advanced-menu links are gone. Help explains instead of offering another Place shortcut. Keys is a Settings category, not another Home button.
- **Paste / Materials / Check my build** for an existing placement live in its Preview settings. Old keyboard shortcuts still work; placement-settings shortcut always opens the same editor. Sub-region editing remains in its Advanced section.

## Highest-priority checks

1. **Same editor, every time**
   - Place a file. Note the Preview settings screen.
   - Back → Home → Ghosts → Open that preview. It must be the exact same layout and controls, not an old/new version.
   - Use your placement-settings hotkey. Same screen, even if a sub-region was selected previously.
   - Change X/Y/Z, nudge, rotate/mirror. Lock the placement or one coordinate and verify movement respects the locks.
   - Advanced → rename, region visibility, reset, entity settings, sub-region editor → Back. Return to the same Preview settings, not a second editor.

2. **No extra menus**
   - Home has Place / Copy / Ghosts / My schematics plus Settings / Help / Advanced.
   - Advanced contains tasks and enabled versioned projects, not another Load/Placements/Selection directory.
   - File picker has no Advanced browser button or auto-place checkbox.
   - Ghosts has two tabs, not buttons bouncing between standalone Loaded/Placements menus.
   - Settings dropdown has six groups; no All, Visuals, Colors, Info overlays or Render Layers duplicate entries.

3. **Ghosts and Back behavior**
   - Create two previews from the same schematic and one from another.
   - Click/select one, reopen editor, return, show/hide, remove. Operations affect the named preview only.
   - Loaded schematics: save an edited schematic, reload it, cancel dirty unload/reload, then unload. Cancelling preserves edits; unloading removes its previews, not world blocks.
   - Search each tab, resize the window and switch tabs; names/actions must stay usable and scrolling must work.
   - Creating a preview from an already-loaded source opens Preview settings too.

4. **Settings / dropdown regression**
   - Search `easyPlace`, a tooltip phrase, and a color/HUD setting while Getting started is selected. Results should come from all groups.
   - Clear search: return to Getting started, not a hidden All page.
   - Change a text setting then switch groups; it must persist. Reset group should reset only the named group, not all global search results.
   - Building & rendering → Global layers → Back returns to Settings.
   - Open both category and mod-selector dropdowns. Entries/scrollbar remain usable; leaving their visible area closes them. Filter then leave/reopen: no stale hidden filter.

5. **Copy a build**
   - One-box mode: edit corners → Save → Back returns to the editor.
   - Named/multi-box mode with no selection: create/select an area → Edit → Save.
   - Change selection mode and pick another saved area. Back must not revive the wrong selection editor or stack identical browsers.
   - Projects open: forbidden area-browser access stays disabled; project editing still works.

6. **Safety and compatibility**
   - Placement Paste is Creative-only, with one confirmation; Cancel/Escape/default Enter makes no world changes. Confirm only on a disposable build area.
   - Old menu hotkeys reach the canonical screens; no hotkey defaults or saved config keys changed.
   - File loading supports .litematic, Sponge, Schematica and vanilla structures; Load only adds no ghost. File Materials/Shift+Materials remain file previews, distinct from the placed build's material count.
   - Tool off/not held: bottom-left instructions should stay quiet. No malilib jar update is needed for this revision.

## Implementation notes

Kept `GuiPlacementConfiguration` as the canonical class and moved the concise controls into its Basic page, rather than replacing a working editor with another implementation. Advanced is part of that same screen. `GuiQuickPlaceAdjust` and `GuiBuildAssist` are deleted. `GuiSchematicLoad` is now abstract conversion support, not a screen users can open. Old config tab enum values are compatibility aliases, not visible categories. Existing files/config formats and hotkey defaults remain intact.

The multi-box/sub-region editors, verifier, material list and file manager remain because they operate on different objects/jobs. This is not a promise that every advanced editor has been visually redesigned. Report the exact screen/path and screenshot for anything still redundant or clipped.
