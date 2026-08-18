# Furnace-style Copper Golem fuel

## Why

Copper Golems currently debit a fixed 200 fuel ticks only after each successful
transport or gathering transaction. This does not read or behave like a
vanilla furnace: the player cannot see a burn meter, and fuel use is tied to
individual outcomes rather than an active work cycle.

## What Changes

- Replace fixed per-transaction fuel debits with a persistent furnace-style
  burn session that decrements once per server tick while a Copper Golem has
  an executable gathering or sorting work cycle.
- Do not burn fuel while the Golem is stopped, idle without a valid job, or
  blocked by missing fuel, tool, target, permission, storage, or a loaded
  destination.
- Persist both remaining and original burn duration so clients can render a
  deterministic fuel bar, including after a restart or after the player
  changes the queued fuel stack.
- Extend the authoritative menu snapshot and formal menu UI with the fuel
  slot, burning state, and furnace-style remaining-burn indicator.

## Impact

- Affects shared Copper Golem fuel accounting for sorting and gathering.
- Extends persisted data and the menu network payload compatibly.
- Replaces fixed per-success fuel tests with server-tick fuel lifecycle tests.
