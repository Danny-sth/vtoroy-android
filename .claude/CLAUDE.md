# Jarvis Android

## Quick Build

```bash
# Build APK
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat -s WakeWordManager:D VoiceActivityDetector:D JarvisApiClient:D
```

## Architecture

```
Wake Word (Porcupine "JARVIS")
       ↓
JarvisListenerService (foreground)
       ↓
VoiceCommandProcessor
       ↓ record
VoiceActivityDetector (Silero VAD, 2s silence)
       ↓
JarvisApiClient (POST /api/voice)
       ↓
AudioPlayer (ExoPlayer)
```

## Key Files

| File | Purpose |
|------|---------|
| `service/JarvisListenerService.kt` | Main foreground service |
| `service/VoiceCommandProcessor.kt` | Recording → API → Playback |
| `wakeword/WakeWordManager.kt` | Porcupine wrapper |
| `audio/VoiceActivityDetector.kt` | Silero VAD |
| `network/JarvisApiClient.kt` | HTTP client |
| `ui/MainScreen.kt` | Compose UI |

## Critical Notes

1. **Wake word is "JARVIS"** (Porcupine built-in)
2. **App-only mode** — mic off when minimized
3. **Requires Porcupine API key** in settings

## Detailed Docs

- **Architecture:** `.claude/docs/architecture.md`
- **Services:** `.claude/docs/services.md`

## Skills

- `build-deploy.md` — build and deploy to device
- `debug-voice.md` — debug voice pipeline
