#!/usr/bin/env python3
"""Consolidation tripwires, no Minecraft client needed. Run from repository root."""
from pathlib import Path
import re

root = Path('src/main/java/fi/dy/masa/litematica')
gui = root / 'gui'
sources = {str(p): p.read_text() for p in root.rglob('*.java')}
all_source = '\n'.join(sources.values())
for removed in ('GuiQuickPlaceAdjust', 'GuiBuildAssist', 'GuiSchematicLoadedList', 'GuiSchematicPlacementsList'):
    assert not (gui / (removed + '.java')).exists(), f'Duplicate screen retained: {removed}'
    assert removed not in all_source, f'Stale route to {removed}'
assert 'public abstract class GuiSchematicLoad' in (gui / 'GuiSchematicLoad.java').read_text()
assert not re.search(r'new GuiSchematicLoad\s*\(', all_source), 'Legacy load UI reachable'
position = (gui / 'GuiQuickPlacePosition.java').read_text()
assert 'new GuiPlacementConfiguration(placement)' in position
assert 'new GuiPlacementConfiguration' in (gui / 'widgets/WidgetSchematicPlacement.java').read_text()
assert 'new GuiPlacementConfiguration' in (root / 'event/KeyCallbacks.java').read_text()

# One GUI home for world paste; execute-operation hotkey continues through shared manager.
paste_screens = [path for path, source in sources.items()
                 if '/gui/' in path and 'pastePlacementToWorld(' in source]
assert paste_screens == [str(gui / 'GuiPlacementConfiguration.java')], paste_screens
layer_screens = [path for path, source in sources.items()
                 if '/gui/' in path and 'new GuiRenderLayer(' in source]
assert layer_screens == [str(gui / 'GuiMainMenu.java')], layer_screens
assert 'new GuiQuickPlace' not in (gui / 'GuiUxHelp.java').read_text(), 'Help must explain, not duplicate navigation'
home = (gui / 'GuiMainMenu.java').read_text()
advanced = home.split('if (this.advanced)')[1].split('\n        else')[0]
for legacy in ('LOAD_SCHEMATICS', 'LOADED_SCHEMATICS', 'SCHEMATIC_PLACEMENTS', 'AREA_EDITOR', 'AREA_SELECTION_BROWSER'):
    assert legacy not in advanced, f'Redundant Advanced menu link: {legacy}'
assert 'GuiSaveSelection.open(this)' in home
assert 'new GuiGhosts()' in home
assert 'EASY_PLACE_MODE.setBooleanValue' not in home
assert 'TOOL_ITEM_ENABLED.setBooleanValue(!Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue())' in home
assert 'Configs.saveToFile();' in home
assert 'this.initGui();' in home
assert 'litematica.gui.ux.keys' not in home, 'Duplicate Home -> Keys route returned'

# Loaded-object actions remain in Ghosts; editing a created placement uses the same editor.
ghosts = (gui / 'GuiGhosts.java').read_text()
assert 'new WidgetListLoadedSchematics' in ghosts and 'new WidgetListSchematicPlacements' in ghosts
assert 'setSelectedSchematicPlacement(entry);' in ghosts
entry = (gui / 'widgets/WidgetSchematicEntry.java').read_text()
for action in ('CREATE_PLACEMENT', 'RELOAD', 'SAVE_TO_FILE', 'UNLOAD', 'requestUnload', 'wasModifiedSinceSaved'):
    assert action in entry
assert 'new fi.dy.masa.litematica.gui.GuiPlacementConfiguration(placement)' in entry
print('PASS: canonical placement/load/ghost/settings routes; removed hubs; retained loaded-object operations')
