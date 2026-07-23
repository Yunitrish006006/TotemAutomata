# Extraction contract

TotemAutomata will own Copper Golem items, registries, menus, payloads, client
screens and visualization, Mixins, persistence and restart tests. Cognition may
register an optional natural-language interpreter adapter; it is never a
required dependency.

## Migration baseline

The repository now contains a standalone Fabric 26.2 build scaffold with
`totem-core` as its only Totem dependency.  The entry points intentionally do
not register Copper Golem gameplay yet: registration moves only with the
matching implementation, payloads, resources and GameTests, so the
compatibility bundle cannot double-register an identifier during the cutover.

Migration proceeds in this order:

1. Move the Copper Wrench transactional unit together: its item, menu, item and
   menu registrations, criterion, all sorting/gathering state, lifecycle hooks,
   payload codecs and server receivers.  The handler is deliberately not split
   from this unit because it owns the stable `deadrecall_*` ItemStack data,
   state revision and server-side authority checks.
2. Replace the current direct dependency on DeadRecall's backpack and portable
   container policy with an optional versioned `TotemContainerSafety` contract,
   while retaining a conservative no-container-safety fallback; then migrate
   the entity and portable-container Mixins.  This policy remains outside Core
   because it is container gameplay, not shared platform infrastructure.
3. Move client screens and visualization once their payload contracts move.
4. Copy GameTests and the restart probe with the server implementation.
5. Run sorting, gathering, stress, restart and assembled-bundle validation.

The first migration unit is intentionally larger than item/menu registration:
the current `CopperGolemWrenchHandler` is the 4,056-line authority boundary for
the Wrench, and it directly owns menu opening, packets, criterion triggering,
sorting/gathering persistence and component-safe backpack handling.  Moving
only its registry would create a second item with none of those guarantees.

## Migrated primitives (not yet cut over)

Automata now owns independently tested implementations of mode/activity IDs,
binding and NBT migration codecs, sorting binding persistence, fuel tag
accounting, request gates and canonical LLM query keys.  It also owns the
generic chat transport, JSON decision parser, item/block classifiers with
server-authoritative decision sinks, a behavior-injected golem controller, the
portable-container bridge, and the first serverbound payload codecs/receiver
registration contract.  None is registered from the module entrypoint yet:
the compatibility bundle remains the live owner until the full Wrench handler,
menu, client screens, remaining payloads and GameTests migrate as one cutover.

## Standalone verification baseline

On 2026-07-23, the assembled Automata JAR was launched in a clean dedicated
server directory with exactly Fabric API and TotemCore. The server reached
`Done`, logging both `TotemAutomata initialized without Cognition dependency`
and the TotemCore initializer. No DeadRecall or Cognition JAR was present.
This establishes standalone dependency safety only; it does not replace the
sorting, gathering, pressure and restart qualification required before cutover.
