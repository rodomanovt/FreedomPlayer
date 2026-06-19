package com.rodomanovt.freedomplayer.repos

import android.content.Context
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
        playlistDao.getAll().map { it.toDomain() }
    }

    suspend fun syncPlaylistsFromRoot(rootFolderUri: Uri): List<Playlist> = withContext(Dispatchers.IO) {
        val rootFolder = DocumentFile.fromTreeUri(context, rootFolderUri)
        if (rootFolder == null || !rootFolder.exists()) return@withContext emptyList()

        val cachedPlaylists = playlistDao.getAll().associateBy { it.folderUri }
        val discoveredPlaylists = mutableListOf<Playlist>()

        rootFolder.listFiles().forEach { folder ->
            if (folder.isDirectory && folder.name?.startsWith('.') != true) {
                val cachedPlaylist = cachedPlaylists[folder.uri.toString()]
                val playlist = cachedPlaylist?.toDomain()
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
                songs.add(song)
                songDao.insertAll(listOf(song.toEntity()))
                onProgress(songs.toList())
                Log.d("MusicRepository", "Indexed file ${song.songPath}")
            }
            else {
                Log.d(
                    "MusicRepository",
                    "Skipped file name=${file.name}, isFile=${file.isFile}, type=${file.type}"
                )
            }
        }

        persistPlaylistMetadata(
            Playlist(
                name = folder.name ?: "Unnamed",
                tracksCount = songs.size,
                folderUri = folder.uri,
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
                val playlist = Playlist(
                    name = folder.name ?: "Unnamed",
                    tracksCount = indexedSongs.size,
                    folderUri = folder.uri,
                    songs = emptyList()
                )

                reindexedPlaylists.add(playlist)
                Log.d("MusicRepository", "Reindexed playlist ${playlist.name}: ${playlist.tracksCount} tracks")
            }

        reindexedPlaylists
    }

    private suspend fun persistPlaylistMetadata(playlist: Playlist) {
        playlistDao.insert(playlist.toEntity())
    }

    private fun scanSongs(folder: DocumentFile): List<Song> {
        val songs = mutableListOf<Song>()

        Log.d("MusicRepository", "Начинаем быстрое сканирование папки: ${folder.name}")

        folder.listFiles().forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                val song = readSong(file, folder)
                songs.add(song)
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
            songPath = file.uri.toString()
        )
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
}

private fun PlaylistEntity.toDomain(): Playlist = Playlist(
    name = name,
    tracksCount = tracksCount,
    folderUri = Uri.parse(folderUri),
    songs = emptyList()
)

private fun SongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    duration = duration,
    playlistPath = playlistPath,
    songPath = songPath
)

private fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    folderUri = folderUri.toString(),
    name = name,
    tracksCount = tracksCount
)

private fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    duration = duration,
    playlistPath = playlistPath,
    songPath = songPath
)
