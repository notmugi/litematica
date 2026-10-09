#!/usr/bin/env python3
"""Headless structural regression check: python3 tests/check-selection-flow.py."""
from pathlib import Path
base = Path('src/main/java/fi/dy/masa/litematica/gui')
router = (base / 'GuiSaveSelection.java').read_text()
editor = (base / 'GuiAreaSelectionEditorNormal.java').read_text()
simple = (base / 'GuiAreaSelectionEditorSimple.java').read_text()
browser = (base / 'GuiAreaSelectionManager.java').read_text()
entry = (base / 'widgets/WidgetAreaSelectionEntry.java').read_text()
assert 'static void open(@Nullable Screen parent)' in router
assert 'manager.getCurrentSelection() == null' in router
assert '!DataManager.getSchematicProjectsManager().hasProjectOpen()' in router
assert 'needsBrowser ? new GuiAreaSelectionManager() : manager.getEditGui()' in router
assert 'gui.setParent(parent)' in router
assert 'parent instanceof GuiAreaSelectionManager' in router
assert 'GuiSchematicSave' not in router and 'addButton' not in router
assert 'GuiSaveSelection.open(this.parent.getParent())' in editor
assert 'manager.openEditGui(null)' not in editor
assert 'this.addNavigationButtons()' in editor
assert 'this.getParent() instanceof GuiAreaSelectionManager browser' in editor
assert 'browser.setParent(this.getParent())' in editor
assert 'this.closeGui(true)' in editor and 'this.closeGui(true)' in browser
assert 'SchematicUtils.saveSchematic(GuiBase.isShiftDown())' in editor
assert 'GuiSaveSelection.open(this)' in browser
activate = browser.index('this.selectionManager.setCurrentSelection(selectionId)', browser.index('public void openSelectionEditor'))
assert activate < browser.index('this.selectionManager.switchSelectionMode()', activate) < browser.index('GuiSaveSelection.open(this)', activate)
assert 'getSelectionManagerGui().openSelectionEditor(selectionId)' in entry
assert 'new GuiAreaSelectionEditorNormal' not in entry
assert 'class GuiAreaSelectionEditorSimple extends GuiAreaSelectionEditorNormal' in simple
assert 'Corner.CORNER_1' in simple and 'Corner.CORNER_2' in simple
assert 'GuiAreaSelectionEditorSubRegion' in editor
print('Selection routing, activation and Back contracts OK')
