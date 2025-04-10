package com.rodomanovt.freedomplayer.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.FragmentSettingsDownloaderBinding
import com.rodomanovt.freedomplayer.viewmodels.SettingsDownloaderViewModel

class SettingsDownloaderFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsDownloaderFragment()
    }

    private val viewModel: SettingsDownloaderViewModel by viewModels()
    private lateinit var binding: FragmentSettingsDownloaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_downloader, container, false)
        return binding.root
    }
}