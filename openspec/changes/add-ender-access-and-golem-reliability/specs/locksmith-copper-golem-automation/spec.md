## ADDED Requirements

### Requirement: Copper Golem automation matrix

For a transfer that crosses a locked-network boundary, Locksmith SHALL apply
the following Copper Golem policy: `DENY` rejects every operator; `TRUSTED`
allows only an identified lock Owner or Manager; and `ALL` allows identified or
anonymous automation except that a known Blocked operator remains denied.
Player AccessMode, friendship, Public access, User membership, and held-key
rules SHALL NOT independently authorise Copper Golem automation.

#### Scenario: Trusted Owner operates the Golem

- **WHEN** a Copper Golem identified with the lock Owner crosses the lock
  boundary while automation mode is TRUSTED
- **THEN** the requested insertion or extraction SHALL be allowed

#### Scenario: Trusted Manager operates the Golem

- **WHEN** a Copper Golem identified with a Manager crosses the lock boundary
  while automation mode is TRUSTED
- **THEN** the requested insertion or extraction SHALL be allowed

#### Scenario: Unknown identified operator uses TRUSTED

- **WHEN** a Copper Golem carries a non-null operator UUID that is neither the
  lock Owner nor a Manager while automation mode is TRUSTED
- **THEN** the operation SHALL be denied before source, destination, or fuel
  mutation

#### Scenario: Owner uses DENY

- **WHEN** even the lock Owner operates a Copper Golem across the lock boundary
  while automation mode is DENY
- **THEN** the automation operation SHALL be denied

#### Scenario: Blocked operator uses ALL

- **WHEN** an identified Blocked player operates a Copper Golem while automation
  mode is ALL
- **THEN** the operation SHALL remain denied

#### Scenario: Anonymous Golem uses ALL

- **WHEN** a Copper Golem has no recorded operator and crosses a lock boundary
  while automation mode is ALL
- **THEN** the operation SHALL be allowed

### Requirement: Same-lock internal transfer

Automata and Locksmith SHALL evaluate a sorting source and destination as one
route. When both endpoints resolve to the same active Lock UUID, the transfer
SHALL be treated as internal to that protected network and SHALL follow normal
sorting behavior regardless of DENY, TRUSTED, or ALL.

#### Scenario: Same-lock sorting in DENY

- **WHEN** a Copper Golem sorts from one container to another container in the
  same active locked network while automation mode is DENY
- **THEN** the route SHALL be allowed without exposing either endpoint across
  the lock boundary

#### Scenario: Different locked networks

- **WHEN** source and destination resolve to different Lock UUIDs
- **THEN** Locksmith SHALL require both source extraction and destination
  insertion to pass the boundary automation matrix before any mutation

#### Scenario: Optional bridge fails

- **WHEN** Locksmith is loaded but its automation API is missing or throws
- **THEN** Automata SHALL deny the pending route without item or fuel mutation

### Requirement: Authorised wrench binding

Before recording a protected container with the Copper Wrench, Automata SHALL
ask Locksmith to evaluate the actual server player's content permission. A
sorting source SHALL require `EXTRACT`; a sorting destination and the copper
source used as a gathering home SHALL require `INSERT`. The decision SHALL use
player access rules rather than the automation-mode matrix.

#### Scenario: Authorised sorting source binding

- **WHEN** a player with EXTRACT permission uses the Copper Wrench to set a
  protected copper source for a Golem in sorting mode
- **THEN** Automata SHALL record the source and may emit its normal successful
  binding feedback, path visual, and advancement criterion

#### Scenario: Authorised destination binding

- **WHEN** a player with INSERT permission uses the Copper Wrench to add a
  protected sorting destination
- **THEN** Automata SHALL record the destination normally

#### Scenario: Authorised gathering home binding

- **WHEN** a player with INSERT permission sets a protected copper source as a
  Golem's gathering home
- **THEN** Automata SHALL record the home normally

#### Scenario: Binding permission is denied

- **WHEN** Locksmith denies the required player operation or its installed
  player-access API fails
- **THEN** Automata SHALL show translated denial feedback and SHALL NOT mutate
  bindings, last-operator identity, criteria, inventory contents, or path visuals

#### Scenario: Existing binding is removed

- **WHEN** a player removes an existing Golem binding without reading or
  mutating the protected container
- **THEN** Automata MAY perform that configuration cleanup without a new
  container content-permission check
