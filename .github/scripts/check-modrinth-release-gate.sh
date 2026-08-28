#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
workflow="$repo_root/.github/workflows/publish-modrinth.yml"
build_workflow="$repo_root/.github/workflows/build.yml"
normalizer="$repo_root/.github/scripts/normalize-modrinth-dry-run.sh"
remote_filter="$repo_root/.github/scripts/verify-modrinth-remote-dependencies.jq"
summary_filter="$repo_root/.github/scripts/modrinth-dependency-summary.jq"
failures=0

fail() {
  printf 'Modrinth release gate: %s\n' "$*" >&2
  failures=$((failures + 1))
}

require_literal() {
  local literal="$1"
  local message="$2"
  grep -Fq -- "$literal" "$workflow" || fail "$message"
}

if ! command -v jq >/dev/null 2>&1; then
  printf 'Modrinth release gate: jq is required.\n' >&2
  exit 2
fi

require_literal 'CORE_REF: 82b21944b1e4865f5d34f13febc5049d936a636f' \
  'release TotemCore commit pin is stale.'
require_literal 'EXCAVATION_REF: 6b54011195b81ec9a9a09146d162ba303ebd8ee4' \
  'release TotemExcavation commit pin is stale.'
require_literal 'TOTEM_CORE_DEPENDENCY_FILE: totem-core-0.7.11.jar' \
  'required TotemCore external file is not exact.'
require_literal 'TOTEM_EXCAVATION_DEPENDENCY_FILE: totem-excavation-0.1.8.jar' \
  'optional TotemExcavation external file is not exact.'
require_literal '.github/staging/modrinth-changelog-*.md' \
  'release changelog changes do not trigger the publication workflow.'
require_literal '.github/scripts/check-modrinth-release-gate.sh' \
  'release-gate script changes do not trigger the publication workflow.'
require_literal '.github/scripts/verify-modrinth-remote-dependencies.jq' \
  'dependency verifier changes do not trigger the publication workflow.'
require_literal '.github/scripts/normalize-modrinth-dry-run.sh' \
  'dry-run parser changes do not trigger the publication workflow.'
require_literal 'clean jar --no-daemon --stacktrace' \
  'release JAR is not built from a clean output directory.'
require_literal 'version ${v} already exists with a different artifact SHA-512; refusing to overwrite it. Bump mod_version.' \
  'existing-version SHA conflicts do not provide a clear version-bump error.'
require_literal 'Modrinth release project: title=' \
  'project title/slug/status summary is not emitted.'
require_literal 'version_type:"release"' \
  'Modrinth create metadata is not pinned to release.'
require_literal 'environment:"client_and_server"' \
  'Modrinth create metadata is not pinned to client_and_server.'
require_literal 'release Minecraft version must be exactly 26.2.' \
  'release Minecraft version is not pinned to 26.2.'
require_literal -- '--argjson deps "$deps"' \
  'Modrinth create metadata does not receive the verified dependency array.'
require_literal -- '-f .github/scripts/verify-modrinth-remote-dependencies.jq' \
  'published dependencies are not checked by the strict verifier.'
if ! grep -Fq 'run: .github/scripts/check-modrinth-release-gate.sh' "$build_workflow"; then
  fail 'normal pull-request CI does not execute the Modrinth release gate.'
fi

if grep -Eq '^[[:space:]]+test([[:space:]]|$)' "$workflow"; then
  fail 'workflow contains bare test commands that can report only status 1.'
fi

if [[ "$($normalizer false)" != false ]]; then
  fail 'manual dry_run=false is not preserved.'
fi
if [[ "$($normalizer true)" != true ]]; then
  fail 'manual dry_run=true is not preserved.'
fi
for invalid in False 0 yes '[]'; do
  if "$normalizer" "$invalid" >/dev/null 2>&1; then
    fail "dry_run parser accepted invalid value $invalid."
  fi
done

verify_dependencies() {
  jq -e \
    --arg fabric P7dR8mSH \
    --arg core_file totem-core-0.7.11.jar \
    --arg excavation_file totem-excavation-0.1.8.jar \
    -f "$remote_filter"
}

accepted=(
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH","version_id":null,"file_name":null},{"dependency_type":"required","project_id":null,"version_id":null,"file_name":"totem-core-0.7.11.jar"},{"dependency_type":"optional","project_id":null,"version_id":null,"file_name":"totem-excavation-0.1.8.jar"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH","version_id":null,"file_name":null},{"dependency_type":"required","project_id":null,"version_id":null,"file_name":null},{"dependency_type":"optional","project_id":null,"version_id":null,"file_name":null}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":null},{"dependency_type":"optional","file_name":"totem-excavation-0.1.8.jar"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"totem-core-0.7.11.jar"},{"dependency_type":"optional","file_name":null}]}'
)
for candidate in "${accepted[@]}"; do
  if ! verify_dependencies <<<"$candidate" >/dev/null; then
    fail 'dependency verifier rejected exact or Modrinth-normalized null metadata.'
  fi
done

rejected=(
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"wrong-core.jar"},{"dependency_type":"optional","file_name":"totem-excavation-0.1.8.jar"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"totem-core-0.7.11.jar"},{"dependency_type":"optional","file_name":"wrong-excavation.jar"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"optional","file_name":"totem-core-0.7.11.jar"},{"dependency_type":"optional","file_name":"totem-excavation-0.1.8.jar"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"totem-core-0.7.11.jar"},{"dependency_type":"optional","file_name":"totem-excavation-0.1.8.jar"},{"dependency_type":"optional","project_id":"extra"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},"error",{"dependency_type":"optional","file_name":"totem-excavation-0.1.8.jar"}]}'
)
for candidate in "${rejected[@]}"; do
  if verify_dependencies <<<"$candidate" >/dev/null 2>&1; then
    fail 'dependency verifier accepted wrong, extra, or non-object metadata.'
  fi
done

summary_input='{"token":"must-not-leak","dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH","private":"must-not-leak"},"error"]}'
summary_expected='{"dependency_count":2,"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH","version_id":null,"file_name_present":false,"file_name":null},{"dependency_type":null,"project_id":null,"version_id":null,"file_name_present":false,"file_name":null}]}'
if ! summary_actual="$(jq -c -f "$summary_filter" <<<"$summary_input")"; then
  fail 'redacted dependency summary rejected test metadata.'
elif [[ "$summary_actual" != "$summary_expected" ]]; then
  fail 'dependency failure summary leaks or adds unapproved fields.'
fi

if (( failures > 0 )); then
  printf 'Modrinth release gate failed with %d error(s).\n' "$failures" >&2
  exit 1
fi

printf 'Modrinth release gate passed: dry-run parsing and exact three-dependency metadata are enforced.\n'
