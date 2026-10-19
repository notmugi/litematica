#!/usr/bin/env python3
"""Run with python3 ux-check-home.py from the repository root; no test dependencies."""
import json
import re
from pathlib import Path

base = Path('src/main/java/fi/dy/masa/litematica/gui')
files = ['GuiMainMenu', 'GuiUxHelp', 'GuiToolModePicker', 'GuiGhosts', 'GuiSaveSelection']
source = '\n'.join((base / (name + '.java')).read_text() for name in files)
strings = json.loads(Path('src/main/resources/assets/litematica/lang/en_us.json').read_text())
for key in re.findall(r'"(litematica\.gui\.ux\.[\w.]+)"', source):
    if key.endswith('.') or key in ('litematica.gui.ux.help.page',):
        continue
    assert key in strings, key
for mode in ('area_selection', 'schematic_placement', 'fill', 'replace_block',
             'paste_schematic', 'grid_paste', 'move', 'delete', 'rebuild'):
    assert 'litematica.gui.ux.picker.' + mode in strings
for page in range(3):
    for suffix in ('title', 'line1', 'line2'):
        assert f'litematica.gui.ux.help.page{page}.{suffix}' in strings
for card in ('place', 'save', 'build', 'files'):
    assert f'litematica.gui.ux.home.{card}.tip' in strings

# Check the actual home sizing expressions, not a second independently hard-coded formula.
home = (base / 'GuiMainMenu.java').read_text()
card_expr = re.search(r'int cardHeight = (.*);', home)[1]
card_expr = card_expr.replace('Math.max', 'max').replace('Math.min', 'min').replace('this.getScreenHeight()', 'height').replace(' / ', ' // ')
for width in (296, 332, 356, 456, 616, 1256):
    for height in (240, 260, 279, 280, 360, 720):
        columns = 2 if width >= 356 else 1
        rows = 4 // columns
        y = 30 + 13 + 13 + (16 if height >= 280 else 0) + 28
        card_height = eval(card_expr, {'__builtins__': {}, 'max': max, 'min': min}, locals())
        last_card_bottom = y + (rows - 1) * (card_height + 5) + card_height
        assert last_card_bottom <= height - 26, (width, height, last_card_bottom)
        assert y + 46 <= height - 26  # task/project links
        assert 64 + 4 * 24 + 20 <= height - 26  # compact mode picker
        assert 118 <= height - 76  # last help text vs. navigation

assert 'isCtrlDown' not in source
assert 'setToolItem(' not in source
assert 'mode.cycle(' not in home
picker = (base / 'GuiToolModePicker.java').read_text()
assert 'next.cycle(this.mc.player, true)' in picker
assert 'button.setEnabled(allowed.contains(mode))' in picker
assert 'this.availableModes().contains(mode)' in picker
assert 'Configs.saveToFile()' in (base / 'GuiUxHelp.java').read_text()
assert 'GuiAreaSelectionManager' not in (base / 'GuiSaveSelection.java').read_text()
print('PASS: home layouts at 36 Minecraft GUI sizes, translation coverage, picker/gating/persistence invariants')
