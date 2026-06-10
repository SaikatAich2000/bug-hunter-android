package com.bughunter

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bughunter.core.push.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BugHunterApplication : Application(), Configuration.Provider {

    // HiltWorkerFactory is wired by hilt-work + @HiltWorker on each Worker.
    // We surface it via Configuration.Provider so WorkManager picks it up
    // on first access (we disable the default startup initializer in the
    // manifest so this path is the only one that runs).
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Channels must exist before any notification is posted. Creating
        // them on every cold start is the canonical idempotent setup —
        // the system de-dupes by channel id, so re-registering is free.
        NotificationChannels.registerAll(this)
    }
}
