# Play Console: Data safety and related forms (draft answers)

These are recommended answers based on what the app actually does (see `docs/privacy.md`). **You enter
and are responsible for the answers in the Play Console.** Google's wording and categories change, so
check each question as you go, and if unsure choose the more conservative (disclosing) answer.

## Privacy policy URL
`https://lipofefeyt.github.io/RetroCompanion/privacy`

## Does the app collect or share user data?
Yes, conservatively: the app sends the user's RetroAchievements **username** and **API key** to
RetroAchievements (a third party) to fetch the user's own data. The developer receives nothing.

| Data type | Collected | Shared | Purpose | Optional | Notes |
|---|---|---|---|---|---|
| Personal info: **User IDs** (RetroAchievements username) | Yes | Yes, with RetroAchievements | App functionality | No | Sent with every request |
| Account credential (the Web API key; if no matching category, declare under "Other info") | Yes | Yes, with RetroAchievements | App functionality | No | Stored encrypted on device; sent only over HTTPS |
| App activity, app interactions, search history | No | No | n/a | n/a | Searches run on the device |
| Location, contacts, photos, files, audio, camera | No | No | n/a | n/a | Not accessed |
| Device or other IDs, advertising ID | No | No | n/a | n/a | No ads or analytics |
| Crash logs / diagnostics | No | No | n/a | n/a | No crash-reporting SDK |

## Security practices
- Data is encrypted in transit: **Yes** (HTTPS only).
- Users can request that data be deleted: the data lives on the device. In-app: Settings, Clear;
  or uninstall. Data held by RetroAchievements is managed on that site.
- Independent security review: No.

## Other declarations
- **Ads:** No.
- **In-app purchases:** No.
- **Target audience:** 13+ / general audience, **not** designed for children.
- **App access:** the app needs a RetroAchievements username and API key to show data. If Google
  asks for access instructions, supply a test account's username and key in the Console's "App
  access" form (never in this repository).
- **Permissions to explain:** `INTERNET` (fetch data), `POST_NOTIFICATIONS` (optional unlock alerts,
  requested only when the user turns them on).
- **Content rating (IARC questionnaire):** answer for a utility/tracker with no violence, no user-to-
  user interaction and no user-generated content features of its own. The app displays game titles and
  achievement descriptions from RetroAchievements; answer the user-generated-content question as
  Google's wording requires.
- **News / government / financial / health app declarations:** None apply.
