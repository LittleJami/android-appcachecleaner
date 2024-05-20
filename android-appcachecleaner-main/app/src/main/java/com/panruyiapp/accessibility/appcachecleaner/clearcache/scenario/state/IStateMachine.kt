package com.panruyiapp.accessibility.appcachecleaner.clearcache.scenario.state

internal interface IStateMachine {
    fun waitState(timeoutMs: Long): Boolean
    fun init()
    fun setDelayForNextApp()
    fun setOpenAppInfo()
    fun setFinishCleanApp()
    fun isFinishCleanApp(): Boolean
    fun setInterrupted()
    fun isInterrupted(): Boolean
    fun setInterruptedByUser()
    fun isInterruptedByUser(): Boolean
    fun isDone(): Boolean
}