package com.rodomanovt.freedomplayer.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.ActivityDownloaderBinding

class DownloaderActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    private lateinit var binding: ActivityDownloaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDownloaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem):Boolean {

        var activityLaunchIntent: Intent? = null

        val id = item.getItemId()
        if (id == R.id.musicPlayer)
        {
            activityLaunchIntent = Intent(this, PlayerActivity::class.java)
        }
        else if (id == R.id.downloader)
        {
            // current
        }
        else if (id == R.id.settings)
        {
            activityLaunchIntent = Intent(this, SettingsActivity::class.java)
        }
        val drawer = binding.drawerLayout
        drawer.closeDrawer(GravityCompat.START)

        if (activityLaunchIntent != null) {
            startActivity(activityLaunchIntent)
        }

        return true
    }
}