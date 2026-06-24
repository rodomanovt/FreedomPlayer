package com.rodomanovt.freedomplayer.repos

import android.content.Context
import com.rodomanovt.freedomplayer.helpers.DownloaderStorageHelper
import com.rodomanovt.freedomplayer.interfaces.DownloaderPlaylistDao
import com.rodomanovt.freedomplayer.model.AppDatabase
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import com.rodomanovt.freedomplayer.model.DownloaderPlaylistEntity
import com.rodomanovt.freedomplayer.model.RemoteSong
import com.rodomanovt.freedomplayer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloaderRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao: DownloaderPlaylistDao = AppDatabase.getInstance(appContext).downloaderPlaylistDao()
    private val storageHelper = DownloaderStorageHelper(appContext)
    private val musicRepository = DownloaderMusicRepository(appContext, storageHelper)

    suspend fun getAllPlaylists(): List<DownloaderPlaylist> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toDomain() }
    }

    suspend fun addPlaylist(name: String, url: String, autoUpdate: Boolean): DownloaderPlaylist =
        withContext(Dispatchers.IO) {
            storageHelper.createPlaylistFolder(name).getOrThrow()

            val id = dao.insert(
                DownloaderPlaylistEntity(
                    name = name.trim(),
                    url = url.trim(),
                    autoUpdate = autoUpdate
                )
            )
            DownloaderPlaylist(
                id = id,
                name = name.trim(),
                url = url.trim(),
                autoUpdate = autoUpdate
            )
        }

    suspend fun updatePlaylist(playlist: DownloaderPlaylist) = withContext(Dispatchers.IO) {
        val existing = dao.getById(playlist.id)
            ?: throw DownloaderStorageHelper.PlaylistFolderException(
                appContext.getString(R.string.playlist_not_found)
            )

        val newName = playlist.name.trim()
        if (existing.name.trim() != newName) {
            storageHelper.renamePlaylistFolder(existing.name, newName).getOrThrow()
        }

        dao.update(
            DownloaderPlaylistEntity(
                id = playlist.id,
                name = newName,
                url = playlist.url.trim(),
                autoUpdate = playlist.autoUpdate
            )
        )
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun getSongsToDownload(playlist: DownloaderPlaylist): List<RemoteSong> =
        musicRepository.getSongsToDownload(playlist)
}

private fun DownloaderPlaylistEntity.toDomain(): DownloaderPlaylist = DownloaderPlaylist(
    id = id,
    name = name,
    url = url,
    autoUpdate = autoUpdate
)
