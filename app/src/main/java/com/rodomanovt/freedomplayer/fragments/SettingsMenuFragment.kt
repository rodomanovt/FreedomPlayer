package com.rodomanovt.freedomplayer.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.FragmentSettingsMenuBinding

class SettingsMenuFragment : Fragment() {

    private lateinit var binding: FragmentSettingsMenuBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_menu, container, false)

        binding.settingsPlayerBtn.setOnClickListener{
            binding.root.findNavController().navigate(R.id.action_settingsMenuFragment_to_settingsPlayerFragment)
        }

        binding.settingsDownloaderBtn.setOnClickListener{
            binding.root.findNavController().navigate(R.id.action_settingsMenuFragment_to_settingsDownloaderFragment)
        }

        binding.settingsUIBtn.setOnClickListener{
            binding.root.findNavController().navigate(R.id.action_settingsMenuFragment_to_settingsUIFragment)
        }


        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsMenuFragment().apply {

            }
    }
}