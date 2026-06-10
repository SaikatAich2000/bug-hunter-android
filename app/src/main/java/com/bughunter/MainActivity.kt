package com.bughunter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bughunter.core.nav.BhRoot
import com.bughunter.core.nav.PushIntentBus
import dagger.hilt.android.AndroidEntryPoint

/**
 * launchMode=singleTop in the manifest means the system reuses the
 * existing instance instead of creating a fresh one when a notification
 * is tapped. The fresh Intent arrives in onNewIntent — onCreate's
 * `intent` is the original launch intent, which is stale.
 *
 * We push every incoming intent onto PushIntentBus; BhRoot collects from
 * the bus and calls NavController.handleDeepLink() so the user lands on
 * the right screen regardless of cold-start vs warm-start.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Cold-start path — Compose Navigation picks up `intent.data` on
        // its own via the deepLinks declared in BhNavHost. We also push
        // it to the bus so any listener that subscribes after the nav
        // host is built still receives it.
        intent?.let { PushIntentBus.publish(it) }
        setContent {
            BhRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Warm-start path — the singleTop activity is already running;
        // forward the new intent through the bus so the nav controller
        // can pop or push to the right destination.
        setIntent(intent)
        PushIntentBus.publish(intent)
    }
}
