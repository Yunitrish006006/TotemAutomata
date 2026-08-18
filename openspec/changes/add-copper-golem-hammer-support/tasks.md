# Tasks: Add Copper Golem Hammer Support

## 1. Optional integration boundary

- [x] 1.1 Add the exact Totem Excavation 0.1.0 development artifact as an
  optional compile/test dependency and declare the runtime Fabric suggestion.
- [x] 1.2 Create an isolated hammer adapter that activates only when Totem
  Excavation is loaded and recognises all seven canonical plus retained legacy
  hammer IDs.
- [x] 1.3 Prove standalone Automata does not resolve Excavation classes or
  change ordinary gathering-tool acceptance when the optional module is absent.

## 2. Copper Golem gathering behavior

- [x] 2.1 Make menu insertion and persisted gathering validation explicitly
  accept supported hammer stacks while still rejecting the Copper Wrench and
  invalid items.
- [x] 2.2 Preserve the full stored hammer stack, including damage,
  enchantments, custom name and any selection Component.
- [x] 2.3 Route hammer targets exclusively through the existing one-target
  gathering break transaction; enforce hammer drop/tag restrictions and never
  start a player area-mining session.
- [x] 2.4 Keep normal permission, chunk, storage, fuel and post-success-only
  durability rules intact for hammer harvesting and cancellation.

## 3. Verification

- [x] 3.1 Add Fabric GameTests for all canonical and legacy hammer acceptance,
  normal tool rejection and menu-stack preservation.
- [x] 3.2 Add end-to-end GameTests proving authorised hammer harvesting,
  tag-restricted drops, enchantment/component retention, post-success tool
  durability, one-target restriction and no player excavation session.
- [x] 3.3 Run JUnit, all relevant GameTests, standalone/no-Excavation startup,
  the integration runtime, and `./gradlew build --stacktrace` with Java 25.
