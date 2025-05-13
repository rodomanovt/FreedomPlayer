package com.rodomanovt.freedomplayer.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.model.Song

class SongsAdapter(
    private val onSongClick: (Song) -> Unit
) : ListAdapter<Song, SongsAdapter.ViewHolder>(SongComparator) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onSongClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.song_title)
        private val artistView: TextView = itemView.findViewById(R.id.song_artist)
        private val durationView: TextView = itemView.findViewById(R.id.song_duration)
        private val albumArtView: ImageView = itemView.findViewById(R.id.song_cover)

        fun bind(song: Song, onSongClick: (Song) -> Unit) {
            titleView.text = song.title
            artistView.text = song.artist
            durationView.text = formatDuration(song.duration)
            albumArtView.setImageBitmap(loadAlbumArt(song.path))

            itemView.setOnClickListener { onSongClick(song) }
        }

        private fun formatDuration(millis: Long): String {
            val seconds = (millis / 1000) % 60
            val minutes = (millis / (1000 * 60)) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

        private fun loadAlbumArt(songPath: String): Bitmap? {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(songPath)
                val artBytes = retriever.embeddedPicture
                BitmapFactory.decodeByteArray(artBytes, 0, artBytes!!.size.also { retriever.release() })
            } catch (e: Exception) {
                retriever.release()
                null
            }
        }
    }

    object SongComparator : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }
}