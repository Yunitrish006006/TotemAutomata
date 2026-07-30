# TotemAutomata

Optional Copper Golem automation for Totem. It depends on TotemCore only.
Cognition is an optional integration: without it, Automata uses its rules, GUI
and manual configuration without class-loading a Cognition implementation.

`0.1.6` is the current candidate built against TotemCore `0.2.0`. Its optional
portable-container safety adapter now consumes TotemRemnant's versioned API
and its rate-limited rejection diagnostics when Remnant is installed, while
retaining a vanilla fallback when it is absent. The immutable
`0.1.3` client-visual artifact remains the rollback baseline.

## Verification baseline

`./gradlew build` passes the unit suite and the Fabric GameTest suite. A clean
dedicated-server bundle containing only Fabric API, `totem-core` and
`totem-automata` was also started successfully on 2026-07-23; its log recorded
both Totem initializers and `Done`. This verifies the module has no required
DeadRecall or Cognition dependency. It is a smoke-installation baseline, not
the final Copper Golem sorting, gathering, pressure and restart qualification.
