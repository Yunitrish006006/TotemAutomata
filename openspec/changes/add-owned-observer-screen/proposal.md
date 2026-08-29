## Why

Observer viewers currently receive a separately drawn approximation of the Copper Golem UI. That duplicate renderer drifts from the owning screen and risks exposing editor secrets.

## What Changes

- Add an Automata-owned, read-only Observer mode to the real `CopperGolemMenuScreen`.
- Publish the screen through the TotemCore semantic Observer provider contract.
- Keep API keys, tokens, prompts and unsent editor text out of semantic state.
- Require visual and runtime gates for all future player-facing Automata screens.

## Impact

- Affected specs: `owned-observer-screen` (new capability).
- Affected code: Copper Golem screen/menu client integration, client entrypoint, tests and CI guidance.
