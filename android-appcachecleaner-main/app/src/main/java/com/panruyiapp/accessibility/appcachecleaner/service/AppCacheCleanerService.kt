package com.panruyiapp.accessibility.appcachecleaner.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.panruyiapp.accessibility.appcachecleaner.BuildConfig
import com.panruyiapp.accessibility.appcachecleaner.R
import com.panruyiapp.accessibility.appcachecleaner.clearcache.AccessibilityClearCacheManager
import com.panruyiapp.accessibility.appcachecleaner.log.Logger
import com.panruyiapp.accessibility.appcachecleaner.ui.view.AccessibilityOverlay
import com.panruyiapp.accessibility.appcachecleaner.util.IIntentServiceCallback
import com.panruyiapp.accessibility.appcachecleaner.util.IntentSettings
import com.panruyiapp.accessibility.appcachecleaner.util.LocalBroadcastManagerServiceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppCacheCleanerService : AccessibilityService(), IIntentServiceCallback {

    companion object {
        private val accessibilityClearCacheManager = AccessibilityClearCacheManager()
    }

    private val logger = Logger()

    private lateinit var accessibilityOverlay: AccessibilityOverlay
    private lateinit var localBroadcastManager: LocalBroadcastManagerServiceHelper

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG)
            logger.onCreate(cacheDir)

        localBroadcastManager = LocalBroadcastManagerServiceHelper(this, this)

        accessibilityOverlay = AccessibilityOverlay {
            accessibilityClearCacheManager.interrupt()
        }
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG)
            logger.onDestroy()

        localBroadcastManager.onDestroy()

        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            accessibilityClearCacheManager.checkEvent(event)
    }

    override fun onInterrupt() {
    }

    override fun onStopAccessibilityService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        disableSelf()
    }

    override fun onSetSettings(intentSettings: IntentSettings?) {
        intentSettings ?: return

        accessibilityClearCacheManager.setSettings(
            intentSettings.scenario,
            AccessibilityClearCacheManager.Settings(
                clearCacheTextList =
                    ArrayList<CharSequence>().apply {
                        intentSettings.clearCacheTextList?.forEach { add(it) }
                        add(getText(R.string.clear_cache_btn_text))
                    },
                clearDataTextList =
                    ArrayList<CharSequence>().apply {
                        intentSettings.clearDataTextList?.forEach { add(it) }
                        add(getText(R.string.clear_user_data_text))
                    },
                storageTextList =
                    ArrayList<CharSequence>().apply {
                        intentSettings.storageTextList?.forEach { add(it) }
                        add(getText(R.string.storage_settings_for_app))
                        add(getText(R.string.storage_label))
                    },
                okTextList =
                    ArrayList<CharSequence>().apply {
                        intentSettings.okTextList?.forEach { add(it) }
                        add(getText(android.R.string.ok))
                    },
                delayForNextAppTimeout = intentSettings.delayForNextAppTimeout,
                maxWaitAppTimeout = intentSettings.maxWaitAppTimeout,
                maxWaitClearCacheButtonTimeout = intentSettings.maxWaitClearCacheButtonTimeout
            )
        )
    }

    override fun onClearCache(pkgList: ArrayList<String>?) {
        if (BuildConfig.DEBUG)
            logger.onClearCache()

        pkgList?.let{
            accessibilityOverlay.show(this)
            val pkgListSize = pkgList.size
            CoroutineScope(Dispatchers.IO).launch {
                accessibilityClearCacheManager.clearCacheApp(
                    pkgList,
                    { index: Int ->
                        accessibilityOverlay.updateCounter(index, pkgListSize)
                    },
                    localBroadcastManager::sendAppInfo,
                    localBroadcastManager::sendFinish)
            }
        } ?: localBroadcastManager.sendFinish(true,
            accessibilityClearCacheManager.isInterruptedByUser(),
            null)
    }

    override fun onCleanCacheFinish() {
        accessibilityOverlay.hide(this)
    }
}
