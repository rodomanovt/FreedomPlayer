package com.rodomanovt.freedomplayer.fragments

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import com.rodomanovt.freedomplayer.R
import com.rodomanovt.freedomplayer.databinding.FragmentSettingsDownloaderBinding
import com.rodomanovt.freedomplayer.helpers.PrefsHelper
import com.rodomanovt.freedomplayer.viewmodels.SettingsDownloaderViewModel
import java.util.Calendar


class SettingsDownloaderFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsDownloaderFragment()
        private const val REQUEST_CODE_OPEN_DIRECTORY = 1
    }

    private val viewModel: SettingsDownloaderViewModel by viewModels()
    private val prefsHelper by lazy { PrefsHelper(requireContext()) }
    private lateinit var binding: FragmentSettingsDownloaderBinding

    //private var directoryPathString: String = "Undefined"
    private var selectedTimeString: String = "Undefined"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings_downloader, container, false)
        binding.settingDownloadPathText.text = prefsHelper.getRootFolderUri()?.path ?: "Undefined"
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
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, selectedHour, selectedMinute ->
                    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

                    selectedTimeString = formattedTime

                    binding.settingAutoDownloadStartTimeText.text = formattedTime
                    Toast.makeText(requireContext(), "changed time to ${selectedTimeString}", Toast.LENGTH_SHORT).show()
                    // TODO: сохранить настроку в бд (вынести во viewmodel)
                    // TODO: реализовать функционал (вынести во viewmodel)
                },
                hour,
                minute,
                true // 24-часовой формат (true) или 12-часовой (false)
            )

            timePickerDialog.show()
        }


        val prefsHelper by lazy { PrefsHelper(requireContext()) }
        val selectFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, flags)
                // Сохраняем URI
                prefsHelper.saveRootFolderUri(uri)

            }
        }

        // Запуск выбора папки
        fun openFolderPicker() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            selectFolderLauncher.launch(intent)
        }

        binding.settingDownloadPathButton.setOnClickListener {
            openFolderPicker()
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
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == RESULT_OK) {
//            data?.data?.let { uri ->
//                // Получаем URI выбранной директории
//
//                // Сохраняем URI для дальнейшего использования
//                requireContext().contentResolver.takePersistableUriPermission(
//                    uri,
//                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                )
//
//
//                binding.settingDownloadPathText.text = uri.path ?: "Undefined"
//                prefsHelper.saveRootFolderUri(uri)
//                Toast.makeText(requireContext(), "changed  to ${prefsHelper.getRootFolderUri()}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
}