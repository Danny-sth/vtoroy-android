# ⛔⛔⛔ НИКОГДА НИЧЕГО НЕ УПРОЩАТЬ!!! ⛔⛔⛔

**МЫ ДЕЛАЕМ СЛОЖНЫЕ ПРОДУКТЫ! НИКАКИХ ЗАГЛУШЕК И ВРЕМЕННЫХ ПРОСТЫХ РЕШЕНИЙ!**
**ИСПОЛЬЗУЕМ ТОКЕНЫ МАКСИМАЛЬНО! ПРИОРИТЕТ — КАЧЕСТВО, А НЕ ПРОСТОТА РАЗРАБОТКИ!**

---

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
Wake Word (Porcupine "JARVIS", sensitivity: 0.5f)
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
ConversationRepository.refreshMessages() (auto-update UI)
       ↓
AudioPlayer (ExoPlayer)
```

### Data Sync (Mobile ↔ Telegram)

```
Voice Command → PostgreSQL (source of truth) → Room DB (cache) → Flow → UI
                      ↓
               Telegram (auto-sync via backend goroutines)
```

**Features:**
- 📱 Mobile messages appear in Telegram with `[Mobile App]` prefix
- 🤖 Assistant responses appear in Telegram with `[Jarvis]` prefix
- 💾 20 message history + Cortex vector memory shared cross-channel
- 🔄 Pull-on-focus refresh: app loads latest messages when gaining foreground
- ⚡ Reactive UI: Room DB Flow automatically updates LazyColumn when messages added

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
| `service/VoiceCommandProcessor.kt` | Recording → API → Playback + auto-refresh |
| `wakeword/WakeWordManager.kt` | Porcupine wrapper (sensitivity: 0.5f) |
| `audio/VoiceActivityDetector.kt` | Silero VAD (2s silence, 10s max recording) |
| `audio/AudioRecorder.kt` | PCM recording, WAV conversion |
| `audio/AudioPlayer.kt` | ExoPlayer for OGG playback |
| `network/JarvisApiClient.kt` | OkHttp client + conversation API endpoints |
| `data/ConversationRepository.kt` | Conversation sync (PostgreSQL + Room DB) |
| `data/local/JarvisDatabase.kt` | Room DB for offline message cache |
| `data/model/Message.kt` | Message data model |
| `data/model/Conversation.kt` | Conversation metadata model |
| `data/SettingsRepository.kt` | DataStore for auth token, Porcupine key |
| `ui/MainScreen.kt` | Compose UI with message history (LazyColumn) |
| `ui/ConversationViewModel.kt` | ViewModel with reactive Flow (auto-updates) |
| `ui/components/MessageBubble.kt` | Message UI component |
| `ui/components/MessagesList.kt` | LazyColumn with auto-scroll |
| `di/AppModule.kt` | Hilt DI configuration |

### Project Structure

```
app/src/main/java/com/jarvis/android/
├── service/          # JarvisListenerService, VoiceCommandProcessor
├── wakeword/         # Porcupine WakeWordManager
├── audio/            # AudioRecorder, AudioPlayer, VoiceActivityDetector
├── network/          # JarvisApiClient + API models
├── data/             # Repositories, Room DB, models
│   ├── ConversationRepository.kt
│   ├── SettingsRepository.kt
│   ├── local/        # Room DB entities, DAOs, database
│   └── model/        # Message, Conversation data classes
├── ui/               # Compose screens + ViewModels
│   ├── MainScreen.kt
│   ├── ConversationViewModel.kt
│   ├── SettingsScreen.kt
│   └── components/   # MessageBubble, MessagesList
├── di/               # Hilt AppModule
└── JarvisState.kt    # State enum
```

## Dependencies

- **Wake Word**: Porcupine Android 3.0.2 (built-in "JARVIS" keyword)
- **VAD**: Silero android-vad 2.0.10
- **Audio**: Media3 ExoPlayer 1.2.1
- **Network**: OkHttp 4.12.0
- **QR Scanning**: MLKit 17.3.0 + CameraX 1.4.0
- **UI**: Jetpack Compose (BOM 2024.11.00) + Material3
- **DI**: Hilt 2.56
- **Database**: Room 2.6.1 (offline message cache)
- **Persistence**: DataStore Preferences (auth tokens)

## API

**Backend**: `https://on-za-menya.online` (jarvis-gateway)
**Auth**: Bearer token (from QR scan)

### Endpoints

```
POST /api/voice
├─ Auth: Bearer mob_xxx
├─ Body: multipart/form-data (audio: WAV)
└─ Response: { text: string, audio: base64 OGG }

GET /api/conversations
├─ Auth: Bearer mob_xxx
└─ Response: [{ id, userId, title, startedAt, lastMessageAt, isActive }]

GET /api/conversations/{id}/messages
├─ Auth: Bearer mob_xxx
├─ Query: ?limit=50
└─ Response: [{ id, conversationId, role, content, createdAt }]

POST /api/conversations
├─ Auth: Bearer mob_xxx
├─ Body: { title?: string }
└─ Response: { id, userId, title, startedAt }
```

**Auth flow**: QR scan → `POST /api/auth/qr/verify` → receive token → store in DataStore

### Telegram Sync

Backend automatically sends Mobile messages to Telegram:
- User: `📱 *[Mobile App]*\n\n{text}`
- Assistant: `🤖 *[Jarvis]*\n\n{response}`
- Command: `/history [N]` - view last N messages in Telegram

## Critical Notes

1. **Wake word is "JARVIS"** (Porcupine built-in, sensitivity: 0.5f)
2. **Porcupine API key required** — set via Settings UI or ADB broadcast
3. **VAD silence timeout**: 2 seconds. Min recording: 0.5s, max: 10s
4. **Audio format**: Recording is 16kHz mono PCM → WAV. Response is OGG.
5. **Lifecycle**: Service binds on ON_START, unbinds+stops on ON_STOP (app-only mode)
6. **Conversation sync**: PostgreSQL (source of truth) + Room DB (cache) + Flow (reactive UI)
7. **Pull-on-focus**: App refreshes messages when gaining foreground (ON_START lifecycle)
8. **Auto-refresh**: After voice command, repository refreshes messages → UI updates automatically
9. **Cross-channel**: Mobile ↔ Telegram sync via backend goroutines (automatic)

## Extended Documentation

See `.claude/` directory for detailed docs and skills:

```
.claude/
├── CLAUDE.md              # Quick reference
├── NOTES.md               # Session memory, decisions
├── settings.json          # Hooks configuration
├── docs/
│   ├── architecture.md    # Full tech stack, state machine
│   ├── services.md        # Android services explained
│   └── dependencies.md    # Libraries reference
├── skills/
│   ├── build-deploy.md    # Build, install, logs
│   ├── debug-voice.md     # Voice pipeline debugging
│   ├── add-feature.md     # Adding new functionality
│   └── troubleshooting.md # Common errors and fixes
└── specs/
    ├── plans/             # Plan templates
    ├── requirements/      # Requirements templates
    └── tasks/             # Task templates
```
