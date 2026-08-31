#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
workflow="$repo_root/.github/workflows/publish-modrinth.yml"
build_workflow="$repo_root/.github/workflows/build.yml"
normalizer="$repo_root/.github/scripts/normalize-modrinth-dry-run.sh"
remote_filter="$repo_root/.github/scripts/verify-modrinth-remote-dependencies.jq"
summary_filter="$repo_root/.github/scripts/modrinth-dependency-summary.jq"
error_filter="$repo_root/.github/scripts/modrinth-error-summary.jq"
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

require_literal 'CORE_REF: 71422749875072b585dcf64a4c9f42ee96773543' \
  'release TotemCore commit pin is stale.'
require_literal 'EXCAVATION_REF: 6b54011195b81ec9a9a09146d162ba303ebd8ee4' \
  'release TotemExcavation commit pin is stale.'
require_literal 'TOTEM_EXCAVATION_REFERENCE_VERSION_ID: Klewi9E3' \
  'TotemExcavation 0.1.8 Modrinth reference version is stale.'
require_literal 'TOTEM_CORE_DEPENDENCY_FILE: totem-core-0.7.14.jar' \
  'required TotemCore reference artifact is not exact.'
require_literal "--arg core '>=0.7.14 <0.8.0'" \
  'release JAR metadata validation still accepts a pre-line TotemCore range.'
require_literal 'TOTEM_EXCAVATION_DEPENDENCY_FILE: totem-excavation-0.1.8.jar' \
  'optional TotemExcavation reference artifact is not exact.'
require_literal '.github/staging/modrinth-changelog-*.md' \
  'release changelog changes do not trigger the publication workflow.'
require_literal '.github/scripts/check-modrinth-release-gate.sh' \
  'release-gate script changes do not trigger the publication workflow.'
require_literal '.github/scripts/modrinth-error-summary.jq' \
  'safe Modrinth error-summary changes do not trigger the publication workflow.'
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
require_literal 'Submit project for public review' \
  'successful publication does not submit an unlisted project for public review.'
require_literal -- '--data '\''{"requested_status":"approved"}'\''' \
  'public-review submission does not request approved status explicitly.'
require_literal 'version_type:"release"' \
  'Modrinth create metadata is not pinned to release.'
require_literal 'environment:"client_and_server"' \
  'Modrinth create metadata is not pinned to client_and_server.'
require_literal 'release Minecraft version must be exactly 26.2.' \
  'release Minecraft version is not pinned to 26.2.'
require_literal -- '--argjson deps "$deps"' \
  'Modrinth create metadata does not receive the verified dependency array.'
require_literal '"version/${TOTEM_EXCAVATION_REFERENCE_VERSION_ID}"' \
  'TotemExcavation project dependency is not resolved from the exact 0.1.8 version.'
require_literal '{file_name:$core_file,dependency_type:"required"}' \
  'TotemCore is not emitted as an exact required Modrinth file dependency.'
require_literal '{project_id:$excavation,dependency_type:"optional"}' \
  'TotemExcavation is not emitted as an optional Modrinth project dependency.'
require_literal "printf '%s\\n' \"\$data\" > /tmp/version-data.json" \
  'validated Modrinth metadata is not serialized to the multipart source file.'
require_literal -- "-F 'data=</tmp/version-data.json;type=application/json'" \
  'Modrinth create metadata is not uploaded from a file-safe multipart part.'
require_literal -- '-f .github/scripts/modrinth-error-summary.jq' \
  'Modrinth HTTP errors are not reduced to approved fields.'
require_literal -- '-f .github/scripts/verify-modrinth-remote-dependencies.jq' \
  'published dependencies are not checked by the strict verifier.'
if grep -Fq -- '-F "data=${data}' "$workflow"; then
  fail 'inline multipart JSON can be truncated at changelog semicolons; upload metadata from a file.'
fi
if ! grep -Fq 'run: .github/scripts/check-modrinth-release-gate.sh' "$build_workflow"; then
  fail 'normal pull-request CI does not execute the Modrinth release gate.'
fi
if ! grep -Fq '(.file_name == $core_file or .file_name == null)' "$remote_filter"; then
  fail 'remote dependency verification does not accept Modrinth-normalized null for the exact submitted TotemCore file.'
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
    --arg core_file totem-core-0.7.14.jar \
    --arg excavation excavation-project \
    -f "$remote_filter"
}

accepted=(
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH","version_id":null,"file_name":null},{"dependency_type":"required","project_id":null,"version_id":null,"file_name":"totem-core-0.7.14.jar"},{"dependency_type":"optional","project_id":"excavation-project","version_id":null,"file_name":null}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH","version_id":null,"file_name":null},{"dependency_type":"required","project_id":null,"version_id":null,"file_name":null},{"dependency_type":"optional","project_id":"excavation-project","version_id":null,"file_name":null}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"totem-core-0.7.14.jar"},{"dependency_type":"optional","project_id":"excavation-project"}]}'
)
for candidate in "${accepted[@]}"; do
  if ! verify_dependencies <<<"$candidate" >/dev/null; then
    fail 'dependency verifier rejected exact or Modrinth-normalized project-linked metadata.'
  fi
done

rejected=(
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"wrong-core.jar"},{"dependency_type":"optional","project_id":"excavation-project"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"totem-core-0.7.14.jar"},{"dependency_type":"optional","project_id":"wrong-excavation"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"optional","file_name":"totem-core-0.7.14.jar"},{"dependency_type":"optional","project_id":"excavation-project"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"totem-core-0.7.14.jar","version_id":"wrong-version"},{"dependency_type":"optional","project_id":"excavation-project"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"totem-core-0.7.14.jar"},{"dependency_type":"optional","project_id":"excavation-project","file_name":"wrong.jar"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},{"dependency_type":"required","file_name":"totem-core-0.7.14.jar"},{"dependency_type":"optional","project_id":"excavation-project"},{"dependency_type":"optional","project_id":"extra"}]}'
  '{"dependencies":[{"dependency_type":"required","project_id":"P7dR8mSH"},"error",{"dependency_type":"optional","project_id":"excavation-project"}]}'
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

error_input='{"error":"invalid_input","description":"safe detail","token":"must-not-leak","data":{"project_id":"must-not-leak"}}'
error_expected='{"error":"invalid_input","description":"safe detail"}'
if ! error_actual="$(jq -c -f "$error_filter" <<<"$error_input")"; then
  fail 'safe Modrinth error summary rejected documented error metadata.'
elif [[ "$error_actual" != "$error_expected" ]]; then
  fail 'Modrinth error summary leaks fields outside error and description.'
fi
if [[ "$(jq -c -f "$error_filter" <<<'["not-an-error-object"]')" \
    != '{"error":null,"description":null}' ]]; then
  fail 'Modrinth error summary does not suppress non-object response bodies.'
fi

if (( failures > 0 )); then
  printf 'Modrinth release gate failed with %d error(s).\n' "$failures" >&2
  exit 1
fi

printf 'Modrinth release gate passed: file-safe multipart JSON, redacted errors, and exact project/file dependencies are enforced.\n'
