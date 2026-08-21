## ADDED Requirements

### Requirement: Infinite Nether Star fuel

Automata SHALL accept a Nether Star in the Copper Golem fuel slot. While at
least one Nether Star remains in that slot, every Copper Golem operation that
uses the shared fuel authority SHALL have fuel available without shrinking the
stack or decrementing finite burn state.

#### Scenario: Nether Star powers repeated work

- **WHEN** a Copper Golem completes multiple sorting or gathering work cycles
  with a Nether Star in its fuel slot
- **THEN** every authorised cycle SHALL remain fuelled and the Nether Star count
  SHALL remain unchanged

#### Scenario: Denied work does not mutate fuel

- **WHEN** a Copper Golem operation is denied by target, storage, or Locksmith
  policy while a Nether Star is installed
- **THEN** the fuel stack and all finite burn counters SHALL remain unchanged

#### Scenario: Infinite fuel is removed

- **WHEN** a player removes the Nether Star after finite burn state had been
  paused
- **THEN** ordinary fuel availability SHALL resume from the preserved finite
  state without receiving fabricated burn ticks

#### Scenario: Golem reloads with infinite fuel

- **WHEN** a Copper Golem with a Nether Star in its fuel slot is saved and
  reloaded
- **THEN** it SHALL still report infinite fuel and retain the same stack count

### Requirement: Infinite fuel display

The authoritative Copper Golem snapshot SHALL distinguish infinite fuel from
zero or finite remaining ticks, and the client SHALL render a translated
infinity status or tooltip without inventing a finite burn duration.

#### Scenario: Menu displays Nether Star fuel

- **WHEN** the player opens a Copper Golem menu whose fuel slot contains a
  Nether Star
- **THEN** the menu SHALL show the stack and an infinite-fuel indicator rather
  than a zero-tick or empty-fuel state
