## 1. Contract and dependency

- [x] 1.1 Raise Automata's local, Fabric metadata, CI and release-validation
  TotemCore floor/pin to 0.7.13 and its implementation commit.
- [x] 1.2 Keep Automata as the owner of selection/payload/lifecycle state and
  use only TotemCore's stateless client world-outline API.

## 2. Client rendering

- [x] 2.1 Register one module-owned level render callback and submit only a
  valid selected-Golem, gathering-mode, same-dimension area snapshot.
- [x] 2.2 Render complete areas as inclusive cuboids and incomplete corners as
  block outlines using `DEPTH_TESTED` occlusion.
- [x] 2.3 Remove gathering-area edge/corner particles while preserving source,
  destination, target and blocked-state particles.

## 3. Verification

- [x] 3.1 Unit-test complete/incomplete area submissions and prove none of the
  resulting gizmos is always-on-top.
- [x] 3.2 Client GameTest a partly wall-occluded area and inspect a native-scale
  screenshot proving the outline is visible beside but not through the wall.
- [x] 3.3 Run Automata unit/build, standalone runtime probe, server GameTests,
  Client GameTests and final JAR metadata/class inspection against Core 0.7.13.
- [x] 3.4 Add a dedicated official-namespace Production Runtime workflow that
  loads Automata, Core and optional Excavation from their distribution JARs.
- [x] 3.5 Confirm the Observer-only three-JVM Target/viewer relay gate is not
  applicable to this local world renderer, which changes no Screen/Menu,
  Observer provider, packet or cross-client state.
- [ ] 3.6 Run the new Production Runtime workflow on the release commit and
  record its GitHub Actions result.

## 4. Documentation

- [x] 4.1 Document the depth-tested gathering-area outline and retained
  particle meanings in the README.
- [x] 4.2 Strictly validate this OpenSpec change and record only executed
  evidence, leaving unavailable validation tasks unchecked.
