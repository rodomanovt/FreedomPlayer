package com.rodomanovt.freedomplayer.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var activityLaunchIntent: Intent? = null

        activityLaunchIntent = Intent(this, PlayerActivity::class.java)

        while (true) {
            startActivity(activityLaunchIntent)
        }

        // start DownloaderActivity or SettingsActivity according to default startup page setting
    }
}