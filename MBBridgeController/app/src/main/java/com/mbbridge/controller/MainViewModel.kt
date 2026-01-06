package com.mbbridge.controller

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val isServerRunning: Boolean = false,
    val lastCommand: Command? = null,
    val stats: CommandStats = CommandStats(),
    val logs: List<String> = emptyList(),
    val token: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application),
    MBBridgeHttpServer.CommandListener,
    MBBridgeHttpServer.LogListener {

    companion object {
        private const val TAG = "MBBridgeCtrl"
        private const val MAX_LOGS = 80
    }

    private val context: Context = application.applicationContext
    private val tokenStore = TokenStore(context)

    private val _uiState = MutableStateFlow(UiState(token = tokenStore.getToken() ?: ""))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var service: MBBridgeService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? MBBridgeService.LocalBinder
            service = localBinder?.getService()
            service?.setCommandListener(this@MainViewModel)
            service?.setLogListener(this@MainViewModel)
            updateServerStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            updateServerStatus()
        }
    }

    init {
        val intent = Intent(context, MBBridgeService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun startServer() {
        MBBridgeService.startService(context)
        viewModelScope.launch {
            delay(300)
            updateServerStatus()
        }
    }

    fun stopServer() {
        MBBridgeService.stopService(context)
        updateServerStatus()
    }

    fun simulateCommand(type: CommandType) {
        val command = Command(
            v = when (type) {
                is CommandType.PREV -> 1
                is CommandType.NEXT -> 2
                is CommandType.UNKNOWN -> type.value
            },
            ts = System.currentTimeMillis(),
            source = "simulate"
        )
        onCommandReceived(command)
    }

    fun saveToken(token: String) {
        tokenStore.saveToken(token)
        _uiState.value = _uiState.value.copy(token = token)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logs = emptyList())
    }

    override fun onCommandReceived(command: Command) {
        viewModelScope.launch {
            val current = _uiState.value
            val updatedStats = current.stats.increment(command.getCommandType())
            val logEntry = buildLogEntry(command)
            val newLogs = (listOf(logEntry) + current.logs).take(MAX_LOGS)
            _uiState.value = current.copy(
                lastCommand = command,
                stats = updatedStats,
                logs = newLogs
            )
            Log.i(TAG, "Command received: ${command.getCommandType()}")
        }
    }

    override fun onLog(level: LogLevel, message: String) {
        val timestamp = android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis())
        val entry = "[$timestamp] [${level.name}] $message"
        val current = _uiState.value
        _uiState.value = current.copy(logs = (listOf(entry) + current.logs).take(MAX_LOGS))
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            Log.w(TAG, "Unbind service failed", e)
        }
        service?.setCommandListener(null)
        service?.setLogListener(null)
    }

    private fun updateServerStatus() {
        _uiState.value = _uiState.value.copy(
            isServerRunning = service?.isServerRunning() == true
        )
    }

    private fun buildLogEntry(command: Command): String {
        val timestamp = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", command.ts)
        return "[$timestamp] ${command.getCommandType()} v=${command.v} source=${command.source}"
    }
}
