package com.rodomanovt.freedomplayer.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.activities.PlayerActivity
import com.rodomanovt.freedomplayer.model.Song
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlaybackService : Service() {

    interface PlaybackCallback {
        fun onCurrentSongChanged(song: Song?)
        fun onIsPlayingChanged(isPlaying: Boolean)
        fun onPlaybackProgressChanged(position: Int)
        fun onPlaybackDurationChanged(duration: Int)
        fun onPlaybackError(message: String)
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    private val binder = LocalBinder()
    private val callbacks = CopyOnWriteArraySet<PlaybackCallback>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null
    private var songQueue: List<Song> = emptyList()
    private var currentSongIndex: Int = 0
    private var currentSong: Song? = null
    private var isPlaying: Boolean = false
    private var playbackDuration: Int = 0
    private var playbackProgress: Int = 0
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private lateinit var notificationManager: NotificationManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isForeground: Boolean = false

    private val progressUpdater = object : Runnable {
        override fun run() {
            val player = mediaPlayer
            if (player?.isPlaying == true) {
                playbackProgress = player.currentPosition
                dispatchProgressChanged()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pause()

            AudioManager.AUDIOFOCUS_LOSS -> stopCurrentSong()
        }
    }

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            onPlayPauseClick()
        }

        override fun onPause() {
            pause()
        }

        override fun onSkipToNext() {
            nextTrack()
        }

        override fun onSkipToPrevious() {
            prevTrack()
        }

        override fun onStop() {
            stopCurrentSong()
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setCallback(sessionCallback)
            setSessionActivity(mainActivityIntent())
            isActive = true
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> onPlayPauseClick()
            ACTION_NEXT -> nextTrack()
            ACTION_PREVIOUS -> prevTrack()
            ACTION_STOP -> stopCurrentSong()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopProgressUpdates()
        releasePlayer()
        mediaSession?.release()
        mediaSession = null
        isForeground = false
        scope.cancel()
        super.onDestroy()
    }

    fun registerCallback(callback: PlaybackCallback) {
        callbacks.add(callback)
        callback.onCurrentSongChanged(currentSong)
        callback.onIsPlayingChanged(isPlaying)
        callback.onPlaybackProgressChanged(playbackProgress)
        callback.onPlaybackDurationChanged(playbackDuration)
    }

    fun unregisterCallback(callback: PlaybackCallback) {
        callbacks.remove(callback)
    }

    fun setQueue(songs: List<Song>, index: Int = 0) {
        songQueue = songs
        if (songs.isEmpty()) {
            stopCurrentSong()
            return
        }

        currentSongIndex = index.coerceIn(0, songs.lastIndex)
        songs.getOrNull(currentSongIndex)?.let { play(it) }
    }

    fun play(song: Song) {
        scope.launch {
            startPlayback(song)
        }
    }

    fun nextTrack() {
        if (songQueue.isEmpty()) return

        if (currentSongIndex + 1 < songQueue.size) {
            currentSongIndex++
            songQueue.getOrNull(currentSongIndex)?.let { play(it) }
        } else {
            stopCurrentSong()
        }
    }

    fun prevTrack() {
        if (songQueue.isEmpty()) return

        if (mediaPlayer?.currentPosition ?: 0 > PREVIOUS_TRACK_THRESHOLD_MS) {
            songQueue.getOrNull(currentSongIndex)?.let { play(it) }
            return
        }

        if (currentSongIndex - 1 >= 0) {
            currentSongIndex--
            songQueue.getOrNull(currentSongIndex)?.let { play(it) }
        } else {
            songQueue.getOrNull(currentSongIndex)?.let { play(it) }
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) return

        player.pause()
        isPlaying = false
        stopProgressUpdates()
        updatePlaybackState()
        updateNotification()
        dispatchIsPlayingChanged()
    }

    fun resume() {
        val player = mediaPlayer
        when {
            player?.isPlaying == true -> return
            player != null -> {
                player.start()
                isPlaying = true
                startProgressUpdates()
                updatePlaybackState()
                updateNotification()
                dispatchIsPlayingChanged()
            }

            currentSong != null -> currentSong?.let { play(it) }
        }
    }

    fun stopCurrentSong() {
        releasePlayer()
        currentSong = null
        playbackProgress = 0
        playbackDuration = 0
        isPlaying = false
        stopProgressUpdates()
        updatePlaybackState()
        dispatchSongChanged()
        dispatchIsPlayingChanged()
        dispatchProgressChanged()
        dispatchDurationChanged()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    fun onPlayPauseClick() {
        val player = mediaPlayer
        when {
            player?.isPlaying == true -> pause()
            player != null -> resume()
            currentSong != null -> currentSong?.let { play(it) }
            songQueue.isNotEmpty() -> songQueue.getOrNull(currentSongIndex)?.let { play(it) }
        }
    }

    private suspend fun startPlayback(song: Song) {
        try {
            val playbackData = withContext(Dispatchers.IO) {
                createPlayerAndMetadata(song)
            }

            if (!requestAudioFocus()) {
                playbackData.player.release()
                notifyError("Не удалось получить доступ к аудио")
                return
            }

            releasePlayer()
            mediaPlayer = playbackData.player
            currentSong = playbackData.song
            playbackDuration = playbackData.player.duration
            playbackProgress = 0
            isPlaying = true
            updateMediaSessionMetadata(playbackData.song)
            updatePlaybackState()
            dispatchSongChanged()
            dispatchDurationChanged()
            dispatchProgressChanged()
            dispatchIsPlayingChanged()
            startForeground(NOTIFICATION_ID, buildNotification())
            playbackData.player.start()
            startProgressUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start playback", e)
            notifyError("Не удалось воспроизвести ${song.title}")
        }
    }

    private fun createPlayerAndMetadata(song: Song): PlaybackData {
        val player = MediaPlayer()
        val uri = Uri.parse(song.songPath)
        val descriptor = contentResolver.openFileDescriptor(uri, "r")
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

    private fun requestAudioFocus(): Boolean {
        val manager = audioManager ?: return true
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setAcceptsDelayedFocusGain(true)
                .build()

            audioFocusRequest = request
            manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun releaseAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.run {
            setOnCompletionListener(null)
            runCatching { stop() }
            release()
        }
        mediaPlayer = null
        releaseAudioFocus()
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        handler.post(progressUpdater)
    }

    private fun stopProgressUpdates() {
        handler.removeCallbacks(progressUpdater)
    }

    private fun updateMediaSessionMetadata(song: Song?) {
        val session = mediaSession ?: return
        if (song == null) {
            session.setMetadata(null)
            return
        }

        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .build()
        )
    }

    private fun updatePlaybackState() {
        mediaSession?.isActive = true
    }

    private fun buildNotification(): Notification {
        val song = currentSong
        val title = song?.title ?: getString(R.string.notification_playback_title)
        val artist = song?.artist ?: getString(R.string.app_name)
        val playing = isPlaying

        val previousIntent = serviceIntent(ACTION_PREVIOUS)
        val playPauseIntent = serviceIntent(ACTION_PLAY_PAUSE)
        val nextIntent = serviceIntent(ACTION_NEXT)
        val stopIntent = serviceIntent(ACTION_STOP)

        val playPauseIcon = if (playing) {
            R.drawable.baseline_pause_24
        } else {
            R.drawable.baseline_play_arrow_24
        }
        val playPauseLabel = if (playing) {
            getString(R.string.pausePlayback)
        } else {
            getString(R.string.resumePlayback)
        }

        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(if (playing) getString(R.string.notification_playback_title) else getString(R.string.notification_playback_paused))
            .setContentIntent(mainActivityIntent())
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(R.drawable.baseline_skip_previous_24, getString(R.string.previous), previousIntent)
            .addAction(playPauseIcon, playPauseLabel, playPauseIntent)
            .addAction(R.drawable.baseline_skip_next_24, getString(R.string.next), nextIntent)
            .addAction(R.drawable.baseline_music_note_24, getString(R.string.playerText), stopIntent)
            .setStyle(mediaStyle)
            .build()
    }

    private fun updateNotification() {
        if (currentSong == null) return

        val notification = buildNotification()
        if (isPlaying || !isForeground) {
            startForeground(NOTIFICATION_ID, notification)
            isForeground = true
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun serviceIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                action.hashCode(),
                intent,
                pendingIntentFlags()
            )
        } else {
            PendingIntent.getService(
                this,
                action.hashCode(),
                intent,
                pendingIntentFlags()
            )
        }
    }

    private fun mainActivityIntent(): PendingIntent {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            pendingIntentFlags()
        )
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.playback_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.playback_channel_description)
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun notifyError(message: String) {
        handler.post {
            Log.e(TAG, message)
            callbacks.forEach { it.onPlaybackError(message) }
        }
    }

    private fun dispatchSongChanged() {
        callbacks.forEach { it.onCurrentSongChanged(currentSong) }
    }

    private fun dispatchIsPlayingChanged() {
        callbacks.forEach { it.onIsPlayingChanged(isPlaying) }
    }

    private fun dispatchProgressChanged() {
        callbacks.forEach { it.onPlaybackProgressChanged(playbackProgress) }
    }

    private fun dispatchDurationChanged() {
        callbacks.forEach { it.onPlaybackDurationChanged(playbackDuration) }
    }

    private data class PlaybackData(
        val player: MediaPlayer,
        val song: Song
    )

    companion object {
        private const val TAG = "MusicPlaybackService"
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PREVIOUS_TRACK_THRESHOLD_MS = 5000
        private const val ACTION_PLAY_PAUSE = "com.rodomanovt.freedomplayer.action.PLAY_PAUSE"
        private const val ACTION_NEXT = "com.rodomanovt.freedomplayer.action.NEXT"
        private const val ACTION_PREVIOUS = "com.rodomanovt.freedomplayer.action.PREVIOUS"
        private const val ACTION_STOP = "com.rodomanovt.freedomplayer.action.STOP"
    }
}
