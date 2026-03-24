package com.vtoroy.android.service

import android.app.AppOpsManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.util.Log
import com.vtoroy.android.VtoroyState
import com.vtoroy.android.data.SettingsRepository
import com.vtoroy.android.wakeword.WakeWordManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VtoroyListenerService : Service() {

    companion object {
        private const val TAG = "VtoroyListenerService"
        const val ACTION_START = "com.vtoroy.android.START"
        const val ACTION_STOP = "com.vtoroy.android.STOP"
        private const val WAKE_LOCK_TIMEOUT_MS = 60 * 60 * 1000L // 1 hour
    }

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationManager: VtoroyNotificationManager
    @Inject lateinit var voiceCommandProcessor: VoiceCommandProcessor

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var wakeWordManager: WakeWordManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile
    private var isWakeWordInitialized = false

    private val _state = MutableStateFlow(VtoroyState.IDLE)
    val state: StateFlow<VtoroyState> = _state

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val stateCallback = object : VoiceCommandProcessor.StateCallback {
        override fun onStateChanged(state: VtoroyState) {
            _state.value = state
            notificationManager.updateNotification(state)
        }

        override fun onError(message: String) {
            _errorMessage.value = message
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): VtoroyListenerService = this@VtoroyListenerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        voiceCommandProcessor.initializePlayer()
        acquireWakeLock()
        // Initialize wake word on bind (not just on startCommand)
        initializeWakeWord()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // Don't start as foreground - only work when app is open
        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = notificationManager.createNotification(_state.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                VtoroyNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(VtoroyNotificationManager.NOTIFICATION_ID, notification)
        }
    }

    private fun initializeWakeWord() {
        if (isWakeWordInitialized) {
            Log.d(TAG, "Wake word already initialized, skipping")
            return
        }
        isWakeWordInitialized = true
        serviceScope.launch {
            startWakeWordManager()
        }
    }

    private fun isBackgroundRecordingAllowed(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_RECORD_AUDIO,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_RECORD_AUDIO,
                Process.myUid(),
                packageName
            )
        }
        // MODE_ALLOWED = 0, MODE_IGNORED = 1, MODE_ERRORED = 2, MODE_DEFAULT = 3, MODE_FOREGROUND = 4
        Log.d(TAG, "RECORD_AUDIO appops mode: $mode")
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private suspend fun startWakeWordManager() {
        try {
            val apiKey = settingsRepository.porcupineApiKey.first()
            if (apiKey.isBlank()) {
                _state.value = VtoroyState.ERROR
                _errorMessage.value = "Porcupine API key not configured"
                return
            }

            // Check if background recording is allowed
            if (!isBackgroundRecordingAllowed()) {
                Log.w(TAG, "Background recording not allowed by system")
                _state.value = VtoroyState.ERROR
                _errorMessage.value = "Background mic blocked. Enable in MIUI Settings → Apps → Vtoroy → Permissions → Microphone → Allow always"
                return
            }

            wakeWordManager?.stop()
            wakeWordManager = WakeWordManager(
                context = this@VtoroyListenerService,
                accessKey = apiKey,
                onWakeWordDetected = { onWakeWordDetected() },
                onError = { error ->
                    _state.value = VtoroyState.ERROR
                    _errorMessage.value = error
                }
            )
            wakeWordManager?.start()
            _state.value = VtoroyState.IDLE
            notificationManager.updateNotification(_state.value)
            Log.d(TAG, "Wake word manager started")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing wake word", e)
            _state.value = VtoroyState.ERROR
            _errorMessage.value = "Error: ${e.message}"
        }
    }

    private fun onWakeWordDetected() {
        Log.d(TAG, "Wake word detected!")
        serviceScope.launch { processVoiceCommand() }
    }

    private suspend fun processVoiceCommand() {
        try {
            wakeWordManager?.stop()
            wakeWordManager = null
            voiceCommandProcessor.processVoiceCommand(stateCallback)
        } finally {
            isWakeWordInitialized = false
            initializeWakeWord()
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "vtoroy:wakeword_detection"
        )
        wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        wakeWordManager?.stop()
        voiceCommandProcessor.stopRecording()
        voiceCommandProcessor.releasePlayer()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }
}
