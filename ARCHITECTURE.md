# Architecture

## Screens

- **Home** (`ui/home`) — recently played games with achievement progress, and achievements
  unlocked in the last 7 days (newest first). Each section has its own loading/empty/error state,
  independent Retry, and a "Go to Settings" link specifically on a rejected API key; pull-to-refresh
  reloads both. Tapping a recently played game opens Game Details.
- **Library** (`ui/library`) — the user's full game library with completion progress and client-side
  title search/filter. Supports pull-to-refresh, stale-data indicators, and tapping a game opens Game Details.
- **Settings** (`ui/settings`) — masked RetroAchievements username/Web API key entry, with Save
  and Clear. Opens automatically on first launch when no valid credentials are stored; otherwise
  reachable via the bottom navigation.
- **Game Details** (`ui/gamedetails`) — box art, title, console, and the full achievement list for
  one game, each achievement showing its badge and earned/locked status (with the real unlock date
  once earned).

Navigation (`ui/MainScreen.kt`) is a four-destination `NavHost` (Home, Library, Settings in bottom
navigation; Game Details — `gameDetails/{gameId}`) driven by `HomeViewModel`'s credentials state
for the first-launch redirect.

## Layers

- `data.model` — plain data classes (`UserCredentials`, `RecentlyPlayedGame`, `RecentUnlock`,
  `GameInfo`, `GameAchievement`, `GameProgressSummary`, `UserCompletionProgressPage`) mapped from
  RetroAchievements API JSON via Gson `@SerializedName`. `GameInfo.achievements` is a
  `Map<String, GameAchievement>` (the API returns achievements as an object keyed by ID, not an array);
  `achievementsSortedForDisplay` turns it into a stable list.
- `data.local` — on-device credential storage (`UserPreferencesRepository` with Tink AEAD encryption,
  keyset protected by Android Keystore) and offline response caching (`ResponseCache` backed by
  `DataStoreCacheDao`).
- `data.remote` — `RetroAchievementsApi` (Retrofit interface), `RetrofitClient` (OkHttp/Gson
  wiring, `RedactingLoggingInterceptor`), and `RetroApiException` (typed failure causes).
- `data.repository` — `RetroAchievementsRepository`, which calls the API and maps failures to
  `RetroApiException` subtypes.
- `ui.home` / `ui.library` / `ui.settings` / `ui.gamedetails` / `ui.components` — Compose screens,
  `AndroidViewModel`s, and shared state composables (`SectionState<T>` for per-section
  loading/success/error, `NetworkImage` for Coil-backed images with a placeholder fallback).

## Data flow

```
Settings screen ──save──> UserPreferencesRepository ──encrypt (Tink/Keystore)──> DataStore
                                      │
                                      ▼ (decrypt on read)
                              UserCredentials (Flow)
                                      │
                                      ▼
                              HomeViewModel / SettingsViewModel
                                      │
                                      ▼
                        RetroAchievementsRepository.getRecentlyPlayed /
                        RetroAchievementsRepository.getRecentUnlocks
                                      │
                                      ▼
                   RetroAchievementsApi (Retrofit) ──HTTP──> retroachievements.org/API/*
                                      │
                                      ▼ (success/failure)
                      RecentlyPlayedGame / RecentUnlock  or  RetroApiException
                                      │
                                      ▼
                              HomeUiState (Compose)
```

Credentials never leave the device except as the `y` query parameter sent directly to
`retroachievements.org` over HTTPS; they are never logged (see `RedactingLoggingInterceptor`).

## Libraries and why

| Library | Used for | Why |
|---|---|---|
| Jetpack Compose + Material3 | UI | Project default; declarative screens/state. |
| Navigation-Compose | Screen navigation | Declarative navigation graph (Home, Library, Settings bottom bar + Game Details). |
| DataStore (Preferences) | Credential & response cache storage | Modern replacement for `SharedPreferences`; async, `Flow`-based, handles encrypted credentials and JSON response cache. |
| Tink (`tink-android`) + Android Keystore | Encrypting stored credentials | Explicit project requirement; AEAD encryption with the keyset itself protected by hardware-backed Keystore, rather than `EncryptedSharedPreferences` (explicitly disallowed). |
| Retrofit + OkHttp + Gson | RetroAchievements API client | Mature, well-understood HTTP stack already proven to build on Android here; the official `api-kotlin` client's Android compatibility isn't confirmed (JitPack-only distribution, undocumented transport engine), so this was the chosen fallback per the project's own working rules. |
| Coil | Game icons / achievement badges | Compose-first image loading; wrapped in `NetworkImage` for a placeholder on a blank path or failed load. |

## Image hosting

The official docs don't state a host for the `Image*`/`BadgeName` paths the API returns. This app
resolves them against `https://media.retroachievements.org`, confirmed empirically by live testing
against a real account (recently played game icons, achievement badges, and game box art all load
correctly from it) rather than from docs or an older wiki page that names a different host.
