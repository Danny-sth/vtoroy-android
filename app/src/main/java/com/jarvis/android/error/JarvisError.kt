package com.jarvis.android.error

sealed class JarvisError(val message: String) {
    class NetworkError(message: String, val code: Int? = null) : JarvisError(message)
    class AudioError(message: String) : JarvisError(message)
    class WakeWordError(message: String) : JarvisError(message)
    class PermissionError(val permission: String) : JarvisError("Missing permission: $permission")
    class ConfigurationError(message: String) : JarvisError(message)
}
