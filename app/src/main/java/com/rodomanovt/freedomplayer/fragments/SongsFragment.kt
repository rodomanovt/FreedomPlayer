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
            playerViewModel.playSong(requireContext(), song)
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
            Log.d("SongsFragment", "Получено песен: ${songs.size}")
            if (songs.isEmpty()) {
                showEmptyState(true)
            } else {
                showEmptyState(false)
                adapter.submitList(songs)
            }
        }

        playerViewModel.currentSong.observe(viewLifecycleOwner) { song ->
            if (song != null) {
                updateBottomPlayer(song)
                binding.playerCard.visibility = View.VISIBLE
            } else {
                binding.playerCard.visibility = View.GONE
            }
        }
    }

    private fun updateBottomPlayer(song: Song) {
        binding.playerTitle.text = song.title
        binding.playerArtist.text = song.artist

//        binding.playerCard.setOnClickListener {
//            findNavController().navigate(R.id.action_songsFragment_to_playerFragment)
//        }

        binding.playerPlayPause.setOnClickListener {
            if (binding.playerPlayPause.tag == "playing") {
                binding.playerPlayPause.setImageResource(R.drawable.baseline_arrow_right_24)
                binding.playerPlayPause.tag = "paused"
                playerViewModel.stopCurrentSong()
            } else {
                binding.playerPlayPause.setImageResource(R.drawable.baseline_arrow_circle_down_24)
                binding.playerPlayPause.tag = "playing"
            }
        }
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