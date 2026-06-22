package com.rodomanovt.freedomplayer.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.rodomanovt.freedomplayer.adapters.SongsAdapter
import com.rodomanovt.freedomplayer.adapters.PlaylistHeaderAdapter
import com.rodomanovt.freedomplayer.databinding.FragmentSongsBinding
import com.rodomanovt.freedomplayer.viewmodels.FavoritesViewModel
import com.rodomanovt.freedomplayer.viewmodels.MediaPlayerViewModel
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel

class SongsFragment : Fragment() {
    private lateinit var binding: FragmentSongsBinding
    private lateinit var adapter: SongsAdapter
    private lateinit var headerAdapter: PlaylistHeaderAdapter
    private val viewModel: MusicViewModel by viewModels()
    private val playerViewModel: MediaPlayerViewModel by activityViewModels()
    private val favoritesViewModel: FavoritesViewModel by activityViewModels()
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
        setupObservers()
        loadSongs()
    }

    private fun setupUI() {
        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        headerAdapter = PlaylistHeaderAdapter(
            onPlayClick = { songs ->
                if (songs.isNotEmpty()) {
                    playerViewModel.setShuffleEnabled(requireContext(), false)
                    playerViewModel.setQueue(requireContext(), songs, 0)
                }
            },
            onShufflePlayClick = { songs ->
                if (songs.isNotEmpty()) {
                    playerViewModel.setShuffleEnabled(requireContext(), true)
                    playerViewModel.setQueue(requireContext(), songs, 0, startFromRandom = true)
                }
            }
        )

        arguments?.getString("folderUri")?.let { uriString ->
            val folderUri = Uri.parse(uriString)
            playlistName = getPlaylistNameFromUri(folderUri)
            headerAdapter.submitPlaylist(playlistName, emptyList())
        }

        adapter = SongsAdapter { song ->
            val allSongs = viewModel.songs.value ?: return@SongsAdapter
            playerViewModel.setQueue(requireContext(), allSongs, allSongs.indexOf(song))
        }

        binding.recyclerViewSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ConcatAdapter(headerAdapter, this@SongsFragment.adapter)
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun loadSongs() {
        arguments?.getString("folderUri")?.let { uriString ->
            val folderUri = Uri.parse(uriString)
            viewModel.loadSongs(folderUri)
        }
    }

    private fun setupObservers() {
        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            adapter.submitList(songs)
            headerAdapter.submitPlaylist(playlistName, songs)
            //binding.emptyStateView.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
        }

        playerViewModel.isShuffleEnabled.observe(viewLifecycleOwner) { enabled ->
            headerAdapter.setShuffleEnabled(enabled == true)
        }

        favoritesViewModel.likedSongPaths.observe(viewLifecycleOwner) { likedPaths ->
            adapter.updateLikedSongs(likedPaths)
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
