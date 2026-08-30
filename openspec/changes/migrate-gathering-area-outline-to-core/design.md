## Context

`CopperGolemVisualizationClient` already owns the local selected-Golem state
and receives a bounded, server-authoritative visualization snapshot. Its
gathering-area branch currently samples all twelve cuboid edges into particles
on a tick cadence. TotemCore 0.7.13 provides a stateless gizmo submission API
with explicit depth behavior, but intentionally owns no feature state or
render callback.

## Goals / Non-Goals

### Goals

- Render complete gathering areas as stable, depth-tested cuboid outlines.
- Render a present corner as a depth-tested block outline while the area is
  incomplete.
- Submit only the valid selected Golem's same-dimension gathering snapshot.
- Keep all lifecycle and authority decisions in Automata.

### Non-Goals

- Do not make Automata gathering areas visible through walls.
- Do not migrate path, destination, blocked-state or current-target particles.
- Do not change how players configure areas or how servers authorize and send
  visualization snapshots.
- Do not move cached payloads, selected Golem identity or cleanup state into
  TotemCore.

## Decisions

### Submit from Automata's level-render callback

Automata registers one `LevelRenderEvents.BEFORE_GIZMOS` callback during its
existing client cutover initialization. The callback revalidates the held
Golem, payload validity, gathering mode and current dimension before submitting
the outline. Tick handling continues to own requests and particle-only
visualizations.

### Use one shared depth-tested style

Complete cuboids and incomplete corner blocks use an immutable cyan
`WorldOutlineStyle` with `DEPTH_TESTED`. A small inflation avoids coincident
block-face flicker without changing the represented inclusive block bounds.

### Remove only area edge particles

The twelve particle edges and corner particles are removed once their Core
outline replacement is active. Source/destination lines, target markers and
blocked-state particles remain unchanged because they communicate different
runtime information and are outside the shared selection-box contract.

## Risks / Trade-offs

- A stale payload could otherwise remain visible after the player changes
  items or dimensions. The render callback repeats the same selected-Golem and
  dimension checks used by the tick path, while existing `clear()` lifecycle
  handling remains authoritative.
- Terrain depth testing intentionally hides occluded edges, so a large area is
  less legible from outside a structure. This is the requested privacy and
  spatial behavior for selection ranges.
- The named development runtime rewrites Fabric API tweaker namespaces, so it
  cannot prove that the shipped JAR starts in the official distribution
  namespace. A dedicated Production Runtime workflow loads the built Automata,
  Core and optional Excavation JARs through Loom's production client task.
- The repository's three-JVM requirement belongs to its module-owned Observer
  UI contract: Dedicated Server + Target Client + Observer Client validates a
  remote semantic relay. This change modifies only a local world render
  callback and changes no Screen/Menu, Observer provider, packet or cross-client
  state, so inventing a second client would not exercise an affected contract.

## Migration Plan

1. Add the render callback and Core-backed outline helper.
2. Remove gathering-area edge/corner particle emission.
3. Raise Core build, metadata and CI/release pins to 0.7.13.
4. Validate gizmo depth properties and capture a wall-occlusion Client GameTest
   screenshot.
5. Add and run an official-namespace production Client GameTest gate against
   distribution JARs; keep the Observer-only three-JVM relay gate out of scope.
