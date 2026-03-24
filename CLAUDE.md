# Vtoroy Android

Мобильное голосовое приложение для Vtoroy AI.

## Текущий режим: App-Only

- Сворачиваешь → микрофон выключается
- Открываешь → слушает wake word "JARVIS" (Porcupine built-in)
- Нет фонового режима (экономия батареи)

> **Note**: Wake word "JARVIS" — это built-in keyword в Porcupine SDK.
> Для кастомного "Vtoroy" нужно тренировать модель на console.picovoice.ai

## Архитектура

```
Wake Word (Porcupine) → VAD (Silero, 2s) → Gateway → Vtoroy Agent → TTS → Play
```

## Ключевые файлы

```
service/VtoroyListenerService.kt  — главный сервис
service/VoiceCommandProcessor.kt  — запись → API → воспроизведение
wakeword/WakeWordManager.kt       — Porcupine wrapper
audio/VoiceActivityDetector.kt    — Silero VAD (2 сек тишины)
ui/MainScreen.kt                  — lifecycle observer (app-only)
```

## Команды

```bash
# Build
./gradlew assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Logs
adb logcat -s WakeWordManager:D VoiceActivityDetector:D VtoroyApiClient:D
```

## Backend

Gateway: vtoroy-gateway
Endpoint: POST /api/voice
Auth: Bearer token (получается через QR scan)
