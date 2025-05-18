package com.rodomanovt.freedomplayer.fragments

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.activities.MainActivity
import com.rodomanovt.freedomplayer.adapters.SongsAdapter
import com.rodomanovt.freedomplayer.databinding.FragmentSongsBinding
import com.rodomanovt.freedomplayer.model.Song
import com.rodomanovt.freedomplayer.repos.MusicRepository
import com.rodomanovt.freedomplayer.viewmodels.MediaPlayerViewModel
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel.Companion.loadAlbumArt
import com.rodomanovt.freedomplayer.viewmodels.SettingsDownloaderViewModel

class SongsFragment : Fragment() {
    private lateinit var binding: FragmentSongsBinding
    private lateinit var adapter: SongsAdapter
    private val viewModel: MusicViewModel by viewModels()
    private val playerViewModel: MediaPlayerViewModel by activityViewModels()
    private var playlistName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadSongs()
        setupObservers()
    }

    private fun setupUI() {
        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        arguments?.getString("folderUri")?.let { uriString ->
            val folderUri = Uri.parse(uriString)
            playlistName = getPlaylistNameFromUri(folderUri)
            binding.playlistNameText.text = playlistName
        }

        adapter = SongsAdapter { song ->
            playerViewModel.playOrToggleSong(requireContext(), song)
        }

        binding.recyclerViewSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SongsFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }

        binding.playerCard.visibility = View.GONE
    }

    private fun loadSongs() {
        arguments?.getString("folderUri")?.let { uriString ->
            val folderUri = Uri.parse(uriString)
            val folder = DocumentFile.fromTreeUri(requireContext(), folderUri)
            if (folder != null && folder.canRead()) {
                viewModel.loadSongs(folderUri)
            } else {
                showError("Нет доступа к папке")
            }
        }
    }

    private fun setupObservers() {
        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            adapter.submitList(songs)
            //binding.emptyStateView.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
        }

        playerViewModel.currentSong.observe(viewLifecycleOwner) { song ->
            song?.let { updateBottomPlayer(it) }
            binding.playerCard.visibility = if (song != null) View.VISIBLE else View.GONE
        }
    }

    private fun updateBottomPlayer(song: Song) {
        binding.playerTitle.text = song.title
        binding.playerArtist.text = song.artist
        loadAlbumArt(song, binding.playerAlbumArt)
        binding.playerPlayPause.setImageResource(R.drawable.baseline_pause_24)
        binding.playerPlayPause.tag = "playing"

//        binding.playerCard.setOnClickListener {
//            // Навигация к полноценному плееру
//            findNavController().navigate(R.id.action_songsFragment_to_playerFragment)
//        }

        binding.playerPlayPause.setOnClickListener {
            if (binding.playerPlayPause.tag == "playing") {
                binding.playerPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
                binding.playerPlayPause.tag = "paused"
                playerViewModel.pause()
            } else {
                binding.playerPlayPause.setImageResource(R.drawable.baseline_pause_24)
                binding.playerPlayPause.tag = "playing"
                playerViewModel.resume()
            }
        }

        playerViewModel.playbackProgress.observe(viewLifecycleOwner) { position ->
            binding.playerProgressBar.progress = position
            binding.playerCurrentTime.text = formatDuration(position.toLong())
        }

        playerViewModel.playbackDuration.observe(viewLifecycleOwner) { duration ->
            binding.playerProgressBar.min = 0
            binding.playerProgressBar.max = duration
        }

        // Обновляем кнопку проигрывания
        playerViewModel.isPlaying.observe(viewLifecycleOwner) { playing ->
            binding.playerPlayPause.setImageResource(
                if (playing == true) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
            )
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun getPlaylistNameFromUri(uri: Uri): String {
        return uri.lastPathSegment?.split("/")?.lastOrNull() ?: "Плейлист"
    }

    private fun showEmptyState(show: Boolean) {
        binding.recyclerViewSongs.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}