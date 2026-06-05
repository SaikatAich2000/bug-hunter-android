# Design reference

Reference material for contributors. The catalogs below describe what the Android client targets — useful when adding endpoints, DTOs, screens, or theme tokens.

| File | Contents |
|---|---|
| [api_catalog.md](api_catalog.md) | Every backend endpoint the app calls, grouped by feature. |
| [schema_catalog.md](schema_catalog.md) | Pydantic → Moshi DTO mapping for every payload. |
| [ui_design.md](ui_design.md) | Brand palette, typography, spacing, shapes — the source of truth for `BrandTokens`. |
| [auth_flow.md](auth_flow.md) | Login / TOTP / signup / reset / invite state machines. |
| [ux_flows.md](ux_flows.md) | Screen-level user journeys. |

These are reference docs, not requirements. Code is the source of truth — if a catalog and the code disagree, fix whichever is wrong and update both.
