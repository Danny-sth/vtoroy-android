# Session Notes

> Persistent memory between Claude Code sessions

## Current Work

_Empty - add notes as you work_

## Decisions Made

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-03-24 | Renamed vtoroy-android → jarvis-android | Full rebrand |
| 2026-03-25 | Using "JARVIS" wake word | Porcupine built-in, best quality |

## Known Issues

| Issue | Status | Notes |
|-------|--------|-------|
| - | - | - |

## Architecture Decisions

- **Wake Word**: Porcupine SDK with built-in "JARVIS" keyword
- **VAD**: Silero (2 second silence detection)
- **Audio**: ExoPlayer for OGG playback
- **Auth**: QR code scan → mobile session token

---

_Update this file when making important decisions or discovering issues._
