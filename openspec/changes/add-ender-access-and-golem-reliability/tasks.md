# Tasks

## 1. Remnant upgrade

- [x] 1.1 Register the unique Ender Access upgrade, shaped recipe, item model,
  creative-tab entry, translations, and live Remnant manual recipe page.
- [x] 1.2 Add a server-validated backpack menu action that opens only the
  requesting player's vanilla Ender Chest inventory.
- [x] 1.3 Add the conditional vanilla-style screen button and tooltip without
  disturbing the existing crafting-panel and scrolling fixes.
- [x] 1.4 Create the canonical 16x16 upgrade texture, run the strict art
  validator immediately, and capture accepted real-client screenshots.

## 2. Infinite fuel

- [x] 2.1 Accept Nether Stars in the shared Copper Golem fuel slot and expose
  an authoritative infinite-fuel state.
- [x] 2.2 Bypass all finite fuel consumption while a Nether Star is installed,
  preserving the star count and paused finite burn counters.
- [x] 2.3 Render a translated infinity status and keep the behavior compatible
  with the pending furnace-style fuel proposal.

## 3. Sorting transfer

- [x] 3.1 Add a failing live-mixin reproduction for the item-return regression.
- [x] 3.2 Preflight and revalidate a complete source-to-destination route before
  each inventory mutation and fuel debit.
- [x] 3.3 Commit each destination insertion once, clear completed in-flight
  state, and return only a genuine remainder.
- [x] 3.4 Cover full, partial, rejected, dynamic-change, and later-tick paths
  with deterministic GameTests.

## 4. Locksmith integration

- [x] 4.1 Correct Locksmith's automation policy to the documented
  DENY/TRUSTED/ALL operator matrix.
- [x] 4.2 Use the route-aware Locksmith API for Copper Golem sorting while
  keeping optional integration fail closed when Locksmith is present.
- [x] 4.3 Add a player-aware Locksmith bridge and gate sorting-source,
  sorting-destination, and gathering-home wrench binding before mutation.
- [x] 4.4 Add Locksmith unit tests and Automata pairwise GameTests for same-lock,
  boundary, role, anonymous, and adapter-failure cases.
- [x] 4.5 Add wrench GameTests proving allowed bindings succeed and denied
  bindings change neither Golem state, operator identity, criteria, nor visuals.

## 5. Validation

- [x] 5.1 Run focused unit tests and compile checks in all three repositories.
- [x] 5.2 Run server GameTests, client visual GameTests, and restart probes
  affected by the changes.
- [x] 5.3 Run the strict pixel-art validator and inspect the captured UI at
  native scale.
- [x] 5.4 Run `openspec validate add-ender-access-and-golem-reliability --strict`
  and record final evidence.

### Validation evidence (2026-08-20)

- Compile/focused unit checks passed in TotemRemnant, TotemAutomata, and
  TotemLocksmith with Java 25 and TotemCore 0.7.2.
- Required server GameTests passed: Remnant 48/48, Automata 31/31, and
  Locksmith 19/19.
- Affected Remnant and Automata client visual GameTests passed; the accepted
  Ender Access screenshots are `backpack-ender-access-en-us.png` and
  `backpack-ender-access-zh-tw.png` under Remnant's backpack-inventory
  artifacts.
- Two-JVM seed/verify restart probes passed for all three repositories with no
  failure markers. Automata verified that the Nether Star stack, infinite-fuel
  state, and paused finite burn counter survived reload.
- The Totem art-direction strict validator accepted
  `upgrade_ender_access.png`; native-scale English and Traditional Chinese UI
  inspection found no clipping or overlap.
- `openspec validate add-ender-access-and-golem-reliability --strict` passed.
