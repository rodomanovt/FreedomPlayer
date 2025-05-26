package com.rodomanovt.freedomplayer.model

import android.net.Uri

data class Playlist(
    val name: String,
    val tracksCount: Int,
    val folderUri: Uri,
    val songs: List<Song> = emptyList()
)