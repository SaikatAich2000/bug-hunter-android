# Bug Hunter Android

Native Android client for **Bug Hunter** — a multi-tenant bug, requirement, and task tracker. Talks to a [Bug Hunter backend](https://github.com/) over HTTPS.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt)
![minSdk 29](https://img.shields.io/badge/minSdk-29-brightgreen)
![targetSdk 36](https://img.shields.io/badge/targetSdk-36-blue)
![Kotlin 2.0](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF)

## Features

- **Full parity with the web app** — login (incl. TOTP), signup, password reset, dashboard with KPIs, work items (bugs / requirements / tasks) with filters and saved views, projects + members, organizations + branding + invitations + webhooks, events, audit log, sessions admin, profile, 2FA, DSAR export, and the Sleuth chatbot.
- **Adaptive UI** — bottom nav on phones, nav rail on tablets, permanent drawer on large screens. Material 3 throughout.
- **Light & dark theme** with manual override (System / Light / Dark) wired to the Bug Hunter brand palette.
- **Secure session handling** — cookie jar backed by `EncryptedSharedPreferences` + Android Keystore, CSRF double-submit, 401 auto-logout, debug-only HTTP logging with header redaction.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.0.20 |
| Build | AGP 8.5.2, Gradle 8.10.2 |
| UI | Jetpack Compose BOM 2024.09.03, Material 3 1.3.0 |
| Navigation | navigation-compose 2.8.0 |
| DI | Hilt 2.52 (KSP) |
| Network | Retrofit 2.11 + OkHttp 4.12 + Moshi 1.15 (codegen) |
| Async | Coroutines 1.8.1, Flow, StateFlow |
| Storage | DataStore-preferences 1.1.1, EncryptedSharedPreferences |
| Images | Coil 2.7.0 |
| QR (TOTP) | ZXing 3.5.3 |

Single `:app` module. Detailed architecture lives under [`_design/`](_design).

## Quick start

```bash
# 1. Clone
git clone https://github.com/YOUR_USERNAME/bug-hunter-android.git
cd bug-hunter-android

# 2. Point at your Bug Hunter backend (default: https://www.bughunter.co.in)
#    Edit gradle.properties → bh.baseUrl=https://your-backend.example.com

# 3. Open in Android Studio (Koala or newer) → Run ▶
```

Detailed setup, USB/wireless deploy paths, and APK sideload steps in [RUNNING.md](RUNNING.md).

## Project layout

```
app/src/main/java/com/bughunter/
├── core/
│   ├── data/         repositories + local storage
│   ├── domain/       cross-cutting use cases
│   ├── nav/          NavHost, AppShell, sidebar, top bar
│   ├── network/      Retrofit, interceptors, DTOs, cookie jar
│   └── ui/           theme tokens + reusable Bh* composables
└── feature/          one folder per screen family
    ├── auth/         login, signup, TOTP, reset, accept-invite
    ├── dashboard/    KPI strip, type tabs, recent activity
    ├── bugs/         list, detail, create, filters, saved views
    ├── projects/     list, detail, settings, members
    ├── organizations/  settings, branding, members, invitations
    ├── events/       list, detail
    ├── audit/        audit log viewer
    ├── sessions/     active session admin
    ├── webhooks/     CRUD
    ├── settings/     profile, change password/email, 2FA
    ├── dsar/         data export
    └── chatbot/      Sleuth panel
```

## Backend

The app needs a running Bug Hunter backend. Configure the URL via the `bh.baseUrl` Gradle property at build time, or in-app at **Settings → Developer → Base URL** (debug builds only).

The backend API contract is mirrored in [`_design/api_catalog.md`](_design/api_catalog.md) and [`_design/schema_catalog.md`](_design/schema_catalog.md).

## Code quality (SonarQube)

Static analysis + coverage run through the `org.sonarqube` Gradle plugin
(not the Docker `sonar-scanner-cli` the Python backends use — Gradle projects
are analysed in-process). Coverage comes from **Kover**, whose JaCoCo-format
XML SonarQube ingests.

```bash
# 1. Run the JVM unit suite + emit coverage XML
JAVA_HOME=<jdk-17+> ./gradlew testDebugUnitTest koverXmlReportDebug

# 2. Full analysis + upload (needs a SonarQube token)
SONAR_TOKEN=sqp_xxxx ./scripts/sonar-scan.sh      # macOS/Linux
#  .\scripts\sonar-scan.ps1                         # Windows PowerShell
```

The project key is **`Bug-Hunter-Android`**. Other helpers mirror the
backends:

| Script | Purpose |
| --- | --- |
| `scripts/sonar-scan.{ps1,sh}` | run tests + coverage, then `gradle sonar` |
| `scripts/sonar-export.ps1` | pull open issues to `sonar-issues.{json,csv}` |
| `scripts/sonar-mark-hotspots-safe.ps1` | bulk-mark reviewed hotspots SAFE |

**Coverage scope.** Compose UI (`*Screen.kt`, `ui/components`, `ui/theme`,
`core/nav`, charts, chat bubbles), the DI graph, and generated code are
excluded from the coverage denominator in both the Kover report
(`app/build.gradle.kts`) and `sonar.coverage.exclusions` (root
`build.gradle.kts`) — they're exercised by instrumented (emulator) tests, not
the JVM unit suite. The graded surface is the testable logic: ViewModels,
repositories, interceptors, mappers, use-cases, and the `core/ui/util`
validators/formatters/sanitizer.

## Contributing

PRs welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first, run the test suite before opening a PR.

## Security

Found a vulnerability? Don't open a public issue — see [SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE.txt).
