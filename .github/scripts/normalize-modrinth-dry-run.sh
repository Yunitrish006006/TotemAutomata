#!/usr/bin/env bash
set -euo pipefail

requested="${1:-true}"
case "$requested" in
  true|false)
    printf '%s\n' "$requested"
    ;;
  *)
    printf 'Modrinth release source validation failed: dry_run must be true or false.\n' >&2
    exit 1
    ;;
esac
