#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
find "$ROOT/backend/src/main/java/com/smarthome/smart_home_backend/ds" -name '*.java' -print > "$TMP/sources.txt"
printf '%s\n' "$ROOT/scripts/EngineSmokeTest.java" >> "$TMP/sources.txt"
javac -encoding UTF-8 -d "$TMP/classes" @"$TMP/sources.txt"
java -cp "$TMP/classes" EngineSmokeTest
