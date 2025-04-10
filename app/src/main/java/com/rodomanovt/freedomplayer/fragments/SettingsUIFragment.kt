package com.rodomanovt.freedomplayer.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.FragmentSettingsUiBinding
import com.rodomanovt.freedomplayer.viewmodels.SettingsUIViewModel

class SettingsUIFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsUIFragment()
    }

    private val viewModel: SettingsUIViewModel by viewModels()
    private lateinit var binding: FragmentSettingsUiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_ui, container, false)
        return binding.root
    }
}