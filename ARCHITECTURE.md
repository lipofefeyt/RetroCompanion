# Architecture

## Screens

- **Home** (`ui/home`) — recently played games with achievement progress, and achievements
  unlocked in the last 7 days. Shows loading/missing-credentials/error/success states.
- **Settings** (`ui/settings`) — masked RetroAchievements username/Web API key entry, with Save
  and Clear. Opens automatically on first launch when no valid credentials are stored; otherwise
  reachable from Home via the bottom navigation.

Navigation (`ui/MainScreen.kt`) is a simple two-destination `NavHost` (Home, Settings) driven by
`HomeViewModel`'s credentials state for the first-launch redirect.

## Layers

- `data.model` — plain data classes (`UserCredentials`, `RecentlyPlayedGame`, `RecentUnlock`)
  mapped from RetroAchievements API JSON via Gson `@SerializedName`.
- `data.local` — on-device credential storage: `UserPreferencesRepository` (DataStore) and
  `CredentialsCipher`/`AndroidKeystoreCredentialsCipherFactory` (Tink AEAD encryption, keyset
  protected by Android Keystore).
- `data.remote` — `RetroAchievementsApi` (Retrofit interface), `RetrofitClient` (OkHttp/Gson
  wiring, `RedactingLoggingInterceptor`), and `RetroApiException` (typed failure causes).
- `data.repository` — `RetroAchievementsRepository`, which calls the API and maps failures to
  `RetroApiException` subtypes.
- `ui.home` / `ui.settings` / `ui.components` — Compose screens, `AndroidViewModel`s, and shared
  state composables (loading/empty/error).

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
| Navigation-Compose | Home ↔ Settings | Minimal two-destination graph. |
| DataStore (Preferences) | Credential storage | Modern replacement for `SharedPreferences`; async, `Flow`-based. |
| Tink (`tink-android`) + Android Keystore | Encrypting stored credentials | Explicit project requirement; AEAD encryption with the keyset itself protected by hardware-backed Keystore, rather than `EncryptedSharedPreferences` (explicitly disallowed). |
| Retrofit + OkHttp + Gson | RetroAchievements API client | Mature, well-understood HTTP stack already proven to build on Android here; the official `api-kotlin` client's Android compatibility isn't confirmed (JitPack-only distribution, undocumented transport engine), so this was the chosen fallback per the project's own working rules. |
| Coil | Game icons / achievement badges (M3) | Compose-first image loading. |

## Known gaps / M2-M3 scope

- Home screen sections share one combined loading/error state (M2 issue: per-section states, retry,
  pull-to-refresh).
- No game details screen yet (M3).
- The image host used for icon/badge URLs (`media.retroachievements.org`) is inferred from model
  code, not yet confirmed against current docs or a live response (M3 issue).
