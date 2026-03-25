# Skill: Debug Voice Pipeline

## When to Use

- "voice not working", "wake word not detected", "audio problems"

## Voice Pipeline Overview

```
Porcupine → AudioRecorder → VoiceActivityDetector → API → AudioPlayer
```

## Debug Wake Word

```bash
# Check Porcupine logs
adb logcat -s WakeWordManager:D

# Expected output:
# D WakeWordManager: Started listening for wake word
# D WakeWordManager: Wake word detected!
```

### Wake Word Not Detected

1. Check Porcupine API key in Settings
2. Verify microphone permission granted
3. Speak clearly "JARVIS"
4. Check for background noise

## Debug Recording

```bash
adb logcat -s AudioRecorder:D VoiceActivityDetector:D
```

### Recording Issues

1. Check RECORD_AUDIO permission
2. Verify no other app using microphone
3. Check VAD sensitivity (2 sec silence)

## Debug API Call

```bash
adb logcat -s JarvisApiClient:D

# Check request/response
# D JarvisApiClient: Sending voice to API
# D JarvisApiClient: Response: 200 OK
```

### API Issues

1. Check network connectivity
2. Verify auth token valid
3. Check backend logs on VPS:
   ```bash
   ssh root@90.156.230.49 "journalctl -u jarvis-gateway -n 30"
   ```

## Debug Playback

```bash
adb logcat -s AudioPlayer:D ExoPlayer:D
```

### Playback Issues

1. Check audio output (volume, bluetooth)
2. Verify response has audio data
3. Check audio format (OGG Opus)

## Full Pipeline Log

```bash
adb logcat -c && adb logcat | grep -E "WakeWord|Recorder|VoiceActivity|ApiClient|AudioPlayer"
```

## Test Without Wake Word

Set auth token via ADB and trigger manually:

```bash
# Set token
adb shell am broadcast -a com.jarvis.android.SET_AUTH --es token "mob_xxx"

# Trigger recording (not implemented, use app UI)
```

## Network Debug

```bash
# Check connectivity
adb shell ping on-za-menya.online

# Check DNS
adb shell nslookup on-za-menya.online
```

## Common Issues

| Issue | Check |
|-------|-------|
| No wake word | Porcupine key, microphone permission |
| Recording too short | VAD sensitivity, ambient noise |
| API timeout | Network, backend status |
| No audio playback | Volume, audio focus |
