package com.rodomanovt.freedomplayer.model

data class DownloaderPlaylist(
    val id: Long,
    val name: String,
    val url: String,
    val autoUpdate: Boolean,
    val lastDownloadTimestamp: Long? = null
)
