## Validation evidence

Executed locally on 2026-08-31 with Minecraft 26.2, Java 25, Fabric Loader
0.19.3, Fabric API 0.154.2+26.2 and TotemCore 0.7.13.

### Build and static checks

- `.github/scripts/check-modrinth-release-gate.sh` — passed after updating the
  exact Core commit, artifact filename and dependency range to 0.7.13.
- `../TotemCore/gradlew test assemble compileGametestJava --no-daemon
  --stacktrace` — passed, including complete/incomplete/different-dimension
  gizmo submission tests.
- `../TotemCore/gradlew build --no-daemon --stacktrace` — passed and reran all
  39 required server GameTests through the repository's `build` lifecycle.
- `git diff --check` and `jq empty` for production and GameTest Fabric metadata
  — passed.
- Production JAR inspection found
  `dev/totem/automata/client/CopperGolemVisualizationClient.class`; embedded
  metadata reports version 0.1.19 and `totem-core >=0.7.13 <0.8.0`.
- Local JAR: `build/libs/totem-automata-0.1.19.jar`, SHA-256
  `4e4c93c3af7ca86d3f19b7b2689cb2b5d522a1c704e4f2bb4e086e3c6a47541b`.

### Runtime checks

- `../TotemCore/gradlew runGameTest --no-daemon --stacktrace` — all 39 required
  server GameTests passed.
- `runAutomataRestartProbe` with `includeTotemExcavationRuntime=false` and the
  `standalone` phase — passed; the probe wrote `standalone.ok` and loaded
  TotemCore 0.7.13 without TotemExcavation.
- `../TotemCore/gradlew runClientGameTest --no-daemon --stacktrace` — passed,
  including the new gathering-area outline client fixture.
- Inspected `totem-automata-gathering-area-depth-tested.png` at 854x480: cyan
  outline segments remain visible outside the stone-brick wall, while the wall
  hides the portion directly behind it.
- Added `runProductionClientGameTest` and the dedicated `Production Runtime`
  workflow. The task packages the GameTest source set separately and loads the
  main Automata 0.1.19, Core 0.7.13 and optional Excavation 0.1.8 distribution
  JARs in the official namespace.
- A local production launch reached Fabric Loader and correctly identified all
  four distribution mods. The shared development cache then failed closed
  because its Fabric API class tweakers had previously been rewritten to the
  `named` namespace. An isolated-cache retry could not finish because
  `libraries.minecraft.net` DNS resolution was unavailable; this is not
  recorded as a passing Production Runtime run.
- GitHub Actions `Production Runtime` run `33329515187` on release commit
  `04dd84fbe48794a3bcfc7305faa2e7a0e81d7c3b` — passed. Its clean Linux runner
  built the pinned Core and Excavation distribution JARs, then completed all
  production-namespace client GameTests with Automata 0.1.19.

### Publication and hosted validation

- GitHub Actions `Build` run `33329515190` — passed, including the release
  gate, compile/assemble, standalone runtime without Excavation, all 39 server
  GameTests and client GameTests.
- GitHub Actions `Publish Modrinth` run `33329515189` — passed, including clean
  release-JAR construction, remote artifact/dependency verification, public
  review submission and the verified-publication marker commit.
- Modrinth version ID: `PmMsj0VN`; remote SHA-512:
  `e64dce0b5e688fd6e89f5c368cdaa29ed0d0e90ad474efd09cf4c61f13ac820bd224ec2e8675370d6be35e3280d238a6f120286e85ec4ddc460e09eec398a11e`.
- The authenticated publication log reported project slug `totem-automata`,
  status `processing` and requested status `approved`. The anonymous version
  endpoint still returned HTTP 404 during that review state, so public listing
  approval is not claimed.

### OpenSpec

- `npx -y @fission-ai/openspec@latest validate
  migrate-gathering-area-outline-to-core --strict` — passed before
  implementation and again after the final evidence/task update.

### Deliberately pending

- Dedicated Server + Target Client + Observer Client E2E is not applicable:
  the change has no Observer session, semantic provider, remote viewer or
  cross-client packet path. This applicability decision is not claimed as a
  passing three-JVM test.
- Modrinth public listing remains under external review; authenticated upload,
  remote artifact verification and review submission succeeded, but anonymous
  visibility is not claimed until the project leaves `processing`.
