# Repository Guidelines

## Project Structure & Module Organization
- `app/` contains the Android app (HTTP bridge + Compose UI). Source lives in `app/src/main/java/com/mbbridge/controller`, resources in `app/src/main/res`.
- Gradle wrapper and build scripts live at repo root.

## Build, Test, and Development Commands
From repo root:
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
- App listens on `127.0.0.1` and supports optional `X-MBBridge-Token` auth. Avoid committing tokens or device-specific secrets.
