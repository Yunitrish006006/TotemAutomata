## TotemAutomata 0.1.24

- Moves active Copper Golem, Copper Wrench, recipe, advancement, localization,
  and client/server payload identifiers to the canonical Totem namespace.
- Preserves existing Copper Golem and Copper Wrench saves through a one-way
  migration from legacy `deadrecall_*` persisted data to `totem_automata_*`.
- Retires active DeadRecall compatibility payloads and legacy progression
  triggers; legacy progress remains readable only for migration.
- Requires TotemCore 0.7.18 or newer within the 0.7.x compatibility line so
  legacy advancement progress is migrated before normal gameplay resumes.
- Retains optional TotemExcavation 0.1.13 integration.

Minecraft 26.2 · Fabric · Java 25
