package com.duq.android.wakeword

import android.content.Context

/**
 * Factory interface for creating WakeWordManager instances.
 * Allows Hilt injection and testing without Porcupine dependency.
 */
interface WakeWordManagerFactory {
    /**
     * Create a new WakeWordManager instance.
     *
     * @param context Application context
     * @param accessKey Porcupine API key
     * @param onWakeWordDetected Callback when wake word is detected
     * @param onError Callback for errors
     * @return WakeWordManager instance
     */
    fun create(
        context: Context,
        accessKey: String,
        onWakeWordDetected: () -> Unit,
        onError: (String) -> Unit
    ): WakeWordManager
}

/**
 * Default implementation that creates real WakeWordManager instances.
 */
class DefaultWakeWordManagerFactory : WakeWordManagerFactory {
    override fun create(
        context: Context,
        accessKey: String,
        onWakeWordDetected: () -> Unit,
        onError: (String) -> Unit
    ): WakeWordManager {
        return WakeWordManager(
            context = context,
            accessKey = accessKey,
            onWakeWordDetected = onWakeWordDetected,
            onError = onError
        )
    }
}
