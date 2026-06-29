package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
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
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "yt_dlp_temp_${System.currentTimeMillis()}")
        try {
            Log.i(TAG, "downloadTrack called: url=$url")
            tempDir.mkdirs()

            // Get video info to determine smart filename
            val info = try {
                val infoRequest = YoutubeDLRequest(url.trim())
                YoutubeDL.getInstance().getInfo(infoRequest)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get video info for smart naming", e)
                null
            }

            val (artist, title) = TrackNameUtils.getSmartSongName(info?.uploader, info?.title)
            val smartFileName = if (info != null) {
                Log.i(TAG, "Smart naming: [Channel: ${info.uploader}, Title: ${info.title}] -> Result: [$artist - $title]")
                "$artist - $title"
            } else {
                "%(title)s"
            }
            
            val request = YoutubeDLRequest(url.trim())
            YtDlpManager.configureAudioMp3Request(request)
            // Use smart title without ID in filename as requested. 
            // Fallback to %(title)s if info was not available.
            request.addOption("-o", "${tempDir.absolutePath}/$smartFileName.%(ext)s")

            YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                Log.d(TAG, "Progress: ${progress.toInt()}%, eta=${eta}s, line=$line")
                onProgress(progress)
            }

            val downloadedFiles = tempDir.listFiles()
            if (downloadedFiles.isNullOrEmpty()) {
                throw Exception("No files downloaded")
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
                Log.i(TAG, "Download finished and file moved to destination")
                Result.success(resultFile) // Note: returning temp file path might be confusing but it works for now
            } else {
                throw Exception("Failed to copy file to destination")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: url=$url", e)
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
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
            Log.e(TAG, "Error copying to file: ${destination.absolutePath}", e)
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
            Log.e(TAG, "Error copying to DocumentFile: ${destDir.uri}", e)
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
                Log.w(TAG, "Failed to resolve URI to path: $uri", e)
            }
        }
        return null
    }
}
