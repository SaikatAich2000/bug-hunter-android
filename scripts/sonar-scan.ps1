# =============================================================================
#  scripts/sonar-scan.ps1
# -----------------------------------------------------------------------------
#  Windows / PowerShell driver for a full SonarQube analysis of the Android
#  app. Unlike the Python backends (which use the sonar-scanner-cli Docker
#  image), an Android/Gradle project is analysed by the `org.sonarqube`
#  Gradle plugin, so this script just drives Gradle:
#
#      1. testDebugUnitTest    - run the JVM unit suite
#      2. koverXmlReportDebug  - emit JaCoCo-format coverage XML for Sonar
#      3. sonar                - upload analysis + coverage to SonarQube
#
#  Usage:
#      $env:SONAR_TOKEN = "sqp_xxxxxxxxxxxx"
#      .\scripts\sonar-scan.ps1
#
#  Override the host (defaults to http://localhost:9000):
#      $env:SONAR_HOST_URL = "http://localhost:9000"
#
#  Database safety: this script compiles + tests the app and uploads a
#  static-analysis report. It never touches the server's database or any
#  runtime state. Build output lives under build/ (gitignored).
#
#  ASCII-only on purpose. Windows PowerShell 5.1 reads .ps1 files as the
#  console code page when no BOM is present, so any UTF-8 multi-byte char
#  (em-dash, arrow) corrupts string parsing.
# =============================================================================
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Info  { param([string]$msg) Write-Host "[SONAR] $msg" -ForegroundColor Green }
function Warn  { param([string]$msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Abort { param([string]$msg) Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

# --- Config ----------------------------------------------------------------
if (-not $env:SONAR_HOST_URL) { $env:SONAR_HOST_URL = "http://localhost:9000" }

# --- Pre-flight ------------------------------------------------------------
# The Gradle daemon needs a JDK 17+. Android Studio ships a JetBrains Runtime
# (JBR 21) that works out of the box; prefer it if JAVA_HOME isn't already set.
if (-not $env:JAVA_HOME) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path "$jbr\bin\java.exe") {
        $env:JAVA_HOME = $jbr
        Info "JAVA_HOME not set; using Android Studio JBR at $jbr"
    } else {
        Warn "JAVA_HOME is not set and the Android Studio JBR wasn't found."
        Warn "Set JAVA_HOME to a JDK 17+ before running if Gradle can't start."
    }
}

try {
    $resp = Invoke-WebRequest -Uri "$($env:SONAR_HOST_URL)/api/system/status" -TimeoutSec 5 -UseBasicParsing
    if ($resp.StatusCode -ne 200) { throw "non-200" }
} catch {
    Abort "Can't reach SonarQube at $($env:SONAR_HOST_URL). Is the container up? Try: docker start sonarqube"
}
Info "SonarQube is reachable at $($env:SONAR_HOST_URL)"

if (-not $env:SONAR_TOKEN) {
    Warn "No SONAR_TOKEN set - the scan will be submitted anonymously."
    Warn "If your SonarQube has 'Force user authentication' enabled (the"
    Warn "default on recent versions) the scan WILL fail with 401."
    Warn "Generate a token at: $($env:SONAR_HOST_URL)/account/security/"
}

# --- Run ------------------------------------------------------------------
$gradle = Join-Path $Root "gradlew.bat"
if (-not (Test-Path $gradle)) { Abort "gradlew.bat not found at $gradle" }

# The sonarqube plugin reads sonar.host.url / sonar.token from -D system
# properties or the SONAR_HOST_URL / SONAR_TOKEN env vars. We pass them
# explicitly so the behaviour is identical whether or not they're exported.
$gradleArgs = @(
    "testDebugUnitTest",
    "koverXmlReportDebug",
    "sonar",
    "--console=plain",
    "-Dsonar.host.url=$($env:SONAR_HOST_URL)"
)
if ($env:SONAR_TOKEN) {
    $gradleArgs += "-Dsonar.token=$($env:SONAR_TOKEN)"
}

Info "Running: gradlew $($gradleArgs -join ' ')"
& $gradle @gradleArgs
if ($LASTEXITCODE -ne 0) { Abort "Gradle sonar run failed (exit $LASTEXITCODE)" }

Info "Done. Browse results at $($env:SONAR_HOST_URL)/dashboard?id=Bug-Hunter-Android"
