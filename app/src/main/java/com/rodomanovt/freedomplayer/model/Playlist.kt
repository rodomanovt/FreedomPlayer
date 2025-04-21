package com.rodomanovt.freedomplayer.model

import android.net.Uri

data class Playlist(
    val name: String,
    val songs: List<Song>,
    val folderUri: Uri,
){
    val tracksCount: Int
        get() = songs.size
}