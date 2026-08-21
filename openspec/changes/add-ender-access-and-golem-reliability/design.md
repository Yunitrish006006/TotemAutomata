# Design: Ender access and golem reliability

## Context

Remnant already stores unique functional upgrades in server-backed backpack
slots. Its menu can therefore validate a module before handling a standard
menu button action. Minecraft already owns each player's Ender Chest inventory,
so the module should open that inventory directly instead of mirroring it into
the backpack.

Automata currently treats only vanilla furnace fuels as valid and debits a
fixed amount after a successful operation. Its sorting mixin tests the pickup
half of the live vanilla behavior but not a complete pickup, destination
deposit, memory clear, and later-tick sequence.

Locksmith receives Automata's persisted last-operator UUID. The current policy
equates any identified UUID with trusted automation, even when the UUID is not
the lock Owner or a Manager. It also exposes a route-aware `mayTransfer` API,
but Automata calls only the separate `mayExtract` and `mayInsert` endpoints.

## Decisions

### 1. Open vanilla Ender storage

The Ender Access upgrade is a normal unique Remnant upgrade. Its shaped recipe
uses an Ender Chest in the centre, Ender Pearls on the four cardinal positions,
and Iron Ingots in the corners. The recipe is included in the server-synchronised
Remnant manual rather than duplicated as a hard-coded illustration.

When installed, the backpack screen shows one vanilla-style 20x18 action button
beside the main container, with an Ender-themed glyph and translated tooltip.
The client sends only the menu button ID. The server then revalidates that the
player still owns the open backpack menu, still holds the tracked backpack, and
still has the upgrade installed. It opens a three-row vanilla chest menu backed
by `ServerPlayer.getEnderChestInventory()`. A stale or forged action does nothing.

Opening Ender storage closes the backpack through the normal menu lifecycle, so
any transient crafting-grid items follow the existing safe return behavior. No
Ender contents are copied into backpack components, and another player's Ender
inventory is never accepted as input.

### 2. Treat Nether Star as an infinite source

The shared fuel authority recognises a Nether Star in addition to ordinary
fuel values. While the slot contains a Nether Star, every fuel-gated Copper
Golem mode has fuel available, the star count never changes, and finite burn
counters do not decrement or reset. Removing the star resumes the preserved
finite state, if one existed.

The authoritative menu snapshot carries an explicit infinite-fuel flag. The
client renders an infinity status/tooltip rather than zero ticks. This flag is
also the compatibility seam for the pending furnace-style fuel lifecycle:
infinite fuel bypasses active-tick burn without fabricating a burn duration.

### 3. Commit sorting as one transfer

Sorting is modelled as a server-authoritative in-flight transfer:

1. Preflight at least one loaded, category-compatible, capacity-compatible and
   Locksmith-authorised route before removing an item or consuming fuel.
2. Persist the actual source position and slot when pickup succeeds.
3. Revalidate a selected destination immediately before mutation.
4. Insert once and replace the Golem's carried stack only with the returned
   remainder.
5. If the remainder is empty, clear source and tried-destination state before
   vanilla can search again.
6. If every destination becomes invalid, return only the still-carried
   remainder to the remembered source.

A successful destination mutation can never be reconstructed from the original
picked count. Tests exercise the transformed live mixin through a later target
resolution, because controller-only tests cannot catch lifecycle regressions.

### 4. Authorise the complete route

For sorting, Automata uses Locksmith's route-aware transfer API whenever both
source and candidate destination exist. This preserves the established rule
that two endpoints with the same Lock UUID are an internal transfer and do not
cross the protected boundary. Separate endpoint checks remain appropriate for
gathering deposits, which have no locked container source.

The boundary matrix is:

| Mode | Operator | Result |
| --- | --- | --- |
| DENY | Any or none | Deny |
| TRUSTED | Lock Owner or Manager | Allow |
| TRUSTED | User, Blocked, unknown, or none | Deny |
| ALL | Known Blocked operator | Deny |
| ALL | Any other identified operator or none | Allow |

AccessMode, friendship, Public access, and held keys remain player-interaction
grants and do not make a Golem trusted. The operator UUID is the last player
recorded by a successful server-side Copper Wrench interaction; clients cannot
choose the UUID. A stranger can overwrite that last-operator value by legally
interacting with an otherwise unrestricted Golem, which can pause TRUSTED work
but cannot grant access to a lock they do not own or manage.

### 5. Check wrench binding authority

Container binding is a player action, not an automation action. Automata passes
the actual `ServerPlayer` through a separate optional Locksmith player-access
bridge so Locksmith can evaluate physical keys, the physical-key game rule,
Owner/Manager/User roles, Blocked, friendship, Public mode, and administrator
bypass without trusting a client-provided UUID.

Before mutating Golem binding data:

- a sorting source requires `EXTRACT` permission;
- a sorting destination requires `INSERT` permission; and
- the copper source used as a gathering home requires `INSERT` permission.

The check runs before binding mutation, path particles, advancement criteria,
operator reassignment, or any inventory inspection. A denied attempt produces
a translated denial message. Removing an existing binding remains a Golem
configuration cleanup and does not inspect or mutate the protected container,
so it does not require container content permission.

### 6. Fail closed without losing items

When Locksmith is installed but its API is missing or throws, Automata denies
the pending mutation. Permission changes between pickup and deposit leave the
item carried or return it only through an authorised source insertion; they do
not duplicate, silently delete, or debit fuel for a denied preflight.

## Risks

- Changing `DENY` to reject Owner/Manager boundary automation is stricter than
  the current implementation. The explicit three-mode matrix and pairwise tests
  make the player-facing distinction predictable.
- A menu transition can expose crafting-lifecycle bugs. The Ender button tests
  cover both empty and populated crafting grids.
- Sorting targets can change after pickup. Partial-capacity and mid-flight
  permission changes are tested independently from the normal success path.
- The Remnant worktree already contains pending backpack UI and compaction
  fixes; implementation must preserve and build on those changes.

## Verification

- Unit tests for Locksmith's complete mode/operator matrix.
- Pairwise GameTests with Locksmith present for same-lock, different-lock,
  unlocked, Owner, Manager, User, Blocked, unknown, and anonymous routes.
- Wrench GameTests for authorised and denied sorting-source, destination, and
  gathering-home bindings, including no operator/criterion mutation on denial.
- Live transformed sorting GameTests for full deposit, partial remainder,
  rejected route, and a later target-resolution tick.
- Fuel GameTests across sorting and gathering, removal, restart, and ordinary
  fuel regression.
- Remnant server/menu tests plus real client screenshots at supported GUI scales
  in English and Traditional Chinese.
- Strict 16x16 validation for the new module texture and strict OpenSpec
  validation before implementation begins.
