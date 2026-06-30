package com.rodomanovt.freedomplayer.helpers

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DownloadLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog("INFO", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${Log.getStackTraceString(throwable)}"
        } else {
            message
        }
        addLog("ERROR", tag, fullMessage)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLog("WARN", tag, message)
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    private fun addLog(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $level/$tag: $message"
        val currentList = _logs.value.toMutableList()
        currentList.add(logEntry)
        if (currentList.size > 1000) {
            currentList.removeAt(0)
        }
        _logs.value = currentList
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
