package com.rodomanovt.freedomplayer.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rodomanovt.freedomplayer.helpers.YtDlpDownloadHelper
import com.rodomanovt.freedomplayer.helpers.YtDlpManager
import com.rodomanovt.freedomplayer.R
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

    private val _playlistMessage = MutableLiveData<String?>()
    val playlistMessage: LiveData<String?> = _playlistMessage

    init {
        loadPlaylists()
    }

    fun addPlaylist(name: String, url: String, autoUpdate: Boolean) {
        viewModelScope.launch {
            try {
                repository.addPlaylist(name, url, autoUpdate)
                loadPlaylists()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка добавления плейлиста", e)
                _playlistMessage.value = e.message
                    ?: getApplication<Application>().getString(R.string.playlist_add_failed)
            }
        }
    }

    fun clearPlaylistMessage() {
        _playlistMessage.value = null
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
                Log.e(TAG, "Ошибка обновления плейлиста", e)
                _playlistMessage.value = e.message
                    ?: getApplication<Application>().getString(R.string.playlist_update_failed)
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

    fun scanPlaylistForDownload(playlist: DownloaderPlaylist) {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Scanning playlist for download: ${playlist.name} (${playlist.url})")
                _trackDownloadState.value = TrackDownloadState.UpdatingYtDlp
                YtDlpManager.ensureUpdated(getApplication())
                val songs = repository.getSongsToDownload(playlist)
                songs.forEachIndexed { index, song ->
                    Log.i(
                        TAG,
                        "Song to download [${index + 1}/${songs.size}]: ${song.channel} - ${song.name} (${song.url})"
                    )
                }
                Log.i(TAG, "Total songs to download for '${playlist.name}': ${songs.size}")
                _trackDownloadState.value = TrackDownloadState.Idle
                _playlistMessage.value = getApplication<Application>().getString(
                    R.string.playlist_songs_to_download,
                    songs.size
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan playlist '${playlist.name}'", e)
                _trackDownloadState.value = TrackDownloadState.Idle
                _playlistMessage.value = e.message
                    ?: getApplication<Application>().getString(R.string.playlist_scan_failed)
            }
        }
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
