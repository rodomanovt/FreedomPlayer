package com.rodomanovt.freedomplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.FragmentTrackPlayerBinding
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.viewmodels.FavoritesViewModel
import com.rodomanovt.freedomplayer.viewmodels.MediaPlayerViewModel
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel.Companion.loadAlbumArt

class TrackPlayerFragment : Fragment() {
    private lateinit var binding: FragmentTrackPlayerBinding
    private val playerViewModel: MediaPlayerViewModel by activityViewModels()
    private val favoritesViewModel: FavoritesViewModel by activityViewModels()
    private var isUserSeeking = false
    private var currentSongPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.trackCloseButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.trackPrev.setOnClickListener {
            playerViewModel.prevTrack(requireContext())
        }

        binding.trackShuffle.setOnClickListener {
            playerViewModel.toggleShuffle(requireContext())
        }

        binding.trackPlayPause.setOnClickListener {
            if (playerViewModel.isPlaying.value == true) {
                playerViewModel.pause()
            } else {
                playerViewModel.resume(requireContext())
            }
        }

        binding.trackNext.setOnClickListener {
            playerViewModel.nextTrack(requireContext())
        }

        binding.trackLike.setOnClickListener {
            currentSongPath?.let { favoritesViewModel.toggleLike(it) }
        }

        binding.trackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.trackCurrentTime.text = formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val position = seekBar?.progress ?: 0
                isUserSeeking = false
                playerViewModel.seekTo(requireContext(), position)
            }
        })

        setupObservers()
    }

    private fun setupObservers() {
        playerViewModel.currentSong.observe(viewLifecycleOwner) { song ->
            if (song == null) {
                findNavController().navigateUp()
                return@observe
            }

            bindSong(song)
        }

        playerViewModel.nextSong.observe(viewLifecycleOwner) { song ->
            if (song == null) {
                binding.nextTrackSection.visibility = View.GONE
                return@observe
            }

            binding.nextTrackSection.visibility = View.VISIBLE
            binding.nextTrackTitle.text = song.title.ifBlank { getString(R.string.noTrackPlaying) }
            binding.nextTrackArtist.text = song.artist.ifBlank { getString(R.string.undefined) }
            loadAlbumArt(song, binding.nextTrackCover)
        }

        playerViewModel.playbackProgress.observe(viewLifecycleOwner) { position ->
            if (!isUserSeeking) {
                binding.trackSeekBar.progress = position
                binding.trackCurrentTime.text = formatDuration(position.toLong())
            }
        }

        playerViewModel.playbackDuration.observe(viewLifecycleOwner) { duration ->
            val safeDuration = duration.coerceAtLeast(0)
            binding.trackSeekBar.max = safeDuration
            binding.trackSeekBar.isEnabled = safeDuration > 0
            binding.trackTotalTime.text = formatDuration(safeDuration.toLong())
        }

        playerViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            binding.trackPlayPause.setImageResource(
                if (playing == true) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
            )
            binding.trackPlayPause.contentDescription = getString(
                if (playing == true) R.string.pausePlayback else R.string.resumePlayback
            )
        }

        playerViewModel.isShuffleEnabled.observe(viewLifecycleOwner) { enabled ->
            val active = enabled == true
            binding.trackShuffle.setImageResource(
                if (active) R.drawable.baseline_shuffle_on_24 else R.drawable.baseline_shuffle_24
            )
            binding.trackShuffle.contentDescription = getString(
                if (active) R.string.shufflePlaybackEnabled else R.string.shufflePlaybackDisabled
            )
        }

        favoritesViewModel.likedSongPaths.observe(viewLifecycleOwner) {
            updateLikeButton(currentSongPath)
        }
    }

    private fun updateLikeButton(songPath: String?) {
        val isLiked = songPath != null && favoritesViewModel.isLiked(songPath)
        binding.trackLike.setImageResource(
            if (isLiked) R.drawable.baseline_favourite else R.drawable.baseline_favourite_border_24
        )
        binding.trackLike.contentDescription = getString(
            if (isLiked) R.string.unlikeTrack else R.string.likeTrack
        )
    }

    private fun bindSong(song: Song) {
        currentSongPath = song.songPath
        binding.trackTitle.text = song.title.ifBlank { getString(R.string.noTrackPlaying) }
        binding.trackArtist.text = song.artist.ifBlank { getString(R.string.undefined) }
        binding.trackSeekBar.progress = 0
        binding.trackCurrentTime.text = formatDuration(0)
        binding.trackTotalTime.text = formatDuration(song.duration.coerceAtLeast(0))
        binding.trackSeekBar.max = song.duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(0)
        loadAlbumArt(song, binding.trackCover)
        updateLikeButton(song.songPath)
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
