## Context

The first hammer integration deliberately restricted a Copper Golem to one
target and never inspected a player's `area_selection` Component. This change
uses the selection stored on the Golem's own tool stack as a read-only area
template after an ordinary authorised trigger break. The Golem still owns its
own gathering area, storage, fuel and permission context.

`GatheringStorage` uses a shared carried-item capacity of sixteen. The storage
may contain several item/component kinds at once, but the sum of all carried
item counts must stay within that limit. Area excavation must not pre-plan or
commit more drops than the remaining shared capacity.

## Goals / Non-Goals

### Goals

- Execute Totem Excavation's selected-area behavior for a Copper Golem only
  when the Golem carries a supported hammer with a complete, same-dimension
  selection and the normal trigger target lies in it.
- Preserve hammer tier range, efficiency ordering and completion fraction for
  candidate selection, then cap the resulting batch by the remaining carried
  item capacity.
- Keep one shared carried-item limit (currently `16`) while allowing multiple
  item/component kinds to occupy that capacity.
- Resolve drops before admitting each extra target and use those exact drops
  in the existing per-target break transaction.
- Leave the stored selection unchanged and never start, resume or mutate a
  player-owned `ExcavationSessions` job.

### Non-Goals

- Changing the player's hammer selection UI, tier selection range, or player
  excavation session behavior.
- Letting a hammer selection mine outside the Copper Golem's configured area,
  target policy, loaded chunks or permission boundary.
- Turning carried storage into an unrestricted normal inventory, force-loading
  chunks, or guaranteeing an all-or-nothing transaction across several blocks.

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

### Shared-capacity mixed storage

The gathering backpack can hold several distinct `ItemStack` kinds, but the
sum of their counts cannot exceed the shared carried-item limit. It is not a
16-slot chest: sixteen one-count items already fill it, regardless of how many
stack kinds they form.

Legacy worlds that stored one `deadrecall_gathering_storage_stack` are read as
a one-entry mixed inventory and can subsequently accept additional kinds
without losing the old item. The menu exposes the carried kinds as read-only
storage slots while the Golem is running; players may remove them only through
normal stopped/editable menu rules.

### Capacity-aware target planning

For an area job, remaining capacity is the configured maximum minus the sum of
all currently carried item counts. After eligibility sorting and the hammer
tier's completion fraction, Automata resolves each candidate's exact drops and
admits it only when the cumulative count still fits. Different drop types do
not invalidate a batch merely because they differ; only capacity, target,
permission, tool and current-state checks reject them.

Thus an empty Golem with a 16-item limit can plan up to sixteen total drops
across cobblestone, coal, raw iron, raw copper or other eligible kinds. A Golem
already carrying five total items can plan at most eleven more.

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
| Mixed storage is mistaken for a normal 16-slot backpack. | Capacity is calculated by total item count; menu storage remains output-only while operating. |
| Selection overlaps forbidden Golem targets. | Intersect with the existing Golem area, safety, policy and owner-permission checks for every candidate. |
| A later capacity or block-state change invalidates a plan. | Revalidate each target immediately before committing it; stop the affected job without fuel/durability loss. |
| Optional classes break standalone startup. | Isolate the bridge and retain the no-Excavation Dedicated Server probe. |

## Migration Plan

1. Existing single-target gathering and existing stored hammers continue to
   work without tool-data migration.
2. Legacy one-stack carried storage is accepted as the first mixed-storage
   entry and is rewritten in the new slot representation without item loss.
3. The effective carried-item capacity remains sixteen, so existing balance is
   unchanged while mixed drops become possible.
4. In-flight future area jobs are ephemeral and safely disappear on a restart;
   the stored hammer, selection, fuel and carried storage remain persisted.
