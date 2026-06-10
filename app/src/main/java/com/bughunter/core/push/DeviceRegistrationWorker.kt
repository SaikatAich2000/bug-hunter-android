package com.bughunter.core.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bughunter.core.data.repository.DevicesRepository
import com.bughunter.core.network.Result2
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * At-least-once device-token register / unregister.
 *
 * FCM rotates the token outside our control (app reinstall, data clear,
 * Play Services GCM purge). The rotation callback (onNewToken) may fire
 * when the network is unavailable — Wi-Fi is off, captive portal, plane
 * mode — so the naive "call the API directly in onNewToken" pattern
 * silently drops every other rotation.
 *
 * WorkManager solves this for free: persisted, retried with exponential
 * backoff, runs on its own thread, survives process death. The trade-off
 * is a small dependency (~600 KB AAR) and a one-off Configuration.
 * Provider in Application — both well worth it for "the token reaches
 * the server".
 *
 * Two actions:
 *   - register / forceRegister via setInputData with ACTION_REGISTER
 *   - unregister via ACTION_UNREGISTER
 *
 * We use ExistingWorkPolicy.REPLACE on the register chain so a queued
 * register for an old token gets replaced by the newer one (no risk of
 * the server flapping between two tokens).
 */
@HiltWorker
internal class DeviceRegistrationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val devices: DevicesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()
        val token = inputData.getString(KEY_TOKEN) ?: return Result.failure()
        val result = when (action) {
            ACTION_REGISTER -> devices.forceRegister(token)
            ACTION_UNREGISTER -> devices.unregister(token)
            else -> return Result.failure()
        }
        return when (result) {
            is Result2.Ok -> Result.success()
            // Retry on any error — exponential backoff handles transient
            // network blips. WorkManager caps total retries at 5 by
            // default, which is enough for "user steps out of the lift".
            is Result2.Err -> Result.retry()
        }
    }

    companion object {
        const val NAME_REGISTER = "bh_device_register"
        const val NAME_UNREGISTER = "bh_device_unregister"

        const val ACTION_REGISTER = "register"
        const val ACTION_UNREGISTER = "unregister"

        const val KEY_ACTION = "action"
        const val KEY_TOKEN = "token"

        fun enqueueRegister(context: Context, token: String) {
            enqueue(context, NAME_REGISTER, ACTION_REGISTER, token)
        }

        fun enqueueUnregister(context: Context, token: String) {
            enqueue(context, NAME_UNREGISTER, ACTION_UNREGISTER, token)
        }

        private fun enqueue(context: Context, name: String, action: String, token: String) {
            val data: Data = workDataOf(KEY_ACTION to action, KEY_TOKEN to token)
            val request = OneTimeWorkRequestBuilder<DeviceRegistrationWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
