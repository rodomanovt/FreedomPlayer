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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongsAdapter(
private val onSongClick: (Song) -> Unit
) : ListAdapter<Song, SongsAdapter.ViewHolder>(SongDiffCallback) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.song_title)
        private val artistView: TextView = itemView.findViewById(R.id.song_artist)
        private val durationView: TextView = itemView.findViewById(R.id.song_duration)
        private val albumArtView: ImageView = itemView.findViewById(R.id.song_cover)

        private fun loadAlbumArt(song: Song, imageView: ImageView) {
            CoroutineScope(Dispatchers.IO).launch {
                val retriever = MediaMetadataRetriever()
                try {
                    when {
                        // Для URI content://
                        song.songPath.startsWith("content://") -> {
                            val pfd = imageView.context.contentResolver.openFileDescriptor(
                                Uri.parse(song.songPath), "r"
                            )
                            pfd?.use {
                                retriever.setDataSource(it.fileDescriptor)
                            }
                        }

                        // Для абсолютных путей (устаревший способ)
                        song.songPath.startsWith("/") -> {
                            retriever.setDataSource(song.songPath)
                        }

                        // Для URI DocumentFile
                        else -> {
                            val uri = Uri.parse(song.songPath)
                            val pfd = imageView.context.contentResolver.openFileDescriptor(uri, "r")
                            pfd?.use {
                                retriever.setDataSource(it.fileDescriptor)
                            }
                        }
                    }

                    val artBytes = retriever.embeddedPicture
                    withContext(Dispatchers.Main) {
                        if (artBytes != null) {
                            Glide.with(imageView.context)
                                .load(artBytes)
                                .placeholder(R.drawable.baseline_music_note_24)
                                .into(imageView)
                        } else {
                            imageView.setImageResource(R.drawable.baseline_music_note_24)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MusicPlayer", "Error loading album art", e)
                    withContext(Dispatchers.Main) {
                        imageView.setImageResource(R.drawable.baseline_music_note_24)
                    }
                } finally {
                    retriever.release()
                }
            }
        }

        fun bind(song: Song) {
            titleView.text = song.title
            artistView.text = song.artist
            durationView.text = formatDuration(song.duration)

            loadAlbumArt(song, albumArtView)


            Log.d("SongsAdapter", "Loaded ${song.songPath}")

            //itemView.setOnClickListener { onSongClick(song) }

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
        holder.bind(getItem(position))
        Log.d("SongsAdapter", "Binding song at position $position")
    }

    object SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }
}