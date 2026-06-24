package com.rodomanovt.freedomplayer.repos

import android.content.Context
import com.rodomanovt.freedomplayer.interfaces.DownloaderPlaylistDao
import com.rodomanovt.freedomplayer.model.AppDatabase
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import com.rodomanovt.freedomplayer.model.DownloaderPlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloaderRepository(context: Context) {
    private val dao: DownloaderPlaylistDao = AppDatabase.getInstance(context).downloaderPlaylistDao()

    suspend fun getAllPlaylists(): List<DownloaderPlaylist> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toDomain() }
    }

    suspend fun addPlaylist(name: String, url: String, autoUpdate: Boolean): DownloaderPlaylist =
        withContext(Dispatchers.IO) {
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
        dao.update(
            DownloaderPlaylistEntity(
                id = playlist.id,
                name = playlist.name.trim(),
                url = playlist.url.trim(),
                autoUpdate = playlist.autoUpdate
            )
        )
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }
}

private fun DownloaderPlaylistEntity.toDomain(): DownloaderPlaylist = DownloaderPlaylist(
    id = id,
    name = name,
    url = url,
    autoUpdate = autoUpdate
)
