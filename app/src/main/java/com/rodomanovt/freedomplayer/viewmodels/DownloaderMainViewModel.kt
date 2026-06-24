package com.rodomanovt.freedomplayer.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rodomanovt.freedomplayer.helpers.YtDlpDownloadHelper
import com.rodomanovt.freedomplayer.helpers.YtDlpManager
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import com.rodomanovt.freedomplayer.repos.DownloaderRepository
import kotlinx.coroutines.launch

sealed class TrackDownloadState {
    data object Idle : TrackDownloadState()
    data object UpdatingYtDlp : TrackDownloadState()
    data object InProgress : TrackDownloadState()
    data object Success : TrackDownloadState()
    data class Error(val message: String) : TrackDownloadState()
}

class DownloaderMainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "DownloaderMainViewModel"
    }

    private val repository = DownloaderRepository(application)

    private val _playlists = MutableLiveData<List<DownloaderPlaylist>>(emptyList())
    val playlists: LiveData<List<DownloaderPlaylist>> = _playlists

    private val _trackDownloadState = MutableLiveData<TrackDownloadState>(TrackDownloadState.Idle)
    val trackDownloadState: LiveData<TrackDownloadState> = _trackDownloadState

    init {
        loadPlaylists()
    }

    fun addPlaylist(name: String, url: String, autoUpdate: Boolean) {
        viewModelScope.launch {
            try {
                repository.addPlaylist(name, url, autoUpdate)
                loadPlaylists()
            } catch (e: Exception) {
                Log.e("DownloaderMainViewModel", "Ошибка сохранения плейлиста", e)
            }
        }
    }

    fun updatePlaylist(id: Long, name: String, url: String, autoUpdate: Boolean) {
        viewModelScope.launch {
            try {
                repository.updatePlaylist(
                    DownloaderPlaylist(
                        id = id,
                        name = name.trim(),
                        url = url.trim(),
                        autoUpdate = autoUpdate
                    )
                )
                loadPlaylists()
            } catch (e: Exception) {
                Log.e("DownloaderMainViewModel", "Ошибка обновления плейлиста", e)
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(id)
                loadPlaylists()
            } catch (e: Exception) {
                Log.e("DownloaderMainViewModel", "Ошибка удаления плейлиста", e)
            }
        }
    }

    fun downloadTrack(url: String) {
        viewModelScope.launch {
            Log.i(TAG, "downloadTrack requested: $url")
            _trackDownloadState.value = TrackDownloadState.UpdatingYtDlp
            YtDlpManager.ensureUpdated(getApplication())
            _trackDownloadState.value = TrackDownloadState.InProgress
            val result = YtDlpDownloadHelper.downloadTrack(getApplication(), url)
            _trackDownloadState.value = if (result.isSuccess) {
                TrackDownloadState.Success
            } else {
                TrackDownloadState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }

    fun resetTrackDownloadState() {
        _trackDownloadState.value = TrackDownloadState.Idle
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            try {
                _playlists.value = repository.getAllPlaylists()
            } catch (e: Exception) {
                Log.e("DownloaderMainViewModel", "Ошибка загрузки плейлистов", e)
                _playlists.value = emptyList()
            }
        }
    }
}
