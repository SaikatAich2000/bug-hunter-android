# Bug Hunter REST API Catalog

Source of truth for Android Retrofit interface generation. Derived from `docs/api/openapi.json` (v2.8), `docs/api/curl.md`, and `app/main.py`.

Base path: all endpoints are mounted under `/api/*`. Server example: `http://localhost:8000`.

## Auth Model

- Session cookie: `bh_session` — HTTP-only, set by `POST /api/auth/login`, `POST /api/auth/login/totp`, `POST /api/auth/signup`, `POST /api/invitations/accept`. Cleared by `POST /api/auth/logout`.
- CSRF (defence-in-depth, double-submit cookie):
  - Cookie name: `bh_csrf`
  - Header name: `X-CSRF-Token`
  - Required on every mutating call (POST/PUT/PATCH/DELETE on `/api/*`) EXCEPT `signup`, `login`, `forgot-password` (these are CSRF-exempt). Android client must read cookie, echo value in header on every other mutation.
  - Disabled when env `CSRF_PROTECTION=false` (dev only).
- Auth roles (string-valued on `MeOut.role`): `admin`, `manager`, `member`. "manager+" in this catalog means manager OR admin.
- Per-project scoping uses `ProjectMembership` with optional `is_lead=true`.

### Signup vs Login vs TOTP flow

1. Signup (first-run / public-signup):
   - `POST /api/auth/signup` with `SignupIn` → 201 + `MeOut` + sets `bh_session`.
2. Login (no 2FA):
   - `POST /api/auth/login` with `LoginIn` → 200 + sets `bh_session`. Response JSON shape: `{ "ok": true, "user": MeOut }` (untyped in spec — treat as JSON object).
3. Login (2FA on):
   - `POST /api/auth/login` returns 200 with body `{ "pending_2fa": true, "pending_token": "<opaque>" }`. NO session cookie yet.
   - `POST /api/auth/login/totp` with `LoginTotpStepIn { pending_token, code }` (code is 6-digit TOTP or 11-char recovery) → 200 + sets `bh_session`.
4. Logout:
   - `POST /api/auth/logout` → 204, clears cookie.

## Standard Error Envelope

All error responses (4xx, 5xx) follow:
```json
{ "detail": "<string-or-validation-array>" }
```

Pydantic 422 returns `HTTPValidationError`:
```json
{ "detail": [ { "loc": ["body","field"], "msg": "...", "type": "..." }, ... ] }
```

## Standard HTTP Status Codes

| Code | Meaning in this API |
|---|---|
| 200 | Success with body |
| 201 | Created |
| 202 | Accepted (email-change request, webhook test) |
| 204 | Success no body (logout, delete, change-password, profile-step ops) |
| 400 | Bad request / invalid Content-Length |
| 401 | Missing/invalid session (or missing bearer on metrics) |
| 403 | CSRF token mismatch, RBAC denial, invalid metrics token |
| 404 | Not found (also: forgot-password with unknown email — by design) |
| 409 | Conflict (e.g. last-admin self-delete, duplicate email) |
| 413 | Request body exceeds `MAX_REQUEST_BODY_BYTES` |
| 422 | Pydantic validation error |
| 429 | Rate-limited; includes `Retry-After` header (seconds) |
| 500 | Server error |

## Rate Limits & Lockout

Per-IP POST limits (middleware in `app/main.py`). Returns 429 with `Retry-After: <seconds>` header and `{"detail":"Too many attempts. Please try again later."}`:

| Path | Limit | Window |
|---|---|---|
| POST /api/auth/login | 8 | 60 s |
| POST /api/auth/signup | 5 | 600 s |
| POST /api/auth/forgot-password | 3 | 60 s |
| POST /api/auth/email-change/request | 10 | 600 s |
| POST /api/auth/email-change/confirm | 20 | 60 s |
| POST /api/invitations/accept | 10 | 600 s |

Account lockout: after repeated bad-password attempts the server may return 423/429 or `403 {"detail":"Account temporarily locked"}` — client should honour any `Retry-After` regardless of code.

## CSV Endpoints (Accept: text/csv, response is raw CSV stream)

- `GET /api/bugs/export.csv`
- `GET /api/audit/export.csv`

## Multipart Endpoints (Content-Type: multipart/form-data)

- `POST /api/bugs/{bug_id}/attachments` — fields: `file` (binary, required), `comment_id` (int, optional). Server caps streamed upload at 50 MB.

Other binary streams (not multipart):
- `GET /api/bugs/{bug_id}/attachments/{att_id}/download` — returns file bytes with `Content-Disposition`.
- `GET /api/chat/download/{token}` — returns Excel workbook bytes.

---

## meta

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/health | Liveness + version + asset_version | — | `{status,version,asset_version}` (object<string,string>) | none | — | — |
| GET | /api/meta | Enum vocabularies + `allow_public_signup` flag | — | object (statuses, statuses_by_type, priorities, environments, allow_public_signup) | none | — | — |

## auth

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| POST | /api/auth/signup | Create org + admin user, set session | SignupIn | MeOut (201) | none | — | — |
| POST | /api/auth/login | Login step 1 (password) | LoginIn | object (sets cookie OR returns `{pending_2fa, pending_token}`) | none | — | — |
| POST | /api/auth/login/totp | Login step 2 (2FA / recovery) | LoginTotpStepIn | object (sets cookie) | none (uses pending_token) | — | — |
| POST | /api/auth/logout | Clear session | — | 204 | session-cookie | — | — |
| GET | /api/auth/me | Current user profile + branding hints | — | MeOut | session-cookie | — | — |
| POST | /api/auth/change-password | Change own password | ChangePasswordIn | 204 | session-cookie | — | — |
| POST | /api/auth/forgot-password | Email a reset token (404 if unknown — by product decision) | ForgotPasswordIn | 204 | none | — | — |
| POST | /api/auth/reset-password | Consume reset token, set new password | ResetPasswordIn | 204 | none (token-auth) | — | — |
| PUT | /api/auth/profile | Update own name (NOT email) | ProfileUpdateIn | MeOut | session-cookie | — | — |
| POST | /api/auth/email-change/request | Stage new email, mail verification code | EmailChangeRequestIn | 202 object<string,string> | session-cookie | — | — |
| POST | /api/auth/email-change/confirm | Confirm staged email with code | EmailChangeConfirmIn | MeOut | session-cookie | — | — |
| GET | /api/auth/2fa/status | TOTP enrolment status | — | TotpStatus | session-cookie | — | — |
| POST | /api/auth/2fa/begin | Begin TOTP enrolment (returns QR + secret) | — | TotpBeginOut | session-cookie | — | — |
| POST | /api/auth/2fa/confirm | Confirm enrolment with first code | TotpConfirmIn | TotpConfirmOut (recovery codes) | session-cookie | — | — |
| POST | /api/auth/2fa/disable | Disable 2FA (password-confirmed) | TotpDisableIn | 204 | session-cookie | — | — |
| POST | /api/auth/2fa/recovery-codes/regenerate | Re-roll recovery codes | — | TotpConfirmOut | session-cookie | — | — |

## totp

(All TOTP operationally live in `auth` tag; see `/api/auth/2fa/*` rows above. No separate `totp` tag in spec.)

## organizations

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/organization | Get caller's org | — | OrganizationOut | session-cookie | — | — |
| PUT | /api/organization | Update caller's org name/desc | OrganizationUpdate | OrganizationOut | admin | — | — |

## users

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/users | List org users | — | UserOut[] | session-cookie | `include_inactive`:bool=true, `q`:string? | — |
| POST | /api/users | Create user | UserIn | UserOut (201) | admin | — | — |
| GET | /api/users/{user_id} | Get user by id | — | UserOut | session-cookie | — | `user_id`:int |
| PUT | /api/users/{user_id} | Update name/email/role/active/password | UserUpdate | UserOut | admin | — | `user_id`:int |
| DELETE | /api/users/{user_id} | Deactivate or hard-delete user | — | object<string,string> | admin | — | `user_id`:int |

## memberships

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/projects/{project_id}/members | List project members | — | ProjectMembershipOut[] | session-cookie | — | `project_id`:int |
| POST | /api/projects/{project_id}/members | Add member | ProjectMembershipIn | ProjectMembershipOut (201) | manager+ | — | `project_id`:int |
| PUT | /api/projects/{project_id}/members/{user_id} | Change member role | ProjectMembershipUpdate | ProjectMembershipOut | manager+ | — | `project_id`:int, `user_id`:int |
| DELETE | /api/projects/{project_id}/members/{user_id} | Remove member | — | object (additionalProperties) | manager+ | — | `project_id`:int, `user_id`:int |

## invitations

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/invitations | List org invitations | — | InvitationOut[] | admin | — | — |
| POST | /api/invitations | Create invitation (email + role + project_ids + as_lead) | InvitationCreate | InvitationOut (201) | admin | — | — |
| DELETE | /api/invitations/{invitation_id} | Revoke invitation | — | object (additionalProperties) | admin | — | `invitation_id`:int |
| GET | /api/invitations/preview/{token} | Inspect invitation before acceptance | — | InvitationPreview | none (token-auth) | — | `token`:string |
| POST | /api/invitations/accept | Accept + create user account, set session | InvitationAccept | MeOut | none (token-auth) | — | — |

## projects

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/projects | List projects visible to caller | — | ProjectOut[] | session-cookie | — | — |
| POST | /api/projects | Create project | ProjectIn | ProjectOut (201) | admin | — | — |
| GET | /api/projects/{project_id} | Get project | — | ProjectOut | session-cookie | — | `project_id`:int |
| PUT | /api/projects/{project_id} | Update project | ProjectIn | ProjectOut | admin | — | `project_id`:int |
| DELETE | /api/projects/{project_id} | Delete project | — | object (additionalProperties) | admin | — | `project_id`:int |

## bugs

Note: `status`, `priority`, `environment`, `item_type`, `project_id`, `assignee_id` query params on list are arrays (repeat the param: `?status=New&status=In Progress`).

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/bugs/export.csv | Export bugs as CSV (Accept: text/csv) | — | text/csv stream | session-cookie | (same as list) | — |
| GET | /api/bugs | List/search bugs (paginated) | — | BugListResponse | session-cookie | `project_id`:int[]?, `status`:string[]?, `priority`:string[]?, `environment`:string[]?, `reporter_id`:int?, `assignee_id`:int[]?, `item_type`:string[]?, `event_id`:int?, `q`:string?, `page`:int=1, `page_size`:int=50 | — |
| POST | /api/bugs | Create bug/item | BugCreate | BugOut (201) | session-cookie | — | — |
| GET | /api/bugs/{bug_id} | Get full bug detail | — | BugDetail | session-cookie | — | `bug_id`:int |
| PUT | /api/bugs/{bug_id} | Update bug | BugUpdate | BugOut | session-cookie (per-project edit perms) | — | `bug_id`:int |
| DELETE | /api/bugs/{bug_id} | Delete bug | — | object<string,string> | manager+ or reporter | — | `bug_id`:int |
| POST | /api/bugs/bulk-update | Apply diff to many bugs (silently skips forbidden) | BulkUpdateIn | object (additionalProperties) | session-cookie | — | — |
| POST | /api/bugs/bulk-delete | Delete many bugs | BulkDeleteIn | object (additionalProperties) | manager+ | — | — |
| GET | /api/bugs/{bug_id}/comments | List comments | — | CommentOut[] | session-cookie | — | `bug_id`:int |
| POST | /api/bugs/{bug_id}/comments | Add comment | CommentIn | CommentOut (201) | session-cookie | — | `bug_id`:int |
| PUT | /api/bugs/{bug_id}/comments/{comment_id} | Edit comment (admin-only) | CommentIn | CommentOut | admin | — | `bug_id`:int, `comment_id`:int |
| DELETE | /api/bugs/{bug_id}/comments/{comment_id} | Delete comment (admin-only) | — | 204 | admin | — | `bug_id`:int, `comment_id`:int |
| POST | /api/bugs/{bug_id}/attachments | Upload attachment (multipart/form-data) | Body_upload_attachment (file, comment_id?) | AttachmentBrief (201) | session-cookie | — | `bug_id`:int |
| GET | /api/bugs/{bug_id}/attachments/{att_id}/download | Stream attachment bytes | — | binary stream + Content-Disposition | session-cookie | — | `bug_id`:int, `att_id`:int |
| DELETE | /api/bugs/{bug_id}/attachments/{att_id} | Delete attachment | — | object (additionalProperties) | manager+ or uploader | — | `bug_id`:int, `att_id`:int |
| GET | /api/bugs/{bug_id}/activity | Per-bug audit trail | — | ActivityOut[] | session-cookie | — | `bug_id`:int |

## events

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/events | List events (optionally for a date) | — | EventOut[] | session-cookie | `scheduled_for`:string? (YYYY-MM-DD) | — |
| POST | /api/events | Create event | EventCreate | EventOut (201) | manager+ | — | — |
| GET | /api/events/{event_id} | Get event detail + linked bugs | — | EventDetailOut | session-cookie | — | `event_id`:int |
| PUT | /api/events/{event_id} | Update event | EventUpdate | EventOut | manager+ | — | `event_id`:int |
| DELETE | /api/events/{event_id} | Delete event | — | object<string,string> | manager+ | — | `event_id`:int |

## stats

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/stats | Dashboard aggregates (KPIs, breakdowns, 14-day timeline) | — | StatsOut | session-cookie | `item_type`:string? (scope all aggregations except `by_type`/`event_count`) | — |

## saved_views

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/saved-views | List own + shared views | — | SavedViewOut[] | session-cookie | — | — |
| POST | /api/saved-views | Create saved view (filters JSON blob) | SavedViewIn | SavedViewOut (201) | session-cookie | — | — |
| GET | /api/saved-views/{view_id} | Get saved view | — | SavedViewOut | session-cookie | — | `view_id`:int |
| PUT | /api/saved-views/{view_id} | Update saved view | SavedViewUpdateIn | SavedViewOut | session-cookie (owner or admin) | — | `view_id`:int |
| DELETE | /api/saved-views/{view_id} | Delete saved view | — | 204 | session-cookie (owner or admin) | — | `view_id`:int |

## custom_fields

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/projects/{project_id}/custom-fields | List custom fields for project | — | CustomFieldOut[] | session-cookie | — | `project_id`:int |
| POST | /api/projects/{project_id}/custom-fields | Create field | CustomFieldIn | CustomFieldOut (201) | manager+ | — | `project_id`:int |
| PUT | /api/projects/{project_id}/custom-fields/{field_id} | Update field | CustomFieldUpdateIn | CustomFieldOut | manager+ | — | `project_id`:int, `field_id`:int |
| DELETE | /api/projects/{project_id}/custom-fields/{field_id} | Delete field | — | 204 | manager+ | — | `project_id`:int, `field_id`:int |
| GET | /api/bugs/{bug_id}/custom-values | List custom values for a bug | — | CustomValueOut[] | session-cookie | — | `bug_id`:int |
| PUT | /api/bugs/{bug_id}/custom-values | Set custom values (array body) | CustomValueOut[] (as request) | CustomValueOut[] | session-cookie | — | `bug_id`:int |

## audit

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/audit | List audit events (broad `q` matches IDs, names, titles, actions, types) | — | ActivityOut[] | admin | `entity_type`:string?, `actor_user_id`:int?, `q`:string?, `limit`:int=5000 (max 10000), `offset`:int=0 | — |
| GET | /api/audit/export.csv | Export filtered audit as CSV (Accept: text/csv) | — | text/csv stream | admin | `entity_type`:string?, `actor_user_id`:int?, `q`:string?, `limit`:int=10000 (max 100000) | — |

## sessions

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/sessions | List caller's active sessions | — | SessionOut[] | session-cookie | — | — |
| DELETE | /api/sessions/{session_id} | Revoke one session (own) | — | object<string,string> | session-cookie | — | `session_id`:int |

## branding

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/branding | Get org branding (logo, accent, email_from_override) | — | BrandingOut | session-cookie | — | — |
| PUT | /api/branding | Update org branding | BrandingIn | BrandingOut | admin | — | — |

## webhooks

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/webhooks | List org webhooks | — | WebhookOut[] | admin | — | — |
| POST | /api/webhooks | Create webhook (name, url, events[], is_active) | WebhookIn | WebhookOut (201) | admin | — | — |
| GET | /api/webhooks/{hook_id} | Get webhook | — | WebhookOut | admin | — | `hook_id`:int |
| PUT | /api/webhooks/{hook_id} | Update webhook | WebhookUpdateIn | WebhookOut | admin | — | `hook_id`:int |
| DELETE | /api/webhooks/{hook_id} | Delete webhook | — | 204 | admin | — | `hook_id`:int |
| POST | /api/webhooks/{hook_id}/test | Fire a test ping | — | object<string,string> (202) | admin | — | `hook_id`:int |

## dsar

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| GET | /api/auth/data-export | Export caller's own personal data (single JSON bundle) | — | object (additionalProperties) | session-cookie | — | — |
| DELETE | /api/auth/account | Permanently delete own account (409 if last admin) | DeleteAccountIn | 204 | session-cookie | — | — |

## chatbot

| Method | Path | Description | Request Body | Response | Auth | Query | Path |
|---|---|---|---|---|---|---|---|
| POST | /api/chat/ask | Natural-language Q&A (always 200; "no match" is in payload) | ChatIn | ChatOut | session-cookie | — | — |
| GET | /api/chat/download/{token} | Download a previously-staged Excel workbook | — | binary (xlsx) stream | session-cookie | — | `token`:string |

---

## Schema Reference (key request/response models)

Use these as Retrofit DTO names; field details live in `docs/api/openapi.json#/components/schemas/<Name>`.

- Auth in: `SignupIn`, `LoginIn`, `LoginTotpStepIn`, `ChangePasswordIn`, `ForgotPasswordIn`, `ResetPasswordIn`, `ProfileUpdateIn`, `EmailChangeRequestIn`, `EmailChangeConfirmIn`, `TotpConfirmIn`, `TotpDisableIn`, `DeleteAccountIn`.
- Auth out: `MeOut`, `TotpStatus`, `TotpBeginOut`, `TotpConfirmOut`.
- Org/branding: `OrganizationOut`, `OrganizationUpdate`, `BrandingIn`, `BrandingOut`, `BrandingInfo`.
- Users: `UserIn`, `UserUpdate`, `UserOut`, `UserBrief`.
- Invitations: `InvitationCreate`, `InvitationOut`, `InvitationPreview`, `InvitationAccept`.
- Projects: `ProjectIn`, `ProjectOut`.
- Memberships: `ProjectMembershipIn`, `ProjectMembershipUpdate`, `ProjectMembershipOut`.
- Bugs: `BugCreate`, `BugUpdate`, `BugOut`, `BugDetail`, `BugListResponse`, `BulkUpdateIn`, `BulkDeleteIn`.
- Comments/attachments: `CommentIn`, `CommentOut`, `AttachmentBrief`, `Body_upload_attachment_api_bugs__bug_id__attachments_post`.
- Activity/audit: `ActivityOut`.
- Events: `EventCreate`, `EventUpdate`, `EventOut`, `EventDetailOut`.
- Stats: `StatsOut`.
- Sessions: `SessionOut`.
- Webhooks: `WebhookIn`, `WebhookUpdateIn`, `WebhookOut`.
- Saved views: `SavedViewIn`, `SavedViewUpdateIn`, `SavedViewOut`.
- Custom fields: `CustomFieldIn`, `CustomFieldUpdateIn`, `CustomFieldOut`, `CustomValueOut`.
- Chatbot: `ChatIn`, `ChatOut`.
- Errors: `HTTPValidationError`, `ValidationError`.

## Notes for Android Implementer

- Always send `Accept: application/json` except CSV/binary endpoints; CSV endpoints use `Accept: text/csv` and attachment download yields opaque bytes.
- Carry the `bh_session` cookie across all calls (use OkHttp `CookieJar`).
- On every non-exempt mutation, read `bh_csrf` from cookies and add `X-CSRF-Token: <value>` header.
- Treat 401 globally as "session expired → relaunch login flow"; treat 403 with `CSRF`-related detail as "refetch cookie + retry once".
- Respect `Retry-After` on 429 with exponential backoff at minimum.
- `POST /api/bugs/{bug_id}/attachments` must use `multipart/form-data`; all other mutations are `application/json`.
- Bug list query params that are arrays must be sent as repeated keys (`status=New&status=In Progress`), not comma-joined.
- The `/api/auth/login` response is polymorphic: branch on presence of `pending_2fa` field to decide whether to call `/api/auth/login/totp`.
