package com.rodomanovt.freedomplayer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.rodomanovt.freedomplayer.helpers.FavoritesHelper

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val favoritesHelper = FavoritesHelper(application)
    private val _likedSongPaths = MutableLiveData(favoritesHelper.getLikedSongPaths())
    val likedSongPaths: LiveData<Set<String>> = _likedSongPaths

    fun isLiked(songPath: String): Boolean =
        _likedSongPaths.value?.contains(songPath) == true

    fun toggleLike(songPath: String) {
        val updated = _likedSongPaths.value?.toMutableSet() ?: mutableSetOf()
        if (!updated.add(songPath)) {
            updated.remove(songPath)
        }
        favoritesHelper.saveLikedSongPaths(updated)
        _likedSongPaths.value = updated
    }
}
