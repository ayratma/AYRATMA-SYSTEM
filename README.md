# AYRATMA SYSTEM

Native Android application (Kotlin + Jetpack Compose) with a Termux-compatible local AYRATMA CORE backend.

## Android dashboard v0.3.0
The dashboard now has working navigation and screens for:
- CORE AI chat (through the local backend)
- Specialist agent catalogue
- Persistent CORE memory
- Task planning
- SAF file open and save
- Device RAM and storage monitoring
- Approval records
- Evolution, Cloud, and Project Centre status/planning screens

The Android app connects to `http://127.0.0.1:8765`. Android cleartext traffic is disabled generally and permitted only for loopback addresses in the network security configuration.

## Start the backend in Termux
From the repository's backend folder:

```sh
pkg install python
python server.py
```

The backend listens on `127.0.0.1:8765`. Keep Termux running while using the connected screens.

## Local AI model
The backend's `/chat` endpoint uses a user-started local llama.cpp OpenAI-compatible server at `127.0.0.1:8080`. The model runtime and GGUF weights are not bundled or downloaded automatically. Without that server, the app reports that the model is unavailable instead of fabricating an AI answer.

## Current limitations
- Specialist routing is deterministic; the catalogue is not six independently trained models.
- Android chat uses one local model endpoint with a specialist system prompt; full multi-agent tool orchestration is not yet implemented.
- Cloud sync, autonomous self-development, automatic web research, and automatic APK updates are not yet implemented.
- Evolution and Cloud screens are honest status/planning surfaces, not active remote services.
- File access is user-selected through Android's Storage Access Framework. No root or protected app-data access is provided.
- Critical changes require an explicit approval flow; no unrestricted shell execution is exposed.

## Build
A push to `main` triggers GitHub Actions to run backend smoke tests and build a debug APK artifact/release.
