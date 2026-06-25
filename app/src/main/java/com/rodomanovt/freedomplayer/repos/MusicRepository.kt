package com.rodomanovt.freedomplayer.repos

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.rodomanovt.freedomplayer.model.AppDatabase
import com.rodomanovt.freedomplayer.model.Playlist
import com.rodomanovt.freedomplayer.model.PlaylistEntity
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val playlistDao = db.playlistDao()
    private val songDao = db.songDao()

    suspend fun getPlaylistsFromDb(): List<Playlist> = withContext(Dispatchers.IO) {
        playlistDao.getAll().map { it.toDomain(loadSongs = true) }
    }

    suspend fun syncPlaylistsFromRoot(rootFolderUri: Uri): List<Playlist> = withContext(Dispatchers.IO) {
        val rootFolder = DocumentFile.fromTreeUri(context, rootFolderUri)
        if (rootFolder == null || !rootFolder.exists()) return@withContext emptyList()

        val cachedPlaylists = playlistDao.getAll().associateBy { it.folderUri }
        val discoveredPlaylists = mutableListOf<Playlist>()

        rootFolder.listFiles().forEach { folder ->
            if (folder.isDirectory && folder.name?.startsWith('.') != true) {
                val cachedPlaylist = cachedPlaylists[folder.uri.toString()]
                val playlist = cachedPlaylist?.let {
                    it.toDomain(loadSongs = it.tracksCount > 0)
                }
                    ?: Playlist(
                        name = folder.name ?: "Unnamed",
                        tracksCount = 0,
                        folderUri = folder.uri,
                        songs = emptyList()
                    )

                if (cachedPlaylist == null || cachedPlaylist.name != playlist.name || cachedPlaylist.tracksCount != playlist.tracksCount) {
                    persistPlaylistMetadata(playlist)
                }
                discoveredPlaylists.add(playlist)
            }
        }

        Log.d("MusicRepository", "Найдено папок-плейлистов: ${discoveredPlaylists.size}")
        discoveredPlaylists.sortedBy { it.name.lowercase() }
    }

    suspend fun getAllPlaylists(rootFolderUri: Uri): List<Playlist> = syncPlaylistsFromRoot(rootFolderUri)

    suspend fun getPlaylistByUri(uri: Uri): Playlist? = withContext(Dispatchers.IO) {
        playlistDao.getPlaylistByFolderUri(uri.toString())?.toDomain(loadSongs = false)
    }

    suspend fun getSongsFromFolder(folder: DocumentFile): List<Song> = withContext(Dispatchers.IO) {
        val songs = scanSongs(folder)
        Log.d("MusicRepository", "Загружено треков: ${songs.size}")
        songs
    }

    suspend fun getSongsFromDb(folderUri: String): List<Song> {
        return withContext(Dispatchers.IO) {
            val songs = songDao.getSongsByFolder(folderUri)
            Log.d("MusicRepository", "Загружено из БД песен для $folderUri: ${songs.size}")
            songs.map { it.toDomain() }
        }
    }

    suspend fun scanAndSaveSongs(
        folder: DocumentFile,
        onProgress: (List<Song>) -> Unit
    ): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        Log.d("MusicRepository", "Начинаем инкрементное сканирование папки: ${folder.name}")

        folder.listFiles().forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                val song = readSong(file, folder)
                insertSongByModifiedTime(songs, song)
                songDao.insertAll(listOf(song.toEntity()))
                onProgress(songs.toList())
                Log.d("MusicRepository", "Indexed file ${song.songPath}")
            } else {
                Log.d(
                    "MusicRepository",
                    "Skipped file name=${file.name}, isFile=${file.isFile}, type=${file.type}"
                )
            }
        }

        val existingPlaylist = playlistDao.getPlaylistByFolderUri(folder.uri.toString())
        
        persistPlaylistMetadata(
            Playlist(
                name = folder.name ?: "Unnamed",
                tracksCount = songs.size,
                folderUri = folder.uri,
                lastDownloadTimestamp = existingPlaylist?.lastDownloadTimestamp,
                songs = emptyList()
            )
        )

        songs
    }

    suspend fun enrichSongsMetadata(
        folder: DocumentFile,
        currentSongs: List<Song>,
        onProgress: (List<Song>) -> Unit
    ): List<Song> = withContext(Dispatchers.IO) {
        val songs = currentSongs.toMutableList()

        Log.d("MusicRepository", "Начинаем обогащение метаданных папки: ${folder.name}")

        folder.listFiles().forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                val song = readSongWithMetadata(file, folder)
                songDao.insertAll(listOf(song.toEntity()))
                upsertSongByPath(songs, song)
                onProgress(songs.toList())
                Log.d("MusicRepository", "Enriched file ${song.songPath}")
            }
        }

        val existingPlaylist = playlistDao.getPlaylistByFolderUri(folder.uri.toString())
        
        persistPlaylistMetadata(
            Playlist(
                name = folder.name ?: "Unnamed",
                tracksCount = songs.size,
                folderUri = folder.uri,
                lastDownloadTimestamp = existingPlaylist?.lastDownloadTimestamp,
                songs = emptyList()
            )
        )

        songs
    }

    suspend fun reindexAllPlaylists(rootFolderUri: Uri): List<Playlist> = withContext(Dispatchers.IO) {
        val rootFolder = DocumentFile.fromTreeUri(context, rootFolderUri)
            ?: return@withContext emptyList()

        if (!rootFolder.exists()) return@withContext emptyList()

        val reindexedPlaylists = mutableListOf<Playlist>()

        rootFolder.listFiles()
            .filter { it.isDirectory && it.name?.startsWith('.') != true }
            .sortedBy { it.name?.lowercase() ?: "" }
            .forEach { folder ->
                val folderPath = folder.uri.toString()
                songDao.deleteSongsByFolder(folderPath)

                val indexedSongs = scanAndSaveSongs(folder) { }
                val playlist = getPlaylistByUri(folder.uri) ?: Playlist(
                    name = folder.name ?: "Unnamed",
                    tracksCount = indexedSongs.size,
                    folderUri = folder.uri,
                    songs = indexedSongs.take(4)
                )

                reindexedPlaylists.add(playlist)
                Log.d("MusicRepository", "Reindexed playlist ${playlist.name}: ${playlist.tracksCount} tracks")
            }

        reindexedPlaylists
    }

    private suspend fun persistPlaylistMetadata(playlist: Playlist) {
        playlistDao.insert(playlist.toEntity())
    }

    private suspend fun PlaylistEntity.toDomain(loadSongs: Boolean): Playlist {
        val songs = if (loadSongs && tracksCount > 0) {
            songDao.getTopSongsByFolder(folderUri).map { it.toDomain() }
        } else {
            emptyList()
        }

        return Playlist(
            name = name,
            tracksCount = tracksCount,
            folderUri = Uri.parse(folderUri),
            lastDownloadTimestamp = lastDownloadTimestamp,
            songs = songs
        )
    }

    private fun scanSongs(folder: DocumentFile): List<Song> {
        val songs = mutableListOf<Song>()

        Log.d("MusicRepository", "Начинаем быстрое сканирование папки: ${folder.name}")

        folder.listFiles().forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                val song = readSong(file, folder)
                insertSongByModifiedTime(songs, song)
                Log.d("MusicRepository", "Added file ${song.songPath}")
            } else {
                Log.d(
                    "MusicRepository",
                    "Skipped file name=${file.name}, isFile=${file.isFile}, type=${file.type}"
                )
            }
        }

        return songs
    }

    private fun readSong(file: DocumentFile, folder: DocumentFile): Song {
        val displayName = file.name?.substringBeforeLast('.')?.ifBlank { null }
            ?: file.name
            ?: "Unknown"

        return Song(
            id = file.uri.hashCode().toLong(),
            title = displayName,
            artist = "",
            duration = 0L,
            playlistPath = folder.uri.toString(),
            songPath = file.uri.toString(),
            lastModified = file.lastModified()
        )
    }

    private fun readSongWithMetadata(file: DocumentFile, folder: DocumentFile): Song {
        val retriever = MediaMetadataRetriever()
        return try {
            val pfd = context.contentResolver.openFileDescriptor(file.uri, "r")
            pfd?.use {
                retriever.setDataSource(it.fileDescriptor)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { value -> value.isNotBlank() }
                    ?: file.name?.substringBeforeLast('.')?.ifBlank { null }
                    ?: file.name
                    ?: "Unknown"
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { value -> value.isNotBlank() }
                    ?: "Unknown"
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L

                Song(
                    id = file.uri.hashCode().toLong(),
                    title = title,
                    artist = artist,
                    duration = duration,
                    playlistPath = folder.uri.toString(),
                    songPath = file.uri.toString(),
                    lastModified = file.lastModified()
                )
            } ?: readSong(file, folder)
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error enriching metadata for ${file.name}", e)
            readSong(file, folder)
        } finally {
            retriever.release()
        }
    }

    private suspend fun saveSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        if (songs.isNotEmpty()) {
            songDao.insertAll(songs.map { it.toEntity() })
        }
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        if (!file.isFile) return false

        val mimeType = file.type?.lowercase()
        if (mimeType?.startsWith("audio/") == true) return true

        val fileName = file.name?.lowercase() ?: return false
        return fileName.endsWith(".mp3") ||
            fileName.endsWith(".m4a") ||
            fileName.endsWith(".aac") ||
            fileName.endsWith(".flac") ||
            fileName.endsWith(".wav") ||
            fileName.endsWith(".ogg") ||
            fileName.endsWith(".opus") ||
            fileName.endsWith(".wma")
    }

    private fun insertSongByModifiedTime(songs: MutableList<Song>, song: Song) {
        val comparator = compareByDescending<Song> { it.lastModified }
            .thenBy { it.title.lowercase() }
            .thenBy { it.songPath }

        var index = 0
        while (index < songs.size && comparator.compare(songs[index], song) <= 0) {
            index++
        }

        songs.add(index, song)
    }

    private fun upsertSongByPath(songs: MutableList<Song>, song: Song) {
        val existingIndex = songs.indexOfFirst { it.songPath == song.songPath }
        if (existingIndex >= 0) {
            songs[existingIndex] = song
            return
        }

        insertSongByModifiedTime(songs, song)
    }
}

private fun SongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    duration = duration,
    playlistPath = playlistPath,
    songPath = songPath,
    lastModified = lastModified
)

private fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    folderUri = folderUri.toString(),
    name = name,
    tracksCount = tracksCount,
    lastDownloadTimestamp = lastDownloadTimestamp
)

private fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    duration = duration,
    playlistPath = playlistPath,
    songPath = songPath,
    lastModified = lastModified
)
