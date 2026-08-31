## Context

`CopperGolemVisualizationClient` already receives a bounded,
server-authoritative snapshot for the locally selected Golem. It renders the
gathering area through Core each frame, but source/destination relationships
remain tick-sampled particle chains capped at 48 points.

Minecraft 26.2 exposes `Gizmos.line(Vec3, Vec3, int, float)`. TotemCore already
owns the stateless world-outline submission seam and explicit depth behavior,
so a generic line helper belongs there while all feature state stays in
Automata.

## Goals / Non-Goals

### Goals

- Render stable solid links from the selected Copper Golem to same-dimension
  source and destination containers.
- Preserve source, available-destination and unavailable-destination meanings
  through distinct line colours.
- Hide occluded link portions behind terrain.
- Remove the superseded source/destination particle chains.

### Non-Goals

- Do not make container links visible through walls.
- Do not change gathering-target or blocked-state markers.
- Do not move selected-Golem, payload, request, validation or cleanup state
  into Core.
- Do not change server packet contents or container availability authority.

## Decisions

### Extend the existing Core outline API

Core adds `TotemWorldOutlines.line(from, to, style)`. The helper validates its
arguments, submits the vanilla line gizmo and applies `setAlwaysOnTop()` only
for `THROUGH_WALLS`, matching block/cuboid behavior. `WorldOutlineStyle` remains
the common immutable colour/width/occlusion value.

### Submit every valid render frame

Automata's existing `BEFORE_GIZMOS` callback revalidates held Golem identity,
payload validity and current dimension, then submits the source line and, in
sorting mode, all bounded destination lines. Gathering mode retains only its
source link and current target marker.

### Keep availability semantics in Automata

Automata uses orange for an available source, green for an available
destination and red for any unavailable container. Core owns no meaning for
these colours.

## Risks / Trade-offs

- Lines ending at block centres partly enter the destination block. This gives
  a precise endpoint and is consistent across container shapes.
- Depth testing can hide an entire relationship behind terrain. That is the
  requested non-through-wall behavior and prevents an unintended locator.
- A new Core API requires an updated pinned Core artifact before Automata CI or
  publication can pass. The implementation can be tested locally first; commit
  pins remain a separate release-preparation step requiring a real Core commit.

## Migration Plan

1. Add and unit-test the Core line helper.
2. Build the new Core artifact locally.
3. Move Automata source/destination relationships into its render callback and
   remove their particle-chain implementation.
4. Add unit and native-scale client visual coverage, then run development and
   production-runtime validation.
5. After commit authorization, pin Automata CI/release to the real Core commit
   and prepare coordinated versions for publication.
