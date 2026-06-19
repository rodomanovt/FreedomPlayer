package com.rodomanovt.freedomplayer.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(Intent(this, PlayerActivity::class.java))
        finish()

        // start DownloaderActivity or SettingsActivity according to default startup page setting
    }
}
