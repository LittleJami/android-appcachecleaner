package com.panruyiapp.accessibility.appcachecleaner.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.annotation.UiThread
import com.panruyiapp.accessibility.appcachecleaner.R

class FilterListDialogBuilder {

    companion object {

        @JvmStatic
        @UiContext
        @UiThread
        @RequiresApi(Build.VERSION_CODES.O)
        fun buildMinCacheSizeDialog(context: Context,
                                    minCacheSizeStr: String?,
                                    onOkClick: (String?) -> Unit): Dialog {
            val inputEditText = EditText(context).apply {
                setText(minCacheSizeStr)
                hint = "0 KB"
                minHeight = AlertDialogBuilder.getMinTouchTargetSize(context)
                minWidth = AlertDialogBuilder.getMinTouchTargetSize(context)
            }

            return AlertDialogBuilder(context)
                .setTitle(R.string.dialog_filter_min_cache_size_title)
                .setMessage(R.string.dialog_filter_min_cache_size_message)
                .setView(inputEditText)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    onOkClick(inputEditText.text?.trim().toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()
        }
    }
}