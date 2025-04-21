package com.rodomanovt.freedomplayer.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rodomanovt.freedomplayer.model.Playlist
import com.rodomanovt.freedomplayer.repos.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val _playlists = MutableLiveData<List<Playlist>>()
    val playlists: LiveData<List<Playlist>> = _playlists

    fun loadPlaylists(rootFolderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = repository.getPlaylists(rootFolderUri)
            _playlists.postValue(playlists)
        }
    }
}
