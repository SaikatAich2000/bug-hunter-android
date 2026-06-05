# Bug Hunter SPA — UX flows reference (for Compose port)

Source files: `app/static/index.html`, `app/static/app.js`, `app/static/login.js`, `app/static/signup.js`, `app/static/chatbot.js`. Every observation below corresponds to a concrete handler or DOM node in those files.

## Top-level navigation
The shell is a left **sidebar + main pane** (not bottom tabs). The sidebar holds branding (org logo, version), an **org banner** (org name + slug + role), the primary nav, then collapsible sections for Projects, Users (admin-only), and an Account card with Profile / Change password / Log out, plus an "Export CSV" and a theme toggle in the footer. A hamburger button in the topbar opens the sidebar on narrow viewports (`menuBtn` + `sidebarBackdrop`); the sidebar can be collapsed and the choice is persisted in `localStorage`.

Primary nav order (`.nav-btn[data-view]`): **Work Items, Events, Analytics, Invitations** (manager+), **Audit Trail** (manager+), **Sessions** (admin only). `data-needs-role` is enforced by `applyRoleVisibility()` — buttons the user can't reach are removed from the DOM, not just disabled.

The topbar contains the **page title**, a **search input** (visible only on Work Items), and a **split "+ New" button**: main click creates the last-used type (persisted as `defaultNewType`), the caret opens a menu (`🐞 New Bug`, `📐 New Requirement` [manager+], `✅ New Task` [manager+]).

Under the topbar sit two scope controls that apply to both Work Items and Analytics: **type tabs** (All / 🐞 Bugs / 📐 Requirements / ✅ Tasks, each with a count badge from `by_type`) and the **KPI strip** (Total / Open / Resolved / Closed / Resolve Later). Tabs and KPI strip are hidden on Audit, Sessions, Invitations, Events.

## Bug list
- **Columns per tab** (`TAB_COLUMNS`): All shows id, type-marked title, project, status, priority, env, assignees, attachment count, actions. Bug omits the type marker. Requirement drops env. Task swaps env for due-date + event.
- **ID format**: `PROJECTKEY-N` when the project has a key, else `#N`. Numbering is global across types.
- **Severity / status / env badges** are rendered as `<span class="badge" data-status="…">` with CSS color tokens; chart colors are defined in `kindColor()` (Critical = `#c5524a`, High = `#d4a05a`, Medium = `#5a9fd4`, Low = `#8b8270`; PROD red, UAT amber, DEV blue).
- **Sorting**: server-driven, no client sort UI. The list is paged (`pageSize = 50`); the pagination bar shows `← Prev | Page X of Y (N items) | Next →`. There is **no infinite scroll** — explicit paging.
- **Filtering**: a horizontal filter bar of six multi-select dropdowns (Project, Type, Status, Priority, Env, Assignee) plus a **Clear** button. Type filter hides when a specific tab is active; env filter hides on Requirement/Task tabs. The active filter state is mirrored into the URL (`syncFiltersToUrl`) so reload restores the view.
- **Search** (`#search`): debounced 250 ms; matches title, description, or `#id`.
- **KPI tiles** act as one-tap status filters (toggle on/off; clicking the active tile clears).
- **Saved views**: not implemented; the URL is the share mechanism.
- **Empty state**: `<div id="emptyState">No items match your filters</div>` shown when the table body is empty.
- **Create entry point**: the topbar "+ New …" split button, or sidebar context (e.g. "+ Add Task" inside an Event).
- **Row delete** is admin-only and shown as a trash icon in the actions column; clicking a row body opens the bug modal in edit mode.

## Bug detail
Bug detail is **not a separate screen** — it's the same modal as create. The modal title becomes `🐞 Bug #N` with the bug title as subtitle. Layout is **two columns** above 900 px (single column below), with a full-width title field on top:

- **Main column** (left): description (rich-text editor), an Attachments section, a Comments section, and an Activity history collapsed in a `<details>` summary.
- **Side column** (right): Type, Project, Status, Priority, Environment, Reporter (disabled — fixed to current user), Assignees (chip picker), Event, Due date, and read-only Created / Updated timestamps.

**Status / priority / assignee / due-date changes happen inline in the side column** — there's no separate "Edit" modal or pencil-per-field. Save submits the whole form via the Save changes button. Status options are scoped per type (`statuses_by_type`), and legacy values are preserved with a `(legacy)` suffix.

**Comments section** displays the count `(N)`. Each row shows avatar + author, timestamp, optional admin edit (✎) / delete (🗑) icons, sanitized-HTML body, and attached files rendered as the same attachment-card grid. Order is whatever the server returns — Bug Hunter renders them newest-first server-side (per repo memory). The composer is a rich-text editor (B/I/U/list/quote/code/image) with a `📎 Attach files` button, a staged-file preview row, and a **Post comment** button. **Ctrl/Cmd+Enter** posts.

**Attachments section** (`#bugAttachmentsSection`) sits between description and comments so bug-level files are visually grouped with metadata, not buried in the thread. Layout is a CSS **grid of attachment cards** (`attachment-grid`) — raster images render inline as `<img>`, videos as `<video controls>`, everything else as a file-icon link. Each card has Name, size, uploader, and `View / Download / Delete` (delete admin-only). SVGs are explicitly downloaded, not embedded. A `📎 Add attachment` label-input in the section head stages files; the staged list shows a thumbnail strip with X-to-remove. Empty copy: `No attachments yet…`.

**Read-only mode**: if the current user is a member and the item is a Requirement/Task, the form locks all inputs, hides the submit / delete / comment composer, and shows a banner: `Read-only — only admins and managers can edit requirements.`

## Sleuth chatbot
Implemented in a self-contained IIFE (`chatbot.js`), mounted on every page except `/login` and `/reset`.

- **FAB**: fixed bottom-right (`.sleuth-fab`), shows the Sleuth SVG icon. Click toggles a side panel; **Ctrl/Cmd + /** also opens it. The FAB sets `aria-expanded`.
- **Panel** (`.sleuth-panel`): header with avatar + "Sleuth · Your Bug Hunter assistant" + a clear (↻) and close (✕). Body is `aria-live="polite"`. Input is an auto-growing textarea (max 120 px) + Send button. **Enter** sends; **Shift+Enter** newline. **Esc** closes.
- **Welcome** on first open lists capabilities verbatim (e.g. *show open bugs assigned to alice*, *how many critical bugs in PROD?*, *export bugs in apollo to excel*, *bug 42*, *close bug 5*, *reopen #12*, *assign bug 7 to bob*, *set bug 3 priority to high*, *comment on #5: looks fixed*, *create a bug titled "Login broken" in project Apollo*, type **help** for the full guide).
- **Streaming**: not a stream — a single POST to `/api/chat/ask` with a "typing" three-dot indicator while in-flight. Response is a list of typed blocks rendered into one bot bubble: `text` (with limited markdown: bold/italic/code/links/`-`-bullets), `table` (rows can be clickable to open the bug — clicking dispatches a `sleuth:open-bug` CustomEvent the SPA handles), `file` (xlsx download card with row count + size + Download button hitting `/api/chat/download/<token>`), `suggestions` (chip-row of follow-up prompts that auto-send), `confirm` (a Yes / Cancel pair for write actions; both buttons disable on click to prevent double-fire).

## Stats / dashboard
Analytics view (`#viewAnalytics`) is a 6-card grid:
1. **Timeline** — SVG line + filled area for the last 14 days.
2. **By Status** — bar chart colored per status.
3. **By Priority** — bar chart.
4. **By Environment** — bar chart (card hidden on Requirement/Task tabs).
5. **By Project** — horizontal `bar-row` list with project swatch.
6. **Top Assignees** — horizontal bar list with avatar initials.

Titles change with the active tab (`Items over the last 14 days` → `Bugs over the last 14 days` / `Requirements …` / `Tasks …`). KPI strip is shown above the charts too. There is no drill-through from a chart — clicking is a tooltip only — but the type tabs and KPI tiles re-scope every card.

## Projects + Organizations + Members
**Org switching**: not implemented in the web SPA; the user belongs to one org (shown in the org banner). Branding (logo + accent color) is per-org and applied at boot via CSS custom-property overrides.

**Projects** live in the sidebar as a list; each row has a color swatch (toggles the project filter when clicked), the name + key chip, and admin/lead-only icon actions: 👥 manage members, ✎ edit, 🗑 delete. "+ New Project" is admin-only. Project form: Name (required), Key (uppercase pattern), Color (color input), Description.

**Project members modal** (`#modalProjectMembers`): a row to add a user by select + role (Member / Lead), then a list of current members with their role.

**Users sidebar** (admin/manager only): list of active users with edit/delete; "+ New User" creates a user with Name / Email / Role (Member / Manager / Admin — each row carries its own helper subtitle in the dropdown) / Password / Active checkbox.

**Invitations view** (manager+): "+ Invite a teammate" opens a modal with Email, Role, optional project checklist, and "Make them a lead on the selected projects" checkbox. Helper text: `They'll get an email with a link to set their password`, and on the project picker: `Members and managers only see projects they're added to`. Pending invites expire after 7 days. Empty copy: `No invitations yet. Click + Invite a teammate to send one.`

## Notification / feedback patterns
- **Toasts**: single `#toast` element, types `info / success / error`, auto-dismiss after 3.5 s. Errors with `silent` flag (auth-redirects) are suppressed.
- **Modal confirms**: shared `#modalConfirm` (small card). `confirmDialog()` returns a Promise; Esc resolves false, OK button label and danger color are configurable.
- **Inline errors**: server-validation errors (Pydantic) are concatenated and toasted; client-side `Title is required`, `Please pick a project`, etc., as red toasts.
- **Global blocking loader**: `#globalLoader` overlay shown via `withLoader(thunk, "Working…")` for every state-changing call — covers the viewport with `pointer-events: all` so accidental double-clicks can't fire.
- **Loading skeletons**: not used — the loader overlay is the universal busy state. Empty placeholders use `Loading…` text (e.g. the bug table head before first render).
- **Version drift toast**: every 5 min the SPA polls `/api/health`; if `asset_version` changed, a single info toast: `New version available — reload the page when ready`.

## Forms
- **Validation**: HTML5 attributes (`required`, `minlength`, `maxlength`, `pattern`) plus a server round-trip. Login/signup do an extra `clientValidate` (email must contain `@`, password ≥ 8 chars and mix letters + digits).
- **Password strength meter**: no visual meter. The signup helper text is just `Password needs at least 8 characters` / `Password should mix letters and numbers`.
- **Email format** is checked by `<input type="email">` and by the server.
- **File upload UX**: web uses a `<label class="comment-attach-btn">` wrapping a hidden `<input type="file" multiple>`, plus **paste-from-clipboard** in the rich-text editor (pasted files are routed to the staging bucket or uploaded directly). Drag-drop is not wired; the equivalent on Compose is the system file picker (`ActivityResultContracts.GetMultipleContents`) plus the **Photos / Camera** chooser. There's a hard-coded **unsafe-extension/MIME blocklist** (`_UNSAFE_EXTS`, `_UNSAFE_MIMES`) — `.exe`, `.bat`, `.sh`, `.lnk`, executables, scripts — blocked with toast `Blocked unsafe file: …`. Mirror this client-side on Android.
- **Custom date picker** replaces native date inputs with a popover calendar.
- **Custom selects** replace native `<select data-bh-select>` with a styled button + listbox popover; native control is hidden but kept for form submit.
- **Rich-text editor** (`data-bh-rt`): toolbar B / I / U / S, bullet, numbered, blockquote, code block, insert image; Ctrl+B/I/U shortcuts; paste handles inline images.
- **Keyboard**: Escape closes top modal; Ctrl/Cmd+Enter posts comment; Ctrl/Cmd+/ opens Sleuth; Tab order within forms follows DOM order.

## Empty states
Quote verbatim:
- Bug list: **"No items match your filters"**
- Projects sidebar: **"No projects yet."**
- Users sidebar: **"No users yet — click + to add."**
- Events grid (no events at all): **"No events yet. Click + New Event to create one — a standup, a sprint meeting, whatever you want to track"**
- Events grid (filtered out): **"No events match the current filter. Try clearing the search or date"**
- Event detail, no items: **"No items in this event yet — click + Add Task to create one"**
- Event detail, filtered out: **"No tasks in this event match the current filter. Try clearing the search or filters"**
- Attachments: **"No attachments yet…"**
- Activity: **"No activity yet."**
- Sessions: **"No active sessions."**
- Invitations: **"No invitations yet. Click + Invite a teammate to send one."**
- Audit: **"No audit events match"**
- Sleuth bot fallback when response has no renderable blocks: **"(no answer)"**

## Refresh / sync
- **No pull-to-refresh**, no scroll-triggered fetch.
- Each view has an explicit **Refresh** button (Events, Audit, Sessions, Invitations). The work-items list refetches when filters / tab / pagination / KPI change.
- **Session poll**: `/api/auth/me` is hit every 15 s, and again on `visibilitychange → visible`. 401/403 ⇒ redirect to `/login.html` with a toast `Your session ended. Redirecting to login…`. Compose should mirror this with a foreground lifecycle observer / periodic ping.
- **Version-drift poll**: `/api/health` every 5 min, surfaced as a non-blocking toast (user reloads themselves; no auto-reload to avoid losing form input).
- **After any mutation**, the caller re-fetches the affected bug (`api/bugs/{id}`) and rerenders inline sections + the row in the list, so attachment counts and badges update without a full reload.
- **Deep links**: `location.hash = "bug-<id>"` opens the bug modal on first load — Sleuth uses this as a fallback when the SPA isn't around to handle its CustomEvent.
