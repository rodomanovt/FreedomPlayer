package com.rodomanovt.freedomplayer.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.playback.MusicPlaybackService
import java.util.ArrayDeque

class MediaPlayerViewModel : ViewModel(), MusicPlaybackService.PlaybackCallback {
    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> get() = _currentSong

    private val _nextSong = MutableLiveData<Song?>()
    val nextSong: LiveData<Song?> get() = _nextSong

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _playbackProgress = MutableLiveData(0)
    val playbackProgress: LiveData<Int> get() = _playbackProgress

    private val _playbackDuration = MutableLiveData(0)
    val playbackDuration: LiveData<Int> get() = _playbackDuration

    private val _isShuffleEnabled = MutableLiveData(false)
    val isShuffleEnabled: LiveData<Boolean> get() = _isShuffleEnabled

    private var appContext: Context? = null
    private var boundService: MusicPlaybackService? = null
    private var isBound = false
    private val pendingActions = ArrayDeque<(MusicPlaybackService) -> Unit>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val playbackService = (service as? MusicPlaybackService.LocalBinder)?.getService()
                ?: return

            boundService = playbackService
            isBound = true
            playbackService.registerCallback(this@MediaPlayerViewModel)

            while (pendingActions.isNotEmpty()) {
                pendingActions.removeFirst().invoke(playbackService)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService?.unregisterCallback(this@MediaPlayerViewModel)
            boundService = null
            isBound = false
        }
    }

    fun connect(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        if (isBound) return

        applicationContext.bindService(
            android.content.Intent(applicationContext, MusicPlaybackService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun setQueue(context: Context, songs: List<Song>, index: Int = 0, startFromRandom: Boolean = false) {
        submit(context) { service -> service.setQueue(songs, index, startFromRandom) }
    }

    fun setShuffleEnabled(context: Context, enabled: Boolean) {
        submit(context) { service -> service.setShuffleEnabled(enabled) }
    }

    fun toggleShuffle(context: Context) {
        submit(context) { service -> service.toggleShuffle() }
    }

    fun play(context: Context, song: Song) {
        submit(context) { service -> service.play(song) }
    }

    fun nextTrack(context: Context) {
        submit(context) { service -> service.nextTrack() }
    }

    fun prevTrack(context: Context) {
        submit(context) { service -> service.prevTrack() }
    }

    fun pause() {
        boundService?.pause()
    }

    fun resume(context: Context) {
        submit(context) { service -> service.resume() }
    }

    fun stopCurrentSong() {
        boundService?.stopCurrentSong()
    }

    fun onPlayPauseClick(context: Context) {
        submit(context) { service -> service.onPlayPauseClick() }
    }

    fun seekTo(context: Context, position: Int) {
        submit(context) { service -> service.seekTo(position) }
    }

    override fun onCurrentSongChanged(song: Song?) {
        _currentSong.postValue(song)
    }

    override fun onNextSongChanged(song: Song?) {
        _nextSong.postValue(song)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.postValue(isPlaying)
    }

    override fun onPlaybackProgressChanged(position: Int) {
        _playbackProgress.postValue(position)
    }

    override fun onPlaybackDurationChanged(duration: Int) {
        _playbackDuration.postValue(duration)
    }

    override fun onShuffleModeChanged(enabled: Boolean) {
        _isShuffleEnabled.postValue(enabled)
    }

    override fun onPlaybackError(message: String) {
        showPlaybackError(message)
    }

    override fun onCleared() {
        boundService?.unregisterCallback(this)
        if (isBound) {
            runCatching {
                appContext?.unbindService(connection)
            }
        }
        boundService = null
        isBound = false
        super.onCleared()
    }

    private fun submit(context: Context, action: (MusicPlaybackService) -> Unit) {
        connect(context)
        val service = boundService
        if (service != null) {
            action(service)
        } else {
            pendingActions.addLast(action)
        }
    }

    private fun showPlaybackError(message: String) {
        appContext?.let { context ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
