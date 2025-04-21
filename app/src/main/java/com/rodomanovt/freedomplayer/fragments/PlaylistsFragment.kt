package com.rodomanovt.freedomplayer.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("test", "1")
    }

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


//        binding.recyclerViewPlaylists.layoutManager = LinearLayoutManager(requireContext())
//        binding.recyclerViewPlaylists.adapter = PlaylistAdapter(emptyList()) { playlist ->
//            findNavController().navigate(
//                R.id.action_playlistsFragment_to_songsFragment,
//                bundleOf("folderUri" to playlist.folderUri.toString())
//            )
//        }
//
//        // Загрузка данных
//        val uri = PrefsHelper(requireContext()).getRootFolderUri()
//        if (uri != null) {
//            viewModel.loadPlaylists(uri) // Загружаем данные
//            viewModel.playlists.observe(viewLifecycleOwner) { playlists -> // ✅ Теперь observe работает!
//                (binding.recyclerViewPlaylists.adapter as? PlaylistAdapter)?.updateData(playlists)
//            }
//        }

        // 1. Инициализация адаптера с пустым списком
        adapter = PlaylistAdapter(emptyList()) { playlist ->
            findNavController().navigate(
                R.id.action_playlistsFragment_to_songsFragment,
                bundleOf("folderUri" to playlist.folderUri.toString())
            )
        }

        // 2. Настройка RecyclerView
        binding.recyclerViewPlaylists.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapter // Используем наш адаптер
            setHasFixedSize(true) // Оптимизация, если размер элементов фиксирован
        }

        // 3. Загрузка данных
        PrefsHelper(requireContext()).getRootFolderUri()?.let { uri ->
            viewModel.loadPlaylists(uri)
            viewModel.playlists.observe(viewLifecycleOwner) { playlists: List<Playlist> ->
                adapter.updateData(playlists)
                binding.recyclerViewPlaylists.adapter = adapter
            }
        }

    }
}