## Why

Copper Golems can already carry and single-target mine with Totem Excavation
hammers, but they do not perform the hammers' selected-area excavation. The
area operation needs its own server authority so it can be bounded by a
Golem's carried storage without borrowing a player's active session.

## What Changes

- Let a Copper Golem start a Golem-owned, bounded hammer-area job after its
  normal authorised trigger break occurs inside the complete selection stored
  on its equipped Totem Excavation hammer.
- Reuse the hammer tier's range, target ordering and completion fraction, but
  intersect every extra target with the Copper Golem's own gathering area,
  target rules, permissions, loaded chunks and safety restrictions.
- Make the gathering carried-item limit a central configurable server value
  (default `16`) rather than a hard-coded hammer limit. The selected extra
  targets' resolved drops SHALL fit within `maximum carried items - current
  carried items` and the existing one-stack/component storage rule.
- Keep all individual breaks on Automata's existing authorised drop, fuel and
  durability transaction. The stored hammer selection is read-only; no player
  excavation session or player-held stack is used.

## Impact

- Affected specs: `copper-golem-hammer-area-excavation`
- Affected code: gathering storage/configuration, optional Excavation adapter,
  gathering scheduler, break transaction and Fabric GameTests in TotemAutomata.
- Optional external module: Totem Excavation `0.1.0+`; standalone Automata
  remains valid when it is absent.
