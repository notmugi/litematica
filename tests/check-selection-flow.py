#!/usr/bin/env python3
"""Structural contracts; executable dialog regression lives in client-smoke."""
from pathlib import Path
base = Path('src/main/java/fi/dy/masa/litematica')
router = (base / 'gui/GuiSaveSelection.java').read_text()
editor = (base / 'gui/GuiAreaSelectionEditorNormal.java').read_text()
manager = (base / 'selection/SelectionManager.java').read_text()
assert 'GuiAreaSelectionManager' not in router + editor
assert 'createNewSelection' not in router
assert 'manager.getEditGui()' in router
factory = manager.split('public GuiBase getEditGui()')[1].split('public void openEditGui')[0]
assert 'return new GuiAreaSelectionEditorNormal(selection)' in factory
assert 'open_gui.no_selection' not in factory
callback = editor.split('protected static class SubRegionCreator')[1].split('protected static class CheckBoxListener')[0]
assert 'createNewSubRegionIfDoesntExist' in callback
assert 'openGui(' not in callback and 'GuiSaveSelection.open(' not in callback
init = editor.split('public void initGui()')[1].split('protected void createSelectionEditFields')[0]
assert init.index('manager.getCurrentSelection()') < init.index('super.initGui()')
assert 'this.reCreateListWidget()' in init
assert 'this.addNavigationButtons()' in init
assert 'GuiSaveSelection.open(this.parent.getParent())' in editor
assert 'if (this.selection == null) return;' in editor
print('PASS: no forced selection/browser; model refresh precedes list init; dialog callback does not navigate')
