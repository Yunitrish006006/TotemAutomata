## ADDED Requirements

### Requirement: Furnace-style Copper Golem fuel lifecycle

Automata SHALL consume Copper Golem fuel as a persistent furnace-style burn
session. While a running Golem has an authorised, loaded and executable
sorting or gathering work cycle, its remaining burn time SHALL decrease by
one server tick per server tick. Automata SHALL not burn fuel while the Golem
is stopped, idle without executable work, or blocked by missing fuel, tool,
target, storage, permission or loaded destination.

#### Scenario: Active gathering burns fuel over time

- **WHEN** a running Copper Golem has a valid gathering target, a usable tool
  and a valid fuel item
- **THEN** Automata SHALL consume one fuel item to begin a burn session and
  decrement its remaining burn time on each active work-cycle tick

#### Scenario: Blocked Golem preserves fuel

- **WHEN** a Copper Golem has no authorised executable work because it is
  stopped, lacks a target, is blocked, or lacks a valid tool
- **THEN** Automata SHALL not consume a queued fuel item or decrement an
  existing burn session

### Requirement: Persistent burn meter state

Automata SHALL persist both the remaining burn ticks and the original duration
of the consumed fuel item. The authoritative Copper Golem menu snapshot SHALL
provide both values so the client can render a deterministic furnace-style
fuel meter across menu reopen, fuel-slot changes and server restart.

#### Scenario: Partially burned fuel is displayed

- **WHEN** a Copper Golem has a partially consumed burn session
- **THEN** the menu SHALL show its queued fuel stack and a lit meter whose
  fill represents remaining burn ticks divided by the original duration

#### Scenario: Legacy stored burn time

- **WHEN** an existing Copper Golem has remaining burn ticks but no stored
  original duration from an older save
- **THEN** Automata SHALL preserve that fuel availability and display a safe
  indeterminate or remaining-only fuel state until it starts a new session

### Requirement: Furnace-like fuel handoff

When an active burn session reaches zero, Automata SHALL wait until the next
executable work-cycle tick before consuming exactly one further valid fuel
item. It SHALL preserve a fuel item's crafting remainder and enter the
existing no-fuel state when no valid fuel is queued.

#### Scenario: Fuel runs out during productive work

- **WHEN** a Copper Golem exhausts its final burn tick and has no valid fuel
  item left in its fuel slot
- **THEN** its next attempted executable work cycle SHALL enter the no-fuel
  state without completing an unfuelled transaction
