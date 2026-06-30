package com.rodomanovt.freedomplayer.repos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.helpers.DownloaderStorageHelper
import com.rodomanovt.freedomplayer.helpers.YtDlpDownloadHelper
import com.rodomanovt.freedomplayer.helpers.YtDlpManager
import com.rodomanovt.freedomplayer.model.AppDatabase
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import com.rodomanovt.freedomplayer.model.DownloaderPlaylistEntity
import com.rodomanovt.freedomplayer.model.RemoteSong
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DownloaderMusicRepository(
    private val context: Context,
    private val storageHelper: DownloaderStorageHelper
) {

    suspend fun getRemoteSongsFromPlaylist(playlist: DownloaderPlaylist): List<RemoteSong> =
        withContext(Dispatchers.IO) {
            val request = YoutubeDLRequest(playlist.url.trim())
            request.addOption("--flat-playlist")
            request.addOption("--skip-download")
            request.addOption("--ignore-errors")
            request.addOption("--no-warnings")
            request.addOption("--dump-single-json")
            YtDlpManager.configureRequest(request)

            val response = YoutubeDL.getInstance().execute(request)
            Log.d(TAG, "yt-dlp stdout length=${response.out.length}, stderr length=${response.err.length}")
            parseRemoteSongs(response.out).also { songs ->
                if (songs.isEmpty()) {
                    Log.w(TAG, "No songs parsed from playlist.")
                } else {
                    Log.i(TAG, "Parsed ${songs.size} remote songs from playlist")
                }
            }
        }

    suspend fun getLocalSongUrisFromPlaylist(playlist: DownloaderPlaylist): List<Uri> =
        withContext(Dispatchers.IO) {
            storageHelper.listMp3Uris(playlist.name)
        }

    data class ExtractedMetadata(val url: String? = null, val id: String? = null)

    suspend fun getAllDownloadedMetadata(paths: List<Uri>): List<ExtractedMetadata> = withContext(Dispatchers.IO) {
        paths.map { uri -> extractMetadataFromMp3(uri) }
    }

    suspend fun getSongsToDownload(playlist: DownloaderPlaylist): List<RemoteSong> =
        withContext(Dispatchers.IO) {
            val allRemoteSongs = getRemoteSongsFromPlaylist(playlist)
            val allLocalSongUris = getLocalSongUrisFromPlaylist(playlist)
            
            val localMetadatas = getAllDownloadedMetadata(allLocalSongUris)
            val localDownloadedUrls = localMetadatas.mapNotNull { it.url?.let { u -> normalizeUrl(u) } }.toSet()
            val localDownloadedIds = localMetadatas.mapNotNull { it.id?.lowercase() }.toMutableSet()
            
            localMetadatas.forEach { meta ->
                meta.url?.let { extractVideoId(it) }?.let { localDownloadedIds.add(it.lowercase()) }
            }

            val localFilenames = allLocalSongUris.mapNotNull { uri ->
                DocumentFile.fromSingleUri(context, uri)?.name?.lowercase()
            }
            localFilenames.forEach { name ->
                extractVideoIdFromFilename(name)?.let { localDownloadedIds.add(it.lowercase()) }
            }

            Log.i(
                TAG,
                "Playlist '${playlist.name}': remote=${allRemoteSongs.size}, local files=${allLocalSongUris.size}, known unique IDs=${localDownloadedIds.size}"
            )

            allRemoteSongs.filter { song ->
                val videoId = extractVideoId(song.url)?.lowercase()
                val normalizedUrl = normalizeUrl(song.url)

                if (localDownloadedUrls.contains(normalizedUrl)) {
                    Log.d(TAG, "Skipping '${song.name}': found by URL metadata")
                    return@filter false
                }

                if (videoId != null && localDownloadedIds.contains(videoId)) {
                    Log.d(TAG, "Skipping '${song.name}': found by Video ID ($videoId)")
                    return@filter false
                }

                Log.d(TAG, "Adding '${song.name}' to download queue (ID: $videoId)")
                true
            }
        }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            "v=([a-zA-Z0-9_-]{11})",
            "be/([a-zA-Z0-9_-]{11})",
            "embed/([a-zA-Z0-9_-]{11})",
            "shorts/([a-zA-Z0-9_-]{11})",
            "watch\\?v=([a-zA-Z0-9_-]{11})"
        )
        for (p in patterns) {
            val match = Regex(p).find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractVideoIdFromFilename(filename: String): String? {
        // Look for 11-char YouTube ID pattern. Usually it's in brackets [mjE36DCLfag]
        val match = Regex("([a-zA-Z0-9_-]{11})").find(filename)
        return match?.groupValues?.get(1)
    }

    suspend fun downloadSongs(
        playlist: DownloaderPlaylist,
        songs: List<RemoteSong>
    ) = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "downloader_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.downloaderText),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = playlist.id.toInt().let { if (it == 0) playlist.hashCode() else it }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(context.getString(R.string.download_track_title) + ": " + playlist.name)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        val playlistFolder = storageHelper.getPlaylistDocumentFile(playlist.name)

        var downloaded = 0
        val total = songs.size

        try {
            songs.asReversed().forEach { song ->
                Log.i(TAG, "Starting download: ${song.name} from ${song.url}")
                val progressText = context.getString(R.string.playlist_songs_to_download, total) +
                        " (Загружено: $downloaded / $total)"

                builder.setContentText("${song.name}\n$progressText")
                    .setProgress(total, downloaded, false)
                notificationManager.notify(notificationId, builder.build())

                val result = YtDlpDownloadHelper.downloadTrack(
                    context, 
                    song.url, 
                    playlistFolder, 
                    tag = "playlist_${playlist.id}"
                )
                if (result.isSuccess) {
                    downloaded++
                    Log.i(TAG, "Successfully downloaded: ${song.name}")
                } else {
                    Log.e(TAG, "Failed to download ${song.name}: ${result.exceptionOrNull()?.message}")
                }
            }
            
            builder.setContentTitle(context.getString(R.string.download_success))
                .setContentText(context.getString(R.string.download_success_details, downloaded, total, playlist.name))
                .setProgress(0, 0, false)
                .setOngoing(false)
            notificationManager.notify(notificationId, builder.build())

        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.i(TAG, "Download job for playlist ${playlist.name} cancelled")
            notificationManager.cancel(notificationId)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during playlist download: ${e.message}")
            builder.setContentTitle(context.getString(R.string.download_failed, e.message))
                .setOngoing(false)
                .setProgress(0, 0, false)
            notificationManager.notify(notificationId, builder.build())
        } finally {
            if (downloaded > 0) {
                playlistFolder?.let { folder ->
                    Log.i(TAG, "Triggering re-indexing for playlist: ${playlist.name}")
                    MusicRepository(context).scanAndSaveSongs(folder) { }
                }
            }
            updateLastDownloadTimestamp(playlist, playlistFolder?.uri)
        }

        Log.i(TAG, "Finished downloading playlist ${playlist.name}: $downloaded/$total success")
    }

    suspend fun updatePlaylistTimestamp(playlist: DownloaderPlaylist) = withContext(Dispatchers.IO) {
        val folderUri = storageHelper.getPlaylistDocumentFile(playlist.name)?.uri
        updateLastDownloadTimestamp(playlist, folderUri)
    }

    private suspend fun updateLastDownloadTimestamp(playlist: DownloaderPlaylist, folderUri: Uri?) {
        val now = System.currentTimeMillis()
        val db = AppDatabase.getInstance(context)

        Log.i(TAG, "Updating last download timestamp for playlist: ${playlist.name} to $now")

        db.downloaderPlaylistDao().getById(playlist.id)?.let { entity: DownloaderPlaylistEntity ->
            db.downloaderPlaylistDao().update(entity.copy(lastDownloadTimestamp = now))
        }

        folderUri?.toString()?.let { uriString ->
            db.playlistDao().getPlaylistByFolderUri(uriString)?.let { entity ->
                db.playlistDao().insert(entity.copy(lastDownloadTimestamp = now))
            }
        }
    }

    private fun parseRemoteSongs(jsonOutput: String): List<RemoteSong> {
        val trimmed = jsonOutput.trim()
        if (trimmed.isEmpty()) return emptyList()

        val remoteSongs = mutableListOf<RemoteSong>()

        if (trimmed.startsWith("[")) {
            parseEntriesArray(JSONArray(trimmed), remoteSongs)
        } else if (trimmed.startsWith("{")) {
            val lines = trimmed.lines().filter { it.isNotBlank() }
            if (lines.size > 1) {
                for (line in lines) {
                    val obj = runCatching { JSONObject(line) }.getOrNull() ?: continue
                    val entries = obj.optJSONArray("entries")
                    if (entries != null && entries.length() > 0) {
                        parseEntriesArray(entries, remoteSongs)
                    } else {
                        parseEntry(obj)?.let { remoteSongs.add(it) }
                    }
                }
                return remoteSongs.distinctBy { it.url }
            } else {
                val root = JSONObject(trimmed)
                val entries = root.optJSONArray("entries")
                if (entries != null) {
                    parseEntriesArray(entries, remoteSongs)
                } else {
                    parseEntry(root)?.let { remoteSongs.add(it) }
                }
            }
        }

        return remoteSongs
    }

    private fun parseEntriesArray(entries: JSONArray, output: MutableList<RemoteSong>) {
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            parseEntry(entry)?.let { output.add(it) }
        }
    }

    private fun parseEntry(entry: JSONObject): RemoteSong? {
        if (!isVideoEntry(entry)) return null

        val channel = entry.optString("channel").takeIf { it.isNotBlank() }
            ?: entry.optString("uploader").takeIf { it.isNotBlank() }
            ?: entry.optString("artist").takeIf { it.isNotBlank() }
            ?: "Unknown artist"
        val name = entry.optString("title").takeIf { it.isNotBlank() } ?: "Unknown Title"
        val url = resolveEntryUrl(entry) ?: return null

        return RemoteSong(channel = channel, name = name, url = url)
    }

    private fun isVideoEntry(entry: JSONObject): Boolean {
        when (entry.optString("_type")) {
            "playlist" -> return false
            "url", "video" -> return true
        }

        entry.optJSONArray("entries")?.takeIf { it.length() > 0 }?.let { return false }

        val id = entry.optString("id")
        if (id.isNotBlank() && isPlaylistId(id)) return false

        val webpageUrl = entry.optString("webpage_url")
        if (webpageUrl.contains("playlist?") || webpageUrl.contains("/playlist")) {
            return false
        }

        return id.isNotBlank() || entry.optString("url").isNotBlank()
    }

    private fun isPlaylistId(id: String): Boolean {
        return id.startsWith("PL") ||
            id.startsWith("LL") ||
            id.startsWith("RD") ||
            id.startsWith("VL") ||
            id.startsWith("OLAK5uy_")
    }

    private fun resolveEntryUrl(entry: JSONObject): String? {
        val id = entry.optString("id").takeIf { it.isNotBlank() }
        val rawUrl = entry.optString("url").takeIf { it.isNotBlank() }
            ?: entry.optString("webpage_url").takeIf { it.isNotBlank() }

        return when {
            rawUrl != null && rawUrl.startsWith("http") -> rawUrl
            id != null -> "https://www.youtube.com/watch?v=$id"
            rawUrl != null -> "https://www.youtube.com/watch?v=$rawUrl"
            else -> null
        }
    }

    private fun extractMetadataFromMp3(uri: Uri): ExtractedMetadata {
        val tempFile = File.createTempFile("mp3meta", ".mp3", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.copyTo(tempFile.outputStream())
            } ?: return ExtractedMetadata()

            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tag ?: return ExtractedMetadata()
            
            var url: String? = null
            var id: String? = null

            for (field in tag.fields) {
                if (field is AbstractID3v2Frame) {
                    val body = field.body
                    if (body is FrameBodyTXXX && body.description == "purl" && body.text.isNotBlank()) {
                        url = body.text
                    }
                }
            }
            
            val comment = tag.getFirst(org.jaudiotagger.tag.FieldKey.COMMENT)
            if (!comment.isNullOrBlank()) {
                val commentStr = comment.toString()
                if (commentStr.startsWith("http")) {
                    url = commentStr
                } else if (commentStr.length == 11 && !commentStr.contains(" ")) {
                    id = commentStr
                }
            }

            ExtractedMetadata(url = url, id = id)
        } catch (e: Exception) {
            Log.w(TAG, "Metadata extraction failed for $uri: ${e.message}")
            ExtractedMetadata()
        } finally {
            tempFile.delete()
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().replace("www.", "").replace("music.", "")
    }

    companion object {
        private const val TAG = "DownloaderMusicRepository"
    }
}
