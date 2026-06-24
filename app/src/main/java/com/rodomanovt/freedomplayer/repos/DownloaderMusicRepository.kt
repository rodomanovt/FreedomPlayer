package com.rodomanovt.freedomplayer.repos

import android.content.Context
import android.net.Uri
import android.util.Log
import com.rodomanovt.freedomplayer.helpers.DownloaderStorageHelper
import com.rodomanovt.freedomplayer.helpers.YtDlpManager
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import com.rodomanovt.freedomplayer.model.RemoteSong
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
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
            if (response.err.isNotBlank()) {
                Log.d(TAG, "yt-dlp stderr: ${response.err.take(1000)}")
            }
            parseRemoteSongs(response.out).also { songs ->
                if (songs.isEmpty()) {
                    Log.w(TAG, "No songs parsed from playlist. Raw stdout: ${response.out.take(2000)}")
                } else {
                    Log.i(TAG, "Parsed ${songs.size} remote songs from playlist")
                }
            }
        }

    suspend fun getLocalSongUrisFromPlaylist(playlist: DownloaderPlaylist): List<Uri> =
        withContext(Dispatchers.IO) {
            storageHelper.listMp3Uris(playlist.name)
        }

    suspend fun getAllDownloadedUrls(paths: List<Uri>): Set<String> = withContext(Dispatchers.IO) {
        val result = mutableSetOf<String>()
        for (uri in paths) {
            extractPurlFromMp3(uri)?.let { result.add(normalizeUrl(it)) }
        }
        result
    }

    fun isDownloaded(targetUrl: String, downloadedUrls: Set<String>): Boolean {
        return normalizeUrl(targetUrl) in downloadedUrls
    }

    suspend fun getSongsToDownload(playlist: DownloaderPlaylist): List<RemoteSong> =
        withContext(Dispatchers.IO) {
            val allRemoteSongs = getRemoteSongsFromPlaylist(playlist)
            val allLocalSongPaths = getLocalSongUrisFromPlaylist(playlist)
            val allDownloadedUrls = getAllDownloadedUrls(allLocalSongPaths)

            Log.i(
                TAG,
                "Playlist '${playlist.name}': remote=${allRemoteSongs.size}, " +
                    "local mp3=${allLocalSongPaths.size}, with purl=${allDownloadedUrls.size}"
            )

            allRemoteSongs.filter { song ->
                !isDownloaded(song.url, allDownloadedUrls)
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

    private fun extractPurlFromMp3(uri: Uri): String? {
        val tempFile = File.createTempFile("mp3meta", ".mp3", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tag ?: return null
            for (field in tag.fields) {
                if (field is AbstractID3v2Frame) {
                    val body = field.body
                    if (body is FrameBodyTXXX && body.description == "purl" && body.text.isNotBlank()) {
                        return body.text
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read purl from $uri", e)
            null
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
