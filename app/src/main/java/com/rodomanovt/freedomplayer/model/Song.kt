package com.rodomanovt.freedomplayer.model

import android.graphics.Bitmap
import java.io.Serializable

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val album: String? = null,
    val albumArt: Serializable? = null
)
