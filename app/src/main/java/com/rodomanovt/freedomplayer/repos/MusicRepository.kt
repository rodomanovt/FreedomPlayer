package com.rodomanovt.freedomplayer.repos

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.model.AppDatabase
import com.rodomanovt.freedomplayer.model.Playlist
import com.rodomanovt.freedomplayer.model.PlaylistEntity
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MusicRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val playlistDao = db.playlistDao()
    private val songDao = db.songDao()

    suspend fun getPlaylistsFromDb(): List<Playlist> = withContext(Dispatchers.IO) {
        playlistDao.getAll().map { entity ->
            Playlist(
                name = entity.name,
                tracksCount = entity.tracksCount,
                folderUri = Uri.parse(entity.folderUri),
                songs = emptyList()
            )
        }
    }

    suspend fun scanForNewPlaylists(rootFolderUri: Uri): List<Playlist> = withContext(Dispatchers.IO) {
        val existingInDb = playlistDao.getAll().map { it.folderUri }.toSet()
        val newPlaylists = mutableListOf<Playlist>()

        val rootFolder = DocumentFile.fromTreeUri(context, rootFolderUri)
        if (rootFolder == null || !rootFolder.exists()) return@withContext emptyList()

        rootFolder.listFiles().forEach { folder ->
            if (folder.isDirectory && folder.name?.startsWith('.') != true) {
                val folderUriStr = folder.uri.toString()
                if (!existingInDb.contains(folderUriStr)) {
                    val songs = getSongsFromFolder(folder)
                    val playlist = Playlist(
                        name = folder.name ?: "Unnamed",
                        tracksCount = songs.size,
                        folderUri = folder.uri,
                        songs = songs
                    )

                    // Сохраняем в БД
                    playlistDao.insert(PlaylistEntity(
                        folderUri = folderUriStr,
                        name = folder.name!!,
                        tracksCount = songs.size
                    ))
                    songDao.insertAll(songs.map {
                        SongEntity(
                            id = it.id,
                            title = it.title,
                            artist = it.artist,
                            duration = it.duration,
                            path = it.path
                        )
                    })

                    newPlaylists.add(playlist)
                }
            }
        }

        Log.d("MusicRepository", "Найдено новых плейлистов: ${newPlaylists.size}")
        return@withContext newPlaylists
    }

    suspend fun getAllPlaylists(rootFolderUri: Uri): List<Playlist> = withContext(Dispatchers.IO) {
        val fromDb = getPlaylistsFromDb()
        val newOnes = scanForNewPlaylists(rootFolderUri)

        return@withContext fromDb + newOnes
    }

    fun getSongsFromFolder(folder: DocumentFile): List<Song> {
        val songs = mutableListOf<Song>()
        val retriever = MediaMetadataRetriever()

        folder.listFiles().forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                try {
                    val pfd = context.contentResolver.openFileDescriptor(file.uri, "r")
                    pfd?.use { fd ->
                        retriever.setDataSource(fd.fileDescriptor)

                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: file.name?.substringBeforeLast(".")
                            ?: "Unknown"
                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: "Unknown"
                        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

                        val song = Song(
                            id = file.uri.hashCode().toLong(),
                            title = title,
                            artist = artist,
                            duration = duration,
                            path = file.uri.toString(),
                            albumArt = retriever.embeddedPicture ?: R.drawable.baseline_music_note_24
                        )
                        Log.d("MusicRepository", "Added $artist - $title")
                        songs.add(song)
                    }
                } catch (e: Exception) {
                    Log.e("MusicRepository", "Ошибка чтения метаданных для ${file.name}", e)
                }
            }
        }

        retriever.release()
        return songs
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        return file.type == "audio/mpeg" || file.name?.endsWith(".mp3", ignoreCase = true) == true
    }
}