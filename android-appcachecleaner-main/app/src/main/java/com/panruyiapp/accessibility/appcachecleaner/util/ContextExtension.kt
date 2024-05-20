package com.panruyiapp.accessibility.appcachecleaner.util

import android.content.Context
import android.content.res.Configuration
import com.panruyiapp.accessibility.appcachecleaner.config.SharedPreferencesManager

suspend fun Context.getDayNightModeContext(): Context {
    return when (SharedPreferencesManager.UI.getNightMode(this)) {
        true -> {
            val uiModeFlag = Configuration.UI_MODE_NIGHT_YES
            val config = Configuration(this.resources.configuration)
            config.uiMode = uiModeFlag or (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
            this.createConfigurationContext(config)
        }
        else -> this
    }
}