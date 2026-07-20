#!/usr/bin/env bash
# Runs the usage-scan compiler plugin over a downstream repo and collects every reference
# into the watched kiteui packages, across whatever Kotlin compile tasks you name.
#
# Usage:
#   scan-repo.sh <repo-dir> <out-subdir> <gradle-compile-task>...
#
# Example (all frontend targets of an app):
#   scan-repo.sh ~/Projects/ls-kiteui-starter starter \
#       :apps:compileKotlinJvmSsr :apps:compileKotlinJs \
#       :apps:compileDebugKotlinAndroid :apps:compileKotlinIosArm64
#
# The plugin jar must already be built:  ../../gradlew -p . :plugin:jar
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="${1:?repo dir}"; shift
SUBDIR="${1:?out subdir}"; shift

export USAGE_SCAN_JAR="$HERE/plugin/build/libs/plugin.jar"
export USAGE_SCAN_OUT="$HERE/out/$SUBDIR"
export USAGE_SCAN_MODE="${USAGE_SCAN_MODE:-refs}"
export USAGE_SCAN_PREFIXES="${USAGE_SCAN_PREFIXES:-com.lightningkite.kiteui}"

[ -f "$USAGE_SCAN_JAR" ] || { echo "Build the plugin first: gradlew -p tools/usage-scan :plugin:jar"; exit 1; }
rm -rf "$USAGE_SCAN_OUT"

( cd "$REPO" && ./gradlew --init-script "$HERE/inject.init.gradle.kts" "$@" --rerun-tasks --console=plain )

echo "Wrote $(cat "$USAGE_SCAN_OUT"/*.txt 2>/dev/null | sort -u | wc -l | tr -d ' ') unique keys to $USAGE_SCAN_OUT"
