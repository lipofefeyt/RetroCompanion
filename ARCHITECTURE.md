# Architecture

## Screens

The bottom navigation has six tabs; Game Details is reached from them.

- **Home** (`ui/home`) — recently played games with achievement progress, and achievements
  unlocked in the last 7 days (newest first, with a search field). Each section has its own
  loading/empty/error state, independent Retry, and a "Go to Settings" link specifically on a
  rejected API key; pull-to-refresh reloads both. Tapping a game opens Game Details.
- **Search** (`ui/search`) — one search field over both the full game library and recently
  unlocked achievements.
- **Library** (`ui/library`) — the user's full game library with completion progress, hardcore
  unlock counts, client-side title search, and favorites pinned to the top. A **Wishlist** filter
  chip switches to the local wishlist (including games never played) and offers a one-way import of
  the site's Want to Play list. Supports pull-to-refresh and stale-data indicators; tapping a game
  opens Game Details.
- **Stats** (`ui/stats`) — points and rank, library totals (completion, hardcore ratio, mastered /
  near-complete counts, per-console averages), a **Closest to Mastery** list, and **Unlock
  Activity**: current/longest daily streak, active days and a 26-week heatmap.
- **Profile** (`ui/profile`) — the user's profile summary (avatar, motto, points, rank).
- **Settings** (`ui/settings`) — masked username/Web API key entry with Save and Clear, the
  System/Light/Dark theme, and the **Unlock notifications** opt-in. Opens automatically on first
  launch when no valid credentials are stored.
- **Game Details** (`ui/gamedetails`) — box art, title, console, and the full achievement list with
  badges, earned/locked status, unlock dates, rarity (% of players), status filters and a rarity
  sort. The top bar has favorite (star), wishlist (bookmark) and, once the game is completed, a
  share action; rare earned achievements (< 10% of players) have their own share button.

Navigation (`ui/MainScreen.kt`) is a `NavHost` driven by `HomeViewModel`'s credentials state for the
first-launch redirect. `MainActivity` also accepts a `GAME_ID` intent extra (set by unlock
notifications) and opens that game's details, whether the app was closed or already running.

## Layers

- `data.model` — plain data classes (`UserCredentials`, `RecentlyPlayedGame`, `RecentUnlock`,
  `GameInfo`, `GameAchievement`, `GameProgressSummary`, `WantToPlayGame`, `WishlistEntry`,
  `UnlockStamp`, ...) mapped from RetroAchievements API JSON via Gson `@SerializedName`.
  `GameInfo.achievements` is a `Map<String, GameAchievement>` (the API returns achievements as an
  object keyed by ID, not an array); `achievementsSortedForDisplay` turns it into a stable list.
- `data.local` — on-device storage, all in DataStore:
  - `UserPreferencesRepository`: credentials (Tink AEAD, keyset protected by Android Keystore),
    favorites, theme and the notification opt-in.
  - `DataStoreWishlistRepository` (`WishlistSource`): the local wishlist, kept with title, console
    and icon so it renders offline.
  - `ResponseCache` over `DataStoreCacheDao`: API responses as JSON for stale-while-revalidate.
- `data.remote` — `RetroAchievementsApi` (Retrofit interface), `RetrofitClient` (OkHttp/Gson
  wiring, `RedactingLoggingInterceptor`), and `RetroApiException` (typed failure causes).
- `data.repository` — `RetroAchievementsRepository`: calls the API, maps failures to
  `RetroApiException` subtypes, and shares identical in-flight requests (see *Rate limiting*).
- `data.polling` — background unlock polling: `UnlockPoller` (one poll), `UnlockDeduper` (pure
  dedup logic) with its `PollingStateStore`, `UnlockPollWorker`/`UnlockPollingScheduler`
  (WorkManager), and `AndroidUnlockNotifier` with `UnlockNotificationPlan` (what to show).
- `ui.<screen>` — Compose screens, `AndroidViewModel`s and UI-state classes. Shared pieces live in
  `ui.components` (`SectionState<T>`, `NetworkImage`, `WishlistIcons`, stale-data banner, ...).
- `ui.share` — share cards: `ShareCardContent` (pure text), `ShareCardRenderer` (Canvas drawing),
  `ShareCardSharer` and `ShareFiles` (PNG in the app cache, shared through a `FileProvider` that
  exposes only `cache/share_cards/`).

Logic that can be pure is kept free of Android types so it is unit tested on the JVM:
`LibraryStats`, `UnlockActivity`/`UnlockChunks` (streaks, heatmap, chunking), `UnlockDeduper`,
`UnlockNotificationPlan`, `ShareCardContent`, `mergeWishlist`.

## Data flow

```
Settings screen ──save──> UserPreferencesRepository ──encrypt (Tink/Keystore)──> DataStore
                                      │
                                      ▼ (decrypt on read)
                              UserCredentials (Flow)
                                      │
                                      ▼
                    Home / Library / Stats / ... ViewModels
                                      │
                                      ▼
                        RetroAchievementsRepository.get*
                                      │
                                      ▼
                   RetroAchievementsApi (Retrofit) ──HTTP──> retroachievements.org/API/*
                                      │
                                      ▼ (success/failure)
                          data models  or  RetroApiException
                                      │
                                      ▼
                       UiState (Compose) ◄── ResponseCache (stale-while-revalidate)
```

Credentials never leave the device except as the `y` query parameter sent directly to
`retroachievements.org` over HTTPS; they are never logged (see `RedactingLoggingInterceptor`).
Share cards and notifications contain only game/achievement details, never the username or key.

### Rate limiting

RetroAchievements rate-limits but publishes no number. Every screen loads when the app starts, so
the repository shares identical in-flight requests (same call, credentials and arguments) between
callers; once one finishes, the next call hits the network again. The Stats heatmap is the largest
consumer, so it fetches 90-day chunks of unlock history one at a time after the lighter sections,
spaces them out, retries a rate-limited chunk with a backoff, and caches each chunk (settled chunks
are never refetched; only the current one is).

### Unlock activity

`API_GetAchievementsEarnedBetween.php` (epoch `f`/`t`) returns per-unlock dates. Dates are read as
UTC and bucketed into local calendar days; this UTC assumption is not stated in the API docs.
Each achievement is counted once (earliest date) so softcore and hardcore unlocks don't double up.

### Background notifications

```
Settings switch ──> permission check (POST_NOTIFICATIONS on Android 13+) ──> UnlockPollingScheduler
                                                                                    │ every ~15 min,
                                                                                    ▼ network required
                                  UnlockPollWorker ──> UnlockPoller ──> 1 request (recent unlocks)
                                                          │
                                       UnlockDeduper + PollingStateStore (seen ids, high-water date)
                                                          │ new unlocks only
                                                          ▼
                                                AndroidUnlockNotifier ──> "Achievement unlocks" channel
```

The first run, a different account, or a gap of more than a day only records what's there and
notifies nothing. Android may delay periodic work well beyond 15 minutes (Doze, battery saver).

### Wishlist

The site's Want to Play list is read-only in the Web API, so the wishlist is local. Importing it is
one-way and merge-only: games already on the wishlist are untouched, nothing is removed, and
nothing is sent back to the site. Clearing credentials in Settings also clears favorites, the
wishlist, the notification opt-in and cached responses.

## Libraries and why

| Library | Used for | Why |
|---|---|---|
| Jetpack Compose + Material3 | UI | Project default; declarative screens/state. |
| Navigation-Compose | Screen navigation | Declarative navigation graph (six tabs + Game Details). |
| DataStore (Preferences) | Credentials, favorites, wishlist, polling state, response cache | Modern replacement for `SharedPreferences`; async, `Flow`-based. |
| Tink (`tink-android`) + Android Keystore | Encrypting stored credentials | Explicit project requirement; AEAD encryption with the keyset protected by hardware-backed Keystore, rather than `EncryptedSharedPreferences` (explicitly disallowed). |
| Retrofit + OkHttp + Gson | RetroAchievements API client | Mature, well-understood HTTP stack already proven to build on Android here; the official `api-kotlin` client's Android compatibility isn't confirmed (JitPack-only distribution, undocumented transport engine), so this was the chosen fallback per the project's own working rules. |
| Coil | Game icons / achievement badges / share-card icons | Compose-first image loading; wrapped in `NetworkImage` for a placeholder on a blank path or failed load. |
| WorkManager | Background unlock polling | Survives restarts and respects battery/network constraints. |
| AndroidX Core (`FileProvider`, `NotificationManagerCompat`) | Share cards, notifications | Standard APIs for sharing files and posting notifications across API levels. |

`minSdk` is 24 and core-library desugaring is not enabled, so date handling uses `TimeZone` and
`SimpleDateFormat` rather than `java.time`.

## Testing

- Unit tests (`app/src/test`, JVM): ViewModels with fakes (`FakeRetroAchievementsApi`,
  `FakeUserCredentialsSource`, `FakeWishlistSource`, `FakeCacheDao`), repositories, parsing of saved
  sample responses (`app/src/test/resources`, fake data only), and all the pure logic listed above.
- Instrumentation tests (`app/src/androidTest`): the share-card renderer, which needs a real
  `Canvas`. Run with `connectedDebugAndroidTest` on a device or emulator.
- CI runs unit tests, the debug build and Android Lint.

## Image hosting

The official docs don't state a host for the `Image*`/`BadgeName` paths the API returns. This app
resolves them against `https://media.retroachievements.org`, confirmed empirically by live testing
against a real account (recently played game icons, achievement badges, and game box art all load
correctly from it) rather than from docs or an older wiki page that names a different host.
