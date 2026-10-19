# Legacy 1.21.1 selection editor regression

Run `JAVA_HOME=/path/to/jdk21 sh tests/client-smoke/run.sh` on a graphical desktop.
This is a dev-only mod/source set, not packaged in the release jar. It opens an
isolated client in `build/ux-smoke/run`; no player instance or world is accessed.

Exercises actual Add Box clicks, cancel/Enter on malilib's input dialog, checks
that the returned editor tracks the active selection, navigation, Clear,
Simple/Normal switches, reopening, and zero-box areas. The second box in the clear
case is seeded through the model; tool clicks in a world are not tested.

Audit run: test sources compile, but the 1.21.1 client stalls inside native
`glfwCreateWindow` before UI initialization on this machine. No runtime PASS is
claimed. The equivalent 26.2 test passes at scales 3 and 2 and fails on pre-fix code.

This branch still has the classic UI. It is not a complete backport of liteux.
The pre-existing legacy Simple clear implementation resets its box to the origin;
porting 26.2's empty-corner model safely requires more work and is not covered by
this editor-lifecycle fix.
