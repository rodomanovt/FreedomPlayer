package com.rodomanovt.freedomplayer.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val album: String? = null
)
