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

    fun loadExistingAndCheckForNewPlaylists(rootFolderUri: Uri) {
        viewModelScope.launch {
            try {
                val allPlaylists = repository.getAllPlaylists(rootFolderUri)
                _playlists.postValue(allPlaylists)
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Ошибка загрузки плейлистов", e)
                _playlists.postValue(emptyList())
            }
        }
    }

    fun loadSongs(folderUri: Uri) {
        viewModelScope.launch {
            try {
                val folder = DocumentFile.fromTreeUri(app, folderUri)
                if (folder != null && folder.exists()) {
                    val songs = repository.getSongsFromFolder(folder)
                    _songs.postValue(songs)
                } else {
                    Log.e("MusicViewModel", "Папка не существует или недоступна")
                    _songs.postValue(emptyList())
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Ошибка при загрузке треков", e)
                _songs.postValue(emptyList())
            }
        }
    }
}