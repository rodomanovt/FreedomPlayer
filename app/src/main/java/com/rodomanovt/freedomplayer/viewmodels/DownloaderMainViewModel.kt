package com.rodomanovt.freedomplayer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rodomanovt.freedomplayer.helpers.YtDlpDownloadHelper
import com.rodomanovt.freedomplayer.helpers.YtDlpManager
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.helpers.DownloadLogger
import com.rodomanovt.freedomplayer.download.PlaylistDownloadManager
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import com.rodomanovt.freedomplayer.repos.DownloaderRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

sealed class TrackDownloadState {
    data object Idle : TrackDownloadState()
    data object UpdatingYtDlp : TrackDownloadState()
    data object InProgress : TrackDownloadState()
    data class Success(val path: String? = null) : TrackDownloadState()
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

    private val downloadManager = PlaylistDownloadManager.get(application)
    private val _activeDownloadIds = MutableLiveData<Set<Long>>(downloadManager.activeDownloadIds.value)
    val activeDownloadIds: LiveData<Set<Long>> = _activeDownloadIds

    init {
        loadPlaylists()
        viewModelScope.launch {
            downloadManager.activeDownloadIds.collect { _activeDownloadIds.postValue(it) }
        }
        viewModelScope.launch {
            downloadManager.trackDownloadState.collect { _trackDownloadState.postValue(it) }
        }
        viewModelScope.launch {
            downloadManager.playlistMessages.collect { _playlistMessage.postValue(it) }
        }
        viewModelScope.launch {
            downloadManager.playlistRefreshes.collect { refreshPlaylists() }
        }
    }

    fun stopDownload(playlistId: Long) {
        DownloadLogger.i(TAG, "Stopping download for playlist ID: $playlistId")
        downloadManager.stop(playlistId)
    }

    fun addPlaylist(name: String, url: String, autoUpdate: Boolean) {
        viewModelScope.launch {
            try {
                repository.addPlaylist(name, url, autoUpdate)
                loadPlaylists()
            } catch (e: Exception) {
                DownloadLogger.e(TAG, "Error adding playlist", e)
                _playlistMessage.value = e.message
                    ?: getApplication<Application>().getString(R.string.playlist_add_failed)
            }
        }
    }

    fun clearPlaylistMessage() {
        _playlistMessage.value = null
    }

    fun updatePlaylist(id: Long, name: String, url: String, autoUpdate: Boolean, lastDownloadTimestamp: Long?) {
        viewModelScope.launch {
            try {
                repository.updatePlaylist(
                    DownloaderPlaylist(
                        id = id,
                        name = name.trim(),
                        url = url.trim(),
                        autoUpdate = autoUpdate,
                        lastDownloadTimestamp = lastDownloadTimestamp
                    )
                )
                loadPlaylists()
            } catch (e: Exception) {
                DownloadLogger.e(TAG, "Error updating playlist", e)
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
                DownloadLogger.e("DownloaderMainViewModel", "Error deleting playlist", e)
            }
        }
    }

    fun downloadTrack(url: String) {
        viewModelScope.launch {
            DownloadLogger.i(TAG, "downloadTrack requested: $url")
            _trackDownloadState.value = TrackDownloadState.UpdatingYtDlp
            YtDlpManager.ensureUpdated(getApplication())
            _trackDownloadState.value = TrackDownloadState.InProgress
            val result = YtDlpDownloadHelper.downloadTrack(getApplication(), url)
            _trackDownloadState.value = if (result.isSuccess) {
                TrackDownloadState.Success(result.getOrNull()?.absolutePath)
            } else {
                TrackDownloadState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }

    fun downloadVideo(url: String) {
        viewModelScope.launch {
            DownloadLogger.i(TAG, "downloadVideo requested: $url")
            _trackDownloadState.value = TrackDownloadState.UpdatingYtDlp
            YtDlpManager.ensureUpdated(getApplication())
            _trackDownloadState.value = TrackDownloadState.InProgress
            val result = YtDlpDownloadHelper.downloadVideo1080p(getApplication(), url)
            _trackDownloadState.value = if (result.isSuccess) {
                TrackDownloadState.Success(result.getOrNull()?.absolutePath)
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
                DownloadLogger.i(TAG, "Scanning playlist for download: ${playlist.name} (${playlist.url})")
                _trackDownloadState.value = TrackDownloadState.UpdatingYtDlp
                YtDlpManager.ensureUpdated(getApplication())
                val songs = repository.getSongsToDownload(playlist)
                songs.forEachIndexed { index, song ->
                    DownloadLogger.i(
                        TAG,
                        "Song to download [${index + 1}/${songs.size}]: ${song.channel} - ${song.name} (${song.url})"
                    )
                }
                DownloadLogger.i(TAG, "Total songs to download for '${playlist.name}': ${songs.size}")
                _trackDownloadState.value = TrackDownloadState.Idle
                _playlistMessage.value = getApplication<Application>().getString(
                    R.string.playlist_songs_to_download,
                    songs.size
                )
            } catch (e: Exception) {
                DownloadLogger.e(TAG, "Failed to scan playlist '${playlist.name}'", e)
                _trackDownloadState.value = TrackDownloadState.Idle
                _playlistMessage.value = e.message
                    ?: getApplication<Application>().getString(R.string.playlist_scan_failed)
            }
        }
    }

    fun downloadPlaylist(playlist: DownloaderPlaylist) {
        downloadManager.download(playlist)
    }

    fun downloadAllPlaylists() {
        val currentPlaylists = _playlists.value ?: return
        val playlistsToDownload = currentPlaylists.filter { it.autoUpdate }
        
        if (playlistsToDownload.isEmpty()) {
            _playlistMessage.value = getApplication<Application>().getString(R.string.no_playlists_for_auto_update)
            return
        }

        playlistsToDownload.forEach { playlist ->
            downloadPlaylist(playlist)
        }
    }

    private suspend fun refreshPlaylists() {
        try {
            val list = repository.getAllPlaylists()
            _playlists.postValue(list)
        } catch (e: Exception) {
            DownloadLogger.e(TAG, "Error updating playlists", e)
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            refreshPlaylists()
        }
    }
}
