# Load-browser checks

Run `./tests/quick-place/check.sh` (Java 25, no client or testing dependency).
The Java check covers absolute coordinate parsing, signs/whitespace, build-height
and horizontal limits, custom world heights, overflow and malformed values.
The source contracts cover conversion reuse, material previews, canonical manager
file actions, world/commit guards, shared editor handoff and keyboard dispatch.

Compile with local temporary storage (no client launch):

```sh
mkdir -p .work/tmp
JAVA_HOME=/usr/lib/jvm/java-25-openjdk TMPDIR="$PWD/.work/tmp" \
  JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$PWD/.work/tmp" ./gradlew compileJava --offline
```

## User-run UI acceptance

- Open the load browser from Home and old Load buttons. There is no Advanced
  browser or auto-place checkbox. Each supported format (litematic, Schematica,
  Sponge, vanilla structure) enables exactly one Next and schematic Materials.
- Metadata/thumbnail previews retain the existing browser behavior (foreign
  formats may lack previews). Materials previews do not load/place anything;
  Shift+Materials selects sub-regions, including multiple regions. Back/Escape
  from materials returns to the browser.
- Home → My schematics retains schematic rename, file rename, and custom JSON/TXT
  material lists. These file-management actions are not duplicated in the picker.
- Next shows Here / Coordinates / Load only without registering anything.
  Back/Next reuses the parsed object. Cancel before committing adds nothing.
- Load only adds one loaded object and no placement. Here/Coordinates add one
  loaded object and one enabled ghost, then open the same placement editor used
  by existing placements. No real blocks change; no layer/paste shortcuts here.
- Blank, fractional, relative, overflowing, out-of-height and outside-border
  coordinates are rejected; values remain editable. Resize preserves input.
- Tab/Shift+Tab traverse fields/buttons. Space/Enter while editing never places.
  Browser footer focus cannot type into the search field; returning focus to the
  browser restores native search behavior. Mouse clears footer keyboard focus.
- Double-clicking Coordinates must not immediately confirm. Returning to a
  committed screen must not create another placement. Unreadable/corrupt files
  and unexpected commit failures show persistent errors, not silent transitions.
- Disconnect/change dimension while retaining a screen: stale actions must refuse
  to mutate anything. Check layouts/tooltips at 320×240 and larger GUI scales.

These focused checks are not a substitute for user UI testing. Anchor coordinates,
not every schematic block, are constrained to world bounds. Old persisted
create-placement-on-load data is left intact but no longer drives this flow.
