# Ender access and golem reliability

## Why

Remnant backpacks need an explicit module for opening the player's own Ender
Chest. Copper Golems also need a non-consumable end-game fuel option, while
the live sorting route currently lacks an end-to-end commit test and can send
items back to the source after the player expected a successful sort.

The Locksmith integration has two related policy gaps. `TRUSTED` currently
accepts any non-null last-operator UUID instead of checking whether that player
is trusted by the lock, and Automata checks source and destination separately,
so it cannot recognise a transfer that stays inside one locked network.

## Changes

- Add a unique Remnant Ender Access upgrade with a live-manual recipe page and
  a vanilla-style backpack button that opens only the current player's own
  Ender Chest.
- Accept a Nether Star in the Copper Golem fuel slot as infinite fuel without
  consuming the star or finite burn state.
- Make sorting transfers commit exactly once: deposited items stay deposited,
  and only a genuine remainder may return to the remembered source.
- Add a route-aware Automata-to-Locksmith check so same-lock transfers remain
  internal and boundary transfers use an explicit automation-mode matrix.
- Require the actual wrench player's Locksmith content permission before a
  protected container can be recorded as a sorting source, sorting destination,
  or gathering home.
- Define `DENY` as no boundary automation, `TRUSTED` as Owner/Manager-operated
  automation, and `ALL` as unrestricted automation except a known Blocked
  operator.

## Impact

- Repositories: TotemRemnant, TotemAutomata, and TotemLocksmith.
- Player assets: one strict 16x16 module texture plus backpack UI and manual
  updates in English and Traditional Chinese.
- Persistence: no new inventory copy; Ender Chest data remains vanilla-owned.
  Nether Star fuel reuses the existing persisted fuel stack and preserves any
  paused finite burn counters.
- Compatibility: the infinite-fuel state must remain compatible with the
  active `add-furnace-style-copper-golem-fuel` proposal by bypassing its future
  per-tick debit while a Nether Star is installed.
