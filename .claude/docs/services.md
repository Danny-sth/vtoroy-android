# Vtoroy Android Services

## VtoroyListenerService

Main foreground service for wake word detection.

**File**: `service/VtoroyListenerService.kt`

**Lifecycle**:
- Started when app opens (via MainScreen lifecycle observer)
- Stopped when app minimizes
- Shows foreground notification

**Responsibilities**:
- Initialize WakeWordManager with Porcupine
- Handle wake word detection callback
- Coordinate with VoiceCommandProcessor
- Manage notification state

## VoiceCommandProcessor

Voice recording and processing pipeline.

**File**: `service/VoiceCommandProcessor.kt`

**Flow**:
1. Start recording (AudioRecorder)
2. Monitor for silence (VoiceActivityDetector)
3. Stop recording on 2s silence
4. Send audio to API (VtoroyApiClient)
5. Play response (AudioPlayer)

## VtoroyNotificationManager

Notification lifecycle management.

**File**: `service/VtoroyNotificationManager.kt`

**Channels**:
- `vtoroy_listener_channel` — foreground service notification

**States**:
- IDLE: "Listening for wake word"
- LISTENING: "Recording..."
- PROCESSING: "Processing..."
- PLAYING: "Playing response"

## VtoroyAccessibilityService

Background mode support for MIUI/restricted OEMs.

**File**: `service/VtoroyAccessibilityService.kt`

**Purpose**: Keep service alive on aggressive battery optimization

## VtoroyVoiceInteractionService

System assistant integration (optional).

**File**: `service/VtoroyVoiceInteractionService.kt`

**Purpose**: Register as system voice assistant

## BootReceiver

Start service on device boot.

**File**: `service/BootReceiver.kt`

**Intent**: `android.intent.action.BOOT_COMPLETED`

## AuthReceiver

ADB intent receiver for testing.

**File**: `service/AuthReceiver.kt`

**Actions**:
- `com.vtoroy.android.SET_AUTH` — set auth token via ADB
- `com.vtoroy.android.START` — start service
- `com.vtoroy.android.STOP` — stop service

**Usage**:
```bash
adb shell am broadcast -a com.vtoroy.android.SET_AUTH --es token "mob_xxx"
```
