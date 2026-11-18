# Requirements — Version 1

Scope is tracked as GitHub issues across milestones. This document summarizes the functional
requirements; see the linked issues for full acceptance criteria.

## M1: Foundation

1. **Secure credential storage** ([#1](https://github.com/lipofefeyt/RetroCompanion/issues/1)) —
   Username and Web API key are stored in DataStore, encrypted with Tink AEAD, with the keyset
   protected by Android Keystore and excluded from Android backups. A decryption failure must not
   crash the app; it falls back to "no credentials stored."
2. **Settings screen** ([#2](https://github.com/lipofefeyt/RetroCompanion/issues/2)) — Masked Web
   API key field, Save/Clear actions, opens automatically on first launch, reachable from Home,
   blocks saving while either field is empty.
3. **RetroAchievements API client** ([#3](https://github.com/lipofefeyt/RetroCompanion/issues/3)) —
   Retrofit/OkHttp/Gson client sending the key as `y`, never logging it, with distinct error types
   for a rejected key, rate limiting, no network, and unexpected responses, verified against saved
   sample responses.
4. **Project documentation** ([#4](https://github.com/lipofefeyt/RetroCompanion/issues/4)) — This
   set of docs.

## M2: Home screen

5. **Recently played games with progress** ([#5](https://github.com/lipofefeyt/RetroCompanion/issues/5))
   — `API_GetUserRecentlyPlayedGames.php` with an explicit `c` (default 10, max 50); games with no
   achievements render without error.
6. **Achievements unlocked in the last 7 days** ([#6](https://github.com/lipofefeyt/RetroCompanion/issues/6))
   — `API_GetUserRecentAchievements.php` with `m=10080` always sent explicitly; newest first.
7. **Loading/empty/error states per section** ([#7](https://github.com/lipofefeyt/RetroCompanion/issues/7))
   — Each Home section has its own state; a rejected key links to Settings; Retry and
   pull-to-refresh; one section's failure doesn't hide the other's data.

## M3: Game details

8. **Game details screen** ([#8](https://github.com/lipofefeyt/RetroCompanion/issues/8)) —
   `API_GetGameInfoAndUserProgress.php`; `Achievements` is parsed as an object keyed by achievement
   ID; `DateEarned`/`DateEarnedHardcore` are only present once earned.
9. **Game icons and achievement badges** ([#9](https://github.com/lipofefeyt/RetroCompanion/issues/9))
   — Coil-loaded images with placeholders for missing paths; image host confirmed against current
   docs/live response before shipping.

## M4: Offline Caching & Game Library

10. **Offline response caching** — `ResponseCache` backed by DataStore (`CacheDao`) persists
    API responses as JSON. Implements stale-while-revalidate: stale data displays immediately
    with a relative-time banner while fresh data revalidates in the background or when network fails.
11. **Game library screen** — Full game completion progress via `API_GetUserCompletionProgress.php`,
    paged through up to a safety cap; client-side title search; progress indicators; reachable
    via the bottom navigation "Library" tab; tapping a game opens Game Details.

## Favorites, profile, theme and search

12. **Favorites** ([#15](https://github.com/lipofefeyt/RetroCompanion/issues/15)) — Star a game from
    Game Details or the Library; favorites are pinned at the top of the Library.
13. **Profile screen** ([#16](https://github.com/lipofefeyt/RetroCompanion/issues/16)) — Avatar,
    motto, points and rank.
14. **Theme toggle** ([#17](https://github.com/lipofefeyt/RetroCompanion/issues/17)) —
    System/Light/Dark, saved on the device.
15. **Search and rarity** ([#18](https://github.com/lipofefeyt/RetroCompanion/issues/18),
    [#19](https://github.com/lipofefeyt/RetroCompanion/issues/19),
    [#20](https://github.com/lipofefeyt/RetroCompanion/issues/20),
    [#23](https://github.com/lipofefeyt/RetroCompanion/issues/23)) — Search recent unlocks, show
    achievement rarity with a rarity sort, hardcore counts on Library rows, and a unified Search tab.
16. **Stats and polish** ([#21](https://github.com/lipofefeyt/RetroCompanion/issues/21),
    [#22](https://github.com/lipofefeyt/RetroCompanion/issues/22)) — A Stats tab aggregating library
    and profile data; screen transitions, empty states and refresh haptics.
17. **CI** ([#10](https://github.com/lipofefeyt/RetroCompanion/issues/10),
    [#24](https://github.com/lipofefeyt/RetroCompanion/issues/24)) — Unit tests, debug build and
    Android Lint on every push and pull request.

## Wishlist & backlog

18. **Local wishlist** ([#25](https://github.com/lipofefeyt/RetroCompanion/issues/25)) — A bookmark
    toggle on Game Details and Library rows, stored on-device (the site's Want to Play list is
    read-only in the Web API). Separate from Favorites.
19. **Wishlist filter** ([#26](https://github.com/lipofefeyt/RetroCompanion/issues/26)) — A Library
    filter that also lists never-played games and works offline.
20. **Want to Play import** ([#27](https://github.com/lipofefeyt/RetroCompanion/issues/27)) —
    One-way, merge-only import via `API_GetUserWantToPlayList.php` (paged, `c` max 500). Nothing is
    removed locally and nothing is sent back to the site.

## Streaks & milestones

21. **Unlock streaks and heatmap** ([#28](https://github.com/lipofefeyt/RetroCompanion/issues/28)) —
    Current/longest daily streak, active days and a 26-week heatmap from
    `API_GetAchievementsEarnedBetween.php`, fetched in cached 90-day chunks. Unlock dates are read
    as UTC (not stated in the API docs) and bucketed into local days.
22. **Closest to mastery** ([#29](https://github.com/lipofefeyt/RetroCompanion/issues/29)) — The
    top started-but-unmastered games by exact completion, with no extra API call.

## Share cards

23. **Share as an image** ([#30](https://github.com/lipofefeyt/RetroCompanion/issues/30)) — Share a
    completed game ("Mastered" if all hardcore, else "Completed") or an earned achievement under 10%
    rarity as a PNG through a `FileProvider` limited to `cache/share_cards/`. Cards carry no
    account details.

## Unlock notifications

24. **Background polling** ([#31](https://github.com/lipofefeyt/RetroCompanion/issues/31)) — A
    periodic WorkManager job makes one `API_GetUserRecentAchievements.php` request per run and
    never notifies twice for an achievement or about a backlog.
25. **Opt-in and notifications** ([#32](https://github.com/lipofefeyt/RetroCompanion/issues/32)) — A
    Settings switch (off by default) that requests `POST_NOTIFICATIONS` on Android 13+, a dedicated
    "Achievement unlocks" channel, and tap-through to the game's details.

## Non-functional requirements

- The real RetroAchievements API key is never requested, printed, committed, or present in test
  fixtures; tests use sample/fake data only.
- `local.properties` and line-ending-only changes are never committed.
- Any RetroAchievements endpoint or field name relied on is checked against the current docs at
  https://api-docs.retroachievements.org before shipping.
- Requests stay modest: RetroAchievements rate-limits without publishing a limit, so identical
  concurrent requests are shared, bulk history is cached in chunks, and background polling makes one
  request per run.
- Clearing credentials removes everything tied to the account: favorites, wishlist, notification
  opt-in, polling schedule and cached responses.
