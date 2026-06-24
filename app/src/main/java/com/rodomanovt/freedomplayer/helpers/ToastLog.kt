package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes

object ToastLog {

    fun show(
        context: Context,
        message: String,
        length: Int = Toast.LENGTH_SHORT,
        tag: String = TAG
    ) {
        Log.i(tag, message)
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
