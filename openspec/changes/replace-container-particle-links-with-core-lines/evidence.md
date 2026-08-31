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

### Applicability and deliberately pending work

- Dedicated Server + Target Client + Observer Client E2E is not applicable:
  this change adds no Screen/Menu, Observer provider/session, semantic relay,
  remote-viewer state or cross-client packet path. This decision is not
  represented as a passing three-JVM test.
- Automata's clean official-namespace Production Runtime, Build workflow and
  Modrinth publication remain pending until the validated 0.1.20 commit is
  pushed. They are not represented as passing before their GitHub Actions
  results exist.
