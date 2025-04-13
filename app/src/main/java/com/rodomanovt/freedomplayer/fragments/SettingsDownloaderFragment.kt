package com.rodomanovt.freedomplayer.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.FragmentSettingsDownloaderBinding
import com.rodomanovt.freedomplayer.viewmodels.SettingsDownloaderViewModel

class SettingsDownloaderFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsDownloaderFragment()
        private const val REQUEST_CODE_OPEN_DIRECTORY = 1
    }

    private val viewModel: SettingsDownloaderViewModel by viewModels()
    private lateinit var binding: FragmentSettingsDownloaderBinding

    private lateinit var directoryUri: Uri


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_downloader, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.settingAutoDownloadSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: сохранить настроку в бд (вынести во viewmodel)
            Toast.makeText(requireContext(), "selected ${isChecked}", Toast.LENGTH_SHORT).show()
            if (isChecked) {
                // TODO: реализовать функционал (вынести во viemodel)
            } else {
            }
        }


        binding.settingMobileNetworkDownloadSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: сохранить настроку в бд (вынести во viewmodel)
            Toast.makeText(requireContext(), "selected ${isChecked}", Toast.LENGTH_SHORT).show()
            if (isChecked) {
                // TODO: реализовать функционал (вынести во viemodel)
            } else {
            }
        }


        binding.settingAutoDownloadStartTimeButton.setOnClickListener {
            Toast.makeText(requireContext(), "changed time", Toast.LENGTH_SHORT).show()
        }


        binding.settingDownloadPathButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)

            if (directoryUri != null) {
//                requireContext().contentResolver.takePersistableUriPermission(
//                    directoryUri,
//                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                )
                Toast.makeText(requireContext(), "changed  to ${directoryUri}", Toast.LENGTH_SHORT).show()
            }
        }


        val arrayFormats = resources.getStringArray(R.array.fileFormatSettingSpinner)
        binding.settingFileFormatSpinner.adapter = ArrayAdapter<String>(requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            arrayFormats)

        binding.settingFileFormatSpinner.onItemSelectedListener = object  : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                Toast.makeText(context, "selected ${arrayFormats[position]}", Toast.LENGTH_SHORT).show()
                // TODO: сохранить настроку в бд (вынести во viewmodel)
                // TODO: реализовать функционал (вынести во viewmodel)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }


        val arrayBitrates = resources.getStringArray(R.array.bitrateSettingSpinner)
        binding.settingBitrateSpinner.adapter = ArrayAdapter<String>(requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            arrayBitrates)

        binding.settingBitrateSpinner.onItemSelectedListener = object  : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                Toast.makeText(context, "selected ${arrayBitrates[position]}", Toast.LENGTH_SHORT).show()
                // TODO: сохранить настроку в бд (вынести во viewmodel)
                // TODO: реализовать функционал (вынести во viewmodel)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }


        binding.settingSmartNaming.setOnCheckedChangeListener { _, isChecked ->
            // TODO: сохранить настроку в бд (вынести во viewmodel)
            Toast.makeText(requireContext(), "selected ${isChecked}", Toast.LENGTH_SHORT).show()
            if (isChecked) {
                // TODO: реализовать функционал (вынести во viemodel)
            } else {
            }
        }


        binding.settingFileNameTemplateEditText.doOnTextChanged { text, start, before, count ->
            Toast.makeText(requireContext(), "template changed to $text", Toast.LENGTH_SHORT).show()
            // TODO: сохранить настроку в бд (вынести во viewmodel)
            // TODO: реализовать функционал (вынести во viewmodel)
        }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Получаем URI выбранной директории
                directoryUri = uri

                // Сохраняем URI для дальнейшего использования
                requireContext().contentResolver.takePersistableUriPermission(
                    directoryUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
    }
}