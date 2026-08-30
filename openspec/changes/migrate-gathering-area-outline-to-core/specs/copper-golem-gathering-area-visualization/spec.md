## ADDED Requirements

### Requirement: Module-owned gathering-area outline

TotemAutomata SHALL render the configured gathering area from its existing
valid, server-authoritative Copper Golem visualization snapshot through the
TotemCore client world-outline API. Automata MUST retain ownership of the held
Golem identity, cached payload, request cadence, render callback and cleanup
state.

#### Scenario: Complete current-dimension gathering area

- **WHEN** the local player holds a Copper Wrench bound to the payload's Golem,
  the payload is valid and in gathering mode, and both area corners belong to
  the current dimension
- **THEN** Automata submits one inclusive cuboid outline for the configured
  block range through TotemCore

#### Scenario: Incomplete area

- **WHEN** the valid selected-Golem snapshot contains only one configured area
  corner in the current dimension
- **THEN** Automata submits a block outline for that corner without inventing a
  complete range

#### Scenario: Stale or unrelated snapshot

- **WHEN** the held Golem, dimension, mode or payload validity no longer
  matches the active gathering visualization
- **THEN** Automata submits no gathering-area outline

### Requirement: Terrain-occluded selection presentation

Every Automata gathering-area cuboid and corner outline SHALL use TotemCore's
`DEPTH_TESTED` occlusion mode. Automata MUST NOT make this selection range
always-on-top or visible through opaque terrain.

#### Scenario: Area partly behind an opaque wall

- **WHEN** part of the gathering-area outline is geometrically behind an
  opaque wall from the local camera
- **THEN** the unobstructed portions remain visible and the wall hides the
  occluded portions

### Requirement: Preserve non-area visualization semantics

Migrating the configured area SHALL NOT remove or reinterpret Automata's
particle communication for source links, sorting destinations, current
gathering targets or blocked activity. The superseded gathering-area edge and
corner particles MUST stop being emitted.

#### Scenario: Gathering target and area are both present

- **WHEN** a valid gathering snapshot contains a configured area and a current
  target
- **THEN** the area is submitted as a Core outline while the target retains its
  existing Automata particle marker
