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
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.activities.MainActivity
import com.rodomanovt.freedomplayer.adapters.SongsAdapter
import com.rodomanovt.freedomplayer.databinding.FragmentSongsBinding
import com.rodomanovt.freedomplayer.repos.MusicRepository
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel
import com.rodomanovt.freedomplayer.viewmodels.SettingsDownloaderViewModel

class SongsFragment : Fragment() {
    private lateinit var binding: FragmentSongsBinding
    private lateinit var adapter: SongsAdapter
    private val viewModel: MusicViewModel by viewModels()
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

        binding.backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        arguments?.getString("folderUri")?.let { uriString ->
            val folderUri = Uri.parse(uriString)
            playlistName = getPlaylistNameFromUri(folderUri)
            binding.playlistNameText.text = playlistName
        }

        setupRecyclerView()
        loadSongs()
        setupObservers() // ✅ НЕ ЗАБУДИТЕ ЭТОТ МЕТОД
    }

    private fun getPlaylistNameFromUri(uri: Uri): String {
        return uri.lastPathSegment?.split("/")?.lastOrNull() ?: "Плейлист"
    }

    private fun setupRecyclerView() {
        adapter = SongsAdapter { song ->
            // Логика при нажатии на песню
        }
        binding.recyclerViewSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SongsFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
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
    }

    private fun showEmptyState(show: Boolean) {
        binding.apply {
            recyclerViewSongs.visibility = if (show) View.GONE else View.VISIBLE
            //emptyStateView.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}