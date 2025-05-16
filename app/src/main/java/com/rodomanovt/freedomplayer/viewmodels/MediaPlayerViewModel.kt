package com.rodomanovt.freedomplayer.viewmodels

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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
    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> get() = _currentSong

    private var mediaPlayer: MediaPlayer? = null


    fun playSong(context: Context, song: Song) {
        mediaPlayer?.release() // Освобождаем предыдущий плеер

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, Uri.parse(song.songPath))
            setOnPreparedListener {
                start()
                _currentSong.postValue(song) // 🔥 Важно: обновляем LiveData после начала воспроизведения
            }
            prepareAsync() // Используем асинхронную подготовку
        }
    }

    fun stopCurrentSong() {
        mediaPlayer?.let {
            it.stop()
            //it.release()
        }
        //mediaPlayer = null
        //_currentSong.postValue(null)
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}