package com.rodomanovt.freedomplayer.adapters

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class PlaylistHeaderAdapter(
    private val onPlayClick: (List<Song>) -> Unit,
    private val onShufflePlayClick: (List<Song>) -> Unit
) : RecyclerView.Adapter<PlaylistHeaderAdapter.ViewHolder>() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val artworkCache = ConcurrentHashMap<String, SongArtwork>()
    private var playlistName: String = ""
    private var songs: List<Song> = emptyList()
    private var isShuffleEnabled: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_header, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(playlistName, songs)
    }

    override fun getItemCount(): Int = 1

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scope.coroutineContext.cancelChildren()
    }

    fun submitPlaylist(name: String, songs: List<Song>) {
        playlistName = name
        this.songs = songs
        notifyDataSetChanged()
    }

    fun setShuffleEnabled(enabled: Boolean) {
        if (isShuffleEnabled == enabled) return
        isShuffleEnabled = enabled
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverTopLeft: ImageView = itemView.findViewById(R.id.coverTopLeft)
        private val coverTopRight: ImageView = itemView.findViewById(R.id.coverTopRight)
        private val coverBottomLeft: ImageView = itemView.findViewById(R.id.coverBottomLeft)
        private val coverBottomRight: ImageView = itemView.findViewById(R.id.coverBottomRight)
        private val titleView: TextView = itemView.findViewById(R.id.textPlaylistTitle)
        private val playButton: ImageButton = itemView.findViewById(R.id.buttonPlayPlaylist)
        private val shuffleButton: ImageButton = itemView.findViewById(R.id.buttonShufflePlaylist)

        fun bind(name: String, songs: List<Song>) {
            titleView.text = name
            val hasSongs = songs.isNotEmpty()
            playButton.isEnabled = hasSongs
            shuffleButton.isEnabled = hasSongs
            shuffleButton.setImageResource(
                if (isShuffleEnabled) R.drawable.baseline_shuffle_on_24 else R.drawable.baseline_shuffle_24
            )
            shuffleButton.contentDescription = itemView.context.getString(
                if (isShuffleEnabled) R.string.shufflePlaybackEnabled else R.string.shufflePlaybackDisabled
            )
            playButton.setOnClickListener { onPlayClick(songs) }
            shuffleButton.setOnClickListener { onShufflePlayClick(songs) }

            val coverViews = listOf(coverTopLeft, coverTopRight, coverBottomLeft, coverBottomRight)
            coverViews.forEach { view ->
                view.tag = null
                view.setImageResource(R.drawable.baseline_queue_music_24)
            }

            songs.take(4).forEachIndexed { index, song ->
                bindCover(coverViews[index], song.songPath)
            }
        }

        private fun bindCover(imageView: ImageView, songPath: String) {
            imageView.tag = songPath
            imageView.setImageResource(R.drawable.baseline_queue_music_24)

            val cachedArtwork = artworkCache[songPath]
            if (cachedArtwork != null) {
                applyArtwork(imageView, songPath, cachedArtwork)
                return
            }

            scope.launch {
                val artwork = withContext(Dispatchers.IO) { readArtwork(itemView.context, songPath) }
                artworkCache[songPath] = artwork
                applyArtwork(imageView, songPath, artwork)
            }
        }

        private fun applyArtwork(imageView: ImageView, songPath: String, artwork: SongArtwork) {
            if (imageView.tag != songPath) return

            val bytes = artwork.bytes
            if (bytes != null) {
                Glide.with(imageView)
                    .load(bytes)
                    .centerCrop()
                    .placeholder(R.drawable.baseline_queue_music_24)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.baseline_queue_music_24)
            }
        }
    }

    private fun readArtwork(context: Context, songPath: String): SongArtwork {
        val retriever = MediaMetadataRetriever()
        return try {
            when {
                songPath.startsWith("content://") -> {
                    context.contentResolver.openFileDescriptor(Uri.parse(songPath), "r")?.use {
                        retriever.setDataSource(it.fileDescriptor)
                    }
                }

                songPath.startsWith("/") -> retriever.setDataSource(songPath)

                else -> {
                    context.contentResolver.openFileDescriptor(Uri.parse(songPath), "r")?.use {
                        retriever.setDataSource(it.fileDescriptor)
                    }
                }
            }

            SongArtwork(retriever.embeddedPicture)
        } catch (_: Exception) {
            SongArtwork(null)
        } finally {
            retriever.release()
        }
    }

    data class SongArtwork(val bytes: ByteArray?)
}
