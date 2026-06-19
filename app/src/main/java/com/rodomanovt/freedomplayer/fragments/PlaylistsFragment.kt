package com.rodomanovt.freedomplayer.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.adapters.PlaylistAdapter
import com.rodomanovt.freedomplayer.databinding.FragmentPlaylistsBinding
import com.rodomanovt.freedomplayer.helpers.PrefsHelper
import com.rodomanovt.freedomplayer.model.Playlist
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel
import com.rodomanovt.freedomplayer.viewmodels.SettingsDownloaderViewModel

class PlaylistsFragment : Fragment() {
    private lateinit var binding: FragmentPlaylistsBinding
    private val viewModel: MusicViewModel by viewModels()
    private lateinit var adapter: PlaylistAdapter
    private var rootFolderUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PlaylistAdapter() { playlist ->
            findNavController().navigate(
                R.id.action_playlistsFragment_to_songsFragment,
                bundleOf("folderUri" to playlist.folderUri.toString())
            )
        }

        binding.recyclerViewPlaylists.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            adapter = this@PlaylistsFragment.adapter
        }

        rootFolderUri = PrefsHelper(requireContext()).getRootFolderUri()

        binding.buttonReindexPlaylists.setOnClickListener {
            val uri = rootFolderUri
            if (uri != null) {
                Toast.makeText(requireContext(), "Переиндексация запущена", Toast.LENGTH_SHORT).show()
                viewModel.reindexAllPlaylists(uri)
            } else {
                Toast.makeText(requireContext(), "Выберите корневую папку", Toast.LENGTH_SHORT).show()
            }
        }

        rootFolderUri?.let { uri ->
            Log.d("PlaylistsFragment", "Загружаем существующие и новые плейлисты...")
            viewModel.loadExistingAndCheckForNewPlaylists(uri)

            viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
                Log.d("PlaylistsFragment", "Получено плейлистов: ${playlists.size}")
                adapter.submitList(playlists)
            }

            viewModel.isReindexing.observe(viewLifecycleOwner) { isReindexing ->
                binding.buttonReindexPlaylists.isEnabled = !isReindexing
                binding.reindexProgress.visibility = if (isReindexing) View.VISIBLE else View.GONE
            }
        } ?: run {
            Log.e("PlaylistsFragment", "Корневая папка не задана")
            Toast.makeText(requireContext(), "Выберите корневую папку", Toast.LENGTH_LONG).show()
        }
    }
}
