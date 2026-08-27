package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object YtDlpDownloadHelper {

    private const val TAG = "YtDlpDownloadHelper"
    private const val DOWNLOAD_SUBDIR = "FreedomPlayer"

    fun getDownloadDirectory(context: Context): File {
        val prefsHelper = PrefsHelper(context)
        val rootUri = prefsHelper.getRootFolderUri()
        if (rootUri != null) {
            val path = resolveUriToPath(context, rootUri)
            if (path != null) {
                return File(path).also { it.mkdirs() }
            }
        }

        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DOWNLOAD_SUBDIR
        ).also { it.mkdirs() }
    }

    suspend fun downloadTrack(
        context: Context,
        url: String,
        destFolder: Any? = null, // Can be File or DocumentFile
        tag: String? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = DownloadPowerLock.withWakeLock(context) {
        withContext(Dispatchers.IO) {
            val actualTag = tag ?: "dl_${System.currentTimeMillis()}"

            // Register cancellation handler
            val job = coroutineContext[kotlinx.coroutines.Job]
            val registration = job?.invokeOnCompletion {
                if (it is kotlinx.coroutines.CancellationException) {
                    DownloadLogger.i(TAG, "Cancellation received for tag: $actualTag")
                    YoutubeDL.getInstance().destroyProcessById(actualTag)
                }
            }

            var lastException: Exception? = null
            val maxRetries = 3

            try {
                for (attempt in 1..maxRetries) {
                    val tempDir = File(context.cacheDir, "yt_dlp_temp_${System.currentTimeMillis()}_$attempt")
                    try {
                        DownloadLogger.i(TAG, "downloadTrack attempt $attempt/$maxRetries: url=$url, tag=$actualTag")
                        tempDir.mkdirs()

                        // Get video info to determine smart filename
                        val info = try {
                            val infoRequest = YoutubeDLRequest(url.trim())
                            YoutubeDL.getInstance().getInfo(infoRequest)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            DownloadLogger.w(TAG, "Failed to get video info for smart naming")
                            null
                        }

                        val (artist, title) = TrackNameUtils.getSmartSongName(info?.uploader, info?.title)
                        val smartFileName = if (info != null) {
                            DownloadLogger.i(TAG, "Smart naming: [Channel: ${info.uploader}, Title: ${info.title}] -> Result: [$artist - $title]")
                            "$artist - $title"
                        } else {
                            "%(title)s"
                        }

                        val request = YoutubeDLRequest(url.trim())
                        YtDlpManager.configureAudioMp3Request(request)
                        request.addOption("-o", "${tempDir.absolutePath}/$smartFileName.%(ext)s")

                        // Special handling for 403 retries
                        if (attempt > 1 && lastException?.message?.contains("403") == true) {
                            DownloadLogger.i(TAG, "Attempting 403 fix: adding ios player client")
                            request.addOption("--extractor-args", "youtube:player_client=ios,web")
                        }

                        YoutubeDL.getInstance().execute(request, actualTag) { progress, eta, line ->
                            DownloadLogger.d(TAG, "Progress: ${progress.toInt()}%, eta=${eta}s, line=$line")
                            onProgress(progress)
                        }

                        val downloadedFiles = tempDir.listFiles()
                        if (downloadedFiles.isNullOrEmpty()) {
                            throw Exception("No files downloaded or process cancelled")
                        }

                        val resultFile = downloadedFiles.find { it.extension.lowercase() == "mp3" }
                            ?: downloadedFiles.first()

                        val success = when (destFolder) {
                            is DocumentFile -> copyToDocumentFile(context, resultFile, destFolder)
                            is File -> copyToFile(resultFile, File(destFolder, resultFile.name))
                            else -> {
                                val defaultFolder = getDownloadDirectory(context)
                                copyToFile(resultFile, File(defaultFolder, resultFile.name))
                            }
                        }

                        if (success) {
                            DownloadLogger.i(TAG, "Download finished and file moved to destination")
                            return@withContext Result.success(resultFile)
                        } else {
                            throw Exception("Failed to copy file to destination")
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e

                        lastException = e
                        val errorMessage = e.message ?: ""
                        val isRetryableError = errorMessage.contains("No address associated with hostname") ||
                            errorMessage.contains("Unable to download webpage") ||
                            errorMessage.contains("Connection refused") ||
                            errorMessage.contains("Timed out") ||
                            errorMessage.contains("403") ||
                            errorMessage.contains("Forbidden")

                        if (isRetryableError && attempt < maxRetries) {
                            val delayMs = 2000L * attempt
                            DownloadLogger.w(TAG, "Retryable error on attempt $attempt: ${errorMessage.take(100)}. Retrying in ${delayMs}ms...")
                            kotlinx.coroutines.delay(delayMs)
                            continue
                        } else {
                            throw e
                        }
                    } finally {
                        tempDir.deleteRecursively()
                    }
                }
                Result.failure(lastException ?: Exception("Unknown error"))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    DownloadLogger.i(TAG, "Download cancelled: url=$url")
                    throw e
                }
                DownloadLogger.e(TAG, "Download failed after all attempts: url=$url", e)
                Result.failure(e)
            } finally {
                registration?.dispose()
            }
        }
    }

    suspend fun downloadVideo1080p(
        context: Context,
        url: String,
        tag: String? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = DownloadPowerLock.withWakeLock(context) {
        withContext(Dispatchers.IO) {
            val actualTag = tag ?: "dl_${System.currentTimeMillis()}"
            val job = coroutineContext[kotlinx.coroutines.Job]
            val registration = job?.invokeOnCompletion {
                if (it is kotlinx.coroutines.CancellationException) {
                    DownloadLogger.i(TAG, "Cancellation received for tag: $actualTag")
                    YoutubeDL.getInstance().destroyProcessById(actualTag)
                }
            }

            var lastException: Exception? = null
            val maxRetries = 3

            try {
                for (attempt in 1..maxRetries) {
                    val tempDir = File(context.cacheDir, "yt_dlp_temp_${System.currentTimeMillis()}_$attempt")
                    try {
                        DownloadLogger.i(TAG, "downloadVideo1080p attempt $attempt/$maxRetries: url=$url, tag=$actualTag")
                        tempDir.mkdirs()

                        val request = YoutubeDLRequest(url.trim())
                        YtDlpManager.configureVideo1080pRequest(request)
                        request.addOption("-o", "${tempDir.absolutePath}/%(title)s.%(ext)s")

                        // Special handling for 403 retries
                        if (attempt > 1 && lastException?.message?.contains("403") == true) {
                            DownloadLogger.i(TAG, "Attempting 403 fix: adding ios player client")
                            request.addOption("--extractor-args", "youtube:player_client=ios,web")
                        }

                        YoutubeDL.getInstance().execute(request, actualTag) { progress, eta, line ->
                            DownloadLogger.d(TAG, "Progress: ${progress.toInt()}%, eta=${eta}s, line=$line")
                            onProgress(progress)
                        }

                        val downloadedFiles = tempDir.listFiles()
                        if (downloadedFiles.isNullOrEmpty()) {
                            throw Exception("No files downloaded or process cancelled")
                        }

                        val resultFile = downloadedFiles.firstOrNull { it.extension.lowercase() == "mp4" }
                            ?: downloadedFiles.first()
                        val destinationFile = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            resultFile.name
                        )
                        val success = copyToFile(resultFile, destinationFile)

                        if (success) {
                            DownloadLogger.i(TAG, "Download finished and file moved to destination")
                            return@withContext Result.success(destinationFile)
                        } else {
                            throw Exception("Failed to copy file to destination")
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e

                        lastException = e
                        val errorMessage = e.message ?: ""
                        val isRetryableError = errorMessage.contains("No address associated with hostname") ||
                            errorMessage.contains("Unable to download webpage") ||
                            errorMessage.contains("Connection refused") ||
                            errorMessage.contains("Timed out") ||
                            errorMessage.contains("403") ||
                            errorMessage.contains("Forbidden")

                        if (isRetryableError && attempt < maxRetries) {
                            val delayMs = 2000L * attempt
                            DownloadLogger.w(TAG, "Retryable error on attempt $attempt: ${errorMessage.take(100)}. Retrying in ${delayMs}ms...")
                            kotlinx.coroutines.delay(delayMs)
                            continue
                        } else {
                            throw e
                        }
                    } finally {
                        tempDir.deleteRecursively()
                    }
                }
                Result.failure(lastException ?: Exception("Unknown error"))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    DownloadLogger.i(TAG, "Download cancelled: url=$url")
                    throw e
                }
                DownloadLogger.e(TAG, "Download failed after all attempts: url=$url", e)
                Result.failure(e)
            } finally {
                registration?.dispose()
            }
        }
    }

    private fun copyToFile(source: File, destination: File): Boolean {
        return try {
            destination.parentFile?.mkdirs()
            if (source.renameTo(destination)) return true
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            DownloadLogger.e(TAG, "Error copying to file: ${destination.absolutePath}", e)
            false
        }
    }

    private fun copyToDocumentFile(context: Context, source: File, destDir: DocumentFile): Boolean {
        return try {
            val mimeType = "audio/mpeg"
            val destFile = destDir.findFile(source.name) ?: destDir.createFile(mimeType, source.name)
            if (destFile == null) return false

            context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            DownloadLogger.e(TAG, "Error copying to DocumentFile: ${destDir.uri}", e)
            false
        }
    }

    private fun resolveUriToPath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        
        if (uri.scheme == "content" && uri.authority == "com.android.externalstorage.documents") {
            try {
                val docId = if (DocumentsContract.isTreeUri(uri)) {
                    DocumentsContract.getTreeDocumentId(uri)
                } else {
                    DocumentsContract.getDocumentId(uri)
                }
                
                val split = docId.split(":")
                val type = split[0]
                val relativePath = if (split.size > 1) split[1] else ""

                if ("primary".equals(type, ignoreCase = true)) {
                    return "${Environment.getExternalStorageDirectory()}/$relativePath"
                } else {
                    val externalMediaDirs = context.getExternalMediaDirs()
                    for (dir in externalMediaDirs) {
                        if (dir != null) {
                            val path = dir.absolutePath
                            if (path.contains(type)) {
                                val rootPath = path.substring(0, path.indexOf(type) + type.length)
                                return "$rootPath/$relativePath"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                DownloadLogger.w(TAG, "Failed to resolve URI to path: $uri")
            }
        }
        return null
    }
}
