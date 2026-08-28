## ADDED Requirements

### Requirement: Bounded fair gathering search

Automata SHALL cap lightweight gathering discovery at 32 block positions per
Copper Golem per server tick and 256 positions across the server per tick.
Automata MUST rotate allocation between loaded managed Copper Golems so a
stable iteration order cannot indefinitely starve a searcher.

#### Scenario: One searching Golem
- **WHEN** one running gathering Copper Golem has no current target in a larger configured area
- **THEN** Automata SHALL inspect no more than 32 lightweight positions during that server tick

#### Scenario: More than eight searching Golems
- **WHEN** more than eight loaded Copper Golems concurrently need search work
- **THEN** Automata SHALL inspect no more than 256 total positions in that server tick
- **AND** Automata SHALL rotate later allocations so every continuously searching Golem eventually receives budget

### Requirement: Stable target before search

Automata MUST process a persisted, cheaply valid gathering target without
performing cursor discovery or replacing that target. It SHALL clear a target
only when it is outside the configured area, unloaded, air or liquid,
unbreakable, unsafe, a home/bound container, or otherwise no longer a cheap
candidate.

#### Scenario: Existing target remains valid
- **WHEN** a running gathering Copper Golem begins a tick with a persisted target that passes cheap validity checks
- **THEN** Automata SHALL perform zero gathering cursor inspections
- **AND** Automata SHALL continue moving to or working on that same target

#### Scenario: Existing target becomes invalid
- **WHEN** a persisted target becomes unloaded or fails a cheap validity check
- **THEN** Automata SHALL clear that target without force-loading its chunk
- **AND** subsequent search work SHALL remain subject to the shared bounded budget

### Requirement: Two-phase candidate validation

Automata SHALL perform lightweight world-state discovery before expensive
navigation, loot, storage, operator permission, tag, or LLM validation. A
Copper Golem MUST advance no more than one discovered candidate into expensive
validation per server tick.

#### Scenario: Lightweight candidates are rejected
- **WHEN** a bounded scan encounters multiple non-air blocks that fail later target policy or permission checks
- **THEN** Automata SHALL perform expensive validation for at most one candidate during that Golem tick
- **AND** Automata SHALL resume after the examined position on a later budgeted tick

#### Scenario: Candidate commit remains authorised
- **WHEN** a candidate reaches destructive break commit
- **THEN** Automata MUST recheck loaded state, drops, capacity, owner-backed permission, and break events before changing the world

### Requirement: Throttled navigation

Automata SHALL reuse a valid active navigation path and MUST NOT normally
request a new path for the same gathering target or home more than once per ten
server ticks. Each recomputation SHALL attempt at most one selected stand
destination.

#### Scenario: Active movement continues
- **WHEN** a Copper Golem has an active path to the same target and has not become stuck
- **THEN** Automata SHALL continue that path without requesting another path

#### Scenario: Path recomputation becomes necessary
- **WHEN** the target changes
- **THEN** Automata MAY immediately request one new path to one selected loaded destination
- **WHEN** the same-target path completes, becomes invalid, or the Golem is stuck
- **AND** at least ten ticks have elapsed since the previous request
- **THEN** Automata MAY request one new path to one selected loaded destination

### Requirement: Event-scoped Copper Golem tracking

Automata SHALL use server entity load/unload events to add and remove Copper
Golems from its scheduler. It MUST NOT periodically traverse all loaded
entities to discover managed Golems, and its routine tracking cost MUST be
independent of unrelated animals and villagers.

#### Scenario: Unrelated entity pressure
- **WHEN** a level contains additional loaded villagers, animals, or other non-Copper-Golem entities
- **THEN** Automata SHALL perform no additional discovery iteration for those entities

#### Scenario: Copper Golem unloads
- **WHEN** a tracked Copper Golem unloads or is removed
- **THEN** Automata SHALL remove its scheduler entry without force-loading the entity or chunk

### Requirement: Stopped means no automation work

When transport is disabled, Automata SHALL perform no gathering scan,
candidate validation, path request, break progress, deposit, or LLM warmup.
Automata MAY perform one transition that stops existing navigation, clears a
virtual display item, and persists the `STOPPED` activity.

#### Scenario: Disabled Golem remains loaded
- **WHEN** a gathering Copper Golem is disabled and remains loaded for multiple ticks
- **THEN** after the initial stop transition Automata SHALL schedule zero gathering and LLM work for it

### Requirement: Compatible persisted state

Automata MUST preserve all existing `deadrecall_*` Copper Golem keys and item
Component serialization. Cursor/target changes, break progress, and inventory
transactions SHALL remain restart-safe, while transient path checkpoints MAY
be reconstructed after reload.

#### Scenario: Restart with a selected target
- **WHEN** a world reloads with an existing persisted gathering target and cursor
- **THEN** Automata SHALL resume that target before scanning and SHALL retain the stored cursor for later search

### Requirement: Bounded blocked-sorting rechecks

Automata SHALL retain existing blocked sorting source, binding, and destination
hash semantics but MUST apply retry backoff from the existing 10 ticks up to
200 ticks while those
hashes remain unchanged. This scheduling change MUST NOT alter exactly-once
insertion, source return, or permission ordering.

#### Scenario: Blocked inventories remain unchanged
- **WHEN** repeated due checks find the same blocked source, bindings, and destinations
- **THEN** Automata SHALL exponentially increase the next retry delay up to 200 ticks

#### Scenario: A due retry observes a route change
- **WHEN** a due blocked check finds changed inventory or binding state
- **THEN** Automata SHALL clear blocked state and allow the normal preflighted exactly-once sorting route to run
