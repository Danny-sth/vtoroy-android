# Jarvis Android Architecture

## Overview

Android voice command app using Jetpack Compose, Porcupine wake word detection, and Silero VAD.

## Tech Stack

- **Language**: Kotlin 2.2.0
- **UI**: Jetpack Compose + Material3
- **DI**: Hilt
- **Wake Word**: Porcupine SDK
- **VAD**: Silero (android-vad)
- **Audio**: ExoPlayer (Media3)
- **Network**: OkHttp
- **Persistence**: DataStore

## Project Structure

```
app/src/main/java/com/jarvis/android/
├── JarvisApplication.kt      # Hilt application
├── MainActivity.kt           # Entry point
├── JarvisState.kt            # State enum
├── service/
│   ├── JarvisListenerService.kt      # Foreground service
│   ├── VoiceCommandProcessor.kt      # Voice pipeline
│   ├── JarvisNotificationManager.kt  # Notifications
│   ├── JarvisAccessibilityService.kt # MIUI background
│   └── ...
├── network/
│   └── JarvisApiClient.kt    # HTTP client
├── audio/
│   ├── AudioRecorder.kt
│   ├── AudioPlayer.kt        # ExoPlayer
│   └── VoiceActivityDetector.kt  # Silero VAD
├── wakeword/
│   └── WakeWordManager.kt    # Porcupine
├── ui/
│   ├── JarvisApp.kt          # Navigation
│   ├── MainScreen.kt         # Main UI
│   ├── SettingsScreen.kt     # Settings
│   └── QrScanner.kt          # QR auth
├── data/
│   └── SettingsRepository.kt # DataStore
└── di/
    └── AppModule.kt          # Hilt module
```

## State Machine

```
IDLE → (wake word) → LISTENING → (silence) → PROCESSING → (response) → PLAYING → IDLE
                                     ↓ (error)
                                   ERROR
```

## Voice Pipeline

```
1. WakeWordManager detects "JARVIS"
2. JarvisListenerService triggers recording
3. VoiceCommandProcessor starts AudioRecorder
4. VoiceActivityDetector monitors for 2s silence
5. On silence: stop recording, send to API
6. JarvisApiClient POST /api/voice with WAV
7. Parse response: text + base64 audio
8. AudioPlayer plays OGG response
9. Return to IDLE state
```

## Authentication

```
1. User opens Settings
2. Scans QR code with gateway URL + code
3. JarvisApiClient.verifyQrCode(code)
4. Backend returns: { token, telegram_id, expires_at }
5. Token stored in DataStore
6. Token used for /api/voice requests
```

## Dependencies

```kotlin
// Wake word
"ai.picovoice:porcupine-android:3.0.2"

// Voice Activity Detection
"com.github.gkonovalov.android-vad:silero:2.0.10"

// Audio playback
"androidx.media3:media3-exoplayer:1.2.1"

// QR scanning
"com.google.mlkit:barcode-scanning:17.3.0"
```

## Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## Build Configuration

```kotlin
android {
    namespace = "com.jarvis.android"
    applicationId = "com.jarvis.android"
    compileSdk = 34
    minSdk = 26
    targetSdk = 34
}
```
