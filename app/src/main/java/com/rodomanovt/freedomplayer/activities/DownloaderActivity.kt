package com.rodomanovt.freedomplayer.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.ActivityDownloaderBinding
import com.rodomanovt.freedomplayer.helpers.DownloadLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloaderActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    private lateinit var binding: ActivityDownloaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDownloaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navView.setNavigationItemSelectedListener(this)

        setupConsole()
    }

    private fun setupConsole() {
        binding.btnToggleConsole.setOnClickListener {
            binding.consoleContainer.visibility = View.VISIBLE
            updateConsoleText(DownloadLogger.logs.value)
        }

        binding.btnCloseConsole.setOnClickListener {
            binding.consoleContainer.visibility = View.GONE
        }

        lifecycleScope.launch {
            DownloadLogger.logs
                .collect { logs ->
                if (binding.consoleContainer.visibility == View.VISIBLE) {
                    updateConsoleText(logs)
                }
            }
        }
    }

    private var lastUpdateJob: kotlinx.coroutines.Job? = null
    private fun updateConsoleText(logs: List<String>) {
        lastUpdateJob?.cancel()
        lastUpdateJob = lifecycleScope.launch {
            // Conflate updates to avoid hammering the UI thread
            val text = withContext(kotlinx.coroutines.Dispatchers.Default) {
                logs.joinToString("\n")
            }
            binding.consoleText.text = text
            binding.consoleScroll.post {
                binding.consoleScroll.fullScroll(View.FOCUS_DOWN)
            }
        }
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