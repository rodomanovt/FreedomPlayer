package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.os.Environment
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object YtDlpDownloadHelper {

    private const val TAG = "YtDlpDownloadHelper"
    private const val DOWNLOAD_SUBDIR = "FreedomPlayer"

    fun getDownloadDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DOWNLOAD_SUBDIR
        ).also { it.mkdirs() }
    }

    suspend fun downloadTrack(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "downloadTrack called: url=$url")

            val downloadDir = getDownloadDirectory()
            val ytDlpVersion = YoutubeDL.getInstance().versionName(context)
            Log.i(TAG, "Starting audio download (mp3 + cover): url=$url, dir=${downloadDir.absolutePath}, yt-dlp=$ytDlpVersion")

            val request = YoutubeDLRequest(url.trim())
            YtDlpManager.configureAudioMp3Request(request)
            request.addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")

            YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                Log.d(TAG, "Progress: ${progress.toInt()}%, eta=${eta}s, line=$line")
                onProgress(progress)
            }

            Log.i(TAG, "Download finished: url=$url")
            Result.success(downloadDir)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: url=$url", e)
            Result.failure(e)
        }
    }
}
