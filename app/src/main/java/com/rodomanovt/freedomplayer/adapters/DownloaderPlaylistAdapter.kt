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
    private val onStopDownloadClick: (DownloaderPlaylist) -> Unit,
    private val onMenuClick: (DownloaderPlaylist, View) -> Unit
) : ListAdapter<DownloaderPlaylist, DownloaderPlaylistAdapter.ViewHolder>(Comparator) {

    private val dateFormat = SimpleDateFormat("Обновлено dd.MM.yy HH:mm", Locale.getDefault())
    private var activeDownloadIds: Set<Long> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_downloader_playlist, parent, false)
        return ViewHolder(view, onDownloadClick, onStopDownloadClick, onMenuClick, dateFormat)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), activeDownloadIds)
    }

    fun setActiveDownloadIds(ids: Set<Long>) {
        activeDownloadIds = ids
        notifyDataSetChanged()
    }

    class ViewHolder(
        itemView: View,
        private val onDownloadClick: (DownloaderPlaylist) -> Unit,
        private val onStopDownloadClick: (DownloaderPlaylist) -> Unit,
        private val onMenuClick: (DownloaderPlaylist, View) -> Unit,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.textViewPlaylistName)
        private val lastUpdateView: TextView = itemView.findViewById(R.id.textViewLastUpdate)
        private val autoUpdateView: View = itemView.findViewById(R.id.imageViewAutoUpdate)
        private val downloadButton: ImageButton = itemView.findViewById(R.id.buttonDownload)
        private val progressLayout: View = itemView.findViewById(R.id.layoutDownloadProgress)
        private val stopButton: ImageButton = itemView.findViewById(R.id.buttonStopDownload)
        private val menuButton: ImageButton = itemView.findViewById(R.id.buttonMenu)

        fun bind(playlist: DownloaderPlaylist, activeIds: Set<Long>) {
            nameView.text = playlist.name
            autoUpdateView.isVisible = playlist.autoUpdate
            
            val timestamp = playlist.lastDownloadTimestamp
            if (timestamp != null) {
                lastUpdateView.isVisible = true
                lastUpdateView.text = dateFormat.format(Date(timestamp))
            } else {
                lastUpdateView.isVisible = false
            }

            val isQueuedOrDownloading = activeIds.contains(playlist.id)
            downloadButton.isVisible = !isQueuedOrDownloading
            progressLayout.isVisible = isQueuedOrDownloading
            
            downloadButton.setOnClickListener { onDownloadClick(playlist) }
            stopButton.setOnClickListener { onStopDownloadClick(playlist) }
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
