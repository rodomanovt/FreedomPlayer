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
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _playbackProgress = MutableLiveData<Int>(0)
    val playbackProgress: LiveData<Int> get() = _playbackProgress

    private val _playbackDuration = MutableLiveData<Int>(0)
    val playbackDuration: LiveData<Int> get() = _playbackDuration

    private lateinit var songQueue: List<Song>
    private var currentSongIndex: Int = 0

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

    // 🔁 Установить очередь и начать воспроизведение
    fun setQueue(context: Context, songs: List<Song>, index: Int = 0) {
        this.songQueue = songs
        this.currentSongIndex = index
        songs.getOrNull(index)?.let { play(context, it) }
    }

    // ▶️ Воспроизвести конкретный трек
    fun play(context: Context, song: Song) {
        viewModelScope.launch {
            try {
                val player = MediaPlayer().apply {
                    val uri = Uri.parse(song.songPath)
                    val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                        ?: throw IOException("Не удалось открыть файл")

                    setDataSource(descriptor.fileDescriptor)
                    prepare()
                    start()

                    setOnCompletionListener {
                        nextTrack(context)
                    }
                }

                mediaPlayer?.release()
                mediaPlayer = player
                _currentSong.postValue(song)
                _isPlaying.postValue(true)

                _playbackDuration.postValue(player.duration)
                handler.removeCallbacks(updateRunnable)
                handler.post(updateRunnable)
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Ошибка воспроизведения", e)
                Toast.makeText(context, "Не удалось воспроизвести ${song.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ⏭ Перейти к следующему треку
    fun nextTrack(context: Context) {
        if (currentSongIndex + 1 < songQueue.size) {
            currentSongIndex++
            play(context, songQueue[currentSongIndex])
        } else {
            stopCurrentSong()
        }
    }

    // ⏮ Перейти к предыдущему треку
    fun prevTrack(context: Context) {
        if (currentSongIndex - 1 >= 0) {
            currentSongIndex--
            play(context, songQueue[currentSongIndex])
        }
    }

    // ⏸ Поставить на паузу
    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.postValue(false)
        handler.removeCallbacks(updateRunnable)
    }

    // ▶️ Возобновить
    fun resume(context: Context) {
        mediaPlayer?.start()
        _isPlaying.postValue(true)
        handler.post(updateRunnable)
    }

    // ⏹ Остановить плеер
    fun stopCurrentSong() {
        mediaPlayer?.release()
        mediaPlayer = null
        _currentSong.postValue(null)
        _isPlaying.postValue(false)
        _playbackProgress.postValue(0)
        _playbackDuration.postValue(0)
        handler.removeCallbacks(updateRunnable)
    }

    // 🔄 Для кликов с кнопок
    fun onPlayPauseClick(context: Context) {
        if (_isPlaying.value == true) {
            pause()
        } else {
            mediaPlayer?.let {
                resume(context)
            } ?: run {
                _currentSong.value?.let { song ->
                    play(context, song)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        handler.removeCallbacks(updateRunnable)
    }
}