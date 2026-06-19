package com.rodomanovt.freedomplayer.adapters

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancelChildren
import java.util.concurrent.ConcurrentHashMap

class SongsAdapter(
    private val onSongClick: (Song) -> Unit
) : ListAdapter<Song, SongsAdapter.ViewHolder>(SongDiffCallback) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val previewCache = ConcurrentHashMap<String, SongPreview>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.song_title)
        private val artistView: TextView = itemView.findViewById(R.id.song_artist)
        private val durationView: TextView = itemView.findViewById(R.id.song_duration)
        private val albumArtView: ImageView = itemView.findViewById(R.id.song_cover)

        fun bind(song: Song) {
            titleView.text = song.title
            artistView.text = song.artist.ifBlank { "Неизвестный исполнитель" }
            durationView.text = if (song.duration > 0) formatDuration(song.duration) else "--:--"
            albumArtView.setImageResource(R.drawable.baseline_music_note_24)
            itemView.tag = song.songPath

            Log.d("SongsAdapter", "Loaded ${song.songPath}")
        }

        private fun formatDuration(millis: Long): String {
            val seconds = (millis / 1000) % 60
            val minutes = (millis / 1000 / 60) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song)
        holder.itemView.setOnClickListener { onSongClick(song) }
        loadPreviewIfNeeded(holder, song)

        Log.d("SongsAdapter", "Binding song at position $position")
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scope.coroutineContext.cancelChildren()
    }

    private fun loadPreviewIfNeeded(holder: ViewHolder, song: Song) {
        val cachedPreview = previewCache[song.songPath]
        if (cachedPreview != null) {
            applyPreview(holder, song.songPath, cachedPreview)
            return
        }

        scope.launch {
            val preview = withContext(Dispatchers.IO) { readPreview(holder.itemView.context, song.songPath) }
            previewCache[song.songPath] = preview
            applyPreview(holder, song.songPath, preview)
        }
    }

    private fun applyPreview(holder: ViewHolder, songPath: String, preview: SongPreview) {
        if (holder.itemView.tag != songPath) return

        val artistView = holder.itemView.findViewById<TextView>(R.id.song_artist)
        val durationView = holder.itemView.findViewById<TextView>(R.id.song_duration)
        val albumArtView = holder.itemView.findViewById<ImageView>(R.id.song_cover)

        if (preview.artist.isNotBlank()) {
            artistView.text = preview.artist
        }

        if (preview.duration > 0) {
            durationView.text = formatDuration(preview.duration)
        }

        preview.artBytes?.let { artBytes ->
            Glide.with(holder.itemView.context)
                .load(artBytes)
                .placeholder(R.drawable.baseline_music_note_24)
                .into(albumArtView)
        }
    }

    private fun readPreview(context: android.content.Context, songPath: String): SongPreview {
        val retriever = MediaMetadataRetriever()
        return try {
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(songPath), "r")
            pfd?.use {
                retriever.setDataSource(it.fileDescriptor)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { value -> value.isNotBlank() }
                    ?: ""
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val artBytes = retriever.embeddedPicture
                SongPreview(duration = duration, artist = artist, artBytes = artBytes)
            } ?: SongPreview()
        } catch (e: Exception) {
            Log.e("SongsAdapter", "Error loading preview for $songPath", e)
            SongPreview()
        } finally {
            retriever.release()
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 1000 / 60) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    object SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }

    data class SongPreview(
        val duration: Long = 0L,
        val artist: String = "",
        val artBytes: ByteArray? = null
    )
}
