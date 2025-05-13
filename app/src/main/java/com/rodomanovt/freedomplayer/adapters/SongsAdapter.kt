package com.rodomanovt.freedomplayer.adapters

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.model.Song

class SongsAdapter(
    private var songs: List<Song>,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<SongsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.song_title)
        private val artistView: TextView = itemView.findViewById(R.id.song_artist)
        private val durationView: TextView = itemView.findViewById(R.id.song_duration)
        private val albumArtView: ImageView = itemView.findViewById(R.id.song_cover)


        fun bind(song: Song) {
            titleView.text = song.title
            artistView.text = song.artist
            durationView.text = formatDuration(song.duration)

            // Загрузка обложки
            loadAlbumArt(song.path)

            itemView.setOnClickListener { onSongClick(song) }
        }

        private fun formatDuration(millis: Long): String {
            val seconds = (millis / 1000) % 60
            val minutes = (millis / (1000 * 60)) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

        private fun loadAlbumArt(songPath: String) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(songPath)
                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    albumArtView.setImageBitmap(bitmap)
                } else {
                    albumArtView.setImageResource(R.drawable.baseline_music_note_24)
                }
            } catch (e: Exception) {
                albumArtView.setImageResource(R.drawable.baseline_music_note_24)
            } finally {
                retriever.release()
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size

    fun updateData(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }
}