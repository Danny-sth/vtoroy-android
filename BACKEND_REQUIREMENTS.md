# Backend Requirements (vtoroy-gateway)

## Endpoint: POST /api/voice

### Description
Endpoint for processing voice commands from Android app. Accepts audio file, processes through Vtoroy (STT -> Agent -> TTS) and returns audio response.

### Request

```
POST /api/voice
Authorization: Bearer <token>
Content-Type: multipart/form-data

Body:
  - audio: file (audio/wav)
```

### Response

**Success (200 OK)**
```json
{
  "text": "response text",
  "audio": "base64 encoded ogg"
}
```

**Errors**
- `401 Unauthorized` - invalid or missing token
- `400 Bad Request` - missing or invalid audio file
- `500 Internal Server Error` - processing error

### Processing Logic

```
1. MobileAuth middleware validates Bearer token
2. Get audio file from multipart/form-data
3. Save to temp WAV file
4. STT: whisper-stt CLI → text
5. Send to Vtoroy agent
6. TTS: edge-tts CLI → audio
7. Return JSON with text + base64 audio
8. Clean up temp files
```

### VPS Dependencies
- `whisper-stt` — STT CLI (faster-whisper)
- `edge-tts` — TTS CLI (Microsoft Edge TTS)
- `ffmpeg` — audio conversion

### Testing

```bash
curl -X POST \
  -H "Authorization: Bearer mob_xxx" \
  -F "audio=@test.wav" \
  https://on-za-menya.online/api/voice
```
