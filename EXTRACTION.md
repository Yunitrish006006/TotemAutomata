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

The additive primitives are now also exercised by six Fabric GameTests: legacy
binding migration, fuel persistence, component-safe sorting, binding restart
round-tripping, and duplicate-request/backoff pressure handling. These tests
qualify the extracted primitives only; the live Wrench handler, gathering
interaction and compatibility-bundle cutover remain pending.

On 2026-07-24, the persisted Copper Golem schema passed a second restart gate
in two separate Dedicated Server JVMs.  The seed server created a marked
Golem in gathering mode with revision/activity/fuel state, a source binding,
bindings list, and named/damaged tool plus named storage ItemStack components.
The verify server reloaded the same world and asserted every value before
writing `verify.ok` (the seed process wrote `seed.ok`).  This is evidence for
the persistence/restart portion of the qualification. Together with the
standalone launch and the passing sorting/gathering/request-pressure Fabric
GameTests, it completes the module's test qualification; it does not make the
inactive live sorting/gathering Wrench runtime a completed cutover.

The complete clientbound `deadrecall:copper_wrench_bindings` menu snapshot is
now also owned by Automata, including the sorting, gathering, LLM and binding
sections of its legacy wire layout. Its registration remains inactive until the
Wrench authority and menu are moved together, preventing duplicate payload
registration in a compatibility-bundle installation.

Automata also owns the local client visualization implementation. It resolves
the preserved wrench item ID and selected-golem ItemStack data without a
DeadRecall class dependency, but its receiver and tick hook remain inactive
until the same complete cutover.

The first server-side Wrench authority boundary is now extracted as well.
`CopperGolemWrenchAccess` validates the selected Wrench, golem identity,
dimension, management distance and optimistic revision before exposing a
golem to a mutation. Future menu and payload handlers must use this boundary;
it remains inactive until the operations they authorize migrate with it.

The Wrench item's tooltip behavior is also module-owned and reads the same
selection contract. Item registration and its resources remain bundle-owned
until the complete menu/interaction unit can be activated without duplicate
`deadrecall:copper_wrench` content.
# Client menu editing

`CopperGolemMenuEditor` owns the network-independent, optimistic edits made by
the Copper Golem menu: binding LLM prompt/toggle changes, cached allow/deny
decisions, and gathering LLM prompt normalization.  The inactive client
cutover shell can use it when the rendering screen is moved; it deliberately
does not register a screen or send packets by itself.

`CopperGolemMenuClientController` now owns the corresponding snapshot and
command creation.  The later screen migration only needs to render its
snapshot and pass its command records to the legacy-ID packet actions.

`CopperGolemMenuScreenSession` provides that lifecycle seam: it opens and
closes against the inactive cutover router, applies only its golem's snapshot,
and dispatches controller commands through the existing legacy payload IDs.
It now covers the full serverbound menu command set, including API settings,
connection tests, and gathering-target edits.

`CopperGolemMenuScreenLifecycle` now replaces the legacy screen's static
current/pending payload lifecycle with an Automata-owned session instance.

`CopperGolemMenuPanelLayout` now owns the preserved responsive panel geometry
for the pending concrete renderer.

`CopperGolemMenuUiState` now owns the migrated screen's tab, selection, and
scroll state.

# Wrench gesture planning

`CopperWrenchInteractionPlanner` contains the authoritative decision table
for left-click, use-block, and shift-use-golem Wrench gestures.  It is inactive
until the live callback adapter migrates, but locks down the sorting/gathering
intent rules independently of Fabric event registration.

`CopperGolemWrenchAccess.resolveSelectedGolem` supplies the shared
selection/liveness/dimension check for that future adapter; menu mutations add
their existing distance, held-Wrench, and revision requirements on top.

# Gathering configuration

`GatheringConfiguration` now owns the persisted gathering-area corners,
legacy area limits, and manual block-target list.  The live gathering tick and
callback adapter remain to be moved, but can use this schema without touching
the legacy handler.
It also converts a complete current-dimension area into validated runtime scan
bounds for the gathering behavior.

`GatheringLlmState` now owns gathering prompt revisions and cached block/tag
decisions, including stale-response rejection.  The future tick loop can use
its cache lookup and request classifications only when no cached decision is
available.

`GatheringTargetPolicy` owns the scanner's manual-target, cache, and
classification-request precedence.  World safety, permissions, navigation,
and storage checks remain in the runtime tick extraction.

`GatheringStorage` now owns the legacy single-kind, 16-item gathering carry
limit and safe drop normalization/merge behavior used before breaking a block.

`GatheringDeposit` now owns the home-container capacity simulation and atomic
insertion rules, plus the activity conditions that require the golem to return
its carried storage before scanning further targets.

`GatheringTickPlan` now owns the top-level decision between blocked area/home,
returning to deposit, and scanning; the future behavior adapter executes the
selected world-aware branch.

`PersistedGatheringBehavior` now attaches the extracted prerequisites and
persisted scanner to Automata's `CopperGolemBehavior` lifecycle.  It remains
inactive until its injected world operations complete navigation, breaking,
and deposit execution.

`GatheringToolDamage` now owns the enchantment-aware post-break tool damage
operation required by that final break execution branch.

`GatheringDrops` now owns safe block-drop resolution and delegates the
single-kind/16-item acceptance rule to `GatheringStorage`.

`GatheringBlockSafety` owns the legacy unsafe-block exclusion list used by
world target validation.

`GatheringTargetPreconditions` now owns the remaining non-navigation target
checks: area membership, source/bound-container exclusion, chunk loading,
breakability, safety, and container exclusion.

`GatheringBreakPermission` now owns legacy operator, spawn-protection,
interaction, GameMaster, restriction, and tool checks before a break event is
fired.

`GatheringBreakEvents` owns the Fabric BEFORE/CANCELED event gate used after
permission succeeds and before the gathering break transaction commits.

`GatheringBreakTransaction` now atomically prepares the persisted fuel,
storage, tool-damage, and target-clear updates for an approved break.

`GatheringBreakProgress` now owns visible crack timing, progress persistence,
and reset behavior leading up to that transaction.

`GatheringBlockBreaker` now owns the event-gated destructive break, carried
drop commit, post-break event/visual effects, and broken-tool outcome.

`GatheringHomeDeposit` now owns navigation to the configured source, atomic
home-container deposit, and the return/deposit/full/searching transitions.

`GatheringHomeResolver` now resolves the persisted source binding into the
loaded copper-chest container used by that deposit executor.

`GatheringNavigation` now owns candidate mining positions, path priority,
collision/floor checks, stuck-target tracking, and skipped-target persistence.

`GatheringOperator` now owns the legacy last-operator persistence used for
gathering protection and Fabric break-event authorization.

`DefaultGatheringWorldOperations` now concretely supplies home/fuel/rules,
target eligibility, deposit, and stop operations to `PersistedGatheringBehavior`.

`GatheringScanCursor` now owns the legacy top-down, budgeted scan order and
retry timing.  The future world adapter provides the candidate predicate and
persists the returned cursor/activity state.

`GatheringRuntimeState` maps those scan outcomes to the legacy target,
cursor, activity, and retry NBT keys, including the reset behavior used by
configuration updates and completed scans.

`PersistedGatheringScanner` combines the cursor and state layers into the
per-tick call used by the future world adapter, which only supplies a
world-aware candidate predicate.

# Wrench callback cutover seam

`CopperWrenchCallbackRegistration` contains the inactive Fabric event
registration surface for Automata.  Its authority interface is intentionally
unimplemented until the full live Wrench execution adapter is ready; this
prevents duplicate callback ownership during the additive phase.

`CopperWrenchStateMutator` now supplies the callback authority's persisted
source, destination, gathering-corner, and manual-target mutations, including
legacy revision bumps and scan resets.

`CopperWrenchSelection` also now owns selection writes/clears, so the
eventual shift-use-golem authority does not need DeadRecall item-data helpers.

`PersistedCopperWrenchInteractionAuthority` now executes the extracted
gesture plans for persisted source/destination/gathering mutations and
shift-use selection.  Its menu opener is injected and it remains unregistered
until the complete callback/menu cutover.

`PersistedCopperGolemMenuOpener` supplies that injected menu opener.  It is
also cutover-only because it references the preserved menu type and refreshes
the clientbound menu snapshot after opening.


The same authority now retains the legacy Copper Ingot repair interaction,
including durability-safe item consumption and wax-particle feedback.

`CopperWrenchInteractionDebounce` preserves the entity-to-block suppression
and gathering target click debounce timing; it is ready to be wired into the
registered authority as part of final Wrench parity.

`CopperWrenchFeedback` provides the legacy overlay message keys for the
extracted authority's binding and gathering outcomes.

`CopperWrenchPathVisualization` restores the 28-point Wax On particle path
shown when a source or sorting destination is successfully bound.

The Wrench authority now accepts an injected first-binding criterion hook;
the final cutover supplies Automata's registered advancement trigger without
loading it during the additive phase.
