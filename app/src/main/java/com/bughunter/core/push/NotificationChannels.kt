package com.bughunter.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.bughunter.R

/**
 * Three notification channels mirroring the email triggers on the server:
 *
 *  - bh_mentions     — IMPORTANCE_HIGH, vibrates. Used when the current
 *                      user is @mentioned in a comment. High-touch.
 *  - bh_assignments  — IMPORTANCE_DEFAULT. Used when an item is assigned
 *                      to the user. Direct work item, but doesn't peek.
 *  - bh_activity     — IMPORTANCE_LOW, silent. Status changes / comments
 *                      on items the user follows. Background context.
 *
 * Channels are created once on Application.onCreate. Creating a channel
 * a second time is a no-op (the system de-dupes by id), so the call is
 * idempotent — safe to invoke on every cold start.
 *
 * IMPORTANT: never RENAME or remove a channel — the system keeps user
 * overrides (volume, vibration toggle, "this channel is silenced")
 * keyed by ID. Renaming an existing channel resets every user's
 * preference; deleting one drops their custom settings on the floor.
 * Add a NEW channel and stop using the old one if behaviour needs to
 * change.
 */
internal object NotificationChannels {

    const val MENTIONS = "bh_mentions"
    const val ASSIGNMENTS = "bh_assignments"
    const val ACTIVITY = "bh_activity"

    /** Resolve an incoming push's `channel` data field to a real channel id.
     *  Unknown / missing channel → activity (lowest importance). */
    fun resolve(raw: String?): String = when (raw?.lowercase()) {
        "mention", "mentions", MENTIONS -> MENTIONS
        "assignment", "assignments", ASSIGNMENTS -> ASSIGNMENTS
        else -> ACTIVITY
    }

    fun registerAll(context: Context) {
        // Notification channels are an O+ concept; minSdk 29 guarantees they
        // exist, so no SDK guard is needed. Creating a channel that already
        // exists is a no-op, so this is safe to call on every cold start.
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    MENTIONS,
                    context.getString(R.string.fcm_channel_mentions_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.fcm_channel_mentions_desc)
                    enableVibration(true)
                },
                NotificationChannel(
                    ASSIGNMENTS,
                    context.getString(R.string.fcm_channel_assignments_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.fcm_channel_assignments_desc)
                },
                NotificationChannel(
                    ACTIVITY,
                    context.getString(R.string.fcm_channel_activity_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.fcm_channel_activity_desc)
                },
            ),
        )
    }
}
