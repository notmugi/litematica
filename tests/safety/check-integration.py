from pathlib import Path
base = Path('src/main/java/fi/dy/masa/litematica')
project = (base / 'schematic/projects/SchematicProject.java').read_text()
operation = project.split('public void pasteToWorld()')[1].split('public void deleteLastSeenArea')[0]
assert operation.count('UxSafety.confirm(') == 1
assert 'prepareProjectPaste(placement, mc)' in operation
assert 'prepareProjectDelete(area, completion, mc)' in operation
assert operation.index('PASTE_TO_MCFUNCTION') < operation.index('prepareProjectDelete')
assert 'this::pasteCurrentPlacement' not in operation
manager = (base / 'gui/GuiSchematicProjectManager.java').read_text()
assert 'GuiConfirmAction' not in manager and 'PlaceToWorldExecutor' not in manager
safety = (base / 'util/UxSafety.java').read_text()
assert 'area.toJson().deepCopy()' in safety and 'range.toJson().deepCopy()' in safety
assert 'heldTriggers.removeIf(code -> !KeybindMulti.isKeyDown(code))' in safety
assert 'code < 340 || code > 347' in safety
move = (base / 'util/SchematicUtils.java').read_text().split('private static void moveCurrentlySelectedWorldRegionToConfirmed')[1].split('public static void cloneSelectionArea')[0]
assert move.count('return stillCurrent.getAsBoolean() && super.canExecute()') == 4
assert 'mc.getConnection() == connection && mc.getSingleplayerServer() == server' in move
assert 'pendingMove.getAsBoolean()' in move
# Guard the actual layout's viewport clamps, pagination and both keyboard/mouse navigation.
gui = (base / 'gui/GuiUxConfirm.java').read_text()
for text in ('this.getScreenWidth() - 20', 'this.getScreenHeight() - 80', 'lines.subList(', 'case 266', 'case 267', 'onMouseScrolled', 'onMouseReleased', 'keyReleased', 'pollHeldTriggers()'):
    assert text in gui, text
print('Safety integration checks passed')
paste_manager = (base / 'schematic/placement/SchematicPlacementManager.java').read_text()
prepared = paste_manager.split('public Runnable prepareProjectPaste(')[1].split('private void pastePlacementToWorldConfirmed')[0]
assert 'PASTE_TO_MCFUNCTION' in prepared
assert 'pastePlacementToWorldConfirmed' in prepared and 'UxSafety.confirm(' not in prepared
