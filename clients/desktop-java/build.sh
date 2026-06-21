#!/usr/bin/env bash
# Build klienta JavaFX (clients/desktop-java/) lancuchem JDK 21 + Maven 3.9.x.
# Bez argumentow: clean compile. JavaFX nie ma tu konfiguracji pakowania do fat-jara.
set -euo pipefail

: "${JAVA_HOME21:?ustaw zmienna JAVA_HOME21 -> JDK 21}"
: "${MAVEN_HOME9:?ustaw zmienna MAVEN_HOME9 -> Maven 3.9.x}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$#" -eq 0 ]; then set -- clean compile; fi

JAVA_HOME="$JAVA_HOME21" exec "$MAVEN_HOME9/bin/mvn" -B -f "$ROOT/pom.xml" "$@"
