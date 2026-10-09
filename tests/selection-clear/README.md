# Clear selection regression (liteux.6)

Run `sh tests/selection-clear/check.sh` from any directory (Java 25, dependencies cached).
No Minecraft window is launched. Java's source launcher runs the **real** selection
classes and SelectionManager with the production runtime classpath and assertions.
Test files/logs stay in `build/selection-clear-check`; temporary storage is `.work`.

The check reproduces liteux.5's bug: `setCurrentSelection(null)` leaves Simple mode
geometry untouched. It then checks the actual `clearCurrentSelection()` action:

- Simple selection corners, selected-corner flag, manual origin and grab all clear.
- Exactly one empty box stays selected, ready for normal stick corner setters.
- Clear twice is harmless; `hasSelectionToClear()` becomes false.
- Save/reload/copy of empty and half-set geometry never adds a corner at `(0,0,0)`.
- Empty calculated origin/size are zero, not the old bounds sentinel at 60 million.
- Coordinates of a cleared box can still be read/edited without null crashes.
- Named selections are unselected, but their in-memory geometry and saved file survive.
- Project Simple and Normal geometry clears; project stays open and is marked dirty.
- Ghosts button and browser Unselect route to the shared action, not preview removal.

Headless setup bootstraps Minecraft registries, not the client. For project config's
static default-directory initialization only, the test uses an unconstructed client
object with `gameDirectory` set, then immediately restores the singleton to null.
No player world, renderer, launcher config, or game client constructor is involved.
Reflection seeds grabs/projects that otherwise require interactive tool input.

User check after restarting: make a box with the stick → Ghosts → Clear selection.
The area box/origin should disappear, existing schematic previews should remain,
and the next stick clicks should create a fresh area. Rejoin once to check that the
cleared area stays empty. Copy a build should open without a crash after clearing.
