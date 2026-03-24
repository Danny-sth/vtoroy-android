# Session Notes

> Persistent memory between Claude Code sessions

## Current Work

_Empty - add notes as you work_

## Decisions Made

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-03-24 | Renamed jarvis-android → vtoroy-android | Unified naming |
| 2026-03-24 | Keep "JARVIS" wake word | Porcupine built-in, custom requires training |

## Known Issues

| Issue | Status | Notes |
|-------|--------|-------|
| Wake word is "JARVIS" | By design | Need custom Porcupine model for "Vtoroy" |

## Architecture Decisions

- **Wake Word**: Porcupine SDK with built-in "JARVIS" keyword
- **VAD**: Silero (2 second silence detection)
- **Audio**: ExoPlayer for OGG playback
- **Auth**: QR code scan → mobile session token

---

_Update this file when making important decisions or discovering issues._
