## ADDED Requirements

### Requirement: Optional Totem Excavation availability

TotemAutomata SHALL remain independently startable without Totem Excavation,
and SHALL activate its Copper Golem hammer adapter only when a compatible
Totem Excavation module is installed.

#### Scenario: Standalone Automata server

- **WHEN** a Dedicated Server loads TotemAutomata and TotemCore without
  Totem Excavation
- **THEN** Automata SHALL start without resolving a Totem Excavation class and
  ordinary gathering tools SHALL retain their existing behavior

#### Scenario: Compatible Excavation module is present

- **WHEN** a Dedicated Server loads TotemAutomata with Totem Excavation 0.1.0
  or a compatible later release
- **THEN** the hammer adapter SHALL be active and recognise its supported
  hammer stacks

### Requirement: Copper Golem hammer tool acceptance

The Copper Golem gathering tool slot and persisted tool validation SHALL
accept all canonical `totem:excavation/<tier>_hammer` items and each retained
`blossom:<tier>_hammer` alias, while retaining the existing Copper Wrench and
invalid-item rejection rules.

#### Scenario: Canonical hammer inserted

- **WHEN** a player inserts a canonical Totem Excavation hammer into a stopped
  Copper Golem's editable gathering tool slot
- **THEN** the slot SHALL retain exactly one stack with all its Components

#### Scenario: Legacy hammer remains stored

- **WHEN** a Copper Golem already stores a retained Blossom hammer alias
- **THEN** Automata SHALL accept and use that exact stack without replacing or
  discarding its damage, enchantments, custom name or selection Component

### Requirement: Bounded hammer harvesting

When a Copper Golem holds a supported hammer, Automata SHALL use its existing
server-authoritative per-target gathering transaction and SHALL honour the
hammer's normal mining speed and drop eligibility.

#### Scenario: Eligible target is harvested

- **WHEN** an authorised Copper Golem reaches an eligible loaded target in its
  configured gathering area with storage and fuel available
- **THEN** it SHALL harvest through the normal permission, drop, storage and
  post-success durability path

#### Scenario: Ineligible hammer target

- **WHEN** a configured gathering target is not eligible for the held hammer's
  normal drop predicate
- **THEN** Automata SHALL not break the target, consume fuel or apply hammer
  durability

### Requirement: Player excavation isolation

Copper Golem hammer use SHALL NOT create, read, mutate or resume a player's
Totem Excavation area selection or player excavation session.

#### Scenario: Golem mines a selected target

- **WHEN** a Copper Golem breaks a target with a hammer
- **THEN** it SHALL affect only the one validated gathering target and retain
  the player's separate hammer selection unchanged
