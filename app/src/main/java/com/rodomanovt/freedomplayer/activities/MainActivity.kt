package com.rodomanovt.freedomplayer.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rodomanovt.freedomplayer.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var activityLaunchIntent: Intent? = null

        activityLaunchIntent = Intent(this, DownloaderActivity::class.java)

        startActivity(activityLaunchIntent)

        // start DownloaderActivity or SettingsActivity according to default startup page setting
    }
}