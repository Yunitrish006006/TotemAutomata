# Copper Golem inventory conservation verification

Validated on 2026-09-09 with Java 25, Minecraft 26.2, Fabric Loader 0.19.3,
Fabric API 0.154.2+26.2, TotemCore 0.7.18 and TotemExcavation 0.1.13.

`CopperGolemSlotContainer` retains mutable live stacks and commits their changes
to the existing persisted authority. Storage compaction does not change live slot
indices during vanilla click iteration. External authority changes refresh the
affected contents without restoring stale state in unrelated slots.

The registered `CopperGolemInventoryGameTest` exercises the actual Minecraft
menu click dispatcher with the persisted authority. Its 11 cases cover partial
and full fuel/storage shift transfers, fuel merging, left/right pickup and place,
pickup-all, stable storage indices, autonomous updates, reopening compacted
storage, damaged-tool preservation and running-gathering extraction denial.
Mock inventories are explicitly cleared after login to give these tests a
controlled initial state.

Reproduce from the module directory:

```sh
env JAVA_HOME=/home/thomas/.local/temurin-25 ../TotemCore/gradlew \
  --gradle-user-home /tmp/totem-gui-fix-automata-gradle \
  --project-cache-dir /tmp/totem-gui-fix-automata-cache \
  build runGameTest --offline
```

The final run completed `build` successfully: 85 JUnit tests in 38 suites and all
50 required server GameTests passed, with no failures or skipped JUnit tests.
Local output is
in `/tmp/totem-gui-diagnosis/automata-final-validation.log`; the earlier fixture
failure is retained in `automata-fixture-failure.log` in the same directory.

These tests call the menu directly with a mock server player. They validate
inventory behavior and the existing running-gathering edit gate, not packet-level
wrench authorization or a connected-client interaction. No Screen/Menu layout,
Observer provider, payload, authority API or persistent schema changes are part
of this fix. Client screenshots and production/three-JVM probes were not rerun.
