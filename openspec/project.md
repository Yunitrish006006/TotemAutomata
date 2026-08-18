# Project Context

## Purpose

TotemAutomata turns vanilla Copper Golems into server-authoritative sorting and
gathering helpers. It owns their persisted configuration, player-authorised
interactions, safe target selection, drops, fuel and tool durability.

## Tech Stack

- Java 25, Fabric Loader 0.19.3+, Minecraft 26.2 and Fabric API 0.154.2+26.2
- modern-yarn mappings and Fabric Loom 1.17.12
- TotemCore exactly 0.5.0; optional Totem modules are isolated integrations

## Project Conventions

### Code Style

- Keep server authority and persisted Copper Golem schema logic under
  `dev.totem.automata.copper`.
- Treat player packets and client state as untrusted; validate the player,
  golem, dimension, target and current state on the server.
- Preserve existing persisted `deadrecall_*` keys until a separately approved
  migration retires them.

### Architecture Patterns

- `PersistedGatheringBehavior` owns scan and activity state; injected world
  operations perform navigation, harvesting and deposits.
- Optional integrations must not prevent standalone Automata from starting;
  isolate their classes and only activate them when the providing mod is
  installed.
- A Copper Golem's configured gathering area is independent from a player's
  item-owned selection state.

### Testing Strategy

- Use JUnit for deterministic pure logic and Fabric GameTests for runtime
  registration, persistence, gathering and Dedicated Server behavior.
- Run `./gradlew build --stacktrace` with Java 25 before handoff. Client
  visual tests are required only when client UI or rendering changes.

### Git Workflow

- Keep changes focused; do not commit, publish or deploy without explicit
  user instruction.
- Keep OpenSpec changes active until deployment validation is complete.

## Domain Context

- Gathering uses a golem's own configured area, fuel, storage and owner-backed
  break permission. It intentionally does not impersonate a player.
- A valid gathering tool is stored as one full `ItemStack`, including damage,
  enchantments and Components. Drops and durability update only after a
  successful server-side break.

## Important Constraints

- Never force-load chunks, bypass player break permissions, duplicate drops or
  consume fuel/durability for a rejected break.
- A Totem Excavation hammer must retain its normal target/drop constraints;
  its player-only area session MUST NOT run from a Copper Golem.

## External Dependencies

- Required: `totem-core =0.5.0`.
- Optional: `totem-excavation >=0.1.0` for explicit hammer support; existing
  standalone Automata installations remain valid without it.
