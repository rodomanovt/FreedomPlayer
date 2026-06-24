package com.rodomanovt.freedomplayer

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.rodomanovt.freedomplayer.helpers.YtDlpManager

class FreedomPlayerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            YtDlpManager.startBackgroundUpdate(this)
        } catch (e: YoutubeDLException) {
            Log.e(TAG, "Failed to initialize yt-dlp", e)
        }
    }

    companion object {
        private const val TAG = "FreedomPlayerApplication"
    }
}
