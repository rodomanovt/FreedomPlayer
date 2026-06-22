package com.rodomanovt.freedomplayer.helpers

import android.content.Context

class FavoritesHelper(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLikedSongPaths(): Set<String> =
        prefs.getStringSet(KEY_LIKED_SONGS, emptySet())?.toSet() ?: emptySet()

    fun saveLikedSongPaths(paths: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_LIKED_SONGS, HashSet(paths))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "MusicPlayerPrefs"
        private const val KEY_LIKED_SONGS = "LIKED_SONG_PATHS"
    }
}
