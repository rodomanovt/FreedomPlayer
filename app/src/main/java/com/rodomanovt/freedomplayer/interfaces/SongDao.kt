package com.rodomanovt.freedomplayer.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rodomanovt.freedomplayer.model.SongEntity

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE playlistPath = :folderUri")
    suspend fun deleteSongsByFolder(folderUri: String)

    @Query("SELECT * FROM songs WHERE playlistPath = :folderUri")
    suspend fun getSongsByFolder(folderUri: String): List<SongEntity>

}
