package com.rodomanovt.freedomplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloader_playlists")
data class DownloaderPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val autoUpdate: Boolean,
    val lastDownloadTimestamp: Long? = null
)
