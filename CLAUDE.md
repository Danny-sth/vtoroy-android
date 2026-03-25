# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk

# Clean build
./gradlew clean assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

## Debugging

```bash
# View voice pipeline logs
adb logcat -s WakeWordManager:D VoiceActivityDetector:D JarvisApiClient:D

# View all app logs
adb logcat | grep -E "Jarvis|WakeWord|VoiceActivity|ApiClient"

# Crash logs
adb logcat -b crash

# Set auth token via ADB (for testing)
adb shell am broadcast -a com.jarvis.android.SET_AUTH --es token "mob_xxx"
adb shell am broadcast -a com.jarvis.android.SET_AUTH --es porcupine_key "YOUR_KEY"
```

## Architecture

**App-only mode**: Microphone is disabled when app is minimized. No background processing.

### Voice Pipeline

```
Wake Word (Porcupine "JARVIS")
       ↓
JarvisListenerService (foreground service)
       ↓
VoiceCommandProcessor (orchestrates recording)
       ↓
AudioRecorder (PCM 16kHz mono → WAV)
       ↓
VoiceActivityDetector (Silero VAD, 2s silence timeout)
       ↓
JarvisApiClient (POST /api/voice → JSON + base64 OGG)
       ↓
AudioPlayer (ExoPlayer)
```

### State Machine

```
IDLE → (wake word) → LISTENING → (2s silence) → PROCESSING → (API response) → PLAYING → IDLE
                                                    ↓ (error)
                                                  ERROR
```

### Key Files

| File | Purpose |
|------|---------|
| `service/JarvisListenerService.kt` | Main foreground service, wake word coordination |
| `service/VoiceCommandProcessor.kt` | Recording → API → Playback pipeline |
| `wakeword/WakeWordManager.kt` | Porcupine wrapper (sensitivity: 0.85) |
| `audio/VoiceActivityDetector.kt` | Silero VAD (2s silence, 10s max recording) |
| `audio/AudioRecorder.kt` | PCM recording, WAV conversion |
| `audio/AudioPlayer.kt` | ExoPlayer for OGG playback |
| `network/JarvisApiClient.kt` | OkHttp client, multipart upload |
| `data/SettingsRepository.kt` | DataStore for auth token, Porcupine key |
| `ui/MainScreen.kt` | Compose UI, lifecycle observer (binds/unbinds service) |
| `di/AppModule.kt` | Hilt DI configuration |

### Project Structure

```
app/src/main/java/com/jarvis/android/
├── service/          # JarvisListenerService, VoiceCommandProcessor
├── wakeword/         # Porcupine WakeWordManager
├── audio/            # AudioRecorder, AudioPlayer, VoiceActivityDetector
├── network/          # JarvisApiClient
├── ui/               # Compose screens (MainScreen, SettingsScreen, QrScanner)
├── data/             # SettingsRepository (DataStore)
├── di/               # Hilt AppModule
└── JarvisState.kt    # State enum
```

## Dependencies

- **Wake Word**: Porcupine Android 3.0.2 (built-in "JARVIS" keyword)
- **VAD**: Silero android-vad 2.0.10
- **Audio**: Media3 ExoPlayer 1.2.1
- **Network**: OkHttp 4.12.0
- **QR Scanning**: MLKit 17.3.0 + CameraX 1.4.0
- **UI**: Jetpack Compose (BOM 2024.01.00) + Material3
- **DI**: Hilt 2.56
- **Persistence**: DataStore Preferences

## API

**Backend**: `https://on-za-menya.online`

**Endpoint**: `POST /api/voice`
```
Authorization: Bearer <token>
Content-Type: multipart/form-data
Body: audio (audio/wav)

Response: { "text": "...", "audio": "base64 OGG" }
```

**Auth flow**: QR scan → `POST /api/auth/qr/verify` → receive token → store in DataStore

## Critical Notes

1. **Wake word is "JARVIS"** (Porcupine built-in)
2. **Porcupine API key required** — set via Settings UI or ADB broadcast
3. **VAD silence timeout**: 2 seconds. Min recording: 0.5s, max: 10s
4. **Audio format**: Recording is 16kHz mono PCM → WAV. Response is OGG.
5. **Lifecycle**: Service binds on ON_START, unbinds+stops on ON_STOP (app-only mode)
