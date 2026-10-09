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
handler = ghosts.split('private void clearSelection()')[1].split('private void showTab')[0]
assert 'getSelectionManager().clearCurrentSelection()' in handler
assert 'setCurrentSelection(null)' not in handler
assert 'removeSchematicPlacement' not in handler
assert 'getSelectionManager().hasSelectionToClear()' in ghosts
browser = (base / 'gui/GuiAreaSelectionManager.java').read_text()
unselect = browser.split('else if (this.type == ButtonType.UNSELECT)')[1].split('public enum')[0]
assert 'clearCurrentSelection()' in unselect
print('PASS: Ghosts and browser clear use the mode-aware action, not named-ID reset or preview removal')
PY
