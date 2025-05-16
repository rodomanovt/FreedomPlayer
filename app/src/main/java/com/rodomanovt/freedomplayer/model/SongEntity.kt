package com.rodomanovt.freedomplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val playlistPath: String,
    val songPath: String,
    //val albumArt: ByteArray? = null // Можно хранить URI или путь к обложке
)