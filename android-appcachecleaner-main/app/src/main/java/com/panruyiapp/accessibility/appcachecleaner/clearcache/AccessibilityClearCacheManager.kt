package com.panruyiapp.accessibility.appcachecleaner.clearcache

import android.view.accessibility.AccessibilityEvent
import com.panruyiapp.accessibility.appcachecleaner.BuildConfig
import com.panruyiapp.accessibility.appcachecleaner.clearcache.scenario.BaseClearCacheScenario
import com.panruyiapp.accessibility.appcachecleaner.clearcache.scenario.DefaultClearCacheScenario
import com.panruyiapp.accessibility.appcachecleaner.clearcache.scenario.XiaomiMIUIClearCacheScenario
import com.panruyiapp.accessibility.appcachecleaner.const.Constant
import com.panruyiapp.accessibility.appcachecleaner.log.Logger
import com.panruyiapp.accessibility.appcachecleaner.util.showTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction3

class AccessibilityClearCacheManager {

    data class Settings(
        val clearCacheTextList: ArrayList<CharSequence>,
        val clearDataTextList: ArrayList<CharSequence>,
        val storageTextList: ArrayList<CharSequence>,
        val okTextList: ArrayList<CharSequence>,
        val delayForNextAppTimeout: Int?,
        val maxWaitAppTimeout: Int?,
        val maxWaitClearCacheButtonTimeout: Int?,
    )

    fun setSettings(scenario: Constant.Scenario?, settings: Settings) {
        scenario?.let {
            cacheCleanScenario =
                when (it) {
                    Constant.Scenario.DEFAULT -> DefaultClearCacheScenario()
                    Constant.Scenario.XIAOMI_MIUI -> XiaomiMIUIClearCacheScenario()
                }
        }

        cacheCleanScenario.setExtraSearchText(
            settings.clearCacheTextList,
            settings.clearDataTextList,
            settings.storageTextList,
            settings.okTextList)

        settings.maxWaitAppTimeout?.let {
            cacheCleanScenario.setMaxWaitAppTimeout(it)
        }

        settings.maxWaitClearCacheButtonTimeout?.let {
            cacheCleanScenario.setMaxWaitClearCacheButtonTimeout(it)
        }

        settings.delayForNextAppTimeout?.let {
            cacheCleanScenario.setDelayForNextAppTimeout(it)
        }
    }

    fun clearCacheApp(pkgList: ArrayList<String>,
                      updatePosition: (Int) -> Unit,
                      openAppInfo: KFunction1<String, Unit>,
                      finish: KFunction3<Boolean, Boolean, String?, Unit>) {

        cacheCleanScenario.stateMachine.init()

        var currentPkg: String? = null

        for ((index, pkg) in pkgList.withIndex()) {
            if (BuildConfig.DEBUG)
                Logger.d("clearCacheApp: package name = $pkg")

            currentPkg = pkg

            updatePosition(index)

            // everything is possible...
            if (pkg.trim().isEmpty()) continue

            // state not changes, something goes wrong...
            if (cacheCleanScenario.stateMachine.isInterrupted()) break

            cacheCleanScenario.stateMachine.setOpenAppInfo()
            if (BuildConfig.DEBUG)
                Logger.d("clearCacheApp: open AppInfo")
            openAppInfo(pkg)

            // state not changes, something goes wrong...
            if (cacheCleanScenario.stateMachine.isInterrupted()) break

            cacheCleanScenario.processState()

            // something goes wrong...
            if (cacheCleanScenario.stateMachine.isInterrupted()) break
        }

        val interrupted = cacheCleanScenario.stateMachine.isInterrupted()
        cacheCleanScenario.stateMachine.init()

        finish(interrupted, isInterruptedByUser(),
            currentPkg.takeIf { interrupted && !isInterruptedByUser() })
    }

    fun checkEvent(event: AccessibilityEvent) {

        if (cacheCleanScenario.stateMachine.isDone()) return

        if (event.source == null) {
            cacheCleanScenario.stateMachine.setFinishCleanApp()
            return
        }

        val nodeInfo = event.source!!

        if (BuildConfig.DEBUG) {
            Logger.d("===>>> TREE BEGIN <<<===")
            nodeInfo.showTree(0)
            Logger.d("===>>> TREE END <<<===")
        }

        CoroutineScope(Dispatchers.IO).launch {
            cacheCleanScenario.doCacheClean(nodeInfo)
        }
    }

    fun interrupt() {
        cacheCleanScenario.stateMachine.setInterruptedByUser()
        if (cacheCleanScenario.stateMachine.isDone()) return
        cacheCleanScenario.stateMachine.setInterrupted()
    }

    fun isInterruptedByUser(): Boolean {
        return cacheCleanScenario.stateMachine.isInterruptedByUser()
    }

    companion object {
        private var cacheCleanScenario: BaseClearCacheScenario = DefaultClearCacheScenario()
    }
}
