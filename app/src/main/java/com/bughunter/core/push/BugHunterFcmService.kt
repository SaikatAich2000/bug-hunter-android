package com.bughunter.core.push

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.bughunter.MainActivity
import com.bughunter.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

/**
 * Receives the wire-level FCM push from Google Play Services and turns it
 * into a notification the user can see + tap.
 *
 * Why we only use **data** payloads (not notification payloads):
 *
 *   - notification payloads bypass onMessageReceived() when the app is
 *     in the background. FCM auto-posts them and we lose the chance to
 *     route them to the right channel or attach a deep-link intent.
 *   - data payloads always hit onMessageReceived(). We render every
 *     notification ourselves, so behaviour is identical whether the
 *     app is foreground, background, or fully killed.
 *
 * Expected payload (set by the backend in push_service.py):
 *
 *   {
 *     "channel":   "mentions" | "assignments" | "activity",
 *     "title":     "…",
 *     "body":      "…",
 *     "deep_link": "bh://bug/123" | "bh://event/45" | "bh://requirement/9",
 *     "tag":       "bug:123"   // optional — same tag replaces older
 *                              // notifications for the same item.
 *   }
 *
 * onNewToken delegates to the WorkManager-backed register job so the
 * call survives a flaky network at rotation time.
 */
@AndroidEntryPoint
internal class BugHunterFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // We can't inject a coroutine scope into a Service this lean;
        // enqueueing the work is synchronous and survives this method
        // returning + the service being destroyed.
        DeviceRegistrationWorker.enqueueRegister(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val channelId = NotificationChannels.resolve(data["channel"])
        val title = data["title"]
            // Backend always sends `title` in data. Fall back to the
            // notification payload if a stray legacy notification-key
            // push still gets through — better than dropping it.
            ?: message.notification?.title
            ?: getString(R.string.app_name)
        val body = data["body"] ?: message.notification?.body ?: ""
        val deepLink = data["deep_link"]
        val tag = data["tag"]  // optional — null disables tag-based dedupe

        val notification = buildNotification(
            context = this,
            channelId = channelId,
            title = title,
            body = body,
            deepLink = deepLink,
        )
        val manager = getSystemService<NotificationManager>() ?: return
        val notificationId = tag?.hashCode() ?: Random.Default.nextInt()
        // Posting with a tag lets later notifications for the SAME item
        // (same `tag` data field) replace older ones — e.g. five status
        // changes on bug 42 collapse to the latest, not five separate
        // entries. Without a tag we use a random id so concurrent posts
        // don't stomp on each other.
        manager.notify(tag, notificationId, notification)
    }

    private fun buildNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        deepLink: String?,
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Carry the deep link as the Intent data URI so MainActivity
            // can use the same router that handles VIEW intents from
            // app-link clicks. Falls through to the dashboard when null.
            if (deepLink != null) data = Uri.parse(deepLink)
            // Tag the source so debug builds can distinguish push-driven
            // launches from cold-start launches in the navigation log.
            putExtra(EXTRA_FROM_PUSH, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (deepLink ?: "dashboard").hashCode(),
            intent,
            // FLAG_IMMUTABLE is mandatory on API 31+. UPDATE_CURRENT so a
            // queued PendingIntent for the same request code picks up
            // the new extras (different deep link) instead of replaying
            // the stale one.
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val EXTRA_FROM_PUSH = "bh.from_push"
    }
}
