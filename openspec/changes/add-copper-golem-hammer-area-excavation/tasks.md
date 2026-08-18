# Tasks: Add Copper Golem Hammer Area Excavation

## 1. Capacity and optional bridge

- [ ] 1.1 Replace the hard-coded gathering capacity with one bounded
  `maxCarriedItemCount` server setting (default `16`) and update all storage
  fit/full/normalization paths to use it.
- [ ] 1.2 Extend the optional Totem Excavation bridge to read a complete
  stored hammer selection and tier behavior only when the module is loaded.
- [ ] 1.3 Prove standalone Automata still starts and accepts ordinary tools
  with Totem Excavation absent.

## 2. Area-job behavior

- [ ] 2.1 Start a Golem-owned, bounded area job only after an ordinary
  authorised hammer trigger break inside a complete same-dimension selection.
- [ ] 2.2 Scan and sort selected candidates using hammer tier behavior while
  enforcing the Golem's own area, target policy, safety, loaded-chunk and
  owner-permission constraints.
- [ ] 2.3 Plan only exact resolved drops that fit the simulated remaining
  carrying capacity and the existing one-stack/component storage contract.
- [ ] 2.4 Commit planned targets through the normal per-target event, drop,
  fuel and enchantment-aware durability transaction, with no player session
  or selection mutation.
- [ ] 2.5 Cancel jobs safely when the Golem stops, its tool/selection changes,
  the target becomes invalid, or the server restarts.

## 3. Verification

- [ ] 3.1 Add unit tests for configurable capacity and aggregate planned-drop
  limits, including partially full and incompatible carried stacks.
- [ ] 3.2 Add Fabric GameTests for a selected hammer-area batch, capacity
  truncation, exact drops, post-success fuel/durability, one-Golem-area and
  no-player-session boundaries.
- [ ] 3.3 Run JUnit, all relevant Fabric GameTests, the standalone/no-
  Excavation probe, strict OpenSpec validation and Java 25 full build.
