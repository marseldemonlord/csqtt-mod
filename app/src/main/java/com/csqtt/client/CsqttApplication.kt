// SPDX-FileCopyrightText: 2026 amurcanov
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.csqtt.client

import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class CsqttApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val crashFile = java.io.File(dir, "crash.txt")
                crashFile.writeText("CRASH on [${thread.name}]: ${throwable.message}\n${throwable.stackTraceToString()}")
                DeployManager.writeError("CRASH on [${thread.name}]: ${throwable.message}\n${throwable.stackTraceToString()}")
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
        DeployManager.init(this)

        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                TunnelManager.running.collect {
                    VpnWidgetProvider.updateAllWidgets(this@CsqttApplication)
                }
            } catch (e: Exception) {
                Log.e("CsqttApp", "Не удалось обновить виджеты: ${e.message}")
            }
        }

        val settingsStore = SettingsStore(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                settingsStore.loggingEnabled.collect { enabled ->
                    TunnelManager.isLoggingEnabled = enabled
                }
            } catch (e: Exception) {
                Log.e("CsqttApp", "Не удалось отслеживать флаг логирования: ${e.message}")
            }
        }
    }
}
