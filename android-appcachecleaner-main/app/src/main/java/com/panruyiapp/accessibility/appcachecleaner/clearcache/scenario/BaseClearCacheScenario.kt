package com.panruyiapp.accessibility.appcachecleaner.clearcache.scenario

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.panruyiapp.accessibility.appcachecleaner.clearcache.scenario.state.IStateMachine
import com.panruyiapp.accessibility.appcachecleaner.const.Constant
import com.panruyiapp.accessibility.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_DELAY_FOR_NEXT_APP_MS
import com.panruyiapp.accessibility.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_WAIT_APP_PERFORM_CLICK_MS
import com.panruyiapp.accessibility.appcachecleaner.const.Constant.Settings.CacheClean.Companion.DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS
import com.panruyiapp.accessibility.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MAX_DELAY_FOR_NEXT_APP_MS
import com.panruyiapp.accessibility.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_DELAY_FOR_NEXT_APP_MS
import com.panruyiapp.accessibility.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_DELAY_PERFORM_CLICK_MS
import com.panruyiapp.accessibility.appcachecleaner.const.Constant.Settings.CacheClean.Companion.MIN_WAIT_CLEAR_CACHE_BUTTON_MS
import com.panruyiapp.accessibility.appcachecleaner.log.Logger
import com.panruyiapp.accessibility.appcachecleaner.util.performClick
import kotlinx.coroutines.delay

internal abstract class BaseClearCacheScenario {

    protected val arrayTextClearCacheButton = ArrayList<CharSequence>()
    protected val arrayTextClearDataButton = ArrayList<CharSequence>()
    protected val arrayTextStorageAndCacheMenu = ArrayList<CharSequence>()
    protected val arrayTextOkButton = ArrayList<CharSequence>()

    protected var delayForNextAppTimeoutMs =
        DEFAULT_DELAY_FOR_NEXT_APP_MS
    protected var maxWaitAppTimeoutMs =
        DEFAULT_WAIT_APP_PERFORM_CLICK_MS
    protected var maxWaitClearCacheButtonTimeoutMs =
        DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS
    protected var maxPerformClickCountTries =
        (DEFAULT_WAIT_APP_PERFORM_CLICK_MS - MIN_DELAY_PERFORM_CLICK_MS) / MIN_DELAY_PERFORM_CLICK_MS

    abstract suspend fun doCacheClean(nodeInfo: AccessibilityNodeInfo)
    abstract fun processState()

    abstract val stateMachine: IStateMachine

    fun setExtraSearchText(clearCacheTextList: ArrayList<CharSequence>,
                           clearDataTextList: ArrayList<CharSequence>,
                           storageTextList: ArrayList<CharSequence>,
                           okTextList: ArrayList<CharSequence>) {
        arrayTextClearCacheButton.clear()
        arrayTextClearCacheButton.addAll(clearCacheTextList)

        arrayTextClearDataButton.clear()
        arrayTextClearDataButton.addAll(clearDataTextList)

        arrayTextStorageAndCacheMenu.clear()
        arrayTextStorageAndCacheMenu.addAll(storageTextList)

        arrayTextOkButton.clear()
        arrayTextOkButton.addAll(okTextList)
    }

    fun setDelayForNextAppTimeout(timeout: Int) {
        delayForNextAppTimeoutMs = timeout * 1000

        if (delayForNextAppTimeoutMs >= MAX_DELAY_FOR_NEXT_APP_MS)
            delayForNextAppTimeoutMs = MAX_DELAY_FOR_NEXT_APP_MS

        if (delayForNextAppTimeoutMs < MIN_DELAY_FOR_NEXT_APP_MS)
            delayForNextAppTimeoutMs = MIN_DELAY_FOR_NEXT_APP_MS
    }

    fun setMaxWaitAppTimeout(timeout: Int) {
        maxWaitAppTimeoutMs = timeout * 1000

        if (maxWaitAppTimeoutMs < Constant.Settings.CacheClean.MIN_WAIT_APP_PERFORM_CLICK_MS)
            maxWaitAppTimeoutMs = Constant.Settings.CacheClean.MIN_WAIT_APP_PERFORM_CLICK_MS

        maxPerformClickCountTries =
            (maxWaitAppTimeoutMs - MIN_DELAY_PERFORM_CLICK_MS) / MIN_DELAY_PERFORM_CLICK_MS
    }

    fun setMaxWaitClearCacheButtonTimeout(timeout: Int) {
        maxWaitClearCacheButtonTimeoutMs = timeout * 1000

        if (maxWaitClearCacheButtonTimeoutMs >= maxWaitAppTimeoutMs)
            maxWaitClearCacheButtonTimeoutMs = maxWaitAppTimeoutMs - 1000

        if (maxWaitClearCacheButtonTimeoutMs < MIN_WAIT_CLEAR_CACHE_BUTTON_MS)
            maxWaitClearCacheButtonTimeoutMs = MIN_WAIT_CLEAR_CACHE_BUTTON_MS
    }

    protected suspend fun doPerformClick(nodeInfo: AccessibilityNodeInfo,
                                       debugText: String): Boolean?
    {
        Logger.d("found $debugText")
        if (nodeInfo.isEnabled) {
            Logger.d("$debugText is enabled")

            var result: Boolean?
            var tries: Long = 0
            do {
                result = nodeInfo.performClick()
                when (result) {
                    true -> Logger.d("perform action click on $debugText")
                    false -> Logger.e("no perform action click on $debugText")
                    else -> Logger.e("not found clickable view for $debugText")
                }

                if (result == true)
                    break

                if (tries++ >= maxPerformClickCountTries)
                    break

                delay(MIN_DELAY_PERFORM_CLICK_MS.toLong())
            } while (result != true)

            return (result == true)
        }

        return null
    }

    protected fun doPerformScrollForward(nodeInfo: AccessibilityNodeInfo,
                                       debugText: String): Boolean?
    {
        Logger.d("found $debugText")
        if (nodeInfo.isEnabled) {
            Logger.d("$debugText is enabled")

            val result = nodeInfo.performAction(
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id,
                Bundle()
            )
            when (result) {
                true -> Logger.d("perform action scroll forward on $debugText")
                false -> Logger.e("no perform action scroll forward on $debugText")
            }

            return result
        }

        return null
    }
}