#!/usr/bin/env bash
# =============================================================================
#  scripts/sonar-scan.sh
# -----------------------------------------------------------------------------
#  Run the JVM unit suite with coverage, then push the analysis into the
#  locally-running SonarQube via the org.sonarqube Gradle plugin.
#
#  Unlike the Python backends (sonar-scanner-cli Docker image), an
#  Android/Gradle project is analysed by the Gradle plugin directly:
#
#      1. testDebugUnitTest    - run the JVM unit suite
#      2. koverXmlReportDebug  - emit JaCoCo-format coverage XML for Sonar
#      3. sonar                - upload analysis + coverage to SonarQube
#
#  Usage:
#      SONAR_TOKEN=sqp_xxxxxxxxxxxx ./scripts/sonar-scan.sh
#
#  Defaults (override via env):
#      SONAR_HOST_URL = http://localhost:9000
#      SONAR_TOKEN    = (none -> anonymous; rejected if force-auth is on)
#
#  Database safety: compiles + tests the app and uploads a static-analysis
#  report only. No database access, no runtime state. Build output lives
#  under build/ (gitignored).
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info() { echo -e "${GREEN}[SONAR]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
abort() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"
SONAR_TOKEN="${SONAR_TOKEN:-}"

# The Gradle daemon needs JDK 17+. Prefer Android Studio's bundled JBR 21
# when JAVA_HOME isn't already pointing somewhere usable.
if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in \
    "/c/Program Files/Android/Android Studio/jbr" \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "$HOME/Android/android-studio/jbr"; do
    if [[ -x "${candidate}/bin/java" ]]; then
      export JAVA_HOME="${candidate}"
      info "JAVA_HOME not set; using Android Studio JBR at ${candidate}"
      break
    fi
  done
fi

if ! curl -sf -o /dev/null -m 5 "${SONAR_HOST_URL}/api/system/status"; then
  abort "Can't reach SonarQube at ${SONAR_HOST_URL}. Is the container up? (docker start sonarqube)"
fi
info "SonarQube is reachable at ${SONAR_HOST_URL}"

if [[ -z "${SONAR_TOKEN}" ]]; then
  warn "No SONAR_TOKEN set - the scan will be submitted anonymously."
  warn "If 'Force user authentication' is enabled the scan WILL fail with 401."
  warn "Generate a token at: ${SONAR_HOST_URL}/account/security/"
fi

GRADLE_ARGS=(
  testDebugUnitTest
  koverXmlReportDebug
  sonar
  --console=plain
  "-Dsonar.host.url=${SONAR_HOST_URL}"
)
if [[ -n "${SONAR_TOKEN}" ]]; then
  GRADLE_ARGS+=("-Dsonar.token=${SONAR_TOKEN}")
fi

info "Running: ./gradlew ${GRADLE_ARGS[*]}"
./gradlew "${GRADLE_ARGS[@]}"

info "Done. Browse results at ${SONAR_HOST_URL}/dashboard?id=Bug-Hunter-Android"
