#!/usr/bin/env bash
# Build rdzenia Spring (server/) lancuchem JDK 21 + Maven 3.9.x.
# Bez argumentow: clean package. Mozna podac wlasne cele, np. ./build.sh clean test
set -euo pipefail

: "${JAVA_HOME21:?ustaw zmienna JAVA_HOME21 -> JDK 21}"
: "${MAVEN_HOME9:?ustaw zmienna MAVEN_HOME9 -> Maven 3.9.x}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$#" -eq 0 ]; then set -- clean package; fi

JAVA_HOME="$JAVA_HOME21" exec "$MAVEN_HOME9/bin/mvn" -B -f "$ROOT/pom.xml" "$@"
