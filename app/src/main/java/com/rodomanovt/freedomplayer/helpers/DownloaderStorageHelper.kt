package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.rodomanovt.freedomplayer.R

class DownloaderStorageHelper(private val context: Context) {

    private val prefsHelper = PrefsHelper(context)

    fun createPlaylistFolder(playlistName: String): Result<Uri> {
        val root = getWritableRoot().getOrElse { return Result.failure(it) }

        val folderName = sanitizeFolderName(playlistName)
        if (folderName.isBlank()) {
            return Result.failure(
                PlaylistFolderException(context.getString(R.string.playlist_folder_invalid_name))
            )
        }

        root.findFile(folderName)?.takeIf { it.isDirectory }?.let { existing ->
            Log.i(TAG, "Playlist folder already exists: $folderName")
            return Result.success(existing.uri)
        }

        val created = root.createDirectory(folderName)
            ?: return Result.failure(
                PlaylistFolderException(
                    context.getString(R.string.playlist_folder_create_failed, folderName)
                )
            )

        Log.i(TAG, "Created playlist folder: $folderName at ${created.uri}")
        return Result.success(created.uri)
    }

    fun renamePlaylistFolder(oldName: String, newName: String): Result<Uri> {
        val root = getWritableRoot().getOrElse { return Result.failure(it) }

        val oldFolderName = sanitizeFolderName(oldName)
        val newFolderName = sanitizeFolderName(newName)

        if (newFolderName.isBlank()) {
            return Result.failure(
                PlaylistFolderException(context.getString(R.string.playlist_folder_invalid_name))
            )
        }

        if (oldFolderName == newFolderName) {
            val unchanged = root.findFile(oldFolderName)?.takeIf { it.isDirectory }
            return if (unchanged != null) {
                Result.success(unchanged.uri)
            } else {
                createPlaylistFolder(newName)
            }
        }

        root.findFile(newFolderName)?.takeIf { it.isDirectory }?.let {
            return Result.failure(
                PlaylistFolderException(
                    context.getString(R.string.playlist_folder_name_exists, newFolderName)
                )
            )
        }

        val oldFolder = root.findFile(oldFolderName)?.takeIf { it.isDirectory }
            ?: return createPlaylistFolder(newName)

        if (!oldFolder.renameTo(newFolderName)) {
            return Result.failure(
                PlaylistFolderException(
                    context.getString(R.string.playlist_folder_rename_failed, newFolderName)
                )
            )
        }

        Log.i(TAG, "Renamed playlist folder: $oldFolderName -> $newFolderName")
        return Result.success(oldFolder.uri)
    }

    private fun getWritableRoot(): Result<DocumentFile> {
        val rootUri = prefsHelper.getRootFolderUri()
            ?: return Result.failure(
                PlaylistFolderException(context.getString(R.string.download_path_not_set))
            )

        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: return Result.failure(
                PlaylistFolderException(context.getString(R.string.download_path_inaccessible))
            )

        if (!root.canWrite()) {
            return Result.failure(
                PlaylistFolderException(context.getString(R.string.download_path_no_write))
            )
        }

        return Result.success(root)
    }

    fun listMp3Uris(playlistName: String): List<Uri> {
        val root = getWritableRoot().getOrNull() ?: return emptyList()
        val folderName = sanitizeFolderName(playlistName)
        val folder = root.findFile(folderName)?.takeIf { it.isDirectory } ?: return emptyList()

        return folder.listFiles()
            .filter { file ->
                file.isFile && file.name?.endsWith(".mp3", ignoreCase = true) == true
            }
            .map { it.uri }
    }

    private fun sanitizeFolderName(name: String): String {
        return name.trim().replace(INVALID_FOLDER_CHARS, "_")
    }

    class PlaylistFolderException(message: String) : Exception(message)

    companion object {
        private const val TAG = "DownloaderStorageHelper"
        private val INVALID_FOLDER_CHARS = Regex("[/\\\\:*?\"<>|]")
    }
}
