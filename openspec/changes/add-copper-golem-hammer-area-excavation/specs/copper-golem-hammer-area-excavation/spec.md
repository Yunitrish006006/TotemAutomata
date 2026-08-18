## ADDED Requirements

### Requirement: Capacity-aware Copper Golem hammer area excavation

When a Copper Golem completes its normal authorised hammer break at a target inside the complete, same-dimension selection stored on that hammer, Automata SHALL run a bounded Golem-owned selected-area job for eligible extra targets.
The job SHALL apply the hammer tier's range, efficiency ordering and completion
fraction, while intersecting every extra target with the Golem's configured
gathering area, target rules, safety checks, loaded chunks and owner-backed
break permission.

#### Scenario: Successful selected-area batch

- **WHEN** a Copper Golem with storage, fuel and a supported selected hammer
  completes a normal authorised trigger break within both its gathering area
  and the stored hammer selection
- **THEN** Automata SHALL process only eligible selected-area extras through
  bounded per-target gathering transactions

#### Scenario: Selection lies outside Golem authority

- **WHEN** a stored hammer selection contains positions outside the Golem's
  configured gathering area, target policy, loaded chunks or break permission
- **THEN** those positions SHALL not be scanned as breakable area-job targets
  or consume fuel/durability

### Requirement: Remaining carried-capacity limit

Automata SHALL expose the Copper Golem maximum carried item count as one
bounded server setting with a default of `16`. Before an area-job target is
admitted, Automata SHALL resolve its drops and require the aggregate planned
drops to fit both the remaining capacity (`maximum - current count`) and the
existing single item/component storage contract.

#### Scenario: Partially filled matching storage

- **WHEN** a Copper Golem has a maximum carried count of sixteen and already
  carries five matching items
- **THEN** its selected-area job SHALL plan no more than eleven additional
  matching dropped items

#### Scenario: Incompatible or overflowing drops

- **WHEN** an area candidate's resolved drops would exceed remaining capacity
  or differ from the currently carried item/components
- **THEN** Automata SHALL exclude that candidate without truncating or
  replacing its drops

### Requirement: Golem-only hammer-area lifecycle

Automata SHALL keep a Copper Golem hammer-area job separate from player
excavation sessions and player-held item state. It SHALL not mutate the stored
hammer selection and SHALL safely cancel an uncommitted job when its Golem
stops, changes tool/selection, encounters an invalid target, or the server
restarts.

#### Scenario: Player state remains untouched

- **WHEN** a Copper Golem executes extra selected-area hammer targets
- **THEN** Automata SHALL not create or resume `ExcavationSessions` for a
  player and SHALL retain the stored hammer's `area_selection` Component

#### Scenario: Rejected planned target

- **WHEN** a planned area target fails a current-state, permission, storage or
  break-event check immediately before commit
- **THEN** that target SHALL not consume fuel or hammer durability and the
  remaining uncommitted area job SHALL stop safely
