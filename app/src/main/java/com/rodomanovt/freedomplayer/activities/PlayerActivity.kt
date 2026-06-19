package com.rodomanovt.freedomplayer.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityPlayerBinding
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navView.setNavigationItemSelectedListener(this)
        requestNotificationPermissionIfNeeded()
    }

    override fun onNavigationItemSelected(item: MenuItem):Boolean {

        var activityLaunchIntent: Intent? = null

        val id = item.getItemId()
        if (id == R.id.musicPlayer)
        {
            // current
        }
        else if (id == R.id.downloader)
        {
            activityLaunchIntent = Intent(this, DownloaderActivity::class.java)
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

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
