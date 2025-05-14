package com.rodomanovt.freedomplayer.repos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.model.AppDatabase
import com.rodomanovt.freedomplayer.model.Playlist
import com.rodomanovt.freedomplayer.model.PlaylistEntity
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable


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
                    playlistDao.insert(
                        PlaylistEntity(
                        folderUri = folderUriStr,
                        name = folder.name!!,
                        tracksCount = songs.size
                    )
                    )
                    songDao.insertAll(songs.map {
                        SongEntity(
                            id = it.id,
                            title = it.title,
                            artist = it.artist,
                            duration = it.duration,
                            songPath = it.songPath,
                            playlistPath = it.playlistPath
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
        Log.d("MusicRepository", "Начинаем сканирование папки: ${folder.name}")


        folder.listFiles().forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                try {
                    val pfd = context.contentResolver.openFileDescriptor(file.uri, "r")
                    pfd?.use { fd ->
                        retriever.setDataSource(fd.fileDescriptor)

                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: file.name?.substringBeforeLast(".") ?: "Unknown"
                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: "Unknown"
                        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

                        val artBytes = retriever.embeddedPicture
                        val albumArt: Any = if (artBytes != null) {
                            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                        } else {
                            R.drawable.baseline_music_note_24
                        }

                        songs.add(
                            Song(
                                id = file.uri.hashCode().toLong(),
                                title = title,
                                artist = artist,
                                duration = duration,
                                playlistPath = folder.uri.toString(),
                                songPath = file.uri.toString(),
                                albumArt = albumArt
                            )
                        )
                        Log.d("MusicRepository", "Added $artist - $title")
                    }
                } catch (e: Exception) {
                    Log.e("MusicRepository", "Error reading metadata for ${file.name}", e)
                }
            }
        }
        Log.d("MusicRepository", "Загружено треков: ${songs.size}")
        retriever.release()
        return songs
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        return file.type == "audio/mpeg" || file.name?.endsWith(".mp3", ignoreCase = true) == true
    }

    suspend fun getSongsFromDb(folderUri: String): List<Song> {
        val songs = songDao.getSongsByFolder(folderUri)
        Log.d("MusicRepository", "Загружено из БД песен для ${folderUri}: ${songs.size}")
        return songs.map { entity ->
            Song(
                id = entity.id,
                title = entity.title,
                artist = entity.artist,
                duration = entity.duration,
                playlistPath = entity.playlistPath,
                songPath = entity.songPath,
                albumArt = if (entity.albumArt != null) {
                    BitmapFactory.decodeByteArray(entity.albumArt, 0, entity.albumArt.size)
                } else {
                    R.drawable.baseline_music_note_24
                }
            )
        }
    }

    suspend fun scanAndSaveSongs(folder: DocumentFile): List<Song> {
        val songs = mutableListOf<Song>()
        val retriever = MediaMetadataRetriever()

        folder.listFiles().forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                try {
                    val pfd = context.contentResolver.openFileDescriptor(file.uri, "r")
                    pfd?.use { fd ->
                        retriever.setDataSource(fd.fileDescriptor)

                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: file.name?.substringBeforeLast(".") ?: "Unknown"
                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown"
                        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

                        val song = Song(
                            id = file.uri.hashCode().toLong(),
                            title = title,
                            artist = artist,
                            duration = duration,
                            playlistPath = folder.uri.toString(),
                            songPath = file.uri.toString(),
                            albumArt = retriever.embeddedPicture ?: R.drawable.baseline_music_note_24
                        )
                        Log.d("MusicRepository", "Scanned and added $artist - $title" )
                        songs.add(song)
                    }
                } catch (e: Exception) {
                    Log.e("MusicRepository", "Error reading metadata for ${file.name}", e)
                }
            }
        }

        retriever.release()

        // Сохраняем в БД
        songDao.insertAll(songs.map {
            SongEntity(
                id = it.id,
                title = it.title,
                artist = it.artist,
                duration = it.duration,
                playlistPath = it.playlistPath,
                songPath = it.songPath,
                albumArt = null // Можно сохранять URI или Bitmap как Uri.parse(it.path).toString()
            )
        })

        return songs
    }


//    private fun loadAlbumArt(albumArtBytes: ByteArray?): Any {
//        return if (albumArtBytes != null) {
//            val bitmap = BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.size)
//            bitmap
//        } else {
//            R.drawable.baseline_music_note_24
//        }
//    }
}