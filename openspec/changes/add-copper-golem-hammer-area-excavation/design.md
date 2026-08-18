## Context

The first hammer integration deliberately restricted a Copper Golem to one
target and never inspected a player's `area_selection` Component. This change
uses the selection stored on the Golem's own tool stack as a read-only area
template after an ordinary authorised trigger break. The Golem still owns its
own gathering area, storage, fuel and permission context.

`GatheringStorage` currently has a one-stack/component storage model with a
hard-coded capacity of sixteen. Area excavation must not pre-plan or commit
more drops than the still available capacity.

## Goals / Non-Goals

### Goals

- Execute Totem Excavation's selected-area behavior for a Copper Golem only
  when the Golem carries a supported hammer with a complete, same-dimension
  selection and the normal trigger target lies in it.
- Preserve hammer tier range, efficiency ordering and completion fraction for
  candidate selection, then cap the resulting batch by the remaining carried
  item capacity.
- Expose the maximum carried item count as one bounded server setting with a
  default of `16`, so future balancing changes have one source of truth.
- Resolve drops before admitting each extra target and use those exact drops
  in the existing per-target break transaction.
- Leave the stored selection unchanged and never start, resume or mutate a
  player-owned `ExcavationSessions` job.

### Non-Goals

- Changing the player's hammer selection UI, tier selection range, or player
  excavation session behavior.
- Letting a hammer selection mine outside the Copper Golem's configured area,
  target policy, loaded chunks or permission boundary.
- Adding a multi-stack Golem inventory, force-loading chunks, or guaranteeing
  an all-or-nothing transaction across several blocks.

## Decisions

### Golem-owned bounded session

Automata will maintain a separate server-side area-job manager keyed by the
Copper Golem. It scans a maximum hammer-selection volume incrementally, keeps
only the bounded candidates/planned targets needed for the current job, and
processes extra targets using a bounded per-tick budget. The job is cancelled
without side effects when its Golem stops, loses/replaces its hammer or its
selection, or the server restarts.

The normal gathered trigger block remains an ordinary one-target break. Only
after that successful break does Automata schedule the selected-area extras.
This matches the hammer's manual-break trigger while retaining normal Golem
permission, fuel and durability semantics.

### Capacity-aware target planning

`GatheringStorage.maxCarriedItemCount()` will read a bounded server startup
setting named `totem.automata.max-carried-items` (default `16`, valid range
`1..64`). Every storage helper derives its limit from this value.

For an area job, remaining capacity is the configured maximum minus the
current carried stack count. After eligibility sorting and the hammer tier's
completion fraction, Automata resolves each candidate's drops and admits it
only when the simulated storage still accepts the exact item/component type
and the cumulative count does not exceed the remaining capacity. Thus an
empty Golem carrying a 16-item limit can plan at most sixteen matching drops;
a Golem already carrying five can plan at most eleven. Different drop types
continue to be rejected by the existing one-stack storage contract.

### Exact drops and per-target transactions

Planning passes the pre-resolved drop list into a narrow overload of
`GatheringBlockBreaker`. Immediately before each break that overload repeats
the normal current-state, permission, storage and event checks, then commits
the supplied drops through the existing fuel and enchantment-aware durability
transaction. A rejected target consumes neither fuel nor durability and is
not replaced by an unbounded retry in the same job.

### Optional-module boundary

All direct Totem Excavation Component and tier access remains inside a
dedicated optional-area bridge. Normal gathering reaches that bridge only
after Fabric confirms the optional module and a supported hammer stack. The
standalone probe continues to exercise ordinary gathering-tool acceptance
without resolving Excavation classes.

## Risks / Trade-offs

| Risk | Mitigation |
| --- | --- |
| Large valid selections can stall a server tick. | Incremental scan/planning plus bounded target work per tick; never force-load chunks. |
| Loot can vary by tool/enchantments. | Plan with the exact resolved drops and store those values for the break transaction. |
| Selection overlaps forbidden Golem targets. | Intersect with the existing Golem area, safety, policy and owner-permission checks for every candidate. |
| A later capacity or block-state change invalidates a plan. | Revalidate each target immediately before committing it; stop the affected job without fuel/durability loss. |
| Optional classes break standalone startup. | Isolate the bridge and retain the no-Excavation Dedicated Server probe. |

## Migration Plan

1. Existing single-target gathering and existing stored hammers continue to
   work without data migration.
2. Servers retain the former effective capacity through the default setting of
   `16`; operators can later alter one startup setting for balance changes.
3. In-flight area jobs are ephemeral and safely disappear on a restart; the
   stored hammer, selection, fuel and carried storage remain persisted.
