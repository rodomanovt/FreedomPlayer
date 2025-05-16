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
        viewModelScope.launch {
            try {
                val player = MediaPlayer().apply {
                    val uri = Uri.parse(song.songPath)
                    val resolver = context.contentResolver
                    val descriptor = resolver.openFileDescriptor(uri, "r")
                        ?: throw IOException("Не удалось открыть файловый дескриптор")

                    // Воспроизводим через FileDescriptor
                    setDataSource(descriptor.fileDescriptor)
                    prepare()
                    start()
                }

                // Освобождаем старый плеер, если он был
                mediaPlayer?.release()
                mediaPlayer = player
            } catch (e: Exception) {
                Log.e("MediaPlayerViewModel", "Ошибка воспроизведения песни", e)
                Toast.makeText(context, "Не удалось воспроизвести ${song.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopCurrentSong() {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null
        _currentSong.postValue(null)
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}