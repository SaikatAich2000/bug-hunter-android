package com.bughunter.core.network.api

// Discovery hub for the Retrofit interfaces. There is no longer a single
// aggregator interface, but this object documents where each tag's Api lives
// so future maintainers can find the right surface from one entry point.
//
//   MetaApi          -> /api/health, /api/meta
//   AuthApi          -> /api/auth (signup, login, logout, me, password, profile, email-change, data-export, account)
//   TotpApi          -> /api/auth/2fa
//   OrganizationApi  -> /api/organization
//   BrandingApi      -> /api/branding
//   UsersApi         -> /api/users
//   InvitationsApi   -> /api/invitations
//   ProjectsApi      -> /api/projects
//   MembershipsApi   -> /api/projects/{id}/members
//   BugsApi          -> /api/bugs (list, detail, bulk, comments, attachments, csv export, activity)
//   EventsApi        -> /api/events
//   StatsApi         -> /api/stats
//   SavedViewsApi    -> /api/saved-views
//   CustomFieldsApi  -> /api/projects/{id}/custom-fields, /api/bugs/{id}/custom-values
//   AuditApi         -> /api/audit
//   SessionsApi      -> /api/sessions
//   WebhooksApi      -> /api/webhooks
//   ChatApi          -> /api/chat
internal object BugHunterApi
