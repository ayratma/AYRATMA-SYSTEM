# AYRATMA SYSTEM

Native Android application foundation (Kotlin + Jetpack Compose) plus a separate local AYRATMA CORE backend (Python standard library).

## Android app
- Package: `com.ayratma.system`
- Minimum Android: API 26
- Current Android UI includes local SQLite notes/tasks, user-selected text-file open/save, and device RAM/storage information.
- AI model inference is not yet integrated into the Android app.

## AYRATMA CORE backend
See [backend/README.md](backend/README.md). The Termux-compatible backend currently provides:
- SQLite persistence for memory, planned tasks, approval records, and events.
- Specialist registry: ANALYST, ENGINEER, GUARD, RESEARCHER, ANDROID_EXPERT, DEBUGGER.
- Deterministic task routing and plan generation.
- Local HTTP endpoints for health, agents, tasks, memory, and approvals.
- Python smoke tests run by GitHub Actions.

Run from Termux:
```sh
cd ~/AYRATMA-SYSTEM/backend
python server.py
```

The server binds to `127.0.0.1:8765` by default and is intended for local use only.

## Not yet integrated
The current specialist router is rule-based, not a generative AI. A local GGUF/llama.cpp runtime, online model provider, Android-to-backend connection, autonomous agent execution, web research connector, cloud deployment, and self-update engine remain future work. The backend does not execute shell commands or make unrestricted file/system changes.

## Build
Pushes to `main` or a manual GitHub Actions run build a debug APK. The workflow also runs backend smoke tests and uploads the APK artifact.
