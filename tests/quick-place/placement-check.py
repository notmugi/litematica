#!/usr/bin/env python3
"""Runnable source tripwires for the Minecraft-bound single placement editor."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[2]
gui = root / 'src/main/java/fi/dy/masa/litematica/gui'
editor = (gui / 'GuiPlacementConfiguration.java').read_text()
context = (gui / 'PlacementUiContext.java').read_text()
region = (gui / 'GuiSubRegionConfiguration.java').read_text()
assert not (gui / 'GuiQuickPlaceAdjust.java').exists()
assert 'public GuiPlacementConfiguration(SchematicPlacement placement)' in editor
assert 'private boolean advanced;' in editor  # defaults to basic, no global page state
assert 'return this.advanced ? super.getListWidget() : null;' in editor
assert 'this.createBasicPage();' in editor
assert 'new GuiPlacementConfiguration(this.placement)' not in editor
assert 'GuiQuickPlace.' not in editor  # direct hotkey entry does not require a browser
for source in (editor, region):
    for duplicate in ('GuiRenderLayer', 'WidgetSlider', 'setLayerMode', 'getRenderLayerRange'):
        assert duplicate not in source

# No placement writes / world edits / render-setting writes when opening or resizing.
init = editor.split('public void initGui()', 1)[1].split('protected WidgetListPlacementSubRegions getListWidget()', 1)[0]
for write in ('pastePlacementToWorld(', 'setOrigin(', 'setRotation(', 'setMirror(', 'setBooleanValue(', 'toggleLocked('):
    assert write not in init
assert editor.count('pastePlacementToWorld(this.placement, false, true, this.mc)') == 1
assert 'pastePlacementToWorldConfirmed' not in editor
assert 'EntityUtils.isCreativeMode(this.mc.player)' in editor
for guard in ('this.mc.level != this.world', 'this.mc.player != this.player', 'this.mc.getConnection() != this.connection',
              'DataManager.getSchematicPlacementManager() != this.manager',
              'getAllSchematicsPlacements().contains(this.placement)'):
    assert guard in context
assert 'this.parent.context.valid(this.parent)' in editor
assert 'this.parent.valid()' in region
assert 'getAllSubRegionsPlacements().contains(this.placement)' in region
assert 'this.parent.getParent() instanceof GuiPlacementConfiguration editor' in region

# Core setters preserve the placement/coordinate lock semantics; movement validates effective origin.
for capability in ('setOrigin(pos, this)', 'setRotation(rotation, this.parent)', 'setMirror(mirror, this.parent)',
                   'setCoordinateLocked(type,', 'isInsideBuildHeight(pos)', 'getWorldBorder().isWithinBounds(pos)',
                   'resetAllSubRegionsToSchematicValues', 'getCurrentEntries()', 'setSubRegionsEnabledState',
                   'toggleIgnoreEntities', 'toggleRenderEnclosingBox', 'setRenderSchematic', 'toggleLocked',
                   'setName', 'WidgetListPlacementSubRegions', 'new GuiMaterialList', 'new GuiSchematicVerifier'):
    assert capability in editor
assert 'textField.setEditable(!this.placement.isLocked() && !this.placement.isCoordinateLocked(type))' in editor
assert 'this.placement.isLocked()' in editor
assert 'type.getDisplayName(StringUtils.translate(' in editor  # on/off labels contain a %s placeholder
restore = editor.split('private void restorePreview()', 1)[1].split('private void useTool()', 1)[0]
for switch in ('ENABLE_RENDERING', 'ENABLE_SCHEMATIC_RENDERING', 'ENABLE_SCHEMATIC_BLOCKS'):
    write = f'Configs.Visuals.{switch}.setBooleanValue(true);'
    assert write in restore and editor.count(write) == 1
assert editor.index('if (fieldFocus >= 0) return super.onKeyTyped(input);') < editor.index('input.key() == 257')
assert 'this.fields.get(i).setFocusedWrapper(i == this.focused)' in editor
assert 'this.pageButtons(x, 212, width)' in editor and 212 + 20 <= 240
strings = json.loads((root / 'src/main/resources/assets/litematica/lang/en_us.json').read_text())
fragment = root / '.work/baby-placement-strings.json'
if fragment.exists():
    strings.update(json.loads(fragment.read_text()))
for key in ('title', 'basic', 'advanced', 'lock_axis', 'unlock_axis'):
    assert strings['litematica.gui.placement_editor.' + key]
print('Single placement editor source contracts passed')
