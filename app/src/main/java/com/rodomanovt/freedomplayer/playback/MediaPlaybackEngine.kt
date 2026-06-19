package com.rodomanovt.freedomplayer.playback

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.rodomanovt.freedomplayer.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

internal class MediaPlaybackEngine(
    private val context: Context,
    private val onCurrentSongChanged: (Song?) -> Unit,
    private val onIsPlayingChanged: (Boolean) -> Unit,
    private val onPlaybackProgressChanged: (Int) -> Unit,
    private val onPlaybackDurationChanged: (Int) -> Unit,
    private val onError: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null
    private var songQueue: List<Song> = emptyList()
    private var currentSongIndex: Int = 0
    private var currentSong: Song? = null

    private val progressUpdater = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    onPlaybackProgressChanged(player.currentPosition)
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    fun setQueue(songs: List<Song>, index: Int = 0) {
        songQueue = songs
        currentSongIndex = index.coerceIn(0, songs.lastIndex.coerceAtLeast(0))
        songs.getOrNull(currentSongIndex)?.let { play(it) }
    }

    fun play(song: Song) {
        scope.launch {
            startPlayback(song)
        }
    }

    fun nextTrack() {
        if (currentSongIndex + 1 < songQueue.size) {
            currentSongIndex++
            songQueue.getOrNull(currentSongIndex)?.let { play(it) }
        } else {
            stopCurrentSong()
        }
    }

    fun prevTrack() {
        if (currentSongIndex - 1 >= 0) {
            currentSongIndex--
            songQueue.getOrNull(currentSongIndex)?.let { play(it) }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        onIsPlayingChanged(false)
        stopProgressUpdates()
    }

    fun resume() {
        mediaPlayer?.start()
        onIsPlayingChanged(true)
        startProgressUpdates()
    }

    fun stopCurrentSong() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentSong = null
        onCurrentSongChanged(null)
        onIsPlayingChanged(false)
        onPlaybackProgressChanged(0)
        onPlaybackDurationChanged(0)
        stopProgressUpdates()
    }

    fun onPlayPauseClick() {
        val player = mediaPlayer
        when {
            player?.isPlaying == true -> pause()
            player != null -> resume()
            currentSong != null -> currentSong?.let { play(it) }
        }
    }

    fun release() {
        stopCurrentSong()
        scope.cancel()
    }

    private suspend fun startPlayback(song: Song) {
        try {
            val playbackData = withContext(Dispatchers.IO) {
                createPlayerAndMetadata(song)
            }

            mediaPlayer?.release()
            mediaPlayer = playbackData.player
            currentSong = playbackData.song
            onCurrentSongChanged(playbackData.song)
            onIsPlayingChanged(true)
            onPlaybackDurationChanged(playbackData.player.duration)
            startProgressUpdates()
            playbackData.player.start()
        } catch (e: Exception) {
            notifyError("Не удалось воспроизвести ${song.title}")
        }
    }

    private fun createPlayerAndMetadata(song: Song): PlaybackData {
        val player = MediaPlayer()
        val uri = Uri.parse(song.songPath)
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Не удалось открыть файл")

        val retriever = MediaMetadataRetriever()
        var resolvedSong = song

        descriptor.use { fd ->
            retriever.setDataSource(fd.fileDescriptor)
            player.setDataSource(fd.fileDescriptor)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: song.title
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: song.artist
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?: song.duration

            resolvedSong = song.copy(
                title = title,
                artist = artist,
                duration = duration
            )
        }

        try {
            player.prepare()
            player.setOnCompletionListener {
                nextTrack()
            }
        } finally {
            retriever.release()
        }

        return PlaybackData(player = player, song = resolvedSong)
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        handler.post(progressUpdater)
    }

    private fun stopProgressUpdates() {
        handler.removeCallbacks(progressUpdater)
    }

    private fun notifyError(message: String) {
        mainHandler.post {
            onError(message)
        }
    }

    private data class PlaybackData(
        val player: MediaPlayer,
        val song: Song
    )
}
