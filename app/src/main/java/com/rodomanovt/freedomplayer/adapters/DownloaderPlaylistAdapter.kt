package com.rodomanovt.freedomplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.model.DownloaderPlaylist
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloaderPlaylistAdapter(
    private val onDownloadClick: (DownloaderPlaylist) -> Unit,
    private val onMenuClick: (DownloaderPlaylist, View) -> Unit
) : ListAdapter<DownloaderPlaylist, DownloaderPlaylistAdapter.ViewHolder>(Comparator) {

    private val dateFormat = SimpleDateFormat("Обновлено dd.MM.yy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_downloader_playlist, parent, false)
        return ViewHolder(view, onDownloadClick, onMenuClick, dateFormat)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onDownloadClick: (DownloaderPlaylist) -> Unit,
        private val onMenuClick: (DownloaderPlaylist, View) -> Unit,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.textViewPlaylistName)
        private val lastUpdateView: TextView = itemView.findViewById(R.id.textViewLastUpdate)
        private val downloadButton: ImageButton = itemView.findViewById(R.id.buttonDownload)
        private val menuButton: ImageButton = itemView.findViewById(R.id.buttonMenu)

        fun bind(playlist: DownloaderPlaylist) {
            nameView.text = playlist.name
            
            val timestamp = playlist.lastDownloadTimestamp
            if (timestamp != null) {
                lastUpdateView.isVisible = true
                lastUpdateView.text = dateFormat.format(Date(timestamp))
            } else {
                lastUpdateView.isVisible = false
            }

            downloadButton.isEnabled = true
            downloadButton.setOnClickListener { onDownloadClick(playlist) }
            menuButton.setOnClickListener { onMenuClick(playlist, it) }
        }
    }

    companion object {
        private val Comparator = object : DiffUtil.ItemCallback<DownloaderPlaylist>() {
            override fun areItemsTheSame(oldItem: DownloaderPlaylist, newItem: DownloaderPlaylist): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DownloaderPlaylist, newItem: DownloaderPlaylist): Boolean {
                return oldItem == newItem
            }
        }
    }
}
