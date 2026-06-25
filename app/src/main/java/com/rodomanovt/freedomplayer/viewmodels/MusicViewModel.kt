package com.rodomanovt.freedomplayer.viewmodels

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.ImageView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.helpers.PrefsHelper
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
    private val _currentPlaylist = MutableLiveData<Playlist?>()
    val currentPlaylist: LiveData<Playlist?> = _currentPlaylist
    private val _isReindexing = MutableLiveData(false)
    val isReindexing: LiveData<Boolean> = _isReindexing

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

    fun reindexAllPlaylists(rootFolderUri: Uri) {
        viewModelScope.launch {
            if (_isReindexing.value == true) return@launch
            _isReindexing.value = true
            try {
                val reindexedPlaylists = repository.reindexAllPlaylists(rootFolderUri)
                _playlists.value = reindexedPlaylists
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Ошибка переиндексации плейлистов", e)
            } finally {
                _isReindexing.value = false
            }
        }
    }


    fun loadSongs(folderUri: Uri) {
        viewModelScope.launch {
            try {
                val playlist = repository.getPlaylistByUri(folderUri)
                _currentPlaylist.value = playlist

                val folder = resolveFolder(folderUri)
                if (folder != null && folder.exists()) {
                    val songsFromDb = repository.getSongsFromDb(folderUri.toString())
                    if (songsFromDb.isNotEmpty()) {
                        _songs.value = songsFromDb
                        if (needsMetadataRefresh(songsFromDb)) {
                            viewModelScope.launch {
                                val enrichedSongs = repository.enrichSongsMetadata(folder, songsFromDb) { indexedSongs ->
                                    viewModelScope.launch(Dispatchers.Main.immediate) {
                                        _songs.value = indexedSongs
                                    }
                                }
                                _songs.value = enrichedSongs
                                _currentPlaylist.value = repository.getPlaylistByUri(folderUri)
                            }
                        }
                    } else {
                        _songs.value = emptyList()
                        val scannedSongs = repository.scanAndSaveSongs(folder) { indexedSongs ->
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _songs.value = indexedSongs
                            }
                        }
                        _songs.value = scannedSongs
                        _currentPlaylist.value = repository.getPlaylistByUri(folderUri)
                        
                        if (scannedSongs.isNotEmpty()) {
                            viewModelScope.launch {
                                val enrichedSongs = repository.enrichSongsMetadata(folder, scannedSongs) { indexedSongs ->
                                    viewModelScope.launch(Dispatchers.Main.immediate) {
                                        _songs.value = indexedSongs
                                    }
                                }
                                _songs.value = enrichedSongs
                                _currentPlaylist.value = repository.getPlaylistByUri(folderUri)
                            }
                        }
                    }
                } else {
                    _songs.value = emptyList()
                    Log.e("MusicViewModel", "Folder not accessible")
                }
            } catch (e: Exception) {
                _songs.value = emptyList()
                Log.e("MusicViewModel", "Error loading songs", e)
            }
        }
    }

    private fun resolveFolder(folderUri: Uri): DocumentFile? {
        PrefsHelper(app).getRootFolderUri()?.let { rootTreeUri ->
            val root = DocumentFile.fromTreeUri(app, rootTreeUri)
            if (root != null && root.exists()) {
                findFolderUnderRoot(root, rootTreeUri, folderUri)?.let { return it }
            }
        }

        return DocumentFile.fromTreeUri(app, folderUri)
            ?: DocumentFile.fromSingleUri(app, folderUri)
    }

    private fun needsMetadataRefresh(songs: List<Song>): Boolean {
        return songs.any { it.artist.isBlank() || it.duration == 0L }
    }

    private fun findFolderUnderRoot(
        root: DocumentFile,
        rootTreeUri: Uri,
        targetUri: Uri
    ): DocumentFile? {
        return try {
            val rootDocId = DocumentsContract.getTreeDocumentId(rootTreeUri)
            val targetDocId = DocumentsContract.getDocumentId(targetUri)

            if (targetDocId == rootDocId) return root
            if (!targetDocId.startsWith(rootDocId)) return null

            val relativePath = targetDocId.removePrefix(rootDocId).trimStart('/')
            if (relativePath.isBlank()) return root

            var current = root
            for (segment in relativePath.split('/').filter { it.isNotBlank() }) {
                val next = current.listFiles().firstOrNull { it.name == segment }
                    ?: return null
                if (!next.isDirectory) return null
                current = next
            }

            current
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Ошибка поиска папки внутри корня", e)
            null
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
