package com.rodomanovt.freedomplayer.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rodomanovt.freedomplayer.model.Playlist
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.repos.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicViewModel(val app: Application) : AndroidViewModel(app) {
    private val repository = MusicRepository(app)
    private val _playlists = MutableLiveData<List<Playlist>>()
    val playlists: LiveData<List<Playlist>> = _playlists
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    fun loadSongs(folderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val folder = DocumentFile.fromTreeUri(app, folderUri)
                if (folder != null && folder.exists()) {
                    val songsList = repository.getSongsFromFolder(folder)
                    _songs.postValue(songsList)
                } else {
                    _songs.postValue(emptyList())
                    Log.e("MusicViewModel", "Folder not accessible")
                }
            } catch (e: Exception) {
                _songs.postValue(emptyList())
                Log.e("MusicViewModel", "Error loading songs", e)
            }
        }
    }

    // TODO: записывать плейлисты в бд

    fun loadPlaylists(rootFolderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = repository.getPlaylists(rootFolderUri)
            _playlists.postValue(playlists)
        }
    }
}
