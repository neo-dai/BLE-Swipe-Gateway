# Repository Guidelines

## Project Structure & Module Organization
- Root `app/` is a minimal Android app (BLE page turner demo). Source lives in `app/src/main/java/com/example/blepageturner`, resources in `app/src/main/res`.
- `MBBridgeController/` is a separate, full Android app (HTTP bridge + Compose UI). Source lives in `MBBridgeController/app/src/main/java/com/mbbridge/controller`, resources in `MBBridgeController/app/src/main/res`.
- Each app has its own Gradle wrapper and build scripts; run commands from the relevant directory.

## Build, Test, and Development Commands
Root app (from repo root):
- `./gradlew :app:assembleDebug` — build debug APK.
- `./gradlew :app:installDebug` — install to a connected device/emulator.

MBBridgeController (from `MBBridgeController/`):
- `./gradlew assembleDebug` — build debug APK.
- `./gradlew installDebug` — install to a connected device/emulator.
- `./gradlew test` — run JVM unit tests.
- `./gradlew connectedAndroidTest` — run instrumentation tests.
- `./test.sh` or `python3 test.py` — sends local HTTP test commands (requires adb and, for Python, `adb forward tcp:27123 tcp:27123`).

## Coding Style & Naming Conventions
- Kotlin is the primary language. Use 4-space indentation and standard Android/Kotlin style.
- Class names in `UpperCamelCase`; functions/variables in `lowerCamelCase`.
- Compose `@Composable` functions follow `UpperCamelCase` naming.
- Resource files use `lower_snake_case` (e.g., `activity_main.xml`, `ic_launcher.png`).

## Testing Guidelines
- Unit tests use JUnit 4; instrumentation tests use AndroidX Test/Espresso.
- Place unit tests under `app/src/test` and instrumentation tests under `app/src/androidTest`.
- Name test files `*Test.kt` to match Android Studio defaults.

## Commit & Pull Request Guidelines
- Commit messages in history are short, imperative, and capitalized (e.g., "Add ...", "Fix ..."). Follow that pattern.
- PRs should include: a brief description, testing notes (commands run), and screenshots or recordings for UI changes. Link related issues when applicable.

## Security & Configuration Tips
- MBBridgeController listens on `127.0.0.1:27123` and supports optional `X-MBBridge-Token` auth. Avoid committing tokens or device-specific secrets.
