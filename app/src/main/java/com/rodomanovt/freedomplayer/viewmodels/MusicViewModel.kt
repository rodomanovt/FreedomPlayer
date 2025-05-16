package com.rodomanovt.freedomplayer.viewmodels

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.model.Playlist
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.repos.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    val songsFromDb = repository.getSongsFromDb(folderUri.toString())
                    if (songsFromDb.isNotEmpty()) {
                        _songs.postValue(songsFromDb)
                    } else {
                        val scannedSongs = repository.scanAndSaveSongs(folder)
                        _songs.postValue(scannedSongs)
                    }
                } else {
                    _songs.postValue(emptyList())
                    Log.e("MusicViewModel", "Folder not accessible")
                }
            } catch (e: Exception) {
                _songs.postValue(emptyList())
                Log.e("MusicViewModel", "Error loading songs", e)
            }
        }
    }

    companion object {
        fun loadAlbumArt(song: Song, imageView: ImageView) {
            CoroutineScope(Dispatchers.IO).launch {
                val retriever = MediaMetadataRetriever()
                try {
                    when {
                        // Для URI content://
                        song.songPath.startsWith("content://") -> {
                            val pfd = imageView.context.contentResolver.openFileDescriptor(
                                Uri.parse(song.songPath), "r"
                            )
                            pfd?.use {
                                retriever.setDataSource(it.fileDescriptor)
                            }
                        }

                        // Для абсолютных путей (устаревший способ)
                        song.songPath.startsWith("/") -> {
                            retriever.setDataSource(song.songPath)
                        }

                        // Для URI DocumentFile
                        else -> {
                            val uri = Uri.parse(song.songPath)
                            val pfd = imageView.context.contentResolver.openFileDescriptor(uri, "r")
                            pfd?.use {
                                retriever.setDataSource(it.fileDescriptor)
                            }
                        }
                    }

                    val artBytes = retriever.embeddedPicture
                    withContext(Dispatchers.Main) {
                        if (artBytes != null) {
                            Glide.with(imageView.context)
                                .load(artBytes)
                                .placeholder(R.drawable.baseline_music_note_24)
                                .into(imageView)
                        } else {
                            imageView.setImageResource(R.drawable.baseline_music_note_24)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MusicPlayer", "Error loading album art", e)
                    withContext(Dispatchers.Main) {
                        imageView.setImageResource(R.drawable.baseline_music_note_24)
                    }
                } finally {
                    retriever.release()
                }
            }
        }
    }
}