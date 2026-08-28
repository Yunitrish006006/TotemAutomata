## Context

`PersistedGatheringBehavior` currently scans before consuming its persisted
target, so a valid target can be replaced every server tick. Each scan may
inspect 512 positions and performs navigation destination generation, loot
resolution, storage decoding, permission checks, tag collection, and LLM
request preparation for candidates. Movement then calls `moveTo` repeatedly,
which creates new paths. Separately, `CopperGolemController` traverses
`ServerLevel#getAllEntities()` every second, making cost scale with villagers,
animals, and every other loaded entity.

Fabric API 0.154.2+26.2 includes
`ServerEntityEvents.ENTITY_LOAD` and `ENTITY_UNLOAD`. Automata registers before
world entities load, so these events are the primary lifecycle authority. A
direct `track` seam remains available for tests and same-tick state mutations;
no periodic all-entity reconciliation is needed.

## Goals / Non-Goals

- Goals:
  - Bound server-thread search and path work independently of configured area
    volume and unrelated entity count.
  - Preserve stable target, break permission, loaded chunk, storage, drops,
    durability, and restart behavior.
  - Make scheduler regressions testable with operation counts rather than
    wall-clock timing.
- Non-Goals:
  - Change gathering yields, tools, hammer selection rules, fuel economy, or
    sorting transfer commit order.
  - Force-load chunks or add asynchronous world access.
  - Introduce a new persisted schema version or retire legacy NBT keys.

## Decisions

### Event-driven Copper Golem tracking

The lifecycle registration subscribes to Copper Golem entity load/unload
events. The controller stores only loaded Copper Golem UUIDs and dimensions;
it never enumerates all entities. It checks the behavior predicate before doing
managed work, allowing stopped/unconfigured Golems to remain cheap and become
active without a world scan.

### Shared fair search budget

Each controller tick creates one 256-position server budget. Managed Golems are
visited from a rotating start index and a searching Golem can claim at most 32
positions. Sorting Golems and Golems with a current target do not claim search
budget. Rotation prevents a stable map order from starving later Golems when
more than eight searchers are active.

Search uses two phases. The cursor performs only loaded-area, block-state, home,
bound-container, safety, and container exclusion checks. At most one discovered
candidate per Golem tick advances to navigation feasibility, drops, capacity,
operator permission, target policy, and optional LLM classification. A rejected
candidate leaves the cursor after that position and resumes later.

### Stable target state

A persisted target takes precedence over search. A cheap validity guard clears
it only when it is outside the configured area, unloaded, air/liquid,
unbreakable/unsafe, or now a protected container. Otherwise the behavior moves
or works on that exact target. Full break permission, drops, and capacity are
rechecked immediately before the destructive commit.

The existing activity identifiers represent the scheduler states:
`STOPPED`, `SEARCHING`, `MOVING_TO_TARGET`, `WORKING`, `RETURNING_HOME`,
`DEPOSITING`, and the existing `BLOCKED_*` backoff states. No incompatible NBT
state is introduced.

### Navigation throttle

Target and home movement retain an in-memory per-entity navigation checkpoint.
An active path is reused. Normal recomputation is no more frequent than once
per ten server ticks and each recomputation attempts one chosen destination,
not every stand position. Target changes, invalid targets, completed paths, or
stuck detection can invalidate the checkpoint. Persisted target/activity data
remains authoritative across restart; losing the transient path checkpoint only
causes one fresh path computation after reload.

### Persistence and stopped behavior

Hot-loop movement bookkeeping stays transient; NBT is written on target/cursor
changes, break progress, inventory transaction commits, and actual activity
transitions. Existing keys and item component codecs remain unchanged.

When transport is disabled, gathering returns before search, validation,
navigation, deposit, break progress, and LLM warmup. A one-time transition may
stop navigation, clear the displayed virtual item, and persist `STOPPED`.

### Sorting backoff

Blocked sorting retains its existing source/binding/destination hashes and
exactly-once transaction flow. Only the hash recheck cadence changes: an
unchanged block doubles its retry delay from the existing 10 ticks up to 200
ticks. A state
change clears blocked state on the next due check. Permission-denied sources
are re-authorized without reading their inventory.

## Risks / Trade-offs

- A large search area takes longer to cover because search work is intentionally
  bounded. Rotating global allocation prevents starvation and preserves TPS.
- Entity lifecycle event failure would leave a Golem unticked. Registration is
  performed during mod initialization before worlds load, and explicit `track`
  remains a narrow recovery seam without scanning unrelated entities.
- Delayed blocked sorting retries can take up to ten seconds after an inventory
  change. This is preferable to repeated full-container hashing; explicit
  configuration changes already clear/reset relevant state.
- A transient navigation checkpoint is not persisted. The persisted target is,
  so restart recovery safely computes one new path without losing work state.

## Migration Plan

No data migration is required. Existing worlds retain all `deadrecall_*` target,
cursor, activity, binding, fuel, tool, storage, and sorting transaction keys.
Rollback restores the old scheduler and ignores any additive retry timing keys.

## Open Questions

- A future change may replace polling hashes with container dirty notifications
  and an item-signature/binding-revision route cache. That requires broader
  integration coverage and is not necessary to preserve correctness here.
