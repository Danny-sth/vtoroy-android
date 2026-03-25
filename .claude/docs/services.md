# Jarvis Android Services

## JarvisListenerService

Main foreground service for wake word detection.

**File**: `service/JarvisListenerService.kt`

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
4. Send audio to API (JarvisApiClient)
5. Play response (AudioPlayer)

## JarvisNotificationManager

Notification lifecycle management.

**File**: `service/JarvisNotificationManager.kt`

**Channels**:
- `jarvis_listener_channel` — foreground service notification

**States**:
- IDLE: "Listening for wake word"
- LISTENING: "Recording..."
- PROCESSING: "Processing..."
- PLAYING: "Playing response"

## JarvisAccessibilityService

Background mode support for MIUI/restricted OEMs.

**File**: `service/JarvisAccessibilityService.kt`

**Purpose**: Keep service alive on aggressive battery optimization

## JarvisVoiceInteractionService

System assistant integration (optional).

**File**: `service/JarvisVoiceInteractionService.kt`

**Purpose**: Register as system voice assistant

## BootReceiver

Start service on device boot.

**File**: `service/BootReceiver.kt`

**Intent**: `android.intent.action.BOOT_COMPLETED`

## AuthReceiver

ADB intent receiver for testing.

**File**: `service/AuthReceiver.kt`

**Actions**:
- `com.jarvis.android.SET_AUTH` — set auth token via ADB
- `com.jarvis.android.START` — start service
- `com.jarvis.android.STOP` — stop service

**Usage**:
```bash
adb shell am broadcast -a com.jarvis.android.SET_AUTH --es token "mob_xxx"
```
