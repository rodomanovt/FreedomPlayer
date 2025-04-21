package com.rodomanovt.freedomplayer.repos

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.rodomanovt.freedomplayer.model.Playlist
import com.rodomanovt.freedomplayer.model.Song

class MusicRepository(private val context: Context) {
    private val contentResolver = context.contentResolver

    // Получение всех плейлистов (папок с музыкой)
    fun getPlaylists(rootFolderUri: Uri): List<Playlist> {
        val playlists = mutableListOf<Playlist>()

        Log.d("MusicRepository", "Loading playlists from: $rootFolderUri")


        // Запрос к корневой папке (используем DocumentFile для работы с URI)
        val rootFolder = DocumentFile.fromTreeUri(context, rootFolderUri)
        Log.d("MusicRepository", "Root folder exists: ${rootFolder?.exists()}")
        rootFolder?.listFiles()?.forEach { folder ->
            if (folder.isDirectory && folder.name?.get(0) != '.') {
                val songs = getSongsFromFolder(folder)

//                folder.listFiles().forEach { file ->
//                    Log.d("FileDebug", """
//                    Имя: ${file.name}
//                    Тип: ${file.type}
//                    Доступен: ${file.canRead()}
//                    URI: ${file.uri}
//                """.trimIndent())
//                }

                Log.d("MusicRepository", "Found folder: ${folder.name} with ${songs.size} songs")

                if (true
                    //&& songs.isNotEmpty()
                    ) {
                    playlists.add(Playlist(folder.name ?: "Unknown", songs, folder.uri))
                }
            }
        }

        Log.d("MusicRepository", "Total playlists found: ${playlists.size}")
        return playlists
    }


    private fun getSongsFromFolder(folder: DocumentFile): List<Song> {
        val songs = mutableListOf<Song>()
        val retriever = MediaMetadataRetriever()

        folder.listFiles().forEach { file ->
            if (file.isFile && (file.type == "audio/mpeg" || file.name?.endsWith(".mp3", ignoreCase = true) == true)) {
                try {
                    // Открываем файл через FileDescriptor
                    val pfd = context.contentResolver.openFileDescriptor(file.uri, "r")
                    pfd?.use { parcelFd ->
                        retriever.setDataSource(parcelFd.fileDescriptor)

                        // Извлекаем метаданные
                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: file.name?.substringBeforeLast(".")
                            ?: "Unknown"

                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                            ?: "Unknown"

                        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull()
                            ?: 0L

                        songs.add(
                            Song(
                                id = file.uri.hashCode().toLong(),
                                title = title,
                                artist = artist,
                                duration = duration,
                                path = file.uri.toString()
                            )
                        )

                        Log.d("MusicRepository", "Added song: $title - $artist ($duration ms)")
                    }
                } catch (e: Exception) {
                    Log.e("MusicRepository", "Error reading metadata for ${file.name}", e)
                }
            }
        }

        retriever.release()
        return songs
    }


}