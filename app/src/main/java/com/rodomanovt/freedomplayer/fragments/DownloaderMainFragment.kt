package com.rodomanovt.freedomplayer.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rodomanovt.freedomplayer.viewmodels.DownloaderMainViewModel
import com.rodomanovt.freedomplayer.R

class DownloaderMainFragment : Fragment() {

    companion object {
        fun newInstance() = DownloaderMainFragment()
    }

    private val viewModel: DownloaderMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_downloader_main, container, false)
    }
}