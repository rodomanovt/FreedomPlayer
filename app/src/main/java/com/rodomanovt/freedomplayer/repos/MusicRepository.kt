package com.rodomanovt.freedomplayer.repos

import android.content.ContentUris
import android.content.Context
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

    // Получение песен из папки
    private fun getSongsFromFolder(folder: DocumentFile): List<Song> {
        val songs = mutableListOf<Song>()
        //val folder = DocumentFile.fromTreeUri(context, folder.uri)

//        val testFolder = DocumentFile.fromTreeUri(context, Uri.parse(folder?.uri.toString()))
//        testFolder?.listFiles()?.forEach { file ->
//            Log.d("TEST", "File: ${file.name}, URI: ${file.uri}, Type: ${file.type}")
//        }

        folder.listFiles().forEach { file ->
            if (file.type == "audio/mpeg" ) {
                songs.add(
                    Song(
                        id = file.uri.hashCode().toLong(),
                        title = file.name ?: "Unknown",
                        artist = "Unknown",
                        duration = 0,
                        path = file.uri.toString()
                    )
                )
            }
        }
        return songs
    }
//
//    fun getSongsFromFolder(context: Context, folderUri: Uri): List<Song> {
//        val songs = mutableListOf<Song>()
//
//        // 1. Получаем DocumentFile для папки
//        val folder = DocumentFile.fromTreeUri(context, folderUri)
//        if (folder == null || !folder.exists()) {
//            Log.e("MusicRepository", "Folder not found or no access")
//            return emptyList()
//        }
//
//        // 2. Получаем относительный путь папки для MediaStore запроса
//        val folderPath = getFolderPathFromTreeUri(context, folderUri) ?: run {
//            Log.e("MusicRepository", "Couldn't resolve folder path")
//            return emptyList()
//        }
//
//        // 3. Запрос к MediaStore
//        val projection = arrayOf(
//            MediaStore.Audio.Media._ID,
//            MediaStore.Audio.Media.TITLE,
//            MediaStore.Audio.Media.ARTIST,
//            MediaStore.Audio.Media.DURATION,
//            MediaStore.Audio.Media.DATE_ADDED
//        )
//
//        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
//        val selectionArgs = arrayOf("$folderPath%")
//
//        context.contentResolver.query(
//            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//            projection,
//            selection,
//            selectionArgs,
//            "${MediaStore.Audio.Media.DATE_ADDED} DESC" // Сортировка по дате добавления
//        )?.use { cursor ->
//            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
//            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
//            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
//            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
//
//            while (cursor.moveToNext()) {
//                val id = cursor.getLong(idCol)
//                val uri = ContentUris.withAppendedId(
//                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                    id
//                )
//
//                songs.add(
//                    Song(
//                        id = id,
//                        title = cursor.getString(titleCol) ?: "Unknown",
//                        artist = cursor.getString(artistCol) ?: "Unknown",
//                        duration = cursor.getLong(durationCol),
//                        path = uri.toString()
//                    )
//                )
//            }
//        }
//
//        Log.d("MusicRepository", "Found ${songs.size} songs in folder")
//        return songs
//    }
//
//    // Вспомогательная функция для получения относительного пути из tree Uri
//    private fun getFolderPathFromTreeUri(context: Context, treeUri: Uri): String? {
//        if (!DocumentsContract.isTreeUri(treeUri)) return null
//
//        val docId = DocumentsContract.getTreeDocumentId(treeUri)
//        val split = docId.split(":")
//        if (split.size < 2) return null
//
//        val type = split[0]
//        val path = split[1]
//
//        return when (type) {
//            "primary" -> path
//            "home" -> path
//            else -> null
//        }?.let {
//            if (it.startsWith("/")) it else "/$it"
//        }
//    }

}