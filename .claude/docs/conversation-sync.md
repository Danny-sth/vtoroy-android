# Conversation Sync (Mobile ↔ Telegram)

> Unified conversation history with automatic cross-channel synchronization

## Overview

Messages sent from the mobile app appear automatically in Telegram, and vice versa. All conversations are stored in PostgreSQL (backend) and cached locally in Room DB (mobile) for offline access.

## Architecture

```
┌─────────────┐
│   Mobile    │ Voice Command
│     App     │────────────────┐
└─────────────┘                │
       │                       ▼
       │              ┌──────────────────┐
       │              │ jarvis-gateway   │
       │              │  (Backend VPS)   │
       │              └──────────────────┘
       │                       │
       │              ┌────────┴────────┐
       │              ▼                 ▼
       │      ┌──────────────┐  ┌─────────────┐
       │      │ PostgreSQL   │  │  Telegram   │
       │      │ (Source of   │  │  Bot (auto- │
       │      │  Truth)      │  │   push)     │
       │      └──────────────┘  └─────────────┘
       │              │
       │ Pull-on-focus│
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│  Room DB    │  │  Room DB    │
│  (Cache)    │◄─┤  (reactive  │
└─────────────┘  │   Flow)     │
       │         └─────────────┘
       │                │
       ▼                ▼
   ┌──────────────────────┐
   │   Compose UI         │
   │   (LazyColumn with   │
   │    auto-scroll)      │
   └──────────────────────┘
```

## Data Flow

### Mobile → Backend → Telegram

```
1. User says "JARVIS" + voice command
2. VoiceCommandProcessor sends WAV to POST /api/voice
3. Backend (jarvis-gateway):
   a. STT: audio → text
   b. Save user message to PostgreSQL
   c. Send to Telegram: "📱 [Mobile App]\n\n{text}" (goroutine)
   d. Call Jarvis agent (with 20 msg history + Cortex)
   e. Save assistant response to PostgreSQL
   f. Send to Telegram: "🤖 [Jarvis]\n\n{response}" (goroutine)
   g. TTS: text → OGG
   h. Return JSON { text, audio }
4. VoiceCommandProcessor:
   a. Play audio (ExoPlayer)
   b. Call conversationRepository.refreshMessages()
5. ConversationRepository:
   a. GET /api/conversations/{id}/messages
   b. Save to Room DB (replace cache)
6. Room DB Flow emits new data
7. ConversationViewModel updates StateFlow
8. Compose UI (MessagesList) auto-refreshes
```

### Telegram → Mobile (Pull)

```
1. User opens mobile app (ON_START lifecycle event)
2. MainScreen triggers viewModel.loadConversationsAndMessages()
3. ConversationRepository:
   a. GET /api/conversations/{id}/messages
   b. Save to Room DB
4. Room DB Flow emits data
5. UI updates with new messages from Telegram
```

## Key Components

### Backend (jarvis-gateway)

| File | Changes |
|------|---------|
| `internal/handlers/voice.go` | Added goroutines to send user msg + assistant response to Telegram |
| `internal/handlers/telegram.go` | Added `/history [N]` command to view messages in Telegram |
| `internal/session/service.go` | Existing: `GetOrCreateConversationID()`, `SaveMessageSimple()`, `GetRecentMessagesSimple()` |

### Mobile (jarvis-android)

| Component | Purpose |
|-----------|---------|
| `ConversationRepository` | Sync between PostgreSQL (API) and Room DB |
| `JarvisDatabase` (Room) | Offline cache: `ConversationEntity`, `MessageEntity` |
| `ConversationViewModel` | Reactive Flow: `messages` StateFlow auto-updates on DB change |
| `MessageBubble` | UI component for user/assistant messages |
| `MessagesList` | LazyColumn with auto-scroll, displays message history |
| `MainScreen` | Shows ArcReactor + LazyColumn history below |

### API Endpoints

```http
GET /api/conversations
Authorization: Bearer mob_xxx
Response: [{ id, userId, title, startedAt, lastMessageAt, isActive }]

GET /api/conversations/{id}/messages?limit=50
Authorization: Bearer mob_xxx
Response: [{ id, conversationId, role, content, createdAt }]

POST /api/conversations
Authorization: Bearer mob_xxx
Body: { title?: string }
Response: { id, userId, title, startedAt }
```

## Reactive Flow Pattern

```kotlin
// ConversationViewModel.kt
private val _currentConversationId = MutableStateFlow<String?>(null)

// Auto-updating messages via Room DB Flow + flatMapLatest
val messages: StateFlow<List<Message>> = _currentConversationId
    .flatMapLatest { conversationId ->
        if (conversationId != null) {
            conversationRepository.getMessagesFlow(conversationId)
        } else {
            flowOf(emptyList())
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

**Result**: When Room DB updates (e.g., after `refreshMessages()`), the Flow automatically emits new data, and Compose UI re-renders the MessagesList.

## Telegram Sync Format

### User Message
```
📱 *[Mobile App]*

Какая погода сегодня?
```

### Assistant Response
```
🤖 *[Jarvis]*

Sir, temperature is 15°C with partly cloudy skies.
```

### Commands
```
/history      # Show last 10 messages (default)
/history 20   # Show last 20 messages (max 50)
/help         # Show available commands
```

## Memory & Context

- **Short-term**: 20 most recent messages from PostgreSQL passed to agent on each request
- **Long-term**: Cortex vector memory (semantic search, fact extraction)
- **Cross-channel**: Same conversation ID used for both Mobile and Telegram, so memory is shared

## Pull-on-Focus Strategy

**Rationale**: Battery efficient, simple, reliable

**How it works**:
1. `MainScreen` observes lifecycle events
2. On `Lifecycle.Event.ON_START` (app gains foreground):
   ```kotlin
   DisposableEffect(lifecycleOwner) {
       val observer = LifecycleEventObserver { _, event ->
           if (event == Lifecycle.Event.ON_START) {
               viewModel.loadConversationsAndMessages()
           }
       }
       lifecycleOwner.lifecycle.addObserver(observer)
       onDispose {
           lifecycleOwner.lifecycle.removeObserver(observer)
       }
   }
   ```
3. `loadConversationsAndMessages()` calls `conversationRepository.refreshMessages()`
4. Repository fetches from API and updates Room DB
5. Flow emits new data → UI updates

**Alternative rejected**: WebSocket/push notifications (battery drain, complexity)

## Testing

### Manual Test: Mobile → Telegram
1. Open mobile app
2. Say "JARVIS" + voice command
3. Open Telegram chat with Jarvis
4. Verify 2 messages appear:
   - `📱 [Mobile App]` with your command
   - `🤖 [Jarvis]` with agent response

### Manual Test: Telegram → Mobile
1. Send text message in Telegram to Jarvis
2. Wait for agent response
3. Open mobile app (or pull-to-refresh if already open)
4. Verify messages from Telegram appear in LazyColumn

### Manual Test: Memory
1. In mobile app: "JARVIS, remember that my favorite color is blue"
2. Wait for response
3. In Telegram: "What's my favorite color?"
4. Agent should respond: "Your favorite color is blue"
5. Verify cross-channel memory works

## Troubleshooting

### UI not updating after voice command
- **Check**: VoiceCommandProcessor calls `conversationRepository.refreshMessages()`
- **Check**: Room DB Flow is active (ViewModel not destroyed)
- **Check**: StateFlow collection in Compose (via `collectAsState()`)

### Messages not appearing in Telegram
- **Check**: Backend logs: `journalctl -u jarvis-gateway | grep telegram`
- **Check**: Goroutines executing (look for `[telegram] Sent text message`)
- **Check**: Telegram bot token valid in backend config

### Duplicate messages in UI
- **Check**: `refreshMessages()` called multiple times
- **Check**: Room DB `@Insert(onConflict = OnConflictStrategy.REPLACE)` strategy

### Old messages still showing after refresh
- **Check**: `clearMessages()` called before inserting new batch
- **Check**: API returns correct conversation ID
- **Check**: Room DB query uses correct conversation ID filter

---

**Last Updated**: 2026-03-31
**Status**: ✅ Fully implemented and deployed
