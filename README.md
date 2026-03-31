# Jarvis Android

> Voice-controlled AI assistant for Android with unified conversation history

Mobile client for Jarvis AI with wake word detection, voice activity detection, and automatic cross-channel synchronization with Telegram.

## Features

- 🎤 **Wake Word Detection** - "JARVIS" using Porcupine (sensitivity: 0.5f)
- 🗣️ **Voice Activity Detection** - Silero VAD with 2s silence timeout
- 💬 **Unified Conversations** - Shared history between Mobile and Telegram
- 🔄 **Auto-Sync** - Messages automatically appear in Telegram
- ⚡ **Reactive UI** - Room DB + Flow for instant updates
- 🧠 **Cross-Channel Memory** - 20 message history + Cortex vector memory
- 📱 **App-Only Mode** - Microphone disabled when minimized (battery efficient)

## Quick Start

### Prerequisites

- Android SDK 26+ (Android 8.0+)
- Porcupine API key ([get free key](https://console.picovoice.ai/))
- Mobile auth token (scan QR from Jarvis web dashboard)

### Installation

1. Clone repository:
   ```bash
   git clone https://github.com/Danny-sth/vtoroy-android.git jarvis-android
   cd jarvis-android
   ```

2. Build APK:
   ```bash
   ./gradlew assembleDebug
   ```

3. Install on device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. Configure app:
   - Open app → Settings
   - Scan QR code from Jarvis dashboard
   - Enter Porcupine API key
   - Grant microphone permission

### Quick Test

```bash
# Set credentials via ADB (for testing)
adb shell am broadcast -a com.jarvis.android.SET_AUTH \
  --es token "mob_xxx" \
  --es porcupine_key "YOUR_KEY"

# View logs
adb logcat -s JarvisListenerService:D WakeWordManager:D
```

## Architecture

```
Wake Word (Porcupine "JARVIS")
       ↓
JarvisListenerService (foreground)
       ↓
VoiceCommandProcessor
       ↓
AudioRecorder (PCM 16kHz → WAV)
       ↓
VoiceActivityDetector (Silero, 2s silence)
       ↓
POST /api/voice → PostgreSQL → Room DB → Flow → UI
       ↓                ↓
   AudioPlayer    Telegram (auto-sync)
```

### Data Sync

```
Mobile App → jarvis-gateway → PostgreSQL (source of truth)
                                    ↓
                    ┌───────────────┴──────────────┐
                    ▼                              ▼
              Room DB (cache)                Telegram Bot
                    ↓                        (auto-push)
              Flow → LazyColumn
            (reactive updates)
```

**Features:**
- 📱 Mobile messages → Telegram: `*[Mobile App]*\n\n{text}`
- 🤖 Assistant responses → Telegram: `*[Jarvis]*\n\n{response}`
- 🔄 Pull-on-focus: app loads latest messages when gaining foreground
- ⚡ Reactive UI: Room Flow auto-updates when new messages arrive

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.2.0 |
| UI | Jetpack Compose (BOM 2024.11.00) + Material3 |
| DI | Hilt 2.56 |
| Database | Room 2.6.1 (offline cache) |
| Network | OkHttp 4.12.0 |
| Wake Word | Porcupine Android 3.0.2 |
| VAD | Silero android-vad 2.0.10 |
| Audio | Media3 ExoPlayer 1.2.1 |
| QR Scan | MLKit 17.3.0 + CameraX 1.4.0 |
| Persistence | DataStore Preferences |

## Project Structure

```
app/src/main/java/com/jarvis/android/
├── service/                  # JarvisListenerService, VoiceCommandProcessor
├── wakeword/                 # Porcupine WakeWordManager
├── audio/                    # AudioRecorder, AudioPlayer, VAD
├── network/                  # JarvisApiClient + API models
├── data/                     # Repositories, Room DB, models
│   ├── ConversationRepository.kt
│   ├── local/                # Room entities, DAOs, database
│   └── model/                # Message, Conversation
├── ui/                       # Compose screens + ViewModels
│   ├── MainScreen.kt
│   ├── ConversationViewModel.kt
│   └── components/           # MessageBubble, MessagesList
├── di/                       # Hilt modules
└── util/                     # Constants, permissions
```

## API Integration

**Backend**: `https://on-za-menya.online` (jarvis-gateway)

### Endpoints

```http
POST /api/voice
Authorization: Bearer mob_xxx
Content-Type: multipart/form-data
Body: audio (WAV file)
Response: { text: string, audio: base64 OGG }

GET /api/conversations
Authorization: Bearer mob_xxx
Response: [{ id, userId, title, startedAt, lastMessageAt }]

GET /api/conversations/{id}/messages?limit=50
Authorization: Bearer mob_xxx
Response: [{ id, conversationId, role, content, createdAt }]

POST /api/conversations
Authorization: Bearer mob_xxx
Body: { title?: string }
Response: { id, userId, title, startedAt }
```

### Telegram Integration

Backend automatically syncs Mobile messages to Telegram:
- User message: `📱 *[Mobile App]*\n\n{command}`
- Assistant response: `🤖 *[Jarvis]*\n\n{response}`
- Commands: `/history [N]` - view last N messages

## Development

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Run tests
./gradlew test
```

### Debugging

```bash
# Voice pipeline logs
adb logcat -s WakeWordManager:D VoiceActivityDetector:D JarvisApiClient:D

# All app logs
adb logcat | grep -E "Jarvis|WakeWord|VoiceActivity|ApiClient"

# Conversation sync logs
adb logcat -s ConversationViewModel:D ConversationRepository:D

# Clear app data
adb shell pm clear com.jarvis.android
```

### Testing

#### Manual Test: Voice Command
1. Open app
2. Say "JARVIS"
3. Say command (e.g., "What's the weather?")
4. Check Telegram for auto-synced messages

#### Manual Test: Pull-to-Refresh
1. Send message in Telegram to Jarvis
2. Open mobile app (or swipe down to refresh)
3. Verify Telegram messages appear in LazyColumn

#### Manual Test: Memory
1. Mobile: "JARVIS, remember my favorite color is blue"
2. Telegram: "What's my favorite color?"
3. Verify agent remembers cross-channel

## Configuration

### Wake Word Sensitivity

Adjust in `wakeword/WakeWordManager.kt`:
```kotlin
.setSensitivity(0.5f)  // 0.0-1.0: lower = faster, higher = stricter
```

### VAD Silence Timeout

Adjust in `audio/VoiceActivityDetector.kt`:
```kotlin
private val silenceTimeoutMs = 2000L  // milliseconds
```

## Troubleshooting

### Wake word not detected
- Check Porcupine API key is valid
- Lower sensitivity (0.5f → 0.3f)
- Ensure microphone permission granted
- Check logs: `adb logcat -s WakeWordManager:D`

### UI not updating
- Check Room DB Flow is active
- Verify `refreshMessages()` called after voice command
- Check logs: `adb logcat -s ConversationViewModel:D`

### Messages not in Telegram
- Check backend logs: `ssh root@90.156.230.49 "journalctl -u jarvis-gateway | grep telegram"`
- Verify goroutines executing
- Check Telegram bot token

## Documentation

Detailed documentation in `.claude/` directory:

```
.claude/
├── CLAUDE.md                    # Quick reference for developers
├── NOTES.md                     # Session notes, decisions
├── docs/
│   ├── architecture.md          # Full tech stack, state machine
│   ├── services.md              # Android services explained
│   ├── dependencies.md          # Libraries reference
│   └── conversation-sync.md     # Conversation sync guide
└── skills/
    ├── build-deploy.md          # Build and deployment
    ├── debug-voice.md           # Voice pipeline debugging
    ├── add-feature.md           # Adding features
    └── troubleshooting.md       # Common errors
```

## Contributing

1. Fork repository
2. Create feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -am 'Add feature'`
4. Push to branch: `git push origin feature/my-feature`
5. Submit pull request

## License

Private project - © 2026 Danny-sth

## Related Projects

- [jarvis-gateway](https://github.com/Danny-sth/jarvis-gateway) - Backend API gateway
- [not-that-jarvis](https://github.com/Danny-sth/not-that-jarvis) - Core AI agent

## Support

For issues and questions, see `.claude/skills/troubleshooting.md` or create an issue.

---

**Built with ❤️ and Claude Code**
