## Why

Copper Golem gathering currently performs expensive target validation and path
creation on the server thread every tick, while controller discovery scans every
loaded entity once per second. In populated villages this makes unrelated
animals and villagers appear stalled because all entity simulation shares the
same server tick.

## What Changes

- Replace full-level entity discovery with Fabric Copper Golem load/unload
  tracking.
- Give gathering searches a fair server-wide budget with a strict per-Golem
  cap, and separate cheap candidate discovery from expensive validation.
- Keep a selected gathering target stable until it is completed or invalid,
  without scanning for a replacement every tick.
- Reuse active navigation and throttle path recomputation for target and home
  movement.
- Make stopped Copper Golems perform no scan, path, break, deposit, or LLM
  warmup work.
- Avoid redundant persisted-state writes on unchanged activity transitions and
  apply bounded backoff to blocked sorting rechecks without changing transfer
  commit semantics.
- Add deterministic scheduler and instrumentation regression coverage.

## Impact

- Affected specs: `copper-golem-work-scheduler` (new capability).
- Affected code: Copper Golem lifecycle registration/controller, gathering
  behavior/scanner/navigation/deposit, persisted runtime data access, and
  blocked sorting retry scheduling.
- Existing `deadrecall_*` NBT keys, operator-backed permissions, loaded-only
  behavior, storage limits, and exactly-once sorting commits remain compatible.
- The active hammer, fuel, Locksmith, and sorting reliability changes retain
  their safety and transaction constraints.
