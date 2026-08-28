## 1. Scheduling and lifecycle

- [x] 1.1 Add a deterministic shared scan-budget allocator with a 256-position
  server cap, 32-position per-Golem cap, and rotating fairness.
- [x] 1.2 Replace periodic `getAllEntities` discovery with Fabric Copper Golem
  load/unload tracking and retain a direct narrow tracking seam.
- [x] 1.3 Add deterministic controller diagnostics for tracked/ticked work
  without wall-clock assertions.

## 2. Gathering state and validation

- [x] 2.1 Make a persisted valid target take precedence over scanning and clear
  only cheaply invalid targets.
- [x] 2.2 Split cheap cursor discovery from one bounded expensive candidate
  validation step and reduce the cursor budget from 512 to 32.
- [x] 2.3 Make disabled gathering transition once to `STOPPED` and perform no
  scan, navigation, break, deposit, or LLM warmup work.
- [x] 2.4 Avoid redundant CustomData reads/writes on unchanged gathering
  activity transitions while preserving legacy keys and restart recovery.

## 3. Navigation and sorting

- [x] 3.1 Reuse active target/home paths and enforce a ten-tick normal
  recomputation interval with one destination attempt per recomputation.
- [x] 3.2 Preserve stuck-target skip recovery and clear transient navigation
  checkpoints on target completion/invalidation.
- [x] 3.3 Add exponential blocked-sorting hash retry backoff without changing
  pickup, insertion, return, or exactly-once commit semantics.
- [x] 3.4 Leave dirty-container route caching as a scoped follow-up if it cannot
  be proven without weakening transaction correctness.

## 4. Verification

- [x] 4.1 Add deterministic unit tests for zero scan planning with a target,
  per-Golem/server scan caps and fairness, one expensive validation per scan
  step, stopped zero-work planning, and navigation recomputation cadence.
- [x] 4.2 Add/update GameTests for event tracking, target persistence, loaded-only
  behavior, permission-safe gathering, sorting exactly-once, and restart state.
- [x] 4.3 Run focused JUnit tests, `./gradlew test`, the applicable Fabric
  GameTests/restart probe, and `./gradlew build --stacktrace`.
