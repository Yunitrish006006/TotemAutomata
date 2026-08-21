## ADDED Requirements

### Requirement: Preflighted sorting route

Before removing a source item or consuming fuel, Automata SHALL identify at
least one loaded destination that accepts the item by category, has compatible
capacity, and is authorised for the complete Locksmith route. It SHALL
revalidate the selected destination immediately before insertion.

#### Scenario: No destination is eligible

- **WHEN** no bound destination passes category, capacity, loading, and
  Locksmith route checks
- **THEN** the source inventory and fuel SHALL remain unchanged

#### Scenario: Destination becomes denied in flight

- **WHEN** a previously eligible destination fails revalidation after pickup
- **THEN** Automata SHALL not mutate that destination and SHALL retain or safely
  return the carried stack without duplicating it

### Requirement: Exactly-once sorting commit

Automata SHALL commit each destination insertion exactly once. Successfully
inserted items SHALL remain in the destination, the Golem's carried stack SHALL
contain only the actual insertion remainder, and completed source memory SHALL
be cleared before a later target search can return items.

#### Scenario: Complete insertion succeeds

- **WHEN** a destination accepts the entire carried stack
- **THEN** the destination SHALL gain that stack exactly once, the source SHALL
  remain reduced, the Golem hand SHALL be empty, and remembered transfer state
  SHALL be cleared

#### Scenario: Partial insertion succeeds

- **WHEN** a destination accepts only part of the carried stack
- **THEN** the accepted amount SHALL remain in the destination and only the
  unaccepted remainder MAY be offered to another destination or returned to the
  remembered source

#### Scenario: Later behavior tick runs

- **WHEN** vanilla transport target resolution runs after a complete insertion
- **THEN** it SHALL NOT reconstruct or return the previously deposited stack to
  the source
