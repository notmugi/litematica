#!/usr/bin/env python3
"""Source contracts for Minecraft-bound UI; coordinate parsing is checked in Java."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[2]
gui = root / 'src/main/java/fi/dy/masa/litematica/gui'
browser = (gui / 'GuiQuickPlace.java').read_text()
loader = (gui / 'GuiSchematicLoad.java').read_text()
position = (gui / 'GuiQuickPlacePosition.java').read_text()
manager = (gui / 'GuiSchematicManager.java').read_text()

# One concrete browser; conversion remains shared and previews never register anything.
assert 'public abstract class GuiSchematicLoad' in loader
assert 'protected GuiSchematicLoad()' in loader
for old in ('WidgetCheckBox', 'CreatePlacementOnLoad', 'SchematicRenamer', 'FileRenamer', 'SchematicHolder'):
    assert old not in loader
assert 'new GuiSchematicLoad(' not in browser
assert 'text("legacy")' not in browser
assert browser.count('text("next")') == 1
assert 'next.setEnabled(valid)' in browser and 'materials.setEnabled(valid)' in browser
assert 'this.loadSelectedSchematic();' in browser
assert 'this.previewSelectedMaterials();' in browser
assert 'if (this.positionScreen == null)' in browser
for conversion in ('LitematicaSchematic.createFromFile', 'convertSchematicaSchematicToLitematicaSchematic',
                   'convertStructureToLitematicaSchematic', 'convertSpongeSchematicToLitematicaSchematic'):
    assert conversion in loader
assert 'Files.isRegularFile(file)' in loader and 'Files.isReadable(file)' in loader
assert 'catch (RuntimeException e)' in loader
assert 'if (GuiBase.isShiftDown())' in loader
assert 'GuiStringListSelection' in loader
assert 'new MaterialListSchematic(schematic, regions, true)' in loader
assert 'new MaterialListSchematic(schematic, true)' in loader
# Manager remains the canonical file-operation/custom-list destination.
for capability in ('new SchematicRenamer(', 'new FileRenamer('):
    assert capability in manager
assert 'GuiSchematicBrowserBase' in loader  # retains WidgetSchematicBrowser metadata/thumbnail previews

# Guard world identity and repeated commits; initial/existing placement share one editor.
assert 'if (this.committed || !this.browser.checkWorld(this)) return;' in position
assert 'if (!this.committed && this.browser.checkWorld(this))' in position
# Legacy malilib delivers individual clicks, not double-click event records.
assert 'onMouseClicked(int mouseX, int mouseY, int mouseButton)' in position
assert position.count('addSchematicPlacement(') == 1
assert 'this.mc.world != this.world' in browser
assert 'DataManager.getSchematicPlacementManager() != this.manager' in browser
assert 'new GuiPlacementConfiguration(placement)' in position
assert 'GuiQuickPlaceAdjust' not in position
assert position.index('this.committed = true') < position.index('addSchematic(this.schematic, true)')
assert 'catch (RuntimeException e)' in position
assert 'isOutOfHeightLimit(pos)' in position and 'getWorldBorder().contains(pos)' in position
assert 'InfoUtils.showInGameMessage(type' in browser
assert 'GuiQuickPlace.keepError(type, duration, key, args);' in position
for source in (browser, loader, position):
    assert 'GuiRenderLayer' not in source and 'pastePlacement' not in source

# Space/Enter cannot activate a button while any coordinate field owns focus.
assert position.index('if (fieldFocus >= 0) return super.onKeyTyped(keyCode, scanCode, modifiers);') < position.index('keyCode == 257')
assert 'this.fields[i].setFocused(i == this.focused)' in position
assert 'GuiBase.isShiftDown() ? -1 : 1' in position
assert 'this.focused >= 0 || super.onCharTyped(charIn, modifiers)' in browser
assert 'this.focused >= 0 && keyCode != 256' in browser
strings = json.loads((root / 'src/main/resources/assets/litematica/lang/en_us.json').read_text())
fragment = root / '.work/baby-browser-strings.json'
if fragment.exists():
    strings.update(json.loads(fragment.read_text()))
for key in ('browser_hint', 'commit_failed', 'materials', 'coordinate_tip'):
    assert strings['litematica.gui.quick_place.' + key]
print('Quick-place integration source contracts passed')
