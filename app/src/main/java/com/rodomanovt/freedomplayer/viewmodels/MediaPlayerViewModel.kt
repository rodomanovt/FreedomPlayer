package com.rodomanovt.freedomplayer.viewmodels

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodomanovt.freedomplayer.model.Song
import kotlinx.coroutines.launch
import java.io.IOException

class MediaPlayerViewModel : ViewModel() {
    private var mediaPlayer: MediaPlayer? = null
    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> get() = _currentSong

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean?> get() = _isPlaying

    private val _playbackProgress = MutableLiveData<Int>(0)
    val playbackProgress: LiveData<Int> get() = _playbackProgress

    private val _playbackDuration = MutableLiveData<Int>(0)
    val playbackDuration: LiveData<Int> get() = _playbackDuration

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    _playbackProgress.postValue(player.currentPosition)
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    fun playOrToggleSong(context: Context, song: Song) {
        viewModelScope.launch {
            try {
                if (_currentSong.value == song && mediaPlayer?.isPlaying == true) {
                    pause()
                } else if (_currentSong.value == song && !mediaPlayer?.isPlaying!!) {
                    resume()
                } else {
                    resetAndPlay(context, song)
                }
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Ошибка воспроизведения", e)
                Toast.makeText(context, "Не удалось воспроизвести ${song.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetAndPlay(context: Context, song: Song) {
        mediaPlayer?.release()
        mediaPlayer = null
        _currentSong.postValue(song)

        val uri = Uri.parse(song.songPath)
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Не удалось открыть файл")

        val player = MediaPlayer().apply {
            setDataSource(descriptor.fileDescriptor)
            prepare()
            start()

            setOnCompletionListener {
                stopCurrentSong()
            }
        }

        descriptor.close()
        mediaPlayer = player
        _isPlaying.postValue(true)

        _playbackDuration.postValue(player.duration)
        handler.post(updateRunnable)
    }

    fun resume() {
        mediaPlayer?.start()
        _isPlaying.postValue(true)
        handler.post(updateRunnable)
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.postValue(false)
        handler.removeCallbacks(updateRunnable)
    }

    fun stopCurrentSong() {
        mediaPlayer?.release()
        mediaPlayer = null
        _currentSong.postValue(null)
        _isPlaying.postValue(false)
        _playbackProgress.postValue(0)
        _playbackDuration.postValue(0)
        handler.removeCallbacks(updateRunnable)
    }

    fun onCided() {
        super.onCleared()
        mediaPlayer?.release()
        handler.removeCallbacks(updateRunnable)
    }
}