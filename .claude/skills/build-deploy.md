# Skill: Build and Deploy

## When to Use

- "build APK", "deploy to phone", "install app"

## Build Debug APK

```bash
cd /home/danny/Documents/projects/vtoroy-android
./gradlew assembleDebug
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk`

## Install on Device

```bash
# Install (replace existing)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or fresh install
adb uninstall com.vtoroy.android
adb install app/build/outputs/apk/debug/app-debug.apk
```

## One-liner Build + Install

```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## View Logs

```bash
# All Vtoroy logs
adb logcat | grep -E "Vtoroy|WakeWord|VoiceActivity|ApiClient"

# Specific tags
adb logcat -s VtoroyListenerService:D WakeWordManager:D VoiceActivityDetector:D

# Clear and follow
adb logcat -c && adb logcat -s VtoroyListenerService:D
```

## Release Build

```bash
# Requires signing config in build.gradle.kts
./gradlew assembleRelease
```

## Clean Build

```bash
./gradlew clean assembleDebug
```

## Troubleshooting

### Build fails

```bash
# Clean Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Sync project
./gradlew --refresh-dependencies
```

### Install fails

```bash
# Check device connected
adb devices

# Enable USB debugging on device
# Settings → Developer Options → USB Debugging

# Uninstall old version first
adb uninstall com.vtoroy.android
```

### App crashes on start

```bash
# Check crash logs
adb logcat -b crash

# Check for missing permissions
adb logcat | grep "Permission"
```
