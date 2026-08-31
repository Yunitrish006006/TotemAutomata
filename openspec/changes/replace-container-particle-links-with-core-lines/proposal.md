## Why

The selected Copper Golem currently communicates its source and destination
container relationships by respawning a bounded series of particles every few
client ticks. Minecraft 26.2 already provides stable solid line gizmos, and the
shared Core world-outline contract should expose that primitive so feature
modules do not each own a custom line renderer.

## What Changes

- Add a stateless, client-only two-point line submission helper to TotemCore's
  existing world-outline API, reusing its immutable colour, width and explicit
  occlusion style.
- Replace Automata's source/destination particle links with solid Core lines
  submitted from Automata's existing selected-Golem render callback.
- Use depth testing so terrain hides occluded line portions.
- Preserve the blocked-state smoke and current gathering-target particle
  markers, which communicate different runtime state.
- Preserve Automata's selected-Golem identity, server-authoritative snapshot,
  dimension checks and cleanup lifecycle.

## Impact

- Affected specs: `copper-golem-container-link-visualization`
- Affected repositories: TotemCore public client API and TotemAutomata client
  visualization/tests/documentation
- No persisted data, packets, permissions, server behavior, Screen/Menu or
  Observer semantics change.
