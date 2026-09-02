## Validation evidence

Executed locally on 2026-08-31 with Minecraft 26.2, Java 25, Fabric Loader
0.19.3, Fabric API 0.154.2+26.2, TotemCore 0.7.14 and TotemAutomata 0.1.20
artifacts.

### Build and static checks

- `./gradlew build --no-daemon --stacktrace` in TotemCore — passed, including
  line endpoint, colour, width, depth-tested and through-wall unit coverage.
- `../TotemCore/gradlew -PtotemCoreJar=/Volumes/DataExtended/workspaces/TotemCore/build/libs/totem-core-0.7.14.jar clean build --no-daemon --stacktrace`
  in TotemAutomata — passed, including JUnit and all 39 required server
  GameTests.
- `git diff --check` in both repositories and `jq empty` for production and
  GameTest Fabric metadata — passed.
- `javap` on `totem-core-0.7.14.jar` confirmed the new public
  `TotemWorldOutlines.line(Vec3, Vec3, WorldOutlineStyle)` method.
- The Automata JAR contains the updated
  `CopperGolemVisualizationClient.class`; the removed particle-chain helper,
  particle cap and source/destination particle types are absent from its
  source.
- `npx -y @fission-ai/openspec@latest validate
  replace-container-particle-links-with-core-lines --strict` — passed after
  the implementation, task and evidence updates.
- `.github/scripts/check-modrinth-release-gate.sh` — passed with the exact
  Core implementation commit, 0.7.14 artifact and `>=0.7.14 <0.8.0` metadata
  requirement.
- Final local JAR `build/libs/totem-automata-0.1.20.jar` reports version
  0.1.20 and the exact Core range; SHA-512:
  `5e3c237345a7943c4ecbba277e3849a916c380b1244901b37453fe7b623da42b16a44cec91e11525213f02218fcbc7f58fb98cc6b1b48ae36ee8cc5ff8c0424c`.

### Runtime and visual checks

- `../TotemCore/gradlew runClientGameTest --no-daemon --stacktrace` — passed,
  including the new container-link depth fixture and all existing native
  client fixtures.
- Inspected `totem-automata-container-links-depth-tested.png` at 854x480. The
  orange source segment and green/red destination segments remain visible on
  the exposed sides, while the stone-brick wall hides their central portions.
  SHA-256:
  `521db3181a0278f1c84794999ec1ea2c7abd0e53b6981bbd73bf83f9c27a8cf8`.
- `runAutomataRestartProbe` with `includeTotemExcavationRuntime=false` and the
  `standalone` phase — passed; Fabric Loader omitted TotemExcavation and the
  probe wrote `standalone.ok` without a failure marker.

### Core release prerequisite

- TotemCore 0.7.14 implementation commit
  `71422749875072b585dcf64a4c9f42ee96773543` was pushed to `master`.
- TotemCore GitHub Actions Build run `33356760350` passed, including the
  Core-only Dedicated Server startup check.
- TotemCore Publish Modrinth run `33356760558` passed and verified version ID
  `AUvvmQsk`; release marker commit `8407f3a` was fetched and fast-forwarded.

### Automata release and applicability

- Dedicated Server + Target Client + Observer Client E2E is not applicable:
  this change adds no Screen/Menu, Observer provider/session, semantic relay,
  remote-viewer state or cross-client packet path. This decision is not
  represented as a passing three-JVM test.
- TotemAutomata 0.1.20 release commit
  `52b99ad32bdc76f89565921c51b5bf1a413702fe` was pushed to `master`.
- Build run `33357583619` passed the release gate, pinned dependency builds,
  standalone runtime, all 39 required server GameTests and native Client
  GameTests.
- Production Runtime run `33357583605` passed all official-namespace Client
  GameTests against the distribution JARs for Automata 0.1.20, Core 0.7.14 and
  Excavation 0.1.8.
- Publish Modrinth run `33357583618` passed clean JAR construction, remote
  dependency/artifact verification and public-review handling. It published
  version ID `lCFCHYGC` with the same SHA-512 recorded above.
- Release marker commit `460d070886e4e2363f5ee17e8d5144647304b0dd` was
  fetched and fast-forwarded; both local TotemCore and TotemAutomata working
  trees were synchronized with their `origin/master` branches before this
  final evidence update.

### Live Golem endpoint follow-up — 2026-09-02

- `compileJava compileClientJava compileGametestJava` passed with Java 25,
  TotemCore 0.7.16 and TotemExcavation 0.1.10.
- `runClientGameTest` passed in 2m 38s. The container-link fixture moved a real
  client-tracked Copper Golem from X=0.5 to X=1.5 while retaining a deliberately
  stale fallback snapshot, then verified the submitted line origin followed the
  moved entity.
- `build --no-daemon --stacktrace` passed in 3m 13s, including all 39 required
  server GameTests and JUnit checks.
- `openspec validate replace-container-particle-links-with-core-lines --strict`
  passed.
- Inspected the updated 854x480 native-scale screenshot. The orange, green and
  red lines meet the visible Copper Golem while the stone-brick wall continues
  to hide occluded portions. SHA-256:
  `48adb4e32a67f0cc64b5d339536f8b6420e09f1b7e0082fdcc44af5a34ff4984`.
