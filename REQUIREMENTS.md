# Requirements — Version 1

Version 1 scope is tracked as GitHub issues across three milestones. This document summarizes the
functional requirements; see the linked issues for full acceptance criteria.

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

## Non-functional requirements

- The real RetroAchievements API key is never requested, printed, committed, or present in test
  fixtures; tests use sample/fake data only.
- `local.properties` and line-ending-only changes are never committed.
- Any RetroAchievements endpoint or field name relied on is checked against the current docs at
  https://api-docs.retroachievements.org before shipping.
