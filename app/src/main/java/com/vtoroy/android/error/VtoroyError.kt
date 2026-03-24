package com.vtoroy.android.error

sealed class VtoroyError(val message: String) {
    class NetworkError(message: String, val code: Int? = null) : VtoroyError(message)
    class AudioError(message: String) : VtoroyError(message)
    class WakeWordError(message: String) : VtoroyError(message)
    class PermissionError(val permission: String) : VtoroyError("Missing permission: $permission")
    class ConfigurationError(message: String) : VtoroyError(message)
}
