package com.rodomanovt.freedomplayer.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_ui, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arrayThemes = resources.getStringArray(R.array.themesSettingSpinner)
        binding.settingThemeSpinner.adapter = ArrayAdapter<String>(requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            arrayThemes)

        binding.settingThemeSpinner.onItemSelectedListener = object  : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                Toast.makeText(context, "selected ${arrayThemes[position]}", Toast.LENGTH_SHORT).show()
                // TODO: сохранить настроку в бд (вынести во viewmodel)
                // TODO: реализовать функционал (вынести во viewmodel)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }


        val arrayLanguages = resources.getStringArray(R.array.languageSettingSpinner)
        binding.settingLanguageSpinner.adapter = ArrayAdapter<String>(requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            arrayLanguages)

        binding.settingLanguageSpinner.onItemSelectedListener = object  : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                Toast.makeText(context, "selected ${arrayLanguages[position]}", Toast.LENGTH_SHORT).show()
                // TODO: сохранить настройку в бд (вынести во viewmodel)
                // TODO: реализовать функционал (вынести во viewmodel)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }


        val arrayActivities = resources.getStringArray(R.array.defaultActivitySettingSpinner)
        binding.settingDefaultActivitySpinner.adapter = ArrayAdapter<String>(requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            arrayActivities)

        binding.settingDefaultActivitySpinner.onItemSelectedListener = object  : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                Toast.makeText(context, "selected ${arrayActivities[position]}", Toast.LENGTH_SHORT).show()
                // TODO: сохранить настройку в бд (вынести во viewmodel)
                // TODO: реализовать функционал (вынести во viewmodel)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }




    }
}