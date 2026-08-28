package com.rodomanovt.freedomplayer.download

import android.content.Context
import com.rodomanovt.freedomplayer.helpers.DownloadLogger
import com.rodomanovt.freedomplayer.helpers.YtDlpManager
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import com.rodomanovt.freedomplayer.repos.DownloaderRepository
import com.rodomanovt.freedomplayer.viewmodels.TrackDownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Process-scoped queue. UI recreation must not cancel playlist downloads. */
class PlaylistDownloadManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val repository = DownloaderRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val downloadJobs = mutableMapOf<Long, Job>()
    private val downloadQueue = mutableListOf<DownloaderPlaylist>()
    private var isProcessingQueue = false

    private val _activeDownloadIds = MutableStateFlow<Set<Long>>(emptySet())
    val activeDownloadIds: StateFlow<Set<Long>> = _activeDownloadIds.asStateFlow()

    private val _trackDownloadState = MutableStateFlow<TrackDownloadState>(TrackDownloadState.Idle)
    val trackDownloadState: StateFlow<TrackDownloadState> = _trackDownloadState.asStateFlow()

    private val _playlistMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val playlistMessages: SharedFlow<String> = _playlistMessages
    private val _playlistRefreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playlistRefreshes: SharedFlow<Unit> = _playlistRefreshes

    init {
        DownloadLogger.i(TAG, "Playlist download manager initialized")
    }

    fun download(playlist: DownloaderPlaylist) {
        if (_activeDownloadIds.value.contains(playlist.id)) return
        if (downloadQueue.isEmpty() && !isProcessingQueue) {
            DownloadForegroundService.start(appContext)
        }
        downloadQueue.add(playlist)
        _activeDownloadIds.value = _activeDownloadIds.value + playlist.id
        if (!isProcessingQueue) processNext()
    }

    fun stop(playlistId: Long) {
        downloadJobs[playlistId]?.cancel()
            ?: run {
                downloadQueue.removeAll { it.id == playlistId }
                _activeDownloadIds.value = _activeDownloadIds.value - playlistId
            }
    }

    private fun processNext() {
        if (downloadQueue.isEmpty()) {
            isProcessingQueue = false
            DownloadForegroundService.stop(appContext)
            return
        }

        isProcessingQueue = true
        val playlist = downloadQueue.removeAt(0)
        val job = scope.launch {
            try {
                _trackDownloadState.value = TrackDownloadState.UpdatingYtDlp
                YtDlpManager.ensureUpdated(appContext)
                _trackDownloadState.value = TrackDownloadState.InProgress
                val songs = repository.getSongsToDownload(playlist)
                if (songs.isNotEmpty()) {
                    repository.downloadSongs(playlist, songs)
                    _trackDownloadState.value = TrackDownloadState.Success()
                } else {
                    repository.updatePlaylistTimestamp(playlist)
                    _trackDownloadState.value = TrackDownloadState.Idle
                    _playlistMessages.tryEmit("No new tracks to download in ${playlist.name}")
                }
                _playlistRefreshes.tryEmit(Unit)
            } catch (e: CancellationException) {
                DownloadLogger.i(TAG, "Download cancelled for playlist '${playlist.name}'")
                _trackDownloadState.value = TrackDownloadState.Idle
            } catch (e: Exception) {
                DownloadLogger.e(TAG, "Failed to download playlist '${playlist.name}'", e)
                _trackDownloadState.value = TrackDownloadState.Error(e.message ?: "Unknown error")
            } finally {
                downloadJobs.remove(playlist.id)
                _activeDownloadIds.value = _activeDownloadIds.value - playlist.id
                processNext()
            }
        }
        downloadJobs[playlist.id] = job
    }

    companion object {
        private const val TAG = "PlaylistDownloadManager"
        @Volatile private var instance: PlaylistDownloadManager? = null

        fun get(context: Context): PlaylistDownloadManager =
            instance ?: synchronized(this) {
                instance ?: PlaylistDownloadManager(context).also { instance = it }
            }
    }
}
