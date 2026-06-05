# Bug Hunter — Android Auth & Session State Machine

Authoritative reference for the Android client. Derived from the server source of truth:
`app/routes/auth.py`, `app/routes/totp.py`, `app/routes/invitations.py`, `app/auth.py`,
`app/csrf.py`, `app/account_lockout.py`, `app/password_breach.py`.

All endpoints are rooted at `${API_BASE_URL}` (e.g. `https://bh.example.com`). The server
exposes a cookie-based session — there is no Bearer/JWT header path. The Android client
must behave like a browser SPA: persist cookies, echo the CSRF cookie as a header on every
mutating request, and treat `401` as "session ended".

---

## Cookie session

### Server-side facts

| Property | Value | Source |
| --- | --- | --- |
| Cookie name | `bh_session` | `auth.COOKIE_NAME` |
| Path | `/` | `set_session_cookie` |
| HttpOnly | `true` | `set_session_cookie` |
| SameSite | `Lax` | `set_session_cookie` |
| Secure | `settings.COOKIE_SECURE` (true in prod, false in local dev) | `set_session_cookie` |
| TTL (max_age) | `settings.SESSION_TTL_SECONDS` | `set_session_cookie` |
| Signer | `itsdangerous.TimestampSigner` with salt `"bh-session-v4"` and key `SESSION_SECRET` | `auth._signer` |
| Payload | `"{user_id}:{session_version}:{jti}"` signed + timestamped | `make_session_token` |

The cookie is issued by:
- `POST /api/auth/signup` (201)
- `POST /api/auth/login` (200) — only when the user does NOT have TOTP enabled
- `POST /api/auth/login/totp` (200) — second step when TOTP IS enabled
- `POST /api/auth/change-password` (204) — server rotates the session
- `POST /api/invitations/accept` (200)

The cookie is cleared by `POST /api/auth/logout` (204).

Per-session revocation: the cookie carries a `jti` that maps to a row in `sessions`. The
server deletes the row on logout and on password change (deletes ALL of the user's
sessions). The cookie still validates the signature but `get_current_user` will reject it
because the `jti` row is gone — resulting in `401`.

Session-version invalidation: the cookie also carries `session_version`. Password change /
reset bumps `user.session_version`, invalidating every previously-issued cookie globally
(401 on next request).

### Android implication

The session cookie is a long-lived bearer credential — it must be persisted across
process death and stored encrypted at rest.

**Recommended stack:**

```
OkHttp(...)
  .cookieJar(EncryptedPersistentCookieJar)
  .addInterceptor(AuthInterceptor)
  .addInterceptor(CsrfInterceptor)
```

Two acceptable implementations:

1. **`PersistentCookieJar` (franmontiel/PersistentCookieJar) + Tink/Jetpack Security.**
   Wrap its `SharedPrefsCookiePersistor` with `EncryptedSharedPreferences` so the cookie
   payload sits inside a Keystore-encrypted blob. Simpler to drop in.

2. **Custom `CookieJar` backed by Jetpack DataStore (Preferences) with
   `EncryptedFile` / Tink AEAD.** Preferred for new code — DataStore is the modern
   replacement for SharedPreferences, gives a Flow-based API, and avoids the
   PersistentCookieJar maintenance status.

**Do NOT use plain `SharedPreferences`** or write the cookie to external storage. The
cookie is httpOnly to keep the WebView/JS layer away from it; on Android the equivalent
threat is another app reading unencrypted prefs from a rooted device or a misconfigured
backup. Mark the persistence file with `android:allowBackup="false"` or explicitly
exclude it via `dataExtractionRules`.

Cookie domain: the server does not set `Domain=`, so OkHttp will scope it to the host
that issued it. If the client also talks to a staging host, use a separate jar.

---

## CSRF

### Server-side facts

| Property | Value | Source |
| --- | --- | --- |
| Cookie name | `bh_csrf` | `csrf.CSRF_COOKIE` |
| Header name | `X-CSRF-Token` | `csrf.CSRF_HEADER` |
| HttpOnly | `false` (load-bearing — SPA must read it) | `issue_csrf_cookie` |
| SameSite | `Lax` | `issue_csrf_cookie` |
| Secure | `settings.COOKIE_SECURE` | `issue_csrf_cookie` |
| Max-Age | `settings.SESSION_TTL_SECONDS` | `issue_csrf_cookie` |
| Pattern | Double-submit: header value MUST match cookie value | `CSRFMiddleware.dispatch` |
| Comparison | `secrets.compare_digest` (constant-time) | `CSRFMiddleware.dispatch` |

The cookie is seeded by the server on GETs to `/`, `*.html`, `/login`, `/signup` when
absent. The Android client will never hit those HTML routes, so the client must be
prepared to receive a cookie on the first authenticated response too — the middleware
always seeds when absent on those entry points, but for API-only clients the first
seed happens via any response that sets it (currently the login/signup/accept handlers do
not explicitly mint a CSRF cookie; the middleware seeds it on the document GETs).

**Effective rule for the Android client:** the CSRF cookie is born when the first GET
that returns `Set-Cookie: bh_csrf=…` happens. If the cookie jar does not yet have one,
the client should call a cheap GET (e.g. `GET /api/auth/me`) right after login to give
the middleware a chance to mint it. In practice the login response chain already pulls
through a GET (the SPA loads `/index.html` first); the Android client will need to
synthesize one.

Methods that require the header:
- ALL `POST`, `PUT`, `PATCH`, `DELETE` to paths starting with `/api/`
- EXCEPT the bootstrap paths in `_EXEMPT_PATHS`:
  - `POST /api/auth/login`
  - `POST /api/auth/signup`
  - `POST /api/auth/forgot-password`
  - `POST /api/auth/reset-password`
  - `POST /api/invitations/accept`

`GET`, `HEAD`, `OPTIONS` are always exempt.

On mismatch the server returns `403 {"detail": "CSRF check failed. Reload the page and try again."}`.

### Android implication — interceptor

```kotlin
class CsrfInterceptor(private val cookieJar: CookieJar, private val httpUrl: HttpUrl) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val needsCsrf = req.method in MUTATING && !EXEMPT.any { req.url.encodedPath == it }
        if (!needsCsrf) return chain.proceed(req)

        val csrf = cookieJar.loadForRequest(httpUrl)
            .firstOrNull { it.name == "bh_csrf" }?.value
            ?: return chain.proceed(req) // first call before cookie is seeded — server will 403; trigger a /me to seed

        val withHeader = req.newBuilder().header("X-CSRF-Token", csrf).build()
        return chain.proceed(withHeader)
    }
    companion object {
        private val MUTATING = setOf("POST", "PUT", "PATCH", "DELETE")
        private val EXEMPT = setOf(
            "/api/auth/login", "/api/auth/signup",
            "/api/auth/forgot-password", "/api/auth/reset-password",
            "/api/invitations/accept",
        )
    }
}
```

On 403 with that exact detail string the repository layer should:
1. fire a `GET /api/auth/me` to re-seed `bh_csrf`,
2. retry the original call once,
3. if it still 403s, surface the error.

---

## Login flow (state diagram)

```
                            +---------------------+
                            |  UNAUTHENTICATED    |
                            +----------+----------+
                                       |
              POST /api/auth/login     |
              (email, password)        |
                                       v
              +------------------------+--------------------------+
              |                                                   |
   200 + Set-Cookie bh_session                 200 {requires_totp:true, pending_token}
   (TOTP off)                                  (TOTP on)
              |                                                   |
              v                                                   v
   +-----------------+                                  +-------------------+
   | AUTHENTICATED   |<------+                          |  AWAITING_TOTP    |
   +-----------------+       |                          +---------+---------+
        ^   |                |                                    |
        |   | 401 on any     |    POST /api/auth/login/totp       |
        |   | API call       |    (pending_token, code)           |
        |   v                |                                    v
        |   logout            \---- 200 + Set-Cookie bh_session --/
        |                                                         |
        +---------------------------------------------------------+

   ANY login step with 429:
              |
              v
   +---------------------+
   |  LOCKED_OUT(until)  |  retry-after seconds from header
   +---------------------+
```

### Transition: UNAUTHENTICATED → AUTHENTICATED (no 2FA) or AWAITING_TOTP (2FA on)

- **Endpoint:** `POST /api/auth/login`
- **Request body (JSON):**
  ```json
  { "email": "alice@example.com", "password": "..." }
  ```
- **CSRF:** not required (exempt path).
- **Success (TOTP off) — 200:** body is `MeOut`:
  ```json
  {
    "id": 7, "name": "Alice", "email": "alice@example.com", "role": "admin",
    "is_active": true, "org_id": 3,
    "organization_name": "Acme", "organization_slug": "acme",
    "totp_enabled": false,
    "branding": { "logo_data_url": null, "accent_color": null }
  }
  ```
  Server set `Set-Cookie: bh_session=...`. Transition to AUTHENTICATED.
- **Success (TOTP on) — 200:**
  ```json
  { "requires_totp": true, "pending_token": "<opaque, max 400 chars>" }
  ```
  NO session cookie set. Transition to AWAITING_TOTP. The `pending_token` is short-lived
  (see `app/totp.py`); treat it as opaque and store it in memory only, never on disk.
- **Failure — 401:**
  ```json
  { "detail": "Invalid email or password" }
  ```
  Covers unknown email, wrong password, and disabled account (deliberately unified to
  prevent enumeration). Increment local fail counter for UX hints; do NOT auto-retry.
- **Failure — 429 (locked out):**
  ```json
  { "detail": "Too many failed sign-in attempts. Please try again later." }
  ```
  Response includes `Retry-After: <seconds>` header. Transition to
  `LOCKED_OUT_UNTIL(now + retryAfter)`. Defaults: 10 fails per 15 min triggers a 15-min
  lock (env-configurable on the server).
- **Failure — 400 (breach):** not raised by login (breach check only runs on signup / set
  / reset paths).
- **Retry window:** unbounded between attempts, but each failure ticks the lockout
  bucket on the server. The Android client should add a small UI backoff (1–2 s) after
  a 401 to discourage frantic retries.

### Transition: AWAITING_TOTP → AUTHENTICATED

- **Endpoint:** `POST /api/auth/login/totp`
- **Request body (JSON):**
  ```json
  { "pending_token": "<from prior step>", "code": "123456" }
  ```
  `code` is 6–20 chars (6-digit TOTP or 11-char recovery code; server uppercases & strips).
- **CSRF:** NOT in `_EXEMPT_PATHS` strictly, BUT a successful login from the prior step
  did not yet seed the session cookie. The path `/api/auth/login/totp` is not exempt; it
  IS a `POST /api/...` so the middleware will check it. The client must already have a
  `bh_csrf` cookie at this point — if missing, run a `GET /api/auth/me` (will 401 but
  may seed the cookie). Practically: the first GET that hit a `*.html` or `/login` route
  would have seeded it; for the Android client, seed by performing any GET that returns
  through the CSRF middleware before the TOTP step.
- **Success — 200:** body is `MeOut` (same shape as login success above), `Set-Cookie:
  bh_session`. Transition to AUTHENTICATED.
- **Failure — 400 expired token:**
  ```json
  { "detail": "Login session expired. Sign in again." }
  ```
  Transition back to UNAUTHENTICATED, clear `pending_token`.
- **Failure — 400 invalid token:**
  ```json
  { "detail": "Login session invalid. Sign in again." }
  ```
  Same handling — back to UNAUTHENTICATED.
- **Failure — 400 wrong code:**
  ```json
  { "detail": "Invalid code. Try again or use a recovery code." }
  ```
  Stay in AWAITING_TOTP. Ticks the lockout counter (server-side).
- **Failure — 429:** as for login. Transition to LOCKED_OUT.
- **Retry window:** stay on the screen; allow re-entry until pending_token expires.

### Transition: AUTHENTICATED → UNAUTHENTICATED (logout)

- **Endpoint:** `POST /api/auth/logout`
- **CSRF:** required.
- **Success — 204:** clears `bh_session` cookie. Clear local AuthState.
- **No body.** Logout is idempotent: even with an invalid cookie, it returns 204.

### Transition: AUTHENTICATED → UNAUTHENTICATED (server-forced)

- Any API call returns `401 {"detail": "Not authenticated"}`. Reasons:
  - Cookie signature invalid / expired (TTL elapsed)
  - `user.session_version` bumped (password change/reset somewhere else)
  - `jti` row deleted (this session revoked by an admin or by password change)
  - User deactivated

Handling: see [401 auto-logout policy](#401-auto-logout-policy) below.

### State: LOCKED_OUT_UNTIL(instant)

- Entered on any `429` from `/api/auth/login` or `/api/auth/login/totp`.
- Exit: when `Instant.now() >= instant`, transition to UNAUTHENTICATED.
- While locked, the UI should disable the login button and show a countdown derived from
  `Retry-After`.
- A success on `login` clears the bucket server-side via `account_lockout.clear`; a
  success on `login/totp` does the same.

---

## Signup flow

```
   UNAUTHENTICATED
        |
        |  POST /api/auth/signup
        |  { name, email, password, organization_name }
        v
   +---------------------------------------------+
   | 201 + Set-Cookie bh_session  -> AUTHENTICATED |
   | 403 public signup disabled                    |
   | 409 email already exists                      |
   | 400 password breached                         |
   +---------------------------------------------+
```

- **Endpoint:** `POST /api/auth/signup`
- **CSRF:** not required (exempt).
- **Request body — `SignupIn`:** `name`, `email`, `password`, `organization_name`.
- **Success — 201:** body is `MeOut` with `role: "admin"` (signup user owns the new org).
  `Set-Cookie: bh_session` already issued. Transition to AUTHENTICATED.
- **Failure — 403:** `{"detail": "Public sign-up is disabled. Ask your administrator for an invite."}`
- **Failure — 409 duplicate:** `{"detail": "An account with that email already exists. Try signing in."}`
- **Failure — 400 breach:** `{"detail": "This password appears in a known breach corpus. Please choose a different one."}`
- **Breach check:** HIBP via k-anonymity. Fail-open server-side on network error; the
  client cannot detect this — just trust the 400.

### Accept-invite alternative path

```
   UNAUTHENTICATED
        |
        |  GET /api/invitations/preview/{token}   (no CSRF, no auth)
        v
   +-------------------------------------+
   | 200 InvitationPreview               |
   | 404/400 invalid / used / revoked / expired |
   +-------------------------------------+
        |
        |  POST /api/invitations/accept
        |  { token, name, password }
        v
   +-------------------------------------+
   | 200 + Set-Cookie bh_session  -> AUTHENTICATED |
   | 400 invalid token / used / expired           |
   | 409 email already registered                  |
   +-------------------------------------+
```

- **Preview:** `GET /api/invitations/preview/{token}` — returns `email`, `organization_name`,
  `role`, `expires_at`, `invited_by_name`. Use to render the accept screen with confidence
  before the user types a password.
- **Accept:** `POST /api/invitations/accept` — body `{ token, name, password }`.
  Returns `MeOut`-like body (no `totp_enabled` / `branding` fields — note the slight
  divergence; the Android model should tolerate missing keys here).
- Both paths are CSRF-exempt.
- Invitation TTL is 7 days (`INVITATION_TTL`).

---

## TOTP flows

All TOTP endpoints (except the login-step ones documented above) require an
**authenticated session** (`bh_session` + valid `bh_csrf` header on mutations).

### Status

- **Endpoint:** `GET /api/auth/2fa/status`
- **Success — 200:**
  ```json
  { "enabled": true, "enrolled_at": "2026-01-15T10:22:00Z", "unused_recovery_codes": 8 }
  ```
- If `TOTP_ENABLED` is false site-wide, returns `{"enabled": false}`.

### Enable

1. **Begin:**
   - `POST /api/auth/2fa/begin` (no body)
   - 200: `{ "secret": "JBSWY...", "otpauth_uri": "otpauth://totp/Bug%20Hunter:alice@example.com?secret=...&issuer=Bug%20Hunter" }`
   - 403 if `TOTP_ENABLED=false`.
   - 409 if already enabled (must disable first).
   - **Client renders the QR locally** from `otpauth_uri` (e.g. ZXing). The server does
     NOT return a base64 QR image — only the URI + raw secret.

2. **Confirm:**
   - `POST /api/auth/2fa/confirm` body `{ "code": "123456" }` (6–10 chars).
   - 200: `{ "enabled": true, "recovery_codes": ["ABCD-EFGH-IJKL", ...] }`
   - 400 if code wrong or no enrolment in progress.
   - 409 if already enabled.
   - **Recovery codes are shown ONCE** — render a screen forcing the user to copy/save
     them before navigating away. Do not persist them in app storage.

### Disable

- `POST /api/auth/2fa/disable` body `{ "password": "..." }` (sudo-mode re-auth).
- 204 on success. Recovery codes invalidated server-side.
- 400 `{"detail": "Current password is incorrect."}` on bad password.
- Note: the server endpoint asks for password only — the on-device prompt only needs the
  password, not a fresh TOTP code (this differs from the prompt suggested in the
  task brief; trust the source).

### Regenerate recovery codes

- `POST /api/auth/2fa/recovery-codes/regenerate` (no body).
- 200: `{ "enabled": true, "recovery_codes": [...] }`. Old codes invalidated.
- 400 if 2FA not enabled.

### Login second step

Documented in the [login flow](#login-flow-state-diagram). Endpoint:
`POST /api/auth/login/totp` body `{ pending_token, code }`. The path takes `email` only
implicitly — `pending_token` encodes the user id (verified server-side). The Android
client SHOULD NOT pass `email` again here.

---

## Password reset

```
   UNAUTHENTICATED
        |
        |  POST /api/auth/forgot-password { email }
        v
   204 always (enterprise mode — no enumeration)
   404 only if ALLOW_ACCOUNT_ENUMERATION=true (consumer mode)
        |
        |  user clicks email link with raw token
        v
   POST /api/auth/reset-password { token, new_password }
        |
        +-- 204 on success (all sessions invalidated server-side; user must log in again)
        +-- 400 invalid/expired token
        +-- 400 breached password
```

- **Forgot password:**
  - `POST /api/auth/forgot-password` body `{ "email": "..." }`.
  - CSRF-exempt.
  - **Always shows 204 in enterprise mode** — the client must NEVER tell the user
    whether the email exists; show a generic "Check your inbox" message.
  - Token TTL: 2 hours (`PASSWORD_RESET_TTL`).

- **Reset password:**
  - `POST /api/auth/reset-password` body `{ "token": "...", "new_password": "..." }`.
  - CSRF-exempt.
  - 204 on success. Invalidates all of the user's existing sessions (bumps
    `session_version` and deletes session rows). The client should redirect to login.
  - 400 `{"detail": "Invalid or expired reset token"}` on bad token / used token /
    inactive user.
  - 400 breach detail (same string as signup) if `new_password` is in HIBP corpus.

- **Password rules:**
  - Server-side minimum: enforced via Pydantic `ResetPasswordIn`/`SignupIn` schemas
    (length floor; see `app/schemas.py` — typical Bug Hunter minimum is 8 chars).
  - Breach: rejected via HIBP k-anonymity (`is_password_breached`).
  - bcrypt input is SHA-256 pre-hashed, so 72-byte limit doesn't matter — clients can
    send arbitrarily long passwords.
  - **Android client should mirror the breach check messaging** but never replicate the
    check locally — always defer to server response.

- **Change password (authenticated):**
  - `POST /api/auth/change-password` body `{ "current_password", "new_password" }`.
  - CSRF required.
  - 204: server rotates the session cookie (deletes all other sessions, issues a fresh
    one for this caller). The OkHttp jar will pick up the new cookie automatically; the
    other devices' sessions are now dead.
  - 400 wrong current password / breached new password.

---

## Email change

Two-step verified change.

```
   AUTHENTICATED
        |
        |  POST /api/auth/email-change/request
        |  { current_password, new_email }
        v
   202 {"message": "Verification code sent to ..."}
   400 wrong password / same email
   409 email already in use
        |
        |  user reads 6-digit code from inbox
        v
   POST /api/auth/email-change/confirm { code }
        |
        +-- 200 MeOut with updated email
        +-- 400 wrong code (with attempts remaining)
        +-- 400 too many attempts / code expired
        +-- 409 email claimed during 15-min window (race)
```

- **Request:** sudo-mode (current password required), 15-minute TTL, max 5 wrong-code
  attempts. CSRF required.
- **Confirm:** CSRF required. Returns the refreshed `MeOut` — the Android client must
  update its cached user from this response.
- **No cookie rotation** — same `bh_session` continues to be valid.

---

## Error envelope

The server always uses FastAPI's `HTTPException` shape:

```json
{ "detail": "<string>" }
```

`detail` is always a string for these endpoints (the server never returns the list form
that some FastAPI validators emit; for `422` validation errors from Pydantic it WILL be
a list of dicts — handle both shapes in the parser, e.g. `String | List<ValidationError>`).

### Critical detail strings the client must detect

| Status | Detail (exact) | Meaning / handling |
| --- | --- | --- |
| 401 | `Not authenticated` | Session cookie missing/invalid/expired/revoked. Force logout. |
| 401 | `Invalid email or password` | Login failed (unknown email, wrong password, or inactive). Show generic error. |
| 429 | `Too many failed sign-in attempts. Please try again later.` | Honor `Retry-After` header. Transition to LOCKED_OUT. |
| 400 | `This password appears in a known breach corpus. Please choose a different one.` | Show as inline field error on the password input. |
| 400 | `Invalid or expired reset token` | Reset page should send user back to /forgot-password. |
| 400 | `Login session expired. Sign in again.` | Drop pending TOTP, back to login. |
| 400 | `Login session invalid. Sign in again.` | Same — drop pending TOTP. |
| 400 | `Invalid code. Try again or use a recovery code.` | Stay on TOTP step; clear the code field. |
| 400 | `Current password is incorrect` / `Current password is incorrect.` | Show on the current-password field. (Note: trailing period varies by endpoint — match prefix.) |
| 400 | `Wrong code. N attempt(s) left.` | Email-change. Surface the remaining count. |
| 400 | `Too many wrong codes. Start the change again.` | Email-change. Reset flow. |
| 400 | `Code expired. Start the change again.` | Email-change. Reset flow. |
| 403 | `CSRF check failed. Reload the page and try again.` | Re-seed cookie via `GET /api/auth/me`, retry once. |
| 403 | `Public sign-up is disabled. Ask your administrator for an invite.` | Hide signup affordance going forward. |
| 409 | `An account with that email already exists. Try signing in.` | Surface on email field; offer "Sign in" link. |
| 409 | `That email is already in use. Try signing in with it instead.` | Email-change variant. |
| 404/400 | `Invalid or expired invitation` / `This invitation has already been used.` / `This invitation has been revoked.` / `This invitation has expired.` | Accept-invite screen — surface, no retry. |

The client SHOULD match by exact equality (and accept the small period-vs-no-period
variance noted above). Substring/contains matching is also acceptable since the strings
are stable in the source.

---

## Recommended Android architecture

### Sealed AuthState

```kotlin
sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class AwaitingTotp(
        val pendingToken: String,
        val email: String,           // for display only
    ) : AuthState
    data class Authenticated(
        val me: Me,                  // MeOut shape
    ) : AuthState
    data class LockedOut(
        val until: Instant,
    ) : AuthState
}
```

The ViewModel exposes a `StateFlow<AuthState>` and a one-shot `Channel<AuthEvent>` for
side-effects (toast / nav). `AwaitingTotp` and `LockedOut` are transient — never
persisted to disk:

- `pendingToken` is short-lived server-side; if the process dies, the user must restart
  the login.
- `LockedOut.until` is derived from `Retry-After`; on cold start re-derive from the next
  attempt's response.

`Authenticated.me` SHOULD be cached (DataStore) so the app can boot to a logged-in shell
without waiting for `/api/auth/me`. Always refresh on resume — if `/me` 401s, drop to
`Unauthenticated`.

### Session DataStore keys

A single `Preferences` DataStore named `auth_prefs.preferences_pb` (or an
`EncryptedFile`-backed Proto DataStore for the cookie blob):

| Key | Type | Notes |
| --- | --- | --- |
| `cookie_jar_blob` | `ByteArray` (encrypted) | Serialized cookies; owned by the CookieJar. |
| `last_known_me` | `String` (JSON) | Cached `MeOut` for warm start; refreshed on every `/me` success. |
| `last_known_org_id` | `Int` | Used by repository scoping; redundant with `last_known_me`. |
| `last_locked_out_until_epoch_ms` | `Long` | Only set when user explicitly hit a lockout; cleared on first successful login. |
| `totp_enabled_cached` | `Boolean` | For UI prompts before /me returns. |

Do NOT store `pending_token`, recovery codes, raw password, or CSRF token in DataStore.

### AuthInterceptor responsibilities

The OkHttp interceptor chain has three layers, in this order:

1. **`CookieJar`** — built-in OkHttp mechanism. Persists `bh_session` + `bh_csrf` via the
   encrypted persistor described above. Nothing custom needed beyond the jar.

2. **`CsrfInterceptor`** — for `POST`/`PUT`/`PATCH`/`DELETE` to `/api/*` that is NOT in
   the exempt set, copy the `bh_csrf` cookie value into the `X-CSRF-Token` header.

3. **`AuthInterceptor`** — observes responses:
   - On `401`: emit an `AuthEvent.Unauthorized` to the ViewModel layer (see policy
     below). Do NOT retry.
   - On `403` with detail `CSRF check failed. ...`: try to re-seed (single `GET /api/auth/me`),
     replay the original request once. If still 403, propagate.
   - On `429` with `Retry-After`: emit `AuthEvent.LockedOut(until)`. Pass the response
     through unchanged.
   - Adds a `User-Agent` (`Bug Hunter Android/<version>`) and `Accept: application/json`.

### 401 auto-logout policy

Any non-login API call that returns `401` is treated as session loss. The interceptor:

1. Drops the cookie jar (clear `bh_session` and `bh_csrf` entries; keep CSRF if reused
   across logins is desired, but simplest is full clear).
2. Wipes `last_known_me` from DataStore.
3. Emits a one-shot `AuthEvent.LoggedOut(reason = ServerExpired)`.
4. Root nav navigates to the login screen, popping the back stack.

Exception: the login and login/totp calls return 401 / 400 as part of normal failure
modes. The interceptor MUST NOT treat 401 from `/api/auth/login` or `/api/auth/login/totp`
as session loss — those map to in-screen error messages instead. Implement this with a
path allow-list in the interceptor:

```kotlin
private val IGNORED_FOR_AUTOLOGOUT = setOf(
    "/api/auth/login", "/api/auth/login/totp",
    "/api/auth/signup", "/api/auth/forgot-password",
    "/api/auth/reset-password", "/api/invitations/accept",
    "/api/invitations/preview",
)
```

Resume hook: on `Lifecycle.Event.ON_RESUME` from the root activity, fire a fire-and-forget
`GET /api/auth/me`. If it 200s, refresh `last_known_me`. If it 401s, the interceptor will
trigger the auto-logout path. This catches the "user changed password on another device"
case immediately.

Cold-start hook: same — issue `GET /api/auth/me` before showing the main UI; only paint
the cached `me` while the request is in flight to avoid a blank screen.

---

## Quick reference: endpoint table

| Path | Method | CSRF | Auth | Notes |
| --- | --- | --- | --- | --- |
| `/api/auth/signup` | POST | — | — | Issues session. |
| `/api/auth/login` | POST | — | — | May return `requires_totp`. |
| `/api/auth/login/totp` | POST | required | — | Trades `pending_token` + code for session. |
| `/api/auth/logout` | POST | required | required | 204. |
| `/api/auth/me` | GET | — | required | Whoami / session probe. |
| `/api/auth/change-password` | POST | required | required | Rotates session. |
| `/api/auth/forgot-password` | POST | — | — | Always 204 in enterprise mode. |
| `/api/auth/reset-password` | POST | — | — | Kills all sessions. |
| `/api/auth/profile` | PUT | required | required | Name only. |
| `/api/auth/email-change/request` | POST | required | required | Sudo-mode. |
| `/api/auth/email-change/confirm` | POST | required | required | 6-digit code. |
| `/api/auth/2fa/status` | GET | — | required | |
| `/api/auth/2fa/begin` | POST | required | required | Returns secret + otpauth URI. |
| `/api/auth/2fa/confirm` | POST | required | required | Returns recovery codes once. |
| `/api/auth/2fa/disable` | POST | required | required | Password-only sudo. |
| `/api/auth/2fa/recovery-codes/regenerate` | POST | required | required | |
| `/api/invitations/preview/{token}` | GET | — | — | |
| `/api/invitations/accept` | POST | — | — | Issues session. |
