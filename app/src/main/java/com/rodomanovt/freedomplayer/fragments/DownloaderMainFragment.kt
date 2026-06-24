package com.rodomanovt.freedomplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.adapters.DownloaderPlaylistAdapter
import com.rodomanovt.freedomplayer.helpers.ToastLog
import com.rodomanovt.freedomplayer.databinding.DialogAddDownloaderPlaylistBinding
import com.rodomanovt.freedomplayer.databinding.DialogDownloadTrackBinding
import com.rodomanovt.freedomplayer.databinding.FragmentDownloaderMainBinding
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import com.rodomanovt.freedomplayer.viewmodels.DownloaderMainViewModel
import com.rodomanovt.freedomplayer.viewmodels.TrackDownloadState

class DownloaderMainFragment : Fragment() {

    companion object {
        private const val LOG_TAG = "DownloaderMainFragment"
    }

    private var _binding: FragmentDownloaderMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DownloaderMainViewModel by viewModels()
    private lateinit var adapter: DownloaderPlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloaderMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DownloaderPlaylistAdapter(
            onDownloadClick = { playlist -> viewModel.scanPlaylistForDownload(playlist) },
            onMenuClick = { playlist, anchor -> showPlaylistMenu(playlist, anchor) }
        )

        binding.recyclerViewPlaylists.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DownloaderMainFragment.adapter
        }

        binding.addButton.setOnClickListener {
            showAddPlaylistDialog()
        }

        binding.downloadTrackButton.setOnClickListener {
            showDownloadTrackDialog()
        }

        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            adapter.submitList(playlists)
        }

        viewModel.playlistMessage.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            ToastLog.show(requireContext(), message, tag = LOG_TAG)
            viewModel.clearPlaylistMessage()
        }

        viewModel.trackDownloadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                TrackDownloadState.Idle -> Unit
                TrackDownloadState.UpdatingYtDlp -> {
                    ToastLog.show(requireContext(), R.string.ytdlp_updating, tag = LOG_TAG)
                }
                TrackDownloadState.InProgress -> {
                    ToastLog.show(requireContext(), R.string.download_started, tag = LOG_TAG)
                }
                TrackDownloadState.Success -> {
                    ToastLog.show(requireContext(), R.string.download_success, Toast.LENGTH_LONG, LOG_TAG)
                    viewModel.resetTrackDownloadState()
                }
                is TrackDownloadState.Error -> {
                    ToastLog.show(
                        requireContext(),
                        R.string.download_failed,
                        Toast.LENGTH_LONG,
                        LOG_TAG,
                        state.message
                    )
                    viewModel.resetTrackDownloadState()
                }
            }
        }
    }

    private fun showDownloadTrackDialog() {
        val dialogBinding = DialogDownloadTrackBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.download_track_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.download_playlist, null)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .apply {
                setOnShowListener {
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val url = dialogBinding.editTextTrackUrl.text?.toString().orEmpty()
                        if (url.isBlank()) {
                            ToastLog.show(requireContext(), R.string.track_url_required, tag = LOG_TAG)
                            return@setOnClickListener
                        }

                        viewModel.downloadTrack(url)
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun showPlaylistMenu(playlist: DownloaderPlaylist, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.downloader_playlist_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_playlist -> {
                        showEditPlaylistDialog(playlist)
                        true
                    }
                    R.id.action_delete_playlist -> {
                        showDeletePlaylistDialog(playlist)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showAddPlaylistDialog() {
        showPlaylistDialog(
            titleRes = R.string.add_playlist_title,
            positiveButtonRes = R.string.add,
            onSubmit = { name, url, autoUpdate ->
                viewModel.addPlaylist(name, url, autoUpdate)
            }
        )
    }

    private fun showEditPlaylistDialog(playlist: DownloaderPlaylist) {
        showPlaylistDialog(
            titleRes = R.string.edit_playlist_title,
            positiveButtonRes = R.string.save,
            initialName = playlist.name,
            initialUrl = playlist.url,
            initialAutoUpdate = playlist.autoUpdate,
            onSubmit = { name, url, autoUpdate ->
                viewModel.updatePlaylist(playlist.id, name, url, autoUpdate)
            }
        )
    }

    private fun showDeletePlaylistDialog(playlist: DownloaderPlaylist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_playlist_confirm_title)
            .setMessage(getString(R.string.delete_playlist_confirm_message, playlist.name))
            .setPositiveButton(R.string.delete_playlist) { _, _ ->
                viewModel.deletePlaylist(playlist.id)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showPlaylistDialog(
        titleRes: Int,
        positiveButtonRes: Int,
        initialName: String = "",
        initialUrl: String = "",
        initialAutoUpdate: Boolean = false,
        onSubmit: (name: String, url: String, autoUpdate: Boolean) -> Unit
    ) {
        val dialogBinding = DialogAddDownloaderPlaylistBinding.inflate(layoutInflater)
        dialogBinding.editTextPlaylistName.setText(initialName)
        dialogBinding.editTextPlaylistUrl.setText(initialUrl)
        dialogBinding.checkBoxAutoUpdate.isChecked = initialAutoUpdate

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(dialogBinding.root)
            .setPositiveButton(positiveButtonRes, null)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .apply {
                setOnShowListener {
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = dialogBinding.editTextPlaylistName.text?.toString().orEmpty()
                        val url = dialogBinding.editTextPlaylistUrl.text?.toString().orEmpty()
                        val autoUpdate = dialogBinding.checkBoxAutoUpdate.isChecked

                        if (name.isBlank() || url.isBlank()) {
                            ToastLog.show(requireContext(), R.string.playlist_fields_required, tag = LOG_TAG)
                            return@setOnClickListener
                        }

                        onSubmit(name, url, autoUpdate)
                        dismiss()
                    }
                }
                show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
