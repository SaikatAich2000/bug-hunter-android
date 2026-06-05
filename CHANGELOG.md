# Changelog

All notable changes to Bug Hunter Android. Format follows [Keep a Changelog](https://keepachangelog.com/).

## [2.8.0] — 2026-06-05

Initial public release of the native Android client for Bug Hunter.

### Added

- **Auth**: login, signup, TOTP / recovery codes, password reset, accept-invite.
- **Dashboard**: KPI strip (Total / Open / Resolved / Closed / Resolve Later), type tabs (All / Bugs / Requirements / Tasks), recent items, recent activity.
- **Work items**: list with type tabs, filters (project, status, priority, environment, assignee), search, paginated. Detail view with comments, attachments, custom fields. Create / edit. Saved views.
- **Projects**: list + create, detail, settings, member management.
- **Organizations**: settings, branding, members, invitations, webhooks.
- **Events**: list and detail.
- **Audit log** viewer.
- **Sessions** admin and per-user "my sessions".
- **Profile**: change password, change email, 2FA enrolment with QR.
- **DSAR**: data export, account deletion.
- **Sleuth chatbot** panel.
- **Adaptive UI**: bottom nav on phones, nav rail on small tablets, permanent drawer on large screens.
- **Theme**: light + dark with manual override (System / Light / Dark) wired to the Bug Hunter brand palette.
- **Top app bar** with Bug Hunter logo, page title, theme toggle, settings shortcut, and hamburger menu (Compact).
- **Adaptive launcher icon** with brand-colored background.
- **Sign-out confirmation** dialog.
- **Password fields**: show/hide toggle.

### Security

- `EncryptedSharedPreferences` cookie jar (AES-256-GCM, Android Keystore-backed master key).
- CSRF double-submit interceptor.
- 401 → auto-logout.
- HTTP body logging redacts `Cookie` / `Set-Cookie` and runs only in debug builds.
- Cloud backup excludes the cookie store via `data_extraction_rules.xml`.
- Cleartext traffic disabled; HTTPS only.

### Tech baseline

Kotlin 2.0.20 · AGP 8.5.2 · Gradle 8.10.2 · Compose BOM 2024.09.03 · Material 3 1.3.0 · Hilt 2.52 · Retrofit 2.11 · OkHttp 4.12 · Moshi 1.15 · Coroutines 1.8.1 · DataStore 1.1.1 · security-crypto 1.1.0-alpha06 · Coil 2.7.0 · Navigation-compose 2.8.0 · ZXing 3.5.3.

### Known limitations

- No offline cache — online-only.
- No multi-org switching.
- No biometric unlock yet — OS lock screen is the only barrier.
- No analytics / crash reporter wired in.
- English only.
- No widgets, quick-settings tiles, or Wear OS variant.
