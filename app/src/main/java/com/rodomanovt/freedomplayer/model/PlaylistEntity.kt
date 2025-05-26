package com.rodomanovt.freedomplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val folderUri: String,
    val name: String,
    val tracksCount: Int
)