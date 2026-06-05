# Bug Hunter — Visual Design Language for Android (Material 3 / Compose)

Source files audited:
- `app/static/styles.css` (v3.2 visual refresh, 3923 lines)
- `app/static/chatbot.css` (Sleuth chatbot styles)
- `app/static/index.html` (SPA shell, 934 lines)
- `app/static/login.html`, `signup.html`, `reset.html`, `accept-invite.html`

Brand identity: indigo → sky-blue gradient (`#6366f1 → #818cf8 → #38bdf8`) over deep midnight surfaces. Two themes (dark default + light) driven by `data-theme` attribute. PWA `theme-color` meta = `#6366f1`.

---

## Color tokens

All values are taken verbatim from `:root` / `[data-theme="dark"]` and `[data-theme="light"]` blocks in `styles.css`. Role labels (in parens) are the one-word semantic intent.

### Shared / utility

| Token | Dark value | Light value | Role |
|---|---|---|---|
| `--accent-rgb` | `129, 140, 248` | `99, 102, 241` | accent-rgb |

### Surfaces & background

| Token | Dark | Light | Role |
|---|---|---|---|
| `--bg` | `#0a0e1a` | `#f4f6fb` | background |
| `--bg-grad` | radial(`rgba(99,102,241,0.08)`) + radial(`rgba(56,189,248,0.06)`) on `#0a0e1a` | radial(`rgba(99,102,241,0.06)`) + radial(`rgba(56,189,248,0.05)`) on `#f4f6fb` | background-gradient |
| `--bg-elev` | `#131829` | `#ffffff` | surface |
| `--bg-elev-2` | `#1a2138` | `#f1f4fb` | surface-2 |
| `--bg-elev-3` | `#232c47` | `#e6ecf6` | surface-3 |
| `--surface-glass` | `rgba(26,33,56,0.72)` | `rgba(255,255,255,0.78)` | glass-overlay |
| `--kpi-bg` | `linear-gradient(140deg, #1a2138 0%, #131829 100%)` | `#ffffff` | kpi-surface |

### Borders

| Token | Dark | Light | Role |
|---|---|---|---|
| `--border` | `#232c47` | `#e2e8f0` | outline |
| `--border-strong` | `#344063` | `#c8d2e0` | outline-strong |
| `--border-soft` | `rgba(255,255,255,0.05)` | `rgba(15,23,42,0.05)` | outline-soft |

### Text

| Token | Dark | Light | Role |
|---|---|---|---|
| `--text` | `#eef2ff` | `#0f172a` | on-surface |
| `--text-muted` | `#9aa6c4` | `#475569` | on-surface-variant |
| `--text-faint` | `#5e6a85` | `#94a3b8` | on-surface-disabled |

### Accent / primary

| Token | Dark | Light | Role |
|---|---|---|---|
| `--accent` | `#818cf8` | `#6366f1` | primary |
| `--accent-strong` | `#6366f1` | `#4f46e5` | primary-strong |
| `--accent-2` | `#38bdf8` | `#0284c7` | secondary |
| `--accent-soft` | `rgba(129,140,248,0.16)` | `rgba(99,102,241,0.12)` | primary-container |
| `--accent-glow` | `rgba(129,140,248,0.35)` | `rgba(99,102,241,0.25)` | primary-glow |
| `--accent-text` | `#ffffff` | `#ffffff` | on-primary |
| `--accent-grad` | `linear-gradient(135deg, #6366f1 0%, #818cf8 50%, #38bdf8 100%)` | `linear-gradient(135deg, #4f46e5 0%, #6366f1 50%, #0284c7 100%)` | primary-gradient |
| `--accent-grad-hover` | `linear-gradient(135deg, #4f46e5 0%, #6366f1 50%, #0ea5e9 100%)` | `linear-gradient(135deg, #4338ca 0%, #4f46e5 50%, #0369a1 100%)` | primary-gradient-pressed |

### Status colors (semantic)

| Token | Dark | Light | Role |
|---|---|---|---|
| `--danger` | `#f43f5e` | `#dc2626` | error |
| `--danger-strong` | `#e11d48` | `#b91c1c` | error-strong |
| `--danger-soft` | `rgba(244,63,94,0.14)` | `rgba(220,38,38,0.10)` | error-container |
| `--danger-grad` | `linear-gradient(135deg, #e11d48, #f43f5e)` | `linear-gradient(135deg, #b91c1c, #dc2626)` | error-gradient |
| `--warn` | `#f59e0b` | `#d97706` | warning |
| `--warn-soft` | `rgba(245,158,11,0.14)` | `rgba(217,119,6,0.10)` | warning-container |
| `--ok` | `#10b981` | `#059669` | success |
| `--ok-soft` | `rgba(16,185,129,0.14)` | `rgba(5,150,105,0.10)` | success-container |

### Work-item status palette

| Token | Dark | Light | Role |
|---|---|---|---|
| `--status-new` | `#38bdf8` | `#0284c7` | status-new |
| `--status-progress` | `#f59e0b` | `#d97706` | status-in-progress |
| `--status-resolved` | `#10b981` | `#059669` | status-resolved |
| `--status-closed` | `#94a3b8` | `#64748b` | status-closed |
| `--status-reopened` | `#a78bfa` | `#7c3aed` | status-reopened |
| `--status-not-bug` | `#64748b` | `#94a3b8` | status-not-bug |
| `--status-later` | `#f59e0b` | `#d97706` | status-resolve-later |

### Priority palette

| Token | Dark | Light | Role |
|---|---|---|---|
| `--p-low` | `#94a3b8` | `#64748b` | priority-low |
| `--p-medium` | `#38bdf8` | `#0284c7` | priority-medium |
| `--p-high` | `#f59e0b` | `#d97706` | priority-high |
| `--p-critical` | `#f43f5e` | `#dc2626` | priority-critical |

### Environment palette

| Token | Dark | Light | Role |
|---|---|---|---|
| `--env-dev` | `#10b981` | `#059669` | env-dev |
| `--env-uat` | `#f59e0b` | `#d97706` | env-uat |
| `--env-prod` | `#f43f5e` | `#dc2626` | env-prod |

### Shadows / focus / scrollbar

| Token | Dark | Light | Role |
|---|---|---|---|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.3), 0 1px 3px rgba(0,0,0,0.18)` | `0 1px 2px rgba(15,23,42,0.06), 0 1px 3px rgba(15,23,42,0.04)` | elevation-1 |
| `--shadow` | `0 4px 12px rgba(0,0,0,0.30), 0 1px 3px rgba(0,0,0,0.20)` | `0 4px 12px rgba(15,23,42,0.08), 0 1px 3px rgba(15,23,42,0.05)` | elevation-2 |
| `--shadow-lg` | `0 12px 32px rgba(0,0,0,0.45), 0 4px 12px rgba(0,0,0,0.25)` | `0 16px 40px rgba(15,23,42,0.16), 0 4px 12px rgba(15,23,42,0.08)` | elevation-3 |
| `--shadow-xl` | `0 24px 56px rgba(0,0,0,0.55), 0 8px 24px rgba(0,0,0,0.30)` | `0 28px 64px rgba(15,23,42,0.20), 0 12px 32px rgba(15,23,42,0.10)` | elevation-4 |
| `--shadow-focus` | `0 0 0 3px var(--accent-glow)` | `0 0 0 3px var(--accent-glow)` | focus-ring |
| `--scrollbar-thumb` | `#2a3553` | `#cbd5e1` | scrollbar |
| `--scrollbar-thumb-hover` | `#3a4870` | `#94a3b8` | scrollbar-hover |

### Hard-coded literals worth lifting into M3 tokens

These appear inline in CSS, not in `:root`, but matter for Compose parity:

- Invite status pills (no theme variant — same in dark and light):
  - `invite-status-pending`: bg `#1e3a5f`, text `#e0eaf8`
  - `invite-status-accepted`: bg `#1f4d36`, text `#d4f0dd`
  - `invite-status-revoked`: bg `#5f1f1f`, text `#f8d7d7`
  - `invite-status-expired`: bg `#5f4216`, text `#fbe7c4`
- Modal backdrop: `rgba(2,6,23,0.66)` + `blur(4px)`
- Sidebar backdrop (mobile drawer): `rgba(2,6,23,0.55)` + `blur(2px)`
- Global loader scrim: `rgba(8,12,22,0.42)` + `blur(2px)`
- Readonly banner accent: `rgba(248,165,50,0.16) → 0.08` gradient on `rgba(248,165,50,0.32)` border (the only place an amber is hard-coded outside `--warn`).
- Sleuth status dot (online): `#4ade80` with green glow `rgba(74,222,128,0.7)`.
- Selection highlight: `::selection { background: var(--accent-soft) }`.

---

## Typography

- **Font family stack** (all surfaces): `"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`. Android default: bundle **Inter** as a resource; fall back to `Roboto`.
- **Monospace stack** (chips, code, audit pills, `proj-key`, code-input verification field): `ui-monospace, "SF Mono", Menlo, Monaco, Consolas, monospace`. Android: `JetBrains Mono` or `Roboto Mono`.
- **Base body**: 14 px / line-height 1.55 / antialiased.
- **Tabular numbers**: `font-variant-numeric: tabular-nums` on `.kpi-num`, `.col-id`, `.att-count`, `.session-line3`, `.audit-time`, `.activity-time`, `.comment-time`. Use M3 `FontFeatureSettings("tnum")` on numeric `Text`.

### Type scale (mapped to M3 roles)

| Bug Hunter usage | Size | Weight | Letter-spacing | Suggested M3 role |
|---|---|---|---|---|
| KPI number | 28 px | 700 | -0.02em | `displaySmall` (tabular) |
| Auth page H1 (`.auth-brand h1`) | 26 px | 800 | -0.02em | `headlineMedium` (gradient text) |
| Page title (`.page-title`) | 19 px | 700 | -0.01em | `titleLarge` |
| Modal H2 / brand title | 17 px (modal-head, page-intro h2) / 17 px (brand) | 700 | -0.01em | `titleMedium` |
| Event card name | 15 px | 600 | — | `titleMedium` |
| Auth submit label | 15 px | 600 | — | `labelLarge` |
| Title cell (table row) | 14 px | 600 | — | `bodyMedium` strong |
| Nav button label | 14 px | 500 | — | `labelLarge` |
| Body / form input | 13.5 px | 400 | — | `bodyMedium` |
| Bug-title input (large) | 19 px | 600 | -0.01em | `titleLarge` |
| Body row / dropdown row / chip | 13 px | 400-500 | — | `bodyMedium` |
| Audit / session secondary | 12-13 px | 400-500 | — | `bodySmall` |
| Section heading (uppercase) `bug-section h3`, side-section-header, profile section title | 11-12 px | 700 | 0.06-0.08em uppercase | `labelSmall` ALL-CAPS |
| Badge / pill / type-tab count | 10.5-11 px | 600-700 | 0.02-0.07em | `labelSmall` |
| Footnote / brand-version | 10-11 px | 600-700 | 0.05em uppercase | `labelSmall` |
| Code-input (TOTP / 2FA) | 20 px monospace, letter-spacing 0.4em, center | 400 | — | OTP field |

### Weights used
`400` (body), `500` (nav, links, chips), `600` (input/title strong, primary btn label, comment author), `700` (titles, KPI nums, badges, table-th, brand), `800` (only auth H1).

### Line heights
- Body: `1.55`
- Headings (page-intro H2, modal H2): default (~1.2)
- Comments rendered body / blockquote / list: `1.5-1.6`
- Title cell (2-line clamped): `1.35`
- Auth help / tagline: `1.5-1.55`

### Special text effects
- **Gradient text** (`background-clip: text` + `-webkit-text-fill-color: transparent`): brand title, auth H1, KPI numbers. Each KPI number has its own gradient: open=`status-new → accent-2`, resolved=`status-resolved → #34d399`, closed=`status-closed → #cbd5e1`, later=`warn → #fbbf24`, total=`accent-grad`. In Compose, use `Brush.linearGradient` painted into a `TextStyle` via `drawWithContent { drawText(...); drawRect(brush, blendMode = SrcAtop) }`.

---

## Spacing & radii

### Spacing scale (extracted from observed paddings/gaps)

| Token | Value | Where it appears |
|---|---|---|
| xxs | 2 px | nav gap, badge inner |
| xs | 4 px | tiny gaps, chip padding |
| sm | 6 px | tight gaps, chip-pill padding |
| md | 8-10 px | row gaps, small btn padding-y |
| base | 12 px | most card paddings-y, gap defaults |
| lg | 14-16 px | card padding, modal-foot padding |
| xl | 18-22 px | section padding (sidebar 18px 14px, topbar 16px 24px, page-intro 22px 24px 18px) |
| xxl | 24-28 px | main view padding (`.view { padding: 0 24px 24px }`), modal-bug grid gap |
| xxxl | 32-36 px | auth card padding (`36px 32px`), upload-zone padding |

### Border-radius scale

| Value | Where |
|---|---|
| 4 px | tiny: proj-key chip, ms-check, attach-action button |
| 6 px | rich-text toolbar buttons, role pill, sel-row, code inline |
| 7 px | nav-btn, side-item, icon-btn, role pill, ms-row, link-btn |
| 8 px | scrollbar thumb, audit-icon, badge pill internals, attach-staged, comment-edit-input, sleuth-toggle, sleuth-confirm-btn |
| 9 px | btn, .field input/select/textarea, ms-btn, search input variant, comment-attach-btn, member chips, auth-alert |
| 10 px | search-wrap input, ms-panel, audit-item, comment, attach-card, chart-card edges, account-card, sleuth-msg input/send |
| 12 px | KPI tile, table-scroll, empty-state, page-intro-icon, sessions/audit boxed controls, events-grid card, charts-grid card |
| 14 px | modal-card, global-loader-card, sleuth panel, calendar popover |
| 16 px | sleuth-panel (panel container) |
| 18 px | auth-card |
| 999 px (pill) | badges, chips, attach-pill, att-count, event-pill, audit-action/entity tags, toast, invite-status, sleuth-chip / suggestions, sleuth-system msg |
| circular (50 %) | avatars, swatches, sleuth-fab, attach-staged remove, spinners |

### Shadow / elevation (already shown in color section)
- `--shadow-sm` for cards-at-rest (charts, audit/sessions container boxes).
- `--shadow` for hover state of charts / events cards.
- `--shadow-lg` for popovers (ms-panel, calendar, select), toasts.
- `--shadow-xl` for modals, mobile drawer when open, auth-card.
- Custom: primary button glow = `0 1px 2px rgba(0,0,0,0.10), 0 4px 12px var(--accent-glow)`.
- Sleuth FAB: `0 8px 22px var(--accent-glow), 0 4px 10px rgba(0,0,0,0.25), inset 0 1px 0 rgba(255,255,255,0.18)`.

### Motion
- Standard easing: `cubic-bezier(0.4, 0, 0.2, 1)` (M3 standard).
- Pop / spring: `cubic-bezier(0.34, 1.56, 0.64, 1)` — used for modal-rise, toast-rise, sleuth panel rise, brand-logo hover, sleuth-toggle slider.
- Hover lifts of −1/−2 px (`translateY(-1px)` / `-2px`) plus shadow swap.
- All transitions disabled under `prefers-reduced-motion: reduce`. Honor `LocalAccessibilityManager` in Compose.

---

## Component patterns

### Buttons

Shared shape: 9 px radius, inline-flex with 6 px gap (icon + label), `padding: 8px 16px`, font 13 px / weight 500, `white-space: nowrap`, focus ring = `--shadow-focus` (3 px accent glow), active `translateY(1px)`, disabled opacity 0.5.

- **Primary (`.btn.primary`)** — fill = `--accent-grad`, color white, font-weight 600, transparent border, shadow `0 1px 2px rgba(0,0,0,0.10), 0 4px 12px var(--accent-glow)`. Hover → `--accent-grad-hover` + larger glow (`0 6px 16px var(--accent-glow)`). Auth submit variant adds `width: 100%; padding: 12px 16px; font-size: 15px; font-weight: 600`. In M3: `Button` with `colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)` and a `Modifier.background(brush = Brush.linearGradient(...))`.
- **Secondary (default `.btn`)** — bg `--bg-elev`, border `--border`, text `--text`. Hover swaps to `--bg-elev-2` and border `--border-strong`. M3 mapping: `OutlinedButton` with custom outline / `tonalElevation = 1.dp`.
- **Ghost (`.btn.ghost`)** — fully transparent bg, no shadow, hover only adds `--bg-elev-2` fill. M3 mapping: `TextButton`.
- **Danger (`.btn.danger`)** — fill = `--danger-grad`, color white, weight 600, shadow tinted red `0 4px 12px rgba(244,63,94,0.30)`. Hover: solid `linear-gradient(135deg, danger-strong, danger)`.
- **Icon button (`.icon-btn`)** — 30×30 square, 7 px radius, transparent bg, color `--text-muted`. Hover bg `--bg-elev-2`, color `--text`. Danger variant: hover fill `--danger`, text white. M3: `IconButton` with custom hover colors.
- **Link button (`.link-btn`)** — sidebar text actions (Profile, Change password, Logout, Export CSV, Theme). No bg, 7 px radius, padding 7px 10px, color `--text-muted`, hover `--text` on `--bg-elev-2`.
- **Split-button caret (`.new-item-caret`)** — flat right side joins the primary main button (left rounded only). Left side has a `1px solid rgba(255,255,255,0.16)` divider. Drops a menu `top: 100%+4px; right:0` with 9 px radius, `--bg-elev` bg, `--border-strong` outline, `shadow 0 8px 24px rgba(0,0,0,0.35)`.

### Cards

- **Generic card** — `bg-elev` + 1px `--border` + 12 px radius + `--shadow-sm`. Hover `translateY(-2px)` + `--shadow`. Examples: chart-card (18 px padding), audit-controls (boxed group `14px 16px`), events-controls, sessions-controls.
- **KPI tile** — `--kpi-bg` (dark = subtle gradient, light = white), 12 px radius, 1px `--border`, `16px 18px` padding. Hover lifts and overlays a soft accent gradient via `::before { background: linear-gradient(135deg, transparent 60%, var(--accent-soft) 100%) }`. Active (selected filter): `border-color: --accent` + outer ring `0 0 0 1px var(--accent)`. Compose: render KPI as `Card(onClick)` with conditional outline + `Box` over the content for the corner-glow gradient overlay.
- **Event card (`.event-card`)** — `bg-elev`, `--border-strong` outline, 12 px radius, 16 px padding, vertical flex with 10 px gap. Hover: `border-color: --accent`, lift `-1px`, deeper shadow `0 4px 16px rgba(0,0,0,0.25)`. Content order: icon + name + scheduled-for date (accent-colored), then stat row (`task count`), then manager chips wrap.
- **Audit row** — `bg-elev` background, 1px `--border`, 10 px radius, 12px-16px padding, hover bg `--bg-elev-2`. Has an avatar-sized `audit-icon` (32×32, 8 px radius, `--accent-soft` fill, `--accent` glyph) and `audit-action` / `audit-entity` monospace pills.
- **Session row** — 36 px avatar + main + actions in a 3-column grid, 14×16 padding. `.is-current` highlights with accent border and `--accent-soft` background tint, plus an "Current" gradient flag chip.
- **Comment** (in modal) — `bg-elev-2`, `--border` outline, **left border 3 px solid `--accent`** (accent rail), 10 px radius, 14×16 padding. Hover: border lightens. Inside: head row (author/time), body (whitespace-prewrap rendered HTML), optional attachment strip separated by a 1 px `--border` divider on top.
- **Attach card** — vertical layout: 120 px preview area on `--bg` bg with bottom 1 px `--border`, then meta (name 2-line clamp + size), then 2-button action row separated by a 1 px top border. Hover: lifts `-2px`, deeper shadow.

### Inputs

All `.field input`, `.field select`, `.field textarea` share:
- bg `--bg-elev`, color `--text`, border `1px solid --border-strong`, radius 9 px, padding `10px 13px`, font 13.5 px.
- Inner highlight + drop shadow `inset 0 1px 0 rgba(255,255,255,0.02), 0 1px 2px rgba(0,0,0,0.18)`.
- Hover: border keeps but bg `--bg-elev-3`.
- Focus: border `--accent`, bg `--bg-elev-3`, `--shadow-focus` ring (3 px accent glow).
- Placeholder `--text-faint`.
- Disabled: opacity 0.55, border-style **dashed**, cursor not-allowed.
- Label above (in a `.field`): 12 px, weight 600, color `--text-muted`, optional red `*` (`<em>` colored `--danger`).
- Field-help small text: 11-12 px `--text-faint`.

Variants:
- **Search input** (topbar) — 10 px radius, 14 px padding, hover/focus border swaps to accent.
- **Code input** (TOTP / verification) — monospace 20 px, letter-spacing 0.4em, center-aligned.
- **Custom date button (`.bh-date-btn`)** — 10 px radius, hosts icon + label + clear `×` round button. The native `<input type=date>` is visually hidden; popover calendar grid follows.
- **Custom select (`.bh-sel-btn`)** — visually identical to `.field select`, paired with floating popover containing `.bh-sel-row` items (8 px radius, hover bg `--bg-elev-2`, selected uses `--accent-soft` + `--accent` text + weight 600).
- **Checkbox** — uses native browser checkbox with `accent-color: var(--accent)`.
- **Multi-select dropdown (`.ms-btn`)** — pill-like button with caret; panel is a `--bg-elev` card containing `.ms-row` items each with a small 16×16 check square (4 px radius). Selected rows show `--accent-soft` bg, `--accent` text, filled check square.

### Modals

- Overlay fixed inset:0, bg `rgba(2,6,23,0.66)` + `backdrop-filter: blur(4px)`, z-index 1000 (confirm modal 1100).
- Card `bg-elev`, 1px `--border`, radius 14 px, max-width tiers: default 540 / sm 400 / lg 760 / xl 920 / xxl 1400 + 95vw; max-height `92vh / 92dvh`.
- Animation: `modal-rise 0.22s cubic-bezier(0.34, 1.56, 0.64, 1)` (fade + 12 px slide-up + 0.98 scale).
- Header `.modal-head`: 16×20 padding, bottom 1 px `--border`, H2 17 px weight 700, with close `✕` icon-btn on the right.
- Body `.modal-body`: 18×20 padding-top + auto-scroll, `-webkit-overflow-scrolling: touch`.
- Footer `.modal-foot`: justify-end, 14×20 padding, top 1 px `--border`, **sticky-bottom** inside scrolling body — actions always visible.
- Compact confirm: 380 max-width, body padding 14×20×4, foot 12×20×16, no top border on foot. Buttons: `[Cancel ghost] [Delete danger]`.
- Bug modal (xxl): special 2-column grid `minmax(0,1fr) 360px` with 28 px gap, side column is a sticky meta panel (`bg-elev-2`, 10 px radius, 18 px padding). Title field uses a larger 19 px / 600 weight input. Below 900 px it collapses to single column.

### Chips / tags / status badges

- **Badge** (`.badge`) — pill 999 px radius, padding `3px 10px`, font 11 px / weight 600, letter-spacing 0.02em, **transparent fill with currentColor 10% opacity overlay** via `::before`, border 1 px `currentColor`. The color is driven by `data-status` / `data-priority` / `data-env` attributes (palette listed above). Critical priority gets a stronger 16 % overlay. Env badges use specific border alpha colors (e.g. PROD border `rgba(244,63,94,0.45)`). Compose: `Surface(color = severityColor.copy(alpha=0.1f), border = BorderStroke(1.dp, severityColor)) { Text(...) }`.
- **Assignee chip** — pill with mini avatar + name, bg `--bg-elev-2`, 1 px `--border`, padding `2px 8px 2px 4px`, font 11 px. Name truncates at 160 px max-width with ellipsis. `.avatar` (22×22 circle, 10 px font weight 700, fill `--accent-grad`, white text, glow shadow `0 1px 2px rgba(0,0,0,0.15)`). Mini avatar variant `.avatar.mini` is 18×18 / 9 px font.
- **Filter chip** (`.chip` in chip-picker) — pill 999 px, padding `5px 11px`, bg `--bg-elev`, 1 px `--border`, font 12 px. Hover: border accent + lift `-1px`. **Selected**: fill `--accent-grad`, white text, transparent border, shadow `0 2px 6px var(--accent-glow)`. Sub-label inside (e.g. role) lightens to `rgba(255,255,255,0.85)` when selected.
- **Attach pill** — pill with paperclip count, bg `--bg-elev-2`, 1 px `--border`, font 11 px `--text-muted`.
- **Event pill** — `--accent-soft` bg, `--accent` text, 999 px radius, font 12 px.
- **Att-count pill** — same as attach-pill but accent-soft bg / accent text.
- **Project key chip (`.proj-key`)** — small monospace caps, padding `1px 6px`, radius 4 px, bg `rgba(255,255,255,0.06)` (dark) / `rgba(0,0,0,0.05)` (light), color `--text-muted`. Like Jira's "WEB-42".
- **Invite-status pill** — solid fill / on-color pairs listed above. 999 px radius, font 10 px caps weight 700.
- **Audit action / entity tags** — small monospace pills inline with the actor name.

### Side nav / top bar

- **Sidebar**: 280 px wide (`64px` when collapsed), `bg-elev`, right border `--border`, sticky / max-height 100vh, internal scrolling. Sections: brand row, org banner, primary nav, projects list, users list, account card, footer (export / theme).
- **Brand row**: 44 px logo + gradient title + uppercase version + small collapse icon-btn (26×26). Logo hover rotates `-8deg` and scales `1.05`.
- **Org banner**: subtle accent gradient pill `linear-gradient(135deg, rgba(accent,0.08), rgba(accent,0.02))`, 10 px radius, displays org name + lowercase meta line.
- **Nav button (`.nav-btn`)**: full-width, transparent, padding `10px 12px`, radius 9 px, font 14 px / weight 500, 12 px gap to icon. Hover bg `--bg-elev-2`. **Active**: bg `--accent-soft`, text `--accent`, weight 600, plus a 3 px gradient rail (`--accent-grad`) on the left at 22 %–78 % of height (rounded right corners).
- **Side-section header**: 11 px uppercase 0.08em-letter-spacing, `--text-muted`, with a `+` icon-btn for create.
- **Side-list items**: 13 px, 7 px radius, padding `7px 10px`, with optional 10 px swatch dot (colored project), hover bg `--bg-elev-2`, active bg `--accent-soft` text `--accent`.
- **Account card**: `--bg-elev-2`, 1 px `--border`, 10 px radius, 10 px padding, 36 px gradient avatar + name/role/email (truncated). Below it a column of link-btns.
- **Topbar**: sticky top, `padding: 16px 24px`, bottom 1 px `--border`, **glass surface** = `--surface-glass` + `backdrop-filter: blur(12px)` (use Material 3 large top app bar w/ translucent scrim in Compose). Layout: hamburger (mobile only), page title (19 px / 700 / -0.01em), centered search (max-width 480 px), then "+ New" split button.
- **Type tabs (`.type-tabs`)**: horizontal scroll-x row, 4 px gaps, padding `0 24px`, bottom 1 px `--border`. Each `.type-tab` 10×14 padding, font 14 px / 600 / `--text-muted`, hover `--text`. Active: `--text` text + 2 px bottom border `--accent` (overlapping the row border via `margin-bottom: -1px`). Counter pill on the right: bg `--bg-elev-2`, when active `--accent` bg with white text.

### Empty states

- `.empty-state` — `bg-elev` (note: explicitly `--bg-elev` not transparent), 1 px **dashed** `--border`, 12 px radius, padding 56 px, center-aligned, `--text-muted` text. Used when list is empty.
- `.no-content` — italic, `--text-faint`, padding 28 px 8 px, centered (inside modal sections).
- `.events-empty`, `.sessions-empty`, `.bug-attach-empty` — variants with 1 px dashed border and smaller paddings.
- `.upload-zone` — dashed `2px --border`, 12 px radius, `bg-elev`. `.drag-over` swaps to solid `--accent` border + `--accent-soft` bg.

### Toasts

- `.toast` — fixed bottom 28 px, centered horizontally, pill 999 px, bg `--bg-elev`, 1 px `--border-strong`, padding `12px 20px`, font 13.5 px weight 500, `--shadow-lg`. Animation: `toast-rise 0.25s` spring (fade + 16 px slide-up). z-index 200.
- Variants: `.error` → border `--danger`, text `--danger`, bg `--danger-soft`. `.success` → analogous with `--ok`.

### Attachment thumbnails

- **Attach card (`.attach-card`)** — see Cards section. Preview area 120 px tall on `--bg`. Image/video uses `object-fit: cover`. File icons render as a 38 px `--text-muted` emoji. Meta name 2-line clamped + size line. Actions row contains Download / Delete pseudo-buttons.
- **Staged thumb (`.attach-staged`)** — inline pill with 28 px thumbnail or icon square (5 px radius) + name (130 px ellipsis) + size. On hover: a small −6/-6 px red circle remove `×` appears (18 px round, bg `--danger`, white).
- **Comment attach button** (`.comment-attach-btn`) — 9 px radius pill, `--bg-elev-2` bg, hover swaps to accent-soft / accent text.

### Comment threads

- See Comments above. List `gap: 10px`.
- Comment form: 1 px top divider, vertical stack with textarea (84 px min, vertical resize), then a flex row with `Attach files` button, staged-files preview, and right-aligned `Post comment` primary button.
- Edit-in-place: replaces body with `comment-edit-input` textarea + Save/Cancel buttons (small 6×14 padding, font 12 px).
- Comment admin actions (edit / pencil + delete / trash) are 65 % opacity icon-buttons in the head-right, full opacity on hover.
- Activity row variant: lighter `--bg-elev-2` background, 11×14 padding, 9 px radius. Used in the collapsible activity history.

### Sleuth chatbot FAB + panel

- **FAB** (`.sleuth-fab`): fixed right 22 / bottom 22, 56×56 circle, fill `--accent-grad`, white glyph (30 px svg icon with subtle drop shadow). Triple-shadow: `0 8px 22px var(--accent-glow), 0 4px 10px rgba(0,0,0,0.25), inset 0 1px 0 rgba(255,255,255,0.18)`. Animated **pulse ring** (`::after`, 2 px accent border, scale 0.9 → 1.18, opacity 0.55 → 0, 2.6 s). Hover lifts `-2px` and scales `1.05`. Unread badge (`::before`) sits top-right `-2/-2`, 20 px minimum round pill, bg `--danger-strong`, 2 px `--bg` border, white text 11 px weight 700. z-index 900.
- **Panel** (`.sleuth-panel`): fixed right 22 / bottom 90, 400×560 (mobile: left/right 12, height 80vh), `bg-elev` bg, 1 px `--border`, 16 px radius, `--shadow-xl`. Origin bottom-right; rise animation 0.22 s spring.
- **Header**: gradient `--accent-grad` strip with overlay radial highlights, padding `13 14`. Avatar 36 px round translucent `rgba(255,255,255,0.18)` over the gradient with backdrop-blur. Title + "Online" status with pulsing green dot (`#4ade80`). Header action icon-btns are 28×28 with translucent white fill.
- **Tabs**: row of 3 (Chat / History / Settings), bg `--bg-elev-2`, bottom 1 px `--border`, each tab 12 px / 600, active gets `--accent` text + 2 px `--accent` bottom border.
- **Messages**:
  - User: align-end, `--accent-grad` fill, white text, radius 14 px with `border-bottom-right-radius: 4px` (tail), glow shadow.
  - Bot: align-start, `--bg-elev` bg, `--border` outline, `border-bottom-left-radius: 4px`.
  - Error: `--danger-soft` bg + `--danger` border/text.
  - System: center pill, `--bg-elev-2`, `--text-muted`, 999 px radius, 12 px.
  - Inline markdown rendered: bold, italic, inline code (`--bg-elev-2` bg / `--accent` color), `<ul>`, `<ol>`, `<a>` (accent, underlined).
- **Typing indicator**: bot-style bubble with 3 accent dots, staggered keyframe bounce.
- **Chips / suggestions / quick-replies**: 999 px pill, `--bg-elev` or `--bg-elev-2`, hover swaps to `--accent-soft` border `--accent` text.
- **Confirm prompts**: summary line + two-button row (`Yes` = gradient primary, `No` = secondary).
- **Inline table**: 10 px radius wrapper, `--bg-elev`, 1 px `--border`. Header monospace caps. Clickable rows hover `--accent-soft`.
- **File download block**: 38 px accent-soft icon square + name/meta + gradient "Download" pill button.
- **Input row**: 11×12 padding, `--bg-elev`, top 1 px `--border`. Textarea: `--bg-elev-2` bg, 10 px radius, 38 px min / 110 px max height. Send button: 55×38 gradient pill with paper-plane glyph.
- **History tab**: 14 px-padded list of 10 px radius cards (`--bg-elev` + 1 px `--border`). Hover slides right `+2 px`. Empty state italic centered.
- **Settings**: rows with label + control (select / native checkbox / toggle switch). Custom 38×22 **toggle switch**: track `--border`, knob 16 px white circle, checked = `--accent-grad` track with knob translated 16 px.
- **Clear-history button** (`.sleuth-clear-btn`): `--danger-soft` bg, 1 px `--danger` border, `--danger` text, hover swaps to solid `--danger-strong` fill white text.

---

## Screen inventory

### Authentication / onboarding (separate HTML files, decorated background)

1. **Sign in (`login.html#loginForm`)** — Hero logo (68 px floating animation), "Bug Hunter" gradient H1, "Sign in to continue" tagline. Form: email, password, primary "Sign in" button. Auth-links row: "Forgot your password?" left, theme toggle right. Footer: "New here? Create an organization". Reached by visiting `/login.html` (root for unauthed users).
2. **TOTP / 2FA challenge (`login.html#totpForm`)** — Shown after correct password if 2FA enabled. Help paragraph, 6-digit code input (`one-time-code`, monospace), primary Verify button, "← Use a different account" back link. Reached by completing step-1 login.
3. **Forgot password (`login.html#forgotForm`)** — Toggled from sign-in via "Forgot your password?". Help paragraph, email input, "Send reset link" primary, "← Back to sign in" link.
4. **Reset password (`reset.html`)** — Linked from emailed reset URL. New password + confirm fields, "Set new password" primary submit, "← Back to sign in".
5. **Create organization / Sign up (`signup.html`)** — Wide auth card. Fields: organization name, your name, work email, password (with field-help hint). Submit "Create organization". Fineprint explaining admin role. Footer "Already have an account? Sign in".
6. **Accept invitation (`accept-invite.html`)** — Wide card. Initially shows loading spinner + "Checking your invitation…". On success: `invite-preview` banner ("You've been invited to join **Org** by **Inviter** as **Role**" with email shown in monospace code), name field, password field, "Accept & sign in" primary. On error: alert + "Go to sign in" link. Linked from emailed invite URL.

### Main SPA (`index.html`, switched via `setView()`)

7. **Work Items / List view (`#viewList`)** — Default tab "All Work Items". Topbar (search + "+ New Bug" split button). Type tabs row with counters (All / Bugs / Requirements / Tasks). KPI strip (Total / Open / Resolved / Closed / Resolve Later — clickable filters). Filter bar (Project / Type / Status / Priority / Environment / Assignee multi-selects + Clear button). Then the bug-table (sortable columns: ID, Title, Status, Priority, Env, Project, Event, Assignees, Due, Att). Empty-state and pagination at the bottom. Row click → Bug detail modal.
8. **Events list view (`#viewEvents` list mode)** — Reached via sidebar "Events". Page-intro banner (📅 + heading "Events"). Controls row ("+ New Event", Refresh, summary). Filter bar (search by name/description, date filter, Clear). Grid of event-cards (`auto-fill, minmax(280px,1fr)`). Empty state copy.
9. **Event detail view (`#viewEvents` detail mode)** — Reached by clicking an event card. Head row with "← Back" button, event name, "✎ Edit", "🗑 Delete". Meta block (scheduled-for, description, managers). Controls bar ("+ Add Task"). Inner filter bar (search, status MS, priority MS, assignee MS, Clear). The tasks render as another `bug-table`-style list scoped to the event.
10. **Analytics view (`#viewAnalytics`)** — Charts grid (auto-fill minmax 360 px). Cards: "Items over the last 14 days" (timeline), "By Status", "By Priority", "By Environment" (hidden for Requirement/Task tabs), "By Project", "Top Assignees". Each card is a chart-card with bar-chart rendering. Reached via sidebar "Analytics".
11. **Audit Trail view (`#viewAudit`)** — Manager+/Admin. Page-intro (🛡). Controls (entity filter select, actor filter select, search, Clear, Refresh). List of `audit-item` rows with icon + actor + action pill + entity pill + detail line + time. Reached via sidebar "Audit Trail".
12. **Sessions view (`#viewSessions`)** — Admin-only. Page-intro (🔐). Boxed controls with help text + Refresh. List of `session-row` grids (avatar + main meta + actions). Current device highlighted with accent border + "Current" gradient flag. Reached via sidebar "Sessions".
13. **Invitations view (`#viewInvitations`)** — Manager+/Admin. Page-intro (📨). Controls ("+ Invite a teammate", Refresh). List of invitation rows with status pill (Pending/Accepted/Revoked/Expired) and actions. Reached via sidebar "Invitations".

### Modal-based screens (overlay the main views)

14. **Bug modal — Create mode (`#modalBug` create)** — Header "New Bug" (and similar for "New Requirement" / "New Task"). Title input full-width. Description rich-text. Pre-creation attachment uploader. Side-panel meta (Type, Project, Status, Priority, Environment, Reporter (locked), Assignees chip-picker, Event, Due date). Foot: Cancel ghost / **Create** primary. Reached from "+ New Bug" split button.
15. **Bug modal — View/Edit mode (`#modalBug` edit)** — Header "Bug #N · Title" + subtitle. Same form, but with delete button (admin-only) in head, Attachments grid, Comments thread + composer, collapsible Activity history. Side panel also shows Created/Updated timestamps. Foot: Cancel / **Save** primary. Reached by clicking a row in the table.
16. **Project create/edit modal (`#modalProject`)** — Name, Key (uppercase pattern, monospace), Color picker, Description textarea. Save primary. Reached from sidebar Projects "+" or project hover edit (admin).
17. **User create/edit modal (`#modalUser`)** — Name, Email, Role select (Member/Manager/Admin), Password (with hint), `is_active` checkbox row. Save primary. Reached from sidebar Users "+" (admin).
18. **Project members modal (`#modalProjectMembers`)** — Add-row (user select + role select + Add ghost button), then list of member-row cards (avatar + name/email + role select + Remove danger). Reached from project context menu.
19. **Profile modal (`#modalProfile`)** — Multi-section card. Identity section (editable display name with Save). Account section (Role / Organization read-only fields, monospace values). Email change section — two-step: request (new email + current password, "Send verification code") then confirm (6-digit code, monospace, "Confirm"/"Cancel"). Reached from sidebar "👤 Profile".
20. **Change password modal (`#modalChangePassword`)** — Current / New / Confirm fields, "Update password" primary. Reached from sidebar "🔑 Change password".
21. **Invite a teammate modal (`#modalInvite`)** — Email, Role, Add to projects (scrolling checkbox list with swatch + name), "Make them a lead" check-row, "Send invite" primary. Reached from Invitations view "+ Invite a teammate".
22. **Event create/edit modal (`#modalEvent`)** — Name, Scheduled-for date, Managers chip-picker (admin/manager users only), Description textarea. Save primary. Reached from Events list "+ New Event" or detail "✎ Edit".
23. **Confirm dialog (`#modalConfirm`)** — Small compact card (380 px). Title, message paragraph, **[Cancel ghost] [Delete danger]** buttons. z-index 1100 so it stacks above other modals. Reached from any destructive action (logout, delete bug/user/project, revoke session, remove member, etc.).
24. **Sleuth chatbot panel (always-on FAB)** — Persistent FAB bottom-right. Click opens floating panel with **Chat / History / Settings** tabs. Chat tab: messages list with quick-reply chips and suggestion chips, optional inline confirm prompts, inline tables, inline file-download blocks, then input row with textarea + send. Reached from the FAB on any authenticated screen (hidden during loader & under modals).

### Global / chrome (overlays not tied to a single screen)

25. **Global blocking loader (`#globalLoader`)** — Full-viewport scrim `rgba(8,12,22,0.42)` + blur. Center card with 42 px spinner ring (accent border-top) + "Working…" text. Triggered by any in-flight network action (`showLoader/hideLoader`).
26. **Mobile sidebar drawer** — Same sidebar slides in from the left at <900 px breakpoint over a `--bg` scrim (`rgba(2,6,23,0.55)`). Opened via topbar `☰` hamburger.

---

## Mobile-specific notes

Breakpoints in CSS (smallest to largest):
- **`@media (max-width: 1100px)`** — Bug modal grid narrows side column to 320 px.
- **`@media (max-width: 900px)`** — App grid collapses to single column. Sidebar becomes a fixed left-edge drawer with `transform: translateX(-100%)` (transition `0.25s cubic-bezier(0.4,0,0.2,1)`), `.open` class slides in. Hamburger button shown. Bug modal grid → single column, side panel becomes a dashed-outline section. Bug-table hides the actions/env/att/assignees/priority columns. Project / event title-cells capped to 130 / 140 px.
- **`@media (max-width: 720px)` (in chatbot.css and key sizing)** — Sleuth FAB shrinks to 52×52, repositions to bottom-right 14 px. Sleuth panel becomes left:12 / right:12 / bottom:78, height 80vh, max-height `calc(100vh - 96px)`, radius 14 px.
- **`@media (max-width: 700px)`** — KPI strip → 2 columns + 14 px padding. Filter-bar 0×12. Topbar 12 px. View padding 0×12. `.row` becomes column. Modals max-height 95vh. Modal `.xxl` width 100 %. Charts grid → 1 column. Audit / Sessions / page-intro paddings shrink. Multi-select buttons go 50 % flex-basis. Session row collapses to single column. Audit-time wraps to its own row. Invitations controls compress, member-add stacks. Invite-status pill shrinks (9 px / 2×7 padding).
- **`@media (max-width: 600px)`** — Events filter-bar inputs become 100 % flex-basis.
- **`@media (max-width: 540px)`** — Sleuth FAB 52×52 final, panel left/right insets 12 px.
- **`@media (max-width: 500px)`** — KPI strip stays 2-col but row 5 spans full width (`grid-column: span 2`). KPI tile padding 12×14, KPI num shrinks to 22 px. Page title 17 px. **Topbar wraps** (`flex-wrap: wrap`) and pushes search to a full-width row below title+new-button. Multi-select wraps to 100 %.
  - **Modals go full-screen**: `.modal { padding: env(safe-area-inset-top, 0) 0 env(safe-area-inset-bottom, 0) }`. `.modal-card { max-height: 100dvh; height: 100dvh; border-radius: 0; max-width: 100% }`. Confirm dialog explicitly opts OUT: stays card-sized with `width: calc(100% - 32px); max-width: 380px; max-height: 80dvh; border-radius: 14px; margin: auto`. In Compose: use a `Dialog` with `DialogProperties(usePlatformDefaultWidth = false)` and apply `WindowInsets.safeContent` padding; only the confirm dialog keeps the bottom-sheet-like rounded card.
  - Attachment grid → 2 columns. Bug-modal head action buttons shrink (6×10 / 12 px).
  - Auth pages: cards drop all chrome (`max-width: 100%; border-radius: 0; box-shadow: none; border: none`), auth-shell padding 0, body uses `env(safe-area-inset-top/-bottom)` and `align-items: stretch`. Invite project list max-height 160 px.
- **`@media (max-width: 380px)`** — Custom date popover becomes `calc(100vw - 24px)` wide.

### `dvh` (dynamic viewport height) usage
- `.modal-card { max-height: 92vh; max-height: 92dvh }` — newer browsers prefer `dvh` so mobile browser chrome shrinkage doesn't clip the modal.
- `.modal-card { max-height: 100dvh; height: 100dvh }` at ≤500 px to actually fill the visible viewport.
- `#modalConfirm .modal-card { max-height: 80dvh }` to keep it dialog-sized.
- `.bh-rt-editor { max-height: 60vh }` for the rich-text composer.
- `.members-list { max-height: 50vh }`.
- Compose mapping: use `LocalDensity` + `WindowInsets.systemBars` rather than fixed dp; for "as big as possible without clipping" prefer `Modifier.fillMaxHeight()` inside a window-insets-aware `Dialog`.

### Safe-area insets
- Mobile modal at ≤500 px: `padding: env(safe-area-inset-top, 0) 0 env(safe-area-inset-bottom, 0)`. Auth body matches.
- iOS PWA meta tags in `<head>`: `apple-mobile-web-app-capable=yes`, `apple-mobile-web-app-status-bar-style=black-translucent`, `theme-color=#6366f1`.
- Compose mapping: wrap content with `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` for auth and modal screens; allow status bar to draw under the gradient where appropriate (cf. `auth-body::before` decorative blobs).

### Other mobile patterns to mirror
- Glass topbar: `backdrop-filter: blur(12px)` on the topbar + `--surface-glass`. Use `Modifier.hazeChild` (haze lib) or a translucent solid `Color` with high alpha — Android < 12 doesn't support backdrop blur natively, fall back to opaque `--bg-elev` at 95 % alpha.
- Pull-to-refresh isn't present in CSS but the explicit Refresh buttons (Events, Audit, Sessions, Invitations) suggest an Android `PullToRefresh` would be a natural enhancement.
- Bug-table on phones: priority/assignees/env/att columns are dropped — for Compose, render a `LazyColumn` of card-rows instead of a horizontal table at compact widths.
- Mobile sidebar uses a translucent backdrop (`rgba(2,6,23,0.55)` + blur 2 px) and a 280 px drawer — Compose `ModalNavigationDrawer` matches.
- The chatbot panel uses `position: fixed` + dvh-friendly height. On Compose use a bottom-sheet variant on phones; on tablets keep the floating 400×560 anchored card.
- `prefers-reduced-motion: reduce` shortens all animation/transition durations to 0.01 ms — honor `LocalAccessibilityManager.current.areAnimationsEnabled` in Compose.

---

### Color literals reference (for theme JSON / Compose `Color(0xFF…)` mapping)

Quick dump of unique hex literals across both themes for the M3 designer (not exhaustive of every rgba) so you can build a single `Color.kt`:

```
#0a0e1a  #131829  #1a2138  #232c47  #344063  #2a3553  #3a4870
#eef2ff  #9aa6c4  #5e6a85
#818cf8  #6366f1  #4f46e5  #4338ca
#38bdf8  #0ea5e9  #0284c7  #0369a1
#f43f5e  #e11d48  #dc2626  #b91c1c
#f59e0b  #fbbf24  #d97706
#10b981  #34d399  #059669
#94a3b8  #cbd5e1  #64748b
#a78bfa  #7c3aed
#f4f6fb  #ffffff  #f1f4fb  #e6ecf6  #e2e8f0  #c8d2e0
#0f172a  #475569
#4ade80
#1e3a5f #e0eaf8  #1f4d36 #d4f0dd  #5f1f1f #f8d7d7  #5f4216 #fbe7c4
```
