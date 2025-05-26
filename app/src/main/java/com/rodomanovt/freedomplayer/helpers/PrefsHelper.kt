package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.net.Uri
import android.util.Log

class PrefsHelper(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "MusicPlayerPrefs"
        private const val KEY_URI = "ROOT_FOLDER_URI"
    }

    fun saveRootFolderUri(uri: Uri) {
        val uriString = uri.toString()
        Log.d("PrefsHelper", "Saving URI: $uriString")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URI, uriString)
            .apply()
    }

    fun getRootFolderUri(): Uri? {
        val uriString = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URI, null)

        Log.d("PrefsHelper", "Loaded URI: $uriString")
        return uriString?.let { Uri.parse(it) }
    }
}