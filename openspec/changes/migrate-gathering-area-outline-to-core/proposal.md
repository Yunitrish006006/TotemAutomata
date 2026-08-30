## Why

Automata currently redraws a Copper Golem's configured gathering area by
spawning edge particles every few client ticks. TotemCore 0.7.13 now owns the
shared stateless world-outline primitive, so Automata should use the same
depth-tested selection presentation as other module-owned selections.

## What Changes

- Replace gathering-area edge particles with a module-owned render callback
  that submits the authoritative area cuboid through TotemCore.
- Use `DEPTH_TESTED` occlusion so opaque terrain hides the portions of the
  selection outline that the local player cannot see.
- Keep incomplete corner markers, selected-Golem state, request cadence,
  payload validation and cleanup inside Automata.
- Preserve non-area visualization particles for source paths, destinations,
  current targets and blocked activity.
- Raise Automata's minimum TotemCore version to 0.7.13 and pin CI/release
  validation to the published Core implementation commit.

## Impact

- Affected specs: `copper-golem-gathering-area-visualization`
- Affected code: Automata client visualization, client tests, Gradle/Fabric
  dependency metadata and CI/release pins
- No persisted data, server authority, packets, permissions or Observer
  transport semantics change.
