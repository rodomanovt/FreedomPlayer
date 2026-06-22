package com.rodomanovt.freedomplayer.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationView
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.ActivityPlayerBinding
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.viewmodels.MediaPlayerViewModel
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel.Companion.loadAlbumArt

class PlayerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityPlayerBinding
    private val playerViewModel: MediaPlayerViewModel by viewModels()
    private var isOnTrackPlayerScreen = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navView.setNavigationItemSelectedListener(this)
        setupMiniPlayer()
        requestNotificationPermissionIfNeeded()
    }

    private fun setupMiniPlayer() {
        val miniPlayer = binding.miniPlayer

        miniPlayer.playerCard.setOnClickListener {
            if (playerViewModel.currentSong.value != null) {
                navController().navigate(R.id.action_global_trackPlayerFragment)
            }
        }

        miniPlayer.playerPlayPause.setOnClickListener {
            if (playerViewModel.isPlaying.value == true) {
                playerViewModel.pause()
            } else {
                playerViewModel.resume(this)
            }
        }

        miniPlayer.playerNext.setOnClickListener {
            playerViewModel.nextTrack(this)
        }

        miniPlayer.playerPrev.setOnClickListener {
            playerViewModel.prevTrack(this)
        }

        playerViewModel.currentSong.observe(this) { song ->
            song?.let { updateMiniPlayer(it) }
            updateMiniPlayerVisibility(song)
        }

        playerViewModel.playbackProgress.observe(this) { position ->
            miniPlayer.playerProgressBar.progress = position
            miniPlayer.playerCurrentTime.text = formatDuration(position.toLong())
        }

        playerViewModel.playbackDuration.observe(this) { duration ->
            miniPlayer.playerProgressBar.min = 0
            miniPlayer.playerProgressBar.max = duration
        }

        playerViewModel.isPlaying.observe(this) { playing ->
            miniPlayer.playerPlayPause.setImageResource(
                if (playing == true) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
            )
            miniPlayer.playerPlayPause.contentDescription = getString(
                if (playing == true) R.string.pausePlayback else R.string.resumePlayback
            )
        }

        navController().addOnDestinationChangedListener { _, destination, _ ->
            isOnTrackPlayerScreen = destination.id == R.id.trackPlayerFragment
            updateMiniPlayerVisibility(playerViewModel.currentSong.value)
        }
    }

    private fun updateMiniPlayerVisibility(song: Song?) {
        binding.miniPlayer.playerCard.visibility =
            if (song != null && !isOnTrackPlayerScreen) View.VISIBLE else View.GONE
    }

    private fun updateMiniPlayer(song: Song) {
        val miniPlayer = binding.miniPlayer
        miniPlayer.playerTitle.text = song.title
        miniPlayer.playerArtist.text = song.artist
        loadAlbumArt(song, miniPlayer.playerAlbumArt)
    }

    private fun navController() =
        (supportFragmentManager.findFragmentById(R.id.settings_nav_host_fragment_container) as NavHostFragment)
            .navController

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        var activityLaunchIntent: Intent? = null

        val id = item.getItemId()
        if (id == R.id.musicPlayer)
        {
            // current
        }
        else if (id == R.id.downloader)
        {
            activityLaunchIntent = Intent(this, DownloaderActivity::class.java)
        }
        else if (id == R.id.settings)
        {
            activityLaunchIntent = Intent(this, SettingsActivity::class.java)
        }
        val drawer = binding.drawerLayout
        drawer.closeDrawer(GravityCompat.START)

        if (activityLaunchIntent != null) {
            startActivity(activityLaunchIntent)
        }

        return true
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
