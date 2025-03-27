package com.rodomanovt.freedomplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rodomanovt.freedomplayer.databinding.ActivityDownloaderBinding

class DownloaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDownloaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}