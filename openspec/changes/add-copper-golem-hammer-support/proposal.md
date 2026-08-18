## Why

Copper Golems can hold generic gathering tools, but Totem Excavation hammers
need an explicit, versioned integration contract so their tag-based mining,
enchantments, durability and legacy aliases work safely without accidentally
starting a player-owned area-mining session.

## What Changes

- Add an optional Totem Excavation integration to TotemAutomata, with an exact
  development artifact and a soft runtime dependency.
- Explicitly recognise every canonical and retained legacy Totem Excavation
  hammer as a valid Copper Golem gathering tool.
- Preserve the Copper Golem's own bounded gathering scheduler: each authorised
  target is evaluated, mined, dropped and damaged through the existing
  server-side gathering path; no player item component or player excavation
  session is created.
- Add server GameTests for canonical/legacy hammer acceptance, real harvesting,
  durability, drops, cancellation and standalone-no-Excavation startup.

## Impact

- Affected specs: `copper-golem-hammer-integration`
- Affected code: Automata Gradle/mod metadata, gathering tool policy, menu
  authority, gathering break path and Fabric GameTests.
- Optional external module: Totem Excavation 0.1.0 or later.
