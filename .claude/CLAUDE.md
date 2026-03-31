# ⛔⛔⛔ НИКОГДА НИЧЕГО НЕ УПРОЩАТЬ!!! ⛔⛔⛔

**МЫ ДЕЛАЕМ СЛОЖНЫЕ ПРОДУКТЫ! НИКАКИХ ЗАГЛУШЕК И ВРЕМЕННЫХ ПРОСТЫХ РЕШЕНИЙ!**
**ИСПОЛЬЗУЕМ ТОКЕНЫ МАКСИМАЛЬНО! ПРИОРИТЕТ — КАЧЕСТВО, А НЕ ПРОСТОТА РАЗРАБОТКИ!**

---

# Jarvis Android

> Mobile voice assistant client for Vtoroy AI.

## Critical Rules

1. **ALWAYS build and test** after code changes
2. **CHECK LOGS** before saying "done" — `adb logcat -s JarvisListenerService:D`
3. **Wake word is "JARVIS"** — Porcupine built-in, do not change
4. **App-only mode** — mic disabled when minimized (by design)

## Quick Deploy

```bash
# One-liner: build + install + logs
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb logcat -s JarvisListenerService:D WakeWordManager:D
```

## Architecture

```
Wake Word (Porcupine "JARVIS", sensitivity: 0.5f)
       ↓
JarvisListenerService (foreground)
       ↓
VoiceCommandProcessor (orchestrates pipeline)
       ↓ record
VoiceActivityDetector (Silero VAD, 2s silence)
       ↓
JarvisApiClient (POST /api/voice)
       ↓
ConversationRepository.refreshMessages() (auto-update UI)
       ↓
AudioPlayer (ExoPlayer)
```

### Data Flow

```
Voice Command → API → PostgreSQL → Room DB → Flow → UI (auto-refresh)
                                    ↓
                              Telegram (auto-sync via backend goroutines)
```

### Message Sync (Mobile ↔ Telegram)

- **PostgreSQL**: Single source of truth for all conversations
- **Room DB**: Local cache for offline access + reactive Flow updates
- **Pull-on-focus**: Mobile app refreshes messages when gaining foreground
- **Auto-push**: Backend sends Mobile messages to Telegram automatically
- **Cross-channel memory**: 20 message history + Cortex vector memory shared

## Key Files

| File | Purpose |
|------|---------|
| `service/JarvisListenerService.kt` | Main foreground service, lifecycle |
| `service/VoiceCommandProcessor.kt` | Recording → API → Playback flow + auto-refresh |
| `wakeword/WakeWordManager.kt` | Porcupine wrapper (sensitivity: 0.5f) |
| `audio/VoiceActivityDetector.kt` | Silero VAD, silence detection |
| `network/JarvisApiClient.kt` | HTTP client, /api/voice + conversation endpoints |
| `data/ConversationRepository.kt` | Conversation sync, Room DB + API integration |
| `data/local/JarvisDatabase.kt` | Room DB for offline message cache |
| `data/model/Message.kt` | Message data model (user/assistant) |
| `data/model/Conversation.kt` | Conversation metadata model |
| `ui/MainScreen.kt` | Compose UI with message history (LazyColumn) |
| `ui/ConversationViewModel.kt` | ViewModel with reactive Flow for auto-updates |
| `ui/components/MessageBubble.kt` | Message UI component (text bubbles) |
| `ui/components/MessagesList.kt` | LazyColumn with auto-scroll |
| `ui/SettingsScreen.kt` | QR scan, API key input |

## Package Structure

```
com.jarvis.android/
├── JarvisApplication.kt      # Hilt app entry
├── audio/                    # VAD, AudioPlayer, AudioRecorder
├── data/                     # Repository, Room DB, models
│   ├── ConversationRepository.kt
│   ├── SettingsRepository.kt
│   ├── local/                # Room DB entities, DAOs, database
│   └── model/                # Message, Conversation data classes
├── di/                       # Hilt modules
├── model/                    # JarvisState enum
├── network/                  # JarvisApiClient + API models
├── service/                  # Foreground services
├── ui/                       # Compose screens + ViewModels
│   ├── MainScreen.kt
│   ├── ConversationViewModel.kt
│   └── components/           # MessageBubble, MessagesList
├── util/                     # Constants, permissions
└── wakeword/                 # Porcupine manager
```

## Backend Integration

**Gateway**: `jarvis-gateway` on VPS (`https://on-za-menya.online`)
**Auth**: Bearer token (from QR code)

### API Endpoints

```
POST /api/voice
├─ Auth: Bearer mob_xxx
├─ Body: multipart/form-data (audio: WAV file)
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

### Telegram Integration

Backend automatically sends Mobile messages to Telegram:
- User message: `📱 *[Mobile App]*\n\n{text}`
- Assistant response: `🤖 *[Jarvis]*\n\n{response}`
- Commands: `/history [N]` - view last N messages

## Detailed Docs

| Doc | Content |
|-----|---------|
| `docs/architecture.md` | Full tech stack, state machine, voice pipeline |
| `docs/services.md` | All Android services explained |
| `docs/dependencies.md` | Libraries, versions, purpose |

## Skills

| Skill | When to use |
|-------|-------------|
| `build-deploy.md` | Build APK, install, view logs |
| `debug-voice.md` | Wake word or voice pipeline issues |
| `add-feature.md` | Adding new functionality |
| `troubleshooting.md` | Common errors and fixes |

## Common Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Logs (all relevant tags)
adb logcat -s JarvisListenerService:D WakeWordManager:D VoiceActivityDetector:D JarvisApiClient:D VoiceCommandProcessor:D

# Clear app data
adb shell pm clear com.jarvis.android

# Uninstall
adb uninstall com.jarvis.android
```

## Environment

- **Kotlin**: 2.2.0
- **Compose**: Material3
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

---

_See NOTES.md for session memory and decisions._
