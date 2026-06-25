package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object YtDlpManager {

    private const val TAG = "YtDlpManager"
    private const val UPDATE_TIMEOUT_MS = 90_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initMutex = Mutex()
    private var updateDeferred: CompletableDeferred<Unit>? = null

    fun startBackgroundUpdate(context: Context) {
        Log.i(TAG, "Scheduling background yt-dlp update")
        scope.launch {
            ensureUpdated(context.applicationContext)
        }
    }

    suspend fun ensureUpdated(context: Context) {
        val appContext = context.applicationContext

        val deferred = initMutex.withLock {
            updateDeferred ?: CompletableDeferred<Unit>().also { deferred ->
                updateDeferred = deferred
                scope.launch {
                    runUpdate(appContext, deferred)
                }
            }
        }

        if (!deferred.isCompleted) {
            Log.i(TAG, "Waiting for yt-dlp update to finish...")
        }
        deferred.await()
    }

    private suspend fun runUpdate(context: Context, deferred: CompletableDeferred<Unit>) {
        try {
            val versionBefore = YoutubeDL.getInstance().versionName(context)
            Log.i(TAG, "Updating yt-dlp (current: $versionBefore)...")

            val status = withContext(Dispatchers.IO) {
                val future = CompletableFuture.supplyAsync {
                    YoutubeDL.getInstance().updateYoutubeDL(
                        context,
                        YoutubeDL.UpdateChannel.NIGHTLY
                    )
                }
                try {
                    future.get(UPDATE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                } catch (e: TimeoutException) {
                    Log.w(TAG, "yt-dlp update timed out after ${UPDATE_TIMEOUT_MS / 1000}s, using current binary")
                    null
                }
            }

            if (status != null) {
                val versionAfter = YoutubeDL.getInstance().versionName(context)
                Log.i(TAG, "yt-dlp update result: $status, version: $versionAfter")
            }
        } catch (e: Exception) {
            Log.e(TAG, "yt-dlp update failed, using current binary", e)
        } finally {
            if (!deferred.isCompleted) {
                deferred.complete(Unit)
            }
            Log.i(TAG, "yt-dlp is ready, version: ${YoutubeDL.getInstance().versionName(context)}")
        }
    }

    fun configureRequest(request: YoutubeDLRequest) {
        request.addOption("--remote-components", "ejs:github")
    }

    fun configureAudioMp3Request(request: YoutubeDLRequest) {
        configureRequest(request)
        request.addOption("-x")
        request.addOption("--audio-format", "mp3")
        request.addOption("--embed-thumbnail")
        request.addOption("--embed-metadata")
        // Write the original URL to a custom metadata field that ffmpeg can map to TXXX:purl
        request.addOption("--parse-metadata", "webpage_url:%(meta_purl)s")
    }
}
