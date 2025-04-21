package com.rodomanovt.freedomplayer.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.rodomanovt.freedomplayer.databinding.FragmentSongsBinding
import com.rodomanovt.freedomplayer.repos.MusicRepository
import com.rodomanovt.freedomplayer.viewmodels.MusicViewModel
import com.rodomanovt.freedomplayer.viewmodels.SettingsDownloaderViewModel

class SongsFragment : Fragment() {
    private lateinit var binding: FragmentSongsBinding
    private val viewModel: MusicViewModel by viewModels()


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

//        // Получаем URI папки из аргументов
//        val folderUri = Uri.parse(requireArguments().getString("folderUri") ?: "")
//        val songs = MusicRepository(requireContext()).getSongsFromFolder(folderUri)
//
//        val adapter = SongsAdapter(songs) { song ->
//            // Навигация к плееру с передачей пути к песне
//            findNavController().navigate(
//                R.id.action_songs_to_player,
//                bundleOf("songPath" to song.path)
//            )
//        }
//        binding.recyclerViewSongs.adapter = adapter
    }
}