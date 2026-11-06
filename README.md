# RetroCompanion

An Android companion app for [RetroAchievements](https://retroachievements.org) that shows your
recently played games, achievement progress, and recent unlocks.

## Prerequisites

- Android Studio (current stable) or a JDK 11+ toolchain with the Android SDK installed.
- Android SDK Platform matching `compileSdk`/`targetSdk` 37 (see `app/build.gradle.kts`).
- A RetroAchievements account and [Web API key](https://retroachievements.org/settings) (Settings
  → Keys on the RetroAchievements site). **Never commit this key.** It's entered at runtime in the
  app's Settings screen and stored encrypted on-device — it never belongs in source control,
  `local.properties`, or test files.

## Setup

1. Clone the repo and open it in Android Studio, **or** set `sdk.dir` in `local.properties` to
   your local Android SDK path (this file is git-ignored; never commit it).
2. Sync Gradle.

## Build, run, and test

From Android Studio: use the standard Run/Debug and the Gradle panel's `testDebugUnitTest` task.

From the command line:

```sh
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run unit tests (JVM, no device needed)
```

**WSL note:** if the Android SDK lives on the Windows side (e.g. installed via Android Studio on
Windows, with `sdk.dir` in `local.properties` pointing at a Windows path), `./gradlew` run directly
from a WSL shell won't be able to resolve that SDK path. In that case, run the Windows wrapper
through `cmd.exe` from the project's `/mnt/c/...` path instead:

```sh
cmd.exe /c "gradlew.bat assembleDebug --console=plain"
cmd.exe /c "gradlew.bat testDebugUnitTest --console=plain"
```

Do not edit `sdk.dir` to "fix" this — it's meant to point at wherever the SDK actually lives for
whichever toolchain (WSL or Windows) runs the build.

## First-run flow

On first launch (no credentials stored yet), the app opens directly to **Settings**. Enter your
RetroAchievements username and Web API key and tap **Save**. The app then shows **Home**, with:

- Recently played games and achievement progress.
- Achievements unlocked in the last 7 days.

The bottom navigation provides access to **Home**, your full **Library** (with title search and
progress tracking), and **Settings** (with a **Clear** action to remove stored credentials).

## Project layout

See [ARCHITECTURE.md](ARCHITECTURE.md) for screens, layers, data flow, and library choices, and
[REQUIREMENTS.md](REQUIREMENTS.md) for the version 1 feature scope. [CLAUDE.md](CLAUDE.md) documents
this project's working rules for AI-assisted changes.
