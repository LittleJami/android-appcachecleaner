package com.panruyiapp.accessibility.appcachecleaner.ui.dialog

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.UiContext
import androidx.annotation.UiThread
import com.panruyiapp.accessibility.appcachecleaner.R
import com.panruyiapp.accessibility.appcachecleaner.util.ActivityHelper
import com.panruyiapp.accessibility.appcachecleaner.util.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PermissionDialogBuilder {
    companion object {
        @JvmStatic
        @UiContext
        @UiThread
        fun buildAccessibilityPermissionDialog(context: Context): Dialog {
            return AlertDialogBuilder(context)
                .setTitle(R.string.text_enable_accessibility_title)
                .setMessage(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        R.string.text_enable_accessibility_message_api33
                    else
                        R.string.text_enable_accessibility_message)
                .setPositiveButton(R.string.allow) { _, _ ->
                    ActivityHelper.showAccessibilitySettings(context)
                }
                .setNegativeButton(R.string.deny) { _, _ ->
                }
                .create()
        }

        @JvmStatic
        @UiContext
        @UiThread
        fun buildUsageStatsPermissionDialog(context: Context): Dialog {
            return AlertDialogBuilder(context)
                .setTitle(R.string.text_enable_usage_stats_title)
                .setMessage(R.string.text_enable_usage_stats_message)
                .setPositiveButton(R.string.allow) { _, _ ->
                    ActivityHelper.showUsageAccessSettings(context)
                }
                .setNegativeButton(R.string.deny) { _, _ ->
                }
                .create()
        }

        @JvmStatic
        @UiContext
        @UiThread
        fun buildWriteExternalStoragePermissionDialog(context: Context,
            requestPermissionLauncher: ActivityResultLauncher<String>): Dialog {
            return AlertDialogBuilder(context)
                .setTitle(R.string.debug_text_enable_write_external_storage_permission)
                .setPositiveButton(R.string.allow) { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        if (PermissionChecker.checkWriteExternalStoragePermission(context))
                            return@launch
                        CoroutineScope(Dispatchers.Main).launch {
                            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }
                .create()
        }
    }
}