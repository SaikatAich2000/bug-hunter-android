# Bug Hunter Pydantic Schema Catalog

Source: `app/schemas.py` (v4.0 multi-tenant). FK semantics cross-referenced from `app/models.py`.
This catalog is the input contract for generating Kotlin DTO data classes for the Android client.

Notes on conventions used throughout:
- All `datetime` fields are ISO 8601 with timezone (UTC) — map to `java.time.Instant` / `OffsetDateTime` or `kotlinx.datetime.Instant`.
- `due_date` and `scheduled_for` are **strings** in `YYYY-MM-DD` form, not datetimes — keep them as `String` in Kotlin to preserve fidelity (server validates the format).
- `description` and `comment body` fields contain **server-sanitized HTML** (allowlisted tags). Treat as HTML, not plain text.
- All IDs are server-assigned `Int` (32-bit). There are NO UUIDs anywhere in this codebase.
- Server validators normalize choice fields (status / priority / environment / role / item_type) to canonical case before storage — clients should send canonical values to avoid surprises but case-insensitive input is accepted.
- A flag like `can_edit` / `can_manage` / `can_delete` is a server-computed permission hint, not stored state.

---

## Organization schemas

### OrganizationOut
- purpose: response shape for the caller's own org details.
- fields:
  - id: Int — primary key.
  - name: String — display name (2..120 chars).
  - slug: String — URL-safe identifier, globally unique.
  - description: String — free text (may be empty).
  - created_at: datetime.
- response model for: `GET /api/organization` (organizations tag), `PUT /api/organization` (organizations tag).

### OrganizationUpdate
- purpose: admin-only PATCH of org details.
- fields:
  - name: String? optional — 2..120 chars, stripped.
  - description: String? optional — max 1000 chars, stripped.
- request body for: `PUT /api/organization`.

---

## Sign-up schemas

### SignupIn
- purpose: first-time sign-up — creates an org plus its admin user in one shot.
- fields:
  - name: String — 2..120 chars (the user's display name).
  - email: String — valid email, max 254 chars, lowercased server-side.
  - password: String — 8..200 chars, must contain letter+digit, blocked-list rejected.
  - organization_name: String — 2..120 chars.
- request body for: `POST /api/auth/signup` (returns `MeOut`, 201).

---

## Invitation schemas

### InvitationCreate
- purpose: admin/manager creates a new invite email.
- fields:
  - email: String — valid email, max 254.
  - role: String — default `"member"`. Allowed: `admin`, `manager`, `member`.
  - project_ids: List<Int> — defaults to empty; deduplicated.
  - as_lead: Boolean — default false. If true new user lands as `lead` on each listed project.
- request body for: `POST /api/invitations` (returns `InvitationOut`, 201).

### InvitationOut
- purpose: server-side admin view of an invite row.
- fields:
  - id: Int.
  - org_id: Int.
  - email: String.
  - role: String — enum (admin | manager | member).
  - invited_by_user_id: Int? optional (nullable when the inviter was deleted).
  - invited_by_name: String — denormalized at create time so it survives inviter deletion.
  - initial_project_ids: String — comma-separated list of project ids (legacy storage shape, parse on client if needed).
  - expires_at: datetime.
  - accepted_at: datetime? optional.
  - revoked_at: datetime? optional.
  - created_at: datetime.
- response model for: `POST /api/invitations`, `GET /api/invitations` (returns `list[InvitationOut]`).

### InvitationPreview
- purpose: public, unauthenticated view of an invite token — what the invitee sees before accepting. Deliberately minimal; never reveals inviter email.
- fields:
  - email: String — the invited address.
  - organization_name: String.
  - role: String — enum (admin | manager | member).
  - expires_at: datetime.
  - invited_by_name: String.
- response model for: `GET /api/invitations/preview/{token}`.

### InvitationAccept
- purpose: invitee posts this to complete acceptance (creates the User row).
- fields:
  - token: String — opaque server-issued token.
  - name: String — 2..120 chars.
  - password: String — 8..200 chars, letter+digit required.
- request body for: `POST /api/invitations/accept` (returns `MeOut`).

---

## User schemas

### UserIn
- purpose: admin creates a user directly (alternative to inviting).
- fields:
  - name: String — 2..120 chars.
  - email: String — valid email, max 254.
  - role: String — default `"member"`. Allowed: `admin`, `manager`, `member`.
  - password: String — 8..200, letter+digit, blocked-list rejected.
  - is_active: Boolean — default true.
- request body for: `POST /api/users` (returns `UserOut`, 201).

### UserUpdate
- purpose: admin patches an existing user. All fields optional.
- fields:
  - name: String? — 2..120 if present.
  - email: String? — max 254, validated if present.
  - role: String? — enum (admin | manager | member).
  - is_active: Boolean? optional.
  - password: String? optional — same strength rules as UserIn.
- request body for: `PUT /api/users/{user_id}`.

### UserOut
- purpose: user row response for admin listings + details.
- fields:
  - id: Int.
  - name: String.
  - email: String.
  - role: String — enum (admin | manager | member).
  - is_active: Boolean.
  - created_at: datetime.
  - updated_at: datetime.
- response model for: `GET /api/users`, `GET /api/users/{user_id}`, `POST /api/users`, `PUT /api/users/{user_id}`.

---

## Auth schemas

### LoginIn
- purpose: credentials body for password login.
- fields:
  - email: String — validated email.
  - password: String — no client-side strength check on login (server still hashes/compares).
- request body for: `POST /api/auth/login`.

### ChangePasswordIn
- purpose: authenticated user changes their own password.
- fields:
  - current_password: String — 1..200.
  - new_password: String — strength-checked (8..200, letter+digit, blocked-list).
- request body for: `POST /api/auth/change-password` (204 No Content).

### ForgotPasswordIn
- purpose: kicks off email-based reset.
- fields:
  - email: String — validated.
- request body for: `POST /api/auth/forgot-password` (204 No Content, doesn't leak existence).

### ResetPasswordIn
- purpose: completes reset using the emailed token.
- fields:
  - token: String — single-use token from the reset email.
  - new_password: String — strength-checked.
- request body for: `POST /api/auth/reset-password` (204).

### ProfileUpdateIn
- purpose: self-service rename. Email and role are NOT editable here.
- fields:
  - name: String — 2..120, stripped.
- request body for: `PUT /api/auth/profile` (returns `MeOut`).

### EmailChangeRequestIn
- purpose: step 1 of email-change — prove password, nominate new address.
- fields:
  - new_email: String — validated.
  - current_password: String — 1..200.
- request body for: `POST /api/auth/email-change/request` (202 Accepted).

### EmailChangeConfirmIn
- purpose: step 2 of email-change — submit the 6-digit code emailed to the new address.
- fields:
  - code: String — exactly 6 digits (`^\d{6}$`).
- request body for: `POST /api/auth/email-change/confirm` (returns `MeOut`).

### BrandingInfo
- purpose: lightweight per-org theming subset safe for any user. Embedded inside `MeOut.branding`.
- fields:
  - logo_data_url: String? optional — image data: URL.
  - accent_color: String? optional — CSS hex.

### MeOut
- purpose: the "who am I" payload, returned after login / on refresh / after profile mutations. Includes org info so the SPA can theme + scope without a second call.
- fields:
  - id: Int.
  - name: String.
  - email: String.
  - role: String — enum (admin | manager | member).
  - is_active: Boolean.
  - org_id: Int.
  - organization_name: String.
  - organization_slug: String.
  - totp_enabled: Boolean — default false.
  - branding: BrandingInfo? optional.
- response model for: `GET /api/auth/me`, `POST /api/auth/login` (success path), `POST /api/auth/signup` (201), `PUT /api/auth/profile`, `POST /api/auth/email-change/confirm`, `POST /api/invitations/accept`.

### UserBrief
- purpose: compact user reference embedded in other DTOs (reporter, assignees, event managers).
- fields:
  - id: Int.
  - name: String.
  - email: String.
  - role: String — enum (admin | manager | member).

---

## Project schemas

### ProjectIn
- purpose: create or replace-update payload for a project.
- fields:
  - name: String — 2..120, stripped.
  - key: String? optional — uppercase A-Z start, A-Z0-9 only, 2..16 chars. Auto-uppercased.
  - description: String — default `""`, max 1000, stripped.
  - color: String — default `"#c9764f"`, must match `^#[0-9a-fA-F]{6}$`.
- request body for: `POST /api/projects` (201), `PUT /api/projects/{project_id}`.

### ProjectOut
- purpose: response shape for a project.
- fields:
  - id: Int.
  - name: String.
  - key: String — may be empty in legacy rows.
  - description: String.
  - color: String — hex.
  - created_at: datetime.
  - updated_at: datetime.
  - can_manage: Boolean — default false, server-computed permission hint.
  - member_count: Int — default 0.
- response model for: `GET /api/projects` (list), `GET /api/projects/{project_id}`, `POST /api/projects`, `PUT /api/projects/{project_id}`.

---

## Project membership schemas

### ProjectMembershipIn
- purpose: add a user to a project.
- fields:
  - user_id: Int.
  - role: String — default `"member"`. Allowed: `lead`, `member` (project-level).
- request body for: `POST /api/projects/{project_id}/members` (returns `ProjectMembershipOut`).

### ProjectMembershipUpdate
- purpose: change an existing member's project role.
- fields:
  - role: String — Allowed: `lead`, `member`.
- request body for: `PUT /api/projects/{project_id}/members/{user_id}`.

### ProjectMembershipOut
- purpose: composite row for the membership panel — combines org-level + project-level info.
- fields:
  - id: Int — the project_memberships row id.
  - user_id: Int.
  - user_name: String.
  - user_email: String.
  - user_role: String — org-level role (admin | manager | member).
  - project_role: String — project-level role (lead | member).
  - created_at: datetime.
- response model for: `GET /api/projects/{project_id}/members` (list), `POST /api/projects/{project_id}/members`, `PUT /api/projects/{project_id}/members/{user_id}`.

---

## Bug / work-item schemas

### BugCreate
- purpose: create a new work item (item_type can be Bug, Requirement or Task — all share the bugs table).
- fields:
  - project_id: Int.
  - title: String — 3..200 chars, stripped.
  - description: String — default `""`, max 1_000_000 (rich HTML, sanitized server-side).
  - reporter_id: Int? optional.
  - assignee_ids: List<Int> — default empty, deduped preserving order.
  - status: String — default `"New"`. Must belong to the chosen item_type's set (see Enums).
  - priority: String — default `"Medium"`. Allowed: `Low`, `Medium`, `High`, `Critical`.
  - environment: String — default `"DEV"`. Allowed: `DEV`, `UAT`, `PROD`.
  - due_date: String? optional — `YYYY-MM-DD` or null/empty.
  - item_type: String — default `"Bug"`. Allowed: `Bug`, `Requirement`, `Task`.
  - event_id: Int? optional — links the item to an Event container.
- model-level invariant: status must be valid for the chosen item_type.
- request body for: `POST /api/bugs` (returns `BugOut`, 201).

### BugUpdate
- purpose: PATCH-style update. Every field optional; missing fields are left unchanged.
- fields: same shapes as `BugCreate` but all optional. status validity vs item_type is enforced in the route, not the schema.
  - project_id: Int? optional (move to a different project).
  - title: String? optional — 3..200.
  - description: String? optional — sanitized.
  - reporter_id: Int? optional.
  - assignee_ids: List<Int>? optional — deduped if present.
  - status: String? optional.
  - priority: String? optional.
  - environment: String? optional.
  - due_date: String? optional — `YYYY-MM-DD` or null/empty.
  - item_type: String? optional.
  - event_id: Int? optional — explicit null detaches from event.
- request body for: `PUT /api/bugs/{bug_id}` (returns `BugOut`).

### AttachmentBrief
- purpose: lightweight attachment row — metadata only, no blob.
- fields:
  - id: Int.
  - filename: String.
  - content_type: String — MIME.
  - size_bytes: Int.
  - uploader_user_id: Int? optional.
  - uploader_name: String — denormalized at upload time.
  - comment_id: Int? optional — non-null if attachment belongs to a comment, null if attached to the bug itself.
  - created_at: datetime.
- response model for: `POST /api/bugs/{bug_id}/attachments` (201). Embedded in `BugOut.attachments` (via BugDetail), `BugDetail.attachments`, `CommentOut.attachments`.

### BugOut
- purpose: standard response shape for a single work item.
- fields:
  - id: Int.
  - project_id: Int.
  - project_name: String? optional.
  - project_key: String? optional.
  - item_type: String — default `"Bug"`. Enum: Bug | Requirement | Task.
  - event_id: Int? optional.
  - event_name: String? optional.
  - title: String.
  - description: String — sanitized HTML.
  - reporter: UserBrief? optional.
  - assignees: List<UserBrief> — default empty.
  - status: String.
  - priority: String — enum.
  - environment: String — enum.
  - due_date: String? — `YYYY-MM-DD` or null.
  - created_at: datetime.
  - updated_at: datetime.
  - attachment_count: Int — default 0.
  - can_edit: Boolean — server-computed; default false.
- response model for: `POST /api/bugs` (201), `PUT /api/bugs/{bug_id}`. Embedded as list items in `BugListResponse.items`.

### BugListResponse
- purpose: paginated list wrapper.
- fields:
  - items: List<BugOut>.
  - page: Int.
  - page_size: Int.
  - total: Int.
  - pages: Int — total page count.
- response model for: `GET /api/bugs`.

### BugDetail
- purpose: full single-bug payload — extends `BugOut` with comments/activity/attachments.
- fields: all of `BugOut` plus:
  - comments: List<CommentOut> — default empty.
  - activities: List<ActivityOut> — default empty.
  - attachments: List<AttachmentBrief> — default empty.
- response model for: `GET /api/bugs/{bug_id}`.

---

## Event schemas (v2.4 containers)

### EventCreate
- purpose: create a new event container (sprint/standup/incident-debrief).
- fields:
  - name: String — 2..200, stripped.
  - description: String — default `""`, max 10_000, stripped.
  - scheduled_for: String? optional — `YYYY-MM-DD`.
  - manager_ids: List<Int> — default empty. Each manager must be admin or manager role in the actor's org (route-enforced).
- request body for: `POST /api/events` (returns `EventOut`, 201).

### EventUpdate
- purpose: PATCH for an event. All fields optional.
- fields:
  - name: String? — 2..200 if present.
  - description: String? — max 10_000.
  - scheduled_for: String? — `YYYY-MM-DD` or null/empty (clears the date).
  - manager_ids: List<Int>? — replaces the full set if present.
- request body for: `PUT /api/events/{event_id}` (returns `EventOut`).

### EventOut
- purpose: response shape for an event (without item list).
- fields:
  - id: Int.
  - name: String.
  - description: String.
  - scheduled_for: String? optional — `YYYY-MM-DD`.
  - managers: List<UserBrief> — default empty.
  - item_count: Int — default 0.
  - created_at: datetime.
  - updated_at: datetime.
  - can_edit: Boolean — default false.
  - can_delete: Boolean — default false.
- response model for: `GET /api/events` (list), `POST /api/events` (201), `PUT /api/events/{event_id}`. Base class for `EventDetailOut`.

### EventItemBrief
- purpose: lightweight item row for the event-detail panel; mirrors the main bug table columns.
- fields:
  - id: Int — work item id.
  - item_type: String — enum (Bug | Requirement | Task).
  - title: String.
  - project_id: Int.
  - project_name: String? optional.
  - project_key: String? optional.
  - status: String.
  - priority: String.
  - environment: String.
  - due_date: String? optional — `YYYY-MM-DD`.
  - assignees: List<UserBrief> — default empty.
  - attachment_count: Int — default 0.
- embedded in `EventDetailOut.items`.

### EventDetailOut
- purpose: event row plus its contained items.
- fields: all of `EventOut` plus:
  - items: List<EventItemBrief> — default empty.
- response model for: `GET /api/events/{event_id}`.

---

## Comment / activity schemas

### CommentIn
- purpose: create or replace a comment body.
- fields:
  - body: String — 1..200_000, rich HTML, sanitized server-side. Must contain at least one visible char OR an `<img>` tag (image-only screenshot comments allowed).
- request body for: `POST /api/bugs/{bug_id}/comments` (returns `CommentOut`, 201), `PUT /api/bugs/{bug_id}/comments/{comment_id}` (returns `CommentOut`).

### CommentOut
- purpose: response shape for a single comment.
- fields:
  - id: Int.
  - bug_id: Int.
  - author_user_id: Int? optional (null if author deleted).
  - author_name: String — denormalized.
  - body: String — sanitized HTML.
  - created_at: datetime.
  - attachments: List<AttachmentBrief> — default empty.
- response model for: `GET /api/bugs/{bug_id}/comments` (list), `POST /api/bugs/{bug_id}/comments` (201), `PUT /api/bugs/{bug_id}/comments/{comment_id}`. Embedded in `BugDetail.comments`.

### ActivityOut
- purpose: one audit-log entry.
- fields:
  - id: Int.
  - bug_id: Int? optional — null for non-bug events (e.g. `user_invited`).
  - entity_type: String — e.g. `"bug"`, `"user"`, `"project"`, `"event"` (open vocabulary).
  - entity_id: Int? optional — survives the entity itself (preserves searchability).
  - actor_user_id: Int? optional.
  - actor_name: String — denormalized, default `"system"`.
  - action: String — short verb, e.g. `"created"`, `"updated"`, `"status_changed"`.
  - detail: String — free-text supplement, may be empty.
  - created_at: datetime.
- response model for: `GET /api/audit` (list — audit tag), `GET /api/bugs/{bug_id}/activity` (list). Embedded in `BugDetail.activities`.

---

## Sessions schema

### SessionOut
- purpose: row in the admin sessions panel (active logins, Keycloak-style).
- fields:
  - id: Int.
  - user_id: Int.
  - user_name: String? optional.
  - user_email: String? optional.
  - user_role: String? optional — enum (admin | manager | member).
  - ip_address: String.
  - user_agent: String.
  - created_at: datetime.
  - last_seen_at: datetime.
  - expires_at: datetime.
  - is_current: Boolean — default false; true for the row matching the caller's own session.
- response model for: `GET /api/sessions` (list — sessions tag, admin only).

---

## Stats schema

### StatsOut
- purpose: aggregated dashboard counts.
- fields:
  - bugs: Int — total work items in scope.
  - open: Int.
  - resolved: Int.
  - closed: Int.
  - resolve_later: Int.
  - projects: Int — default 0.
  - users: Int — default 0.
  - by_status: Map<String, Int> — keys are status names.
  - by_priority: Map<String, Int>.
  - by_environment: Map<String, Int>.
  - by_type: Map<String, Int> — default empty. Always GLOBAL (not filtered by `?item_type=` query). Includes an `"Event"` key.
  - by_project: List<Map<String, Any>> — heterogeneous rows, parse leniently.
  - by_assignee: List<Map<String, Any>>.
  - timeline: List<Map<String, Any>>.
- response model for: `GET /api/stats`.

Suggested Kotlin shape: model `by_project`, `by_assignee`, `timeline` as `List<Map<String, JsonElement>>` (kotlinx.serialization) or `List<Map<String, Any?>>`. Do NOT assume a fixed inner schema.

---

## Enums & union types

These appear inline as plain `str` fields constrained by validators; treat each as a Kotlin `enum class` (or sealed class of constants) so DTOs are type-safe.

### Role (org-level) — `User.role`, `UserOut.role`, `MeOut.role`, `UserBrief.role`, `InvitationCreate.role`, `InvitationOut.role`, `InvitationPreview.role`, `UserIn.role`, `UserUpdate.role`, `SessionOut.user_role`, `ProjectMembershipOut.user_role`
Allowed values (verbatim, lowercase):
- `admin`
- `manager`
- `member`

### ProjectRole (project-level) — `ProjectMembershipIn.role`, `ProjectMembershipUpdate.role`, `ProjectMembershipOut.project_role`
Allowed values (verbatim, lowercase):
- `lead`
- `member`

### ItemType — `BugCreate.item_type`, `BugUpdate.item_type`, `BugOut.item_type`, `EventItemBrief.item_type`
Allowed values:
- `Bug` (default)
- `Requirement`
- `Task`

### Priority — `BugCreate.priority`, `BugUpdate.priority`, `BugOut.priority`, `EventItemBrief.priority`
Allowed values:
- `Low`
- `Medium`
- `High`
- `Critical`

### Environment — `BugCreate.environment`, `BugUpdate.environment`, `BugOut.environment`, `EventItemBrief.environment`
Allowed values:
- `DEV`
- `UAT`
- `PROD`

### Status — `BugCreate.status`, `BugUpdate.status`, `BugOut.status`, `EventItemBrief.status`
Status is per-item-type. `New` appears in every list so legacy rows always remain valid.

Status set by item_type (source of truth `STATUSES_BY_TYPE`):
- **Bug**: `New`, `In Progress`, `Resolved`, `Closed`, `Reopened`, `Not a Bug`, `Resolve Later`.
- **Requirement**: `New`, `In Review`, `Approved`, `Implemented`, `Rejected`, `Deferred`.
- **Task**: `New`, `In Progress`, `Done`, `Blocked`, `Cancelled`.

Union (`ALLOWED_STATUSES`) — accepted by list-filter endpoints (`GET /api/bugs?status=`):
- `New`, `In Progress`, `Resolved`, `Closed`, `Reopened`, `Not a Bug`, `Resolve Later`, `In Review`, `Approved`, `Implemented`, `Rejected`, `Deferred`, `Done`, `Blocked`, `Cancelled`.

Special:
- `EXCLUDED_FROM_TOTAL_STATUSES = ["Not a Bug"]` — these are kept out of the `bugs` total in stats but still shown in lists.

### Recommended Kotlin modelling
- For all of the above, generate a sealed `enum class` with a string `value` field, plus an `@JsonValue`/serializer that round-trips the canonical wire form. Status should be one big enum (union of all values) and an additional helper that lists which statuses are valid per `ItemType`.
- Server normalizes case-insensitively but always returns canonical case — clients should send canonical case to round-trip without re-translation.

---

## ID conventions

### Integer IDs (all of them)
Every model in this codebase uses `Int` (autoincrement) primary keys. There are NO UUIDs and NO string PKs. Models:
- Organization, User, Invitation, PasswordResetToken, EmailChangeRequest, Project, ProjectMembership, Event, Bug, Comment, Attachment, Activity, Session, TotpRecoveryCode, SavedView, Webhook, CustomField, BugCustomValue.

### Models with `org_id` (multi-tenant scoping)
The following ORM tables carry an `org_id` FK so every row is tenant-scoped. The DTOs that surface this directly are noted; otherwise the value is enforced server-side and never crosses the wire.
- User — surfaced as `MeOut.org_id`.
- Project — server-enforced, not on DTOs (project rows are already in tenant scope of the caller).
- Event — server-enforced, not on DTOs.
- Invitation — surfaced as `InvitationOut.org_id`.
- Activity — server-enforced, not on DTOs.
- SavedView — server-enforced.
- Webhook — server-enforced.

Models scoped via FK chain (no direct `org_id`): Bug (via Project), Comment (via Bug), Attachment (via Bug), ProjectMembership (via Project), BugCustomValue (via Bug), CustomField (via Project).

### Models with timestamps
- **`created_at` only**: Comment, Attachment, Activity, Session (also `last_seen_at`, `expires_at`), PasswordResetToken (also `expires_at`, `used_at`), Invitation (also `expires_at`, `accepted_at`, `revoked_at`), EmailChangeRequest (also `expires_at`, `used_at`), ProjectMembership, TotpRecoveryCode (also `used_at`).
- **`created_at` + `updated_at`**: Organization, User, Project, Event, Bug, SavedView, Webhook, BugCustomValue.
- **No timestamps**: CustomField has `created_at` only; `bug_assignees` and `event_managers` are pure junction tables with no timestamp columns.

DTOs that expose timestamps (Kotlin should treat all of these as `Instant`):
- `created_at`: OrganizationOut, InvitationOut, UserOut, ProjectOut, ProjectMembershipOut, BugOut, EventOut, EventItemBrief (implicit via inheritance? no — not included), CommentOut, ActivityOut, AttachmentBrief, SessionOut.
- `updated_at`: OrganizationOut, UserOut, ProjectOut, BugOut, EventOut.
- `expires_at`: InvitationOut, InvitationPreview, SessionOut.
- `accepted_at`, `revoked_at`: InvitationOut.
- `last_seen_at`: SessionOut.

### Date-only string fields (NOT datetimes)
Stored as `String` in `YYYY-MM-DD` form — keep as `String` in Kotlin (or `LocalDate` with a custom serializer):
- `BugCreate.due_date`, `BugUpdate.due_date`, `BugOut.due_date`, `EventItemBrief.due_date`.
- `EventCreate.scheduled_for`, `EventUpdate.scheduled_for`, `EventOut.scheduled_for`.

### HTML-bearing string fields (sanitized server-side, render as HTML, never plain text)
- `BugCreate.description`, `BugUpdate.description`, `BugOut.description`.
- `CommentIn.body`, `CommentOut.body`.
Treat these specially in the Android renderer — they may contain `<img src="data:image/...">` from pasted screenshots, plus the formatting tags listed in the sanitizer allowlist (`p`, `br`, `div`, `span`, `b/strong`, `i/em`, `u`, `s/strike/del/ins`, `ul/ol/li`, `blockquote`, `pre/code`, `h1`..`h6`, `a`, `img`).

### Permission hint fields (server-computed, advisory)
- `ProjectOut.can_manage`, `BugOut.can_edit`, `EventOut.can_edit`, `EventOut.can_delete`. Always defaulted to false; never echoed back from clients.

### Denormalized name fields (do not edit, treat as read-only mirrors)
- `InvitationOut.invited_by_name`, `CommentOut.author_name`, `ActivityOut.actor_name`, `AttachmentBrief.uploader_name`. These outlive their referenced user — when `*_user_id` is null the name still reads.

---

## Notes for DTO generation

1. Pydantic `Optional[X]` / `X | None` maps to Kotlin nullable `X?`. Defaulted-to-empty collections (`Field(default_factory=list)`) should default to `emptyList()`, never null.
2. Pydantic `Field(default=...)` should become a Kotlin default argument so DTOs are convenient to construct.
3. For request DTOs intended as PATCHes (anything ending in `Update`), every field is independently optional and absent fields should be **omitted from the JSON body**, not sent as `null`. Use `@JsonInclude(NON_NULL)` (Jackson) or `@EncodeDefault(NEVER)` (kotlinx.serialization) at the property level so unmodified fields don't accidentally clear server state.
4. `BugUpdate.event_id`: a null value is meaningful (detach from event) versus omission (leave unchanged). Model with a nullable wrapper sentinel (e.g. `Optional<Int?>` pattern) or expose an explicit `clearEvent: Boolean` companion flag.
5. `StatsOut.by_project`, `by_assignee`, `timeline` carry heterogeneous shapes — prefer `List<JsonObject>` over a strict data class.
6. Schema sources of truth NOT in `app/schemas.py` (defined inside route modules — out of scope for this catalog but worth knowing about for future generation passes): `LoginTotpStepIn`, `TotpStatus`, `TotpBeginOut`, `TotpConfirmIn`, `TotpConfirmOut`, `TotpDisableIn`, `BrandingIn`, `BrandingOut`, `SavedViewIn`, `SavedViewUpdateIn`, `SavedViewOut`, `WebhookIn`, `WebhookUpdateIn`, `WebhookOut`, `CustomFieldIn`, `CustomFieldUpdateIn`, `CustomFieldOut`, `CustomValueOut`, `BulkUpdateIn`, `BulkDeleteIn`, `DeleteAccountIn`.
