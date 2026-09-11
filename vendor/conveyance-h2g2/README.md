# conveyance-h2g2

A composable-set library for [Conveyance](https://github.com/HereLiesAz/Conveyance): The h2g2 style system -- hues, ground-rotation surfaces, and the 8-step Jost type scale -- ported from HG2Gui.

## What this is

Per [azphalt's `spec/composable.md`](https://github.com/HereLiesAz/azphalt/blob/main/spec/composable.md),
a `kind: "composable"` `.azp` package is a **pure header**: it names this artifact's Gradle
coordinates (`library.group` / `library.artifact`) and selects a `templateId`, `hue`,
`surface`, `scale`, and `act` from it. It carries no code of its own. This repository *is* the
artifact a composable package's `library` block points at -- the `.azp` package itself is
authored and published separately, wherever its author chooses; this repo does not need to hold
one.

Example composable manifest referencing this library:

```jsonc
{
  "azphalt": "0.1",
  "id": "com.hereliesaz.azphalt.example",
  "name": "Example",
  "version": "1.0.0",
  "kind": "composable",
  "license": "MIT",
  "compat": ">=0.1",
  "composable": {
    "library": { "group": "com.hereliesaz.conveyance", "artifact": "conveyance-h2g2", "version": "0.1.0" },
    "elements": [
      { "id": "confirm-record", "templateId": "h2g2.tile.record", "hue": "confirm-record", "surface": "recordTile", "scale": "lead", "act": "create", "jobs": ["confirms a destructive action"] }
    ]
  },
  "files": {}
}
```

## What's here

Ported from HG2Gui's own Azphalt style system (h2g2's origin), values unchanged unless noted:

- **`H2g2`** (`H2g2.kt`) -- the 14-entry hue/cap palette and `hueOf(id)`, a deterministic hash
  from any identifier to a hue. This is [`Job.Identify`](https://github.com/HereLiesAz/Conveyance)
  territory, not `Channel.Hue` -- it tells elements apart, it doesn't rank them.
- **`Ground`/`GroundState`** (`Grounds.kt`) -- the six weighted background themes and their
  fold-crease gradient. Adapted from a process-wide singleton (h2g2's own app-level pattern) to a
  `GroundState` a host instantiates and scopes itself, since a library shouldn't impose global
  mutable state on its consumer.
- **`H2g2Type`/`h2g2Type()`** (`Type.kt`) -- the eight-step type scale (`hero` through `micro`).
  Defaults to `FontFamily.Default`; the original uses Jost specifically, but bundling that
  typeface's font files is a per-host asset decision this library doesn't make for you -- pass
  your own Jost `FontFamily` to `h2g2Type()` for the exact original look.
- **`H2g2Surface`** (`Surfaces.kt`) -- **four** shapes: `recordTile`/`note`, from the source-code
  `AzphaltSurface` object, plus `well` (18dp) -- a shape that object doesn't name, but the actual
  design mockups (`docs/HG2Gui Surfaces.dc.html`) use consistently: an ink-background container
  for raw/monospace output, nested inside a `recordTile` or standing on its own. Everything else
  is the fully-rounded `capsule`. `note` is not a lesser shape despite one design doc
  (`docs/DESIGN.md`, which is scoped to the pill menu specifically) suggesting otherwise -- the
  file explorer's own `folderTileShape(isOpen, width)` picks between all three of
  `recordTile`/`note`/`capsule` by state and size, treating them as equally deliberate.
- **`H2g2Page`** (`Page.kt`) -- the one-line convenience for painting a `Ground` behind a whole
  screen. Deliberately **not** a `Templates.registry` entry: every composable manifest element
  requires a non-empty `act` (azphalt `spec/composable.md`), and a page background isn't an
  actionable control -- `Ground`/`GroundState`/`Modifier.ground` (`Grounds.kt`) are already a
  complete, directly callable API; this just wraps the common case.
- **`Templates`** (`Templates.kt`) -- the `templateId` registry. Six templates:
  `h2g2.tile.record` (a `recordTile`-shaped element, one line or a title+`subtitle` two-line
  form), `h2g2.tile.record.detail` (a `recordTile` with a nested `well` holding
  `detailLines` -- the raw-output-panel pattern from the mockups), `h2g2.tile.record.capped` (a
  `recordTile` with `endCapText` riding the right end), `h2g2.tile.note` (a `note`-shaped
  element, an `eyebrow`-step `subtitle` label *above* the body line -- a note is labeled before
  it's read, not captioned after), `h2g2.pill.action` (a plain `capsule`-shaped element), and
  `h2g2.pill.capped` (a `capsule` with `endCapText`) -- all `Offer`-backed, hue-colored via
  `indexOf`, type-set via `h2g2Type().step(scale)`. The two `.capped` templates are the first to
  use `H2g2.caps` and the `endCap` type step, both previously ported but unused -- `docs/DESIGN.md`
  §2's own "label and end-cap sit together at the right end," read for a natural-width tile/pill
  rather than the pill menu's own anchored, overhung ones, with the "darker mate on the end-cap"
  coloring from §7 ("Unchanged from Azphalt") layered on top.

## Status

Covers h2g2's full surface vocabulary (all four shapes have a template) and, per shape, more than
one act/layout pairing where a real pattern from the source app justified it: a record tile can
carry a nested well, an end-cap, or neither. What's still not here: Jost as the actual default
rather than an opt-in -- bundling that typeface's font files remains a per-host asset decision
this library doesn't make for you -- and a `.capped` variant of `h2g2.tile.note`, which the
source app's own vocabulary doesn't obviously call for (a note is small and self-contained; an
end-cap is a pill-menu/record-tile pattern for a "further detail rides the right end" reading, and
forcing it onto every shape regardless of whether the source app actually uses it there is exactly
the kind of unreconciled addition this port has tried to avoid).

An adversarial audit found and this repo has since fixed two real defects: every template's
`Offer` was missing `Modifier.tell(owesTell, weight).clickable { engage() }` -- every element
rendered but was inert, since nothing ever engaged the act on tap (matches
`conveyance-demo/.../Gallery.kt`'s own wiring now); and `hueSeed` resolution went straight to
`H2g2.hueOf`'s hash, so a manifest author naming a real hue (`"hue": "violet"`) never actually got
violet -- `H2g2.indexOf` now checks `hueNames` for an exact match first, falling back to the hash
for anything else.

## Using it

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.HereLiesAz:conveyance-h2g2:main-SNAPSHOT")
}
```

Resolved via [JitPack](https://jitpack.io) directly from this repository -- `conveyance-core` and
`conveyance-compose` both apply `maven-publish`, which is all JitPack needs, so there is no
separate publish step to configure. Conveyance itself has no tagged release yet, so this artifact
and its upstream dependency on Conveyance both pin to `main-SNAPSHOT` for now; switch both to a
real tag once one exists.
