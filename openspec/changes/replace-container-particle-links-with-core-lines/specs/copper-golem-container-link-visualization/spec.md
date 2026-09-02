## ADDED Requirements

### Requirement: Shared solid container links

TotemAutomata SHALL submit source and destination relationships through
TotemCore's stateless two-point world-line API from Automata's own render
callback. Automata MUST retain ownership of selected-Golem identity, payload,
request cadence, validation and cleanup state.

#### Scenario: Sorting snapshot with source and destinations

- **WHEN** the locally selected Copper Golem has a valid sorting snapshot with
  a same-dimension source and bounded same-dimension destinations
- **THEN** Automata submits one solid link from the Golem to each present
  source/destination container

#### Scenario: Gathering snapshot

- **WHEN** the valid selected-Golem snapshot is in gathering mode
- **THEN** Automata submits the source link without treating sorting
  destinations as active links

#### Scenario: Selected Golem moves between snapshot updates

- **WHEN** a valid selected Copper Golem is present in the client level and
  moves while the server relationship snapshot remains unchanged
- **THEN** every rendered link starts at that entity's current interpolated
  position instead of the sampled snapshot position

#### Scenario: Stale or cross-dimension entry

- **WHEN** the selected Golem or payload is stale, invalid or in another
  dimension, or an individual container entry belongs to another dimension
- **THEN** Automata submits no corresponding container link

### Requirement: Depth-tested availability presentation

Every container relationship line SHALL use terrain depth testing. Available
sources SHALL be orange, available destinations SHALL be green, and unavailable
containers SHALL be red. No container relationship line may be always-on-top.

#### Scenario: Link partly behind an opaque wall

- **WHEN** a source or destination line passes geometrically behind opaque
  terrain from the local camera
- **THEN** its unobstructed segment remains visible and the terrain hides the
  occluded segment

### Requirement: Retain non-link particles

Migrating container relationships SHALL stop the superseded
source/destination particle chains without removing Automata's blocked-state
smoke or current gathering-target particle marker.

#### Scenario: Blocked gathering activity with a current target

- **WHEN** a valid gathering snapshot reports blocked activity and a current
  target
- **THEN** the solid source link is rendered while the blocked and target
  particle indicators retain their existing behavior
