# Security Policy

## Supported versions

Only the latest minor release receives security fixes.

| Version | Supported |
|---------|-----------|
| 2.8.x   | ✅        |
| < 2.8   | ❌        |

## Reporting a vulnerability

**Please do not open a public GitHub issue for security findings.**

Use **GitHub's private vulnerability reporting** (repo *Security* tab → *Report a vulnerability*). This keeps the report confidential until a fix ships.

Include:

- App `versionName` / `versionCode` (Settings → About, or `app/build.gradle.kts`).
- Backend version the app was pointed at.
- Reproduction steps and impact assessment.
- Suggested fix (optional).

## Response

- Acknowledgement within **5 working days**.
- Triaged severity within **10 working days**.
- Patches on a best-effort timeline; coordinated disclosure preferred.
- Credit on request once the fix is public.

## What the app does to protect users

| Concern | Mitigation |
|---|---|
| Session theft from local storage | `EncryptedSharedPreferences` (AES-256-GCM, Android Keystore-backed master key) for the cookie jar. |
| Cloud backup leak | `data_extraction_rules.xml` + `backup_rules.xml` exclude `bh_cookies.xml` and `auth_prefs.preferences_pb`. |
| CSRF | Double-submit interceptor reads the `bh_csrf` cookie and sets `X-CSRF-Token` on every mutating non-exempt request. |
| Hostile MITM | Cleartext traffic disabled — HTTPS only. Custom CAs disallowed in release builds. |
| HTTP body leak in logs | `HttpLoggingInterceptor` runs at `BODY` level only in debug builds, with `Cookie` and `Set-Cookie` headers redacted. |
| Unsafe attachment uploads | `UploadAttachmentsUseCase` blocks executable extensions (`.exe`, `.bat`, `.sh`, `.dll`, etc.). |
| Account-existence enumeration | Identical inline error copy for wrong-password vs unknown-email. |
| Weak passwords | Strength meter on every password set screen (8+ chars, letter + digit minimum). |
| Compromised device | OS lock screen is the only barrier in v2.8.x. Biometric prompt planned for a future release. |

The backend enforces many of these defenses server-side; the Android client mirrors the responses to avoid client-side leaks.
