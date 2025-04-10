package com.rodomanovt.freedomplayer.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.FragmentSettingsPlayerBinding
import com.rodomanovt.freedomplayer.viewmodels.SettingsPlayerViewModel

class SettingsPlayerFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsPlayerFragment()
    }

    private val viewModel: SettingsPlayerViewModel by viewModels()
    private lateinit var binding: FragmentSettingsPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_player, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}