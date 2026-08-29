## Context

TotemVanillaTweaks coordinates Observer sessions but must not own a lookalike of another module's UI.

## Goals / Non-Goals

- Goals: reuse Automata's production Screen, remain framebuffer-free and make all observer input inert.
- Non-goals: impersonate the target player or relay editable LLM configuration.

## Decisions

- Automata exposes a lazy client provider through TotemCore; the returned screen is the production `CopperGolemMenuScreen` in Observer mode.
- Observer mode suppresses menu actions, text editing, lifecycle requests and mutation packets.
- Semantic snapshots are bounded, versioned and secret-free.

## Risks / Trade-offs

- Optional provider mismatch produces an explicit unsupported screen rather than a duplicated fallback.

## Migration Plan

Ship TotemCore contract first, then Automata provider, then VanillaTweaks consumer.
