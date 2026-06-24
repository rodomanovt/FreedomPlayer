package com.rodomanovt.freedomplayer.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rodomanovt.freedomplayer.model.DownloaderPlaylistEntity

@Dao
interface DownloaderPlaylistDao {
    @Insert
    suspend fun insert(playlist: DownloaderPlaylistEntity): Long

    @Update
    suspend fun update(playlist: DownloaderPlaylistEntity)

    @Query("DELETE FROM downloader_playlists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM downloader_playlists WHERE id = :id")
    suspend fun getById(id: Long): DownloaderPlaylistEntity?

    @Query("SELECT * FROM downloader_playlists ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<DownloaderPlaylistEntity>
}
