# Design: Furnace-style Copper Golem fuel

## Context

`CopperGolemFuelService` currently consumes one fuel item on demand, then
deducts `200` stored burn ticks for a completed transport transaction. The
stored remaining ticks are persisted, but no original burn duration is kept
for a UI meter.

## Decisions

### 1. Burn only during an executable work cycle

Each server tick, a running Copper Golem burns one fuel tick only when it has
an authorised, loaded and actionable unit of sorting or gathering work. This
includes travelling toward or executing that selected work. It does not burn
while stopped, waiting with no valid job, or in a blocked state. This matches
the furnace principle that fuel burns only while there is valid work to do,
without charging a player for an unavailable target or failed permission.

### 2. Start and continue fuel like a furnace

When an executable cycle needs fuel and no remaining burn ticks exist, consume
exactly one valid item from the fuel slot, preserve any crafting remainder,
and initialise remaining ticks from the server fuel values. Decrement the
remaining value once for that same tick. When it reaches zero, the next
executable tick starts the next item; if none exists, transition to the
existing no-fuel state.

### 3. Persist a burn-duration pair

Persist `remainingBurnTicks` and `burnDurationTicks` together. The latter is
the duration of the already-consumed item and is never inferred from a fuel
stack that the player may subsequently replace. Existing persisted remaining
ticks without a duration remain usable; display an indeterminate/remaining
state until the next fuel item starts a new session.

### 4. Authoritative UI state

The snapshot adds original burn duration alongside remaining ticks. The menu
renders the queued fuel stack, a lit/unlit furnace-style meter, and an
accessible numeric remaining/total tooltip. The test-only visual prototype
is replaced by production rendering only after the layout is accepted.

### 5. Transaction separation

Fuel eligibility remains a precondition for starting a transaction. Fuel
burning is advanced by the Golem work loop, not by a successful block break
or inventory transfer. A failed target must not independently charge an
extra fuel unit; any fuel spent before a late transaction rejection is only
the ordinary elapsed active-cycle tick.

## Risks and mitigations

- **Fuel economy changes:** tick-based draining can differ substantially from
  the old 200-tick debit. Cover coal, short-lived fuel, idle, blocked,
  traversal and restart cases with GameTests.
- **Client desync:** put both values in the authoritative payload and clamp
  render percentages, rather than deriving state client-side.
- **Legacy data:** preserve existing remaining ticks and introduce the total
  duration only when a new fuel item is consumed.
