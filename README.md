# AYRATMA SYSTEM

Android application foundation using Kotlin and Jetpack Compose.

## Current build
- Native Android application with a dark dashboard.
- Module navigation and honest placeholder screens.
- GitHub Actions workflow builds a debug APK.
- Package: `com.ayratma.system`.
- Minimum Android: 8.0 (API 26).

## Status
This is the first installable application shell. CORE AI, local model runtime, SQLite memory, cloud services, and device integrations are not connected yet.

## Build
Pushes to `main` or a manual run in Actions build the debug APK. Download the artifact named `AYRATMA-SYSTEM-debug-apk`.

## Safety
Future device operations use Android-supported APIs, user-selected Storage Access Framework locations, explicit approval for sensitive actions, sandboxing, and audit logs. No root escalation or security bypass is included.
