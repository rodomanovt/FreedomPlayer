package com.rodomanovt.freedomplayer.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DownloadPowerLock {

    private const val TAG_SUFFIX = "DownloadPowerLock"
    private const val WAKE_LOCK_TIMEOUT_MS = 3 * 60 * 60 * 1000L

    @SuppressLint("WakelockTimeout")
    suspend fun <T> withWakeLock(context: Context, block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "${appContext.packageName}:$TAG_SUFFIX"
            ).apply {
                setReferenceCounted(false)
            }

            try {
                wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
                block()
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
}
