#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}"
mkdir -p .work
export TMPDIR="$PWD/.work"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$PWD/.work"
./gradlew -I tests/selection-clear/check.gradle checkSelectionClear --offline
python3 - <<'PY'
from pathlib import Path
base = Path('src/main/java/fi/dy/masa/litematica')
ghosts = (base / 'gui/GuiGhosts.java').read_text()
assert 'clearCurrentSelection()' not in ghosts
editor = (base / 'gui/GuiAreaSelectionEditorNormal.java').read_text()
assert 'getSelectionManager().clearCurrentSelection()' in editor
assert 'getSelectionManager().hasSelectionToClear()' in editor
assert 'removeSchematicPlacement' not in editor
browser = (base / 'gui/GuiAreaSelectionManager.java').read_text()
unselect = browser.split('else if (this.type == ButtonType.UNSELECT)')[1].split('public enum')[0]
assert 'clearCurrentSelection()' in unselect
print('PASS: Editor and browser clear use the mode-aware action, not named-ID reset or preview removal')
PY
