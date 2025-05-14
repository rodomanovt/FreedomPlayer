package com.rodomanovt.freedomplayer.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val playlistPath: String,
    val songPath: String,
    val album: String? = null,
    val albumArt: Any
)
