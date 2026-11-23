# RetroCompanion

An Android companion app for [RetroAchievements](https://retroachievements.org) that shows your
recently played games, achievement progress, and recent unlocks, and adds a wishlist, streak
tracking, share cards and unlock notifications.

## Features

- **Home** — recently played games with progress and the last 7 days of unlocks.
- **Search** — one search over your whole library and recently unlocked achievements.
- **Library** — every game with completion progress, favorites (star) and a local **wishlist**
  (bookmark) you can filter to and seed from your RetroAchievements Want to Play list.
- **Stats** — totals, per-console averages, games closest to mastery, daily unlock streaks and a
  26-week heatmap.
- **Game Details** — full achievement list with rarity, filters and sorting; share a completed game
  or a rare achievement as an image card.
- **Unlock notifications** — optional, off by default; checks about every 15 minutes in the
  background and notifies on new unlocks (a notification can arrive up to ~15 minutes after the
  unlock).
- **Profile**, **Settings** (credentials, theme), offline caching with stale-data banners.

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
./gradlew lintDebug            # Android Lint (also run in CI)
./gradlew installDebug         # install on a connected device or emulator
./gradlew connectedDebugAndroidTest   # instrumentation tests (needs a device or emulator)
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

The bottom navigation provides access to **Home**, **Search**, **Library**, **Stats**,
**Profile** and **Settings**. Settings has a **Clear** action that removes stored credentials and,
with them, your favorites, wishlist, notification opt-in and cached data.

### Unlock notifications

Turn on **Unlock notifications** in Settings. On Android 13+ the app explains why and asks for the
notification permission; if you deny it (or notifications are off in system settings) the switch
stays off and a button opens the app's notification settings. Android may delay the background
check well beyond 15 minutes when battery saver or Doze is active.

## Privacy and releasing

See the [privacy policy](docs/privacy.md) for what the app stores and sends (everything stays on the
device except requests to RetroAchievements). [RELEASING.md](RELEASING.md) covers signing and the
release checklist, and `store/` holds the draft Google Play listing text and Data safety answers.

## Built with AI assistance

Most of this app's code was written with [Claude Code](https://claude.com/claude-code), an AI coding
assistant, working from instructions in [CLAUDE.md](CLAUDE.md) and the GitHub issues. Commits it
helped write carry a `Co-Authored-By: Claude` trailer. If that matters to you (for example, a
distribution channel with a policy on AI-generated code), please take it into account.

## Project layout

See [ARCHITECTURE.md](ARCHITECTURE.md) for screens, layers, data flow, and library choices, and
[REQUIREMENTS.md](REQUIREMENTS.md) for the version 1 feature scope. [CLAUDE.md](CLAUDE.md) documents
this project's working rules for AI-assisted changes.
