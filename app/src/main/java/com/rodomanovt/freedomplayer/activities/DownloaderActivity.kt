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

    }

    override fun onNavigationItemSelected(item: MenuItem):Boolean {
        Toast.makeText(this, "1", Toast.LENGTH_SHORT).show()

        var activityLaunchIntent: Intent? = null

        val id = item.getItemId()
        if (id == R.id.musicPlayer)
        {
            activityLaunchIntent = Intent(this, PlayerActivity::class.java)
            Toast.makeText(this, "player", Toast.LENGTH_SHORT).show()
        }
        else if (id == R.id.downloader)
        {
            // current
            Toast.makeText(this, "dl", Toast.LENGTH_SHORT).show()

        }
        else if (id == R.id.settings)
        {
            Toast.makeText(this, "sett", Toast.LENGTH_SHORT).show()
            activityLaunchIntent = Intent(this, SettingsActivity::class.java)
        }
        val drawer = binding.drawerLayout
        drawer.bringToFront()
        drawer.closeDrawer(GravityCompat.START)

        startActivity(activityLaunchIntent)

        return true
    }
}