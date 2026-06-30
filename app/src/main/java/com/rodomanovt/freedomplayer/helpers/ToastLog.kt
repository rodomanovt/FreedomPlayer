package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.rodomanovt.freedomplayer.helpers.DownloadLogger

object ToastLog {

    fun show(
        context: Context,
        message: String,
        length: Int = Toast.LENGTH_SHORT,
        tag: String = TAG
    ) {
        DownloadLogger.i(tag, message)
        Toast.makeText(context, message, length).show()
    }

    fun show(
        context: Context,
        @StringRes resId: Int,
        length: Int = Toast.LENGTH_SHORT,
        tag: String = TAG,
        vararg formatArgs: Any
    ) {
        show(context, context.getString(resId, *formatArgs), length, tag)
    }

    private const val TAG = "Toast"
}
