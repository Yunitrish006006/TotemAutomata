## Context

`PersistedGatheringBehavior` already owns Copper Golem scans, movement, fuel,
storage and per-target breaking. `GatheringBlockBreaker` resolves drops with
the stored `ItemStack`, applies permission hooks, commits a transaction, and
damages the tool after a successful break. Totem Excavation hammers expose
their normal tool semantics from the `ItemStack`, but their selected area and
player session are deliberately player-owned.

## Goals / Non-Goals

### Goals

- Let a Copper Golem use every canonical `totem:excavation/*_hammer` and each
  retained `blossom:*_hammer` alias as a gathering tool when Totem Excavation
  is installed.
- Retain normal speed, drop eligibility, enchantments, Components and
  durability semantics for the stored hammer.
- Keep Automata functional when Totem Excavation is absent.

### Non-Goals

- Starting or sharing a player's `area_selection` component or excavation
  session.
- Letting a golem mine outside its configured gathering area, force chunks,
  bypass permissions, or bulk-break targets in a single tick.
- Making Totem Excavation a mandatory dependency of TotemAutomata.

## Decisions

### Optional, isolated dependency

Automata will compile and test against the exact local Totem Excavation 0.1.0
development artifact, declare it as an optional Fabric suggestion, and load
the adapter only when Fabric reports the module is present. The normal
gathering path continues to work with non-hammer tools in a standalone
Automata installation.

### Reuse the existing gathering transaction

The adapter only answers whether a stack is a supported hammer and exposes
diagnostic metadata. Target validation, break progress, permissions, drops,
tool damage, fuel and storage remain in Automata's existing server-authority
path. This means an Automation break stays one validated target at a time.

### Preserve alias stacks without player migration

Legacy aliases are accepted while Totem Excavation retains them. A Copper
Golem does not claim or replace the stack, because the player-only lazy
migration belongs to Totem Excavation and replacement could discard an
otherwise valid stored Component. Newly supplied stacks are expected to be
canonical.

## Risks / Trade-offs

| Risk | Mitigation |
| --- | --- |
| Optional module classes break standalone startup. | Isolate class loading behind Fabric presence and GameTest both present/absent configurations. |
| Bulk mining bypasses protection or fuel rules. | Reuse the existing per-target permission and transaction path; never call a hammer player session. |
| A legacy alias loses Components. | Accept the original stack unchanged and test custom Components/damage preservation. |
| Future hammer API drift silently changes behavior. | Pin the development artifact and test canonical plus legacy IDs; update through a separate OpenSpec for incompatible API changes. |

## Migration Plan

1. Ship Automata hammer support as optional with no Copper Golem data-schema
   migration.
2. Existing generic tools and stored stacks continue unchanged.
3. While Totem Excavation retains aliases, a stored legacy hammer is usable
   without conversion.
4. When Totem Excavation later removes aliases, update this adapter only in
   that module's separately approved compatibility change.
